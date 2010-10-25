package com.piusvelte.sonet;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SonetDatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "sonet.db";
	private static final int DATABASE_VERSION = 3;
	public static final String TABLE_ACCOUNTS = "accounts";
	public static final String _ID = "_id";
	public static final String USERNAME = "username";
	public static final String TOKEN = "token";
	public static final String SECRET = "secret";
	public static final String SERVICE = "service";
	public static final String EXPIRY = "expiry";
	public static final String TIMEZONE = "timezone";
	
	public SonetDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table if not exists " + TABLE_ACCOUNTS
				+ " (" + _ID + " integer primary key autoincrement, "
				+ USERNAME + " text, "
				+ TOKEN + " text, "
				+ SECRET + " text, "
				+ SERVICE + " integer, "
				+ EXPIRY + " integer, "
				+ TIMEZONE + " integer);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			// add column for expiry
			db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
			db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
			db.execSQL("create table if not exists " + TABLE_ACCOUNTS
					+ " (" + _ID + " integer primary key autoincrement, "
					+ USERNAME + " text not null, "
					+ TOKEN + " text not null, "
					+ SECRET + " text not null, "
					+ SERVICE + " integer, "
					+ EXPIRY + " integer);");
			db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + _ID + "," + USERNAME + "," + TOKEN + "," + SECRET + "," + SERVICE + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
			db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
		}
		if (oldVersion < 3) {
			// remove not null constraints as facebook uses oauth2 and doesn't require a secret, add timezone
			db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
			db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
			db.execSQL("create table if not exists " + TABLE_ACCOUNTS
					+ " (" + _ID + " integer primary key autoincrement, "
					+ USERNAME + " text, "
					+ TOKEN + " text, "
					+ SECRET + " text, "
					+ SERVICE + " integer, "
					+ EXPIRY + " integer, "
					+ TIMEZONE + " integer);");
			db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + _ID + "," + USERNAME + "," + TOKEN + "," + SECRET + "," + SERVICE + "," + EXPIRY + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
			db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
		}
	}

}
