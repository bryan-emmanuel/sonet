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

import com.piusvelte.eidos.Eidos;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.StatusImages;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.WidgetAccountsView;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;
import com.piusvelte.sonet.util.DatabaseUtils;

import java.util.HashMap;

public class SonetProvider extends ContentProvider {

    private static final String TAG = "SonetProvider";

    public static final String AUTHORITY = "com.piusvelte.sonet.SonetProvider";
    public static final String PRO_AUTHORITY = "com.piusvelte.sonetpro.SonetProvider";

    private static final UriMatcher sUriMatcher;

    private static final int ACCOUNTS = 0;
    private static final int WIDGETS = 1;
    private static final int STATUSES = 2;
    public static final int STATUSES_STYLES = 3;
    private static final int STATUSES_STYLES_WIDGET = 4;
    private static final int ENTITIES = 5;
    private static final int WIDGET_ACCOUNTS = 6;
    private static final int WIDGET_ACCOUNTS_VIEW = 7;
    public static final int NOTIFICATIONS = 8;
    protected static final int WIDGETS_SETTINGS = 9;
    protected static final int DISTINCT_WIDGETS_SETTINGS = 10;
    protected static final int STATUS_LINKS = 11;
    protected static final int STATUS_IMAGES = 12;

    protected static final String DATABASE_NAME = "sonet.db";
    private static final int DATABASE_VERSION = 28;

    private static HashMap<String, String> accountsProjectionMap;

    private static HashMap<String, String> widgetsProjectionMap;

    private static HashMap<String, String> statusesProjectionMap;

    private static HashMap<String, String> statuses_stylesProjectionMap;

    private static HashMap<String, String> entitiesProjectionMap;

    private static HashMap<String, String> widget_accountsProjectionMap;

    private static HashMap<String, String> widget_accounts_viewProjectionMap;

    private static HashMap<String, String> notificationsProjectionMap;

    private static final String VIEW_DISTINCT_WIDGETS_SETTINGS = "distinct_widgets_settings";

    private static HashMap<String, String> status_linksProjectionMap;

    private static HashMap<String, String> status_imagesProjectionMap;

    private DatabaseHelper mDatabaseHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(AUTHORITY, Accounts.TABLE, ACCOUNTS);
        sUriMatcher.addURI(PRO_AUTHORITY, Accounts.TABLE, ACCOUNTS);

        accountsProjectionMap = new HashMap<>();
        accountsProjectionMap.put(Accounts._ID, Accounts._ID);
        accountsProjectionMap.put(Accounts.USERNAME, Accounts.USERNAME);
        accountsProjectionMap.put(Accounts.TOKEN, Accounts.TOKEN);
        accountsProjectionMap.put(Accounts.SECRET, Accounts.SECRET);
        accountsProjectionMap.put(Accounts.SERVICE, Accounts.SERVICE);
        accountsProjectionMap.put(Accounts.EXPIRY, Accounts.EXPIRY);
        accountsProjectionMap.put(Accounts.SID, Accounts.SID);

        sUriMatcher.addURI(AUTHORITY, WidgetAccounts.TABLE, WIDGET_ACCOUNTS);
        sUriMatcher.addURI(PRO_AUTHORITY, WidgetAccounts.TABLE, WIDGET_ACCOUNTS);

        widget_accountsProjectionMap = new HashMap<>();
        widget_accountsProjectionMap.put(WidgetAccounts._ID, WidgetAccounts._ID);
        widget_accountsProjectionMap.put(WidgetAccounts.ACCOUNT, WidgetAccounts.ACCOUNT);
        widget_accountsProjectionMap.put(WidgetAccounts.WIDGET, WidgetAccounts.WIDGET);

        sUriMatcher.addURI(AUTHORITY, WidgetAccountsView.VIEW, WIDGET_ACCOUNTS_VIEW);
        sUriMatcher.addURI(PRO_AUTHORITY, WidgetAccountsView.VIEW, WIDGET_ACCOUNTS_VIEW);

        widget_accounts_viewProjectionMap = new HashMap<>();
        widget_accounts_viewProjectionMap.put(WidgetAccountsView._ID, WidgetAccountsView._ID);
        widget_accounts_viewProjectionMap.put(WidgetAccountsView.ACCOUNT, WidgetAccountsView.ACCOUNT);
        widget_accounts_viewProjectionMap.put(WidgetAccountsView.WIDGET, WidgetAccountsView.WIDGET);
        widget_accounts_viewProjectionMap.put(WidgetAccountsView.USERNAME, WidgetAccountsView.USERNAME);
        widget_accounts_viewProjectionMap.put(WidgetAccountsView.TOKEN, WidgetAccountsView.TOKEN);
        widget_accounts_viewProjectionMap.put(WidgetAccountsView.SECRET, WidgetAccountsView.SECRET);
        widget_accounts_viewProjectionMap.put(WidgetAccountsView.SERVICE, WidgetAccountsView.SERVICE);
        widget_accounts_viewProjectionMap.put(WidgetAccountsView.EXPIRY, WidgetAccountsView.EXPIRY);
        widget_accounts_viewProjectionMap.put(WidgetAccountsView.SID, WidgetAccountsView.SID);

        sUriMatcher.addURI(AUTHORITY, Widgets.TABLE, WIDGETS);
        sUriMatcher.addURI(PRO_AUTHORITY, Widgets.TABLE, WIDGETS);

        widgetsProjectionMap = new HashMap<>();
        widgetsProjectionMap.put(Widgets._ID, Widgets._ID);
        widgetsProjectionMap.put(Widgets.WIDGET, Widgets.WIDGET);
        widgetsProjectionMap.put(Widgets.INTERVAL, Widgets.INTERVAL);
        widgetsProjectionMap.put(Widgets.TIME24HR, Widgets.TIME24HR);
        widgetsProjectionMap.put(Widgets.ACCOUNT, Widgets.ACCOUNT);
        widgetsProjectionMap.put(Widgets.BACKGROUND_UPDATE, Widgets.BACKGROUND_UPDATE);
        widgetsProjectionMap.put(Widgets.SOUND, Widgets.SOUND);
        widgetsProjectionMap.put(Widgets.VIBRATE, Widgets.VIBRATE);
        widgetsProjectionMap.put(Widgets.LIGHTS, Widgets.LIGHTS);
        widgetsProjectionMap.put(Widgets.INSTANT_UPLOAD, Widgets.INSTANT_UPLOAD);

        sUriMatcher.addURI(AUTHORITY, Statuses.TABLE, STATUSES);
        sUriMatcher.addURI(PRO_AUTHORITY, Statuses.TABLE, STATUSES);

        statusesProjectionMap = new HashMap<>();
        statusesProjectionMap.put(Statuses._ID, Statuses._ID);
        statusesProjectionMap.put(Statuses.CREATED, Statuses.CREATED);
        statusesProjectionMap.put(Statuses.MESSAGE, Statuses.MESSAGE);
        statusesProjectionMap.put(Statuses.SERVICE, Statuses.SERVICE);
        statusesProjectionMap.put(Statuses.CREATEDTEXT, Statuses.CREATEDTEXT);
        statusesProjectionMap.put(Statuses.WIDGET, Statuses.WIDGET);
        statusesProjectionMap.put(Statuses.ACCOUNT, Statuses.ACCOUNT);
        statusesProjectionMap.put(Statuses.SID, Statuses.SID);
        statusesProjectionMap.put(Statuses.ENTITY, Statuses.ENTITY);

        sUriMatcher.addURI(AUTHORITY, StatusesStyles.VIEW, STATUSES_STYLES);
        sUriMatcher.addURI(PRO_AUTHORITY, StatusesStyles.VIEW, STATUSES_STYLES);
        sUriMatcher.addURI(AUTHORITY, StatusesStyles.VIEW + "/*", STATUSES_STYLES_WIDGET);
        sUriMatcher.addURI(PRO_AUTHORITY, StatusesStyles.VIEW + "/*", STATUSES_STYLES_WIDGET);

        statuses_stylesProjectionMap = new HashMap<>();
        statuses_stylesProjectionMap.put(StatusesStyles._ID, StatusesStyles._ID);
        statuses_stylesProjectionMap.put(StatusesStyles.CREATED, StatusesStyles.CREATED);
        statuses_stylesProjectionMap.put(StatusesStyles.FRIEND, StatusesStyles.FRIEND);
        statuses_stylesProjectionMap.put(StatusesStyles.MESSAGE, StatusesStyles.MESSAGE);
        statuses_stylesProjectionMap.put(StatusesStyles.SERVICE, StatusesStyles.SERVICE);
        statuses_stylesProjectionMap.put(StatusesStyles.CREATEDTEXT, StatusesStyles.CREATEDTEXT);
        statuses_stylesProjectionMap.put(StatusesStyles.WIDGET, StatusesStyles.WIDGET);
        statuses_stylesProjectionMap.put(StatusesStyles.ACCOUNT, StatusesStyles.ACCOUNT);
        statuses_stylesProjectionMap.put(StatusesStyles.SID, StatusesStyles.SID);
        statuses_stylesProjectionMap.put(StatusesStyles.ENTITY, StatusesStyles.ENTITY);
        statuses_stylesProjectionMap.put(StatusesStyles.ESID, StatusesStyles.ESID);
        statuses_stylesProjectionMap.put(StatusesStyles.IMAGE_URL, StatusesStyles.IMAGE_URL);
        statuses_stylesProjectionMap.put(StatusesStyles.PROFILE_URL, StatusesStyles.PROFILE_URL);

        sUriMatcher.addURI(AUTHORITY, Entity.TABLE, ENTITIES);
        sUriMatcher.addURI(PRO_AUTHORITY, Entity.TABLE, ENTITIES);

        entitiesProjectionMap = new HashMap<>();
        entitiesProjectionMap.put(Entity._ID, Entity._ID);
        entitiesProjectionMap.put(Entity.ESID, Entity.ESID);
        entitiesProjectionMap.put(Entity.FRIEND, Entity.FRIEND);
        entitiesProjectionMap.put(Entity.ACCOUNT, Entity.ACCOUNT);
        entitiesProjectionMap.put(Entity.PROFILE_URL, Entity.PROFILE_URL);

        sUriMatcher.addURI(AUTHORITY, Notifications.TABLE, NOTIFICATIONS);
        sUriMatcher.addURI(PRO_AUTHORITY, Notifications.TABLE, NOTIFICATIONS);
        notificationsProjectionMap = new HashMap<>();
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

        sUriMatcher.addURI(AUTHORITY, WidgetsSettings.VIEW, WIDGETS_SETTINGS);
        sUriMatcher.addURI(PRO_AUTHORITY, WidgetsSettings.VIEW, WIDGETS_SETTINGS);

        sUriMatcher.addURI(AUTHORITY, VIEW_DISTINCT_WIDGETS_SETTINGS, DISTINCT_WIDGETS_SETTINGS);
        sUriMatcher.addURI(PRO_AUTHORITY, VIEW_DISTINCT_WIDGETS_SETTINGS, DISTINCT_WIDGETS_SETTINGS);

        sUriMatcher.addURI(AUTHORITY, StatusLinks.TABLE, STATUS_LINKS);
        sUriMatcher.addURI(PRO_AUTHORITY, StatusLinks.TABLE, STATUS_LINKS);
        status_linksProjectionMap = new HashMap<>();
        status_linksProjectionMap.put(StatusLinks._ID, StatusLinks._ID);
        status_linksProjectionMap.put(StatusLinks.STATUS_ID, StatusLinks.STATUS_ID);
        status_linksProjectionMap.put(StatusLinks.LINK_URI, StatusLinks.LINK_URI);
        status_linksProjectionMap.put(StatusLinks.LINK_TYPE, StatusLinks.LINK_TYPE);

        sUriMatcher.addURI(AUTHORITY, StatusImages.TABLE, STATUS_IMAGES);
        sUriMatcher.addURI(PRO_AUTHORITY, StatusImages.TABLE, STATUS_IMAGES);
        status_imagesProjectionMap = new HashMap<>();
        status_imagesProjectionMap.put(StatusImages._ID, StatusImages._ID);
        status_imagesProjectionMap.put(StatusImages.STATUS_ID, StatusImages.STATUS_ID);
        status_imagesProjectionMap.put(StatusImages.URL, StatusImages.URL);
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
                return WidgetAccounts.CONTENT_TYPE;
            case WIDGET_ACCOUNTS_VIEW:
                return WidgetAccountsView.CONTENT_TYPE;
            case WIDGETS:
                return Widgets.CONTENT_TYPE;
            case STATUSES:
                return Statuses.CONTENT_TYPE;
            case STATUSES_STYLES:
                return StatusesStyles.CONTENT_TYPE;
            case STATUSES_STYLES_WIDGET:
                return StatusesStyles.CONTENT_TYPE;
            case ENTITIES:
                return Entity.CONTENT_TYPE;
            case NOTIFICATIONS:
                return Notifications.CONTENT_TYPE;
            case WIDGETS_SETTINGS:
                return WidgetsSettings.CONTENT_TYPE;
            case DISTINCT_WIDGETS_SETTINGS:
                return WidgetsSettings.CONTENT_TYPE;
            case STATUS_LINKS:
                return StatusLinks.CONTENT_TYPE;
            case STATUS_IMAGES:
                return StatusImages.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String whereClause, String[] whereArgs) {
        SQLiteDatabase db;
        synchronized (Eidos.DatabaseLock) {
            db = mDatabaseHelper.getWritableDatabase();
        }
        int count;
        switch (sUriMatcher.match(uri)) {
            case ACCOUNTS:
                synchronized (Eidos.DatabaseLock) {
                    count = db.delete(Accounts.TABLE, whereClause, whereArgs);
                }
                break;

            case WIDGET_ACCOUNTS:
                synchronized (Eidos.DatabaseLock) {
                    count = db.delete(WidgetAccounts.TABLE, whereClause, whereArgs);
                }
                break;

            case WIDGETS:
                count = db.delete(Widgets.TABLE, whereClause, whereArgs);
                break;

            case STATUSES:
                synchronized (Eidos.DatabaseLock) {
                    count = db.delete(Statuses.TABLE, whereClause, whereArgs);
                }
                break;

            case ENTITIES:
                synchronized (Eidos.DatabaseLock) {
                    count = db.delete(Entity.TABLE, whereClause, whereArgs);
                }
                break;

            case NOTIFICATIONS:
                synchronized (Eidos.DatabaseLock) {
                    count = db.delete(Notifications.TABLE, whereClause, whereArgs);
                }
                break;

            case WIDGETS_SETTINGS:
                synchronized (Eidos.DatabaseLock) {
                    count = db.delete(WidgetsSettings.VIEW, whereClause, whereArgs);
                }
                break;

            case STATUS_LINKS:
                synchronized (Eidos.DatabaseLock) {
                    count = db.delete(StatusLinks.TABLE, whereClause, whereArgs);
                }
                break;

            case STATUS_IMAGES:
                synchronized (Eidos.DatabaseLock) {
                    count = db.delete(StatusImages.TABLE, whereClause, whereArgs);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db;

        synchronized (Eidos.DatabaseLock) {
            db = mDatabaseHelper.getWritableDatabase();
        }

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

                synchronized (Eidos.DatabaseLock) {
                    rowId = db.insert(Accounts.TABLE, Accounts._ID, values);
                }

                returnUri = ContentUris.withAppendedId(Accounts.getContentUri(getContext()), rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                break;

            case WIDGET_ACCOUNTS:
                synchronized (Eidos.DatabaseLock) {
                    rowId = db.insert(WidgetAccounts.TABLE, WidgetAccounts._ID, values);
                }

                returnUri = ContentUris.withAppendedId(WidgetAccounts.getContentUri(getContext()), rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                break;

            case WIDGETS:
                synchronized (Eidos.DatabaseLock) {
                    rowId = db.insert(Widgets.TABLE, Widgets._ID, values);
                }

                returnUri = ContentUris.withAppendedId(Widgets.getContentUri(getContext()), rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                break;

            case STATUSES:
                // encrypt the data
                sonetCrypto = SonetCrypto.getInstance(getContext());

                if (values.containsKey(Statuses.SID)) {
                    values.put(Statuses.SID, sonetCrypto.Encrypt(values.getAsString(Statuses.SID)));
                }

                synchronized (Eidos.DatabaseLock) {
                    rowId = db.insert(Statuses.TABLE, Accounts._ID, values);
                }

                returnUri = ContentUris.withAppendedId(Accounts.getContentUri(getContext()), rowId);
                // many statuses will be inserted at once, so don't trigger a refresh for each one
                //			getContext().getContentResolver().notifyChange(returnUri, null);
                break;

            case ENTITIES:
                // encrypt the data
                sonetCrypto = SonetCrypto.getInstance(getContext());

                if (values.containsKey(Entity.ESID)) {
                    values.put(Entity.ESID, sonetCrypto.Encrypt(values.getAsString(Entity.ESID)));
                }

                synchronized (Eidos.DatabaseLock) {
                    rowId = db.insert(Entity.TABLE, Entity._ID, values);
                }

                returnUri = ContentUris.withAppendedId(Entity.getContentUri(getContext()), rowId);
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

                synchronized (Eidos.DatabaseLock) {
                    rowId = db.insert(Notifications.TABLE, Notifications._ID, values);
                }
                returnUri = ContentUris.withAppendedId(Notifications.getContentUri(getContext()), rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                break;

            case WIDGETS_SETTINGS:
                synchronized (Eidos.DatabaseLock) {
                    rowId = db.insert(WidgetsSettings.VIEW, WidgetsSettings._ID, values);
                }

                returnUri = ContentUris.withAppendedId(WidgetsSettings.getContentUri(getContext()), rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                break;

            case STATUS_LINKS:
                synchronized (Eidos.DatabaseLock) {
                    rowId = db.insert(StatusLinks.TABLE, StatusLinks._ID, values);
                }

                returnUri = ContentUris.withAppendedId(StatusLinks.getContentUri(getContext()), rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                break;

            case STATUS_IMAGES:
                synchronized (Eidos.DatabaseLock) {
                    rowId = db.insert(StatusImages.TABLE, StatusImages._ID, values);
                }

                returnUri = ContentUris.withAppendedId(StatusImages.getContentUri(getContext()), rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
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
            case ACCOUNTS:
                qb.setTables(Accounts.TABLE);
                qb.setProjectionMap(accountsProjectionMap);
                break;

            case WIDGET_ACCOUNTS:
                qb.setTables(WidgetAccounts.TABLE);
                qb.setProjectionMap(widget_accountsProjectionMap);
                break;

            case WIDGET_ACCOUNTS_VIEW:
                qb.setTables(WidgetAccountsView.VIEW);
                qb.setProjectionMap(widget_accounts_viewProjectionMap);
                break;

            case WIDGETS:
                qb.setTables(Widgets.TABLE);
                qb.setProjectionMap(widgetsProjectionMap);
                break;

            case STATUSES:
                qb.setTables(Statuses.TABLE);
                qb.setProjectionMap(statusesProjectionMap);
                break;

            case STATUSES_STYLES:
                qb.setTables(StatusesStyles.VIEW);
                qb.setProjectionMap(statuses_stylesProjectionMap);
                break;

            case STATUSES_STYLES_WIDGET:
                qb.setTables(StatusesStyles.VIEW);
                qb.setProjectionMap(statuses_stylesProjectionMap);

                if ((selection == null) || (selectionArgs == null)) {
                    selection = StatusesStyles.WIDGET + "=?";
                    selectionArgs = new String[] { uri.getLastPathSegment() };
                }

                break;

            case ENTITIES:
                qb.setTables(Entity.TABLE);
                qb.setProjectionMap(entitiesProjectionMap);
                break;

            case NOTIFICATIONS:
                qb.setTables(Notifications.TABLE);
                qb.setProjectionMap(notificationsProjectionMap);
                break;

            case WIDGETS_SETTINGS:
                qb.setTables(WidgetsSettings.VIEW);
                qb.setProjectionMap(widgetsProjectionMap);
                break;

            case DISTINCT_WIDGETS_SETTINGS:
                qb.setTables(WidgetsSettings.VIEW);
                qb.setProjectionMap(widgetsProjectionMap);
                qb.setDistinct(true);
                break;

            case STATUS_LINKS:
                qb.setTables(StatusLinks.TABLE);
                qb.setProjectionMap(status_linksProjectionMap);
                break;

            case STATUS_IMAGES:
                qb.setTables(StatusImages.TABLE);
                qb.setProjectionMap(status_imagesProjectionMap);
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
        SQLiteDatabase db;

        synchronized (Eidos.DatabaseLock) {
            db = mDatabaseHelper.getWritableDatabase();
        }

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

                synchronized (Eidos.DatabaseLock) {
                    count = db.update(Accounts.TABLE, values, selection, selectionArgs);
                }
                break;

            case WIDGET_ACCOUNTS:
                synchronized (Eidos.DatabaseLock) {
                    count = db.update(WidgetAccounts.TABLE, values, selection, selectionArgs);
                }
                break;

            case WIDGETS:
                synchronized (Eidos.DatabaseLock) {
                    count = db.update(Widgets.TABLE, values, selection, selectionArgs);
                }
                break;

            case STATUSES:
                // encrypt the data
                sonetCrypto = SonetCrypto.getInstance(getContext());

                if (values.containsKey(Statuses.SID)) {
                    values.put(Statuses.SID, sonetCrypto.Encrypt(values.getAsString(Statuses.SID)));
                }

                synchronized (Eidos.DatabaseLock) {
                    count = db.update(Statuses.TABLE, values, selection, selectionArgs);
                }
                break;

            case ENTITIES:
                // encrypt the data
                sonetCrypto = SonetCrypto.getInstance(getContext());

                if (values.containsKey(Entity.ESID)) {
                    values.put(Entity.ESID, sonetCrypto.Encrypt(values.getAsString(Entity.ESID)));
                }

                synchronized (Eidos.DatabaseLock) {
                    count = db.update(Entity.TABLE, values, selection, selectionArgs);
                }
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

                synchronized (Eidos.DatabaseLock) {
                    count = db.update(Notifications.TABLE, values, selection, selectionArgs);
                }
                break;

            case STATUS_LINKS:
                synchronized (Eidos.DatabaseLock) {
                    count = db.update(StatusLinks.TABLE, values, selection, selectionArgs);
                }
                break;

            case STATUS_IMAGES:
                synchronized (Eidos.DatabaseLock) {
                    count = db.update(StatusImages.TABLE, values, selection, selectionArgs);
                }
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
            Accounts.createTable(db);
            WidgetAccounts.createTable(db);
            WidgetAccountsView.createView(db);
            Widgets.createTable(db);
            Statuses.createTable(db);
            Entity.createTable(db);
            Notifications.createTable(db);
            StatusLinks.createTable(db);
            StatusImages.createTable(db);
            StatusesStyles.createView(db);
            WidgetsSettings.createView(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // aliases used for addCase
            String[] aliases = new String[] { "a", "b", "c" };

            if (oldVersion < 2) {
                // add column for expiry
                DatabaseUtils.growTable(db, Accounts.TABLE, Accounts.EXPIRY, "integer", "", true);
            }

            if (oldVersion < 3) {
                // remove not null constraints as facebook uses oauth2 and doesn't require a secret, add timezone
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
                db.execSQL("create temp table " + Accounts.TABLE + "_bkp as select * from " + Accounts.TABLE + ";");
                db.execSQL("drop table if exists " + Accounts.TABLE + ";");
                db.execSQL("create table if not exists " + Accounts.TABLE
                        + " (" + Accounts._ID + " integer primary key autoincrement, "
                        + Accounts.USERNAME + " text, "
                        + Accounts.TOKEN + " text, "
                        + Accounts.SECRET + " text, "
                        + Accounts.SERVICE + " integer, "
                        + Accounts.EXPIRY + " integer, "
                        + "timezone integer);");
                db.execSQL(
                        "insert into " + Accounts.TABLE + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," +
                                Accounts.SECRET + "," + Accounts.SERVICE + "," + Accounts.EXPIRY + ",\"\" from " + Accounts.TABLE + "_bkp;");
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
            }

            if (oldVersion < 4) {
                // add column for widget
                DatabaseUtils.growTable(db, Accounts.TABLE, "widget", "integer", "", true);
                // move preferences to db
                db.execSQL("create table if not exists " + Widgets.TABLE
                        + " (" + Widgets._ID + " integer primary key autoincrement, "
                        + Widgets.WIDGET + " integer, "
                        + Widgets.INTERVAL + " integer, "
                        + Widgets.TIME24HR + " integer);");
            }

            if (oldVersion < 5) {
                // cache for statuses
                db.execSQL("create table if not exists " + Statuses.TABLE
                        + " (" + Statuses._ID + " integer primary key autoincrement, "
                        + Statuses.CREATED + " integer, "
                        + "link text, "
                        + "friend text, "
                        + "profile blob, "
                        + Statuses.MESSAGE + " text, "
                        + Statuses.SERVICE + " integer, "
                        + Statuses.CREATEDTEXT + " text, "
                        + Statuses.WIDGET + " integer);");
            }

            if (oldVersion < 6) {
                // NO-OP textsize columns removed in version 28
            }

            if (oldVersion < 7) {
                // add column for account to handle account specific widget settings
                DatabaseUtils.growTable(db, Widgets.TABLE, Widgets.ACCOUNT, "integer", Long.toString(Sonet.INVALID_ACCOUNT_ID), false);
                // add column for status background and rename createdText > createdtext
                db.execSQL("create temp table " + Statuses.TABLE + "_bkp as select * from " + Statuses.TABLE + ";");
                db.execSQL("drop table if exists " + Statuses.TABLE + ";");
                db.execSQL("create table if not exists " + Statuses.TABLE
                        + " (" + Statuses._ID + " integer primary key autoincrement, "
                        + Statuses.CREATED + " integer, "
                        + "link text, "
                        + "friend text, "
                        + "profile blob, "
                        + Statuses.MESSAGE + " text, "
                        + Statuses.SERVICE + " integer, "
                        + Statuses.CREATEDTEXT + " text, "
                        + Statuses.WIDGET + " integer, "
                        + Statuses.ACCOUNT + " integer);");
                db.execSQL("insert into " + Statuses.TABLE
                        + " select "
                        + Statuses._ID + ","
                        + Statuses.CREATED + ","
                        + "link,"
                        + "profile,"
                        + Statuses.MESSAGE + ","
                        + Statuses.SERVICE + ","
                        + "createdText,"
                        + Statuses.WIDGET + ","
                        + Sonet.INVALID_ACCOUNT_ID + ",null from " + Statuses.TABLE + "_bkp;");
                db.execSQL("drop table if exists " + Statuses.TABLE + "_bkp;");
                // create a view for the statuses and account/widget/default styles
                StatusesStyles.createView(db);
            }

            if (oldVersion < 8) {
                // change the view to be more efficient
                db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                StatusesStyles.createView(db);
            }

            if (oldVersion < 9) {
                // support additional timezones, with partial hour increments
                // change timezone column from integer to real
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
                db.execSQL("create temp table " + Accounts.TABLE + "_bkp as select * from " + Accounts.TABLE + ";");
                db.execSQL("drop table if exists " + Accounts.TABLE + ";");
                db.execSQL("create table if not exists " + Accounts.TABLE
                        + " (" + Accounts._ID + " integer primary key autoincrement, "
                        + Accounts.USERNAME + " text, "
                        + Accounts.TOKEN + " text, "
                        + Accounts.SECRET + " text, "
                        + Accounts.SERVICE + " integer, "
                        + Accounts.EXPIRY + " integer, "
                        + "timezone real, "
                        + "widget integer);");
                db.execSQL("insert into " + Accounts.TABLE
                        + " select "
                        + Accounts._ID + ","
                        + Accounts.USERNAME + ","
                        + Accounts.TOKEN + ","
                        + Accounts.SECRET + ","
                        + Accounts.SERVICE + ","
                        + Accounts.EXPIRY + ","
                        + "timezone,"
                        + "widget from " + Accounts.TABLE + "_bkp;");
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
            }

            if (oldVersion < 10) {
                // NO-OP icons removed in version 28
            }

            if (oldVersion < 11) {
                // using device timezone, doesn't need to be stored now
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
                db.execSQL("create temp table " + Accounts.TABLE + "_bkp as select * from " + Accounts.TABLE + ";");
                db.execSQL("drop table if exists " + Accounts.TABLE + ";");
                db.execSQL("create table if not exists " + Accounts.TABLE
                        + " (" + Accounts._ID + " integer primary key autoincrement, "
                        + Accounts.USERNAME + " text, "
                        + Accounts.TOKEN + " text, "
                        + Accounts.SECRET + " text, "
                        + Accounts.SERVICE + " integer, "
                        + Accounts.EXPIRY + " integer, "
                        + "widget integer);");
                db.execSQL("insert into " + Accounts.TABLE
                        + " select "
                        + Accounts._ID + ","
                        + Accounts.USERNAME + ","
                        + Accounts.TOKEN + ","
                        + Accounts.SECRET + ","
                        + Accounts.SERVICE + ","
                        + Accounts.EXPIRY + ","
                        + "widget from " + Accounts.TABLE + "_bkp;");
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
            }

            if (oldVersion < 12) {
                // store the service id's for posting and linking
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
                db.execSQL("create temp table " + Accounts.TABLE + "_bkp as select * from " + Accounts.TABLE + ";");
                db.execSQL("drop table if exists " + Accounts.TABLE + ";");
                db.execSQL("create table if not exists " + Accounts.TABLE
                        + " (" + Accounts._ID + " integer primary key autoincrement, "
                        + Accounts.USERNAME + " text, "
                        + Accounts.TOKEN + " text, "
                        + Accounts.SECRET + " text, "
                        + Accounts.SERVICE + " integer, "
                        + Accounts.EXPIRY + " integer, "
                        + "widget integer, "
                        + Accounts.SID + " integer);");
                db.execSQL("insert into " + Accounts.TABLE
                        + " select "
                        + Accounts._ID + ","
                        + Accounts.USERNAME + ","
                        + Accounts.TOKEN + ","
                        + Accounts.SECRET + ","
                        + Accounts.SERVICE + ","
                        + Accounts.EXPIRY + ","
                        + "widget,\"\" from " + Accounts.TABLE + "_bkp;");
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
                db.execSQL("drop table if exists " + Statuses.TABLE + "_bkp;");
                db.execSQL("create temp table " + Statuses.TABLE + "_bkp as select * from " + Statuses.TABLE + ";");
                DatabaseUtils.growTable(db, Statuses.TABLE, Statuses.SID, "integer", "", true);
                DatabaseUtils.growTable(db, Statuses.TABLE, Statuses.ENTITY, "integer", "", true);
                db.execSQL("create table if not exists " + Entity.TABLE
                        + " (" + Entity._ID + " integer primary key autoincrement, "
                        + Entity.FRIEND + " text, "
                        + Entity.ACCOUNT + " integer, "
                        + Entity.ESID + " text);");
                Cursor from_bkp = db.query(Statuses.TABLE + "_bkp", new String[] { Statuses._ID, "friend", "profile" }, null, null, null, null, null);

                if (from_bkp.moveToFirst()) {
                    int iid = from_bkp.getColumnIndex(Statuses._ID);
                    int ifriend = from_bkp.getColumnIndex("friend");

                    while (!from_bkp.isAfterLast()) {
                        ContentValues values = new ContentValues();
                        values.put(Entity.FRIEND, from_bkp.getString(ifriend));
                        int id = (int) db.insert(Entity.TABLE, Entity._ID, values);
                        values = new ContentValues();
                        values.put(Statuses.ENTITY, id);
                        db.update(Statuses.TABLE, values, Statuses._ID + "=?", new String[] { Integer.toString(from_bkp.getInt(iid)) });
                        from_bkp.moveToNext();
                    }
                }

                from_bkp.close();
                db.execSQL("drop table if exists " + Statuses.TABLE + "_bkp;");
                db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                StatusesStyles.createView(db);
                // background updating option
                DatabaseUtils.growTable(db, Widgets.TABLE, Widgets.BACKGROUND_UPDATE, "integer", "1", false);
            }

            if (oldVersion < 13) {
                // NO-OP scrollable removed in version 28
            }

            if (oldVersion < 14) {
                // need to redesign the accounts table so that multiple widgets can use the same accounts
                db.execSQL("create table if not exists " + WidgetAccounts.TABLE
                        + " (" + WidgetAccounts._ID + " integer primary key autoincrement, "
                        + WidgetAccounts.ACCOUNT + " integer, "
                        + WidgetAccounts.WIDGET + " integer);");
                // migrate accounts over to widget_accounts
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
                db.execSQL("create temp table " + Accounts.TABLE + "_bkp as select * from " + Accounts.TABLE + ";");
                db.execSQL("drop table if exists " + Accounts.TABLE + ";");
                db.execSQL("create table if not exists " + Accounts.TABLE
                        + " (" + Accounts._ID + " integer primary key autoincrement, "
                        + Accounts.USERNAME + " text, "
                        + Accounts.TOKEN + " text, "
                        + Accounts.SECRET + " text, "
                        + Accounts.SERVICE + " integer, "
                        + Accounts.EXPIRY + " integer, "
                        + Accounts.SID + " text);");
                Cursor accounts = db.query(Accounts.TABLE + "_bkp",
                        new String[] { Accounts._ID,
                                Accounts.USERNAME,
                                Accounts.TOKEN,
                                Accounts.SECRET,
                                Accounts.SERVICE,
                                Accounts.EXPIRY,
                                "widget",
                                Accounts.SID },
                        null,
                        null,
                        null,
                        null,
                        null);

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
                        values.put(WidgetAccounts.ACCOUNT, accounts.getLong(iid));
                        values.put(WidgetAccounts.WIDGET, accounts.getInt(iwidget));
                        db.insert(WidgetAccounts.TABLE, WidgetAccounts._ID, values);
                        values.clear();
                        values.put(Accounts._ID, accounts.getLong(iid));
                        values.put(Accounts.USERNAME, accounts.getString(iusername));
                        values.put(Accounts.TOKEN, accounts.getString(itoken));
                        values.put(Accounts.SECRET, accounts.getString(isecret));
                        values.put(Accounts.SERVICE, accounts.getString(iservice));
                        values.put(Accounts.EXPIRY, accounts.getLong(iexpiry));
                        values.put(Accounts.SID, accounts.getString(isid));
                        db.insert(Accounts.TABLE, Accounts._ID, values);
                        accounts.moveToNext();
                    }
                }

                accounts.close();
                db.execSQL("drop table if exists " + Accounts.TABLE + "_bkp;");
                db.execSQL("create view if not exists " + WidgetAccountsView.VIEW + " as select "
                        + WidgetAccounts.TABLE + "." + WidgetAccounts._ID
                        + "," + WidgetAccounts.ACCOUNT
                        + "," + WidgetAccounts.WIDGET
                        + "," + Accounts.EXPIRY
                        + "," + Accounts.SECRET
                        + "," + Accounts.SERVICE
                        + "," + Accounts.SID
                        + "," + Accounts.TOKEN
                        + "," + Accounts.USERNAME
                        + " from "
                        + WidgetAccounts.TABLE
                        + "," + Accounts.TABLE
                        + " where "
                        + Accounts.TABLE + "." + Accounts._ID + "=" + WidgetAccounts.ACCOUNT
                        + ";");
            }

            if (oldVersion < 15) {
                DatabaseUtils.growTable(db, Widgets.TABLE, Widgets.SOUND, "integer", "0", false);
                DatabaseUtils.growTable(db, Widgets.TABLE, Widgets.VIBRATE, "integer", "0", false);
                DatabaseUtils.growTable(db, Widgets.TABLE, Widgets.LIGHTS, "integer", "0", false);
                // notifications
                db.execSQL("create table if not exists " + Notifications.TABLE
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
                DatabaseUtils.growTable(db, Statuses.TABLE, Statuses.FRIEND_OVERRIDE, "text", "null", false);
                db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                StatusesStyles.createView(db);
            }

            if (oldVersion < 16) {
                // create a view for the widget settings
                db.execSQL("create view if not exists " + WidgetsSettings.VIEW + " as select a."
                        + Widgets._ID + " as " + Widgets._ID
                        + ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
                        + "," + DatabaseUtils.addCase(aliases, Widgets.INTERVAL, Sonet.default_interval, Widgets.INTERVAL)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.TIME24HR, 0, Widgets.TIME24HR)
                        + ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
                        + "," + DatabaseUtils.addCase(aliases, Widgets.BACKGROUND_UPDATE, 1, Widgets.BACKGROUND_UPDATE)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.SOUND, 0, Widgets.SOUND)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.VIBRATE, 0, Widgets.VIBRATE)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.LIGHTS, 0, Widgets.LIGHTS)
                        + " from " + Widgets.TABLE + " a,"
                        + Widgets.TABLE + " b,"
                        + Widgets.TABLE + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." +
                        Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
            }

            if (oldVersion < 17) {
                // add updated column, this will clear all current notifications, to avoid duplicates
                db.execSQL("drop table if exists " + Notifications.TABLE + ";");
                db.execSQL("create table if not exists " + Notifications.TABLE
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
                db.execSQL("drop table if exists " + Statuses.TABLE + "_bkp;");
                db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                StatusesStyles.createView(db);
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
                    db.execSQL("drop table if exists " + Notifications.TABLE + ";");
                    db.execSQL("create table if not exists " + Notifications.TABLE
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
                    db.execSQL("drop table if exists " + Statuses.TABLE + "_bkp;");
                    db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                    StatusesStyles.createView(db);
                }
            }

            if (oldVersion < 19) {
                // add support for instant upload
                DatabaseUtils.growTable(db, Widgets.TABLE, Widgets.INSTANT_UPLOAD, "integer", "0", false);
                db.execSQL("drop view if exists " + WidgetsSettings.VIEW + ";");
                db.execSQL("create view if not exists " + WidgetsSettings.VIEW + " as select a."
                        + Widgets._ID + " as " + Widgets._ID
                        + ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
                        + "," + DatabaseUtils.addCase(aliases, Widgets.INTERVAL, Sonet.default_interval, Widgets.INTERVAL)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.TIME24HR, 0, Widgets.TIME24HR)
                        + ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
                        + "," + DatabaseUtils.addCase(aliases, Widgets.BACKGROUND_UPDATE, 1, Widgets.BACKGROUND_UPDATE)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.SOUND, 0, Widgets.SOUND)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.VIBRATE, 0, Widgets.VIBRATE)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.LIGHTS, 0, Widgets.LIGHTS)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.INSTANT_UPLOAD, 0, Widgets.INSTANT_UPLOAD)
                        + " from " + Widgets.TABLE + " a,"
                        + Widgets.TABLE + " b,"
                        + Widgets.TABLE + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." +
                        Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
            }

            if (oldVersion < 20) {
                // move instant upload setting from account specific to widget specific
                Cursor c = db
                        .query(Widgets.TABLE, new String[] { Widgets.WIDGET }, Widgets.ACCOUNT + "!=-1 and " + Widgets.INSTANT_UPLOAD + "=1", null,
                                null, null, null);
                if (c.moveToFirst()) {
                    while (!c.isAfterLast()) {
                        ContentValues values = new ContentValues();
                        values.put(Widgets.INSTANT_UPLOAD, 1);
                        db.update(Widgets.TABLE,
                                values,
                                Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=-1",
                                new String[] { Integer.toString(c.getInt(0)) });
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
                    db.execSQL("drop table if exists " + Notifications.TABLE + "_bkp;");
                    db.execSQL("create temp table " + Notifications.TABLE + "_bkp as select * from " + Notifications.TABLE + ";");
                    db.execSQL("drop table if exists " + Notifications.TABLE + ";");
                    db.execSQL("create table if not exists " + Notifications.TABLE
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
                    db.execSQL("insert into " + Notifications.TABLE
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
                            + "," + Notifications.CREATED + " from " + Notifications.TABLE + "_bkp;");
                    db.execSQL("drop table if exists " + Notifications.TABLE + "_bkp;");
                }
            }

            if (oldVersion < 22) {
                db.execSQL("create table if not exists " + StatusLinks.TABLE
                        + " (" + StatusLinks._ID + " integer primary key autoincrement, "
                        + StatusLinks.STATUS_ID + " integer, "
                        + StatusLinks.LINK_URI + " text, "
                        + StatusLinks.LINK_TYPE + " text);");
            }

            if (oldVersion < 23) {
                // clean up duplicate widget settings
                Cursor c = db
                        .query(Widgets.TABLE, new String[] { Widgets._ID }, Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1", null, null, null,
                                null);
                if (c.moveToFirst()) {
                    if (c.moveToNext()) {
                        while (!c.isAfterLast()) {
                            db.delete(Widgets.TABLE, Widgets._ID + "=?", new String[] { Long.toString(c.getLong(0)) });
                            c.moveToNext();
                        }
                    }
                }
                c.close();
            }

            if (oldVersion < 24) {
                db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                StatusesStyles.createView(db);
                db.execSQL("drop view if exists " + WidgetsSettings.VIEW + ";");
                db.execSQL("create view if not exists " + WidgetsSettings.VIEW + " as select a."
                        + Widgets._ID + " as " + Widgets._ID
                        + ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
                        + "," + DatabaseUtils.addCase(aliases, Widgets.INTERVAL, Sonet.default_interval, Widgets.INTERVAL)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.TIME24HR, 0, Widgets.TIME24HR)
                        + ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
                        + "," + DatabaseUtils.addCase(aliases, Widgets.BACKGROUND_UPDATE, 1, Widgets.BACKGROUND_UPDATE)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.SOUND, 0, Widgets.SOUND)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.VIBRATE, 0, Widgets.VIBRATE)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.LIGHTS, 0, Widgets.LIGHTS)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.INSTANT_UPLOAD, 0, Widgets.INSTANT_UPLOAD)
                        + " from " + Widgets.TABLE + " a,"
                        + Widgets.TABLE + " b,"
                        + Widgets.TABLE + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." +
                        Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
            }

            if (oldVersion < 25) {
                db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                StatusesStyles.createView(db);
                db.execSQL("drop view if exists " + WidgetsSettings.VIEW + ";");
                // create a view for the widget settings
                db.execSQL("create view if not exists " + WidgetsSettings.VIEW + " as select a."
                        + Widgets._ID + " as " + Widgets._ID
                        + ",a." + Widgets.WIDGET + " as " + Widgets.WIDGET
                        + "," + DatabaseUtils.addCase(aliases, Widgets.INTERVAL, Sonet.default_interval, Widgets.INTERVAL)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.TIME24HR, 0, Widgets.TIME24HR)
                        + ",a." + Widgets.ACCOUNT + " as " + Widgets.ACCOUNT
                        + "," + DatabaseUtils.addCase(aliases, Widgets.BACKGROUND_UPDATE, 1, Widgets.BACKGROUND_UPDATE)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.SOUND, 0, Widgets.SOUND)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.VIBRATE, 0, Widgets.VIBRATE)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.LIGHTS, 0, Widgets.LIGHTS)
                        + "," + DatabaseUtils.addCase(aliases, Widgets.INSTANT_UPLOAD, 0, Widgets.INSTANT_UPLOAD)
                        + " from " + Widgets.TABLE + " a,"
                        + Widgets.TABLE + " b,"
                        + Widgets.TABLE + " c WHERE b." + Widgets.WIDGET + "=a." + Widgets.WIDGET + " and b." + Widgets.ACCOUNT + "=-1 and c." +
                        Widgets.WIDGET + "=0 and c." + Widgets.ACCOUNT + "=-1;");
            }

            if (oldVersion < 26) {
                db.execSQL("drop table if exists " + StatusImages.TABLE + ";");
                db.execSQL("create table if not exists " + StatusImages.TABLE
                        + " (" + StatusImages._ID + " integer primary key autoincrement, "
                        + StatusImages.STATUS_ID + " integer);");
                db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                StatusesStyles.createView(db);
            }

            // add profile_url
            if (oldVersion < 27) {
                DatabaseUtils.growTable(db,
                        Entity.TABLE,
                        Entity.PROFILE_URL,
                        "text",
                        "null",
                        false);
                db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                StatusesStyles.createView(db);
            }

            // remove deprecated style columns everywhere
            // load images as needed by Picasso with url, instead of storing a blob
            if (oldVersion < 28) {
                // drop Entity.PROFILE
                Entity.migrateTable(db);
                // drop styles columns
                Widgets.migrateTable(db);
                db.execSQL("drop view if exists " + WidgetsSettings.VIEW + ";");
                WidgetsSettings.createView(db);
                // move to url for image, instead of blob
                StatusImages.migrateTable(db);
                Statuses.migrateTable(db);
                db.execSQL("drop view if exists " + StatusesStyles.VIEW + ";");
                StatusesStyles.createView(db);
            }
        }
    }
}
