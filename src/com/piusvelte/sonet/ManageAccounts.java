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
import static com.piusvelte.sonet.Sonet.TWITTER_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_REQUEST;
import static com.piusvelte.sonet.Sonet.FACEBOOK_PERMISSIONS;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;

import static com.piusvelte.sonet.Tokens.TWITTER_KEY;
import static com.piusvelte.sonet.Tokens.TWITTER_SECRET;
import static com.piusvelte.sonet.Tokens.FACEBOOK_ID;
import static com.piusvelte.sonet.Tokens.MYSPACE_KEY;
import static com.piusvelte.sonet.Tokens.MYSPACE_SECRET;

import com.piusvelte.sonet.Sonet.Accounts;

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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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

public class ManageAccounts extends ListActivity implements OnClickListener, DialogInterface.OnClickListener, DialogListener, IMSSessionCallback {
	private static final String TAG = "ManageAccounts";
	private static final int REAUTH_ID = Menu.FIRST;
	private static final int SETTINGS_ID = Menu.FIRST + 1;
	private static final int DELETE_ID = Menu.FIRST + 2;
	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private String request_token,
	request_secret;
	private MSSession mMSSession;
	private boolean mUpdateWidget = false;

	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");
	private static String MYSPACE_CALLBACK = "sonet://myspace";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		if ((i != null) && i.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) mAppWidgetId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setContentView(R.layout.accounts);
		registerForContextMenu(getListView());
		((Button) findViewById(R.id.button_add_account)).setOnClickListener(this);
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		final long item = id;
		final CharSequence[] items = {getString(R.string.re_authenticate), getString(R.string.account_settings)};
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				which++; //fix indexing
				switch (which) {
				case REAUTH_ID:
					Cursor c = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SERVICE}, Accounts._ID + "=" + item, null, null);
					if (c.moveToFirst())
						getAuth(c.getInt(c.getColumnIndex(Accounts.SERVICE)));
					c.close();
					break;
				case SETTINGS_ID:
					startActivity(new Intent(ManageAccounts.this, AccountSettings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, item));
					break;
				}
				dialog.cancel();
			}
		}).show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.delete_account);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == DELETE_ID) getContentResolver().delete(Accounts.CONTENT_URI, Accounts._ID + "=" + ((AdapterContextMenuInfo) item.getMenuInfo()).id, null);
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
		listAccounts();	
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mUpdateWidget) startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
	}

	private void listAccounts() {
		Cursor c = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.USERNAME, Accounts.SERVICE}, Accounts.WIDGET + "=?", new String[]{Integer.toString(mAppWidgetId)}, null);
		setListAdapter(new SimpleCursorAdapter(ManageAccounts.this, R.layout.accounts_row, c, new String[] {Accounts.USERNAME}, new int[] {R.id.account_username}));
		c.close();
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
					mUpdateWidget = true;
					ContentValues values = new ContentValues();
					values.put(Accounts.USERNAME, (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(consumer.getToken(), consumer.getTokenSecret()))).getScreenName());
					values.put(Accounts.TOKEN, consumer.getToken());
					values.put(Accounts.SECRET, consumer.getTokenSecret());
					values.put(Accounts.EXPIRY, 0);
					values.put(Accounts.SERVICE, TWITTER);
					values.put(Accounts.TIMEZONE, 0);
					values.put(Accounts.WIDGET, mAppWidgetId);
					getContentResolver().insert(Accounts.CONTENT_URI, values);
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
					final String username = json.getString("name");
					final int timezone = Integer.parseInt(json.getString(Accounts.TIMEZONE));
					ManageAccounts.this.runOnUiThread(new Runnable() {
						public void run() {
							mUpdateWidget = true;
							ContentValues values = new ContentValues();
							values.put(Accounts.USERNAME, username);
							values.put(Accounts.TOKEN, mFacebook.getAccessToken());
							values.put(Accounts.SECRET, "");
							values.put(Accounts.EXPIRY, (int) mFacebook.getAccessExpires());
							values.put(Accounts.SERVICE, FACEBOOK);
							values.put(Accounts.TIMEZONE, timezone);
							values.put(Accounts.WIDGET, mAppWidgetId);
							getContentResolver().insert(Accounts.CONTENT_URI, values);
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
				final String username = userObject.get("userName").toString();
				ManageAccounts.this.runOnUiThread(new Runnable() {
					public void run() {
						ContentValues values = new ContentValues();
						values.put(Accounts.USERNAME, username);
						values.put(Accounts.TOKEN, mMSSession.getToken());
						values.put(Accounts.SECRET, mMSSession.getTokenSecret());
						values.put(Accounts.EXPIRY, 0);
						values.put(Accounts.SERVICE, MYSPACE);
						values.put(Accounts.TIMEZONE, 0);
						values.put(Accounts.WIDGET, mAppWidgetId);
						Uri uri = getContentResolver().insert(Accounts.CONTENT_URI, values);
						// get the timezone, index set to GMT
						final Uri timezoneUri = uri;
						(new AlertDialog.Builder(ManageAccounts.this))
						.setTitle(R.string.timezone)
						.setSingleChoiceItems(R.array.timezone_entries, 12, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mUpdateWidget = true;
								ContentValues values = new ContentValues();
								values.put(Accounts.TIMEZONE, Integer.parseInt(getResources().getStringArray(R.array.timezone_values)[which]));
								getContentResolver().update(timezoneUri, values, null, null);
								dialog.cancel();
							}
						})
						.show();
					}
				});
			}                       
		}
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
