package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.piusvelte.eidos.Eidos;
import com.piusvelte.sonet.AccountSettings;
import com.piusvelte.sonet.OAuthLogin;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Settings;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetService;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.AccountsStyles;
import com.piusvelte.sonet.provider.StatusesStyles;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.service.AccountUpdateService;
import com.piusvelte.sonet.social.Client;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.RSS;
import static com.piusvelte.sonet.Sonet.SMS;
import static com.piusvelte.sonet.Sonet.sBFOptions;
import static com.piusvelte.sonet.SonetProvider.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetProvider.TABLE_WIDGETS;
import static com.piusvelte.sonet.SonetProvider.TABLE_WIDGET_ACCOUNTS;

/**
 * Created by bemmanuel on 4/19/15.
 */
public class AccountsList extends ListFragment implements LoaderManager.LoaderCallbacks {

    private static final int LOADER_ACCOUNTS = 0;
    private static final int LOADER_ACCOUNT = 1;

    private static final int DELETE_ID = 0;

    private static final String ARG_APP_WIDGET_ID = "app_widget_id";
    private static final String ARG_ACCOUNT_ID = "account_id";

    private static final String DIALOG_ACCOUNT_OPTIONS = "dialog:account_options";
    private static final String DIALOG_ADD_ACCOUNT = "dialog:add_account";

    private static final int REQUEST_ACCOUNT_OPTIONS = 0;
    private static final int REQUEST_ADD_ACCOUNT = 1;
    private static final int REQUEST_REFRESH = 2;

    private static final int REAUTH_ID = 0;
    private static final int SETTINGS_ID = 1;
    private static final int ENABLE_ID = 2;

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean mAddingAccount;
    private boolean mUpdateWidget = false;
    private SimpleCursorAdapter mAdapter;

    private View mLoadingView;

    public static AccountsList newInstance(int appWidgetId) {
        AccountsList accountsList = new AccountsList();
        Bundle args = new Bundle();
        args.putInt(ARG_APP_WIDGET_ID, appWidgetId);
        accountsList.setArguments(args);
        return accountsList;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppWidgetId = getArguments().getInt(ARG_APP_WIDGET_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.accounts, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLoadingView = view.findViewById(R.id.loading);
        registerForContextMenu(getListView());
        mAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.widget_item,
                null,
                new String[] { StatusesStyles.FRIEND,
                        StatusesStyles.MESSAGE,
                        StatusesStyles.CREATEDTEXT,
                        StatusesStyles.PROFILE,
                        StatusesStyles.ICON },
                new int[] { R.id.friend,
                        R.id.message,
                        R.id.created,
                        R.id.profile,
                        R.id.icon },
                0);
        mAdapter.setViewBinder(new AccountsViewBinder(getResources()));
        setListAdapter(mAdapter);
        getLoaderManager().initLoader(LOADER_ACCOUNTS, null, this);
    }

    @Override
    public void onPause() {
        if (!mAddingAccount && mUpdateWidget) {
            Eidos.requestBackup(getActivity());
            Toast.makeText(getActivity(), getString(R.string.refreshing), Toast.LENGTH_LONG)
                    .show();
            getActivity().startService(new Intent(getActivity(), SonetService.class)
                    .setAction(ACTION_REFRESH)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { mAppWidgetId }));
        }

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mLoadingView = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_manageaccounts, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.button_add_account:
                // add a new account
                String[] services = getResources().getStringArray(R.array.service_entries);
                ItemsDialogFragment.newInstance(services, REQUEST_ADD_ACCOUNT)
                        .show(getChildFragmentManager(), DIALOG_ADD_ACCOUNT);
                return true;

            case R.id.default_widget_settings:
                mAddingAccount = true;
                startActivityForResult(new Intent(getActivity(), Settings.class)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId), REQUEST_REFRESH);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.delete_account);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_ID:
                getActivity().startService(AccountUpdateService.obtainIntent(getActivity(),
                        AccountUpdateService.ACTION_DELETE,
                        ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id,
                        AppWidgetManager.INVALID_APPWIDGET_ID));
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        boolean isEnabled = ((TextView) v.findViewById(R.id.message)).getText().toString().contains("enabled");
        final CharSequence[] items = { getString(R.string.authenticate_account), getString(R.string.account_settings), getString(
                isEnabled ? R.string.disable : R.string.enable) };
        AccountOptionsDialogFragment.newInstance(REQUEST_ACCOUNT_OPTIONS, id, items, isEnabled)
                .show(getChildFragmentManager(), DIALOG_ACCOUNT_OPTIONS);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ACCOUNTS:
                int appWidgetId = getArguments().getInt(ARG_APP_WIDGET_ID);
                return new CursorLoader(getActivity(),
                        AccountsStyles.getContentUri(getActivity()),
                        new String[] {
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

                                "(case when (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" +
                                        appWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + ") is not null then " +
                                        "(select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" +
                                        appWidgetId + " and " + Widgets.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID + " limit 1)"
                                        + "when (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" +
                                        appWidgetId + " and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.DISPLAY_PROFILE + " " +
                                        "from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=" + appWidgetId + " and " + Widgets.ACCOUNT + "=-1" +
                                        " limit 1)"
                                        + "when (select " + Widgets.DISPLAY_PROFILE + " from " + TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 " +
                                        "and " + Widgets.ACCOUNT + "=-1) is not null then (select " + Widgets.DISPLAY_PROFILE + " from " +
                                        TABLE_WIDGETS + " where " + Widgets.WIDGET + "=0 and " + Widgets.ACCOUNT + "=-1 limit 1)"
                                        + "else 1 end) as " + StatusesStyles.PROFILE,

                                "(case when (select " + WidgetAccounts.WIDGET + " from " + TABLE_WIDGET_ACCOUNTS + " where " + WidgetAccounts
                                        .WIDGET + "=" + appWidgetId + " and " + WidgetAccounts.ACCOUNT + "=" + TABLE_ACCOUNTS + "." + Accounts._ID
                                        + " limit 1) is null then 'this account is disabled for this widget, select to enable' " +
                                        "else 'this account is enabled for this widget, select to change settings' end) as " + StatusesStyles
                                        .MESSAGE,

                                Accounts.USERNAME + " as " + StatusesStyles.CREATEDTEXT,

                                Accounts.SERVICE + " as " + StatusesStyles.ICON
                        },
                        null,
                        null,
                        null);

            case LOADER_ACCOUNT:
                long accountId = args.getLong(ARG_ACCOUNT_ID);
                return new CursorLoader(getActivity(),
                        Accounts.getContentUri(getActivity()),
                        new String[] { Accounts._ID, Accounts.SERVICE },
                        Accounts._ID + "=?",
                        new String[] { Long.toString(accountId) },
                        null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()) {
            case LOADER_ACCOUNTS:
                mAdapter.changeCursor((Cursor) data);
                break;

            case LOADER_ACCOUNT:
                mLoadingView.setVisibility(View.GONE);

                if (data instanceof Cursor) {
                    Cursor cursor = (Cursor) data;

                    if (cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(Accounts._ID));
                        int service = cursor.getInt(cursor.getColumnIndexOrThrow(Accounts.SERVICE));

                        if ((service != SMS) && (service != RSS)) {
                            mAddingAccount = true;
                            startActivityForResult(new Intent(getActivity(), OAuthLogin.class)
                                            .putExtra(Accounts.SERVICE, service)
                                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                                            .putExtra(Sonet.EXTRA_ACCOUNT_ID, id),
                                    REQUEST_REFRESH);
                        }
                    }

                    cursor.close();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        switch (loader.getId()) {
            case LOADER_ACCOUNTS:
                mAdapter.changeCursor(null);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ACCOUNT_OPTIONS:
                if (resultCode == Activity.RESULT_OK) {
                    long id = AccountOptionsDialogFragment.getAccountId(data, 0);
                    int which = ItemsDialogFragment.getWhich(data, 0);

                    switch (which) {
                        case REAUTH_ID:
                            Bundle args = new Bundle();
                            args.putLong(ARG_ACCOUNT_ID, id);
                            getLoaderManager().restartLoader(LOADER_ACCOUNT, args, this);
                            mLoadingView.setVisibility(View.VISIBLE);
                            break;
                        case SETTINGS_ID:
                            mAddingAccount = true;
                            startActivityForResult(new Intent(getActivity(), AccountSettings.class)
                                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, id),
                                    REQUEST_REFRESH);
                            break;
                        case ENABLE_ID:
                            boolean isEnabled = AccountOptionsDialogFragment.getIsEnabled(data, true);

                            if (isEnabled) {
                                getActivity().startService(
                                        AccountUpdateService.obtainIntent(getActivity(), AccountUpdateService.ACTION_DISABLE, id, mAppWidgetId));
                            } else {
                                // enable the account
                                getActivity().startService(
                                        AccountUpdateService.obtainIntent(getActivity(), AccountUpdateService.ACTION_ENABLE, id, mAppWidgetId));
                            }

                            mUpdateWidget = true;
                            break;
                    }
                }
                break;

            case REQUEST_ADD_ACCOUNT:
                mAddingAccount = true;
                startActivityForResult(new Intent(getActivity(), OAuthLogin.class)
                        .putExtra(Accounts.SERVICE,
                                Integer.parseInt(getResources().getStringArray(R.array.service_values)[ItemsDialogFragment.getWhich(data, 0)]))
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
                        .putExtra(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID)
                        , REQUEST_REFRESH);
                break;

            case REQUEST_REFRESH:
                mAddingAccount = false;

                if (resultCode == Activity.RESULT_OK) {
                    mUpdateWidget = true;
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private static class AccountsViewBinder implements SimpleCursorAdapter.ViewBinder {

        @NonNull
        private Resources mResources;

        AccountsViewBinder(@NonNull Resources resources) {
            mResources = resources;
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (columnIndex == cursor.getColumnIndex(StatusesStyles.FRIEND)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.MESSAGE)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.PROFILE)) {
                Bitmap bmp = BitmapFactory.decodeResource(mResources, R.drawable.ic_account_box_grey600_48dp, sBFOptions);

                if (bmp != null) {
                    ((ImageView) view).setImageBitmap(bmp);
                }

                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.CREATEDTEXT)) {
                ((TextView) view).setText(cursor.getString(columnIndex));
                return true;
            } else if (columnIndex == cursor.getColumnIndex(StatusesStyles.ICON)) {
                Bitmap bmp = BitmapFactory.decodeResource(mResources, Client.Network.get(cursor.getInt(columnIndex)).getIcon(), sBFOptions);

                if (bmp != null) {
                    ((ImageView) view).setImageBitmap(bmp);
                }

                return true;
            }

            return false;
        }
    }
}
