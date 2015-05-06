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

import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Notifications;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.provider.WidgetsSettings;
import com.piusvelte.sonet.social.Client;

import org.apache.http.client.HttpClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.piusvelte.sonet.Sonet.PRO;
import static com.piusvelte.sonet.Sonet.RESULT_REFRESH;
import static com.piusvelte.sonet.Sonet.sRFC822;
import static com.piusvelte.sonet.Sonet.sTimeZone;

public class SonetNotifications extends ListActivity {
    // list the current notifications
    // check for cache versions in statuses first, falling back on reloading them from the service
    private static final int CLEAR = 1;
    private static final String TAG = "SonetNotifications";
    private SonetCrypto mSonetCrypto;
    private SimpleDateFormat mSimpleDateFormat = null;

    // expanding notifications, check any statuses that have been commented on in the past 24 hours
    // this requires tracking the last comment date

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications);
        if (!getPackageName().toLowerCase().contains(PRO)) {
            AdView adView = new AdView(this, AdSize.BANNER, BuildConfig.GOOGLEAD_ID);
            ((FrameLayout) findViewById(R.id.ad)).addView(adView);
            adView.loadAd(new AdRequest());
        }
        registerForContextMenu(getListView());
        setResult(RESULT_OK);
    }

    @Override
    protected void onListItemClick(ListView list, final View view, int position, final long id) {
        super.onListItemClick(list, view, position, id);
        // load SonetComments.java, the notification will be clear there
        startActivityForResult(
                new Intent(this, SonetComments.class).setData(Uri.withAppendedPath(Notifications.getContentUri(this), Long.toString(id))),
                RESULT_REFRESH);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        // create clearing option
        menu.add(0, CLEAR, 0, R.string.clear);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        if (item.getItemId() == CLEAR) {
            final ProgressDialog loadingDialog = new ProgressDialog(this);
            final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... arg0) {
                    // clear all notifications
                    ContentValues values = new ContentValues();
                    values.put(Notifications.CLEARED, 1);
                    SonetNotifications.this.getContentResolver()
                            .update(Notifications.getContentUri(SonetNotifications.this), values, Notifications._ID + "=?",
                                    new String[] { Long.toString(((AdapterContextMenuInfo) item.getMenuInfo()).id) });
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    SonetNotifications.this.finish();
                }
            };
            loadingDialog.setMessage(getString(R.string.loading));
            loadingDialog.setCancelable(true);
            loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (!asyncTask.isCancelled()) asyncTask.cancel(true);
                }
            });
            loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            loadingDialog.show();
            asyncTask.execute();
        }
        return super.onContextItemSelected(item);
        // clear
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_notifications, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final ProgressDialog loadingDialog = new ProgressDialog(this);
        final AsyncTask<Integer, String, Boolean> asyncTask = new AsyncTask<Integer, String, Boolean>() {

            @Override
            protected Boolean doInBackground(Integer... arg0) {
                if (arg0[0] == R.id.menu_notifications_refresh) {
                    // select all accounts with notifications set
                    Cursor widgets = getContentResolver()
                            .query(WidgetsSettings.getDistinctContentUri(SonetNotifications.this), new String[] { Widgets.ACCOUNT },
                                    Widgets.ACCOUNT + "!=-1 and (" + Widgets.LIGHTS + "=1 or " + Widgets.VIBRATE + "=1 or " + Widgets.SOUND + "=1)",
                                    null, null);
                    if (widgets.moveToFirst()) {
                        mSonetCrypto = SonetCrypto.getInstance(getApplicationContext());
                        HttpClient httpClient = SonetHttpClient.getThreadSafeClient(getApplicationContext());

                        while (!widgets.isAfterLast()) {
                            long accountId = widgets.getLong(0);
                            ArrayList<String> notificationSids = new ArrayList<>();
                            Cursor account = getContentResolver().query(Accounts.getContentUri(SonetNotifications.this),
                                    new String[] { Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE, Accounts.SID }, Accounts._ID + "=?",
                                    new String[] { Long.toString(accountId) }, null);
                            if (account.moveToFirst()) {
                                // for each account, for each notification, check for updates
                                // if there are no updates past 24hrs and cleared, delete
                                String token = mSonetCrypto.Decrypt(account.getString(0));
                                String secret = mSonetCrypto.Decrypt(account.getString(1));
                                int service = account.getInt(2);
                                String accountEsid = mSonetCrypto.Decrypt(account.getString(3));

                                Client client = new Client.Builder(SonetNotifications.this)
                                        .setNetwork(service)
                                        .setCredentials(token, secret)
                                        .setAccount(accountEsid)
                                        .build();

                                client.getNotifications(accountId, new String[1]);

                                // remove old notifications
                                getContentResolver().delete(Notifications.getContentUri(SonetNotifications.this),
                                        Notifications.CLEARED + "=1 and " + Notifications.ACCOUNT + "=? and " + Notifications.CREATED + "<?",
                                        new String[] { Long.toString(accountId), Long.toString(System.currentTimeMillis() - 86400000) });
                            }
                            account.close();
                            widgets.moveToNext();
                        }
                    } else {
                        publishProgress("No notifications have been set up on any accounts.");
                    }
                    widgets.close();
                    return false;
                } else if (arg0[0] == R.id.menu_notifications_clear_all) {
                    // clear all notifications
                    ContentValues values = new ContentValues();
                    values.put(Notifications.CLEARED, 1);
                    SonetNotifications.this.getContentResolver().update(Notifications.getContentUri(SonetNotifications.this), values, null, null);
                    return true;
                }
                return false;
            }

            @Override
            protected void onProgressUpdate(String... messages) {
                (Toast.makeText(SonetNotifications.this, messages[0], Toast.LENGTH_LONG)).show();
            }

            @Override
            protected void onPostExecute(Boolean finish) {
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                if (finish) {
                    SonetNotifications.this.finish();
                }
            }

            private void addNotification(String sid, String esid, String friend, String message, long created, long accountId, String notification) {
                ContentValues values = new ContentValues();
                values.put(Notifications.SID, sid);
                values.put(Notifications.ESID, esid);
                values.put(Notifications.FRIEND, friend);
                values.put(Notifications.MESSAGE, message);
                values.put(Notifications.CREATED, created);
                values.put(Notifications.ACCOUNT, accountId);
                values.put(Notifications.NOTIFICATION, notification);
                values.put(Notifications.CLEARED, 0);
                values.put(Notifications.UPDATED, created);
                getContentResolver().insert(Notifications.getContentUri(SonetNotifications.this), values);
            }

            private long parseDate(String date, String format) {
                if (date != null) {
                    // hack for the literal 'Z'
                    if (date.substring(date.length() - 1).equals("Z")) {
                        date = date.substring(0, date.length() - 2) + "+0000";
                    }
                    Date created = null;
                    if (format != null) {
                        if (mSimpleDateFormat == null) {
                            mSimpleDateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
                            // all dates should be GMT/UTC
                            mSimpleDateFormat.setTimeZone(sTimeZone);
                        }
                        try {
                            created = mSimpleDateFormat.parse(date);
                            return created.getTime();
                        } catch (ParseException e) {
                            Log.e(TAG, e.toString());
                        }
                    } else {
                        // attempt to parse RSS date
                        if (mSimpleDateFormat != null) {
                            try {
                                created = mSimpleDateFormat.parse(date);
                                return created.getTime();
                            } catch (ParseException e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                        for (String rfc822 : sRFC822) {
                            mSimpleDateFormat = new SimpleDateFormat(rfc822, Locale.ENGLISH);
                            mSimpleDateFormat.setTimeZone(sTimeZone);
                            try {
                                if ((created = mSimpleDateFormat.parse(date)) != null) {
                                    return created.getTime();
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    }
                }
                return System.currentTimeMillis();
            }
        };
        loadingDialog.setMessage(getString(R.string.loading));
        loadingDialog.setCancelable(true);
        loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!asyncTask.isCancelled()) asyncTask.cancel(true);
            }
        });
        loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        loadingDialog.show();
        asyncTask.execute(item.getItemId());
        return true;
        //		return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // cancel any notifications
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Sonet.NOTIFY_ID);
        loadNotifications();
    }

    private final SimpleCursorAdapter.ViewBinder mViewBinder = new SimpleCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (columnIndex == cursor.getColumnIndex(Notifications.CLEARED)) {
                view.setEnabled(cursor.getInt(columnIndex) != 1);
                return true;
            } else {
                return false;
            }
        }
    };

    private void loadNotifications() {
        Cursor c = this.managedQuery(Notifications.getContentUri(SonetNotifications.this),
                new String[] { Notifications._ID, Notifications.CLEARED, Notifications.NOTIFICATION }, Notifications.CLEARED + "!=1", null, null);
        SimpleCursorAdapter sca = new SimpleCursorAdapter(this, R.layout.notifications_row, c,
                new String[] { Notifications.CLEARED, Notifications.NOTIFICATION }, new int[] { R.id.notification, R.id.notification });
        sca.setViewBinder(mViewBinder);
        setListAdapter(sca);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // don't finish the activity, in case there are other notifications to view
//		if ((requestCode == RESULT_REFRESH) && (resultCode == RESULT_OK)) {
//			finish();
//		}
    }
}
