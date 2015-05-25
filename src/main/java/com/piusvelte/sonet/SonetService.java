/*
 * Sonet - Android Social Networking Widget
 * Copyright (C) 2009 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.sonet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.RemoteViews;

import com.piusvelte.eidos.Eidos;
import com.piusvelte.sonet.loader.SMSLoader;
import com.piusvelte.sonet.loader.StatusesLoader;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;

import java.util.ArrayList;
import java.util.HashMap;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.SMS_RECEIVED;
import static com.piusvelte.sonet.Sonet.initAccountSettings;

public class SonetService extends Service {
    private static final String TAG = "SonetService";
    public final static HashMap<Integer, StatusesLoader> mStatusesLoaders = new HashMap<>();
    public final ArrayList<SMSLoader> mSMSLoaders = new ArrayList<>();
    public AlarmManager mAlarmManager;
    public ConnectivityManager mConnectivityManager;
    public String mNotify = null;

    public int mStartId = Sonet.INVALID_SERVICE;

    @Override
    public void onCreate() {
        super.onCreate();
        // handle version changes
        int currVer = 0;

        try {
            currVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (!sp.contains(getString(R.string.key_version)) || (currVer > sp.getInt(getString(R.string.key_version), 0))) {
            sp.edit().putInt(getString(R.string.key_version), currVer).apply();
            Eidos.requestBackup(this);
        }

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        // check the instant upload settings
        startService(new Intent(this, SonetUploader.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        start(intent);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        mStartId = startId;
        start(intent);
    }

    private void start(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();

            if (BuildConfig.DEBUG) Log.d(TAG, "action:" + action);

            if (ACTION_REFRESH.equals(action)) {
                if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
                    putValidatedUpdates(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS),
                            1);
                } else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                    putValidatedUpdates(new int[] { intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                    AppWidgetManager.INVALID_APPWIDGET_ID) },
                            1);
                } else if (intent.getData() != null) {
                    putValidatedUpdates(new int[] { Integer.parseInt(intent.getData().getLastPathSegment()) },
                            1);
                } else {
                    putValidatedUpdates(null, 0);
                }
            } else if (SMS_RECEIVED.equals(action)) {
                // parse the sms, and notify any widgets which have sms enabled
                Bundle bundle = intent.getExtras();
                Object[] pdus = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdus.length; i++) {
                    SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    SMSLoader smsLoader = new SMSLoader(this);
                    mSMSLoaders.add(smsLoader);
                    smsLoader.execute(msg);
                }
            } else {
                // this might be a widget update from the widget refresh button
                int appWidgetId;

                try {
                    appWidgetId = Integer.parseInt(action);
                    putValidatedUpdates(new int[] { appWidgetId }, 1);
                } catch (NumberFormatException e) {
                    Log.d(TAG, "unknown action:" + action);
                }
            }
        }
    }

    public void putValidatedUpdates(int[] appWidgetIds, int reload) {
        int[] awi = Sonet.getWidgets(getApplicationContext(), AppWidgetManager.getInstance(getApplicationContext()));
        if ((appWidgetIds != null) && (appWidgetIds.length > 0)) {
            // check for phantom widgets
            for (int appWidgetId : appWidgetIds) {
                // About.java will send an invalid appwidget id
                if ((appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) || Sonet.arrayContains(awi, appWidgetId)) {
                    putNewUpdate(appWidgetId, reload);
                } else {
                    // remove phantom widgets
                    getContentResolver().delete(Widgets.getContentUri(this),
                            Widgets.WIDGET + "=?",
                            new String[] { Integer.toString(appWidgetId) });
                    getContentResolver().delete(WidgetAccounts.getContentUri(this),
                            WidgetAccounts.WIDGET + "=?",
                            new String[] { Integer.toString(appWidgetId) });
                    getContentResolver().delete(Statuses.getContentUri(this),
                            Statuses.WIDGET + "=?",
                            new String[] { Integer.toString(appWidgetId) });
                }
            }
        } else if ((awi != null) && (awi.length > 0)) {
            for (int appWidgetId : awi) {
                putNewUpdate(appWidgetId, reload);
            }
        }
    }

    protected void putNewUpdate(int widget, int reload) {
        // if the widget is already loading, don't load another
        if (mStatusesLoaders.isEmpty()
                || !mStatusesLoaders.containsKey(widget)
                || (reload == 1 && mStatusesLoaders.get(widget).cancel(true))) {
            StatusesLoader loader = new StatusesLoader(this);
            mStatusesLoaders.put(widget, loader);
            loader.execute(widget, reload);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (!mStatusesLoaders.isEmpty()) {
            for (StatusesLoader statusesLoader : mStatusesLoaders.values()) {
                statusesLoader.cancel(true);
            }

            mStatusesLoaders.clear();
        }

        if (!mSMSLoaders.isEmpty()) {
            for (SMSLoader smsLoader : mSMSLoaders) {
                smsLoader.cancel(true);
            }

            mSMSLoaders.clear();
        }

        super.onDestroy();
    }

    public Cursor getSettingsCursor(int appWidgetId) {
        Cursor settings = getContentResolver().query(WidgetsSettings.getContentUri(this),
                new String[] { Widgets.INTERVAL,
                        Widgets.BACKGROUND_UPDATE },
                Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                new String[] { Integer.toString(appWidgetId),
                        Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                null);

        if (!settings.moveToFirst()) {
            settings.close();
            settings = getContentResolver().query(WidgetsSettings.getContentUri(this),
                    new String[] { Widgets.INTERVAL,
                            Widgets.BACKGROUND_UPDATE },
                    Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                    new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),
                            Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                    null);

            if (!settings.moveToFirst()) {
                initAccountSettings(this, AppWidgetManager.INVALID_APPWIDGET_ID, Sonet.INVALID_ACCOUNT_ID);
            }

            // don't insert a duplicate row
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                initAccountSettings(this, appWidgetId, Sonet.INVALID_ACCOUNT_ID);
            }
        }

        return settings;
    }

    public void buildWidgetButtons(Integer appWidgetId) {
        final String widget = Integer.toString(appWidgetId);
        // Push update for this widget to the home screen
        // wrap RemoteViews for backward compatibility
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);

        views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this,
                0,
                new Intent(this, SonetCreatePost.class)
                        .setData(Uri.withAppendedPath(Widgets.getContentUri(this), widget)),
                0));
        views.setOnClickPendingIntent(R.id.button_configure, PendingIntent
                .getActivity(this, 0, new Intent(this, About.class).setAction(widget), 0));
        views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent
                .getService(this, 0, new Intent(this, SonetService.class).setAction(widget), 0));

        final AppWidgetManager mgr = AppWidgetManager.getInstance(this);

        try {
            final Intent intent = new Intent(this, SonetRemoteViewsService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(appWidgetId, R.id.messages, intent);
            views.setEmptyView(R.id.messages, R.id.empty_messages);
            // Bind a click listener template for the contents of the message list
            final Intent onClickIntent = new Intent(this, SonetComments.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId); no longer widget specific
            onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent onClickPendingIntent = PendingIntent
                    .getActivity(this,
                            0,
                            onClickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.messages, onClickPendingIntent);
        } catch (NumberFormatException e) {
            Log.e(TAG, e.toString());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.toString());
        }

        mgr.updateAppWidget(appWidgetId, views);
        mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.messages);
    }
}
