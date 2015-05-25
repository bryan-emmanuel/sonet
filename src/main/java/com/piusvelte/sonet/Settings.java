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

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.piusvelte.sonet.fragment.NotificationSettingsDialogFragment;
import com.piusvelte.sonet.fragment.TimeSettingsDialogFragment;
import com.piusvelte.sonet.fragment.UpdateSettingsDialogFragment;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;

import static com.piusvelte.sonet.Sonet.initAccountSettings;

@Deprecated
public class Settings extends BaseActivity
        implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_SETTINGS = 0;

    private static final int REQUEST_UPDATE_SETTINGS = 0;
    private static final int REQUEST_NOTIFICATIONS = 1;
    private static final int REQUEST_TIME_SETTINGS = 2;

    private static final String DIALOG_UPDATE_SETTINGS = "dialog:update_settings";
    private static final String DIALOG_NOTIFICATIONS = "dialog:notifications";
    private static final String DIALOG_TIME_SETTINGS = "dialog:time_settings";

    private int mInterval_value = Sonet.default_interval;
    private int mStatuses_per_account_value = Sonet.default_statuses_per_account;
    private boolean mTime24hr_value = Sonet.default_time24hr;
    private boolean mBackgroundUpdate_value = Sonet.default_backgroundUpdate;
    private boolean mSound_value = Sonet.default_sound;
    private boolean mVibrate_value = Sonet.default_vibrate;
    private boolean mLights_value = Sonet.default_lights;
    private boolean mInstantUpload_value = Sonet.default_instantUpload;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private String mWidgetSettingsId = null;
    private Button mBtn_update;
    private Button mBtn_notification;
    private CheckBox mChk_instantUpload;
    private TextView mBtn_name;
    private TextView mBtn_time;
    private TextView mBtn_message;
    private View mLoadingView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.preferences);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setupAd();

        Intent i = getIntent();

        if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        }

        mBtn_update = (Button) findViewById(R.id.settings_update);
        mBtn_notification = (Button) findViewById(R.id.settings_notification);
        mChk_instantUpload = (CheckBox) findViewById(R.id.instantupload);
        mBtn_name = (TextView) findViewById(R.id.friend);
        mBtn_time = (TextView) findViewById(R.id.created);
        mBtn_message = (TextView) findViewById(R.id.message);
        mLoadingView = findViewById(R.id.loading);

        mLoadingView.setVisibility(View.VISIBLE);
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
                                Widgets.TIME24HR,
                                Widgets.STATUSES_PER_ACCOUNT,
                                Widgets.BACKGROUND_UPDATE,
                                Widgets.SOUND,
                                Widgets.VIBRATE,
                                Widgets.LIGHTS,
                                Widgets.INSTANT_UPLOAD },
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
                mLoadingView.setVisibility(View.GONE);

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
                        mTime24hr_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.TIME24HR)) == 1;
                        mStatuses_per_account_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.STATUSES_PER_ACCOUNT));
                        mBackgroundUpdate_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.BACKGROUND_UPDATE)) == 1;
                        mSound_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.SOUND)) == 1;
                        mVibrate_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.VIBRATE)) == 1;
                        mLights_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.LIGHTS)) == 1;
                        mInstantUpload_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.INSTANT_UPLOAD)) == 1;

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

                        mBtn_time.setOnClickListener(this);
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
            UpdateSettingsDialogFragment.newInstance(REQUEST_UPDATE_SETTINGS, mInterval_value, mBackgroundUpdate_value)
                    .show(getSupportFragmentManager(), DIALOG_UPDATE_SETTINGS);
        } else if (v == mBtn_notification) {
            // sound
            // light
            // vibrate
            NotificationSettingsDialogFragment.newInstance(mSound_value, mVibrate_value, mLights_value, REQUEST_NOTIFICATIONS)
                    .show(getSupportFragmentManager(), DIALOG_NOTIFICATIONS);
        } else if (v == mBtn_time) {
            // color
            // textsize
            TimeSettingsDialogFragment.newInstance(REQUEST_TIME_SETTINGS, mTime24hr_value)
                    .show(getSupportFragmentManager(), DIALOG_TIME_SETTINGS);
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

            case REQUEST_TIME_SETTINGS:
                if (result == RESULT_OK) {
                    boolean time24hr = TimeSettingsDialogFragment.is24hr(data, mTime24hr_value);

                    if (time24hr != mTime24hr_value) {
                        mTime24hr_value = time24hr;
                        updateDatabase(Widgets.TIME24HR, time24hr ? 1 : 0);
                    }
                }
                break;
        }
    }
}
