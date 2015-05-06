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

import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.piusvelte.sonet.fragment.BaseDialogFragment;
import com.piusvelte.sonet.fragment.ButtonSettingsDialogFragment;
import com.piusvelte.sonet.fragment.LoadingDialogFragment;
import com.piusvelte.sonet.fragment.MessageSettingsDialogFragment;
import com.piusvelte.sonet.fragment.NameSettingsDialogFragment;
import com.piusvelte.sonet.fragment.NotificationSettingsDialogFragment;
import com.piusvelte.sonet.fragment.ProfileSettingsDialogFragment;
import com.piusvelte.sonet.fragment.SingleChoiceDialogFragment;
import com.piusvelte.sonet.fragment.TimeSettingsDialogFragment;
import com.piusvelte.sonet.fragment.UpdateSettingsDialogFragment;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;

import static com.piusvelte.sonet.Sonet.PRO;
import static com.piusvelte.sonet.Sonet.initAccountSettings;

public class Settings extends FragmentActivity
        implements View.OnClickListener, BaseDialogFragment.OnResultListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_SETTINGS = 0;

    private static final int REQUEST_UPDATE_SETTINGS = 0;
    private static final int REQUEST_NOTIFICATIONS = 1;
    private static final int REQUEST_MARGIN = 2;
    private static final int REQUEST_BUTTON_SETTINGS = 3;
    private static final int REQUEST_NAME_SETTINGS = 4;
    private static final int REQUEST_TIME_SETTINGS = 5;
    private static final int REQUEST_PROFILE_SETTINGS = 6;
    private static final int REQUEST_MESSAGE_SETTINGS = 7;
    private static final int REQUEST_LOADING_SETTINGS = 8;

    private static final String DIALOG_UPDATE_SETTINGS = "dialog:update_settings";
    private static final String DIALOG_NOTIFICATIONS = "dialog:notifications";
    private static final String DIALOG_MARGIN = "dialog:margin";
    private static final String DIALOG_BUTTON_SETTINGS = "dialog:button_settings";
    private static final String DIALOG_NAME_SETTINGS = "dialog:name_settings";
    private static final String DIALOG_TIME_SETTINGS = "dialog:time_settings";
    private static final String DIALOG_PROFILE_SETTINGS = "dialog:profile_settings";
    private static final String DIALOG_MESSAGE_SETTINGS = "dialog:message_settings";
    private static final String DIALOG_LOADING_SETTINGS = "dialog:loading_settings";

    private int mInterval_value = Sonet.default_interval;
    private int mButtons_bg_color_value = Sonet.default_buttons_bg_color;
    private int mButtons_color_value = Sonet.default_buttons_color;
    private int mButtons_textsize_value = Sonet.default_buttons_textsize;
    private int mMessages_bg_color_value = Sonet.default_message_bg_color;
    private int mMessages_color_value = Sonet.default_message_color;
    private int mMessages_textsize_value = Sonet.default_messages_textsize;
    private int mFriend_color_value = Sonet.default_friend_color;
    private int mFriend_textsize_value = Sonet.default_friend_textsize;
    private int mCreated_color_value = Sonet.default_created_color;
    private int mCreated_textsize_value = Sonet.default_created_textsize;
    private int mStatuses_per_account_value = Sonet.default_statuses_per_account;
    private int mMargin_value = Sonet.default_margin;
    private int mProfiles_bg_color_value = Sonet.default_message_bg_color;
    private int mFriend_bg_color_value = Sonet.default_friend_bg_color;
    private int mScrollable_version = 0;
    private boolean mHasButtons_value = Sonet.default_hasButtons;
    private boolean mTime24hr_value = Sonet.default_time24hr;
    private boolean mIcon_value = Sonet.default_hasIcon;
    private boolean mBackgroundUpdate_value = Sonet.default_backgroundUpdate;
    private boolean mSound_value = Sonet.default_sound;
    private boolean mVibrate_value = Sonet.default_vibrate;
    private boolean mLights_value = Sonet.default_lights;
    private boolean mDisplay_profile_value = Sonet.default_include_profile;
    private boolean mInstantUpload_value = Sonet.default_instantUpload;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private String mWidgetSettingsId = null;
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
            AdView adView = new AdView(this, AdSize.BANNER, BuildConfig.GOOGLEAD_ID);
            ((FrameLayout) findViewById(R.id.ad)).addView(adView);
            adView.loadAd(new AdRequest());
        }

        Intent i = getIntent();

        if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        }

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

        if (wp != null) {
            findViewById(R.id.ad).getRootView().setBackgroundDrawable(wp);
        }

        LoadingDialogFragment.newInstance(REQUEST_LOADING_SETTINGS)
                .show(getSupportFragmentManager(), DIALOG_LOADING_SETTINGS);
        getSupportLoaderManager().initLoader(LOADER_SETTINGS, null, this);
    }

    private void updateDatabase(String column, int value) {
        ContentValues values = new ContentValues();
        values.put(column, value);
        getContentResolver().update(Widgets.getContentUri(this), values, Widgets._ID + "=?", new String[] { mWidgetSettingsId });
        setResult(RESULT_OK);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_SETTINGS:
                return new CursorLoader(this,
                        WidgetsSettings.getContentUri(this),
                        new String[] { Widgets._ID,
                                Widgets.WIDGET,
                                Widgets.INTERVAL,
                                Widgets.BUTTONS_BG_COLOR,
                                Widgets.BUTTONS_COLOR,
                                Widgets.BUTTONS_TEXTSIZE,
                                Widgets.MESSAGES_BG_COLOR,
                                Widgets.MESSAGES_COLOR,
                                Widgets.MESSAGES_TEXTSIZE,
                                Widgets.FRIEND_COLOR,
                                Widgets.FRIEND_TEXTSIZE,
                                Widgets.CREATED_COLOR,
                                Widgets.CREATED_TEXTSIZE,
                                Widgets.HASBUTTONS,
                                Widgets.TIME24HR,
                                Widgets.ICON,
                                Widgets.STATUSES_PER_ACCOUNT,
                                Widgets.BACKGROUND_UPDATE,
                                Widgets.SCROLLABLE,
                                Widgets.SOUND,
                                Widgets.VIBRATE,
                                Widgets.LIGHTS,
                                Widgets.DISPLAY_PROFILE,
                                Widgets.INSTANT_UPLOAD,
                                Widgets.MARGIN,
                                Widgets.PROFILES_BG_COLOR,
                                Widgets.FRIEND_BG_COLOR },
                        "(" + Widgets.WIDGET + "=? or " + Widgets.WIDGET + "=?) and " + Widgets.ACCOUNT + "=?",
                        new String[] { Integer.toString(mAppWidgetId), Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long
                                .toString(Sonet.INVALID_ACCOUNT_ID) },
                        Widgets.WIDGET + " DESC, " + Widgets.ACCOUNT + " DESC");

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_SETTINGS:
                DialogFragment dialogFragment = (DialogFragment) getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_SETTINGS);

                if (dialogFragment != null) {
                    dialogFragment.dismiss();
                }

                // determine if we have settings or need to initialize them
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        // insert rows for settings records that are missing
                        mWidgetSettingsId = String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(Widgets._ID)));
                        int widget = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.WIDGET));

                        if (widget == AppWidgetManager.INVALID_APPWIDGET_ID && mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            // the first row is the generic non-widget, non-account, the cursor should only have this record
                            // we need one specific for this widget
                            mWidgetSettingsId = initAccountSettings(this, mAppWidgetId, Sonet.INVALID_ACCOUNT_ID);
                        }

                        // get the settings
                        mInterval_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.INTERVAL));
                        mButtons_bg_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.BUTTONS_BG_COLOR));
                        mButtons_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.BUTTONS_COLOR));
                        mButtons_textsize_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.BUTTONS_TEXTSIZE));
                        mMessages_bg_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.MESSAGES_BG_COLOR));
                        mMessages_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.MESSAGES_COLOR));
                        mMessages_textsize_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.MESSAGES_TEXTSIZE));
                        mFriend_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.FRIEND_COLOR));
                        mFriend_textsize_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.FRIEND_TEXTSIZE));
                        mCreated_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.CREATED_COLOR));
                        mCreated_textsize_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.CREATED_TEXTSIZE));
                        mHasButtons_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.HASBUTTONS)) == 1;
                        mTime24hr_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.TIME24HR)) == 1;
                        mIcon_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.ICON)) == 1;
                        mStatuses_per_account_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.STATUSES_PER_ACCOUNT));
                        mBackgroundUpdate_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.BACKGROUND_UPDATE)) == 1;
                        mScrollable_version = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.SCROLLABLE));
                        mSound_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.SOUND)) == 1;
                        mVibrate_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.VIBRATE)) == 1;
                        mLights_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.LIGHTS)) == 1;
                        mDisplay_profile_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.DISPLAY_PROFILE)) == 1;
                        mInstantUpload_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.INSTANT_UPLOAD)) == 1;
                        mMargin_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.MARGIN));
                        mProfiles_bg_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.PROFILES_BG_COLOR));
                        mFriend_bg_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.FRIEND_BG_COLOR));

                        mBtn_update.setOnClickListener(this);
                        mBtn_notification.setOnClickListener(this);

                        mChk_instantUpload.setChecked(mInstantUpload_value);
                        mChk_instantUpload.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                            @Override
                            public void onCheckedChanged(CompoundButton buttonView,
                                    boolean isChecked) {
                                // facebook only
                                if (isChecked) {
                                    (Toast.makeText(Settings.this, "Currently, the photo will only be uploaded Facebook accounts.",
                                            Toast.LENGTH_LONG)).show();
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
                    } else {
                        // got nothing, init all, Loader should requery
                        initAccountSettings(this, AppWidgetManager.INVALID_APPWIDGET_ID, Sonet.INVALID_ACCOUNT_ID);

                        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            mWidgetSettingsId = initAccountSettings(this, mAppWidgetId, Sonet.INVALID_ACCOUNT_ID);
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onClick(View v) {
        if (v == mBtn_update) {
            // interval
            // statuses per account
            // background
            UpdateSettingsDialogFragment.newInstance(REQUEST_UPDATE_SETTINGS, mInterval_value, mStatuses_per_account_value, mBackgroundUpdate_value)
                    .show(getSupportFragmentManager(), DIALOG_UPDATE_SETTINGS);
        } else if (v == mBtn_notification) {
            // sound
            // light
            // vibrate
            NotificationSettingsDialogFragment.newInstance(mSound_value, mVibrate_value, mLights_value, REQUEST_NOTIFICATIONS)
                    .show(getSupportFragmentManager(), DIALOG_NOTIFICATIONS);
        } else if (v == mBtn_margin) {
            int which = 0;
            String[] items = getResources().getStringArray(R.array.margin_values);

            for (int i = 0; i < items.length; i++) {
                if (Integer.parseInt(items[i]) == mMargin_value) {
                    which = i;
                    break;
                }
            }

            SingleChoiceDialogFragment.newInstance(items, which, REQUEST_MARGIN)
                    .show(getSupportFragmentManager(), DIALOG_MARGIN);
        } else if (v == mBtn_buttons) {
            // enabled
            // bg color
            // color
            // textsize
            ButtonSettingsDialogFragment
                    .newInstance(REQUEST_BUTTON_SETTINGS, mButtons_color_value, mButtons_textsize_value, mButtons_bg_color_value, mHasButtons_value)
                    .show(getSupportFragmentManager(), DIALOG_BUTTON_SETTINGS);
        } else if (v == mBtn_name) {
            // bg color
            // color
            // textsize
            NameSettingsDialogFragment.newInstance(REQUEST_NAME_SETTINGS, mFriend_color_value, mFriend_textsize_value, mFriend_bg_color_value)
                    .show(getSupportFragmentManager(), DIALOG_NAME_SETTINGS);
        } else if (v == mBtn_time) {
            // color
            // textsize
            TimeSettingsDialogFragment.newInstance(REQUEST_TIME_SETTINGS, mCreated_color_value, mCreated_textsize_value, mTime24hr_value)
                    .show(getSupportFragmentManager(), DIALOG_TIME_SETTINGS);
        } else if (v == mBtn_profile) {
            // enabled
            // bg color
            ProfileSettingsDialogFragment.newInstance(REQUEST_PROFILE_SETTINGS, mProfiles_bg_color_value, mDisplay_profile_value)
                    .show(getSupportFragmentManager(), DIALOG_PROFILE_SETTINGS);
        } else if (v == mBtn_message) {
            // color
            // bg color
            // text size
            // icon enabled
            MessageSettingsDialogFragment
                    .newInstance(REQUEST_MESSAGE_SETTINGS, mMessages_color_value, mMessages_textsize_value, mMessages_bg_color_value, mIcon_value)
                    .show(getSupportFragmentManager(), DIALOG_MESSAGE_SETTINGS);
        }
    }

    @Override
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            case REQUEST_UPDATE_SETTINGS:
                if (result == RESULT_OK) {
                    int interval = UpdateSettingsDialogFragment.getUpdateInterval(data, mInterval_value);

                    if (interval != mInterval_value) {
                        mInterval_value = interval;
                        updateDatabase(Widgets.INTERVAL, mInterval_value);
                    }

                    int count = UpdateSettingsDialogFragment.getStatusCount(data, mStatuses_per_account_value);

                    if (count != mStatuses_per_account_value) {
                        mStatuses_per_account_value = count;
                        updateDatabase(Widgets.STATUSES_PER_ACCOUNT, mStatuses_per_account_value);
                    }

                    boolean backgroundUpdates = UpdateSettingsDialogFragment.hasBackgroundUpdate(data, mBackgroundUpdate_value);

                    if (backgroundUpdates != mBackgroundUpdate_value) {
                        mBackgroundUpdate_value = backgroundUpdates;
                        updateDatabase(Widgets.BACKGROUND_UPDATE, mBackgroundUpdate_value ? 1 : 0);
                    }
                }
                break;

            case REQUEST_NOTIFICATIONS:
                if (result == RESULT_OK) {
                    boolean value = NotificationSettingsDialogFragment.hasLights(data, mSound_value);

                    if (value != mSound_value) {
                        mSound_value = value;
                        updateDatabase(Widgets.SOUND, mSound_value ? 1 : 0);
                    }

                    value = NotificationSettingsDialogFragment.hasVibrate(data, mVibrate_value);

                    if (value != mVibrate_value) {
                        mVibrate_value = value;
                        updateDatabase(Widgets.VIBRATE, mVibrate_value ? 1 : 0);
                    }

                    value = NotificationSettingsDialogFragment.hasLights(data, mLights_value);

                    if (value != mLights_value) {
                        mLights_value = value;
                        updateDatabase(Widgets.LIGHTS, mLights_value ? 1 : 0);
                    }
                }
                break;

            case REQUEST_MARGIN:
                if (result == RESULT_OK) {
                    int which = SingleChoiceDialogFragment.getWhich(data, 0);
                    int value = Integer.parseInt(getResources().getStringArray(R.array.margin_values)[which]);

                    if (value != mMargin_value) {
                        mMargin_value = value;
                        updateDatabase(Widgets.MARGIN, mMargin_value);
                    }
                }
                break;

            case REQUEST_BUTTON_SETTINGS:
                if (result == RESULT_OK) {
                    int color = ButtonSettingsDialogFragment.getColor(data, mButtons_color_value);

                    if (color != mButtons_color_value) {
                        mButtons_color_value = color;
                        updateDatabase(Widgets.BUTTONS_COLOR, mButtons_color_value);
                        mBtn_buttons.setTextColor(mButtons_color_value);
                    }

                    int size = ButtonSettingsDialogFragment.getSize(data, mButtons_textsize_value);

                    if (size != mButtons_textsize_value) {
                        mButtons_textsize_value = size;
                        updateDatabase(Widgets.BUTTONS_TEXTSIZE, mButtons_textsize_value);
                        mBtn_buttons.setTextSize(mButtons_textsize_value);
                    }

                    int background = ButtonSettingsDialogFragment.getBackground(data, mButtons_bg_color_value);

                    if (background != mButtons_bg_color_value) {
                        mButtons_bg_color_value = background;
                        updateDatabase(Widgets.BUTTONS_BG_COLOR, mButtons_bg_color_value);
                        mBtn_buttons.setBackgroundColor(mButtons_bg_color_value);
                    }

                    boolean hasButtons = ButtonSettingsDialogFragment.hasButtons(data, mHasButtons_value);

                    if (hasButtons != mHasButtons_value) {
                        mHasButtons_value = hasButtons;
                        updateDatabase(Widgets.HASBUTTONS, mHasButtons_value ? 1 : 0);
                    }
                }
                break;

            case REQUEST_NAME_SETTINGS:
                if (result == RESULT_OK) {
                    int value = NameSettingsDialogFragment.getColor(data, mFriend_color_value);

                    if (value != mFriend_color_value) {
                        mFriend_color_value = value;
                        updateDatabase(Widgets.FRIEND_COLOR, value);
                        mBtn_name.setTextColor(mFriend_color_value);
                    }

                    value = NameSettingsDialogFragment.getSize(data, mFriend_textsize_value);

                    if (value != mFriend_textsize_value) {
                        mFriend_textsize_value = value;
                        updateDatabase(Widgets.FRIEND_TEXTSIZE, mFriend_textsize_value);
                        mBtn_name.setTextSize(mFriend_textsize_value);
                    }

                    value = NameSettingsDialogFragment.getBackground(data, mFriend_bg_color_value);

                    if (value != mFriend_bg_color_value) {
                        mFriend_bg_color_value = value;
                        updateDatabase(Widgets.FRIEND_BG_COLOR, value);
                        mBtn_name.setBackgroundColor(mFriend_bg_color_value);
                        mBtn_time.setBackgroundColor(mFriend_bg_color_value);
                    }
                }
                break;

            case REQUEST_TIME_SETTINGS:
                if (result == RESULT_OK) {
                    int value = TimeSettingsDialogFragment.getColor(data, mCreated_color_value);

                    if (value != mCreated_color_value) {
                        mCreated_color_value = value;
                        updateDatabase(Widgets.CREATED_COLOR, value);
                        mBtn_time.setTextColor(mCreated_color_value);
                    }

                    value = TimeSettingsDialogFragment.getSize(data, mCreated_textsize_value);

                    if (value != mCreated_textsize_value) {
                        mCreated_textsize_value = value;
                        updateDatabase(Widgets.CREATED_TEXTSIZE, mCreated_textsize_value);
                        mBtn_time.setTextSize(mCreated_textsize_value);
                    }

                    boolean time24hr = TimeSettingsDialogFragment.is24hr(data, mTime24hr_value);

                    if (time24hr != mTime24hr_value) {
                        mTime24hr_value = time24hr;
                        updateDatabase(Widgets.TIME24HR, time24hr ? 1 : 0);
                    }
                }
                break;

            case REQUEST_PROFILE_SETTINGS:
                if (result == RESULT_OK) {
                    int background = ProfileSettingsDialogFragment.getBackground(data, mProfiles_bg_color_value);

                    if (background != mProfiles_bg_color_value) {
                        mProfiles_bg_color_value = background;
                        updateDatabase(Widgets.PROFILES_BG_COLOR, background);
                        mBtn_profile.setBackgroundColor(mProfiles_bg_color_value);
                    }

                    boolean profile = ProfileSettingsDialogFragment.hasProfile(data, mDisplay_profile_value);

                    if (profile != mDisplay_profile_value) {
                        mDisplay_profile_value = profile;
                        updateDatabase(Widgets.DISPLAY_PROFILE, profile ? 1 : 0);
                    }
                }
                break;

            case REQUEST_MESSAGE_SETTINGS:
                if (result == RESULT_OK) {
                    int value = MessageSettingsDialogFragment.getColor(data, mMessages_color_value);

                    if (value != mMessages_color_value) {
                        mMessages_color_value = value;
                        updateDatabase(Widgets.MESSAGES_COLOR, value);
                        mBtn_message.setTextColor(mMessages_color_value);
                    }

                    value = MessageSettingsDialogFragment.getSize(data, mMessages_textsize_value);

                    if (value != mMessages_textsize_value) {
                        mMessages_textsize_value = value;
                        updateDatabase(Widgets.MESSAGES_TEXTSIZE, mMessages_textsize_value);
                        mBtn_message.setTextSize(mMessages_textsize_value);
                    }

                    value = MessageSettingsDialogFragment.getBackground(data, mMessages_bg_color_value);

                    if (value != mMessages_bg_color_value) {
                        mMessages_bg_color_value = value;
                        updateDatabase(Widgets.MESSAGES_BG_COLOR, value);
                        mBtn_message.setBackgroundColor(mMessages_bg_color_value);
                    }

                    boolean icon = MessageSettingsDialogFragment.hasIcon(data, mIcon_value);

                    if (icon != mIcon_value) {
                        mIcon_value = icon;
                        updateDatabase(Widgets.ICON, icon ? 1 : 0);
                    }
                }
                break;
        }
    }
}
