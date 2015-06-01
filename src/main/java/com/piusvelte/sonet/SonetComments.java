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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.piusvelte.sonet.fragment.CommentsList;
import com.piusvelte.sonet.provider.StatusLinks;
import com.piusvelte.sonet.provider.StatusesStyles;

public class SonetComments extends BaseActivity {

    private static final String FRAGMENT_COMMENTS_LIST = "fragment:comments_list";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // allow posting to multiple services if an account is defined
        // allow selecting which accounts to use
        // get existing comments, allow liking|unliking those comments
        setContentView(R.layout.comments);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setupAd();

        setResult(RESULT_OK);

        Intent intent = getIntent();

        if (intent == null) {
            Toast.makeText(this, getString(R.string.failure), Toast.LENGTH_LONG).show();
            finish();
        } else {
            if (intent.hasExtra(StatusLinks.STATUS_ID)) {
                CommentsList fragment = (CommentsList) getSupportFragmentManager().findFragmentByTag(FRAGMENT_COMMENTS_LIST);

                if (fragment == null) {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.comments_list_container,
                                    CommentsList.newInstance(Uri.withAppendedPath(StatusesStyles.getContentUri(this),
                                            intent.getStringExtra(StatusLinks.STATUS_ID))),
                                    FRAGMENT_COMMENTS_LIST)
                            .commit();
                }
            } else {
                Toast.makeText(this, getString(R.string.failure), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);

        if (intent.hasExtra(StatusLinks.STATUS_ID)) {
            CommentsList fragment = (CommentsList) getSupportFragmentManager().findFragmentByTag(FRAGMENT_COMMENTS_LIST);

            if (fragment != null) {
                fragment.setData(Uri.withAppendedPath(StatusesStyles.getContentUri(this),
                        intent.getStringExtra(StatusLinks.STATUS_ID)));
            }
        } else {
            Toast.makeText(this, getString(R.string.failure), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent data) {
        // NO-OP
    }
}