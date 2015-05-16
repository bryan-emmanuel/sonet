package com.piusvelte.sonet.social;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;
import com.piusvelte.sonet.provider.Entities;
import com.piusvelte.sonet.provider.Statuses;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static com.piusvelte.sonet.Sonet.Saccess_token;
import static com.piusvelte.sonet.Sonet.Sbody;
import static com.piusvelte.sonet.Sonet.Scomments;
import static com.piusvelte.sonet.Sonet.ScreatedDate;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Sitems;
import static com.piusvelte.sonet.Sonet.Sname;
import static com.piusvelte.sonet.Sonet.Sphoto;
import static com.piusvelte.sonet.Sonet.SsmallPhotoUrl;
import static com.piusvelte.sonet.Sonet.Stext;
import static com.piusvelte.sonet.Sonet.Stotal;
import static com.piusvelte.sonet.Sonet.Suser;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class Chatter extends Client {

    private static final String CHATTER_URL_AUTHORIZE = "https://login.salesforce" +
            ".com/services/oauth2/authorize?response_type=token&display=touch&client_id=%s&redirect_uri=%s";
    private static final String CHATTER_URL_ACCESS = "https://login.salesforce" +
            ".com/services/oauth2/token?grant_type=refresh_token&client_id=%s&refresh_token=%s";
    private static final String CHATTER_URL_ME = "%s/services/data/v22.0/chatter/users/me";
    private static final String CHATTER_URL_POST = "%s/services/data/v22.0/chatter/feeds/news/me/feed-items?text=%s";
    private static final String CHATTER_URL_COMMENT = "%s/services/data/v22.0/chatter/feed-items/%s/comments?text=%s";
    private static final String CHATTER_URL_FEED = "%s/services/data/v22.0/chatter/feeds/news/me/feed-items";
    private static final String CHATTER_URL_LIKES = "%s/services/data/v22.0/chatter/feed-items/%s/likes";
    private static final String CHATTER_URL_LIKE = "%s/services/data/v22.0/chatter/likes/%s";
    private static final String CHATTER_URL_COMMENTS = "%s/services/data/v22.0/chatter/feed-items/%s/comments";
    private static final String CHATTER_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String INSTANCE_URL = "instance_url";

    private String mChatterInstance = null;
    private String mChatterToken = null;

    public Chatter(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public String getProfileUrl(@NonNull String esid) {
        if (getChatterInstance()) {
            return mChatterInstance + "/" + esid;
        }

        return null;
    }

    private boolean hasChatterInstance() {
        return !TextUtils.isEmpty(mChatterInstance) && !TextUtils.isEmpty(mChatterToken);
    }

    private boolean getChatterInstance() {
        if (!hasChatterInstance()) {
            String response = null;
            String url = String.format(CHATTER_URL_ACCESS, BuildConfig.CHATTER_KEY, mToken);
            OkHttpClient client = SonetHttpClient.getOkHttpClientInstance();
            Request request = new Request.Builder()
                    .url(url)
                    .post(null)
                    .build();
            try {
                response = client.newCall(request)
                        .execute()
                        .body()
                        .string();
            } catch (IOException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(mTag, "request error; url=" + url, e);
                }
            }

            if (!TextUtils.isEmpty(response)) {
                try {
                    JSONObject jobj = new JSONObject(response);

                    if (jobj.has(INSTANCE_URL) && jobj.has(Saccess_token)) {
                        mChatterInstance = jobj.getString(INSTANCE_URL);
                        mChatterToken = jobj.getString(Saccess_token);
                        return hasChatterInstance();
                    }
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
                }
            }
        }

        return false;
    }

    @Nullable
    @Override
    public Set<String> getNotificationStatusIds(long account, String[] notificationMessage) {
        return null;
    }

    @Nullable
    @Override
    public String getFeedResponse(int status_count) {
        if (getChatterInstance()) {
            Request request = new Request.Builder()
                    .url(String.format(CHATTER_URL_FEED, mChatterInstance))
                    .addHeader("Authorization", "OAuth " + mChatterToken)
                    .build();
            return SonetHttpClient.getResponse(request);
        }

        return null;
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        return new JSONObject(response).getJSONArray(Sitems);
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
        ArrayList<String[]> links = new ArrayList<>();
        JSONObject friendObj = item.getJSONObject(Suser);
        JSONObject photo = friendObj.getJSONObject(Sphoto);
        JSONObject comments = item.getJSONObject(Scomments);
        long date = parseDate(item.getString(ScreatedDate), CHATTER_DATE_FORMAT);

        addStatusItem(date,
                friendObj.getString(Sname),
                display_profile ? photo.getString(SsmallPhotoUrl) + "?oauth_token=" + mChatterToken : null,
                String.format(getString(R.string.messageWithCommentCount), item.getJSONObject(Sbody).getString(Stext), comments.getInt(Stotal)),
                time24hr,
                appWidgetId,
                account,
                item.getString(Sid),
                friendObj.getString(Sid),
                links
        );
    }

    @Nullable
    @Override
    public void getNotificationMessage(long account, String[] notificationMessage) {
        // NO-OP
    }

    @Override
    public void getNotifications(long account, String[] notificationMessage) {
        // NO-OP
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        if (getChatterInstance()) {
            Request request = new Request.Builder()
                    .url(String.format(CHATTER_URL_POST, mChatterInstance, Uri.encode(message)))
                    .addHeader("Authorization", "OAuth " + mChatterToken)
                    .post(null)
                    .build();
            return SonetHttpClient.request(request);
        }

        return false;
    }

    @Override
    public boolean isLikeable(String statusId) {
        return true;
    }

    @Override
    public boolean isLiked(String statusId, String accountId) {
        if (getChatterInstance()) {
            Request request = new Request.Builder()
                    .url(String.format(CHATTER_URL_LIKES, mChatterInstance, statusId))
                    .addHeader("Authorization", "OAuth " + mChatterToken)
                    .build();
            String response = SonetHttpClient.getResponse(request);

            if (!TextUtils.isEmpty(response)) {
                try {
                    JSONObject jobj = new JSONObject(response);

                    if (jobj.getInt(Stotal) > 0) {
                        JSONArray likes = jobj.getJSONArray("likes");

                        for (int i = 0, i2 = likes.length(); i < i2; i++) {
                            JSONObject like = likes.getJSONObject(i);

                            if (like.getJSONObject(Suser).getString(Sid).equals(accountId)) {
                                // TODO need like id to unlike = like.getString(Sid);
                                return true;
                            }
                        }
                    }
                } catch (JSONException e) {
                    if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
                }
            }
        }

        return false;
    }

    @Override
    public boolean likeStatus(String statusId, String accountId, boolean doLike) {
        Request.Builder builder = new Request.Builder();

        if (doLike) {
            builder.url(String.format(CHATTER_URL_LIKES, mChatterInstance, statusId));
            builder.post(null);
        } else {
            builder.url(String.format(CHATTER_URL_LIKE, mChatterInstance, "" /* TODO replace this string with the like id from isLiked */));
            builder.delete();
        }

        builder.addHeader("Authorization", "OAuth " + mChatterToken);
        return SonetHttpClient.request(builder.build());
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
        if (getChatterInstance()) {
            Request request = new Request.Builder()
                    .url(String.format(CHATTER_URL_COMMENTS, mChatterInstance, statusId))
                    .addHeader("Authorization", "OAuth " + mChatterToken)
                    .build();
            return SonetHttpClient.getResponse(request);
        }

        return null;
    }

    @Nullable
    @Override
    public JSONArray parseComments(@NonNull String response) throws JSONException {
        JSONObject chats = new JSONObject(response);

        if (chats.getInt(Stotal) > 0) {
            return chats.getJSONArray(Scomments);
        }

        return null;
    }

    @Nullable
    @Override
    public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr) throws JSONException {
        HashMap<String, String> commentMap = new HashMap<String, String>();
        commentMap.put(Statuses.SID, jsonComment.getString(Sid));
        commentMap.put(Entities.FRIEND, jsonComment.getJSONObject(Suser).getString(Sname));
        commentMap.put(Statuses.MESSAGE, jsonComment.getJSONObject(Sbody).getString(Stext));
        commentMap.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(parseDate(jsonComment.getString(ScreatedDate), CHATTER_DATE_FORMAT), time24hr));
        // TODO does this have the like id for unliking?
        commentMap.put(getString(R.string.like), "");
        return commentMap;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
        return null;
    }

    @Override
    public boolean sendComment(@NonNull String statusId, @NonNull String message) {
        if (getChatterInstance()) {
            Request request = new Request.Builder()
                    .url(String.format(CHATTER_URL_COMMENT, mChatterInstance, statusId, Uri.encode(message)))
                    .post(null)
                    .addHeader("Authorization", "OAuth " + mChatterToken)
                    .build();
            return SonetHttpClient.request(request);
        }

        return false;
    }

    @Override
    public List<HashMap<String, String>> getFriends() {
        return null;
    }

    @Override
    String getApiKey() {
        return BuildConfig.CHATTER_KEY;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.CHATTER_SECRET;
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return Uri.parse("sonet://chatter");
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
        Uri uri = Uri.parse(authenticatedUrl);
        String token = uri.getQueryParameter(Saccess_token);
        String refresh_token = uri.getQueryParameter("refresh_token");
        String instance_url = uri.getQueryParameter("instance_url");

        Request request = new Request.Builder()
                .url(String.format(CHATTER_URL_ME, instance_url))
                .addHeader("Authorization", "OAuth " + token)
                .build();

        String httpRespnose = SonetHttpClient.getResponse(request);

        if (!TextUtils.isEmpty(httpRespnose)) {
            try {
                JSONObject jobj = new JSONObject(httpRespnose);
                if (jobj.has(Sname) && jobj.has(Sid)) {
                    MemberAuthentication memberAuthentication = new MemberAuthentication();
                    memberAuthentication.username = jobj.getString(Sname);
                    memberAuthentication.token = refresh_token;
                    memberAuthentication.secret = "";
                    memberAuthentication.expiry = 0;
                    memberAuthentication.network = mNetwork;
                    memberAuthentication.id = jobj.getString(Sid);
                    return memberAuthentication;
                }
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.d(mTag, "error parse chatter me: " + httpRespnose, e);
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getAuthUrl(@NonNull SonetOAuth sonetOAuth) {
        return String.format(CHATTER_URL_AUTHORIZE, BuildConfig.CHATTER_KEY, getCallback().toString());
    }
}
