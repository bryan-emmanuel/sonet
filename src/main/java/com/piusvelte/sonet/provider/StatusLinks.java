package com.piusvelte.sonet.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public final class StatusLinks implements BaseColumns {

    private StatusLinks() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/status_links");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.status_links";
    public static final String STATUS_ID = "status_id";
    public static final String LINK_URI = "link_uri";
    public static final String LINK_TYPE = "link_type";
}
