package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.piusvelte.sonet.ColorPickerDialog;

/**
 * Created by bemmanuel on 4/18/15.
 */
public class ColorPickerDialogFragment extends BaseDialogFragment implements ColorPickerDialog.OnColorChangedListener {

    private static final String ARG_COLOR = "color";

    public static ColorPickerDialogFragment newInstance(int requestCode, int color) {
        ColorPickerDialogFragment dialogFragment = new ColorPickerDialogFragment();
        dialogFragment.setRequestCode(requestCode);

        Bundle args = dialogFragment.getArguments();
        args.putInt(ARG_COLOR, color);

        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new ColorPickerDialog(getActivity(), this, getArguments().getInt(ARG_COLOR));
    }

    @Override
    public void colorChanged(int color) {
        getArguments().putInt(ARG_COLOR, color);
        deliverResult(Activity.RESULT_OK);
    }

    @Override
    public void colorUpdate(int color) {
        // NO-OP
    }

    public static int getColor(@Nullable Intent data, int defaultValue) {
        if (data == null) {
            return defaultValue;
        }

        return data.getIntExtra(ARG_COLOR, defaultValue);
    }
}
