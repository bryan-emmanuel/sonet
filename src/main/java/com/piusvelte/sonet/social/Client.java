package com.piusvelte.sonet.social;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.DrawableRes;
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
import com.piusvelte.sonet.provider.StatusImages;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.Statuses;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import static com.piusvelte.sonet.Sonet.Simgur;
import static com.piusvelte.sonet.Sonet.Slink;
import static com.piusvelte.sonet.Sonet.getBlob;
import static com.piusvelte.sonet.Sonet.getCropSize;
import static com.piusvelte.sonet.Sonet.insertStatusImageBg;
import static com.piusvelte.sonet.Sonet.sBFOptions;
import static com.piusvelte.sonet.Sonet.sRFC822;
import static com.piusvelte.sonet.Sonet.sTimeZone;

/**
 * Created by bemmanuel on 2/15/15.
 */
abstract public class Client {

    String mTag;

    Context mContext;
    String mToken;
    String mSecret;
    String mAccountEsid;
    int mNetwork;
    SonetOAuth mOAuth;

    private SimpleDateFormat mSimpleDateFormat = null;

    public Client(Context context, String token, String secret, String accountEsid, int network) {
        mTag = getClass().getSimpleName();
        mContext = context;
        mToken = token;
        mSecret = secret;
        mAccountEsid = accountEsid;
        mNetwork = network;
    }

    public enum Network {
        Twitter, Facebook, MySpace, Foursquare, LinkedIn, Sms, Rss, IdentiCa, GooglePlus, Pinterest, Chatter;

        public static Network get(int network) {
            return Network.values()[network];
        }

        public static int[] getIcons() {
            int[] icons = new int[values().length];
            int iconIndex = 0;

            for (Network network : values()) {
                icons[iconIndex] = network.getIcon();
                iconIndex++;
            }

            return icons;
        }

        public Client getClient(Context context, String token, String secret, String accountEntityId) {
            switch (this) {
                case Twitter:
                    return new Twitter(context, token, secret, accountEntityId, this.ordinal());

                case Facebook:
                    return new Facebook(context, token, secret, accountEntityId, this.ordinal());

                case MySpace:
                    return new MySpace(context, token, secret, accountEntityId, this.ordinal());

                case Foursquare:
                    return new Foursquare(context, token, secret, accountEntityId, this.ordinal());

                case LinkedIn:
                    return new LinkedIn(context, token, secret, accountEntityId, this.ordinal());

                case Sms:
                    throw new IllegalArgumentException("SMS is not a SocialClient");

                case Rss:
                    return new Rss(context, token, secret, accountEntityId, this.ordinal());

                case IdentiCa:
                    return new IdentiCa(context, token, secret, accountEntityId, this.ordinal());

                case GooglePlus:
                    return new GooglePlus(context, token, secret, accountEntityId, this.ordinal());

                case Pinterest:
                    return new Pinterest(context, token, secret, accountEntityId, this.ordinal());

                case Chatter:
                    return new Chatter(context, token, secret, accountEntityId, this.ordinal());

                default:
                    throw new IllegalArgumentException("Unsupported network: " + this);
            }
        }

        @DrawableRes
        public int getIcon() {
            switch (this) {
                case Twitter:
                    return R.drawable.twitter;

                case Facebook:
                    return R.drawable.facebook;

                case MySpace:
                    return R.drawable.myspace;

                case Foursquare:
                    return R.drawable.foursquare;

                case LinkedIn:
                    return R.drawable.linkedin;

                case Sms:
                    return R.drawable.sms;

                case Rss:
                    return R.drawable.rss;

                case IdentiCa:
                    return R.drawable.identica;

                case GooglePlus:
                    return R.drawable.googleplus;

                case Pinterest:
                    // TODO replace this
                    return R.drawable.buzz;

                case Chatter:
                    return R.drawable.salesforce;

                default:
                    throw new IllegalArgumentException("Unsupported network: " + this);
            }
        }
    }

    public static class Builder {

        private Context mContext;
        private Network mNetwork;
        private String mToken;
        private String mSecret;
        private String mAccountEsid;

        public Builder(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        public Builder setNetwork(Network network) {
            mNetwork = network;
            return this;
        }

        public Builder setNetwork(int network) {
            mNetwork = Network.get(network);
            return this;
        }

        public Builder setCredentials(String token, String secret) {
            mToken = token;
            mSecret = secret;
            return this;
        }

        public Builder setAccount(String accountEsid) {
            mAccountEsid = accountEsid;
            return this;
        }

        public Client build() {
            return mNetwork.getClient(mContext, mToken, mSecret, mAccountEsid);
        }
    }

    public String getFeed(int appWidgetId, String widget, long account, int status_count, boolean time24hr, boolean display_profile, int notifications, HttpClient httpClient) {
        String[] notificationMessage = new String[1];
        Set<String> notificationSids = null;
        boolean doNotify = notifications != 0;

        if (doNotify) {
            notificationSids = getNotificationStatusIds(account, notificationMessage);
        }

        String response = getFeedResponse(status_count);

        if (!TextUtils.isEmpty(response)) {
            JSONArray feedItems;
            int parseCount;

            try {
                feedItems = parseFeed(response);

                if (feedItems != null) {
                    parseCount = Math.min(feedItems.length(), status_count);

                    if (parseCount > 0) {
                        removeOldStatuses(widget, Long.toString(account));

                        for (int itemIdx = 0; itemIdx < parseCount; itemIdx++) {
                            JSONObject item = feedItems.getJSONObject(itemIdx);

                            if (item != null) {
                                addFeedItem(item, display_profile, time24hr, appWidgetId, account, httpClient, notificationSids, notificationMessage, doNotify);
                            }
                        }
                    } else if (this instanceof MySpace) {
                        // warn about myspace permissions
                        addStatusItem(0,
                                getString(R.string.myspace_permissions_title),
                                null,
                                getString(R.string.myspace_permissions_message),
                                time24hr,
                                appWidgetId,
                                account,
                                "",
                                "",
                                new ArrayList<String[]>(),
                                httpClient);
                    }
                }
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) {
                    Log.d(mTag, "error parsing feed response: " + response, e);
                }
            }
        } else if (this instanceof MySpace) {
            // warn about myspace permissions
            addStatusItem(0,
                    getString(R.string.myspace_permissions_title),
                    null,
                    getString(R.string.myspace_permissions_message),
                    time24hr,
                    appWidgetId,
                    account,
                    "",
                    "",
                    new ArrayList<String[]>(),
                    httpClient);
        }

        if (doNotify) {
            getNotificationMessage(account, notificationMessage);
            return notificationMessage[0];
        }

        return null;
    }

    @Nullable
    abstract public Set<String> getNotificationStatusIds(long account, String[] notificationMessage);

    @Nullable
    abstract public String getFeedResponse(int status_count);

    @Nullable
    abstract public JSONArray parseFeed(@NonNull String response) throws JSONException;

    @Nullable
    abstract public void addFeedItem(@NonNull JSONObject item, boolean display_profile, boolean time24hr, int appWidgetId, long account, HttpClient httpClient, Set<String> notificationSids, String[] notificationMessage, boolean doNotify) throws JSONException;

    @Nullable
    abstract public void getNotificationMessage(long account, String[] notificationMessage);

    abstract public void getNotifications(long account, String[] notificationMessage);

    abstract public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags);

    abstract public boolean isLikeable(String statusId);

    abstract public boolean isLiked(String statusId, String accountId);

    abstract public boolean likeStatus(String statusId, String accountId, boolean doLike);

    abstract public String getLikeText(boolean isLiked);

    abstract public boolean isCommentable(String statusId);

    abstract public String getCommentPretext(String accountId);

    public List<HashMap<String, String>> getComments(@NonNull String statusId, boolean time24hr) {
        List<HashMap<String, String>> parsedComments = new ArrayList<>();

        String response = getCommentsResponse(statusId);

        if (!TextUtils.isEmpty(response)) {
            JSONArray jsonComments = null;

            try {
                jsonComments = parseComments(response);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.d(mTag, "exception parsing: " + response, e);
            }

            if (jsonComments != null) {
                for (int commentsIdx = 0, commentsLength = jsonComments.length(); commentsIdx < commentsLength; commentsIdx++) {
                    JSONObject comment = null;

                    try {
                        comment = jsonComments.getJSONObject(commentsIdx);
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) Log.d(mTag, "exception getting comment", e);
                    }

                    if (comment != null) {
                        HashMap<String, String> parsedComment = null;

                        try {
                            parsedComment = parseComment(statusId, comment, time24hr);
                        } catch (JSONException e) {
                            if (BuildConfig.DEBUG) Log.d(mTag, "exception parsing comment", e);
                        }

                        if (parsedComment != null) {
                            parsedComments.add(parsedComment);
                        }
                    }
                }
            }
        }

        return parsedComments;
    }

    @Nullable
    abstract public String getCommentsResponse(String statusId);

    @Nullable
    abstract public JSONArray parseComments(@NonNull String response) throws JSONException;

    @Nullable
    abstract public HashMap<String, String> parseComment(@NonNull String statusId, @NonNull JSONObject jsonComment, boolean time24hr) throws JSONException;

    abstract public LinkedHashMap<String, String> getLocations(String latitude, String longitude);

    abstract public boolean sendComment(@NonNull String statusId, @NonNull String message);

    abstract String getApiKey();

    abstract String getApiSecret();

    @Nullable
    abstract public Uri getCallback();

    abstract String getRequestUrl();

    abstract String getAccessUrl();

    abstract String getAuthorizeUrl();

    abstract public String getCallbackUrl();

    abstract boolean isOAuth10a();

    abstract public MemberAuthentication getMemberAuthentication(@NonNull SonetOAuth sonetOAuth, @NonNull String authenticatedUrl);

    @NonNull
    public SonetOAuth getLoginOAuth() {
        return new SonetOAuth(getApiKey(), getApiSecret());
    }

    @Nullable
    public String getAuthUrl(@NonNull SonetOAuth sonetOAuth) {
        try {
            return sonetOAuth.getAuthUrl(getRequestUrl(), getAccessUrl(), getAuthorizeUrl(), getCallbackUrl(), isOAuth10a(), null);
        } catch (OAuthMessageSignerException e) {
            if (BuildConfig.DEBUG) Log.d(mTag, e.toString());
        } catch (OAuthNotAuthorizedException e) {
            if (BuildConfig.DEBUG) Log.d(mTag, e.toString());
        } catch (OAuthExpectationFailedException e) {
            if (BuildConfig.DEBUG) Log.d(mTag, e.toString());
        } catch (OAuthCommunicationException e) {
            if (BuildConfig.DEBUG) Log.d(mTag, e.toString());
        }

        return null;
    }

    SonetOAuth getOAuth() {
        if (mOAuth == null) {
            mOAuth = new SonetOAuth(getApiKey(), getApiSecret(), mToken, mSecret);
        }

        return mOAuth;
    }

    String getString(int resId) {
        return mContext.getString(resId);
    }

    Resources getResources() {
        return mContext.getResources();
    }

    ContentResolver getContentResolver() {
        return mContext.getContentResolver();
    }

    String getFirstPhotoUrl(String[] parts) {
        if (parts.length > 1) {
            Uri uri = Uri.parse(parts[1]);

            if (uri.getHost().equals(Simgur)) {
                return parts[1];
            }
        }

        return null;
    }

    String getPostFriendOverride(String friend) {
        return null;
    }

    String getPostFriend(String friend) {
        return null;
    }

    void formatLink(Matcher matcher, StringBuffer stringBuffer, String link) {
        matcher.appendReplacement(stringBuffer, "(" + Slink + ": " + Uri.parse(link).getHost() + ")");
    }

    void addStatusItem(long created, String friend, String url, String message, boolean time24hr, int appWidgetId, long accountId, String sid, String esid, HttpClient httpClient) {
        addStatusItem(created, friend, url, message, time24hr, appWidgetId, accountId, sid, esid, new ArrayList<String[]>(), httpClient);
    }

    void addStatusItem(long created, String friend, String url, String message, boolean time24hr, int appWidgetId, long accountId, String sid, String esid, ArrayList<String[]> links, HttpClient httpClient) {
        long id;
        byte[] profile = null;

        if (url != null) {
            // get profile
            profile = SonetHttpClient.httpBlobResponse(httpClient, new HttpGet(url));
        }

        if (profile == null) {
            profile = getBlob(getResources(), R.drawable.ic_contact_picture);
        }

        String friend_override = getPostFriendOverride(friend);
        friend = getPostFriend(friend);

        Cursor entity = getContentResolver().query(Entities.getContentUri(mContext), new String[]{Entities._ID}, Entities.ACCOUNT + "=? and " + Entities.ESID + "=?", new String[]{Long.toString(accountId), SonetCrypto.getInstance(mContext).Encrypt(esid)}, null);

        if (entity.moveToFirst()) {
            id = entity.getLong(0);
        } else {
            ContentValues entityValues = new ContentValues();
            entityValues.put(Entities.ESID, esid);
            entityValues.put(Entities.FRIEND, friend);
            entityValues.put(Entities.PROFILE, profile);
            entityValues.put(Entities.ACCOUNT, accountId);
            id = Long.parseLong(getContentResolver().insert(Entities.getContentUri(mContext), entityValues).getLastPathSegment());
        }

        entity.close();
        // facebook sid comes in as esid_sid, the esid_ may need to be removed
        //		if (serviceId == FACEBOOK) {
        //			int split = sid.indexOf("_");
        //			if ((split > 0) && (split < sid.length())) {
        //				sid = sid.substring(sid.indexOf("_") + 1);
        //			}
        //		}
        // update the account statuses

        // parse any links
        Matcher m = Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(message);
        StringBuffer sb = new StringBuffer(message.length());

        while (m.find()) {
            String link = m.group();
            // check existing links before adding
            boolean exists = false;

            for (String[] l : links) {
                if (l[1].equals(link)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                links.add(new String[]{Slink, link});
                formatLink(m, sb, link);
            }
        }

        m.appendTail(sb);
        message = sb.toString();
        ContentValues values = new ContentValues();
        values.put(Statuses.CREATED, created);
        values.put(Statuses.ENTITY, id);
        values.put(Statuses.MESSAGE, message);
        values.put(Statuses.SERVICE, mNetwork);
        values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
        values.put(Statuses.WIDGET, appWidgetId);
        values.put(Statuses.ACCOUNT, accountId);
        values.put(Statuses.SID, sid);
        values.put(Statuses.FRIEND_OVERRIDE, friend_override);
        long statusId = Long.parseLong(getContentResolver().insert(Statuses.getContentUri(mContext), values).getLastPathSegment());
        String imageUrl = null;

        for (String[] s : links) {
            // get the first photo
            if (imageUrl == null) {
                imageUrl = getFirstPhotoUrl(s);
            }

            ContentValues linkValues = new ContentValues();
            linkValues.put(StatusLinks.STATUS_ID, statusId);
            linkValues.put(StatusLinks.LINK_TYPE, s[0]);
            linkValues.put(StatusLinks.LINK_URI, s[1]);
            getContentResolver().insert(StatusLinks.getContentUri(mContext), linkValues);
        }

        boolean insertEmptyImage = true;

        if (imageUrl != null) {
            byte[] image = null;

            if (url != null) {
                image = SonetHttpClient.httpBlobResponse(httpClient, new HttpGet(imageUrl));
            }

            if (image != null) {
                Bitmap imageBmp = BitmapFactory.decodeByteArray(image, 0, image.length, sBFOptions);

                if (imageBmp != null) {
                    Bitmap scaledImageBmp = null;
                    Bitmap croppedBmp = null;
                    int width = imageBmp.getWidth();
                    int height = imageBmp.getHeight();
                    // default to landscape
                    int scaledWidth;
                    int scaledHeight;
                    double targetHeightRatio;
                    double targetWidthRatio;

                    if (width > height) {
                        //landscape
                        scaledWidth = 192;
                        scaledHeight = 144;
                        targetHeightRatio = 0.75;
                        targetWidthRatio = 4.0 / 3;
                    } else {
                        //portrait
                        scaledWidth = 144;
                        scaledHeight = 192;
                        targetHeightRatio = 4.0 / 3;
                        targetWidthRatio = 0.75;
                    }

                    int targetSize = (int) Math.round(width * targetHeightRatio);

                    if (height > targetSize) {
                        // center crop the height
                        targetSize = getCropSize(height, targetSize);
                        croppedBmp = Bitmap.createBitmap(imageBmp, 0, targetSize, width, height - targetSize);
                    } else {
                        targetSize = (int) Math.round(height * targetWidthRatio);

                        if (width > targetSize) {
                            // center crop the width
                            targetSize = getCropSize(width, targetSize);
                            croppedBmp = Bitmap.createBitmap(imageBmp, targetSize, 0, width - targetSize, height);
                        }
                    }

                    if (croppedBmp != null) {
                        scaledImageBmp = Bitmap.createScaledBitmap(croppedBmp, scaledWidth, scaledHeight, true);
                        croppedBmp.recycle();
                        croppedBmp = null;
                    } else {
                        scaledImageBmp = Bitmap.createScaledBitmap(imageBmp, scaledWidth, scaledHeight, true);
                    }

                    imageBmp.recycle();
                    imageBmp = null;

                    if (scaledImageBmp != null) {
                        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
                        scaledImageBmp.compress(Bitmap.CompressFormat.PNG, 100, imageStream);
                        image = imageStream.toByteArray();
                        scaledImageBmp.recycle();
                        scaledImageBmp = null;

                        if (image != null) {
                            insertEmptyImage = !insertStatusImageBg(mContext, statusId, image, scaledHeight);
                        }
                    }
                }
            }
        }

        // remote views can be reused, avoid images being repeated across multiple statuses
        if (insertEmptyImage) {
            insertStatusImageBg(mContext, statusId, null, 1);
        }
    }

    void removeOldStatuses(String widgetId, String accountId) {
        Cursor statuses = getContentResolver().query(Statuses.getContentUri(mContext), new String[]{Statuses._ID}, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widgetId, accountId}, null);

        if (statuses.moveToFirst()) {
            while (!statuses.isAfterLast()) {
                String id = Long.toString(statuses.getLong(0));
                getContentResolver().delete(StatusLinks.getContentUri(mContext), StatusLinks.STATUS_ID + "=?", new String[]{id});
                getContentResolver().delete(StatusImages.getContentUri(mContext), StatusImages.STATUS_ID + "=?", new String[]{id});
                statuses.moveToNext();
            }
        }

        statuses.close();
        getContentResolver().delete(Statuses.getContentUri(mContext), Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{widgetId, accountId});
        Cursor entities = getContentResolver().query(Entities.getContentUri(mContext), new String[]{Entities._ID}, Entities.ACCOUNT + "=?", new String[]{accountId}, null);

        if (entities.moveToFirst()) {
            while (!entities.isAfterLast()) {
                Cursor s = getContentResolver().query(Statuses.getContentUri(mContext), new String[]{Statuses._ID}, Statuses.ACCOUNT + "=? and " + Statuses.WIDGET + " !=?", new String[]{accountId, widgetId}, null);
                if (!s.moveToFirst()) {
                    // not in use, remove it
                    getContentResolver().delete(Entities.getContentUri(mContext), Entities._ID + "=?", new String[]{Long.toString(entities.getLong(0))});
                }
                s.close();
                entities.moveToNext();
            }
        }

        entities.close();
    }

    void addNotification(String sid, String esid, String friend, String message, long created, long accountId, String notification) {
        ContentValues values = new ContentValues();
        values.put(Notifications.SID, sid);
        values.put(Notifications.ESID, esid);
        values.put(Notifications.FRIEND, friend);
        values.put(Notifications.MESSAGE, message);
        values.put(Notifications.CREATED, created);
        values.put(Notifications.ACCOUNT, accountId);
        values.put(Notifications.NOTIFICATION, notification);
        values.put(Notifications.CLEARED, 0);
        values.put(Notifications.UPDATED, created);
        getContentResolver().insert(Notifications.getContentUri(mContext), values);
    }

    String updateNotification(long notificationId, long created_time, String accountEsid, String esid, String name, boolean cleared) {
        String message = null;
        // new comment
        ContentValues values = new ContentValues();
        values.put(Notifications.UPDATED, created_time);

        if (accountEsid.equals(esid)) {
            // user's own comment, clear the notification
            values.put(Notifications.CLEARED, 1);
        } else if (cleared) {
            values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), name));
            values.put(Notifications.CLEARED, 0);
            message = String.format(getString(R.string.friendcommented), name);
        } else {
            values.put(Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), name + " and others"));
            message = String.format(getString(R.string.friendcommented), name + " and others");
        }

        getContentResolver().update(Notifications.getContentUri(mContext), values, Notifications._ID + "=?", new String[]{Long.toString(notificationId)});
        return message;
    }

    void updateNotificationMessage(String[] originalMessage, String newMessage) {
        if (TextUtils.isEmpty(originalMessage[0])) {
            originalMessage[0] = newMessage;
        } else if (!TextUtils.isEmpty(newMessage)) {
            originalMessage[0] = mContext.getString(R.string.notify_multiple_updates);
        }
    }

    long parseDate(String date, String format) {
        if (date != null) {
            // hack for the literal 'Z'
            if (date.substring(date.length() - 1).equals("Z")) {
                date = date.substring(0, date.length() - 2) + "+0000";
            }

            Date created = null;

            if (format != null) {
                if (mSimpleDateFormat == null) {
                    mSimpleDateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
                    // all dates should be GMT/UTC
                    mSimpleDateFormat.setTimeZone(sTimeZone);
                }

                try {
                    created = mSimpleDateFormat.parse(date);
                    return created.getTime();
                } catch (ParseException e) {
                    Log.e(mTag, e.toString());
                }
            } else {
                // attempt to parse RSS date
                if (mSimpleDateFormat != null) {
                    try {
                        created = mSimpleDateFormat.parse(date);
                        return created.getTime();
                    } catch (ParseException e) {
                        Log.e(mTag, e.toString());
                    }
                }

                for (String rfc822 : sRFC822) {
                    mSimpleDateFormat = new SimpleDateFormat(rfc822, Locale.ENGLISH);
                    mSimpleDateFormat.setTimeZone(sTimeZone);

                    try {
                        if ((created = mSimpleDateFormat.parse(date)) != null) {
                            return created.getTime();
                        }
                    } catch (ParseException e) {
                        Log.e(mTag, e.toString());
                    }
                }
            }
        }

        return System.currentTimeMillis();
    }

    public static class MemberAuthentication {

        public String username;
        public String token;
        public String secret;
        public int expiry;
        public int network;
        public String id;

    }
}
