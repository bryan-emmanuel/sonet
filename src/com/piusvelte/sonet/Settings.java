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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.CompoundButton;
import android.widget.RemoteViews;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(getString(R.string.key_preferences));
		addPreferencesFromResource(R.xml.preferences);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		(getSharedPreferences(getString(R.string.key_preferences), MODE_PRIVATE)).registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		(getSharedPreferences(getString(R.string.key_preferences), MODE_PRIVATE)).unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(getResources().getString(R.string.key_body_background))) {
//			(new RemoteViews(this.getPackageName(), R.layout.widget)).setTextColor(android.R.id.list, Color.parseColor(sharedPreferences.getString(key, getResources().getString(R.string.default_body_background))));
		} else if (key.equals(getResources().getString(R.string.key_body_text))) (new RemoteViews(this.getPackageName(), R.layout.widget)).setTextColor(android.R.id.list, Color.parseColor(sharedPreferences.getString(key, getResources().getString(R.string.default_body_text))));
		else if (key.equals(getResources().getString(R.string.key_head_background))) {
//			(new RemoteViews(this.getPackageName(), R.layout.widget)).setTextColor(R.id.head, Color.parseColor(sharedPreferences.getString(key, getResources().getString(R.string.default_head_background))));
		} else if (key.equals(getResources().getString(R.string.key_head_text))) (new RemoteViews(this.getPackageName(), R.layout.widget)).setTextColor(R.id.head, Color.parseColor(sharedPreferences.getString(key, getResources().getString(R.string.default_head_text))));
	}

}
