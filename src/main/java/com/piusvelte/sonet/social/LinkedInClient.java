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
import com.piusvelte.sonet.SonetOAuth;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.piusvelte.sonet.Sonet.LINKEDIN_BASE_URL;
import static com.piusvelte.sonet.Sonet.LINKEDIN_HEADERS;
import static com.piusvelte.sonet.Sonet.LINKEDIN_POST;
import static com.piusvelte.sonet.Sonet.LINKEDIN_POST_BODY;
import static com.piusvelte.sonet.Sonet.LINKEDIN_UPDATE;
import static com.piusvelte.sonet.Sonet.LINKEDIN_UPDATES;
import static com.piusvelte.sonet.Sonet.LINKEDIN_UPDATE_COMMENTS;
import static com.piusvelte.sonet.Sonet.S_total;
import static com.piusvelte.sonet.Sonet.Sbody;
import static com.piusvelte.sonet.Sonet.Scomment;
import static com.piusvelte.sonet.Sonet.Sconnections;
import static com.piusvelte.sonet.Sonet.ScurrentShare;
import static com.piusvelte.sonet.Sonet.SfirstName;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.SisCommentable;
import static com.piusvelte.sonet.Sonet.Sjob;
import static com.piusvelte.sonet.Sonet.SlastName;
import static com.piusvelte.sonet.Sonet.SmemberGroups;
import static com.piusvelte.sonet.Sonet.Sname;
import static com.piusvelte.sonet.Sonet.Sperson;
import static com.piusvelte.sonet.Sonet.SpersonActivities;
import static com.piusvelte.sonet.Sonet.SpictureUrl;
import static com.piusvelte.sonet.Sonet.Sposition;
import static com.piusvelte.sonet.Sonet.SrecommendationSnippet;
import static com.piusvelte.sonet.Sonet.SrecommendationsGiven;
import static com.piusvelte.sonet.Sonet.Srecommendee;
import static com.piusvelte.sonet.Sonet.Stimestamp;
import static com.piusvelte.sonet.Sonet.Stitle;
import static com.piusvelte.sonet.Sonet.SupdateComments;
import static com.piusvelte.sonet.Sonet.SupdateContent;
import static com.piusvelte.sonet.Sonet.SupdateKey;
import static com.piusvelte.sonet.Sonet.SupdateType;
import static com.piusvelte.sonet.Sonet.Svalues;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class LinkedInClient extends SocialClient {

    private static final String IS_LIKABLE = "isLikable";
    private static final String IS_LIKED = "isLiked";
    private static final String IS_COMMENTABLE = "isCommentable";

    public LinkedInClient(Context context, String token, String secret, String accountEsid) {
        super(context, token, secret, accountEsid);
    }

    private static final <T extends HttpUriRequest> T addHeaders(@NonNull T httpUriRequest) {
        for (String[] header : LINKEDIN_HEADERS) {
            httpUriRequest.setHeader(header[0], header[1]);
        }

        return httpUriRequest;
    }

    @Override
    public String getFeed(int appWidgetId, String widget, long account, int service, int status_count, boolean time24hr, boolean display_profile, int notifications, HttpClient httpClient) {
        String notificationMessage = null;
        String response;
        JSONArray statusesArray;
        ArrayList<String[]> links = new ArrayList<String[]>();
        HttpGet httpGet;
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

                    // store sids, to avoid duplicates when requesting the latest feed
                    if (!notificationSids.contains(sid)) {
                        notificationSids.add(sid);
                    }

                    // get comments for current notifications
                    httpGet = addHeaders(new HttpGet(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, sid)));

                    if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(httpGet))) != null) {
                        // check for a newer post, if it's the user's own, then set CLEARED=0
                        try {
                            JSONObject jsonResponse = new JSONObject(response);

                            if (jsonResponse.has(S_total) && (jsonResponse.getInt(S_total) != 0)) {
                                commentsArray = jsonResponse.getJSONArray(Svalues);
                                int i2 = commentsArray.length();

                                if (i2 > 0) {
                                    for (int i = 0; i < i2; i++) {
                                        commentObj = commentsArray.getJSONObject(i);
                                        long created_time = commentObj.getLong(Stimestamp);

                                        if (created_time > updated) {
                                            friendObj = commentObj.getJSONObject(Sperson);
                                            notificationMessage = updateNotificationMessage(notificationMessage,
                                                    updateNotification(notificationId, created_time, mAccountEsid, friendObj.getString(Sid), friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName), cleared));
                                        }
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

        httpGet = addHeaders(new HttpGet(String.format(LINKEDIN_UPDATES, LINKEDIN_BASE_URL)));

        // parse the response
        if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(httpGet))) != null) {
            try {
                statusesArray = new JSONObject(response).getJSONArray(Svalues);
                // if there are updates, clear the cache
                int e2 = statusesArray.length();
                if (e2 > 0) {
                    removeOldStatuses(widget, Long.toString(account));

                    for (int e = 0; e < e2; e++) {
                        links.clear();
                        statusObj = statusesArray.getJSONObject(e);
                        String updateType = statusObj.getString(SupdateType);
                        JSONObject updateContent = statusObj.getJSONObject(SupdateContent);
                        String update = null;

                        if (((update = Sonet.LinkedIn_UpdateTypes.getMessage(updateType)) != null) && updateContent.has(Sperson)) {
                            friendObj = updateContent.getJSONObject(Sperson);

                            if (Sonet.LinkedIn_UpdateTypes.APPS.name().equals(updateType)) {
                                if (friendObj.has(SpersonActivities)) {
                                    JSONObject personActivities = friendObj.getJSONObject(SpersonActivities);

                                    if (personActivities.has(Svalues)) {
                                        JSONArray updates = personActivities.getJSONArray(Svalues);

                                        for (int u = 0, u2 = updates.length(); u < u2; u++) {
                                            update += updates.getJSONObject(u).getString(Sbody);
                                            if (u < (updates.length() - 1))
                                                update += ", ";
                                        }
                                    }
                                }
                            } else if (Sonet.LinkedIn_UpdateTypes.CONN.name().equals(updateType)) {
                                if (friendObj.has(Sconnections)) {
                                    JSONObject connections = friendObj.getJSONObject(Sconnections);

                                    if (connections.has(Svalues)) {
                                        JSONArray updates = connections.getJSONArray(Svalues);

                                        for (int u = 0, u2 = updates.length(); u < u2; u++) {
                                            update += updates.getJSONObject(u).getString(SfirstName) + " " + updates.getJSONObject(u).getString(SlastName);

                                            if (u < (updates.length() - 1)) {
                                                update += ", ";
                                            }
                                        }
                                    }
                                }
                            } else if (Sonet.LinkedIn_UpdateTypes.JOBP.name().equals(updateType)) {
                                if (updateContent.has(Sjob) && updateContent.getJSONObject(Sjob).has(Sposition) && updateContent.getJSONObject(Sjob).getJSONObject(Sposition).has(Stitle)) {
                                    update += updateContent.getJSONObject(Sjob).getJSONObject(Sposition).getString(Stitle);
                                }
                            } else if (Sonet.LinkedIn_UpdateTypes.JGRP.name().equals(updateType)) {
                                if (friendObj.has(SmemberGroups)) {
                                    JSONObject memberGroups = friendObj.getJSONObject(SmemberGroups);

                                    if (memberGroups.has(Svalues)) {
                                        JSONArray updates = memberGroups.getJSONArray(Svalues);

                                        for (int u = 0, u2 = updates.length(); u < u2; u++) {
                                            update += updates.getJSONObject(u).getString(Sname);

                                            if (u < (updates.length() - 1)) {
                                                update += ", ";
                                            }
                                        }
                                    }
                                }
                            } else if (Sonet.LinkedIn_UpdateTypes.PREC.name().equals(updateType)) {
                                if (friendObj.has(SrecommendationsGiven)) {
                                    JSONObject recommendationsGiven = friendObj.getJSONObject(SrecommendationsGiven);

                                    if (recommendationsGiven.has(Svalues)) {
                                        JSONArray updates = recommendationsGiven.getJSONArray(Svalues);
                                        for (int u = 0, u2 = updates.length(); u < u2; u++) {
                                            JSONObject recommendation = updates.getJSONObject(u);
                                            JSONObject recommendee = recommendation.getJSONObject(Srecommendee);
                                            if (recommendee.has(SfirstName))
                                                update += recommendee.getString(SfirstName);
                                            if (recommendee.has(SlastName))
                                                update += recommendee.getString(SlastName);
                                            if (recommendation.has(SrecommendationSnippet))
                                                update += ":" + recommendation.getString(SrecommendationSnippet);

                                            if (u < (updates.length() - 1)) {
                                                update += ", ";
                                            }
                                        }
                                    }
                                }
                            } else if (Sonet.LinkedIn_UpdateTypes.SHAR.name().equals(updateType) && friendObj.has(ScurrentShare)) {
                                JSONObject currentShare = friendObj.getJSONObject(ScurrentShare);

                                if (currentShare.has(Scomment)) {
                                    update = currentShare.getString(Scomment);
                                }
                            }

                            long date = statusObj.getLong(Stimestamp);
                            sid = statusObj.has(SupdateKey) ? statusObj.getString(SupdateKey) : null;
                            esid = friendObj.getString(Sid);
                            friend = friendObj.getString(SfirstName) + " " + friendObj.getString(SlastName);
                            int commentCount = 0;
                            String notification = null;

                            if (statusObj.has(SupdateComments)) {
                                JSONObject updateComments = statusObj.getJSONObject(SupdateComments);

                                if (updateComments.has(Svalues)) {
                                    commentsArray = updateComments.getJSONArray(Svalues);
                                    commentCount = commentsArray.length();

                                    if (!notificationSids.contains(sid) && (commentCount > 0)) {
                                        // default hasCommented to whether or not these comments are for the own user's status
                                        boolean hasCommented = notification != null || esid.equals(mAccountEsid);

                                        for (int c2 = 0; c2 < commentCount; c2++) {
                                            commentObj = commentsArray.getJSONObject(c2);

                                            if (commentObj.has(Sperson)) {
                                                JSONObject c4 = commentObj.getJSONObject(Sperson);

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
                            }

                            if ((notifications != 0) && (notification != null)) {
                                // new notification
                                addNotification(sid, esid, friend, update, date, account, notification);
                                notificationMessage = updateNotificationMessage(notificationMessage, notification);
                            }

                            if (e < status_count) {
                                addStatusItem(date,
                                        friend,
                                        display_profile && friendObj.has(SpictureUrl) ? friendObj.getString(SpictureUrl) : null,
                                        (statusObj.has(SisCommentable) && statusObj.getBoolean(SisCommentable) ? String.format(getString(R.string.messageWithCommentCount), update, commentCount) : update),
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
            } catch (JSONException e) {
                Log.e(mTag, service + ":" + e.toString());
            }
        }

        return notificationMessage;
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        HttpPost httpPost;

        try {
            httpPost = new HttpPost(String.format(LINKEDIN_POST, LINKEDIN_BASE_URL));
            httpPost.setEntity(new StringEntity(String.format(LINKEDIN_POST_BODY, "", message)));
            httpPost.addHeader(new BasicHeader("Content-Type", "application/xml"));
            return SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(httpPost)) != null;
        } catch (IOException e) {
            Log.e(mTag, e.toString());
        }

        return false;
    }

    @Nullable
    private JSONObject getStatus(String statusId) {
        HttpGet httpGet = addHeaders(new HttpGet(String.format(LINKEDIN_UPDATE, LINKEDIN_BASE_URL, statusId)));
        String response = SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(httpGet));

        if (response != null) {
            try {
                return new JSONObject(response);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        return null;
    }

    @Override
    public boolean isLikeable(String statusId) {
        return isLikeable(getStatus(statusId));
    }

    private boolean isLikeable(@Nullable JSONObject jsonStatus) {
        if (jsonStatus != null) {
            return jsonStatus.has(IS_LIKABLE);
        }

        return false;
    }

    @Override
    public boolean isLiked(String statusId, String accountId) {
        JSONObject jsonStatus = getStatus(statusId);

        if (jsonStatus != null && isLikeable(jsonStatus)) {
            try {
                return jsonStatus.has(IS_LIKED) && jsonStatus.getBoolean(IS_LIKED);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        return false;
    }

    @Override
    public String getLikeText(boolean isLiked) {
        return getString(isLiked ? R.string.unlike : R.string.like);
    }

    @Override
    public boolean isCommentable(String statusId) {
        JSONObject jsonStatus = getStatus(statusId);

        if (jsonStatus != null && jsonStatus.has(IS_COMMENTABLE)) {
            try {
                return jsonStatus.getBoolean(IS_COMMENTABLE);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        return false;
    }

    @Override
    public String getCommentPretext(String accountId) {
        return null;
    }

    @Nullable
    @Override
    public String getCommentsResponse(String statusId) {
        return SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(addHeaders(new HttpGet(String.format(LINKEDIN_UPDATE_COMMENTS, LINKEDIN_BASE_URL, statusId)))));
    }

    @Nullable
    @Override
    public JSONArray parseComments(@NonNull String response) throws JSONException {
        JSONObject jsonResponse = new JSONObject(response);

        if (jsonResponse.has(S_total) && (jsonResponse.getInt(S_total) > 0)) {
            return jsonResponse.getJSONArray(Svalues);
        }

        return null;
    }

    @Nullable
    @Override
    public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr) throws JSONException {
        JSONObject person = jsonComment.getJSONObject(Sperson);
        HashMap<String, String> commentMap = new HashMap<>();
        commentMap.put(Sonet.Statuses.SID, jsonComment.getString(Sid));
        commentMap.put(Sonet.Entities.FRIEND, person.getString(SfirstName) + " " + person.getString(SlastName));
        commentMap.put(Sonet.Statuses.MESSAGE, jsonComment.getString(Scomment));
        commentMap.put(Sonet.Statuses.CREATEDTEXT, Sonet.getCreatedText(jsonComment.getLong(Stimestamp), time24hr));
        commentMap.put(getString(R.string.like), "");
        return commentMap;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        return null;
    }

    @Override
    String getApiKey() {
        return BuildConfig.LINKEDIN_KEY;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.LINKEDIN_SECRET;
    }
}
