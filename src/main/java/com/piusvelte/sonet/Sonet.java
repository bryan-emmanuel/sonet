/*
 * Sonet - Android Social Networking Widget
 * Copyright (C) 2009 Bryan Emmanuel
 *
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.sonet;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.annotation.Nullable;

import com.piusvelte.sonet.provider.StatusImages;
import com.piusvelte.sonet.provider.Widgets;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sonet {

    private static final String TAG = "Sonet";

    protected static final String PRO = "pro";
    public static final String Saccess_token = "access_token";
    public static final String Sexpires_in = "expires_in";
    public static final String ACTION_REFRESH = "com.piusvelte.sonet.Sonet.REFRESH";
    protected static final String ACTION_PAGE_UP = "com.piusvelte.sonet.Sonet.PAGE_UP";
    protected static final String ACTION_PAGE_DOWN = "com.piusvelte.sonet.Sonet.PAGE_DOWN";
    public static final String ACTION_ON_CLICK = "com.piusvelte.sonet.Sonet.ON_CLICK";
    public static final String ACTION_UPLOAD = "com.piusvelte.sonet.Sonet.UPLOAD";
    protected static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String EXTRA_ACCOUNT_ID = "com.piusvelte.sonet.Sonet.ACCOUNT_ID";
    public static final String EXTRA_SCROLLABLE_VERSION = "com.piusvelte.sonet.Sonet.SCROLLABLE_VERSION";
    public static final long INVALID_ACCOUNT_ID = -1;
    public static final int RESULT_REFRESH = 1;
    protected static int NOTIFY_ID = 1;

    public static final int TWITTER = 0;
    public static final int FACEBOOK = 1;
    public static final int MYSPACE = 2;
    public static final int FOURSQUARE = 4;
    public static final int LINKEDIN = 5;
    public static final int SMS = 6;
    public static final int RSS = 7;
    public static final int IDENTICA = 8;
    public static final int GOOGLEPLUS = 9;
    public static final int PINTEREST = 10;
    public static final int CHATTER = 11;

    protected static final String AM = "a.m.";
    protected static final String PM = "p.m.";
    protected static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    public static final String Sid = "id";
    public static final String Sname = "name";
    public static final String Suser = "user";
    public static final String Screated_at = "created_at";
    public static final String Sprofile_image_url = "profile_image_url";
    public static final String Stext = "text";
    public static final String Sdata = "data";
    public static final String Screated_time = "created_time";
    public static final String Sfrom = "from";
    public static final String Stype = "type";
    public static final String Smessage = "message";
    public static final String Sto = "to";
    public static final String Slink = "link";
    public static final String Sstatus = "status";
    public static final String Scomment = "comment";
    public static final String Scomments = "comments";
    public static final String Sperson = "person";
    public static final String Svalues = "values";
    public static final String SupdateComments = "updateComments";
    public static final String SupdateKey = "updateKey";
    public static final String Stimestamp = "timestamp";
    public static final String ScurrentShare = "currentShare";
    public static final String SupdateType = "updateType";
    public static final String SupdateContent = "updateContent";
    public static final String SpersonActivities = "personActivities";
    public static final String Sconnections = "connections";
    public static final String S_total = "_total";
    public static final String Svenue = "venue";
    public static final String Srecent = "recent";
    public static final String SrecommendationsGiven = "recommendationsGiven";
    public static final String Sjob = "job";
    public static final String Sposition = "position";
    public static final String SmemberGroups = "memberGroups";

    public static final String Sauthor = "author";
    public static final String Sentry = "entry";
    public static final String SpostedDate = "postedDate";
    public static final String SdisplayName = "displayName";
    public static final String Sphoto = "photo";
    public static final String SsmallPhotoUrl = "smallPhotoUrl";
    public static final String SmoodStatusLastUpdated = "moodStatusLastUpdated";
    public static final String SthumbnailUrl = "thumbnailUrl";
    public static final String SrecentComments = "recentComments";
    public static final String SuserId = "userId";
    public static final String SstatusId = "statusId";
    public static final String Sitem = "item";
    public static final String Sitems = "items";
    public static final String Sobject = "object";
    public static final String Spublished = "published";
    public static final String Sinstance_url = "instance_url";
    public static final String Scontent = "content";
    public static final String SoriginalContent = "originalContent";
    public static final String Sreplies = "replies";
    public static final String Simage = "image";
    public static final String Surl = "url";
    public static final String Sactor = "actor";
    public static final String StotalItems = "totalItems";
    public static final String ScreatedDate = "createdDate";
    public static final String Sbody = "body";
    public static final String Stotal = "total";
    public static final String ScreatedAt = "createdAt";
    public static final String SfirstName = "firstName";
    public static final String SlastName = "lastName";
    public static final String Sresponse = "response";
    public static final String Scheckin = "checkin";
    public static final String Sshout = "shout";
    public static final String Stitle = "title";
    public static final String Sdescription = "description";
    public static final String Spubdate = "pubdate";
    public static final String SpictureUrl = "pictureUrl";
    public static final String SisCommentable = "isCommentable";
    public static final String SrecommendationSnippet = "recommendationSnippet";
    public static final String Srecommendee = "recommendee";
    public static final String Sscreen_name = "screen_name";
    public static final String Sin_reply_to_status_id = "in_reply_to_status_id";
    public static final String Suser_likes = "user_likes";
    public static final String ScommentId = "commentId";
    public static final String Sgroups = "groups";
    public static final String SNearby = "Nearby";
    public static final String Splaces = "places";
    public static final String Sresult = "result";
    public static final String Sfull_name = "full_name";
    public static final String Ssource = "source";
    public static final String Sstory = "story";
    public static final String Smobile = "mobile";
    public static final String Simage_url = "image_url";
    public static final String Scounts = "counts";
    public static final String Simages = "images";
    public static final String Susername = "username";
    public static final String Spicture = "picture";
    public static final String Sboard = "board";
    public static final String Simgur = "i.imgur.com";
    public static final String Splace = "place";
    public static final String Stags = "tags";

    private static final String POWER_SERVICE = Context.POWER_SERVICE;
    private static WakeLock sWakeLock;

    static boolean hasLock() {
        return (sWakeLock != null);
    }

    public static void acquire(Context context) {
        if (hasLock()) sWakeLock.release();
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        sWakeLock.acquire();
    }

    static void release() {
        if (hasLock()) {
            sWakeLock.release();
            sWakeLock = null;
        }
    }

    public static final int INVALID_SERVICE = -1;

    protected static final int default_interval = 3600000;
    protected static final int default_buttons_bg_color = 0x88000000;
    protected static final int default_buttons_color = 0xFFFFFFFF;
    public static final int default_message_bg_color = 0x88FFFFFF;
    public static final int default_message_color = 0xFF000000;
    public static final int default_friend_color = 0xFFFFFFFF;
    public static final int default_created_color = 0xFFFFFFFF;
    protected static final int default_buttons_textsize = 14;
    public static final int default_messages_textsize = 14;
    public static final int default_friend_textsize = 14;
    public static final int default_created_textsize = 14;
    protected static final int default_statuses_per_account = 10;
    protected static final boolean default_include_profile = true;
    protected static final int default_margin = 0;
    public static final int default_friend_bg_color = 0x88000000;
    protected static final boolean default_hasButtons = false;
    protected static final boolean default_time24hr = false;
    protected static final boolean default_hasIcon = true;
    protected static final boolean default_backgroundUpdate = true;
    protected static final boolean default_sound = false;
    protected static final boolean default_vibrate = false;
    protected static final boolean default_lights = false;
    protected static final boolean default_instantUpload = false;

    private Sonet() {
        // not instantiable
    }

    public static final String getAuthority(Context context) {
        return !context.getPackageName().toLowerCase().contains(PRO) ? SonetProvider.AUTHORITY : SonetProvider.PRO_AUTHORITY;
    }

    public static final TimeZone sTimeZone = TimeZone.getTimeZone("GMT");

    public static final String[] sRFC822 = { "EEE, d MMM yy HH:mm:ss z", "EEE, d MMM yy HH:mm z", "EEE, d MMM yyyy HH:mm:ss z", "EEE, d MMM yyyy " +
            "HH:mm z", "d MMM yy HH:mm z", "d MMM yy HH:mm:ss z", "d MMM yyyy HH:mm z", "d MMM yyyy HH:mm:ss z" };

    public static String getCreatedText(long epoch, boolean time24hr) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epoch);
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTimeInMillis(System.currentTimeMillis());
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        // check if the date is from the same day
        if ((calendar.get(Calendar.ERA) == todayCal.get(Calendar.ERA)) && (calendar.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)) && (calendar
                .get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR))) {
            if (time24hr) {
                return String.format("%d:%02d", hours, calendar.get(Calendar.MINUTE));
            } else {
                // set am/pm
                if (hours == 0) {
                    return String.format("%d:%02d%s", 12, calendar.get(Calendar.MINUTE), Sonet.AM);
                } else if (hours < 12) {
                    return String.format("%d:%02d%s", hours, calendar.get(Calendar.MINUTE), Sonet.AM);
                } else if (hours == 12) {
                    return String.format("%d:%02d%s", hours, calendar.get(Calendar.MINUTE), Sonet.PM);
                } else {
                    return String.format("%d:%02d%s", hours - 12, calendar.get(Calendar.MINUTE), Sonet.PM);
                }
            }
        } else {
            return String.format("%s %d", Sonet.MONTHS[calendar.get(Calendar.MONTH)], calendar.get(Calendar.DATE));
        }
    }

    public static int[] getWidgets(Context context, AppWidgetManager awm) {
        int[] widgets = new int[0];
        Class[] clazzes = new Class[] { SonetWidget_2x2.class, SonetWidget_2x3.class, SonetWidget_2x4.class, SonetWidget_4x2.class, SonetWidget_4x3
                .class, SonetWidget_4x4.class };

        for (Class clazz : clazzes) {
            widgets = Sonet.arrayCat(widgets, awm.getAppWidgetIds(new ComponentName(context, clazz)));
        }

        return widgets;
    }

    public static String getServiceName(Resources r, int service) {
        String[] entries = r.getStringArray(R.array.service_entries);
        String[] values = r.getStringArray(R.array.service_values);

        for (int i = 0, l = values.length; i < l; i++) {
            if (Integer.toString(service).equals(values[i])) {
                return entries[i];
            }
        }

        return null;
    }

    public static int[] arrayCat(int[] a, int[] b) {
        int[] c;

        for (int i = 0, i2 = b.length; i < i2; i++) {
            int cLen = a.length;
            c = new int[cLen];

            for (int n = 0; n < cLen; n++) {
                c[n] = a[n];
            }

            a = new int[cLen + 1];

            for (int n = 0; n < cLen; n++) {
                a[n] = c[n];
            }

            a[cLen] = b[i];
        }

        return a;
    }

    public static boolean arrayContains(int[] a, int b) {
        for (int c : a) {
            if (c == b) {
                return true;
            }
        }

        return false;
    }

    public static BitmapFactory.Options sBFOptions = new BitmapFactory.Options();

    static {
        sBFOptions.inDither = false;
        sBFOptions.inPurgeable = true; // allow this memory to be reclaimed
        sBFOptions.inInputShareable = true; // share the reference, rather than copy
        sBFOptions.inTempStorage = new byte[32 * 1024]; // allocate temporary memory
    }

    public static boolean HasValues(String[] values) {
        boolean hasValues = values != null;
        if (hasValues) {
            for (String value : values) {
                if (value == null) {
                    hasValues = false;
                    break;
                }
            }
        }
        return hasValues;
    }

    @Nullable
    public static Bitmap getBitmap(InputStream is) {
        return BitmapFactory.decodeStream(is, null, sBFOptions);
    }

    @Nullable
    public static Bitmap getBitmap(Resources r, int i) {
        return BitmapFactory.decodeResource(r, i, sBFOptions);
    }

    @Nullable
    public static Bitmap getCircleCrop(Bitmap in) {
        if (in != null) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new BitmapShader(in, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
            int radius = Math.min(in.getWidth(), in.getHeight()) / 2;

            Bitmap out = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            canvas.drawCircle(radius, radius, radius, paint);

            in.recycle();
            return out;
        }

        return null;
    }

    @Nullable
    public static byte[] getBlob(Bitmap bmp) {
        if (bmp != null) {
            ByteArrayOutputStream blob = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, blob);
            bmp.recycle();
            return blob.toByteArray();
        }

        return null;
    }

    public static Matcher getLinksMatcher(String raw) {
        return Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(raw);
    }

    public static byte[] createBackground(int color) {
        Bitmap b = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(color);
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, s);
        byte[] bg = s.toByteArray();

        if (b != null) {
            b.recycle();
        }

        return bg;
    }

    public static String initAccountSettings(Context context, int widget, long account) {
        ContentValues values = new ContentValues();
        values.put(Widgets.WIDGET, widget);
        values.put(Widgets.ACCOUNT, account);
        return context.getContentResolver().insert(Widgets.getContentUri(context), values).getLastPathSegment();
    }

    public static int getCropSize(int src, int dst) {
        return (int) Math.round((src - dst) / 2.0);
    }

    public static boolean insertStatusImageBg(Context context, long statusId, byte[] bImg, int height) {
        Bitmap bmpBg = Bitmap.createBitmap(1, height, Config.ARGB_8888);
        ByteArrayOutputStream baosBg = new ByteArrayOutputStream();
        bmpBg.compress(Bitmap.CompressFormat.PNG, 100, baosBg);
        byte[] bBg = baosBg.toByteArray();
        bmpBg.recycle();

        if (bBg != null) {
            ContentValues imageValues = new ContentValues();
            imageValues.put(StatusImages.STATUS_ID, statusId);
            imageValues.put(StatusImages.IMAGE, (bImg != null ? bImg : bBg));
            imageValues.put(StatusImages.IMAGE_BG, bBg);
            context.getContentResolver().insert(StatusImages.getContentUri(context), imageValues);
            return true;
        }

        return false;
    }
}
