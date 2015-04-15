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
import static com.piusvelte.sonet.Sonet.initAccountSettings;

import com.google.ads.*;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;

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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AccountSettings extends FragmentActivity implements OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_WIDGET_SETTINGS = 0;

    private int mMessages_bg_color_value = Sonet.default_message_bg_color;
    private int mMessages_color_value = Sonet.default_message_color;
    private int mMessages_textsize_value = Sonet.default_messages_textsize;
    private int mFriend_color_value = Sonet.default_friend_color;
    private int mFriend_textsize_value = Sonet.default_friend_textsize;
    private int mCreated_color_value = Sonet.default_created_color;
    private int mCreated_textsize_value = Sonet.default_created_textsize;
    private int mStatuses_per_account_value = Sonet.default_statuses_per_account;
    private int mProfiles_bg_color_value = Sonet.default_message_bg_color;
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
            AdView adView = new AdView(this, AdSize.BANNER, BuildConfig.GOOGLEAD_ID);
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

        mStatuses_per_account = (Button) findViewById(R.id.statuses_per_account);
        mBtn_notification = (Button) findViewById(R.id.settings_notification);
        mBtn_name = (Button) findViewById(R.id.settings_name);
        mBtn_time = (Button) findViewById(R.id.settings_time);
        mBtn_profile = (ImageButton) findViewById(R.id.settings_profile);
        mBtn_message = (Button) findViewById(R.id.settings_message);

        Drawable wp = WallpaperManager.getInstance(getApplicationContext()).getDrawable();

        if (wp != null) {
            findViewById(R.id.ad).getRootView().setBackgroundDrawable(wp);
        }

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

        // TODO indicate loading to the user
        getSupportLoaderManager().initLoader(LOADER_WIDGET_SETTINGS, null, this);
    }

    @Override
    protected void onPause() {
        if ((mDialog != null) && mDialog.isShowing()) {
            mDialog.cancel();
        }

        super.onPause();
    }

    private void updateDatabase(String column, int value) {
        ContentValues values = new ContentValues();
        values.put(column, value);
        getContentResolver().update(Widgets.getContentUri(this), values, Widgets._ID + "=?", new String[]{mWidgetAccountSettingsId});
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
                        }
                    }, mFriend_bg_color_value);
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
                            }
                        }, mFriend_color_value);
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
                            }
                        }, mCreated_color_value);
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
                    mTime24hr_value = isChecked;
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
                        }
                    }, mProfiles_bg_color_value);
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
                        }
                    }, mMessages_bg_color_value);
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
                            }
                        }, mMessages_color_value);
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_WIDGET_SETTINGS:
                return new CursorLoader(this,
                        WidgetsSettings.getContentUri(this),
                        new String[]{Widgets._ID,
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
                                Widgets.PROFILES_BG_COLOR,
                                Widgets.FRIEND_BG_COLOR},
                        "(" + Widgets.WIDGET + "=? or " + Widgets.WIDGET + "=?) and (" + Widgets.ACCOUNT + "=? or " + Widgets.ACCOUNT + "=?)",
                        new String[]{Integer.toString(mAppWidgetId), Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID), Long.toString(mAccountId), Long.toString(Sonet.INVALID_ACCOUNT_ID)},
                        Widgets.WIDGET + " DESC, " + Widgets.ACCOUNT + " DESC");

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_WIDGET_SETTINGS:
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
                        mProfiles_bg_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.PROFILES_BG_COLOR));
                        mFriend_bg_color_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.FRIEND_BG_COLOR));
                    } else {
                        // got nothing, init all
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
}
