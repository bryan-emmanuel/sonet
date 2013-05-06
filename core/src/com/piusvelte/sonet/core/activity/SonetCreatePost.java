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
package com.piusvelte.sonet.core.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.ads.*;
import com.piusvelte.sonet.core.R;
import com.piusvelte.sonet.core.Sonet;
import com.piusvelte.sonet.core.SonetTokens;
import com.piusvelte.sonet.core.R.id;
import com.piusvelte.sonet.core.R.layout;
import com.piusvelte.sonet.core.R.menu;
import com.piusvelte.sonet.core.R.string;
import com.piusvelte.sonet.core.Sonet.Accounts;
import com.piusvelte.sonet.core.Sonet.Widgets;
import com.piusvelte.sonet.core.task.LocationTask;
import com.piusvelte.sonet.core.task.PostTask;
import com.piusvelte.sonet.core.task.chatter.ChatterPostTask;
import com.piusvelte.sonet.core.task.facebook.FacebookLocationTask;
import com.piusvelte.sonet.core.task.facebook.FacebookPostTask;
import com.piusvelte.sonet.core.task.foursquare.FoursquareLocationTask;
import com.piusvelte.sonet.core.task.foursquare.FoursquarePostTask;
import com.piusvelte.sonet.core.task.identica.IdenticaPostTask;
import com.piusvelte.sonet.core.task.linkedin.LinkedInPostTask;
import com.piusvelte.sonet.core.task.myspace.MySpacePostTask;
import com.piusvelte.sonet.core.task.twitter.TwitterLocationTask;
import com.piusvelte.sonet.core.task.twitter.TwitterPostTask;

import static com.piusvelte.sonet.core.Sonet.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SonetCreatePost extends Activity implements OnKeyListener, OnClickListener, TextWatcher {
	private static final String TAG = "SonetCreatePost";
	private HashMap<Long, String> mAccountsLocation = new HashMap<Long, String>();
	private HashMap<Long, String[]> mAccountsTags = new HashMap<Long, String[]>();
	private HashMap<Long, Integer> mAccountsService = new HashMap<Long, Integer>();
	private EditText mMessage;
	private ImageButton mSend;
	private TextView mCount;
	private String mLat = null;
	private String mLong = null;
	private static final int PHOTO = 1;
	private static final int TAGS = 2;
	private String mPhotoPath;
	private AlertDialog mDialog;
	private static final List<Integer> sLocationSupported = new ArrayList<Integer>();
	private static final List<Integer> sPhotoSupported = new ArrayList<Integer>();
	private static final List<Integer> sTaggingSupported = new ArrayList<Integer>();
	private ProgressDialog loadingDialog = new ProgressDialog(this);

	static {
		sLocationSupported.add(TWITTER);
		sLocationSupported.add(FACEBOOK);
		sLocationSupported.add(FOURSQUARE);
		sPhotoSupported.add(FACEBOOK);
		sTaggingSupported.add(FACEBOOK);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// allow posting to multiple services if an account is defined
		// allow selecting which accounts to use
		// get existing comments, allow liking|unliking those comments
		setContentView(R.layout.post);
		if (!getPackageName().toLowerCase().contains(PRO)) {
			AdView adView = new AdView(this, AdSize.BANNER, SonetTokens.GOOGLE_AD_ID);
			((LinearLayout) findViewById(R.id.ad)).addView(adView);
			adView.loadAd(new AdRequest());
		}
		mMessage = (EditText) findViewById(R.id.message);
		mSend = (ImageButton) findViewById(R.id.send);
		mCount = (TextView) findViewById(R.id.count);
		mMessage.addTextChangedListener(this);
		mMessage.setOnKeyListener(this);
		mSend.setOnClickListener(this);
		loadingDialog.setMessage(getString(R.string.loading));
		loadingDialog.setCancelable(true);
		loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		setResult(RESULT_OK);
	}

	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if (intent != null) {
			String action = intent.getAction();
			if ((action != null) && action.equals(Intent.ACTION_SEND)) {
				if (intent.hasExtra(Intent.EXTRA_STREAM))
					getPhoto((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
				if (intent.hasExtra(Intent.EXTRA_TEXT)) {
					final String text = intent.getStringExtra(Intent.EXTRA_TEXT);
					mMessage.setText(text);
					mCount.setText(Integer.toString(text.length()));
				}
				chooseAccounts();
			} else {
				Uri data = intent.getData();
				if ((data != null) && data.toString().contains(Accounts.getContentUri(this).toString())) {
					// default to the account passed in, but allow selecting additional accounts
					Cursor account = this.getContentResolver().query(Accounts.getContentUri(this), new String[]{Accounts._ID, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{data.getLastPathSegment()}, null);
					if (account.moveToFirst())
						mAccountsService.put(account.getLong(0), account.getInt(1));
					account.close();
				} else if (intent.hasExtra(Widgets.INSTANT_UPLOAD)) {
					// check if a photo path was passed and prompt user to select the account
					setPhoto(intent.getStringExtra(Widgets.INSTANT_UPLOAD));
					chooseAccounts();
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if ((mDialog != null) && mDialog.isShowing())
			mDialog.dismiss();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_post, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.menu_post_accounts)
			chooseAccounts();
		else if (itemId == R.id.menu_post_photo) {
			boolean supported = false;
			Iterator<Integer> services = mAccountsService.values().iterator();
			while (services.hasNext() && ((supported = sPhotoSupported.contains(services.next())) == false));
			if (supported) {
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(Intent.createChooser(intent, "Select Picture"), PHOTO);
			} else
				unsupportedToast(sPhotoSupported);
		} else if (itemId == R.id.menu_post_location) {
			LocationManager locationManager = (LocationManager) SonetCreatePost.this.getSystemService(Context.LOCATION_SERVICE);
			Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (location != null) {
				mLat = Double.toString(location.getLatitude());
				mLong = Double.toString(location.getLongitude());
				if (mAccountsService.size() == 1) {
					if (sLocationSupported.contains(mAccountsService.values().iterator().next()))
						setLocation(mAccountsService.keySet().iterator().next());
					else
						unsupportedToast(sLocationSupported);
				} else {
					// dialog to select an account
					Iterator<Long> accountIds = mAccountsService.keySet().iterator();
					HashMap<Long, String> accountEntries = new HashMap<Long, String>();
					while (accountIds.hasNext()) {
						Long accountId = accountIds.next();
						Cursor account = this.getContentResolver().query(Accounts.getContentUri(this), new String[]{Accounts._ID, ACCOUNTS_QUERY}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
						if (account.moveToFirst() && sLocationSupported.contains(mAccountsService.get(accountId)))
							accountEntries.put(account.getLong(account.getColumnIndex(Accounts._ID)), account.getString(account.getColumnIndex(Accounts.USERNAME)));
					}
					int size = accountEntries.size();
					if (size != 0) {
						final long[] accountIndexes = new long[size];
						final String[] accounts = new String[size];
						int i = 0;
						Iterator<Map.Entry<Long, String>> entries = accountEntries.entrySet().iterator();
						while (entries.hasNext()) {
							Map.Entry<Long, String> entry = entries.next();
							accountIndexes[i] = entry.getKey();
							accounts[i++] = entry.getValue();
						}
						mDialog = (new AlertDialog.Builder(this))
								.setTitle(R.string.accounts)
								.setSingleChoiceItems(accounts, -1, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										setLocation(accountIndexes[which]);
										dialog.dismiss();
									}
								})
								.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								})
								.create();
						mDialog.show();
					} else
						unsupportedToast(sLocationSupported);
				}
			} else
				(Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_LONG)).show();
		}
		return super.onOptionsItemSelected(item);
	}

	private void setLocation(final long accountId) {
		final LocationTask locationTask;
		int service = mAccountsService.get(accountId);
		if (service == TWITTER)
			locationTask = new TwitterLocationTask(this, accountId);
		else if (service == FACEBOOK)
			locationTask = new FacebookLocationTask(this, accountId);
		else if (service == FOURSQUARE)
			locationTask = new FoursquareLocationTask(this, accountId);
		else
			return;
		loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
			@Override
			public void onCancel(DialogInterface dialog) {
				if (!locationTask.isCancelled())
					locationTask.cancel(true);
			}
		});
		loadingDialog.show();
		locationTask.getLocations(mLat, mLong);
	}

	private void unsupportedToast(List<Integer> supportedServices) {
		StringBuilder message = new StringBuilder();
		message.append("This feature is currently supported only for ");
		for (int i = 0, l = supportedServices.size(); i < l; i++) {
			message.append(Sonet.getServiceName(getResources(), supportedServices.get(i)));
			if (i == (l - 1))
				message.append(".");
			else if (i == (l - 2))
				message.append(", and ");
			else
				message.append(", ");
		}
		Toast.makeText(getApplicationContext(), message.toString(), Toast.LENGTH_LONG).show();
	}

	@Override
	public void onClick(View v) {
		if (v == mSend) {
			if (!mAccountsService.isEmpty())
				postNextAccount();
			else
				(Toast.makeText(SonetCreatePost.this, "no accounts selected", Toast.LENGTH_LONG)).show();
		}
	}

	protected void getPhoto(Uri uri) {
		final AsyncTask<Uri, Void, String> asyncTask = new AsyncTask<Uri, Void, String>() {
			@Override
			protected String doInBackground(Uri... imgUri) {
				String[] projection = new String[]{MediaStore.Images.Media.DATA};
				String path = null;
				Cursor c = getContentResolver().query(imgUri[0], projection, null, null, null);
				if ((c != null) && c.moveToFirst()) {
					path = c.getString(c.getColumnIndex(projection[0]));
				} else {
					// some file manages send the path through the uri
					path = imgUri[0].getPath();
				}
				c.close();
				return path;
			}

			@Override
			protected void onPostExecute(String path) {
				if (loadingDialog.isShowing())
					loadingDialog.dismiss();
				if (path != null)
					setPhoto(path);
				else
					(Toast.makeText(SonetCreatePost.this, "error retrieving the photo path", Toast.LENGTH_LONG)).show();
			}
		};
		loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
			@Override
			public void onCancel(DialogInterface dialog) {
				if (!asyncTask.isCancelled())
					asyncTask.cancel(true);
			}
		});
		loadingDialog.show();
		asyncTask.execute(uri);
	}

	protected void setPhoto(String path) {
		mPhotoPath = path;
		(Toast.makeText(SonetCreatePost.this, "Currently, the photo will only be uploaded Facebook accounts.", Toast.LENGTH_LONG)).show();
	}

	protected void selectFriends(long accountId) {
		if ((mAccountsService.get(accountId) == FACEBOOK) && (!mAccountsLocation.containsKey(accountId) || (mAccountsLocation.get(accountId) == null)))
			(Toast.makeText(SonetCreatePost.this, "To tag friends, Facebook requires a location to be included.", Toast.LENGTH_LONG)).show();
		else
			startActivityForResult(Sonet.getPackageIntent(this, SelectFriends.class).putExtra(Accounts.SID, accountId).putExtra(Stags, mAccountsTags.get(accountId)), TAGS);
	}

	protected void chooseAccounts() {
		// don't limit accounts to the widget...
		Cursor c = this.getContentResolver().query(Accounts.getContentUri(this), new String[]{Accounts._ID, ACCOUNTS_QUERY, Accounts.SERVICE}, null, null, null);
		if (c.moveToFirst()) {
			int i = 0;;
			int count = c.getCount();
			final long[] accountIndexes = new long[count];
			final String[] accounts = new String[count];
			final boolean[] defaults = new boolean[count];
			final int[] accountServices = new int[count];
			while (!c.isAfterLast()) {
				long id = c.getLong(0);
				accountIndexes[i] = id;
				accounts[i] = c.getString(1);
				accountServices[i] = c.getInt(2);
				defaults[i++] = mAccountsService.containsKey(id);
				c.moveToNext();
			}
			mDialog = (new AlertDialog.Builder(this))
					.setTitle(R.string.accounts)
					.setMultiChoiceItems(accounts, defaults, new DialogInterface.OnMultiChoiceClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which, boolean isChecked) {
							if (isChecked) {
								final long accountId = accountIndexes[which];
								mAccountsService.put(accountId, accountServices[which]);
								if (sLocationSupported.contains(accountServices[which])) {
									if (mLat == null) {
										LocationManager locationManager = (LocationManager) SonetCreatePost.this.getSystemService(Context.LOCATION_SERVICE);
										Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
										if (location != null) {
											mLat = Double.toString(location.getLatitude());
											mLong = Double.toString(location.getLongitude());
										}										
									}
									if ((mLat != null) && (mLong != null)) {
										dialog.cancel();
										mDialog = (new AlertDialog.Builder(SonetCreatePost.this))
												.setTitle(R.string.set_location)
												.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialog, int which) {
														setLocation(accountId);
														dialog.dismiss();
													}
												})
												.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialog, int which) {
														dialog.dismiss();
													}
												})
												.create();
										mDialog.show();
									}
								}
							} else {
								mAccountsService.remove(accountIndexes[which]);
								mAccountsLocation.remove(accountIndexes[which]);
								mAccountsTags.remove(accountIndexes[which]);
							}
						}
					})
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
					.create();
			mDialog.show();
		}
		c.close();
	}

	public void onPostProgress(String[] params) {
		if (params.length == 1)
			loadingDialog.setMessage(String.format(getString(R.string.sending), params[0]));
		else
			(Toast.makeText(SonetCreatePost.this, params[0] + " " + params[1], Toast.LENGTH_LONG)).show();
	}

	public void postNextAccount() {
		if (!mAccountsService.isEmpty()) {
			final PostTask postTask;
			long accountId = mAccountsService.keySet().iterator().next();
			int service = mAccountsService.remove(accountId);
			if (service == TWITTER)
				postTask = new TwitterPostTask(this, accountId);
			else if (service == FACEBOOK)
				postTask = new FacebookPostTask(this, accountId);
			else if (service == MYSPACE)
				postTask = new MySpacePostTask(this, accountId);
			else if (service == FOURSQUARE)
				postTask = new FoursquarePostTask(this, accountId);
			else if (service == LINKEDIN)
				postTask = new LinkedInPostTask(this, accountId);
			else if (service == IDENTICA)
				postTask = new IdenticaPostTask(this, accountId);
			else if (service == CHATTER)
				postTask = new ChatterPostTask(this, accountId);
			else {
				postNextAccount();
				return;
			}
			loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
				@Override
				public void onCancel(DialogInterface dialog) {
					if (!postTask.isCancelled())
						postTask.cancel(true);
				}
			});
			if (!loadingDialog.isShowing())
				loadingDialog.show();
			postTask.post(mMessage.getText().toString(), mPhotoPath, mLat, mLong, mAccountsLocation.get(accountId), mAccountsTags.get(accountId));
		} else {
			if (loadingDialog.isShowing())
				loadingDialog.cancel();
			finish();
		}
	}

	public void onLocationsFound(final long accountId, final String[] ids, String[] names) {
		if (loadingDialog.isShowing())
			loadingDialog.cancel();
		if (ids.length > 0) {
			mDialog = (new AlertDialog.Builder(SonetCreatePost.this))
					.setSingleChoiceItems(names, -1, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mAccountsLocation.put(accountId, ids[which]);
							dialog.dismiss();
						}
					})
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					})
					.create();
			mDialog.show();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PHOTO:
			if (resultCode == RESULT_OK)
				getPhoto(data.getData());
			break;
		case TAGS:
			if ((resultCode == RESULT_OK) && data.hasExtra(Stags) && data.hasExtra(Accounts.SID))
				mAccountsTags.put(data.getLongExtra(Accounts.SID, Sonet.INVALID_ACCOUNT_ID), data.getStringArrayExtra(Stags));
			break;
		}
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		mCount.setText(Integer.toString(mMessage.getText().toString().length()));
		return false;
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		mCount.setText(Integer.toString(arg0.toString().length()));
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

}