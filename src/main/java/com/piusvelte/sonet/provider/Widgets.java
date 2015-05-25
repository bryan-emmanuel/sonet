package com.piusvelte.sonet.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public class Widgets implements BaseColumns {

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
    @Deprecated
    public static final String HASBUTTONS = "hasbuttons";
    @Deprecated
    public static final String BUTTONS_BG_COLOR = "buttons_bg_color";
    @Deprecated
    public static final String BUTTONS_COLOR = "buttons_color";
    @Deprecated
    public static final String MESSAGES_BG_COLOR = "messages_bg_color";
    @Deprecated
    public static final String MESSAGES_COLOR = "messages_color";
    public static final String TIME24HR = "time24hr";
    @Deprecated
    public static final String FRIEND_COLOR = "friend_color";
    @Deprecated
    public static final String CREATED_COLOR = "created_color";
    @Deprecated
    public static final String SCROLLABLE = "scrollable";
    @Deprecated
    public static final String BUTTONS_TEXTSIZE = "buttons_textsize";
    @Deprecated
    public static final String MESSAGES_TEXTSIZE = "messages_textsize";
    @Deprecated
    public static final String FRIEND_TEXTSIZE = "friend_textsize";
    @Deprecated
    public static final String CREATED_TEXTSIZE = "created_textsize";
    @Deprecated
    public static final String ICON = "icon";
    @Deprecated
    public static final String STATUSES_PER_ACCOUNT = "statuses_per_account";
    // make background updating optional
    public static final String BACKGROUND_UPDATE = "background_update";
    public static final String SOUND = "sound";
    public static final String VIBRATE = "vibrate";
    public static final String LIGHTS = "lights";
    @Deprecated
    public static final String DISPLAY_PROFILE = "display_profile";
    public static final String INSTANT_UPLOAD = "instant_upload";
    @Deprecated
    public static final String MARGIN = "margin";
    @Deprecated
    public static final String PROFILES_BG_COLOR = "profiles_bg_color";
    @Deprecated
    public static final String FRIEND_BG_COLOR = "friend_bg_color";
}
