package com.piusvelte.sonet.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.util.DatabaseUtils;

/**
 * Created by bemmanuel on 3/22/15.
 */
public final class WidgetsSettings implements BaseColumns {

    public static final String VIEW = "widgets_settings";

    private WidgetsSettings() {
    }

    public static Uri getContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/widgets_settings");
    }

    public static Uri getDistinctContentUri(Context context) {
        return Uri.parse("content://" + Sonet.getAuthority(context) + "/distinct_widgets_settings");
    }

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.piusvelte.widgets_settings";

    public static void createView(@NonNull SQLiteDatabase db) {
        String[] aliases = new String[] { "a", "b", "c" };

        db.execSQL("create view if not exists " + WidgetsSettings.VIEW + " as select a."
                + Widgets._ID + " as " + Widgets._ID
                + ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
                + "," + DatabaseUtils.addCase(aliases, Widgets.INTERVAL, Sonet.default_interval, Widgets.INTERVAL)
                + "," + DatabaseUtils.addCase(aliases, Widgets.TIME24HR, 0, Widgets.TIME24HR)
                + ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
                + "," + DatabaseUtils.addCase(aliases, Widgets.BACKGROUND_UPDATE, 1, Widgets.BACKGROUND_UPDATE)
                + "," + DatabaseUtils.addCase(aliases, Widgets.SOUND, 0, Widgets.SOUND)
                + "," + DatabaseUtils.addCase(aliases, Widgets.VIBRATE, 0, Widgets.VIBRATE)
                + "," + DatabaseUtils.addCase(aliases, Widgets.LIGHTS, 0, Widgets.LIGHTS)
                + "," + DatabaseUtils.addCase(aliases, Widgets.INSTANT_UPLOAD, 0, Widgets.INSTANT_UPLOAD)
                + " from " + Widgets.TABLE + " a,"
                + Widgets.TABLE + " b,"
                + Widgets.TABLE + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." +
                Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
    }
}
