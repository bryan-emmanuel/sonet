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

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;

import com.piusvelte.sonet.fragment.AccountsList;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;

public class ManageAccounts extends BaseActivity {
    private static final String TAG = "ManageAccounts";

    private static final String FRAGMENT_ACCOUNTS_LIST = "fragment:accounts_list";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accounts_container);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setupAd();

        int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

        Intent intent = getIntent();

        if (intent != null) {
            Bundle extras = intent.getExtras();

            if (extras != null) {
                appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                // if called from widget, the id is set in the action, as pendingintents must have a unique action
            } else if ((intent.getAction() != null) && (!intent.getAction().equals(ACTION_REFRESH)) && (!intent.getAction()
                    .equals(Intent.ACTION_VIEW))) {
                appWidgetId = Integer.parseInt(intent.getAction());
            }
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);

        if (fragment == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.accounts_list_container, AccountsList.newInstance(appWidgetId), FRAGMENT_ACCOUNTS_LIST)
                    .commit();
        }
    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent data) {
        // NO-OP
    }
}
