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
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;
import com.piusvelte.sonet.provider.Entities;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import static com.piusvelte.sonet.Sonet.Screated_at;
import static com.piusvelte.sonet.Sonet.Sfull_name;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Sin_reply_to_status_id;
import static com.piusvelte.sonet.Sonet.Sname;
import static com.piusvelte.sonet.Sonet.Splaces;
import static com.piusvelte.sonet.Sonet.Sprofile_image_url;
import static com.piusvelte.sonet.Sonet.Sresult;
import static com.piusvelte.sonet.Sonet.Sscreen_name;
import static com.piusvelte.sonet.Sonet.Sstatus;
import static com.piusvelte.sonet.Sonet.Stext;
import static com.piusvelte.sonet.Sonet.Suser;
import static com.piusvelte.sonet.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.Sonet.TWITTER_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.TWITTER_MENTIONS;
import static com.piusvelte.sonet.Sonet.TWITTER_RETWEET;
import static com.piusvelte.sonet.Sonet.TWITTER_SEARCH;
import static com.piusvelte.sonet.Sonet.TWITTER_SINCE_ID;
import static com.piusvelte.sonet.Sonet.TWITTER_UPDATE;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_FEED;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_REQUEST;
import static com.piusvelte.sonet.Sonet.TWITTER_USER;
import static oauth.signpost.OAuth.OAUTH_VERIFIER;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class Twitter extends Client {

    public Twitter(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return Uri.parse("sonet://twitter");
    }

    @Override
    String getRequestUrl() {
        return String.format(getRequestUrlFormat(), getBaseUrl());
    }

    @Override
    String getAccessUrl() {
        return String.format(getAccessUrlFormat(), getBaseUrl());
    }

    @Override
    String getAuthorizeUrl() {
        return String.format(getAuthorizeUrlFormat(), getBaseUrl());
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

        if (!TextUtils.isEmpty(verifier) && sonetOAuth.retrieveAccessToken(verifier)) {
            String httpResponse = SonetHttpClient.httpResponse(mContext, sonetOAuth.getSignedRequest(new HttpGet(getVerifyCredentialsUrl())));

            if (!TextUtils.isEmpty(httpResponse)) {
                try {
                    JSONObject jobj = new JSONObject(httpResponse);
                    MemberAuthentication memberAuthentication = new MemberAuthentication();
                    memberAuthentication.username = jobj.getString(Sscreen_name);
                    memberAuthentication.token = sonetOAuth.getToken();
                    memberAuthentication.secret = sonetOAuth.getTokenSecret();
                    memberAuthentication.expiry = 0;
                    memberAuthentication.network = mNetwork;
                    memberAuthentication.id = jobj.getString(Sid);
                    return memberAuthentication;
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) Log.e(mTag, e.getMessage());
                }
            }
        }

        return null;
    }

    String getBaseUrl() {
        return TWITTER_BASE_URL;
    }

    String getFeedUrl() {
        return TWITTER_URL_FEED;
    }

    String getMentionsUrl() {
        return TWITTER_MENTIONS;
    }

    String getUpdateUrl() {
        return TWITTER_UPDATE;
    }

    String getSearchUrl() {
        return TWITTER_SEARCH;
    }

    String getUserUrl() {
        return TWITTER_USER;
    }

    String getRetweetUrl() {
        return TWITTER_RETWEET;
    }

    String getRequestUrlFormat() {
        return TWITTER_URL_REQUEST;
    }

    String getAccessUrlFormat() {
        return TWITTER_URL_ACCESS;
    }

    String getAuthorizeUrlFormat() {
        return TWITTER_URL_AUTHORIZE;
    }

    String getVerifyCredentialsUrl() {
        return String.format(Sonet.TWITTER_VERIFY_CREDENTIALS, getBaseUrl());
    }

    @Override
    void formatLink(Matcher matcher, StringBuffer stringBuffer, String link) {
        // NO-OP
    }

    @Nullable
    @Override
    public Set<String> getNotificationStatusIds(long account, String[] notificationMessage) {
        return null;
    }

    @Nullable
    @Override
    public String getFeedResponse(int status_count) {
        return SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(getFeedUrl(), getBaseUrl(), status_count))));
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        return new JSONArray(response);
    }

    @Nullable
    @Override
    public void addFeedItem(@NonNull JSONObject item, boolean display_profile, boolean time24hr, int appWidgetId, long account, HttpClient httpClient, Set<String> notificationSids, String[] notificationMessage, boolean doNotify) throws JSONException {
        JSONObject user = item.getJSONObject(Suser);

        addStatusItem(parseDate(item.getString(Screated_at), TWITTER_DATE_FORMAT),
                user.getString(Sname),
                display_profile ? user.getString(Sprofile_image_url) : null,
                item.getString(Stext),
                time24hr,
                appWidgetId,
                account,
                item.getString(Sid),
                user.getString(Sid),
                httpClient);
    }

    @Nullable
    @Override
    public void getNotificationMessage(long account, String[] notificationMessage) {
        getNotifications(account, notificationMessage);
    }

    @Override
    public void getNotifications(long account, String[] notificationMessage) {
        ArrayList<String> notificationSids = new ArrayList<>();
        String sid;
        String friend;
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext), new String[]{Notifications.SID}, Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);

        // loop over notifications
        if (currentNotifications.moveToFirst()) {
            // store sids, to avoid duplicates when requesting the latest feed
            sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(0));

            if (!notificationSids.contains(sid)) {
                notificationSids.add(sid);
            }
        }

        currentNotifications.close();
        // limit to oldest status
        String last_sid = null;
        Cursor last_status = getContentResolver().query(Statuses.getContentUri(mContext), new String[]{Statuses.SID}, Statuses.ACCOUNT + "=?", new String[]{Long.toString(account)}, Statuses.CREATED + " ASC LIMIT 1");

        if (last_status.moveToFirst()) {
            last_sid = SonetCrypto.getInstance(mContext).Decrypt(last_status.getString(0));
        }

        last_status.close();

        // get all mentions since the oldest status for this account
        String response = SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(getMentionsUrl(), getBaseUrl(), last_sid != null ? String.format(TWITTER_SINCE_ID, last_sid) : ""))));

        if (!TextUtils.isEmpty(response)) {
            try {
                JSONObject statusObj;
                JSONObject friendObj;
                JSONArray statusesArray = new JSONArray(response);

                for (int i = 0, i2 = statusesArray.length(); i < i2; i++) {
                    statusObj = statusesArray.getJSONObject(i);
                    friendObj = statusObj.getJSONObject(Suser);

                    if (!friendObj.getString(Sid).equals(mAccountEsid) && !notificationSids.contains(statusObj.getString(Sid))) {
                        friend = friendObj.getString(Sname);
                        addNotification(statusObj.getString(Sid), friendObj.getString(Sid), friend, statusObj.getString(Stext), parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT), account, friend + " mentioned you on Twitter");
                        updateNotificationMessage(notificationMessage, friend + " mentioned you on Twitter");
                    }
                }
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, "error parsing response", e);
            }
        }
    }

    private int getNext140CharactersIndex(@NonNull String message, int startIndex) {
        // check if the message needs to be trimmed
        int messageLength = message.length();

        if (messageLength - startIndex > 140) {
            // this is the max stopIndex
            int stopIndex = Math.min(messageLength, startIndex + 140);

            // find the last space to break on, defaulting to cutting at startIndex + 140
            for (int nextCharIndex = stopIndex - 1; nextCharIndex > startIndex; nextCharIndex--) {
                if (" ".equals(message.substring(nextCharIndex, nextCharIndex + 1))) {
                    stopIndex = nextCharIndex + 1;
                    break;
                }
            }

            return stopIndex;
        }

        return messageLength;
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        boolean result = false;
        HttpPost httpPost;
        int startTweetIndex = 0;

        // limit tweets to 140, breaking up the message if necessary
        do {
            int endTweetIndex = getNext140CharactersIndex(message, startTweetIndex);

            String tweet = message.substring(startTweetIndex, endTweetIndex);

            httpPost = new HttpPost(String.format(getUpdateUrl(), getBaseUrl()));
            // resolve Error 417 Expectation by Twitter
            httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(Sstatus, tweet));

            if (placeId != null) {
                params.add(new BasicNameValuePair("place_id", placeId));
                params.add(new BasicNameValuePair("lat", latitude));
                params.add(new BasicNameValuePair("long", longitude));
            }

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                result = SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(httpPost)) != null;
            } catch (UnsupportedEncodingException e) {
                Log.e(mTag, e.toString());
                break;
            }

            // advance the start index for the next tweet
            startTweetIndex = endTweetIndex;
        } while (startTweetIndex < message.length());

        return result;
    }

    @Override
    public boolean isLikeable(String statusId) {
        // retweetable
        return true;
    }

    @Override
    public boolean isLiked(String statusId, String accountId) {
        return false;
    }

    @Override
    public boolean likeStatus(String statusId, String accountId, boolean doLike) {
        if (doLike) {
            // retweet
            HttpPost httpPost = new HttpPost(String.format(getRetweetUrl(), getBaseUrl(), statusId));
            // resolve Error 417 Expectation by Twitter
            httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
            return !TextUtils.isEmpty(SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(httpPost)));
        }

        return false;
    }

    @Override
    public String getLikeText(boolean isLiked) {
        return getString(R.string.retweet);
    }

    @Override
    public boolean isCommentable(String statusId) {
        return true;
    }

    @Override
    public String getCommentPretext(String accountId) {
        return "@" + getScreenName(accountId) + " ";
    }

    @Override
    public String getCommentsResponse(String statusId) {
        return SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(getMentionsUrl(), getBaseUrl(), String.format(TWITTER_SINCE_ID, statusId)))));
    }

    @Override
    public JSONArray parseComments(String response) throws JSONException {
        return new JSONArray(response);
    }

    @Override
    public HashMap<String, String> parseComment(String statusId, JSONObject jsonComment, boolean time24hr) throws JSONException {
        String replyId = null;

        try {
            replyId = jsonComment.getString(Sin_reply_to_status_id);
        } catch (JSONException e) {
            if (BuildConfig.DEBUG) Log.d(mTag, "exception getting reply id", e);
        }

        if (statusId.equals(replyId)) {
            HashMap<String, String> commentMap = new HashMap<>();
            commentMap.put(Statuses.SID, jsonComment.getString(Sid));
            commentMap.put(Entities.FRIEND, jsonComment.getJSONObject(Suser).getString(Sname));
            commentMap.put(Statuses.MESSAGE, jsonComment.getString(Stext));
            commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(parseDate(jsonComment.getString(Screated_at), TWITTER_DATE_FORMAT), time24hr));
            commentMap.put(getString(R.string.like), getLikeText(true));
            return commentMap;
        }

        return null;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        // anonymous requests are rate limited to 150 per hour
        // authenticated requests are rate limited to 350 per hour, so authenticate this!
        String response = SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(getSearchUrl(), getBaseUrl(), latitude, longitude))));

        if (response != null) {
            LinkedHashMap<String, String> locations = new LinkedHashMap<String, String>();

            try {
                JSONArray places = new JSONObject(response).getJSONObject(Sresult).getJSONArray(Splaces);

                for (int i = 0, i2 = places.length(); i < i2; i++) {
                    JSONObject place = places.getJSONObject(i);
                    locations.put(place.getString(Sid), place.getString(Sfull_name));
                }
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }

            return locations;
        }

        return null;
    }

    @Override
    public boolean sendComment(@NonNull String statusId, @NonNull String message) {
        boolean success;
        HttpPost httpPost;
        int startTweetIndex = 0;

        // limit tweets to 140, breaking up the message if necessary
        do {
            int endTweetIndex = getNext140CharactersIndex(message, startTweetIndex);

            String tweet = message.substring(startTweetIndex, endTweetIndex);

            httpPost = new HttpPost(String.format(getUpdateUrl(), getBaseUrl()));
            // resolve Error 417 Expectation by Twitter
            httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(Sstatus, tweet));
            params.add(new BasicNameValuePair(Sin_reply_to_status_id, statusId));

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params));
                success = !TextUtils.isEmpty(SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(httpPost)));
            } catch (UnsupportedEncodingException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
                return false;
            }

            // advance the start index for the next tweet
            startTweetIndex = endTweetIndex;
        } while (startTweetIndex < message.length());

        return success;
    }

    @Nullable
    private String getScreenName(String accountId) {
        String response = SonetHttpClient.httpResponse(mContext, getOAuth().getSignedRequest(new HttpGet(String.format(getUserUrl(), getBaseUrl(), accountId))));

        if (response != null) {
            try {
                JSONObject user = new JSONObject(response);
                return user.getString(Sscreen_name);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        return null;
    }

    @Override
    String getApiKey() {
        return BuildConfig.TWITTER_KEY;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.TWITTER_SECRET;
    }
}
