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
public class Widgets implements BaseColumns {

    public static final String TABLE = "widgets";

    private Widgets() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/widgets");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widgets";

    public static final String WIDGET = "widget";
    // account specific settings per widget
    public static final String ACCOUNT = "account";
    public static final String INTERVAL = "interval";
    public static final String TIME24HR = "time24hr";
    // make background updating optional
    public static final String BACKGROUND_UPDATE = "background_update";
    public static final String SOUND = "sound";
    public static final String VIBRATE = "vibrate";
    public static final String LIGHTS = "lights";
    public static final String INSTANT_UPLOAD = "instant_upload";

    public static void createTable(@NonNull SQLiteDatabase db) {
        db.execSQL("create table if not exists " + TABLE
                + " (" + _ID + " integer primary key autoincrement, "
                + WIDGET + " integer, "
                + INTERVAL + " integer, "
                + TIME24HR + " integer, "
                + ACCOUNT + " integer, "
                + BACKGROUND_UPDATE + " integer, "
                + SOUND + " integer, "
                + VIBRATE + " integer, "
                + LIGHTS + " integer, "
                + INSTANT_UPLOAD + " integer);");
    }

    public static void migrateTable(@NonNull SQLiteDatabase db) {
        db.execSQL("drop table if exists " + TABLE + "_bkp;");
        db.execSQL("create temp table " + TABLE + "_bkp as select * from " + TABLE + ";");
        db.execSQL("drop table if exists " + TABLE + ";");
        createTable(db);
        db.execSQL("insert into " + TABLE
                + " select "
                + _ID
                + "," + WIDGET
                + "," + INTERVAL
                + "," + TIME24HR
                + "," + ACCOUNT
                + "," + BACKGROUND_UPDATE
                + "," + SOUND
                + "," + VIBRATE
                + "," + LIGHTS
                + "," + INSTANT_UPLOAD + " from " + TABLE + "_bkp;");
        db.execSQL("drop table if exists " + TABLE + "_bkp;");
    }
}
