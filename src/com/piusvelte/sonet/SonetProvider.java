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

import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_STATUSES;
import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATEDTEXT;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND;
import static com.piusvelte.sonet.SonetDatabaseHelper.PROFILE;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGE;
import static com.piusvelte.sonet.SonetDatabaseHelper.LINK;
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.Sonet.TAG;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class SonetProvider extends ContentProvider {

	public static final String AUTHORITY = "com.piusvelte.sonet.SonetProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/statuses");

	private static final UriMatcher sUriMatcher;

	private static final int STATUSES = 0;
	
	private SonetDatabaseHelper mSonetDatabaseHelper;

	public static final String[] PROJECTION_APPWIDGETS = new String[] {
		SonetProviderColumns._id.toString(),
		SonetProviderColumns.created.toString(),
		SonetProviderColumns.link.toString(),
		SonetProviderColumns.friend.toString(),
		SonetProviderColumns.profile.toString(),
		SonetProviderColumns.message.toString(),
		SonetProviderColumns.service.toString(),
		SonetProviderColumns.createdtext.toString(),
		SonetProviderColumns.widget.toString()};

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, TABLE_STATUSES + "/*", STATUSES);
	}

	public enum SonetProviderColumns {
		_id, created, link, friend, profile, message, service, createdtext, widget
	}

	@Override
	public int delete(Uri uri, String whereClause, String[] whereArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {
		mSonetDatabaseHelper = new SonetDatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Log.v(TAG,"query widget: "+uri.getLastPathSegment());
		SQLiteDatabase db = mSonetDatabaseHelper.getReadableDatabase();
		Cursor c = db.rawQuery("select " + _ID + "," + CREATED + "," + LINK + "," + FRIEND + "," + PROFILE + "," + MESSAGE + "," + SERVICE + "," + CREATEDTEXT + "," + WIDGET + " from " + TABLE_STATUSES + " where " + WIDGET + "=" + uri.getLastPathSegment() + " order by " + CREATED + " desc", null);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}


    public static void notifyDatabaseModification(Context context, int appWidgetId) {
            Uri widgetUri = CONTENT_URI.buildUpon().appendEncodedPath(Integer.toString(appWidgetId)).build();
            Log.d(TAG, "notifyDatabaseModification -> UPDATE widgetUri : " + widgetUri);
            context.getContentResolver().notifyChange(widgetUri, null);
    }

}
