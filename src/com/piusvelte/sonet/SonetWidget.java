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

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.TAG;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.SCROLLABLE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.BoundRemoteViews;
import mobi.intuitit.android.widget.SimpleRemoteViews;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class SonetWidget extends AppWidgetProvider {
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		context.startService(new Intent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.v(TAG,"onReceive: "+action);
		if (action.equals(ACTION_REFRESH)) {
			int[] appWidgetIds;
			if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) appWidgetIds = new int[]{intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)};
			else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			else appWidgetIds = new int[]{AppWidgetManager.INVALID_APPWIDGET_ID};
			context.startService(new Intent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));	
		} else if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) onDeleted(context, new int[]{appWidgetId});
			else super.onReceive(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_READY)) {
            if (intent.getExtras().getInt(LauncherIntent.Extra.EXTRA_API_VERSION, 1) >= 2) onAppWidgetReady(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_FINISH)) {
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_ITEM_CLICK)) {
			// onItemClickListener
			onItemClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_VIEW_CLICK)) {
			// onClickListener
			onClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Error.ERROR_SCROLL_CURSOR)) {
			// An error occured
			Log.d(TAG, intent.getStringExtra(LauncherIntent.Extra.EXTRA_ERROR_MESSAGE) + "");
		} else super.onReceive(context, intent);
	}

	@Override
	public final void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(context);
		SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		for (int appWidgetId : appWidgetIds) {
			alarmManager.cancel(PendingIntent.getService(context, 0, new Intent(context, SonetService.class).setAction(Integer.toString(appWidgetId)), 0));
			db.delete(TABLE_WIDGETS, WIDGET + "=" + appWidgetId, null);
			db.delete(TABLE_ACCOUNTS, WIDGET + "=" + appWidgetId, null);
		}
		db.close();
		sonetDatabaseHelper.close();
	}
	
	public void onAppWidgetReady(Context context, Intent intent) {
		int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		
		if (appWidgetId < 0) return;
		
		SonetWidget.buildScrollable(context, appWidgetId);
	}
	
	public static void buildScrollable(Context context, int appWidgetId) {

		String appWidgetUri = SonetProvider.CONTENT_URI.buildUpon().appendEncodedPath(Integer.toString(appWidgetId)).toString();
		Intent replaceDummy = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);
		// Put widget info
		replaceDummy.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);
		replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.messages);

		SimpleRemoteViews listView = new SimpleRemoteViews(R.layout.widget_listview);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_REMOTEVIEWS, listView);

		BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.widget_item);
		itemViews.setBoundBitmap(R.id.profile, "setImageBitmap", SonetProvider.SonetProviderColumns.profile.ordinal(), 0);
		itemViews.setBoundCharSequence(R.id.friend, "setText", SonetProvider.SonetProviderColumns.friend.ordinal(), 0);
		itemViews.setBoundCharSequence(R.id.created, "setText", SonetProvider.SonetProviderColumns.createdtext.ordinal(), 0);
		itemViews.setBoundCharSequence(R.id.message, "setText", SonetProvider.SonetProviderColumns.message.ordinal(), 0);

		// pull settings to style the list
		SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(context);
		SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
		Cursor settings = db.rawQuery("select " + _ID + "," + MESSAGES_COLOR + "," + FRIEND_COLOR + "," + CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + appWidgetId, null);
		if (settings.getCount() > 0) {
			settings.moveToFirst();
			itemViews.setTextColor(R.id.friend, settings.getInt(settings.getColumnIndex(FRIEND_COLOR)));
			itemViews.setTextColor(R.id.created, settings.getInt(settings.getColumnIndex(CREATED_COLOR)));
			itemViews.setTextColor(R.id.message, settings.getInt(settings.getColumnIndex(MESSAGES_COLOR)));
			// prevent SonetService from replacing the view with the non-scrolling layout
			ContentValues values = new ContentValues();
			values.put(SCROLLABLE, 1);
			db.update(TABLE_WIDGETS, values, WIDGET + "=" + appWidgetId, null);
		}
		settings.close();
		db.close();
		sonetDatabaseHelper.close();

		Intent i= new Intent(context, SonetWidget.class)
		.setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
		.setData(Uri.parse(appWidgetUri))
		.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		itemViews.SetBoundOnClickIntent(R.id.profile, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.SonetProviderColumns.link.ordinal());
		itemViews.SetBoundOnClickIntent(R.id.friend, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.SonetProviderColumns.link.ordinal());
		itemViews.SetBoundOnClickIntent(R.id.created, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.SonetProviderColumns.link.ordinal());
		itemViews.SetBoundOnClickIntent(R.id.message, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.SonetProviderColumns.link.ordinal());

		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);

		putProvider(replaceDummy, appWidgetUri);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);

		// Send it out
		Log.v(TAG,"sendBroadcast");
		context.sendBroadcast(replaceDummy);
	}

	public static void putProvider(Intent intent, String widgetUri) {
		if (intent == null)
			return;

		String whereClause = WIDGET + "=" + Uri.parse(widgetUri).getLastPathSegment();
		String orderBy = CREATED + " desc";
		String[] selectionArgs = null;

		// Put the data uri in as a string. Do not use setData, Home++ does not
		// have a filter for that
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, widgetUri);
		Log.d(TAG, "widgetUri pushed to Launcher : " + widgetUri);

		// Other arguments for managed query
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, SonetProvider.PROJECTION_APPWIDGETS);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, orderBy);
	}

    /**
     * On click of a child view in an item
     */
    private void onClick(Context context, Intent intent) {
            int itemPosition = intent.getIntExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, -1);
            int viewId = intent.getIntExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, -1);

            int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID);

            Log.d(TAG, "appWidgetId = " + appWidgetId + " / itemPosition = " + itemPosition + " / viewId = " + viewId);

    }

    /**
     * On click of an item
     */
    private void onItemClick(Context context, Intent intent) {
            int itemPosition = intent.getIntExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, -1);
            int viewId = intent.getIntExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, -1);

            int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID);

            Log.d(TAG, "appWidgetId = " + appWidgetId + " / itemPosition = " + itemPosition + " / viewId = " + viewId);
    }
}