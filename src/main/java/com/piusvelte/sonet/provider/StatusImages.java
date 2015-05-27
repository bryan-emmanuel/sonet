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
public final class StatusImages implements BaseColumns {

    public static final String TABLE = "status_images";

    private StatusImages() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/status_images");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.status_images";
    public static final String STATUS_ID = "status_id";
    public static final String URL = "url";

    public static void createTable(@NonNull SQLiteDatabase db) {
        db.execSQL("create table if not exists " + TABLE
                + " (" + _ID + " integer primary key autoincrement, "
                + STATUS_ID + " integer, "
                + URL + " text);");
    }

    public static void migrateTable(@NonNull SQLiteDatabase db) {
        db.execSQL("drop table if exists " + TABLE + "_bkp;");
        db.execSQL("create temp table " + TABLE + "_bkp as select * from " + TABLE + ";");
        db.execSQL("drop table if exists " + TABLE + ";");
        createTable(db);
        db.execSQL("insert into " + TABLE
                + " select "
                + _ID
                + "," + STATUS_ID
                + "," + URL + " from " + TABLE + "_bkp;");
        db.execSQL("drop table if exists " + TABLE + "_bkp;");
    }
}
