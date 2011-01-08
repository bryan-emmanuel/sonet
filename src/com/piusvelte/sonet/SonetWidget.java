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

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Widgets;

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
			if (intent.getExtras().getInt(LauncherIntent.Extra.EXTRA_API_VERSION, 1) >= 2)
				appWidgetReady(context, intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
		} else if (Sonet.ACTION_BUILD_SCROLL.equals(action))
			appWidgetReady(context, intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
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
		for (int appWidgetId : appWidgetIds) {
			((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getService(context, 0, new Intent(context, SonetService.class).setAction(Integer.toString(appWidgetId)), 0));
			context.getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=" + appWidgetId, null);
			context.getContentResolver().delete(Accounts.CONTENT_URI, Accounts.WIDGET + "=" + appWidgetId, null);
			context.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=" + appWidgetId, null);
		}
	}

	private void onClick(Context context, Intent intent) {
		boolean hasbuttons = false;
		int service = -1;
		String link = null;
		String appWidgetId = Integer.toString(intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
		Uri uri = Widgets.CONTENT_URI;
		Cursor c = context.getContentResolver().query(uri, new String[]{Widgets._ID, Widgets.HASBUTTONS}, Widgets.WIDGET + "=" + appWidgetId, null, null);
		if (c.moveToFirst()) hasbuttons = c.getInt(c.getColumnIndex(Widgets.HASBUTTONS)) == 1;
		c.close();
		Uri statusUri = Uri.withAppendedPath(Statuses.CONTENT_URI, Integer.toString(intent.getIntExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, -1)));
		Cursor item = context.getContentResolver().query(statusUri, new String[]{Statuses._ID, Statuses.SERVICE, Statuses.LINK}, null, null, null);
		if (item.moveToFirst()) {
			item.moveToFirst();
			service = item.getInt(item.getColumnIndex(Statuses.SERVICE));
			link = item.getString(item.getColumnIndex(Statuses.LINK));
			if (link != null) context.startActivity(hasbuttons ? new Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) : new Intent(context, StatusDialog.class).setAction(appWidgetId+"`"+service+"`"+link).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		}
	}
	
	private void appWidgetReady(Context context, int appWidgetId) {

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

		Cursor c = context.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.MESSAGES_COLOR, Widgets.FRIEND_COLOR, Widgets.CREATED_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_TEXTSIZE, Widgets.MESSAGES_TEXTSIZE, Widgets.SCROLLABLE}, Widgets.WIDGET + "=" + appWidgetId, null, null);
		if (c.moveToFirst()) {
			if (c.getInt(c.getColumnIndex(Widgets.SCROLLABLE)) != 1) {
				ContentValues values = new ContentValues();
				values.put(Widgets.SCROLLABLE, 1);
				context.getContentResolver().update(Widgets.CONTENT_URI, values, Widgets.WIDGET + "=" + appWidgetId, null);
			}
			itemViews.setTextColor(R.id.friend, c.getInt(c.getColumnIndex(Widgets.FRIEND_COLOR)));
			itemViews.setTextColor(R.id.created, c.getInt(c.getColumnIndex(Widgets.CREATED_COLOR)));
			itemViews.setTextColor(R.id.message, c.getInt(c.getColumnIndex(Widgets.MESSAGES_COLOR)));
			itemViews.setFloat(R.id.friend, "setTextSize", c.getInt(c.getColumnIndex(Widgets.FRIEND_TEXTSIZE)));
			itemViews.setFloat(R.id.created, "setTextSize", c.getInt(c.getColumnIndex(Widgets.CREATED_TEXTSIZE)));
			itemViews.setFloat(R.id.message, "setTextSize", c.getInt(c.getColumnIndex(Widgets.MESSAGES_TEXTSIZE)));
		}
		c.close();
		
		Intent i= new Intent(context, context.getClass())
		.setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
		.setData(Statuses.CONTENT_URI)
		.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		
		itemViews.SetBoundOnClickIntent(R.id.item, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.SonetProviderColumns._id.ordinal());

		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);
		replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);
		
		putProvider(replaceDummy, appWidgetId);
		
		context.sendBroadcast(replaceDummy);
		
	}

    public static void putProvider(Intent intent, int appWidgetId) {
            if (intent == null)
                    return;

    		// Put the data uri in as a string. Do not use setData, Home++ does not
    		// have a filter for that
    		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, Statuses.CONTENT_URI.toString());

    		String selectionArgs = null;
    		
    		// Other arguments for managed query
    		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, new String[]{Statuses._ID, Statuses.CREATED, Statuses.LINK, Statuses.FRIEND, Statuses.PROFILE, Statuses.MESSAGE, Statuses.SERVICE, Statuses.CREATEDTEXT, Statuses.WIDGET});
    		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, Statuses.WIDGET + "=" + appWidgetId);
    		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
    		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, Statuses.CREATED + " desc");
    }

}