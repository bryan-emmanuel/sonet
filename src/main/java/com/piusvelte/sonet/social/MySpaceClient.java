package com.piusvelte.sonet.social;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.MYSPACE_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.MYSPACE_HISTORY;
import static com.piusvelte.sonet.Sonet.MYSPACE_STATUSMOOD_BODY;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOOD;
import static com.piusvelte.sonet.Sonet.MYSPACE_URL_STATUSMOODCOMMENTS;
import static com.piusvelte.sonet.Sonet.Sauthor;
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

/**
 * Created by bemmanuel on 2/15/15.
 */
public class MySpaceClient extends SocialClient {

    public MySpaceClient(Context context, String token, String secret, String accountEsid) {
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
        SonetOAuth sonetOAuth = getOAuth();

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
                    esid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(4));

                    // store sids, to avoid duplicates when requesting the latest feed
                    if (!notificationSids.contains(sid)) {
                        notificationSids.add(sid);
                    }

                    // get comments for current notifications
                    if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_URL_STATUSMOODCOMMENTS, MYSPACE_BASE_URL, esid, sid))))) != null) {
                        // check for a newer post, if it's the user's own, then set CLEARED=0
                        try {
                            commentsArray = new JSONObject(response).getJSONArray(Sentry);
                            final int i2 = commentsArray.length();

                            if (i2 > 0) {
                                for (int i = 0; i < i2; i++) {
                                    commentObj = commentsArray.getJSONObject(i);
                                    long created_time = parseDate(commentObj.getString(SpostedDate), MYSPACE_DATE_FORMAT);

                                    if (created_time > updated) {
                                        friendObj = commentObj.getJSONObject(Sauthor);
                                        notificationMessage = updateNotificationMessage(notificationMessage,
                                                updateNotification(notificationId, created_time, mAccountEsid, friendObj.getString(Sid), friendObj.getString(SdisplayName), cleared));
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
        if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_HISTORY, MYSPACE_BASE_URL))))) != null) {
            try {
                statusesArray = new JSONObject(response).getJSONArray(Sentry);
                // if there are updates, clear the cache
                int e2 = statusesArray.length();

                if (e2 > 0) {
                    removeOldStatuses(widget, Long.toString(account));

                    for (int e = 0; e < e2; e++) {
                        links.clear();
                        statusObj = statusesArray.getJSONObject(e);
                        friendObj = statusObj.getJSONObject(Sauthor);
                        long date = parseDate(statusObj.getString(SmoodStatusLastUpdated), MYSPACE_DATE_FORMAT);
                        esid = statusObj.getString(SuserId);
                        int commentCount = 0;
                        sid = statusObj.getString(SstatusId);
                        friend = friendObj.getString(SdisplayName);
                        String statusValue = statusObj.getString(Sstatus);
                        String notification = null;
                        if (statusObj.has(SrecentComments)) {
                            commentsArray = statusObj.getJSONArray(SrecentComments);
                            commentCount = commentsArray.length();

                            // notifications
                            if ((sid != null) && !notificationSids.contains(sid) && (commentCount > 0)) {
                                // default hasCommented to whether or not these comments are for the own user's status
                                boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                                for (int c2 = 0; c2 < commentCount; c2++) {
                                    commentObj = commentsArray.getJSONObject(c2);

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

                        if ((notifications != 0) && (notification != null)) {
                            // new notification
                            addNotification(sid, esid, friend, statusValue, date, account, notification);
                            notificationMessage = updateNotificationMessage(notificationMessage, notification);
                        }

                        if (e < status_count) {
                            addStatusItem(date,
                                    friend,
                                    display_profile ? friendObj.getString(SthumbnailUrl) : null,
                                    String.format(getString(R.string.messageWithCommentCount), statusValue, commentCount),
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
                } else {
                    // warn about myspace permissions
                    addStatusItem(0,
                            getString(R.string.myspace_permissions_title),
                            null,
                            getString(R.string.myspace_permissions_message),
                            service,
                            time24hr,
                            appWidgetId,
                            account,
                            "",
                            "",
                            new ArrayList<String[]>(),
                            httpClient);
                }
            } catch (JSONException e) {
                Log.e(mTag, service + ":" + e.toString());
            }
        } else {
            // warn about myspace permissions
            addStatusItem(0,
                    getString(R.string.myspace_permissions_title),
                    null,
                    getString(R.string.myspace_permissions_message),
                    service,
                    time24hr,
                    appWidgetId,
                    account,
                    "",
                    "",
                    new ArrayList<String[]>(),
                    httpClient);
        }

        return notificationMessage;
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
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        return null;
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
