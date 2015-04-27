package com.piusvelte.sonet.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public final class StatusesStyles implements BaseColumns {

    // this is actually a view, joining the account/widget/default styles to the statuses

    private StatusesStyles() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/statuses_styles");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.statuses_styles";

    public static final String CREATED = "created";
    public static final String FRIEND = "friend";
    public static final String PROFILE = "profile";
    public static final String MESSAGE = "message";
    public static final String SERVICE = "service";
    public static final String WIDGET = "widget";
    // account specific settings per widget
    public static final String ACCOUNT = "account";
    public static final String CREATEDTEXT = "createdtext";
    public static final String MESSAGES_COLOR = "messages_color";
    public static final String FRIEND_COLOR = "friend_color";
    public static final String CREATED_COLOR = "created_color";
    public static final String MESSAGES_TEXTSIZE = "messages_textsize";
    public static final String FRIEND_TEXTSIZE = "friend_textsize";
    public static final String CREATED_TEXTSIZE = "created_textsize";
    public static final String STATUS_BG = "status_bg";
    public static final String ICON = "icon";
    // service id, for posting and linking
    public static final String SID = "sid";
    // store friend and profile data in a separate table
    public static final String ENTITY = "entity";
    public static final String ESID = "esid";
    public static final String PROFILE_BG = "profiles_bg_color";
    public static final String FRIEND_BG = "friend_bg";
    public static final String IMAGE_BG = "image_bg";
    public static final String IMAGE = "image";
}
