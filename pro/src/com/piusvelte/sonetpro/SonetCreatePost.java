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
package com.piusvelte.sonetpro;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.ads.*;
import com.piusvelte.sonetpro.R;
import com.piusvelte.sonetpro.Sonet.Accounts;
import com.piusvelte.sonetpro.Sonet.Widgets;

import static com.piusvelte.sonetpro.Sonet.*;
import static com.piusvelte.sonetpro.SonetTokens.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SonetCreatePost extends Activity implements OnKeyListener, OnClickListener, TextWatcher {
	private static final String TAG = "SonetCreatePost";
	private HashMap<Long, String> mAccountsToPost = new HashMap<Long, String>();
	private EditText mMessage;
	private Button mSend;
	private Button mLocation;
	private Button mAccounts;
	private TextView mCount;
	private ImageButton mPhoto;
	private String mLat = null,
			mLong = null;
	private SonetCrypto mSonetCrypto;
	private static final int PHOTO = 1;
	private String mPhotoPath;
	private HttpClient mHttpClient;
	private AlertDialog mDialog;

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
		mSend = (Button) findViewById(R.id.send);
		mLocation = (Button) findViewById(R.id.location);
		mAccounts = (Button) findViewById(R.id.accounts);
		mCount = (TextView) findViewById(R.id.count);
		mPhoto = (ImageButton) findViewById(R.id.photo);

		// load secretkey
		mSonetCrypto = SonetCrypto.getInstance(getApplicationContext());
		mHttpClient = SonetHttpClient.getThreadSafeClient(getApplicationContext());
		final Intent i = getIntent();
		if (i != null) {
			final String action = i.getAction();
			if ((action != null) && action.equals(Intent.ACTION_SEND)) {
				if (i.hasExtra(Intent.EXTRA_STREAM)) {
					getPhoto((Uri) i.getParcelableExtra(Intent.EXTRA_STREAM));
				}
				if (i.hasExtra(Intent.EXTRA_TEXT)) {
					final String text = i.getStringExtra(Intent.EXTRA_TEXT);
					mMessage.setText(text);
					mCount.setText(Integer.toString(text.length()));
				}
				chooseAccounts();
			} else {
				Uri data = i.getData();
				if ((data != null) && data.toString().contains(Accounts.CONTENT_URI.toString())) {
					// default to the account passed in, but allow selecting additional accounts
					Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{data.getLastPathSegment()}, null);
					if (account.moveToFirst()) {
						mAccountsToPost.put(account.getLong(0), null);
						switch (account.getInt(2)) {
						case FACEBOOK:
							mPhoto.setEnabled(true);
						case TWITTER:
						case FOURSQUARE:
							mLocation.setEnabled(true);
						}
					}
					account.close();
				} else if (i.hasExtra(Widgets.INSTANT_UPLOAD)) {
					// check if a photo path was passed and prompt user to select the account
					setPhoto(i.getStringExtra(Widgets.INSTANT_UPLOAD));
					chooseAccounts();
				}
			}
		}
		mAccounts.setOnClickListener(this);
		mLocation.setOnClickListener(this);
		mMessage.addTextChangedListener(this);
		mMessage.setOnKeyListener(this);
		mSend.setOnClickListener(this);
		mPhoto.setOnClickListener(this);

		setResult(RESULT_OK);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if ((mDialog != null) && mDialog.isShowing()) {
			mDialog.dismiss();
		}
	}

	private void setLocation(final long accountId) {
		final ProgressDialog loadingDialog = new ProgressDialog(this);
		final AsyncTask<Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>() {

			int serviceId;

			@Override
			protected String doInBackground(Void... none) {
				Cursor account = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SERVICE, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
				if (account.moveToFirst()) {
					SonetOAuth sonetOAuth;
					serviceId = account.getInt(account.getColumnIndex(Accounts.SERVICE));
					switch (serviceId) {
					case TWITTER:
						// anonymous requests are rate limited to 150 per hour
						// authenticated requests are rate limited to 350 per hour, so authenticate this!
						sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))), mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.SECRET))));
						return SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(new HttpGet(String.format(TWITTER_SEARCH, TWITTER_BASE_URL, mLat, mLong))));
					case FACEBOOK:
						return SonetHttpClient.httpResponse(mHttpClient, new HttpGet(String.format(FACEBOOK_SEARCH, FACEBOOK_BASE_URL, mLat, mLong, Saccess_token, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))))));
					case FOURSQUARE:
						return SonetHttpClient.httpResponse(mHttpClient, new HttpGet(String.format(FOURSQUARE_SEARCH, FOURSQUARE_BASE_URL, mLat, mLong, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))))));
					}
				}
				account.close();
				return null;
			}

			@Override
			protected void onPostExecute(String response) {
				if (loadingDialog.isShowing()) loadingDialog.dismiss();
				if (response != null) {
					switch (serviceId) {
					case TWITTER:
						try {
							JSONArray places = new JSONObject(response).getJSONObject(Sresult).getJSONArray(Splaces);
							final String placesNames[] = new String[places.length()];
							final String placesIds[] = new String[places.length()];
							for (int i = 0, i2 = places.length(); i < i2; i++) {
								JSONObject place = places.getJSONObject(i);
								placesNames[i] = place.getString(Sfull_name);
								placesIds[i] = place.getString(Sid);
							}
							mDialog = (new AlertDialog.Builder(SonetCreatePost.this))
							.setSingleChoiceItems(placesNames, -1, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mAccountsToPost.put(accountId, placesIds[which]);
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
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
						break;
					case FACEBOOK:
						try {
							JSONArray places = new JSONObject(response).getJSONArray(Sdata);
							final String placesNames[] = new String[places.length()];
							final String placesIds[] = new String[places.length()];
							for (int i = 0, i2 = places.length(); i < i2; i++) {
								JSONObject place = places.getJSONObject(i);
								placesNames[i] = place.getString(Sname);
								placesIds[i] = place.getString(Sid);
							}
							mDialog = (new AlertDialog.Builder(SonetCreatePost.this))
							.setSingleChoiceItems(placesNames, -1, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									mAccountsToPost.put(accountId, placesIds[which]);
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
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
						break;
					case FOURSQUARE:
						try {
							JSONArray groups = new JSONObject(response).getJSONObject(Sresponse).getJSONArray(Sgroups);
							for (int g = 0, g2 = groups.length(); g < g2; g++) {
								JSONObject group = groups.getJSONObject(g);
								if (group.getString(Sname).equals(SNearby)) {
									JSONArray places = group.getJSONArray(Sitems);
									final String placesNames[] = new String[places.length()];
									final String placesIds[] = new String[places.length()];
									for (int i = 0, i2 = places.length(); i < i2; i++) {
										JSONObject place = places.getJSONObject(i);
										placesNames[i] = place.getString(Sname);
										placesIds[i] = place.getString(Sid);
									}
									mDialog = (new AlertDialog.Builder(SonetCreatePost.this))
									.setSingleChoiceItems(placesNames, -1, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											mAccountsToPost.put(accountId, placesIds[which]);
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
									break;
								}
							}
						} catch (JSONException e) {
							Log.e(TAG, e.toString());
						}
						break;
					}
				} else {
					(Toast.makeText(SonetCreatePost.this, getString(R.string.failure), Toast.LENGTH_LONG)).show();
				}
			}

		};
		loadingDialog.setMessage(getString(R.string.loading));
		loadingDialog.setCancelable(true);
		loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
			@Override
			public void onCancel(DialogInterface dialog) {
				if (!asyncTask.isCancelled()) asyncTask.cancel(true);
			}
		});
		loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		loadingDialog.show();
		asyncTask.execute();
	}

	@Override
	public void onClick(View v) {
		if (v == mLocation) {
			// set the location
			if (mAccountsToPost.size() > 0) {
				LocationManager locationManager = (LocationManager) SonetCreatePost.this.getSystemService(Context.LOCATION_SERVICE);
				Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				if (location != null) {
					mLat = Double.toString(location.getLatitude());
					mLong = Double.toString(location.getLongitude());
					if (mAccountsToPost.size() == 1) {
						setLocation(mAccountsToPost.keySet().iterator().next());
					} else {
						// dialog to select an account
						Iterator<Long> accountIds = mAccountsToPost.keySet().iterator();
						HashMap<Long, String> accountEntries = new HashMap<Long, String>();
						while (accountIds.hasNext()) {
							Cursor account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(accountIds.next())}, null);
							if (account.moveToFirst()) {
								int service = account.getInt(account.getColumnIndex(Accounts.SERVICE));
								// only get accounts which have been selected and are supported for location
								if ((service == TWITTER) || (service == FACEBOOK) || (service == FOURSQUARE)) {
									accountEntries.put(account.getLong(account.getColumnIndex(Accounts._ID)), account.getString(account.getColumnIndex(Accounts.USERNAME)));
								}
							}
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
						}
					}
				} else {
					(Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_LONG)).show();
				}
			}
		} else if (v == mSend) {
			if (!mAccountsToPost.isEmpty()) {
				mMessage.setEnabled(false);
				mSend.setEnabled(false);
				mAccounts.setEnabled(false);
				mLocation.setEnabled(false);
				final ProgressDialog loadingDialog = new ProgressDialog(this);
				final AsyncTask<Void, String, Void> asyncTask = new AsyncTask<Void, String, Void>() {
					@Override
					protected Void doInBackground(Void... arg0) {
						Iterator<Map.Entry<Long, String>> entrySet = mAccountsToPost.entrySet().iterator();
						while (entrySet.hasNext()) {
							Map.Entry<Long, String> entry = entrySet.next();
							final long accountId = entry.getKey();
							final String placeId = entry.getValue();
							// post or comment!
							Cursor account = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE}, Accounts._ID + "=?", new String[]{Long.toString(accountId)}, null);
							if (account.moveToFirst()) {
								int service = account.getInt(account.getColumnIndex(Accounts.SERVICE));
								final String serviceName = Sonet.getServiceName(getResources(), service);
								publishProgress(serviceName);
								String message;
								SonetOAuth sonetOAuth;
								HttpPost httpPost;
								String response = null;
								switch (service) {
								case TWITTER:
									sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))), mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.SECRET))));
									// limit tweets to 140, breaking up the message if necessary
									message = mMessage.getText().toString();
									while (message.length() > 0) {
										final String send;
										if (message.length() > 140) {
											// need to break on a word
											int end = 0;
											int nextSpace = 0;
											for (int i = 0, i2 = message.length(); i < i2; i++) {
												end = nextSpace;
												if (message.substring(i, i + 1).equals(" ")) {
													nextSpace = i;
												}
											}
											// in case there are no spaces, just break on 140
											if (end == 0) {
												end = 140;
											}
											send = message.substring(0, end);
											message = message.substring(end + 1);
										} else {
											send = message;
											message = "";
										}
										httpPost = new HttpPost(String.format(TWITTER_UPDATE, TWITTER_BASE_URL));
										// resolve Error 417 Expectation by Twitter
										httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
										List<NameValuePair> params = new ArrayList<NameValuePair>();
										params.add(new BasicNameValuePair(Sstatus, send));
										if (placeId != null) {
											params.add(new BasicNameValuePair("place_id", placeId));
											params.add(new BasicNameValuePair("lat", mLat));
											params.add(new BasicNameValuePair("long", mLong));
										}
										try {
											httpPost.setEntity(new UrlEncodedFormEntity(params));
											response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPost));
										} catch (UnsupportedEncodingException e) {
											Log.e(TAG, e.toString());
										}
										publishProgress(serviceName, getString(response != null ? R.string.success : R.string.failure));
									}
									break;
								case FACEBOOK:
									if (mPhotoPath != null) {
										// upload photo
										// uploading a photo takes a long time, have the service handle it
										startService(
												new Intent(SonetCreatePost.this.getApplicationContext(), SonetService.class)
												.setAction(Sonet.ACTION_UPLOAD)
												.putExtra(Accounts.TOKEN, account.getString(account.getColumnIndex(Accounts.TOKEN)))
												.putExtra(Widgets.INSTANT_UPLOAD, mPhotoPath)
												.putExtra(Statuses.MESSAGE, mMessage.getText().toString())
										);
										publishProgress(serviceName + " photo");
//										httpPost = new HttpPost(String.format(FACEBOOK_PHOTOS, FACEBOOK_BASE_URL, Saccess_token, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN)))));
//										MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
//										File file = new File(mPhotoPath);
//										ContentBody fileBody = new FileBody(file);
//										entity.addPart("source", fileBody);
//										try {
//											entity.addPart("message", new StringBody(mMessage.getText().toString()));
//											httpPost.setEntity(entity);
//											response = SonetHttpClient.httpResponse(mHttpClient, httpPost);
//										} catch (UnsupportedEncodingException e) {
//											Log.e(TAG,e.toString());
//										}
//										publishProgress(serviceName + " photo", getString(response != null ? R.string.success : R.string.failure));
										// send checkins separately
										if (placeId != null) {
											//checkin
											httpPost = new HttpPost(String.format(FACEBOOK_CHECKIN, FACEBOOK_BASE_URL, Saccess_token, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN)))));
											List<NameValuePair> params = new ArrayList<NameValuePair>();
											params.add(new BasicNameValuePair("place", placeId));
											params.add(new BasicNameValuePair("coordinates", String.format(FACEBOOK_COORDINATES, mLat, mLong)));
											params.add(new BasicNameValuePair("message", mMessage.getText().toString()));
											try {
												httpPost.setEntity(new UrlEncodedFormEntity(params));
												response = SonetHttpClient.httpResponse(mHttpClient, httpPost);
											} catch (UnsupportedEncodingException e) {
												Log.e(TAG,e.toString());
											}
											publishProgress(serviceName + " checkin", getString(response != null ? R.string.success : R.string.failure));
										}
									} else if (placeId != null) {
										//checkin
										httpPost = new HttpPost(String.format(FACEBOOK_CHECKIN, FACEBOOK_BASE_URL, Saccess_token, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN)))));
										List<NameValuePair> params = new ArrayList<NameValuePair>();
										params.add(new BasicNameValuePair("place", placeId));
										params.add(new BasicNameValuePair("coordinates", String.format(FACEBOOK_COORDINATES, mLat, mLong)));
										params.add(new BasicNameValuePair("message", mMessage.getText().toString()));
										try {
											httpPost.setEntity(new UrlEncodedFormEntity(params));
											response = SonetHttpClient.httpResponse(mHttpClient, httpPost);
										} catch (UnsupportedEncodingException e) {
											Log.e(TAG,e.toString());
										}
										publishProgress(serviceName + " checkin", getString(response != null ? R.string.success : R.string.failure));
									} else {
										// regular post
										httpPost = new HttpPost(String.format(FACEBOOK_POST, FACEBOOK_BASE_URL, Saccess_token, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN)))));
										List<NameValuePair> params = new ArrayList<NameValuePair>();
										params.add(new BasicNameValuePair("message", mMessage.getText().toString()));
										try {
											httpPost.setEntity(new UrlEncodedFormEntity(params));
											response = SonetHttpClient.httpResponse(mHttpClient, httpPost);
										} catch (UnsupportedEncodingException e) {
											Log.e(TAG,e.toString());
										}
										publishProgress(serviceName, getString(response != null ? R.string.success : R.string.failure));
									}
									break;
								case MYSPACE:
									sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))), mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.SECRET))));
									try {
										HttpPut httpPut = new HttpPut(String.format(MYSPACE_URL_STATUSMOOD, MYSPACE_BASE_URL));
										httpPut.setEntity(new StringEntity(String.format(MYSPACE_STATUSMOOD_BODY, mMessage.getText().toString())));
										response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPut));
									} catch (IOException e) {
										Log.e(TAG, e.toString());
									}
									// warn users about myspace permissions
									if (response != null) {
										publishProgress(serviceName, getString(R.string.success));
									} else {
										publishProgress(serviceName, getString(R.string.failure) + " " + getString(R.string.myspace_permissions_message));
									}
									break;
								case FOURSQUARE:
									try {
										message = URLEncoder.encode(mMessage.getText().toString(), "UTF-8");
										if (placeId != null) {
											if (message != null) {
												httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN, FOURSQUARE_BASE_URL, placeId, message, mLat, mLong, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN)))));
											} else {
												httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_SHOUT, FOURSQUARE_BASE_URL, placeId, mLat, mLong, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN)))));												
											}
										} else {
											httpPost = new HttpPost(String.format(FOURSQUARE_CHECKIN_NO_VENUE, FOURSQUARE_BASE_URL, message, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN)))));
										}
										response = SonetHttpClient.httpResponse(mHttpClient, httpPost);
									} catch (UnsupportedEncodingException e) {
										Log.e(TAG, e.toString());
									}
									publishProgress(serviceName, getString(response != null ? R.string.success : R.string.failure));
									break;
								case LINKEDIN:
									sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))), mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.SECRET))));
									try {
										httpPost = new HttpPost(String.format(LINKEDIN_POST, LINKEDIN_BASE_URL));
										httpPost.setEntity(new StringEntity(String.format(LINKEDIN_POST_BODY, "", mMessage.getText().toString())));
										httpPost.addHeader(new BasicHeader("Content-Type", "application/xml"));
										response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPost));
									} catch (IOException e) {
										Log.e(TAG, e.toString());
									}
									publishProgress(serviceName, getString(response != null ? R.string.success : R.string.failure));
									break;
								case IDENTICA:
									sonetOAuth = new SonetOAuth(IDENTICA_KEY, IDENTICA_SECRET, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))), mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.SECRET))));
									// limit tweets to 140, breaking up the message if necessary
									message = mMessage.getText().toString();
									while (message.length() > 0) {
										final String send;
										if (message.length() > 140) {
											// need to break on a word
											int end = 0;
											int nextSpace = 0;
											for (int i = 0, i2 = message.length(); i < i2; i++) {
												end = nextSpace;
												if (message.substring(i, i + 1).equals(" ")) {
													nextSpace = i;
												}
											}
											// in case there are no spaces, just break on 140
											if (end == 0) {
												end = 140;
											}
											send = message.substring(0, end);
											message = message.substring(end + 1);
										} else {
											send = message;
											message = "";
										}
										httpPost = new HttpPost(String.format(IDENTICA_UPDATE, IDENTICA_BASE_URL));
										// resolve Error 417 Expectation by Twitter
										httpPost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
										List<NameValuePair> params = new ArrayList<NameValuePair>();
										params.add(new BasicNameValuePair(Sstatus, send));
										if (placeId != null) {
											params.add(new BasicNameValuePair("place_id", placeId));
											params.add(new BasicNameValuePair("lat", mLat));
											params.add(new BasicNameValuePair("long", mLong));
										}
										try {
											httpPost.setEntity(new UrlEncodedFormEntity(params));
											response = SonetHttpClient.httpResponse(mHttpClient, sonetOAuth.getSignedRequest(httpPost));
										} catch (UnsupportedEncodingException e) {
											Log.e(TAG, e.toString());
										}
										publishProgress(serviceName, getString(response != null ? R.string.success : R.string.failure));
									}
									break;
								case CHATTER:
									// need to get an updated access_token
									response = SonetHttpClient.httpResponse(mHttpClient, new HttpPost(String.format(CHATTER_URL_ACCESS, CHATTER_KEY, mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))))));
									if (response != null) {
										try {
											JSONObject jobj = new JSONObject(response);
											if (jobj.has("instance_url") && jobj.has(Saccess_token)) {
												httpPost = new HttpPost(String.format(CHATTER_URL_POST, jobj.getString("instance_url"), Uri.encode(mMessage.getText().toString())));
												httpPost.setHeader("Authorization", "OAuth " + jobj.getString(Saccess_token));
												response = SonetHttpClient.httpResponse(mHttpClient, httpPost);
											}
										} catch (JSONException e) {
											Log.e(TAG, serviceName + ":" + e.toString());
											Log.e(TAG, response);
										}
									}
									publishProgress(serviceName, getString(response != null ? R.string.success : R.string.failure));
									break;
								}
							}
							account.close();
						}
						return null;
					}

					@Override
					protected void onProgressUpdate(String... params) {
						if (params.length == 1) {
							loadingDialog.setMessage(String.format(getString(R.string.sending), params[0]));
						} else {
							(Toast.makeText(SonetCreatePost.this, params[0] + " " + params[1], Toast.LENGTH_LONG)).show();
						}
					}

					@Override
					protected void onPostExecute(Void result) {
						if (loadingDialog.isShowing()) loadingDialog.dismiss();
						finish();
					}

				};
				loadingDialog.setMessage(getString(R.string.loading));
				loadingDialog.setCancelable(true);
				loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
					@Override
					public void onCancel(DialogInterface dialog) {
						if (!asyncTask.isCancelled()) asyncTask.cancel(true);
					}
				});
				loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				loadingDialog.show();
				asyncTask.execute();
			} else {
				(Toast.makeText(SonetCreatePost.this, "no accounts selected", Toast.LENGTH_LONG)).show();
				mMessage.setEnabled(true);
				mSend.setEnabled(true);
				mAccounts.setEnabled(true);
				mLocation.setEnabled(true);
			}
		} else if (v == mAccounts) {
			chooseAccounts();
		} else if (v == mPhoto) {
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(Intent.createChooser(intent, "Select Picture"), PHOTO);
		}
	}

	protected void getPhoto(Uri uri) {
		mPhoto.setEnabled(false);
		final ProgressDialog loadingDialog = new ProgressDialog(this);
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
				if (loadingDialog.isShowing()) loadingDialog.dismiss();
				if (path != null) {
					setPhoto(path);
				} else {
					(Toast.makeText(SonetCreatePost.this, "error retrieving the photo path", Toast.LENGTH_LONG)).show();
				}
				mPhoto.setEnabled(true);
			}
		};
		loadingDialog.setMessage(getString(R.string.loading));
		loadingDialog.setCancelable(true);
		loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {				
			@Override
			public void onCancel(DialogInterface dialog) {
				if (!asyncTask.isCancelled()) asyncTask.cancel(true);
			}
		});
		loadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		loadingDialog.show();
		asyncTask.execute(uri);
	}

	protected void setPhoto(String path) {
		mPhotoPath = path;
		if (mPhotoPath == null) {
			mPhoto.setImageResource(android.R.drawable.ic_menu_camera);
		} else {
			Bitmap b = BitmapFactory.decodeFile(mPhotoPath, Sonet.sBFOptions);
			if (b != null) {
				mPhoto.setImageBitmap(Bitmap.createScaledBitmap(b, 48, 48, false));
				b.recycle();
				(Toast.makeText(SonetCreatePost.this, "Currently, the photo will only be uploaded Facebook accounts.", Toast.LENGTH_LONG)).show();
			} else {
				mPhoto.setImageResource(android.R.drawable.ic_menu_camera);
				(Toast.makeText(SonetCreatePost.this, "Error retrieving photo.", Toast.LENGTH_LONG)).show();
			}
		}
	}

	protected void chooseAccounts() {
		// don't limit accounts to the widget...
		Cursor c = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY, Accounts.SERVICE}, null, null, null);
		if (c.moveToFirst()) {
			int i = 0,
					count = c.getCount();
			final long[] accountIndexes = new long[count];
			final String[] accounts = new String[count];
			final boolean[] defaults = new boolean[count];
			final int[] accountServices = new int[count];
			while (!c.isAfterLast()) {
				long id = c.getLong(0);
				accountIndexes[i] = id;
				accounts[i] = c.getString(1);
				accountServices[i] = c.getInt(2);
				defaults[i++] = mAccountsToPost.containsKey(id);
				c.moveToNext();
			}
			mDialog = (new AlertDialog.Builder(this))
			.setTitle(R.string.accounts)
			.setMultiChoiceItems(accounts, defaults, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					if (isChecked) {
						final long accountId = accountIndexes[which];
						mAccountsToPost.put(accountId, null);
						// set location, only for supported services, TWITTER, FACEBOOK, FOURSQUARE
						switch (accountServices[which]) {
						case FACEBOOK:
							if (!mPhoto.isEnabled()) {
								mPhoto.setEnabled(true);
							}
						case TWITTER:
						case CHATTER:
						case FOURSQUARE:
							if (!mLocation.isEnabled()) {
								mLocation.setEnabled(true);
							}
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
						mAccountsToPost.remove(accountIndexes[which]);
						boolean photoEnabled = false;
						boolean locationEnabled = false;
						// check selected accounts for options
						for (int i = 0, i2 = accountServices.length; i < i2; i++) {
							// skip the account which was removed
							if (i != which) {
								switch (accountServices[i]) {
								case FACEBOOK:
									if (!photoEnabled) {
										photoEnabled = defaults[i];
									}
								case TWITTER:
								case FOURSQUARE:
									if (!locationEnabled) {
										locationEnabled = defaults[i];
									}
								}
								if (photoEnabled && locationEnabled) {
									break;
								}
							}
						}
						mLocation.setEnabled(locationEnabled);
						mPhoto.setEnabled(photoEnabled);
						if (!photoEnabled) {
							setPhoto(null);
						}
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case PHOTO:
			if (resultCode == RESULT_OK) {
				getPhoto(data.getData());
			}
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