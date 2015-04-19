package com.piusvelte.sonet.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.piusvelte.sonet.R;

/**
 * Created by bemmanuel on 4/18/15.
 */
public class ProfileSettingsDialogFragment extends BaseColorSizeBackgroundSettingsDialogFragment implements CompoundButton.OnCheckedChangeListener {

    private static final String ARG_PROFILE = "profile";

    public static MessageSettingsDialogFragment newInstance(int requestCode, int background, boolean profile) {
        MessageSettingsDialogFragment dialogFragment = new MessageSettingsDialogFragment();
        dialogFragment.setArguments(requestCode, 0, 0, background);
        dialogFragment.getArguments().putBoolean(ARG_PROFILE, profile);
        return dialogFragment;
    }

    @Override
    protected int getTitle() {
        return R.string.settings_profile;
    }

    @Override
    protected int getLayout() {
        return R.layout.settings_profile;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        CheckBox profile = (CheckBox) root.findViewById(R.id.display_profile);
        profile.setChecked(getArguments().getBoolean(ARG_PROFILE));
        profile.setOnCheckedChangeListener(this);

        return root;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.display_profile:
                getArguments().putBoolean(ARG_PROFILE, isChecked);
                break;
        }
    }

    public static boolean hasProfile(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_PROFILE, defaultValue);
    }
}
