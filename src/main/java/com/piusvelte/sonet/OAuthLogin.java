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

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
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

import com.piusvelte.sonet.fragment.RssNameDialogFragment;
import com.piusvelte.sonet.fragment.RssUrlDialogFragment;
import com.piusvelte.sonet.loader.MemberAuthenticationLoader;
import com.piusvelte.sonet.loader.OAuthLoginLoader;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.social.Client;
import com.piusvelte.sonet.social.GooglePlus;

import static com.piusvelte.sonet.Sonet.PINTEREST;
import static com.piusvelte.sonet.Sonet.RSS;
import static com.piusvelte.sonet.Sonet.SMS;

public class OAuthLogin extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "OAuthLogin";

    private static final String DIALOG_RSS_URL = "dialog:rss_url";
    private static final String DIALOG_RSS_NAME = "dialog:rss_name";

    private static final int REQUEST_RSS_URL = 0;
    private static final int REQUEST_RSS_NAME = 1;

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

    private String addAccount(String username, String token, String secret, int expiry, int service, String sid) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "add account; username=" + username + ", service=" + service + ", sid=" + sid + ", mAccountId=" + mAccountId);
        }

        String accountId;
        ContentValues values = new ContentValues();
        values.put(Accounts.USERNAME, username);
        values.put(Accounts.TOKEN, token);
        values.put(Accounts.SECRET, secret);
        values.put(Accounts.EXPIRY, expiry);
        values.put(Accounts.SERVICE, service);
        values.put(Accounts.SID, sid);

        if (mAccountId != Sonet.INVALID_ACCOUNT_ID) {
            // re-authenticating
            accountId = Long.toString(mAccountId);
            getContentResolver().update(Accounts.getContentUri(this), values, Accounts._ID + "=?", new String[] { Long.toString(mAccountId) });
        } else {
            // new account
            accountId = getContentResolver().insert(Accounts.getContentUri(this), values).getLastPathSegment();
            values.clear();
            values.put(WidgetAccounts.ACCOUNT, accountId);
            values.put(WidgetAccounts.WIDGET, mWidgetId);
            getContentResolver().insert(WidgetAccounts.getContentUri(this), values);
        }

        setResult(RESULT_OK);
        return accountId;
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
                public void onPageFinished(WebView view, String url) {
                    // just google here
                    if (url != null && mOAuthLogin.mOAuthLoginLoaderResult.client instanceof GooglePlus) {
                        Uri uri = Uri.parse(url);
                        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
                        matcher.addURI("accounts.google.com", "o/oauth2/approval", 1);

                        if (matcher.match(uri) == 1) {
                            mOAuthLogin.mLoadingView.setVisibility(View.VISIBLE);
                            Bundle args = new Bundle();
                            args.putString(LOADER_ARG_AUTHENTICATED_URL, view.getTitle());
                            mOAuthLogin.getSupportLoaderManager().restartLoader(LOADER_MEMBER_AUTHENTICATION,
                                    args,
                                    new MemberAuthenticationLoaderCallbacks(mOAuthLogin, mOAuthLogin.mOAuthLoginLoaderResult));
                        }
                    }
                }

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
            // TODO IntentService?
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
        }

        finish();
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
}
