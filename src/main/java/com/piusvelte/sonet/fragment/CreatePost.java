package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.loader.PhotoPathLoader;
import com.piusvelte.sonet.loader.SendPostLoader;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Widgets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.TWITTER;

/**
 * Created by bemmanuel on 6/4/15.
 */
public class CreatePost extends Fragment implements TextWatcher, View.OnKeyListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ACCOUNT = 0;
    private static final int LOADER_SEND_POST = 1;
    private static final int LOADER_PHOTO = 2;

    private static final String FRAGMENT_CHOOSE_POST_ACCOUNTS = "fragment:choose_post_accounts";

    private static final String DIALOG_CHOOSE_LOCATION_ACCOUNT = "dialog:choose_location_account";
    private static final String DIALOG_CHOOSE_LOCATION = "dialog:choose_location";

    private static final String LOADER_ARG_ACCOUNT_ID = "account_id";
    private static final String LOADER_ARG_PHOTO_URI = "photo_uri";
    private static final String LOADER_ARG_ACCOUNTS = "accounts";
    private static final String LOADER_ARG_MESSAGE = "message";

    private static final int REQUEST_CHOOSE_POST_ACCOUNTS = 0;
    private static final int REQUEST_CHOOSE_LOCATION = 1;
    private static final int REQUEST_PHOTO = 2;
//    private static final int REQUEST_TAGS = 3;

    private static final String STATE_MESSAGE = "state:message";

    // TODO move this to Client implementations
    private static final List<Integer> sPhotoSupported = new ArrayList<>();
    private static final List<Integer> sTaggingSupported = new ArrayList<>();

    static {
        sPhotoSupported.add(FACEBOOK);
        sTaggingSupported.add(FACEBOOK);
    }

    private HashSet<ChoosePostAccounts.Account> mAccounts = new HashSet<>();
    private EditText mMessage;
    private TextView mCount;
    private View mLoadingView;
    private String mPhotoPath;

    private SendPostLoaderCallbacks mSendPostLoaderCallbacks = new SendPostLoaderCallbacks(this);
    private PhotoPathLoaderCallbacks mPhotoPathLoaderCallbacks = new PhotoPathLoaderCallbacks(this);

    public static CreatePost newInstance(@Nullable Bundle extras) {
        CreatePost createPost = new CreatePost();
        Bundle args = new Bundle();

        if (extras != null) {
            args.putAll(extras);
        }

        createPost.setArguments(extras);
        return createPost;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.post, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessage = (EditText) view.findViewById(R.id.message);
        mCount = (TextView) view.findViewById(R.id.count);
        mLoadingView = view.findViewById(R.id.loading);

        mMessage.addTextChangedListener(this);
        mMessage.setOnKeyListener(this);

        LoaderManager loaderManager = getLoaderManager();

        if (savedInstanceState != null) {
            mMessage.setText(savedInstanceState.getString(STATE_MESSAGE));

            if (loaderManager.hasRunningLoaders()) {
                // TODO test this! >_<
                if (loaderManager.getLoader(LOADER_SEND_POST) != null) {
                    loaderManager.initLoader(LOADER_SEND_POST, null, mSendPostLoaderCallbacks);
                } else if (loaderManager.getLoader(LOADER_PHOTO) != null) {
                    loaderManager.initLoader(LOADER_PHOTO, null, mPhotoPathLoaderCallbacks);
                } else if (loaderManager.getLoader(LOADER_ACCOUNT) != null) {
                    loaderManager.initLoader(LOADER_ACCOUNT, null, this);
                }
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        mMessage = null;
        mCount = null;
        mLoadingView = null;
        super.onDestroyView();
    }

    // TODO support passing in arguments
    private void handleIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if ((action != null) && action.equals(Intent.ACTION_SEND)) {
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    getPhoto((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
                }

                if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                    final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                    mMessage.setText(text);
                    mCount.setText(Integer.toString(text.length()));
                }

                chooseAccounts();
            } else {
                Uri data = intent.getData();

                if ((data != null) && data.toString().contains(Accounts.getContentUri(getActivity()).toString())) {
                    Bundle args = new Bundle();
                    args.putString(LOADER_ARG_ACCOUNT_ID, data.getLastPathSegment());
                    getLoaderManager().restartLoader(LOADER_ACCOUNT, args, this);
                    mLoadingView.setVisibility(View.VISIBLE);
                } else if (intent.hasExtra(Widgets.INSTANT_UPLOAD)) {
                    // check if a photo path was passed and prompt user to select the account
                    setPhoto(intent.getStringExtra(Widgets.INSTANT_UPLOAD));
                    chooseAccounts();
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_post, menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_MESSAGE, mMessage.getText().toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Fragment fragment;
        switch (item.getItemId()) {
            case android.R.id.home:
                fragment = getFragmentManager().findFragmentByTag(FRAGMENT_CHOOSE_POST_ACCOUNTS);

                if (fragment != null) {
                    getFragmentManager().popBackStack();
                } else {
                    getActivity().finish();
                }
                return true;

            case R.id.menu_post_accounts:
                chooseAccounts();
                return true;

            case R.id.menu_post_photo:
                boolean supported = false;

                for (ChoosePostAccounts.Account account : mAccounts) {
                    supported = sPhotoSupported.contains(account.service);

                    if (supported) {
                        break;
                    }
                }

                if (supported) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_PHOTO);
                } else {
                    unsupportedToast(sPhotoSupported);
                }
                return true;

            case R.id.menu_send:
                if (!mAccounts.isEmpty()) {
                    Bundle args = new Bundle();
                    args.putParcelableArrayList(LOADER_ARG_ACCOUNTS, new ArrayList<>(mAccounts));
                    args.putString(LOADER_ARG_MESSAGE, mMessage.getText().toString());
                    args.putString(LOADER_ARG_PHOTO_URI, mPhotoPath);
                    getLoaderManager().restartLoader(LOADER_SEND_POST, args, mSendPostLoaderCallbacks);
                    mLoadingView.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getActivity(), "no accounts selected", Toast.LENGTH_LONG).show();
                }
                return true;

//		} else if (itemId == R.id.menu_post_tags) {
//			if (mAccountsService.size() == 1) {
//				if (sTaggingSupported.contains(mAccountsService.values().iterator().next()))
//					selectFriends(mAccountsService.keySet().iterator().next());
//				else
//					unsupportedToast(sTaggingSupported);
//			} else {
//				// dialog to select an account
//				Iterator<Long> accountIds = mAccountsService.keySet().iterator();
//				HashMap<Long, String> accountEntries = new HashMap<Long, String>();
//				while (accountIds.hasNext()) {
//					Long accountId = accountIds.next();
//					Cursor account = this.getContentResolver().query(Accounts.getContentUri(this), new String[]{Accounts._ID, ACCOUNTS_QUERY},
// Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
//					if (account.moveToFirst() && sTaggingSupported.contains(mAccountsService.get(accountId)))
//						accountEntries.put(account.getLong(0), account.getString(1));
//				}
//				int size = accountEntries.size();
//				if (size != 0) {
//					final long[] accountIndexes = new long[size];
//					final String[] accounts = new String[size];
//					int i = 0;
//					Iterator<Map.Entry<Long, String>> entries = accountEntries.entrySet().iterator();
//					while (entries.hasNext()) {
//						Map.Entry<Long, String> entry = entries.next();
//						accountIndexes[i] = entry.getKey();
//						accounts[i++] = entry.getValue();
//					}
//					mDialog = (new AlertDialog.Builder(this))
//							.setTitle(R.string.accounts)
//							.setSingleChoiceItems(accounts, -1, new DialogInterface.OnClickListener() {
//								@Override
//								public void onClick(DialogInterface dialog, int which) {
//									selectFriends(accountIndexes[which]);
//									dialog.dismiss();
//								}
//							})
//							.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
//								@Override
//								public void onClick(DialogInterface dialog, int which) {
//									dialog.dismiss();
//								}
//							})
//							.create();
//					mDialog.show();
//				} else
//					unsupportedToast(sTaggingSupported);
//			}

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void chooseAccounts() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);

        Fragment fragment = ChoosePostAccounts.newInstance(REQUEST_CHOOSE_POST_ACCOUNTS, mAccounts);
        fragment.setTargetFragment(this, REQUEST_CHOOSE_POST_ACCOUNTS);
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container,
                        fragment,
                        FRAGMENT_CHOOSE_POST_ACCOUNTS)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // NO-OP
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // NO-OP
    }

    @Override
    public void afterTextChanged(Editable s) {
        mCount.setText(Integer.toString(s.toString().length()));
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        mCount.setText(Integer.toString(mMessage.getText().toString().length()));
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ACCOUNT:
                return new CursorLoader(getActivity(),
                        Accounts.getContentUri(getActivity()),
                        new String[] { Accounts._ID, Accounts.SERVICE },
                        Accounts._ID + "=?",
                        new String[] { args.getString(LOADER_ARG_ACCOUNT_ID) },
                        null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ACCOUNT:
                mLoadingView.setVisibility(View.GONE);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        ChoosePostAccounts.Account account = new ChoosePostAccounts.Account();
                        account.id = cursor.getLong(cursor.getColumnIndexOrThrow(Accounts._ID));
                        account.service = cursor.getInt(cursor.getColumnIndexOrThrow(Accounts.SERVICE));
                        mAccounts.add(account);
                    }
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // NO-OP
    }

    private static class SendPostLoaderCallbacks implements LoaderManager.LoaderCallbacks<Boolean> {

        private CreatePost mCreatePost;

        public SendPostLoaderCallbacks(@NonNull CreatePost createPost) {
            mCreatePost = createPost;
        }

        @Override
        public Loader<Boolean> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_SEND_POST:
                    ArrayList<ChoosePostAccounts.Account> accounts = args.getParcelableArrayList(LOADER_ARG_ACCOUNTS);
                    return new SendPostLoader(mCreatePost.getActivity(),
                            accounts,
                            args.getString(LOADER_ARG_MESSAGE),
                            args.getString(LOADER_ARG_PHOTO_URI));

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Boolean> loader, Boolean data) {
            switch (loader.getId()) {
                case LOADER_SEND_POST:
                    mCreatePost.onPostSendResult(data);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Boolean> loader) {
            // NO-OP
        }
    }

    private void unsupportedToast(List<Integer> supportedServices) {
        StringBuilder message = new StringBuilder();
        message.append("This feature is currently supported only for ");

        for (int i = 0, l = supportedServices.size(); i < l; i++) {
            message.append(Sonet.getServiceName(getResources(), supportedServices.get(i)));

            if (i == (l - 1)) {
                message.append(".");
            } else if (i == (l - 2)) {
                message.append(", and ");
            } else {
                message.append(", ");
            }
        }

        Toast.makeText(getActivity(), message.toString(), Toast.LENGTH_LONG).show();
    }

    protected void getPhoto(Uri uri) {
        Bundle args = new Bundle();
        args.putString(LOADER_ARG_PHOTO_URI, uri.toString());
        getLoaderManager().restartLoader(LOADER_PHOTO, args, mPhotoPathLoaderCallbacks);
        mLoadingView.setVisibility(View.VISIBLE);
    }

    protected void setPhoto(String path) {
        mPhotoPath = path;
        Toast.makeText(getActivity(), "Currently, the photo will only be uploaded Facebook accounts.", Toast.LENGTH_LONG).show();
    }

    private void setPhotoPath(String path) {
        mLoadingView.setVisibility(View.GONE);

        if (!TextUtils.isEmpty(path)) {
            setPhoto(path);
        } else {
            Toast.makeText(getActivity(), "error retrieving the photo path", Toast.LENGTH_LONG).show();
        }
    }

    private void onPostSendResult(Boolean result) {
        mLoadingView.setVisibility(View.GONE);

        if (Boolean.TRUE.equals(result)) {
            Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_LONG).show();
        }

        getActivity().finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    getPhoto(data.getData());
                }
                break;

//            case REQUEST_TAGS:
//                if (resultCode == Activity.RESULT_OK && data.hasExtra(Stags) && data.hasExtra(Accounts.SID)) {
//                    long id = data.getLongExtra(Accounts.SID, Sonet.INVALID_ACCOUNT_ID);
//
//                    for (ChoosePostAccounts.Account account : mAccounts) {
//                        if (account.id == id) {
//                            account.tags = Arrays.asList(data.getStringArrayExtra(Stags));
//                            break;
//                        }
//                    }
//                }
//                break;

            case REQUEST_CHOOSE_POST_ACCOUNTS:
                if (resultCode == Activity.RESULT_OK) {
                    mAccounts.clear();
                    List<ChoosePostAccounts.Account> selectedAccounts = ChoosePostAccounts.getAccounts(data);

                    if (selectedAccounts != null) {
                        mAccounts.addAll(selectedAccounts);
                    }
                }
                break;

            case REQUEST_CHOOSE_LOCATION:
                if (resultCode == Activity.RESULT_OK) {
                    long accountId = ChooseLocation.getAccountId(data);
                    String latitude = ChooseLocation.getLatitude(data);
                    String longitude = ChooseLocation.getLongitude(data);
                    String location = ChooseLocation.getLocation(data);

                    for (ChoosePostAccounts.Account account : mAccounts) {
                        if (account.id == accountId) {
                            account.latitude = latitude;
                            account.longitude = longitude;
                            account.location = location;
                            break;
                        }
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private static class PhotoPathLoaderCallbacks implements LoaderManager.LoaderCallbacks<String> {

        private CreatePost mCreatePost;

        PhotoPathLoaderCallbacks(@NonNull CreatePost createPost) {
            mCreatePost = createPost;
        }

        @Override
        public Loader<String> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_PHOTO:
                    return new PhotoPathLoader(mCreatePost.getActivity(), Uri.parse(args.getString(LOADER_ARG_PHOTO_URI)));

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String data) {
            switch (loader.getId()) {
                case LOADER_PHOTO:
                    mCreatePost.setPhotoPath(data);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
            // NO-OP
        }
    }
}
