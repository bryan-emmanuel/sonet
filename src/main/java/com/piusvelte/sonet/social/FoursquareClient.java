package com.piusvelte.sonet.social;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.piusvelte.sonet.Sonet.FOURSQUARE_ADDCOMMENT;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_BASE_URL;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_CHECKIN;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_CHECKINS;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_CHECKIN_NO_SHOUT;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_CHECKIN_NO_VENUE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_GET_CHECKIN;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_SEARCH;
import static com.piusvelte.sonet.Sonet.SNearby;
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
public class FoursquareClient extends SocialClient {

    public FoursquareClient(Context context, String token, String secret, String accountEsid) {
        super(context, token, secret, accountEsid);
    }

    @Override
    public String getFeed(int appWidgetId, String widget, long account, int service, int status_count, boolean time24hr, boolean display_profile, int notifications, HttpClient httpClient) {
        String notificationMessage = null;
        String response;
        JSONArray statusesArray;
        ArrayList<String[]> links = new ArrayList<String[]>();
        final ArrayList<String> notificationSids = new ArrayList<String>();
        JSONObject statusObj;
        JSONObject friendObj;
        JSONArray commentsArray;
        JSONObject commentObj;
        Cursor currentNotifications;
        String sid;
        String esid;
        long notificationId;
        long updated;
        boolean cleared;
        String friend;
        // notifications
        if (notifications != 0) {
            currentNotifications = getContentResolver().query(Sonet.Notifications.getContentUri(mContext), new String[]{Sonet.Notifications._ID, Sonet.Notifications.SID, Sonet.Notifications.UPDATED, Sonet.Notifications.CLEARED, Sonet.Notifications.ESID}, Sonet.Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);
            // loop over notifications
            if (currentNotifications.moveToFirst()) {
                while (!currentNotifications.isAfterLast()) {
                    notificationId = currentNotifications.getLong(0);
                    sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                    updated = currentNotifications.getLong(2);
                    cleared = currentNotifications.getInt(3) == 1;

                    // store sids, to avoid duplicates when requesting the latest feed
                    if (!notificationSids.contains(sid)) {
                        notificationSids.add(sid);
                    }

                    // get comments for current notifications
                    if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, sid, mToken)))) != null) {
                        // check for a newer post, if it's the user's own, then set CLEARED=0
                        try {
                            commentsArray = new JSONObject(response).getJSONObject(Sresponse).getJSONObject(Scheckin).getJSONObject(Scomments).getJSONArray(Sitems);
                            int i2 = commentsArray.length();

                            if (i2 > 0) {
                                for (int i = 0; i < i2; i++) {
                                    commentObj = commentsArray.getJSONObject(i);
                                    long created_time = commentObj.getLong(ScreatedAt) * 1000;

                                    if (created_time > updated) {
                                        friendObj = commentObj.getJSONObject(Suser);
                                        notificationMessage = updateNotificationMessage(notificationMessage,
                                                updateNotification(notificationId, created_time, mAccountEsid, friendObj.getString(Sid), friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName), cleared));
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(mTag, service + ":" + e.toString());
                        }
                    }
                    currentNotifications.moveToNext();
                }
            }
            currentNotifications.close();
        }
        // parse the response
        if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(FOURSQUARE_CHECKINS, FOURSQUARE_BASE_URL, mToken)))) != null) {
            try {
                statusesArray = new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Srecent);
                // if there are updates, clear the cache
                int e2 = statusesArray.length();
                if (e2 > 0) {
                    removeOldStatuses(widget, Long.toString(account));

                    for (int e = 0; e < e2; e++) {
                        links.clear();
                        statusObj = statusesArray.getJSONObject(e);
                        friendObj = statusObj.getJSONObject(Suser);
                        String shout = "";

                        if (statusObj.has(Sshout)) {
                            shout = statusObj.getString(Sshout) + "\n";
                        }

                        if (statusObj.has(Svenue)) {
                            JSONObject venue = statusObj.getJSONObject(Svenue);

                            if (venue.has(Sname)) {
                                shout += "@" + venue.getString(Sname);
                            }
                        }

                        long date = statusObj.getLong(ScreatedAt) * 1000;
                        // notifications
                        esid = friendObj.getString(Sid);
                        int commentCount = 0;
                        sid = statusObj.getString(Sid);
                        friend = friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName);
                        String notification = null;

                        if (statusObj.has(Scomments)) {
                            commentsArray = statusObj.getJSONObject(Scomments).getJSONArray(Sitems);
                            commentCount = commentsArray.length();

                            if (!notificationSids.contains(sid) && (commentCount > 0)) {
                                // default hasCommented to whether or not these comments are for the own user's status
                                boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                                for (int c2 = 0; c2 < commentCount; c2++) {
                                    commentObj = commentsArray.getJSONObject(c2);

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
                                            notification = String.format(getString(R.string.friendcommented), c4.getString(SfirstName) + " " + c4.getString(SlastName));
                                        }
                                    }
                                }
                            }
                        }

                        if ((notifications != 0) && (notification != null)) {
                            // new notification
                            addNotification(sid, esid, friend, shout, date, account, notification);
                            notificationMessage = updateNotificationMessage(notificationMessage, notification);
                        }

                        if (e < status_count) {
                            addStatusItem(date,
                                    friend,
                                    display_profile ? friendObj.getString(Sphoto) : null,
                                    String.format(getString(R.string.messageWithCommentCount), shout, commentCount),
                                    service,
                                    time24hr,
                                    appWidgetId,
                                    account,
                                    sid,
                                    esid,
                                    links,
                                    httpClient);
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(mTag, service + ":" + e.toString());
                Log.e(mTag, response);
            }
        }

        return notificationMessage;
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

        HttpPost httpPost;

        if (!TextUtils.isEmpty(placeId)) {
            if (!TextUtils.isEmpty(message)) {
                httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN, FOURSQUARE_BASE_URL, placeId, message, latitude, longitude, mToken));
            } else {
                httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_SHOUT, FOURSQUARE_BASE_URL, placeId, latitude, longitude, mToken));
            }
        } else {
            httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_VENUE, FOURSQUARE_BASE_URL, message, mToken));
        }

        return SonetHttpClient.httpResponse(mContext, httpPost) != null;
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
        return SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(FOURSQUARE_GET_CHECKIN, FOURSQUARE_BASE_URL, statusId, mToken)));
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
        commentMap.put(Sonet.Statuses.SID, jsonComment.getString(Sid));
        commentMap.put(Sonet.Entities.FRIEND, user.getString(SfirstName) + " " + user.getString(SlastName));
        commentMap.put(Sonet.Statuses.MESSAGE, jsonComment.getString(Stext));
        commentMap.put(Sonet.Statuses.CREATEDTEXT, Sonet.getCreatedText(jsonComment.getLong(ScreatedAt) * 1000, time24hr));
        commentMap.put(getString(R.string.like), "");
        return commentMap;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        String response = SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(FOURSQUARE_SEARCH, FOURSQUARE_BASE_URL, latitude, longitude, mToken)));

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
            HttpPost httpPost = new HttpPost(String.format(FOURSQUARE_ADDCOMMENT, FOURSQUARE_BASE_URL, statusId, message, mToken));
            return !TextUtils.isEmpty(SonetHttpClient.httpResponse(mContext, httpPost));
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
        }

        return false;
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
