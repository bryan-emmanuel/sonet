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
package com.piusvelte.sonet.core;

import static com.piusvelte.sonet.core.Sonet.PRO;

import com.google.ads.*;
import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet.Widgets;
import com.piusvelte.sonet.core.Sonet.Widgets_settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AccountSettings extends Activity implements View.OnClickListener, OnCheckedChangeListener {
	private int mMessages_bg_color_value = Sonet.default_message_bg_color,
	mMessages_color_value = Sonet.default_message_color,
	mMessages_textsize_value = Sonet.default_messages_textsize,
	mFriend_color_value = Sonet.default_friend_color,
	mFriend_textsize_value = Sonet.default_friend_textsize,
	mCreated_color_value = Sonet.default_created_color,
	mCreated_textsize_value = Sonet.default_created_textsize,
	mStatuses_per_account_value = Sonet.default_statuses_per_account,
	mProfiles_bg_color_value = Sonet.default_message_bg_color,
	mFriend_bg_color_value = Sonet.default_friend_bg_color;
	private Button mMessages_bg_color;
	private Button mMessages_color;
	private Button mMessages_textsize;
	private Button mFriend_color;
	private Button mFriend_textsize;
	private Button mCreated_color;
	private Button mCreated_textsize;
	private CheckBox mTime24hr;
	private CheckBox mIcon;
	private Button mStatuses_per_account;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private long mAccountId = Sonet.INVALID_ACCOUNT_ID;
	private String mWidgetAccountSettingsId = null;
	private CheckBox mSound;
	private CheckBox mVibrate;
	private CheckBox mLights;
	private Button mProfiles_bg_color;
	private Button mFriend_bg_color;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		setContentView(R.layout.account_preferences);
		if (!getPackageName().toLowerCase().contains(PRO)) {
			AdView adView = new AdView(this, AdSize.BANNER, SonetTokens.GOOGLE_AD_ID);
			((LinearLayout) findViewById(R.id.ad)).addView(adView);
			adView.loadAd(new AdRequest());
		}
		Intent i = getIntent();
		if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
			mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		}
		if (i.hasExtra(Sonet.EXTRA_ACCOUNT_ID)) {
			mAccountId = i.getLongExtra(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID);
		}

		mMessages_bg_color = (Button) findViewById(R.id.messages_bg_color);
		mMessages_color = (Button) findViewById(R.id.messages_color);
		mMessages_textsize = (Button) findViewById(R.id.messages_textsize);
		mFriend_color = (Button) findViewById(R.id.friend_color);
		mFriend_textsize = (Button) findViewById(R.id.friend_textsize);
		mCreated_color = (Button) findViewById(R.id.created_color);
		mCreated_textsize = (Button) findViewById(R.id.created_textsize);
		mTime24hr = (CheckBox) findViewById(R.id.time24hr);
		mIcon = (CheckBox) findViewById(R.id.icon);
		mStatuses_per_account = (Button) findViewById(R.id.statuses_per_account);
		mSound = (CheckBox) findViewById(R.id.sound);
		mVibrate = (CheckBox) findViewById(R.id.vibrate);
		mLights = (CheckBox) findViewById(R.id.lights);
		mProfiles_bg_color = (Button) findViewById(R.id.profile_bg_color);
		mFriend_bg_color = (Button) findViewById(R.id.friend_bg_color);

		int scrollableVersion = 0;

		// get this account/widgets settings, falling back on the defaults...
		Cursor c = this.getContentResolver().query(Widgets_settings.getContentUri(this), new String[]{Widgets._ID, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(mAppWidgetId), Long.toString(mAccountId)}, null);
		if (!c.moveToFirst()) {
			c.close();
			c = this.getContentResolver().query(Widgets_settings.getContentUri(this), new String[]{Widgets._ID, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(mAppWidgetId), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
			if (!c.moveToFirst()) {
				c.close();
				c = this.getContentResolver().query(Widgets_settings.getContentUri(this), new String[]{Widgets._ID, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
				if (!c.moveToFirst()) {
					// initialize account settings
					ContentValues values = new ContentValues();
					values.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
					values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
					mWidgetAccountSettingsId = getContentResolver().insert(Widgets.getContentUri(this), values).getLastPathSegment();
				}
				if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
					// initialize account settings
					ContentValues values = new ContentValues();
					values.put(Widgets.WIDGET, mAppWidgetId);
					values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
					mWidgetAccountSettingsId = getContentResolver().insert(Widgets.getContentUri(this), values).getLastPathSegment();
				}
			}
			// initialize account settings
			ContentValues values = new ContentValues();
			values.put(Widgets.WIDGET, mAppWidgetId);
			values.put(Widgets.ACCOUNT, mAccountId);
			mWidgetAccountSettingsId = getContentResolver().insert(Widgets.getContentUri(this), values).getLastPathSegment();
		}
		if (c.moveToFirst()) {
			// if settings were initialized, then use that mWidgetAccountSettingsId
			if (mWidgetAccountSettingsId == null) {
				mWidgetAccountSettingsId = Integer.toString(c.getInt(0));
			}
			mMessages_color_value = c.getInt(1);
			mMessages_textsize_value = c.getInt(2);
			mFriend_color_value = c.getInt(3);
			mFriend_textsize_value = c.getInt(4);
			mCreated_color_value = c.getInt(5);
			mCreated_textsize_value = c.getInt(6);
			mTime24hr.setChecked(c.getInt(7) == 1);
			mMessages_bg_color_value = c.getInt(8);
			mIcon.setChecked(c.getInt(9) == 1);
			mStatuses_per_account_value = c.getInt(10);
			scrollableVersion = c.getInt(11);
			mSound.setChecked(c.getInt(12) == 1);
			mVibrate.setChecked(c.getInt(13) == 1);
			mLights.setChecked(c.getInt(14) == 1);
			mProfiles_bg_color_value = c.getInt(15);
			mFriend_bg_color_value = c.getInt(16);
		}
		c.close();

		mMessages_bg_color.setOnClickListener(this);
		mTime24hr.setOnCheckedChangeListener(this);
		mIcon.setOnCheckedChangeListener(this);
		mStatuses_per_account.setOnClickListener(this);
		mSound.setOnCheckedChangeListener(this);
		mVibrate.setOnCheckedChangeListener(this);
		mLights.setOnCheckedChangeListener(this);
		mProfiles_bg_color.setOnClickListener(this);
		mFriend_bg_color.setOnClickListener(this);

		if (scrollableVersion == 1) {
			mMessages_color.setEnabled(false);
			mMessages_textsize.setEnabled(false);
			mFriend_color.setEnabled(false);
			mFriend_textsize.setEnabled(false);
			mCreated_color.setEnabled(false);
			mCreated_textsize.setEnabled(false);			
		} else {
			mMessages_color.setOnClickListener(this);
			mMessages_textsize.setOnClickListener(this);
			mFriend_color.setOnClickListener(this);
			mFriend_textsize.setOnClickListener(this);
			mCreated_color.setOnClickListener(this);
			mCreated_textsize.setOnClickListener(this);			
		}

	}

	private void updateDatabase(String column, int value) {
		ContentValues values = new ContentValues();
		values.put(column, value);
		this.getContentResolver().update(Widgets.getContentUri(this), values, Widgets._ID + "=?", new String[]{mWidgetAccountSettingsId});
		setResult(RESULT_OK);
	}

	ColorPickerDialog.OnColorChangedListener mBodyBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			AccountSettings.this.mMessages_bg_color_value = color;
			updateDatabase(Widgets.MESSAGES_BG_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mBodyTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			AccountSettings.this.mMessages_color_value = color;
			updateDatabase(Widgets.MESSAGES_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mFriendTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			AccountSettings.this.mFriend_color_value = color;
			updateDatabase(Widgets.FRIEND_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mCreatedTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			AccountSettings.this.mCreated_color_value = color;
			updateDatabase(Widgets.CREATED_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mProfileBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			AccountSettings.this.mProfiles_bg_color_value = color;
			updateDatabase(Widgets.PROFILES_BG_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mFriendBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			AccountSettings.this.mFriend_bg_color_value = color;
			updateDatabase(Widgets.FRIEND_BG_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	@Override
	public void onClick(View v) {
		if (v == mMessages_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyBackgroundColorListener, this.mMessages_bg_color_value);
			cp.show();
		} else if (v == mMessages_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyTextColorListener, this.mMessages_color_value);
			cp.show();
		} else if (v == mMessages_textsize) {
			int index = 0;
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == this.mMessages_textsize_value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AccountSettings.this.mMessages_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
					updateDatabase(Widgets.MESSAGES_TEXTSIZE, mMessages_textsize_value);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();			
		} else if (v == mFriend_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mFriendTextColorListener, this.mFriend_color_value);
			cp.show();
		} else if (v == mFriend_textsize) {
			int index = 0;
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == this.mFriend_textsize_value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AccountSettings.this.mFriend_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
					updateDatabase(Widgets.FRIEND_TEXTSIZE, mFriend_textsize_value);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();			
		} else if (v == mCreated_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mCreatedTextColorListener, this.mCreated_color_value);
			cp.show();
		} else if (v == mCreated_textsize) {
			int index = 0;
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == this.mCreated_textsize_value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AccountSettings.this.mCreated_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
					updateDatabase(Widgets.CREATED_TEXTSIZE, mCreated_textsize_value);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();			
		} else if (v == mStatuses_per_account) {
			int index = 0;
			String[] values = getResources().getStringArray(R.array.status_count_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == this.mStatuses_per_account_value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.status_count_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AccountSettings.this.mStatuses_per_account_value = Integer.parseInt(getResources().getStringArray(R.array.status_count_values)[which]);
					updateDatabase(Widgets.STATUSES_PER_ACCOUNT, mStatuses_per_account_value);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();
		} else if (v == mProfiles_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mProfileBackgroundColorListener, this.mProfiles_bg_color_value);
			cp.show();
		} else if (v == mFriend_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mFriendBackgroundColorListener, this.mFriend_bg_color_value);
			cp.show();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView == mTime24hr) {
			updateDatabase(Widgets.TIME24HR, isChecked ? 1 : 0);
		} else if (buttonView == mIcon) {
			updateDatabase(Widgets.ICON, isChecked ? 1 : 0);
		} else if (buttonView == mSound) {
			updateDatabase(Widgets.SOUND, isChecked ? 1 : 0);
		} else if (buttonView == mVibrate) {
			updateDatabase(Widgets.VIBRATE, isChecked ? 1 : 0);
		} else if (buttonView == mLights) {
			updateDatabase(Widgets.LIGHTS, isChecked ? 1 : 0);
		}
	}
}
