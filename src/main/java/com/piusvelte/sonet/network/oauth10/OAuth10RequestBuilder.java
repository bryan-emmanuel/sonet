package com.piusvelte.sonet.network.oauth10;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.piusvelte.sonet.BuildConfig;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okio.Buffer;

/**
 * Created by bemmanuel on 6/11/15.
 */
public abstract class OAuth10RequestBuilder extends Request.Builder {

    private static final String TAG = OAuth10RequestBuilder.class.getSimpleName();

    @NonNull
    private String mConsumerKey;
    @NonNull
    protected String mConsumerSecret;
    private HttpUrl mUrl;
    private String mMethod = "GET";
    @Nullable
    private String mFormBody;

    public enum OAuthParameter {
        realm,
        oauth_token,
        oauth_callback,
        oauth_verifier,
        oauth_consumer_key,
        oauth_version,
        oauth_signature_method,
        oauth_timestamp,
        oauth_nonce;

        @NonNull
        String getNameValuePair(@NonNull String value) {
            return name() + "=" + value;
        }
    }

    public OAuth10RequestBuilder(@NonNull String consumerKey, @NonNull String consumerSecret) {
        mConsumerKey = consumerKey;
        mConsumerSecret = consumerSecret;
    }

    @Override
    public Request.Builder url(HttpUrl url) {
        mUrl = url;
        return super.url(url);
    }

    @Override
    public Request.Builder method(String method, RequestBody body) {
        mMethod = method;

        if (body != null && body.contentType().toString().startsWith("application/x-www-form-urlencoded")) {
            Buffer sink = new Buffer();
            try {
                body.writeTo(sink);
                mFormBody = sink.readUtf8();
            } catch (IOException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "error getting form body", e);
                }
            }
        }

        return super.method(method, body);
    }

    @Override
    public Request build() {
        try {
            signRequest();
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error signing request", e);
            }
        }

        return super.build();
    }

    private Set<String> getOAuthParameters() {
        Set<String> oAuthParameters = new HashSet<>();
        oAuthParameters.add(OAuthParameter.oauth_signature_method.getNameValuePair("HMAC-SHA1"));
        oAuthParameters.add(OAuthParameter.oauth_consumer_key.getNameValuePair(mConsumerKey));
        oAuthParameters.add(OAuthParameter.oauth_version.getNameValuePair("1.0"));
        oAuthParameters.add(OAuthParameter.oauth_timestamp.getNameValuePair(Long.toString(System.currentTimeMillis() / 1000L)));
        oAuthParameters.add(OAuthParameter.oauth_nonce.getNameValuePair(Long.toString((new Random()).nextLong())));
        onAddOAuthParameters(oAuthParameters);
        return oAuthParameters;
    }

    protected abstract void onAddOAuthParameters(@NonNull Set<String> parameters);

    @NonNull
    private Set<String> getSigningParameters() {
        SortedSet<String> keyValuePairs = new TreeSet<>();

        Set<String> keys = mUrl.queryParameterNames();

        if (keys != null) {
            for (String key : keys) {
                List<String> values = mUrl.queryParameterValues(key);

                if (values != null) {
                    for (String value : values) {
                        keyValuePairs.add(key + "=" + value);
                    }
                } else {
                    keyValuePairs.add(key + "=");
                }
            }
        }

        if (!TextUtils.isEmpty(mFormBody)) {
            String[] formBodyPairs = mFormBody.split("&");

            for (String formBodyPair : formBodyPairs) {
                if (!TextUtils.isEmpty(formBodyPair)) {
                    String[] keyValue = formBodyPair.split("=");

                    if (keyValue.length > 1) {
                        keyValuePairs.add(keyValue[0] + "=" + keyValue[1]);
                    } else if (keyValue.length == 1) {
                        keyValuePairs.add(keyValue[0] + "=");
                    }
                }
            }
        }

        return keyValuePairs;
    }

    private void signRequest() throws UnsupportedEncodingException {
        Set<String> signingParameters = getSigningParameters();
        Set<String> oAuthParameters = getOAuthParameters();
        signingParameters.addAll(oAuthParameters);

        String signatureHeader = "OAuth ";

        for (String oAuthParameter : oAuthParameters) {
            signatureHeader += oAuthParameter.replace("=", "=\"") + "\", ";
        }

        signatureHeader += "oauth_signature=\"" + URLEncoder.encode(createSignature(signingParameters), "UTF-8") + "\"";
        addHeader("Authorization", signatureHeader);
    }

    @Nullable
    private String createSignature(@NonNull Set<String> parameters) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(createKey());
            String signatureBaseString = getSignatureBaseString(parameters);
            byte[] text = signatureBaseString.getBytes("UTF-8");
            return Base64.encodeToString(mac.doFinal(text), Base64.DEFAULT).trim();
        } catch (UnsupportedEncodingException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "error encoding; mConsumerSecret=" + mConsumerSecret
                        + ", keyValuePairs=" + TextUtils.join("&", parameters), e);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "error creating hash; mConsumerSecret=" + mConsumerSecret
                        + ", keyValuePairs=" + TextUtils.join("&", parameters), e);
            }
        }

        return null;
    }

    protected SecretKeySpec createKey() throws UnsupportedEncodingException {
        String keyData = URLEncoder.encode(mConsumerSecret, "UTF-8") + "&";
        byte[] keyBytes = keyData.getBytes("UTF-8");
        return new SecretKeySpec(keyBytes, "HmacSHA1");
    }

    private String getSignatureBaseString(@NonNull Set<String> parameters)
            throws UnsupportedEncodingException {
        return mMethod
                + "&" + URLEncoder.encode(mUrl.scheme() + "://" + getPort() + mUrl.host() + mUrl.encodedPath(), "UTF-8")
                + "&" + URLEncoder.encode(TextUtils.join("&", parameters), "UTF-8");
    }

    @NonNull
    private String getPort() {
        if (!(mUrl.scheme().equals("http") && mUrl.port() == 80)
                && !(mUrl.scheme().equals("https") && mUrl.port() == 443)) {
            return String.valueOf(mUrl.port());
        }

        return "";
    }
}
