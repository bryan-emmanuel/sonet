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
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
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

import com.piusvelte.sonet.adapter.AccountAdapter;
import com.piusvelte.sonet.adapter.AccountProfileAdapter;
import com.piusvelte.sonet.adapter.MenuItemAdapter;
import com.piusvelte.sonet.fragment.AccountsList;
import com.piusvelte.sonet.fragment.ConfirmationDialogFragment;
import com.piusvelte.sonet.fragment.Feed;
import com.piusvelte.sonet.fragment.NotificationsList;
import com.piusvelte.sonet.fragment.Settings;
import com.piusvelte.sonet.loader.AccountsProfilesLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.RESULT_REFRESH;

public class About extends BaseActivity implements AdapterView.OnItemClickListener {
    // TODO there should be nothing widget specific here
    private int[] mAppWidgetIds;
    // TODO there should be nothing widget specific here
    private AppWidgetManager mAppWidgetManager;
    // TODO places which update the widgets should call startService themselves
    private boolean mUpdateWidget = false;
    private static final String TAG = "About";

    public static final int LOADER_ACOUNT_PROFILES = 0;

    private static final String FRAGMENT_CONTENT = "fragment:content";

    private static final String DIALOG_ABOUT = "dialog:about";

    private static final String FRAGMENT_ACCOUNT_DETAIL = "fragment:account_detail";

    private DrawerLayout mDrawer;
    private GridView mDrawerAccounts;
    private ListView mDrawerPrimary;
    private ListView mDrawerSecondary;
    private AccountProfileAdapter mDrawerAccountsAdapter;
    private MenuItemAdapter mDrawerPrimaryAdapter;
    private MenuItemAdapter mDrawerSecondaryAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    List<HashMap<String, String>> mAccounts = new ArrayList<>();
    private AccountsProfilesLoaderCallback mAccountsProfilesLoaderCallback = new AccountsProfilesLoaderCallback(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setupAd();

        setupActionBar();
        setupDrawer();

        Fragment content = getSupportFragmentManager().findFragmentByTag(FRAGMENT_CONTENT);

        if (content == null) {
            mDrawerPrimary.setItemChecked(0, true);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_fragment_container, new Feed(), FRAGMENT_CONTENT)
                    .commit();
        }

        getSupportLoaderManager().initLoader(LOADER_ACOUNT_PROFILES, null, mAccountsProfilesLoaderCallback);
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
                mAccounts
        );
        mDrawerAccounts.setAdapter(mDrawerAccountsAdapter);
        mDrawerAccounts.setOnItemClickListener(this);

        mDrawerPrimary = (ListView) drawerContainer.findViewById(R.id.drawer_primary);
        mDrawerPrimaryAdapter = new MenuItemAdapter(this, R.menu.menu_drawer_primary, android.R.layout.simple_list_item_activated_1);
        mDrawerPrimary.setAdapter(mDrawerPrimaryAdapter);
        mDrawerPrimary.setOnItemClickListener(this);

        mDrawerSecondary = (ListView) drawerContainer.findViewById(R.id.drawer_secondary);
        mDrawerSecondaryAdapter = new MenuItemAdapter(this, R.menu.menu_drawer_secondary, android.R.layout.simple_list_item_1);
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
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_fragment_container,
                                    new Feed(),
                                    FRAGMENT_CONTENT)
                            .commit();
                    return true;

                case R.id.menu_refresh:
                    // TODO handle this in Feed, show loading, cleared onLoadFinished
                    startService(new Intent(this, SonetService.class)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                            .setAction(ACTION_REFRESH));
                    return true;

                case R.id.menu_about_default_settings:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_fragment_container,
                                    Settings.newInstance(),
                                    FRAGMENT_CONTENT)
                            .commit();
                    return true;

                case R.id.menu_refresh_all_widgets:
                    // TODO show loading, cleared onLoadFinished
                    Toast.makeText(this, R.string.refreshing, Toast.LENGTH_LONG).show();
                    startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
                    return true;

                case R.id.menu_about_notifications:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_fragment_container,
                                    new NotificationsList(),
                                    FRAGMENT_CONTENT)
                            .commit();
                    return true;

                case R.id.menu_accounts:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_fragment_container,
                                    AccountsList.newInstance(),
                                    FRAGMENT_CONTENT)
                            .commit();
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

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            default:
                // NO-OP
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDrawerAccounts) {
            mDrawer.closeDrawers();
            HashMap<String, String> account = mDrawerAccountsAdapter.getItem(position);
            getSupportFragmentManager().beginTransaction()
                    .add(Settings.newInstance(AccountAdapter.getAccountId(account),
                                    AccountAdapter.getAccountService(account),
                                    AccountAdapter.getAccountProfileUrl(account),
                                    AccountAdapter.getAccountUsername(account)),
                            FRAGMENT_ACCOUNT_DETAIL)
                    .addToBackStack(null)
                    .commit();
            // TODO animate the home bars to arrow and update toolbar title
        } else if (parent == mDrawerPrimary) {
            onOptionsItemSelected(mDrawerPrimaryAdapter.getItem(position));
            mDrawer.closeDrawers();
        } else if (parent == mDrawerSecondary) {
            onOptionsItemSelected(mDrawerSecondaryAdapter.getItem(position));
        }
    }

    private void setAccounts(List<HashMap<String, String>> accounts) {
        mAccounts.clear();

        if (accounts != null) {
            mAccounts.addAll(accounts);
        }

        mDrawerAccountsAdapter.notifyDataSetChanged();
    }

    private static class AccountsProfilesLoaderCallback implements LoaderManager.LoaderCallbacks<List<HashMap<String, String>>> {

        private About mAbout;

        AccountsProfilesLoaderCallback(@NonNull About about) {
            mAbout = about;
        }

        @Override
        public Loader<List<HashMap<String, String>>> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_ACOUNT_PROFILES:
                    return new AccountsProfilesLoader(mAbout);

                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<List<HashMap<String, String>>> loader, List<HashMap<String, String>> data) {
            switch (loader.getId()) {
                case LOADER_ACOUNT_PROFILES:
                    mAbout.setAccounts(data);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<List<HashMap<String, String>>> loader) {
            switch (loader.getId()) {
                case LOADER_ACOUNT_PROFILES:
                    mAbout.setAccounts(null);
                    break;
            }
        }
    }
}
