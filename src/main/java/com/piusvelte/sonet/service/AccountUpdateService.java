package com.piusvelte.sonet.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.StatusImages;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.Widgets;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by bemmanuel on 4/20/15.
 */
public class AccountUpdateService extends IntentService {

    @Deprecated
    public static final String ACTION_ENABLE = "ACTION_ENABLE";
    @Deprecated
    public static final String ACTION_DISABLE = "ACTION_DISABLE";
    public static final String ACTION_DELETE = "ACTION_DELETE";

    @StringDef({ ACTION_ENABLE, ACTION_DISABLE, ACTION_DELETE })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {
    }

    private static final String EXTRA_ACCOUNT_ID = "EXTRA_ACCOUNT_ID";
    private static final String EXTRA_APPWIDGET_ID = "EXTRA_APPWIDGET_ID";

    public AccountUpdateService() {
        super(AccountUpdateService.class.getSimpleName());
    }

    public static Intent obtainIntent(@NonNull Context context, @Action String action, long accountId, int appwidgetId) {
        return new Intent(context, AccountUpdateService.class)
                .setAction(action)
                .putExtra(EXTRA_ACCOUNT_ID, accountId)
                .putExtra(EXTRA_APPWIDGET_ID, appwidgetId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            long accountId = intent.getLongExtra(EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID);
            int appwidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            Cursor statuses;

            switch (intent.getAction()) {
                case ACTION_ENABLE:
                    // XXX accounts are no longer widget specific
                    ContentValues values = new ContentValues();
                    values.put(WidgetAccounts.ACCOUNT, accountId);
                    values.put(WidgetAccounts.WIDGET, appwidgetId);
                    getContentResolver().insert(WidgetAccounts.getContentUri(this), values);
                    break;

                case ACTION_DISABLE:
                    // XXX accounts are no longer widget specific
                    // disable the account, remove settings and statuses
                    getContentResolver().delete(Widgets.getContentUri(this),
                            Widgets.ACCOUNT + "=? and " + Widgets.WIDGET + "=?",
                            new String[] { Long.toString(accountId), Integer.toString(appwidgetId) });
                    getContentResolver().delete(WidgetAccounts.getContentUri(this),
                            WidgetAccounts.ACCOUNT + "=? and " + WidgetAccounts.WIDGET + "=?",
                            new String[] { Long.toString(accountId), Integer.toString(appwidgetId) });
                    statuses = getContentResolver().query(Statuses.getContentUri(this),
                            new String[] { Statuses._ID },
                            Statuses.ACCOUNT + "=? and " + Statuses.WIDGET + "=?",
                            new String[] { Long.toString(accountId), Integer.toString(appwidgetId) },
                            null);

                    if (statuses.moveToFirst()) {
                        int statusIdIndex = statuses.getColumnIndexOrThrow(Statuses._ID);

                        while (!statuses.isAfterLast()) {
                            long statusId = statuses.getLong(statusIdIndex);
                            String[] statusQueryArgs = new String[] { String.valueOf(statusId) };
                            getContentResolver().delete(StatusLinks.getContentUri(this),
                                    StatusLinks.STATUS_ID + "=?",
                                    statusQueryArgs);
                            getContentResolver().delete(StatusImages.getContentUri(this),
                                    StatusImages.STATUS_ID + "=?",
                                    statusQueryArgs);
                            statuses.moveToNext();
                        }
                    }

                    statuses.close();
                    getContentResolver().delete(Statuses.getContentUri(this),
                            Statuses.ACCOUNT + "=? and " + Statuses.WIDGET + "=?",
                            new String[] { Long.toString(accountId),
                                    Integer.toString(appwidgetId) });
                    break;

                case ACTION_DELETE:
                    String[] queryArgs = new String[] { String.valueOf(accountId) };
                    getContentResolver().delete(Accounts.getContentUri(this),
                            Accounts._ID + "=?",
                            queryArgs);
                    // need to delete the statuses and settings for all accounts
                    getContentResolver().delete(Widgets.getContentUri(this),
                            Widgets.ACCOUNT + "=?",
                            queryArgs);
                    statuses = getContentResolver().query(Statuses.getContentUri(this),
                            new String[] { Statuses._ID },
                            Statuses.ACCOUNT + "=?",
                            queryArgs,
                            null);

                    if (statuses.moveToFirst()) {
                        int statusIdIndex = statuses.getColumnIndexOrThrow(Statuses._ID);

                        while (!statuses.isAfterLast()) {
                            long statusId = statuses.getLong(statusIdIndex);
                            String[] statusQueryArgs = new String[] { String.valueOf(statusId) };

                            getContentResolver().delete(StatusLinks.getContentUri(this),
                                    StatusLinks.STATUS_ID + "=?",
                                    statusQueryArgs);
                            getContentResolver().delete(StatusImages.getContentUri(this),
                                    StatusImages.STATUS_ID + "=?",
                                    statusQueryArgs);
                            statuses.moveToNext();
                        }
                    }

                    statuses.close();
                    getContentResolver().delete(Statuses.getContentUri(this),
                            Statuses.ACCOUNT + "=?",
                            queryArgs);
                    getContentResolver().delete(WidgetAccounts.getContentUri(this),
                            WidgetAccounts.ACCOUNT + "=?",
                            queryArgs);
                    getContentResolver().delete(Notifications.getContentUri(this),
                            Notifications.ACCOUNT + "=?",
                            queryArgs);
                    break;
            }
        }
    }
}
