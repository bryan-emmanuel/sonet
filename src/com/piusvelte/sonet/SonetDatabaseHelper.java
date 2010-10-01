package com.piusvelte.sonet;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SonetDatabaseHelper extends SQLiteOpenHelper {
	public static final String TAG = "Sonet";
	private static final String DATABASE_NAME = "sonet.db";
	private static final int DATABASE_VERSION = 1;
	public static final String TABLE_ACCOUNTS = "accounts";
	public static final String _ID = "_id";
	public static final String USERNAME = "username";
	public static final String TOKEN = "token";
	public static final String SECRET = "secret";
	public static final String SERVICE = "service";
	public static final String TWITTER_URL_REQUEST = "https://api.twitter.com/oauth/request_token";
	public static final String TWITTER_URL_ACCESS = "https://api.twitter.com/oauth/access_token";
	public static final String TWITTER_URL_AUTHORIZE = "https://api.twitter.com/oauth/authorize";
	public static final String TWITTER_KEY = "";
	public static final String TWITTER_SECRET = "";
	public static final String FACEBOOK_KEY = "";
	public static final String FACEBOOK_SECRET = "";
	
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
