package com.piusvelte.sonet.social;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.PhotoUploadService;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
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

import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.Sonet.FACEBOOK_COMMENTS;
import static com.piusvelte.sonet.Sonet.FACEBOOK_HOME;
import static com.piusvelte.sonet.Sonet.FACEBOOK_LIKES;
import static com.piusvelte.sonet.Sonet.FACEBOOK_PICTURE;
import static com.piusvelte.sonet.Sonet.FACEBOOK_POST;
import static com.piusvelte.sonet.Sonet.FACEBOOK_SEARCH;
import static com.piusvelte.sonet.Sonet.FACEBOOK_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.FACEBOOK_URL_ME;
import static com.piusvelte.sonet.Sonet.Saccess_token;
import static com.piusvelte.sonet.Sonet.Scomments;
import static com.piusvelte.sonet.Sonet.Screated_time;
import static com.piusvelte.sonet.Sonet.Sdata;
import static com.piusvelte.sonet.Sonet.Sexpires_in;
import static com.piusvelte.sonet.Sonet.Sfrom;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Slink;
import static com.piusvelte.sonet.Sonet.Smessage;
import static com.piusvelte.sonet.Sonet.Sname;
import static com.piusvelte.sonet.Sonet.Sphoto;
import static com.piusvelte.sonet.Sonet.Spicture;
import static com.piusvelte.sonet.Sonet.Splace;
import static com.piusvelte.sonet.Sonet.Ssource;
import static com.piusvelte.sonet.Sonet.Sstatus;
import static com.piusvelte.sonet.Sonet.Sstory;
import static com.piusvelte.sonet.Sonet.Stags;
import static com.piusvelte.sonet.Sonet.Sto;
import static com.piusvelte.sonet.Sonet.Stype;
import static com.piusvelte.sonet.Sonet.Suser_likes;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class FacebookClient extends SocialClient {

    public FacebookClient(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return Uri.parse("fbconnect://success");
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
        Uri uri = Uri.parse(authenticatedUrl);
        String token = uri.getQueryParameter(Saccess_token);

        if (!TextUtils.isEmpty(token)) {
            String expiryValue = uri.getQueryParameter(Sexpires_in);
            int expiry = 0;

            if (!TextUtils.isEmpty(expiryValue) && !"0".equals(expiryValue)) {
                expiry = (int) System.currentTimeMillis() + Integer.parseInt(expiryValue) * 1000;
            }

            String httpResponse = SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(FACEBOOK_URL_ME, FACEBOOK_BASE_URL, Saccess_token, token)));

            if (!TextUtils.isEmpty(httpResponse)) {
                try {
                    JSONObject jobj = new JSONObject(httpResponse);

                    if (jobj.has(Sname) && jobj.has(Sid)) {
                        MemberAuthentication memberAuthentication = new MemberAuthentication();
                        memberAuthentication.username = jobj.getString(Sname);
                        memberAuthentication.token = token;
                        memberAuthentication.secret = "";
                        memberAuthentication.expiry = expiry;
                        memberAuthentication.network = mNetwork;
                        memberAuthentication.id = jobj.getString(Sid);
                        return memberAuthentication;
                    }
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) {
                        Log.d(mTag, "error parsing facebook me: " + httpResponse, e);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getAuthUrl(@NonNull SonetOAuth sonetOAuth) {
        return String.format(FACEBOOK_URL_AUTHORIZE, FACEBOOK_BASE_URL, BuildConfig.FACEBOOK_ID, getCallback().toString());
    }

    @Override
    String getFirstPhotoUrl(String[] parts) {
        // facebook wall post handling
        if (parts.length > 0 && Spicture.equals(parts[0])) {
            return parts[1];
        }

        return super.getFirstPhotoUrl(parts);
    }

    @Override
    String getPostFriendOverride(String friend) {
        // facebook wall post handling
        if (friend.indexOf(">") > 0) {
            return friend;
        }

        return super.getPostFriendOverride(friend);
    }

    @Override
    String getPostFriend(String friend) {
        if (friend.indexOf(">") > 0) {
            return friend.substring(0, friend.indexOf(">") - 1);
        }

        return super.getPostFriend(friend);
    }

    private void getNotifications(long account, @NonNull Set<String> notificationSids, @Nullable String[] notificationMessage) {
        Cursor currentNotifications = mContext.getContentResolver().query(Sonet.Notifications.getContentUri(mContext), new String[]{Sonet.Notifications._ID, Sonet.Notifications.SID, Sonet.Notifications.UPDATED, Sonet.Notifications.CLEARED, Sonet.Notifications.ESID}, Sonet.Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);

        // loop over notifications
        if (currentNotifications.moveToFirst()) {
            long notificationId;
            long updated;
            String sid;
            boolean cleared;

            while (!currentNotifications.isAfterLast()) {
                notificationId = currentNotifications.getLong(0);
                sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                updated = currentNotifications.getLong(2);
                cleared = currentNotifications.getInt(3) == 1;

                // store sids, to avoid duplicates when requesting the latest feed
                notificationSids.add(sid);

                // get comments for current notifications
                String response = SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, sid, Saccess_token, mToken)));

                if (!TextUtils.isEmpty(response)) {
                    JSONArray commentsArray;

                    // check for a newer post, if it's the user's own, then set CLEARED=0
                    try {
                        commentsArray = new JSONObject(response).getJSONArray(Sdata);
                        final int i2 = commentsArray.length();

                        if (i2 > 0) {
                            for (int i = 0; i < i2; i++) {
                                JSONObject commentObj = commentsArray.getJSONObject(i);
                                final long created_time = commentObj.getLong(Screated_time) * 1000;

                                if (created_time > updated) {
                                    final JSONObject from = commentObj.getJSONObject(Sfrom);
                                    updateNotificationMessage(notificationMessage,
                                            updateNotification(notificationId, created_time, mAccountEsid, from.getString(Sid), from.getString(Sname), cleared));
                                }
                            }
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG)
                            Log.e(mTag, "error parsing notifications: " + response, e);
                    }
                }

                currentNotifications.moveToNext();
            }
        }

        currentNotifications.close();
    }

    @Nullable
    @Override
    public Set<String> getNotificationStatusIds(long account, String[] notificationMessage) {
        Set<String> notificationSids = new HashSet<>();
        getNotifications(account, notificationSids, notificationMessage);
        return notificationSids;
    }

    @Nullable
    @Override
    public String getFeedResponse(int status_count) {
        return SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(FACEBOOK_HOME, FACEBOOK_BASE_URL, Saccess_token, mToken)));
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        return new JSONObject(response).getJSONArray(Sdata);
    }

    @Nullable
    @Override
    public void addFeedItem(@NonNull JSONObject item, boolean display_profile, boolean time24hr, int appWidgetId, long account, HttpClient httpClient, Set<String> notificationSids, String[] notificationMessage, boolean doNotify) throws JSONException {
        ArrayList<String[]> links = new ArrayList<>();

        // only parse status types, not photo, video or link
        if (item.has(Stype) && item.has(Sfrom) && item.has(Sid)) {
            JSONObject friendObj = item.getJSONObject("from");

            if (friendObj.has(Sname) && friendObj.has(Sid)) {
                String friend = friendObj.getString(Sname);
                String esid = friendObj.getString(Sid);
                String sid = item.getString(Sid);
                StringBuilder message = new StringBuilder();

                if (item.has(Smessage)) {
                    message.append(item.getString(Smessage));
                } else if (item.has(Sstory)) {
                    message.append(item.getString(Sstory));
                }

                if (item.has(Spicture)) {
                    links.add(new String[]{Spicture, item.getString(Spicture)});
                }

                if (item.has(Slink)) {
                    links.add(new String[]{item.getString(Stype), item.getString(Slink)});

                    if (!item.has(Spicture) || !item.getString(Stype).equals(Sphoto)) {
                        message.append("(");
                        message.append(item.getString(Stype));
                        message.append(": ");
                        message.append(Uri.parse(item.getString(Slink)).getHost());
                        message.append(")");
                    }
                }

                if (item.has(Ssource)) {
                    links.add(new String[]{item.getString(Stype), item.getString(Ssource)});

                    if (!item.has(Spicture) || !item.getString(Stype).equals(Sphoto)) {
                        message.append("(");
                        message.append(item.getString(Stype));
                        message.append(": ");
                        message.append(Uri.parse(item.getString(Ssource)).getHost());
                        message.append(")");
                    }
                }

                long date = item.getLong(Screated_time) * 1000;
                String notification = null;

                if (item.has(Sto)) {
                    // handle wall messages from one friend to another
                    JSONObject t = item.getJSONObject(Sto);

                    if (t.has(Sdata)) {
                        JSONObject n = t.getJSONArray(Sdata).getJSONObject(0);

                        if (n.has(Sname)) {
                            friend += " > " + n.getString(Sname);

                            if (!notificationSids.contains(sid) && n.has(Sid) && (n.getString(Sid).equals(mAccountEsid))) {
                                notification = String.format(getString(R.string.friendcommented), friend);
                            }
                        }
                    }
                }
                int commentCount = 0;

                if (item.has(Scomments)) {
                    JSONObject jo = item.getJSONObject(Scomments);

                    if (jo.has(Sdata)) {
                        JSONArray commentsArray = jo.getJSONArray(Sdata);
                        commentCount = commentsArray.length();

                        if (!notificationSids.contains(sid) && (commentCount > 0)) {
                            // default hasCommented to whether or not these comments are for the own user's status
                            boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                            for (int c2 = 0; c2 < commentCount; c2++) {
                                JSONObject commentObj = commentsArray.getJSONObject(c2);
                                // if new notification, or updated

                                if (commentObj.has(Sfrom)) {
                                    JSONObject c4 = commentObj.getJSONObject(Sfrom);

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
                                        notification = String.format(getString(R.string.friendcommented), c4.getString(Sname));
                                    }
                                }
                            }
                        }
                    }
                }

                if (doNotify && notification != null) {
                    // new notification
                    addNotification(sid, esid, friend, message.toString(), item.getLong(Screated_time) * 1000, account, notification);
                    updateNotificationMessage(notificationMessage, notification);
                }

                addStatusItem(date,
                        friend,
                        display_profile ? String.format(FACEBOOK_PICTURE, esid) : null,
                        String.format(getString(R.string.messageWithCommentCount), message.toString(), commentCount),
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

    @Nullable
    @Override
    public void getNotificationMessage(long account, String[] notificationMessage) {
        // NO-OP
    }

    @Override
    public void getNotifications(long account, String[] notificationMessage) {
        Set<String> notificationSids = new HashSet<>();
        getNotifications(account, notificationSids, notificationMessage);

        if (!notificationSids.isEmpty()) {
            // check the latest feed
            String response = SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(FACEBOOK_HOME, FACEBOOK_BASE_URL, Saccess_token, mToken)));

            if (!TextUtils.isEmpty(response)) {
                try {
                    JSONArray jarr = new JSONObject(response).getJSONArray(Sdata);
                    // if there are updates, clear the cache
                    int d2 = jarr.length();

                    if (d2 > 0) {
                        for (int d = 0; d < d2; d++) {
                            JSONObject o = jarr.getJSONObject(d);
                            String sid = o.getString(Sid);

                            // if already notified, ignore
                            if (!notificationSids.contains(sid)) {
                                // only parse status types, not photo, video or link
                                if (o.has(Stype) && o.has(Sfrom)) {
                                    JSONObject f = o.getJSONObject(Sfrom);

                                    if (f.has(Sname) && f.has(Sid)) {
                                        String notification = null;
                                        String esid = f.getString(Sid);
                                        String friend = f.getString(Sname);

                                        if (o.has(Sto)) {
                                            // handle wall messages from one friend to another
                                            JSONObject t = o.getJSONObject(Sto);

                                            if (t.has(Sdata)) {
                                                JSONObject n = t.getJSONArray(Sdata).getJSONObject(0);

                                                if (n.has(Sname)) {
                                                    if (n.has(Sid) && (n.getString(Sid).equals(mAccountEsid))) {
                                                        notification = String.format(getString(R.string.friendcommented), friend);
                                                    }
                                                }
                                            }
                                        }
                                        int commentCount = 0;

                                        if (o.has(Scomments)) {
                                            JSONObject jo = o.getJSONObject(Scomments);

                                            if (jo.has(Sdata)) {
                                                JSONArray comments = jo.getJSONArray(Sdata);
                                                commentCount = comments.length();

                                                // notifications
                                                if ((sid != null) && (commentCount > 0)) {
                                                    // default hasCommented to whether or not these comments are for the own user's status
                                                    boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                                                    for (int c2 = 0; c2 < commentCount; c2++) {
                                                        JSONObject c3 = comments.getJSONObject(c2);

                                                        if (c3.has(Sfrom)) {
                                                            JSONObject c4 = c3.getJSONObject(Sfrom);

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
                                                                notification = String.format(getString(R.string.friendcommented), c4.getString(Sname));
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (notification != null) {
                                            String message = o.has(Smessage) ? o.getString(Smessage) : null;

                                            if (!o.getString(Stype).equals(Sstatus) && o.has(Slink)) {
                                                message = message == null ? "[" + o.getString(Stype) + "]" : "[" + o.getString(Stype) + "]";
                                            }

                                            // new notification
                                            addNotification(sid, esid, friend, message, o.getLong(Screated_time) * 1000, account, notification);
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
        StringBuilder formattedTags = null;

        if (tags != null && tags.length > 0) {
            formattedTags = new StringBuilder();
            formattedTags.append("[");
            String tag_format;

            if (!TextUtils.isEmpty(photoPath)) {
                tag_format = "{\"tag_uid\":\"%s\",\"x\":0,\"y\":0}";
            } else {
                tag_format = "%s";
            }

            for (int i = 0, l = tags.length; i < l; i++) {
                if (i > 0) {
                    formattedTags.append(",");
                }

                formattedTags.append(String.format(tag_format, tags[i]));
            }

            formattedTags.append("]");
        }

        if (!TextUtils.isEmpty(photoPath)) {
            // upload photo
            // uploading a photo takes a long time, have the service handle it
            Intent i = Sonet.getPackageIntent(mContext, PhotoUploadService.class);
            i.setAction(Sonet.ACTION_UPLOAD);
            i.putExtra(Sonet.Accounts.TOKEN, mToken);
            i.putExtra(Sonet.Widgets.INSTANT_UPLOAD, photoPath);
            i.putExtra(Sonet.Statuses.MESSAGE, message);
            i.putExtra(Splace, placeId);

            if (tags != null) {
                i.putExtra(Stags, tags.toString());
            }

            mContext.startService(i);
            return true;
        } else {
            // regular post
            HttpPost httpPost = new HttpPost(String.format(FACEBOOK_POST, FACEBOOK_BASE_URL, Saccess_token, mToken));
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(Smessage, message));

            if (placeId != null) {
                params.add(new BasicNameValuePair(Splace, placeId));
            }

            if (tags != null) {
                params.add(new BasicNameValuePair(Stags, tags.toString()));
            }

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                return SonetHttpClient.httpResponse(mContext, httpPost) != null;
            } catch (UnsupportedEncodingException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        return false;
    }

    @Override
    public boolean isLikeable(String statusId) {
        return true;
    }

    @Override
    public boolean isLiked(String statusId, String accountId) {
        String response = SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, statusId, Saccess_token, mToken)));

        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray likes = new JSONObject(response).getJSONArray(Sdata);

                for (int i = 0, i2 = likes.length(); i < i2; i++) {
                    JSONObject like = likes.getJSONObject(i);

                    if (like.getString(Sid).equals(accountId)) {
                        return true;
                    }
                }
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        return false;
    }

    @Override
    public boolean likeStatus(String statusId, String accountId, boolean doLike) {
        if (doLike) {
            return SonetHttpClient.httpResponse(mContext, new HttpPost(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, statusId, Saccess_token, mToken))) != null;
        } else {
            HttpDelete httpDelete = new HttpDelete(String.format(FACEBOOK_LIKES, FACEBOOK_BASE_URL, statusId, Saccess_token, mToken));
            httpDelete.setHeader("Content-Length", "0");
            return SonetHttpClient.httpResponse(mContext, httpDelete) != null;
        }
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
        return SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, statusId, Saccess_token, mToken)));
    }

    @Nullable
    @Override
    public JSONArray parseComments(@NonNull String response) throws JSONException {
        return new JSONObject(response).getJSONArray(Sdata);
    }

    @Nullable
    @Override
    public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr) throws JSONException {
        HashMap<String, String> commentMap = new HashMap<>();
        commentMap.put(Sonet.Statuses.SID, jsonComment.getString(Sid));
        commentMap.put(Sonet.Entities.FRIEND, jsonComment.getJSONObject(Sfrom).getString(Sname));
        commentMap.put(Sonet.Statuses.MESSAGE, jsonComment.getString(Smessage));
        commentMap.put(Sonet.Statuses.CREATEDTEXT, Sonet.getCreatedText(jsonComment.getLong(Screated_time) * 1000, time24hr));
        commentMap.put(getString(R.string.like), getLikeText(jsonComment.has(Suser_likes) && jsonComment.getBoolean(Suser_likes)));
        return commentMap;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        String response = SonetHttpClient.httpResponse(mContext, new HttpGet(String.format(FACEBOOK_SEARCH, FACEBOOK_BASE_URL, latitude, longitude, Saccess_token, mToken)));

        if (response != null) {
            LinkedHashMap<String, String> locations = new LinkedHashMap<String, String>();

            try {
                JSONArray places = new JSONObject(response).getJSONArray(Sdata);

                for (int i = 0, i2 = places.length(); i < i2; i++) {
                    JSONObject place = places.getJSONObject(i);
                    locations.put(place.getString(Sid), place.getString(Sname));
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
        HttpPost httpPost = new HttpPost(String.format(FACEBOOK_COMMENTS, FACEBOOK_BASE_URL, statusId, Saccess_token, mToken));
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(Smessage, message));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            return !TextUtils.isEmpty(SonetHttpClient.httpResponse(mContext, httpPost));
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
        }

        return false;
    }

    @Override
    String getApiKey() {
        return BuildConfig.FACEBOOK_ID;
    }

    @Override
    String getApiSecret() {
        return null;
    }
}
