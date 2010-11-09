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

import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import static com.piusvelte.sonet.SonetDatabaseHelper.USERNAME;
import static com.piusvelte.sonet.SonetDatabaseHelper.SECRET;
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TOKEN;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.SonetDatabaseHelper.EXPIRY;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIMEZONE;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.HASBUTTONS;
import static com.piusvelte.sonet.SonetDatabaseHelper.INTERVAL;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGE_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGE_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIME24HR;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIME_COLOR;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class Settings extends PreferenceActivity {
	private SharedPreferences mSharedPreferences;
	private Preference mHeadBackground;
	private Preference mHeadText;
	private Preference mBodyBackground;
	private Preference mBodyText;
	private Preference mFriendText;
	private Preference mCreatedText;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private SonetDatabaseHelper mSonetDatabaseHelper;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		SonetDatabaseHelper mSonetDatabaseHelper = new SonetDatabaseHelper(this);
		getPreferenceManager().setSharedPreferencesName(getString(R.string.key_preferences));
		addPreferencesFromResource(R.xml.preferences);
		PreferenceScreen prefSet = getPreferenceScreen();
		mHeadBackground = prefSet.findPreference(getString(R.string.key_head_background));
		mHeadText = prefSet.findPreference(getString(R.string.key_head_text));
		mBodyBackground = prefSet.findPreference(getString(R.string.key_body_background));
		mBodyText = prefSet.findPreference(getString(R.string.key_body_text));
		mFriendText = prefSet.findPreference(getString(R.string.key_friend_text));
		mCreatedText = prefSet.findPreference(getString(R.string.key_created_text));
		mSharedPreferences = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
	}

	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == mHeadBackground) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadBackgroundColorListener, readHeadBackgroundColor());
			cp.show();
		} else if (preference == mHeadText) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadTextColorListener, readHeadTextColor());
			cp.show();
		} else if (preference == mBodyBackground) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyBackgroundColorListener, readBodyBackgroundColor());
			cp.show();
		} else if (preference == mBodyText) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyTextColorListener, readBodyTextColor());
			cp.show();
		} else if (preference == mFriendText) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mFriendTextColorListener, readFriendTextColor());
			cp.show();
		} else if (preference == mCreatedText) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mCreatedTextColorListener, readCreatedTextColor());
			cp.show();
		}
		return true;
	}

	private int readHeadBackgroundColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_head_background), getString(R.string.default_head_background)));
	}
	ColorPickerDialog.OnColorChangedListener mHeadBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
//			Editor spe = mSharedPreferences.edit();
//			spe.putString(getResources().getString(R.string.key_head_background), Integer.toString(color));
//			spe.commit();
			SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(BUTTONS_BG_COLOR, color);
			db.update(TABLE_WIDGETS, values, WIDGET + "=" + mAppWidgetId, null);
			db.close();
		}

		public void colorUpdate(int color) {
		}
	};

	private int readHeadTextColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_head_text), getString(R.string.default_head_text)));
	}
	ColorPickerDialog.OnColorChangedListener mHeadTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
//			Editor spe = mSharedPreferences.edit();
//			spe.putString(getResources().getString(R.string.key_head_text), Integer.toString(color));
//			spe.commit();
			SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(BUTTONS_COLOR, color);
			db.update(TABLE_WIDGETS, values, WIDGET + "=" + mAppWidgetId, null);
			db.close();
		}

		public void colorUpdate(int color) {
		}
	};

	private int readBodyBackgroundColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_body_background), getString(R.string.default_body_background)));
	}
	ColorPickerDialog.OnColorChangedListener mBodyBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
//			Editor spe = mSharedPreferences.edit();
//			spe.putString(getResources().getString(R.string.key_body_background), Integer.toString(color));
//			spe.commit();
			SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(MESSAGE_BG_COLOR, color);
			db.update(TABLE_WIDGETS, values, WIDGET + "=" + mAppWidgetId, null);
			db.close();
		}

		public void colorUpdate(int color) {
		}
	};

	private int readBodyTextColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_body_text), getString(R.string.default_body_text)));
	}
	ColorPickerDialog.OnColorChangedListener mBodyTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
//			Editor spe = mSharedPreferences.edit();
//			spe.putString(getResources().getString(R.string.key_body_text), Integer.toString(color));
//			spe.commit();
			SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(MESSAGE_COLOR, color);
			db.update(TABLE_WIDGETS, values, WIDGET + "=" + mAppWidgetId, null);
			db.close();
		}

		public void colorUpdate(int color) {
		}
	};

	private int readFriendTextColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_friend_text), getString(R.string.default_friend_text)));
	}
	ColorPickerDialog.OnColorChangedListener mFriendTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
//			Editor spe = mSharedPreferences.edit();
//			spe.putString(getResources().getString(R.string.key_friend_text), Integer.toString(color));
//			spe.commit();
			SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(FRIEND_COLOR, color);
			db.update(TABLE_WIDGETS, values, WIDGET + "=" + mAppWidgetId, null);
			db.close();
		}

		public void colorUpdate(int color) {
		}
	};

	private int readCreatedTextColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_created_text), getString(R.string.default_created_text)));
	}
	ColorPickerDialog.OnColorChangedListener mCreatedTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
//			Editor spe = mSharedPreferences.edit();
//			spe.putString(getResources().getString(R.string.key_created_text), Integer.toString(color));
//			spe.commit();
			SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(TIME_COLOR, color);
			db.update(TABLE_WIDGETS, values, WIDGET + "=" + mAppWidgetId, null);
			db.close();
		}

		public void colorUpdate(int color) {
		}
	};

}
