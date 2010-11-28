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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SonetDatabaseHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "sonet.db";
	private static final int DATABASE_VERSION = 5;
	public static final String TABLE_ACCOUNTS = "accounts";
	public static final String _ID = "_id";
	public static final String USERNAME = "username";
	public static final String TOKEN = "token";
	public static final String SECRET = "secret";
	public static final String SERVICE = "service";
	public static final String EXPIRY = "expiry";
	public static final String TIMEZONE = "timezone";
	public static final String WIDGET = "widget";
	public static final String TABLE_WIDGETS = "widgets";
	public static final String INTERVAL = "interval";
	public static final String HASBUTTONS = "hasbuttons";
	public static final String BUTTONS_BG_COLOR = "buttons_bg_color";
	public static final String BUTTONS_COLOR = "buttons_color";
	public static final String MESSAGES_BG_COLOR = "messages_bg_color";
	public static final String MESSAGES_COLOR = "messages_color";
	public static final String TIME24HR = "time24hr";
	public static final String FRIEND_COLOR = "friend_color";
	public static final String CREATED_COLOR = "created_color";
	public static final String TABLE_STATUSES = "statuses";
	public static final String CREATED = "created";
	public static final String LINK = "link";
	public static final String FRIEND = "friend";
	public static final String PROFILE = "profile";
	public static final String MESSAGE = "message";
	public static final String CREATEDTEXT = "createdText";
	public static final String SCROLLABLE = "scrollable";
	
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
				+ TIMEZONE + " integer, "
				+ WIDGET + " integer);");
		db.execSQL("create table if not exists " + TABLE_WIDGETS
				+ " (" + _ID + " integer primary key autoincrement, "
				+ WIDGET + " integer, "
				+ INTERVAL + " integer, "
				+ HASBUTTONS + " integer, "
				+ BUTTONS_BG_COLOR + " integer, "
				+ BUTTONS_COLOR + " integer, "
				+ FRIEND_COLOR + " integer, "
				+ CREATED_COLOR + " integer, "
				+ MESSAGES_BG_COLOR + " integer, "
				+ MESSAGES_COLOR + " integer, "
				+ TIME24HR + " integer, "
				+ SCROLLABLE + " integer);");
		db.execSQL("create table if not exists " + TABLE_STATUSES
				+ " (" + _ID + " integer primary key autoincrement, "
				+ CREATED + " integer, "
				+ LINK + " text, "
				+ FRIEND + " text, "
				+ PROFILE + " blob, "
				+ MESSAGE + " text, "
				+ SERVICE + " integer, "
				+ CREATEDTEXT + " text, "
				+ WIDGET + " integer);");
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
		if (oldVersion < 4) {
			// add column for widget
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
					+ TIMEZONE + " integer, "
					+ WIDGET + " integer);");
			db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + _ID + "," + USERNAME + "," + TOKEN + "," + SECRET + "," + SERVICE + "," + EXPIRY + "," + TIMEZONE + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
			db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			// move preferences to db
			db.execSQL("create table if not exists " + TABLE_WIDGETS
					+ " (" + _ID + " integer primary key autoincrement, "
					+ WIDGET + " integer, "
					+ INTERVAL + " integer, "
					+ HASBUTTONS + " integer, "
					+ BUTTONS_BG_COLOR + " integer, "
					+ BUTTONS_COLOR + " integer, "
					+ FRIEND_COLOR + " integer, "
					+ CREATED_COLOR + " integer, "
					+ MESSAGES_BG_COLOR + " integer, "
					+ MESSAGES_COLOR + " integer, "
					+ TIME24HR + " integer);");
		}
		if (oldVersion < 5) {
			// cache for statuses
			db.execSQL("create table if not exists " + TABLE_STATUSES
					+ " (" + _ID + " integer primary key autoincrement, "
					+ CREATED + " integer, "
					+ LINK + " text, "
					+ FRIEND + " text, "
					+ PROFILE + " blob, "
					+ MESSAGE + " text, "
					+ SERVICE + " integer, "
					+ CREATEDTEXT + " text, "
					+ WIDGET + " integer);");
			// column for scrollable
			db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
			db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
			db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
			db.execSQL("create table if not exists " + TABLE_WIDGETS
					+ " (" + _ID + " integer primary key autoincrement, "
					+ WIDGET + " integer, "
					+ INTERVAL + " integer, "
					+ HASBUTTONS + " integer, "
					+ BUTTONS_BG_COLOR + " integer, "
					+ BUTTONS_COLOR + " integer, "
					+ FRIEND_COLOR + " integer, "
					+ CREATED_COLOR + " integer, "
					+ MESSAGES_BG_COLOR + " integer, "
					+ MESSAGES_COLOR + " integer, "
					+ TIME24HR + " integer, "
					+ SCROLLABLE + " integer);");
			db.execSQL("insert into " + TABLE_WIDGETS
					+ " select "
					+ _ID + ","
					+ WIDGET + ","
					+ INTERVAL + ","
					+ HASBUTTONS + ","
					+ BUTTONS_BG_COLOR + ","
					+ BUTTONS_COLOR + ","
					+ FRIEND_COLOR + ","
					+ CREATED_COLOR + ","
					+ MESSAGES_BG_COLOR + ","
					+ MESSAGES_COLOR + ","
					+ TIME24HR + ",0 from " + TABLE_WIDGETS + "_bkp;");
			db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
		}
	}

}
