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

import static com.piusvelte.sonet.Sonet.*;
import static com.piusvelte.sonet.SonetTokens.*;

import java.util.regex.Matcher;

import mobi.intuitit.android.content.LauncherIntent;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;
import com.piusvelte.sonet.Sonet.Widget_accounts;
import com.piusvelte.sonet.Sonet.Widgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.util.Log;
import android.widget.Toast;

public class StatusDialog extends Activity implements OnClickListener {
	private static final String TAG = "StatusDialog";
	private int mAppWidgetId = -1;
	private long mAccount = Sonet.INVALID_ACCOUNT_ID;
	private Uri mData;
	private static final int COMMENT = 0;
	private static final int POST = 1;
	private static final int SETTINGS = 2;
	private static final int NOTIFICATIONS = 3;
	private static final int REFRESH = 4;
	private static final int PROFILE = 5;
	private int[] mAppWidgetIds;
	private String[] items = null;
	private String mSid = null;
	private String mEsid = null;
	private int mService;
	private String mServiceName = null;
	private ProgressDialog mLoadingDialog;
	private StatusLoader mStatusLoader;
	private Rect mRect;
	private SonetCrypto mSonetCrypto;
	private boolean mFinish = false;
	private String mFilePath = null;
	private AlertDialog mDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// load secretkey
		mSonetCrypto = SonetCrypto.getInstance(getApplicationContext());
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(Widgets.INSTANT_UPLOAD)) {
				mFilePath = intent.getStringExtra(Widgets.INSTANT_UPLOAD);
				Log.d(TAG,"upload photo?"+mFilePath);
			} else {
				mData = intent.getData();
				if (mData != null) {
					mData = intent.getData();
					if (intent.hasExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS)) {
						mRect = intent.getParcelableExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS);
					} else {
						mRect = intent.getSourceBounds();
					}
					Log.d(TAG,"data:"+mData.toString());
					// need to use a thread here to avoid anr
					mLoadingDialog = new ProgressDialog(this);
					mLoadingDialog.setMessage(getString(R.string.loading));
					mLoadingDialog.setCancelable(true);
					mLoadingDialog.setOnCancelListener(new OnCancelListener() {

						@Override
						public void onCancel(DialogInterface arg0) {
							if (mStatusLoader != null) {
								mStatusLoader.cancel(true);
							}
							finish();
						}
						
					});
					mLoadingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							StatusDialog.this.finish();
						}
						
					});
					mLoadingDialog.show();
					mStatusLoader = new StatusLoader();
					mStatusLoader.execute();
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mFilePath != null) {
			mDialog = (new AlertDialog.Builder(this))
			.setTitle(R.string.uploadprompt)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivityForResult(new Intent(getApplicationContext(), SonetCreatePost.class).putExtra(Widgets.INSTANT_UPLOAD, mFilePath), RESULT_REFRESH);
					dialog.dismiss();
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					StatusDialog.this.finish();
				}
			}).create();
			mDialog.show();
		} else {
			// check if the dialog is still loading
			if (mFinish) {
				finish();
			} else if ((mLoadingDialog == null) || !mLoadingDialog.isShowing()) {
				showDialog();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if ((mLoadingDialog != null) && mLoadingDialog.isShowing()) {
			mLoadingDialog.dismiss();
		}
		if (mStatusLoader != null) {
			mStatusLoader.cancel(true);
		}
		if ((mDialog != null) && mDialog.isShowing()) {
			mDialog.dismiss();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == RESULT_REFRESH) && (resultCode == RESULT_OK)) {
			finish();
		}
	}

	private void showDialog() {
		if (mService == SMS) {
			// if mRect go straight to message app...
			if (mRect != null) {
				QuickContact.showQuickContact(this, mRect, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, mEsid), QuickContact.MODE_LARGE, null);
			} else {
				startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + mEsid)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			}
		} else if (mService == RSS) {
			if (mEsid != null) {
				startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(mEsid)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			} else {
				(Toast.makeText(StatusDialog.this, "RSS item has no link", Toast.LENGTH_LONG)).show();
				StatusDialog.this.finish();
			}
		} else if (items != null) {
			// offer options for Comment, Post, Settings and Refresh
			// loading the likes/retweet and other options takes too long, so load them in the SonetCreatePost.class
			mDialog = (new AlertDialog.Builder(this))
			.setItems(items, this)
			.setCancelable(true)
			.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface arg0) {
					StatusDialog.this.finish();
				}
				
			})
			.create();
			mDialog.show();
		} else {
			if (mAppWidgetId != Sonet.INVALID_ACCOUNT_ID) {
				// informational messages go to settings
				mFinish = true;
				startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			} else {
				(Toast.makeText(StatusDialog.this, "This widget is reloading. Please try again after it has completed or use the app to update the widget.", Toast.LENGTH_LONG)).show();
				// force widgets rebuild
				startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH));
				finish();
			}
		}
	}

	private void onErrorExit(String serviceName) {
		(Toast.makeText(StatusDialog.this, serviceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
		StatusDialog.this.finish();
	}

	@Override
	public void onClick(final DialogInterface dialog, int which) {
		switch (which) {
		case COMMENT:
			if (mAppWidgetId != -1) {
				if (mService == GOOGLEPLUS) {
					//TODO: open browser for now...
					startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://plus.google.com")));
				} else if (mService == PINTEREST) {
					if (mSid != null) {
						startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format(PINTEREST_PIN, mSid))));
					} else {
						startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://pinterest.com")));
					}
				} else {
					startActivity(new Intent(this, SonetComments.class).setData(mData).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				}
			} else {
				(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
			}
			dialog.cancel();
			break;
		case POST:
			if (mAppWidgetId != -1) {
				if (mService == GOOGLEPLUS) {
					//TODO: open browser for now...
					startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://plus.google.com")));
				} else if (mService == PINTEREST) {
					startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://pinterest.com")));
				} else {
					startActivity(new Intent(this, SonetCreatePost.class).setData(Uri.withAppendedPath(Accounts.CONTENT_URI, Long.toString(mAccount))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				}
				dialog.cancel();
			} else {
				// no widget sent in, dialog to select one
				String[] widgets = getAllWidgets();
				if (widgets.length > 0) {
					mDialog = (new AlertDialog.Builder(this))
					.setItems(widgets, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// no account, dialog to select one
							// don't limit accounts to the widget
							Cursor c = StatusDialog.this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY}, null, null, null);
							if (c.moveToFirst()) {
								int iid = c.getColumnIndex(Accounts._ID),
										iusername = c.getColumnIndex(Accounts.USERNAME),
										i = 0;
								final long[] accountIndexes = new long[c.getCount()];
								final String[] accounts = new String[c.getCount()];
								while (!c.isAfterLast()) {
									long id = c.getLong(iid);
									accountIndexes[i] = id;
									accounts[i++] = c.getString(iusername);
									c.moveToNext();
								}
								arg0.cancel();
								mDialog = (new AlertDialog.Builder(StatusDialog.this))
								.setTitle(R.string.accounts)
								.setSingleChoiceItems(accounts, -1, new OnClickListener() {
									@Override
									public void onClick(DialogInterface arg0, int which) {
										startActivity(new Intent(StatusDialog.this, SonetCreatePost.class).setData(Uri.withAppendedPath(Accounts.CONTENT_URI, Long.toString(accountIndexes[which]))));
										arg0.cancel();
									}
								})
								.setCancelable(true)
								.setOnCancelListener(new OnCancelListener() {
									@Override
									public void onCancel(DialogInterface arg0) {
										dialog.cancel();
									}
								})
								.create();
								mDialog.show();
							} else {
								(Toast.makeText(StatusDialog.this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
								dialog.cancel();
							}
							c.close();
						}					
					})
					.setCancelable(true)
					.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface arg0) {
							dialog.cancel();
						}						
					}).create();
					mDialog.show();
				} else {
					(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
					dialog.cancel();
				}
			}
			break;
		case SETTINGS:
			if (mAppWidgetId != -1) {
				startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
				dialog.cancel();
			} else {
				// no widget sent in, dialog to select one
				String[] widgets = getAllWidgets();
				if (widgets.length > 0) {
					mDialog = (new AlertDialog.Builder(this))
					.setItems(widgets, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							startActivity(new Intent(StatusDialog.this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[arg1]));
							arg0.cancel();
						}					
					})
					.setCancelable(true)
					.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface arg0) {
							dialog.cancel();
						}
					})
					.create();
					mDialog.show();
				} else {
					(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
					dialog.cancel();
				}
			}
			break;
		case NOTIFICATIONS:
			startActivity(new Intent(this, SonetNotifications.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			dialog.cancel();
		case REFRESH:
			if (mAppWidgetId != -1) {
				(Toast.makeText(getApplicationContext(), getString(R.string.refreshing), Toast.LENGTH_LONG)).show();
				startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
				dialog.cancel();
			} else {
				// no widget sent in, dialog to select one
				String[] widgets = getAllWidgets();
				if (widgets.length > 0) {
					mDialog = (new AlertDialog.Builder(this))
					.setItems(widgets, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							(Toast.makeText(StatusDialog.this.getApplicationContext(), getString(R.string.refreshing), Toast.LENGTH_LONG)).show();
							startService(new Intent(StatusDialog.this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetIds[arg1]}));
							arg0.cancel();
						}					
					})
					.setPositiveButton(R.string.refreshallwidgets, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int which) {
							// refresh all
							(Toast.makeText(StatusDialog.this.getApplicationContext(), getString(R.string.refreshing), Toast.LENGTH_LONG)).show();
							startService(new Intent(StatusDialog.this, SonetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, mAppWidgetIds));
							arg0.cancel();
						}
					})
					.setCancelable(true)
					.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface arg0) {
							dialog.cancel();
						}						
					})
					.create();
					mDialog.show();
				} else {
					dialog.cancel();
				}
			}
			break;
		case PROFILE:
			Cursor account;
			final AsyncTask<String, Void, String> asyncTask;
			// get the resources
			switch (mService) {
			case TWITTER:
				account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
				if (account.moveToFirst()) {
					final ProgressDialog loadingDialog = new ProgressDialog(this);
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							SonetOAuth sonetOAuth = new SonetOAuth(TWITTER_KEY, TWITTER_SECRET, arg0[0], arg0[1]);
							return SonetHttpClient.httpResponse(SonetHttpClient.getThreadSafeClient(getApplicationContext()), sonetOAuth.getSignedRequest(new HttpGet(String.format(TWITTER_USER, TWITTER_BASE_URL, mEsid))));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									JSONObject user = new JSONObject(response);
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format(TWITTER_PROFILE, user.getString("screen_name")))));
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(mServiceName);
								}
							} else {
								onErrorExit(mServiceName);
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
					asyncTask.execute(mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))), mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.SECRET))));
				}
				account.close();
				break;
			case FACEBOOK:
				account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
				if (account.moveToFirst()) {
					final ProgressDialog loadingDialog = new ProgressDialog(this);
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							return SonetHttpClient.httpResponse(SonetHttpClient.getThreadSafeClient(getApplicationContext()), new HttpGet(String.format(FACEBOOK_USER, FACEBOOK_BASE_URL, mEsid, Saccess_token, arg0[0])));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse((new JSONObject(response)).getString("link"))));
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(mServiceName);
								}
							} else {
								onErrorExit(mServiceName);
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
					asyncTask.execute(mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))));
				}
				account.close();
				break;
			case MYSPACE:
				account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
				if (account.moveToFirst()) {
					final ProgressDialog loadingDialog = new ProgressDialog(this);
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							SonetOAuth sonetOAuth = new SonetOAuth(MYSPACE_KEY, MYSPACE_SECRET, arg0[0], arg0[1]);
							return SonetHttpClient.httpResponse(SonetHttpClient.getThreadSafeClient(getApplicationContext()), sonetOAuth.getSignedRequest(new HttpGet(String.format(MYSPACE_USER, MYSPACE_BASE_URL, mEsid))));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse((new JSONObject(response)).getJSONObject("person").getString("profileUrl"))));
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(mServiceName);
								}
							} else {
								onErrorExit(mServiceName);
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
					asyncTask.execute(mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))), mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.SECRET))));
				}
				account.close();
				break;
			case FOURSQUARE:
				startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format(FOURSQUARE_URL_PROFILE, mEsid))));
				break;
			case LINKEDIN:
				account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
				if (account.moveToFirst()) {
					final ProgressDialog loadingDialog = new ProgressDialog(this);
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							SonetOAuth sonetOAuth = new SonetOAuth(LINKEDIN_KEY, LINKEDIN_SECRET, arg0[0], arg0[1]);
							HttpGet httpGet = new HttpGet(String.format(LINKEDIN_URL_USER, mEsid));
							for (String[] header : LINKEDIN_HEADERS) httpGet.setHeader(header[0], header[1]);
							return SonetHttpClient.httpResponse(SonetHttpClient.getThreadSafeClient(getApplicationContext()), sonetOAuth.getSignedRequest(httpGet));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse((new JSONObject(response)).getJSONObject("siteStandardProfileRequest").getString("url").replaceAll("\\\\", ""))));
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(mServiceName);
								}
							} else {
								onErrorExit(mServiceName);
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
					asyncTask.execute(mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))), mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.SECRET))));
				}
				account.close();
				break;
			case IDENTICA:
				account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
				if (account.moveToFirst()) {
					final ProgressDialog loadingDialog = new ProgressDialog(this);
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							SonetOAuth sonetOAuth = new SonetOAuth(IDENTICA_KEY, IDENTICA_SECRET, arg0[0], arg0[1]);
							return SonetHttpClient.httpResponse(SonetHttpClient.getThreadSafeClient(getApplicationContext()), sonetOAuth.getSignedRequest(new HttpGet(String.format(IDENTICA_USER, IDENTICA_BASE_URL, mEsid))));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									JSONObject user = new JSONObject(response);
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format(IDENTICA_PROFILE, user.getString("screen_name")))));
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(mServiceName);
								}
							} else {
								onErrorExit(mServiceName);
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
					asyncTask.execute(mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))), mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.SECRET))));
				}
				account.close();
				break;
			case GOOGLEPLUS:
				startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format(GOOGLEPLUS_PROFILE, mEsid))));
				break;
			case PINTEREST:
				if (mEsid != null) {
					startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format(PINTEREST_PROFILE, mEsid))));
				} else {
					startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://pinterest.com")));
				}
				break;
			case CHATTER:
				account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
				if (account.moveToFirst()) {
					final ProgressDialog loadingDialog = new ProgressDialog(this);
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							// need to get an instance
							return SonetHttpClient.httpResponse(SonetHttpClient.getThreadSafeClient(getApplicationContext()), new HttpPost(String.format(CHATTER_URL_ACCESS, CHATTER_KEY, arg0[0])));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									JSONObject jobj = new JSONObject(response);
									if (jobj.has("instance_url")) {
										startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(jobj.getString("instance_url") + "/" + mEsid)));
									}
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(mServiceName);
								}
							} else {
								onErrorExit(mServiceName);
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
					asyncTask.execute(mSonetCrypto.Decrypt(account.getString(account.getColumnIndex(Accounts.TOKEN))));
				}
				account.close();
				break;
			}
			break;
		default:
			if ((items != null) && (which < items.length) && (items[which] != null)) {
				// open link
				Matcher m = Sonet.getLinksMatcher(items[which]);
				if (m.find()) {
					startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(m.group())));
				}
			} else {
				(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
			}
		}
	}

	private String[] getAllWidgets() {
		mAppWidgetIds = new int[0];
		// validate appwidgetids from appwidgetmanager
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		mAppWidgetIds = Sonet.arrayCat(
				Sonet.arrayCat(appWidgetManager.getAppWidgetIds(new ComponentName(
						this, SonetWidget_4x2.class)),
						appWidgetManager.getAppWidgetIds(new ComponentName(
								this, SonetWidget_4x3.class))),
								appWidgetManager.getAppWidgetIds(new ComponentName(this,
										SonetWidget_4x4.class)));
		int[] removeAppWidgets = new int[0];
		this.getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=?", new String[] { "" });
		this.getContentResolver().delete(Widget_accounts.CONTENT_URI, Widget_accounts.WIDGET + "=?", new String[] { "" });
		Cursor widgets = this.getContentResolver().query(Widgets.CONTENT_URI, new String[] {Widgets._ID, Widgets.WIDGET}, Widgets.ACCOUNT + "=?", new String[] { Long.toString(Sonet.INVALID_ACCOUNT_ID) }, null);
		if (widgets.moveToFirst()) {
			int iwidget = widgets.getColumnIndex(Widgets.WIDGET), appWidgetId;
			while (!widgets.isAfterLast()) {
				appWidgetId = widgets.getInt(iwidget);
				if ((appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) && !Sonet.arrayContains(mAppWidgetIds, appWidgetId)) removeAppWidgets = Sonet.arrayAdd(removeAppWidgets, appWidgetId);
				widgets.moveToNext();
			}
		}
		widgets.close();
		if (removeAppWidgets.length > 0) {
			// remove phantom widgets
			for (int appWidgetId : removeAppWidgets) {
				this.getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
				this.getContentResolver().delete(Widget_accounts.CONTENT_URI, Widget_accounts.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
				this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
			}
		}
		String[] widgetsOptions = new String[mAppWidgetIds.length];
		for (int i = 0; i < mAppWidgetIds.length; i++) {
			AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(mAppWidgetIds[i]);
			String providerName = info.provider.getClassName();
			widgetsOptions[i] = Integer.toString(mAppWidgetIds[i])
					+ " ("
					+ (providerName == SonetWidget_4x2.class.getName() ? "4x2"
							: providerName == SonetWidget_4x3.class
							.getName() ? "4x3" : "4x4") + ")";
		}
		return widgetsOptions;
	}

	class StatusLoader extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG,"get status: "+mData.getLastPathSegment());
			Cursor c = getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.WIDGET, Statuses_styles.ACCOUNT, Statuses_styles.ESID, Statuses_styles.MESSAGE, Statuses_styles.FRIEND, Statuses_styles.SERVICE, Statuses_styles.SID}, Statuses_styles._ID + "=?", new String[] {mData.getLastPathSegment()}, null);
			if (c.moveToFirst()) {
				mAppWidgetId = c.getInt(1);
				mAccount = c.getLong(2);
				Log.d(TAG,"account: "+mAccount);
				// informational messages go directly to settings, otherwise, load up the options
				if (mAccount != Sonet.INVALID_ACCOUNT_ID) {
					mService = c.getInt(6);
					Log.d(TAG,"service: "+mService);
					if (mService == PINTEREST) {
						// pinterest uses the username for the profile page
						mEsid = c.getString(5);
					} else {
						mEsid = mSonetCrypto.Decrypt(c.getString(3));
					}
					mSid = mSonetCrypto.Decrypt(c.getString(7));
					if (mService == SMS) {
						// lookup the contact, else null mRect
						Cursor phones = getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(mEsid)), new String[]{ContactsContract.PhoneLookup.LOOKUP_KEY}, null, null, null);
						if (phones.moveToFirst()) {
							mEsid = phones.getString(0);
						} else {
							mRect = null;
						}
						phones.close();
					} else if (mService != RSS) {
						mServiceName = Sonet.getServiceName(getResources(), mService);
						//						// parse any links
						//						Matcher m = Sonet.getLinksMatcher(c.getString(4));
						//						int count = 0;
						//						while (m.find()) {
						//							count++;
						//						}
						// get links from table
						Cursor links = getContentResolver().query(Status_links.CONTENT_URI, new String[]{Status_links.LINK_URI, Status_links.LINK_TYPE}, Status_links.STATUS_ID + "=?", new String[]{Long.toString(c.getLong(0))}, null);
						//						count += links.getCount();
						int count = links.getCount();
						items = new String[PROFILE + count + 1];
						// for facebook wall posts, remove everything after the " > "
						String friend = c.getString(5);
						if (friend.indexOf(">") > 0) {
							friend = friend.substring(0, friend.indexOf(">") - 1);
						}
						if (mService == TWITTER) {
							items[COMMENT] = getString(R.string.reply) + " @" + friend;
							items[POST] = getString(R.string.tweet);
						} else if (mService == IDENTICA) {
							items[COMMENT] = getString(R.string.reply) + " @" + friend;
							items[POST] = String.format(getString(R.string.update_status), mServiceName);
						} else {
							items[COMMENT] = String.format(getString(R.string.comment_status), friend);
							items[POST] = String.format(getString(R.string.update_status), mServiceName);
						}
						items[SETTINGS] = getString(R.string.accounts_and_settings);
						items[NOTIFICATIONS] = getString(R.string.notifications);
						items[REFRESH] = getString(R.string.button_refresh);
						items[PROFILE] = String.format(getString(R.string.userProfile), friend);
						count = PROFILE + 1;
						//						m.reset();
						//						while (m.find()) {
						//							items[count++] = m.group();
						//						}
						// links
						if (links.moveToFirst()) {
							while (!links.isAfterLast()) {
								items[count++] = links.getString(1) + ": " + links.getString(0);
								links.moveToNext();
							}
						}
						links.close();
					}
				}
			}
			c.close();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mLoadingDialog.dismiss();
			showDialog();
		}
	}

}
