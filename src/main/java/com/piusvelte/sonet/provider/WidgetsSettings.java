package com.piusvelte.sonet.provider;

import android.app.Notification;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.database.Cursor;
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

    public static final Settings getSettings(@NonNull Context context) {
        return getSettings(context, Sonet.INVALID_ACCOUNT_ID);
    }

    public static final Settings getSettings(@NonNull Context context, long account) {
        return getSettings(context, AppWidgetManager.INVALID_APPWIDGET_ID, account);
    }

    public static final Settings getSettings(@NonNull Context context, int widget, long account) {
        Settings settings = new Settings();

        // no longer supporting widget specific settings
        widget = AppWidgetManager.INVALID_APPWIDGET_ID;

        Cursor userSettings = context.getContentResolver().query(getContentUri(context),
                new String[] { Widgets._ID,
                        Widgets.INTERVAL,
                        Widgets.TIME24HR,
                        Widgets.SOUND,
                        Widgets.VIBRATE,
                        Widgets.LIGHTS,
                        Widgets.BACKGROUND_UPDATE,
                        Widgets.INSTANT_UPLOAD },
                "(" + Widgets.WIDGET + "=? or " + Widgets.WIDGET + "=?) and (" + Widgets.ACCOUNT + "=? or " + Widgets.ACCOUNT + "=?)",
                new String[] { Integer.toString(widget),
                        Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),
                        Long.toString(account),
                        Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                Widgets.WIDGET + " DESC, " + Widgets.ACCOUNT + " DESC");

        if (userSettings.moveToFirst()) {
            settings.interval = userSettings.getInt(userSettings.getColumnIndexOrThrow(Widgets.INTERVAL));
            settings.isTime24hr = userSettings.getInt(userSettings.getColumnIndexOrThrow(Widgets.TIME24HR)) != 0;
            settings.isBackgroundUpdate = userSettings.getInt(userSettings.getColumnIndexOrThrow(Widgets.BACKGROUND_UPDATE)) != 0;
            settings.isSound = userSettings.getInt(userSettings.getColumnIndexOrThrow(Widgets.SOUND)) != 0;
            settings.isVibrate = userSettings.getInt(userSettings.getColumnIndexOrThrow(Widgets.VIBRATE)) != 0;
            settings.isLights = userSettings.getInt(userSettings.getColumnIndexOrThrow(Widgets.LIGHTS)) != 0;
            settings.isInstantUpload = userSettings.getInt(userSettings.getColumnIndexOrThrow(Widgets.INSTANT_UPLOAD)) != 0;
        }

        userSettings.close();
        return settings;
    }

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
                + Widgets.TABLE + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET
                + " and b." + Widgets.ACCOUNT + "=" + Sonet.INVALID_ACCOUNT_ID
                + " and c." + Widgets.WIDGET + "=" + AppWidgetManager.INVALID_APPWIDGET_ID
                + " and c." + Widgets.ACCOUNT + "=" + Sonet.INVALID_ACCOUNT_ID + ";");
    }

    public static class Settings {
        public int interval = 3600000;
        public boolean isTime24hr = false;
        public boolean isBackgroundUpdate = true;
        public boolean isSound = false;
        public boolean isVibrate = false;
        public boolean isLights = false;
        public boolean isInstantUpload = false;

        public int notificationsMask() {
            int mask = 0;
            if (isSound) mask |= Notification.DEFAULT_SOUND;
            if (isVibrate) mask |= Notification.DEFAULT_VIBRATE;
            if (isLights) mask |= Notification.DEFAULT_LIGHTS;
            return mask;
        }
    }
}
