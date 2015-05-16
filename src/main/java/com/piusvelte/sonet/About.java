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
import android.appwidget.AppWidgetProviderInfo;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import com.piusvelte.sonet.adapter.AccountProfileAdapter;
import com.piusvelte.sonet.adapter.MenuItemAdapter;
import com.piusvelte.sonet.fragment.ConfirmationDialogFragment;
import com.piusvelte.sonet.fragment.ItemsDialogFragment;
import com.piusvelte.sonet.fragment.WidgetsList;
import com.piusvelte.sonet.loader.AccountsProfilesLoader;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.RESULT_REFRESH;

public class About extends BaseActivity implements LoaderManager.LoaderCallbacks, AdapterView.OnItemClickListener {
    private int[] mAppWidgetIds;
    private AppWidgetManager mAppWidgetManager;
    private boolean mUpdateWidget = false;
    private static final String TAG = "About";

    private static final String FRAGMENT_WIDGETS_LIST = "widgets_list";

    private static final int LOADER_DEFAULT_WIDGET = 0;
    private static final int LOADER_ACCOUNTS = 1;

    private static final String DIALOG_ABOUT = "dialog:about";
    private static final String DIALOG_WIDGETS = "dialog:widgets";

    private static final int REQUEST_WIDGET = 0;

    private DrawerLayout mDrawer;
    private ListView mDrawerOptions;
    private AccountProfileAdapter mAccountsAdapter;
    private MenuItemAdapter mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    List<HashMap<String, String>> mAccounts = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setupAd();

        setupActionBar();
        setupDrawer();

        Fragment widgetsList = getSupportFragmentManager().findFragmentByTag(FRAGMENT_WIDGETS_LIST);

        if (widgetsList == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.widgets_list_container, new WidgetsList(), FRAGMENT_WIDGETS_LIST)
                    .commit();
        }

        getSupportLoaderManager().initLoader(LOADER_DEFAULT_WIDGET, null, this);
        getSupportLoaderManager().initLoader(LOADER_ACCOUNTS, null, this);
    }

    private void setupDrawer() {
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawer,
                R.string.drawer_open,
                R.string.drawer_closed) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0);
            }
        };
        mDrawer.setDrawerListener(mDrawerToggle);

        GridView accounts = (GridView) findViewById(R.id.drawer_accounts);

        mAccountsAdapter = new AccountProfileAdapter(this,
                mAccounts,
                R.layout.account_profile,
                new String[] { Accounts.SID,
                        Accounts.SERVICE },
                new int[] { R.id.profile,
                        R.id.icon });

        accounts.setAdapter(mAccountsAdapter);

        mDrawerOptions = (ListView) findViewById(R.id.drawer_options);

        mAdapter = new MenuItemAdapter(this, R.menu.navigation_about);
        mDrawerOptions.setAdapter(mAdapter);
        mDrawerOptions.setItemChecked(0, true);
        mDrawerOptions.setOnItemClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mDrawerOptions.setFitsSystemWindows(true);
        }
    }

    private void setupActionBar() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                actionBar.setHomeButtonEnabled(true);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
            switch (item.getItemId()) {
                case R.id.menu_feed:
                    // TODO replace content with WidgetsList; currently the only content
                    return true;

                case R.id.menu_about_refresh:
                    // TODO handle this in WidgetsList, show loading, cleared onLoadFinished
                    startService(new Intent(this, SonetService.class)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID).setAction(ACTION_REFRESH));
                    return true;

                case R.id.menu_about_accounts_and_settings:
                    // TODO replace fragment instead of using Activity
                    startActivity(new Intent(this, ManageAccounts.class)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
                    return true;

                case R.id.menu_about_default_settings:
                    // TODO replace fragment instead of using Activity
                    startActivityForResult(new Intent(this, Settings.class), RESULT_REFRESH);
                    return true;

                case R.id.menu_about_refresh_widgets:
                    // TODO show loading, cleared onLoadFinished
                    startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
                    return true;

                case R.id.menu_about_notifications:
                    // TODO replace fragment instead of using Activity
                    startActivity(new Intent(this, SonetNotifications.class));
                    return true;

                case R.id.menu_about_widget_settings:
                    // TODO replace fragment instead of using Activity
                    if (mAppWidgetIds.length > 0) {
                        String[] widgets = new String[mAppWidgetIds.length];

                        for (int i = 0, i2 = mAppWidgetIds.length; i < i2; i++) {
                            AppWidgetProviderInfo info = mAppWidgetManager.getAppWidgetInfo(mAppWidgetIds[i]);
                            String providerName = info.provider.getClassName();
                            widgets[i] = Integer.toString(mAppWidgetIds[i]) + " (" + providerName + ")";
                        }

                        ItemsDialogFragment.newInstance(widgets, REQUEST_WIDGET)
                                .show(getSupportFragmentManager(), DIALOG_WIDGETS);
                    } else {
                        Toast.makeText(this, getString(R.string.nowidgets), Toast.LENGTH_LONG).show();
                    }

                    return true;

                case R.id.menu_about_about:
                    ConfirmationDialogFragment.newInstance(R.string.about_title, R.string.about, -1)
                            .show(getSupportFragmentManager(), DIALOG_ABOUT);
                    return true;

                default:
                    return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAppWidgetIds = new int[0];
        // validate appwidgetids from appwidgetmanager
        mAppWidgetManager = AppWidgetManager.getInstance(About.this);
        mAppWidgetIds = Sonet.getWidgets(getApplicationContext(), mAppWidgetManager);
    }

    @Override
    protected void onPause() {
        if (mUpdateWidget) {
            (Toast.makeText(getApplicationContext(), getString(R.string.refreshing), Toast.LENGTH_LONG)).show();
            startService(new Intent(this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
        }

        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_REFRESH:
                if (resultCode == RESULT_OK) {
                    mUpdateWidget = true;
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            case REQUEST_WIDGET:
                startActivity(new Intent(this, ManageAccounts.class)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[ItemsDialogFragment.getWhich(data, 0)]));
                break;
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_DEFAULT_WIDGET:
                return new CursorLoader(this, WidgetsSettings.getContentUri(this),
                        new String[] { Widgets.DISPLAY_PROFILE },
                        Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?",
                        new String[] { String.valueOf(AppWidgetManager.INVALID_APPWIDGET_ID),
                                String.valueOf(Accounts.INVALID_ACCOUNT_ID) },
                        null);

            case LOADER_ACCOUNTS:
                return new AccountsProfilesLoader(this);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object result) {
        switch (loader.getId()) {
            case LOADER_DEFAULT_WIDGET:
                if (result instanceof Cursor) {
                    Cursor cursor = (Cursor) result;

                    if (cursor.moveToFirst()) {
                        // TODO used to determine showing profile in WidgetsList
                        //boolean showProfile = cursor.getInt(cursor.getColumnIndex(Widgets.DISPLAY_PROFILE)) != 0;
                    } else {
                        // initialize account settings
                        ContentValues values = new ContentValues();
                        values.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
                        values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
                        getContentResolver().insert(Widgets.getContentUri(About.this), values).getLastPathSegment();
                    }
                } else {
                    // initialize account settings
                    ContentValues values = new ContentValues();
                    values.put(Widgets.WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
                    values.put(Widgets.ACCOUNT, Sonet.INVALID_ACCOUNT_ID);
                    getContentResolver().insert(Widgets.getContentUri(About.this), values).getLastPathSegment();
                }
                break;

            case LOADER_ACCOUNTS:
                mAccounts.clear();

                if (result instanceof List<?>) {
                    mAccounts.addAll((List<HashMap<String, String>>) result);
                }

                mAccountsAdapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        switch (loader.getId()) {
            case LOADER_ACCOUNTS:
                mAccounts.clear();
                mAccountsAdapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mDrawerOptions.setItemChecked(position, true);
        mDrawer.closeDrawers();
        onOptionsItemSelected(mAdapter.getItem(position));
    }
}
