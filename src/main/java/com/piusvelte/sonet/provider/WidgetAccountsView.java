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
public class WidgetAccountsView implements BaseColumns {

    @Deprecated
    public static final String VIEW = "widget_accounts_view";

    private WidgetAccountsView() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/widget_accounts_view");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widget_accounts_view";

    public static final String ACCOUNT = "account";
    public static final String WIDGET = "widget";
    public static final String USERNAME = "username";
    public static final String TOKEN = "token";
    public static final String SECRET = "secret";
    public static final String SERVICE = "service";
    public static final String EXPIRY = "expiry";
    public static final String SID = "sid";

    public static void createView(@NonNull SQLiteDatabase db) {
        db.execSQL("create view if not exists " + VIEW + " as select "
                + WidgetAccounts.TABLE + "." + WidgetAccounts._ID
                + "," + WidgetAccounts.ACCOUNT
                + "," + WidgetAccounts.WIDGET
                + "," + Accounts.EXPIRY
                + "," + Accounts.SECRET
                + "," + Accounts.SERVICE
                + "," + Accounts.SID
                + "," + Accounts.TOKEN
                + "," + Accounts.USERNAME
                + " from "
                + WidgetAccounts.TABLE
                + "," + Accounts.TABLE
                + " where "
                + Accounts.TABLE + "." + Accounts._ID + "=" + WidgetAccounts.ACCOUNT
                + ";");
    }
}
