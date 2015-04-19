package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.piusvelte.sonet.R;

/**
 * Created by bemmanuel on 4/11/15.
 */
public abstract class BaseColorSizeBackgroundSettingsDialogFragment extends BaseDialogFragment implements View.OnClickListener {

    private static final String ARG_COLOR = "color";
    private static final String ARG_SIZE = "size";
    private static final String ARG_BACKGROUND = "background";

    private static final int REQUEST_COLOR = 0;
    private static final int REQUEST_SIZE = 1;
    private static final int REQUEST_BACKGROUND = 2;

    private static final String DIALOG_COLOR = "dialog:color";
    private static final String DIALOG_SIZE = "dialog:size";
    private static final String DIALOG_BACKGROUND = "dialog:background";

    protected void setArguments(int requestCode, int color, int size, int background) {
        setRequestCode(requestCode);

        Bundle args = getArguments();
        args.putInt(ARG_COLOR, color);
        args.putInt(ARG_SIZE, size);
        args.putInt(ARG_BACKGROUND, background);
    }

    @StringRes
    protected abstract int getTitle();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(getTitle());
        return dialog;
    }

    @LayoutRes
    protected abstract int getLayout();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(getLayout(), container, false);

        View color = root.findViewById(R.id.color);

        if (color != null) color.setOnClickListener(this);

        View size = root.findViewById(R.id.size);

        if (size != null) size.setOnClickListener(this);

        View background = root.findViewById(R.id.background);

        if (background != null) background.setOnClickListener(this);

        return root;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        deliverResult(Activity.RESULT_OK);
    }

    public static int getColor(@Nullable Intent intent, int defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getIntExtra(ARG_COLOR, defaultValue);
    }

    public static int getSize(@Nullable Intent intent, int defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getIntExtra(ARG_SIZE, defaultValue);
    }

    public static int getBackground(@Nullable Intent intent, int defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        return intent.getIntExtra(ARG_BACKGROUND, defaultValue);
    }

    @Override
    public void onClick(View v) {
        DialogFragment dialogFragment;

        switch (v.getId()) {
            case R.id.color:
                dialogFragment = ColorPickerDialogFragment.newInstance(REQUEST_COLOR, getArguments().getInt(ARG_COLOR));
                dialogFragment.show(getChildFragmentManager(), DIALOG_COLOR);
                break;

            case R.id.size:
                int which = 0;
                String[] values = getResources().getStringArray(R.array.textsize_values);

                for (int i = 0; i < values.length; i++) {
                    if (Integer.parseInt(values[i]) == getArguments().getInt(ARG_SIZE)) {
                        which = i;
                        break;
                    }
                }

                dialogFragment = SingleChoiceDialogFragment.newInstance(values, which, REQUEST_SIZE);
                dialogFragment.show(getChildFragmentManager(), DIALOG_SIZE);
                break;

            case R.id.background:
                dialogFragment = ColorPickerDialogFragment.newInstance(REQUEST_BACKGROUND, getArguments().getInt(ARG_BACKGROUND));
                dialogFragment.show(getChildFragmentManager(), DIALOG_BACKGROUND);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_COLOR:
                getArguments().putInt(ARG_COLOR, ColorPickerDialogFragment.getColor(data, getArguments().getInt(ARG_COLOR)));
                break;

            case REQUEST_SIZE:
                getArguments().putInt(ARG_SIZE, Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[SingleChoiceDialogFragment.getWhich(data, 0)]));
                break;

            case REQUEST_BACKGROUND:
                getArguments().putInt(ARG_BACKGROUND, ColorPickerDialogFragment.getColor(data, getArguments().getInt(ARG_BACKGROUND)));
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
