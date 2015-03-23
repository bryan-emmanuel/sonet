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

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.PRO;
import static com.piusvelte.sonet.Sonet.RESULT_REFRESH;
import static com.piusvelte.sonet.Sonet.RSS;
import static com.piusvelte.sonet.Sonet.SMS;
import static com.piusvelte.sonet.Sonet.sBFOptions;
import static com.piusvelte.sonet.SonetProvider.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetProvider.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetProvider.TABLE_WIDGET_ACCOUNTS;

import com.example.android.actionbarcompat.ActionBarListActivity;
import com.google.ads.*;
import com.piusvelte.eidos.Eidos;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.AccountsStyles;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.StatusImages;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.Statuses;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.social.Client;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageAccounts extends ActionBarListActivity implements DialogInterface.OnClickListener {
    private static final int REAUTH_ID = Menu.FIRST;
    private static final int SETTINGS_ID = Menu.FIRST + 1;
    private static final int ENABLE_ID = Menu.FIRST + 2;
    private static final int DELETE_ID = Menu.FIRST + 3;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean mAddingAccount,
            mUpdateWidget = false;
    private AlertDialog mDialog;
    private static final String TAG = "ManageAccounts";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.accounts);
        if (!getPackageName().toLowerCase().contains(PRO)) {
            AdView adView = new AdView(this, AdSize.BANNER, BuildConfig.GOOGLEAD_ID);
            ((LinearLayout) findViewById(R.id.ad)).addView(adView);
            adView.loadAd(new AdRequest());
        }

        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null)
                mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                // if called from widget, the id is set in the action, as pendingintents must have a unique action
            else if ((intent.getAction() != null) && (!intent.getAction().equals(ACTION_REFRESH)) && (!intent.getAction().equals(Intent.ACTION_VIEW)))
                mAppWidgetId = Integer.parseInt(intent.getAction());
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);

        registerForContextMenu(getListView());

        Drawable wp = WallpaperManager.getInstance(getApplicationContext()).getDrawable();
        if (wp != null)
            findViewById(R.id.ad).getRootView().setBackgroundDrawable(wp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_manageaccounts, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.button_add_account) {
            // add a new account
            String[] services = getResources().getStringArray(R.array.service_entries);
            mDialog = (new AlertDialog.Builder(this))
                    .setItems(services, this)
                    .create();
            mDialog.show();
        } else if (itemId == R.id.default_widget_settings) {
            mAddingAccount = true;
            startActivityForResult(Sonet.getPackageIntent(this, Settings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId), RESULT_REFRESH);
        }
        return super.onOptionsItemSelected(item);
    }

    private final SimpleCursorAdapter.ViewBinder mViewBinder = new SimpleCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (columnIndex == cursor.getColumnIndex(StatusesStyles.FRIEND)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.FRIEND_TEXTSIZE)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.MESSAGE)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.MESSAGES_TEXTSIZE)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.STATUS_BG)) {
                Bitmap bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                canvas.drawColor(cursor.getInt(columnIndex));
                ((ImageView) view).setImageBitmap(bmp);
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.PROFILE)) {
                Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_contact_picture, sBFOptions);

                if (bmp != null) {
                    ((ImageView) view).setImageBitmap(bmp);
                }

                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.FRIEND + "2")) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.FRIEND_TEXTSIZE)));
                ((TextView) view).setTextColor(cursor.getInt(cursor.getColumnIndex(StatusesStyles.FRIEND_COLOR)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.CREATEDTEXT)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.CREATED_TEXTSIZE)));
                ((TextView) view).setTextColor(cursor.getInt(cursor.getColumnIndex(StatusesStyles.CREATED_COLOR)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.MESSAGE + "2")) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                ((TextView) view).setTextSize(cursor.getLong(cursor.getColumnIndex(StatusesStyles.MESSAGES_TEXTSIZE)));
                ((TextView) view).setTextColor(cursor.getInt(cursor.getColumnIndex(StatusesStyles.MESSAGES_COLOR)));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.ICON)) {
                Bitmap bmp = BitmapFactory.decodeResource(getResources(), Client.Network.get(cursor.getInt(columnIndex)).getIcon(), sBFOptions);

                if (bmp != null) {
                    ((ImageView) view).setImageBitmap(bmp);
                }

                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.PROFILE_BG)) {
                Bitmap bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                canvas.drawColor(cursor.getInt(columnIndex));
                ((ImageView) view).setImageBitmap(bmp);
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.FRIEND_BG)) {
                Bitmap bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                canvas.drawColor(cursor.getInt(columnIndex));
                ((ImageView) view).setImageBitmap(bmp);
                return true;
            } else
                return false;
        }
    };

    @Override
    protected void onListItemClick(ListView list, final View view, int position, final long id) {
        super.onListItemClick(list, view, position, id);
        final CharSequence[] items = {getString(R.string.re_authenticate), getString(R.string.account_settings), getString(((TextView) view.findViewById(R.id.message)).getText().toString().contains("enabled") ? R.string.disable : R.string.enable)};
        mDialog = (new AlertDialog.Builder(this))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        which++; //fix indexing
                        switch (which) {
                            case REAUTH_ID:
                                // need the account id if reauthenticating
                                Cursor c = getContentResolver().query(Accounts.getContentUri(ManageAccounts.this), new String[]{Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(id)}, null);
                                if (c.moveToFirst()) {
                                    int service = c.getInt(0);
                                    if ((service != SMS) && (service != RSS)) {
                                        mAddingAccount = true;
                                        startActivityForResult(Sonet.getPackageIntent(ManageAccounts.this, OAuthLogin.class).putExtra(Accounts.SERVICE, service).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, id), RESULT_REFRESH);
                                    }
                                }
                                c.close();
                                break;
                            case SETTINGS_ID:
                                mAddingAccount = true;
                                startActivityForResult(Sonet.getPackageIntent(ManageAccounts.this, AccountSettings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, id), RESULT_REFRESH);
                                break;
                            case ENABLE_ID:
                                if (((TextView) view.findViewById(R.id.message)).getText().toString().contains("enabled")) {
                                    // disable the account, remove settings and statuses
                                    getContentResolver().delete(Widgets.getContentUri(ManageAccounts.this), Widgets.ACCOUNT + "=? and " + Widgets.WIDGET + "=?", new String[]{Long.toString(id), Integer.toString(mAppWidgetId)});
                                    getContentResolver().delete(WidgetAccounts.getContentUri(ManageAccounts.this), WidgetAccounts.ACCOUNT + "=? and " + WidgetAccounts.WIDGET + "=?", new String[]{Long.toString(id), Integer.toString(mAppWidgetId)});
                                    Cursor statuses = getContentResolver().query(Statuses.getContentUri(ManageAccounts.this), new String[]{Statuses._ID}, Statuses.ACCOUNT + "=? and " + Statuses.WIDGET + "=?", new String[]{Long.toString(id), Integer.toString(mAppWidgetId)}, null);
                                    if (statuses.moveToFirst()) {
                                        while (!statuses.isAfterLast()) {
                                            getContentResolver().delete(StatusLinks.getContentUri(ManageAccounts.this), StatusLinks.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
                                            getContentResolver().delete(StatusImages.getContentUri(ManageAccounts.this), StatusImages.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
                                            statuses.moveToNext();
                                        }
                                    }
                                    statuses.close();
                                    getContentResolver().delete(Statuses.getContentUri(ManageAccounts.this), Statuses.ACCOUNT + "=? and " + Statuses.WIDGET + "=?", new String[]{Long.toString(id), Integer.toString(mAppWidgetId)});
                                    listAccounts();
                                } else {
                                    // enable the account
                                    ContentValues values = new ContentValues();
                                    values.put(WidgetAccounts.ACCOUNT, id);
                                    values.put(WidgetAccounts.WIDGET, mAppWidgetId);
                                    ManageAccounts.this.getContentResolver().insert(WidgetAccounts.getContentUri(ManageAccounts.this), values);
                                    listAccounts();
                                }
                                mUpdateWidget = true;
                                listAccounts();
                                break;
                        }
                        dialog.cancel();
                    }
                })
                .create();
        mDialog.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.delete_account);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == DELETE_ID) {
            mUpdateWidget = true;
            String accountId = Long.toString(((AdapterContextMenuInfo) item.getMenuInfo()).id);
            getContentResolver().delete(Accounts.getContentUri(this), Accounts._ID + "=?", new String[]{accountId});
            // need to delete the statuses and settings for all accounts
            getContentResolver().delete(Widgets.getContentUri(this), Widgets.ACCOUNT + "=?", new String[]{accountId});
            Cursor statuses = getContentResolver().query(Statuses.getContentUri(this), new String[]{Statuses._ID}, Statuses.ACCOUNT + "=?", new String[]{accountId}, null);
            if (statuses.moveToFirst()) {
                while (!statuses.isAfterLast()) {
                    getContentResolver().delete(StatusLinks.getContentUri(this), StatusLinks.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
                    getContentResolver().delete(StatusImages.getContentUri(this), StatusImages.STATUS_ID + "=?", new String[]{Long.toString(statuses.getLong(0))});
                    statuses.moveToNext();
                }
            }
            statuses.close();
            getContentResolver().delete(Statuses.getContentUri(this), Statuses.ACCOUNT + "=?", new String[]{accountId});
            getContentResolver().delete(WidgetAccounts.getContentUri(this), WidgetAccounts.ACCOUNT + "=?", new String[]{accountId});
            getContentResolver().delete(Notifications.getContentUri(this), Notifications.ACCOUNT + "=?", new String[]{accountId});
            listAccounts();
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listAccounts();
        mAddingAccount = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!mAddingAccount && mUpdateWidget) {
            Eidos.requestBackup(this);
            (Toast.makeText(getApplicationContext(), getString(R.string.refreshing), Toast.LENGTH_LONG)).show();
            startService(Sonet.getPackageIntent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
        }
        if ((mDialog != null) && mDialog.isShowing())
            mDialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == RESULT_REFRESH) && (resultCode == RESULT_OK))
            mUpdateWidget = true;
    }

    private void listAccounts() {
        // list all accounts, checking the checkbox if they are enabled for this widget
        // prepend service name to username

        Cursor c = this.managedQuery(AccountsStyles.getContentUri(this), new String[]{
                Accounts._ID,

                "(case when " + Accounts.SERVICE + "=" + Sonet.TWITTER + " then 'Twitter: ' when "
                        + Accounts.SERVICE + "=" + Sonet.FACEBOOK + " then 'Facebook: ' when "
                        + Accounts.SERVICE + "=" + Sonet.MYSPACE + " then 'MySpace: ' when "
                        + Accounts.SERVICE + "=" + Sonet.LINKEDIN + " then 'LinkedIn: ' when "
                        + Accounts.SERVICE + "=" + Sonet.FOURSQUARE + " then 'Foursquare: ' when "
                        + Accounts.SERVICE + "=" + Sonet.CHATTER + " then 'Chatter: ' when "
                        + Accounts.SERVICE + "=" + Sonet.RSS + " then 'RSS: ' when "
                        + Accounts.SERVICE + "=" + Sonet.IDENTICA + " then 'Identi.ca: ' when "
                        + Accounts.SERVICE + "=" + Sonet.GOOGLEPLUS + " then 'Google+: ' when "
                        + Accounts.SERVICE + "=" + Sonet.PINTEREST + " then 'Pinterest: ' else '' end) as " + StatusesStyles.FRIEND,

                "(case when " + Accounts.SERVICE + "=" + Sonet.TWITTER + " then 'Twitter: ' when "
                        + Accounts.SERVICE + "=" + Sonet.FACEBOOK + " then 'Facebook: ' when "
                        + Accounts.SERVICE + "=" + Sonet.MYSPACE + " then 'MySpace: ' when "
                        + Accounts.SERVICE + "=" + Sonet.LINKEDIN + " then 'LinkedIn: ' when "
                        + Accounts.SERVICE + "=" + Sonet.FOURSQUARE + " then 'Foursquare: ' when "
                        + Accounts.SERVICE + "=" + Sonet.CHATTER + " then 'Chatter: ' when "
                        + Accounts.SERVICE + "=" + Sonet.RSS + " then 'RSS: ' when "
                        + Accounts.SERVICE + "=" + Sonet.IDENTICA + " then 'Identi.ca: ' when "
                        + Accounts.SERVICE + "=" + Sonet.GOOGLEPLUS + " then 'Google+: ' when "
                        + Accounts.SERVICE + "=" + Sonet.PINTEREST + " then 'Pinterest: ' else '' end) as " + StatusesStyles.FRIEND + "2",

                "(case when (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else 1 end) as " + StatusesStyles.PROFILE,

                "(case when (select " + WidgetAccounts.WIDGET + " from " + TABLE_WIDGET_ACCOUNTS + " where " + WidgetAccounts.WIDGET + "=" + mAppWidgetId + " and " + WidgetAccounts.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1) is null then 'this account is disabled for this widget, select to enable' else 'account is enabled for this widget, select to change settings' end) as " + StatusesStyles.MESSAGE,

                "(case when (select " + WidgetAccounts.WIDGET + " from " + TABLE_WIDGET_ACCOUNTS + " where " + WidgetAccounts.WIDGET + "=" + mAppWidgetId + " and " + WidgetAccounts.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1) is null then 'this account is disabled for this widget, select to enable' else 'account is enabled for this widget, select to change settings' end) as " + StatusesStyles.MESSAGE + "2",

                Accounts.USERNAME + " as " + StatusesStyles.CREATEDTEXT,

                "(case when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else " + Sonet.default_message_color + " end) as " + StatusesStyles.MESSAGES_COLOR,

                "(case when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else " + Sonet.default_friend_color + " end) as " + StatusesStyles.FRIEND_COLOR,

                "(case when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.CREATED_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else " + Sonet.default_created_color + " end) as " + StatusesStyles.CREATED_COLOR,

                "(case when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else " + Sonet.default_messages_textsize + " end) as " + StatusesStyles.MESSAGES_TEXTSIZE,

                "(case when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else " + Sonet.default_friend_textsize + " end) as " + StatusesStyles.FRIEND_TEXTSIZE,

                "(case when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.CREATED_TEXTSIZE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else " + Sonet.default_created_textsize + " end) as " + StatusesStyles.CREATED_TEXTSIZE,

                "(case when (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.MESSAGES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else " + Sonet.default_message_bg_color + " end) as " + StatusesStyles.STATUS_BG,

                Accounts.SERVICE + " as " + StatusesStyles.ICON,

                "(case when (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.PROFILES_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else " + Sonet.default_message_bg_color + " end) as " + StatusesStyles.PROFILE_BG,

                "(case when (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                        + "when (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + mAppWidgetId + " and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "when (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.FRIEND_BG_COLOR + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                        + "else " + Sonet.default_friend_bg_color + " end) as " + StatusesStyles.FRIEND_BG
        }, null, null, null);
        SimpleCursorAdapter sca = new SimpleCursorAdapter(ManageAccounts.this, R.layout.widget_item, c, new String[]{StatusesStyles.FRIEND, StatusesStyles.FRIEND + "2", StatusesStyles.MESSAGE, StatusesStyles.MESSAGE + "2", StatusesStyles.STATUS_BG, StatusesStyles.CREATEDTEXT, StatusesStyles.PROFILE, StatusesStyles.ICON, StatusesStyles.PROFILE_BG, StatusesStyles.FRIEND_BG}, new int[]{R.id.friend_bg_clear, R.id.friend, R.id.message_bg_clear, R.id.message, R.id.status_bg, R.id.created, R.id.profile, R.id.icon, R.id.profile_bg, R.id.friend_bg});
        sca.setViewBinder(mViewBinder);
        setListAdapter(sca);
    }

    public void onClick(DialogInterface dialog, int which) {
        mAddingAccount = true;
        startActivityForResult(Sonet.getPackageIntent(this, OAuthLogin.class).putExtra(Accounts.SERVICE, Integer.parseInt(getResources().getStringArray(R.array.service_values)[which])).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID), RESULT_REFRESH);
        dialog.cancel();
    }

}
