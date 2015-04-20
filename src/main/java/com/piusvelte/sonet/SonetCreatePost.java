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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.ads.*;
import com.piusvelte.sonet.fragment.BaseDialogFragment;
import com.piusvelte.sonet.fragment.ChooseAccountsDialogFragment;
import com.piusvelte.sonet.fragment.ChooseLocationDialogFragment;
import com.piusvelte.sonet.fragment.ConfirmSetLocationDialogFragment;
import com.piusvelte.sonet.fragment.MultiChoiceDialogFragment;
import com.piusvelte.sonet.fragment.LoadingDialogFragment;
import com.piusvelte.sonet.fragment.ChooseAccountDialogFragment;
import com.piusvelte.sonet.loader.LocationLoader;
import com.piusvelte.sonet.loader.SendPostLoader;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Widgets;

import static com.piusvelte.sonet.Sonet.*;

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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SonetCreatePost extends FragmentActivity implements OnKeyListener, OnClickListener, TextWatcher, LoaderManager.LoaderCallbacks, BaseDialogFragment.OnResultListener, MultiChoiceDialogFragment.OnMultiChoiceClickListener {
    private static final String TAG = "SonetCreatePost";

    private static final int LOADER_ACCOUNT = 0;
    private static final int LOADER_LOCATION = 1;
    private static final int LOADER_SEND_POST = 2;
    private static final int LOADER_PHOTO = 3;
    private static final int LOADER_ACCOUNT_NAMES = 4;
    private static final int LOADER_ACCOUNTS_NAMES = 5;

    private static final String DIALOG_LOADING = "dialog:loading";
    private static final String DIALOG_ACCOUNT = "dialog:account";
    private static final String DIALOG_LOCATION = "dialog:location";
    private static final String DIALOG_ACCOUNTS = "dialog:accounts";
    private static final String DIALOG_SET_LOCATION = "dialog:set_location";

    private static final String LOADER_ARG_ACCOUNT_ID = "account_id";
    private static final String LOADER_ARG_LATITUDE = "latitude";
    private static final String LOADER_ARG_LONGITUDE = "longitude";
    private static final String LOADER_ARG_PHOTO_URI = "photo_uri";

    private static final int REQUEST_LOCATION = 0;
    private static final int REQUEST_PHOTO = 1;
    private static final int REQUEST_SEND_POST = 2;
    private static final int REQUEST_ACCOUNT = 3;
    private static final int REQUEST_SELECT_LOCATION = 4;
    private static final int REQUEST_ACCOUNTS = 5;
    private static final int REQUEST_SET_LOCATION = 6;

    private static final String STATE_PENDING_LOADERS = "state:pending_loaders";

    private HashMap<Long, String> mAccountsLocation = new HashMap<>();
    private HashMap<Long, String[]> mAccountsTags = new HashMap<>();
    private HashMap<Long, Integer> mAccountsService = new HashMap<>();
    private EditText mMessage;
    private ImageButton mSend;
    private TextView mCount;
    private String mLat = null;
    private String mLong = null;
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
            ((LinearLayout) findViewById(R.id.ad)).addView(adView);
            adView.loadAd(new AdRequest());
        }

        mMessage = (EditText) findViewById(R.id.message);
        mSend = (ImageButton) findViewById(R.id.send);
        mCount = (TextView) findViewById(R.id.count);
        mMessage.addTextChangedListener(this);
        mMessage.setOnKeyListener(this);
        mSend.setOnClickListener(this);
        setResult(RESULT_OK);
        handleIntent(getIntent());

        LoaderManager loaderManager = getSupportLoaderManager();

        if (loaderManager.hasRunningLoaders() && savedInstanceState != null) {
            int[] loaders = savedInstanceState.getIntArray(STATE_PENDING_LOADERS);

            if (loaders != null) {
                for (int loader : loaders) {
                    mPendingLoaders.add(loader);
                    loaderManager.initLoader(loader, null, this);
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
                    LoadingDialogFragment.newInstance(REQUEST_ACCOUNT).show(getSupportFragmentManager(), DIALOG_LOADING);
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

            for (Integer service : mAccountsService.values()) {
                supported = sPhotoSupported.contains(service);

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
//					Cursor account = this.getContentResolver().query(Accounts.getContentUri(this), new String[]{Accounts._ID, ACCOUNTS_QUERY}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
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
                mLat = Double.toString(location.getLatitude());
                mLong = Double.toString(location.getLongitude());

                if (mAccountsService.size() == 1) {
                    if (sLocationSupported.contains(mAccountsService.values().iterator().next())) {
                        setLocation(mAccountsService.keySet().iterator().next());
                    } else {
                        unsupportedToast(sLocationSupported);
                    }
                } else {
                    getSupportLoaderManager().restartLoader(LOADER_ACCOUNT_NAMES, null, this);
                    LoadingDialogFragment.newInstance(REQUEST_ACCOUNT).show(getSupportFragmentManager(), DIALOG_LOADING);
                }
            } else {
                (Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_LONG)).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setLocation(final long accountId) {
        // TODO make sure this works :D how should it resume?
        Bundle args = new Bundle();
        args.putLong(LOADER_ARG_ACCOUNT_ID, accountId);
        args.putString(LOADER_ARG_LATITUDE, mLat);
        args.putString(LOADER_ARG_LONGITUDE, mLong);
        getSupportLoaderManager().restartLoader(LOADER_LOCATION, args, this);
        LoadingDialogFragment.newInstance(REQUEST_LOCATION).show(getSupportFragmentManager(), DIALOG_LOADING);
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
            if (!mAccountsService.isEmpty()) {
                getSupportLoaderManager().restartLoader(LOADER_SEND_POST, null, this);
                LoadingDialogFragment.newInstance(REQUEST_SEND_POST).show(getSupportFragmentManager(), DIALOG_LOADING);
            } else
                (Toast.makeText(SonetCreatePost.this, "no accounts selected", Toast.LENGTH_LONG)).show();
        }
    }

    protected void getPhoto(Uri uri) {
        // TODO some file manages send the path through the uri.getPath()
        Bundle args = new Bundle();
        args.putString(LOADER_ARG_PHOTO_URI, uri.toString());
        getSupportLoaderManager().restartLoader(LOADER_PHOTO, args, this);
        LoadingDialogFragment.newInstance(REQUEST_PHOTO)
                .show(getSupportFragmentManager(), DIALOG_LOADING);
    }

    protected void setPhoto(String path) {
        mPhotoPath = path;
        (Toast.makeText(SonetCreatePost.this, "Currently, the photo will only be uploaded Facebook accounts.", Toast.LENGTH_LONG)).show();
    }

    protected void selectFriends(long accountId) {
        if ((mAccountsService.get(accountId) == FACEBOOK) && (!mAccountsLocation.containsKey(accountId) || (mAccountsLocation.get(accountId) == null))) {
            (Toast.makeText(SonetCreatePost.this, "To tag friends, Facebook requires a location to be included.", Toast.LENGTH_LONG)).show();
        } else {
            startActivityForResult(Sonet.getPackageIntent(this, SelectFriends.class).putExtra(Accounts.SID, accountId).putExtra(Stags, mAccountsTags.get(accountId)), TAGS);
        }
    }

    protected void chooseAccounts() {
        // don't limit accounts to the widget...
        getSupportLoaderManager().restartLoader(LOADER_ACCOUNTS_NAMES, null, this);
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
                    mAccountsTags.put(data.getLongExtra(Accounts.SID, Sonet.INVALID_ACCOUNT_ID), data.getStringArrayExtra(Stags));
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
                        new String[]{Accounts._ID, Accounts.SERVICE},
                        Accounts._ID + "=?",
                        new String[]{args.getString(LOADER_ARG_ACCOUNT_ID)},
                        null);

            case LOADER_LOCATION:
                return new LocationLoader(this,
                        args.getLong(LOADER_ARG_ACCOUNT_ID),
                        args.getString(LOADER_ARG_LATITUDE),
                        args.getString(LOADER_ARG_LONGITUDE));

            case LOADER_SEND_POST:
                return new SendPostLoader(this,
                        mAccountsService,
                        mMessage.getText().toString(),
                        mAccountsLocation,
                        mLat,
                        mLong,
                        mPhotoPath,
                        mAccountsTags);

            case LOADER_PHOTO:
                return new CursorLoader(this,
                        Uri.parse(args.getString(LOADER_ARG_PHOTO_URI)),
                        new String[]{MediaStore.Images.Media.DATA},
                        null,
                        null,
                        null);

            case LOADER_ACCOUNT_NAMES:
                return new CursorLoader(this,
                        Accounts.getContentUri(this),
                        new String[]{Accounts._ID, Accounts.SERVICE, Accounts.USERNAME},
                        Accounts._ID + " in (?)",
                        new String[]{TextUtils.join(",", mAccountsService.keySet())},
                        null);

            case LOADER_ACCOUNTS_NAMES:
                return new CursorLoader(this,
                        Accounts.getContentUri(this),
                        new String[]{Accounts._ID, Accounts.SERVICE, Accounts.USERNAME},
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
                dismissLoading();
                if (data instanceof Cursor) {
                    Cursor cursor = (Cursor) data;

                    if (cursor.moveToFirst()) {
                        mAccountsService.put(cursor.getLong(cursor.getColumnIndexOrThrow(Accounts._ID)),
                                cursor.getInt(cursor.getColumnIndexOrThrow(Accounts.SERVICE)));
                    }
                }
                break;

            case LOADER_LOCATION:
                dismissLoading();

                if (data instanceof LocationLoader.LocationResult) {
                    final LocationLoader.LocationResult result = (LocationLoader.LocationResult) data;

                    ChooseLocationDialogFragment.newInstance(result.accountId, result.locations, "Select Location", REQUEST_SELECT_LOCATION)
                            .show(getSupportFragmentManager(), DIALOG_LOCATION);
                } else {
                    (Toast.makeText(SonetCreatePost.this, getString(R.string.failure), Toast.LENGTH_LONG)).show();
                }
                break;

            case LOADER_SEND_POST:
                dismissLoading();

                if (data instanceof Boolean) {
                    // TODO determine success
                    if ((Boolean) data) {
                        // TODO toast?
                    }
                }
                break;

            case LOADER_PHOTO:
                dismissLoading();

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
                        (Toast.makeText(SonetCreatePost.this, "error retrieving the photo path", Toast.LENGTH_LONG)).show();
                    }
                }
                break;

            case LOADER_ACCOUNT_NAMES:
                dismissLoading();

                if (data instanceof Cursor) {
                    Cursor cursor = (Cursor) data;

                    if (cursor.moveToFirst()) {
                        final int idIndex = cursor.getColumnIndexOrThrow(Accounts._ID);
                        final int serviceIndex = cursor.getColumnIndexOrThrow(Accounts.SERVICE);
                        final int usernameIndex = cursor.getColumnIndexOrThrow(Accounts.USERNAME);
                        HashMap<Long, String> accounts = new HashMap<>();

                        while (!cursor.isAfterLast()) {
                            int service = cursor.getInt(serviceIndex);

                            if (sLocationSupported.contains(service)) {
                                accounts.put(cursor.getLong(idIndex), cursor.getString(usernameIndex));
                            }

                            cursor.moveToNext();
                        }

                        if (!accounts.isEmpty()) {
                            ChooseAccountDialogFragment.newInstance(accounts, getString(R.string.accounts), REQUEST_ACCOUNT)
                                    .show(getSupportFragmentManager(), DIALOG_ACCOUNT);
                        } else {
                            unsupportedToast(sLocationSupported);
                        }
                    } else {
                        unsupportedToast(sLocationSupported);
                    }
                } else {
                    unsupportedToast(sLocationSupported);
                }
                break;

            case LOADER_ACCOUNTS_NAMES:
                dismissLoading();

                if (data instanceof Cursor) {
                    Cursor cursor = (Cursor) data;

                    if (cursor.moveToFirst()) {
                        final int idIndex = cursor.getColumnIndexOrThrow(Accounts._ID);
                        final int serviceIndex = cursor.getColumnIndexOrThrow(Accounts.SERVICE);
                        final int usernameIndex = cursor.getColumnIndexOrThrow(Accounts.USERNAME);

                        List<Accounts.Account> accounts = new ArrayList<>();

                        while (!cursor.isAfterLast()) {
                            Accounts.Account account = new Accounts.Account();
                            account.id = cursor.getLong(idIndex);
                            account.service = cursor.getInt(serviceIndex);
                            account.username = cursor.getString(usernameIndex);

                            accounts.add(account);
                            cursor.moveToNext();
                        }

                        if (!accounts.isEmpty()) {
                            ChooseAccountsDialogFragment.newInstance(accounts, mAccountsService, getString(R.string.accounts), REQUEST_ACCOUNTS)
                                    .show(getSupportFragmentManager(), DIALOG_ACCOUNTS);
                        } // TODO else Toast?
                    } // TODO else Toast?
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

    private void dismissLoading() {
        DialogFragment loadingDialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING);

        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    @Override
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            case REQUEST_LOCATION:
                if (result == RESULT_CANCELED) {
                    getSupportLoaderManager().destroyLoader(LOADER_LOCATION);
                }
                break;

            case REQUEST_SEND_POST:
                if (result == RESULT_CANCELED) {
                    getSupportLoaderManager().destroyLoader(LOADER_SEND_POST);
                }
                break;

            case REQUEST_PHOTO:
                if (result == RESULT_CANCELED) {
                    getSupportLoaderManager().destroyLoader(LOADER_PHOTO);
                }
                break;

            case REQUEST_ACCOUNT:
                if (result == RESULT_OK) {
                    long accountId = ChooseAccountDialogFragment.getSelectedId(data);

                    if (accountId != INVALID_ACCOUNT_ID) {
                        setLocation(accountId);
                    }
                }
                break;

            case REQUEST_SELECT_LOCATION:
                if (result == RESULT_OK) {
                    long accountId = ChooseLocationDialogFragment.getAccount(data);

                    if (accountId != INVALID_ACCOUNT_ID) {
                        String location = ChooseLocationDialogFragment.getSelectedId(data, null);

                        if (!TextUtils.isEmpty(location)) {
                            mAccountsLocation.put(accountId, location);
                        }
                    }
                }
                break;

            case REQUEST_SET_LOCATION:
                if (result == RESULT_OK) {
                    long accountId = ConfirmSetLocationDialogFragment.getAccountId(data);

                    if (accountId != INVALID_ACCOUNT_ID) {
                        setLocation(INVALID_ACCOUNT_ID);
                    }
                }
                break;
        }
    }

    @Override
    public void onClick(MultiChoiceDialogFragment multiChoiceDialogFragment, int requestCode, int which, boolean isChecked) {
        switch (requestCode) {
            case REQUEST_ACCOUNTS:
                long accountId = ChooseAccountsDialogFragment.getAccountId(multiChoiceDialogFragment, which);

                if (accountId != INVALID_ACCOUNT_ID) {
                    if (isChecked) {
                        int service = ChooseAccountsDialogFragment.getService(multiChoiceDialogFragment, which);

                        if (service != -1) {
                            mAccountsService.put(accountId, service);

                            if (sLocationSupported.contains(service)) {
                                if (mLat == null) {
                                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                                    if (location != null) {
                                        mLat = Double.toString(location.getLatitude());
                                        mLong = Double.toString(location.getLongitude());
                                    }
                                }

                                if ((mLat != null) && (mLong != null)) {
                                    ConfirmSetLocationDialogFragment.newInstance(accountId, R.string.set_location, REQUEST_SET_LOCATION)
                                            .show(getSupportFragmentManager(), DIALOG_SET_LOCATION);
                                }
                            }
                        }
                    } else {
                        mAccountsService.remove(accountId);
                        mAccountsLocation.remove(accountId);
                        mAccountsTags.remove(accountId);
                    }
                }
                break;
        }
    }
}