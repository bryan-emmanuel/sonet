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

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.piusvelte.sonet.fragment.NotificationsList;

import static com.piusvelte.sonet.Sonet.PRO;

public class SonetNotifications extends AppCompatActivity {
    // list the current notifications
    // check for cache versions in statuses first, falling back on reloading them from the service
    private static final String TAG = "SonetNotifications";

    private static final String FRAGMENT_NOTIFICATIONS_LIST = "notifications_list";

    // expanding notifications, check any statuses that have been commented on in the past 24 hours
    // this requires tracking the last comment date

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications);

        if (!getPackageName().toLowerCase().contains(PRO)) {
            AdView adView = new AdView(this, AdSize.BANNER, BuildConfig.GOOGLEAD_ID);
            ((FrameLayout) findViewById(R.id.ad)).addView(adView);
            adView.loadAd(new AdRequest());
        }

        setResult(RESULT_OK);

        Fragment notificationsList = getSupportFragmentManager().findFragmentByTag(FRAGMENT_NOTIFICATIONS_LIST);

        if (notificationsList == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.notifications_list_container, new NotificationsList(), FRAGMENT_NOTIFICATIONS_LIST)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // cancel any notifications
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Sonet.NOTIFY_ID);
    }
}
