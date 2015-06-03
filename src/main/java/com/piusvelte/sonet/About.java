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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.piusvelte.sonet.adapter.AccountProfileAdapter;
import com.piusvelte.sonet.adapter.MenuItemAdapter;
import com.piusvelte.sonet.fragment.AccountsList;
import com.piusvelte.sonet.fragment.ConfirmationDialogFragment;
import com.piusvelte.sonet.fragment.Feed;
import com.piusvelte.sonet.fragment.NotificationsList;
import com.piusvelte.sonet.fragment.Settings;
import com.piusvelte.sonet.loader.AccountsProfilesLoader;
import com.piusvelte.sonet.util.ScreenTransformation;
import com.squareup.picasso.Picasso;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;

public class About extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    // TODO there should be nothing widget specific here
    private int[] mAppWidgetIds;
    // TODO there should be nothing widget specific here
    private AppWidgetManager mAppWidgetManager;

    private static final String TAG = "About";

    public static final int DRAWER_FEED = 0;
    public static final int DRAWER_SETTINGS = 1;
    public static final int DRAWER_NOTIFICATIONS = 2;
    public static final int DRAWER_ACCOUNTS = 3;

    @IntDef({ DRAWER_FEED, DRAWER_SETTINGS, DRAWER_NOTIFICATIONS, DRAWER_ACCOUNTS })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DrawerItem {
    }

    public static final int LOADER_ACOUNT_PROFILES = 0;

    private static final String EXTRA_DRAWER_ITEM = "drawer_item";

    private static final String FRAGMENT_CONTENT = "fragment:content";

    private static final String DIALOG_ABOUT = "dialog:about";

    private static final String FRAGMENT_ACCOUNT_DETAIL = "fragment:account_detail";

    private DrawerLayout mDrawerLayout;
    private View mDrawerContainer;
    private GridView mDrawerAccounts;
    private ImageView mDrawerAccountsBackground;
    private ListView mDrawerPrimary;
    private ListView mDrawerSecondary;
    private AccountProfileAdapter mDrawerAccountsAdapter;
    private MenuItemAdapter mDrawerPrimaryAdapter;
    private MenuItemAdapter mDrawerSecondaryAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    List<HashMap<String, String>> mAccounts = new ArrayList<>();
    private AccountsProfilesLoaderCallback mAccountsProfilesLoaderCallback = new AccountsProfilesLoaderCallback(this);
    private ScreenTransformation mScreenTransformation;

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
        setupContent();
        getSupportLoaderManager().initLoader(LOADER_ACOUNT_PROFILES, null, mAccountsProfilesLoaderCallback);
    }

    private void setupContent() {
        int selectedPosition;
        Intent intent = getIntent();

        if (intent != null) {
            selectedPosition = intent.getIntExtra(EXTRA_DRAWER_ITEM, DRAWER_FEED);
        } else {
            selectedPosition = DRAWER_FEED;
        }

        mDrawerPrimary.setItemChecked(selectedPosition, true);
        setContent(selectedPosition);
        MenuItem selectedItem = mDrawerPrimaryAdapter.getItem(selectedPosition);
        onOptionsItemSelected(selectedItem);
    }

    private void setupDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new DrawerToggle(this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_closed) {
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerContainer = mDrawerLayout.findViewById(R.id.drawer_container);

        mDrawerAccounts = (GridView) mDrawerContainer.findViewById(R.id.drawer_accounts);
        mDrawerAccountsAdapter = new AccountProfileAdapter(this,
                mAccounts
        );
        mDrawerAccounts.setAdapter(mDrawerAccountsAdapter);
        mDrawerAccounts.setOnItemClickListener(this);

        mDrawerAccountsBackground = (ImageView) mDrawerContainer.findViewById(R.id.drawer_account_background);

        View emptyAccounts = mDrawerContainer.findViewById(R.id.empty_accounts);
        emptyAccounts.setOnClickListener(this);
        mDrawerAccounts.setEmptyView(emptyAccounts);

        mDrawerPrimary = (ListView) mDrawerContainer.findViewById(R.id.drawer_primary);
        mDrawerPrimaryAdapter = new MenuItemAdapter(this, R.menu.menu_drawer_primary, android.R.layout.simple_list_item_activated_1);
        mDrawerPrimary.setAdapter(mDrawerPrimaryAdapter);
        mDrawerPrimary.setOnItemClickListener(this);

        mDrawerSecondary = (ListView) mDrawerContainer.findViewById(R.id.drawer_secondary);
        mDrawerSecondaryAdapter = new MenuItemAdapter(this, R.menu.menu_drawer_secondary, android.R.layout.simple_list_item_1);
        mDrawerSecondary.setAdapter(mDrawerSecondaryAdapter);
        mDrawerSecondary.setOnItemClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mDrawerContainer.setFitsSystemWindows(true);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mDrawerLayout.isDrawerOpen(mDrawerContainer)) {
            menu.clear();
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
            switch (item.getItemId()) {
                // menu_drawer_primary
                case R.id.menu_feed:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_fragment_container,
                                    new Feed(),
                                    FRAGMENT_CONTENT)
                            .commit();
                    return true;

                case R.id.menu_about_default_settings:
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_fragment_container,
                                    Settings.newInstance(),
                                    FRAGMENT_CONTENT)
                            .commit();
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

                // menu_drawer_secondary
                case R.id.menu_refresh_all_widgets:
                    Toast.makeText(this, R.string.refreshing, Toast.LENGTH_LONG).show();
                    startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
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
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            default:
                // NO-OP
                break;
        }
    }

    private void setContent(int position) {
        MenuItem selectedItem = mDrawerPrimaryAdapter.getItem(position);
        onOptionsItemSelected(selectedItem);
        mDrawerLayout.closeDrawers();
        getSupportActionBar().setTitle(selectedItem.getTitle());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDrawerAccounts) {
            mDrawerLayout.closeDrawers();
            mDrawerPrimary.setItemChecked(DRAWER_ACCOUNTS, true);
            setContent(DRAWER_ACCOUNTS);
            supportInvalidateOptionsMenu();
            // TODO got directly to account settings; animate the home bars to arrow and update toolbar title
//            HashMap<String, String> account = mDrawerAccountsAdapter.getItem(position);
//            getSupportFragmentManager().beginTransaction()
//                    .add(Settings.newInstance(AccountAdapter.getAccountId(account),
//                                    AccountAdapter.getAccountService(account),
//                                    AccountAdapter.getAccountProfileUrl(account),
//                                    AccountAdapter.getAccountUsername(account)),
//                            FRAGMENT_ACCOUNT_DETAIL)
//                    .addToBackStack(null)
//                    .commit();
        } else if (parent == mDrawerPrimary) {
            setContent(position);
        } else if (parent == mDrawerSecondary) {
            onOptionsItemSelected(mDrawerSecondaryAdapter.getItem(position));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.empty_accounts:
                mDrawerPrimary.setItemChecked(DRAWER_ACCOUNTS, true);
                setContent(DRAWER_ACCOUNTS);
                supportInvalidateOptionsMenu();
                break;
        }
    }

    private void setAccounts(List<HashMap<String, String>> accounts) {
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

        mDrawerAccountsAdapter.notifyDataSetChanged();
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
