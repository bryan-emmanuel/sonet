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

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.HASBUTTONS;
import static com.piusvelte.sonet.SonetDatabaseHelper.INTERVAL;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_BG_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIME24HR;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_COLOR;
import static com.piusvelte.sonet.SonetDatabaseHelper.BUTTONS_TEXTSIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.MESSAGES_TEXTSIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.FRIEND_TEXTSIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.CREATED_TEXTSIZE;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class Settings extends Activity implements View.OnClickListener, ServiceConnection {
	private int mInterval_value,
	mButtons_bg_color_value,
	mButtons_color_value,
	mButtons_textsize_value,
	mMessages_bg_color_value,
	mMessages_color_value,
	mMessages_textsize_value,
	mFriend_color_value,
	mFriend_textsize_value,
	mCreated_color_value,
	mCreated_textsize_value;
	private Button mInterval;
	private CheckBox mHasButtons;
	private Button mButtons_bg_color;
	private Button mButtons_color;
	private Button mButtons_textsize;
	private Button mMessages_bg_color;
	private Button mMessages_color;
	private Button mMessages_textsize;
	private Button mFriend_color;
	private Button mFriend_textsize;
	private Button mCreated_color;
	private Button mCreated_textsize;
	private CheckBox mTime24hr;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private boolean mUpdateWidget = false;
	private ISonetService mSonetService;
	private ISonetUI.Stub mSonetUI = new ISonetUI.Stub() {
		@Override
		public void setDefaultSettings(int interval_value,
				int buttons_bg_color_value, int buttons_color_value,
				int buttons_textsize_value, int messages_bg_color_value,
				int messages_color_value, int messages_textsize_value,
				int friend_color_value, int friend_textsize_value,
				int created_color_value, int created_textsize_value,
				boolean hasButtons, boolean time24hr) throws RemoteException {
			mInterval_value = interval_value;
			mButtons_bg_color_value = buttons_bg_color_value;
			mButtons_color_value = buttons_color_value;
			mButtons_textsize_value = buttons_textsize_value;
			mMessages_bg_color_value = messages_bg_color_value;
			mMessages_color_value = messages_color_value;
			mMessages_textsize_value = messages_textsize_value;
			mFriend_color_value = friend_color_value;
			mFriend_textsize_value = friend_textsize_value;
			mCreated_color_value = created_color_value;
			mCreated_textsize_value = created_textsize_value;
			mInterval.setOnClickListener(Settings.this);
			mButtons_bg_color.setOnClickListener(Settings.this);
			mButtons_color.setOnClickListener(Settings.this);
			mButtons_textsize.setOnClickListener(Settings.this);
			mMessages_bg_color.setOnClickListener(Settings.this);
			mMessages_color.setOnClickListener(Settings.this);
			mMessages_textsize.setOnClickListener(Settings.this);
			mFriend_color.setOnClickListener(Settings.this);
			mFriend_textsize.setOnClickListener(Settings.this);
			mCreated_color.setOnClickListener(Settings.this);
			mCreated_textsize.setOnClickListener(Settings.this);
			mHasButtons.setChecked(hasButtons);
			mHasButtons.setOnCheckedChangeListener(mHasButtonsListener);
			mTime24hr.setChecked(time24hr);
			mTime24hr.setOnCheckedChangeListener(mTime24hrListener);
		}

		@Override
		public void listAccounts() throws RemoteException {
		}

		@Override
		public void getAuth(int service) throws RemoteException {
		}

		@Override
		public void getTimezone(int account) throws RemoteException {
		}

		@Override
		public void buildScrollableWidget(int messages_color, int friend_color,
				int created_color, int friend_textsize, int created_textsize,
				int messages_textsize) throws RemoteException {
		}

		@Override
		public void widgetOnClick(boolean hasbuttons, int service, String link)
				throws RemoteException {
			// TODO Auto-generated method stub
			
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferences);
		Intent i = getIntent();
		if (i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		mInterval = (Button) findViewById(R.id.interval);
		mHasButtons = (CheckBox) findViewById(R.id.hasbuttons);
		mButtons_bg_color = (Button) findViewById(R.id.buttons_bg_color);
		mButtons_color = (Button) findViewById(R.id.buttons_color);
		mButtons_textsize = (Button) findViewById(R.id.buttons_textsize);
		mMessages_bg_color = (Button) findViewById(R.id.messages_bg_color);
		mMessages_color = (Button) findViewById(R.id.messages_color);
		mMessages_textsize = (Button) findViewById(R.id.messages_textsize);
		mFriend_color = (Button) findViewById(R.id.friend_color);
		mFriend_textsize = (Button) findViewById(R.id.friend_textsize);
		mCreated_color = (Button) findViewById(R.id.created_color);
		mCreated_textsize = (Button) findViewById(R.id.created_textsize);
		mTime24hr = (CheckBox) findViewById(R.id.time24hr);
		mInterval_value = Integer.parseInt(getString(R.string.default_interval));
		mButtons_bg_color_value = Integer.parseInt(getString(R.string.buttons_bg_color));
		mButtons_color_value = Integer.parseInt(getString(R.string.buttons_color));
		mButtons_textsize_value = Integer.parseInt(getString(R.string.buttons_textsize));
		mMessages_bg_color_value = Integer.parseInt(getString(R.string.message_bg_color));
		mMessages_color_value = Integer.parseInt(getString(R.string.message_color));
		mMessages_textsize_value = Integer.parseInt(getString(R.string.messages_textsize));
		mFriend_color_value = Integer.parseInt(getString(R.string.friend_color));
		mFriend_textsize_value = Integer.parseInt(getString(R.string.friend_textsize));
		mCreated_color_value = Integer.parseInt(getString(R.string.created_color));
		mCreated_textsize_value = Integer.parseInt(getString(R.string.created_textsize));
		bindService(new Intent(this, SonetService.class), this, BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mUpdateWidget) startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
	}
	
	@Override
	protected void onDestroy() {
		unbindService(this);
		super.onDestroy();
	}

	private void updateDatabase(String column, int value) {
		if (mSonetService != null)
			try {
				mSonetService.setIntSetting(mAppWidgetId, column, value);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		mUpdateWidget = true;
	}
	
	ColorPickerDialog.OnColorChangedListener mHeadBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			updateDatabase(BUTTONS_BG_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mHeadTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			updateDatabase(BUTTONS_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mBodyBackgroundColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			updateDatabase(MESSAGES_BG_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mBodyTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			updateDatabase(MESSAGES_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mFriendTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			updateDatabase(FRIEND_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	ColorPickerDialog.OnColorChangedListener mCreatedTextColorListener =
		new ColorPickerDialog.OnColorChangedListener() {

		public void colorChanged(int color) {
			updateDatabase(CREATED_COLOR, color);
		}

		public void colorUpdate(int color) {}
	};

	CompoundButton.OnCheckedChangeListener mHasButtonsListener =
		new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateDatabase(HASBUTTONS, isChecked ? 1 : 0);
		}
	};

	CompoundButton.OnCheckedChangeListener mTime24hrListener =
		new CompoundButton.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateDatabase(TIME24HR, isChecked ? 1 : 0);
		}
	};

	@Override
	public void onClick(View v) {
		if (v == mInterval) {
			int index = 0,
			value = this.mInterval_value;
			String[] values = getResources().getStringArray(R.array.interval_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.interval_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					updateDatabase(INTERVAL, Integer.parseInt(getResources().getStringArray(R.array.interval_values)[which]));
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();
		} else if (v == mButtons_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadBackgroundColorListener, this.mButtons_bg_color_value);
			cp.show();
		} else if (v == mButtons_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mHeadTextColorListener, this.mButtons_color_value);
			cp.show();
		} else if (v == mButtons_textsize) {
			int index = 0;
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == this.mButtons_textsize_value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					updateDatabase(BUTTONS_TEXTSIZE, Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]));
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();
		} else if (v == mMessages_bg_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyBackgroundColorListener, this.mMessages_bg_color_value);
			cp.show();
		} else if (v == mMessages_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mBodyTextColorListener, this.mMessages_color_value);
			cp.show();
		} else if (v == mMessages_textsize) {
			int index = 0;
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == this.mMessages_textsize_value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					updateDatabase(MESSAGES_TEXTSIZE, Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]));
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();			
		} else if (v == mFriend_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mFriendTextColorListener, this.mFriend_color_value);
			cp.show();
		} else if (v == mFriend_textsize) {
			int index = 0;
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == this.mFriend_textsize_value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					updateDatabase(FRIEND_TEXTSIZE, Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]));
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();			
		} else if (v == mCreated_color) {
			ColorPickerDialog cp = new ColorPickerDialog(this, mCreatedTextColorListener, this.mCreated_color_value);
			cp.show();
		} else if (v == mCreated_textsize) {
			int index = 0;
			String[] values = getResources().getStringArray(R.array.textsize_values);
			for (int i = 0; i < values.length; i++) {
				if (Integer.parseInt(values[i]) == this.mCreated_textsize_value) {
					index = i;
					break;
				}
			}
			(new AlertDialog.Builder(this))
			.setSingleChoiceItems(R.array.textsize_entries, index, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					updateDatabase(CREATED_TEXTSIZE, Integer.parseInt(getResources().getStringArray(R.array.textsize_values)[which]));
					dialog.cancel();
				}
			})
			.setCancelable(true)
			.show();			
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mSonetService = ISonetService.Stub.asInterface((IBinder) service);
		if (mSonetUI != null) {
			try {
				mSonetService.setCallback(mSonetUI.asBinder());
			} catch (RemoteException e) {}
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mSonetService = null;
	}
}
