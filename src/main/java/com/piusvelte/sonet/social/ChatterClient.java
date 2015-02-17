package com.piusvelte.sonet.social;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.SonetHttpClient;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.piusvelte.sonet.Sonet.CHATTER_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_FEED;
import static com.piusvelte.sonet.Sonet.CHATTER_URL_POST;
import static com.piusvelte.sonet.Sonet.Saccess_token;
import static com.piusvelte.sonet.Sonet.Sbody;
import static com.piusvelte.sonet.Sonet.Scomments;
import static com.piusvelte.sonet.Sonet.ScreatedDate;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Sinstance_url;
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

    public ChatterClient(Context context, String token, String secret, String accountEsid) {
        super(context, token, secret, accountEsid);
    }

    @Override
    public String getFeed(int appWidgetId, String widget, long account, int service, int status_count, boolean time24hr, boolean display_profile, int notifications, HttpClient httpClient) {
        String response;
        JSONArray statusesArray;
        ArrayList<String[]> links = new ArrayList<String[]>();
        HttpGet httpGet;
        JSONObject statusObj;
        JSONObject friendObj;
        // need to get an updated access_token
        String accessResponse = SonetHttpClient.httpResponse(httpClient, new HttpPost(String.format(CHATTER_URL_ACCESS, BuildConfig.CHATTER_KEY, mToken)));
        
        if (accessResponse != null) {
            try {
                JSONObject jobj = new JSONObject(accessResponse);
                
                if (jobj.has(Sinstance_url) && jobj.has(Saccess_token)) {
                    httpGet = new HttpGet(String.format(CHATTER_URL_FEED, jobj.getString(Sinstance_url)));
                    String chatterToken = jobj.getString(Saccess_token);
                    httpGet.setHeader("Authorization", "OAuth " + chatterToken);
                    
                    if ((response = SonetHttpClient.httpResponse(httpClient, httpGet)) != null) {
                        try {
                            statusesArray = new JSONObject(response).getJSONArray(Sitems);
                            // if there are updates, clear the cache
                            int e2 = statusesArray.length();
                            
                            if (e2 > 0) {
                                removeOldStatuses(widget, Long.toString(account));
                                
                                for (int e = 0; (e < e2) && (e < status_count); e++) {
                                    links.clear();
                                    statusObj = statusesArray.getJSONObject(e);
                                    friendObj = statusObj.getJSONObject(Suser);
                                    JSONObject photo = friendObj.getJSONObject(Sphoto);
                                    JSONObject comments = statusObj.getJSONObject(Scomments);
                                    long date = parseDate(statusObj.getString(ScreatedDate), CHATTER_DATE_FORMAT);
                                    
                                    if (e < status_count) {
                                        addStatusItem(date,
                                                friendObj.getString(Sname),
                                                display_profile ? photo.getString(SsmallPhotoUrl) + "?oauth_token=" + chatterToken : null,
                                                String.format(getString(R.string.messageWithCommentCount), statusObj.getJSONObject(Sbody).getString(Stext), comments.getInt(Stotal)),
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
                            Log.e(mTag, response);
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(mTag, service + ":" + e.toString());
                Log.e(mTag, accessResponse);
            }
        }

        return null;
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        // need to get an updated access_token
        String response = SonetHttpClient.httpResponse(mContext, new HttpPost(String.format(CHATTER_URL_ACCESS, BuildConfig.CHATTER_KEY, mToken)));

        if (response != null) {
            try {
                JSONObject jobj = new JSONObject(response);

                if (jobj.has("instance_url") && jobj.has(Saccess_token)) {
                    HttpPost httpPost = new HttpPost(String.format(CHATTER_URL_POST, jobj.getString("instance_url"), Uri.encode(message)));
                    httpPost.setHeader("Authorization", "OAuth " + jobj.getString(Saccess_token));
                    return SonetHttpClient.httpResponse(mContext, httpPost) != null;
                }
            } catch (JSONException e) {
                Log.e(mTag, "Chatter:" + e.toString());
                Log.e(mTag, response);
            }
        }

        return false;
    }

    @Override
    public LinkedHashMap<String, String> getLocations(String latitude, String longitude) {
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
}
