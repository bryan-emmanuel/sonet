package com.piusvelte.sonet;

import static com.piusvelte.sonet.SonetDatabaseHelper.TAG;
import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import static com.piusvelte.sonet.SonetDatabaseHelper.USERNAME;
import static com.piusvelte.sonet.SonetDatabaseHelper.SECRET;
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TOKEN;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TWITTER_KEY;
import static com.piusvelte.sonet.SonetDatabaseHelper.TWITTER_SECRET;
import static com.piusvelte.sonet.SonetDatabaseHelper.TWITTER_URL_ACCESS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TWITTER_URL_AUTHORIZE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TWITTER_URL_REQUEST;

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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.ContextMenu;
//import android.view.LayoutInflater;
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
	private int mService = 0;

	private OAuthProvider provider;
	private CommonsHttpOAuthConsumer consumer;

	private String CALLBACK_URL = "sonet://oauth";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accounts);
		registerForContextMenu(getListView());
		((Button) findViewById(R.id.button_add_account)).setOnClickListener(this);
		mSonetDatabaseHelper = new SonetDatabaseHelper(this);
		listAccounts();
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
		/* login prompt
		LayoutInflater inflater = LayoutInflater.from(this);
		final View authentication = inflater.inflate(R.layout.authentication, null);
		final EditText user = (EditText) authentication.findViewById(R.id.username);
		final EditText pass = (EditText) authentication.findViewById(R.id.password);
		pass.setTransformationMethod(new PasswordTransformationMethod());
		final SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
		Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{_ID, USERNAME, PASSWORD}, _ID + "=" + account, null, null, null, null);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			user.setText(cursor.getString(cursor.getColumnIndex(USERNAME)));
			pass.setText(cursor.getString(cursor.getColumnIndex(PASSWORD)));
		}
		cursor.close();
		user.setOnClickListener(this);
		pass.setOnClickListener(this);
		(new AlertDialog.Builder(this))
		.setView(authentication)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ContentValues values = new ContentValues();
				values.put(USERNAME, user.getText().toString());
				values.put(PASSWORD, pass.getText().toString());
				db.update(TABLE_ACCOUNTS, values, _ID + "=" + account, null);
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		})
		.show();
		 */
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

	@Override
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
	public void onClick(DialogInterface dialog, int which) {
		getAuth(which, NO_ACCOUNT);
		/* auth prompt
		LayoutInflater inflater = LayoutInflater.from(this);
		final int service = which;
		final View authentication = inflater.inflate(R.layout.authentication, null);
		final EditText username = (EditText) authentication.findViewById(R.id.username);
		username.setOnClickListener(this);
		final EditText password = (EditText) authentication.findViewById(R.id.password);
		password.setOnClickListener(this);
		(new AlertDialog.Builder(this))
		.setView(authentication)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
				ContentValues values = new ContentValues();
				values.put(TOKEN, username.getText().toString());
				values.put(SECRET, password.getText().toString());
				values.put(SERVICE, service);
				db.insert(TABLE_ACCOUNTS, TOKEN, values);
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		})
		.show();
		 */
		dialog.cancel();
	}

	@Override
	protected void onNewIntent(Intent intent) {

		super.onNewIntent(intent);

		Uri uri = intent.getData();
		if (uri != null && uri.toString().startsWith(CALLBACK_URL)) {

			String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);

			try {
				// this will populate token and token_secret in consumer
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
				values.put(SERVICE, mService);
				db.insert(TABLE_ACCOUNTS, TOKEN, values);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}

		}
	}

	private void getAuth(int service, long account) {
		switch (service) {
		case TWITTER:
			mService = TWITTER;
			try {
				consumer = new CommonsHttpOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
				provider = new DefaultOAuthProvider(TWITTER_URL_REQUEST, TWITTER_URL_ACCESS, TWITTER_URL_AUTHORIZE);
				provider.setOAuth10a(true);
				String authUrl = provider.retrieveRequestToken(consumer, CALLBACK_URL);
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			break;
		case FACEBOOK:
			break;
		}

	}
	
	private void listAccounts() {
		SQLiteDatabase db = mSonetDatabaseHelper.getWritableDatabase();
		Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{_ID, TOKEN, SECRET, SERVICE}, null, null, null, null, null);
		startManagingCursor(cursor);
		setListAdapter(new SimpleCursorAdapter(this, R.layout.accounts_row, cursor, new String[] {TOKEN}, new int[] {R.id.account_username}));
		db.close();
	}

}
