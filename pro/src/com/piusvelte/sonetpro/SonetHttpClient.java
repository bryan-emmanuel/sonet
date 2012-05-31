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

import static com.piusvelte.sonetpro.Sonet.getBlob;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
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

	private static final int CONNECTION_TIMEOUT = 60 * 1000;
	private static final int SO_TIMEOUT = 5 * 60 * 1000;
	private static final String TAG = "SonetHttpClient";
	private static DefaultHttpClient sHttpClient = null;

	private SonetHttpClient(Context context) {
	}

	protected static DefaultHttpClient getThreadSafeClient(Context context) {
		if (sHttpClient == null) {
			Log.d(TAG,"create http client");
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
			sHttpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, registry), params);
		}
		return sHttpClient;
	}

	protected static byte[] httpBlobResponse(HttpClient httpClient, HttpUriRequest httpRequest) {
		if (httpClient != null) {
			HttpResponse httpResponse;
			HttpEntity entity = null;
			try {
				httpResponse = httpClient.execute(httpRequest);
				StatusLine statusLine = httpResponse.getStatusLine();
				entity = httpResponse.getEntity();

				switch(statusLine.getStatusCode()) {
				case 200:
				case 201:
				case 204:
					if (entity != null) {
						return getBlob(new FlushedInputStream(entity.getContent()));
					}
					break;
				}
			} catch (ClientProtocolException e) {
				Log.e(TAG, e.toString());
				try {
					httpRequest.abort();
				} catch (UnsupportedOperationException ignore) {
					Log.e(TAG, ignore.toString());
				}
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				try {
					httpRequest.abort();
				} catch (UnsupportedOperationException ignore) {
					Log.e(TAG, ignore.toString());
				}
			} finally {
				if (entity != null) {
					try {
						entity.consumeContent();
					} catch (IOException e) {
						Log.e(TAG, e.toString());
					}					
				}
			}
		}
		return null;
	}

	protected static String httpResponse(HttpClient httpClient, HttpUriRequest httpRequest) {
		String response = null;
		if (httpClient != null) {
			HttpResponse httpResponse;
			HttpEntity entity = null;
			try {
				httpResponse = httpClient.execute(httpRequest);
				StatusLine statusLine = httpResponse.getStatusLine();
				entity = httpResponse.getEntity();

				switch(statusLine.getStatusCode()) {
				case 200:
				case 201:
				case 204:
					if (entity != null) {
						InputStream is = entity.getContent();
			            ByteArrayOutputStream content = new ByteArrayOutputStream();
			            byte[] sBuffer = new byte[512];
			            int readBytes = 0;
			            while ((readBytes = is.read(sBuffer)) != -1) {
			                content.write(sBuffer, 0, readBytes);
			            }
						response = new String(content.toByteArray());
					} else {
						response = "OK";
					}
					break;
				default:
					Log.e(TAG,httpRequest.getURI().toString());
					Log.e(TAG,""+statusLine.getStatusCode()+" "+statusLine.getReasonPhrase());
					if (entity != null) {
						InputStream is = entity.getContent();
			            ByteArrayOutputStream content = new ByteArrayOutputStream();
			            byte[] sBuffer = new byte[512];
			            int readBytes = 0;
			            while ((readBytes = is.read(sBuffer)) != -1) {
			                content.write(sBuffer, 0, readBytes);
			            }
						Log.e(TAG,"response:"+new String(content.toByteArray()));
					}
					break;
				}
			} catch (ClientProtocolException e) {
				Log.e(TAG, e.toString());
				try {
					httpRequest.abort();
				} catch (UnsupportedOperationException ignore) {
					Log.e(TAG, ignore.toString());
				}
			} catch (IllegalStateException e) {
				Log.e(TAG, e.toString());
				try {
					httpRequest.abort();
				} catch (UnsupportedOperationException ignore) {
					Log.e(TAG, ignore.toString());
				}
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				try {
					httpRequest.abort();
				} catch (UnsupportedOperationException ignore) {
					Log.e(TAG, ignore.toString());
				}
			} finally {
				if (entity != null) {
					try {
						entity.consumeContent();
					} catch (IOException e) {
						Log.e(TAG, e.toString());
					}					
				}
			}
		}
		return response;
	}
	
	protected static class FlushedInputStream extends FilterInputStream {
	    public FlushedInputStream(InputStream inputStream) {
	        super(inputStream);
	    }

	    @Override
	    public long skip(long n) throws IOException {
	        long totalBytesSkipped = 0L;
	        while (totalBytesSkipped < n) {
	            long bytesSkipped = in.skip(n - totalBytesSkipped);
	            if (bytesSkipped == 0L) {
	                  int nextByte = read();
	                  if (nextByte < 0) {
	                      break;  // we reached EOF
	                  } else {
	                      bytesSkipped = 1; // we read one byte
	                  }
	           }
	            totalBytesSkipped += bytesSkipped;
	        }
	        return totalBytesSkipped;
	    }
	}

}
