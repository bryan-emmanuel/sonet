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

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import static com.piusvelte.sonet.Sonet.CHATTER_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_COMMENT;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_COMMENTS;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_FEED;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_LIKE;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_LIKES;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_POST;
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
public class ChatterClient extends SocialClient {

    private static final String INSTANCE_URL = "instance_url";

    private String mChatterInstance = null;
    private String mChatterToken = null;

    public ChatterClient(Context context, String token, String secret, String accountEsid) {
        super(context, token, secret, accountEsid);
    }

    private boolean hasChatterInstance() {
        return !TextUtils.isEmpty(mChatterInstance) && !TextUtils.isEmpty(mChatterToken);
    }

    private boolean getChatterInstance() {
        if (!hasChatterInstance()) {
            String response = SonetHttpClient.httpResponse(mContext, new HttpPost(String.format(CHATTER_URL_ACCESS, BuildConfig.CHATTER_KEY, mToken)));

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
            HttpGet httpGet = new HttpGet(String.format(CHATTER_URL_FEED, mChatterInstance));
            httpGet.setHeader("Authorization", "OAuth " + mChatterToken);
            return SonetHttpClient.httpResponse(mContext, httpGet);
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
    public void addFeedItem(@NonNull JSONObject item, boolean display_profile, int service, boolean time24hr, int appWidgetId, long account, HttpClient httpClient, Set<String> notificationSids, String[] notificationMessage, boolean doNotify) throws JSONException {
        ArrayList<String[]> links = new ArrayList<>();
        JSONObject friendObj = item.getJSONObject(Suser);
        JSONObject photo = friendObj.getJSONObject(Sphoto);
        JSONObject comments = item.getJSONObject(Scomments);
        long date = parseDate(item.getString(ScreatedDate), CHATTER_DATE_FORMAT);

        addStatusItem(date,
                friendObj.getString(Sname),
                display_profile ? photo.getString(SsmallPhotoUrl) + "?oauth_token=" + mChatterToken : null,
                String.format(getString(R.string.messageWithCommentCount), item.getJSONObject(Sbody).getString(Stext), comments.getInt(Stotal)),
                service,
                time24hr,
                appWidgetId,
                account,
                item.getString(Sid),
                friendObj.getString(Sid),
                links,
                httpClient);
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
            HttpPost httpPost = new HttpPost(String.format(CHATTER_URL_POST, mChatterInstance, Uri.encode(message)));
            httpPost.setHeader("Authorization", "OAuth " + mChatterToken);
            return SonetHttpClient.httpResponse(mContext, httpPost) != null;
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
            HttpGet httpGet = new HttpGet(String.format(CHATTER_URL_LIKES, mChatterInstance, statusId));
            httpGet.setHeader("Authorization", "OAuth " + mChatterToken);
            String response = SonetHttpClient.httpResponse(mContext, httpGet);

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
        HttpUriRequest httpRequest;

        if (doLike) {
            httpRequest = new HttpPost(String.format(CHATTER_URL_LIKES, mChatterInstance, statusId));
        } else {
            httpRequest = new HttpDelete(String.format(CHATTER_URL_LIKE, mChatterInstance, "" /* TODO replace this string with the like id from isLiked */));
        }

        httpRequest.setHeader("Authorization", "OAuth " + mChatterToken);
        return SonetHttpClient.httpResponse(mContext, httpRequest) != null;
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
            HttpGet httpGet = new HttpGet(String.format(CHATTER_URL_COMMENTS, mChatterInstance, statusId));
            httpGet.setHeader("Authorization", "OAuth " + mChatterToken);
            return SonetHttpClient.httpResponse(mContext, httpGet);
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
        commentMap.put(Sonet.Statuses.SID, jsonComment.getString(Sid));
        commentMap.put(Sonet.Entities.FRIEND, jsonComment.getJSONObject(Suser).getString(Sname));
        commentMap.put(Sonet.Statuses.MESSAGE, jsonComment.getJSONObject(Sbody).getString(Stext));
        commentMap.put(Sonet.Statuses.CREATEDTEXT, Sonet.getCreatedText(parseDate(jsonComment.getString(ScreatedDate), CHATTER_DATE_FORMAT), time24hr));
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
            HttpPost httpPost = new HttpPost(String.format(CHATTER_URL_COMMENT, mChatterInstance, statusId, Uri.encode(message)));
            httpPost.setHeader("Authorization", "OAuth " + mChatterToken);
            return !TextUtils.isEmpty(SonetHttpClient.httpResponse(mContext, httpPost));
        }

        return false;
    }

    @Override
    String getApiKey() {
        return BuildConfig.CHATTER_KEY;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.CHATTER_SECRET;
    }
}
