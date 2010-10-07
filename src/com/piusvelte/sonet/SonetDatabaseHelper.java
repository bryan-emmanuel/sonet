package com.piusvelte.sonet;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SonetDatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "sonet.db";
	private static final int DATABASE_VERSION = 1;
	public static final String TABLE_ACCOUNTS = "accounts";
	public static final String _ID = "_id";
	public static final String USERNAME = "username";
	public static final String TOKEN = "token";
	public static final String SECRET = "secret";
	public static final String SERVICE = "service";
	
	public SonetDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table if not exists " + TABLE_ACCOUNTS
				+ " (" + _ID + " integer primary key autoincrement, "
				+ USERNAME + " text not null, "
				+ TOKEN + " text not null, "
				+ SECRET + " text not null, "
				+ SERVICE + " integer);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

}
