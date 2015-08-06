package com.piusvelte.sonet.social;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Moments;
import com.google.android.gms.plus.People;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.moments.ItemScope;
import com.google.android.gms.plus.model.moments.Moment;
import com.google.android.gms.plus.model.moments.MomentBuffer;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.plus.model.people.PersonBuffer;
import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.provider.Notifications;
import com.squareup.okhttp.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static com.piusvelte.sonet.Sonet.Sactor;
import static com.piusvelte.sonet.Sonet.Scontent;
import static com.piusvelte.sonet.Sonet.SdisplayName;
import static com.piusvelte.sonet.Sonet.Sid;
import static com.piusvelte.sonet.Sonet.Simage;
import static com.piusvelte.sonet.Sonet.Sitems;
import static com.piusvelte.sonet.Sonet.Sobject;
import static com.piusvelte.sonet.Sonet.SoriginalContent;
import static com.piusvelte.sonet.Sonet.Spublished;
import static com.piusvelte.sonet.Sonet.Sreplies;
import static com.piusvelte.sonet.Sonet.StotalItems;
import static com.piusvelte.sonet.Sonet.Surl;

/**
 * Created by bemmanuel on 2/15/15.
 */
public class GooglePlus extends Client {

    private static final String GOOGLEPLUS_BASE_URL = "https://www.googleapis.com/plus/v1/";
    private static final String GOOGLEPLUS_ACTIVITIES = "%speople/%s/activities/%s?maxResults=%s&access_token=%s";
    private static final String GOOGLEPLUS_ACTIVITY = "%sactivities/%s?access_token=%s";
    private static final String GOOGLEPLUS_PROFILE = "https://plus.google.com/%s";
    private static final String GOOGLEPLUS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    private static GoogleApiClient sGoogleApiClient;

    /** ConnectionResult used for resolving connection failure */
    private ConnectionResult mConnectionResult;

    public GooglePlus(Context context, String token, String secret, String accountEsid, int network) {
        super(context, token, secret, accountEsid, network);
    }

    private boolean connectClient() {
        if (BuildConfig.DEBUG) {
            Log.d(mTag, "GoogleApiClient connect, is connected?" + (sGoogleApiClient != null && sGoogleApiClient.isConnected()));
        }

        if (sGoogleApiClient == null) {
            sGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Plus.API)
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    .addScope(Plus.SCOPE_PLUS_PROFILE)
                    .setAccountName(mAccountEsid)
                    .build();
            mConnectionResult = sGoogleApiClient.blockingConnect();

            if (mConnectionResult.isSuccess()) {
                if (BuildConfig.DEBUG) {
                    Log.d(mTag, "GoogleApiClient connected");
                }

                mConnectionResult = null;
                return true;
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(mTag, "GoogleApiClient connect failed; code=" + mConnectionResult.getErrorCode() + ", " + mConnectionResult.toString());
                }
            }

            sGoogleApiClient = null;
            return false;
        } else if (!sGoogleApiClient.isConnected()) {
            sGoogleApiClient = null;
            return connectClient();
        }

        return true;
    }

    @Override
    public boolean hasConnectionError() {
        return mConnectionResult != null;
    }

    @Override
    public boolean resolveConnectionError(@NonNull Activity activity, int requestCode) {
        if (hasConnectionError()) {
            try {
                mConnectionResult.startResolutionForResult(activity, requestCode);
                mConnectionResult = null;
                return true;
            } catch (IntentSender.SendIntentException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(mTag, "error resolving connection error", e);
                }

                mConnectionResult = null;
                return false;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public String getProfileUrl(@NonNull String esid) {
        return String.format(GOOGLEPLUS_PROFILE, esid);
    }

    @Nullable
    @Override
    public String getProfilePhotoUrl() {
        synchronized (GooglePlus.class) {
            if (connectClient()) {
                Person person = Plus.PeopleApi.getCurrentPerson(sGoogleApiClient);

                if (person.hasImage()) {
                    Person.Image image = person.getImage();

                    if (image.hasUrl()) {
                        return image.getUrl();
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getProfilePhotoUrl(String esid) {
        String url = null;

        synchronized (GooglePlus.class) {
            if (connectClient()) {
                People.LoadPeopleResult result = Plus.PeopleApi.load(sGoogleApiClient, esid).await();

                if (result != null && result.getStatus().isSuccess()) {
                    PersonBuffer buffer = result.getPersonBuffer();

                    if (buffer.getCount() > 0) {
                        Person person = buffer.get(0);

                        if (person.hasImage()) {
                            Person.Image image = person.getImage();

                            if (image.hasUrl()) {
                                url = image.getUrl();
                            }
                        }
                    }

                    buffer.close();
                }
            }
        }

        return url;
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

    @Nullable
    private String getAccessToken(@NonNull String refreshToken) {
        return null;
    }

    @Override
    public MemberAuthentication getMemberAuthentication(@NonNull String authenticatedUrl) {
        synchronized (GooglePlus.class) {
            if (connectClient()) {
                Person person = Plus.PeopleApi.getCurrentPerson(sGoogleApiClient);

                if (BuildConfig.DEBUG) {
                    Log.d(mTag, "getMemberAuthentication; person=" + person);
                }

                if (person != null) {
                    MemberAuthentication memberAuthentication = new MemberAuthentication();
                    memberAuthentication.username = person.getDisplayName();
                    // person.getId() is the "id", but the @gmail account is what's required to authenticate
                    memberAuthentication.id = mAccountEsid;
                    memberAuthentication.network = mNetwork;

                    return memberAuthentication;
                }
            }
        }

        return null;
    }

    @Nullable
    @Override
    public String getAuthUrl() {
        return null;
    }

    @Nullable
    @Override
    public Set<String> getNotificationStatusIds(long account, String[] notificationMessage) {
        Set<String> notificationSids = new HashSet<>();
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext),
                new String[] { Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID },
                Notifications.ACCOUNT + "=?", new String[] { Long.toString(account) }, null);

        // loop over notifications
        if (currentNotifications.moveToFirst()) {
            while (!currentNotifications.isAfterLast()) {
                long notificationId = currentNotifications.getLong(0);
                String sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                long updated = currentNotifications.getLong(2);
                boolean cleared = currentNotifications.getInt(3) == 1;

                // store sids, to avoid duplicates when requesting the latest feed
                if (!notificationSids.contains(sid)) {
                    notificationSids.add(sid);
                }

                // TODO
                // get comments for current notifications
//                String response = SonetHttpClient.httpResponse(String.format(GOOGLEPLUS_ACTIVITY, GOOGLEPLUS_BASE_URL, sid,
// access_token)));
//
//                if (!TextUtils.isEmpty(response)) {
//                    // check for a newer post, if it's the user's own, then set CLEARED=0
//                    try {
//                        JSONObject item = new JSONObject(response);
//
//                        if (item.has(Sobject)) {
//                            JSONObject object = item.getJSONObject(Sobject);
//
//                            if (object.has(Sreplies)) {
//                                int commentCount = 0;
//                                JSONObject replies = object.getJSONObject(Sreplies);
//
//                                if (replies.has(StotalItems)) {
//                                    commentCount = replies.getInt(StotalItems);
//                                }
//                            }
//                        }
//                    } catch (JSONException e) {
//                        // TODO
//                    }
//                }

                currentNotifications.moveToNext();
            }
        }

        currentNotifications.close();
        return notificationSids;
    }

    @Override
    public String getFeed(int appWidgetId,
            String widget,
            long account,
            int status_count,
            boolean time24hr,
            boolean display_profile,
            int notifications) {
        String[] notificationMessage = new String[1];
        Set<String> notificationSids = null;
        boolean doNotify = notifications != 0;

        if (doNotify) {
            notificationSids = getNotificationStatusIds(account, notificationMessage);
        }

        synchronized (GooglePlus.class) {
            if (connectClient()) {
                Moments.LoadMomentsResult result = Plus.MomentsApi.load(sGoogleApiClient).await();

                if (BuildConfig.DEBUG) {
                    Log.d(mTag, "feed result=" + result);
                }

                if (result != null && result.getStatus().isSuccess()) {
                    MomentBuffer buffer = result.getMomentBuffer();
                    int parseCount = Math.min(buffer.getCount(), status_count);

                    if (BuildConfig.DEBUG) {
                        Log.d(mTag, "feed count=" + parseCount);
                    }

                    if (parseCount > 0) {
                        removeOldStatuses(widget, Long.toString(account));

                        for (int i = 0; i < parseCount; i++) {
                            Moment moment = buffer.get(i);

                            if (BuildConfig.DEBUG) {
                                Log.d(mTag, "moment=" + moment.toString());
                            }

                            String statusId = moment.getId();
                            long date = parseDate(moment.getStartDate(), GOOGLEPLUS_DATE_FORMAT);

                            ItemScope creator = moment.getResult();
                            String entityId = creator.getId();
                            String name = creator.getGivenName() + " " + creator.getFamilyName();
                            String image = creator.getImage();
                            String content = creator.getDescription();

                            addStatusItem(date,
                                    name,
                                    image,
                                    String.format(getString(R.string.messageWithCommentCount), content, 0),
                                    time24hr,
                                    appWidgetId,
                                    account,
                                    statusId,
                                    entityId,
                                    new ArrayList<String[]>());
                        }
                    }

                    buffer.close();
                }
            }
        }

        if (doNotify) {
            getNotificationMessage(account, notificationMessage);
            return notificationMessage[0];
        }

        return null;
    }

    @Nullable
    @Override
    public String getFeedResponse(int status_count) {
        return null;
    }

    @Nullable
    @Override
    public JSONArray parseFeed(@NonNull String response) throws JSONException {
        return null;
    }

    @Override
    public void addFeedItem(@NonNull JSONObject item,
            boolean display_profile,
            boolean time24hr,
            int appWidgetId,
            long account,
            Set<String> notificationSids,
            String[] notificationMessage,
            boolean doNotify) throws JSONException {
        // NO-OP
    }

    @Override
    public void getNotificationMessage(long account, String[] notificationMessage) {
        // NO-OP
    }

    @Override
    public void getNotifications(long account, String[] notificationMessage) {
        // deprecated
        Cursor currentNotifications = getContentResolver().query(Notifications.getContentUri(mContext),
                new String[] { Notifications._ID, Notifications.SID, Notifications.UPDATED, Notifications.CLEARED, Notifications.ESID },
                Notifications.ACCOUNT + "=?", new String[] { Long.toString(account) }, null);

        if (currentNotifications.moveToFirst()) {
            Set<String> notificationSids = new HashSet<>();
            String accessToken = getAccessToken(mToken);

            try {
                if (!TextUtils.isEmpty(accessToken)) {
                    String response;

                    while (!currentNotifications.isAfterLast()) {
                        long notificationId = currentNotifications.getLong(0);
                        String sid = SonetCrypto.getInstance(mContext).Decrypt(currentNotifications.getString(1));
                        long updated = currentNotifications.getLong(2);
                        boolean cleared = currentNotifications.getInt(3) == 1;
                        // store sids, to avoid duplicates when requesting the latest feed
                        if (!notificationSids.contains(sid)) {
                            notificationSids.add(sid);
                        }
                        // get comments for current notifications
                        Request request = new Request.Builder()
                                .url(String.format(GOOGLEPLUS_ACTIVITY, GOOGLEPLUS_BASE_URL, sid, accessToken))
                                .build();
                        response = SonetHttpClient.getResponse(request);

                        if (!TextUtils.isEmpty(response)) {
                            // check for a newer post, if it's the user's own, then set CLEARED=0
                            try {
                                JSONObject item = new JSONObject(response);

                                if (item.has(Sobject)) {
                                    JSONObject object = item.getJSONObject(Sobject);

                                    if (object.has(Sreplies)) {
                                        int commentCount = 0;
                                        JSONObject replies = object.getJSONObject(Sreplies);

                                        if (replies.has(StotalItems)) {
                                            //TODO: notifications
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
                            }
                        }
                        currentNotifications.moveToNext();
                    }

                    Request request = new Request.Builder()
                            .url(String.format(GOOGLEPLUS_ACTIVITIES, GOOGLEPLUS_BASE_URL, "me", "public", 20, accessToken))
                            .build();
                    response = SonetHttpClient.getResponse(request);

                    // get new feed
                    if (!TextUtils.isEmpty(response)) {
                        JSONObject r = new JSONObject(response);

                        if (r.has(Sitems)) {
                            JSONArray items = r.getJSONArray(Sitems);

                            for (int i1 = 0, i2 = items.length(); i1 < i2; i1++) {
                                JSONObject item = items.getJSONObject(i1);

                                if (item.has(Sactor) && item.has(Sobject)) {
                                    JSONObject actor = item.getJSONObject(Sactor);
                                    JSONObject object = item.getJSONObject(Sobject);

                                    if (item.has(Sid) && actor.has(Sid) && actor.has(SdisplayName) && item.has(Spublished) && object
                                            .has(Sreplies) && object.has(SoriginalContent)) {
                                        String sid = item.getString(Sid);
                                        String esid = actor.getString(Sid);
                                        String friend = actor.getString(SdisplayName);
                                        String originalContent = object.getString(SoriginalContent);

                                        if ((originalContent == null) || (originalContent.length() == 0)) {
                                            originalContent = object.getString(Scontent);
                                        }

                                        String photo = null;

                                        if (actor.has(Simage)) {
                                            JSONObject image = actor.getJSONObject(Simage);
                                            if (image.has(Surl)) {
                                                photo = image.getString(Surl);
                                            }
                                        }

                                        long date = parseDate(item.getString(Spublished), GOOGLEPLUS_DATE_FORMAT);
                                        int commentCount = 0;
                                        JSONObject replies = object.getJSONObject(Sreplies);
                                        String notification = null;

                                        if (replies.has(StotalItems)) {
//                                                Log.d(TAG, Sreplies + ":" + replies.toString());
                                            commentCount = replies.getInt(StotalItems);
                                        }

                                        if (notification != null) {
                                            // new notification
                                            addNotification(sid, esid, friend, originalContent, date, account, notification);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e(mTag, e.toString());
            }
        }

        currentNotifications.close();
    }

    @Override
    public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags) {
        return false;
    }

    @Override
    public boolean isLikeable(String statusId) {
        return true;
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
        return "+1";
    }

    @Override
    public boolean isCommentable(String statusId) {
        return true;
    }

    @Override
    public String getCommentPretext(String accountId) {
        return null;
    }

    @Override
    public void onDelete() {
        synchronized (GooglePlus.class) {
            if (connectClient()) {
                Plus.AccountApi.revokeAccessAndDisconnect(sGoogleApiClient).await();
            }
        }
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
        return BuildConfig.GOOGLECLIENT_ID;
    }

    @Override
    String getApiSecret() {
        return BuildConfig.GOOGLECLIENT_SECRET;
    }
}
