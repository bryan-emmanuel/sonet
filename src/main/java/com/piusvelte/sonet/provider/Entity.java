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
public final class Entity implements BaseColumns {

    public static final String TABLE = "entities";

    private Entity() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/entities");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.entities";

    public static final String ESID = "esid";
    public static final String FRIEND = "friend";
    public static final String PROFILE_URL = "profile_url";
    public static final String ACCOUNT = "account";

    public static void createTable(@NonNull SQLiteDatabase db) {
        db.execSQL("create table if not exists " + TABLE
                + " (" + _ID + " integer primary key autoincrement, "
                + FRIEND + " text, "
                + ACCOUNT + " integer, "
                + ESID + " text, "
                + PROFILE_URL + " text);");
    }

    public static void migrateTable(@NonNull SQLiteDatabase db) {
        db.execSQL("drop table if exists " + TABLE + "_bkp;");
        db.execSQL("create temp table " + TABLE + "_bkp as select * from " + TABLE + ";");
        db.execSQL("drop table if exists " + TABLE + ";");
        createTable(db);
        db.execSQL("insert into " + TABLE
                + " select "
                + _ID
                + "," + FRIEND
                + "," + ACCOUNT
                + "," + ESID
                + "," + PROFILE_URL + " from " + TABLE + "_bkp;");
        db.execSQL("drop table if exists " + TABLE + "_bkp;");
    }
}
