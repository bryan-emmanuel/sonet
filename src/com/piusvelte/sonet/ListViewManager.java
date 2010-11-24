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

import mobi.intuitit.android.content.LauncherIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
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
            int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID);

            if (appWidgetId < 0) {
                    Log.d(TAG, "Cannot get app widget id from ready intent");
                    return;
            }

            Intent replaceDummy = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);

            // Put widget info
            replaceDummy.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.messages);

            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);

            // Give a layout resource to be inflated. If this is not given, Home++
            // will create one

            // Put adapter info
            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID, R.layout.widget_listview);
            replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.widget_item);
            putProvider(replaceDummy, SonetProvider.CONTENT_URI.buildUpon().appendEncodedPath(Integer.toString(appWidgetId)).toString());
            putMapping(replaceDummy);

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
    public static void putMapping(Intent intent) {
            if (intent == null)
                    return;

            final int layout_items = 4;

            int[] cursorIndices = new int[layout_items];
            int[] viewTypes = new int[layout_items];
            int[] layoutIds = new int[layout_items];
            boolean[] clickable = new boolean[layout_items];
            int iItem = 0;
            
            cursorIndices[iItem] = SonetProvider.SonetProviderColumns.profile.ordinal();
            viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
            layoutIds[iItem] = R.id.profile;
            clickable[iItem] = false;

            iItem++;
            
            cursorIndices[iItem] = SonetProvider.SonetProviderColumns.friend.ordinal();
            viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
            layoutIds[iItem] = R.id.screenname;
            clickable[iItem] = false;
            
            iItem++;
            
            cursorIndices[iItem] = SonetProvider.SonetProviderColumns.createdtext.ordinal();
            viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
            layoutIds[iItem] = R.id.created;
            clickable[iItem] = false;
            
            iItem++;
            
            cursorIndices[iItem] = SonetProvider.SonetProviderColumns.message.ordinal();
            viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
            layoutIds[iItem] = R.id.message;
            clickable[iItem] = false;       

            intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
            intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
            intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
            intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);

    }
}
