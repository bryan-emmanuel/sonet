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
import android.support.v4.app.FragmentActivity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.piusvelte.sonet.fragment.CommentsList;

import static com.piusvelte.sonet.Sonet.PRO;

public class SonetComments extends FragmentActivity {

    private static final String FRAGMENT_COMMENTS_LIST = "fragment:comments_list";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // allow posting to multiple services if an account is defined
        // allow selecting which accounts to use
        // get existing comments, allow liking|unliking those comments
        setContentView(R.layout.comments);

        if (!getPackageName().toLowerCase().contains(PRO)) {
            AdView adView = new AdView(this, AdSize.BANNER, BuildConfig.GOOGLEAD_ID);
            ((FrameLayout) findViewById(R.id.ad)).addView(adView);
            adView.loadAd(new AdRequest());
        }

        setResult(RESULT_OK);

        Intent intent = getIntent();

        if (intent == null) {
            Toast.makeText(this, getString(R.string.failure), Toast.LENGTH_LONG).show();
            finish();
        } else {
            Uri data = intent.getData();

            if (data == null) {
                Toast.makeText(this, getString(R.string.failure), Toast.LENGTH_LONG).show();
                finish();
            } else {
                CommentsList fragment = (CommentsList) getSupportFragmentManager().findFragmentByTag(FRAGMENT_COMMENTS_LIST);

                if (fragment == null) {
                    getSupportFragmentManager().beginTransaction()
                            .add(R.id.comments_list_container, new CommentsList(), FRAGMENT_COMMENTS_LIST)
                            .commit();
                }
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);

        Uri data = intent.getData();

        if (data == null) {
            Toast.makeText(this, getString(R.string.failure), Toast.LENGTH_LONG).show();
            finish();
        } else {
            CommentsList fragment = (CommentsList) getSupportFragmentManager().findFragmentByTag(FRAGMENT_COMMENTS_LIST);

            if (fragment != null) {
                fragment.setData(data);
            }
        }
    }
}