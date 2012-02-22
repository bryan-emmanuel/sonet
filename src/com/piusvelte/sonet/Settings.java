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

import static com.piusvelte.sonet.Sonet.PRO;

import com.google.ads.*;
import com.piusvelte.sonet.Sonet.Widgets;
import com.piusvelte.sonet.Sonet.Widgets_settings;

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
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Settings extends Activity implements View.OnClickListener, OnCheckedChangeListener {
	private int mInterval_value = Sonet.default_interval,
	mButtons_bg_color_value = Sonet.default_buttons_bg_color,
	mButtons_color_value = Sonet.default_buttons_color,
	mButtons_textsize_value = Sonet.default_buttons_textsize,
	mMessages_bg_color_value = Sonet.default_message_bg_color,
	mMessages_color_value = Sonet.default_message_color,
	mMessages_textsize_value = Sonet.default_messages_textsize,
	mFriend_color_value = Sonet.default_friend_color,
	mFriend_textsize_value = Sonet.default_friend_textsize,
	mCreated_color_value = Sonet.default_created_color,
	mCreated_textsize_value = Sonet.default_created_textsize,
	mStatuses_per_account_value = Sonet.default_statuses_per_account,
	mMargin_value = Sonet.default_margin,
	mProfiles_bg_color_value = Sonet.default_message_bg_color,
	mFriend_bg_color_value = Sonet.default_friend_bg_color;
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
	private CheckBox mIcon;
	private CheckBox mBackgroundUpdate;
	private Button mStatuses_per_account;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private String mWidgetSettingsId;
	private CheckBox mSound;
	private CheckBox mVibrate;
	private CheckBox mLights;
	private CheckBox mDisplay_profile;
	private CheckBox mInstantUpload;
	private Button mMargin;
	private Button mProfiles_bg_color;
	private Button mFriend_bg_color;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		setContentView(R.layout.preferences);
		if (!getPackageName().toLowerCase().contains(PRO)) {
			AdView adView = new AdView(this, AdSize.BANNER, SonetTokens.GOOGLE_AD_ID);
			((LinearLayout) findViewById(R.id.ad)).addView(adView);
			adView.loadAd(new AdRequest());
		}
		Intent i = getIntent();
		if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
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
		mIcon = (CheckBox) findViewById(R.id.icon);
		mStatuses_per_account = (Button) findViewById(R.id.statuses_per_account);
		mBackgroundUpdate = (CheckBox) findViewById(R.id.background_update);
		mSound = (CheckBox) findViewById(R.id.sound);
		mVibrate = (CheckBox) findViewById(R.id.vibrate);
		mLights = (CheckBox) findViewById(R.id.lights);
		mDisplay_profile = (CheckBox) findViewById(R.id.display_profile);
		mInstantUpload = (CheckBox) findViewById(R.id.instantupload);
		mMargin = (Button) findViewById(R.id.margin);
		mProfiles_bg_color = (Button) findViewById(R.id.profile_bg_color);
		mFriend_bg_color = (Button) findViewById(R.id.friend_bg_color);

		int scrollableVersion = 0;

		Cursor c = this.getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets._ID, Widgets.INTERVAL, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_TEXTSIZE, Widgets.MESSAGES_BG_COLOR, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.HASBUTTONS, Widgets.TIME24HR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.BACKGROUND_UPDATE, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.DISPLAY_PROFILE, Widgets.INSTANT_UPLOAD, Widgets.MARGIN, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(mAppWidgetId), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
		if (!c.moveToFirst()) {
			c.close();
			c = this.getContentResolver().query(Widgets_settings.CONTENT_URI, new String[]{Widgets._ID, Widgets.INTERVAL, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_TEXTSIZE, Widgets.MESSAGES_BG_COLOR, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.HASBUTTONS, Widgets.TIME24HR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.BACKGROUND_UPDATE, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.DISPLAY_PROFILE, Widgets.INSTANT_UPLOAD, Widgets.MARGIN, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
			if (!c.moveToFirst()) {
				// initialize widget settings
				ContentValues values = new ContentValues();
				values.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
				values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
				mWidgetSettingsId = this.getContentResolver().insert(Widgets.CONTENT_URI, values).getLastPathSegment();
			}
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				// initialize widget settings
				ContentValues values = new ContentValues();
				values.put(Widgets.WIDGET, mAppWidgetId);
				values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
				mWidgetSettingsId = this.getContentResolver().insert(Widgets.CONTENT_URI, values).getLastPathSegment();
			}
		}
		if (c.moveToFirst()) {
			mWidgetSettingsId = Integer.toString(c.getInt(0));
			mInterval_value = c.getInt(1);
			mButtons_bg_color_value = c.getInt(2);
			mButtons_color_value = c.getInt(3);
			mButtons_textsize_value = c.getInt(4);
			mMessages_bg_color_value = c.getInt(5);
			mMessages_color_value = c.getInt(6);
			mMessages_textsize_value = c.getInt(7);
			mFriend_color_value = c.getInt(8);
			mFriend_textsize_value = c.getInt(9);
			mCreated_color_value = c.getInt(10);
			mCreated_textsize_value = c.getInt(11);
			mHasButtons.setChecked(c.getInt(12) == 1);
			mTime24hr.setChecked(c.getInt(13) == 1);
			mIcon.setChecked(c.getInt(14) == 1);
			mStatuses_per_account_value = c.getInt(15);
			mBackgroundUpdate.setChecked(c.getInt(16) == 1);
			scrollableVersion = c.getInt(17);
			mSound.setChecked(c.getInt(18) == 1);
			mVibrate.setChecked(c.getInt(19) == 1);
			mLights.setChecked(c.getInt(20) == 1);
			mDisplay_profile.setChecked(c.getInt(21) == 1);
			mInstantUpload.setChecked(c.getInt(22) == 1);
			mMargin_value = c.getInt(23);
			mProfiles_bg_color_value = c.getInt(24);
			mFriend_bg_color_value = c.getInt(25);
		}
		c.close();

		mInterval.setOnClickListener(this);
		mButtons_bg_color.setOnClickListener(this);
		mButtons_color.setOnClickListener(this);
		mButtons_textsize.setOnClickListener(this);
		mMessages_bg_color.setOnClickListener(this);
		mHasButtons.setOnCheckedChangeListener(this);
		mTime24hr.setOnCheckedChangeListener(this);
		mIcon.setOnCheckedChangeListener(this);
		mStatuses_per_account.setOnClickListener(this);
		mBackgroundUpdate.setOnClickListener(this);
		mSound.setOnCheckedChangeListener(this);
		mVibrate.setOnCheckedChangeListener(this);
		mLights.setOnCheckedChangeListener(this);
		mDisplay_profile.setOnCheckedChangeListener(this);
		mMargin.setOnClickListener(this);
		mProfiles_bg_color.setOnClickListener(this);
		mInstantUpload.setOnCheckedChangeListener(this);
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
		this.getContentResolver().update(Widgets.CONTENT_URI, values, Widgets._ID + "=?", new String[]{mWidgetSettingsId});
		setResult(RESULT_OK);
	}

	ColorPickerDialog.OnColorChangedListener mHeadBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			Settings.this.mButtons_bg_color_value = color;
			updateDatabase(Widgets.BUTTONS_BG_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mHeadTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			Settings.this.mButtons_color_value = color;
			updateDatabase(Widgets.BUTTONS_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mBodyBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			Settings.this.mMessages_bg_color_value = color;
			updateDatabase(Widgets.MESSAGES_BG_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mBodyTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			Settings.this.mMessages_color_value = color;
			updateDatabase(Widgets.MESSAGES_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mFriendTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			Settings.this.mFriend_color_value = color;
			updateDatabase(Widgets.FRIEND_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mCreatedTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			Settings.this.mCreated_color_value = color;
			updateDatabase(Widgets.CREATED_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mProfileBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			Settings.this.mProfiles_bg_color_value = color;
			updateDatabase(Widgets.PROFILES_BG_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mFriendBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			Settings.this.mFriend_bg_color_value = color;
			updateDatabase(Widgets.FRIEND_BG_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	@Override
	public void onClick(View v) {
		if (v == mInterval) {
			int index = 0,
			value = this.mInterval_value;
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
					Settings.this.mInterval_value = Integer.parseInt(getResources().getStringArray(R.array.interval_values)[which]);
					updateDatabase(Widgets.INTERVAL, mInterval_value);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();
		} else if (v == mButtons_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadBackgroundColorListener, this.mButtons_bg_color_value);
			cp.show();
		} else if (v == mButtons_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadTextColorListener, this.mButtons_color_value);
			cp.show();
		} else if (v == mButtons_textsize) {
			int index = 0;
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == this.mButtons_textsize_value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Settings.this.mButtons_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
					updateDatabase(Widgets.BUTTONS_TEXTSIZE, mButtons_textsize_value);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();
		} else if (v == mMessages_bg_color) {
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
					Settings.this.mMessages_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
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
					Settings.this.mFriend_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
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
					Settings.this.mCreated_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
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
					Settings.this.mStatuses_per_account_value = Integer.parseInt(getResources().getStringArray(R.array.status_count_values)[which]);
					updateDatabase(Widgets.STATUSES_PER_ACCOUNT, mStatuses_per_account_value);
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();
		} else if (v == mMargin) {
			int index = 0,
			value = this.mMargin_value;
			String[] values = getResources().getStringArray(R.array.margin_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.margin_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Settings.this.mMargin_value = Integer.parseInt(getResources().getStringArray(R.array.margin_values)[which]);
					updateDatabase(Widgets.MARGIN, mMargin_value);
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
		if (buttonView == mHasButtons) {
			updateDatabase(Widgets.HASBUTTONS, isChecked ? 1 : 0);
		} else if (buttonView == mTime24hr) {
			updateDatabase(Widgets.TIME24HR, isChecked ? 1 : 0);
		} else if (buttonView == mIcon) {
			updateDatabase(Widgets.ICON, isChecked ? 1 : 0);
		} else if (buttonView == mBackgroundUpdate) {
			updateDatabase(Widgets.BACKGROUND_UPDATE, isChecked ? 1 : 0);
		} else if (buttonView == mSound) {
			updateDatabase(Widgets.SOUND, isChecked ? 1 : 0);
		} else if (buttonView == mVibrate) {
			updateDatabase(Widgets.VIBRATE, isChecked ? 1 : 0);
		} else if (buttonView == mLights) {
			updateDatabase(Widgets.LIGHTS, isChecked ? 1 : 0);
		} else if (buttonView == mDisplay_profile) {
			updateDatabase(Widgets.DISPLAY_PROFILE, isChecked ? 1 : 0);
		} else if (buttonView == mInstantUpload) {
			// facebook only
			if (isChecked) {
				(Toast.makeText(Settings.this, "Currently, the photo will only be uploaded Facebook accounts.", Toast.LENGTH_LONG)).show();
			}
			updateDatabase(Widgets.INSTANT_UPLOAD, isChecked ? 1 : 0);
		}
	}
}
