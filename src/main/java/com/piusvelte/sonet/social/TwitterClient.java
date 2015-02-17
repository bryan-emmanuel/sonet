package com.piusvelte.sonet.social;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;

import static com.piusvelte.sonet.Sonet.Screated_at;
import static com.piusvelte.sonet.Sonet.Sfull_name;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Sname;
import static com.piusvelte.sonet.Sonet.Splaces;
import static com.piusvelte.sonet.Sonet.Sprofile_image_url;
import static com.piusvelte.sonet.Sonet.Sresult;
import static com.piusvelte.sonet.Sonet.Sstatus;
import static com.piusvelte.sonet.Sonet.Stext;
import static com.piusvelte.sonet.Sonet.Suser;
import static com.piusvelte.sonet.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.Sonet.TWITTER_DATE_FORMAT;
import static com.piusvelte.sonet.Sonet.TWITTER_MENTIONS;
import static com.piusvelte.sonet.Sonet.TWITTER_SEARCH;
import static com.piusvelte.sonet.Sonet.TWITTER_SINCE_ID;
import static com.piusvelte.sonet.Sonet.TWITTER_UPDATE;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_FEED;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class TwitterClient extends SocialClient {

    public TwitterClient(Context context, String token, String secret, String accountEsid) {
        super(context, token, secret, accountEsid);
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

    @Override
    void formatLink(Matcher matcher, StringBuffer stringBuffer, String link) {
        // NO-OP
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
        String friend;
        SonetOAuth sonetOAuth = getOAuth();

        // parse the response
        if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(getFeedUrl(), getBaseUrl(), status_count))))) != null) {
            // if not a full_refresh, only update the status_bg and icons
            try {
                statusesArray = new JSONArray(response);
                // if there are updates, clear the cache
                int e2 = statusesArray.length();

                if (e2 > 0) {
                    removeOldStatuses(widget, Long.toString(account));

                    for (int e = 0; (e < e2) && (e < status_count); e++) {
                        links.clear();
                        statusObj = statusesArray.getJSONObject(e);
                        friendObj = statusObj.getJSONObject(Suser);
                        addStatusItem(parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT),
                                friendObj.getString(Sname),
                                display_profile ? friendObj.getString(Sprofile_image_url) : null,
                                statusObj.getString(Stext),
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
            } catch (JSONException e) {
                Log.e(mTag, service + ":" + e.toString());
            }
        }

        // notifications
        if (notifications != 0) {
            currentNotifications = getContentResolver().query(Sonet.Notifications.getContentUri(mContext), new String[]{Sonet.Notifications.SID}, Sonet.Notifications.ACCOUNT + "=?", new String[]{Long.toString(account)}, null);

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
            Cursor last_status = getContentResolver().query(Sonet.Statuses.getContentUri(mContext), new String[]{Sonet.Statuses.SID}, Sonet.Statuses.ACCOUNT + "=?", new String[]{Long.toString(account)}, Sonet.Statuses.CREATED + " ASC LIMIT 1");

            if (last_status.moveToFirst()) {
                last_sid = SonetCrypto.getInstance(mContext).Decrypt(last_status.getString(0));
            }

            last_status.close();

            // get all mentions since the oldest status for this account
            if ((response = SonetHttpClient.httpResponse(httpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(getMentionsUrl(), getBaseUrl(), last_sid != null ? String.format(TWITTER_SINCE_ID, last_sid) : ""))))) != null) {
                try {
                    statusesArray = new JSONArray(response);

                    for (int i = 0, i2 = statusesArray.length(); i < i2; i++) {
                        statusObj = statusesArray.getJSONObject(i);
                        friendObj = statusObj.getJSONObject(Suser);

                        if (!friendObj.getString(Sid).equals(mAccountEsid) && !notificationSids.contains(statusObj.getString(Sid))) {
                            friend = friendObj.getString(Sname);
                            addNotification(statusObj.getString(Sid), friendObj.getString(Sid), friend, statusObj.getString(Stext), parseDate(statusObj.getString(Screated_at), TWITTER_DATE_FORMAT), account, friend + " mentioned you on Twitter");
                            notificationMessage = updateNotificationMessage(notificationMessage, friend + " mentioned you on Twitter");
                        }
                    }
                } catch (JSONException e) {
                    Log.e(mTag, service + ":" + e.toString());
                }
            }
        }

        return notificationMessage;
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        boolean result = false;
        HttpPost httpPost;

        // limit tweets to 140, breaking up the message if necessary
        while (message.length() > 0) {
            final String send;

            if (message.length() > 140) {
                // need to break on a word
                int end = 0;
                int nextSpace = 0;

                for (int i = 0, i2 = message.length(); i < i2; i++) {
                    end = nextSpace;

                    if (message.substring(i, i + 1).equals(" ")) {
                        nextSpace = i;
                    }
                }

                // in case there are no spaces, just break on 140
                if (end == 0) {
                    end = 140;
                }

                send = message.substring(0, end);
                message = message.substring(end + 1);
            } else {
                send = message;
                message = "";
            }

            httpPost = new HttpPost(String.format(getUpdateUrl(), getBaseUrl()));
            // resolve Error 417 Expectation by Twitter
            httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(Sstatus, send));

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
        }

        return result;
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
                Log.e(mTag, e.toString());
            }

            return locations;
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
