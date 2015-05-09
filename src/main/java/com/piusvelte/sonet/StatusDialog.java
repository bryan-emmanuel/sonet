/*
 * Sonet - Android Social Networking Widget
 * Copyright (C) 2009 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.sonet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.piusvelte.sonet.fragment.ConfirmationDialogFragment;
import com.piusvelte.sonet.fragment.StatusOptions;
import com.piusvelte.sonet.provider.Widgets;

import mobi.intuitit.android.content.LauncherIntent;

import static com.piusvelte.sonet.Sonet.RESULT_REFRESH;

public class StatusDialog extends BaseActivity {
    private static final String TAG = "StatusDialog";

    private static final int REQUEST_CONFIRM_UPLOAD = 1;

    private static final String FRAGMENT_STATUS_OPTIONS = "status_options";

    private static final String DIALOG_CONFIRM_UPLOAD = "dialog:confirm_upload";

    private String mFilePath = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent.hasExtra(Widgets.INSTANT_UPLOAD)) {
            mFilePath = intent.getStringExtra(Widgets.INSTANT_UPLOAD);

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "upload photo?" + mFilePath);
            }

            if (getSupportFragmentManager().findFragmentByTag(DIALOG_CONFIRM_UPLOAD) == null) {
                ConfirmationDialogFragment.newInstance(R.string.uploadprompt, REQUEST_CONFIRM_UPLOAD)
                        .show(getSupportFragmentManager(), DIALOG_CONFIRM_UPLOAD);
            }
        } else {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_STATUS_OPTIONS);

            if (fragment == null) {
                Rect rect;

                if (intent.hasExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS)) {
                    rect = intent.getParcelableExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS);
                } else {
                    rect = intent.getSourceBounds();
                }

                getSupportFragmentManager().beginTransaction()
                        .add(android.R.id.content, StatusOptions.newInstance(getIntent().getData().toString(), rect), FRAGMENT_STATUS_OPTIONS)
                        .commit();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_REFRESH:
                if (resultCode == RESULT_OK) {
                    finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONFIRM_UPLOAD:
                if (resultCode == Activity.RESULT_OK) {
                    startActivityForResult(new Intent(this, SonetCreatePost.class)
                                    .putExtra(Widgets.INSTANT_UPLOAD, mFilePath),
                            RESULT_REFRESH);
                } else {
                    finish();
                }
                break;
        }
    }
}
