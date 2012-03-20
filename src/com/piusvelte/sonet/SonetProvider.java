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

import static com.piusvelte.sonet.Sonet.sDatabaseLock;

import java.util.HashMap;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Accounts_styles;
import com.piusvelte.sonet.Sonet.Entities;
import com.piusvelte.sonet.Sonet.Notifications;
import com.piusvelte.sonet.Sonet.Status_images;
import com.piusvelte.sonet.Sonet.Status_links;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widget_accounts;
import com.piusvelte.sonet.Sonet.Widget_accounts_view;
import com.piusvelte.sonet.Sonet.Widgets;
import com.piusvelte.sonet.Sonet.Widgets_settings;

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
	protected static final int STATUSES_STYLES = 3;
	private static final int STATUSES_STYLES_WIDGET = 4;
	private static final int ENTITIES = 5;
	private static final int WIDGET_ACCOUNTS = 6;
	private static final int WIDGET_ACCOUNTS_VIEW = 7;
	protected static final int NOTIFICATIONS = 8;
	protected static final int WIDGETS_SETTINGS = 9;
	protected static final int DISTINCT_WIDGETS_SETTINGS = 10;
	protected static final int STATUS_LINKS = 11;
	private static final int ACCOUNTS_STYLES_VIEW = 12;
	protected static final int STATUS_IMAGES = 13;

	protected static final String DATABASE_NAME = "sonet.db";
	private static final int DATABASE_VERSION = 26;

	protected static final String TABLE_ACCOUNTS = "accounts";
	private static HashMap<String, String> accountsProjectionMap;

	protected static final String TABLE_WIDGETS = "widgets";
	private static HashMap<String, String> widgetsProjectionMap;

	private static final String TABLE_STATUSES = "statuses";
	private static HashMap<String, String> statusesProjectionMap;

	protected static final String VIEW_STATUSES_STYLES = "statuses_styles";
	private static HashMap<String, String> statuses_stylesProjectionMap;

	private static final String TABLE_ENTITIES = "entities";
	private static HashMap<String, String> entitiesProjectionMap;

	protected static final String TABLE_WIDGET_ACCOUNTS = "widget_accounts";
	private static HashMap<String, String> widget_accountsProjectionMap;

	private static final String VIEW_WIDGET_ACCOUNTS = "widget_accounts_view";
	private static HashMap<String, String> widget_accounts_viewProjectionMap;

	protected static final String TABLE_NOTIFICATIONS = "notifications";
	private static HashMap<String, String> notificationsProjectionMap;

	private static final String VIEW_WIDGETS_SETTINGS = "widgets_settings";

	private static final String VIEW_DISTINCT_WIDGETS_SETTINGS = "distinct_widgets_settings";

	protected static final String TABLE_STATUS_LINKS = "status_links";
	private static HashMap<String, String> status_linksProjectionMap;

	private static final String ACCOUNTS_STYLES = "accounts_styles";
	
	protected static final String TABLE_STATUS_IMAGES = "status_images";
	private static HashMap<String, String> status_imagesProjectionMap;

	private DatabaseHelper mDatabaseHelper;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		sUriMatcher.addURI(AUTHORITY, TABLE_ACCOUNTS, ACCOUNTS);
		sUriMatcher.addURI(AUTHORITY, ACCOUNTS_STYLES, ACCOUNTS_STYLES_VIEW);

		accountsProjectionMap = new HashMap<String, String>();
		accountsProjectionMap.put(Accounts._ID, Accounts._ID);
		accountsProjectionMap.put(Accounts.USERNAME, Accounts.USERNAME);
		accountsProjectionMap.put(Accounts.TOKEN, Accounts.TOKEN);
		accountsProjectionMap.put(Accounts.SECRET, Accounts.SECRET);
		accountsProjectionMap.put(Accounts.SERVICE, Accounts.SERVICE);
		accountsProjectionMap.put(Accounts.EXPIRY, Accounts.EXPIRY);
		accountsProjectionMap.put(Accounts.SID, Accounts.SID);

		sUriMatcher.addURI(AUTHORITY, TABLE_WIDGET_ACCOUNTS, WIDGET_ACCOUNTS);

		widget_accountsProjectionMap = new HashMap<String, String>();
		widget_accountsProjectionMap.put(Widget_accounts._ID, Widget_accounts._ID);
		widget_accountsProjectionMap.put(Widget_accounts.ACCOUNT, Widget_accounts.ACCOUNT);
		widget_accountsProjectionMap.put(Widget_accounts.WIDGET, Widget_accounts.WIDGET);

		sUriMatcher.addURI(AUTHORITY, VIEW_WIDGET_ACCOUNTS, WIDGET_ACCOUNTS_VIEW);

		widget_accounts_viewProjectionMap = new HashMap<String, String>();
		widget_accounts_viewProjectionMap.put(Widget_accounts_view._ID, Widget_accounts_view._ID);
		widget_accounts_viewProjectionMap.put(Widget_accounts_view.ACCOUNT, Widget_accounts_view.ACCOUNT);
		widget_accounts_viewProjectionMap.put(Widget_accounts_view.WIDGET, Widget_accounts_view.WIDGET);
		widget_accounts_viewProjectionMap.put(Widget_accounts_view.USERNAME, Widget_accounts_view.USERNAME);
		widget_accounts_viewProjectionMap.put(Widget_accounts_view.TOKEN, Widget_accounts_view.TOKEN);
		widget_accounts_viewProjectionMap.put(Widget_accounts_view.SECRET, Widget_accounts_view.SECRET);
		widget_accounts_viewProjectionMap.put(Widget_accounts_view.SERVICE, Widget_accounts_view.SERVICE);
		widget_accounts_viewProjectionMap.put(Widget_accounts_view.EXPIRY, Widget_accounts_view.EXPIRY);
		widget_accounts_viewProjectionMap.put(Widget_accounts_view.SID, Widget_accounts_view.SID);

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
		widgetsProjectionMap.put(Widgets.SOUND, Widgets.SOUND);
		widgetsProjectionMap.put(Widgets.VIBRATE, Widgets.VIBRATE);
		widgetsProjectionMap.put(Widgets.LIGHTS, Widgets.LIGHTS);
		widgetsProjectionMap.put(Widgets.DISPLAY_PROFILE, Widgets.DISPLAY_PROFILE);
		widgetsProjectionMap.put(Widgets.INSTANT_UPLOAD, Widgets.INSTANT_UPLOAD);
		widgetsProjectionMap.put(Widgets.MARGIN, Widgets.MARGIN);
		widgetsProjectionMap.put(Widgets.PROFILES_BG_COLOR, Widgets.PROFILES_BG_COLOR);
		widgetsProjectionMap.put(Widgets.FRIEND_BG_COLOR, Widgets.FRIEND_BG_COLOR);

		sUriMatcher.addURI(AUTHORITY, TABLE_STATUSES, STATUSES);

		statusesProjectionMap = new HashMap<String, String>();
		statusesProjectionMap.put(Statuses._ID, Statuses._ID);
		statusesProjectionMap.put(Statuses.CREATED, Statuses.CREATED);
		statusesProjectionMap.put(Statuses.MESSAGE, Statuses.MESSAGE);
		statusesProjectionMap.put(Statuses.SERVICE, Statuses.SERVICE);
		statusesProjectionMap.put(Statuses.CREATEDTEXT, Statuses.CREATEDTEXT);
		statusesProjectionMap.put(Statuses.WIDGET, Statuses.WIDGET);
		statusesProjectionMap.put(Statuses.ACCOUNT, Statuses.ACCOUNT);
		statusesProjectionMap.put(Statuses.ICON, Statuses.ICON);
		statusesProjectionMap.put(Statuses.SID, Statuses.SID);
		statusesProjectionMap.put(Statuses.ENTITY, Statuses.ENTITY);
		statusesProjectionMap.put(Statuses.PROFILE_BG, Statuses.PROFILE_BG);
		statusesProjectionMap.put(Statuses.FRIEND_BG, Statuses.FRIEND_BG);

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
		statuses_stylesProjectionMap.put(Statuses_styles.PROFILE_BG, Statuses_styles.PROFILE_BG);
		statuses_stylesProjectionMap.put(Statuses_styles.FRIEND_BG, Statuses_styles.FRIEND_BG);
		statuses_stylesProjectionMap.put(Statuses_styles.IMAGE_BG, Statuses_styles.IMAGE_BG);
		statuses_stylesProjectionMap.put(Statuses_styles.IMAGE, Statuses_styles.IMAGE);

		sUriMatcher.addURI(AUTHORITY, TABLE_ENTITIES, ENTITIES);

		entitiesProjectionMap = new HashMap<String, String>();
		entitiesProjectionMap.put(Entities._ID, Entities._ID);
		entitiesProjectionMap.put(Entities.ESID, Entities.ESID);
		entitiesProjectionMap.put(Entities.FRIEND, Entities.FRIEND);
		entitiesProjectionMap.put(Entities.PROFILE, Entities.PROFILE);
		entitiesProjectionMap.put(Entities.ACCOUNT, Entities.ACCOUNT);

		sUriMatcher.addURI(AUTHORITY, TABLE_NOTIFICATIONS, NOTIFICATIONS);
		notificationsProjectionMap = new HashMap<String, String>();
		notificationsProjectionMap.put(Notifications._ID, Notifications._ID);
		notificationsProjectionMap.put(Notifications.SID, Notifications.SID);
		notificationsProjectionMap.put(Notifications.ESID, Notifications.ESID);
		notificationsProjectionMap.put(Notifications.FRIEND, Notifications.FRIEND);
		notificationsProjectionMap.put(Notifications.MESSAGE, Notifications.MESSAGE);
		notificationsProjectionMap.put(Notifications.CREATED, Notifications.CREATED);
		notificationsProjectionMap.put(Notifications.NOTIFICATION, Notifications.NOTIFICATION);
		notificationsProjectionMap.put(Notifications.ACCOUNT, Notifications.ACCOUNT);
		notificationsProjectionMap.put(Notifications.CLEARED, Notifications.CLEARED);
		notificationsProjectionMap.put(Notifications.UPDATED, Notifications.UPDATED);

		sUriMatcher.addURI(AUTHORITY, VIEW_WIDGETS_SETTINGS, WIDGETS_SETTINGS);

		sUriMatcher.addURI(AUTHORITY, VIEW_DISTINCT_WIDGETS_SETTINGS, DISTINCT_WIDGETS_SETTINGS);

		sUriMatcher.addURI(AUTHORITY, TABLE_STATUS_LINKS, STATUS_LINKS);
		status_linksProjectionMap = new HashMap<String, String>();
		status_linksProjectionMap.put(Status_links._ID, Status_links._ID);
		status_linksProjectionMap.put(Status_links.STATUS_ID, Status_links.STATUS_ID);
		status_linksProjectionMap.put(Status_links.LINK_URI, Status_links.LINK_URI);
		status_linksProjectionMap.put(Status_links.LINK_TYPE, Status_links.LINK_TYPE);
		
		sUriMatcher.addURI(AUTHORITY, TABLE_STATUS_IMAGES, STATUS_IMAGES);
		status_imagesProjectionMap = new HashMap<String, String>();
		status_imagesProjectionMap.put(Status_images._ID, Status_images._ID);
		status_imagesProjectionMap.put(Status_images.STATUS_ID, Status_images.STATUS_ID);
		status_imagesProjectionMap.put(Status_images.IMAGE, Status_images.IMAGE);
		status_imagesProjectionMap.put(Status_images.IMAGE_BG, Status_images.IMAGE_BG);

	}

	public enum StatusesStylesColumns {
		_id, friend, profile, message, createdtext, messages_color, friend_color, created_color, messages_textsize, friend_textsize, created_textsize, status_bg, icon, profile_bg, friend_bg, image_bg, image
	}

	public enum StatusesStylesColumnsNoProfile {
		_id, friend, message, createdtext, messages_color, friend_color, created_color, messages_textsize, friend_textsize, created_textsize, status_bg, icon, friend_bg, image_bg, image
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
		case WIDGET_ACCOUNTS:
			return Widget_accounts.CONTENT_TYPE;
		case WIDGET_ACCOUNTS_VIEW:
			return Widget_accounts_view.CONTENT_TYPE;
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
		case NOTIFICATIONS:
			return Notifications.CONTENT_TYPE;
		case WIDGETS_SETTINGS:
			return Widgets_settings.CONTENT_TYPE;
		case DISTINCT_WIDGETS_SETTINGS:
			return Widgets_settings.CONTENT_TYPE;
		case STATUS_LINKS:
			return Status_links.CONTENT_TYPE;
		case ACCOUNTS_STYLES_VIEW:
			return Accounts_styles.CONTENT_TYPE;
		case STATUS_IMAGES:
			return Status_images.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String whereClause, String[] whereArgs) {
		synchronized (sDatabaseLock) {
			SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
			int count;
			switch (sUriMatcher.match(uri)) {
			case ACCOUNTS:
				count = db.delete(TABLE_ACCOUNTS, whereClause, whereArgs);
				break;
			case WIDGET_ACCOUNTS:
				count = db.delete(TABLE_WIDGET_ACCOUNTS, whereClause, whereArgs);
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
			case NOTIFICATIONS:
				count = db.delete(TABLE_NOTIFICATIONS, whereClause, whereArgs);
				break;
			case WIDGETS_SETTINGS:
				count = db.delete(VIEW_WIDGETS_SETTINGS, whereClause, whereArgs);
				break;
			case STATUS_LINKS:
				count = db.delete(TABLE_STATUS_LINKS, whereClause, whereArgs);
				break;
			case STATUS_IMAGES:
				count = db.delete(TABLE_STATUS_IMAGES, whereClause, whereArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
			getContext().getContentResolver().notifyChange(uri, null);
			return count;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		synchronized (sDatabaseLock) {
			SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
			long rowId;
			Uri returnUri = null;
			SonetCrypto sonetCrypto;
			switch (sUriMatcher.match(uri)) {
			case ACCOUNTS:
				// encrypt the data
				sonetCrypto = SonetCrypto.getInstance(getContext());
				if (values.containsKey(Accounts.TOKEN)) {
					values.put(Accounts.TOKEN, sonetCrypto.Encrypt(values.getAsString(Accounts.TOKEN)));
				}
				if (values.containsKey(Accounts.SECRET)) {
					values.put(Accounts.SECRET, sonetCrypto.Encrypt(values.getAsString(Accounts.SECRET)));
				}
				if (values.containsKey(Accounts.SID)) {
					values.put(Accounts.SID, sonetCrypto.Encrypt(values.getAsString(Accounts.SID)));
				}
				rowId = db.insert(TABLE_ACCOUNTS, Accounts._ID, values);
				returnUri = ContentUris.withAppendedId(Accounts.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(returnUri, null);
				break;
			case WIDGET_ACCOUNTS:
				rowId = db.insert(TABLE_WIDGET_ACCOUNTS, Widget_accounts._ID, values);
				returnUri = ContentUris.withAppendedId(Widget_accounts.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(returnUri, null);
				break;
			case WIDGETS:
				rowId = db.insert(TABLE_WIDGETS, Widgets._ID, values);
				returnUri = ContentUris.withAppendedId(Widgets.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(returnUri, null);
				break;
			case STATUSES:
				// encrypt the data
				sonetCrypto = SonetCrypto.getInstance(getContext());
				if (values.containsKey(Statuses.SID)) {
					values.put(Statuses.SID, sonetCrypto.Encrypt(values.getAsString(Statuses.SID)));
				}
				rowId = db.insert(TABLE_STATUSES, Accounts._ID, values);
				returnUri = ContentUris.withAppendedId(Accounts.CONTENT_URI, rowId);
				// many statuses will be inserted at once, so don't trigger a refresh for each one
				//			getContext().getContentResolver().notifyChange(returnUri, null);
				break;
			case ENTITIES:
				// encrypt the data
				sonetCrypto = SonetCrypto.getInstance(getContext());
				if (values.containsKey(Entities.ESID)) {
					values.put(Entities.ESID, sonetCrypto.Encrypt(values.getAsString(Entities.ESID)));
				}
				rowId = db.insert(TABLE_ENTITIES, Entities._ID, values);
				returnUri = ContentUris.withAppendedId(Entities.CONTENT_URI, rowId);
				break;
			case NOTIFICATIONS:
				// encrypt the data
				sonetCrypto = SonetCrypto.getInstance(getContext());
				if (values.containsKey(Notifications.SID)) {
					values.put(Notifications.SID, sonetCrypto.Encrypt(values.getAsString(Notifications.SID)));
				}
				if (values.containsKey(Notifications.ESID)) {
					values.put(Notifications.ESID, sonetCrypto.Encrypt(values.getAsString(Notifications.ESID)));
				}
				rowId = db.insert(TABLE_NOTIFICATIONS, Notifications._ID, values);
				returnUri = ContentUris.withAppendedId(Notifications.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(returnUri, null);
				break;
			case WIDGETS_SETTINGS:
				rowId = db.insert(VIEW_WIDGETS_SETTINGS, Widgets_settings._ID, values);
				returnUri = ContentUris.withAppendedId(Widgets_settings.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(returnUri, null);
				break;
			case STATUS_LINKS:
				rowId = db.insert(TABLE_STATUS_LINKS, Status_links._ID, values);
				returnUri = ContentUris.withAppendedId(Status_links.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(returnUri, null);
				break;
			case STATUS_IMAGES:
				rowId = db.insert(TABLE_STATUS_IMAGES, Status_images._ID, values);
				returnUri = ContentUris.withAppendedId(Status_images.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(returnUri, null);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
			return returnUri;
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
		synchronized (sDatabaseLock) {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
			Cursor c;
			switch (sUriMatcher.match(uri)) {
			case ACCOUNTS:
				qb.setTables(TABLE_ACCOUNTS);
				qb.setProjectionMap(accountsProjectionMap);
				break;
			case WIDGET_ACCOUNTS:
				qb.setTables(TABLE_WIDGET_ACCOUNTS);
				qb.setProjectionMap(widget_accountsProjectionMap);
				break;
			case WIDGET_ACCOUNTS_VIEW:
				qb.setTables(VIEW_WIDGET_ACCOUNTS);
				qb.setProjectionMap(widget_accounts_viewProjectionMap);
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
				if ((selection == null) || (selectionArgs == null)) {
					selection = Statuses_styles.WIDGET + "=?";
					selectionArgs = new String[]{uri.getLastPathSegment()};
				}
				break;
			case ENTITIES:
				qb.setTables(TABLE_ENTITIES);
				qb.setProjectionMap(entitiesProjectionMap);
				break;
			case NOTIFICATIONS:
				qb.setTables(TABLE_NOTIFICATIONS);
				qb.setProjectionMap(notificationsProjectionMap);
				break;
			case WIDGETS_SETTINGS:
				qb.setTables(VIEW_WIDGETS_SETTINGS);
				qb.setProjectionMap(widgetsProjectionMap);
				break;
			case DISTINCT_WIDGETS_SETTINGS:
				qb.setTables(VIEW_WIDGETS_SETTINGS);
				qb.setProjectionMap(widgetsProjectionMap);
				qb.setDistinct(true);
				break;
			case STATUS_LINKS:
				qb.setTables(TABLE_STATUS_LINKS);
				qb.setProjectionMap(status_linksProjectionMap);
				break;
			case ACCOUNTS_STYLES_VIEW:
				qb.setTables(TABLE_ACCOUNTS);
				qb.setProjectionMap(statuses_stylesProjectionMap);
				break;
			case STATUS_IMAGES:
				qb.setTables(TABLE_STATUS_IMAGES);
				qb.setProjectionMap(status_imagesProjectionMap);
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
			c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
			c.setNotificationUri(getContext().getContentResolver(), uri);
			return c;
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		synchronized (sDatabaseLock) {
			SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

			int count;
			SonetCrypto sonetCrypto;
			switch (sUriMatcher.match(uri)) {
			case ACCOUNTS:
				// encrypt the data
				sonetCrypto = SonetCrypto.getInstance(getContext());
				if (values.containsKey(Accounts.TOKEN)) {
					values.put(Accounts.TOKEN, sonetCrypto.Encrypt(values.getAsString(Accounts.TOKEN)));
				}
				if (values.containsKey(Accounts.SECRET)) {
					values.put(Accounts.SECRET, sonetCrypto.Encrypt(values.getAsString(Accounts.SECRET)));
				}
				if (values.containsKey(Accounts.SID)) {
					values.put(Accounts.SID, sonetCrypto.Encrypt(values.getAsString(Accounts.SID)));
				}
				count = db.update(TABLE_ACCOUNTS, values, selection, selectionArgs);
				break;
			case WIDGET_ACCOUNTS:
				count = db.update(TABLE_WIDGET_ACCOUNTS, values, selection, selectionArgs);
				break;
			case WIDGETS:
				count = db.update(TABLE_WIDGETS, values, selection, selectionArgs);
				break;
			case STATUSES:
				// encrypt the data
				sonetCrypto = SonetCrypto.getInstance(getContext());
				if (values.containsKey(Statuses.SID)) {
					values.put(Statuses.SID, sonetCrypto.Encrypt(values.getAsString(Statuses.SID)));
				}
				count = db.update(TABLE_STATUSES, values, selection, selectionArgs);
				break;
			case ENTITIES:
				// encrypt the data
				sonetCrypto = SonetCrypto.getInstance(getContext());
				if (values.containsKey(Entities.ESID)) {
					values.put(Entities.ESID, sonetCrypto.Encrypt(values.getAsString(Entities.ESID)));
				}
				count = db.update(TABLE_ENTITIES, values, selection, selectionArgs);
				break;
			case NOTIFICATIONS:
				// encrypt the data
				sonetCrypto = SonetCrypto.getInstance(getContext());
				if (values.containsKey(Notifications.SID)) {
					values.put(Notifications.SID, sonetCrypto.Encrypt(values.getAsString(Notifications.SID)));
				}
				if (values.containsKey(Notifications.ESID)) {
					values.put(Notifications.ESID, sonetCrypto.Encrypt(values.getAsString(Notifications.ESID)));
				}
				count = db.update(TABLE_NOTIFICATIONS, values, selection, selectionArgs);
				break;
			case STATUS_LINKS:
				count = db.update(TABLE_STATUS_LINKS, values, selection, selectionArgs);
				break;
			case STATUS_IMAGES:
				count = db.update(TABLE_STATUS_IMAGES, values, selection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
			getContext().getContentResolver().notifyChange(uri, null);
			return count;
		}
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
					+ Accounts.SID + " text);");
			db.execSQL("create table if not exists " + TABLE_WIDGET_ACCOUNTS
					+ " (" + Widget_accounts._ID + " integer primary key autoincrement, "
					+ Widget_accounts.ACCOUNT + " integer, "
					+ Widget_accounts.WIDGET + " integer);");
			db.execSQL("create view if not exists " + VIEW_WIDGET_ACCOUNTS + " as select "
					+ TABLE_WIDGET_ACCOUNTS + "." + Widget_accounts._ID
					+ "," + Widget_accounts.ACCOUNT
					+ "," + Widget_accounts.WIDGET
					+ "," + Accounts.EXPIRY
					+ "," + Accounts.SECRET
					+ "," + Accounts.SERVICE
					+ "," + Accounts.SID
					+ "," + Accounts.TOKEN
					+ "," + Accounts.USERNAME
					+ " from "
					+ TABLE_WIDGET_ACCOUNTS
					+ "," + TABLE_ACCOUNTS
					+ " where "
					+ TABLE_ACCOUNTS + "." + Accounts._ID + "=" + Widget_accounts.ACCOUNT
					+ ";");
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
					+ Widgets.BACKGROUND_UPDATE + " integer, "
					+ Widgets.SOUND + " integer, "
					+ Widgets.VIBRATE + " integer, "
					+ Widgets.LIGHTS + " integer, "
					+ Widgets.DISPLAY_PROFILE + " integer, "
					+ Widgets.INSTANT_UPLOAD + " integer, "
					+ Widgets.MARGIN + " integer, "
					+ Widgets.PROFILES_BG_COLOR + " integer, "
					+ Widgets.FRIEND_BG_COLOR + " integer);");
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
					+ Statuses.ENTITY + " integer, "
					+ Statuses.FRIEND_OVERRIDE + " text, "
					+ Statuses.PROFILE_BG + " blob, "
					+ Statuses.FRIEND_BG + " blob);");
			db.execSQL("create table if not exists " + TABLE_ENTITIES
					+ " (" + Entities._ID + " integer primary key autoincrement, "
					+ Entities.FRIEND + " text, "
					+ Entities.PROFILE + " blob, "
					+ Entities.ACCOUNT + " integer, "
					+ Entities.ESID + " text);");
			// notifications
			db.execSQL("create table if not exists " + TABLE_NOTIFICATIONS
					+ " (" + Notifications._ID + " integer primary key autoincrement, "
					+ Notifications.SID + " text, "
					+ Notifications.ESID + " text, "
					+ Notifications.FRIEND + " text, "
					+ Notifications.MESSAGE + " text, "
					+ Notifications.CREATED + " integer, "
					+ Notifications.NOTIFICATION + " text, "
					+ Notifications.ACCOUNT + " integer, "
					+ Notifications.CLEARED + " integer, "
					+ Notifications.UPDATED + " integer);");
			db.execSQL("create table if not exists " + TABLE_STATUS_LINKS
					+ " (" + Status_links._ID + " integer primary key autoincrement, "
					+ Status_links.STATUS_ID + " integer, "
					+ Status_links.LINK_URI + " text, "
					+ Status_links.LINK_TYPE + " text);");
			db.execSQL("create table if not exists " + TABLE_STATUS_IMAGES
					+ " (" + Status_images._ID + " integer primary key autoincrement, "
					+ Status_images.STATUS_ID + " integer, "
					+ Status_images.IMAGE + " blob, "
					+ Status_images.IMAGE_BG + " blob);");
			db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
					"s." + Statuses._ID + " as " + Statuses_styles._ID
					+ ",s." + Statuses.CREATED + " as " + Statuses_styles.CREATED
					+ ",(case when " + "s." + Statuses.FRIEND_OVERRIDE + " != \"\" then " + "s." + Statuses.FRIEND_OVERRIDE + " else " + "e." + Entities.FRIEND + " end) as " + Statuses_styles.FRIEND
					+ ",e." + Entities.PROFILE + " as " + Statuses_styles.PROFILE
					+ ",s." + Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE
					+ ",s." + Statuses.SERVICE + " as " + Statuses_styles.SERVICE
					+ ",s." + Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT
					+ ",s." + Statuses.WIDGET + " as " + Statuses_styles.WIDGET
					+ ",s." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT
					+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
					+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
					+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
					+ " else " + Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR
					+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
					+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
					+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
					+ " else " + Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR
					+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
					+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
					+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
					+ " else " + Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR
					+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
					+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
					+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
					+ " else " + Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE
					+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
					+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
					+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
					+ " else " + Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE
					+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
					+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
					+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
					+ " else " + Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE
					+ ",s." + Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG
					+ ",s." + Statuses.ICON + " as " + Statuses_styles.ICON
					+ ",s." + Statuses.SID + " as " + Statuses_styles.SID
					+ ",e." + Entities._ID + " as " + Statuses_styles.ENTITY
					+ ",e." + Entities.ESID + " as " + Statuses_styles.ESID
					+ ",s." + Statuses.PROFILE_BG + " as " + Statuses_styles.PROFILE_BG
					+ ",s." + Statuses.FRIEND_BG + " as " + Statuses_styles.FRIEND_BG
					+ ",i." + Status_images.IMAGE_BG + " as " + Statuses_styles.IMAGE_BG
					+ ",i." + Status_images.IMAGE + " as " + Statuses_styles.IMAGE
					+ " from " + TABLE_STATUSES + " s," + TABLE_ENTITIES + " e," + TABLE_WIDGETS + " a," + TABLE_WIDGETS + " b," + TABLE_WIDGETS + " c"
					+ " left join " + TABLE_STATUS_IMAGES + " i"
					+ " on i." + Status_images.STATUS_ID + "=s." + Statuses._ID
					+ " where "
					+ "e." + Entities._ID + "=s." + Statuses.ENTITY
					+ " and a." + Widgets.WIDGET + "=s." + Statuses.WIDGET
					+ " and a." + Widgets.ACCOUNT + "=s." + Statuses.ACCOUNT
					+ " and b." + Widgets.WIDGET + "=s." + Statuses.WIDGET
					+ " and b." + Widgets.ACCOUNT + "=-1"
					+ " and c." + Widgets.WIDGET + "=0"
					+ " and c." + Widgets.ACCOUNT + "=-1;");
			// create a view for the widget settings
			db.execSQL("create view if not exists " + VIEW_WIDGETS_SETTINGS + " as select a."
					+ Widgets._ID + " as " + Widgets._ID
					+ ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
					+ ",(case when a." + Widgets.INTERVAL + " is not null then a." + Widgets.INTERVAL
					+ " when b." + Widgets.INTERVAL + " is not null then b. " + Widgets.INTERVAL
					+ " when c." + Widgets.INTERVAL + " is not null then c." + Widgets.INTERVAL
					+ " else " + Sonet.default_interval + " end) as " + Widgets.INTERVAL
					+ ",(case when a." + Widgets.HASBUTTONS + " is not null then a." + Widgets.HASBUTTONS
					+ " when b." + Widgets.HASBUTTONS + " is not null then b. " + Widgets.HASBUTTONS
					+ " when c." + Widgets.HASBUTTONS + " is not null then c." + Widgets.HASBUTTONS
					+ " else 0 end) as " + Widgets.HASBUTTONS
					+ ",(case when a." + Widgets.BUTTONS_BG_COLOR + " is not null then a." + Widgets.BUTTONS_BG_COLOR
					+ " when b." + Widgets.BUTTONS_BG_COLOR + " is not null then b. " + Widgets.BUTTONS_BG_COLOR
					+ " when c." + Widgets.BUTTONS_BG_COLOR + " is not null then c." + Widgets.BUTTONS_BG_COLOR
					+ " else " + Sonet.default_buttons_bg_color + " end) as " + Widgets.BUTTONS_BG_COLOR
					+ ",(case when a." + Widgets.BUTTONS_COLOR + " is not null then a." + Widgets.BUTTONS_COLOR
					+ " when b." + Widgets.BUTTONS_COLOR + " is not null then b. " + Widgets.BUTTONS_COLOR
					+ " when c." + Widgets.BUTTONS_COLOR + " is not null then c." + Widgets.BUTTONS_COLOR
					+ " else " + Sonet.default_buttons_color + " end) as " + Widgets.BUTTONS_COLOR
					+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
					+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
					+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
					+ " else " + Sonet.default_friend_color + " end) as " + Widgets.FRIEND_COLOR
					+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
					+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
					+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
					+ " else " + Sonet.default_created_color + " end) as " + Widgets.CREATED_COLOR
					+ ",(case when a." + Widgets.MESSAGES_BG_COLOR + " is not null then a." + Widgets.MESSAGES_BG_COLOR
					+ " when b." + Widgets.MESSAGES_BG_COLOR + " is not null then b. " + Widgets.MESSAGES_BG_COLOR
					+ " when c." + Widgets.MESSAGES_BG_COLOR + " is not null then c." + Widgets.MESSAGES_BG_COLOR
					+ " else " + Sonet.default_message_bg_color + " end) as " + Widgets.MESSAGES_BG_COLOR
					+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
					+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
					+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
					+ " else " + Sonet.default_message_color + " end) as " + Widgets.MESSAGES_COLOR
					+ ",(case when a." + Widgets.TIME24HR + " is not null then a." + Widgets.TIME24HR
					+ " when b." + Widgets.TIME24HR + " is not null then b. " + Widgets.TIME24HR
					+ " when c." + Widgets.TIME24HR + " is not null then c." + Widgets.TIME24HR
					+ " else 0 end) as " + Widgets.TIME24HR
					+ ",(case when a." + Widgets.SCROLLABLE + " is not null then a." + Widgets.SCROLLABLE
					+ " when b." + Widgets.SCROLLABLE + " is not null then b. " + Widgets.SCROLLABLE
					+ " when c." + Widgets.SCROLLABLE + " is not null then c." + Widgets.SCROLLABLE
					+ " else 0 end) as " + Widgets.SCROLLABLE
					+ ",(case when a." + Widgets.BUTTONS_TEXTSIZE + " is not null then a." + Widgets.BUTTONS_TEXTSIZE
					+ " when b." + Widgets.BUTTONS_TEXTSIZE + " is not null then b. " + Widgets.BUTTONS_TEXTSIZE
					+ " when c." + Widgets.BUTTONS_TEXTSIZE + " is not null then c." + Widgets.BUTTONS_TEXTSIZE
					+ " else " + Sonet.default_buttons_textsize + " end) as " + Widgets.BUTTONS_TEXTSIZE
					+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
					+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
					+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
					+ " else " + Sonet.default_messages_textsize + " end) as " + Widgets.MESSAGES_TEXTSIZE
					+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
					+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
					+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
					+ " else " + Sonet.default_friend_textsize + " end) as " + Widgets.FRIEND_TEXTSIZE
					+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
					+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
					+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
					+ " else " + Sonet.default_created_textsize + " end) as " + Widgets.CREATED_TEXTSIZE
					+ ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
					+ ",(case when a." + Widgets.ICON + " is not null then a." + Widgets.ICON
					+ " when b." + Widgets.ICON + " is not null then b. " + Widgets.ICON
					+ " when c." + Widgets.ICON + " is not null then c." + Widgets.ICON
					+ " else 1 end) as " + Widgets.ICON
					+ ",(case when a." + Widgets.STATUSES_PER_ACCOUNT + " is not null then a." + Widgets.STATUSES_PER_ACCOUNT
					+ " when b." + Widgets.STATUSES_PER_ACCOUNT + " is not null then b. " + Widgets.STATUSES_PER_ACCOUNT
					+ " when c." + Widgets.STATUSES_PER_ACCOUNT + " is not null then c." + Widgets.STATUSES_PER_ACCOUNT
					+ " else " + Sonet.default_statuses_per_account + " end) as " + Widgets.STATUSES_PER_ACCOUNT
					+ ",(case when a." + Widgets.BACKGROUND_UPDATE + " is not null then a." + Widgets.BACKGROUND_UPDATE
					+ " when b." + Widgets.BACKGROUND_UPDATE + " is not null then b. " + Widgets.BACKGROUND_UPDATE
					+ " when c." + Widgets.BACKGROUND_UPDATE + " is not null then c." + Widgets.BACKGROUND_UPDATE
					+ " else 1 end) as " + Widgets.BACKGROUND_UPDATE
					+ ",(case when a." + Widgets.SOUND + " is not null then a." + Widgets.SOUND
					+ " when b." + Widgets.SOUND + " is not null then b. " + Widgets.SOUND
					+ " when c." + Widgets.SOUND + " is not null then c." + Widgets.SOUND
					+ " else 0 end) as " + Widgets.SOUND
					+ ",(case when a." + Widgets.VIBRATE + " is not null then a." + Widgets.VIBRATE
					+ " when b." + Widgets.VIBRATE + " is not null then b. " + Widgets.VIBRATE
					+ " when c." + Widgets.VIBRATE + " is not null then c." + Widgets.VIBRATE
					+ " else 0 end) as " + Widgets.VIBRATE
					+ ",(case when a." + Widgets.LIGHTS + " is not null then a." + Widgets.LIGHTS
					+ " when b." + Widgets.LIGHTS + " is not null then b. " + Widgets.LIGHTS
					+ " when c." + Widgets.LIGHTS + " is not null then c." + Widgets.LIGHTS
					+ " else 0 end) as " + Widgets.LIGHTS
					+ ",(case when a." + Widgets.DISPLAY_PROFILE + " is not null then a." + Widgets.DISPLAY_PROFILE
					+ " when b." + Widgets.DISPLAY_PROFILE + " is not null then b. " + Widgets.DISPLAY_PROFILE
					+ " when c." + Widgets.DISPLAY_PROFILE + " is not null then c." + Widgets.DISPLAY_PROFILE
					+ " else 1 end) as " + Widgets.DISPLAY_PROFILE
					+ ",(case when a." + Widgets.INSTANT_UPLOAD + " is not null then a." + Widgets.INSTANT_UPLOAD
					+ " when b." + Widgets.INSTANT_UPLOAD + " is not null then b. " + Widgets.INSTANT_UPLOAD
					+ " when c." + Widgets.INSTANT_UPLOAD + " is not null then c." + Widgets.INSTANT_UPLOAD
					+ " else 0 end) as " + Widgets.INSTANT_UPLOAD
					+ ",(case when a." + Widgets.MARGIN + " is not null then a." + Widgets.MARGIN
					+ " when b." + Widgets.MARGIN + " is not null then b. " + Widgets.MARGIN
					+ " when c." + Widgets.MARGIN + " is not null then c." + Widgets.MARGIN
					+ " else 0 end) as " + Widgets.MARGIN
					+ ",(case when a." + Widgets.PROFILES_BG_COLOR + " is not null then a." + Widgets.PROFILES_BG_COLOR
					+ " when b." + Widgets.PROFILES_BG_COLOR + " is not null then b. " + Widgets.PROFILES_BG_COLOR
					+ " when c." + Widgets.PROFILES_BG_COLOR + " is not null then c." + Widgets.PROFILES_BG_COLOR
					+ " else " + Sonet.default_message_bg_color + " end) as " + Widgets.PROFILES_BG_COLOR
					+ ",(case when a." + Widgets.FRIEND_BG_COLOR + " is not null then a." + Widgets.FRIEND_BG_COLOR
					+ " when b." + Widgets.FRIEND_BG_COLOR + " is not null then b. " + Widgets.FRIEND_BG_COLOR
					+ " when c." + Widgets.FRIEND_BG_COLOR + " is not null then c." + Widgets.FRIEND_BG_COLOR
					+ " else " + Sonet.default_friend_bg_color + " end) as " + Widgets.FRIEND_BG_COLOR
					+ " from " + TABLE_WIDGETS + " a,"
					+ TABLE_WIDGETS + " b,"
					+ TABLE_WIDGETS + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." + Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				// add column for expiry
				growTable(db, TABLE_ACCOUNTS, Accounts.EXPIRY, "integer", "", true);
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
				growTable(db, TABLE_ACCOUNTS, "widget", "integer", "", true);
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
				growTable(db, TABLE_WIDGETS, Widgets.SCROLLABLE, "integer", "0", false);
			}
			if (oldVersion < 6) {
				// add columns for textsize
				growTable(db, TABLE_WIDGETS, Widgets.BUTTONS_TEXTSIZE, "integer", "14", false);
				growTable(db, TABLE_WIDGETS, Widgets.MESSAGES_TEXTSIZE, "integer", "14", false);
				growTable(db, TABLE_WIDGETS, Widgets.FRIEND_TEXTSIZE, "integer", "14", false);
				growTable(db, TABLE_WIDGETS, Widgets.CREATED_TEXTSIZE, "integer", "14", false);
			}
			if (oldVersion < 7) {
				// add column for account to handle account specific widget settings
				growTable(db, TABLE_WIDGETS, Widgets.ACCOUNT, "integer", Long.toString(Sonet.INVALID_ACCOUNT_ID), false);
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
						TABLE_STATUSES + "." + Statuses._ID + " as " + Statuses_styles._ID + ","
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
						+ "widget integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS
						+ " select "
						+ Accounts._ID + ","
						+ Accounts.USERNAME + ","
						+ Accounts.TOKEN + ","
						+ Accounts.SECRET + ","
						+ Accounts.SERVICE + ","
						+ Accounts.EXPIRY + ","
						+ "timezone,"
						+ "widget from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 10) {
				// add support for service icons
				growTable(db, TABLE_WIDGETS, Widgets.ICON, "blob", "null", false);
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + " as " + Statuses_styles._ID + ","
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
				growTable(db, TABLE_WIDGETS, Widgets.STATUSES_PER_ACCOUNT, "integer", Integer.toString(Sonet.default_statuses_per_account), false);
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
						+ "widget integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS
						+ " select "
						+ Accounts._ID + ","
						+ Accounts.USERNAME + ","
						+ Accounts.TOKEN + ","
						+ Accounts.SECRET + ","
						+ Accounts.SERVICE + ","
						+ Accounts.EXPIRY + ","
						+ "widget from " + TABLE_ACCOUNTS + "_bkp;");
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
						+ "widget integer, "
						+ Accounts.SID + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS
						+ " select "
						+ Accounts._ID + ","
						+ Accounts.USERNAME + ","
						+ Accounts.TOKEN + ","
						+ Accounts.SECRET + ","
						+ Accounts.SERVICE + ","
						+ Accounts.EXPIRY + ","
						+ "widget,\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("create temp table " + TABLE_STATUSES + "_bkp as select * from " + TABLE_STATUSES + ";");
				growTable(db, TABLE_STATUSES, Statuses.SID, "integer", "", true);
				growTable(db, TABLE_STATUSES, Statuses.ENTITY, "integer", "", true);
				db.execSQL("create table if not exists " + TABLE_ENTITIES
						+ " (" + Entities._ID + " integer primary key autoincrement, "
						+ Entities.FRIEND + " text, "
						+ Entities.PROFILE + " blob, "
						+ Entities.ACCOUNT + " integer, "
						+ Entities.ESID + " text);");
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
						TABLE_STATUSES + "." + Statuses._ID + " as " + Statuses_styles._ID + ","
						+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
						+ Entities.FRIEND + " as " + Statuses_styles.FRIEND + ","
						+ Entities.PROFILE + " as " + Statuses_styles.PROFILE + ","
						+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
						+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
						+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
						+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
						+ TABLE_STATUSES + "." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
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
						+ TABLE_ENTITIES + "." + Entities._ID + " as " + Statuses_styles.ENTITY + ","
						+ Entities.ESID + " as " + Statuses_styles.ESID
						+ " from " + TABLE_STATUSES + "," + TABLE_ENTITIES
						+ " where " + TABLE_ENTITIES + "." + Entities._ID + "=" + Statuses.ENTITY);
				// background updating option
				growTable(db, TABLE_WIDGETS, Widgets.BACKGROUND_UPDATE, "integer", "1", false);
			}
			if (oldVersion < 13) {
				// scrollable will now store the version rather than being a boolean, so moving 1 > 2, as the default supported version is 2
				ContentValues values = new ContentValues();
				values.put(Widgets.SCROLLABLE, 0);
				db.update(TABLE_WIDGETS, values, Widgets.SCROLLABLE + "!=?", new String[]{"1"});
				values = new ContentValues();
				values.put(Widgets.SCROLLABLE, 2);
				db.update(TABLE_WIDGETS, values, Widgets.SCROLLABLE + "=?", new String[]{"1"});
			}
			if (oldVersion < 14) {
				// need to redesign the accounts table so that multiple widgets can use the same accounts
				db.execSQL("create table if not exists " + TABLE_WIDGET_ACCOUNTS
						+ " (" + Widget_accounts._ID + " integer primary key autoincrement, "
						+ Widget_accounts.ACCOUNT + " integer, "
						+ Widget_accounts.WIDGET + " integer);");
				// migrate accounts over to widget_accounts
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
						+ Accounts.SID + " text);");
				Cursor accounts = db.query(TABLE_ACCOUNTS + "_bkp", new String[]{Accounts._ID, Accounts.USERNAME, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE, Accounts.EXPIRY, "widget", Accounts.SID}, null, null, null, null, null);
				if (accounts.moveToFirst()) {
					int iid = accounts.getColumnIndex(Accounts._ID),
							iusername = accounts.getColumnIndex(Accounts.USERNAME),
							itoken = accounts.getColumnIndex(Accounts.TOKEN),
							isecret = accounts.getColumnIndex(Accounts.SECRET),
							iservice = accounts.getColumnIndex(Accounts.SERVICE),
							iexpiry = accounts.getColumnIndex(Accounts.EXPIRY),
							iwidget = accounts.getColumnIndex("widget"),
							isid = accounts.getColumnIndex(Accounts.SID);
					while (!accounts.isAfterLast()) {
						ContentValues values = new ContentValues();
						values.put(Widget_accounts.ACCOUNT, accounts.getLong(iid));
						values.put(Widget_accounts.WIDGET, accounts.getInt(iwidget));
						db.insert(TABLE_WIDGET_ACCOUNTS, Widget_accounts._ID, values);
						values.clear();
						values.put(Accounts._ID, accounts.getLong(iid));
						values.put(Accounts.USERNAME, accounts.getString(iusername));
						values.put(Accounts.TOKEN, accounts.getString(itoken));
						values.put(Accounts.SECRET, accounts.getString(isecret));
						values.put(Accounts.SERVICE, accounts.getString(iservice));
						values.put(Accounts.EXPIRY, accounts.getLong(iexpiry));
						values.put(Accounts.SID, accounts.getString(isid));
						db.insert(TABLE_ACCOUNTS, Accounts._ID, values);
						accounts.moveToNext();
					}
				}
				accounts.close();
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create view if not exists " + VIEW_WIDGET_ACCOUNTS + " as select "
						+ TABLE_WIDGET_ACCOUNTS + "." + Widget_accounts._ID
						+ "," + Widget_accounts.ACCOUNT
						+ "," + Widget_accounts.WIDGET
						+ "," + Accounts.EXPIRY
						+ "," + Accounts.SECRET
						+ "," + Accounts.SERVICE
						+ "," + Accounts.SID
						+ "," + Accounts.TOKEN
						+ "," + Accounts.USERNAME
						+ " from "
						+ TABLE_WIDGET_ACCOUNTS
						+ "," + TABLE_ACCOUNTS
						+ " where "
						+ TABLE_ACCOUNTS + "." + Accounts._ID + "=" + Widget_accounts.ACCOUNT
						+ ";");
			}
			if (oldVersion < 15) {
				// add support for optional profile pics
				growTable(db, TABLE_WIDGETS, Widgets.SOUND, "integer", "0", false);
				growTable(db, TABLE_WIDGETS, Widgets.VIBRATE, "integer", "0", false);
				growTable(db, TABLE_WIDGETS, Widgets.LIGHTS, "integer", "0", false);
				growTable(db, TABLE_WIDGETS, Widgets.DISPLAY_PROFILE, "integer", "1", false);
				// notifications
				db.execSQL("create table if not exists " + TABLE_NOTIFICATIONS
						+ " (" + Notifications._ID + " integer primary key autoincrement, "
						+ Notifications.SID + " text, "
						+ Notifications.ESID + " text, "
						+ Notifications.FRIEND + " text, "
						+ Notifications.MESSAGE + " text, "
						+ Notifications.CREATED + " integer, "
						+ Notifications.NOTIFICATION + " text, "
						+ Notifications.ACCOUNT + " integer, "
						+ Notifications.CLEARED + " integer);");
				// allow friend name override in cases of facebook wall posts
				growTable(db, TABLE_STATUSES, Statuses.FRIEND_OVERRIDE, "text", "null", false);
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						TABLE_STATUSES + "." + Statuses._ID + " as " + Statuses_styles._ID + ","
						+ Statuses.CREATED + " as " + Statuses_styles.CREATED + ","
						+ "(case when " + Statuses.FRIEND_OVERRIDE + " != \"\" then " + Statuses.FRIEND_OVERRIDE + " else " + Entities.FRIEND + " end) as " + Statuses_styles.FRIEND + ","
						+ Entities.PROFILE + " as " + Statuses_styles.PROFILE + ","
						+ Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE + ","
						+ Statuses.SERVICE + " as " + Statuses_styles.SERVICE + ","
						+ Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT + ","
						+ Statuses.WIDGET + " as " + Statuses_styles.WIDGET + ","
						+ TABLE_STATUSES + "." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT + ","
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
						+ TABLE_ENTITIES + "." + Entities._ID + " as " + Statuses_styles.ENTITY + ","
						+ Entities.ESID + " as " + Statuses_styles.ESID
						+ " from " + TABLE_STATUSES + "," + TABLE_ENTITIES
						+ " where " + TABLE_ENTITIES + "." + Entities._ID + "=" + Statuses.ENTITY + ";");
			}
			if (oldVersion < 16) {
				// create a view for the widget settings
				db.execSQL("create view if not exists " + VIEW_WIDGETS_SETTINGS + " as select a."
						+ Widgets._ID + " as " + Widgets._ID
						+ ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
						+ ",(case when a." + Widgets.INTERVAL + " is not null then a." + Widgets.INTERVAL
						+ " when b." + Widgets.INTERVAL + " is not null then b. " + Widgets.INTERVAL
						+ " when c." + Widgets.INTERVAL + " is not null then c." + Widgets.INTERVAL
						+ " else " + Sonet.default_interval + " end) as " + Widgets.INTERVAL
						+ ",(case when a." + Widgets.HASBUTTONS + " is not null then a." + Widgets.HASBUTTONS
						+ " when b." + Widgets.HASBUTTONS + " is not null then b. " + Widgets.HASBUTTONS
						+ " when c." + Widgets.HASBUTTONS + " is not null then c." + Widgets.HASBUTTONS
						+ " else 0 end) as " + Widgets.HASBUTTONS
						+ ",(case when a." + Widgets.BUTTONS_BG_COLOR + " is not null then a." + Widgets.BUTTONS_BG_COLOR
						+ " when b." + Widgets.BUTTONS_BG_COLOR + " is not null then b. " + Widgets.BUTTONS_BG_COLOR
						+ " when c." + Widgets.BUTTONS_BG_COLOR + " is not null then c." + Widgets.BUTTONS_BG_COLOR
						+ " else " + Sonet.default_buttons_bg_color + " end) as " + Widgets.BUTTONS_BG_COLOR
						+ ",(case when a." + Widgets.BUTTONS_COLOR + " is not null then a." + Widgets.BUTTONS_COLOR
						+ " when b." + Widgets.BUTTONS_COLOR + " is not null then b. " + Widgets.BUTTONS_COLOR
						+ " when c." + Widgets.BUTTONS_COLOR + " is not null then c." + Widgets.BUTTONS_COLOR
						+ " else " + Sonet.default_buttons_color + " end) as " + Widgets.BUTTONS_COLOR
						+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
						+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
						+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
						+ " else " + Sonet.default_friend_color + " end) as " + Widgets.FRIEND_COLOR
						+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
						+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
						+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
						+ " else " + Sonet.default_created_color + " end) as " + Widgets.CREATED_COLOR
						+ ",(case when a." + Widgets.MESSAGES_BG_COLOR + " is not null then a." + Widgets.MESSAGES_BG_COLOR
						+ " when b." + Widgets.MESSAGES_BG_COLOR + " is not null then b. " + Widgets.MESSAGES_BG_COLOR
						+ " when c." + Widgets.MESSAGES_BG_COLOR + " is not null then c." + Widgets.MESSAGES_BG_COLOR
						+ " else " + Sonet.default_message_bg_color + " end) as " + Widgets.MESSAGES_BG_COLOR
						+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
						+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
						+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
						+ " else " + Sonet.default_message_color + " end) as " + Widgets.MESSAGES_COLOR
						+ ",(case when a." + Widgets.TIME24HR + " is not null then a." + Widgets.TIME24HR
						+ " when b." + Widgets.TIME24HR + " is not null then b. " + Widgets.TIME24HR
						+ " when c." + Widgets.TIME24HR + " is not null then c." + Widgets.TIME24HR
						+ " else 0 end) as " + Widgets.TIME24HR
						+ ",(case when a." + Widgets.SCROLLABLE + " is not null then a." + Widgets.SCROLLABLE
						+ " when b." + Widgets.SCROLLABLE + " is not null then b. " + Widgets.SCROLLABLE
						+ " when c." + Widgets.SCROLLABLE + " is not null then c." + Widgets.SCROLLABLE
						+ " else 0 end) as " + Widgets.SCROLLABLE
						+ ",(case when a." + Widgets.BUTTONS_TEXTSIZE + " is not null then a." + Widgets.BUTTONS_TEXTSIZE
						+ " when b." + Widgets.BUTTONS_TEXTSIZE + " is not null then b. " + Widgets.BUTTONS_TEXTSIZE
						+ " when c." + Widgets.BUTTONS_TEXTSIZE + " is not null then c." + Widgets.BUTTONS_TEXTSIZE
						+ " else " + Sonet.default_buttons_textsize + " end) as " + Widgets.BUTTONS_TEXTSIZE
						+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
						+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
						+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
						+ " else " + Sonet.default_messages_textsize + " end) as " + Widgets.MESSAGES_TEXTSIZE
						+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
						+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
						+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
						+ " else " + Sonet.default_friend_textsize + " end) as " + Widgets.FRIEND_TEXTSIZE
						+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
						+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
						+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
						+ " else " + Sonet.default_created_textsize + " end) as " + Widgets.CREATED_TEXTSIZE
						+ ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
						+ ",(case when a." + Widgets.ICON + " is not null then a." + Widgets.ICON
						+ " when b." + Widgets.ICON + " is not null then b. " + Widgets.ICON
						+ " when c." + Widgets.ICON + " is not null then c." + Widgets.ICON
						+ " else 1 end) as " + Widgets.ICON
						+ ",(case when a." + Widgets.STATUSES_PER_ACCOUNT + " is not null then a." + Widgets.STATUSES_PER_ACCOUNT
						+ " when b." + Widgets.STATUSES_PER_ACCOUNT + " is not null then b. " + Widgets.STATUSES_PER_ACCOUNT
						+ " when c." + Widgets.STATUSES_PER_ACCOUNT + " is not null then c." + Widgets.STATUSES_PER_ACCOUNT
						+ " else " + Sonet.default_statuses_per_account + " end) as " + Widgets.STATUSES_PER_ACCOUNT
						+ ",(case when a." + Widgets.BACKGROUND_UPDATE + " is not null then a." + Widgets.BACKGROUND_UPDATE
						+ " when b." + Widgets.BACKGROUND_UPDATE + " is not null then b. " + Widgets.BACKGROUND_UPDATE
						+ " when c." + Widgets.BACKGROUND_UPDATE + " is not null then c." + Widgets.BACKGROUND_UPDATE
						+ " else 1 end) as " + Widgets.BACKGROUND_UPDATE
						+ ",(case when a." + Widgets.SOUND + " is not null then a." + Widgets.SOUND
						+ " when b." + Widgets.SOUND + " is not null then b. " + Widgets.SOUND
						+ " when c." + Widgets.SOUND + " is not null then c." + Widgets.SOUND
						+ " else 0 end) as " + Widgets.SOUND
						+ ",(case when a." + Widgets.VIBRATE + " is not null then a." + Widgets.VIBRATE
						+ " when b." + Widgets.VIBRATE + " is not null then b. " + Widgets.VIBRATE
						+ " when c." + Widgets.VIBRATE + " is not null then c." + Widgets.VIBRATE
						+ " else 0 end) as " + Widgets.VIBRATE
						+ ",(case when a." + Widgets.LIGHTS + " is not null then a." + Widgets.LIGHTS
						+ " when b." + Widgets.LIGHTS + " is not null then b. " + Widgets.LIGHTS
						+ " when c." + Widgets.LIGHTS + " is not null then c." + Widgets.LIGHTS
						+ " else 0 end) as " + Widgets.LIGHTS
						+ ",(case when a." + Widgets.DISPLAY_PROFILE + " is not null then a." + Widgets.DISPLAY_PROFILE
						+ " when b." + Widgets.DISPLAY_PROFILE + " is not null then b. " + Widgets.DISPLAY_PROFILE
						+ " when c." + Widgets.DISPLAY_PROFILE + " is not null then c." + Widgets.DISPLAY_PROFILE
						+ " else 1 end) as " + Widgets.DISPLAY_PROFILE
						+ " from " + TABLE_WIDGETS + " a,"
						+ TABLE_WIDGETS + " b,"
						+ TABLE_WIDGETS + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." + Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
			}
			if (oldVersion < 17) {
				// add updated column, this will clear all current notifications, to avoid duplicates
				db.execSQL("drop table if exists " + TABLE_NOTIFICATIONS + ";");
				db.execSQL("create table if not exists " + TABLE_NOTIFICATIONS
						+ " (" + Notifications._ID + " integer primary key autoincrement, "
						+ Notifications.SID + " text, "
						+ Notifications.ESID + " text, "
						+ Notifications.FRIEND + " text, "
						+ Notifications.MESSAGE + " text, "
						+ Notifications.CREATED + " integer, "
						+ Notifications.NOTIFICATION + " text, "
						+ Notifications.ACCOUNT + " integer, "
						+ Notifications.CLEARED + " integer, "
						+ Notifications.UPDATED + " integer);");
				// update statuses view to account for the new default settings handling
				db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						"s." + Statuses._ID + " as " + Statuses_styles._ID
						+ ",s." + Statuses.CREATED + " as " + Statuses_styles.CREATED
						+ ",(case when " + "s." + Statuses.FRIEND_OVERRIDE + " != \"\" then " + "s." + Statuses.FRIEND_OVERRIDE + " else " + "e." + Entities.FRIEND + " end) as " + Statuses_styles.FRIEND
						+ ",e." + Entities.PROFILE + " as " + Statuses_styles.PROFILE
						+ ",s." + Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE
						+ ",s." + Statuses.SERVICE + " as " + Statuses_styles.SERVICE
						+ ",s." + Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT
						+ ",s." + Statuses.WIDGET + " as " + Statuses_styles.WIDGET
						+ ",s." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT
						+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
						+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
						+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
						+ " else " + Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR
						+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
						+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
						+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
						+ " else " + Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR
						+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
						+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
						+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
						+ " else " + Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR
						+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
						+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
						+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
						+ " else " + Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE
						+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
						+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
						+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
						+ " else " + Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE
						+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
						+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
						+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
						+ " else " + Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE
						+ ",s." + Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG
						+ ",s." + Statuses.ICON + " as " + Statuses_styles.ICON
						+ ",s." + Statuses.SID + " as " + Statuses_styles.SID
						+ ",e." + Entities._ID + " as " + Statuses_styles.ENTITY
						+ ",e." + Entities.ESID + " as " + Statuses_styles.ESID
						+ " from " + TABLE_STATUSES + " s," + TABLE_ENTITIES + " e," + TABLE_WIDGETS + " a," + TABLE_WIDGETS + " b," + TABLE_WIDGETS + " c"
						+ " where "
						+ "e." + Entities._ID + "=s." + Statuses.ENTITY
						+ " and a." + Widgets.WIDGET + "=s." + Statuses.WIDGET
						+ " and a." + Widgets.ACCOUNT + "=s." + Statuses.ACCOUNT
						+ " and b." + Widgets.WIDGET + "=s." + Statuses.WIDGET
						+ " and b." + Widgets.ACCOUNT + "=-1"
						+ " and c." + Widgets.WIDGET + "=0"
						+ " and c." + Widgets.ACCOUNT + "=-1;");
			}
			if (oldVersion < 18) {
				// an error was reported where the updated column wasn't added, attempt to  verify that the last database update went through
				boolean update = false;
				Cursor c = db.rawQuery("select sql from sqlite_master where name='notifications';", null);
				if (c.moveToFirst()) {
					String sql = c.getString(0);
					if (!sql.contains(Notifications.UPDATED)) {
						update = true;
					}
				} else {
					update = true;
				}
				c.close();
				if (update) {
					// add updated column, this will clear all current notifications, to avoid duplicates
					db.execSQL("drop table if exists " + TABLE_NOTIFICATIONS + ";");
					db.execSQL("create table if not exists " + TABLE_NOTIFICATIONS
							+ " (" + Notifications._ID + " integer primary key autoincrement, "
							+ Notifications.SID + " text, "
							+ Notifications.ESID + " text, "
							+ Notifications.FRIEND + " text, "
							+ Notifications.MESSAGE + " text, "
							+ Notifications.CREATED + " integer, "
							+ Notifications.NOTIFICATION + " text, "
							+ Notifications.ACCOUNT + " integer, "
							+ Notifications.CLEARED + " integer, "
							+ Notifications.UPDATED + " integer);");
					// update statuses view to account for the new default settings handling
					db.execSQL("drop table if exists " + TABLE_STATUSES + "_bkp;");
					db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
					db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
							"s." + Statuses._ID + " as " + Statuses_styles._ID
							+ ",s." + Statuses.CREATED + " as " + Statuses_styles.CREATED
							+ ",(case when " + "s." + Statuses.FRIEND_OVERRIDE + " != \"\" then " + "s." + Statuses.FRIEND_OVERRIDE + " else " + "e." + Entities.FRIEND + " end) as " + Statuses_styles.FRIEND
							+ ",e." + Entities.PROFILE + " as " + Statuses_styles.PROFILE
							+ ",s." + Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE
							+ ",s." + Statuses.SERVICE + " as " + Statuses_styles.SERVICE
							+ ",s." + Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT
							+ ",s." + Statuses.WIDGET + " as " + Statuses_styles.WIDGET
							+ ",s." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT
							+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
							+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
							+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
							+ " else " + Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR
							+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
							+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
							+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
							+ " else " + Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR
							+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
							+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
							+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
							+ " else " + Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR
							+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
							+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
							+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
							+ " else " + Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE
							+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
							+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
							+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
							+ " else " + Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE
							+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
							+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
							+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
							+ " else " + Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE
							+ ",s." + Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG
							+ ",s." + Statuses.ICON + " as " + Statuses_styles.ICON
							+ ",s." + Statuses.SID + " as " + Statuses_styles.SID
							+ ",e." + Entities._ID + " as " + Statuses_styles.ENTITY
							+ ",e." + Entities.ESID + " as " + Statuses_styles.ESID
							+ " from " + TABLE_STATUSES + " s," + TABLE_ENTITIES + " e," + TABLE_WIDGETS + " a," + TABLE_WIDGETS + " b," + TABLE_WIDGETS + " c"
							+ " where "
							+ "e." + Entities._ID + "=s." + Statuses.ENTITY
							+ " and a." + Widgets.WIDGET + "=s." + Statuses.WIDGET
							+ " and a." + Widgets.ACCOUNT + "=s." + Statuses.ACCOUNT
							+ " and b." + Widgets.WIDGET + "=s." + Statuses.WIDGET
							+ " and b." + Widgets.ACCOUNT + "=-1"
							+ " and c." + Widgets.WIDGET + "=0"
							+ " and c." + Widgets.ACCOUNT + "=-1;");
				}
			}
			if (oldVersion < 19) {
				// add support for instant upload
				growTable(db, TABLE_WIDGETS, Widgets.INSTANT_UPLOAD, "integer", "0", false);
				growTable(db, TABLE_WIDGETS, Widgets.MARGIN, "integer", "0", false);
				db.execSQL("drop view if exists " + VIEW_WIDGETS_SETTINGS + ";");
				db.execSQL("create view if not exists " + VIEW_WIDGETS_SETTINGS + " as select a."
						+ Widgets._ID + " as " + Widgets._ID
						+ ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
						+ ",(case when a." + Widgets.INTERVAL + " is not null then a." + Widgets.INTERVAL
						+ " when b." + Widgets.INTERVAL + " is not null then b. " + Widgets.INTERVAL
						+ " when c." + Widgets.INTERVAL + " is not null then c." + Widgets.INTERVAL
						+ " else " + Sonet.default_interval + " end) as " + Widgets.INTERVAL
						+ ",(case when a." + Widgets.HASBUTTONS + " is not null then a." + Widgets.HASBUTTONS
						+ " when b." + Widgets.HASBUTTONS + " is not null then b. " + Widgets.HASBUTTONS
						+ " when c." + Widgets.HASBUTTONS + " is not null then c." + Widgets.HASBUTTONS
						+ " else 0 end) as " + Widgets.HASBUTTONS
						+ ",(case when a." + Widgets.BUTTONS_BG_COLOR + " is not null then a." + Widgets.BUTTONS_BG_COLOR
						+ " when b." + Widgets.BUTTONS_BG_COLOR + " is not null then b. " + Widgets.BUTTONS_BG_COLOR
						+ " when c." + Widgets.BUTTONS_BG_COLOR + " is not null then c." + Widgets.BUTTONS_BG_COLOR
						+ " else " + Sonet.default_buttons_bg_color + " end) as " + Widgets.BUTTONS_BG_COLOR
						+ ",(case when a." + Widgets.BUTTONS_COLOR + " is not null then a." + Widgets.BUTTONS_COLOR
						+ " when b." + Widgets.BUTTONS_COLOR + " is not null then b. " + Widgets.BUTTONS_COLOR
						+ " when c." + Widgets.BUTTONS_COLOR + " is not null then c." + Widgets.BUTTONS_COLOR
						+ " else " + Sonet.default_buttons_color + " end) as " + Widgets.BUTTONS_COLOR
						+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
						+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
						+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
						+ " else " + Sonet.default_friend_color + " end) as " + Widgets.FRIEND_COLOR
						+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
						+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
						+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
						+ " else " + Sonet.default_created_color + " end) as " + Widgets.CREATED_COLOR
						+ ",(case when a." + Widgets.MESSAGES_BG_COLOR + " is not null then a." + Widgets.MESSAGES_BG_COLOR
						+ " when b." + Widgets.MESSAGES_BG_COLOR + " is not null then b. " + Widgets.MESSAGES_BG_COLOR
						+ " when c." + Widgets.MESSAGES_BG_COLOR + " is not null then c." + Widgets.MESSAGES_BG_COLOR
						+ " else " + Sonet.default_message_bg_color + " end) as " + Widgets.MESSAGES_BG_COLOR
						+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
						+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
						+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
						+ " else " + Sonet.default_message_color + " end) as " + Widgets.MESSAGES_COLOR
						+ ",(case when a." + Widgets.TIME24HR + " is not null then a." + Widgets.TIME24HR
						+ " when b." + Widgets.TIME24HR + " is not null then b. " + Widgets.TIME24HR
						+ " when c." + Widgets.TIME24HR + " is not null then c." + Widgets.TIME24HR
						+ " else 0 end) as " + Widgets.TIME24HR
						+ ",(case when a." + Widgets.SCROLLABLE + " is not null then a." + Widgets.SCROLLABLE
						+ " when b." + Widgets.SCROLLABLE + " is not null then b. " + Widgets.SCROLLABLE
						+ " when c." + Widgets.SCROLLABLE + " is not null then c." + Widgets.SCROLLABLE
						+ " else 0 end) as " + Widgets.SCROLLABLE
						+ ",(case when a." + Widgets.BUTTONS_TEXTSIZE + " is not null then a." + Widgets.BUTTONS_TEXTSIZE
						+ " when b." + Widgets.BUTTONS_TEXTSIZE + " is not null then b. " + Widgets.BUTTONS_TEXTSIZE
						+ " when c." + Widgets.BUTTONS_TEXTSIZE + " is not null then c." + Widgets.BUTTONS_TEXTSIZE
						+ " else " + Sonet.default_buttons_textsize + " end) as " + Widgets.BUTTONS_TEXTSIZE
						+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
						+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
						+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
						+ " else " + Sonet.default_messages_textsize + " end) as " + Widgets.MESSAGES_TEXTSIZE
						+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
						+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
						+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
						+ " else " + Sonet.default_friend_textsize + " end) as " + Widgets.FRIEND_TEXTSIZE
						+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
						+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
						+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
						+ " else " + Sonet.default_created_textsize + " end) as " + Widgets.CREATED_TEXTSIZE
						+ ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
						+ ",(case when a." + Widgets.ICON + " is not null then a." + Widgets.ICON
						+ " when b." + Widgets.ICON + " is not null then b. " + Widgets.ICON
						+ " when c." + Widgets.ICON + " is not null then c." + Widgets.ICON
						+ " else 1 end) as " + Widgets.ICON
						+ ",(case when a." + Widgets.STATUSES_PER_ACCOUNT + " is not null then a." + Widgets.STATUSES_PER_ACCOUNT
						+ " when b." + Widgets.STATUSES_PER_ACCOUNT + " is not null then b. " + Widgets.STATUSES_PER_ACCOUNT
						+ " when c." + Widgets.STATUSES_PER_ACCOUNT + " is not null then c." + Widgets.STATUSES_PER_ACCOUNT
						+ " else " + Sonet.default_statuses_per_account + " end) as " + Widgets.STATUSES_PER_ACCOUNT
						+ ",(case when a." + Widgets.BACKGROUND_UPDATE + " is not null then a." + Widgets.BACKGROUND_UPDATE
						+ " when b." + Widgets.BACKGROUND_UPDATE + " is not null then b. " + Widgets.BACKGROUND_UPDATE
						+ " when c." + Widgets.BACKGROUND_UPDATE + " is not null then c." + Widgets.BACKGROUND_UPDATE
						+ " else 1 end) as " + Widgets.BACKGROUND_UPDATE
						+ ",(case when a." + Widgets.SOUND + " is not null then a." + Widgets.SOUND
						+ " when b." + Widgets.SOUND + " is not null then b. " + Widgets.SOUND
						+ " when c." + Widgets.SOUND + " is not null then c." + Widgets.SOUND
						+ " else 0 end) as " + Widgets.SOUND
						+ ",(case when a." + Widgets.VIBRATE + " is not null then a." + Widgets.VIBRATE
						+ " when b." + Widgets.VIBRATE + " is not null then b. " + Widgets.VIBRATE
						+ " when c." + Widgets.VIBRATE + " is not null then c." + Widgets.VIBRATE
						+ " else 0 end) as " + Widgets.VIBRATE
						+ ",(case when a." + Widgets.LIGHTS + " is not null then a." + Widgets.LIGHTS
						+ " when b." + Widgets.LIGHTS + " is not null then b. " + Widgets.LIGHTS
						+ " when c." + Widgets.LIGHTS + " is not null then c." + Widgets.LIGHTS
						+ " else 0 end) as " + Widgets.LIGHTS
						+ ",(case when a." + Widgets.DISPLAY_PROFILE + " is not null then a." + Widgets.DISPLAY_PROFILE
						+ " when b." + Widgets.DISPLAY_PROFILE + " is not null then b. " + Widgets.DISPLAY_PROFILE
						+ " when c." + Widgets.DISPLAY_PROFILE + " is not null then c." + Widgets.DISPLAY_PROFILE
						+ " else 1 end) as " + Widgets.DISPLAY_PROFILE
						+ ",(case when a." + Widgets.INSTANT_UPLOAD + " is not null then a." + Widgets.INSTANT_UPLOAD
						+ " when b." + Widgets.INSTANT_UPLOAD + " is not null then b. " + Widgets.INSTANT_UPLOAD
						+ " when c." + Widgets.INSTANT_UPLOAD + " is not null then c." + Widgets.INSTANT_UPLOAD
						+ " else 0 end) as " + Widgets.INSTANT_UPLOAD
						+ ",(case when a." + Widgets.MARGIN + " is not null then a." + Widgets.MARGIN
						+ " when b." + Widgets.MARGIN + " is not null then b. " + Widgets.MARGIN
						+ " when c." + Widgets.MARGIN + " is not null then c." + Widgets.MARGIN
						+ " else 0 end) as " + Widgets.MARGIN
						+ " from " + TABLE_WIDGETS + " a,"
						+ TABLE_WIDGETS + " b,"
						+ TABLE_WIDGETS + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." + Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
			}
			if (oldVersion < 20) {
				// move instant upload setting from account specific to widget specific
				Cursor c = db.query(TABLE_WIDGETS, new String[]{Widgets.WIDGET}, Widgets.ACCOUNT + "!=-1 and " + Widgets.INSTANT_UPLOAD + "=1", null, null, null, null);
				if (c.moveToFirst()) {
					while(!c.isAfterLast()) {
						ContentValues values = new ContentValues();
						values.put(Widgets.INSTANT_UPLOAD, 1);
						db.update(TABLE_WIDGETS, values, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=-1", new String[]{Integer.toString(c.getInt(0))});
						c.moveToNext();
					}
				}
				c.close();
			}
			if (oldVersion < 21) {
				// update wasn't added in onCreate, patch it up
				boolean update = false;
				Cursor c = db.rawQuery("select sql from sqlite_master where name='notifications';", null);
				if (c.moveToFirst()) {
					String sql = c.getString(0);
					if (!sql.contains(Notifications.UPDATED)) {
						update = true;
					}
				} else {
					update = true;
				}
				c.close();
				if (update) {
					// add updated column, this will clear all current notifications, to avoid duplicates
					db.execSQL("drop table if exists " + TABLE_NOTIFICATIONS + "_bkp;");
					db.execSQL("create temp table " + TABLE_NOTIFICATIONS + "_bkp as select * from " + TABLE_NOTIFICATIONS + ";");
					db.execSQL("drop table if exists " + TABLE_NOTIFICATIONS + ";");
					db.execSQL("create table if not exists " + TABLE_NOTIFICATIONS
							+ " (" + Notifications._ID + " integer primary key autoincrement, "
							+ Notifications.SID + " text, "
							+ Notifications.ESID + " text, "
							+ Notifications.FRIEND + " text, "
							+ Notifications.MESSAGE + " text, "
							+ Notifications.CREATED + " integer, "
							+ Notifications.NOTIFICATION + " text, "
							+ Notifications.ACCOUNT + " integer, "
							+ Notifications.CLEARED + " integer, "
							+ Notifications.UPDATED + " integer);");
					db.execSQL("insert into " + TABLE_NOTIFICATIONS
							+ " select "
							+ Notifications._ID
							+ "," + Notifications.SID
							+ "," + Notifications.ESID
							+ "," + Notifications.FRIEND
							+ "," + Notifications.MESSAGE
							+ "," + Notifications.CREATED
							+ "," + Notifications.NOTIFICATION
							+ "," + Notifications.ACCOUNT
							+ "," + Notifications.CLEARED
							+ "," + Notifications.CREATED + " from " + TABLE_NOTIFICATIONS + "_bkp;");
					db.execSQL("drop table if exists " + TABLE_NOTIFICATIONS + "_bkp;");
				}
			}
			if (oldVersion < 22) {
				db.execSQL("create table if not exists " + TABLE_STATUS_LINKS
						+ " (" + Status_links._ID + " integer primary key autoincrement, "
						+ Status_links.STATUS_ID + " integer, "
						+ Status_links.LINK_URI + " text, "
						+ Status_links.LINK_TYPE + " text);");
			}
			if (oldVersion < 23) {
				// clean up duplicate widget settings
				Cursor c = db.query(TABLE_WIDGETS, new String[]{Widgets._ID}, Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1", null, null, null, null);
				if (c.moveToFirst()) {
					if (c.moveToNext()) {
						while (!c.isAfterLast()) {
							db.delete(TABLE_WIDGETS, Widgets._ID + "=?", new String[]{Long.toString(c.getLong(0))});
							c.moveToNext();
						}
					}
				}
				c.close();
			}
			if (oldVersion < 24) {
				growTable(db, TABLE_WIDGETS, Widgets.PROFILES_BG_COLOR, "integer", Widgets.MESSAGES_BG_COLOR, false);
				growTable(db, TABLE_STATUSES, Statuses.PROFILE_BG, "blob", Statuses.STATUS_BG, false);
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						"s." + Statuses._ID + " as " + Statuses_styles._ID
						+ ",s." + Statuses.CREATED + " as " + Statuses_styles.CREATED
						+ ",(case when " + "s." + Statuses.FRIEND_OVERRIDE + " != \"\" then " + "s." + Statuses.FRIEND_OVERRIDE + " else " + "e." + Entities.FRIEND + " end) as " + Statuses_styles.FRIEND
						+ ",e." + Entities.PROFILE + " as " + Statuses_styles.PROFILE
						+ ",s." + Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE
						+ ",s." + Statuses.SERVICE + " as " + Statuses_styles.SERVICE
						+ ",s." + Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT
						+ ",s." + Statuses.WIDGET + " as " + Statuses_styles.WIDGET
						+ ",s." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT
						+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
						+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
						+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
						+ " else " + Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR
						+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
						+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
						+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
						+ " else " + Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR
						+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
						+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
						+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
						+ " else " + Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR
						+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
						+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
						+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
						+ " else " + Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE
						+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
						+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
						+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
						+ " else " + Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE
						+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
						+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
						+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
						+ " else " + Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE
						+ ",s." + Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG
						+ ",s." + Statuses.ICON + " as " + Statuses_styles.ICON
						+ ",s." + Statuses.SID + " as " + Statuses_styles.SID
						+ ",e." + Entities._ID + " as " + Statuses_styles.ENTITY
						+ ",e." + Entities.ESID + " as " + Statuses_styles.ESID
						+ ",s." + Statuses.PROFILE_BG + " as " + Statuses_styles.PROFILE_BG
						+ " from " + TABLE_STATUSES + " s," + TABLE_ENTITIES + " e," + TABLE_WIDGETS + " a," + TABLE_WIDGETS + " b," + TABLE_WIDGETS + " c"
						+ " where "
						+ "e." + Entities._ID + "=s." + Statuses.ENTITY
						+ " and a." + Widgets.WIDGET + "=s." + Statuses.WIDGET
						+ " and a." + Widgets.ACCOUNT + "=s." + Statuses.ACCOUNT
						+ " and b." + Widgets.WIDGET + "=s." + Statuses.WIDGET
						+ " and b." + Widgets.ACCOUNT + "=-1"
						+ " and c." + Widgets.WIDGET + "=0"
						+ " and c." + Widgets.ACCOUNT + "=-1;");
				db.execSQL("drop view if exists " + VIEW_WIDGETS_SETTINGS + ";");
				db.execSQL("create view if not exists " + VIEW_WIDGETS_SETTINGS + " as select a."
						+ Widgets._ID + " as " + Widgets._ID
						+ ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
						+ ",(case when a." + Widgets.INTERVAL + " is not null then a." + Widgets.INTERVAL
						+ " when b." + Widgets.INTERVAL + " is not null then b. " + Widgets.INTERVAL
						+ " when c." + Widgets.INTERVAL + " is not null then c." + Widgets.INTERVAL
						+ " else " + Sonet.default_interval + " end) as " + Widgets.INTERVAL
						+ ",(case when a." + Widgets.HASBUTTONS + " is not null then a." + Widgets.HASBUTTONS
						+ " when b." + Widgets.HASBUTTONS + " is not null then b. " + Widgets.HASBUTTONS
						+ " when c." + Widgets.HASBUTTONS + " is not null then c." + Widgets.HASBUTTONS
						+ " else 0 end) as " + Widgets.HASBUTTONS
						+ ",(case when a." + Widgets.BUTTONS_BG_COLOR + " is not null then a." + Widgets.BUTTONS_BG_COLOR
						+ " when b." + Widgets.BUTTONS_BG_COLOR + " is not null then b. " + Widgets.BUTTONS_BG_COLOR
						+ " when c." + Widgets.BUTTONS_BG_COLOR + " is not null then c." + Widgets.BUTTONS_BG_COLOR
						+ " else " + Sonet.default_buttons_bg_color + " end) as " + Widgets.BUTTONS_BG_COLOR
						+ ",(case when a." + Widgets.BUTTONS_COLOR + " is not null then a." + Widgets.BUTTONS_COLOR
						+ " when b." + Widgets.BUTTONS_COLOR + " is not null then b. " + Widgets.BUTTONS_COLOR
						+ " when c." + Widgets.BUTTONS_COLOR + " is not null then c." + Widgets.BUTTONS_COLOR
						+ " else " + Sonet.default_buttons_color + " end) as " + Widgets.BUTTONS_COLOR
						+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
						+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
						+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
						+ " else " + Sonet.default_friend_color + " end) as " + Widgets.FRIEND_COLOR
						+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
						+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
						+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
						+ " else " + Sonet.default_created_color + " end) as " + Widgets.CREATED_COLOR
						+ ",(case when a." + Widgets.MESSAGES_BG_COLOR + " is not null then a." + Widgets.MESSAGES_BG_COLOR
						+ " when b." + Widgets.MESSAGES_BG_COLOR + " is not null then b. " + Widgets.MESSAGES_BG_COLOR
						+ " when c." + Widgets.MESSAGES_BG_COLOR + " is not null then c." + Widgets.MESSAGES_BG_COLOR
						+ " else " + Sonet.default_message_bg_color + " end) as " + Widgets.MESSAGES_BG_COLOR
						+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
						+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
						+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
						+ " else " + Sonet.default_message_color + " end) as " + Widgets.MESSAGES_COLOR
						+ ",(case when a." + Widgets.TIME24HR + " is not null then a." + Widgets.TIME24HR
						+ " when b." + Widgets.TIME24HR + " is not null then b. " + Widgets.TIME24HR
						+ " when c." + Widgets.TIME24HR + " is not null then c." + Widgets.TIME24HR
						+ " else 0 end) as " + Widgets.TIME24HR
						+ ",(case when a." + Widgets.SCROLLABLE + " is not null then a." + Widgets.SCROLLABLE
						+ " when b." + Widgets.SCROLLABLE + " is not null then b. " + Widgets.SCROLLABLE
						+ " when c." + Widgets.SCROLLABLE + " is not null then c." + Widgets.SCROLLABLE
						+ " else 0 end) as " + Widgets.SCROLLABLE
						+ ",(case when a." + Widgets.BUTTONS_TEXTSIZE + " is not null then a." + Widgets.BUTTONS_TEXTSIZE
						+ " when b." + Widgets.BUTTONS_TEXTSIZE + " is not null then b. " + Widgets.BUTTONS_TEXTSIZE
						+ " when c." + Widgets.BUTTONS_TEXTSIZE + " is not null then c." + Widgets.BUTTONS_TEXTSIZE
						+ " else " + Sonet.default_buttons_textsize + " end) as " + Widgets.BUTTONS_TEXTSIZE
						+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
						+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
						+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
						+ " else " + Sonet.default_messages_textsize + " end) as " + Widgets.MESSAGES_TEXTSIZE
						+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
						+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
						+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
						+ " else " + Sonet.default_friend_textsize + " end) as " + Widgets.FRIEND_TEXTSIZE
						+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
						+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
						+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
						+ " else " + Sonet.default_created_textsize + " end) as " + Widgets.CREATED_TEXTSIZE
						+ ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
						+ ",(case when a." + Widgets.ICON + " is not null then a." + Widgets.ICON
						+ " when b." + Widgets.ICON + " is not null then b. " + Widgets.ICON
						+ " when c." + Widgets.ICON + " is not null then c." + Widgets.ICON
						+ " else 1 end) as " + Widgets.ICON
						+ ",(case when a." + Widgets.STATUSES_PER_ACCOUNT + " is not null then a." + Widgets.STATUSES_PER_ACCOUNT
						+ " when b." + Widgets.STATUSES_PER_ACCOUNT + " is not null then b. " + Widgets.STATUSES_PER_ACCOUNT
						+ " when c." + Widgets.STATUSES_PER_ACCOUNT + " is not null then c." + Widgets.STATUSES_PER_ACCOUNT
						+ " else " + Sonet.default_statuses_per_account + " end) as " + Widgets.STATUSES_PER_ACCOUNT
						+ ",(case when a." + Widgets.BACKGROUND_UPDATE + " is not null then a." + Widgets.BACKGROUND_UPDATE
						+ " when b." + Widgets.BACKGROUND_UPDATE + " is not null then b. " + Widgets.BACKGROUND_UPDATE
						+ " when c." + Widgets.BACKGROUND_UPDATE + " is not null then c." + Widgets.BACKGROUND_UPDATE
						+ " else 1 end) as " + Widgets.BACKGROUND_UPDATE
						+ ",(case when a." + Widgets.SOUND + " is not null then a." + Widgets.SOUND
						+ " when b." + Widgets.SOUND + " is not null then b. " + Widgets.SOUND
						+ " when c." + Widgets.SOUND + " is not null then c." + Widgets.SOUND
						+ " else 0 end) as " + Widgets.SOUND
						+ ",(case when a." + Widgets.VIBRATE + " is not null then a." + Widgets.VIBRATE
						+ " when b." + Widgets.VIBRATE + " is not null then b. " + Widgets.VIBRATE
						+ " when c." + Widgets.VIBRATE + " is not null then c." + Widgets.VIBRATE
						+ " else 0 end) as " + Widgets.VIBRATE
						+ ",(case when a." + Widgets.LIGHTS + " is not null then a." + Widgets.LIGHTS
						+ " when b." + Widgets.LIGHTS + " is not null then b. " + Widgets.LIGHTS
						+ " when c." + Widgets.LIGHTS + " is not null then c." + Widgets.LIGHTS
						+ " else 0 end) as " + Widgets.LIGHTS
						+ ",(case when a." + Widgets.DISPLAY_PROFILE + " is not null then a." + Widgets.DISPLAY_PROFILE
						+ " when b." + Widgets.DISPLAY_PROFILE + " is not null then b. " + Widgets.DISPLAY_PROFILE
						+ " when c." + Widgets.DISPLAY_PROFILE + " is not null then c." + Widgets.DISPLAY_PROFILE
						+ " else 1 end) as " + Widgets.DISPLAY_PROFILE
						+ ",(case when a." + Widgets.INSTANT_UPLOAD + " is not null then a." + Widgets.INSTANT_UPLOAD
						+ " when b." + Widgets.INSTANT_UPLOAD + " is not null then b. " + Widgets.INSTANT_UPLOAD
						+ " when c." + Widgets.INSTANT_UPLOAD + " is not null then c." + Widgets.INSTANT_UPLOAD
						+ " else 0 end) as " + Widgets.INSTANT_UPLOAD
						+ ",(case when a." + Widgets.MARGIN + " is not null then a." + Widgets.MARGIN
						+ " when b." + Widgets.MARGIN + " is not null then b. " + Widgets.MARGIN
						+ " when c." + Widgets.MARGIN + " is not null then c." + Widgets.MARGIN
						+ " else 0 end) as " + Widgets.MARGIN
						+ ",(case when a." + Widgets.PROFILES_BG_COLOR + " is not null then a." + Widgets.PROFILES_BG_COLOR
						+ " when b." + Widgets.PROFILES_BG_COLOR + " is not null then b. " + Widgets.PROFILES_BG_COLOR
						+ " when c." + Widgets.PROFILES_BG_COLOR + " is not null then c." + Widgets.PROFILES_BG_COLOR
						+ " else " + Sonet.default_message_bg_color + " end) as " + Widgets.PROFILES_BG_COLOR
						+ " from " + TABLE_WIDGETS + " a,"
						+ TABLE_WIDGETS + " b,"
						+ TABLE_WIDGETS + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." + Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
			}
			if (oldVersion < 25) {
				growTable(db, TABLE_STATUSES, Statuses.FRIEND_BG, "blob", Statuses.STATUS_BG, false);
				growTable(db, TABLE_WIDGETS, Widgets.FRIEND_BG_COLOR, "integer", Widgets.MESSAGES_BG_COLOR, false);
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						"s." + Statuses._ID + " as " + Statuses_styles._ID
						+ ",s." + Statuses.CREATED + " as " + Statuses_styles.CREATED
						+ ",(case when " + "s." + Statuses.FRIEND_OVERRIDE + " != \"\" then " + "s." + Statuses.FRIEND_OVERRIDE + " else " + "e." + Entities.FRIEND + " end) as " + Statuses_styles.FRIEND
						+ ",e." + Entities.PROFILE + " as " + Statuses_styles.PROFILE
						+ ",s." + Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE
						+ ",s." + Statuses.SERVICE + " as " + Statuses_styles.SERVICE
						+ ",s." + Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT
						+ ",s." + Statuses.WIDGET + " as " + Statuses_styles.WIDGET
						+ ",s." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT
						+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
						+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
						+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
						+ " else " + Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR
						+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
						+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
						+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
						+ " else " + Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR
						+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
						+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
						+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
						+ " else " + Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR
						+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
						+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
						+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
						+ " else " + Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE
						+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
						+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
						+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
						+ " else " + Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE
						+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
						+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
						+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
						+ " else " + Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE
						+ ",s." + Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG
						+ ",s." + Statuses.ICON + " as " + Statuses_styles.ICON
						+ ",s." + Statuses.SID + " as " + Statuses_styles.SID
						+ ",e." + Entities._ID + " as " + Statuses_styles.ENTITY
						+ ",e." + Entities.ESID + " as " + Statuses_styles.ESID
						+ ",s." + Statuses.PROFILE_BG + " as " + Statuses_styles.PROFILE_BG
						+ ",s." + Statuses.FRIEND_BG + " as " + Statuses_styles.FRIEND_BG
						+ " from " + TABLE_STATUSES + " s," + TABLE_ENTITIES + " e," + TABLE_WIDGETS + " a," + TABLE_WIDGETS + " b," + TABLE_WIDGETS + " c"
						+ " where "
						+ "e." + Entities._ID + "=s." + Statuses.ENTITY
						+ " and a." + Widgets.WIDGET + "=s." + Statuses.WIDGET
						+ " and a." + Widgets.ACCOUNT + "=s." + Statuses.ACCOUNT
						+ " and b." + Widgets.WIDGET + "=s." + Statuses.WIDGET
						+ " and b." + Widgets.ACCOUNT + "=-1"
						+ " and c." + Widgets.WIDGET + "=0"
						+ " and c." + Widgets.ACCOUNT + "=-1;");
				db.execSQL("drop view if exists " + VIEW_WIDGETS_SETTINGS + ";");
				// create a view for the widget settings
				db.execSQL("create view if not exists " + VIEW_WIDGETS_SETTINGS + " as select a."
						+ Widgets._ID + " as " + Widgets._ID
						+ ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
						+ ",(case when a." + Widgets.INTERVAL + " is not null then a." + Widgets.INTERVAL
						+ " when b." + Widgets.INTERVAL + " is not null then b. " + Widgets.INTERVAL
						+ " when c." + Widgets.INTERVAL + " is not null then c." + Widgets.INTERVAL
						+ " else " + Sonet.default_interval + " end) as " + Widgets.INTERVAL
						+ ",(case when a." + Widgets.HASBUTTONS + " is not null then a." + Widgets.HASBUTTONS
						+ " when b." + Widgets.HASBUTTONS + " is not null then b. " + Widgets.HASBUTTONS
						+ " when c." + Widgets.HASBUTTONS + " is not null then c." + Widgets.HASBUTTONS
						+ " else 0 end) as " + Widgets.HASBUTTONS
						+ ",(case when a." + Widgets.BUTTONS_BG_COLOR + " is not null then a." + Widgets.BUTTONS_BG_COLOR
						+ " when b." + Widgets.BUTTONS_BG_COLOR + " is not null then b. " + Widgets.BUTTONS_BG_COLOR
						+ " when c." + Widgets.BUTTONS_BG_COLOR + " is not null then c." + Widgets.BUTTONS_BG_COLOR
						+ " else " + Sonet.default_buttons_bg_color + " end) as " + Widgets.BUTTONS_BG_COLOR
						+ ",(case when a." + Widgets.BUTTONS_COLOR + " is not null then a." + Widgets.BUTTONS_COLOR
						+ " when b." + Widgets.BUTTONS_COLOR + " is not null then b. " + Widgets.BUTTONS_COLOR
						+ " when c." + Widgets.BUTTONS_COLOR + " is not null then c." + Widgets.BUTTONS_COLOR
						+ " else " + Sonet.default_buttons_color + " end) as " + Widgets.BUTTONS_COLOR
						+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
						+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
						+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
						+ " else " + Sonet.default_friend_color + " end) as " + Widgets.FRIEND_COLOR
						+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
						+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
						+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
						+ " else " + Sonet.default_created_color + " end) as " + Widgets.CREATED_COLOR
						+ ",(case when a." + Widgets.MESSAGES_BG_COLOR + " is not null then a." + Widgets.MESSAGES_BG_COLOR
						+ " when b." + Widgets.MESSAGES_BG_COLOR + " is not null then b. " + Widgets.MESSAGES_BG_COLOR
						+ " when c." + Widgets.MESSAGES_BG_COLOR + " is not null then c." + Widgets.MESSAGES_BG_COLOR
						+ " else " + Sonet.default_message_bg_color + " end) as " + Widgets.MESSAGES_BG_COLOR
						+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
						+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
						+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
						+ " else " + Sonet.default_message_color + " end) as " + Widgets.MESSAGES_COLOR
						+ ",(case when a." + Widgets.TIME24HR + " is not null then a." + Widgets.TIME24HR
						+ " when b." + Widgets.TIME24HR + " is not null then b. " + Widgets.TIME24HR
						+ " when c." + Widgets.TIME24HR + " is not null then c." + Widgets.TIME24HR
						+ " else 0 end) as " + Widgets.TIME24HR
						+ ",(case when a." + Widgets.SCROLLABLE + " is not null then a." + Widgets.SCROLLABLE
						+ " when b." + Widgets.SCROLLABLE + " is not null then b. " + Widgets.SCROLLABLE
						+ " when c." + Widgets.SCROLLABLE + " is not null then c." + Widgets.SCROLLABLE
						+ " else 0 end) as " + Widgets.SCROLLABLE
						+ ",(case when a." + Widgets.BUTTONS_TEXTSIZE + " is not null then a." + Widgets.BUTTONS_TEXTSIZE
						+ " when b." + Widgets.BUTTONS_TEXTSIZE + " is not null then b. " + Widgets.BUTTONS_TEXTSIZE
						+ " when c." + Widgets.BUTTONS_TEXTSIZE + " is not null then c." + Widgets.BUTTONS_TEXTSIZE
						+ " else " + Sonet.default_buttons_textsize + " end) as " + Widgets.BUTTONS_TEXTSIZE
						+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
						+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
						+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
						+ " else " + Sonet.default_messages_textsize + " end) as " + Widgets.MESSAGES_TEXTSIZE
						+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
						+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
						+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
						+ " else " + Sonet.default_friend_textsize + " end) as " + Widgets.FRIEND_TEXTSIZE
						+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
						+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
						+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
						+ " else " + Sonet.default_created_textsize + " end) as " + Widgets.CREATED_TEXTSIZE
						+ ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
						+ ",(case when a." + Widgets.ICON + " is not null then a." + Widgets.ICON
						+ " when b." + Widgets.ICON + " is not null then b. " + Widgets.ICON
						+ " when c." + Widgets.ICON + " is not null then c." + Widgets.ICON
						+ " else 1 end) as " + Widgets.ICON
						+ ",(case when a." + Widgets.STATUSES_PER_ACCOUNT + " is not null then a." + Widgets.STATUSES_PER_ACCOUNT
						+ " when b." + Widgets.STATUSES_PER_ACCOUNT + " is not null then b. " + Widgets.STATUSES_PER_ACCOUNT
						+ " when c." + Widgets.STATUSES_PER_ACCOUNT + " is not null then c." + Widgets.STATUSES_PER_ACCOUNT
						+ " else " + Sonet.default_statuses_per_account + " end) as " + Widgets.STATUSES_PER_ACCOUNT
						+ ",(case when a." + Widgets.BACKGROUND_UPDATE + " is not null then a." + Widgets.BACKGROUND_UPDATE
						+ " when b." + Widgets.BACKGROUND_UPDATE + " is not null then b. " + Widgets.BACKGROUND_UPDATE
						+ " when c." + Widgets.BACKGROUND_UPDATE + " is not null then c." + Widgets.BACKGROUND_UPDATE
						+ " else 1 end) as " + Widgets.BACKGROUND_UPDATE
						+ ",(case when a." + Widgets.SOUND + " is not null then a." + Widgets.SOUND
						+ " when b." + Widgets.SOUND + " is not null then b. " + Widgets.SOUND
						+ " when c." + Widgets.SOUND + " is not null then c." + Widgets.SOUND
						+ " else 0 end) as " + Widgets.SOUND
						+ ",(case when a." + Widgets.VIBRATE + " is not null then a." + Widgets.VIBRATE
						+ " when b." + Widgets.VIBRATE + " is not null then b. " + Widgets.VIBRATE
						+ " when c." + Widgets.VIBRATE + " is not null then c." + Widgets.VIBRATE
						+ " else 0 end) as " + Widgets.VIBRATE
						+ ",(case when a." + Widgets.LIGHTS + " is not null then a." + Widgets.LIGHTS
						+ " when b." + Widgets.LIGHTS + " is not null then b. " + Widgets.LIGHTS
						+ " when c." + Widgets.LIGHTS + " is not null then c." + Widgets.LIGHTS
						+ " else 0 end) as " + Widgets.LIGHTS
						+ ",(case when a." + Widgets.DISPLAY_PROFILE + " is not null then a." + Widgets.DISPLAY_PROFILE
						+ " when b." + Widgets.DISPLAY_PROFILE + " is not null then b. " + Widgets.DISPLAY_PROFILE
						+ " when c." + Widgets.DISPLAY_PROFILE + " is not null then c." + Widgets.DISPLAY_PROFILE
						+ " else 1 end) as " + Widgets.DISPLAY_PROFILE
						+ ",(case when a." + Widgets.INSTANT_UPLOAD + " is not null then a." + Widgets.INSTANT_UPLOAD
						+ " when b." + Widgets.INSTANT_UPLOAD + " is not null then b. " + Widgets.INSTANT_UPLOAD
						+ " when c." + Widgets.INSTANT_UPLOAD + " is not null then c." + Widgets.INSTANT_UPLOAD
						+ " else 0 end) as " + Widgets.INSTANT_UPLOAD
						+ ",(case when a." + Widgets.MARGIN + " is not null then a." + Widgets.MARGIN
						+ " when b." + Widgets.MARGIN + " is not null then b. " + Widgets.MARGIN
						+ " when c." + Widgets.MARGIN + " is not null then c." + Widgets.MARGIN
						+ " else 0 end) as " + Widgets.MARGIN
						+ ",(case when a." + Widgets.PROFILES_BG_COLOR + " is not null then a." + Widgets.PROFILES_BG_COLOR
						+ " when b." + Widgets.PROFILES_BG_COLOR + " is not null then b. " + Widgets.PROFILES_BG_COLOR
						+ " when c." + Widgets.PROFILES_BG_COLOR + " is not null then c." + Widgets.PROFILES_BG_COLOR
						+ " else " + Sonet.default_message_bg_color + " end) as " + Widgets.PROFILES_BG_COLOR
						+ ",(case when a." + Widgets.FRIEND_BG_COLOR + " is not null then a." + Widgets.FRIEND_BG_COLOR
						+ " when b." + Widgets.FRIEND_BG_COLOR + " is not null then b. " + Widgets.FRIEND_BG_COLOR
						+ " when c." + Widgets.FRIEND_BG_COLOR + " is not null then c." + Widgets.FRIEND_BG_COLOR
						+ " else " + Sonet.default_friend_bg_color + " end) as " + Widgets.FRIEND_BG_COLOR
						+ " from " + TABLE_WIDGETS + " a,"
						+ TABLE_WIDGETS + " b,"
						+ TABLE_WIDGETS + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." + Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
			}
			if (oldVersion < 26) {
				db.execSQL("drop table if exists " + TABLE_STATUS_IMAGES + ";");
				db.execSQL("create table if not exists " + TABLE_STATUS_IMAGES
						+ " (" + Status_images._ID + " integer primary key autoincrement, "
						+ Status_images.STATUS_ID + " integer, "
						+ Status_images.IMAGE + " blob, "
						+ Status_images.IMAGE_BG + " blob);");
				db.execSQL("drop view if exists " + VIEW_STATUSES_STYLES + ";");
				db.execSQL("create view if not exists " + VIEW_STATUSES_STYLES + " as select " +
						"s." + Statuses._ID + " as " + Statuses_styles._ID
						+ ",s." + Statuses.CREATED + " as " + Statuses_styles.CREATED
						+ ",(case when " + "s." + Statuses.FRIEND_OVERRIDE + " != \"\" then " + "s." + Statuses.FRIEND_OVERRIDE + " else " + "e." + Entities.FRIEND + " end) as " + Statuses_styles.FRIEND
						+ ",e." + Entities.PROFILE + " as " + Statuses_styles.PROFILE
						+ ",s." + Statuses.MESSAGE + " as " + Statuses_styles.MESSAGE
						+ ",s." + Statuses.SERVICE + " as " + Statuses_styles.SERVICE
						+ ",s." + Statuses.CREATEDTEXT + " as " + Statuses_styles.CREATEDTEXT
						+ ",s." + Statuses.WIDGET + " as " + Statuses_styles.WIDGET
						+ ",s." + Statuses.ACCOUNT + " as " + Statuses_styles.ACCOUNT
						+ ",(case when a." + Widgets.FRIEND_COLOR + " is not null then a." + Widgets.FRIEND_COLOR
						+ " when b." + Widgets.FRIEND_COLOR + " is not null then b. " + Widgets.FRIEND_COLOR
						+ " when c." + Widgets.FRIEND_COLOR + " is not null then c." + Widgets.FRIEND_COLOR
						+ " else " + Sonet.default_friend_color + " end) as " + Statuses_styles.FRIEND_COLOR
						+ ",(case when a." + Widgets.CREATED_COLOR + " is not null then a." + Widgets.CREATED_COLOR
						+ " when b." + Widgets.CREATED_COLOR + " is not null then b. " + Widgets.CREATED_COLOR
						+ " when c." + Widgets.CREATED_COLOR + " is not null then c." + Widgets.CREATED_COLOR
						+ " else " + Sonet.default_created_color + " end) as " + Statuses_styles.CREATED_COLOR
						+ ",(case when a." + Widgets.MESSAGES_COLOR + " is not null then a." + Widgets.MESSAGES_COLOR
						+ " when b." + Widgets.MESSAGES_COLOR + " is not null then b. " + Widgets.MESSAGES_COLOR
						+ " when c." + Widgets.MESSAGES_COLOR + " is not null then c." + Widgets.MESSAGES_COLOR
						+ " else " + Sonet.default_message_color + " end) as " + Statuses_styles.MESSAGES_COLOR
						+ ",(case when a." + Widgets.MESSAGES_TEXTSIZE + " is not null then a." + Widgets.MESSAGES_TEXTSIZE
						+ " when b." + Widgets.MESSAGES_TEXTSIZE + " is not null then b. " + Widgets.MESSAGES_TEXTSIZE
						+ " when c." + Widgets.MESSAGES_TEXTSIZE + " is not null then c." + Widgets.MESSAGES_TEXTSIZE
						+ " else " + Sonet.default_messages_textsize + " end) as " + Statuses_styles.MESSAGES_TEXTSIZE
						+ ",(case when a." + Widgets.FRIEND_TEXTSIZE + " is not null then a." + Widgets.FRIEND_TEXTSIZE
						+ " when b." + Widgets.FRIEND_TEXTSIZE + " is not null then b. " + Widgets.FRIEND_TEXTSIZE
						+ " when c." + Widgets.FRIEND_TEXTSIZE + " is not null then c." + Widgets.FRIEND_TEXTSIZE
						+ " else " + Sonet.default_friend_textsize + " end) as " + Statuses_styles.FRIEND_TEXTSIZE
						+ ",(case when a." + Widgets.CREATED_TEXTSIZE + " is not null then a." + Widgets.CREATED_TEXTSIZE
						+ " when b." + Widgets.CREATED_TEXTSIZE + " is not null then b. " + Widgets.CREATED_TEXTSIZE
						+ " when c." + Widgets.CREATED_TEXTSIZE + " is not null then c." + Widgets.CREATED_TEXTSIZE
						+ " else " + Sonet.default_created_textsize + " end) as " + Statuses_styles.CREATED_TEXTSIZE
						+ ",s." + Statuses.STATUS_BG + " as " + Statuses_styles.STATUS_BG
						+ ",s." + Statuses.ICON + " as " + Statuses_styles.ICON
						+ ",s." + Statuses.SID + " as " + Statuses_styles.SID
						+ ",e." + Entities._ID + " as " + Statuses_styles.ENTITY
						+ ",e." + Entities.ESID + " as " + Statuses_styles.ESID
						+ ",s." + Statuses.PROFILE_BG + " as " + Statuses_styles.PROFILE_BG
						+ ",s." + Statuses.FRIEND_BG + " as " + Statuses_styles.FRIEND_BG
						+ ",i." + Status_images.IMAGE_BG + " as " + Statuses_styles.IMAGE_BG
						+ ",i." + Status_images.IMAGE + " as " + Statuses_styles.IMAGE
						+ " from " + TABLE_STATUSES + " s," + TABLE_ENTITIES + " e," + TABLE_WIDGETS + " a," + TABLE_WIDGETS + " b," + TABLE_WIDGETS + " c"
						+ " left join " + TABLE_STATUS_IMAGES + " i"
						+ " on i." + Status_images.STATUS_ID + "=s." + Statuses._ID
						+ " where "
						+ "e." + Entities._ID + "=s." + Statuses.ENTITY
						+ " and a." + Widgets.WIDGET + "=s." + Statuses.WIDGET
						+ " and a." + Widgets.ACCOUNT + "=s." + Statuses.ACCOUNT
						+ " and b." + Widgets.WIDGET + "=s." + Statuses.WIDGET
						+ " and b." + Widgets.ACCOUNT + "=-1"
						+ " and c." + Widgets.WIDGET + "=0"
						+ " and c." + Widgets.ACCOUNT + "=-1;");
			}
		}

		private boolean growTable(SQLiteDatabase db, String tableName, String columnName, String columnType, String columnValue, boolean quotedValue) {
			boolean success = false;
			Cursor sqlite_master = db.query("sqlite_master", new String[]{"sql"}, "tbl_name=? and type='table'", new String[]{tableName}, null, null, null);
			if (sqlite_master.moveToFirst()) {
				// get the existing table structure
				String sql = sqlite_master.getString(0);
				db.execSQL("drop table if exists " + tableName + "_bkp;");
				db.execSQL("create temp table " + tableName + "_bkp as select * from " + tableName + ";");
				db.execSQL("drop table if exists " + tableName + ";");
				// create new table
				StringBuilder createTable = new StringBuilder();
				createTable.append(sql.substring(0, sql.length() - 1));
				createTable.append(", ");
				createTable.append(columnName);
				createTable.append(" ");
				createTable.append(columnType);
				createTable.append(");");
				db.execSQL(createTable.toString());
				// restore data
				String columnData = sql.substring(sql.indexOf("(") + 1);
				if (columnData.length() > 0) {
					StringBuilder insertStmt = new StringBuilder();
					insertStmt.append("insert into ");
					insertStmt.append(tableName);
					insertStmt.append(" select ");
					// interate over columns
					int previousColumn = 0;
					insertStmt.append(columnData.substring(previousColumn, columnData.indexOf(" ")));
					insertStmt.append(",");
					while ((previousColumn = columnData.indexOf(",", previousColumn)) != -1) {
						previousColumn += 2;
						insertStmt.append(columnData.substring(previousColumn, (columnData.indexOf(" ", previousColumn) - previousColumn) + previousColumn));
						insertStmt.append(",");
					}
					// add new column value
					if (quotedValue) {
						insertStmt.append("'");
						insertStmt.append(columnValue);
						insertStmt.append("'");
					} else {
						insertStmt.append(columnValue);
					}
					insertStmt.append(" from ");
					insertStmt.append(tableName);
					insertStmt.append("_bkp;");
					db.execSQL(insertStmt.toString());
					success = true;
				}
				db.execSQL("drop table if exists " + tableName + "_bkp;");
			}
			sqlite_master.close();
			return success;
		}
	}	

}
