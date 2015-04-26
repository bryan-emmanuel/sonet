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

import static com.piusvelte.sonet.Sonet.*;

import mobi.intuitit.android.content.LauncherIntent;

import com.piusvelte.sonet.fragment.BaseDialogFragment;
import com.piusvelte.sonet.fragment.ChooseAccountDialogFragment;
import com.piusvelte.sonet.fragment.ChooseWidgetDialogFragment;
import com.piusvelte.sonet.fragment.ConfirmationDialogFragment;
import com.piusvelte.sonet.fragment.ItemsDialogFragment;
import com.piusvelte.sonet.fragment.LoadingDialogFragment;
import com.piusvelte.sonet.loader.StatusLoader;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.Widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.support.annotation.IntDef;
import android.support.annotation.StringDef;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;

public class StatusDialog extends FragmentActivity implements LoaderManager.LoaderCallbacks, BaseDialogFragment.OnResultListener {
    private static final String TAG = "StatusDialog";

    private static final int LOADER_STATUS = 0;
    private static final int LOADER_ACCOUNTS = 1;
    private static final int LOADER_PROFILE = 2;

    private static final int REQUEST_LOADING_STATUS = 0;
    private static final int REQUEST_CONFIRM_UPLOAD = 1;
    private static final int REQUEST_ITEMS = 2;
    private static final int REQUEST_CHOOSE_ACCOUNT = 3;
    private static final int REQUEST_LOADING_ACCOUNTS = 4;
    private static final int REQUEST_CHOOSE_WIDGET = 5;
    private static final int REQUEST_REFRESH_WIDGET = 6;
    private static final int REQUEST_LOADING_PROFILE = 7;

    @IntDef({REQUEST_LOADING_STATUS, REQUEST_CONFIRM_UPLOAD, REQUEST_ITEMS, REQUEST_CHOOSE_ACCOUNT, REQUEST_LOADING_ACCOUNTS, REQUEST_CHOOSE_WIDGET, REQUEST_REFRESH_WIDGET, REQUEST_LOADING_PROFILE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface RequestCode {
    }

    private static final String DIALOG_LOADING_STATUS = "dialog:loading_status";
    private static final String DIALOG_CONFIRM_UPLOAD = "dialog:confirm_upload";
    private static final String DIALOG_ITEMS = "dialog:items";
    private static final String DIALOG_CHOOSE_ACCOUNT = "dialog:choose_account";
    private static final String DIALOG_LOADING_ACCOUNTS = "dialog:loading_accounts";
    private static final String DIALOG_CHOOSE_WIDGET = "dialog:choose_widget";
    private static final String DIALOG_REFRESH_WIDGET = "dialog:refresh_widget";
    private static final String DIALOG_LOADING_PROFILE = "dialog:loading_profile";

    @StringDef({DIALOG_LOADING_STATUS, DIALOG_CONFIRM_UPLOAD, DIALOG_ITEMS, DIALOG_CHOOSE_ACCOUNT, DIALOG_LOADING_ACCOUNTS, DIALOG_CHOOSE_WIDGET, DIALOG_REFRESH_WIDGET, DIALOG_LOADING_PROFILE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DialogTag {
    }

    private static final String ARG_DATA = "data";
    private static final String ARG_RECT = "rect";
    private static final String ARG_ACCOUNT_ID = "account_id";
    private static final String ARG_ESID = "esid";

    private StatusLoader.Result mStatusLoaderResult;

    @Deprecated
    private int[] mAppWidgetIds;
    private boolean mFinish = false;
    private String mFilePath = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (intent.hasExtra(Widgets.INSTANT_UPLOAD)) {
            mFilePath = intent.getStringExtra(Widgets.INSTANT_UPLOAD);

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "upload photo?" + mFilePath);
            }

            if (getSupportFragmentManager().findFragmentByTag(DIALOG_CONFIRM_UPLOAD) == null) {
                ConfirmationDialogFragment.newInstance(R.string.uploadprompt, REQUEST_CONFIRM_UPLOAD)
                        .show(getSupportFragmentManager(), DIALOG_CONFIRM_UPLOAD);
            }
        } else {
            Rect rect;

            if (intent.hasExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS)) {
                rect = intent.getParcelableExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS);
            } else {
                rect = intent.getSourceBounds();
            }

            if (getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_STATUS) == null) {
                LoadingDialogFragment.newInstance(REQUEST_LOADING_STATUS)
                        .show(getSupportFragmentManager(), DIALOG_LOADING_STATUS);
            }

            Bundle args = new Bundle();
            args.putString(ARG_DATA, getIntent().getData().toString());
            args.putParcelable(ARG_RECT, rect);
            getSupportLoaderManager().initLoader(LOADER_STATUS, args, this);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);

        if (intent.hasExtra(Widgets.INSTANT_UPLOAD)) {
            mFilePath = intent.getStringExtra(Widgets.INSTANT_UPLOAD);

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "upload photo?" + mFilePath);
            }

            if (getSupportFragmentManager().findFragmentByTag(DIALOG_CONFIRM_UPLOAD) == null) {
                ConfirmationDialogFragment.newInstance(R.string.uploadprompt, REQUEST_CONFIRM_UPLOAD)
                        .show(getSupportFragmentManager(), DIALOG_CONFIRM_UPLOAD);
            }
        } else {
            Rect rect;

            if (intent.hasExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS)) {
                rect = intent.getParcelableExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS);
            } else {
                rect = intent.getSourceBounds();
            }

            if (getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_STATUS) == null) {
                LoadingDialogFragment.newInstance(REQUEST_LOADING_STATUS)
                        .show(getSupportFragmentManager(), DIALOG_LOADING_STATUS);
            }

            Bundle args = new Bundle();
            args.putString(ARG_DATA, getIntent().getData().toString());
            args.putParcelable(ARG_RECT, rect);
            getSupportLoaderManager().restartLoader(LOADER_STATUS, args, this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO is this still needed? read through what mFinish does...
        if (TextUtils.isEmpty(mFilePath)) {
            // check if the dialog is still loading
            if (mFinish) {
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_REFRESH:
                if (resultCode == RESULT_OK) {
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showDialog() {
        if (mStatusLoaderResult.service == SMS) {
            // if mRect go straight to message app...
            if (mStatusLoaderResult.rect != null) {
                QuickContact.showQuickContact(this, mStatusLoaderResult.rect, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, mStatusLoaderResult.esid), QuickContact.MODE_LARGE, null);
            } else {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + mStatusLoaderResult.esid)));
                finish();
            }
        } else if (mStatusLoaderResult.service == RSS) {
            if (mStatusLoaderResult.esid != null) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(mStatusLoaderResult.esid)));
                finish();
            } else {
                (Toast.makeText(StatusDialog.this, "RSS item has no link", Toast.LENGTH_LONG)).show();
                finish();
            }
        } else if (mStatusLoaderResult.items != null) {
            // offer options for Comment, Post, Settings and Refresh
            ItemsDialogFragment.newInstance(mStatusLoaderResult.items, REQUEST_ITEMS)
                    .show(getSupportFragmentManager(), DIALOG_ITEMS);
        } else {
            if (mStatusLoaderResult.appwidgetId != Sonet.INVALID_ACCOUNT_ID) {
                // informational messages go to settings
                mFinish = true;
                startActivity(Sonet.getPackageIntent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mStatusLoaderResult.appwidgetId));
                finish();
            } else {
                (Toast.makeText(StatusDialog.this, R.string.widget_loading, Toast.LENGTH_LONG)).show();
                // force widgets rebuild
                startService(Sonet.getPackageIntent(this, SonetService.class).setAction(ACTION_REFRESH));
                finish();
            }
        }
    }

    private String[] getAllWidgets() {
        // validate appwidgetids with appwidgetmanager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetIds = Sonet.getWidgets(getApplicationContext(), appWidgetManager);

        // older versions had a null widget, remove them
        getContentResolver().delete(Widgets.getContentUri(StatusDialog.this), Widgets.WIDGET + "=?", new String[]{""});
        getContentResolver().delete(WidgetAccounts.getContentUri(StatusDialog.this), WidgetAccounts.WIDGET + "=?", new String[]{""});

        String[] widgetsOptions = new String[mAppWidgetIds.length];

        for (int i = 0; i < mAppWidgetIds.length; i++) {
            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(mAppWidgetIds[i]);
            String providerName = info.provider.getClassName();
            widgetsOptions[i] = Integer.toString(mAppWidgetIds[i]) + " (" + providerName + ")";
        }

        return widgetsOptions;
    }

    private boolean dismissDialog(@DialogTag String tag) {
        DialogFragment dialogFragment = (DialogFragment) getSupportFragmentManager().findFragmentByTag(tag);

        if (dialogFragment != null) {
            dialogFragment.dismiss();
            return true;
        }

        return false;
    }

    private void chooseWidget(@RequestCode int requestCode, @DialogTag String tag) {
        // no widget sent in, dialog to select one
        // TODO change the way this HashMap is populated, drop mAppWidgetIds
        String[] widgets = getAllWidgets();

        if (widgets.length > 0) {
            HashMap<Integer, String> widgetItems = new HashMap<>();
            int index = 0;

            for (int id : mAppWidgetIds) {
                widgetItems.put(id, widgets[index]);
                index++;
            }

            ChooseWidgetDialogFragment.newInstance(widgetItems, requestCode)
                    .show(getSupportFragmentManager(), tag);
        } else {
            Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_STATUS:
                return new StatusLoader(this, Uri.parse(args.getString(ARG_DATA)), args.<Rect>getParcelable(ARG_RECT));

            case LOADER_ACCOUNTS:
                return new CursorLoader(this,
                        Accounts.getContentUri(this),
                        new String[]{Accounts._ID, Accounts.ACCOUNTS_QUERY},
                        null,
                        null,
                        null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {

        switch (loader.getId()) {
            case LOADER_STATUS:
                dismissDialog(DIALOG_LOADING_STATUS);

                if (data instanceof StatusLoader.Result) {
                    mStatusLoaderResult = (StatusLoader.Result) data;
                }

                showDialog();
                break;

            case LOADER_ACCOUNTS:
                dismissDialog(DIALOG_LOADING_ACCOUNTS);

                if (data instanceof Cursor) {
                    Cursor cursor = (Cursor) data;

                    if (cursor.moveToFirst()) {
                        int indexId = cursor.getColumnIndex(Accounts._ID);
                        int indexUsername = cursor.getColumnIndex(Accounts.USERNAME);

                        HashMap<Long, String> accounts = new HashMap<>();

                        while (!cursor.isAfterLast()) {
                            accounts.put(cursor.getLong(indexId), cursor.getString(indexUsername));
                            cursor.moveToNext();
                        }

                        ChooseAccountDialogFragment.newInstance(accounts, getString(R.string.accounts), REQUEST_CHOOSE_ACCOUNT)
                                .show(getSupportFragmentManager(), DIALOG_CHOOSE_ACCOUNT);
                    } else {
                        Toast.makeText(StatusDialog.this, getString(R.string.error_status), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
                break;

            case LOADER_PROFILE:
                dismissDialog(DIALOG_LOADING_PROFILE);

                if (data instanceof String) {
                    String url = (String) data;
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
                } else {
                    Toast.makeText(this, mStatusLoaderResult.serviceName + " " + getString(R.string.failure), Toast.LENGTH_LONG).show();
                }

                finish();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        switch (loader.getId()) {
            case LOADER_STATUS:
                DialogFragment dialogFragment = (DialogFragment) getSupportFragmentManager().findFragmentByTag(DIALOG_LOADING_STATUS);

                if (dialogFragment != null) {
                    dialogFragment.dismiss();
                }
                break;
        }
    }

    @Override
    public void onResult(int requestCode, int result, Intent data) {
        switch (requestCode) {
            case REQUEST_LOADING_STATUS:
                finish();
                break;

            case REQUEST_CONFIRM_UPLOAD:
                if (result == RESULT_OK) {
                    startActivityForResult(Sonet.getPackageIntent(getApplicationContext(), SonetCreatePost.class)
                                    .putExtra(Widgets.INSTANT_UPLOAD, mFilePath),
                            RESULT_REFRESH);
                } else {
                    finish();
                }
                break;

            case REQUEST_ITEMS:
                if (result == RESULT_OK) {
                    int which = ItemsDialogFragment.getWhich(data, 0);

                    switch (which) {
                        case StatusLoader.Result.COMMENT:
                            if (mStatusLoaderResult.appwidgetId != -1) {
                                if (mStatusLoaderResult.service == GOOGLEPLUS) {
                                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://plus.google.com")));
                                } else if (mStatusLoaderResult.service == PINTEREST) {
                                    if (mStatusLoaderResult.sid != null) {
                                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format(PINTEREST_PIN, mStatusLoaderResult.sid))));
                                    } else {
                                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://pinterest.com")));
                                    }
                                } else {
                                    startActivity(Sonet.getPackageIntent(this, SonetComments.class).setData(mStatusLoaderResult.data));
                                }
                            } else {
                                (Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
                            }

                            finish();
                            break;

                        case StatusLoader.Result.POST:
                            if (mStatusLoaderResult.appwidgetId != -1) {
                                if (mStatusLoaderResult.service == GOOGLEPLUS) {
                                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://plus.google.com")));
                                } else if (mStatusLoaderResult.service == PINTEREST) {
                                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://pinterest.com")));
                                } else {
                                    startActivity(Sonet.getPackageIntent(this, SonetCreatePost.class).setData(Uri.withAppendedPath(Accounts.getContentUri(StatusDialog.this), Long.toString(mStatusLoaderResult.accountId))));
                                }

                                finish();
                            } else {
                                // no widget sent in, load accounts to select
                                LoadingDialogFragment.newInstance(REQUEST_LOADING_ACCOUNTS)
                                        .show(getSupportFragmentManager(), DIALOG_LOADING_ACCOUNTS);
                                getSupportLoaderManager().restartLoader(LOADER_ACCOUNTS, null, this);
                            }
                            break;

                        case StatusLoader.Result.SETTINGS:
                            if (mStatusLoaderResult.appwidgetId != -1) {
                                startActivity(Sonet.getPackageIntent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mStatusLoaderResult.appwidgetId));
                                finish();
                            } else {
                                chooseWidget(REQUEST_CHOOSE_WIDGET, DIALOG_CHOOSE_WIDGET);
                            }
                            break;

                        case StatusLoader.Result.NOTIFICATIONS:
                            startActivity(Sonet.getPackageIntent(this, SonetNotifications.class));
                            finish();
                            break;

                        case StatusLoader.Result.REFRESH:
                            if (mStatusLoaderResult.appwidgetId != -1) {
                                Toast.makeText(getApplicationContext(), getString(R.string.refreshing), Toast.LENGTH_LONG).show();
                                startService(Sonet.getPackageIntent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mStatusLoaderResult.appwidgetId}));
                            } else {
                                chooseWidget(REQUEST_REFRESH_WIDGET, DIALOG_REFRESH_WIDGET);
                            }
                            break;

                        case StatusLoader.Result.PROFILE:
                            LoadingDialogFragment.newInstance(REQUEST_LOADING_PROFILE)
                                    .show(getSupportFragmentManager(), DIALOG_LOADING_PROFILE);
                            Bundle args = new Bundle();
                            args.putLong(ARG_ACCOUNT_ID, mStatusLoaderResult.accountId);
                            args.putString(ARG_ESID, mStatusLoaderResult.esid);
                            getSupportLoaderManager().restartLoader(LOADER_PROFILE, args, this);
                            break;

                        default:
                            if (mStatusLoaderResult.itemsData != null && which < mStatusLoaderResult.itemsData.length && mStatusLoaderResult.itemsData[which] != null) {
                                // open link
                                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(mStatusLoaderResult.itemsData[which])));
                            } else {
                                Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG).show();
                            }

                            finish();
                            break;
                    }
                }
                break;

            case REQUEST_CHOOSE_ACCOUNT:
                if (result == RESULT_OK) {
                    long id = ChooseAccountDialogFragment.getSelectedId(data);
                    startActivity(Sonet.getPackageIntent(StatusDialog.this, SonetCreatePost.class)
                            .setData(Uri.withAppendedPath(Accounts.getContentUri(StatusDialog.this), Long.toString(id))));
                }

                finish();
                break;

            case REQUEST_LOADING_ACCOUNTS:
                if (result != RESULT_OK) {
                    finish();
                }
                break;

            case REQUEST_CHOOSE_WIDGET:
                if (result == RESULT_OK) {
                    int id = ChooseWidgetDialogFragment.getSelectedId(data);
                    startActivity(Sonet.getPackageIntent(StatusDialog.this, ManageAccounts.class)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id));
                }

                finish();
                break;

            case REQUEST_REFRESH_WIDGET:
                if (result == RESULT_OK) {
                    int id = ChooseWidgetDialogFragment.getSelectedId(data);
                    Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_LONG).show();
                    startService(Sonet.getPackageIntent(StatusDialog.this, SonetService.class)
                            .setAction(ACTION_REFRESH)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{id}));
                }

                finish();
                break;

            case REQUEST_LOADING_PROFILE:
                if (result != RESULT_OK) {
                    finish();
                }
                break;
        }
    }
}
