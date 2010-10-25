package com.piusvelte.sonet;

import static com.piusvelte.sonet.Sonet.TAG;
import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import static com.piusvelte.sonet.SonetDatabaseHelper.USERNAME;
import static com.piusvelte.sonet.SonetDatabaseHelper.SECRET;
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TOKEN;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetDatabaseHelper.EXPIRY;
import static com.piusvelte.sonet.Sonet.TWITTER_KEY;
import static com.piusvelte.sonet.Sonet.TWITTER_SECRET;
import static com.piusvelte.sonet.Sonet.FACEBOOK_KEY;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_ACCESS;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_AUTHORIZE;
import static com.piusvelte.sonet.Sonet.TWITTER_URL_REQUEST;
import static com.piusvelte.sonet.Sonet.FACEBOOK_PERMISSIONS;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook.DialogListener;

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageAccounts extends ListActivity implements OnClickListener, android.content.DialogInterface.OnClickListener {
	private static final int DELETE_ID = Menu.FIRST;
	private SonetDatabaseHelper mSonetDatabaseHelper;
	private static final long NO_ACCOUNT = -1;
	private static final int TWITTER = 0;
	private static final int FACEBOOK = 1;
    private Facebook mFacebook;
    private AsyncFacebookRunner mAsyncRunner;

	private static Uri TWITTER_CALLBACK = Uri.parse("sonet://twitter");

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			getAuth(cursor.getInt(cursor.getColumnIndex(SERVICE)), id);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.delete_account);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == DELETE_ID) {
			SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
			db.delete(TABLE_ACCOUNTS, _ID + "=" + ((AdapterContextMenuInfo) item.getMenuInfo()).position, null);
		}
		return super.onContextItemSelected(item);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_add_account:
			// add a new account
			String[] services = getResources().getStringArray(R.array.service_entries);
			CharSequence[] items = new CharSequence[services.length];
			for (int i = 0; i < services.length; i++) items[i] = services[i];
			(new AlertDialog.Builder(this))
			.setItems(items, this)
			.show();
			break;
		case R.id.username:
			((EditText) v).setText("");
			break;
		case R.id.password:
			((EditText) v).setTransformationMethod(new PasswordTransformationMethod());
			((EditText) v).setText("");
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		listAccounts();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Uri uri = intent.getData();
		if (uri != null) {
			if (TWITTER_CALLBACK.getScheme().equals(uri.getScheme())) {
				try {
					// this will populate token and token_secret in consumer
					String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
					SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
					CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
					// use the requestToken and secret from earlier
					consumer.setTokenWithSecret(sp.getString(getString(R.string.key_requesttoken), ""), sp.getString(getString(R.string.key_requestsecret), ""));
					// throw away cached requesttoken & secret
					Editor spe = sp.edit();
					spe.putString(getString(R.string.key_requesttoken), "");
					spe.putString(getString(R.string.key_requestsecret), "");
					spe.commit();
					OAuthProvider provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
					provider.setOAuth10a(true);
					provider.retrieveAccessToken(consumer, verifier);
					AccessToken accessToken = new AccessToken(consumer.getToken(), consumer.getTokenSecret());
					Twitter twitter = new TwitterFactory().getInstance();
					twitter.setOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
					twitter.setOAuthAccessToken(accessToken);
					SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
					ContentValues values = new ContentValues();
					values.put(USERNAME, twitter.getScreenName());
					values.put(TOKEN, consumer.getToken());
					values.put(SECRET, consumer.getTokenSecret());
					values.put(SERVICE, TWITTER);
					db.insert(TABLE_ACCOUNTS, TOKEN, values);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void getAuth(int service, long account) {
		switch (service) {
		case TWITTER:
			try {
				CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
				OAuthProvider provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
				provider.setOAuth10a(true);
				String authUrl = provider.retrieveRequestToken(consumer, TWITTER_CALLBACK.toString());
				/*
				 * need to save the requestToken and secret
				 */
				SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
				Editor spe = sp.edit();
				spe.putString(getString(R.string.key_requesttoken), consumer.getToken());
				spe.putString(getString(R.string.key_requestsecret), consumer.getTokenSecret());
				spe.commit();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
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
            mFacebook.authorize(this, FACEBOOK_KEY, FACEBOOK_PERMISSIONS, new LoginDialogListener());
			break;
		}

	}

	private void listAccounts() {
		SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
		Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{_ID, USERNAME, SERVICE}, null, null, null, null, null);
		startManagingCursor(cursor);
		setListAdapter(new SimpleCursorAdapter(this, R.layout.accounts_row, cursor, new String[] {USERNAME}, new int[] {R.id.account_username}));
		db.close();
	}

	public void onClick(DialogInterface dialog, int which) {
		getAuth(which, NO_ACCOUNT);
		dialog.cancel();
	}
	
    private final class LoginDialogListener implements DialogListener {
        public void onComplete(Bundle values) {
        	mAsyncRunner.request("me", new RequestListener() {
				@Override
				public void onComplete(String response) {
		            try {
		                JSONObject json = Util.parseJson(response);
						SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
						ContentValues values = new ContentValues();
						values.put(USERNAME, json.getString("name"));
						values.put(TOKEN, mFacebook.getAccessToken());
						values.put(EXPIRY, mFacebook.getAccessExpires());
						values.put(SERVICE, FACEBOOK);
						db.insert(TABLE_ACCOUNTS, TOKEN, values);
		                ManageAccounts.this.runOnUiThread(new Runnable() {
		                    public void run() {
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
    }

}
