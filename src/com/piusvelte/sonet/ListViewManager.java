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

import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_COLOR;
//import static com.piusvelte.sonet.SonetDatabaseHelper.HASBUTTONS;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.BoundRemoteViews;
import mobi.intuitit.android.widget.SimpleRemoteViews;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class ListViewManager {

    private static final String TAG = "ListViewManager";

    /**
     * Receive ready intent from Launcher, prepare scroll view resources
     */
    public static void onAppWidgetReady(Context context, Intent intent) {
            if (intent == null)
                    return;

            Log.d(TAG, "onAppWidgetReady");

            // try new method
            int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            if (appWidgetId < 0) {
                    Log.d(TAG, "Cannot get app widget id from ready intent");
                    return;
            }
            
            String appWidgetUri = SonetProvider.CONTENT_URI.buildUpon().appendEncodedPath(Integer.toString(appWidgetId)).toString();
            Intent replaceDummy = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);
            // Put widget info
            replaceDummy.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);
            replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.messages);
            
            // pull settings to style the list
//			Boolean hasbuttons;
			int	messages_color,
			friend_color,
			created_color;
			SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(context);
			SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();
			Cursor settings = db.rawQuery("select " + _ID /*+ "," + HASBUTTONS*/ + "," + MESSAGES_COLOR + "," + FRIEND_COLOR + "," + CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + appWidgetId, null);
			if (settings.getCount() > 0) {
				settings.moveToFirst();
//				hasbuttons = settings.getInt(settings.getColumnIndex(HASBUTTONS)) == 1;
				messages_color = settings.getInt(settings.getColumnIndex(MESSAGES_COLOR));
				friend_color = settings.getInt(settings.getColumnIndex(FRIEND_COLOR));
				created_color = settings.getInt(settings.getColumnIndex(CREATED_COLOR));
			} else {
				// this shouldn't occur, as the service should migrate the preferences to the db before this method is called
				SharedPreferences sp = (SharedPreferences) context.getSharedPreferences(context.getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
//				hasbuttons = sp.getBoolean(context.getString(R.string.key_display_buttons), true);
				messages_color = Integer.parseInt(sp.getString(context.getString(R.string.key_body_text), context.getString(R.string.default_message_color)));
				friend_color = Integer.parseInt(sp.getString(context.getString(R.string.key_friend_text), context.getString(R.string.default_friend_color)));
				created_color = Integer.parseInt(sp.getString(context.getString(R.string.key_created_text), context.getString(R.string.default_created_color)));
			}
			settings.close();
			db.close();
			sonetDatabaseHelper.close();

//            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID, R.layout.widget_listview);
//            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.widget_item);
			
            SimpleRemoteViews listView = new SimpleRemoteViews(R.layout.widget_listview);
            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_REMOTEVIEWS, listView);
            
            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_REMOTEVIEWS, listView);
            
            BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.widget_item);
            itemViews.setBoundBitmap(R.id.profile, "setImageBitmap", SonetProvider.SonetProviderColumns.profile.ordinal(), 0);
            itemViews.setBoundCharSequence(R.id.friend, "setText", SonetProvider.SonetProviderColumns.friend.ordinal(), 0);
            itemViews.setTextColor(R.id.friend, friend_color);
            itemViews.setBoundCharSequence(R.id.created, "setText", SonetProvider.SonetProviderColumns.createdtext.ordinal(), 0);
            itemViews.setTextColor(R.id.created, created_color);
            itemViews.setBoundCharSequence(R.id.message, "setText", SonetProvider.SonetProviderColumns.message.ordinal(), 0);
            itemViews.setTextColor(R.id.message, messages_color);

            itemViews.SetBoundOnClickIntent(R.id.item, PendingIntent.getBroadcast(context, 0, new Intent(context, context.getClass()).setAction(LauncherIntent.Action.ACTION_VIEW_CLICK).setData(Uri.parse(appWidgetUri)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId), 0), LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS, SonetProvider.SonetProviderColumns.link.ordinal());

            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);

            putProvider(replaceDummy, appWidgetUri);
//            putMapping(replaceDummy);

            // Launcher can set onClickListener for each children of an item. Without
            // explictly put this
            // extra, it will just set onItemClickListener by default
            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);

            // Send it out
            context.sendBroadcast(replaceDummy);
    }

    /**
     * Put provider info as extras in the specified intent
     * 
     * @param intent
     */
    public static void putProvider(Intent intent, String widgetUri) {
            if (intent == null)
                    return;

            String whereClause = null;
            String orderBy = null;
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
     * Put mapping info as extras in intent
     */
//    public static void putMapping(Intent intent) {
//            if (intent == null)
//                    return;
//
//            final int layout_items = 4;
//
//            int[] cursorIndices = new int[layout_items];
//            int[] viewTypes = new int[layout_items];
//            int[] layoutIds = new int[layout_items];
//            boolean[] clickable = new boolean[layout_items];
//            int iItem = 0;
//            
//            cursorIndices[iItem] = SonetProvider.SonetProviderColumns.profile.ordinal();
//            viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
//            layoutIds[iItem] = R.id.profile;
//            clickable[iItem] = false;
//
//            iItem++;
//            
//            cursorIndices[iItem] = SonetProvider.SonetProviderColumns.friend.ordinal();
//            viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
//            layoutIds[iItem] = R.id.screenname;
//            clickable[iItem] = false;
//            
//            iItem++;
//            
//            cursorIndices[iItem] = SonetProvider.SonetProviderColumns.createdtext.ordinal();
//            viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
//            layoutIds[iItem] = R.id.created;
//            clickable[iItem] = false;
//            
//            iItem++;
//            
//            cursorIndices[iItem] = SonetProvider.SonetProviderColumns.message.ordinal();
//            viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
//            layoutIds[iItem] = R.id.message;
//            clickable[iItem] = false;       
//
//            intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
//            intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
//            intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
//            intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
//
//    }
}
