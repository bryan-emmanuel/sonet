package com.piusvelte.sonet.provider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import com.piusvelte.sonet.Sonet;

/**
 * Created by bemmanuel on 3/22/15.
 */
public final class Entities implements BaseColumns {

    private Entities() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/entities");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.entities";

    public static final String ESID = "esid";
    public static final String FRIEND = "friend";
    public static final String PROFILE = "profile";
    public static final String ACCOUNT = "account";

}
