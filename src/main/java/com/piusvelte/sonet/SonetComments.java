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
import java.util.List;
import java.util.regex.Matcher;

import com.google.ads.*;

import static com.piusvelte.sonet.Sonet.*;

import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Entities;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;
import com.piusvelte.sonet.social.Client;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SonetComments extends ListActivity implements OnKeyListener, OnClickListener, TextWatcher, DialogInterface.OnClickListener, OnCancelListener {
    private int mService = Sonet.INVALID_SERVICE;
    private long mAccount;
    private String mSid = null;
    private String mEsid = null;
    private EditText mMessage;
    private ImageButton mSend;
    private TextView mCount;
    private List<HashMap<String, String>> mComments = new ArrayList<>();
    private boolean mTime24hr = false;
    private String mToken = null;
    private String mSecret = null;
    private String mAccountSid = null;
    private String mServiceName = null;
    private Uri mData = null;
    private String[] items = null;
    private AlertDialog mDialog;
    @Nullable
    private Client mClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // allow posting to multiple services if an account is defined
        // allow selecting which accounts to use
        // get existing comments, allow liking|unliking those comments
        setContentView(R.layout.comments);

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
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();

        if (intent != null) {
            mData = intent.getData();
        }

        if (mData == null) {
            (Toast.makeText(this, getString(R.string.failure), Toast.LENGTH_LONG)).show();
            finish();
        } else {
            loadComments();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if ((mDialog != null) && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSend) {
            if (!TextUtils.isEmpty(mMessage.getText().toString()) && mClient != null) {
                mMessage.setEnabled(false);
                mSend.setEnabled(false);
                // post or comment!
                final ProgressDialog loadingDialog = new ProgressDialog(this);
                final AsyncTask<Void, String, String> asyncTask = new AsyncTask<Void, String, String>() {
                    @Override
                    protected String doInBackground(Void... arg0) {
                        String serviceName = Sonet.getServiceName(getResources(), mService);
                        publishProgress(serviceName);

                        boolean success = mClient.sendComment(mSid, mMessage.getText().toString());

                        return !success && mService == MYSPACE ? null : serviceName + " " + getString(success ? R.string.success : R.string.failure);
                    }

                    @Override
                    protected void onProgressUpdate(String... params) {
                        loadingDialog.setMessage(String.format(getString(R.string.sending), params[0]));
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        if (result != null) {
                            (Toast.makeText(SonetComments.this, result, Toast.LENGTH_LONG)).show();
                        } else if (mService == MYSPACE) {
                            // myspace permissions
                            (Toast.makeText(SonetComments.this, SonetComments.this.getResources().getStringArray(R.array.service_entries)[MYSPACE] + getString(R.string.failure) + " " + getString(R.string.myspace_permissions_message), Toast.LENGTH_LONG)).show();
                        }

                        if (loadingDialog.isShowing()) loadingDialog.dismiss();
                        finish();
                    }

                };
                loadingDialog.setMessage(getString(R.string.loading));
                loadingDialog.setCancelable(true);
                loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (!asyncTask.isCancelled()) asyncTask.cancel(true);
                    }
                });
                loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                loadingDialog.show();
                asyncTask.execute();
            } else {
                (Toast.makeText(SonetComments.this, "error parsing message body", Toast.LENGTH_LONG)).show();
                mMessage.setEnabled(true);
                mSend.setEnabled(true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_comments, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_comments_refresh) {
            loadComments();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView list, View view, final int position, long id) {
        super.onListItemClick(list, view, position, id);
        HashMap<String, String> comment = mComments.get(position);
        final String sid = comment.get(Statuses.SID);
        final String liked = comment.get(getString(R.string.like));
        final boolean isLiked = getString(R.string.like).equals(liked);

        // wait for previous attempts to finish
        if (!TextUtils.isEmpty(liked) && !liked.equals(getString(R.string.loading))) {
            // parse comment body, as in StatusDialog.java
            Matcher m = Sonet.getLinksMatcher(mComments.get(position).get(Statuses.MESSAGE));
            int count = 0;

            while (m.find()) {
                count++;
            }

            items = new String[count + 1];
            items[0] = mClient.getLikeText(isLiked);
            count = 1;
            m.reset();

            while (m.find()) {
                items[count++] = m.group();
            }

            mDialog = (new AlertDialog.Builder(this))
                    .setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                AsyncTask<String, Void, Void> asyncTask = new AsyncTask<String, Void, Void>() {

                                    boolean success = false;

                                    @Override
                                    protected Void doInBackground(String... arg0) {
                                        success = mClient.likeStatus(sid, mEsid, true);
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        setCommentStatus(0, mClient.getLikeText(success ? !isLiked : isLiked));
                                        (Toast.makeText(SonetComments.this, mServiceName + " " + getString(success ? R.string.success : R.string.failure), Toast.LENGTH_LONG)).show();
                                    }
                                };
                                setCommentStatus(0, getString(R.string.loading));
                                asyncTask.execute();
                            } else {
                                if ((which < items.length) && (items[which] != null)) {
                                    // open link
                                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(items[which])));
                                } else {
                                    (Toast.makeText(SonetComments.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
                                }
                            }
                        }
                    })
                    .setCancelable(true)
                    .setOnCancelListener(this)
                    .create();
            mDialog.show();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.cancel();
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

    private void setCommentStatus(int position, String status) {
        if (mComments.size() > position) {
            HashMap<String, String> comment = mComments.get(position);
            comment.put(getString(R.string.like), status);
            mComments.set(position, comment);
            setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
        }
    }

    private void loadComments() {
        mComments.clear();
        setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
        mMessage.setEnabled(false);
        mMessage.setText(R.string.loading);
        final ProgressDialog loadingDialog = new ProgressDialog(this);
        final AsyncTask<Void, String, Void> asyncTask = new AsyncTask<Void, String, Void>() {

            private String mMessagePretext = null;
            private boolean mIsLikeable = false;
            private boolean mIsLiked = false;
            private boolean mIsCommentable = false;
            private String mLikeText = null;
            private List<HashMap<String, String>> mSocialClientComments = null;

            @Override
            protected Void doInBackground(Void... none) {
                // load the status itself
                if (mData != null) {
                    SonetCrypto sonetCrypto = SonetCrypto.getInstance(getApplicationContext());
                    UriMatcher um = new UriMatcher(UriMatcher.NO_MATCH);
                    String authority = Sonet.getAuthority(SonetComments.this);
                    um.addURI(authority, SonetProvider.VIEW_STATUSES_STYLES + "/*", SonetProvider.STATUSES_STYLES);
                    um.addURI(authority, SonetProvider.TABLE_NOTIFICATIONS + "/*", SonetProvider.NOTIFICATIONS);
                    Cursor status;

                    switch (um.match(mData)) {
                        case SonetProvider.STATUSES_STYLES:
                            status = getContentResolver().query(StatusesStyles.getContentUri(SonetComments.this), new String[]{StatusesStyles.ACCOUNT, StatusesStyles.SID, StatusesStyles.ESID, StatusesStyles.WIDGET, StatusesStyles.SERVICE, StatusesStyles.FRIEND, StatusesStyles.MESSAGE, StatusesStyles.CREATED}, StatusesStyles._ID + "=?", new String[]{mData.getLastPathSegment()}, null);

                            if (status.moveToFirst()) {
                                mService = status.getInt(4);
                                mServiceName = getResources().getStringArray(R.array.service_entries)[mService];
                                mAccount = status.getLong(0);
                                mSid = sonetCrypto.Decrypt(status.getString(1));
                                mEsid = sonetCrypto.Decrypt(status.getString(2));
                                Cursor widget = getContentResolver().query(WidgetsSettings.getContentUri(SonetComments.this), new String[]{Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(status.getInt(3)), Long.toString(mAccount)}, null);

                                if (widget.moveToFirst()) {
                                    mTime24hr = widget.getInt(0) == 1;
                                } else {
                                    Cursor b = getContentResolver().query(WidgetsSettings.getContentUri(SonetComments.this), new String[]{Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(status.getInt(3)), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);

                                    if (b.moveToFirst()) {
                                        mTime24hr = b.getInt(0) == 1;
                                    } else {
                                        Cursor c = getContentResolver().query(WidgetsSettings.getContentUri(SonetComments.this), new String[]{Widgets.TIME24HR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);

                                        if (c.moveToFirst()) {
                                            mTime24hr = c.getInt(0) == 1;
                                        } else {
                                            mTime24hr = false;
                                        }

                                        c.close();
                                    }

                                    b.close();
                                }

                                widget.close();
                                HashMap<String, String> commentMap = new HashMap<String, String>();
                                commentMap.put(Statuses.SID, mSid);
                                commentMap.put(Entities.FRIEND, status.getString(5));
                                commentMap.put(Statuses.MESSAGE, status.getString(6));
                                commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(status.getLong(7), mTime24hr));
                                commentMap.put(getString(R.string.like), mService == TWITTER ? getString(R.string.retweet) : mService == IDENTICA ? getString(R.string.repeat) : "");
                                mComments.add(commentMap);
                                // load the session
                                Cursor account = getContentResolver().query(Accounts.getContentUri(SonetComments.this), new String[]{Accounts.TOKEN, Accounts.SECRET, Accounts.SID}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);

                                if (account.moveToFirst()) {
                                    mToken = sonetCrypto.Decrypt(account.getString(0));
                                    mSecret = sonetCrypto.Decrypt(account.getString(1));
                                    mAccountSid = sonetCrypto.Decrypt(account.getString(2));
                                }

                                account.close();
                            }

                            status.close();
                            break;

                        case SonetProvider.NOTIFICATIONS:
                            Cursor notification = getContentResolver().query(Notifications.getContentUri(SonetComments.this), new String[]{Notifications.ACCOUNT, Notifications.SID, Notifications.ESID, Notifications.FRIEND, Notifications.MESSAGE, Notifications.CREATED}, Notifications._ID + "=?", new String[]{mData.getLastPathSegment()}, null);

                            if (notification.moveToFirst()) {
                                // clear notification
                                ContentValues values = new ContentValues();
                                values.put(Notifications.CLEARED, 1);
                                getContentResolver().update(Notifications.getContentUri(SonetComments.this), values, Notifications._ID + "=?", new String[]{mData.getLastPathSegment()});
                                mAccount = notification.getLong(0);
                                mSid = sonetCrypto.Decrypt(notification.getString(1));
                                mEsid = sonetCrypto.Decrypt(notification.getString(2));
                                mTime24hr = false;
                                // load the session
                                Cursor account = getContentResolver().query(Accounts.getContentUri(SonetComments.this), new String[]{Accounts.TOKEN, Accounts.SECRET, Accounts.SID, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);

                                if (account.moveToFirst()) {
                                    mToken = sonetCrypto.Decrypt(account.getString(0));
                                    mSecret = sonetCrypto.Decrypt(account.getString(1));
                                    mAccountSid = sonetCrypto.Decrypt(account.getString(2));
                                    mService = account.getInt(3);
                                }

                                account.close();
                                HashMap<String, String> commentMap = new HashMap<>();
                                commentMap.put(Statuses.SID, mSid);
                                commentMap.put(Entities.FRIEND, notification.getString(3));
                                commentMap.put(Statuses.MESSAGE, notification.getString(4));
                                commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(notification.getLong(5), mTime24hr));
                                commentMap.put(getString(R.string.like), mService == TWITTER ? getString(R.string.retweet) : getString(R.string.repeat));
                                mComments.add(commentMap);
                                mServiceName = getResources().getStringArray(R.array.service_entries)[mService];
                            }

                            notification.close();
                            break;

                        default:
                            mComments.clear();
                            HashMap<String, String> commentMap = new HashMap<>();
                            commentMap.put(Statuses.SID, "");
                            commentMap.put(Entities.FRIEND, "");
                            commentMap.put(Statuses.MESSAGE, "error, status not found");
                            commentMap.put(Statuses.CREATEDTEXT, "");
                            commentMap.put(getString(R.string.like), "");
                            mComments.add(commentMap);
                    }

                    mClient = new Client.Builder(SonetComments.this)
                            .setNetwork(mService)
                            .setCredentials(mToken, mSecret)
                            .setAccount(mEsid)
                            .build();

                    mMessagePretext = mClient.getCommentPretext(mEsid);
                    mIsLikeable = mClient.isLikeable(mSid);
                    mIsLiked = mClient.isLiked(mSid, mAccountSid);
                    mIsCommentable = mClient.isCommentable(mSid);
                    mLikeText = mClient.getLikeText(mIsLiked);
                    mSocialClientComments = mClient.getComments(mSid, mTime24hr);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mMessage.setText("");

                if (mIsCommentable) {
                    if (!TextUtils.isEmpty(mMessagePretext)) {
                        mMessage.append(mMessagePretext);
                    }
                } else {
                    mSend.setEnabled(false);
                    mMessage.setEnabled(false);
                    mMessage.setText(R.string.uncommentable);
                }

                if (mIsLikeable) {
                    setCommentStatus(0, mLikeText);
                } else {
                    setCommentStatus(0, getString(R.string.unlikable));
                }

                mMessage.setEnabled(true);

                if (mSocialClientComments != null && mSocialClientComments.size() > 0) {
                    mComments.addAll(mSocialClientComments);
                } else {
                    noComments();
                }

                setListAdapter(new SimpleAdapter(SonetComments.this, mComments, R.layout.comment, new String[]{Entities.FRIEND, Statuses.MESSAGE, Statuses.CREATEDTEXT, getString(R.string.like)}, new int[]{R.id.friend, R.id.message, R.id.created, R.id.like}));
                if (loadingDialog.isShowing()) loadingDialog.dismiss();
            }

            private void noComments() {
                HashMap<String, String> commentMap = new HashMap<>();
                commentMap.put(Statuses.SID, "");
                commentMap.put(Entities.FRIEND, "");
                commentMap.put(Statuses.MESSAGE, getString(R.string.no_comments));
                commentMap.put(Statuses.CREATEDTEXT, "");
                commentMap.put(getString(R.string.like), "");
                mComments.add(commentMap);
            }
        };
        loadingDialog.setMessage(getString(R.string.loading));
        loadingDialog.setCancelable(true);
        loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!asyncTask.isCancelled()) asyncTask.cancel(true);
            }
        });
        loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        loadingDialog.show();
        asyncTask.execute();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
    }
}