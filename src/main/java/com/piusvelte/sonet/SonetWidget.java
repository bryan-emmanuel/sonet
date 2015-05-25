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
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.Widgets;

public class SonetWidget extends AppWidgetProvider {
    private static final String TAG = "SonetWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Sonet.acquire(context);
        // this is sent on boot
        // this should reload the widget
        context.startService(new Intent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                .setAction(Sonet.ACTION_REFRESH));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Sonet.ACTION_REFRESH)) {
            Sonet.acquire(context);
            // this should reload the widget
            int[] appWidgetIds;

            if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                appWidgetIds = new int[] { intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) };
            } else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
                appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            } else {
                appWidgetIds = new int[] { AppWidgetManager.INVALID_APPWIDGET_ID };
            }

            context.startService(new Intent(context, SonetService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    .setAction(action));
        } else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
            final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                onDeleted(context, new int[] { appWidgetId });
            } else {
                super.onReceive(context, intent);
            }
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public final void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int appWidgetId : appWidgetIds) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent
                    .getService(context, 0, new Intent(context, SonetService.class).setAction(Integer.toString(appWidgetId)), 0));
            context.getContentResolver()
                    .delete(Widgets.getContentUri(context), Widgets.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
            context.getContentResolver()
                    .delete(WidgetAccounts.getContentUri(context), WidgetAccounts.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
            Cursor statuses = context.getContentResolver()
                    .query(Statuses.getContentUri(context), new String[] { Statuses._ID }, Statuses.WIDGET + "=?",
                            new String[] { Integer.toString(appWidgetId) }, null);

            if (statuses.moveToFirst()) {
                while (!statuses.isAfterLast()) {
                    context.getContentResolver().delete(StatusLinks.getContentUri(context), StatusLinks.STATUS_ID + "=?",
                            new String[] { Long.toString(statuses.getLong(0)) });
                    statuses.moveToNext();
                }
            }

            statuses.close();
            context.getContentResolver()
                    .delete(Statuses.getContentUri(context), Statuses.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
        }
    }
}