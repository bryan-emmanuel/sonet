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

import static com.piusvelte.sonet.Sonet.EXTRA_SCROLLABLE_VERSION;

import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.BoundRemoteViews;

import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;

public class SonetScrollableBuilder extends Service {

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if ((intent != null) && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
			int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			int scrollableVersion = intent.getIntExtra(EXTRA_SCROLLABLE_VERSION, 1);
			if (Sonet.sWidgetsContext.containsKey(appWidgetId)) {
				Context context = Sonet.sWidgetsContext.get(appWidgetId);
				// set widget as scrollable
				String widgetId = Integer.toString(appWidgetId);
				Cursor c = context.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.SCROLLABLE}, Widgets.WIDGET + "=?", new String[]{widgetId}, null);
				if (c.moveToFirst()) {
					if (c.getInt(c.getColumnIndex(Widgets.SCROLLABLE)) == 0) {
						ContentValues values = new ContentValues();
						values.put(Widgets.SCROLLABLE, scrollableVersion);
						context.getContentResolver().update(Widgets.CONTENT_URI, values, Widgets.WIDGET + "=?", new String[] {widgetId});
					}
				}
				c.close();
				
				Intent replaceDummy = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);
				replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_APPWIDGET_ID, appWidgetId);
				replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.messages);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID, R.layout.widget_listview);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);

				// provider
				Uri uri = null;
				String[] projection = null;

				if (scrollableVersion == 1) {
					// provider
					uri = Uri.withAppendedPath(Statuses_styles.CONTENT_URI_V1, widgetId);
					projection = new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.CREATEDTEXT, Statuses_styles.STATUS_BG, Statuses_styles.ICON};
					// mapping for views
					replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.widget_item);
					int[] cursorIndices = new int[8];
					int[] viewTypes = new int[8];
					int[] layoutIds = new int[8];
					int[] defaultResource = new int[8];
					boolean[] clickable = new boolean[8];
					// R.id.friend_bg_clear
					cursorIndices[0] = SonetProvider.StatusesStylesColumnsV1.friend.ordinal();
					viewTypes[0] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
					layoutIds[0] = R.id.friend_bg_clear;
					defaultResource[0] = 0;
					clickable[0] = false;
					// R.id.message_bg_clear
					cursorIndices[1] = SonetProvider.StatusesStylesColumnsV1.message.ordinal();
					viewTypes[1] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
					layoutIds[1] = R.id.message_bg_clear;
					defaultResource[1] = 0;
					clickable[1] = false;
					// R.id.status_bg
					cursorIndices[2] = SonetProvider.StatusesStylesColumnsV1.status_bg.ordinal();
					viewTypes[2] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
					layoutIds[2] = R.id.status_bg;
					defaultResource[2] = 0;
					clickable[2] = false;
					// R.id.profile
					cursorIndices[3] = SonetProvider.StatusesStylesColumnsV1.profile.ordinal();
					viewTypes[3] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
					layoutIds[3] = R.id.profile;
					defaultResource[3] = 0;
					clickable[3] = false;
					// R.id.friend
					cursorIndices[4] = SonetProvider.StatusesStylesColumnsV1.friend.ordinal();
					viewTypes[4] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
					layoutIds[4] = R.id.friend;
					defaultResource[4] = 0;
					clickable[4] = false;
					// R.id.created
					cursorIndices[5] = SonetProvider.StatusesStylesColumnsV1.createdtext.ordinal();
					viewTypes[5] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
					layoutIds[5] = R.id.created;
					defaultResource[5] = 0;
					clickable[5] = false;
					// R.id.message
					cursorIndices[6] = SonetProvider.StatusesStylesColumnsV1.message.ordinal();
					viewTypes[6] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
					layoutIds[6] = R.id.message;
					defaultResource[6] = 0;
					clickable[6] = false;
					// R.id.icon
					cursorIndices[7] = SonetProvider.StatusesStylesColumnsV1.icon.ordinal();
					viewTypes[7] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
					layoutIds[7] = R.id.icon;
					defaultResource[7] = 0;
					clickable[7] = false;
					
					replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
					replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
					replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
					replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_DEFAULT_RESOURCES, defaultResource);
					replaceDummy.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
				} else {
					// provider
					uri = Uri.withAppendedPath(Statuses_styles.CONTENT_URI, widgetId);
					projection = new String[]{Statuses_styles._ID, Statuses_styles.FRIEND, Statuses_styles.PROFILE, Statuses_styles.MESSAGE, Statuses_styles.CREATEDTEXT, Statuses_styles.MESSAGES_COLOR, Statuses_styles.FRIEND_COLOR, Statuses_styles.CREATED_COLOR, Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.STATUS_BG, Statuses_styles.ICON};
					
					// Put widget info
					replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);

					BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.widget_item);
					
					// onclick

					Intent i = new Intent(context, SonetWidget.class)
					.setAction(LauncherIntent.Action.ACTION_VIEW_CLICK)
					.setData(uri)
					.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
					PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
					
					// mapping

					itemViews.SetBoundOnClickIntent(R.id.item, pi, LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.StatusesStylesColumns._id.ordinal());

					itemViews.setBoundCharSequence(R.id.friend_bg_clear, "setText", SonetProvider.StatusesStylesColumns.friend.ordinal(), 0);
					itemViews.setBoundFloat(R.id.friend_bg_clear, "setTextSize", SonetProvider.StatusesStylesColumns.friend_textsize.ordinal());

					itemViews.setBoundCharSequence(R.id.message_bg_clear, "setText", SonetProvider.StatusesStylesColumns.message.ordinal(), 0);
					itemViews.setBoundFloat(R.id.message_bg_clear, "setTextSize", SonetProvider.StatusesStylesColumns.messages_textsize.ordinal());

					itemViews.setBoundBitmap(R.id.status_bg, "setImageBitmap", SonetProvider.StatusesStylesColumns.status_bg.ordinal(), 0);

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
				}
				
				// provider
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, uri.toString());
				// Other arguments for managed query
				String whereClause = Statuses_styles.WIDGET + "=?";
				String[] selectionArgs = new String[]{widgetId};
				String sortOrder = Statuses_styles.CREATED + " desc";
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, projection);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
				replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, sortOrder);

				context.sendBroadcast(replaceDummy);
				Sonet.sWidgetsContext.remove(appWidgetId);

				stopSelf();
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
