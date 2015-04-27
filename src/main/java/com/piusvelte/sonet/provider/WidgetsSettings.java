package com.piusvelte.sonet.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public final class WidgetsSettings implements BaseColumns {

    private WidgetsSettings() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/widgets_settings");
    }

    public static Uri getDistinctContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/distinct_widgets_settings");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widgets_settings";
}
