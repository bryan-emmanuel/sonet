package com.piusvelte.sonet.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public class WidgetAccountsView implements BaseColumns {

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
}
