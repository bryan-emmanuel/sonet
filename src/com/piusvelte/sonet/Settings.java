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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class Settings extends PreferenceActivity {
	private SharedPreferences mSharedPreferences;
	private Preference mHeadBackground;
	private Preference mHeadText;
	private Preference mBodyBackground;
	private Preference mBodyText;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(getString(R.string.key_preferences));
		addPreferencesFromResource(R.xml.preferences);
		PreferenceScreen prefSet = getPreferenceScreen();
		mHeadBackground = prefSet.findPreference(getString(R.string.key_head_background));
		mHeadText = prefSet.findPreference(getString(R.string.key_head_text));
		mBodyBackground = prefSet.findPreference(getString(R.string.key_body_background));
		mBodyText = prefSet.findPreference(getString(R.string.key_body_text));
		mSharedPreferences = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
	}

	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == mHeadBackground) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadBackgroundColorListener, readHeadBackgroundColor());
			cp.show();
		} else if (preference == mHeadText) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadTextColorListener, readHeadTextColor());
			cp.show();
		} else if (preference == mBodyBackground) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyBackgroundColorListener, readBodyBackgroundColor());
			cp.show();
		} else if (preference == mBodyText) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyTextColorListener, readBodyTextColor());
			cp.show();
		}
		return true;
	}

	private int readHeadBackgroundColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_head_background), getString(R.string.default_head_background)));
	}
	ColorPickerDialog.OnColorChangedListener mHeadBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {
		@Override
		public void colorChanged(int color) {
			Editor spe = mSharedPreferences.edit();
			spe.putString(getResources().getString(R.string.key_head_background), Integer.toString(color));
			spe.commit();
		}

		@Override
		public void colorUpdate(int color) {
		}
	};

	private int readHeadTextColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_head_text), getString(R.string.default_head_text)));
	}
	ColorPickerDialog.OnColorChangedListener mHeadTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {
		@Override
		public void colorChanged(int color) {
			Editor spe = mSharedPreferences.edit();
			spe.putString(getResources().getString(R.string.key_head_text), Integer.toString(color));
			spe.commit();
		}

		@Override
		public void colorUpdate(int color) {
		}
	};

	private int readBodyBackgroundColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_body_background), getString(R.string.default_body_background)));
	}
	ColorPickerDialog.OnColorChangedListener mBodyBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {
		@Override
		public void colorChanged(int color) {
			Editor spe = mSharedPreferences.edit();
			spe.putString(getResources().getString(R.string.key_body_background), Integer.toString(color));
			spe.commit();
		}

		@Override
		public void colorUpdate(int color) {
		}
	};

	private int readBodyTextColor() {
		return Integer.parseInt(mSharedPreferences.getString(getString(R.string.key_body_text), getString(R.string.default_body_text)));
	}
	ColorPickerDialog.OnColorChangedListener mBodyTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {
		@Override
		public void colorChanged(int color) {
			Editor spe = mSharedPreferences.edit();
			spe.putString(getResources().getString(R.string.key_body_text), Integer.toString(color));
			spe.commit();
		}

		@Override
		public void colorUpdate(int color) {
		}
	};

}
