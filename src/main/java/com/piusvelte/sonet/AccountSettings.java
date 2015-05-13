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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.piusvelte.sonet.fragment.MessageSettingsDialogFragment;
import com.piusvelte.sonet.fragment.NameSettingsDialogFragment;
import com.piusvelte.sonet.fragment.NotificationSettingsDialogFragment;
import com.piusvelte.sonet.fragment.ProfileSettingsDialogFragment;
import com.piusvelte.sonet.fragment.SingleChoiceDialogFragment;
import com.piusvelte.sonet.fragment.TimeSettingsDialogFragment;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;

import static com.piusvelte.sonet.Sonet.PRO;
import static com.piusvelte.sonet.Sonet.initAccountSettings;

public class AccountSettings extends BaseActivity
        implements OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_WIDGET_SETTINGS = 0;

    private static final String DIALOG_COUNTS = "dialog:counts";
    private static final String DIALOG_NOTIFICATION_SETTINGS = "dialog:notification_settings";
    private static final String DIALOG_NAME_SETTINGS = "dialog:name_settings";
    private static final String DIALOG_TIME_SETTINGS = "dialog:time_settings";
    private static final String DIALOG_PROFILE_SETTINGS = "dialog:profile_settings";
    private static final String DIALOG_MESSAGE_SETTINGS = "dialog:message_settings";

    private static final int REQUEST_COUNTS = 0;
    private static final int REQUEST_NOTIFICATION_SETTINGS = 1;
    private static final int REQUEST_NAME_SETTINGS = 2;
    private static final int REQUEST_TIME_SETTINGS = 3;
    private static final int REQUEST_PROFILE_SETTINGS = 4;
    private static final int REQUEST_MESSAGE_SETTINGS = 5;

    private int mMessages_bg_color_value = Sonet.default_message_bg_color;
    private int mMessages_color_value = Sonet.default_message_color;
    private int mMessages_textsize_value = Sonet.default_messages_textsize;
    private int mFriend_color_value = Sonet.default_friend_color;
    private int mFriend_textsize_value = Sonet.default_friend_textsize;
    private int mCreated_color_value = Sonet.default_created_color;
    private int mCreated_textsize_value = Sonet.default_created_textsize;
    private int mStatuses_per_account_value = Sonet.default_statuses_per_account;
    private int mFriend_bg_color_value = Sonet.default_friend_bg_color;
    private int mScrollable_version = 0;
    private boolean mTime24hr_value = Sonet.default_time24hr;
    private boolean mIcon_value = Sonet.default_hasIcon;
    private boolean mSound_value = Sonet.default_sound;
    private boolean mVibrate_value = Sonet.default_vibrate;
    private boolean mLights_value = Sonet.default_lights;
    private boolean mDisplay_profile_value = Sonet.default_include_profile;
    private Button mStatuses_per_account;
    private Button mBtn_notification;
    private TextView mBtn_name;
    private TextView mBtn_time;
    private ImageView mBtn_profile;
    private TextView mBtn_message;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private long mAccountId = Sonet.INVALID_ACCOUNT_ID;
    private String mWidgetAccountSettingsId = null;
    private View mLoadingView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.account_preferences);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        if (!getPackageName().toLowerCase().contains(PRO)) {
            AdView adView = new AdView(this, AdSize.BANNER, BuildConfig.GOOGLEAD_ID);
            ((FrameLayout) findViewById(R.id.ad)).addView(adView);
            adView.loadAd(new AdRequest());
        }

        Intent i = getIntent();

        if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        }

        if (i.hasExtra(Sonet.EXTRA_ACCOUNT_ID)) {
            mAccountId = i.getLongExtra(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID);
        }

        mStatuses_per_account = (Button) findViewById(R.id.statuses_per_account);
        mBtn_notification = (Button) findViewById(R.id.settings_notification);
        mBtn_name = (TextView) findViewById(R.id.friend);
        mBtn_time = (TextView) findViewById(R.id.created);
        mBtn_profile = (ImageView) findViewById(R.id.profile);
        mBtn_message = (TextView) findViewById(R.id.message);
        mLoadingView = findViewById(R.id.loading);

        Drawable wp = WallpaperManager.getInstance(getApplicationContext()).getDrawable();

        if (wp != null) {
            findViewById(R.id.ad).getRootView().setBackgroundDrawable(wp);
        }

        mLoadingView.setVisibility(View.VISIBLE);
        getSupportLoaderManager().initLoader(LOADER_WIDGET_SETTINGS, null, this);
    }

    private void updateDatabase(String column, int value) {
        ContentValues values = new ContentValues();
        values.put(column, value);
        getContentResolver().update(Widgets.getContentUri(this), values, Widgets._ID + "=?", new String[] { mWidgetAccountSettingsId });
        setResult(RESULT_OK);
    }

    @Override
    public void onClick(View v) {
        if (v == mStatuses_per_account) {
            // interval
            // statuses per account
            // background
            int which = 0;
            String[] values = getResources().getStringArray(R.array.status_counts);

            for (int i = 0; i < values.length; i++) {
                if (Integer.parseInt(values[i]) == mStatuses_per_account_value) {
                    which = i;
                    break;
                }
            }

            SingleChoiceDialogFragment.newInstance(values, which, REQUEST_COUNTS)
                    .show(getSupportFragmentManager(), DIALOG_COUNTS);
        } else if (v == mBtn_notification) {
            NotificationSettingsDialogFragment.newInstance(mSound_value, mVibrate_value, mLights_value, REQUEST_NOTIFICATION_SETTINGS)
                    .show(getSupportFragmentManager(), DIALOG_NOTIFICATION_SETTINGS);
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
            ProfileSettingsDialogFragment.newInstance(REQUEST_PROFILE_SETTINGS, mDisplay_profile_value)
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_WIDGET_SETTINGS:
                return new CursorLoader(this,
                        WidgetsSettings.getContentUri(this),
                        new String[] { Widgets._ID,
                                Widgets.WIDGET,
                                Widgets.ACCOUNT,
                                Widgets.MESSAGES_COLOR,
                                Widgets.MESSAGES_TEXTSIZE,
                                Widgets.FRIEND_COLOR,
                                Widgets.FRIEND_TEXTSIZE,
                                Widgets.CREATED_COLOR,
                                Widgets.CREATED_TEXTSIZE,
                                Widgets.TIME24HR,
                                Widgets.MESSAGES_BG_COLOR,
                                Widgets.ICON,
                                Widgets.STATUSES_PER_ACCOUNT,
                                Widgets.SCROLLABLE,
                                Widgets.SOUND,
                                Widgets.VIBRATE,
                                Widgets.LIGHTS,
                                Widgets.FRIEND_BG_COLOR },
                        "(" + Widgets.WIDGET + "=? or " + Widgets.WIDGET + "=?) and (" + Widgets.ACCOUNT + "=? or " + Widgets.ACCOUNT + "=?)",
                        new String[] { Integer.toString(mAppWidgetId), Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long
                                .toString(mAccountId), Long.toString(Sonet.INVALID_ACCOUNT_ID) },
                        Widgets.WIDGET + " DESC, " + Widgets.ACCOUNT + " DESC");

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_WIDGET_SETTINGS:
                mLoadingView.setVisibility(View.GONE);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        // insert rows for settings records that are missing
                        mWidgetAccountSettingsId = String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(Widgets._ID)));
                        int widget = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.WIDGET));
                        long account = cursor.getLong(cursor.getColumnIndexOrThrow(Widgets.ACCOUNT));

                        if (widget == AppWidgetManager.INVALID_APPWIDGET_ID && mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            // the first row is the generic non-widget, non-account, the cursor should only have this record
                            initAccountSettings(this, mAppWidgetId, Sonet.INVALID_ACCOUNT_ID);
                        }

                        if (account == Accounts.INVALID_ACCOUNT_ID && mAccountId != Accounts.INVALID_ACCOUNT_ID) {
                            // the first row is non-account, insert the account specific row
                            // this id is used, copying any settings changes to this specific record
                            mWidgetAccountSettingsId = initAccountSettings(this, mAppWidgetId, mAccountId);
                        }

                        // get the settings
                        mMessages_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.MESSAGES_COLOR));
                        mMessages_textsize_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.MESSAGES_TEXTSIZE));
                        mFriend_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.FRIEND_COLOR));
                        mFriend_textsize_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.FRIEND_TEXTSIZE));
                        mCreated_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.CREATED_COLOR));
                        mCreated_textsize_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.CREATED_TEXTSIZE));
                        mTime24hr_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.TIME24HR)) == 1;
                        mMessages_bg_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.MESSAGES_BG_COLOR));
                        mIcon_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.ICON)) == 1;
                        mStatuses_per_account_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.STATUSES_PER_ACCOUNT));
                        mScrollable_version = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.SCROLLABLE));
                        mSound_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.SOUND)) == 1;
                        mVibrate_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.VIBRATE)) == 1;
                        mLights_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.LIGHTS)) == 1;
                        mFriend_bg_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.FRIEND_BG_COLOR));

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

                        mBtn_profile.setBackgroundColor(mFriend_bg_color_value);
                        mBtn_profile.setOnClickListener(this);

                        mBtn_message.setBackgroundColor(mMessages_bg_color_value);
                        mBtn_message.setTextColor(mMessages_color_value);
                        mBtn_message.setTextSize(mMessages_textsize_value);
                        mBtn_message.setOnClickListener(this);
                    } else {
                        // got nothing, init all, the Loader should requery
                        initAccountSettings(this, AppWidgetManager.INVALID_APPWIDGET_ID, Sonet.INVALID_ACCOUNT_ID);

                        if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            initAccountSettings(this, mAppWidgetId, Sonet.INVALID_ACCOUNT_ID);
                        }

                        mWidgetAccountSettingsId = initAccountSettings(this, mAppWidgetId, mAccountId);
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            case REQUEST_COUNTS:
                if (result == RESULT_OK) {
                    int which = SingleChoiceDialogFragment.getWhich(data, 0);
                    mStatuses_per_account_value = Integer.parseInt(getResources().getStringArray(R.array.status_counts)[which]);
                    updateDatabase(Widgets.STATUSES_PER_ACCOUNT, mStatuses_per_account_value);
                }
                break;

            case REQUEST_NOTIFICATION_SETTINGS:
                if (result == RESULT_OK) {
                    boolean value = NotificationSettingsDialogFragment.hasSound(data, mSound_value);

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
