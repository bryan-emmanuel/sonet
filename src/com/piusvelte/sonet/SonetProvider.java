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

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widgets;
import com.piusvelte.sonet.Sonet.Statuses;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class SonetProvider extends ContentProvider {

	public static final String AUTHORITY = "com.piusvelte.sonet.SonetProvider";

	private static final UriMatcher sUriMatcher;

	private static final int ACCOUNTS = 0;
	private static final int WIDGETS = 1;
	private static final int STATUSES = 2;
	private static final int STATUSES_STYLES = 3;
	private static final int STATUSES_STYLES_WIDGET = 4;
	private static final int ENTITIES = 5;

	private static final String DATABASE_NAME = "sonet.db";
	private static final int DATABASE_VERSION = 12;

	private static final String TABLE_ACCOUNTS = "accounts";
	private static HashMap<String, String> accountsProjectionMap;

	private static final String TABLE_WIDGETS = "widgets";
	private static HashMap<String, String> widgetsProjectionMap;

	private static final String TABLE_STATUSES = "statuses";
	private static HashMap<String, String> statusesProjectionMap;
	
	private static final String VIEW_STATUSES_STYLES = "statuses_styles";
	private static HashMap<String, String> statuses_stylesProjectionMap;
	
	private static final String TABLE_ENTITIES = "entities";
	private static HashMap<String, String> entitiesProjectionMap;

	private DatabaseHelper mDatabaseHelper;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		sUriMatcher.addURI(AUTHORITY, TABLE_ACCOUNTS, ACCOUNTS);

		accountsProjectionMap = new HashMap<String, String>();
		accountsProjectionMap.put(Accounts._ID, Accounts._ID);
		accountsProjectionMap.put(Accounts.USERNAME, Accounts.USERNAME);
		accountsProjectionMap.put(Accounts.TOKEN, Accounts.TOKEN);
		accountsProjectionMap.put(Accounts.SECRET, Accounts.SECRET);
		accountsProjectionMap.put(Accounts.SERVICE, Accounts.SERVICE);
		accountsProjectionMap.put(Accounts.EXPIRY, Accounts.EXPIRY);
		accountsProjectionMap.put(Accounts.WIDGET, Accounts.WIDGET);
		accountsProjectionMap.put(Accounts.SID, Accounts.SID);

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
//		statusesProjectionMap.put(Statuses.FRIEND, Statuses.FRIEND);
//		statusesProjectionMap.put(Statuses.PROFILE, Statuses.PROFILE);
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
		statuses_stylesProjectionMap.put(Statuses_styles.MESSAGES_COLOR, Statuses_styles.MESSAGES_COLOR);
		statuses_stylesProjectionMap.put(Statuses_styles.FRIEND_COLOR, Statuses_styles.FRIEND_COLOR);
		statuses_stylesProjectionMap.put(Statuses_styles.CREATED_COLOR, Statuses_styles.CREATED_COLOR);
		statuses_stylesProjectionMap.put(Statuses_styles.MESSAGES_TEXTSIZE, Statuses_styles.MESSAGES_TEXTSIZE);
		statuses_stylesProjectionMap.put(Statuses_styles.FRIEND_TEXTSIZE, Statuses_styles.FRIEND_TEXTSIZE);
		statuses_stylesProjectionMap.put(Statuses_styles.CREATED_TEXTSIZE, Statuses_styles.CREATED_TEXTSIZE);
		statuses_stylesProjectionMap.put(Statuses_styles.STATUS_BG, Statuses_styles.STATUS_BG);
		statuses_stylesProjectionMap.put(Statuses_styles.ICON, Statuses_styles.ICON);
		statuses_stylesProjectionMap.put(Statuses_styles.SID, Statuses_styles.SID);
		
		sUriMatcher.addURI(AUTHORITY, TABLE_ENTITIES, ENTITIES);
		
		entitiesProjectionMap.put(Entities._ID, Entities._ID);
		entitiesProjectionMap.put(Entities.SID, Entities.SID);
		entitiesProjectionMap.put(Entities.FRIEND, Entities.FRIEND);
		entitiesProjectionMap.put(Entities.PROFILE, Entities.PROFILE);
		entitiesProjectionMap.put(Entities.ACCOUNT, Entities.ACCOUNT);
	}
	
	public enum StatusesStylesColumns {
		_id, created, friend, profile, message, service, createdtext, widget, messages_color, friend_color, created_color, messages_textsize, friend_textsize, created_textsize, status_bg, icon, sid, entity
	}

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case ACCOUNTS:
			return Accounts.CONTENT_TYPE;
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
		case ACCOUNTS:
			count = db.delete(TABLE_ACCOUNTS, whereClause, whereArgs);
			break;
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
		case ACCOUNTS:
			rowId = db.insert(TABLE_ACCOUNTS, Accounts._ID, values);
			returnUri = ContentUris.withAppendedId(Accounts.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			break;
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
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return returnUri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case ACCOUNTS:
			qb.setTables(TABLE_ACCOUNTS);
			qb.setProjectionMap(accountsProjectionMap);
			break;
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
		case ACCOUNTS:
			count = db.update(TABLE_ACCOUNTS, values, selection, selectionArgs);
			break;
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

	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table if not exists " + TABLE_ACCOUNTS
					+ " (" + Accounts._ID + " integer primary key autoincrement, "
					+ Accounts.USERNAME + " text, "
					+ Accounts.TOKEN + " text, "
					+ Accounts.SECRET + " text, "
					+ Accounts.SERVICE + " integer, "
					+ Accounts.EXPIRY + " integer, "
					+ Accounts.WIDGET + " integer, "
					+ Accounts.SID + " text);");
			db.execSQL("create table if not exists " + TABLE_WIDGETS
					+ " (" + Widgets._ID + " integer primary key autoincrement, "
					+ Widgets.WIDGET + " integer, "
					+ Widgets.INTERVAL + " integer, "
					+ Widgets.HASBUTTONS + " integer, "
					+ Widgets.BUTTONS_BG_COLOR + " integer, "
					+ Widgets.BUTTONS_COLOR + " integer, "
					+ Widgets.FRIEND_COLOR + " integer, "
					+ Widgets.CREATED_COLOR + " integer, "
					+ Widgets.MESSAGES_BG_COLOR + " integer, "
					+ Widgets.MESSAGES_COLOR + " integer, "
					+ Widgets.TIME24HR + " integer, "
					+ Widgets.SCROLLABLE + " integer, "
					+ Widgets.BUTTONS_TEXTSIZE + " integer, "
					+ Widgets.MESSAGES_TEXTSIZE + " integer, "
					+ Widgets.FRIEND_TEXTSIZE + " integer, "
					+ Widgets.CREATED_TEXTSIZE + " integer, "
					+ Widgets.ACCOUNT + " integer, "
					+ Widgets.ICON + " integer, "
					+ Widgets.STATUSES_PER_ACCOUNT + " integer, "
					+ Widgets.BACKGROUND_UPDATE + " integer);");
			db.execSQL("create table if not exists " + TABLE_STATUSES
					+ " (" + Statuses._ID + " integer primary key autoincrement, "
					+ Statuses.CREATED + " integer, "
					+ Statuses.MESSAGE + " text, "
					+ Statuses.SERVICE + " integer, "
					+ Statuses.CREATEDTEXT + " text, "
					+ Statuses.WIDGET + " integer, "
					+ Statuses.ACCOUNT + " integer, "
					+ Statuses.STATUS_BG + " blob, "
					+ Statuses.ICON + " blob, "
					+ Statuses.SID + " text, "
					+ Statuses.ENTITY + " integer);");
			db.execSQL("create table if not exists " + TABLE_ENTITIES
					+ " (" + Entities._ID + " integer primary key autoincrement, "
					+ Entities.FRIEND + " text, "
					+ Entities.PROFILE + " blob, "
					+ Entities.ACCOUNT + " integer, "
					+ Entities.SID + " text);");
			db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
					TABLE_STATUSES + "." + Statuses._ID + ","
					+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
					+ Entities.FRIEND + " as " + Statuses_styles.FRIEND + ","
					+ Entities.PROFILE + " as " + Statuses_styles.PROFILE + ","
					+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
					+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
					+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
					+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
					+ Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
					+ "(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR + ","
					+ "(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR + ","
					+ "(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR + ","
					+ "(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
					+ "(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
					+ "(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
					+ ") when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
					+ " is null) else "	+ Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
					+ Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG + ","
					+ Statuses.ICON + " as " + Statuses_styles.ICON + ","
					+ Statuses.SID + " as " + Statuses_styles.SID
					+ " from " + TABLE_STATUSES + "," + TABLE_ENTITIES
					+ " where " + Entities._ID + "=" + Statuses.ENTITY);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				// add column for expiry
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text not null, "
						+ Accounts.TOKEN + " text not null, "
						+ Accounts.SECRET + " text not null, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," + Accounts.SECRET + "," + Accounts.SERVICE + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 3) {
				// remove not null constraints as facebook uses oauth2 and doesn't require a secret, add timezone
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ "timezone integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," + Accounts.SECRET + "," + Accounts.SERVICE + "," + Accounts.EXPIRY + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 4) {
				// add column for widget
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ "timezone integer, "
						+ Accounts.WIDGET + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," + Accounts.SECRET + "," + Accounts.SERVICE + "," + Accounts.EXPIRY + ",timezone,\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				// move preferences to db
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer);");
			}
			if (oldVersion < 5) {
				// cache for statuses
				db.execSQL("create table if not exists " + TABLE_STATUSES
						+ " (" + Statuses._ID + " integer primary key autoincrement, "
						+ Statuses.CREATED + " integer, "
						+ "link text, "
						+ "friend text, "
						+ "profile blob, "
						+ Statuses.MESSAGE + " text, "
						+ Statuses.SERVICE + " integer, "
						+ Statuses.CREATEDTEXT + " text, "
						+ Statuses.WIDGET + " integer);");
				// column for scrollable
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ",0 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
			}
			if (oldVersion < 6) {
				// add columns for textsize
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ",14,14,14,14 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
			}
			if (oldVersion < 7) {
				// add column for account to handle account specific widget settings
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer, "
						+ Widgets.ACCOUNT + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ","
						+ Widgets.BUTTONS_TEXTSIZE + ","
						+ Widgets.MESSAGES_TEXTSIZE + ","
						+ Widgets.FRIEND_TEXTSIZE + ","
						+ Widgets.CREATED_TEXTSIZE + ","
						+ Sonet.INVALID_ACCOUNT_ID + " from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				// add column for account to handle account specific widget settings
				// add column for status background and rename createdText > createdtext
				db.execSQL("create temp table " + TABLE_STATUSES + "_bkp as select * from " + TABLE_STATUSES + ";");
				db.execSQL("drop table if exists " + TABLE_STATUSES + ";");
				db.execSQL("create table if not exists " + TABLE_STATUSES
						+ " (" + Statuses._ID + " integer primary key autoincrement, "
						+ Statuses.CREATED + " integer, "
						+ "link text, "
						+ "friend text, "
						+ "profile blob, "
						+ Statuses.MESSAGE + " text, "
						+ Statuses.SERVICE + " integer, "
						+ Statuses.CREATEDTEXT + " text, "
						+ Statuses.WIDGET + " integer, "
						+ Statuses.ACCOUNT + " integer, "
						+ Statuses.STATUS_BG + " blob);");
				db.execSQL("insert into " + TABLE_STATUSES
						+ " select "
						+ Statuses._ID + ","
						+ Statuses.CREATED + ","
						+ "link,"
						+ "profile,"
						+ Statuses.MESSAGE + ","
						+ Statuses.SERVICE + ","
						+ "createdText,"
						+ Statuses.WIDGET + ","
						+ Sonet.INVALID_ACCOUNT_ID + ",null from " + TABLE_STATUSES + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				// create a view for the statuses and account/widget/default styles
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + " as " + Statuses_styles._ID + ","
						+ Statuses.CREATED + ","
						+ "link,"
						+ "friend,"
						+ "profile,"
						+ Statuses.MESSAGE + ","
						+ Statuses.SERVICE + ","
						+ Statuses.CREATEDTEXT + ","
						+ Statuses.WIDGET + ","
						+ Statuses.ACCOUNT + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_friend_color
						+ " else (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.FRIEND_COLOR + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_created_color
						+ " else (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.CREATED_COLOR + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_message_color
						+ " else (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.MESSAGES_COLOR + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_messages_textsize
						+ " else (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_friend_textsize
						+ " else (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
						+ "(case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is null then "
						+ "case when (select " + Widgets._ID + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is null then "
						+ Sonet.default_created_textsize
						+ " else (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) end "
						+ "else (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
						+ Statuses.STATUS_BG
						+ " from " + TABLE_STATUSES);
			}
			if (oldVersion < 8) {
				// change the view to be more efficient
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + ","
						+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
						+ "link as link,"
						+ "friend as " + Statuses_styles.FRIEND + ","
						+ "profile as " + Statuses_styles.PROFILE + ","
						+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
						+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
						+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
						+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
						+ Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
						+ "(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR + ","
						+ "(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
						+ "(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
						+ "(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
						+ Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG
						+ " from " + TABLE_STATUSES);
			}
			if (oldVersion < 9) {
				// support additional timezones, with partial hour increments
				// change timezone column from integer to real
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ "timezone real, "
						+ Accounts.WIDGET + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS
						+ " select "
						+ Accounts._ID + ","
						+ Accounts.USERNAME + ","
						+ Accounts.TOKEN + ","
						+ Accounts.SECRET + ","
						+ Accounts.SERVICE + ","
						+ Accounts.EXPIRY + ","
						+ "timezone,"
						+ Accounts.WIDGET + " from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 10) {
				// add support for service icons
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer, "
						+ Widgets.ACCOUNT + " integer, "
						+ Widgets.ICON + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ","
						+ Widgets.BUTTONS_TEXTSIZE + ","
						+ Widgets.MESSAGES_TEXTSIZE + ","
						+ Widgets.FRIEND_TEXTSIZE + ","
						+ Widgets.CREATED_TEXTSIZE + ","
						+ Widgets.ACCOUNT + ",1 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("create temp table " + TABLE_STATUSES + "_bkp as select * from " + TABLE_STATUSES + ";");
				db.execSQL("drop table if exists " + TABLE_STATUSES + ";");
				db.execSQL("create table if not exists " + TABLE_STATUSES
						+ " (" + Statuses._ID + " integer primary key autoincrement, "
						+ Statuses.CREATED + " integer, "
						+ "link text, "
						+ "friend text, "
						+ "profile blob, "
						+ Statuses.MESSAGE + " text, "
						+ Statuses.SERVICE + " integer, "
						+ Statuses.CREATEDTEXT + " text, "
						+ Statuses.WIDGET + " integer, "
						+ Statuses.ACCOUNT + " integer, "
						+ Statuses.STATUS_BG + " blob, "
						+ Statuses.ICON + " blob);");
				db.execSQL("insert into " + TABLE_STATUSES
						+ " select "
						+ Statuses._ID + ","
						+ Statuses.CREATED + ","
						+ "link,"
						+ "friend,"
						+ "profile,"
						+ Statuses.MESSAGE + ","
						+ Statuses.SERVICE + ","
						+ Statuses.CREATEDTEXT + ","
						+ Statuses.WIDGET + ","
						+ Statuses.ACCOUNT + ","
						+ Statuses.STATUS_BG + ",null from " + TABLE_STATUSES + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + ","
						+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
						+ "link as link,"
						+ "friend as " + Statuses_styles.FRIEND + ","
						+ "profile as " + Statuses_styles.PROFILE + ","
						+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
						+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
						+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
						+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
						+ Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
						+ "(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR + ","
						+ "(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
						+ "(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
						+ "(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
						+ Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG + ","
						+ Statuses.ICON + " as " + Statuses_styles.ICON
						+ " from " + TABLE_STATUSES);
			}
			if (oldVersion < 11) {
				// add support for status limit
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer, "
						+ Widgets.ACCOUNT + " integer, "
						+ Widgets.ICON + " integer, "
						+ Widgets.STATUSES_PER_ACCOUNT + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ","
						+ Widgets.BUTTONS_TEXTSIZE + ","
						+ Widgets.MESSAGES_TEXTSIZE + ","
						+ Widgets.FRIEND_TEXTSIZE + ","
						+ Widgets.CREATED_TEXTSIZE + ","
						+ Widgets.ACCOUNT + ","
						+ Widgets.ICON + "," + Sonet.default_statuses_per_account + " from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				// using device timezone, doesn't need to be stored now
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ Accounts.WIDGET + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS
						+ " select "
						+ Accounts._ID + ","
						+ Accounts.USERNAME + ","
						+ Accounts.TOKEN + ","
						+ Accounts.SECRET + ","
						+ Accounts.SERVICE + ","
						+ Accounts.EXPIRY + ","
						+ Accounts.WIDGET + " from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 12) {
				// store the service id's for posting and linking
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ Accounts.WIDGET + " integer, "
						+ Accounts.SID + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS
						+ " select "
						+ Accounts._ID + ","
						+ Accounts.USERNAME + ","
						+ Accounts.TOKEN + ","
						+ Accounts.SECRET + ","
						+ Accounts.SERVICE + ","
						+ Accounts.EXPIRY + ","
						+ Accounts.WIDGET + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("create temp table " + TABLE_STATUSES + "_bkp as select * from " + TABLE_STATUSES + ";");
				db.execSQL("drop table if exists " + TABLE_STATUSES + ";");
				db.execSQL("create table if not exists " + TABLE_STATUSES
						+ " (" + Statuses._ID + " integer primary key autoincrement, "
						+ Statuses.CREATED + " integer, "
						+ Statuses.MESSAGE + " text, "
						+ Statuses.SERVICE + " integer, "
						+ Statuses.CREATEDTEXT + " text, "
						+ Statuses.WIDGET + " integer, "
						+ Statuses.ACCOUNT + " integer, "
						+ Statuses.STATUS_BG + " blob, "
						+ Statuses.ICON + " blob, "
						+ Statuses.SID + " integer, "
						+ Statuses.ENTITY + "integer);");
				db.execSQL("insert into " + TABLE_STATUSES
						+ " select "
						+ Statuses._ID + ","
						+ Statuses.CREATED + ","
						+ Statuses.MESSAGE + ","
						+ Statuses.SERVICE + ","
						+ Statuses.CREATEDTEXT + ","
						+ Statuses.WIDGET + ","
						+ Statuses.ACCOUNT + ","
						+ Statuses.STATUS_BG + ","
						+ Statuses.ICON + ",\"\",\"\" from " + TABLE_STATUSES + "_bkp;");
				db.execSQL("create table if not exists " + TABLE_ENTITIES
						+ " (" + Entities._ID + " integer primary key autoincrement, "
						+ Entities.FRIEND + " text, "
						+ Entities.PROFILE + " blob, "
						+ Entities.ACCOUNT + " integer, "
						+ Entities.SID + " text);");
				Cursor from_bkp =  db.query(TABLE_STATUSES + "_bkp", new String[]{Statuses._ID, "friend", "profile"}, null, null, null, null, null);
				if (from_bkp.moveToFirst()) {
					int iid = from_bkp.getColumnIndex(Statuses._ID),
					ifriend = from_bkp.getColumnIndex("friend"),
					iprofile = from_bkp.getColumnIndex("profile");
					while (!from_bkp.isAfterLast()) {
						ContentValues values = new ContentValues();
						values.put(Entities.FRIEND, from_bkp.getString(ifriend));
						values.put(Entities.PROFILE, from_bkp.getBlob(iprofile));
						int id = (int) db.insert(TABLE_ENTITIES, Entities._ID, values);
						values = new ContentValues();
						values.put(Statuses.ENTITY, id);
						db.update(TABLE_STATUSES, values, Statuses._ID + "=?", new String[]{Integer.toString(from_bkp.getInt(iid))});
						from_bkp.moveToNext();
					}
				}
				from_bkp.close();
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + ","
						+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
						+ Entities.FRIEND + " as " + Statuses_styles.FRIEND + ","
						+ Entities.PROFILE + " as " + Statuses_styles.PROFILE + ","
						+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
						+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
						+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
						+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
						+ Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
						+ "(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR + ","
						+ "(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR + ","
						+ "(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE + ","
						+ "(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE + ","
						+ "(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + "=" + TABLE_STATUSES + "." + Statuses.ACCOUNT
						+ ") when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT + " is null) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + TABLE_WIDGETS + "." + Widgets.WIDGET + "=" + TABLE_STATUSES + "." + Statuses.WIDGET + " and " + TABLE_WIDGETS + "." + Widgets.ACCOUNT
						+ " is null) else "	+ Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE + ","
						+ Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG + ","
						+ Statuses.ICON + " as " + Statuses_styles.ICON + ","
						+ Statuses.SID + " as " + Statuses_styles.SID + ","
						+ Entities._ID + " as " + Statuses_styles.ENTITY
						+ " from " + TABLE_STATUSES + "," + TABLE_ENTITIES
						+ " where " + Entities._ID + "=" + Statuses.ENTITY);
				// background updating option
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer, "
						+ Widgets.ACCOUNT + " integer, "
						+ Widgets.ICON + " integer, "
						+ Widgets.STATUSES_PER_ACCOUNT + " integer, "
						+ Widgets.BACKGROUND_UPDATE + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ","
						+ Widgets.BUTTONS_TEXTSIZE + ","
						+ Widgets.MESSAGES_TEXTSIZE + ","
						+ Widgets.FRIEND_TEXTSIZE + ","
						+ Widgets.CREATED_TEXTSIZE + ","
						+ Widgets.ACCOUNT + ","
						+ Widgets.ICON + ","
						+ Widgets.STATUSES_PER_ACCOUNT + ",1 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
			}
		}

	}	

}
