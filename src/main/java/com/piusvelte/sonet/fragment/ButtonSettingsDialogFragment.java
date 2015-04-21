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
public class ButtonSettingsDialogFragment extends BaseColorSizeBackgroundSettingsDialogFragment implements CompoundButton.OnCheckedChangeListener {

    private static final String ARG_HAS_BUTTONS = "has_buttons";

    public static MessageSettingsDialogFragment newInstance(int requestCode, int color, int size, int background, boolean hasButtons) {
        MessageSettingsDialogFragment dialogFragment = new MessageSettingsDialogFragment();
        dialogFragment.setArguments(requestCode, color, size, background);
        dialogFragment.getArguments().putBoolean(ARG_HAS_BUTTONS, hasButtons);
        return dialogFragment;
    }

    @Override
    protected int getTitle() {
        return R.string.settings_buttons;
    }

    @Override
    protected int getLayout() {
        return R.layout.settings_buttons;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        CheckBox hasButtons = (CheckBox) root.findViewById(R.id.hasbuttons);
        hasButtons.setChecked(getArguments().getBoolean(ARG_HAS_BUTTONS));
        hasButtons.setOnCheckedChangeListener(this);

        return root;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.hasbuttons:
                getArguments().putBoolean(ARG_HAS_BUTTONS, isChecked);
                break;
        }
    }

    public static boolean hasButtons(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_HAS_BUTTONS, defaultValue);
    }
}
