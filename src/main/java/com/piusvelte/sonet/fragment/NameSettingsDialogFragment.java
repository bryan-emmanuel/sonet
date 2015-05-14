package com.piusvelte.sonet.fragment;

import com.piusvelte.sonet.R;

/**
 * Created by bemmanuel on 4/11/15.
 */
public class NameSettingsDialogFragment extends BaseColorSizeBackgroundSettingsDialogFragment {

    public static NameSettingsDialogFragment newInstance(int requestCode, int color, int size) {
        NameSettingsDialogFragment dialogFragment = new NameSettingsDialogFragment();
        dialogFragment.setArguments(requestCode, color, size, 0);
        return dialogFragment;
    }

    @Override
    protected int getTitle() {
        return R.string.settings_name;
    }

    @Override
    protected int getLayout() {
        return R.layout.settings_name;
    }
}
