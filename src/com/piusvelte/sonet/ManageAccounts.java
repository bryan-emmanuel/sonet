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

import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import static com.piusvelte.sonet.SonetDatabaseHelper.USERNAME;
import static com.piusvelte.sonet.SonetDatabaseHelper.SECRET;
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TOKEN;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetDatabaseHelper.EXPIRY;
import static com.piusvelte.sonet.SonetDatabaseHelper.TIMEZONE;
import static com.piusvelte.sonet.SonetDatabaseHelper.WIDGET;
import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_REQUEST;
import static com.piusvelte.sonet.Sonet.FACEBOOK_PERMISSIONS;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Services.TWITTER_KEY;
import static com.piusvelte.sonet.Services.TWITTER_SECRET;
import static com.piusvelte.sonet.Services.FACEBOOK_ID;
import static com.piusvelte.sonet.Services.MYSPACE_KEY;
import static com.piusvelte.sonet.Services.MYSPACE_SECRET;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook.DialogListener;
import com.myspace.sdk.MSLoginActivity;
import com.myspace.sdk.MSRequest;
import com.myspace.sdk.MSSDK;
import com.myspace.sdk.MSSession;
import com.myspace.sdk.MSSession.IMSSessionCallback;

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.signature.SignatureMethod;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

//import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
//import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageAccounts extends ListActivity implements OnClickListener, DialogInterface.OnClickListener, DialogListener, IMSSessionCallback, ServiceConnection {
	private static final String TAG = "ManageAccounts";
	private static final int DELETE_ID = Menu.FIRST;
	private SonetDatabaseHelper mSonetDatabaseHelper;
	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private String request_token,
	request_secret;
	private MSSession mMSSession;
	private boolean mUpdateWidget = false;

	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");
	private static String MYSPACE_CALLBACK = "sonet://myspace";
	

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
		}

		@Override
		public void listAccounts(boolean updateWidget) throws RemoteException {
			mUpdateWidget = updateWidget;
			ManageAccounts.this.listAccounts();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		if ((i != null) && i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setContentView(R.layout.accounts);
		registerForContextMenu(getListView());
		((Button) findViewById(R.id.button_add_account)).setOnClickListener(this);
		mSonetDatabaseHelper = new SonetDatabaseHelper(this);
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
		Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{SERVICE}, _ID + "=" + id, null, null, null, null);
		int service = -1;
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			service = cursor.getInt(cursor.getColumnIndex(SERVICE));
		}
		if (service != -1) getAuth(service);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.delete_account);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if ((item.getItemId() == DELETE_ID) && (mSonetService != null))
			try {
				mSonetService.deleteAccount((int) ((AdapterContextMenuInfo) item.getMenuInfo()).id);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return super.onContextItemSelected(item);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_add_account:
			// add a new account
			String[] services = getResources().getStringArray(R.array.service_entries);
			(new AlertDialog.Builder(this))
			.setItems(services, this)
			.show();
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		bindService(new Intent(this, SonetService.class), this, BIND_AUTO_CREATE);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindService(this);
		if (mUpdateWidget) startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();
		if (uri != null) {
			if (TWITTER_CALLBACK.getScheme().equals(uri.getScheme())) {
				try {
					// use the requestToken and secret from earlier
					SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
					if ((request_token == null) || (request_secret == null)) {
						request_token = sp.getString(getString(R.string.key_requesttoken), "");
						request_secret = sp.getString(getString(R.string.key_requestsecret), "");
					}
					// clear the saved token/secret
					Editor spe = sp.edit();
					spe.putString(getString(R.string.key_requesttoken), "");
					spe.putString(getString(R.string.key_requestsecret), "");
					spe.commit();
					// this will populate token and token_secret in consumer
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
					//					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET, SignatureMethod.HMAC_SHA1);
					consumer.setTokenWithSecret(request_token, request_secret);
					//					OAuthProvider provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
					OAuthProvider provider = new DefaultOAuthProvider(consumer, TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
					provider.setOAuth10a(true);
					//					provider.retrieveAccessToken(consumer, verifier);
					provider.retrieveAccessToken(verifier);
					ContentValues values = new ContentValues();
					values.put(USERNAME, (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(consumer.getToken(), consumer.getTokenSecret()))).getScreenName());
					values.put(TOKEN, consumer.getToken());
					values.put(SECRET, consumer.getTokenSecret());
					values.put(EXPIRY, 0);
					values.put(SERVICE, TWITTER);
					values.put(TIMEZONE, 0);
					values.put(WIDGET, mAppWidgetId);
					SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
					db.insert(TABLE_ACCOUNTS, USERNAME, values);
					db.close();
					mUpdateWidget = true;
					listAccounts();
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void getAuth(int service) {
		switch (service) {
		case TWITTER:
			try {
				// switching to older signpost for myspace
				//				CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
				CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET, SignatureMethod.HMAC_SHA1);
				//				OAuthProvider provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
				OAuthProvider provider = new DefaultOAuthProvider(consumer, TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
				provider.setOAuth10a(true);
				//				String authUrl = provider.retrieveRequestToken(consumer, TWITTER_CALLBACK.toString());
				String authUrl = provider.retrieveRequestToken(TWITTER_CALLBACK.toString());
				/*
				 * need to save the requestToken and secret
				 */
				request_token = consumer.getToken();
				request_secret = consumer.getTokenSecret();
				SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
				Editor spe = sp.edit();
				spe.putString(getString(R.string.key_requesttoken), request_token);
				spe.putString(getString(R.string.key_requestsecret), request_secret);
				spe.commit();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY));
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			break;
		case FACEBOOK:
			mFacebook = new Facebook();
			mAsyncRunner = new AsyncFacebookRunner(mFacebook);
			mFacebook.setAccessToken(null);
			mFacebook.setAccessExpires(0);
			mFacebook.authorize(this, FACEBOOK_ID, FACEBOOK_PERMISSIONS, this);
			break;
		case MYSPACE:
			mMSSession = MSSession.getSession(MYSPACE_KEY, MYSPACE_SECRET, MYSPACE_CALLBACK, this);
			startActivity(new Intent(this, MSLoginActivity.class));
			break;
		}
	}

	private void listAccounts() {
		SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
		Cursor cursor = db.rawQuery("select " + _ID + "," + USERNAME + "," + SERVICE + " from " + TABLE_ACCOUNTS + " where " + WIDGET + "=" + mAppWidgetId, null);
		startManagingCursor(cursor);
		setListAdapter(new SimpleCursorAdapter(this, R.layout.accounts_row, cursor, new String[] {USERNAME}, new int[] {R.id.account_username}));
		db.close();
	}

	public void onClick(DialogInterface dialog, int which) {
		getAuth(which);
		dialog.cancel();
	}

	// facebook
	public void onComplete(Bundle values) {
		mAsyncRunner.request("me", new RequestListener() {
			@Override
			public void onComplete(String response) {
				try {
					JSONObject json = Util.parseJson(response);
					ContentValues values = new ContentValues();
					values.put(USERNAME, json.getString("name"));
					values.put(TOKEN, mFacebook.getAccessToken());
					values.put(SECRET, 0);
					values.put(EXPIRY, mFacebook.getAccessExpires());
					values.put(SERVICE, FACEBOOK);
					values.put(TIMEZONE, json.getString(TIMEZONE));
					values.put(WIDGET, mAppWidgetId);
					SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
					db.insert(TABLE_ACCOUNTS, _ID, values);
					db.close();
					ManageAccounts.this.runOnUiThread(new Runnable() {
						public void run() {
							mUpdateWidget = true;
							listAccounts();
						}
					});
				} catch (JSONException e) {
					Log.e(TAG, e.toString());
				} catch (FacebookError e) {
					Log.e(TAG, e.toString());
				}
			}

			@Override
			public void onFacebookError(FacebookError e) {
				Log.e(TAG, e.toString());
			}

			@Override
			public void onFileNotFoundException(FileNotFoundException e) {
				Log.e(TAG, e.toString());
			}

			@Override
			public void onIOException(IOException e) {
				Log.e(TAG, e.toString());
			}

			@Override
			public void onMalformedURLException(MalformedURLException e) {
				Log.e(TAG, e.toString());
			}
		});
	}

	public void onFacebookError(FacebookError error) {
		Toast.makeText(ManageAccounts.this, error.getMessage(), Toast.LENGTH_LONG).show();
	}

	public void onError(DialogError error) {
		Toast.makeText(ManageAccounts.this, error.getMessage(), Toast.LENGTH_LONG).show();
	}

	public void onCancel() {
		Toast.makeText(ManageAccounts.this, "Authorization canceled", Toast.LENGTH_LONG).show();
	}


	@Override
	public void sessionDidLogin(MSSession session) {
		mMSSession.setToken(session.getToken());
		mMSSession.setTokenSecret(session.getTokenSecret());
		MSSDK.getUserInfo(new MSRequestCallback());
	}

	@Override
	public void sessionDidLogout(MSSession session) {
	}

	private void setTimezone(final int id) {
		// index set to GMT
		(new AlertDialog.Builder(this))
		.setTitle(R.string.timezone)
		.setSingleChoiceItems(R.array.timezone_entries, 12, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContentValues values = new ContentValues();
				values.put(TIMEZONE, Integer.parseInt(getResources().getStringArray(R.array.timezone_values)[which]));
				SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
				db.update(TABLE_ACCOUNTS, values, _ID + "=" + id, null);
				db.close();
				mUpdateWidget = true;
				listAccounts();
				dialog.cancel();
			}
		})
		.show();		
	}

	// MySpace
	private class MSRequestCallback extends MSRequest.MSRequestCallback {

		@Override
		public void requestDidFail(MSRequest request, Throwable error) {
			Log.e(TAG, error.getMessage());
		}

		@Override
		public void requestDidLoad(MSRequest request, Object result) {
			Map<?, ?> data = (Map<?, ?>) result;
			result = data.get("data");
			if (result instanceof Map<?, ?>) {
				Map<?, ?> userObject = (Map<?, ?>) result;
				ContentValues values = new ContentValues();
				values.put(USERNAME, (String) userObject.get("userName"));
				values.put(TOKEN, mMSSession.getToken());
				values.put(SECRET, mMSSession.getTokenSecret());
				values.put(SERVICE, MYSPACE);
				values.put(TIMEZONE, 0);
				values.put(WIDGET, mAppWidgetId);
				SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
				final int id = (int) db.insert(TABLE_ACCOUNTS, _ID, values);
				db.close();
				ManageAccounts.this.runOnUiThread(new Runnable() {
					public void run() {
						setTimezone(id);
					}
				});
			}                       
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

//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event)  {
//		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
//				&& keyCode == KeyEvent.KEYCODE_BACK
//				&& event.getRepeatCount() == 0) onBackPressed();
//		return super.onKeyDown(keyCode, event);
//	}
//
//	@Override
//	public void onBackPressed() {
//		// make sure user is sent back to UI.java instead of reopening the browser for twitter
//		startActivity(new Intent(this, UI.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
//		return;
//	}
}
