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
import static com.piusvelte.sonet.core.Sonet.initAccountSettings;

import com.google.ads.*;
import com.piusvelte.sonet.core.R;
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

public class AccountSettings extends Activity implements View.OnClickListener {
	private int mMessages_bg_color_value = Sonet.default_message_bg_color,
	mMessages_color_value = Sonet.default_message_color,
	mMessages_textsize_value = Sonet.default_messages_textsize,
	mFriend_color_value = Sonet.default_friend_color,
	mFriend_textsize_value = Sonet.default_friend_textsize,
	mCreated_color_value = Sonet.default_created_color,
	mCreated_textsize_value = Sonet.default_created_textsize,
	mStatuses_per_account_value = Sonet.default_statuses_per_account,
	mProfiles_bg_color_value = Sonet.default_message_bg_color,
	mFriend_bg_color_value = Sonet.default_friend_bg_color,
	mScrollable_version = 0;
	private boolean mTime24hr_value = Sonet.default_time24hr,
			mIcon_value = Sonet.default_hasIcon,
			mSound_value = Sonet.default_sound,
			mVibrate_value = Sonet.default_vibrate,
			mLights_value = Sonet.default_lights,
			mDisplay_profile_value = Sonet.default_include_profile;
	private Button mStatuses_per_account;
	private Button mBtn_notification;
	private Button mBtn_name;
	private Button mBtn_time;
	private ImageButton mBtn_profile;
	private Button mBtn_message;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private long mAccountId = Sonet.INVALID_ACCOUNT_ID;
	private String mWidgetAccountSettingsId = null;
	private Dialog mDialog;

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
		if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID))
			mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		if (i.hasExtra(Sonet.EXTRA_ACCOUNT_ID))
			mAccountId = i.getLongExtra(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID);

		mStatuses_per_account = (Button) findViewById(R.id.statuses_per_account);
		mBtn_notification = (Button) findViewById(R.id.settings_notification);
		mBtn_name = (Button) findViewById(R.id.settings_name);
		mBtn_time = (Button) findViewById(R.id.settings_time);
		mBtn_profile = (ImageButton) findViewById(R.id.settings_profile);
		mBtn_message = (Button) findViewById(R.id.settings_message);

		Drawable wp = WallpaperManager.getInstance(getApplicationContext()).getDrawable();
		if (wp != null)
			findViewById(R.id.ad).getRootView().setBackgroundDrawable(wp);

		// get this account/widgets settings, falling back on the defaults...
		Cursor c = this.getContentResolver().query(Widgets_settings.getContentUri(this), new String[]{Widgets._ID, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(mAppWidgetId), Long.toString(mAccountId)}, null);
		if (!c.moveToFirst()) {
			c.close();
			c = this.getContentResolver().query(Widgets_settings.getContentUri(this), new String[]{Widgets._ID, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(mAppWidgetId), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
			if (!c.moveToFirst()) {
				c.close();
				c = this.getContentResolver().query(Widgets_settings.getContentUri(this), new String[]{Widgets._ID, Widgets.MESSAGES_COLOR, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_COLOR, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_COLOR, Widgets.CREATED_TEXTSIZE, Widgets.TIME24HR, Widgets.MESSAGES_BG_COLOR, Widgets.ICON, Widgets.STATUSES_PER_ACCOUNT, Widgets.SCROLLABLE, Widgets.SOUND, Widgets.VIBRATE, Widgets.LIGHTS, Widgets.PROFILES_BG_COLOR, Widgets.FRIEND_BG_COLOR}, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(Sonet.INVALID_ACCOUNT_ID)}, null);
				if (!c.moveToFirst())
					mWidgetAccountSettingsId = initAccountSettings(this, AppWidgetManager.INVALID_APPWIDGET_ID, Sonet.INVALID_ACCOUNT_ID);
				if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
					mWidgetAccountSettingsId = initAccountSettings(this, mAppWidgetId, Sonet.INVALID_ACCOUNT_ID);
			}
			mWidgetAccountSettingsId = initAccountSettings(this, mAppWidgetId, mAccountId);
		}
		if (c.moveToFirst()) {
			// if settings were initialized, then use that mWidgetAccountSettingsId
			if (mWidgetAccountSettingsId == null)
				mWidgetAccountSettingsId = Integer.toString(c.getInt(0));
			mMessages_color_value = c.getInt(1);
			mMessages_textsize_value = c.getInt(2);
			mFriend_color_value = c.getInt(3);
			mFriend_textsize_value = c.getInt(4);
			mCreated_color_value = c.getInt(5);
			mCreated_textsize_value = c.getInt(6);
			mTime24hr_value = c.getInt(7) == 1;
			mMessages_bg_color_value = c.getInt(8);
			mIcon_value = c.getInt(9) == 1;
			mStatuses_per_account_value = c.getInt(10);
			mScrollable_version = c.getInt(11);
			mSound_value = c.getInt(12) == 1;
			mVibrate_value = c.getInt(13) == 1;
			mLights_value = c.getInt(14) == 1;
			mProfiles_bg_color_value = c.getInt(15);
			mFriend_bg_color_value = c.getInt(16);
		}
		c.close();

		mStatuses_per_account.setOnClickListener(this);
		
		mBtn_notification.setOnClickListener(this);
		
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
	
	@Override
	protected void onPause() {
		if ((mDialog != null) && mDialog.isShowing())
			mDialog.cancel();
		super.onPause();
	}

	private void updateDatabase(String column, int value) {
		ContentValues values = new ContentValues();
		values.put(column, value);
		this.getContentResolver().update(Widgets.getContentUri(this), values, Widgets._ID + "=?", new String[]{mWidgetAccountSettingsId});
		setResult(RESULT_OK);
	}

	@Override
	public void onClick(View v) {
		if (v == mStatuses_per_account) {
			// interval
			// statuses per account
			// background
			int index = 0;
			String[] values = getResources().getStringArray(R.array.status_counts);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == mStatuses_per_account_value) {
					index = i;
					break;
				}
			}
			mDialog = (new AlertDialog.Builder(AccountSettings.this))
					.setSingleChoiceItems(R.array.status_counts, index, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mStatuses_per_account_value = Integer.parseInt(getResources().getStringArray(R.array.status_counts)[which]);
							updateDatabase(Widgets.STATUSES_PER_ACCOUNT, mStatuses_per_account_value);
							dialog.cancel();
						}
					})
					.setCancelable(true)
					.create();
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
					ColorPickerDialog cp = new ColorPickerDialog(AccountSettings.this, new ColorPickerDialog.OnColorChangedListener() {

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
							Toast.makeText(AccountSettings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
						} else {
						ColorPickerDialog cp = new ColorPickerDialog(AccountSettings.this, new ColorPickerDialog.OnColorChangedListener() {

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
							Toast.makeText(AccountSettings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
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
						mDialog = (new AlertDialog.Builder(AccountSettings.this))
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
							Toast.makeText(AccountSettings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
						} else {
						ColorPickerDialog cp = new ColorPickerDialog(AccountSettings.this, new ColorPickerDialog.OnColorChangedListener() {

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
							Toast.makeText(AccountSettings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
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
						mDialog = (new AlertDialog.Builder(AccountSettings.this))
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
					ColorPickerDialog cp = new ColorPickerDialog(AccountSettings.this, new ColorPickerDialog.OnColorChangedListener() {

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
					ColorPickerDialog cp = new ColorPickerDialog(AccountSettings.this, new ColorPickerDialog.OnColorChangedListener() {

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
							Toast.makeText(AccountSettings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
						} else {
						ColorPickerDialog cp = new ColorPickerDialog(AccountSettings.this, new ColorPickerDialog.OnColorChangedListener() {

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
							Toast.makeText(AccountSettings.this, "This setting is not supported by the current Android Launcher", Toast.LENGTH_SHORT).show();
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
						mDialog = (new AlertDialog.Builder(AccountSettings.this))
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
