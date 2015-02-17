package com.piusvelte.sonet.social;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetHttpClient;
import com.piusvelte.sonet.SonetOAuth;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.piusvelte.sonet.Sonet.Simgur;
import static com.piusvelte.sonet.Sonet.Slink;
import static com.piusvelte.sonet.Sonet.createBackground;
import static com.piusvelte.sonet.Sonet.getBlob;
import static com.piusvelte.sonet.Sonet.getCropSize;
import static com.piusvelte.sonet.Sonet.insertStatusImageBg;
import static com.piusvelte.sonet.Sonet.sBFOptions;
import static com.piusvelte.sonet.Sonet.sRFC822;
import static com.piusvelte.sonet.Sonet.sTimeZone;

/**
 * Created by bemmanuel on 2/15/15.
 */
abstract public class SocialClient {

    String mTag = SocialClient.class.getSimpleName();

    Context mContext;
    String mToken;
    String mSecret;
    String mAccountEsid;
    SonetOAuth mOAuth;

    private SimpleDateFormat mSimpleDateFormat = null;

    public SocialClient(Context context, String token, String secret, String accountEsid) {
        mContext = context;
        mToken = token;
        mSecret = secret;
        mAccountEsid = accountEsid;
    }

    public static class Builder {

        private Context mContext;
        private int mNetwork;
        private String mToken;
        private String mSecret;
        private String mAccountEsid;

        public Builder(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        public Builder setNetwork(int network) {
            mNetwork = network;
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

        public SocialClient build() {
            switch (mNetwork) {
                case Sonet.TWITTER:
                    return new TwitterClient(mContext, mToken, mSecret, mAccountEsid);

                case Sonet.FACEBOOK:
                    return new FacebookClient(mContext, mToken, mSecret, mAccountEsid);

                case Sonet.MYSPACE:
                    return new MySpaceClient(mContext, mToken, mSecret, mAccountEsid);

                case Sonet.FOURSQUARE:
                    return new FoursquareClient(mContext, mToken, mSecret, mAccountEsid);

                case Sonet.LINKEDIN:
                    return new LinkedInClient(mContext, mToken, mSecret, mAccountEsid);

                case Sonet.RSS:
                    return new RssClient(mContext, mToken, mSecret, mAccountEsid);

                case Sonet.IDENTICA:
                    return new IdentiCaClient(mContext, mToken, mSecret, mAccountEsid);

                case Sonet.GOOGLEPLUS:
                    return new GooglePlusClient(mContext, mToken, mSecret, mAccountEsid);

                case Sonet.PINTEREST:
                    return new PinterestClient(mContext, mToken, mSecret, mAccountEsid);

                case Sonet.CHATTER:
                    return new ChatterClient(mContext, mToken, mSecret, mAccountEsid);

                default:
                    throw new IllegalArgumentException("Unsupported network: " + mNetwork);
            }
        }
    }

    abstract public String getFeed(int appWidgetId, String widget, long account, int service, int status_count, boolean time24hr, boolean display_profile, int notifications, HttpClient httpClient);

    abstract public boolean createPost(String message, String placeId, String latitude, String longitude, String photoPath, String[] tags);

    abstract public LinkedHashMap<String, String> getLocations(String latitude, String longitude);

    abstract String getApiKey();

    abstract String getApiSecret();

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

    void addStatusItem(String widget, String message, int appWidgetId) {
        int status_bg_color = Sonet.default_message_bg_color;
        int profile_bg_color = Sonet.default_message_bg_color;
        int friend_bg_color = Sonet.default_friend_bg_color;
        boolean icon = true;
        Cursor c = getContentResolver().query(Sonet.Widgets_settings.getContentUri(mContext), new String[]{Sonet.Widgets.TIME24HR, Sonet.Widgets.MESSAGES_BG_COLOR, Sonet.Widgets.ICON, Sonet.Widgets.STATUSES_PER_ACCOUNT, Sonet.Widgets.SOUND, Sonet.Widgets.VIBRATE, Sonet.Widgets.LIGHTS, Sonet.Widgets.PROFILES_BG_COLOR, Sonet.Widgets.FRIEND_BG_COLOR}, Sonet.Widgets.WIDGET + "=? and " + Sonet.Widgets.ACCOUNT + "=?", new String[]{widget, Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);

        if (!c.moveToFirst()) {
            // no widget settings
            c.close();
            c = getContentResolver().query(Sonet.Widgets_settings.getContentUri(mContext), new String[]{Sonet.Widgets.TIME24HR, Sonet.Widgets.MESSAGES_BG_COLOR, Sonet.Widgets.ICON, Sonet.Widgets.STATUSES_PER_ACCOUNT, Sonet.Widgets.SOUND, Sonet.Widgets.VIBRATE, Sonet.Widgets.LIGHTS, Sonet.Widgets.PROFILES_BG_COLOR, Sonet.Widgets.FRIEND_BG_COLOR}, Sonet.Widgets.WIDGET + "=? and " + Sonet.Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
        }

        if (c.moveToFirst()) {
            status_bg_color = c.getInt(1);
            icon = c.getInt(2) == 1;
            profile_bg_color = c.getInt(7);
            friend_bg_color = c.getInt(8);
        }

        c.close();
        long id;
        long created = System.currentTimeMillis();
        int service = 0;
        boolean time24hr = false;
        long accountId = Sonet.INVALID_ACCOUNT_ID;
        String sid = "-1";
        String esid = "-1";
        String friend = getString(R.string.app_name);
        byte[] profile = getBlob(getResources(), R.drawable.icon);
        Cursor entity = getContentResolver().query(Sonet.Entities.getContentUri(mContext), new String[]{Sonet.Entities._ID}, Sonet.Entities.ACCOUNT + "=? and " + Sonet.Entities.ESID + "=?", new String[]{Long.toString(accountId), SonetCrypto.getInstance(mContext).Encrypt(esid)}, null);

        if (entity.moveToFirst()) {
            id = entity.getLong(0);
        } else {
            ContentValues entityValues = new ContentValues();
            entityValues.put(Sonet.Entities.ESID, esid);
            entityValues.put(Sonet.Entities.FRIEND, friend);
            entityValues.put(Sonet.Entities.PROFILE, profile);
            entityValues.put(Sonet.Entities.ACCOUNT, accountId);
            id = Long.parseLong(getContentResolver().insert(Sonet.Entities.getContentUri(mContext), entityValues).getLastPathSegment());
        }

        entity.close();
        ContentValues values = new ContentValues();
        values.put(Sonet.Statuses.CREATED, created);
        values.put(Sonet.Statuses.ENTITY, id);
        values.put(Sonet.Statuses.MESSAGE, message);
        values.put(Sonet.Statuses.SERVICE, service);
        values.put(Sonet.Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
        values.put(Sonet.Statuses.WIDGET, appWidgetId);
        values.put(Sonet.Statuses.ACCOUNT, accountId);
        values.put(Sonet.Statuses.SID, sid);
        values.put(Sonet.Statuses.FRIEND_OVERRIDE, friend);
        values.put(Sonet.Statuses.STATUS_BG, createBackground(status_bg_color));
        values.put(Sonet.Statuses.FRIEND_BG, createBackground(friend_bg_color));
        values.put(Sonet.Statuses.PROFILE_BG, createBackground(profile_bg_color));
        Bitmap emptyBmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream imageBgStream = new ByteArrayOutputStream();
        emptyBmp.compress(Bitmap.CompressFormat.PNG, 100, imageBgStream);
        byte[] emptyImg = imageBgStream.toByteArray();
        emptyBmp.recycle();
        emptyBmp = null;

        if (icon && (emptyImg != null)) {
            values.put(Sonet.Statuses.ICON, emptyImg);
        }

        long statusId = Long.parseLong(getContentResolver().insert(Sonet.Statuses.getContentUri(mContext), values).getLastPathSegment());

        // remote views can be reused, avoid images being repeated across multiple statuses
        if (emptyImg != null) {
            ContentValues imageValues = new ContentValues();
            imageValues.put(Sonet.Status_images.STATUS_ID, statusId);
            imageValues.put(Sonet.Status_images.IMAGE, emptyImg);
            imageValues.put(Sonet.Status_images.IMAGE_BG, emptyImg);
            getContentResolver().insert(Sonet.Status_images.getContentUri(mContext), imageValues);
        }
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

    void addStatusItem(long created, String friend, String url, String message, int service, boolean time24hr, int appWidgetId, long accountId, String sid, String esid, ArrayList<String[]> links, HttpClient httpClient) {
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

        Cursor entity = getContentResolver().query(Sonet.Entities.getContentUri(mContext), new String[]{Sonet.Entities._ID}, Sonet.Entities.ACCOUNT + "=? and " + Sonet.Entities.ESID + "=?", new String[]{Long.toString(accountId), SonetCrypto.getInstance(mContext).Encrypt(esid)}, null);

        if (entity.moveToFirst()) {
            id = entity.getLong(0);
        } else {
            ContentValues entityValues = new ContentValues();
            entityValues.put(Sonet.Entities.ESID, esid);
            entityValues.put(Sonet.Entities.FRIEND, friend);
            entityValues.put(Sonet.Entities.PROFILE, profile);
            entityValues.put(Sonet.Entities.ACCOUNT, accountId);
            id = Long.parseLong(getContentResolver().insert(Sonet.Entities.getContentUri(mContext), entityValues).getLastPathSegment());
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
        values.put(Sonet.Statuses.CREATED, created);
        values.put(Sonet.Statuses.ENTITY, id);
        values.put(Sonet.Statuses.MESSAGE, message);
        values.put(Sonet.Statuses.SERVICE, service);
        values.put(Sonet.Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
        values.put(Sonet.Statuses.WIDGET, appWidgetId);
        values.put(Sonet.Statuses.ACCOUNT, accountId);
        values.put(Sonet.Statuses.SID, sid);
        values.put(Sonet.Statuses.FRIEND_OVERRIDE, friend_override);
        long statusId = Long.parseLong(getContentResolver().insert(Sonet.Statuses.getContentUri(mContext), values).getLastPathSegment());
        String imageUrl = null;

        for (String[] s : links) {
            // get the first photo
            if (imageUrl == null) {
                imageUrl = getFirstPhotoUrl(s);
            }

            ContentValues linkValues = new ContentValues();
            linkValues.put(Sonet.Status_links.STATUS_ID, statusId);
            linkValues.put(Sonet.Status_links.LINK_TYPE, s[0]);
            linkValues.put(Sonet.Status_links.LINK_URI, s[1]);
            getContentResolver().insert(Sonet.Status_links.getContentUri(mContext), linkValues);
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
        Cursor statuses = getContentResolver().query(Sonet.Statuses.getContentUri(mContext), new String[]{Sonet.Statuses._ID}, Sonet.Statuses.WIDGET + "=? and " + Sonet.Statuses.ACCOUNT + "=?", new String[]{widgetId, accountId}, null);

        if (statuses.moveToFirst()) {
            while (!statuses.isAfterLast()) {
                String id = Long.toString(statuses.getLong(0));
                getContentResolver().delete(Sonet.Status_links.getContentUri(mContext), Sonet.Status_links.STATUS_ID + "=?", new String[]{id});
                getContentResolver().delete(Sonet.Status_images.getContentUri(mContext), Sonet.Status_images.STATUS_ID + "=?", new String[]{id});
                statuses.moveToNext();
            }
        }

        statuses.close();
        getContentResolver().delete(Sonet.Statuses.getContentUri(mContext), Sonet.Statuses.WIDGET + "=? and " + Sonet.Statuses.ACCOUNT + "=?", new String[]{widgetId, accountId});
        Cursor entities = getContentResolver().query(Sonet.Entities.getContentUri(mContext), new String[]{Sonet.Entities._ID}, Sonet.Entities.ACCOUNT + "=?", new String[]{accountId}, null);

        if (entities.moveToFirst()) {
            while (!entities.isAfterLast()) {
                Cursor s = getContentResolver().query(Sonet.Statuses.getContentUri(mContext), new String[]{Sonet.Statuses._ID}, Sonet.Statuses.ACCOUNT + "=? and " + Sonet.Statuses.WIDGET + " !=?", new String[]{accountId, widgetId}, null);
                if (!s.moveToFirst()) {
                    // not in use, remove it
                    getContentResolver().delete(Sonet.Entities.getContentUri(mContext), Sonet.Entities._ID + "=?", new String[]{Long.toString(entities.getLong(0))});
                }
                s.close();
                entities.moveToNext();
            }
        }

        entities.close();
    }

    void addNotification(String sid, String esid, String friend, String message, long created, long accountId, String notification) {
        ContentValues values = new ContentValues();
        values.put(Sonet.Notifications.SID, sid);
        values.put(Sonet.Notifications.ESID, esid);
        values.put(Sonet.Notifications.FRIEND, friend);
        values.put(Sonet.Notifications.MESSAGE, message);
        values.put(Sonet.Notifications.CREATED, created);
        values.put(Sonet.Notifications.ACCOUNT, accountId);
        values.put(Sonet.Notifications.NOTIFICATION, notification);
        values.put(Sonet.Notifications.CLEARED, 0);
        values.put(Sonet.Notifications.UPDATED, created);
        getContentResolver().insert(Sonet.Notifications.getContentUri(mContext), values);
    }

    String updateNotification(long notificationId, long created_time, String accountEsid, String esid, String name, boolean cleared) {
        String message = null;
        // new comment
        ContentValues values = new ContentValues();
        values.put(Sonet.Notifications.UPDATED, created_time);

        if (accountEsid.equals(esid)) {
            // user's own comment, clear the notification
            values.put(Sonet.Notifications.CLEARED, 1);
        } else if (cleared) {
            values.put(Sonet.Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), name));
            values.put(Sonet.Notifications.CLEARED, 0);
            message = String.format(getString(R.string.friendcommented), name);
        } else {
            values.put(Sonet.Notifications.NOTIFICATION, String.format(getString(R.string.friendcommented), name + " and others"));
            message = String.format(getString(R.string.friendcommented), name + " and others");
        }

        getContentResolver().update(Sonet.Notifications.getContentUri(mContext), values, Sonet.Notifications._ID + "=?", new String[]{Long.toString(notificationId)});
        return message;
    }

    String updateNotificationMessage(String originalMessage, String newMessage) {
        if (TextUtils.isEmpty(originalMessage)) {
            return newMessage;
        } else if (!TextUtils.isEmpty(newMessage)) {
            return mContext.getString(R.string.notify_multiple_updates);
        }

        return originalMessage;
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
}
