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
import com.piusvelte.sonet.core.Sonet.Widgets;
import com.piusvelte.sonet.core.Sonet.Widgets_settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Settings extends Activity implements View.OnClickListener {
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
			mFriend_bg_color_value = Sonet.default_friend_bg_color,
			mScrollable_version = 0;
	private boolean mHasButtons_value = Sonet.default_hasButtons,
			mTime24hr_value = Sonet.default_time24hr,
			mIcon_value = Sonet.default_hasIcon,
			mBackgroundUpdate_value = Sonet.default_backgroundUpdate,
			mSound_value = Sonet.default_sound,
			mVibrate_value = Sonet.default_vibrate,
			mLights_value = Sonet.default_lights,
			mDisplay_profile_value = Sonet.default_include_profile,
			mInstantUpload_value = Sonet.default_instantUpload;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private String mWidgetSettingsId = null;
	private Dialog mDialog;
	private Button mBtn_update;
	private Button mBtn_notification;
	private CheckBox mChk_instantUpload;
	private Button mBtn_margin;
	private Button mBtn_buttons;
	private Button mBtn_name;
	private Button mBtn_time;
	private ImageButton mBtn_profile;
	private Button mBtn_message;

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
		if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID))
			mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		mBtn_update = (Button) findViewById(R.id.settings_update);
		mBtn_notification = (Button) findViewById(R.id.settings_notification);
		mChk_instantUpload = (CheckBox) findViewById(R.id.instantupload);
		mBtn_margin = (Button) findViewById(R.id.margin);
		mBtn_buttons = (Button) findViewById(R.id.settings_buttons);
		mBtn_name = (Button) findViewById(R.id.settings_name);
		mBtn_time = (Button) findViewById(R.id.settings_time);
		mBtn_profile = (ImageButton) findViewById(R.id.settings_profile);
		mBtn_message = (Button) findViewById(R.id.settings_message);

		Drawable wp = WallpaperManager.getInstance(getApplicationContext()).getDrawable();
		if (wp != null)
			findViewById(R.id.ad).getRootView().setBackgroundDrawable(wp);

		Cursor c = this.getContentResolver().query(Widgets_settings.getContentUri(this), new String[]{Widgets._ID, Widgets.INTERVAL, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_TEXTSIZE, Widgets.MESSAGES_BG_COLOR, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.HASBUTTONS, Widgets.TIME24HR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.BACKGROUND_UPDATE, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.DISPLAY_PROFILE, Widgets.INSTANT_UPLOAD, Widgets.MARGIN, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(mAppWidgetId), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
		if (!c.moveToFirst()) {
			c.close();
			c = this.getContentResolver().query(Widgets_settings.getContentUri(this), new String[]{Widgets._ID, Widgets.INTERVAL, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_COLOR, Widgets.BUTTONS_TEXTSIZE, Widgets.MESSAGES_BG_COLOR, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.HASBUTTONS, Widgets.TIME24HR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.BACKGROUND_UPDATE, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.DISPLAY_PROFILE, Widgets.INSTANT_UPLOAD, Widgets.MARGIN, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
			if (!c.moveToFirst()) {
				// initialize widget settings
				ContentValues values = new ContentValues();
				values.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
				values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
				mWidgetSettingsId = getContentResolver().insert(Widgets.getContentUri(this), values).getLastPathSegment();
			}
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				// initialize widget settings
				ContentValues values = new ContentValues();
				values.put(Widgets.WIDGET, mAppWidgetId);
				values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
				mWidgetSettingsId = getContentResolver().insert(Widgets.getContentUri(this), values).getLastPathSegment();
			}
		}
		if (c.moveToFirst()) {
			// if settings were initialized, then use that mWidgetSettingsId
			if (mWidgetSettingsId == null) {
				mWidgetSettingsId = Integer.toString(c.getInt(0));
			}
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
			mHasButtons_value = c.getInt(12) == 1;
			mTime24hr_value = c.getInt(13) == 1;
			mIcon_value = c.getInt(14) == 1;
			mStatuses_per_account_value = c.getInt(15);
			mBackgroundUpdate_value = c.getInt(16) == 1;
			mScrollable_version = c.getInt(17);
			mSound_value = c.getInt(18) == 1;
			mVibrate_value = c.getInt(19) == 1;
			mLights_value = c.getInt(20) == 1;
			mDisplay_profile_value = c.getInt(21) == 1;
			mInstantUpload_value = c.getInt(22) == 1;
			mMargin_value = c.getInt(23);
			mProfiles_bg_color_value = c.getInt(24);
			mFriend_bg_color_value = c.getInt(25);
		}
		c.close();

		mBtn_update.setOnClickListener(this);
		mBtn_notification.setOnClickListener(this);

		mChk_instantUpload.setChecked(mInstantUpload_value);
		mChk_instantUpload.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// facebook only
				if (isChecked) {
					(Toast.makeText(Settings.this, "Currently, the photo will only be uploaded Facebook accounts.", Toast.LENGTH_LONG)).show();
				}
				updateDatabase(Widgets.INSTANT_UPLOAD, isChecked ? 1 : 0);
			}

		});

		mBtn_margin.setOnClickListener(this);

		mBtn_buttons.setBackgroundColor(mButtons_bg_color_value);
		mBtn_buttons.setTextColor(mButtons_color_value);
		mBtn_buttons.setTextSize(mButtons_textsize_value);
		mBtn_buttons.setOnClickListener(this);

		mBtn_name.setBackgroundColor(mFriend_bg_color_value);
		mBtn_name.setTextColor(mFriend_color_value);
		mBtn_name.setTextSize(mFriend_textsize_value);
		mBtn_name.setOnClickListener(this);

		mBtn_time.setBackgroundColor(mFriend_bg_color_value);
		mBtn_time.setTextColor(mCreated_color_value);
		mBtn_time.setTextSize(mCreated_textsize_value);
		mBtn_time.setOnClickListener(this);

		mBtn_profile.setBackgroundColor(mProfiles_bg_color_value);
		mBtn_profile.setOnClickListener(this);

		mBtn_message.setBackgroundColor(mMessages_bg_color_value);
		mBtn_message.setTextColor(mMessages_color_value);
		mBtn_message.setTextSize(mMessages_textsize_value);
		mBtn_message.setOnClickListener(this);

	}

	private void updateDatabase(String column, int value) {
		ContentValues values = new ContentValues();
		values.put(column, value);
		this.getContentResolver().update(Widgets.getContentUri(this), values, Widgets._ID + "=?", new String[]{mWidgetSettingsId});
		setResult(RESULT_OK);
	}


	@Override
	public void onClick(View v) {
		if (v == mBtn_update) {
			// interval
			// statuses per account
			// background
			mDialog = new Dialog(this);
			mDialog.setTitle(R.string.settings_update);
			mDialog.setContentView(R.layout.settings_update);
			Button btn_interval = (Button) mDialog.findViewById(R.id.interval);
			btn_interval.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mDialog.cancel();
					int index = 0;
					int value = mInterval_value;
					String[] values = getResources().getStringArray(R.array.interval_values);
					for (int i = 0; i < values.length; i++) {
						if (Integer.parseInt(values[i]) == value) {
							index = i;
							break;
						}
					}
					mDialog = (new AlertDialog.Builder(Settings.this))
							.setSingleChoiceItems(R.array.interval_entries, index, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mInterval_value = Integer.parseInt(getResources().getStringArray(R.array.interval_values)[which]);
									updateDatabase(Widgets.INTERVAL, mInterval_value);
									dialog.cancel();
								}
							})
							.setCancelable(true)
							.create();
					mDialog.show();
				}

			});

			Button btn_statuses = (Button) mDialog.findViewById(R.id.statuses_per_account);
			btn_statuses.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mDialog.cancel();
					int index = 0;
					String[] values = getResources().getStringArray(R.array.status_count_values);
					for (int i = 0; i < values.length; i++) {
						if (Integer.parseInt(values[i]) == mStatuses_per_account_value) {
							index = i;
							break;
						}
					}
					mDialog = (new AlertDialog.Builder(Settings.this))
							.setSingleChoiceItems(R.array.status_count_entries, index, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mStatuses_per_account_value = Integer.parseInt(getResources().getStringArray(R.array.status_count_values)[which]);
									updateDatabase(Widgets.STATUSES_PER_ACCOUNT, mStatuses_per_account_value);
									dialog.cancel();
								}
							})
							.setCancelable(true)
							.create();
					mDialog.show();
				}

			});

			CheckBox chk_bg = (CheckBox) mDialog.findViewById(R.id.background_update);
			chk_bg.setChecked(mBackgroundUpdate_value);
			chk_bg.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
					mBackgroundUpdate_value = isChecked;
					updateDatabase(Widgets.BACKGROUND_UPDATE, isChecked ? 1 : 0);
				}

			});

			mDialog.show();
		} else if (v == mBtn_notification) {
			// sound
			// light
			// vibrate
			mDialog = new Dialog(this);
			mDialog.setTitle(R.string.settings_notification);
			mDialog.setContentView(R.layout.settings_notification);

			CheckBox chk_sound = (CheckBox) mDialog.findViewById(R.id.sound);
			chk_sound.setChecked(mSound_value);
			chk_sound.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
					mSound_value = isChecked;
					updateDatabase(Widgets.SOUND, isChecked ? 1 : 0);
				}

			});

			CheckBox chk_vibrate = (CheckBox) mDialog.findViewById(R.id.vibrate);
			chk_vibrate.setChecked(mVibrate_value);
			chk_vibrate.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
					mVibrate_value = isChecked;
					updateDatabase(Widgets.VIBRATE, isChecked ? 1 : 0);
				}

			});

			CheckBox chk_lights = (CheckBox) mDialog.findViewById(R.id.lights);
			chk_lights.setChecked(mLights_value);
			chk_lights.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
					mLights_value = isChecked;
					updateDatabase(Widgets.LIGHTS, isChecked ? 1 : 0);
				}

			});

			mDialog.show();
		} else if (v == mBtn_margin) {
			int index = 0,
					value = this.mMargin_value;
			String[] values = getResources().getStringArray(R.array.margin_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == value) {
					index = i;
					break;
				}
			}
			mDialog = (new AlertDialog.Builder(this))
					.setSingleChoiceItems(R.array.margin_entries, index, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mMargin_value = Integer.parseInt(getResources().getStringArray(R.array.margin_values)[which]);
							updateDatabase(Widgets.MARGIN, mMargin_value);
							dialog.cancel();
						}
					})
					.setCancelable(true)
					.create();
			mDialog.show();
		} else if (v == mBtn_buttons) {
			// enabled
			// bg color
			// color
			// textsize
			mDialog = new Dialog(this);
			mDialog.setTitle(R.string.settings_buttons);
			mDialog.setContentView(R.layout.settings_buttons);

			CheckBox chk_hasbuttons = (CheckBox) mDialog.findViewById(R.id.hasbuttons);
			chk_hasbuttons.setChecked(mHasButtons_value);
			chk_hasbuttons.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
					mHasButtons_value = isChecked;
					updateDatabase(Widgets.HASBUTTONS, isChecked ? 1 : 0);
					if (!isChecked) {
						Button btn_buttons_bg_color = (Button) mDialog.findViewById(R.id.buttons_bg_color);
						btn_buttons_bg_color.setEnabled(false);
						Button btn_buttons_color = (Button) mDialog.findViewById(R.id.buttons_color);
						btn_buttons_color.setEnabled(false);
						Button btn_buttons_textsize = (Button) mDialog.findViewById(R.id.buttons_textsize);
						btn_buttons_textsize.setEnabled(false);
					}
				}

			});

			Button btn_buttons_bg_color = (Button) mDialog.findViewById(R.id.buttons_bg_color);
			btn_buttons_bg_color.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ColorPickerDialog cp = new ColorPickerDialog(Settings.this, new ColorPickerDialog.OnColorChangedListener() {

						@Override
						public void colorChanged(int color) {
							mButtons_bg_color_value = color;
							updateDatabase(Widgets.BUTTONS_BG_COLOR, color);
							mBtn_buttons.setBackgroundColor(mButtons_bg_color_value);
						}

						@Override
						public void colorUpdate(int color) {
						}}, mButtons_bg_color_value);
					cp.show();
				}

			});

			Button btn_buttons_color = (Button) mDialog.findViewById(R.id.buttons_color);
			btn_buttons_color.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ColorPickerDialog cp = new ColorPickerDialog(Settings.this, new ColorPickerDialog.OnColorChangedListener() {

						@Override
						public void colorChanged(int color) {
							mButtons_color_value = color;
							updateDatabase(Widgets.BUTTONS_COLOR, color);
							mBtn_buttons.setTextColor(mButtons_color_value);
						}

						@Override
						public void colorUpdate(int color) {
						}}, mButtons_color_value);
					cp.show();
				}

			});

			Button btn_buttons_textsize = (Button) mDialog.findViewById(R.id.buttons_textsize);
			btn_buttons_textsize.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mDialog.cancel();
					int index = 0;
					String[] values = getResources().getStringArray(R.array.textsize_values);
					for (int i = 0; i < values.length; i++) {
						if (Integer.parseInt(values[i]) == mButtons_textsize_value) {
							index = i;
							break;
						}
					}
					mDialog = (new AlertDialog.Builder(Settings.this))
							.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mButtons_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
									updateDatabase(Widgets.BUTTONS_TEXTSIZE, mButtons_textsize_value);
									mBtn_buttons.setTextSize(mButtons_textsize_value);
									dialog.cancel();
								}
							})
							.setCancelable(true)
							.create();
					mDialog.show();
				}

			});

			mDialog.show();
		} else if (v == mBtn_name) {
			// bg color
			// color
			// textsize
			mDialog = new Dialog(this);
			mDialog.setTitle(R.string.settings_name);
			mDialog.setContentView(R.layout.settings_name);

			Button btn_friend_bg_color = (Button) mDialog.findViewById(R.id.friend_bg_color);
			btn_friend_bg_color.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ColorPickerDialog cp = new ColorPickerDialog(Settings.this, new ColorPickerDialog.OnColorChangedListener() {

						@Override
						public void colorChanged(int color) {
							mFriend_bg_color_value = color;
							updateDatabase(Widgets.FRIEND_BG_COLOR, color);
							mBtn_name.setBackgroundColor(mFriend_bg_color_value);
							mBtn_time.setBackgroundColor(mFriend_bg_color_value);
						}

						@Override
						public void colorUpdate(int color) {
						}}, mFriend_bg_color_value);
					cp.show();
				}

			});

			Button btn_friend_color = (Button) mDialog.findViewById(R.id.friend_color);
				btn_friend_color.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						if (mScrollable_version == 1) {
							Toast.makeText(Settings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
						} else {
						ColorPickerDialog cp = new ColorPickerDialog(Settings.this, new ColorPickerDialog.OnColorChangedListener() {

							@Override
							public void colorChanged(int color) {
								mFriend_color_value = color;
								updateDatabase(Widgets.FRIEND_COLOR, color);
								mBtn_name.setTextColor(mFriend_color_value);
							}

							@Override
							public void colorUpdate(int color) {
							}}, mFriend_color_value);
						cp.show();
						}
					}

				});

			Button btn_friend_textsize = (Button) mDialog.findViewById(R.id.friend_textsize);
				btn_friend_textsize.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						if (mScrollable_version == 1) {
							Toast.makeText(Settings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
						} else {
						mDialog.cancel();
						int index = 0;
						String[] values = getResources().getStringArray(R.array.textsize_values);
						for (int i = 0; i < values.length; i++) {
							if (Integer.parseInt(values[i]) == mFriend_textsize_value) {
								index = i;
								break;
							}
						}
						mDialog = (new AlertDialog.Builder(Settings.this))
								.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										mFriend_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
										updateDatabase(Widgets.FRIEND_TEXTSIZE, mFriend_textsize_value);
										mBtn_name.setTextSize(mFriend_textsize_value);
										dialog.cancel();
									}
								})
								.setCancelable(true)
								.create();
						mDialog.show();
						}
					}

				});

			mDialog.show();
		} else if (v == mBtn_time) {
			// color
			// textsize
			mDialog = new Dialog(this);
			mDialog.setTitle(R.string.settings_time);
			mDialog.setContentView(R.layout.settings_time);

			Button btn_created_color = (Button) mDialog.findViewById(R.id.created_color);
				btn_created_color.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						if (mScrollable_version == 1) {
							Toast.makeText(Settings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
						} else {
						ColorPickerDialog cp = new ColorPickerDialog(Settings.this, new ColorPickerDialog.OnColorChangedListener() {

							@Override
							public void colorChanged(int color) {
								mCreated_color_value = color;
								updateDatabase(Widgets.CREATED_COLOR, color);
								mBtn_time.setTextColor(mCreated_color_value);
							}

							@Override
							public void colorUpdate(int color) {
							}}, mCreated_color_value);
						cp.show();
						}
					}

				});

			Button btn_created_textsize = (Button) mDialog.findViewById(R.id.created_textsize);
				btn_created_textsize.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						if (mScrollable_version == 1) {
							Toast.makeText(Settings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
						} else {
						mDialog.cancel();
						int index = 0;
						String[] values = getResources().getStringArray(R.array.textsize_values);
						for (int i = 0; i < values.length; i++) {
							if (Integer.parseInt(values[i]) == mCreated_textsize_value) {
								index = i;
								break;
							}
						}
						mDialog = (new AlertDialog.Builder(Settings.this))
								.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										mCreated_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
										updateDatabase(Widgets.CREATED_TEXTSIZE, mCreated_textsize_value);
										mBtn_time.setTextSize(mCreated_textsize_value);
										dialog.cancel();
									}
								})
								.setCancelable(true)
								.create();
						mDialog.show();
						}
					}

				});

			CheckBox chk_time24hr = (CheckBox) mDialog.findViewById(R.id.time24hr);
			chk_time24hr.setChecked(mTime24hr_value);
			chk_time24hr.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
					mTime24hr_value =  isChecked;
					updateDatabase(Widgets.TIME24HR, isChecked ? 1 : 0);
				}

			});

			mDialog.show();
		} else if (v == mBtn_profile) {
			// enabled
			// bg color
			mDialog = new Dialog(this);
			mDialog.setTitle(R.string.settings_profile);
			mDialog.setContentView(R.layout.settings_profile);

			CheckBox chk_display_profile = (CheckBox) mDialog.findViewById(R.id.display_profile);
			chk_display_profile.setChecked(mDisplay_profile_value);
			chk_display_profile.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
					mDisplay_profile_value = isChecked;
					updateDatabase(Widgets.DISPLAY_PROFILE, isChecked ? 1 : 0);
				}

			});

			Button btn_profile_color = (Button) mDialog.findViewById(R.id.profile_bg_color);
			btn_profile_color.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ColorPickerDialog cp = new ColorPickerDialog(Settings.this, new ColorPickerDialog.OnColorChangedListener() {

						@Override
						public void colorChanged(int color) {
							mProfiles_bg_color_value = color;
							updateDatabase(Widgets.PROFILES_BG_COLOR, color);
							mBtn_profile.setBackgroundColor(mProfiles_bg_color_value);
						}

						@Override
						public void colorUpdate(int color) {
						}}, mProfiles_bg_color_value);
					cp.show();
				}

			});

			mDialog.show();
		} else if (v == mBtn_message) {
			// color
			// bg color
			// text size
			// icon enabled
			mDialog = new Dialog(this);
			mDialog.setTitle(R.string.settings_message);
			mDialog.setContentView(R.layout.settings_message);

			Button btn_messages_bg_color = (Button) mDialog.findViewById(R.id.messages_bg_color);
			btn_messages_bg_color.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ColorPickerDialog cp = new ColorPickerDialog(Settings.this, new ColorPickerDialog.OnColorChangedListener() {

						@Override
						public void colorChanged(int color) {
							mMessages_bg_color_value = color;
							updateDatabase(Widgets.MESSAGES_BG_COLOR, color);
							mBtn_message.setBackgroundColor(mMessages_bg_color_value);
						}

						@Override
						public void colorUpdate(int color) {
						}}, mMessages_bg_color_value);
					cp.show();
				}

			});

			Button btn_messages_color = (Button) mDialog.findViewById(R.id.messages_color);
				btn_messages_color.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						if (mScrollable_version == 1) {
							Toast.makeText(Settings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
						} else {
						ColorPickerDialog cp = new ColorPickerDialog(Settings.this, new ColorPickerDialog.OnColorChangedListener() {

							@Override
							public void colorChanged(int color) {
								mMessages_color_value = color;
								updateDatabase(Widgets.MESSAGES_COLOR, color);
								mBtn_message.setTextColor(mMessages_color_value);
							}

							@Override
							public void colorUpdate(int color) {
							}}, mMessages_color_value);
						cp.show();
						}
					}

				});

			Button btn_messages_textsize = (Button) mDialog.findViewById(R.id.messages_textsize);
				btn_messages_textsize.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						if (mScrollable_version == 1) {
							Toast.makeText(Settings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
						} else {
						mDialog.cancel();
						int index = 0;
						String[] values = getResources().getStringArray(R.array.textsize_values);
						for (int i = 0; i < values.length; i++) {
							if (Integer.parseInt(values[i]) == mMessages_textsize_value) {
								index = i;
								break;
							}
						}
						mDialog = (new AlertDialog.Builder(Settings.this))
								.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										mMessages_textsize_value = Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]);
										updateDatabase(Widgets.MESSAGES_TEXTSIZE, mMessages_textsize_value);
										mBtn_message.setTextSize(mMessages_textsize_value);
										dialog.cancel();
									}
								})
								.setCancelable(true)
								.create();
						mDialog.show();
						}
					}

				});

			CheckBox chk_hasIcon = (CheckBox) mDialog.findViewById(R.id.icon);
			chk_hasIcon.setChecked(mIcon_value);
			chk_hasIcon.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
					mIcon_value = isChecked;
					updateDatabase(Widgets.ICON, isChecked ? 1 : 0);
				}

			});

			mDialog.show();
		}
	}
}
