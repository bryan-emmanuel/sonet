package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.piusvelte.sonet.R;

/**
 * Created by bemmanuel on 4/20/15.
 */
public class UpdateSettingsDialogFragment extends BaseDialogFragment
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String ARG_UPDATE_INTERVAL = "update_interval";
    private static final String ARG_STATUS_COUNT = "status_count";
    private static final String ARG_BACKGROUND_UPDATE = "background_update";

    private static final int REQUEST_INTERVAL = 0;
    private static final int REQUEST_STATUS_COUNT = 1;

    private static final String DIALOG_INTERVAL = "dialog:interval";
    private static final String DIALOG_STATUS_COUNT = "dialog:status_count";

    public static UpdateSettingsDialogFragment newInstance(int requestCode, int updateInterval, int statusCount, boolean updateInBackground) {
        UpdateSettingsDialogFragment dialogFragment = new UpdateSettingsDialogFragment();
        dialogFragment.setRequestCode(requestCode);
        Bundle args = dialogFragment.getArguments();
        args.putInt(ARG_UPDATE_INTERVAL, updateInterval);
        args.putInt(ARG_STATUS_COUNT, statusCount);
        args.putBoolean(ARG_BACKGROUND_UPDATE, updateInBackground);
        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.settings_update);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.settings_update, container, false);

        root.findViewById(R.id.interval).setOnClickListener(this);
        root.findViewById(R.id.statuses_per_account).setOnClickListener(this);

        CheckBox hasBackgroundUpdate = (CheckBox) root.findViewById(R.id.background_update);
        hasBackgroundUpdate.setChecked(getArguments().getBoolean(ARG_BACKGROUND_UPDATE));
        hasBackgroundUpdate.setOnCheckedChangeListener(this);

        return root;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        deliverResult(Activity.RESULT_OK);
    }

    @Override
    public void onClick(View v) {
        int which = 0;
        String[] items;

        switch (v.getId()) {
            case R.id.interval:
                int interval = getArguments().getInt(ARG_UPDATE_INTERVAL);
                String[] values = getResources().getStringArray(R.array.interval_values);

                for (int i = 0; i < values.length; i++) {
                    if (Integer.parseInt(values[i]) == interval) {
                        which = i;
                        break;
                    }
                }

                items = getResources().getStringArray(R.array.interval_entries);
                SingleChoiceDialogFragment.newInstance(items, which, REQUEST_INTERVAL)
                        .show(getChildFragmentManager(), DIALOG_INTERVAL);
                break;

            case R.id.statuses_per_account:
                int count = getArguments().getInt(ARG_STATUS_COUNT);
                items = getResources().getStringArray(R.array.status_counts);

                for (int i = 0; i < items.length; i++) {
                    if (Integer.parseInt(items[i]) == count) {
                        which = i;
                        break;
                    }
                }

                SingleChoiceDialogFragment.newInstance(items, which, REQUEST_STATUS_COUNT)
                        .show(getChildFragmentManager(), DIALOG_STATUS_COUNT);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.background_update:
                getArguments().putBoolean(ARG_BACKGROUND_UPDATE, isChecked);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_INTERVAL:
                if (resultCode == Activity.RESULT_OK) {
                    int which = SingleChoiceDialogFragment.getWhich(data, 0);
                    getArguments().putInt(ARG_UPDATE_INTERVAL, Integer.parseInt(getResources().getStringArray(R.array.interval_values)[which]));
                }
                break;

            case REQUEST_STATUS_COUNT:
                if (resultCode == Activity.RESULT_OK) {
                    int which = SingleChoiceDialogFragment.getWhich(data, 0);
                    getArguments().putInt(ARG_STATUS_COUNT, Integer.parseInt(getResources().getStringArray(R.array.status_counts)[which]));
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    public static int getUpdateInterval(@Nullable Intent intent, int defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getIntExtra(ARG_UPDATE_INTERVAL, defaultValue);
    }

    public static int getStatusCount(@Nullable Intent intent, int defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getIntExtra(ARG_STATUS_COUNT, defaultValue);
    }

    public static boolean hasBackgroundUpdate(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_BACKGROUND_UPDATE, defaultValue);
    }
}
