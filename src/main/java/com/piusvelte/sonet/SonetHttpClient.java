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

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;

public class SonetHttpClient {

    private static final int CONNECTION_TIMEOUT = 60 * 1000;
    private static final int SO_TIMEOUT = 5 * 60 * 1000;
    private static final String TAG = "SonetHttpClient";
    private static DefaultHttpClient sHttpClient = null;
    private static OkHttpClient mOkHttpClient;

    private SonetHttpClient(Context context) {
    }

    public static OkHttpClient getOkHttpClientInstance() {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient();
        }

        return mOkHttpClient;
    }

    @Deprecated
    public static DefaultHttpClient getThreadSafeClient(Context context) {
        if (sHttpClient == null) {
            Log.d(TAG, "create http client");
            SocketFactory sf;
            try {
                Class<?> sslSessionCacheClass = Class.forName("android.net.SSLSessionCache");
                Object sslSessionCache = sslSessionCacheClass.getConstructor(Context.class).newInstance(context);
                Method getHttpSocketFactory = Class.forName("android.net.SSLCertificateSocketFactory")
                        .getMethod("getHttpSocketFactory", new Class<?>[] { int.class, sslSessionCacheClass });
                sf = (SocketFactory) getHttpSocketFactory.invoke(null, CONNECTION_TIMEOUT, sslSessionCache);
            } catch (Exception e) {
                Log.e("HttpClientProvider",
                        "Unable to use android.net.SSLCertificateSocketFactory to get a SSL session caching socket factory, falling back to a " +
                                "non-caching socket factory",
                        e);
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

    @Nullable
    public static String httpResponse(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        return getResponse(request);
    }

    public static boolean request(@NonNull Request request) {
        try {
            return getOkHttpClientInstance()
                    .newCall(request)
                    .execute()
                    .isSuccessful();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "request error, request=" + request, e);
            }
        }

        return false;
    }

    public static String getResponse(@NonNull Request request) {
        OkHttpClient client = getOkHttpClientInstance();
        Response response;

        try {
            response = client.newCall(request)
                    .execute();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "request error; url=" + request.urlString(), e);
            }

            response = null;
        }

        if (response != null && response.isSuccessful()) {
            String body;

            if (response.body() != null) {
                try {
                    body = response.body().string();
                    if (BuildConfig.DEBUG) Log.d(TAG, "response= " + body);
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) Log.e(TAG, "error getting response body", e);
                    body = null;
                }
            } else {
                body = null;
            }

            // TODO a null or empty response is handled by clients as failure
            if (TextUtils.isEmpty(body)) {
                return "OK";
            }

            return body;
        } else {
            if (BuildConfig.DEBUG) Log.e(TAG, "response unsuccessful; request=" + request + "; response=" + response.toString());
        }

        return null;
    }
}
