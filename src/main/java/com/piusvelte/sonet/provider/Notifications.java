package com.piusvelte.sonet.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public final class Notifications implements BaseColumns {
    public static final String TABLE = "notifications";

    // store notifications
    // notifications are marked cleared when viewed
    // notifications are deleted when the feeds are updated, they are not in the new feeds and are marked cleared
    private Notifications() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/notifications");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.notifications";
    public static final String SID = "sid";
    public static final String ESID = "esid";
    public static final String FRIEND = "friend";
    public static final String MESSAGE = "message";
    public static final String CREATED = "created";
    public static final String ACCOUNT = "account";
    public static final String NOTIFICATION = "notification";
    public static final String CLEARED = "cleared";
    public static final String UPDATED = "updated";

    public static void createTable(@NonNull SQLiteDatabase db) {
        db.execSQL("create table if not exists " + TABLE
                + " (" + Notifications._ID + " integer primary key autoincrement, "
                + Notifications.SID + " text, "
                + Notifications.ESID + " text, "
                + Notifications.FRIEND + " text, "
                + Notifications.MESSAGE + " text, "
                + Notifications.CREATED + " integer, "
                + Notifications.NOTIFICATION + " text, "
                + Notifications.ACCOUNT + " integer, "
                + Notifications.CLEARED + " integer, "
                + Notifications.UPDATED + " integer);");
    }
}
