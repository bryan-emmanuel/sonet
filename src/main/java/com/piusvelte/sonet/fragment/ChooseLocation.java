package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.loader.LocationLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.piusvelte.sonet.Sonet.INVALID_ACCOUNT_ID;

/**
 * Created by bemmanuel on 4/30/15.
 */
public class ChooseLocation extends BaseDialogFragment implements LoaderManager.LoaderCallbacks<LocationLoader.LocationResult>,
        AdapterView.OnItemClickListener {

    private static final String ARG_REQUEST_CODE = "request_code";
    private static final String ARG_ID = "id";
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_LOCATION = "location";

    private static final int LOADER_LOCATIONS = 0;

    private View mLoadingView;
    private SimpleAdapter mAdapter;
    private List<HashMap<String, String>> mLocations = new ArrayList<>();

    public static ChooseLocation newInstance(int requestCode, long accountId, String latitude, String longitude) {
        ChooseLocation fragment = new ChooseLocation();
        fragment.setRequestCode(requestCode);
        Bundle args = fragment.getArguments();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        args.putLong(ARG_ID, accountId);
        args.putString(ARG_LATITUDE, latitude);
        args.putString(ARG_LONGITUDE, longitude);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.add_location);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.loading_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLoadingView = view.findViewById(R.id.loading);

        mAdapter = new SimpleAdapter(getActivity(),
                mLocations,
                android.R.layout.simple_list_item_1,
                new String[] { ARG_LOCATION },
                new int[] { android.R.id.text1 });

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setEmptyView(view.findViewById(android.R.id.empty));
        listView.setOnItemClickListener(this);
        listView.setAdapter(mAdapter);

        mLoadingView.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(LOADER_LOCATIONS, getArguments(), this);
    }

    @Override
    public void onDestroyView() {
        mLoadingView = null;
        super.onDestroyView();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        HashMap<String, String> selectedLocation = mLocations.get(position);
        getArguments().putString(ARG_LOCATION, selectedLocation.get(ARG_ID));
        deliverResult(Activity.RESULT_OK);
        dismiss();
    }

    @Override
    public Loader<LocationLoader.LocationResult> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_LOCATIONS:
                return new LocationLoader(getActivity(),
                        args.getLong(ARG_ID),
                        args.getString(ARG_LATITUDE),
                        args.getString(ARG_LONGITUDE));

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<LocationLoader.LocationResult> loader, LocationLoader.LocationResult data) {
        switch (loader.getId()) {
            case LOADER_LOCATIONS:
                mLoadingView.setVisibility(View.GONE);

                if (data != null) {
                    for (Map.Entry<String, String> location : data.locations.entrySet()) {
                        HashMap<String, String> adapterLocation = new HashMap<>();
                        adapterLocation.put(ARG_ID, location.getKey());
                        adapterLocation.put(ARG_LOCATION, location.getValue());
                        mLocations.add(adapterLocation);
                    }

                    mAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.failure), Toast.LENGTH_LONG).show();
                    dismiss();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<LocationLoader.LocationResult> loader) {
        // NO-OP
    }

    public static long getAccountId(@NonNull Intent intent) {
        return intent.getLongExtra(ARG_ID, INVALID_ACCOUNT_ID);
    }

    public static String getLatitude(@NonNull Intent intent) {
        return intent.getStringExtra(ARG_LATITUDE);
    }

    public static String getLongitude(@NonNull Intent intent) {
        return intent.getStringExtra(ARG_LONGITUDE);
    }

    public static String getLocation(@NonNull Intent intent) {
        return intent.getStringExtra(ARG_LOCATION);
    }
}
