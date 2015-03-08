package com.piusvelte.sonet.social;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;

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
import java.util.LinkedHashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.GOOGLEPLUS_ACTIVITIES;
import static com.piusvelte.sonet.Sonet.GOOGLEPLUS_ACTIVITY;
import static com.piusvelte.sonet.Sonet.GOOGLEPLUS_BASE_URL;
import static com.piusvelte.sonet.Sonet.GOOGLEPLUS_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.GOOGLE_ACCESS;
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
public class GooglePlusClient extends SocialClient {

    public GooglePlusClient(Context context, String token, String secret, String accountEsid) {
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
        Cursor currentNotifications;
        String sid;
        String esid;
        long notificationId;
        long updated;
        boolean cleared;
        String friend;
        // get new access token, need different request here
        HttpPost httpPost = new HttpPost(GOOGLE_ACCESS);
        List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
        httpParams.add(new BasicNameValuePair("client_id", BuildConfig.GOOGLECLIENT_ID));
        httpParams.add(new BasicNameValuePair("client_secret", BuildConfig.GOOGLECLIENT_SECRET));
        httpParams.add(new BasicNameValuePair("refresh_token", mToken));
        httpParams.add(new BasicNameValuePair("grant_type", "refresh_token"));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(httpParams));

            if ((response = SonetHttpClient.httpResponse(httpClient, httpPost)) != null) {
                JSONObject j = new JSONObject(response);

                if (j.has("access_token")) {
                    String access_token = j.getString("access_token");

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
                                if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(GOOGLEPLUS_ACTIVITY, GOOGLEPLUS_BASE_URL, sid, access_token)))) != null) {
                                    // check for a newer post, if it's the user's own, then set CLEARED=0
                                    try {
                                        JSONObject item = new JSONObject(response);

                                        if (item.has(Sobject)) {
                                            JSONObject object = item.getJSONObject(Sobject);

                                            if (object.has(Sreplies)) {
                                                int commentCount = 0;
                                                JSONObject replies = object.getJSONObject(Sreplies);

                                                if (replies.has(StotalItems)) {
                                                    commentCount = replies.getInt(StotalItems);
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

                    // get new feed
                    if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(GOOGLEPLUS_ACTIVITIES, GOOGLEPLUS_BASE_URL, "me", "public", status_count, access_token)))) != null) {
                        JSONObject r = new JSONObject(response);

                        if (r.has(Sitems)) {
                            statusesArray = r.getJSONArray(Sitems);
                            removeOldStatuses(widget, Long.toString(account));

                            for (int i1 = 0, i2 = statusesArray.length(); i1 < i2; i1++) {
                                statusObj = statusesArray.getJSONObject(i1);

                                if (statusObj.has(Sactor) && statusObj.has(Sobject)) {
                                    friendObj = statusObj.getJSONObject(Sactor);
                                    JSONObject object = statusObj.getJSONObject(Sobject);

                                    if (statusObj.has(Sid) && friendObj.has(Sid) && friendObj.has(SdisplayName) && statusObj.has(Spublished) && object.has(Sreplies) && object.has(SoriginalContent)) {
                                        sid = statusObj.getString(Sid);
                                        esid = friendObj.getString(Sid);
                                        friend = friendObj.getString(SdisplayName);
                                        String originalContent = object.getString(SoriginalContent);

                                        if ((originalContent == null) || (originalContent.length() == 0)) {
                                            originalContent = object.getString(Scontent);
                                        }

                                        String photo = null;

                                        if (display_profile && friendObj.has(Simage)) {
                                            JSONObject image = friendObj.getJSONObject(Simage);
                                            if (image.has(Surl))
                                                photo = image.getString(Surl);
                                        }

                                        long date = parseDate(statusObj.getString(Spublished), GOOGLEPLUS_DATE_FORMAT);
                                        int commentCount = 0;
                                        JSONObject replies = object.getJSONObject(Sreplies);
                                        String notification = null;

                                        if (replies.has(StotalItems)) {
                                            commentCount = replies.getInt(StotalItems);
                                        }

                                        if ((notifications != 0) && (notification != null)) {
                                            // new notification
                                            addNotification(sid, esid, friend, originalContent, date, account, notification);
                                            notificationMessage = updateNotificationMessage(notificationMessage, notification);
                                        }

                                        if (i1 < status_count) {
                                            addStatusItem(date,
                                                    friend,
                                                    photo,
                                                    String.format(getString(R.string.messageWithCommentCount), originalContent, commentCount),
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
                            }
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(mTag, e.toString());
        } catch (JSONException e) {
            Log.e(mTag, e.toString());
        }

        return notificationMessage;
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
    String getApiKey() {
        return BuildConfig.GOOGLECLIENT_ID;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.GOOGLECLIENT_SECRET;
    }
}
