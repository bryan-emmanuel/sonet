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

import static com.piusvelte.sonet.Sonet.getBlob;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

public class SonetHttpClient {

	private static SonetHttpClient instance;
	private static DefaultHttpClient httpClient;
	private static final int CONNECTION_TIMEOUT = 60 * 1000;
	private static final int SO_TIMEOUT = 5 * 60 * 1000;
	private static final String TAG = "SonetHttpClient";

	private SonetHttpClient(Context context) {
		SocketFactory sf;
		try {
			Class< ?> sslSessionCacheClass = Class.forName("android.net.SSLSessionCache");
			Object sslSessionCache = sslSessionCacheClass.getConstructor(Context.class).newInstance(context);
			Method getHttpSocketFactory = Class.forName("android.net.SSLCertificateSocketFactory").getMethod("getHttpSocketFactory", new Class< ?>[]{int.class, sslSessionCacheClass});
			sf = (SocketFactory) getHttpSocketFactory.invoke(null, CONNECTION_TIMEOUT, sslSessionCache);
		}catch(Exception e){
			Log.e("HttpClientProvider", "Unable to use android.net.SSLCertificateSocketFactory to get a SSL session caching socket factory, falling back to a non-caching socket factory",e);
			sf = SSLSocketFactory.getSocketFactory();
		}
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		registry.register(new Scheme("https", sf, 443));
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, SO_TIMEOUT);
		String versionName;
		try {
			versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
		StringBuilder userAgent = new StringBuilder();
		userAgent.append(context.getPackageName());
		userAgent.append("/");
		userAgent.append(versionName);
		userAgent.append(" (");
		userAgent.append("Linux; U; Android ");
		userAgent.append(Build.VERSION.RELEASE);
		userAgent.append("; ");
		userAgent.append(Locale.getDefault());
		userAgent.append("; ");
		userAgent.append(Build.PRODUCT);
		userAgent.append(")");
		if (HttpProtocolParams.getUserAgent(params) != null) {
			userAgent.append(" ");
			userAgent.append(HttpProtocolParams.getUserAgent(params));
		}
		HttpProtocolParams.setUserAgent(params, userAgent.toString());
		httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, registry), params);
	}

	protected static synchronized SonetHttpClient getInstance(Context context) {
		if (instance == null) {
			instance = new SonetHttpClient(context);
		}
		return instance;
	}

	protected byte[] httpBlobResponse(HttpUriRequest httpRequest) {
		if (httpClient != null) {
			HttpResponse httpResponse;
			try {
				httpResponse = httpClient.execute(httpRequest);
				StatusLine statusLine = httpResponse.getStatusLine();
				HttpEntity entity = httpResponse.getEntity();

				switch(statusLine.getStatusCode()) {
				case 200:
				case 201:
				case 204:
					if (entity != null) {
						return getBlob(entity.getContent());
					}
					break;
				}
			} catch (ClientProtocolException e) {
				Log.e(TAG, e.toString());
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
		}
		return null;
	}

	protected String httpResponse(HttpUriRequest httpRequest) {
		String response = null;
		if (httpClient != null) {
			HttpResponse httpResponse;
			try {
				httpResponse = httpClient.execute(httpRequest);
				StatusLine statusLine = httpResponse.getStatusLine();
				HttpEntity entity = httpResponse.getEntity();

				switch(statusLine.getStatusCode()) {
				case 200:
				case 201:
				case 204:
					if (entity != null) {
						InputStream is = entity.getContent();
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						StringBuilder sb = new StringBuilder();

						String line = null;
						try {
							while ((line = reader.readLine()) != null) {
								sb.append(line + "\n");
							}
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							try {
								is.close();
							} catch (IOException e) {
								Log.e(TAG, e.toString());
							}
						}
						response = sb.toString();
						reader.close();
					} else {
						response = "OK";
					}
					break;
				default:
					Log.e(TAG,httpRequest.getURI().toString());
					Log.e(TAG,""+statusLine.getStatusCode()+" "+statusLine.getReasonPhrase());
					if (entity != null) {
						InputStream is = entity.getContent();
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						StringBuilder sb = new StringBuilder();

						String line = null;
						try {
							while ((line = reader.readLine()) != null) {
								sb.append(line + "\n");
							}
						} catch (IOException e) {
							Log.e(TAG, e.toString());
						} finally {
							try {
								is.close();
							} catch (IOException e) {
								Log.e(TAG, e.toString());
							}
						}
						Log.e(TAG,"response:"+sb.toString());
						reader.close();
					}
					break;
				}
			} catch (ClientProtocolException e) {
				Log.e(TAG, e.toString());
			} catch (IllegalStateException e) {
				Log.e(TAG, e.toString());
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
		}
		return response;
	}

}
