package com.piusvelte.sonet.social;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;
import com.piusvelte.sonet.provider.Notifications;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static com.piusvelte.sonet.Sonet.Saccess_token;
import static com.piusvelte.sonet.Sonet.Sactor;
import static com.piusvelte.sonet.Sonet.Scontent;
import static com.piusvelte.sonet.Sonet.SdisplayName;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Simage;
import static com.piusvelte.sonet.Sonet.Sitems;
import static com.piusvelte.sonet.Sonet.Sobject;
import static com.piusvelte.sonet.Sonet.SoriginalContent;
import static com.piusvelte.sonet.Sonet.Spublished;
import static com.piusvelte.sonet.Sonet.Sreplies;
import static com.piusvelte.sonet.Sonet.StotalItems;
import static com.piusvelte.sonet.Sonet.Surl;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class GooglePlus extends Client {

    private static final String GOOGLEPLUS_AUTHORIZE = "https://accounts.google.com/o/oauth2/auth?client_id=%s&redirect_uri=%s&scope=https://www" +
            ".googleapis.com/auth/plus.me&response_type=code";
    private static final String GOOGLE_ACCESS = "https://accounts.google.com/o/oauth2/token";
    private static final String GOOGLEPLUS_BASE_URL = "https://www.googleapis.com/plus/v1/";
    private static final String GOOGLEPLUS_URL_ME = "%speople/me?fields=displayName,id&access_token=%s";
    private static final String GOOGLEPLUS_ACTIVITIES = "%speople/%s/activities/%s?maxResults=%s&access_token=%s";
    private static final String GOOGLEPLUS_ACTIVITY = "%sactivities/%s?access_token=%s";
    private static final String GOOGLEPLUS_PROFILE = "https://plus.google.com/%s";
    private static final String GOOGLEPLUS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public GooglePlus(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public String getProfileUrl(@NonNull String esid) {
        return String.format(GOOGLEPLUS_PROFILE, esid);
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return null;//Uri.parse("http://localhost")
    }

    @Override
    String getRequestUrl() {
        return null;
    }

    @Override
    String getAccessUrl() {
        return null;
    }

    @Override
    String getAuthorizeUrl() {
        return null;
    }

    @Override
    public String getCallbackUrl() {
        return null;
    }

    @Override
    boolean isOAuth10a() {
        return false;
    }

    @Override
    public MemberAuthentication getMemberAuthentication(@NonNull SonetOAuth sonetOAuth, @NonNull String authenticatedUrl) {
        // get the access_token
        String[] title = authenticatedUrl.split("=");

        if (title != null && title.length > 0) {
            String code = title[1];

            if (!TextUtils.isEmpty(code)) {
                HttpPost httpPost = new HttpPost(GOOGLE_ACCESS);
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("code", code));
                params.add(new BasicNameValuePair("client_id", BuildConfig.GOOGLECLIENT_ID));
                params.add(new BasicNameValuePair("client_secret", BuildConfig.GOOGLECLIENT_SECRET));
                params.add(new BasicNameValuePair("redirect_uri", "urn:ietf:wg:oauth:2.0:oob"));
                params.add(new BasicNameValuePair("grant_type", "authorization_code"));

                try {
                    httpPost.setEntity(new UrlEncodedFormEntity(params));
                    String response = SonetHttpClient.httpResponse(mContext, httpPost);

                    if (!TextUtils.isEmpty(response)) {
                        JSONObject j = new JSONObject(response);
                        if (j.has("access_token") && j.has("refresh_token")) {
                            String refresh_token = j.getString("refresh_token");
                            String httpResponse = SonetHttpClient.httpResponse(mContext,
                                    new HttpGet(String.format(GOOGLEPLUS_URL_ME, GOOGLEPLUS_BASE_URL, j.getString("access_token"))));

                            if (!TextUtils.isEmpty(httpResponse)) {

                                try {
                                    JSONObject jObj = new JSONObject(response);

                                    if (jObj.has(Sid) && jObj.has(SdisplayName)) {
                                        MemberAuthentication memberAuthentication = new MemberAuthentication();
                                        memberAuthentication.username = jObj.getString(SdisplayName);
                                        memberAuthentication.token = refresh_token;
                                        memberAuthentication.secret = "";
                                        memberAuthentication.expiry = 0;
                                        memberAuthentication.network = mNetwork;
                                        memberAuthentication.id = jObj.getString(Sid);
                                        return memberAuthentication;
                                    }
                                } catch (JSONException e) {
                                    if (BuildConfig.DEBUG) {
                                        Log.d(mTag, e.toString());
                                    }
                                }
                            }
                        }
                    } else {
                        return null;
                    }
                } catch (UnsupportedEncodingException e) {
                    if (BuildConfig.DEBUG) {
                        Log.d(mTag, e.toString());
                    }
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) {
                        Log.d(mTag, e.toString());
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getAuthUrl(@NonNull SonetOAuth sonetOAuth) {
        return String.format(GOOGLEPLUS_AUTHORIZE, BuildConfig.GOOGLECLIENT_ID, "urn:ietf:wg:oauth:2.0:oob");
    }

    @Nullable
    @Override
    public Set<String> getNotificationStatusIds(long account, String[] notificationMessage) {
        Set<String> notificationSids = new HashSet<>();
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext),
                new String[] { Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID },
                Notifications.ACCOUNT + "=?", new String[] { Long.toString(account) }, null);

        // loop over notifications
        if (currentNotifications.moveToFirst()) {
            while (!currentNotifications.isAfterLast()) {
                long notificationId = currentNotifications.getLong(0);
                String sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                long updated = currentNotifications.getLong(2);
                boolean cleared = currentNotifications.getInt(3) == 1;

                // store sids, to avoid duplicates when requesting the latest feed
                if (!notificationSids.contains(sid)) {
                    notificationSids.add(sid);
                }

                // TODO
                // get comments for current notifications
//                String response = SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(GOOGLEPLUS_ACTIVITY, GOOGLEPLUS_BASE_URL, sid,
// access_token)));
//
//                if (!TextUtils.isEmpty(response)) {
//                    // check for a newer post, if it's the user's own, then set CLEARED=0
//                    try {
//                        JSONObject item = new JSONObject(response);
//
//                        if (item.has(Sobject)) {
//                            JSONObject object = item.getJSONObject(Sobject);
//
//                            if (object.has(Sreplies)) {
//                                int commentCount = 0;
//                                JSONObject replies = object.getJSONObject(Sreplies);
//
//                                if (replies.has(StotalItems)) {
//                                    commentCount = replies.getInt(StotalItems);
//                                }
//                            }
//                        }
//                    } catch (JSONException e) {
//                        // TODO
//                    }
//                }

                currentNotifications.moveToNext();
            }
        }

        currentNotifications.close();
        return notificationSids;
    }

    @Nullable
    @Override
    public String getFeedResponse(int status_count) {
        // get new access token, need different request here
        HttpPost httpPost = new HttpPost(GOOGLE_ACCESS);
        List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
        httpParams.add(new BasicNameValuePair("client_id", BuildConfig.GOOGLECLIENT_ID));
        httpParams.add(new BasicNameValuePair("client_secret", BuildConfig.GOOGLECLIENT_SECRET));
        httpParams.add(new BasicNameValuePair("refresh_token", mToken));
        httpParams.add(new BasicNameValuePair("grant_type", "refresh_token"));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(httpParams));

            String response = SonetHttpClient.httpResponse(mContext, httpPost);

            if (!TextUtils.isEmpty(response)) {
                JSONObject j = new JSONObject(response);

                if (j.has("access_token")) {
                    String access_token = j.getString("access_token");

                    if (!TextUtils.isEmpty(access_token)) {
                        return SonetHttpClient.httpResponse(mContext,
                                new HttpGet(String.format(GOOGLEPLUS_ACTIVITIES, GOOGLEPLUS_BASE_URL, "me", "public", status_count, access_token)));
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) Log.d(mTag, "error setting entity", e);
        } catch (JSONException e) {
            if (BuildConfig.DEBUG) Log.d(mTag, "error parsing response", e);
        }

        return null;
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        JSONObject r = new JSONObject(response);

        if (r.has(Sitems)) {
            return r.getJSONArray(Sitems);
        }

        return null;
    }

    @Nullable
    @Override
    public void addFeedItem(@NonNull JSONObject item,
            boolean display_profile,
            boolean time24hr,
            int appWidgetId,
            long account,
            HttpClient httpClient,
            Set<String> notificationSids,
            String[] notificationMessage,
            boolean doNotify) throws JSONException {

        if (item.has(Sactor) && item.has(Sobject)) {
            JSONObject friendObj = item.getJSONObject(Sactor);
            JSONObject object = item.getJSONObject(Sobject);

            if (item.has(Sid) && friendObj.has(Sid) && friendObj.has(SdisplayName) && item.has(Spublished) && object.has(Sreplies) && object
                    .has(SoriginalContent)) {
                String sid = item.getString(Sid);
                String esid = friendObj.getString(Sid);
                String friend = friendObj.getString(SdisplayName);
                String originalContent = object.getString(SoriginalContent);

                if ((originalContent == null) || (originalContent.length() == 0)) {
                    originalContent = object.getString(Scontent);
                }

                String photo = null;

                if (display_profile && friendObj.has(Simage)) {
                    JSONObject image = friendObj.getJSONObject(Simage);
                    if (image.has(Surl)) {
                        photo = image.getString(Surl);
                    }
                }

                long date = parseDate(item.getString(Spublished), GOOGLEPLUS_DATE_FORMAT);
                int commentCount = 0;
                JSONObject replies = object.getJSONObject(Sreplies);
//                String notification = null;

                if (replies.has(StotalItems)) {
                    commentCount = replies.getInt(StotalItems);
                }

//                if (doNotify && notification != null) {
//                    // new notification
//                    addNotification(sid, esid, friend, originalContent, date, account, notification);
//                    updateNotificationMessage(notificationMessage, notification);
//                }

                addStatusItem(date,
                        friend,
                        photo,
                        String.format(getString(R.string.messageWithCommentCount), originalContent, commentCount),
                        time24hr,
                        appWidgetId,
                        account,
                        sid,
                        esid,
                        new ArrayList<String[]>(),
                        httpClient);
            }
        }
    }

    @Nullable
    @Override
    public void getNotificationMessage(long account, String[] notificationMessage) {
        // NO-OP
    }

    @Override
    public void getNotifications(long account, String[] notificationMessage) {
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext),
                new String[] { Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID },
                Notifications.ACCOUNT + "=?", new String[] { Long.toString(account) }, null);

        if (currentNotifications.moveToFirst()) {
            Set<String> notificationSids = new HashSet<>();

            // get new access token, need different request here
            HttpPost httpPost = new HttpPost(GOOGLE_ACCESS);
            List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
            httpParams.add(new BasicNameValuePair("client_id", BuildConfig.GOOGLECLIENT_ID));
            httpParams.add(new BasicNameValuePair("client_secret", BuildConfig.GOOGLECLIENT_SECRET));
            httpParams.add(new BasicNameValuePair("refresh_token", mToken));
            httpParams.add(new BasicNameValuePair("grant_type", "refresh_token"));

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(httpParams));
                String response = SonetHttpClient.httpResponse(mContext, httpPost);

                if (!TextUtils.isEmpty(response)) {
                    JSONObject j = new JSONObject(response);
                    if (j.has(Saccess_token)) {
                        String access_token = j.getString(Saccess_token);
                        while (!currentNotifications.isAfterLast()) {
                            long notificationId = currentNotifications.getLong(0);
                            String sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                            long updated = currentNotifications.getLong(2);
                            boolean cleared = currentNotifications.getInt(3) == 1;
                            // store sids, to avoid duplicates when requesting the latest feed
                            if (!notificationSids.contains(sid)) {
                                notificationSids.add(sid);
                            }
                            // get comments for current notifications
                            if ((response = SonetHttpClient.httpResponse(mContext,
                                    new HttpGet(String.format(GOOGLEPLUS_ACTIVITY, GOOGLEPLUS_BASE_URL, sid, access_token)))) != null) {
                                // check for a newer post, if it's the user's own, then set CLEARED=0
                                try {
                                    JSONObject item = new JSONObject(response);
                                    if (item.has(Sobject)) {
                                        JSONObject object = item.getJSONObject(Sobject);
                                        if (object.has(Sreplies)) {
                                            int commentCount = 0;
                                            JSONObject replies = object.getJSONObject(Sreplies);
                                            if (replies.has(StotalItems)) {
                                                //TODO: notifications
                                            }
                                        }
                                    }
                                } catch (JSONException e) {
                                    if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
                                }
                            }
                            currentNotifications.moveToNext();
                        }
                        // get new feed
                        if ((response = SonetHttpClient.httpResponse(mContext,
                                new HttpGet(String.format(GOOGLEPLUS_ACTIVITIES, GOOGLEPLUS_BASE_URL, "me", "public", 20, access_token)))) != null) {
                            JSONObject r = new JSONObject(response);
                            if (r.has(Sitems)) {
                                JSONArray items = r.getJSONArray(Sitems);
                                for (int i1 = 0, i2 = items.length(); i1 < i2; i1++) {
                                    JSONObject item = items.getJSONObject(i1);
                                    if (item.has(Sactor) && item.has(Sobject)) {
                                        JSONObject actor = item.getJSONObject(Sactor);
                                        JSONObject object = item.getJSONObject(Sobject);
                                        if (item.has(Sid) && actor.has(Sid) && actor.has(SdisplayName) && item.has(Spublished) && object
                                                .has(Sreplies) && object.has(SoriginalContent)) {
                                            String sid = item.getString(Sid);
                                            String esid = actor.getString(Sid);
                                            String friend = actor.getString(SdisplayName);
                                            String originalContent = object.getString(SoriginalContent);
                                            if ((originalContent == null) || (originalContent.length() == 0)) {
                                                originalContent = object.getString(Scontent);
                                            }
                                            String photo = null;
                                            if (actor.has(Simage)) {
                                                JSONObject image = actor.getJSONObject(Simage);
                                                if (image.has(Surl)) {
                                                    photo = image.getString(Surl);
                                                }
                                            }
                                            long date = parseDate(item.getString(Spublished), GOOGLEPLUS_DATE_FORMAT);
                                            int commentCount = 0;
                                            JSONObject replies = object.getJSONObject(Sreplies);
                                            String notification = null;

                                            if (replies.has(StotalItems)) {
//                                                Log.d(TAG, Sreplies + ":" + replies.toString());
                                                commentCount = replies.getInt(StotalItems);
                                            }

                                            if (notification != null) {
                                                // new notification
                                                addNotification(sid, esid, friend, originalContent, date, account, notification);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        return false;
    }

    @Override
    public boolean isLikeable(String statusId) {
        return true;
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
        return "+1";
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
        return null;
    }

    @Nullable
    @Override
    public JSONArray parseComments(@NonNull String response) throws JSONException {
        return null;
    }

    @Nullable
    @Override
    public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr) throws JSONException {
        return null;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        return null;
    }

    @Override
    public boolean sendComment(@NonNull String statusId, @NonNull String message) {
        return false;
    }

    @Override
    public List<HashMap<String, String>> getFriends() {
        return null;
    }

    @Override
    String getApiKey() {
        return BuildConfig.GOOGLECLIENT_ID;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.GOOGLECLIENT_SECRET;
    }
}
