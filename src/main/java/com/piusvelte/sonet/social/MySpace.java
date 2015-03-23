package com.piusvelte.sonet.social;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;
import com.piusvelte.sonet.provider.Entities;
import com.piusvelte.sonet.provider.Notifications;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.MYSPACE_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.MYSPACE_HISTORY;
import static com.piusvelte.sonet.Sonet.MYSPACE_STATUSMOODCOMMENTS_BODY;
import static com.piusvelte.sonet.Sonet.MYSPACE_STATUSMOOD_BODY;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_ME;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOOD;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOODCOMMENTS;
import static com.piusvelte.sonet.Sonet.Sauthor;
import static com.piusvelte.sonet.Sonet.Sbody;
import static com.piusvelte.sonet.Sonet.ScommentId;
import static com.piusvelte.sonet.Sonet.SdisplayName;
import static com.piusvelte.sonet.Sonet.Sentry;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.SmoodStatusLastUpdated;
import static com.piusvelte.sonet.Sonet.SpostedDate;
import static com.piusvelte.sonet.Sonet.SrecentComments;
import static com.piusvelte.sonet.Sonet.Sstatus;
import static com.piusvelte.sonet.Sonet.SstatusId;
import static com.piusvelte.sonet.Sonet.SthumbnailUrl;
import static com.piusvelte.sonet.Sonet.SuserId;
import static oauth.signpost.OAuth.OAUTH_VERIFIER;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class MySpace extends Client {

    public MySpace(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return Uri.parse("sonet://myspace");
    }

    @Override
    String getRequestUrl() {
        return Sonet.MYSPACE_URL_REQUEST;
    }

    @Override
    String getAccessUrl() {
        return Sonet.MYSPACE_URL_ACCESS;
    }

    @Override
    String getAuthorizeUrl() {
        return Sonet.MYSPACE_URL_AUTHORIZE;
    }

    @Override
    public String getCallbackUrl() {
        return getCallback().toString();
    }

    @Override
    boolean isOAuth10a() {
        return true;
    }

    @Override
    public MemberAuthentication getMemberAuthentication(@NonNull SonetOAuth sonetOAuth, @NonNull String authenticatedUrl) {
        Uri uri = Uri.parse(authenticatedUrl);
        String verifier = uri.getQueryParameter(OAUTH_VERIFIER);

        if (!TextUtils.isEmpty(verifier)) {
            if (sonetOAuth.retrieveAccessToken(verifier)) {
                String httpResponse = SonetHttpClient.httpResponse(mContext, sonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_URL_ME, MYSPACE_BASE_URL))));

                if (!TextUtils.isEmpty(httpResponse)) {
                    try {
                        JSONObject jobj = new JSONObject(httpResponse);
                        JSONObject person = jobj.getJSONObject("person");

                        if (person.has(SdisplayName) && person.has(Sid)) {
                            MemberAuthentication memberAuthentication = new MemberAuthentication();
                            memberAuthentication.username = person.getString(SdisplayName);
                            memberAuthentication.token = sonetOAuth.getToken();
                            memberAuthentication.secret = sonetOAuth.getTokenSecret();
                            memberAuthentication.expiry = 0;
                            memberAuthentication.network = mNetwork;
                            memberAuthentication.id = person.getString(Sid);
                            return memberAuthentication;
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) {
                            Log.d(mTag, "error parsing me response: " + httpResponse, e);
                        }
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Set<String> getNotificationStatusIds(long account, String[] notificationMessage) {
        Set<String> notificationSids = new HashSet<>();
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext), new String[]{Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);

        // loop over notifications
        if (currentNotifications.moveToFirst()) {
            while (!currentNotifications.isAfterLast()) {
                long notificationId = currentNotifications.getLong(0);
                String sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                long updated = currentNotifications.getLong(2);
                boolean cleared = currentNotifications.getInt(3) == 1;
                String esid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(4));

                // store sids, to avoid duplicates when requesting the latest feed
                notificationSids.add(sid);

                // get comments for current notifications
                String response = SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, esid, sid))));

                if (!TextUtils.isEmpty(response)) {
                    // check for a newer post, if it's the user's own, then set CLEARED=0
                    try {
                        JSONArray commentsArray = new JSONObject(response).getJSONArray(Sentry);
                        final int i2 = commentsArray.length();

                        if (i2 > 0) {
                            for (int i = 0; i < i2; i++) {
                                JSONObject commentObj = commentsArray.getJSONObject(i);
                                long created_time = parseDate(commentObj.getString(SpostedDate), MYSPACE_DATE_FORMAT);

                                if (created_time > updated) {
                                    JSONObject friendObj = commentObj.getJSONObject(Sauthor);
                                    updateNotificationMessage(notificationMessage,
                                            updateNotification(notificationId, created_time, mAccountEsid, friendObj.getString(Sid), friendObj.getString(SdisplayName), cleared));
                                }
                            }
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) Log.d(mTag, "error parsing: " + response, e);
                    }
                }

                currentNotifications.moveToNext();
            }
        }

        currentNotifications.close();
        return notificationSids;
    }

    @Nullable
    @Override
    public String getFeedResponse(int status_count) {
        return SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(MYSPACE_HISTORY, MYSPACE_BASE_URL))));
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        return new JSONObject(response).getJSONArray(Sentry);
    }

    @Nullable
    @Override
    public void addFeedItem(@NonNull JSONObject item, boolean display_profile, boolean time24hr, int appWidgetId, long account, HttpClient httpClient, Set<String> notificationSids, String[] notificationMessage, boolean doNotify) throws JSONException {
        JSONObject friendObj = item.getJSONObject(Sauthor);
        long date = parseDate(item.getString(SmoodStatusLastUpdated), MYSPACE_DATE_FORMAT);
        String esid = item.getString(SuserId);
        int commentCount = 0;
        String sid = item.getString(SstatusId);
        String friend = friendObj.getString(SdisplayName);
        String statusValue = item.getString(Sstatus);
        String notification = null;
        if (item.has(SrecentComments)) {
            JSONArray commentsArray = item.getJSONArray(SrecentComments);
            commentCount = commentsArray.length();

            // notifications
            if ((sid != null) && !notificationSids.contains(sid) && (commentCount > 0)) {
                // default hasCommented to whether or not these comments are for the own user's status
                boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                for (int c2 = 0; c2 < commentCount; c2++) {
                    JSONObject commentObj = commentsArray.getJSONObject(c2);

                    if (commentObj.has(Sauthor)) {
                        JSONObject c4 = commentObj.getJSONObject(Sauthor);

                        if (c4.getString(Sid).equals(mAccountEsid)) {
                            if (!hasCommented) {
                                // the user has commented on this thread, notify any updates
                                hasCommented = true;
                            }

                            // clear any notifications, as the user is already aware
                            if (notification != null) {
                                notification = null;
                            }
                        } else if (hasCommented) {
                            // don't notify about user's own comments
                            // send the parent comment sid
                            notification = String.format(getString(R.string.friendcommented), c4.getString(SdisplayName));
                        }
                    }
                }
            }
        }

        if (doNotify && notification != null) {
            // new notification
            addNotification(sid, esid, friend, statusValue, date, account, notification);
            updateNotificationMessage(notificationMessage, notification);
        }

        addStatusItem(date,
                friend,
                display_profile ? friendObj.getString(SthumbnailUrl) : null,
                String.format(getString(R.string.messageWithCommentCount), statusValue, commentCount),
                time24hr,
                appWidgetId,
                account,
                sid,
                esid,
                new ArrayList<String[]>(),
                httpClient);
    }

    @Nullable
    @Override
    public void getNotificationMessage(long account, String[] notificationMessage) {

    }

    @Override
    public void getNotifications(long account, String[] notificationMessage) {
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext), new String[]{Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);

        if (currentNotifications.moveToFirst()) {
            Set<String> notificationSids = new HashSet<>();

            // loop over notifications
            while (!currentNotifications.isAfterLast()) {
                long notificationId = currentNotifications.getLong(0);
                String sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                long updated = currentNotifications.getLong(2);
                boolean cleared = currentNotifications.getInt(3) == 1;
                String esid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(4));

                // store sids, to avoid duplicates when requesting the latest feed
                notificationSids.add(sid);

                // get comments for current notifications
                String response = SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, esid, sid))));

                if (!TextUtils.isEmpty(response)) {
                    // check for a newer post, if it's the user's own, then set CLEARED=0
                    try {
                        JSONArray comments = new JSONObject(response).getJSONArray(Sentry);
                        int i2 = comments.length();
                        if (i2 > 0) {
                            for (int i = 0; i < i2; i++) {
                                JSONObject comment = comments.getJSONObject(i);
                                long created_time = parseDate(comment.getString(SpostedDate), MYSPACE_DATE_FORMAT);
                                if (created_time > updated) {
                                    // new comment
                                    ContentValues values = new ContentValues();
                                    values.put(Notifications.UPDATED, created_time);
                                    JSONObject author = comment.getJSONObject(Sauthor);
                                    if (mAccountEsid.equals(author.getString(Sid))) {
                                        // user's own comment, clear the notification
                                        values.put(Notifications.CLEARED, 1);
                                    } else if (cleared) {
                                        values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), comment.getString(SdisplayName)));
                                        values.put(Notifications.CLEARED, 0);
                                    } else {
                                        values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), comment.getString(SdisplayName) + " and others"));
                                    }
                                    getContentResolver().update(Notifications.getContentUri(mContext), values, Notifications._ID + "=?", new String[]{Long.toString(notificationId)});
                                }
                            }
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
                    }
                }

                currentNotifications.moveToNext();
            }

            // check the latest feed
            String response = SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(MYSPACE_HISTORY, MYSPACE_BASE_URL))));

            if (!TextUtils.isEmpty(response)) {
                try {
                    JSONArray jarr = new JSONObject(response).getJSONArray(Sentry);
                    // if there are updates, clear the cache
                    int d2 = jarr.length();

                    if (d2 > 0) {
                        for (int d = 0; d < d2; d++) {
                            JSONObject o = jarr.getJSONObject(d);
                            String sid = o.getString(SstatusId);
                            // if already notified, ignore
                            if (!notificationSids.contains(sid)) {
                                if (o.has(Sauthor) && o.has(SrecentComments)) {
                                    JSONObject f = o.getJSONObject(Sauthor);

                                    if (f.has(SdisplayName) && f.has(Sid)) {
                                        String notification = null;
                                        String esid = f.getString(Sid);
                                        String friend = f.getString(SdisplayName);
                                        JSONArray comments = o.getJSONArray(SrecentComments);
                                        int commentCount = comments.length();
                                        // notifications
                                        if ((sid != null) && (commentCount > 0)) {
                                            // default hasCommented to whether or not these comments are for the own user's status
                                            boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                                            for (int c2 = 0; c2 < commentCount; c2++) {
                                                JSONObject c3 = comments.getJSONObject(c2);

                                                if (c3.has(Sauthor)) {
                                                    JSONObject c4 = c3.getJSONObject(Sauthor);

                                                    if (c4.getString(Sid).equals(mAccountEsid)) {
                                                        if (!hasCommented) {
                                                            // the user has commented on this thread, notify any updates
                                                            hasCommented = true;
                                                        }

                                                        // clear any notifications, as the user is already aware
                                                        if (notification != null) {
                                                            notification = null;
                                                        }
                                                    } else if (hasCommented) {
                                                        // don't notify about user's own comments
                                                        // send the parent comment sid
                                                        notification = String.format(getString(R.string.friendcommented), c4.getString(SdisplayName));
                                                    }
                                                }
                                            }
                                        }
                                        if (notification != null) {
                                            // new notification
                                            addNotification(sid, esid, friend, o.getString(Sstatus), parseDate(o.getString("moodStatusLastUpdated"), MYSPACE_DATE_FORMAT), account, notification);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
                }
            }
        }
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        try {
            HttpPut httpPut = new HttpPut(String.format(MYSPACE_URL_STATUSMOOD, MYSPACE_BASE_URL));
            httpPut.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOOD_BODY, message)));
            return SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(httpPut)) != null;
        } catch (IOException e) {
            Log.e(mTag, e.toString());
        }

        return false;
    }

    @Override
    public boolean isLikeable(String statusId) {
        return false;
    }

    @Override
    public boolean isLiked(String statusId, String accountId) {
        return false;
    }

    @Override
    public boolean likeStatus(String statusId, String accountId, boolean doLike) {
        return false;
    }

    @Override
    public String getLikeText(boolean isLiked) {
        return null;
    }

    @Override
    public boolean isCommentable(String statusId) {
        return true;
    }

    @Override
    public String getCommentPretext(String accountId) {
        return null;
    }

    @Nullable
    @Override
    public String getCommentsResponse(String statusId) {
        return SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, mAccountEsid, statusId))));
    }

    @Nullable
    @Override
    public JSONArray parseComments(@NonNull String response) throws JSONException {
        return new JSONObject(response).getJSONArray(Sentry);
    }

    @Nullable
    @Override
    public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr) throws JSONException {
        HashMap<String, String> commentMap = new HashMap<String, String>();
        commentMap.put(Statuses.SID, jsonComment.getString(ScommentId));
        commentMap.put(Entities.FRIEND, jsonComment.getJSONObject(Sauthor).getString(SdisplayName));
        commentMap.put(Statuses.MESSAGE, jsonComment.getString(Sbody));
        commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(parseDate(jsonComment.getString(SpostedDate), MYSPACE_DATE_FORMAT), time24hr));
        commentMap.put(getString(R.string.like), "");
        return commentMap;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        return null;
    }

    @Override
    public boolean sendComment(@NonNull String statusId, @NonNull String message) {
        HttpPost httpPost = new HttpPost(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, mAccountEsid, statusId));

        try {
            httpPost.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOODCOMMENTS_BODY, message)));
            return !TextUtils.isEmpty(SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(httpPost)));
        } catch (IOException e) {
            if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
        }

        return false;
    }

    @Override
    String getApiKey() {
        return BuildConfig.MYSPACE_KEY;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.MYSPACE_SECRET;
    }
}
