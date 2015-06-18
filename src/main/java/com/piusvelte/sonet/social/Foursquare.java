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
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.Statuses;
import com.squareup.okhttp.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static com.piusvelte.sonet.Sonet.SNearby;
import static com.piusvelte.sonet.Sonet.Saccess_token;
import static com.piusvelte.sonet.Sonet.Scheckin;
import static com.piusvelte.sonet.Sonet.Scomments;
import static com.piusvelte.sonet.Sonet.ScreatedAt;
import static com.piusvelte.sonet.Sonet.SfirstName;
import static com.piusvelte.sonet.Sonet.Sgroups;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Sitems;
import static com.piusvelte.sonet.Sonet.SlastName;
import static com.piusvelte.sonet.Sonet.Sname;
import static com.piusvelte.sonet.Sonet.Sphoto;
import static com.piusvelte.sonet.Sonet.Srecent;
import static com.piusvelte.sonet.Sonet.Sresponse;
import static com.piusvelte.sonet.Sonet.Sshout;
import static com.piusvelte.sonet.Sonet.Stext;
import static com.piusvelte.sonet.Sonet.Suser;
import static com.piusvelte.sonet.Sonet.Svenue;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class Foursquare extends Client {

    private static final String API_VERSION = "20140101";
    private static final String PHOTO_SIZE = "500x500";

    private static final String FOURSQUARE_BASE_URL = "https://api.foursquare.com/v2/";
    private static final String FOURSQUARE_URL_AUTHORIZE = "https://foursquare" +
            ".com/oauth2/authorize?client_id=%s&response_type=token&redirect_uri=%s&display=touch";
    private static final String FOURSQUARE_URL_ME = "%susers/self?oauth_token=%s&m=foursquare&v=" + API_VERSION;
    private static final String FOURSQUARE_URL_PROFILE = "https://foursquare.com/user/%s&m=foursquare&v=" + API_VERSION;
    private static final String FOURSQUARE_CHECKINS = "%scheckins/recent?oauth_token=%s&m=foursquare&v=" + API_VERSION;
    private static final String FOURSQUARE_CHECKIN = "%scheckins/add?venueId=%s&shout=%s&ll=%s,%s&broadcast=public&oauth_token=%s&m=foursquare&v="
            + API_VERSION;
    private static final String FOURSQUARE_CHECKIN_NO_VENUE = "%scheckins/add?shout=%s&broadcast=public&oauth_token=%s&m=foursquare&v=" + API_VERSION;
    private static final String FOURSQUARE_CHECKIN_NO_SHOUT = "%scheckins/add?venueId=%s&ll=%s,%s&broadcast=public&oauth_token=%s&m=foursquare&v="
            + API_VERSION;
    private static final String FOURSQUARE_ADDCOMMENT = "%scheckins/%s/addcomment?text=%s&oauth_token=%s&m=foursquare&v=" + API_VERSION;
    private static final String FOURSQUARE_SEARCH = "%svenues/search?ll=%s,%s&intent=checkin&oauth_token=%s&m=foursquare&v=" + API_VERSION;
    private static final String FOURSQUARE_GET_CHECKIN = "%scheckins/%s?oauth_token=%s&m=foursquare&v=" + API_VERSION;
    private static final String FOURSQUARE_URL_USER = "%susers/%s?oauth_token=%s&m=foursquare&v=" + API_VERSION;

    public Foursquare(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public String getProfileUrl(@NonNull String esid) {
        return String.format(FOURSQUARE_URL_PROFILE, esid);
    }

    @Nullable
    @Override
    public String getProfilePhotoUrl(String esid) {
        String httpResponse = SonetHttpClient.httpResponse(String.format(FOURSQUARE_URL_ME, FOURSQUARE_BASE_URL, mToken));

        if (!TextUtils.isEmpty(httpResponse)) {
            JSONObject jobj;

            try {
                jobj = (new JSONObject(httpResponse)).getJSONObject("response").getJSONObject("user");
                JSONObject photo = jobj.getJSONObject(Sphoto);
                return photo.getString("prefix") + PHOTO_SIZE + photo.getString("suffix");
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) {
                    Log.d(mTag, "error parsing foursquare me response: " + httpResponse, e);
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return Uri.parse("sonet://foursquare");
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
    public MemberAuthentication getMemberAuthentication(@NonNull String authenticatedUrl) {
        String token = getParamValue(authenticatedUrl, Saccess_token);

        if (!TextUtils.isEmpty(token)) {
            String httpResponse = SonetHttpClient.httpResponse(String.format(FOURSQUARE_URL_ME, FOURSQUARE_BASE_URL, token));

            if (!TextUtils.isEmpty(httpResponse)) {
                JSONObject jobj;

                try {
                    jobj = (new JSONObject(httpResponse)).getJSONObject("response").getJSONObject("user");

                    if (jobj.has("firstName") && jobj.has(Sid)) {
                        MemberAuthentication memberAuthentication = new MemberAuthentication();
                        memberAuthentication.username = jobj.getString("firstName") + " " + jobj.getString("lastName");
                        memberAuthentication.token = token;
                        memberAuthentication.secret = "";
                        memberAuthentication.expiry = 0;
                        memberAuthentication.network = mNetwork;
                        memberAuthentication.id = jobj.getString(Sid);
                        return memberAuthentication;
                    }
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) {
                        Log.d(mTag, "error parsing foursquare me response: " + httpResponse, e);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getAuthUrl() {
        return String.format(FOURSQUARE_URL_AUTHORIZE, BuildConfig.FOURSQUARE_KEY, getCallback().toString());
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

                // get comments for current notifications
                String response = SonetHttpClient
                        .httpResponse(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, sid, mToken));

                if (!TextUtils.isEmpty(response)) {
                    // check for a newer post, if it's the user's own, then set CLEARED=0
                    try {
                        JSONArray commentsArray = new JSONObject(response).getJSONObject(Sresponse).getJSONObject(Scheckin).getJSONObject(Scomments)
                                .getJSONArray(Sitems);
                        int i2 = commentsArray.length();

                        if (i2 > 0) {
                            for (int i = 0; i < i2; i++) {
                                JSONObject commentObj = commentsArray.getJSONObject(i);
                                long created_time = commentObj.getLong(ScreatedAt) * 1000;

                                if (created_time > updated) {
                                    JSONObject friendObj = commentObj.getJSONObject(Suser);
                                    updateNotificationMessage(notificationMessage,
                                            updateNotification(notificationId, created_time, mAccountEsid, friendObj.getString(Sid),
                                                    friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName), cleared));
                                }
                            }
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) Log.d(mTag, "error parsing notifications", e);
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
        return SonetHttpClient.httpResponse(String.format(FOURSQUARE_CHECKINS, FOURSQUARE_BASE_URL, mToken));
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        return new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Srecent);
    }

    @Nullable
    @Override
    public void addFeedItem(@NonNull JSONObject item,
            boolean display_profile,
            boolean time24hr,
            int appWidgetId,
            long account,
            Set<String> notificationSids,
            String[] notificationMessage,
            boolean doNotify) throws JSONException {
        JSONObject friendObj = item.getJSONObject(Suser);
        String shout = "";

        if (item.has(Sshout)) {
            shout = item.getString(Sshout) + "\n";
        }

        if (item.has(Svenue)) {
            JSONObject venue = item.getJSONObject(Svenue);

            if (venue.has(Sname)) {
                shout += "@" + venue.getString(Sname);
            }
        }

        long date = item.getLong(ScreatedAt) * 1000;
        // notifications
        String esid = friendObj.getString(Sid);
        int commentCount = 0;
        String sid = item.getString(Sid);
        String friend = friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName);
        String notification = null;

        if (item.has(Scomments)) {
            JSONObject commentsJObj = item.getJSONObject(Scomments);

            if (commentsJObj.has(Sitems)) {
                JSONArray commentsArray = item.getJSONObject(Scomments).getJSONArray(Sitems);
                commentCount = commentsArray.length();

                if (!notificationSids.contains(sid) && commentCount > 0) {
                    // default hasCommented to whether or not these comments are for the own user's status
                    boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                    for (int c2 = 0; c2 < commentCount; c2++) {
                        JSONObject commentObj = commentsArray.getJSONObject(c2);

                        if (commentObj.has(Suser)) {
                            JSONObject c4 = commentObj.getJSONObject(Suser);

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
                                notification = String.format(getString(R.string.friendcommented),
                                        c4.getString(SfirstName) + " " + c4.getString(SlastName));
                            }
                        }
                    }
                }
            }
        }

        if (doNotify && notification != null) {
            // new notification
            addNotification(sid, esid, friend, shout, date, account, notification);
            updateNotificationMessage(notificationMessage, notification);
        }

        JSONObject photo = friendObj.getJSONObject(Sphoto);

        addStatusItem(date,
                friend,
                display_profile ? photo.getString("prefix") + PHOTO_SIZE + photo.getString("suffix") : null,
                String.format(getString(R.string.messageWithCommentCount), shout, commentCount),
                time24hr,
                appWidgetId,
                account,
                sid,
                esid
        );
    }

    @Nullable
    @Override
    public void getNotificationMessage(long account, String[] notificationMessage) {

    }

    @Override
    public void getNotifications(long account, String[] notificationMessage) {
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext),
                new String[] { Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID },
                Notifications.ACCOUNT + "=?", new String[] { Long.toString(account) }, null);

        if (currentNotifications.moveToFirst()) {
            Set<String> notificationSids = new HashSet<>();

            // loop over notifications
            while (!currentNotifications.isAfterLast()) {
                long notificationId = currentNotifications.getLong(0);
                String sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                long updated = currentNotifications.getLong(2);
                boolean cleared = currentNotifications.getInt(3) == 1;

                // store sids, to avoid duplicates when requesting the latest feed
                notificationSids.add(sid);

                // get comments for current notifications
                String response = SonetHttpClient
                        .httpResponse(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, sid, mToken));

                if (!TextUtils.isEmpty(response)) {
                    // check for a newer post, if it's the user's own, then set CLEARED=0
                    try {
                        JSONArray comments = new JSONObject(response).getJSONObject(Sresponse).getJSONObject(Scheckin).getJSONObject(Scomments)
                                .getJSONArray(Sitems);
                        int i2 = comments.length();
                        if (i2 > 0) {
                            for (int i = 0; i < i2; i++) {
                                JSONObject comment = comments.getJSONObject(i);
                                long created_time = comment.getLong(ScreatedAt) * 1000;
                                if (created_time > updated) {
                                    // new comment
                                    ContentValues values = new ContentValues();
                                    values.put(Notifications.UPDATED, created_time);
                                    JSONObject user = comment.getJSONObject(Suser);

                                    if (mAccountEsid.equals(user.getString(Sid))) {
                                        // user's own comment, clear the notification
                                        values.put(Notifications.CLEARED, 1);
                                    } else if (cleared) {
                                        values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented),
                                                user.getString(SfirstName) + " " + user.getString(SlastName)));
                                        values.put(Notifications.CLEARED, 0);
                                    } else {
                                        values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented),
                                                user.getString(SfirstName) + " " + user.getString(SlastName) + " and others"));
                                    }

                                    getContentResolver().update(Notifications.getContentUri(mContext), values, Notifications._ID + "=?",
                                            new String[] { Long.toString(notificationId) });
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
            String response = SonetHttpClient.httpResponse(String.format(FOURSQUARE_CHECKINS, FOURSQUARE_BASE_URL, mToken));

            if (!TextUtils.isEmpty(response)) {
                try {
                    JSONArray jarr = new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Srecent);
                    // if there are updates, clear the cache
                    int d2 = jarr.length();

                    if (d2 > 0) {
                        for (int d = 0; d < d2; d++) {
                            JSONObject o = jarr.getJSONObject(d);
                            String sid = o.getString(Sid);

                            // if already notified, ignore
                            if (!notificationSids.contains(sid)) {
                                if (o.has(Suser) && o.has(Scomments)) {
                                    JSONObject f = o.getJSONObject(Suser);
                                    if (f.has(SfirstName) && f.has(SlastName) && f.has(Sid)) {
                                        String notification = null;
                                        String esid = f.getString(Sid);
                                        String friend = f.getString(SfirstName) + " " + f.getString(SlastName);
                                        JSONArray comments = o.getJSONArray(Scomments);
                                        int commentCount = comments.length();

                                        // notifications
                                        if (commentCount > 0) {
                                            // default hasCommented to whether or not these comments are for the own user's status
                                            boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                                            for (int c2 = 0; c2 < commentCount; c2++) {
                                                JSONObject c3 = comments.getJSONObject(c2);

                                                if (c3.has(Suser)) {
                                                    JSONObject c4 = c3.getJSONObject(Suser);

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
                                                        notification = String.format(getString(R.string.friendcommented),
                                                                c4.getString(SfirstName) + " " + c4.getString(SlastName));
                                                    }
                                                }
                                            }
                                        }

                                        if (notification != null) {
                                            String message = "";

                                            if (o.has(Sshout)) {
                                                message = o.getString(Sshout) + "\n";
                                            }

                                            if (o.has(Svenue)) {
                                                JSONObject venue = o.getJSONObject(Svenue);
                                                if (venue.has(Sname)) {
                                                    message += "@" + venue.getString(Sname);
                                                }
                                            }

                                            // new notification
                                            addNotification(sid, esid, friend, message, o.getLong(ScreatedAt) * 1000, account, notification);
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

        currentNotifications.close();
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        try {
            message = URLEncoder.encode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) {
                Log.d(mTag, "url encode exception: " + message, e);
            }
        }

        String url;

        if (!TextUtils.isEmpty(placeId)) {
            if (!TextUtils.isEmpty(message)) {
                url = String.format(FOURSQUARE_CHECKIN, FOURSQUARE_BASE_URL, placeId, message, latitude, longitude, mToken);
            } else {
                url = String.format(FOURSQUARE_CHECKIN_NO_SHOUT, FOURSQUARE_BASE_URL, placeId, latitude, longitude, mToken);
            }
        } else {
            url = String.format(FOURSQUARE_CHECKIN_NO_VENUE, FOURSQUARE_BASE_URL, message, mToken);
        }

        Request request = new Request.Builder()
                .url(url)
                .post(null)
                .build();
        return SonetHttpClient.request(request);
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
        return getString(isLiked ? R.string.unlike : R.string.like);
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
        return SonetHttpClient.httpResponse(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, statusId, mToken));
    }

    @Nullable
    @Override
    public JSONArray parseComments(@NonNull String response) throws JSONException {
        return new JSONObject(response).getJSONObject(Sresponse).getJSONObject(Scheckin).getJSONObject(Scomments).getJSONArray(Sitems);
    }

    @Nullable
    @Override
    public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr) throws JSONException {
        JSONObject user = jsonComment.getJSONObject(Suser);
        HashMap<String, String> commentMap = new HashMap<>();
        commentMap.put(Statuses.SID, jsonComment.getString(Sid));
        commentMap.put(Entity.FRIEND, user.getString(SfirstName) + " " + user.getString(SlastName));
        commentMap.put(Statuses.MESSAGE, jsonComment.getString(Stext));
        commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(jsonComment.getLong(ScreatedAt) * 1000, time24hr));
        commentMap.put(getString(R.string.like), "");
        return commentMap;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        String response = SonetHttpClient
                .httpResponse(String.format(FOURSQUARE_SEARCH, FOURSQUARE_BASE_URL, latitude, longitude, mToken));

        if (!TextUtils.isEmpty(response)) {
            LinkedHashMap<String, String> locations = new LinkedHashMap<String, String>();

            try {
                JSONArray groups = new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Sgroups);
                for (int g = 0, g2 = groups.length(); g < g2; g++) {
                    JSONObject group = groups.getJSONObject(g);

                    if (group.getString(Sname).equals(SNearby)) {
                        JSONArray places = group.getJSONArray(Sitems);

                        for (int i = 0, i2 = places.length(); i < i2; i++) {
                            JSONObject place = places.getJSONObject(i);
                            locations.put(place.getString(Sid), place.getString(Sname));
                        }
                        break;
                    }
                }
            } catch (JSONException e) {
                Log.e(mTag, e.toString());
            }

            return locations;
        }

        return null;
    }

    @Override
    public boolean sendComment(@NonNull String statusId, @NonNull String message) {
        try {
            message = URLEncoder.encode(message, "UTF-8");
            Request request = new Request.Builder()
                    .url(String.format(FOURSQUARE_ADDCOMMENT, FOURSQUARE_BASE_URL, statusId, message, mToken))
                    .post(null)
                    .build();

            return SonetHttpClient.request(request);
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
        }

        return false;
    }

    @Override
    public List<HashMap<String, String>> getFriends() {
        return null;
    }

    @Override
    String getApiKey() {
        return BuildConfig.FOURSQUARE_KEY;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.FOURSQUARE_SECRET;
    }
}
