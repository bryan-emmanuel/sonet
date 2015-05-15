package com.piusvelte.sonet.social;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static com.piusvelte.sonet.Sonet.Sboard;
import static com.piusvelte.sonet.Sonet.Scomments;
import static com.piusvelte.sonet.Sonet.Scounts;
import static com.piusvelte.sonet.Sonet.Screated_at;
import static com.piusvelte.sonet.Sonet.Sdescription;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Simage;
import static com.piusvelte.sonet.Sonet.Simage_url;
import static com.piusvelte.sonet.Sonet.Simages;
import static com.piusvelte.sonet.Sonet.Smobile;
import static com.piusvelte.sonet.Sonet.Suser;
import static com.piusvelte.sonet.Sonet.Susername;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class Pinterest extends Client {

    private static final String PINTEREST_BASE_URL = "https://api.pinterest.com/v2/";
    private static final String PINTEREST_URL_FEED = "%spopular/";
    public static final String PINTEREST_PIN = "https://pinterest.com/pin/%s/";
    private static final String PINTEREST_PROFILE = "https://pinterest.com/%s/";
    private static final String PINTEREST_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public Pinterest(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    @Nullable
    @Override
    public String getProfileUrl(@NonNull String esid) {
        if (esid != null) {
            return String.format(PINTEREST_PROFILE, esid);
        } else {
            return "https://pinterest.com";
        }
    }

    @Nullable
    @Override
    public Uri getCallback() {
        return null;
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
        return null;
    }

    @Override
    String getFirstPhotoUrl(String[] parts) {
        if (parts.length > 0 && parts[0].equals(Simage)) {
            return parts[1];
        }

        return super.getFirstPhotoUrl(parts);
    }

    @Nullable
    @Override
    public Set<String> getNotificationStatusIds(long account, String[] notificationMessage) {
        return null;
    }

    @Nullable
    @Override
    public String getFeedResponse(int status_count) {
        return SonetHttpClient.httpResponse(String.format(PINTEREST_URL_FEED, PINTEREST_BASE_URL));
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        JSONObject pins = new JSONObject(response);
        if (pins.has("pins")) {
            return pins.getJSONArray("pins");
        }

        return null;
    }

    @Nullable
    @Override
    public void addFeedItem(@NonNull JSONObject item,
            boolean display_profile,
            boolean time24hr,
            int appWidgetId,
            long account,
            HttpClient httpClient,
            Set<String> notificationSids,
            String[] notificationMessage,
            boolean doNotify) throws JSONException {
        ArrayList<String[]> links = new ArrayList<>();
        JSONObject friendObj = item.getJSONObject(Suser);
        long date = parseDate(item.getString(Screated_at), PINTEREST_DATE_FORMAT);
        int commentCount = 0;

        if (item.has(Scounts)) {
            JSONObject counts = item.getJSONObject(Scounts);

            if (counts.has(Scomments)) {
                commentCount = counts.getInt(Scomments);
            }
        }

        if (item.has(Simages)) {
            JSONObject images = item.getJSONObject(Simages);

            if (images.has(Smobile)) {
                links.add(new String[] { Simage, images.getString(Smobile) });
            } else if (images.has(Sboard)) {
                links.add(new String[] { Simage, images.getString(Sboard) });
            }
        }

        addStatusItem(date,
                friendObj.getString(Susername),
                display_profile ? friendObj.getString(Simage_url) : null,
                String.format(getString(R.string.messageWithCommentCount), item.getString(Sdescription), commentCount),
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
        return false;
    }

    @Override
    public boolean isLikeable(String statusId) {
        return false;
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
        return null;
    }

    @Override
    public boolean isCommentable(String statusId) {
        return false;
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
    public List<HashMap<String, String>> getFriends() {
        return null;
    }

    @Override
    String getApiKey() {
        return null;
    }

    @Override
    String getApiSecret() {
        return null;
    }
}
