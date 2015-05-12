package com.piusvelte.sonet.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public class Statuses implements BaseColumns {

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
    public static final String STATUS_BG = "status_bg";
    public static final String ICON = "icon";
    // service id for posting and linking
    public static final String SID = "sid";
    // store friend and profile data in a separate table
    public static final String ENTITY = "entity";
    public static final String FRIEND_OVERRIDE = "friend_override";
    @Deprecated
    public static final String PROFILE_BG = "profiles_bg_color";
    public static final String FRIEND_BG = "friend_bg";
}
