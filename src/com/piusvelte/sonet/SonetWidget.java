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
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import static com.piusvelte.sonet.SonetDatabaseHelper.HASBUTTONS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_STATUSES;
import static com.piusvelte.sonet.SonetDatabaseHelper.LINK;
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_TEXTSIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_TEXTSIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_TEXTSIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.SCROLLABLE;
import static com.piusvelte.sonet.Sonet.ACTION_MAKE_SCROLLABLE;
import static com.piusvelte.sonet.Sonet.ACTION_DELETE;
import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.BoundRemoteViews;
import mobi.intuitit.android.widget.SimpleRemoteViews;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class SonetWidget extends AppWidgetProvider {
	private static final String TAG = "SonetWidget";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		context.startService(new Intent(context, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
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
			if (intent.getExtras().getInt(LauncherIntent.Extra.EXTRA_API_VERSION, 1) >= 2) onAppWidgetReady(context, intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
		} else if (Sonet.ACTION_BUILD_SCROLL.equals(action)) onAppWidgetReady(context, intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
		else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_FINISH)) {
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
		context.startService(new Intent(context, SonetService.class).setAction(ACTION_DELETE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
	}

	public void onAppWidgetReady(Context context, int appWidgetId) {

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
		SQLiteDatabase db = sonetDatabaseHelper.getReadableDatabase();
		Cursor settings = db.rawQuery("select " + _ID + "," + MESSAGES_COLOR + "," + FRIEND_COLOR + "," + CREATED_COLOR + "," + FRIEND_TEXTSIZE + "," + CREATED_TEXTSIZE + "," + MESSAGES_TEXTSIZE + "," + SCROLLABLE + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + appWidgetId, null);
		if (settings.getCount() > 0) {
			settings.moveToFirst();
			itemViews.setTextColor(R.id.friend, settings.getInt(settings.getColumnIndex(FRIEND_COLOR)));
			itemViews.setTextColor(R.id.created, settings.getInt(settings.getColumnIndex(CREATED_COLOR)));
			itemViews.setTextColor(R.id.message, settings.getInt(settings.getColumnIndex(MESSAGES_COLOR)));
			itemViews.setFloat(R.id.friend, "setTextSize", settings.getInt(settings.getColumnIndex(FRIEND_TEXTSIZE)));
			itemViews.setFloat(R.id.created, "setTextSize", settings.getInt(settings.getColumnIndex(CREATED_TEXTSIZE)));
			itemViews.setFloat(R.id.message, "setTextSize", settings.getInt(settings.getColumnIndex(MESSAGES_TEXTSIZE)));
			if (settings.getInt(settings.getColumnIndex(SCROLLABLE)) != 1) {
				// prevent SonetService from replacing the view with the non-scrolling layout
				context.startService(new Intent(context, SonetService.class).setAction(ACTION_MAKE_SCROLLABLE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
			}
		}
		settings.close();
		db.close();
		sonetDatabaseHelper.close();

		Intent i= new Intent(context, this.getClass())
		.setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
		.setData(Uri.parse(appWidgetUri))
		.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		itemViews.SetBoundOnClickIntent(R.id.item, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.SonetProviderColumns._id.ordinal());

		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);

		putProvider(replaceDummy, appWidgetUri);

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

		// Other arguments for managed query
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, SonetProvider.PROJECTION_APPWIDGETS);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, orderBy);
	}

	private void onClick(Context context, Intent intent) {

		String rowId = intent.getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS);
		int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

		int service = -1;
		String link = null;
		Boolean hasbuttons = false;
		SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(context);
		SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
		Cursor settings = db.rawQuery("select " + _ID + "," + HASBUTTONS + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + appWidgetId, null);
		if (settings.getCount() > 0) {
			settings.moveToFirst();
			hasbuttons = settings.getInt(settings.getColumnIndex(HASBUTTONS)) == 1;
		}
		settings.close();
		Cursor item = db.rawQuery("select " + _ID + "," + SERVICE + "," + LINK + " from " + TABLE_STATUSES + " where " + _ID + "=" + rowId, null);
		if (item.getCount() > 0) {
			item.moveToFirst();
			service = item.getInt(item.getColumnIndex(SERVICE));
			link = item.getString(item.getColumnIndex(LINK));
		}
		item.close();
		db.close();
		sonetDatabaseHelper.close();
		if (link != null) context.startActivity(hasbuttons ? new Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) : new Intent(context, StatusDialog.class).setAction(appWidgetId+"`"+service+"`"+link).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

	}

}