/*
 * Sonet - Android Social Networking Widget
 * Copyright (C) 2009 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.sonet;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.piusvelte.sonet.fragment.BaseDialogFragment;
import com.piusvelte.sonet.fragment.ChooseAccount;
import com.piusvelte.sonet.fragment.ChooseLocation;
import com.piusvelte.sonet.fragment.ChoosePostAccounts;
import com.piusvelte.sonet.loader.SendPostLoader;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.INVALID_ACCOUNT_ID;
import static com.piusvelte.sonet.Sonet.PRO;
import static com.piusvelte.sonet.Sonet.Stags;
import static com.piusvelte.sonet.Sonet.TWITTER;

public class SonetCreatePost extends BaseActivity
        implements OnKeyListener, OnClickListener, TextWatcher, LoaderManager.LoaderCallbacks {
    private static final String TAG = "SonetCreatePost";

    private static final int LOADER_ACCOUNT = 0;
    private static final int LOADER_SEND_POST = 2;
    private static final int LOADER_PHOTO = 3;

    private static final String DIALOG_CHOOSE_LOCATION_ACCOUNT = "dialog:choose_location_account";
    private static final String DIALOG_CHOOSE_POST_ACCOUNTS = "dialog:choose_post_accounts";
    private static final String DIALOG_CHOOSE_LOCATION = "dialog:choose_location";

    private static final String LOADER_ARG_ACCOUNT_ID = "account_id";
    private static final String LOADER_ARG_PHOTO_URI = "photo_uri";

    private static final int REQUEST_CHOOSE_LOCATION_ACCOUNT = 0;
    private static final int REQUEST_CHOOSE_POST_ACCOUNTS = 1;
    private static final int REQUEST_CHOOSE_LOCATION = 2;

    private static final String STATE_PENDING_LOADERS = "state:pending_loaders";
    private static final String STATE_MESSAGE = "state:message";

    private HashSet<ChoosePostAccounts.Account> mAccounts = new HashSet<>();
    private EditText mMessage;
    private ImageButton mSend;
    private TextView mCount;
    private View mLoadingView;
    private static final int PHOTO = 1;
    private static final int TAGS = 2;
    private String mPhotoPath;

    @NonNull
    private Set<Integer> mPendingLoaders = new HashSet<>();

    // TODO move this to Client implementations
    private static final List<Integer> sLocationSupported = new ArrayList<>();
    private static final List<Integer> sPhotoSupported = new ArrayList<>();
    private static final List<Integer> sTaggingSupported = new ArrayList<>();

    static {
        sLocationSupported.add(TWITTER);
        sLocationSupported.add(FACEBOOK);
        sLocationSupported.add(FOURSQUARE);
        sPhotoSupported.add(FACEBOOK);
        sTaggingSupported.add(FACEBOOK);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // allow posting to multiple services if an account is defined
        // allow selecting which accounts to use
        // get existing comments, allow liking|unliking those comments
        setContentView(R.layout.post);

        if (!getPackageName().toLowerCase().contains(PRO)) {
            AdView adView = new AdView(this, AdSize.BANNER, BuildConfig.GOOGLEAD_ID);
            ((FrameLayout) findViewById(R.id.ad)).addView(adView);
            adView.loadAd(new AdRequest());
        }

        mMessage = (EditText) findViewById(R.id.message);
        mSend = (ImageButton) findViewById(R.id.send);
        mCount = (TextView) findViewById(R.id.count);
        mLoadingView = findViewById(R.id.loading);

        mMessage.addTextChangedListener(this);
        mMessage.setOnKeyListener(this);
        mSend.setOnClickListener(this);
        setResult(RESULT_OK);
        handleIntent(getIntent());

        LoaderManager loaderManager = getSupportLoaderManager();

        if (savedInstanceState != null) {
            mMessage.setText(savedInstanceState.getString(STATE_MESSAGE));

            if (loaderManager.hasRunningLoaders()) {
                int[] loaders = savedInstanceState.getIntArray(STATE_PENDING_LOADERS);

                if (loaders != null) {
                    for (int loader : loaders) {
                        mPendingLoaders.add(loader);
                        loaderManager.initLoader(loader, null, this);
                    }
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int loaderIndex = 0;
        int[] loaders = new int[mPendingLoaders.size()];

        for (Integer loader : mPendingLoaders) {
            loaders[loaderIndex] = loader;
            loaderIndex++;
        }

        outState.putIntArray(STATE_PENDING_LOADERS, loaders);
        outState.putString(STATE_MESSAGE, mMessage.getText().toString());
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

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

                if ((data != null) && data.toString().contains(Accounts.getContentUri(this).toString())) {
                    Bundle args = new Bundle();
                    args.putString(LOADER_ARG_ACCOUNT_ID, data.getLastPathSegment());
                    getSupportLoaderManager().restartLoader(LOADER_ACCOUNT, args, this);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_post, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_post_accounts) {
            chooseAccounts();
        } else if (itemId == R.id.menu_post_photo) {
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
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PHOTO);
            } else {
                unsupportedToast(sPhotoSupported);
            }
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
        } else if (itemId == R.id.menu_post_location) {
            LocationManager locationManager = (LocationManager) SonetCreatePost.this.getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                if (mAccounts.size() == 1) {
                    ChoosePostAccounts.Account account = mAccounts.iterator().next();
                    if (sLocationSupported.contains(account.service)) {
                        setLocation(account.id);
                    } else {
                        unsupportedToast(sLocationSupported);
                    }
                } else {
                    int index = 0;
                    List<Long> supportedAccounts = new ArrayList<>();

                    for (ChoosePostAccounts.Account account : mAccounts) {
                        if (sLocationSupported.contains(account.service)) {
                            supportedAccounts.add(account.id);
                        }
                    }

                    long[] ids = new long[supportedAccounts.size()];

                    for (Long id : supportedAccounts) {
                        ids[index] = id;
                        index++;
                    }

                    getSupportFragmentManager().beginTransaction()
                            .add(ChooseAccount.newInstance(REQUEST_CHOOSE_LOCATION_ACCOUNT, ids), DIALOG_CHOOSE_LOCATION_ACCOUNT)
                            .addToBackStack(null)
                            .commit();
                }
            } else {
                (Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_LONG)).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setLocation(final long accountId) {
        String latitude;
        String longitude;

        // TODO FusedLocationProvider
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (location != null) {
            latitude = Double.toString(location.getLatitude());
            longitude = Double.toString(location.getLongitude());
        } else {
            latitude = null;
            longitude = null;
        }

        if (!TextUtils.isEmpty(latitude) && !TextUtils.isEmpty(longitude)) {

            getSupportFragmentManager().beginTransaction()
                    .add(ChooseLocation.newInstance(REQUEST_CHOOSE_LOCATION, accountId, latitude, longitude), DIALOG_CHOOSE_LOCATION)
                    .addToBackStack(null)
                    .commit();
        }// TODO else, no location available
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

        Toast.makeText(getApplicationContext(), message.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View v) {
        if (v == mSend) {
            if (!mAccounts.isEmpty()) {
                getSupportLoaderManager().restartLoader(LOADER_SEND_POST, null, this);
                mLoadingView.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(SonetCreatePost.this, "no accounts selected", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void getPhoto(Uri uri) {
        // TODO some file manages send the path through the uri.getPath()
        Bundle args = new Bundle();
        args.putString(LOADER_ARG_PHOTO_URI, uri.toString());
        getSupportLoaderManager().restartLoader(LOADER_PHOTO, args, this);
        mLoadingView.setVisibility(View.VISIBLE);
    }

    protected void setPhoto(String path) {
        mPhotoPath = path;
        (Toast.makeText(SonetCreatePost.this, "Currently, the photo will only be uploaded Facebook accounts.", Toast.LENGTH_LONG)).show();
    }

    protected void selectFriends(long accountId) {
        // TODO unsupported
//        if ((mAccountsService.get(accountId) == FACEBOOK) && (!mAccountsLocation.containsKey(accountId) || (mAccountsLocation
//                .get(accountId) == null))) {
//            (Toast.makeText(SonetCreatePost.this, "To tag friends, Facebook requires a location to be included.", Toast.LENGTH_LONG)).show();
//        } else {
//            startActivityForResult(
//                    new Intent(this, SelectFriends.class).putExtra(Accounts.SID, accountId).putExtra(Stags, mAccountsTags.get(accountId)),
//                    TAGS);
//        }
    }

    protected void chooseAccounts() {
        getSupportFragmentManager().beginTransaction()
                .add(ChoosePostAccounts.newInstance(REQUEST_CHOOSE_POST_ACCOUNTS, mAccounts), DIALOG_CHOOSE_POST_ACCOUNTS)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PHOTO:
                if (resultCode == RESULT_OK) {
                    getPhoto(data.getData());
                }
                break;

            case TAGS:
                if ((resultCode == RESULT_OK) && data.hasExtra(Stags) && data.hasExtra(Accounts.SID)) {
                    long id = data.getLongExtra(Accounts.SID, Sonet.INVALID_ACCOUNT_ID);

                    for (ChoosePostAccounts.Account account : mAccounts) {
                        if (account.id == id) {
                            account.tags = Arrays.asList(data.getStringArrayExtra(Stags));
                            break;
                        }
                    }
                }
                break;
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        mCount.setText(Integer.toString(mMessage.getText().toString().length()));
        return false;
    }

    @Override
    public void afterTextChanged(Editable arg0) {
        mCount.setText(Integer.toString(arg0.toString().length()));
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        mPendingLoaders.add(id);

        switch (id) {
            case LOADER_ACCOUNT:
                return new CursorLoader(this,
                        Accounts.getContentUri(this),
                        new String[] { Accounts._ID, Accounts.SERVICE },
                        Accounts._ID + "=?",
                        new String[] { args.getString(LOADER_ARG_ACCOUNT_ID) },
                        null);

            case LOADER_SEND_POST:
                return new SendPostLoader(this,
                        mAccounts,
                        mMessage.getText().toString(),
                        mPhotoPath);

            case LOADER_PHOTO:
                return new CursorLoader(this,
                        Uri.parse(args.getString(LOADER_ARG_PHOTO_URI)),
                        new String[] { MediaStore.Images.Media.DATA },
                        null,
                        null,
                        null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        mPendingLoaders.remove(loader.getId());

        switch (loader.getId()) {
            case LOADER_ACCOUNT:
                mLoadingView.setVisibility(View.GONE);

                if (data instanceof Cursor) {
                    Cursor cursor = (Cursor) data;

                    if (cursor.moveToFirst()) {
                        ChoosePostAccounts.Account account = new ChoosePostAccounts.Account();
                        account.id = cursor.getLong(cursor.getColumnIndexOrThrow(Accounts._ID));
                        account.service = cursor.getInt(cursor.getColumnIndexOrThrow(Accounts.SERVICE));
                        mAccounts.add(account);
                    }
                }
                break;

            case LOADER_SEND_POST:
                mLoadingView.setVisibility(View.GONE);

                if (Boolean.TRUE.equals(data)) {
                    Toast.makeText(this, R.string.success, Toast.LENGTH_LONG).show();
                }

                finish();
                break;

            case LOADER_PHOTO:
                mLoadingView.setVisibility(View.GONE);

                if (data instanceof Cursor) {
                    String path = null;
                    Cursor cursor = (Cursor) data;

                    if (cursor.moveToFirst()) {
                        path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    } else {
                        // some file manages send the path through the uri
                        // TODO
                        // path = imgUri[0].getPath();
                    }

                    if (!TextUtils.isEmpty(path)) {
                        setPhoto(path);
                    } else {
                        Toast.makeText(this, "error retrieving the photo path", Toast.LENGTH_LONG).show();
                    }
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mPendingLoaders.remove(loader.getId());
    }

    @Override
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            case REQUEST_CHOOSE_LOCATION_ACCOUNT:
                if (result == RESULT_OK) {
                    long accountId = ChooseAccount.getSelectedId(data);

                    if (accountId != INVALID_ACCOUNT_ID) {
                        setLocation(accountId);
                    }
                }

                Fragment fragment = getSupportFragmentManager().findFragmentByTag(DIALOG_CHOOSE_LOCATION_ACCOUNT);

                if (fragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .remove(fragment)
                            .commit();
                }
                break;

            case REQUEST_CHOOSE_POST_ACCOUNTS:
                if (result == RESULT_OK) {
                    mAccounts.clear();
                    List<ChoosePostAccounts.Account> selectedAccounts = ChoosePostAccounts.getAccounts(data);

                    if (selectedAccounts != null) {
                        mAccounts.addAll(selectedAccounts);
                    }
                }
                break;

            case REQUEST_CHOOSE_LOCATION:
                if (result == RESULT_OK) {
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
        }
    }
}