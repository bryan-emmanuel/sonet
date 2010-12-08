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

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.HASBUTTONS;
import static com.piusvelte.sonet.SonetDatabaseHelper.INTERVAL;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIME24HR;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_TEXTSIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_TEXTSIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_TEXTSIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_TEXTSIZE;

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

public class Settings extends Activity implements View.OnClickListener {
	private Button mInterval;
	private CheckBox mHasButtons;
	private Button mButtons_bg_color;
	private Button mButtons_color;
	private Button mButtons_textsize;
	private Button mMessages_bg_color;
	private Button mMessages_color;
	private Button mMessages_textsize;
	private Button mFriend_color;
	private Button mFriend_textsize;
	private Button mCreated_color;
	private Button mCreated_textsize;
	private CheckBox mTime24hr;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private SonetDatabaseHelper mSonetDatabaseHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferences);
		Intent i = getIntent();
		if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(Activity.RESULT_OK, (new Intent()).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
		mSonetDatabaseHelper = new SonetDatabaseHelper(this);
		mInterval = (Button) findViewById(R.id.interval);
		mHasButtons = (CheckBox) findViewById(R.id.hasbuttons);
		mButtons_bg_color = (Button) findViewById(R.id.buttons_bg_color);
		mButtons_color = (Button) findViewById(R.id.buttons_color);
		mButtons_textsize = (Button) findViewById(R.id.buttons_textsize);
		mMessages_bg_color = (Button) findViewById(R.id.messages_bg_color);
		mMessages_color = (Button) findViewById(R.id.messages_color);
		mMessages_textsize = (Button) findViewById(R.id.messages_textsize);
		mFriend_color = (Button) findViewById(R.id.friend_color);
		mFriend_textsize = (Button) findViewById(R.id.friend_textsize);
		mCreated_color = (Button) findViewById(R.id.created_color);
		mCreated_textsize = (Button) findViewById(R.id.created_textsize);
		mTime24hr = (CheckBox) findViewById(R.id.time24hr);
		SQLiteDatabase db = mSonetDatabaseHelper.getReadableDatabase();
		Cursor c = db.rawQuery("select " + _ID + "," + HASBUTTONS + "," + TIME24HR + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + mAppWidgetId, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			mHasButtons.setChecked(c.getInt(c.getColumnIndex(HASBUTTONS)) == 1);
			mTime24hr.setChecked(c.getInt(c.getColumnIndex(TIME24HR)) == 1);
		} else mHasButtons.setChecked(true);
		c.close();
		db.close();
		mInterval.setOnClickListener(this);
		mHasButtons.setOnCheckedChangeListener(mHasButtonsListener);
		mButtons_bg_color.setOnClickListener(this);
		mButtons_color.setOnClickListener(this);
		mButtons_textsize.setOnClickListener(this);
		mMessages_bg_color.setOnClickListener(this);
		mMessages_color.setOnClickListener(this);
		mMessages_textsize.setOnClickListener(this);
		mFriend_color.setOnClickListener(this);
		mFriend_textsize.setOnClickListener(this);
		mCreated_color.setOnClickListener(this);
		mCreated_textsize.setOnClickListener(this);
		mTime24hr.setOnCheckedChangeListener(mTime24hrListener);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
	}

	private void updateDatabase(ContentValues values) {
		SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
		db.update(TABLE_WIDGETS, values, WIDGET + "=" + mAppWidgetId, null);
		db.close();		
	}
	
	private int getValue(String column, int default_value) {
		int value;
		SQLiteDatabase db = mSonetDatabaseHelper.getReadableDatabase();
		Cursor c = db.rawQuery("select " + _ID + "," + column + " from " + TABLE_WIDGETS + " where " + WIDGET + "=" + mAppWidgetId, null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			value = c.getInt(c.getColumnIndex(column));
		}
		else value = Integer.parseInt(getString(default_value));
		c.close();
		db.close();
		return value;		
	}
	
	ColorPickerDialog.OnColorChangedListener mHeadBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			ContentValues values = new ContentValues();
			values.put(BUTTONS_BG_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mHeadTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			ContentValues values = new ContentValues();
			values.put(BUTTONS_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mBodyBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			ContentValues values = new ContentValues();
			values.put(MESSAGES_BG_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mBodyTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			ContentValues values = new ContentValues();
			values.put(MESSAGES_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mFriendTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			ContentValues values = new ContentValues();
			values.put(FRIEND_COLOR, color);
			updateDatabase(values);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mCreatedTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
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
			int index = 0,
			value = getValue(INTERVAL, R.string.default_interval);
			String[] values = getResources().getStringArray(R.array.interval_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.interval_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ContentValues values = new ContentValues();
					values.put(INTERVAL, Integer.parseInt(getResources().getStringArray(R.array.interval_values)[which]));
					updateDatabase(values);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();
		} else if (v == mButtons_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadBackgroundColorListener, getValue(BUTTONS_BG_COLOR, R.string.default_buttons_bg_color));
			cp.show();
		} else if (v == mButtons_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadTextColorListener, getValue(BUTTONS_COLOR, R.string.default_buttons_color));
			cp.show();
		} else if (v == mButtons_textsize) {
			int index = 0,
			value = getValue(BUTTONS_TEXTSIZE, R.string.default_buttons_textsize);
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ContentValues values = new ContentValues();
					values.put(BUTTONS_TEXTSIZE, Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]));
					updateDatabase(values);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();
		} else if (v == mMessages_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyBackgroundColorListener, getValue(MESSAGES_BG_COLOR, R.string.default_message_bg_color));
			cp.show();
		} else if (v == mMessages_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyTextColorListener, getValue(MESSAGES_COLOR, R.string.default_message_color));
			cp.show();
		} else if (v == mMessages_textsize) {
			int index = 0,
			value = getValue(MESSAGES_TEXTSIZE, R.string.default_messages_textsize);
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ContentValues values = new ContentValues();
					values.put(MESSAGES_TEXTSIZE, Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]));
					updateDatabase(values);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();			
		} else if (v == mFriend_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mFriendTextColorListener, getValue(FRIEND_COLOR, R.string.default_friend_color));
			cp.show();
		} else if (v == mFriend_textsize) {
			int index = 0,
			value = getValue(FRIEND_TEXTSIZE, R.string.default_buttons_textsize);
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ContentValues values = new ContentValues();
					values.put(FRIEND_TEXTSIZE, Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]));
					updateDatabase(values);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();			
		} else if (v == mCreated_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mCreatedTextColorListener, getValue(CREATED_COLOR, R.string.default_created_color));
			cp.show();
		} else if (v == mCreated_textsize) {
			int index = 0,
			value = getValue(CREATED_TEXTSIZE, R.string.default_buttons_textsize);
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ContentValues values = new ContentValues();
					values.put(CREATED_TEXTSIZE, Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]));
					updateDatabase(values);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();			
		}
	}
}
