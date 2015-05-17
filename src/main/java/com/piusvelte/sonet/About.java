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
import android.support.v4.app.DialogFragment;
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
import android.widget.LinearLayout;
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
import com.piusvelte.sonet.service.AccountUpdateService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.INVALID_ACCOUNT_ID;
import static com.piusvelte.sonet.Sonet.RESULT_REFRESH;

public class About extends BaseActivity implements LoaderManager.LoaderCallbacks, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {
    private int[] mAppWidgetIds;
    private AppWidgetManager mAppWidgetManager;
    private boolean mUpdateWidget = false;
    private static final String TAG = "About";

    private static final String FRAGMENT_WIDGETS_LIST = "widgets_list";

    private static final int LOADER_DEFAULT_WIDGET = 0;
    private static final int LOADER_ACCOUNTS = 1;

    private static final String DIALOG_ABOUT = "dialog:about";
    private static final String DIALOG_WIDGETS = "dialog:widgets";
    private static final String DIALOG_CONFIRM_AUTHENTICATE_ACCOUNT = "dialog:confirm_authenticate_account";
    private static final String DIALOG_CONFIRM_REMOVE_ACCOUNT = "dialog:confirm_remove_account";
    private static final String DIALOG_CHOOSE_NETWORK = "dialog:choose_network";

    private static final int REQUEST_WIDGET = 0;
    private static final int REQUEST_CONFIRM_AUTHENTICATE_ACCOUNT = 1;
    private static final int REQUEST_CONFIRM_REMOVE_ACCOUNT = 2;
    private static final int REQUEST_AUTHENTICATE_ACCOUNT = 3;
    private static final int REQUEST_CHOOSE_NETWORK = 4;
    private static final int REQUEST_ADD_ACCOUNT = 5;

    private DrawerLayout mDrawer;
    private GridView mDrawerAccounts;
    private ListView mDrawerPrimary;
    private ListView mDrawerSecondary;
    private AccountProfileAdapter mDrawerAccountsAdapter;
    private MenuItemAdapter mDrawerPrimaryAdapter;
    private MenuItemAdapter mDrawerSecondaryAdapter;
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

        LinearLayout drawerContainer = (LinearLayout) mDrawer.findViewById(R.id.drawer_container);

        mDrawerAccounts = (GridView) drawerContainer.findViewById(R.id.drawer_accounts);
        mDrawerAccountsAdapter = new AccountProfileAdapter(this,
                mAccounts,
                R.layout.account_profile,
                new String[] { Accounts.SID,
                        Accounts.SERVICE },
                new int[] { R.id.profile,
                        R.id.icon });
        mDrawerAccounts.setAdapter(mDrawerAccountsAdapter);
        mDrawerAccounts.setOnItemClickListener(this);
        mDrawerAccounts.setOnItemLongClickListener(this);

        mDrawerPrimary = (ListView) drawerContainer.findViewById(R.id.drawer_primary);
        mDrawerPrimaryAdapter = new MenuItemAdapter(this, R.menu.menu_drawer_primary);
        mDrawerPrimary.setAdapter(mDrawerPrimaryAdapter);
        mDrawerPrimary.setItemChecked(0, true);
        mDrawerPrimary.setOnItemClickListener(this);

        mDrawerSecondary = (ListView) drawerContainer.findViewById(R.id.drawer_secondary);
        mDrawerSecondaryAdapter = new MenuItemAdapter(this, R.menu.menu_drawer_secondary);
        mDrawerSecondary.setAdapter(mDrawerSecondaryAdapter);
        mDrawerSecondary.setOnItemClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            drawerContainer.setFitsSystemWindows(true);
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

                case R.id.menu_refresh:
                    // TODO handle this in WidgetsList, show loading, cleared onLoadFinished
                    startService(new Intent(this, SonetService.class)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID).setAction(ACTION_REFRESH));
                    return true;

                case R.id.menu_about_default_settings:
                    // TODO replace fragment instead of using Activity
                    startActivityForResult(new Intent(this, Settings.class), RESULT_REFRESH);
                    return true;

                case R.id.menu_refresh_all_widgets:
                    // TODO show loading, cleared onLoadFinished
                    Toast.makeText(this, R.string.refreshing, Toast.LENGTH_LONG).show();
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

                case R.id.menu_about:
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

            case REQUEST_AUTHENTICATE_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    // TODO WidgetsList should refresh... perhaps triggered through ContentObserver
                }
                break;

            case REQUEST_ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    // TODO WidgetsList should refresh... perhaps triggered through ContentObserver
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

            case REQUEST_CONFIRM_AUTHENTICATE_ACCOUNT:
                if (result == RESULT_OK) {
                    startActivityForResult(new Intent(this, OAuthLogin.class)
                                    .putExtra(Accounts.SERVICE, data.getIntExtra(Accounts.SERVICE, 0))
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                                    .putExtra(Sonet.EXTRA_ACCOUNT_ID, data.getLongExtra(Accounts._ID, INVALID_ACCOUNT_ID)),
                            REQUEST_AUTHENTICATE_ACCOUNT);
                }
                break;

            case REQUEST_CONFIRM_REMOVE_ACCOUNT:
                if (result == RESULT_OK) {
                    Toast.makeText(this, R.string.delete_account, Toast.LENGTH_LONG).show();
                    startService(AccountUpdateService.obtainIntent(this,
                            AccountUpdateService.ACTION_DELETE,
                            data.getLongExtra(Accounts._ID, INVALID_ACCOUNT_ID),
                            AppWidgetManager.INVALID_APPWIDGET_ID));
                    // TODO WidgetsList should refresh... perhaps triggered through ContentObserver
                }
                break;

            case REQUEST_CHOOSE_NETWORK:
                if (result == RESULT_OK) {
                    startActivityForResult(new Intent(this, OAuthLogin.class)
                            .putExtra(Accounts.SERVICE,
                                    Integer.parseInt(getResources().getStringArray(R.array.service_values)[ItemsDialogFragment.getWhich(data, 0)]))
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                            .putExtra(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID)
                            , REQUEST_ADD_ACCOUNT);

                }
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

                mDrawerAccountsAdapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        switch (loader.getId()) {
            case LOADER_ACCOUNTS:
                mAccounts.clear();
                mDrawerAccountsAdapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDrawerAccounts) {
            if (id == Sonet.INVALID_ACCOUNT_ID) {
                // Add Account
                String[] services = getResources().getStringArray(R.array.service_entries);
                ItemsDialogFragment.newInstance(services, REQUEST_CHOOSE_NETWORK)
                        .show(getSupportFragmentManager(), DIALOG_CHOOSE_NETWORK);
            } else {
                DialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(R.string.authenticate_account,
                        REQUEST_CONFIRM_AUTHENTICATE_ACCOUNT);
                Bundle args = dialogFragment.getArguments();
                args.putLong(Accounts._ID, id);
                args.putInt(Accounts.SERVICE, Integer.valueOf(mDrawerAccountsAdapter.getItem(position).get(Accounts.SERVICE)));
                dialogFragment.show(getSupportFragmentManager(), DIALOG_CONFIRM_AUTHENTICATE_ACCOUNT);
            }
        } else if (parent == mDrawerPrimary) {
            mDrawerPrimary.setItemChecked(position, true);
            onOptionsItemSelected(mDrawerPrimaryAdapter.getItem(position));
            mDrawer.closeDrawers();
        } else if (parent == mDrawerSecondary) {
            onOptionsItemSelected(mDrawerSecondaryAdapter.getItem(position));
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDrawerAccounts) {
            // can't remove the Add Account option
            if (id != Sonet.INVALID_ACCOUNT_ID) {
                DialogFragment dialogFragment = ConfirmationDialogFragment.newInstance(R.string.delete_account,
                        REQUEST_CONFIRM_REMOVE_ACCOUNT);
                Bundle args = dialogFragment.getArguments();
                args.putLong(Accounts._ID, id);
                dialogFragment.show(getSupportFragmentManager(), DIALOG_CONFIRM_REMOVE_ACCOUNT);
                return true;
            }
        }

        return false;
    }
}
