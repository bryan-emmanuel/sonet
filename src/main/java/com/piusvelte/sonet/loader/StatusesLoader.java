package com.piusvelte.sonet.loader;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.piusvelte.sonet.About;
import com.piusvelte.sonet.BuildConfig;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetCrypto;
import com.piusvelte.sonet.SonetService;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.provider.WidgetAccountsView;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;
import com.piusvelte.sonet.social.Client;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.NOTIFY_ID;

/**
 * Created by bemmanuel on 5/18/15.
 */
public class StatusesLoader extends AsyncTask<Integer, String, Integer> {

    private static final String TAG = "StatusesLoader";

    private SonetService mSonetService;

    public StatusesLoader(@NonNull SonetService sonetService) {
        mSonetService = sonetService;
    }

    @Override
    protected Integer doInBackground(Integer... params) {
        // first handle deletes, then scroll updates, finally regular updates
        final int appWidgetId = params[0];
        final String widget = Integer.toString(appWidgetId);
        final boolean reload = params[1] != 0;

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loading widget:" + widget + ",reload:" + reload);
        }

        WidgetsSettings.Settings settings = WidgetsSettings.getSettings(mSonetService, appWidgetId);
        int refreshInterval = settings.interval;
        boolean backgroundUpdate = settings.isBackgroundUpdate;

        // the widget will start out as the default widget.xml, which simply says "loading..."
        // if there's a cache, that should be quickly reloaded while new updates come down
        // otherwise, replace the widget with "loading..."
        // clear the messages
        mSonetService.getContentResolver().delete(Statuses.getContentUri(mSonetService),
                Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?",
                new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),// all statuses
                        Long.toString(Sonet.INVALID_ACCOUNT_ID) });
        Cursor statuses = mSonetService.getContentResolver().query(Statuses.getContentUri(mSonetService),
                new String[] { Statuses._ID },
                Statuses.WIDGET + "=?",
                new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID) },// all statuses
                null);
        boolean hasCache = statuses.moveToFirst();
        statuses.close();

        // the alarm should always be set, rather than depend on the tasks to complete
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && !hasCache || reload && refreshInterval > 0) {
            mSonetService.mAlarmManager.cancel(PendingIntent.getService(mSonetService,
                    0,
                    new Intent(mSonetService, SonetService.class).setAction(widget),
                    0));
            mSonetService.mAlarmManager
                    .set(backgroundUpdate ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC, System.currentTimeMillis() + refreshInterval,
                            PendingIntent.getService(mSonetService,
                                    0,
                                    new Intent(mSonetService, SonetService.class)
                                            .setData(Uri.withAppendedPath(Widgets.getContentUri(mSonetService),
                                                    widget))
                                            .setAction(ACTION_REFRESH),
                                    0));
        }

        // get the accounts
        Cursor accounts = mSonetService.getContentResolver().query(WidgetAccountsView.getContentUri(mSonetService),
                new String[] { WidgetAccountsView.ACCOUNT,
                        WidgetAccountsView.TOKEN,
                        WidgetAccountsView.SECRET,
                        WidgetAccountsView.SERVICE,
                        WidgetAccountsView.SID },
                WidgetAccountsView.WIDGET + "=?",
                new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID) },// use invalid appwidget to get all accounts
                null);

        // TODO remove this, and indicate loading using the empty view
        if (!hasCache || !accounts.moveToFirst()) {
            // if no cache inform the user that the widget is loading
            addStatusItem(widget, mSonetService.getString(R.string.updating), appWidgetId);
        }

        // TODO remove all of this "loading"
        // loading takes time, so don't leave an empty widget sitting there
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            // build the widget
            mSonetService.buildWidgetButtons(appWidgetId);
        } else {
            // TODO this isn't necessary
            // update the About.java for in-app viewing
            mSonetService.getContentResolver().notifyChange(StatusesStyles.getContentUri(mSonetService), null);
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "loading widget=" + widget + "; accounts count=" + accounts.getCount());
        }

        if (accounts.moveToFirst()) {
            final int accountIndex = accounts.getColumnIndexOrThrow(WidgetAccountsView.ACCOUNT);
            final int tokenIndex = accounts.getColumnIndexOrThrow(WidgetAccountsView.TOKEN);
            final int secretIndex = accounts.getColumnIndexOrThrow(WidgetAccountsView.SECRET);
            final int serviceIndex = accounts.getColumnIndexOrThrow(WidgetAccountsView.SERVICE);
            final int sidIndex = accounts.getColumnIndexOrThrow(WidgetAccountsView.SID);

            // only reload if the token's can be decrypted and if there's no cache or a reload is requested
            if (!hasCache || reload) {
                mSonetService.mNotify = null;
                int notifications = 0;

                // load the updates
                while (!accounts.isAfterLast()) {
                    long account = accounts.getLong(accountIndex);
                    int service = accounts.getInt(serviceIndex);

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "loading account=" + account + "; service=" + service);
                    }

                    WidgetsSettings.Settings accountSettings = WidgetsSettings.getSettings(mSonetService, appWidgetId, account);
                    boolean time24hr = accountSettings.isTime24hr;
                    int status_count = Sonet.default_statuses_per_account;
                    notifications = accountSettings.notificationsMask();

                    // if no connection, only update the status_bg and icons
                    if (mSonetService.mConnectivityManager.getActiveNetworkInfo() != null && mSonetService.mConnectivityManager
                            .getActiveNetworkInfo().isConnected()) {
                        SonetCrypto sonetCrypto = SonetCrypto.getInstance(mSonetService);
                        String token = sonetCrypto.Decrypt(accounts.getString(tokenIndex));
                        String secret = sonetCrypto.Decrypt(accounts.getString(secretIndex));
                        String accountEsid = sonetCrypto.Decrypt(accounts.getString(sidIndex));

                        Client client = new Client.Builder(mSonetService)
                                .setNetwork(service)
                                .setAccount(accountEsid)
                                .setCredentials(token, secret)
                                .build();

                        String notificationMessage = client.getFeed(appWidgetId,
                                Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),// all statuses here
                                account,
                                status_count,
                                time24hr,
                                true,
                                notifications);

                        if (TextUtils.isEmpty(mSonetService.mNotify)) {
                            mSonetService.mNotify = notificationMessage;
                        } else if (!TextUtils.isEmpty(notificationMessage)) {
                            mSonetService.mNotify = mSonetService.getString(R.string.notify_multiple_updates);
                        }

                        // remove old notifications
                        mSonetService.getContentResolver().delete(Notifications.getContentUri(mSonetService),
                                Notifications.CLEARED + "=1 and " + Notifications.ACCOUNT + "=? and " + Notifications.CREATED + "<?",
                                new String[] { Long.toString(account),
                                        Long.toString(System.currentTimeMillis() - 86400000) });
                    } else {
                        // no network connection
                        if (hasCache) {
                            // update created text
                            updateCreatedText(widget, Long.toString(account), time24hr);
                        } else {
                            // TODO remove
                            // clear the "loading" message and display "no connection"
                            mSonetService.getContentResolver()
                                    .delete(Statuses.getContentUri(mSonetService),
                                            Statuses.WIDGET + "=?",
                                            new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID) });
                            addStatusItem(widget, mSonetService.getString(R.string.no_connection), appWidgetId);
                        }
                    }

                    accounts.moveToNext();
                }

                if (notifications != 0 && mSonetService.mNotify != null) {
                    publishProgress(Integer.toString(notifications));
                }
            }

            // TODO remove
            // delete the existing loading and informational messages
            mSonetService.getContentResolver()
                    .delete(Statuses.getContentUri(mSonetService),
                            Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?",
                            new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),
                                    Long.toString(Sonet.INVALID_ACCOUNT_ID) });
            // check statuses again
            Cursor statusesCheck = mSonetService.getContentResolver()
                    .query(Statuses.getContentUri(mSonetService),
                            new String[] { Statuses._ID },
                            Statuses.WIDGET + "=?",
                            new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID) },// all statuses here
                            null);
            hasCache = statusesCheck.moveToFirst();
            statusesCheck.close();

            // TODO remove
            if (!hasCache) {
                // there should be a loading message displaying
                // if no updates have been loaded, display "no updates"
                addStatusItem(widget, mSonetService.getString(R.string.no_updates), appWidgetId);
            }
        } else {
            // no accounts, clear cache
            mSonetService.getContentResolver().delete(Statuses.getContentUri(mSonetService),
                    Statuses.WIDGET + "=?",
                    new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID) });
            // insert no accounts message
            addStatusItem(widget, mSonetService.getString(R.string.no_accounts), appWidgetId);
        }

        accounts.close();

        // always update buttons, if !scrollable update widget both times, otherwise build scrollable first, requery second
        // see if the tasks are finished
        // non-scrollable widgets will be completely rebuilt, while scrollable widgets while be notified to requery
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            mSonetService.buildWidgetButtons(appWidgetId);
        } else {
            // notify change to About.java
            mSonetService.getContentResolver().notifyChange(StatusesStyles.getContentUri(mSonetService), null);
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
            Notification notification = new Notification(R.drawable.notification, mSonetService.mNotify, System.currentTimeMillis());
            notification.setLatestEventInfo(mSonetService.getBaseContext(), "New messages", mSonetService.mNotify,
                    PendingIntent.getActivity(mSonetService, 0,
                            About.createIntent(mSonetService, About.DRAWER_NOTIFICATIONS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), 0));
            notification.defaults |= notifications;
            ((NotificationManager) mSonetService.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFY_ID, notification);
        }
    }

    @Override
    protected void onPostExecute(Integer appWidgetId) {
        // remove self from thread list
        if (!SonetService.mStatusesLoaders.isEmpty() && SonetService.mStatusesLoaders.containsKey(appWidgetId)) {
            SonetService.mStatusesLoaders.remove(appWidgetId);
        }

        //			Log.d(TAG,"finished update, check queue");
        if (SonetService.mStatusesLoaders.isEmpty()) {
            //				Log.d(TAG,"stop service");
            Sonet.release();
            mSonetService.stopSelfResult(mSonetService.mStartId);
        }
    }

    // TODO remove this generic item used for status messages
    private void addStatusItem(String widget, String message, int appWidgetId) {
        long id;
        long created = System.currentTimeMillis();
        int service = 0;
        boolean time24hr = false;
        long accountId = Sonet.INVALID_ACCOUNT_ID;
        String sid = "-1";
        String esid = "";
        String friend = mSonetService.getString(R.string.app_name);

        Cursor entity = mSonetService.getContentResolver().query(Entity.getContentUri(mSonetService),
                new String[] { Entity._ID },
                Entity.ACCOUNT + "=? and " + Entity.ESID + "=?",
                new String[] { Long.toString(accountId),
                        esid },
                null);

        if (entity.moveToFirst()) {
            id = entity.getLong(entity.getColumnIndexOrThrow(Entity._ID));
        } else {
            ContentValues entityValues = new ContentValues();
            entityValues.put(Entity.ESID, esid);
            entityValues.put(Entity.FRIEND, friend);
            entityValues.put(Entity.ACCOUNT, accountId);
            id = Long.parseLong(
                    mSonetService.getContentResolver().insert(Entity.getContentUri(mSonetService), entityValues).getLastPathSegment());
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

        long statusId = Long.parseLong(mSonetService.getContentResolver().insert(Statuses.getContentUri(mSonetService),
                values).getLastPathSegment());
    }

    private boolean updateCreatedText(String widget, String account, boolean time24hr) {
        boolean statuses_updated = false;
        Cursor statuses = mSonetService.getContentResolver()
                .query(Statuses.getContentUri(mSonetService),
                        new String[] { Statuses._ID,
                                Statuses.CREATED },
                        Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?",
                        new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),
                                account },
                        null);

        if (statuses.moveToFirst()) {
            while (!statuses.isAfterLast()) {
                ContentValues values = new ContentValues();
                values.put(Statuses.CREATEDTEXT, Sonet.getCreatedText(statuses.getLong(1), time24hr));
                mSonetService.getContentResolver().update(Statuses.getContentUri(mSonetService),
                        values,
                        Statuses._ID + "=?",
                        new String[] { Long.toString(statuses.getLong(0)) });
                statuses.moveToNext();
            }

            statuses_updated = true;
        }

        statuses.close();
        return statuses_updated;
    }
}
