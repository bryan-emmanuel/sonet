package com.piusvelte.sonet.network.oauth10;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

import com.squareup.okhttp.Request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by bemmanuel on 6/24/15.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TextUtils.class, Base64.class })
@PowerMockIgnore({ "javax.crypto.*" })
public class OAuth10AccessTokenRequestBuilderTest {

    private static final String CONSUMER_KEY = "ConsumerKey";
    private static final String CONSUMER_SECRET = "ConsumerSecret";
    private static final String TOKEN = "Token";
    private static final String TOKEN_SECRET = "TokenSecret";
    private static final String VERIFIER = "Verifier";
    private static final String ACCESS_URL = "https://test/access_token";

    private Request mRequest;

    @Before
    public void setUp() throws Exception {
        // mock TextUtils
        mockStatic(TextUtils.class);

        when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                return a == null || a.length() == 0;
            }
        });

        when(TextUtils.join(any(CharSequence.class), any(Iterable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                CharSequence delimiter = (CharSequence) invocation.getArguments()[0];
                Iterable tokens = (Iterable) invocation.getArguments()[1];

                StringBuilder sb = new StringBuilder();
                boolean firstTime = true;
                for (Object token : tokens) {
                    if (firstTime) {
                        firstTime = false;
                    } else {
                        sb.append(delimiter);
                    }
                    sb.append(token);
                }
                return sb.toString();
            }
        });

        // mock Base64
        mockStatic(Base64.class);

        when(Base64.encodeToString(any(byte[].class), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                byte[] input = (byte[]) invocation.getArguments()[0];

                try {
                    return new String(input, "US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    // US-ASCII is guaranteed to be available.
                    throw new AssertionError(e);
                }
            }
        });

        mRequest = new OAuth10AccessTokenRequestBuilder(CONSUMER_KEY,
                CONSUMER_SECRET,
                TOKEN,
                TOKEN_SECRET,
                VERIFIER)
                .url(ACCESS_URL)
                .build();
    }

    @Test
    public void RequestOAuthHeaderTest() {
        String authorization = mRequest.header("Authorization");

        assertThat(authorization)
                .isNotNull()
                .isNotEmpty();

        String timestamp = getValue(authorization, "oauth_timestamp");

        assertThat(timestamp)
                .isNotNull()
                .isNotEmpty();

        String nonce = getValue(authorization, "oauth_nonce");

        assertThat(nonce)
                .isNotNull()
                .isNotEmpty();

        String signature = getValue(authorization, "oauth_signature");

        assertThat(signature)
                .isNotNull()
                .isNotEmpty();

        assertThat(authorization)
                .startsWith("OAuth ")
                .contains("oauth_consumer_key=\"ConsumerKey\", ")
                .contains("oauth_nonce=" + nonce + ", ")
                .contains("oauth_signature_method=\"HMAC-SHA1\", ")
                .contains("oauth_timestamp=" + timestamp + ", ")
                .contains("oauth_token=\"Token\", ")
                .contains("oauth_verifier=\"Verifier\", ")
                .contains("oauth_version=\"1.0\", ")
                .endsWith("oauth_signature=" + signature);
    }

    @Nullable
    private static String getValue(@Nullable String from, @NonNull String key) {
        if (!TextUtils.isEmpty(from)) {
            int keyIndex = from.indexOf(key + "=");

            if (keyIndex >= 0) {
                keyIndex += key.length() + 1;
                int commaIndex = from.indexOf(",", keyIndex);

                if (commaIndex >= keyIndex) {
                    return from.substring(keyIndex, commaIndex);
                }

                return from.substring(keyIndex);
            }
        }

        return null;
    }
}