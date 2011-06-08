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

import static com.piusvelte.sonet.Sonet.ACCOUNTS_QUERY;
import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.BUZZ_BASE_URL;
import static com.piusvelte.sonet.Sonet.BUZZ_URL_USER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.FACEBOOK_BASE_URL;
import static com.piusvelte.sonet.Sonet.FACEBOOK_USER;
import static com.piusvelte.sonet.Sonet.FOURSQUARE;
import static com.piusvelte.sonet.Sonet.FOURSQUARE_URL_PROFILE;
import static com.piusvelte.sonet.Sonet.LINKEDIN;
import static com.piusvelte.sonet.Sonet.LINKEDIN_HEADERS;
import static com.piusvelte.sonet.Sonet.LINKEDIN_URL_USER;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.MYSPACE_BASE_URL;
import static com.piusvelte.sonet.Sonet.MYSPACE_USER;
import static com.piusvelte.sonet.Sonet.TOKEN;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.TWITTER_BASE_URL;
import static com.piusvelte.sonet.Sonet.TWITTER_PROFILE;
import static com.piusvelte.sonet.Sonet.TWITTER_USER;
import static com.piusvelte.sonet.SonetTokens.BUZZ_API_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_KEY;
import static com.piusvelte.sonet.SonetTokens.BUZZ_SECRET;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_KEY;
import static com.piusvelte.sonet.SonetTokens.LINKEDIN_SECRET;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_KEY;
import static com.piusvelte.sonet.SonetTokens.MYSPACE_SECRET;
import static com.piusvelte.sonet.SonetTokens.TWITTER_KEY;
import static com.piusvelte.sonet.SonetTokens.TWITTER_SECRET;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Statuses_styles;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class StatusDialog extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
	private static final String TAG = "StatusDialog";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private long mAccount = Sonet.INVALID_ACCOUNT_ID;
	private Uri mData;
	private static final int COMMENT = 0;
	private static final int POST = COMMENT + 1;
	private static final int SETTINGS = POST + 1;
	private static final int REFRESH = SETTINGS + 1;
	private static final int PROFILE = REFRESH + 1;
	private int[] mAppWidgetIds;
	private String[] items;
	private String mEsid;
	private int mService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ((getIntent() != null) && (getIntent().getData() != null)) {
			mData = getIntent().getData();
			Cursor c = this.getContentResolver().query(Statuses_styles.CONTENT_URI, new String[]{Statuses_styles._ID, Statuses_styles.WIDGET, Statuses_styles.ACCOUNT, Statuses_styles.ESID, Statuses_styles.MESSAGE, Statuses_styles.FRIEND, Statuses_styles.SERVICE}, Statuses_styles._ID + "=?", new String[] {mData.getLastPathSegment()}, null);
			if (c.moveToFirst()) {
				mAppWidgetId = c.getInt(c.getColumnIndex(Statuses_styles.WIDGET));
				mAccount = c.getLong(c.getColumnIndex(Statuses_styles.ACCOUNT));
				mEsid = Sonet.removeUnderscore(c.getString(c.getColumnIndex(Statuses_styles.ESID)));
				mService = c.getInt(c.getColumnIndex(Statuses_styles.SERVICE));
				// parse any links
				Matcher m = Pattern.compile("\\bhttp(s)?://\\S+\\b", Pattern.CASE_INSENSITIVE).matcher(c.getString(c.getColumnIndex(Statuses_styles.MESSAGE)));
				int count = 0;
				while (m.find()) {
					count++;
				}
				items = new String[PROFILE + count + 1];
				items[COMMENT] = getString(R.string.comment);
				items[POST] = getString(R.string.button_post);
				items[SETTINGS] = getString(R.string.settings);
				items[REFRESH] = getString(R.string.button_refresh);
				items[PROFILE] = String.format(getString(R.string.userProfile), c.getString(c.getColumnIndex(Statuses_styles.FRIEND)));
				count = PROFILE + 1;
				m.reset();
				while (m.find()) {
					items[count++] = m.group();
				}
			} else {
				mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
				items = new String[]{getString(R.string.comment), getString(R.string.button_post), getString(R.string.settings), getString(R.string.button_refresh)};
			}
			c.close();
		} else {
			items = new String[]{getString(R.string.comment), getString(R.string.button_post), getString(R.string.settings), getString(R.string.button_refresh)};
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// offer options for Comment, Post, Settings and Refresh
		// loading the likes/retweet and other options takes too long, so load them in the SonetCreatePost.class
		(new AlertDialog.Builder(this))
		.setItems(items, this)
		.setCancelable(true)
		.setOnCancelListener(this)
		.show();
	}
	
	private void onErrorExit(String serviceName) {
		(Toast.makeText(StatusDialog.this, serviceName + " " + getString(R.string.failure), Toast.LENGTH_LONG)).show();
		StatusDialog.this.finish();
	}

	@Override
	public void onClick(final DialogInterface dialog, int which) {
		switch (which) {
		case COMMENT:
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				startActivity(new Intent(this, SonetComments.class).setData(mData));
			} else {
				(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
			}
			dialog.cancel();
			break;
		case POST:
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {				
				startActivity(new Intent(this, SonetCreatePost.class).setData(Uri.withAppendedPath(Accounts.CONTENT_URI, Long.toString(mAccount))));
				dialog.cancel();
			} else {
				// no widget sent in, dialog to select one
				String[] widgets = getAllWidgets();
				if (widgets.length > 0) {
					(new AlertDialog.Builder(this))
					.setItems(widgets, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// no account, dialog to select one
							Cursor c = StatusDialog.this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, ACCOUNTS_QUERY}, Accounts.WIDGET + "=?", new String[]{Integer.toString(mAppWidgetIds[arg1])}, null);
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
								AlertDialog.Builder accountsDialog = new AlertDialog.Builder(StatusDialog.this);
								accountsDialog.setTitle(R.string.accounts)
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
								.show();
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
					})
					.show();
				} else {
					(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
					dialog.cancel();
				}
			}
			break;
		case SETTINGS:
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				startActivity(new Intent(this, ManageAccounts.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
				dialog.cancel();
			} else {
				// no widget sent in, dialog to select one
				String[] widgets = getAllWidgets();
				if (widgets.length > 0) {
					(new AlertDialog.Builder(this))
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
					.show();
				} else {
					(Toast.makeText(this, getString(R.string.error_status), Toast.LENGTH_LONG)).show();
					dialog.cancel();
				}
			}
			break;
		case REFRESH:
			if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId}));
				dialog.cancel();
			} else {
				// no widget sent in, dialog to select one
				String[] widgets = getAllWidgets();
				if (widgets.length > 0) {
					(new AlertDialog.Builder(this))
					.setItems(widgets, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							startService(new Intent(StatusDialog.this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetIds[arg1]}));
							arg0.cancel();
						}					
					})
					.setPositiveButton(R.string.refreshall, new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int which) {
							// refresh all
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
					.show();
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
							return sonetOAuth.httpResponse(new HttpGet(String.format(TWITTER_USER, TWITTER_BASE_URL, mEsid)));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									JSONArray users = new JSONArray(response);
									if (users.length() > 0) {
										startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(String.format(TWITTER_PROFILE, users.getJSONObject(0).getString("screen_name")))));
									}
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(getString(R.string.twitter));
								}
							} else {
								onErrorExit(getString(R.string.twitter));
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
					asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
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
							return Sonet.httpResponse(new HttpGet(String.format(FACEBOOK_USER, FACEBOOK_BASE_URL, mEsid, TOKEN, arg0[0])));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse((new JSONObject(response)).getString("link"))));
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(getString(R.string.facebook));
								}
							} else {
								onErrorExit(getString(R.string.facebook));
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
					asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)));
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
							return sonetOAuth.httpResponse(new HttpGet(String.format(MYSPACE_USER, MYSPACE_BASE_URL, mEsid)));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse((new JSONObject(response)).getJSONObject("person").getString("profileUrl"))));
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(getString(R.string.myspace));
								}
							} else {
								onErrorExit(getString(R.string.myspace));
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
					asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
				}
				account.close();
				break;
			case BUZZ:
				account = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.TOKEN, Accounts.SECRET}, Accounts._ID + "=?", new String[]{Long.toString(mAccount)}, null);
				if (account.moveToFirst()) {
					final ProgressDialog loadingDialog = new ProgressDialog(this);
					asyncTask = new AsyncTask<String, Void, String>() {
						@Override
						protected String doInBackground(String... arg0) {
							SonetOAuth sonetOAuth = new SonetOAuth(BUZZ_KEY, BUZZ_SECRET, arg0[0], arg0[1]);
							return sonetOAuth.httpResponse(new HttpGet(String.format(BUZZ_URL_USER, BUZZ_BASE_URL, mEsid, BUZZ_API_KEY)));
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse((new JSONObject(response)).getJSONObject("data").getString("profileUrl"))));
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(getString(R.string.buzz));
								}
							} else {
								onErrorExit(getString(R.string.buzz));
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
					asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
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
							return sonetOAuth.httpResponse(httpGet);
						}

						@Override
						protected void onPostExecute(String response) {
							if (loadingDialog.isShowing()) loadingDialog.dismiss();
							if (response != null) {
								try {
									startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse((new JSONObject(response)).getJSONObject("siteStandardProfileRequest").getString("url").replaceAll("\\\\", ""))));
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
									onErrorExit(getString(R.string.linkedin));
								}
							} else {
								onErrorExit(getString(R.string.linkedin));
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
					asyncTask.execute(account.getString(account.getColumnIndex(Accounts.TOKEN)), account.getString(account.getColumnIndex(Accounts.SECRET)));
				}
				account.close();
				break;
			}
			break;
		default:
			// open link
			startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(items[which])));
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
		this.getContentResolver().delete(Widgets.CONTENT_URI,
				Widgets.WIDGET + "=?", new String[] { "" });
		this.getContentResolver().delete(Accounts.CONTENT_URI,
				Accounts.WIDGET + "=?", new String[] { "" });
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
				this.getContentResolver().delete(Accounts.CONTENT_URI, Accounts.WIDGET + "=?", new String[] { Integer.toString(appWidgetId) });
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

	@Override
	public void onCancel(DialogInterface dialog) {
		finish();
	}	

}
