package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.adapter.AccountAdapter;
import com.piusvelte.sonet.adapter.PostAccountsAdapter;
import com.piusvelte.sonet.loader.AccountsProfilesLoaderCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.TWITTER;

/**
 * Created by bemmanuel on 4/30/15.
 */
public class ChoosePostAccounts extends ListFragment
        implements AccountsProfilesLoaderCallback.OnAccountsLoadedListener, AbsListView.MultiChoiceModeListener,
        PostAccountsAdapter.OnLocationClickListener {

    private static final String ARG_REQUEST_CODE = "request_code";
    private static final String ARG_SELECTED_ACCOUNTS = "selected_accounts";

    private static final int LOADER_ACCOUNTS = 0;

    private static final String DIALOG_CONFIRM_SET_LOCATION = "dialog:confirm_set_location";
    private static final String DIALOG_CHOOSE_LOCATION = "dialog:choose_location";

    private static final int REQUEST_CHOOSE_LOCATION = 0;

    // TODO move this to Client implementations
    private static final List<Integer> sLocationSupported = new ArrayList<>();

    static {
        sLocationSupported.add(TWITTER);
        sLocationSupported.add(FACEBOOK);
        sLocationSupported.add(FOURSQUARE);
    }

    private View mLoadingView;
    private List<HashMap<String, String>> mAccounts = new ArrayList<>();
    private PostAccountsAdapter mAdapter;

    public static ChoosePostAccounts newInstance(int requestCode, @NonNull HashSet<Account> selectedAccounts) {
        ChoosePostAccounts fragment = new ChoosePostAccounts();
        Bundle args = new Bundle();
        args.putInt(ARG_REQUEST_CODE, requestCode);
        args.putParcelableArrayList(ARG_SELECTED_ACCOUNTS, new ArrayList<>(selectedAccounts));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.loading_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLoadingView = view.findViewById(R.id.loading);

        ListView listView = getListView();
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        mAdapter = new PostAccountsAdapter(getActivity(), mAccounts, this);
        listView.setAdapter(mAdapter);

        mLoadingView.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(LOADER_ACCOUNTS,
                null,
                new AccountsProfilesLoaderCallback(this, LOADER_ACCOUNTS));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        mLoadingView = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHOOSE_LOCATION:
                if (resultCode == Activity.RESULT_OK) {
                    long accountId = ChooseLocation.getAccountId(data);
                    String latitude = ChooseLocation.getLatitude(data);
                    String longitude = ChooseLocation.getLongitude(data);
                    String location = ChooseLocation.getLocation(data);

                    List<Account> selectedAccounts = getArguments().getParcelableArrayList(ARG_SELECTED_ACCOUNTS);

                    for (Account account : selectedAccounts) {
                        if (account.id == accountId) {
                            account.latitude = latitude;
                            account.longitude = longitude;
                            account.location = location;
                            break;
                        }
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onAccountsLoaded(List<HashMap<String, String>> accounts) {
        mLoadingView.setVisibility(View.GONE);
        mAdapter.clearSelection();
        mAccounts.clear();

        if (accounts != null) {
            mAccounts.addAll(accounts);
        }

        mAdapter.notifyDataSetChanged();

        if (accounts != null) {
            List<Account> selection = getArguments().getParcelableArrayList(ARG_SELECTED_ACCOUNTS);

            if (selection != null) {
                int position = 0;

                for (HashMap<String, String> account : accounts) {
                    for (ChoosePostAccounts.Account selectedAccount : selection) {
                        if (AccountAdapter.getAccountId(account) == selectedAccount.id) {
                            getListView().setItemChecked(position, true);
                        }
                    }

                    position++;
                }
            }
        } else {
            mAdapter.clearSelection();
        }
    }

//    private static class MultiChoiceAdapter extends CursorAdapter {
//
//        private List<Long> selectedIds = new ArrayList<>();
//        private LayoutInflater mInflater;
//        private int mIdIndex;
//        private int mUsernameIndex;
//        private ListView mListView;
//
//        public MultiChoiceAdapter(Context context, Cursor c, ListView listView) {
//            super(context, c, false);
//            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            mListView = listView;
//        }
//
//        @Override
//        public Cursor swapCursor(Cursor newCursor) {
//            Cursor oldCursor = super.swapCursor(newCursor);
//
//            if (newCursor != null) {
//                mIdIndex = newCursor.getColumnIndexOrThrow(Accounts._ID);
//                mUsernameIndex = newCursor.getColumnIndexOrThrow(Accounts.USERNAME);
//            } else {
//                mIdIndex = 0;
//                mUsernameIndex = 0;
//            }
//
//            return oldCursor;
//        }
//
//        @Override
//        public void bindView(View view, Context context, Cursor cursor) {
//            CheckedTextView text = (CheckedTextView) view.findViewById(android.R.id.text1);
//            text.setText(cursor.getString(mUsernameIndex));
//            mListView.setItemChecked(cursor.getPosition(),
//                    selectedIds.contains(cursor.getLong(mIdIndex)));
//        }
//
//        @Override
//        public View newView(Context context, Cursor cursor, ViewGroup parent) {
//            return mInflater.inflate(R.layout.multichoice_item, parent, false);
//        }
//
//        public List<Long> getSelectedIds() {
//            return selectedIds;
//        }
//    }

    public static List<Account> getAccounts(@NonNull Intent intent) {
        return intent.getParcelableArrayListExtra(ARG_SELECTED_ACCOUNTS);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        l.setItemChecked(position, !l.isItemChecked(position));
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        List<Account> selectedAccounts = getArguments().getParcelableArrayList(ARG_SELECTED_ACCOUNTS);

        if (checked) {
            // don't add if already exists
            boolean newSelectedAccount = true;
            // TODO this shouldn't happen :/
            for (Account selectedAccount : selectedAccounts) {
                if (selectedAccount.id == id) {
                    newSelectedAccount = false;
                    break;
                }
            }

            if (newSelectedAccount) {
                Account account = new Account();
                account.id = id;
                account.service = AccountAdapter.getAccountService(mAdapter.getItem(position));
                selectedAccounts.add(account);
            }
        } else {
            ArrayList<Account> newSelectedAccounts = new ArrayList<>(selectedAccounts.size());

            for (Account account : selectedAccounts) {
                if (account.id != id) {
                    newSelectedAccounts.add(account);
                }
            }

            getArguments().putParcelableArrayList(ARG_SELECTED_ACCOUNTS, newSelectedAccounts);
            selectedAccounts = newSelectedAccounts;
        }

        mAdapter.setSelection(position, checked);
        mAdapter.notifyDataSetChanged();
        mode.setTitle(selectedAccounts.size() + " accounts");
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        int requestCode = getArguments().getInt(ARG_REQUEST_CODE);
        Intent intent = new Intent().putExtra(ARG_SELECTED_ACCOUNTS, getArguments().getParcelableArrayList(ARG_SELECTED_ACCOUNTS));

        Fragment target = getTargetFragment();

        if (target != null) {
            target.onActivityResult(requestCode, Activity.RESULT_OK, intent);
        } else {
            Fragment parent = getParentFragment();

            if (parent != null) {
                parent.onActivityResult(requestCode, Activity.RESULT_OK, intent);
            } else {
                Activity activity = getActivity();

                if (activity instanceof BaseDialogFragment.OnResultListener) {
                    ((BaseDialogFragment.OnResultListener) activity).onResult(requestCode, Activity.RESULT_OK, intent);
                }
            }
        }

        getFragmentManager().popBackStack();
    }

    @Override
    public void onLocationClick(int position) {
        HashMap<String, String> account = mAdapter.getItem(position);

        if (sLocationSupported.contains(AccountAdapter.getAccountService(account))) {
            String latitude;
            String longitude;

            // TODO FusedLocationProvider
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                latitude = Double.toString(location.getLatitude());
                longitude = Double.toString(location.getLongitude());
            } else {
                latitude = null;
                longitude = null;
            }

            if (!TextUtils.isEmpty(latitude) && !TextUtils.isEmpty(longitude)) {
                long accountId = AccountAdapter.getAccountId(account);
                // TODO DialogFragment here? This isn't visible
                getFragmentManager().beginTransaction()
                        .add(ChooseLocation.newInstance(REQUEST_CHOOSE_LOCATION, accountId, latitude, longitude), DIALOG_CHOOSE_LOCATION)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    public static class Account implements Parcelable {

        public long id;
        public int service;
        public String latitude;
        public String longitude;
        public String location;
        @Nullable
        public List<String> tags;

        public Account() {
        }

        public Account(Parcel in) {
            id = in.readLong();
            service = in.readInt();
            latitude = in.readString();
            longitude = in.readString();
            location = in.readString();
            tags = in.createStringArrayList();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(id);
            dest.writeInt(service);
            dest.writeString(latitude);
            dest.writeString(longitude);
            dest.writeString(location);
            dest.writeStringList(tags);
        }

        public static final Parcelable.Creator<Account> CREATOR = new Parcelable.Creator<Account>() {
            @Override
            public Account createFromParcel(Parcel source) {
                return new Account(source);
            }

            @Override
            public Account[] newArray(int size) {
                return new Account[size];
            }
        };
    }
}
