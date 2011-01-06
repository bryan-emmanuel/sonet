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
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_ACCOUNTS;
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

import static com.piusvelte.sonet.Tokens.TWITTER_KEY;
import static com.piusvelte.sonet.Tokens.TWITTER_SECRET;
import static com.piusvelte.sonet.Tokens.FACEBOOK_ID;
import static com.piusvelte.sonet.Tokens.MYSPACE_KEY;
import static com.piusvelte.sonet.Tokens.MYSPACE_SECRET;

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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources.NotFoundException;
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
		public void listAccounts() throws RemoteException {
			SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
			Cursor cursor = db.rawQuery("select " + _ID + "," + USERNAME + "," + SERVICE + " from " + TABLE_ACCOUNTS + " where " + WIDGET + "=" + mAppWidgetId, null);
			setListAdapter(new SimpleCursorAdapter(ManageAccounts.this, R.layout.accounts_row, cursor, new String[] {USERNAME}, new int[] {R.id.account_username}));
			cursor.close();
			db.close();
		}

		@Override
		public void getAuth(int service) throws RemoteException {
			if (service != -1) ManageAccounts.this.getAuth(service);
		}

		@Override
		public void getTimezone(int account) throws RemoteException {
			// index set to GMT
			final int id = account;
			(new AlertDialog.Builder(ManageAccounts.this))
			.setTitle(R.string.timezone)
			.setSingleChoiceItems(R.array.timezone_entries, 12, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mUpdateWidget = true;
					if (mSonetService != null) {
						try {
							mSonetService.addTimezone(id, Integer.parseInt(getResources().getStringArray(R.array.timezone_values)[which]));
						} catch (NumberFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					dialog.cancel();
				}
			})
			.show();
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
		if (mSonetService != null) {
			try {
				mSonetService.getAuth((int) id);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.delete_account);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if ((item.getItemId() == DELETE_ID) && (mSonetService != null)) {
			try {
				mSonetService.deleteAccount((int) ((AdapterContextMenuInfo) item.getMenuInfo()).id);
				mUpdateWidget = true;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
					mUpdateWidget = true;
					if (mSonetService != null) mSonetService.addAccount((new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(consumer.getToken(), consumer.getTokenSecret()))).getScreenName(),
							consumer.getToken(),
							consumer.getTokenSecret(),
							0,
							TWITTER,
							0,
							mAppWidgetId);
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
					final int timezone = Integer.parseInt(json.getString(TIMEZONE));
					ManageAccounts.this.runOnUiThread(new Runnable() {
						public void run() {
							mUpdateWidget = true;
							if (mSonetService != null) {
								try {
									mSonetService.addAccount(username,
											mFacebook.getAccessToken(),
											"0",
											(int) mFacebook.getAccessExpires(),
											FACEBOOK,
											timezone,
											mAppWidgetId);
								} catch (NumberFormatException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (RemoteException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
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
						if (mSonetService != null) {
							try {
								mSonetService.addAccount(username,
										mMSSession.getToken(),
										mMSSession.getTokenSecret(),
										0,
										MYSPACE,
										0,
										mAppWidgetId);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
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
				mSonetService.listAccounts();
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
