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
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.Statuses;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Request;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

/**
 * Created by bemmanuel on 2/15/15.
 */
public class Twitter extends Client {

    private static final String TWITTER_BASE_URL = "https://api.twitter.com/";
    private static final String TWITTER_URL_REQUEST = "%soauth/request_token";
    private static final String TWITTER_URL_AUTHORIZE = "%soauth/authorize";
    private static final String TWITTER_URL_ACCESS = "%soauth/access_token";
    private static final String TWITTER_URL_FEED = "%s1.1/statuses/home_timeline.json?count=%s";
    private static final String TWITTER_RETWEET = "%s1.1/statuses/retweets/%s.json";
    private static final String TWITTER_USER = "%s1.1/users/show.json?user_id=%s";
    private static final String TWITTER_UPDATE = "%s1.1/statuses/update.json";
    private static final String TWITTER_SEARCH = "%s1.1/geo/search.json?lat=%s&long=%s";
    private static final String TWITTER_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";
    private static final String TWITTER_MENTIONS = "%s1.1/statuses/mentions_timeline.json%s";
    private static final String TWITTER_SINCE_ID = "?since_id=%s";
    private static final String TWITTER_VERIFY_CREDENTIALS = "%s1.1/account/verify_credentials.json";
    private static final String TWITTER_PROFILE = "http://twitter.com/%s";

    public Twitter(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public String getProfileUrl(@NonNull String esid) {
        Request request = getOAuth10Helper().getBuilder()
                .url(String.format(getUserUrl(), getBaseUrl(), esid))
                .build();

        String response = SonetHttpClient.getResponse(request);

        if (!TextUtils.isEmpty(response)) {

            try {
                JSONObject user = new JSONObject(response);
                return String.format(getProfileUrl(), user.getString("screen_name"));
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(mTag, "Error parsing: " + response, e);
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getProfilePhotoUrl(String esid) {
        Request request = getOAuth10Helper().getBuilder()
                .url(String.format(getUserUrl(), getBaseUrl(), esid))
                .build();

        String response = SonetHttpClient.getResponse(request);

        if (!TextUtils.isEmpty(response)) {

            try {
                JSONObject user = new JSONObject(response);
                return user.getString(Sprofile_image_url);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(mTag, "Error parsing: " + response, e);
                }
            }
        }

        return null;
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
    public MemberAuthentication getMemberAuthentication(@NonNull String authenticatedUrl) {
        if (getOAuth10Helper().getAccessToken(SonetHttpClient.getOkHttpClientInstance(), authenticatedUrl)) {
            Request request = getOAuth10Helper().getBuilder()
                    .url(getVerifyCredentialsUrl())
                    .build();
            String httpResponse = SonetHttpClient.getResponse(request);

            if (!TextUtils.isEmpty(httpResponse)) {
                try {
                    JSONObject jobj = new JSONObject(httpResponse);
                    MemberAuthentication memberAuthentication = new MemberAuthentication();
                    memberAuthentication.username = jobj.getString(Sscreen_name);
                    memberAuthentication.token = getOAuth10Helper().getToken();
                    memberAuthentication.secret = getOAuth10Helper().getSecret();
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
        return String.format(TWITTER_VERIFY_CREDENTIALS, getBaseUrl());
    }

    String getProfileUrl() {
        return TWITTER_PROFILE;
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
        Request request = getOAuth10Helper().getBuilder()
                .url(String.format(getFeedUrl(), getBaseUrl(), status_count))
                .build();
        return SonetHttpClient.getResponse(request);
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        return new JSONArray(response);
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
        JSONObject user = item.getJSONObject(Suser);

        addStatusItem(parseDate(item.getString(Screated_at), TWITTER_DATE_FORMAT),
                user.getString(Sname),
                display_profile ? user.getString(Sprofile_image_url) : null,
                item.getString(Stext),
                time24hr,
                appWidgetId,
                account,
                item.getString(Sid),
                user.getString(Sid));
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
        Cursor currentNotifications = getContentResolver()
                .query(Notifications.getContentUri(mContext), new String[] { Notifications.SID }, Notifications.ACCOUNT + "=?",
                        new String[] { Long.toString(account) }, null);

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
        Cursor last_status = getContentResolver().query(Statuses.getContentUri(mContext), new String[] { Statuses.SID }, Statuses.ACCOUNT + "=?",
                new String[] { Long.toString(account) }, Statuses.CREATED + " ASC LIMIT 1");

        if (last_status.moveToFirst()) {
            last_sid = SonetCrypto.getInstance(mContext).Decrypt(last_status.getString(0));
        }

        last_status.close();

        // get all mentions since the oldest status for this account
        Request request = getOAuth10Helper().getBuilder()
                .url(String.format(getMentionsUrl(), getBaseUrl(), last_sid != null ? String.format(TWITTER_SINCE_ID, last_sid) : ""))
                .build();
        String response = SonetHttpClient.getResponse(request);

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
                        addNotification(statusObj.getString(Sid), friendObj.getString(Sid), friend, statusObj.getString(Stext),
                                parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT), account, friend + " mentioned you on Twitter");
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
        int startTweetIndex = 0;

        // limit tweets to 140, breaking up the message if necessary
        do {
            int endTweetIndex = getNext140CharactersIndex(message, startTweetIndex);

            String tweet = message.substring(startTweetIndex, endTweetIndex);

            FormEncodingBuilder builder = new FormEncodingBuilder()
                    .add("http.protocol.expect-continue", Boolean.FALSE.toString())
                    .add(Sstatus, tweet);

            if (placeId != null) {
                builder.add("place_id", placeId)
                        .add("lat", latitude)
                        .add("long", longitude);
            }

            Request request = getOAuth10Helper().getBuilder()
                    .url(String.format(getUpdateUrl(), getBaseUrl()))
                    .post(builder.build())
                    .build();

            result = SonetHttpClient.request(request);

            if (!result) {
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
            Request request = getOAuth10Helper().getBuilder()
                    .url(String.format(getRetweetUrl(), getBaseUrl(), statusId))
                    .post(new FormEncodingBuilder()
                            .add("http.protocol.expect-continue", Boolean.FALSE.toString())
                            .build())
                    .build();
            return SonetHttpClient.request(request);
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
        Request request = getOAuth10Helper().getBuilder()
                .url(String.format(getMentionsUrl(), getBaseUrl(), String.format(TWITTER_SINCE_ID, statusId)))
                .build();
        return SonetHttpClient.getResponse(request);
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
            commentMap.put(Entity.FRIEND, jsonComment.getJSONObject(Suser).getString(Sname));
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
        Request request = getOAuth10Helper().getBuilder()
                .url(String.format(getSearchUrl(), getBaseUrl(), latitude, longitude))
                .build();
        String response = SonetHttpClient.getResponse(request);

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

            FormEncodingBuilder builder = new FormEncodingBuilder()
                    .add("http.protocol.expect-continue", Boolean.FALSE.toString())
                    .add(Sstatus, tweet)
                    .add(Sin_reply_to_status_id, statusId);

            Request request = getOAuth10Helper().getBuilder()
                    .url(String.format(getUpdateUrl(), getBaseUrl()))
                    .post(builder.build())
                    .build();

            success = SonetHttpClient.request(request);

            if (!success) {
                break;
            }

            // advance the start index for the next tweet
            startTweetIndex = endTweetIndex;
        } while (startTweetIndex < message.length());

        return success;
    }

    @Override
    public List<HashMap<String, String>> getFriends() {
        return null;
    }

    @Nullable
    private String getScreenName(String accountId) {
        Request request = getOAuth10Helper().getBuilder()
                .url(String.format(getUserUrl(), getBaseUrl(), accountId))
                .build();
        String response = SonetHttpClient.getResponse(request);

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
