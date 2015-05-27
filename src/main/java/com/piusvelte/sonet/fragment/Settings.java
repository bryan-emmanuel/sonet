package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.piusvelte.eidos.Eidos;
import com.piusvelte.sonet.OAuthLogin;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetService;
import com.piusvelte.sonet.adapter.MenuItemAdapter;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;
import com.piusvelte.sonet.social.Client;
import com.piusvelte.sonet.util.CircleTransformation;
import com.squareup.picasso.Picasso;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.RSS;
import static com.piusvelte.sonet.Sonet.SMS;

/**
 * Created by bemmanuel on 5/17/15.
 */
public class Settings extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int INVALID_SERVICE = -1;

    private static final int LOADER_WIDGET_SETTINGS = 0;

    private static final String DIALOG_NOTIFICATION_SETTINGS = "dialog:notification_settings";
    private static final String DIALOG_TIME_SETTINGS = "dialog:time_settings";
    private static final String DIALOG_UPDATE_SETTINGS = "dialog:update_settings";

    private static final int REQUEST_AUTHENTICATE = 0;
    private static final int REQUEST_NOTIFICATION_SETTINGS = 1;
    private static final int REQUEST_TIME_SETTINGS = 2;
    private static final int REQUEST_UPDATE_SETTINGS = 3;

    private View mLoadingView;

    private String mWidgetAccountSettingsId = null;
    private boolean mTime24hr_value = Sonet.default_time24hr;
    private boolean mSound_value = Sonet.default_sound;
    private boolean mVibrate_value = Sonet.default_vibrate;
    private boolean mLights_value = Sonet.default_lights;
    private boolean mBackgroundUpdate_value = Sonet.default_backgroundUpdate;
    private int mInterval_value = Sonet.default_interval;
    private boolean mInstantUpload_value = Sonet.default_instantUpload;

    /** account specific settings */
    public static Settings newInstance(long accountId, int service, String profileUrl, String username) {
        Settings settings = new Settings();

        Bundle args = new Bundle();
        args.putLong(Accounts._ID, accountId);
        args.putInt(Accounts.SERVICE, service);
        args.putString(Entity.PROFILE_URL, profileUrl);
        args.putString(Accounts.USERNAME, username);
        settings.setArguments(args);

        return settings;
    }

    /** app-wide settings */
    public static Settings newInstance() {
        Settings settings = newInstance(Sonet.INVALID_ACCOUNT_ID, INVALID_SERVICE, null, null);
        settings.setRetainInstance(true);
        return settings;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.account_detail, container, false);
    }

    private boolean isAccountSettings() {
        return getArguments().getLong(Accounts._ID) != Sonet.INVALID_ACCOUNT_ID;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        Picasso picasso = Picasso.with(getActivity());

        ImageView profile = (ImageView) view.findViewById(R.id.profile);

        String url = args.getString(Entity.PROFILE_URL);

        if (!TextUtils.isEmpty(url)) {
            picasso.load(url)
                    .transform(new CircleTransformation())
                    .into(profile);
        } else {
            picasso.load(R.drawable.ic_account_box_grey600_48dp)
                    .transform(new CircleTransformation())
                    .into(profile);
        }

        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        TextView name = (TextView) view.findViewById(R.id.friend);

        int service = args.getInt(Accounts.SERVICE);
        int iconResId;
        String nameText;
        int menuResId;

        if (service == INVALID_SERVICE) {
            iconResId = R.drawable.icon;
            nameText = getString(R.string.settings);
            menuResId = R.menu.menu_settings;
        } else {
            Client.Network network = Client.Network.get(args.getInt(Accounts.SERVICE));
            iconResId = network.getIcon();
            nameText = network + ": " + args.getString(Accounts.USERNAME);
            menuResId = R.menu.menu_account_detail;
        }

        picasso.load(iconResId)
                .into(icon);

        name.setText(nameText);

        setListAdapter(new MenuItemAdapter(getActivity(),
                menuResId,
                android.R.layout.simple_list_item_1));

        mLoadingView = view.findViewById(R.id.loading);
        mLoadingView.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(LOADER_WIDGET_SETTINGS, getArguments(), this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        int menuItemResId = (int) id;
        switch (menuItemResId) {
            case R.id.menu_background_update:
                UpdateSettingsDialogFragment.newInstance(REQUEST_UPDATE_SETTINGS,
                        mInterval_value,
                        mBackgroundUpdate_value)
                        .show(getChildFragmentManager(),
                                DIALOG_UPDATE_SETTINGS);
                break;

            case R.id.menu_notifications:
                NotificationSettingsDialogFragment.newInstance(mSound_value,
                        mVibrate_value,
                        mLights_value,
                        REQUEST_NOTIFICATION_SETTINGS)
                        .show(getChildFragmentManager(),
                                DIALOG_NOTIFICATION_SETTINGS);
                break;

            case R.id.menu_instant_upload:
                // TODO make this change visible
                mInstantUpload_value = !mInstantUpload_value;
                l.setItemChecked(position, mInstantUpload_value);
                updateDatabase(Widgets.INSTANT_UPLOAD, mInstantUpload_value);
                break;

            case R.id.menu_time:
                TimeSettingsDialogFragment.newInstance(REQUEST_TIME_SETTINGS, mTime24hr_value)
                        .show(getChildFragmentManager(), DIALOG_TIME_SETTINGS);
                break;

            case R.id.menu_authenticate:
                int service = getArguments().getInt(Accounts.SERVICE);

                if (service != SMS && service != RSS) {
                    startActivityForResult(new Intent(getActivity(), OAuthLogin.class)
                                    .putExtra(Accounts.SERVICE, service)
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                                    .putExtra(Sonet.EXTRA_ACCOUNT_ID, id),
                            REQUEST_AUTHENTICATE);
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_AUTHENTICATE:
                if (resultCode == Activity.RESULT_OK) {
                    Eidos.requestBackup(getActivity());
                    getActivity().startService(new Intent(getActivity(), SonetService.class)
                            .setAction(ACTION_REFRESH)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { AppWidgetManager.INVALID_APPWIDGET_ID }));
                }
                break;

            case REQUEST_NOTIFICATION_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
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

            case REQUEST_TIME_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
                    boolean time24hr = TimeSettingsDialogFragment.is24hr(data, mTime24hr_value);

                    if (time24hr != mTime24hr_value) {
                        mTime24hr_value = time24hr;
                        updateDatabase(Widgets.TIME24HR, time24hr ? 1 : 0);
                    }
                }
                break;

            case REQUEST_UPDATE_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
                    int interval = UpdateSettingsDialogFragment.getUpdateInterval(data, mInterval_value);

                    if (interval != mInterval_value) {
                        mInterval_value = interval;
                        updateDatabase(Widgets.INTERVAL, mInterval_value);
                    }

                    boolean backgroundUpdates = UpdateSettingsDialogFragment.hasBackgroundUpdate(data, mBackgroundUpdate_value);

                    if (backgroundUpdates != mBackgroundUpdate_value) {
                        mBackgroundUpdate_value = backgroundUpdates;
                        updateDatabase(Widgets.BACKGROUND_UPDATE, mBackgroundUpdate_value ? 1 : 0);
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_WIDGET_SETTINGS:
                return new CursorLoader(getActivity(),
                        WidgetsSettings.getContentUri(getActivity()),
                        new String[] { Widgets._ID,
                                Widgets.ACCOUNT,
                                Widgets.TIME24HR,
                                Widgets.SOUND,
                                Widgets.VIBRATE,
                                Widgets.LIGHTS,
                                Widgets.BACKGROUND_UPDATE,
                                Widgets.INSTANT_UPLOAD },
                        Widgets.WIDGET + "=? and (" + Widgets.ACCOUNT + "=? or " + Widgets.ACCOUNT + "=?)",
                        new String[] { Integer.toString(AppWidgetManager.INVALID_APPWIDGET_ID),
                                Long.toString(args.getLong(Accounts._ID)),
                                Long.toString(Sonet.INVALID_ACCOUNT_ID) },
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
                        long account = cursor.getLong(cursor.getColumnIndexOrThrow(Widgets.ACCOUNT));

                        if (account == Accounts.INVALID_ACCOUNT_ID && isAccountSettings()) {
                            // the first row is non-account, insert the account specific row
                            // this id is used, copying any settings changes to this specific record
                            mWidgetAccountSettingsId = Sonet.initAccountSettings(getActivity(),
                                    AppWidgetManager.INVALID_APPWIDGET_ID,
                                    getArguments().getLong(Accounts._ID));
                        }

                        // get the settings
                        mTime24hr_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.TIME24HR)) == 1;
                        mSound_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.SOUND)) == 1;
                        mVibrate_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.VIBRATE)) == 1;
                        mLights_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.LIGHTS)) == 1;
                        mBackgroundUpdate_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.BACKGROUND_UPDATE)) == 1;
                        mInstantUpload_value = cursor.getInt(cursor.getColumnIndexOrThrow(Widgets.INSTANT_UPLOAD)) == 1;
                    } else {
                        // got nothing, init all, the Loader should requery
                        Sonet.initAccountSettings(getActivity(), AppWidgetManager.INVALID_APPWIDGET_ID, Sonet.INVALID_ACCOUNT_ID);
                        mWidgetAccountSettingsId = Sonet.initAccountSettings(getActivity(),
                                AppWidgetManager.INVALID_APPWIDGET_ID,
                                getArguments().getLong(Accounts._ID));
                    }
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void updateDatabase(String column, boolean value) {
        updateDatabase(column, value ? 1 : 0);
    }

    private void updateDatabase(String column, int value) {
        // TODO update via Loader
        ContentValues values = new ContentValues();
        values.put(column, value);
        getActivity().getContentResolver()
                .update(Widgets.getContentUri(getActivity()),
                        values,
                        Widgets._ID + "=?",
                        new String[] { mWidgetAccountSettingsId });
    }
}
