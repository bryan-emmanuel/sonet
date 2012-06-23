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
package com.piusvelte.sonet.core;

import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.Sonet.Status_links;
import com.piusvelte.sonet.core.Sonet.Statuses;
import com.piusvelte.sonet.core.Sonet.Statuses_styles;
import com.piusvelte.sonet.core.Sonet.Widget_accounts;
import com.piusvelte.sonet.core.Sonet.Widgets;
import com.piusvelte.sonet.core.SonetService;
import com.piusvelte.sonet.core.StatusDialog;

import mobi.intuitit.android.content.LauncherIntent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class SonetWidget extends AppWidgetProvider {
	private static final String TAG = "SonetWidget";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Sonet.acquire(context);
		// this is sent on boot
		// this should reload the widget
		context.startService(Sonet.getPackageIntent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds).setAction(Sonet.ACTION_REFRESH));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (action.equals(Sonet.ACTION_REFRESH)) {
			Sonet.acquire(context);
			// this should reload the widget
			int[] appWidgetIds;
			if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
				appWidgetIds = new int[]{intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)};
			} else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
				appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			} else {
				appWidgetIds = new int[]{AppWidgetManager.INVALID_APPWIDGET_ID};
			}
			context.startService(Sonet.getPackageIntent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds).setAction(action));
		} else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				onDeleted(context, new int[]{appWidgetId});
			} else {
				super.onReceive(context, intent);
			}
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_READY)) {
			Sonet.acquire(context);
			// ACTION_READY is sent on screen rotation and on boot
			// this should only ever requery...
			final Bundle extras = intent.getExtras();
			context.startService(Sonet.getPackageIntent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)).putExtra(Sonet.EXTRA_SCROLLABLE_VERSION, extras.getInt(LauncherIntent.Extra.EXTRA_API_VERSION, 1)).setAction(action));
		} else if (action.equals(LauncherIntent.Action.ACTION_FINISH)) {
		} else if (action.equals(Sonet.ACTION_ON_CLICK) || action.equals(LauncherIntent.Action.ACTION_VIEW_CLICK) || action.equals(LauncherIntent.Action.ACTION_ITEM_CLICK)) {
			onClick(context, intent);
		} else if (action.equals(LauncherIntent.Error.ERROR_SCROLL_CURSOR)) {
			Log.d(TAG, intent.getStringExtra(LauncherIntent.Extra.EXTRA_ERROR_MESSAGE) + "");
		} else if (action.equals("com.motorola.blur.home.ACTION_SET_WIDGET_SIZE")) {
		} else {
			super.onReceive(context, intent);
		}
	}

	@Override
	public final void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int appWidgetId : appWidgetIds) {
			((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getService(context, 0, Sonet.getPackageIntent(context, SonetService.class).setAction(Integer.toString(appWidgetId)), 0));
			context.getContentResolver().delete(Widgets.getContentUri(context), Widgets.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)});
			context.getContentResolver().delete(Widget_accounts.getContentUri(context), Widget_accounts.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)});
			Cursor statuses = context.getContentResolver().query(Statuses.getContentUri(context), new String[]{Statuses._ID}, Statuses.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)}, null);
			if (statuses.moveToFirst()) {
				while (!statuses.isAfterLast()) {
					context.getContentResolver().delete(Status_links.getContentUri(context), Status_links.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
					statuses.moveToNext();
				}
			}
			statuses.close();
			context.getContentResolver().delete(Statuses.getContentUri(context), Statuses.WIDGET + "=?", new String[]{Integer.toString(appWidgetId)});
		}
	}

	private void onClick(Context context, Intent intent) {
		// send all onClick events to StatusDialog
		String statusId = "";
		if (intent.hasExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS)) {
			statusId = intent.getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS);
		} else if (intent.hasExtra(Sonet.Status_links.STATUS_ID)) {
			statusId = intent.getStringExtra(Sonet.Status_links.STATUS_ID);		
		}
		context.startActivity(intent.setClass(context, Sonet.getPackageClass(context, StatusDialog.class)).setData(Uri.withAppendedPath(Statuses_styles.getContentUri(context), statusId)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
	}

}