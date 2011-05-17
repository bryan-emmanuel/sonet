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

import java.util.HashMap;

import static com.piusvelte.sonet.Sonet.TABLE_ENTITIES;
import static com.piusvelte.sonet.Sonet.TABLE_STATUSES;
import static com.piusvelte.sonet.Sonet.TABLE_WIDGETS;
import static com.piusvelte.sonet.Sonet.VIEW_STATUSES_STYLES;

import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;
import com.piusvelte.sonet.Sonet.Statuses;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class SonetProvider extends ContentProvider {

	public static final String AUTHORITY = "com.piusvelte.sonet.SonetProvider";

	private static final UriMatcher sUriMatcher;

	private static final int WIDGETS = 0;
	private static final int STATUSES = 1;
	private static final int STATUSES_STYLES = 2;
	private static final int STATUSES_STYLES_WIDGET = 3;
	private static final int ENTITIES = 4;

	private static HashMap<String, String> widgetsProjectionMap;

	private static HashMap<String, String> statusesProjectionMap;
	
	private static HashMap<String, String> statuses_stylesProjectionMap;
	
	private static HashMap<String, String> entitiesProjectionMap;

	private Sonet.DatabaseHelper mDatabaseHelper;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		sUriMatcher.addURI(AUTHORITY, TABLE_WIDGETS, WIDGETS);

		widgetsProjectionMap = new HashMap<String, String>();
		widgetsProjectionMap.put(Widgets._ID, Widgets._ID);
		widgetsProjectionMap.put(Widgets.WIDGET, Widgets.WIDGET);
		widgetsProjectionMap.put(Widgets.INTERVAL, Widgets.INTERVAL);
		widgetsProjectionMap.put(Widgets.HASBUTTONS, Widgets.HASBUTTONS);
		widgetsProjectionMap.put(Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_BG_COLOR);
		widgetsProjectionMap.put(Widgets.BUTTONS_COLOR, Widgets.BUTTONS_COLOR);
		widgetsProjectionMap.put(Widgets.MESSAGES_BG_COLOR, Widgets.MESSAGES_BG_COLOR);
		widgetsProjectionMap.put(Widgets.MESSAGES_COLOR, Widgets.MESSAGES_COLOR);
		widgetsProjectionMap.put(Widgets.TIME24HR, Widgets.TIME24HR);
		widgetsProjectionMap.put(Widgets.FRIEND_COLOR, Widgets.FRIEND_COLOR);
		widgetsProjectionMap.put(Widgets.CREATED_COLOR, Widgets.CREATED_COLOR);
		widgetsProjectionMap.put(Widgets.SCROLLABLE, Widgets.SCROLLABLE);
		widgetsProjectionMap.put(Widgets.BUTTONS_TEXTSIZE, Widgets.BUTTONS_TEXTSIZE);
		widgetsProjectionMap.put(Widgets.MESSAGES_TEXTSIZE, Widgets.MESSAGES_TEXTSIZE);
		widgetsProjectionMap.put(Widgets.FRIEND_TEXTSIZE, Widgets.FRIEND_TEXTSIZE);
		widgetsProjectionMap.put(Widgets.CREATED_TEXTSIZE, Widgets.CREATED_TEXTSIZE);
		widgetsProjectionMap.put(Widgets.ACCOUNT, Widgets.ACCOUNT);
		widgetsProjectionMap.put(Widgets.ICON, Widgets.ICON);
		widgetsProjectionMap.put(Widgets.STATUSES_PER_ACCOUNT, Widgets.STATUSES_PER_ACCOUNT);
		widgetsProjectionMap.put(Widgets.BACKGROUND_UPDATE, Widgets.BACKGROUND_UPDATE);

		sUriMatcher.addURI(AUTHORITY, TABLE_STATUSES, STATUSES);

		statusesProjectionMap = new HashMap<String, String>();
		statusesProjectionMap.put(Statuses._ID, Statuses._ID);
		statusesProjectionMap.put(Statuses.CREATED, Statuses.CREATED);
		statusesProjectionMap.put(Statuses.MESSAGE, Statuses.MESSAGE);
		statusesProjectionMap.put(Statuses.SERVICE, Statuses.SERVICE);
		statusesProjectionMap.put(Statuses.CREATEDTEXT, Statuses.CREATEDTEXT);
		statusesProjectionMap.put(Statuses.WIDGET, Statuses.WIDGET);
		statusesProjectionMap.put(Statuses.ICON, Statuses.ICON);
		statusesProjectionMap.put(Statuses.SID, Statuses.SID);
		statusesProjectionMap.put(Statuses.ENTITY, Statuses.ENTITY);

		sUriMatcher.addURI(AUTHORITY, VIEW_STATUSES_STYLES, STATUSES_STYLES);
		sUriMatcher.addURI(AUTHORITY, VIEW_STATUSES_STYLES + "/*", STATUSES_STYLES_WIDGET);

		statuses_stylesProjectionMap = new HashMap<String, String>();
		statuses_stylesProjectionMap.put(Statuses_styles._ID, Statuses_styles._ID);
		statuses_stylesProjectionMap.put(Statuses_styles.CREATED, Statuses_styles.CREATED);
		statuses_stylesProjectionMap.put(Statuses_styles.FRIEND, Statuses_styles.FRIEND);
		statuses_stylesProjectionMap.put(Statuses_styles.PROFILE, Statuses_styles.PROFILE);
		statuses_stylesProjectionMap.put(Statuses_styles.MESSAGE, Statuses_styles.MESSAGE);
		statuses_stylesProjectionMap.put(Statuses_styles.SERVICE, Statuses_styles.SERVICE);
		statuses_stylesProjectionMap.put(Statuses_styles.CREATEDTEXT, Statuses_styles.CREATEDTEXT);
		statuses_stylesProjectionMap.put(Statuses_styles.WIDGET, Statuses_styles.WIDGET);
		statuses_stylesProjectionMap.put(Statuses_styles.ACCOUNT, Statuses_styles.ACCOUNT);
		statuses_stylesProjectionMap.put(Statuses_styles.MESSAGES_COLOR, Statuses_styles.MESSAGES_COLOR);
		statuses_stylesProjectionMap.put(Statuses_styles.FRIEND_COLOR, Statuses_styles.FRIEND_COLOR);
		statuses_stylesProjectionMap.put(Statuses_styles.CREATED_COLOR, Statuses_styles.CREATED_COLOR);
		statuses_stylesProjectionMap.put(Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.MESSAGES_TEXTSIZE);
		statuses_stylesProjectionMap.put(Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE);
		statuses_stylesProjectionMap.put(Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE);
		statuses_stylesProjectionMap.put(Statuses_styles.STATUS_BG, Statuses_styles.STATUS_BG);
		statuses_stylesProjectionMap.put(Statuses_styles.ICON, Statuses_styles.ICON);
		statuses_stylesProjectionMap.put(Statuses_styles.SID, Statuses_styles.SID);
		statuses_stylesProjectionMap.put(Statuses_styles.ENTITY, Statuses_styles.ENTITY);
		statuses_stylesProjectionMap.put(Statuses_styles.ESID, Statuses_styles.ESID);
		
		sUriMatcher.addURI(AUTHORITY, TABLE_ENTITIES, ENTITIES);
		
		entitiesProjectionMap = new HashMap<String, String>();
		entitiesProjectionMap.put(Entities._ID, Entities._ID);
		entitiesProjectionMap.put(Entities.ESID, Entities.ESID);
		entitiesProjectionMap.put(Entities.FRIEND, Entities.FRIEND);
		entitiesProjectionMap.put(Entities.PROFILE, Entities.PROFILE);
		entitiesProjectionMap.put(Entities.ACCOUNT, Entities.ACCOUNT);
	}
	
	public enum StatusesStylesColumns {
		_id, friend, profile, message, createdtext, messages_color, friend_color, created_color, messages_textsize, friend_textsize, created_textsize, status_bg, icon
	}

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new Sonet.DatabaseHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case WIDGETS:
			return Widgets.CONTENT_TYPE;
		case STATUSES:
			return Statuses.CONTENT_TYPE;
		case STATUSES_STYLES:
			return Statuses_styles.CONTENT_TYPE;
		case STATUSES_STYLES_WIDGET:
			return Statuses_styles.CONTENT_TYPE;
		case ENTITIES:
			return Entities.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String whereClause, String[] whereArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case WIDGETS:
			count = db.delete(TABLE_WIDGETS, whereClause, whereArgs);
			break;
		case STATUSES:
			count = db.delete(TABLE_STATUSES, whereClause, whereArgs);
			break;
		case ENTITIES:
			count = db.delete(TABLE_ENTITIES, whereClause, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		long rowId;
		Uri returnUri;
		switch (sUriMatcher.match(uri)) {
		case WIDGETS:
			rowId = db.insert(TABLE_WIDGETS, Widgets._ID, values);
			returnUri = ContentUris.withAppendedId(Widgets.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			break;
		case STATUSES:
			rowId = db.insert(TABLE_STATUSES, Statuses._ID, values);
			returnUri = ContentUris.withAppendedId(Statuses.CONTENT_URI, rowId);
			// many statuses will be inserted at once, so don't trigger a refresh for each one
//			getContext().getContentResolver().notifyChange(returnUri, null);
			break;
		case ENTITIES:
			rowId = db.insert(TABLE_ENTITIES, Entities._ID, values);
			returnUri = ContentUris.withAppendedId(Entities.CONTENT_URI, rowId);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return returnUri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case WIDGETS:
			qb.setTables(TABLE_WIDGETS);
			qb.setProjectionMap(widgetsProjectionMap);
			break;
		case STATUSES:
			qb.setTables(TABLE_STATUSES);
			qb.setProjectionMap(statusesProjectionMap);
			break;
		case STATUSES_STYLES:
			qb.setTables(VIEW_STATUSES_STYLES);
			qb.setProjectionMap(statuses_stylesProjectionMap);
			break;
		case STATUSES_STYLES_WIDGET:
			qb.setTables(VIEW_STATUSES_STYLES);
			qb.setProjectionMap(statuses_stylesProjectionMap);
			selection = Statuses_styles.WIDGET + "=?";
			selectionArgs = new String[]{uri.getLastPathSegment()};
			break;
		case ENTITIES:
			qb.setTables(TABLE_ENTITIES);
			qb.setProjectionMap(entitiesProjectionMap);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

		int count;
		switch (sUriMatcher.match(uri)) {
		case WIDGETS:
			count = db.update(TABLE_WIDGETS, values, selection, selectionArgs);
			break;
		case STATUSES:
			count = db.update(TABLE_STATUSES, values, selection, selectionArgs);
			break;
		case ENTITIES:
			count = db.update(TABLE_ENTITIES, values, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
