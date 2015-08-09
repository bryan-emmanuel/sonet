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
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.piusvelte.sonet.adapter.AccountProfileAdapter;
import com.piusvelte.sonet.fragment.AccountsList;
import com.piusvelte.sonet.fragment.ConfirmationDialogFragment;
import com.piusvelte.sonet.fragment.Feed;
import com.piusvelte.sonet.fragment.Settings;
import com.piusvelte.sonet.loader.AccountsProfilesLoaderCallback;
import com.piusvelte.sonet.util.ScreenTransformation;
import com.squareup.picasso.Picasso;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;

public class About extends BaseActivity implements View.OnClickListener,
        NavigationView.OnNavigationItemSelectedListener, AccountsProfilesLoaderCallback.OnAccountsLoadedListener {
    // TODO there should be nothing widget specific here
    private int[] mAppWidgetIds;
    // TODO there should be nothing widget specific here
    private AppWidgetManager mAppWidgetManager;

    private static final String TAG = About.class.getSimpleName();

    private static final String STATE_SELECTED_DRAWER_ITEM = TAG + ":state:selected_drawer_item";

    public static final int DRAWER_FEED = 0;
    public static final int DRAWER_SETTINGS = 1;
    //public static final int DRAWER_NOTIFICATIONS = 2;
    public static final int DRAWER_ACCOUNTS = 2;

    @IntDef({ DRAWER_FEED, DRAWER_SETTINGS, /**DRAWER_NOTIFICATIONS,*/DRAWER_ACCOUNTS })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DrawerItem {
    }

    public static final int LOADER_ACOUNT_PROFILES = 0;

    private static final String EXTRA_DRAWER_ITEM = "drawer_item";

    private static final String FRAGMENT_CONTENT = "fragment:content";

    private static final String DIALOG_ABOUT = "dialog:about";

    private DrawerLayout mDrawerLayout;
    private NavigationView mDrawerNav;
    private LinearLayout mDrawerAccounts;
    private View mEmptyAccounts;
    private ImageView mDrawerAccountsBackground;
    private AccountProfileAdapter mDrawerAccountsAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    List<HashMap<String, String>> mAccounts = new ArrayList<>();
    private AccountsProfilesLoaderCallback mAccountsProfilesLoaderCallback = new AccountsProfilesLoaderCallback(this, LOADER_ACOUNT_PROFILES);
    private ScreenTransformation mScreenTransformation;
    private boolean mDrawerAccountsOverflowIsExpanded = false;

    public static Intent createIntent(@NonNull Context context) {
        return new Intent(context, About.class);
    }

    public static Intent createIntent(@NonNull Context context, @DrawerItem int selectedItem) {
        return createIntent(context)
                .putExtra(EXTRA_DRAWER_ITEM, selectedItem);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setupAd();

        setupActionBar();
        setupDrawer();
        setupContent(savedInstanceState);
        getSupportLoaderManager().initLoader(LOADER_ACOUNT_PROFILES, null, mAccountsProfilesLoaderCallback);
    }

    private void setupContent(@Nullable Bundle savedInstanceState) {
        int selectedPosition;

        if (savedInstanceState != null) {
            selectedPosition = savedInstanceState.getInt(STATE_SELECTED_DRAWER_ITEM, DRAWER_FEED);
        } else {
            Intent intent = getIntent();

            if (intent != null) {
                selectedPosition = intent.getIntExtra(EXTRA_DRAWER_ITEM, DRAWER_FEED);
            } else {
                selectedPosition = DRAWER_FEED;
            }
        }

        MenuItem menuItem = mDrawerNav.getMenu()
                .getItem(selectedPosition);

        Fragment content = getSupportFragmentManager().findFragmentByTag(FRAGMENT_CONTENT);

        if (content != null) {
            menuItem.setChecked(true);
            getSupportActionBar().setTitle(menuItem.getTitle());
        } else {
            onNavigationItemSelected(menuItem);
        }
    }

    private void setupDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new DrawerToggle(this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_closed) {
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerNav = (NavigationView) mDrawerLayout.findViewById(R.id.drawer_nav);
        mDrawerNav.setNavigationItemSelectedListener(this);

        mDrawerAccounts = (LinearLayout) mDrawerNav.findViewById(R.id.drawer_accounts);
        mDrawerAccountsBackground = (ImageView) mDrawerNav.findViewById(R.id.drawer_account_background);

        mEmptyAccounts = mDrawerNav.findViewById(R.id.empty_accounts);
        mEmptyAccounts.setOnClickListener(this);

        mDrawerAccountsAdapter = new AccountProfileAdapter(this, mAccounts);
        setupAccounts();
    }

    private void setupAccounts() {
        mDrawerAccounts.removeAllViews();

        if (mDrawerAccountsAdapter.isEmpty()) {
            mDrawerAccounts.setVisibility(View.GONE);
            mEmptyAccounts.setVisibility(View.VISIBLE);
        } else {
            LayoutInflater inflater = getLayoutInflater();
            int accountIndex = 0;

            while (accountIndex < mDrawerAccountsAdapter.getCount()) {
                LinearLayout row = (LinearLayout) inflater.inflate(R.layout.account_profiles_row, mDrawerAccounts, false);
                mDrawerAccounts.addView(row);

                for (int columnIndex = 0;
                     columnIndex < getResources().getInteger(R.integer.drawer_accounts_column_count)
                             && accountIndex < mDrawerAccountsAdapter.getCount();
                     columnIndex++) {
                    View accountView = mDrawerAccountsAdapter.getView(accountIndex, null, row);
                    accountView.setOnClickListener(this);

                    row.addView(accountView);
                    accountIndex++;
                }
            }

            mEmptyAccounts.setVisibility(View.GONE);
            mDrawerAccounts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Menu menu = mDrawerNav.getMenu();

        for (int menuIndex = menu.size() - 1; menuIndex >= 0; menuIndex--) {
            if (menu.getItem(menuIndex).isChecked()) {
                outState.putInt(STATE_SELECTED_DRAWER_ITEM, menuIndex);
                break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mDrawerLayout.isDrawerOpen(mDrawerNav)) {
            menu.clear();
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return false;
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
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            default:
                // NO-OP
                break;
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_feed:
                item.setChecked(true);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_fragment_container,
                                new Feed(),
                                FRAGMENT_CONTENT)
                        .commit();
                mDrawerLayout.closeDrawers();
                getSupportActionBar().setTitle(item.getTitle());
                supportInvalidateOptionsMenu();
                return true;
            case R.id.menu_about_default_settings:
                item.setChecked(true);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_fragment_container,
                                Settings.newInstance(),
                                FRAGMENT_CONTENT)
                        .commit();
                mDrawerLayout.closeDrawers();
                getSupportActionBar().setTitle(item.getTitle());
                supportInvalidateOptionsMenu();
                return true;

//            case R.id.menu_about_notifications:
//                item.setChecked(true);
//                getSupportFragmentManager().beginTransaction()
//                        .replace(R.id.content_fragment_container,
//                                new NotificationsList(),
//                                FRAGMENT_CONTENT)
//                        .commit();
//                mDrawerLayout.closeDrawers();
//                getSupportActionBar().setTitle(item.getTitle());
//                supportInvalidateOptionsMenu();
//                return true;

            case R.id.menu_accounts:
                item.setChecked(true);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_fragment_container,
                                AccountsList.newInstance(),
                                FRAGMENT_CONTENT)
                        .commit();
                mDrawerLayout.closeDrawers();
                getSupportActionBar().setTitle(item.getTitle());
                supportInvalidateOptionsMenu();
                return true;

            case R.id.menu_refresh_all_widgets:
                Toast.makeText(this, R.string.refreshing, Toast.LENGTH_LONG).show();
                startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
                mDrawerLayout.closeDrawers();
                return true;

            case R.id.menu_about:
                ConfirmationDialogFragment.newInstance(R.string.about_title, R.string.about, -1)
                        .show(getSupportFragmentManager(), DIALOG_ABOUT);
                mDrawerLayout.closeDrawers();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.account_profile:
                // intentional fall-through
            case R.id.empty_accounts:
                MenuItem menuItem = mDrawerNav.getMenu()
                        .getItem(DRAWER_ACCOUNTS)
                        .setChecked(true);
                onNavigationItemSelected(menuItem);
                break;
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onAccountsLoaded(List<HashMap<String, String>> accounts) {
        mAccounts.clear();

        if (accounts != null) {
            mAccounts.addAll(accounts);

            if (!accounts.isEmpty()) {
                String url = AccountProfileAdapter.getAccountProfileUrl(accounts.get(0));

                if (!TextUtils.isEmpty(url)) {
                    if (mScreenTransformation == null) {
                        mScreenTransformation = new ScreenTransformation(getResources().getColor(R.color.colorPrimaryDark));
                    }

                    Picasso.with(this)
                            .load(url)
                            .transform(mScreenTransformation)
                            .into(mDrawerAccountsBackground);
                }
            }
        }

        setupAccounts();
    }

    private static class DrawerToggle extends ActionBarDrawerToggle {

        private About mAbout;

        public DrawerToggle(About about, DrawerLayout drawerLayout, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(about, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
            mAbout = about;
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            mAbout.supportInvalidateOptionsMenu();
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
            mAbout.supportInvalidateOptionsMenu();
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            super.onDrawerSlide(drawerView, 0);
        }
    }
}
