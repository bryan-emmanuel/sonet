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

import static com.piusvelte.sonet.Sonet.*;

import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.loader.AddRssLoader;
import com.piusvelte.sonet.loader.MemberAuthenticationLoader;
import com.piusvelte.sonet.loader.OAuthLoginLoader;
import com.piusvelte.sonet.social.GooglePlus;
import com.piusvelte.sonet.social.Client;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.UriMatcher;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

public class OAuthLogin extends FragmentActivity implements OnCancelListener, OnClickListener, LoaderManager.LoaderCallbacks<Object> {
    private static final String TAG = "OAuthLogin";
    private ProgressDialog mLoadingDialog;
    private int mWidgetId;
    private long mAccountId;
    private String mServiceName = "unknown";
    private SonetWebView mSonetWebView;

    @Nullable
    private OAuthLoginLoader.OAuthLoginLoaderResult mOAuthLoginLoaderResult;

    private static final int LOADER_OAUTH_LOGIN = 0;
    private static final int LOADER_SMS = 1;
    private static final int LOADER_RSS = 2;
    private static final int LOADER_PINTEREST = 3;
    private static final int LOADER_MEMBER_AUTHENTICATION = 4;

    private static final String LOADER_ARG_NETWORK = "network";
    private static final String LOADER_ARG_RSS_URL = "rss_url";
    private static final String LOADER_ARG_AUTHENTICATED_URL = "authenticated_url";

    @Override
    public Loader<Object> onCreateLoader(int id, Bundle args) {
        Loader loader;

        switch (id) {
            case LOADER_OAUTH_LOGIN:
                loader = new OAuthLoginLoader(this, args.getInt(LOADER_ARG_NETWORK));
                break;

            case LOADER_SMS:
                loader = new CursorLoader(this, Accounts.getContentUri(this), new String[]{Accounts._ID}, Accounts.SERVICE + "=?", new String[]{Integer.toString(SMS)}, null);
                break;

            case LOADER_RSS:
                loader = new AddRssLoader(this, args.getString(LOADER_ARG_RSS_URL));
                break;

            case LOADER_PINTEREST:
                loader = new CursorLoader(this, Accounts.getContentUri(this), new String[]{Accounts._ID}, Accounts.SERVICE + "=?", new String[]{Integer.toString(PINTEREST)}, null);
                break;

            case LOADER_MEMBER_AUTHENTICATION:
                loader = new MemberAuthenticationLoader(this, mOAuthLoginLoaderResult, args.getString(LOADER_ARG_AUTHENTICATED_URL));
                break;

            default:
                loader = null;
                break;
        }

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        switch (loader.getId()) {
            case LOADER_OAUTH_LOGIN:
                dismissLoading();

                if (data instanceof OAuthLoginLoader.OAuthLoginLoaderResult) {
                    mOAuthLoginLoaderResult = (OAuthLoginLoader.OAuthLoginLoaderResult) data;

                    if (!TextUtils.isEmpty(mOAuthLoginLoaderResult.authUrl)) {
                        mSonetWebView.open(mOAuthLoginLoaderResult.authUrl);
                    } else {
                        (Toast.makeText(OAuthLogin.this, String.format(getString(R.string.oauth_error), mServiceName), Toast.LENGTH_LONG)).show();
                        finish();
                    }
                } else {
                    (Toast.makeText(OAuthLogin.this, String.format(getString(R.string.oauth_error), mServiceName), Toast.LENGTH_LONG)).show();
                    finish();
                }

                getSupportLoaderManager().destroyLoader(LOADER_OAUTH_LOGIN);
                break;

            case LOADER_SMS:
                dismissLoading();

                if (data instanceof Cursor) {
                    if (((Cursor) data).moveToFirst()) {
                        (Toast.makeText(OAuthLogin.this, "SMS has already been added.", Toast.LENGTH_LONG)).show();
                    } else {
                        addAccount(getResources().getStringArray(R.array.service_entries)[SMS], null, null, 0, SMS, null);
                    }
                }

                getSupportLoaderManager().destroyLoader(LOADER_SMS);
                finish();
                break;

            case LOADER_RSS:
                dismissLoading();

                if (data instanceof String) {
                    final String rssUrl = (String) data;
                    final EditText rssName = new EditText(this);
                    rssName.setSingleLine();

                    // TODO DialogFragment
                    new AlertDialog.Builder(OAuthLogin.this)
                            .setTitle(R.string.rss_channel)
                            .setView(rssName)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    addAccount(rssName.getText().toString(), null, null, 0, RSS, rssUrl);
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .show();
                }

                getSupportLoaderManager().destroyLoader(LOADER_RSS);
                break;

            case LOADER_PINTEREST:
                dismissLoading();

                if (data instanceof Cursor) {
                    if (((Cursor) data).moveToFirst()) {
                        (Toast.makeText(OAuthLogin.this, "Pinterest has already been added.", Toast.LENGTH_LONG)).show();
                    } else {
                        Toast.makeText(OAuthLogin.this, "Pinterest currently allows only public, non-authenticated viewing.", Toast.LENGTH_LONG).show();
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

                getSupportLoaderManager().destroyLoader(LOADER_PINTEREST);
                finish();
                break;

            case LOADER_MEMBER_AUTHENTICATION:
                dismissLoading();

                if (data instanceof Client.MemberAuthentication) {
                    Client.MemberAuthentication memberAuthentication = (Client.MemberAuthentication) data;
                    addAccount(memberAuthentication.username, memberAuthentication.token, memberAuthentication.secret, memberAuthentication.expiry, memberAuthentication.network, memberAuthentication.id);
                    finish();
                }

                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
        // NO-OP
    }

    private void setupLoading() {
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setMessage(getString(R.string.loading));
        mLoadingDialog.setCancelable(true);
        mLoadingDialog.setOnCancelListener(this);
        mLoadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), this);
    }

    private void showLoading() {
        mLoadingDialog.show();
    }

    private void dismissLoading() {
        if (mLoadingDialog.isShowing()) mLoadingDialog.dismiss();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setupLoading();
        Intent intent = getIntent();

        if (intent != null) {
            Bundle extras = intent.getExtras();

            if (extras != null) {
                int service = extras.getInt(Accounts.SERVICE, Sonet.INVALID_SERVICE);
                mServiceName = Sonet.getServiceName(getResources(), service);
                mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                mAccountId = extras.getLong(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID);
                mSonetWebView = new SonetWebView(this);

                switch (service) {
                    case SMS:
                        showLoading();
                        getSupportLoaderManager().initLoader(LOADER_SMS, null, this);
                        break;

                    case RSS:
                        // prompt for RSS url
                        final EditText rssUrl = new EditText(this);
                        rssUrl.setSingleLine();
                        // TODO DialogFragment
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.rss_url)
                                .setView(rssUrl)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, int which) {
                                        // test the url and add if valid, else Toast error
                                        showLoading();
                                        Bundle args = new Bundle();
                                        args.putString(LOADER_ARG_RSS_URL, rssUrl.getText().toString());
                                        getSupportLoaderManager().initLoader(LOADER_RSS, args, OAuthLogin.this);
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                })
                                .show();
                        break;

                    case PINTEREST:
                        showLoading();
                        getSupportLoaderManager().initLoader(LOADER_PINTEREST, null, this);
                        break;

                    default: {
                        showLoading();
                        Bundle args = new Bundle();
                        args.putInt(LOADER_ARG_NETWORK, service);
                        getSupportLoaderManager().initLoader(LOADER_OAUTH_LOGIN, args, this);
                    }
                    break;

                }
            }
        }
    }

    private String addAccount(String username, String token, String secret, int expiry, int service, String sid) {
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
            getContentResolver().update(Accounts.getContentUri(this), values, Accounts._ID + "=?", new String[]{Long.toString(mAccountId)});
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

    private class SonetWebView {

        private OAuthLogin mOAuthLogin;
        private WebView mWebView;

        public SonetWebView(OAuthLogin oAuthLogin) {
            mOAuthLogin = oAuthLogin;
            mWebView = new WebView(mOAuthLogin);
            mOAuthLogin.setContentView(mWebView);
            mWebView.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    // just google here
                    if (url != null && mOAuthLoginLoaderResult.client instanceof GooglePlus) {
                        Uri uri = Uri.parse(url);
                        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
                        matcher.addURI("accounts.google.com", "o/oauth2/approval", 1);

                        if (matcher.match(uri) == 1) {
                            showLoading();
                            Bundle args = new Bundle();
                            args.putString(LOADER_ARG_AUTHENTICATED_URL, view.getTitle());
                            getSupportLoaderManager().initLoader(LOADER_MEMBER_AUTHENTICATION, args, mOAuthLogin);
                        }
                    }
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url != null) {
                        showLoading();
                        Uri uri = Uri.parse(url);
                        String host = uri.getHost();

                        Uri callback = mOAuthLoginLoaderResult.client.getCallback();

                        if (callback != null && callback.getHost().equals(host)) {
                            showLoading();
                            Bundle args = new Bundle();
                            args.putString(LOADER_ARG_AUTHENTICATED_URL, url);
                            getSupportLoaderManager().initLoader(LOADER_MEMBER_AUTHENTICATION, args, mOAuthLogin);
                        } else {
                            return false;// allow google to redirect
                        }
                    }

                    return true;
                }

            });
            WebSettings webSettings = mWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDefaultTextEncodingName("UTF-8");
        }

        public void open(String url) {
            if (url != null)
                mWebView.loadUrl(url);
            else
                mOAuthLogin.finish();
        }

    }

    @Override
    public void onClick(DialogInterface arg0, int arg1) {
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (getSupportLoaderManager().hasRunningLoaders()) {
            // TODO destroy loaders
        }

        finish();
    }

}
