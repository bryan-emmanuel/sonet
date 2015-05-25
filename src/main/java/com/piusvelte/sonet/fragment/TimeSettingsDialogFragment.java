package com.piusvelte.sonet.fragment;

import android.app.Dialog;
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
 * Created by bemmanuel on 4/11/15.
 */
public class TimeSettingsDialogFragment extends BaseDialogFragment implements CompoundButton.OnCheckedChangeListener {

    private static final String ARG_24HR = "24hr";

    public static TimeSettingsDialogFragment newInstance(int requestCode, boolean time24hr) {
        TimeSettingsDialogFragment dialogFragment = new TimeSettingsDialogFragment();
        dialogFragment.setRequestCode(requestCode);
        dialogFragment.getArguments().putBoolean(ARG_24HR, time24hr);
        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.settings_time);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.settings_time, container, false);
        CheckBox time24hr = (CheckBox) root.findViewById(R.id.time24hr);
        time24hr.setChecked(getArguments().getBoolean(ARG_24HR));
        time24hr.setOnCheckedChangeListener(this);
        return root;
    }

    public static boolean is24hr(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_24HR, defaultValue);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.time24hr:
                getArguments().putBoolean(ARG_24HR, isChecked);
                break;
        }
    }
}
