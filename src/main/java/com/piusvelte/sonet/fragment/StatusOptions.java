package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.StringDef;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.piusvelte.sonet.About;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetComments;
import com.piusvelte.sonet.SonetCreatePost;
import com.piusvelte.sonet.SonetService;
import com.piusvelte.sonet.loader.ProfileUrlLoader;
import com.piusvelte.sonet.loader.StatusLoader;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.WidgetAccounts;
import com.piusvelte.sonet.provider.Widgets;
import com.piusvelte.sonet.social.Pinterest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.GOOGLEPLUS;
import static com.piusvelte.sonet.Sonet.INVALID_ACCOUNT_ID;
import static com.piusvelte.sonet.Sonet.PINTEREST;
import static com.piusvelte.sonet.Sonet.RSS;
import static com.piusvelte.sonet.Sonet.SMS;

/**
 * Created by bemmanuel on 5/8/15.
 */
@Deprecated
public class StatusOptions extends ListFragment implements LoaderManager.LoaderCallbacks {

    private static final String OPTION_NAME = "name";

    private static final int LOADER_STATUS = 0;
    private static final int LOADER_PROFILE = 1;

    private static final int REQUEST_LOADING_STATUS = 0;
    private static final int REQUEST_CHOOSE_ACCOUNT = 1;
    private static final int REQUEST_LOADING_ACCOUNTS = 2;
    private static final int REQUEST_REFRESH_WIDGET = 3;
    private static final int REQUEST_LOADING_PROFILE = 4;

    @IntDef({ REQUEST_LOADING_STATUS,
            REQUEST_CHOOSE_ACCOUNT,
            REQUEST_LOADING_ACCOUNTS,
            REQUEST_REFRESH_WIDGET,
            REQUEST_LOADING_PROFILE })
    @Retention(RetentionPolicy.SOURCE)
    private @interface RequestCode {
    }

    private static final String DIALOG_CHOOSE_ACCOUNT = "dialog:choose_account";
    private static final String DIALOG_REFRESH_WIDGET = "dialog:refresh_widget";

    @StringDef({ DIALOG_CHOOSE_ACCOUNT, DIALOG_REFRESH_WIDGET })
    @Retention(RetentionPolicy.SOURCE)
    private @interface DialogTag {
    }

    private static final String ARG_DATA = "data";
    private static final String ARG_ACCOUNT_ID = "account_id";
    private static final String ARG_ESID = "esid";

    private StatusLoader.Result mStatusLoaderResult;

    @Deprecated
    private int[] mAppWidgetIds;

    private View mLoadingView;
    private SimpleAdapter mAdapter;
    private List<HashMap<String, String>> mOptions = new ArrayList<>();

    public static StatusOptions newInstance(String data) {
        StatusOptions statusOptions = new StatusOptions();
        Bundle args = new Bundle();
        args.putString(ARG_DATA, data);
        statusOptions.setArguments(args);
        return statusOptions;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.status_options, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLoadingView = view.findViewById(R.id.loading);

        mAdapter = new SimpleAdapter(getActivity(),
                mOptions,
                android.R.layout.simple_list_item_1,
                new String[] { OPTION_NAME },
                new int[] { android.R.id.text1 });

        setListAdapter(mAdapter);

        getLoaderManager().initLoader(LOADER_STATUS, getArguments(), this);
    }

    @Override
    public void onDestroyView() {
        mLoadingView = null;
        super.onDestroyView();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case StatusLoader.Result.COMMENT:
                if (mStatusLoaderResult.appwidgetId != -1) {
                    if (mStatusLoaderResult.service == GOOGLEPLUS) {
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://plus.google.com")));
                    } else if (mStatusLoaderResult.service == PINTEREST) {
                        if (mStatusLoaderResult.sid != null) {
                            startActivity(new Intent(Intent.ACTION_VIEW)
                                    .setData(Uri.parse(String.format(Pinterest.PINTEREST_PIN, mStatusLoaderResult.sid))));
                        } else {
                            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://pinterest.com")));
                        }
                    } else {
                        startActivity(new Intent(getActivity(), SonetComments.class).setData(mStatusLoaderResult.data));
                    }
                } else {
                    (Toast.makeText(getActivity(), getString(R.string.error_status), Toast.LENGTH_LONG)).show();
                }

                getActivity().finish();
                break;

            case StatusLoader.Result.POST:
                if (mStatusLoaderResult.appwidgetId != -1) {
                    if (mStatusLoaderResult.service == GOOGLEPLUS) {
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://plus.google.com")));
                    } else if (mStatusLoaderResult.service == PINTEREST) {
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://pinterest.com")));
                    } else {
                        startActivity(new Intent(getActivity(), SonetCreatePost.class).setData(
                                Uri.withAppendedPath(Accounts.getContentUri(getActivity()),
                                        Long.toString(mStatusLoaderResult.accountId))));
                    }

                    getActivity().finish();
                } else {
                    // no widget sent in, load accounts to select
                    getChildFragmentManager().beginTransaction()
                            .add(ChooseAccount.newInstance(REQUEST_CHOOSE_ACCOUNT), DIALOG_CHOOSE_ACCOUNT)
                            .addToBackStack(null)
                            .commit();
                }
                break;

            case StatusLoader.Result.NOTIFICATIONS:
                // TODO go to Notifications *in* About
                startActivity(new Intent(getActivity(), About.class));
                getActivity().finish();
                break;

            case StatusLoader.Result.REFRESH:
                if (mStatusLoaderResult.appwidgetId != -1) {
                    Toast.makeText(getActivity(), getString(R.string.refreshing), Toast.LENGTH_LONG).show();
                    getActivity().startService(new Intent(getActivity(), SonetService.class).setAction(ACTION_REFRESH)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { mStatusLoaderResult.appwidgetId }));
                } else {
                    chooseWidget(REQUEST_REFRESH_WIDGET, DIALOG_REFRESH_WIDGET);
                }
                break;

            case StatusLoader.Result.PROFILE:
                mLoadingView.setVisibility(View.VISIBLE);
                Bundle args = new Bundle();
                args.putLong(ARG_ACCOUNT_ID, mStatusLoaderResult.accountId);
                args.putString(ARG_ESID, mStatusLoaderResult.esid);
                getLoaderManager().restartLoader(LOADER_PROFILE, args, this);
                break;

            default:
                if (mStatusLoaderResult.itemsData != null && position < mStatusLoaderResult.itemsData.length && mStatusLoaderResult
                        .itemsData[position] != null) {
                    // open link
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(mStatusLoaderResult.itemsData[position])));
                } else {
                    Toast.makeText(getActivity(), getString(R.string.error_status), Toast.LENGTH_LONG).show();
                }

                getActivity().finish();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_LOADING_STATUS:
                getActivity().finish();
                break;

            case REQUEST_CHOOSE_ACCOUNT:
                if (resultCode == Activity.RESULT_OK) {
                    long accountId = ChooseAccount.getSelectedId(data);

                    if (accountId != INVALID_ACCOUNT_ID) {
                        startActivity(new Intent(getActivity(), SonetCreatePost.class)
                                .setData(Uri.withAppendedPath(Accounts.getContentUri(getActivity()), Long.toString(accountId))));
                    }
                }

                Fragment fragment = getChildFragmentManager().findFragmentByTag(DIALOG_CHOOSE_ACCOUNT);

                if (fragment != null) {
                    getChildFragmentManager().beginTransaction()
                            .remove(fragment)
                            .commit();
                }
                break;

            case REQUEST_LOADING_ACCOUNTS:
                if (resultCode != Activity.RESULT_OK) {
                    getActivity().finish();
                }
                break;

            case REQUEST_REFRESH_WIDGET:
                if (resultCode == Activity.RESULT_OK) {
                    int id = ChooseWidgetDialogFragment.getSelectedId(data);
                    Toast.makeText(getActivity(), getString(R.string.refreshing), Toast.LENGTH_LONG).show();
                    getActivity().startService(new Intent(getActivity(), SonetService.class)
                            .setAction(ACTION_REFRESH)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { id }));
                }

                getActivity().finish();
                break;

            case REQUEST_LOADING_PROFILE:
                if (resultCode != Activity.RESULT_OK) {
                    getActivity().finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
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
                    .show(getChildFragmentManager(), tag);
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_status), Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    private String[] getAllWidgets() {
        // validate appwidgetids with appwidgetmanager
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getActivity());
        mAppWidgetIds = Sonet.getWidgets(getActivity(), appWidgetManager);

        // older versions had a null widget, remove them
        getActivity().getContentResolver().delete(Widgets.getContentUri(getActivity()), Widgets.WIDGET + "=?", new String[] { "" });
        getActivity().getContentResolver().delete(WidgetAccounts.getContentUri(getActivity()), WidgetAccounts.WIDGET + "=?", new String[] { "" });

        String[] widgetsOptions = new String[mAppWidgetIds.length];

        for (int i = 0; i < mAppWidgetIds.length; i++) {
            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(mAppWidgetIds[i]);
            String providerName = info.provider.getClassName();
            widgetsOptions[i] = Integer.toString(mAppWidgetIds[i]) + " (" + providerName + ")";
        }

        return widgetsOptions;
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_STATUS:
                return new StatusLoader(getActivity(), Uri.parse(args.getString(ARG_DATA)));

            case LOADER_PROFILE:
                return new ProfileUrlLoader(getActivity(), args.getLong(ARG_ACCOUNT_ID), args.getString(ARG_ESID));

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {

        switch (loader.getId()) {
            case LOADER_STATUS:
                mLoadingView.setVisibility(View.GONE);

                if (data instanceof StatusLoader.Result) {
                    mStatusLoaderResult = (StatusLoader.Result) data;

                    if (mStatusLoaderResult.service == SMS) {
                        startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + mStatusLoaderResult.esid)));
                        getActivity().finish();
                    } else if (mStatusLoaderResult.service == RSS) {
                        if (mStatusLoaderResult.esid != null) {
                            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(mStatusLoaderResult.esid)));
                            getActivity().finish();
                        } else {
                            (Toast.makeText(getActivity(), "RSS item has no link", Toast.LENGTH_LONG)).show();
                            getActivity().finish();
                        }
                    } else if (mStatusLoaderResult.items != null) {
                        mOptions.clear();

                        for (String item : mStatusLoaderResult.items) {
                            HashMap<String, String> itemMap = new HashMap<>();
                            itemMap.put(OPTION_NAME, item);
                            mOptions.add(itemMap);
                        }

                        mAdapter.notifyDataSetChanged();
                    } else {
                        (Toast.makeText(getActivity(), R.string.widget_loading, Toast.LENGTH_LONG)).show();
                        // force widgets rebuild
                        getActivity().startService(new Intent(getActivity(), SonetService.class).setAction(ACTION_REFRESH));
                        getActivity().finish();
                    }
                }
                break;

            case LOADER_PROFILE:
                mLoadingView.setVisibility(View.GONE);

                if (data instanceof String) {
                    String url = (String) data;
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
                } else {
                    Toast.makeText(getActivity(), mStatusLoaderResult.serviceName + " " + getString(R.string.failure), Toast.LENGTH_LONG).show();
                }

                getActivity().finish();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // NO-OP
    }
}
