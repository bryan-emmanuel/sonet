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
public class MessageSettingsDialogFragment extends BaseColorSizeBackgroundSettingsDialogFragment implements CompoundButton.OnCheckedChangeListener {

    private static final String ARG_ICON = "icon";

    public static MessageSettingsDialogFragment newInstance(int requestCode, int color, int size, int background, boolean icon) {
        MessageSettingsDialogFragment dialogFragment = new MessageSettingsDialogFragment();
        dialogFragment.setArguments(requestCode, color, size, background);
        dialogFragment.getArguments().putBoolean(ARG_ICON, icon);
        return dialogFragment;
    }

    @Override
    protected int getTitle() {
        return R.string.settings_message;
    }

    @Override
    protected int getLayout() {
        return R.layout.settings_message;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        CheckBox icon = (CheckBox) root.findViewById(R.id.icon);
        icon.setChecked(getArguments().getBoolean(ARG_ICON));
        icon.setOnCheckedChangeListener(this);

        return root;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.icon:
                getArguments().putBoolean(ARG_ICON, isChecked);
                break;
        }
    }

    public static boolean hasIcon(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_ICON, defaultValue);
    }
}
