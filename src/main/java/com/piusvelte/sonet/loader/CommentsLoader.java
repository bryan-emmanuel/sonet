package com.piusvelte.sonet.loader;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetProvider;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;
import com.piusvelte.sonet.social.Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.IDENTICA;
import static com.piusvelte.sonet.Sonet.TWITTER;

/**
 * Created by bemmanuel on 4/21/15.
 */
public class CommentsLoader extends BaseAsyncTaskLoader<CommentsLoader.Result> {

    private Context mContext;
    private Uri mData;

    public CommentsLoader(Context context, @NonNull Uri data) {
        super(context);
        mContext = context.getApplicationContext();
        mData = data;
    }

    @Override
    public CommentsLoader.Result loadInBackground() {
        Result result = new Result();
        SonetCrypto sonetCrypto = SonetCrypto.getInstance(mContext);
        UriMatcher um = new UriMatcher(UriMatcher.NO_MATCH);
        String authority = Sonet.getAuthority(mContext);
        um.addURI(authority, StatusesStyles.VIEW + "/*", SonetProvider.STATUSES_STYLES);
        um.addURI(authority, Notifications.TABLE + "/*", SonetProvider.NOTIFICATIONS);
        Cursor status;
        long mAccount;
        boolean mTime24hr = false;
        String mToken = null;
        String mSecret = null;
        String mAccountSid = null;

        switch (um.match(mData)) {
            case SonetProvider.STATUSES_STYLES:
                status = mContext.getContentResolver().query(StatusesStyles.getContentUri(mContext),
                        new String[] { StatusesStyles.ACCOUNT,
                                StatusesStyles.SID,
                                StatusesStyles.ESID,
                                StatusesStyles.WIDGET,
                                StatusesStyles.SERVICE,
                                StatusesStyles.FRIEND,
                                StatusesStyles.MESSAGE,
                                StatusesStyles.CREATED },
                        StatusesStyles._ID + "=?",
                        new String[] { mData.getLastPathSegment() },
                        null);

                if (status.moveToFirst()) {
                    result.service = status.getInt(4);
                    result.serviceName = mContext.getResources().getStringArray(R.array.service_entries)[result.service];
                    mAccount = status.getLong(0);
                    result.sid = sonetCrypto.Decrypt(status.getString(1));
                    result.esid = sonetCrypto.Decrypt(status.getString(2));
                    Cursor widget = mContext.getContentResolver().query(WidgetsSettings.getContentUri(mContext), new String[] { Widgets.TIME24HR },
                            Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                            new String[] { Integer.toString(status.getInt(3)), Long.toString(mAccount) }, null);

                    if (widget.moveToFirst()) {
                        mTime24hr = widget.getInt(0) == 1;
                    } else {
                        Cursor b = mContext.getContentResolver().query(WidgetsSettings.getContentUri(mContext), new String[] { Widgets.TIME24HR },
                                Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                new String[] { Integer.toString(status.getInt(3)), Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);

                        if (b.moveToFirst()) {
                            mTime24hr = b.getInt(0) == 1;
                        } else {
                            Cursor c = mContext.getContentResolver().query(WidgetsSettings.getContentUri(mContext), new String[] { Widgets.TIME24HR },
                                    Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                    new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                                    null);

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
                    HashMap<String, String> commentMap = new HashMap<>();
                    commentMap.put(Statuses.SID, result.sid);
                    commentMap.put(Entity.FRIEND, status.getString(5));
                    commentMap.put(Statuses.MESSAGE, status.getString(6));
                    commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(status.getLong(7), mTime24hr));
                    commentMap.put(mContext.getString(R.string.like), result.service == TWITTER
                            ? mContext.getString(R.string.retweet)
                            : result.service == IDENTICA ? mContext.getString(R.string.repeat) : "");
                    result.socialClientComments.add(commentMap);
                    // load the session
                    Cursor account = mContext.getContentResolver()
                            .query(Accounts.getContentUri(mContext), new String[] { Accounts.TOKEN, Accounts.SECRET, Accounts.SID },
                                    Accounts._ID + "=?", new String[] { Long.toString(mAccount) }, null);

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
                Cursor notification = mContext.getContentResolver().query(Notifications.getContentUri(mContext),
                        new String[] { Notifications.ACCOUNT, Notifications.SID, Notifications.ESID, Notifications.FRIEND, Notifications.MESSAGE,
                                Notifications.CREATED },
                        Notifications._ID + "=?", new String[] { mData.getLastPathSegment() }, null);

                if (notification.moveToFirst()) {
                    // clear notification
                    ContentValues values = new ContentValues();
                    values.put(Notifications.CLEARED, 1);
                    mContext.getContentResolver().update(Notifications.getContentUri(mContext), values, Notifications._ID + "=?",
                            new String[] { mData.getLastPathSegment() });
                    mAccount = notification.getLong(0);
                    result.sid = sonetCrypto.Decrypt(notification.getString(1));
                    result.esid = sonetCrypto.Decrypt(notification.getString(2));
                    mTime24hr = false;
                    // load the session
                    Cursor account = mContext.getContentResolver()
                            .query(Accounts.getContentUri(mContext), new String[] { Accounts.TOKEN, Accounts.SECRET, Accounts.SID, Accounts.SERVICE },
                                    Accounts._ID + "=?", new String[] { Long.toString(mAccount) }, null);

                    if (account.moveToFirst()) {
                        mToken = sonetCrypto.Decrypt(account.getString(0));
                        mSecret = sonetCrypto.Decrypt(account.getString(1));
                        mAccountSid = sonetCrypto.Decrypt(account.getString(2));
                        result.service = account.getInt(3);
                    }

                    account.close();
                    HashMap<String, String> commentMap = new HashMap<>();
                    commentMap.put(Statuses.SID, result.sid);
                    commentMap.put(Entity.FRIEND, notification.getString(3));
                    commentMap.put(Statuses.MESSAGE, notification.getString(4));
                    commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(notification.getLong(5), mTime24hr));
                    commentMap.put(mContext.getString(R.string.like),
                            result.service == TWITTER ? mContext.getString(R.string.retweet) : mContext.getString(R.string.repeat));
                    result.socialClientComments.add(commentMap);
                    result.serviceName = mContext.getResources().getStringArray(R.array.service_entries)[result.service];
                }

                notification.close();
                break;

            default:
                mTime24hr = false;
                mToken = null;
                mSecret = null;
                mAccountSid = null;
                HashMap<String, String> commentMap = new HashMap<>();
                commentMap.put(Statuses.SID, "");
                commentMap.put(Entity.FRIEND, "");
                commentMap.put(Statuses.MESSAGE, "error, status not found");
                commentMap.put(Statuses.CREATEDTEXT, "");
                commentMap.put(mContext.getString(R.string.like), "");
                result.socialClientComments.add(commentMap);
                break;
        }

        if (!TextUtils.isEmpty(mToken)) {
            result.client = new Client.Builder(mContext)
                    .setNetwork(result.service)
                    .setCredentials(mToken, mSecret)
                    .setAccount(result.esid)
                    .build();

            result.messagePretext = result.client.getCommentPretext(result.esid);
            result.isLikeable = result.client.isLikeable(result.sid);
            result.isLiked = result.client.isLiked(result.sid, mAccountSid);
            result.isCommentable = result.client.isCommentable(result.sid);
            result.likeText = result.client.getLikeText(result.isLiked);
            result.socialClientComments.addAll(result.client.getComments(result.sid, mTime24hr));
        }

        return result;
    }

    public static class Result {
        public int service;
        public String serviceName;
        public String sid;
        public String esid;
        public Client client;
        public String messagePretext = null;
        public boolean isLikeable = false;
        public boolean isLiked = false;
        public boolean isCommentable = false;
        public String likeText = null;
        public List<HashMap<String, String>> socialClientComments = new ArrayList<>();
    }
}
