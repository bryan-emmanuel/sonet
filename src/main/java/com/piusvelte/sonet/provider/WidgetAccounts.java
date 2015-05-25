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
public class WidgetAccounts implements BaseColumns {

    public static final String TABLE = "widget_accounts";

    private WidgetAccounts() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/widget_accounts");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widget_accounts";

    public static final String ACCOUNT = "account";
    public static final String WIDGET = "widget";

    public static void createTable(@NonNull SQLiteDatabase db) {
        db.execSQL("create table if not exists " + TABLE
                + " (" + WidgetAccounts._ID + " integer primary key autoincrement, "
                + WidgetAccounts.ACCOUNT + " integer, "
                + WidgetAccounts.WIDGET + " integer);");
    }
}