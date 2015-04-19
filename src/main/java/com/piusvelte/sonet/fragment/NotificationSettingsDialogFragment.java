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
 * Created by bemmanuel on 4/11/15.
 */
public class NotificationSettingsDialogFragment extends BaseDialogFragment implements CompoundButton.OnCheckedChangeListener {

    private static final String ARG_SOUND = "sound";
    private static final String ARG_VIBRATE = "vibrate";
    private static final String ARG_LIGHTS = "lights";

    public static NotificationSettingsDialogFragment newInstance(boolean sound, boolean vibrate, boolean lights, int requestCode) {
        NotificationSettingsDialogFragment dialogFragment = new NotificationSettingsDialogFragment();
        dialogFragment.setRequestCode(requestCode);

        Bundle args = dialogFragment.getArguments();
        args.putBoolean(ARG_SOUND, sound);
        args.putBoolean(ARG_VIBRATE, vibrate);
        args.putBoolean(ARG_LIGHTS, lights);

        return dialogFragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.settings_notification);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        View root = inflater.inflate(R.layout.settings_notification, container, false);

        CheckBox chk_sound = (CheckBox) root.findViewById(R.id.sound);
        chk_sound.setChecked(args.getBoolean(ARG_SOUND));
        chk_sound.setOnCheckedChangeListener(this);

        CheckBox chk_vibrate = (CheckBox) root.findViewById(R.id.vibrate);
        chk_vibrate.setChecked(args.getBoolean(ARG_VIBRATE));
        chk_vibrate.setOnCheckedChangeListener(this);

        CheckBox chk_lights = (CheckBox) root.findViewById(R.id.lights);
        chk_lights.setChecked(args.getBoolean(ARG_LIGHTS));
        chk_lights.setOnCheckedChangeListener(this);

        return root;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        deliverResult(Activity.RESULT_OK);
    }

    public static boolean hasSound(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_SOUND, defaultValue);
    }

    public static boolean hasVibrate(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_VIBRATE, defaultValue);
    }

    public static boolean hasLights(@Nullable Intent intent, boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getBooleanExtra(ARG_LIGHTS, defaultValue);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.sound:
                getArguments().putBoolean(ARG_SOUND, isChecked);
                break;

            case R.id.vibrate:
                getArguments().putBoolean(ARG_VIBRATE, isChecked);
                break;

            case R.id.lights:
                getArguments().putBoolean(ARG_LIGHTS, isChecked);
                break;
        }
    }
}
