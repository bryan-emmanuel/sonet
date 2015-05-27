package com.piusvelte.sonet.provider;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public class Statuses implements BaseColumns {

    public static final String TABLE = "statuses";

    private Statuses() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/statuses");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.statuses";

    public static final String CREATED = "created";
    public static final String MESSAGE = "message";
    public static final String SERVICE = "service";
    public static final String WIDGET = "widget";
    public static final String CREATEDTEXT = "createdtext";
    // account specific settings per widget
    public static final String ACCOUNT = "account";
    // service id for posting and linking
    public static final String SID = "sid";
    // store friend and profile data in a separate table
    public static final String ENTITY = "entity";
    public static final String FRIEND_OVERRIDE = "friend_override";

    public static void createTable(@NonNull SQLiteDatabase db) {
        db.execSQL("create table if not exists " + TABLE
                + " (" + _ID + " integer primary key autoincrement, "
                + CREATED + " integer, "
                + MESSAGE + " text, "
                + SERVICE + " integer, "
                + CREATEDTEXT + " text, "
                + WIDGET + " integer, "
                + ACCOUNT + " integer, "
                + SID + " text, "
                + ENTITY + " integer, "
                + FRIEND_OVERRIDE + " text);");
    }

    public static void migrateTable(@NonNull SQLiteDatabase db) {
        db.execSQL("drop table if exists " + TABLE + "_bkp;");
        db.execSQL("create temp table " + TABLE + "_bkp as select * from " + TABLE + ";");
        db.execSQL("drop table if exists " + TABLE + ";");
        createTable(db);
        db.execSQL("insert into " + TABLE
                + " select "
                + _ID
                + "," + CREATED
                + "," + MESSAGE
                + "," + SERVICE
                + "," + CREATEDTEXT
                + ",\"" + AppWidgetManager.INVALID_APPWIDGET_ID + "\""// this now uses statuses across all widgets
                + "," + ACCOUNT
                + "," + SID
                + "," + ENTITY
                + "," + FRIEND_OVERRIDE + " from " + TABLE + "_bkp;");
        db.execSQL("drop table if exists " + TABLE + "_bkp;");
    }
}
