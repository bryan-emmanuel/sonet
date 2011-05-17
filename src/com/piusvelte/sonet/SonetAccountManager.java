package com.piusvelte.sonet;

import java.util.HashMap;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.DatabaseHelper;

import static com.piusvelte.sonet.Sonet.TABLE_ACCOUNTS;

public class SonetAccountManager {
	private Context mContext;
	private DatabaseHelper mDatabaseHelper;
	protected static Uri ACCOUNTS_URI = Uri.parse("content://" + SonetProvider.AUTHORITY + "/accounts");
	protected static HashMap<String, String> accountsProjectionMap;

	static {
		accountsProjectionMap = new HashMap<String, String>();
		accountsProjectionMap.put(Accounts._ID, Accounts._ID);
		accountsProjectionMap.put(Accounts.USERNAME, Accounts.USERNAME);
		accountsProjectionMap.put(Accounts.TOKEN, Accounts.TOKEN);
		accountsProjectionMap.put(Accounts.SECRET, Accounts.SECRET);
		accountsProjectionMap.put(Accounts.SERVICE, Accounts.SERVICE);
		accountsProjectionMap.put(Accounts.EXPIRY, Accounts.EXPIRY);
		accountsProjectionMap.put(Accounts.WIDGET, Accounts.WIDGET);
		accountsProjectionMap.put(Accounts.SID, Accounts.SID);
	}
	
	protected SonetAccountManager(Context context) {
		mContext = context;
		mDatabaseHelper = new DatabaseHelper(mContext);		
	}
	
	protected void close() {
		mDatabaseHelper.close();
	}

	protected int delete(String whereClause, String[] whereArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		return db.delete(TABLE_ACCOUNTS, whereClause, whereArgs);
	}

	protected Uri insert(ContentValues values) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		return ContentUris.withAppendedId(ACCOUNTS_URI, db.insert(TABLE_ACCOUNTS, Accounts._ID, values));
	}

	protected Cursor query(String[] projection, String selection, String[] selectionArgs, String orderBy) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(TABLE_ACCOUNTS);
		qb.setProjectionMap(accountsProjectionMap);
		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		return qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
	}

	protected int update(ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		return db.update(TABLE_ACCOUNTS, values, selection, selectionArgs);
	}
}