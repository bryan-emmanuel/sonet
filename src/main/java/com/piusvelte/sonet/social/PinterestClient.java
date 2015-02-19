package com.piusvelte.sonet.social;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.SonetHttpClient;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.piusvelte.sonet.Sonet.PINTEREST_BASE_URL;
import static com.piusvelte.sonet.Sonet.PINTEREST_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.PINTEREST_URL_FEED;
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
public class PinterestClient extends SocialClient {

    public PinterestClient(Context context, String token, String secret, String accountEsid) {
        super(context, token, secret, accountEsid);
    }

    @Override
    String getFirstPhotoUrl(String[] parts) {
        if (parts.length > 0 && parts[0].equals(Simage)) {
            return parts[1];
        }

        return super.getFirstPhotoUrl(parts);
    }

    @Override
    public String getFeed(int appWidgetId, String widget, long account, int service, int status_count, boolean time24hr, boolean display_profile, int notifications, HttpClient httpClient) {
        String response;
        JSONArray statusesArray;
        ArrayList<String[]> links = new ArrayList<String[]>();
        JSONObject statusObj;
        JSONObject friendObj;

        // parse the response
        if ((response = SonetHttpClient.httpResponse(httpClient, new HttpGet(String.format(PINTEREST_URL_FEED, PINTEREST_BASE_URL)))) != null) {
            // if not a full_refresh, only update the status_bg and icons
            try {
                JSONObject pins = new JSONObject(response);
                if (pins.has("pins")) {
                    statusesArray = pins.getJSONArray("pins");
                    // if there are updates, clear the cache
                    int e2 = statusesArray.length();
                    if (e2 > 0) {
                        removeOldStatuses(widget, Long.toString(account));

                        for (int e = 0; e < e2; e++) {
                            links.clear();
                            statusObj = statusesArray.getJSONObject(e);
                            friendObj = statusObj.getJSONObject(Suser);
                            long date = parseDate(statusObj.getString(Screated_at), PINTEREST_DATE_FORMAT);
                            int commentCount = 0;

                            if (statusObj.has(Scounts)) {
                                JSONObject counts = statusObj.getJSONObject(Scounts);

                                if (counts.has(Scomments)) {
                                    commentCount = counts.getInt(Scomments);
                                }
                            }

                            if (statusObj.has(Simages)) {
                                JSONObject images = statusObj.getJSONObject(Simages);

                                if (images.has(Smobile)) {
                                    links.add(new String[]{Simage, images.getString(Smobile)});
                                } else if (images.has(Sboard)) {
                                    links.add(new String[]{Simage, images.getString(Sboard)});
                                }
                            }
                            addStatusItem(date,
                                    friendObj.getString(Susername),
                                    display_profile ? friendObj.getString(Simage_url) : null,
                                    String.format(getString(R.string.messageWithCommentCount), statusObj.getString(Sdescription), commentCount),
                                    service,
                                    time24hr,
                                    appWidgetId,
                                    account,
                                    statusObj.getString(Sid),
                                    friendObj.getString(Sid),
                                    links,
                                    httpClient);
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(mTag, service + ":" + e.toString());
            }
        }

        return null;
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
    String getApiKey() {
        return null;
    }

    @Override
    String getApiSecret() {
        return null;
    }
}
