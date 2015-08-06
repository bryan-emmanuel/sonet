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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.piusvelte.sonet.fragment.ItemsDialogFragment;
import com.piusvelte.sonet.fragment.RssNameDialogFragment;
import com.piusvelte.sonet.fragment.RssUrlDialogFragment;
import com.piusvelte.sonet.loader.AddAccountLoader;
import com.piusvelte.sonet.loader.MemberAuthenticationLoader;
import com.piusvelte.sonet.loader.OAuthLoginLoader;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.social.Client;

import static com.piusvelte.sonet.Sonet.GOOGLEPLUS;
import static com.piusvelte.sonet.Sonet.PINTEREST;
import static com.piusvelte.sonet.Sonet.RSS;
import static com.piusvelte.sonet.Sonet.SMS;

public class OAuthLogin extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "OAuthLogin";

    private static final String DIALOG_RSS_URL = "dialog:rss_url";
    private static final String DIALOG_RSS_NAME = "dialog:rss_name";
    private static final String DIALOG_ADD_SYSTEM_ACCOUNT = "dialog:add_system_account";

    private static final int REQUEST_RSS_URL = 0;
    private static final int REQUEST_RSS_NAME = 1;
    private static final int REQUEST_ADD_SYSTEM_ACCOUNT = 2;
    private static final int REQUEST_RESOLVE_CLIENT_CONNECTION_ERROR = 3;

    private int mWidgetId;
    private long mAccountId;
    private String mServiceName = "unknown";
    private SonetWebView mSonetWebView;
    private View mLoadingView;

    @Nullable
    private OAuthLoginLoader.OAuthLoginLoaderResult mOAuthLoginLoaderResult;

    private static final int LOADER_OAUTH_LOGIN = 0;
    private static final int LOADER_SMS = 1;
    private static final int LOADER_PINTEREST = 2;
    private static final int LOADER_MEMBER_AUTHENTICATION = 3;
    private static final int LOADER_ADD_ACCOUNT = 4;

    private static final String LOADER_ARG_NETWORK = "network";
    private static final String LOADER_ARG_AUTHENTICATED_URL = "authenticated_url";

    private OAuthLoginLoaderCallbacks mOAuthLoginLoaderCallbacks = new OAuthLoginLoaderCallbacks(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);

        mLoadingView = findViewById(R.id.loading);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();

        if (intent != null) {
            Bundle extras = intent.getExtras();

            if (extras != null) {
                int service = extras.getInt(Accounts.SERVICE, Sonet.INVALID_SERVICE);

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "start login for service=" + service);
                }

                mServiceName = Sonet.getServiceName(getResources(), service);
                mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                mAccountId = extras.getLong(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID);
                mSonetWebView = new SonetWebView(this);

                switch (service) {
                    case SMS:
                        mLoadingView.setVisibility(View.VISIBLE);
                        getSupportLoaderManager().initLoader(LOADER_SMS, null, this);
                        break;

                    case RSS:
                        RssUrlDialogFragment.newInstance(REQUEST_RSS_URL)
                                .show(getSupportFragmentManager(), DIALOG_RSS_URL);
                        break;

                    case PINTEREST:
                        mLoadingView.setVisibility(View.VISIBLE);
                        getSupportLoaderManager().initLoader(LOADER_PINTEREST, null, this);
                        break;

                    case GOOGLEPLUS:
                        AccountManager accountManager = AccountManager.get(getApplicationContext());
                        Account[] accounts = accountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

                        if (accounts != null && accounts.length > 0) {
                            String[] names = new String[accounts.length];

                            for (int i = 0; i < names.length; i++) {
                                names[i] = accounts[i].name;
                            }

                            mOAuthLoginLoaderResult = new OAuthLoginLoader.OAuthLoginLoaderResult(this, service);
                            ItemsDialogFragment.newInstance(names, REQUEST_ADD_SYSTEM_ACCOUNT, R.string.select_account)
                                    .show(getSupportFragmentManager(), DIALOG_ADD_SYSTEM_ACCOUNT);
                        }
                        break;

                    default: {
                        mLoadingView.setVisibility(View.VISIBLE);
                        Bundle args = new Bundle();
                        args.putInt(LOADER_ARG_NETWORK, service);
                        getSupportLoaderManager().initLoader(LOADER_OAUTH_LOGIN, args, mOAuthLoginLoaderCallbacks);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_SMS:
                return new CursorLoader(this, Accounts.getContentUri(this), new String[] { Accounts._ID }, Accounts.SERVICE + "=?",
                        new String[] { Integer.toString(SMS) }, null);

            case LOADER_PINTEREST:
                return new CursorLoader(this, Accounts.getContentUri(this), new String[] { Accounts._ID }, Accounts.SERVICE + "=?",
                        new String[] { Integer.toString(PINTEREST) }, null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_SMS:
                mLoadingView.setVisibility(View.GONE);

                if (data != null) {
                    if (data.moveToFirst()) {
                        (Toast.makeText(OAuthLogin.this, "SMS has already been added.", Toast.LENGTH_LONG)).show();
                    } else {
                        addAccount(getResources().getStringArray(R.array.service_entries)[SMS], null, null, 0, SMS, null);
                    }
                }

                finish();
                break;

            case LOADER_PINTEREST:
                mLoadingView.setVisibility(View.GONE);

                if (data != null) {
                    if (data.moveToFirst()) {
                        (Toast.makeText(OAuthLogin.this, "Pinterest has already been added.", Toast.LENGTH_LONG)).show();
                    } else {
                        Toast.makeText(OAuthLogin.this, "Pinterest currently allows only public, non-authenticated viewing.", Toast.LENGTH_LONG)
                                .show();
                        String[] values = getResources().getStringArray(R.array.service_values);
                        String[] entries = getResources().getStringArray(R.array.service_entries);

                        for (int i = 0, l = values.length; i < l; i++) {
                            if (Integer.toString(PINTEREST).equals(values[i])) {
                                addAccount(entries[i], null, null, 0, PINTEREST, null);
                                break;
                            }
                        }
                    }
                }

                finish();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // NO-OP
    }

    private void addAccount(String username, String token, String secret, int expiry, int service, String sid) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "add account; username=" + username + ", service=" + service + ", sid=" + sid + ", mAccountId=" + mAccountId);
        }

        getSupportLoaderManager().restartLoader(LOADER_ADD_ACCOUNT,
                null,
                new AddAccountsCallbacks(LOADER_ADD_ACCOUNT,
                        this,
                        username,
                        token,
                        secret,
                        expiry,
                        service,
                        sid,
                        mAccountId,
                        mWidgetId));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_RESOLVE_CLIENT_CONNECTION_ERROR:
                if (resultCode == RESULT_OK) {
                    mLoadingView.setVisibility(View.VISIBLE);
                    Bundle args = new Bundle();
                    args.putString(LOADER_ARG_AUTHENTICATED_URL, null);
                    getSupportLoaderManager().restartLoader(LOADER_MEMBER_AUTHENTICATION,
                            args,
                            new MemberAuthenticationLoaderCallbacks(this, mOAuthLoginLoaderResult));
                } else {
                    // TODO toast
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            case REQUEST_RSS_URL:
                if (result == RESULT_OK) {
                    RssNameDialogFragment.newInstance(REQUEST_RSS_NAME, RssUrlDialogFragment.getUrl(data, null))
                            .show(getSupportFragmentManager(), DIALOG_RSS_NAME);
                } else {
                    finish();
                }
                break;

            case REQUEST_RSS_NAME:
                if (result == RESULT_OK) {
                    String url = RssNameDialogFragment.getUrl(data, null);
                    String name = RssNameDialogFragment.getName(data, null);
                    addAccount(name, null, null, 0, RSS, url);
                }

                finish();
                break;

            case REQUEST_ADD_SYSTEM_ACCOUNT:
                if (result == Activity.RESULT_OK) {
                    // add the account and refresh
                    String account = ItemsDialogFragment.getItems(data)[ItemsDialogFragment.getWhich(data, 0)];
                    // set this on the result
                    mOAuthLoginLoaderResult.client = Client.Builder.from(mOAuthLoginLoaderResult.client)
                            .setAccount(account)
                            .build();

                    mLoadingView.setVisibility(View.VISIBLE);
                    Bundle args = new Bundle();
                    // this isn't typically expected to be null, but for system accounts, there is no url
                    args.putString(LOADER_ARG_AUTHENTICATED_URL, null);
                    getSupportLoaderManager().restartLoader(LOADER_MEMBER_AUTHENTICATION,
                            args,
                            new MemberAuthenticationLoaderCallbacks(this, mOAuthLoginLoaderResult));
                } else {
                    finish();
                }
                break;
        }
    }

    private static class SonetWebView {

        private OAuthLogin mOAuthLogin;
        private WebView mWebView;

        public SonetWebView(OAuthLogin oAuthLogin) {
            mOAuthLogin = oAuthLogin;
            mWebView = new WebView(mOAuthLogin);
            ((FrameLayout) mOAuthLogin.findViewById(R.id.webview_container)).addView(mWebView);
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (!TextUtils.isEmpty(url) && url.startsWith(mOAuthLogin.mOAuthLoginLoaderResult.client.getCallbackUrl())) {
                        mOAuthLogin.mLoadingView.setVisibility(View.VISIBLE);
                        Bundle args = new Bundle();
                        args.putString(LOADER_ARG_AUTHENTICATED_URL, url);
                        mOAuthLogin.getSupportLoaderManager().restartLoader(LOADER_MEMBER_AUTHENTICATION,
                                args,
                                new MemberAuthenticationLoaderCallbacks(mOAuthLogin, mOAuthLogin.mOAuthLoginLoaderResult));

                        return true;
                    }

                    return false;
                }
            });
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDefaultTextEncodingName("UTF-8");
        }

        public void open(String url) {
            if (url != null) {
                mWebView.loadUrl(url);
            } else {
                mOAuthLogin.finish();
            }
        }
    }

    private void setOAuthLoginLoaderResult(OAuthLoginLoader.OAuthLoginLoaderResult data) {
        mLoadingView.setVisibility(View.GONE);

        if (data != null) {
            mOAuthLoginLoaderResult = data;

            if (!TextUtils.isEmpty(mOAuthLoginLoaderResult.authUrl)) {
                mSonetWebView.open(mOAuthLoginLoaderResult.authUrl);
            } else {
                Toast.makeText(OAuthLogin.this, String.format(getString(R.string.oauth_error), mServiceName), Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(OAuthLogin.this, String.format(getString(R.string.oauth_error), mServiceName), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setMemberAuthentication(Client.MemberAuthentication memberAuthentication) {
        mLoadingView.setVisibility(View.GONE);

        if (memberAuthentication != null) {
            addAccount(memberAuthentication.username,
                    memberAuthentication.token,
                    memberAuthentication.secret,
                    memberAuthentication.expiry,
                    memberAuthentication.network,
                    memberAuthentication.id);
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Client.MemberAuthentication not loaded");
            }

            if (mOAuthLoginLoaderResult == null
                    || mOAuthLoginLoaderResult.client == null
                    || !mOAuthLoginLoaderResult.client.hasConnectionError()
                    || !mOAuthLoginLoaderResult.client.resolveConnectionError(this, REQUEST_RESOLVE_CLIENT_CONNECTION_ERROR)) {
                // TODO toast
                finish();
            }
        }
    }

    private static class OAuthLoginLoaderCallbacks implements LoaderManager.LoaderCallbacks<OAuthLoginLoader.OAuthLoginLoaderResult> {

        @NonNull
        private OAuthLogin mOAuthLogin;

        OAuthLoginLoaderCallbacks(@NonNull OAuthLogin OAuthLogin) {
            mOAuthLogin = OAuthLogin;
        }

        @Override
        public Loader<OAuthLoginLoader.OAuthLoginLoaderResult> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_OAUTH_LOGIN:
                    return new OAuthLoginLoader(mOAuthLogin, args.getInt(LOADER_ARG_NETWORK));

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<OAuthLoginLoader.OAuthLoginLoaderResult> loader, OAuthLoginLoader.OAuthLoginLoaderResult data) {
            switch (loader.getId()) {
                case LOADER_OAUTH_LOGIN:
                    mOAuthLogin.setOAuthLoginLoaderResult(data);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<OAuthLoginLoader.OAuthLoginLoaderResult> loader) {
            // NO-OP
        }
    }

    private static class MemberAuthenticationLoaderCallbacks implements LoaderManager.LoaderCallbacks<Client.MemberAuthentication> {

        @NonNull
        private OAuthLogin mOAuthLogin;
        @NonNull
        private OAuthLoginLoader.OAuthLoginLoaderResult mOAuthLoginLoaderResult;

        MemberAuthenticationLoaderCallbacks(@NonNull OAuthLogin oAuthLogin, @NonNull OAuthLoginLoader.OAuthLoginLoaderResult oAuthLoginLoaderResult) {
            mOAuthLogin = oAuthLogin;
            mOAuthLoginLoaderResult = oAuthLoginLoaderResult;
        }

        @Override
        public Loader<Client.MemberAuthentication> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_MEMBER_AUTHENTICATION:
                    return new MemberAuthenticationLoader(mOAuthLogin,
                            mOAuthLoginLoaderResult,
                            args.getString(LOADER_ARG_AUTHENTICATED_URL));

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<Client.MemberAuthentication> loader, Client.MemberAuthentication data) {
            switch (loader.getId()) {
                case LOADER_MEMBER_AUTHENTICATION:
                    mOAuthLogin.setMemberAuthentication(data);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<Client.MemberAuthentication> loader) {
            // NO-OP
        }
    }

    private static class AddAccountsCallbacks implements LoaderManager.LoaderCallbacks<Boolean> {

        private int mLoaderId;
        private OAuthLogin mOAuthLogin;
        private String mUsername;
        private String mToken;
        private String mSecret;
        private int mExpiry;
        private int mService;
        private String mSid;
        private long mAccountId;
        private int mAppWidgetId;

        public AddAccountsCallbacks(int loaderId,
                OAuthLogin OAuthLogin,
                String username,
                String token,
                String secret,
                int expiry,
                int service,
                String sid,
                long accountId,
                int appWidgetId) {
            mLoaderId = loaderId;
            mOAuthLogin = OAuthLogin;
            mUsername = username;
            mToken = token;
            mSecret = secret;
            mExpiry = expiry;
            mService = service;
            mSid = sid;
            mAccountId = accountId;
            mAppWidgetId = appWidgetId;
        }

        @Override
        public Loader<Boolean> onCreateLoader(int id, Bundle args) {
            if (id == mLoaderId) {
                return new AddAccountLoader(mOAuthLogin, mUsername, mToken, mSecret, mExpiry, mService, mSid, mAccountId, mAppWidgetId);
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<Boolean> loader, Boolean data) {
            if (loader.getId() == mLoaderId) {
                if (Boolean.TRUE.equals(data)) {
                    mOAuthLogin.setResult(RESULT_OK);
                } else {
                    Toast.makeText(mOAuthLogin, "oops, something went wrong", Toast.LENGTH_SHORT).show();
                }

                mOAuthLogin.finish();
            }
        }

        @Override
        public void onLoaderReset(Loader<Boolean> loader) {
            // NO-OP
        }
    }
}
