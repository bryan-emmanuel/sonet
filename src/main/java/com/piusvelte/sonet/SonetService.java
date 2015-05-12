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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.piusvelte.eidos.Eidos;
import com.piusvelte.sonet.provider.Entities;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.StatusImages;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.WidgetAccountsView;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;
import com.piusvelte.sonet.social.Client;

import org.apache.http.client.HttpClient;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.BoundRemoteViews;

import static com.piusvelte.sonet.Sonet.ACTION_ON_CLICK;
import static com.piusvelte.sonet.Sonet.ACTION_PAGE_DOWN;
import static com.piusvelte.sonet.Sonet.ACTION_PAGE_UP;
import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.EXTRA_SCROLLABLE_VERSION;
import static com.piusvelte.sonet.Sonet.NOTIFY_ID;
import static com.piusvelte.sonet.Sonet.SMS;
import static com.piusvelte.sonet.Sonet.SMS_RECEIVED;
import static com.piusvelte.sonet.Sonet.createBackground;
import static com.piusvelte.sonet.Sonet.initAccountSettings;
import static com.piusvelte.sonet.Sonet.sBFOptions;

public class SonetService extends Service {
    private static final String TAG = "SonetService";
    private final static HashMap<Integer, AsyncTask<Integer, String, Integer>> mStatusesLoaders = new HashMap<Integer, AsyncTask<Integer, String,
            Integer>>();
    private final ArrayList<AsyncTask<SmsMessage, String, int[]>> mSMSLoaders = new ArrayList<AsyncTask<SmsMessage, String, int[]>>();
    private AlarmManager mAlarmManager;
    private ConnectivityManager mConnectivityManager;
    private String mNotify = null;

    private static Method sSetRemoteAdapter;
    private static Method sSetPendingIntentTemplate;
    private static Method sSetEmptyView;
    private static Method sNotifyAppWidgetViewDataChanged;
    private static boolean sNativeScrollingSupported = false;

    private int mStartId = Sonet.INVALID_SERVICE;

    static {
        if (Build.VERSION.SDK_INT >= 11) {
            try {
                sSetEmptyView = RemoteViews.class.getMethod("setEmptyView", new Class[] { int.class, int.class });
                sSetPendingIntentTemplate = RemoteViews.class.getMethod("setPendingIntentTemplate", new Class[] { int.class, PendingIntent.class });
                sSetRemoteAdapter = RemoteViews.class.getMethod("setRemoteAdapter", new Class[] { int.class, int.class, Intent.class });
                sNotifyAppWidgetViewDataChanged = AppWidgetManager.class
                        .getMethod("notifyAppWidgetViewDataChanged", new Class[] { int.class, int.class });
                sNativeScrollingSupported = true;
            } catch (NoSuchMethodException nsme) {
                Log.d(TAG, "native scrolling not supported: " + nsme.toString());
            }
        }
    }

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
            sp.edit().putInt(getString(R.string.key_version), currVer).commit();
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
                    putValidatedUpdates(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS), 1);
                } else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                    putValidatedUpdates(new int[] { intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) },
                            1);
                } else if (intent.getData() != null) {
                    putValidatedUpdates(new int[] { Integer.parseInt(intent.getData().getLastPathSegment()) }, 1);
                } else {
                    putValidatedUpdates(null, 0);
                }
            } else if (LauncherIntent.Action.ACTION_READY.equals(action)) {
                if (intent.hasExtra(EXTRA_SCROLLABLE_VERSION) && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                    int scrollableVersion = intent.getIntExtra(EXTRA_SCROLLABLE_VERSION, 1);
                    int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                    // check if the scrollable needs to be built
                    Cursor widget = this.getContentResolver()
                            .query(Widgets.getContentUri(SonetService.this), new String[] { Widgets._ID, Widgets.SCROLLABLE }, Widgets.WIDGET + "=?",
                                    new String[] { Integer.toString(appWidgetId) }, null);

                    if (widget.moveToFirst()) {
                        if (widget.getInt(widget.getColumnIndex(Widgets.SCROLLABLE)) < scrollableVersion) {
                            ContentValues values = new ContentValues();
                            values.put(Widgets.SCROLLABLE, scrollableVersion);
                            // set the scrollable version
                            this.getContentResolver().update(Widgets.getContentUri(SonetService.this), values, Widgets.WIDGET + "=?",
                                    new String[] { Integer.toString(appWidgetId) });
                            putValidatedUpdates(new int[] { appWidgetId }, 1);
                        } else {
                            putValidatedUpdates(new int[] { appWidgetId }, 1);
                        }
                    } else {
                        ContentValues values = new ContentValues();
                        values.put(Widgets.SCROLLABLE, scrollableVersion);
                        // set the scrollable version
                        this.getContentResolver().update(Widgets.getContentUri(SonetService.this), values, Widgets.WIDGET + "=?",
                                new String[] { Integer.toString(appWidgetId) });
                        putValidatedUpdates(new int[] { appWidgetId }, 1);
                    }

                    widget.close();
                } else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                    // requery
                    putValidatedUpdates(new int[] { intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) },
                            0);
                }
            } else if (SMS_RECEIVED.equals(action)) {
                // parse the sms, and notify any widgets which have sms enabled
                Bundle bundle = intent.getExtras();
                Object[] pdus = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdus.length; i++) {
                    SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    AsyncTask<SmsMessage, String, int[]> smsLoader = new AsyncTask<SmsMessage, String, int[]>() {

                        @Override
                        protected int[] doInBackground(SmsMessage... msg) {
                            // check if SMS is enabled anywhere
                            Cursor widgets = getContentResolver().query(WidgetAccountsView.getContentUri(SonetService.this),
                                    new String[] { WidgetAccountsView._ID, WidgetAccountsView.WIDGET, WidgetAccountsView.ACCOUNT },
                                    WidgetAccountsView.SERVICE + "=?", new String[] { Integer.toString(SMS) }, null);
                            int[] appWidgetIds = new int[widgets.getCount()];

                            if (widgets.moveToFirst()) {
                                // insert this message to the statuses db and requery scrollable/rebuild widget
                                // check if this is a contact
                                String phone = msg[0].getOriginatingAddress();
                                String friend = phone;
                                byte[] profile = null;
                                Uri content_uri = null;
                                // unknown numbers crash here in the emulator
                                Cursor phones = getContentResolver()
                                        .query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone)),
                                                new String[] { ContactsContract.PhoneLookup._ID }, null, null, null);

                                if (phones.moveToFirst()) {
                                    content_uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, phones.getLong(0));
                                } else {
                                    Cursor emails = getContentResolver()
                                            .query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(phone)),
                                                    new String[] { ContactsContract.CommonDataKinds.Email._ID }, null, null, null);

                                    if (emails.moveToFirst()) {
                                        content_uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, emails.getLong(0));
                                    }

                                    emails.close();
                                }

                                phones.close();

                                if (content_uri != null) {
                                    // load contact
                                    Cursor contacts = getContentResolver()
                                            .query(content_uri, new String[] { ContactsContract.Contacts.DISPLAY_NAME }, null, null, null);

                                    if (contacts.moveToFirst()) {
                                        friend = contacts.getString(0);
                                    }

                                    contacts.close();

                                    profile = Sonet
                                            .getBlob(Sonet
                                                    .getCircleCrop(Sonet
                                                            .getBitmap(ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(),
                                                                    content_uri))));
                                }

                                long accountId = widgets.getLong(2);
                                long id;
                                ContentValues values = new ContentValues();
                                values.put(Entities.ESID, phone);
                                values.put(Entities.FRIEND, friend);
                                values.put(Entities.PROFILE, profile);
                                values.put(Entities.ACCOUNT, accountId);
                                Cursor entity = getContentResolver().query(Entities.getContentUri(SonetService.this), new String[] { Entities._ID },
                                        Entities.ACCOUNT + "=? and " + Entities.ESID + "=?",
                                        new String[] { Long.toString(accountId), SonetCrypto.getInstance(SonetService.this).Encrypt(phone) }, null);

                                if (entity.moveToFirst()) {
                                    id = entity.getLong(0);
                                    getContentResolver().update(Entities.getContentUri(SonetService.this), values, Entities._ID + "=?",
                                            new String[] { Long.toString(id) });
                                } else {
                                    id = Long.parseLong(
                                            getContentResolver().insert(Entities.getContentUri(SonetService.this), values).getLastPathSegment());
                                }

                                entity.close();
                                values.clear();
                                Long created = msg[0].getTimestampMillis();
                                values.put(Statuses.CREATED, created);
                                values.put(Statuses.ENTITY, id);
                                values.put(Statuses.MESSAGE, msg[0].getMessageBody());
                                values.put(Statuses.SERVICE, SMS);

                                while (!widgets.isAfterLast()) {
                                    int widget = widgets.getInt(1);
                                    appWidgetIds[widgets.getPosition()] = widget;
                                    // get settings
                                    boolean time24hr = true;
                                    int status_bg_color = Sonet.default_message_bg_color;
                                    int friend_bg_color = Sonet.default_friend_bg_color;
                                    boolean icon = true;
                                    int status_count = Sonet.default_statuses_per_account;
                                    int notifications = 0;
                                    Cursor c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                                            new String[] { Widgets.TIME24HR,
                                                    Widgets.MESSAGES_BG_COLOR,
                                                    Widgets.ICON,
                                                    Widgets.STATUSES_PER_ACCOUNT,
                                                    Widgets.SOUND,
                                                    Widgets.VIBRATE,
                                                    Widgets.LIGHTS,
                                                    Widgets.PROFILES_BG_COLOR,
                                                    Widgets.FRIEND_BG_COLOR },
                                            Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                            new String[] { Integer.toString(widget),
                                                    Long.toString(accountId) },
                                            null);

                                    if (!c.moveToFirst()) {
                                        c.close();
                                        c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                                                new String[] { Widgets.TIME24HR,
                                                        Widgets.MESSAGES_BG_COLOR,
                                                        Widgets.ICON,
                                                        Widgets.STATUSES_PER_ACCOUNT,
                                                        Widgets.SOUND,
                                                        Widgets.VIBRATE,
                                                        Widgets.LIGHTS,
                                                        Widgets.PROFILES_BG_COLOR,
                                                        Widgets.FRIEND_BG_COLOR },
                                                Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                                new String[] { Integer.toString(widget),
                                                        Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                                                null);

                                        if (!c.moveToFirst()) {
                                            c.close();
                                            c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                                                    new String[] { Widgets.TIME24HR,
                                                            Widgets.MESSAGES_BG_COLOR,
                                                            Widgets.ICON,
                                                            Widgets.STATUSES_PER_ACCOUNT,
                                                            Widgets.SOUND,
                                                            Widgets.VIBRATE,
                                                            Widgets.LIGHTS,
                                                            Widgets.PROFILES_BG_COLOR,
                                                            Widgets.FRIEND_BG_COLOR },
                                                    Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                                    new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),
                                                            Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                                                    null);

                                            if (!c.moveToFirst()) {
                                                initAccountSettings(SonetService.this, AppWidgetManager.INVALID_APPWIDGET_ID,
                                                        Sonet.INVALID_ACCOUNT_ID);
                                            }

                                            if (widget != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                                initAccountSettings(SonetService.this, widget, Sonet.INVALID_ACCOUNT_ID);
                                            }
                                        }

                                        initAccountSettings(SonetService.this, widget, accountId);
                                    }

                                    if (c.moveToFirst()) {
                                        time24hr = c.getInt(0) == 1;
                                        status_bg_color = c.getInt(1);
                                        icon = c.getInt(2) == 1;
                                        status_count = c.getInt(3);

                                        if (c.getInt(4) == 1) {
                                            notifications |= Notification.DEFAULT_SOUND;
                                        }

                                        if (c.getInt(5) == 1) {
                                            notifications |= Notification.DEFAULT_VIBRATE;
                                        }

                                        if (c.getInt(6) == 1) {
                                            notifications |= Notification.DEFAULT_LIGHTS;
                                        }

                                        friend_bg_color = c.getInt(8);
                                    }

                                    c.close();
                                    values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
                                    // update the bg and icon
                                    // create the status_bg
                                    values.put(Statuses.STATUS_BG, createBackground(status_bg_color));
                                    // friend_bg
                                    values.put(Statuses.FRIEND_BG, createBackground(friend_bg_color));
                                    values.put(Statuses.ICON,
                                            icon ? Sonet.getBlob(Sonet.getBitmap(getResources(), Client.Network.Sms.getIcon())) : null);
                                    // insert the message
                                    values.put(Statuses.WIDGET, widget);
                                    values.put(Statuses.ACCOUNT, accountId);
                                    getContentResolver().insert(Statuses.getContentUri(SonetService.this), values);
                                    // check the status count, removing old sms
                                    Cursor statuses = getContentResolver()
                                            .query(Statuses.getContentUri(SonetService.this), new String[] { Statuses._ID },
                                                    Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?",
                                                    new String[] { Integer.toString(widget), Long.toString(accountId) }, Statuses.CREATED + " desc");

                                    if (statuses.moveToFirst()) {
                                        while (!statuses.isAfterLast()) {
                                            if (statuses.getPosition() >= status_count) {
                                                getContentResolver().delete(Statuses.getContentUri(SonetService.this), Statuses._ID + "=?",
                                                        new String[] { Long.toString(statuses.getLong(statuses.getColumnIndex(Statuses._ID))) });
                                            }

                                            statuses.moveToNext();
                                        }
                                    }

                                    statuses.close();

                                    if (notifications != 0) {
                                        publishProgress(Integer.toString(notifications), friend + " sent a message");
                                    }

                                    widgets.moveToNext();
                                }
                            }

                            widgets.close();
                            return appWidgetIds;
                        }

                        @Override
                        protected void onProgressUpdate(String... updates) {
                            int notifications = Integer.parseInt(updates[0]);

                            if (notifications != 0) {
                                Notification notification = new Notification(R.drawable.notification, updates[1], System.currentTimeMillis());
                                notification.setLatestEventInfo(getBaseContext(), "New messages", updates[1], PendingIntent
                                        .getActivity(SonetService.this, 0, (new Intent(SonetService.this, SonetNotifications.class)), 0));
                                notification.defaults |= notifications;
                                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
                            }
                        }

                        @Override
                        protected void onPostExecute(int[] appWidgetIds) {
                            // remove self from thread list
                            if (!mSMSLoaders.isEmpty()) {
                                mSMSLoaders.remove(this);
                            }

                            putValidatedUpdates(appWidgetIds, 0);
                        }
                    };
                    mSMSLoaders.add(smsLoader);
                    smsLoader.execute(msg);
                }
            } else if (ACTION_PAGE_DOWN.equals(action)) {
                (new PagingTask()).execute(Integer.parseInt(intent.getData().getLastPathSegment()), intent.getIntExtra(ACTION_PAGE_DOWN, 0));
            } else if (ACTION_PAGE_UP.equals(action)) {
                (new PagingTask()).execute(Integer.parseInt(intent.getData().getLastPathSegment()), intent.getIntExtra(ACTION_PAGE_UP, 0));
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

    class PagingTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... arg0) {
            boolean display_profile = true;
            boolean hasbuttons = false;
            int scrollable = 0;
            int buttons_bg_color = Sonet.default_buttons_bg_color;
            int buttons_color = Sonet.default_buttons_color;
            int buttons_textsize = Sonet.default_buttons_textsize;
            int margin = Sonet.default_margin;
            Cursor settings = getSettingsCursor(arg0[0]);

            if (settings.moveToFirst()) {
                hasbuttons = settings.getInt(0) == 1;
                buttons_color = settings.getInt(1);
                buttons_bg_color = settings.getInt(2);
                buttons_textsize = settings.getInt(3);
                scrollable = settings.getInt(4);
                display_profile = settings.getInt(5) == 1;
                margin = settings.getInt(6);
            }

            settings.close();
            // rebuild the widget, using the paging criteria passed in
            buildWidgetButtons(arg0[0], true, arg0[1], hasbuttons, scrollable, buttons_bg_color, buttons_color, buttons_textsize, display_profile,
                    margin);
            return null;
        }
    }

    protected void putValidatedUpdates(int[] appWidgetIds, int reload) {
        int[] awi = Sonet.getWidgets(getApplicationContext(), AppWidgetManager.getInstance(getApplicationContext()));
        if ((appWidgetIds != null) && (appWidgetIds.length > 0)) {
            // check for phantom widgets
            for (int appWidgetId : appWidgetIds) {
                // About.java will send an invalid appwidget id
                if ((appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) || Sonet.arrayContains(awi, appWidgetId)) {
                    putNewUpdate(appWidgetId, reload);
                } else {
                    // remove phantom widgets
                    getContentResolver()
                            .delete(Widgets.getContentUri(SonetService.this), Widgets.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
                    getContentResolver().delete(WidgetAccounts.getContentUri(SonetService.this), WidgetAccounts.WIDGET + "=?",
                            new String[] { Integer.toString(appWidgetId) });
                    getContentResolver().delete(Statuses.getContentUri(SonetService.this), Statuses.WIDGET + "=?",
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
        if (mStatusesLoaders.isEmpty() || !mStatusesLoaders.containsKey(widget) || ((reload == 1) && (mStatusesLoaders.get(widget).cancel(true)))) {
            StatusesLoader loader = new StatusesLoader();
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
            Iterator<AsyncTask<Integer, String, Integer>> itr = mStatusesLoaders.values().iterator();
            while (itr.hasNext()) {
                AsyncTask<Integer, String, Integer> statusesLoader = itr.next();
                statusesLoader.cancel(true);
            }
            mStatusesLoaders.clear();
        }
        if (!mSMSLoaders.isEmpty()) {
            Iterator<AsyncTask<SmsMessage, String, int[]>> itr = mSMSLoaders.iterator();
            while (itr.hasNext()) {
                AsyncTask<SmsMessage, String, int[]> smsLoader = itr.next();
                smsLoader.cancel(true);
            }
            mSMSLoaders.clear();
        }
        super.onDestroy();
    }

    private Cursor getSettingsCursor(int appWidgetId) {
        Cursor settings = getContentResolver().query(WidgetsSettings.getContentUri(this),
                new String[] { Widgets.HASBUTTONS, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_TEXTSIZE, Widgets.SCROLLABLE,
                        Widgets.DISPLAY_PROFILE, Widgets.MARGIN, Widgets.INTERVAL, Widgets.BACKGROUND_UPDATE },
                Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                new String[] { Integer.toString(appWidgetId), Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);
        if (!settings.moveToFirst()) {
            settings.close();
            settings = getContentResolver().query(WidgetsSettings.getContentUri(this),
                    new String[] { Widgets.HASBUTTONS, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_TEXTSIZE, Widgets
                            .SCROLLABLE, Widgets.DISPLAY_PROFILE, Widgets.MARGIN, Widgets.INTERVAL, Widgets.BACKGROUND_UPDATE },
                    Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                    new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);
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

    class StatusesLoader extends AsyncTask<Integer, String, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            // first handle deletes, then scroll updates, finally regular updates
            final int appWidgetId = params[0];
            final String widget = Integer.toString(appWidgetId);
            final boolean reload = params[1] != 0;

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "StatusesLoader;widget:" + widget + ",reload:" + reload);
            }

            int refreshInterval = Sonet.default_interval;
            boolean backgroundUpdate = true;
            boolean display_profile = true;
            boolean hasbuttons = false;
            int scrollable = 0;
            int buttons_bg_color = Sonet.default_buttons_bg_color;
            int buttons_color = Sonet.default_buttons_color;
            int buttons_textsize = Sonet.default_buttons_textsize;
            int margin = Sonet.default_margin;
            Cursor settings = getSettingsCursor(appWidgetId);

            if (settings.moveToFirst()) {
                hasbuttons = settings.getInt(0) == 1;
                buttons_color = settings.getInt(1);
                buttons_bg_color = settings.getInt(2);
                buttons_textsize = settings.getInt(3);
                scrollable = settings.getInt(4);
                display_profile = settings.getInt(5) == 1;
                margin = settings.getInt(6);
                refreshInterval = settings.getInt(7);
                backgroundUpdate = settings.getInt(8) == 1;
            }

            settings.close();
            // the widget will start out as the default widget.xml, which simply says "loading..."
            // if there's a cache, that should be quickly reloaded while new updates come down
            // otherwise, replace the widget with "loading..."
            // clear the messages
            getContentResolver().delete(Statuses.getContentUri(SonetService.this),
                    Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?",
                    new String[] { widget,
                            Long.toString(Sonet.INVALID_ACCOUNT_ID) });
            Cursor statuses = getContentResolver().query(Statuses.getContentUri(SonetService.this),
                    new String[] { Statuses._ID },
                    Statuses.WIDGET + "=?",
                    new String[] { widget },
                    null);
            boolean hasCache = statuses.moveToFirst();
            statuses.close();

            // the alarm should always be set, rather than depend on the tasks to complete
            //			Log.d(TAG,"awi:"+appWidgetId+",hasCache:"+hasCache+",reload:"+reload+",refreshInterval:"+refreshInterval);
            if ((appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) && (!hasCache || reload) && (refreshInterval > 0)) {
                mAlarmManager.cancel(PendingIntent
                        .getService(SonetService.this, 0, new Intent(SonetService.this, SonetService.class).setAction(widget), 0));
                mAlarmManager.set(backgroundUpdate ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC, System.currentTimeMillis() + refreshInterval,
                        PendingIntent.getService(SonetService.this, 0, new Intent(SonetService.this, SonetService.class)
                                .setData(Uri.withAppendedPath(Widgets.getContentUri(SonetService.this), widget)).setAction(ACTION_REFRESH), 0));
                //				Log.d(TAG,"alarm set");
            }

            // get the accounts
            Cursor accounts = getContentResolver().query(WidgetAccountsView.getContentUri(SonetService.this),
                    new String[] { WidgetAccountsView.ACCOUNT,
                            WidgetAccountsView.TOKEN,
                            WidgetAccountsView.SECRET,
                            WidgetAccountsView.SERVICE,
                            WidgetAccountsView.SID },
                    WidgetAccountsView.WIDGET + "=?",
                    new String[] { widget },
                    null);

            if (hasCache && accounts.moveToFirst()) {
                //				Log.d(TAG,"update cache styles");
                // update the styles for existing statuses while fetching new statuses
                while (!accounts.isAfterLast()) {
                    long account = accounts.getLong(0);
                    int service = accounts.getInt(3);
                    int status_bg_color = Sonet.default_message_bg_color;
                    int profile_bg_color = Sonet.default_message_bg_color;
                    int friend_bg_color = Sonet.default_friend_bg_color;
                    boolean icon = true;
                    Cursor c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                            new String[] { Widgets.MESSAGES_BG_COLOR,
                                    Widgets.ICON,
                                    Widgets.PROFILES_BG_COLOR,
                                    Widgets.FRIEND_BG_COLOR },
                            Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                            new String[] { widget,
                                    Long.toString(account) },
                            null);

                    if (!c.moveToFirst()) {
                        // no account settings
                        c.close();
                        c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                                new String[] { Widgets.MESSAGES_BG_COLOR,
                                        Widgets.ICON,
                                        Widgets.PROFILES_BG_COLOR,
                                        Widgets.FRIEND_BG_COLOR },
                                Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                new String[] { widget,
                                        Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                                null);

                        if (!c.moveToFirst()) {
                            // no widget settings
                            c.close();
                            c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                                    new String[] { Widgets.MESSAGES_BG_COLOR,
                                            Widgets.ICON,
                                            Widgets.PROFILES_BG_COLOR,
                                            Widgets.FRIEND_BG_COLOR },
                                    Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                    new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),
                                            Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                                    null);

                            if (!c.moveToFirst()) {
                                initAccountSettings(SonetService.this, AppWidgetManager.INVALID_APPWIDGET_ID, Sonet.INVALID_ACCOUNT_ID);
                            }

                            // don't insert a duplicate row
                            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                initAccountSettings(SonetService.this, appWidgetId, Sonet.INVALID_ACCOUNT_ID);
                            }
                        }

                        initAccountSettings(SonetService.this, appWidgetId, account);
                    }

                    if (c.moveToFirst()) {
                        status_bg_color = c.getInt(0);
                        icon = c.getInt(1) == 1;
                        profile_bg_color = c.getInt(2);
                        friend_bg_color = c.getInt(3);
                    }

                    c.close();
                    // update the bg and icon
                    // create the status_bg
                    ContentValues values = new ContentValues();
                    values.put(Statuses.STATUS_BG, createBackground(status_bg_color));
                    // friend_bg
                    values.put(Statuses.FRIEND_BG, createBackground(friend_bg_color));
                    // icon
                    values.put(Statuses.ICON, icon ? Sonet.getBlob(Sonet.getBitmap(getResources(), Client.Network.get(service).getIcon())) : null);
                    getContentResolver().update(Statuses.getContentUri(SonetService.this),
                            values,
                            Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?",
                            new String[] { widget,
                                    Integer.toString(service),
                                    Long.toString(account) });
                    accounts.moveToNext();
                }
            } else {
                // if no cache inform the user that the widget is loading
                addStatusItem(widget, getString(R.string.updating), appWidgetId);
            }

            // loading takes time, so don't leave an empty widget sitting there
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                // build the widget
                //				Log.d(TAG,"temp widget build");
                buildWidgetButtons(appWidgetId, false, 0, hasbuttons, scrollable, buttons_bg_color, buttons_color, buttons_textsize, display_profile,
                        margin);
            } else {
                // update the About.java for in-app viewing
                //				Log.d(TAG,"temp About build");
                getContentResolver().notifyChange(StatusesStyles.getContentUri(SonetService.this), null);
            }

            if (accounts.moveToFirst()) {
                // only reload if the token's can be decrypted and if there's no cache or a reload is requested
                if (!hasCache || reload) {
                    mNotify = null;
                    int notifications = 0;

                    // load the updates
                    while (!accounts.isAfterLast()) {
                        HttpClient httpClient = SonetHttpClient.getThreadSafeClient(getApplicationContext());
                        long account = accounts.getLong(0);
                        int service = accounts.getInt(3);
                        // get the settings form time24hr and bg_color
                        boolean time24hr = false;
                        int status_bg_color = Sonet.default_message_bg_color;
                        int profile_bg_color = Sonet.default_message_bg_color;
                        int friend_bg_color = Sonet.default_friend_bg_color;
                        boolean icon = true;
                        int status_count = Sonet.default_statuses_per_account;
                        Cursor c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                                new String[] { Widgets.TIME24HR,
                                        Widgets.MESSAGES_BG_COLOR,
                                        Widgets.ICON,
                                        Widgets.STATUSES_PER_ACCOUNT,
                                        Widgets.SOUND,
                                        Widgets.VIBRATE,
                                        Widgets.LIGHTS,
                                        Widgets.PROFILES_BG_COLOR,
                                        Widgets.FRIEND_BG_COLOR },
                                Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                new String[] { widget,
                                        Long.toString(account) },
                                null);

                        if (!c.moveToFirst()) {
                            // no account settings
                            c.close();
                            c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                                    new String[] { Widgets.TIME24HR,
                                            Widgets.MESSAGES_BG_COLOR,
                                            Widgets.ICON,
                                            Widgets.STATUSES_PER_ACCOUNT,
                                            Widgets.SOUND,
                                            Widgets.VIBRATE,
                                            Widgets.LIGHTS,
                                            Widgets.PROFILES_BG_COLOR,
                                            Widgets.FRIEND_BG_COLOR },
                                    Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                    new String[] { widget,
                                            Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                                    null);

                            if (!c.moveToFirst()) {
                                // no widget settings
                                c.close();
                                c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                                        new String[] { Widgets.TIME24HR,
                                                Widgets.MESSAGES_BG_COLOR,
                                                Widgets.ICON,
                                                Widgets.STATUSES_PER_ACCOUNT,
                                                Widgets.SOUND,
                                                Widgets.VIBRATE,
                                                Widgets.LIGHTS,
                                                Widgets.PROFILES_BG_COLOR,
                                                Widgets.FRIEND_BG_COLOR },
                                        Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                                        new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),
                                                Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                                        null);

                                if (!c.moveToFirst()) {
                                    initAccountSettings(SonetService.this, AppWidgetManager.INVALID_APPWIDGET_ID, Sonet.INVALID_ACCOUNT_ID);
                                }

                                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                    initAccountSettings(SonetService.this, appWidgetId, Sonet.INVALID_ACCOUNT_ID);
                                }
                            }

                            initAccountSettings(SonetService.this, appWidgetId, account);
                        }

                        if (c.moveToFirst()) {
                            time24hr = c.getInt(0) == 1;
                            status_bg_color = c.getInt(1);
                            icon = c.getInt(2) == 1;
                            status_count = c.getInt(3);

                            if (c.getInt(4) == 1) {
                                notifications |= Notification.DEFAULT_SOUND;
                            }

                            if (c.getInt(5) == 1) {
                                notifications |= Notification.DEFAULT_VIBRATE;
                            }

                            if (c.getInt(6) == 1) {
                                notifications |= Notification.DEFAULT_LIGHTS;
                            }

                            profile_bg_color = c.getInt(7);
                            friend_bg_color = c.getInt(8);
                        }

                        c.close();

                        // if no connection, only update the status_bg and icons
                        if ((mConnectivityManager.getActiveNetworkInfo() != null) && mConnectivityManager.getActiveNetworkInfo().isConnected()) {
                            SonetCrypto sonetCrypto = SonetCrypto.getInstance(SonetService.this);
                            String token = sonetCrypto.Decrypt(accounts.getString(1));
                            String secret = sonetCrypto.Decrypt(accounts.getString(2));
                            String accountEsid = sonetCrypto.Decrypt(accounts.getString(4));

                            Client client = new Client.Builder(SonetService.this)
                                    .setNetwork(service)
                                    .setAccount(accountEsid)
                                    .setCredentials(token, secret)
                                    .build();

                            String notificationMessage = client
                                    .getFeed(appWidgetId, widget, account, status_count, time24hr, display_profile, notifications, httpClient);

                            if (TextUtils.isEmpty(mNotify)) {
                                mNotify = notificationMessage;
                            } else if (!TextUtils.isEmpty(notificationMessage)) {
                                mNotify = getString(R.string.notify_multiple_updates);
                            }

                            // remove old notifications
                            getContentResolver().delete(Notifications.getContentUri(SonetService.this),
                                    Notifications.CLEARED + "=1 and " + Notifications.ACCOUNT + "=? and " + Notifications.CREATED + "<?",
                                    new String[] { Long.toString(account), Long.toString(System.currentTimeMillis() - 86400000) });
                        } else {
                            // no network connection
                            if (hasCache) {
                                // update created text
                                updateCreatedText(widget, Long.toString(account), time24hr);
                            } else {
                                // clear the "loading" message and display "no connection"
                                getContentResolver()
                                        .delete(Statuses.getContentUri(SonetService.this), Statuses.WIDGET + "=?", new String[] { widget });
                                addStatusItem(widget, getString(R.string.no_connection), appWidgetId);
                            }
                        }

                        // update the bg and icon
                        // create the status_bg
                        ContentValues values = new ContentValues();
                        values.put(Statuses.STATUS_BG, createBackground(status_bg_color));
                        // friend_bg
                        values.put(Statuses.FRIEND_BG, createBackground(friend_bg_color));
                        // icon
                        values.put(Statuses.ICON,
                                icon ? Sonet.getBlob(Sonet.getBitmap(getResources(), Client.Network.get(service).getIcon())) : null);
                        getContentResolver().update(Statuses.getContentUri(SonetService.this), values,
                                Statuses.WIDGET + "=? and " + Statuses.SERVICE + "=? and " + Statuses.ACCOUNT + "=?",
                                new String[] { widget, Integer.toString(service), Long.toString(account) });
                        accounts.moveToNext();
                    }

                    if ((notifications != 0) && (mNotify != null)) {
                        publishProgress(Integer.toString(notifications));
                    }
                }

                // delete the existing loading and informational messages
                getContentResolver().delete(Statuses.getContentUri(SonetService.this), Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?",
                        new String[] { widget, Long.toString(Sonet.INVALID_ACCOUNT_ID) });
                // check statuses again
                Cursor statusesCheck = getContentResolver()
                        .query(Statuses.getContentUri(SonetService.this), new String[] { Statuses._ID }, Statuses.WIDGET + "=?",
                                new String[] { widget }, null);
                hasCache = statusesCheck.moveToFirst();
                statusesCheck.close();

                if (!hasCache) {
                    // there should be a loading message displaying
                    // if no updates have been loaded, display "no updates"
                    addStatusItem(widget, getString(R.string.no_updates), appWidgetId);
                }
            } else {
                // no accounts, clear cache
                getContentResolver().delete(Statuses.getContentUri(SonetService.this), Statuses.WIDGET + "=?", new String[] { widget });
                // insert no accounts message
                addStatusItem(widget, getString(R.string.no_accounts), appWidgetId);
            }

            accounts.close();

            // always update buttons, if !scrollable update widget both times, otherwise build scrollable first, requery second
            // see if the tasks are finished
            // non-scrollable widgets will be completely rebuilt, while scrollable widgets while be notified to requery
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                //				Log.d(TAG,"full widget build");
                buildWidgetButtons(appWidgetId, true, 0, hasbuttons, scrollable, buttons_bg_color, buttons_color, buttons_textsize, display_profile,
                        margin);
            } else {
                //				Log.d(TAG,"full About build");
                // notify change to About.java
                getContentResolver().notifyChange(StatusesStyles.getContentUri(SonetService.this), null);
            }

            return appWidgetId;
        }

        @Override
        protected void onCancelled(Integer appWidgetId) {
            //			Log.d(TAG,"loader cancelled");
        }

        @Override
        protected void onProgressUpdate(String... updates) {
            int notifications = Integer.parseInt(updates[0]);

            if (notifications != 0) {
                Notification notification = new Notification(R.drawable.notification, mNotify, System.currentTimeMillis());
                notification.setLatestEventInfo(getBaseContext(), "New messages", mNotify, PendingIntent.getActivity(SonetService.this, 0,
                        (new Intent(SonetService.this, SonetNotifications.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), 0));
                notification.defaults |= notifications;
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
            }
        }

        @Override
        protected void onPostExecute(Integer appWidgetId) {
            // remove self from thread list
            if (!mStatusesLoaders.isEmpty() && mStatusesLoaders.containsKey(appWidgetId)) {
                mStatusesLoaders.remove(appWidgetId);
            }

            //			Log.d(TAG,"finished update, check queue");
            if (mStatusesLoaders.isEmpty()) {
                //				Log.d(TAG,"stop service");
                Sonet.release();
                stopSelfResult(mStartId);
            }
        }

        private void addStatusItem(String widget, String message, int appWidgetId) {
            int status_bg_color = Sonet.default_message_bg_color;
            int profile_bg_color = Sonet.default_message_bg_color;
            int friend_bg_color = Sonet.default_friend_bg_color;
            boolean icon = true;
            Cursor c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                    new String[] { Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SOUND, Widgets
                            .VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR },
                    Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[] { widget, Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);

            if (!c.moveToFirst()) {
                // no widget settings
                c.close();
                c = getContentResolver().query(WidgetsSettings.getContentUri(SonetService.this),
                        new String[] { Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SOUND,
                                Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR },
                        Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                        new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);
            }

            if (c.moveToFirst()) {
                status_bg_color = c.getInt(1);
                icon = c.getInt(2) == 1;
                profile_bg_color = c.getInt(7);
                friend_bg_color = c.getInt(8);
            }

            c.close();
            long id;
            long created = System.currentTimeMillis();
            int service = 0;
            boolean time24hr = false;
            long accountId = Sonet.INVALID_ACCOUNT_ID;
            String sid = "-1";
            String esid = "";
            String friend = getString(R.string.app_name);
            byte[] profile = Sonet.getBlob(Sonet.getCircleCrop(Sonet.getBitmap(getResources(), R.drawable.icon)));
            Cursor entity = getContentResolver().query(Entities.getContentUri(SonetService.this), new String[] { Entities._ID },
                    Entities.ACCOUNT + "=? and " + Entities.ESID + "=?", new String[] { Long.toString(accountId), esid }, null);

            if (entity.moveToFirst()) {
                id = entity.getLong(0);
            } else {
                ContentValues entityValues = new ContentValues();
                entityValues.put(Entities.ESID, esid);
                entityValues.put(Entities.FRIEND, friend);
                entityValues.put(Entities.PROFILE, profile);
                entityValues.put(Entities.ACCOUNT, accountId);
                id = Long.parseLong(getContentResolver().insert(Entities.getContentUri(SonetService.this), entityValues).getLastPathSegment());
            }

            entity.close();
            ContentValues values = new ContentValues();
            values.put(Statuses.CREATED, created);
            values.put(Statuses.ENTITY, id);
            values.put(Statuses.MESSAGE, message);
            values.put(Statuses.SERVICE, service);
            values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(created, time24hr));
            values.put(Statuses.WIDGET, appWidgetId);
            values.put(Statuses.ACCOUNT, accountId);
            values.put(Statuses.SID, sid);
            values.put(Statuses.FRIEND_OVERRIDE, friend);
            values.put(Statuses.STATUS_BG, createBackground(status_bg_color));
            values.put(Statuses.FRIEND_BG, createBackground(friend_bg_color));
            Bitmap emptyBmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
            ByteArrayOutputStream imageBgStream = new ByteArrayOutputStream();
            emptyBmp.compress(Bitmap.CompressFormat.PNG, 100, imageBgStream);
            byte[] emptyImg = imageBgStream.toByteArray();
            emptyBmp.recycle();

            if (icon && (emptyImg != null)) {
                values.put(Statuses.ICON, emptyImg);
            }

            long statusId = Long.parseLong(getContentResolver().insert(Statuses.getContentUri(SonetService.this), values).getLastPathSegment());

            // remote views can be reused, avoid images being repeated across multiple statuses
            if (emptyImg != null) {
                ContentValues imageValues = new ContentValues();
                imageValues.put(StatusImages.STATUS_ID, statusId);
                imageValues.put(StatusImages.IMAGE, emptyImg);
                imageValues.put(StatusImages.IMAGE_BG, emptyImg);
                getContentResolver().insert(StatusImages.getContentUri(SonetService.this), imageValues);
            }
        }

        private boolean updateCreatedText(String widget, String account, boolean time24hr) {
            boolean statuses_updated = false;
            Cursor statuses = getContentResolver().query(Statuses.getContentUri(SonetService.this), new String[] { Statuses._ID, Statuses.CREATED },
                    Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[] { widget, account }, null);
            if (statuses.moveToFirst()) {
                while (!statuses.isAfterLast()) {
                    ContentValues values = new ContentValues();
                    values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(1), time24hr));
                    getContentResolver().update(Statuses.getContentUri(SonetService.this), values, Statuses._ID + "=?",
                            new String[] { Long.toString(statuses.getLong(0)) });
                    statuses.moveToNext();
                }
                statuses_updated = true;
            }
            statuses.close();
            return statuses_updated;
        }
    }

    private void buildWidgetButtons(Integer appWidgetId,
            boolean updatesReady,
            int page,
            boolean hasbuttons,
            int scrollable,
            int buttons_bg_color,
            int buttons_color,
            int buttons_textsize,
            boolean display_profile,
            int margin) {
        final String widget = Integer.toString(appWidgetId);
        // Push update for this widget to the home screen
        int layout;

        if (hasbuttons) {
            if (sNativeScrollingSupported) {
                if (margin > 0) {
                    layout = R.layout.widget_margin_scrollable;
                } else {
                    layout = R.layout.widget_scrollable;
                }
            } else if (display_profile) {
                if (margin > 0) {
                    layout = R.layout.widget_margin;
                } else {
                    layout = R.layout.widget;
                }
            } else {
                if (margin > 0) {
                    layout = R.layout.widget_noprofile_margin;
                } else {
                    layout = R.layout.widget_noprofile;
                }
            }
        } else {
            if (sNativeScrollingSupported) {
                if (margin > 0) {
                    layout = R.layout.widget_nobuttons_margin_scrollable;
                } else {
                    layout = R.layout.widget_nobuttons_scrollable;
                }
            } else if (display_profile) {
                if (margin > 0) {
                    layout = R.layout.widget_nobuttons_margin;
                } else {
                    layout = R.layout.widget_nobuttons;
                }
            } else {
                if (margin > 0) {
                    layout = R.layout.widget_nobuttons_noprofile_margin;
                } else {
                    layout = R.layout.widget_nobuttons_noprofile;
                }
            }
        }

        // wrap RemoteViews for backward compatibility
        RemoteViews views = new RemoteViews(getPackageName(), layout);

        if (hasbuttons) {
            Bitmap buttons_bg = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
            Canvas buttons_bg_canvas = new Canvas(buttons_bg);
            buttons_bg_canvas.drawColor(buttons_bg_color);
            views.setImageViewBitmap(R.id.buttons_bg, buttons_bg);
            views.setTextColor(R.id.buttons_bg_clear, buttons_bg_color);
            views.setFloat(R.id.buttons_bg_clear, "setTextSize", buttons_textsize);
            views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(SonetService.this, 0,
                    new Intent(SonetService.this, SonetCreatePost.class).setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
                            .setData(Uri.withAppendedPath(Widgets.getContentUri(SonetService.this), widget)), 0));
            views.setTextColor(R.id.button_post, buttons_color);
            views.setFloat(R.id.button_post, "setTextSize", buttons_textsize);
            views.setOnClickPendingIntent(R.id.button_configure, PendingIntent
                    .getActivity(SonetService.this, 0, new Intent(SonetService.this, ManageAccounts.class).setAction(widget), 0));
            views.setTextColor(R.id.button_configure, buttons_color);
            views.setFloat(R.id.button_configure, "setTextSize", buttons_textsize);
            views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent
                    .getService(SonetService.this, 0, new Intent(SonetService.this, SonetService.class).setAction(widget), 0));
            views.setTextColor(R.id.button_refresh, buttons_color);
            views.setFloat(R.id.button_refresh, "setTextSize", buttons_textsize);
            views.setTextColor(R.id.page_up, buttons_color);
            views.setFloat(R.id.page_up, "setTextSize", buttons_textsize);
            views.setTextColor(R.id.page_down, buttons_color);
            views.setFloat(R.id.page_down, "setTextSize", buttons_textsize);
        }
        // set margin
        if (scrollable == 0) {
            final AppWidgetManager mgr = AppWidgetManager.getInstance(SonetService.this);
            // check if native scrolling is supported
            if (sNativeScrollingSupported) {
                // native scrolling
                try {
                    final Intent intent = SonetRemoteViewsServiceWrapper.getRemoteAdapterIntent(SonetService.this);
                    if (intent != null) {
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        intent.putExtra(Widgets.DISPLAY_PROFILE, display_profile);
                        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
                        sSetRemoteAdapter.invoke(views, appWidgetId, R.id.messages, intent);
                        // empty
                        sSetEmptyView.invoke(views, R.id.messages, R.id.empty_messages);
                        // onclick
                        // Bind a click listener template for the contents of the message list
                        final Intent onClickIntent = new Intent(SonetService.this, SonetWidget.class);
                        onClickIntent.setAction(ACTION_ON_CLICK);
                        onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
                        final PendingIntent onClickPendingIntent = PendingIntent
                                .getBroadcast(SonetService.this, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        sSetPendingIntentTemplate.invoke(views, R.id.messages, onClickPendingIntent);
                    } else {
                        // fallback on non-scrolling widget
                        sNativeScrollingSupported = false;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.toString());
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, e.toString());
                } catch (IllegalAccessException e) {
                    Log.e(TAG, e.toString());
                } catch (InvocationTargetException e) {
                    Log.e(TAG, e.toString());
                }
            }
            if (!sNativeScrollingSupported) {
                Cursor statuses_styles = getContentResolver().query(Uri.withAppendedPath(StatusesStyles.getContentUri(SonetService.this), widget),
                        new String[] { StatusesStyles._ID,
                                StatusesStyles.FRIEND,
                                StatusesStyles.PROFILE,
                                StatusesStyles.MESSAGE,
                                StatusesStyles.CREATEDTEXT,
                                StatusesStyles.MESSAGES_COLOR,
                                StatusesStyles.FRIEND_COLOR,
                                StatusesStyles.CREATED_COLOR,
                                StatusesStyles.MESSAGES_TEXTSIZE,
                                StatusesStyles.FRIEND_TEXTSIZE,
                                StatusesStyles.CREATED_TEXTSIZE,
                                StatusesStyles.STATUS_BG,
                                StatusesStyles.ICON,
                                StatusesStyles.IMAGE },
                        null, null, StatusesStyles.CREATED + " DESC LIMIT " + page + ",-1");
                if (statuses_styles.moveToFirst()) {
                    int count_status = 0;
                    views.removeAllViews(R.id.messages);
                    while (!statuses_styles.isAfterLast() && (count_status < 16)) {
                        int friend_color = statuses_styles.getInt(6),
                                created_color = statuses_styles.getInt(7),
                                friend_textsize = statuses_styles.getInt(9),
                                created_textsize = statuses_styles.getInt(10),
                                messages_color = statuses_styles.getInt(5),
                                messages_textsize = statuses_styles.getInt(8);
                        // get the item wrapper
                        RemoteViews itemView;
                        if (display_profile) {
                            itemView = new RemoteViews(getPackageName(), R.layout.widget_item);
                            byte[] profile = statuses_styles.getBlob(2);

                            if (profile != null) {
                                Bitmap profilebmp = BitmapFactory.decodeByteArray(profile, 0, profile.length, sBFOptions);
                                if (profilebmp != null) {
                                    itemView.setImageViewBitmap(R.id.profile, profilebmp);
                                }
                            }
                        } else {
                            itemView = new RemoteViews(getPackageName(), R.layout.widget_item_noprofile);
                        }

                        // set messages background
                        byte[] status_bg = statuses_styles.getBlob(11);
                        if (status_bg != null) {
                            Bitmap status_bgbmp = BitmapFactory.decodeByteArray(status_bg, 0, status_bg.length, sBFOptions);
                            if (status_bgbmp != null) {
                                itemView.setImageViewBitmap(R.id.status_bg, status_bgbmp);
                            }
                        }
                        // set an image
                        byte[] image = statuses_styles.getBlob(13);

                        if (image != null) {
                            Bitmap imageBmp = BitmapFactory.decodeByteArray(image, 0, image.length, sBFOptions);
                            itemView.setImageViewBitmap(R.id.image, imageBmp);
                        }

                        itemView.setTextViewText(R.id.message, statuses_styles.getString(3));
                        itemView.setTextColor(R.id.message, messages_color);
                        itemView.setFloat(R.id.message, "setTextSize", messages_textsize);
                        itemView.setOnClickPendingIntent(R.id.item, PendingIntent.getActivity(SonetService.this, 0,
                                new Intent(SonetService.this, StatusDialog.class).setData(
                                        Uri.withAppendedPath(StatusesStyles.getContentUri(SonetService.this),
                                                Long.toString(statuses_styles.getLong(0)))), 0));
                        itemView.setTextViewText(R.id.friend, statuses_styles.getString(1));
                        itemView.setTextColor(R.id.friend, friend_color);
                        itemView.setFloat(R.id.friend, "setTextSize", friend_textsize);
                        itemView.setTextViewText(R.id.created, statuses_styles.getString(4));
                        itemView.setTextColor(R.id.created, created_color);
                        itemView.setFloat(R.id.created, "setTextSize", created_textsize);
                        // set icons
                        byte[] icon = statuses_styles.getBlob(12);
                        if (icon != null) {
                            Bitmap iconbmp = BitmapFactory.decodeByteArray(icon, 0, icon.length, sBFOptions);
                            if (iconbmp != null) {
                                itemView.setImageViewBitmap(R.id.icon, iconbmp);
                            }
                        }
                        views.addView(R.id.messages, itemView);
                        count_status++;
                        statuses_styles.moveToNext();
                    }
                    if (hasbuttons && (page < statuses_styles.getCount())) {
                        // there are more statuses to show, allow paging down
                        views.setOnClickPendingIntent(R.id.page_down, PendingIntent.getService(SonetService.this, 0,
                                new Intent(SonetService.this, SonetService.class).setAction(ACTION_PAGE_DOWN)
                                        .setData(Uri.withAppendedPath(Widgets.getContentUri(SonetService.this), widget))
                                        .putExtra(ACTION_PAGE_DOWN, page + 1), PendingIntent.FLAG_UPDATE_CURRENT));
                    }
                }
                statuses_styles.close();
                if (hasbuttons && (page > 0)) {
                    views.setOnClickPendingIntent(R.id.page_up, PendingIntent.getService(SonetService.this, 0,
                            new Intent(SonetService.this, SonetService.class).setAction(ACTION_PAGE_UP)
                                    .setData(Uri.withAppendedPath(Widgets.getContentUri(SonetService.this), widget))
                                    .putExtra(ACTION_PAGE_UP, page - 1), PendingIntent.FLAG_UPDATE_CURRENT));
                }
            }
            Log.d(TAG, "update native widget: " + appWidgetId);
            mgr.updateAppWidget(appWidgetId, views);
            if (sNativeScrollingSupported) {
                Log.d(TAG, "trigger widget query: " + appWidgetId);
                try {
                    // trigger query
                    sNotifyAppWidgetViewDataChanged.invoke(mgr, appWidgetId, R.id.messages);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.toString());
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, e.toString());
                } catch (IllegalAccessException e) {
                    Log.e(TAG, e.toString());
                } catch (InvocationTargetException e) {
                    Log.e(TAG, e.toString());
                }
            }
        } else if (updatesReady) {
            //			Log.d(TAG, "notify updatesReady");
            getContentResolver().notifyChange(StatusesStyles.getContentUri(SonetService.this), null);
        } else {
            AppWidgetManager.getInstance(SonetService.this).updateAppWidget(Integer.parseInt(widget), views);
            buildScrollableWidget(appWidgetId, scrollable, display_profile);
        }
    }

    private void buildScrollableWidget(Integer appWidgetId, int scrollableVersion, boolean display_profile) {
        // set widget as scrollable
        Intent replaceDummy = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);
        replaceDummy.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.messages);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID, R.layout.widget_listview);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);

        //provider
        Uri uri = Uri.withAppendedPath(StatusesStyles.getContentUri(SonetService.this), Integer.toString(appWidgetId));
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, uri.toString());
        String[] projection;

        if (display_profile) {
            projection = new String[] { StatusesStyles._ID,
                    StatusesStyles.FRIEND,
                    StatusesStyles.PROFILE,
                    StatusesStyles.MESSAGE,
                    StatusesStyles.CREATEDTEXT,
                    StatusesStyles.MESSAGES_COLOR,
                    StatusesStyles.FRIEND_COLOR,
                    StatusesStyles.CREATED_COLOR,
                    StatusesStyles.MESSAGES_TEXTSIZE,
                    StatusesStyles.FRIEND_TEXTSIZE,
                    StatusesStyles.CREATED_TEXTSIZE,
                    StatusesStyles.STATUS_BG,
                    StatusesStyles.ICON,
                    StatusesStyles.IMAGE };
        } else {
            projection = new String[] { StatusesStyles._ID,
                    StatusesStyles.FRIEND,
                    StatusesStyles.PROFILE,
                    StatusesStyles.MESSAGE,
                    StatusesStyles.CREATEDTEXT,
                    StatusesStyles.MESSAGES_COLOR,
                    StatusesStyles.FRIEND_COLOR,
                    StatusesStyles.CREATED_COLOR,
                    StatusesStyles.MESSAGES_TEXTSIZE,
                    StatusesStyles.FRIEND_TEXTSIZE,
                    StatusesStyles.CREATED_TEXTSIZE,
                    StatusesStyles.STATUS_BG,
                    StatusesStyles.ICON,
                    StatusesStyles.IMAGE };
        }

        String sortOrder = StatusesStyles.CREATED + " DESC";
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, projection);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, sortOrder);
        String whereClause = StatusesStyles.WIDGET + "=?";
        String[] selectionArgs = new String[] { Integer.toString(appWidgetId) };
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_ACTION_VIEW_URI_INDEX, SonetProvider.StatusesStylesColumns._id.ordinal());

        switch (scrollableVersion) {
            case 1:
                if (display_profile) {
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.widget_item);
                    int[] cursorIndices = new int[] { SonetProvider.StatusesStylesColumns.status_bg.ordinal(),
                            SonetProvider.StatusesStylesColumns.profile.ordinal(),
                            SonetProvider.StatusesStylesColumns.friend.ordinal(),
                            SonetProvider.StatusesStylesColumns.createdtext.ordinal(),
                            SonetProvider.StatusesStylesColumns.message.ordinal(),
                            SonetProvider.StatusesStylesColumns.icon.ordinal(),
                            SonetProvider.StatusesStylesColumns.image.ordinal() };
                    int[] viewTypes = new int[] { LauncherIntent.Extra.Scroll.Types.IMAGEBLOB,
                            LauncherIntent.Extra.Scroll.Types.IMAGEBLOB,
                            LauncherIntent.Extra.Scroll.Types.TEXTVIEW,
                            LauncherIntent.Extra.Scroll.Types.TEXTVIEW,
                            LauncherIntent.Extra.Scroll.Types.TEXTVIEW,
                            LauncherIntent.Extra.Scroll.Types.IMAGEBLOB,
                            LauncherIntent.Extra.Scroll.Types.IMAGEBLOB };
                    int[] layoutIds = new int[] { R.id.status_bg,
                            R.id.profile,
                            R.id.friend,
                            R.id.created,
                            R.id.message,
                            R.id.icon,
                            R.id.image };
                    int[] defaultResource = new int[] { 0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0 };
                    boolean[] clickable = new boolean[] { true,
                            false,
                            false,
                            false,
                            false,
                            false,
                            false };
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_DEFAULT_RESOURCES, defaultResource);
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
                } else {
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.widget_item_noprofile);
                    int[] cursorIndices = new int[] { SonetProvider.StatusesStylesColumnsNoProfile.status_bg.ordinal(),
                            SonetProvider.StatusesStylesColumnsNoProfile.friend.ordinal(),
                            SonetProvider.StatusesStylesColumnsNoProfile.createdtext.ordinal(),
                            SonetProvider.StatusesStylesColumnsNoProfile.message.ordinal(),
                            SonetProvider.StatusesStylesColumnsNoProfile.icon.ordinal(),
                            SonetProvider.StatusesStylesColumnsNoProfile.image.ordinal() };
                    int[] viewTypes = new int[] { LauncherIntent.Extra.Scroll.Types.IMAGEBLOB,
                            LauncherIntent.Extra.Scroll.Types.TEXTVIEW,
                            LauncherIntent.Extra.Scroll.Types.TEXTVIEW,
                            LauncherIntent.Extra.Scroll.Types.TEXTVIEW,
                            LauncherIntent.Extra.Scroll.Types.IMAGEBLOB,
                            LauncherIntent.Extra.Scroll.Types.IMAGEBLOB };
                    int[] layoutIds = new int[] { R.id.status_bg,
                            R.id.friend,
                            R.id.created,
                            R.id.message,
                            R.id.icon,
                            R.id.image };
                    int[] defaultResource = new int[] { 0,
                            0,
                            0,
                            0,
                            0,
                            0 };
                    boolean[] clickable = new boolean[] { true,
                            false,
                            false,
                            false,
                            false,
                            false };
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_DEFAULT_RESOURCES, defaultResource);
                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
                }
                break;
            case 2:
                if (display_profile) {
                    BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.widget_item);

                    Intent i = new Intent(SonetService.this, SonetWidget.class)
                            .setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
                            .setData(uri)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    PendingIntent pi = PendingIntent.getBroadcast(SonetService.this, 0, i, 0);

                    itemViews.SetBoundOnClickIntent(R.id.item, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS,
                            SonetProvider.StatusesStylesColumns._id.ordinal());

                    itemViews.setBoundBitmap(R.id.status_bg, "setImageBitmap", SonetProvider.StatusesStylesColumns.status_bg.ordinal(), 0);

                    itemViews.setBoundBitmap(R.id.image, "setImageBitmap", SonetProvider.StatusesStylesColumns.image.ordinal(), 0);

                    itemViews.setBoundBitmap(R.id.profile, "setImageBitmap", SonetProvider.StatusesStylesColumns.profile.ordinal(), 0);
                    itemViews.setBoundCharSequence(R.id.friend, "setText", SonetProvider.StatusesStylesColumns.friend.ordinal(), 0);
                    itemViews.setBoundCharSequence(R.id.created, "setText", SonetProvider.StatusesStylesColumns.createdtext.ordinal(), 0);
                    itemViews.setBoundCharSequence(R.id.message, "setText", SonetProvider.StatusesStylesColumns.message.ordinal(), 0);

                    itemViews.setBoundInt(R.id.friend, "setTextColor", SonetProvider.StatusesStylesColumns.friend_color.ordinal());
                    itemViews.setBoundInt(R.id.created, "setTextColor", SonetProvider.StatusesStylesColumns.created_color.ordinal());
                    itemViews.setBoundInt(R.id.message, "setTextColor", SonetProvider.StatusesStylesColumns.messages_color.ordinal());

                    itemViews.setBoundFloat(R.id.friend, "setTextSize", SonetProvider.StatusesStylesColumns.friend_textsize.ordinal());
                    itemViews.setBoundFloat(R.id.created, "setTextSize", SonetProvider.StatusesStylesColumns.created_textsize.ordinal());
                    itemViews.setBoundFloat(R.id.message, "setTextSize", SonetProvider.StatusesStylesColumns.messages_textsize.ordinal());

                    itemViews.setBoundBitmap(R.id.icon, "setImageBitmap", SonetProvider.StatusesStylesColumns.icon.ordinal(), 0);

                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);
                } else {
                    BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.widget_item_noprofile);

                    Intent i = new Intent(SonetService.this, SonetWidget.class)
                            .setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
                            .setData(uri)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    PendingIntent pi = PendingIntent.getBroadcast(SonetService.this, 0, i, 0);

                    itemViews.SetBoundOnClickIntent(R.id.item, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS,
                            SonetProvider.StatusesStylesColumnsNoProfile._id.ordinal());

                    itemViews.setBoundBitmap(R.id.status_bg, "setImageBitmap", SonetProvider.StatusesStylesColumnsNoProfile.status_bg.ordinal(), 0);

                    itemViews.setBoundBitmap(R.id.image, "setImageBitmap", SonetProvider.StatusesStylesColumnsNoProfile.image.ordinal(), 0);

                    itemViews.setBoundCharSequence(R.id.friend, "setText", SonetProvider.StatusesStylesColumnsNoProfile.friend.ordinal(), 0);
                    itemViews.setBoundCharSequence(R.id.created, "setText", SonetProvider.StatusesStylesColumnsNoProfile.createdtext.ordinal(), 0);
                    itemViews.setBoundCharSequence(R.id.message, "setText", SonetProvider.StatusesStylesColumnsNoProfile.message.ordinal(), 0);

                    itemViews.setBoundInt(R.id.friend, "setTextColor", SonetProvider.StatusesStylesColumnsNoProfile.friend_color.ordinal());
                    itemViews.setBoundInt(R.id.created, "setTextColor", SonetProvider.StatusesStylesColumnsNoProfile.created_color.ordinal());
                    itemViews.setBoundInt(R.id.message, "setTextColor", SonetProvider.StatusesStylesColumnsNoProfile.messages_color.ordinal());

                    itemViews.setBoundFloat(R.id.friend, "setTextSize", SonetProvider.StatusesStylesColumnsNoProfile.friend_textsize.ordinal());
                    itemViews.setBoundFloat(R.id.created, "setTextSize", SonetProvider.StatusesStylesColumnsNoProfile.created_textsize.ordinal());
                    itemViews.setBoundFloat(R.id.message, "setTextSize", SonetProvider.StatusesStylesColumnsNoProfile.messages_textsize.ordinal());

                    itemViews.setBoundBitmap(R.id.icon, "setImageBitmap", SonetProvider.StatusesStylesColumnsNoProfile.icon.ordinal(), 0);

                    replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);
                }
                break;
        }

        sendBroadcast(replaceDummy);
    }
}
