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

import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.HASBUTTONS;
import static com.piusvelte.sonet.SonetDatabaseHelper.INTERVAL;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGE_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGE_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIME24HR;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_COLOR;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class Settings extends Activity implements View.OnClickListener, DialogInterface.OnClickListener {
	private Button mInterval;
	private CheckBox mHasButtons;
	private Button mButtons_bg_color;
	private Button mButtons_color;
	private Button mMessage_bg_color;
	private Button mMessage_color;
	private Button mFriend_color;
	private Button mCreated_color;
	private CheckBox mTime24hr;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private SonetDatabaseHelper mSonetDatabaseHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Intent i = getIntent();
		if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		mSonetDatabaseHelper = new SonetDatabaseHelper(this);
		mInterval = (Button) findViewById(R.id.interval);
		mHasButtons = (CheckBox) findViewById(R.id.hasbuttons);
		mButtons_bg_color = (Button) findViewById(R.id.buttons_bg_color);
		mButtons_color = (Button) findViewById(R.id.buttons_color);
		mMessage_bg_color = (Button) findViewById(R.id.message_bg_color);
		mMessage_color = (Button) findViewById(R.id.message_color);
		mFriend_color = (Button) findViewById(R.id.friend_color);
		mCreated_color = (Button) findViewById(R.id.created_color);
		mTime24hr = (CheckBox) findViewById(R.id.time24hr);
		SQLiteDatabase db = mSonetDatabaseHelper.getReadableDatabase();
		Cursor c = db.rawQuery("select " + HASBUTTONS + "," + TIME24HR + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + mAppWidgetId, null);
		if (c.getCount() > 0) {
			mHasButtons.setChecked(c.getInt(c.getColumnIndex(HASBUTTONS)) == 0 ? false : true);
			mTime24hr.setChecked(c.getInt(c.getColumnIndex(TIME24HR)) == 0 ? false : true);
		}
		c.close();
		db.close();
		mInterval.setOnClickListener(this);
		mHasButtons.setOnCheckedChangeListener(mHasButtonsListener);
		mButtons_bg_color.setOnClickListener(this);
		mButtons_color.setOnClickListener(this);
		mMessage_bg_color.setOnClickListener(this);
		mMessage_color.setOnClickListener(this);
		mFriend_color.setOnClickListener(this);
		mCreated_color.setOnClickListener(this);
		mTime24hr.setOnCheckedChangeListener(mTime24hrListener);
	}

	private void updateDatabase(ContentValues values) {
		SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
		db.update(TABLE_WIDGETS, values, WIDGET + "=" + mAppWidgetId, null);
		db.close();		
	}
	
	private int getValue(String column, int value) {
		int color;
		SQLiteDatabase db = mSonetDatabaseHelper.getReadableDatabase();
		Cursor c = db.rawQuery("select " + column + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + mAppWidgetId, null);
		if (c.getCount() > 0) color = c.getInt(c.getColumnIndex(column));
		else color = Integer.parseInt(getString(value));
		c.close();
		db.close();
		return color;		
	}
	
	ColorPickerDialog.OnColorChangedListener mHeadBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			//			Editor spe = mSharedPreferences.edit();
			//			spe.putString(getResources().getString(R.string.key_head_background), Integer.toString(color));
			//			spe.commit();
			ContentValues values = new ContentValues();
			values.put(BUTTONS_BG_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mHeadTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			//			Editor spe = mSharedPreferences.edit();
			//			spe.putString(getResources().getString(R.string.key_head_text), Integer.toString(color));
			//			spe.commit();
			ContentValues values = new ContentValues();
			values.put(BUTTONS_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mBodyBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			//			Editor spe = mSharedPreferences.edit();
			//			spe.putString(getResources().getString(R.string.key_body_background), Integer.toString(color));
			//			spe.commit();
			ContentValues values = new ContentValues();
			values.put(MESSAGE_BG_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mBodyTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			//			Editor spe = mSharedPreferences.edit();
			//			spe.putString(getResources().getString(R.string.key_body_text), Integer.toString(color));
			//			spe.commit();
			ContentValues values = new ContentValues();
			values.put(MESSAGE_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mFriendTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			//			Editor spe = mSharedPreferences.edit();
			//			spe.putString(getResources().getString(R.string.key_friend_text), Integer.toString(color));
			//			spe.commit();
			ContentValues values = new ContentValues();
			values.put(FRIEND_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mCreatedTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			//			Editor spe = mSharedPreferences.edit();
			//			spe.putString(getResources().getString(R.string.key_created_text), Integer.toString(color));
			//			spe.commit();
			ContentValues values = new ContentValues();
			values.put(CREATED_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	CompoundButton.OnCheckedChangeListener mHasButtonsListener =
		new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			ContentValues values = new ContentValues();
			values.put(HASBUTTONS, isChecked ? 1 : 0);
			updateDatabase(values);
		}
	};

	CompoundButton.OnCheckedChangeListener mTime24hrListener =
		new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			ContentValues values = new ContentValues();
			values.put(TIME24HR, isChecked ? 1 : 0);
			updateDatabase(values);
		}
	};

	@Override
	public void onClick(View v) {
		if (v == mInterval) {
			String[] services = getResources().getStringArray(R.array.interval_entries);
			CharSequence[] items = new CharSequence[services.length];
			for (int i = 0; i < services.length; i++) items[i] = services[i];
			(new AlertDialog.Builder(this))
			.setItems(items, this)
			.setCancelable(true)
			.show();			
		} else if (v == mButtons_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadBackgroundColorListener, getValue(BUTTONS_BG_COLOR, R.string.default_buttons_bg_color));
			cp.show();
		} else if (v == mButtons_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadTextColorListener, getValue(BUTTONS_COLOR, R.string.default_buttons_color));
			cp.show();
		} else if (v == mMessage_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyBackgroundColorListener, getValue(MESSAGE_BG_COLOR, R.string.default_message_bg_color));
			cp.show();
		} else if (v == mMessage_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyTextColorListener, getValue(MESSAGE_COLOR, R.string.default_message_color));
			cp.show();
		} else if (v == mFriend_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mFriendTextColorListener, getValue(FRIEND_COLOR, R.string.default_friend_color));
			cp.show();
		} else if (v == mCreated_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mCreatedTextColorListener, getValue(CREATED_COLOR, R.string.default_created_color));
			cp.show();
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		ContentValues values = new ContentValues();
		values.put(INTERVAL, Integer.parseInt(getResources().getStringArray(R.array.interval_values)[which]));
		updateDatabase(values);
		dialog.cancel();
	}
}
