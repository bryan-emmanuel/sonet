package com.myspace.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

import com.myspace.datamapper.MSDataMapper;
import com.myspace.json.MSJSONSerializer;

/**
 * A MSRequest allows users to make myspace web service requests to retrieve data from myspace or 
 * save into myspace.
 * 
 * <p>
 * You can create a new Request using MSRequest.Builder class. You will need to provide request url, 
 * query/post params, content-type and request callback. You can then call GET/PUT/POST methods to 
 * access myspace resources using web service endpoints. Each request is signed using signpost 
 * mechanism and is asynchronous. 
 * </p>
 * 
 * <p>
 * Response or error is posted back to caller using request callback. Response is in form of native 
 * Java objects e.g. List or Map
 * </p>
 * 
 * @author Nilesh Rane
 */
public class MSRequest {

	private static final String TAG = "MSRequest";

	private MSSession mSession;
	private MSRequestCallback mCallback;
	private String mUrl;
	private Map<String,String> mHeaders;
	private AbstractHttpEntity mPostData;
	private Map<String,Object> mUserData;
	private MSDataMapper mDataMapper;

	public MSRequest(MSSession session) {
		this(session, null);
	}

	public MSRequest(MSRequestCallback callback) {
		this(MSSession.getSession(), callback);
	}

	public MSRequest(MSSession session, MSRequestCallback mRequestCallback) {
		this.mSession = session;
		this.mCallback = mRequestCallback;
	}

	/* getters
    -------------------------------------------------------------------------------------------------------------------- */
	public Map<String,Object> getUserData() {
		return this.mUserData;
	}

	/* get/post/put/delete
    -------------------------------------------------------------------------------------------------------------------- */
	public void get() {
		this.execute(new HttpGet(this.mUrl));
	}

	public void post() {
		this.execute(new HttpPost(this.mUrl));
	}

	public void put() {
		this.execute(new HttpPut(this.mUrl));
	}

	public void delete() {
		this.execute(new HttpDelete(this.mUrl));
	}

	/* execute
    -------------------------------------------------------------------------------------------------------------------- */
	public void execute(HttpRequestBase httpRequest) {		
		// set headers
		if(this.mHeaders != null) {
			for (String headerKey : this.mHeaders.keySet()) {
				httpRequest.setHeader(headerKey, this.mHeaders.get(headerKey));
			}
		}

		// set post body
		if(this.mPostData != null) {
			if(httpRequest instanceof HttpPost) {
				((HttpPost)httpRequest).setEntity(this.mPostData);
			}
			if(httpRequest instanceof HttpPut) {
				((HttpPut)httpRequest).setEntity(this.mPostData);
			}
		}

		try {
			// sign
			this.sign(httpRequest);
		} catch(Exception e) {
			Log.e(TAG, e.toString());
		}

		if(Thread.currentThread().getName().equals("main")) {
			// send asynchronous
			this.sendAsync(httpRequest);
		} else {
			// send synchronous
			this.sendSync(httpRequest);
		}
	}

	/* sign
    -------------------------------------------------------------------------------------------------------------------- */
	public void sign(HttpRequestBase httpRequest) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		MSOAuth oauth = MSOAuth.init(mSession);
		oauth.sign(httpRequest);
	}

	/* send async/sync
    -------------------------------------------------------------------------------------------------------------------- */
	public void sendAsync(HttpRequestBase httpRequest) {
		new APIAsyncTask().execute(httpRequest);
	}

	public void sendSync(HttpRequestBase httpRequest) {
		MSResponse msResponse = process(httpRequest);
		onProcessComplete(msResponse);
	}


	private class APIAsyncTask extends AsyncTask<HttpRequestBase, Void, MSResponse> {

		@Override
		public MSResponse doInBackground(HttpRequestBase... params) {
			return process(params[0]);
		}

		@Override
		public void onPostExecute(MSResponse msResponse) {
			onProcessComplete(msResponse);
		}
	}

	/* perform on background thread
    -------------------------------------------------------------------------------------------------------------------- */
	private MSResponse process(HttpRequestBase httpRequest) {
		MSResponse msResponse = new MSResponse();
		try {
			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			StatusLine statusLine = httpResponse.getStatusLine();
			HttpEntity entity = httpResponse.getEntity();
			String response = "";
			if (entity != null) {
				response = getResponseFromStream(entity.getContent());
			}

			switch(statusLine.getStatusCode()) {
				case 200:
				case 201:   
					Object result = MSResponse.parse(response, this.mDataMapper);
					msResponse.setSuccess(true);
					msResponse.setResult(result);
					break;
				default:
					MSRequestException e;
					String headerName = "x-opensocial-error";
					if(httpResponse.containsHeader(headerName)) {
						Header header = httpResponse.getFirstHeader(headerName);
						String headerValue = header.getValue();
						String errorMessage = statusLine.getReasonPhrase() + " : " + headerName + " : " + headerValue;
						e = new MSRequestException(statusLine.getStatusCode(),  errorMessage);
					} else {
						e = MSResponse.getError(response);
					}
					msResponse.setSuccess(false);
					msResponse.setError(e);
					break;
			}
		} catch (Exception e) {
			msResponse.setSuccess(false);
			msResponse.setError(e);
		}
		return msResponse;
	} 

	public static String getResponseFromStream(InputStream is) {
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
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	/* perform on main thread
    -------------------------------------------------------------------------------------------------------------------- */
	private void onProcessComplete(MSResponse msResponse) {
		if(msResponse != null) {
			if(msResponse.isSuccess()) {
				doLoadCallback(msResponse.getResult());
			}
			else {
				doFailCallback(msResponse.getError());
			}
		} else {
			doFailCallback(new MSRequestException("MSResponse is NULL"));
		}
	}

	/* success/fail callbacks 
    -------------------------------------------------------------------------------------------------------------------- */
	private void doLoadCallback(Object result) {
		if (mCallback != null) {
			mCallback.requestDidLoad(this, result);
		}
	}

	private void doFailCallback(Throwable error) {
		if (mCallback != null) {
			mCallback.requestDidFail(this, error);
		}
	}

	/* callback contract
    -------------------------------------------------------------------------------------------------------------------- */
	public static abstract class MSRequestCallback {
		public abstract void requestDidFail(MSRequest request, Throwable error);
		public abstract void requestDidLoad(MSRequest request, Object result);
	}

	public static class Builder {

		private static final String TAG = "MSRequest.Builder";

		private MSSession mSession;
		private MSRequestCallback mRequestCallback;

		public Builder(MSSession session, MSRequestCallback callback) {
			mSession = session;
			mRequestCallback = callback;
		}

		public static Builder getBuilder() {
			return new Builder(MSSession.getSession(), null);
		}

		public static Builder getBuilder(MSRequestCallback callback) {
			return new Builder(MSSession.getSession(), callback);
		}

		public static Builder getBuilder(MSSession session, MSRequestCallback callback) {
			return new Builder(session, callback);
		}

		public void setRequestDelegate(MSRequestCallback callback) {
			mRequestCallback = callback;
		}

		public void get(String type, Object[] urlArgsData, Map<String,String> queryStringData, Map<String,Object> userData) {
			MSRequest mRequest = this.buildRequest(type, null, urlArgsData, queryStringData, null, null, userData);
			if(mRequest != null) {
				mRequest.get();
			}
		}

		public void post(String url, Map<String,Object> postData, String contentType, Map<String,Object> userData) {
			MSRequest mRequest = this.buildRequest(null, url, null, null, postData, contentType, userData);
			if(mRequest != null) {
				mRequest.post();
			}	
		}

		public void post(String type, Object[] urlArgsData, Map<String,String> queryStringData, Object postData, String contentType, Map<String,Object> userData) {
			MSRequest mRequest = this.buildRequest(type, null, urlArgsData, queryStringData, postData, contentType, userData);
			if(mRequest != null) {
				mRequest.post();
			}			
		}

		public void put(String type, Object[] urlArgsData, Map<String,String> queryStringData, Map<String,Object> postData, String contentType, Map<String,Object> userData) {
			MSRequest mRequest = this.buildRequest(type, null, urlArgsData, queryStringData, postData, contentType, userData);
			if(mRequest != null) {
				mRequest.put();
			}			
		}

		public void delete(String type, Object[] urlArgsData, Map<String,Object> userData) {
			MSRequest mRequest = this.buildRequest(type, null, urlArgsData, null, null, null, userData);
			if(mRequest != null) {
				mRequest.delete();
			}
		}

		@SuppressWarnings("unchecked")
		private MSRequest buildRequest(
				String type,
				String url, 
				Object[] urlArgsData,
				Map<String,String> queryStringData,
				Object postData,
				String contentType,
				Map<String,Object> userData) {

			MSRequest mRequest = new MSRequest(mSession, mRequestCallback);

			try {
				// set user params
				mRequest.mUserData = userData;

				// build request url
				MSDataMapper dataMapper = null;
				if(url == null) {
					// get service url
					dataMapper = getMSDataMapper(type);
					url = dataMapper.getServiceURL();

					// set urlParams
					if(urlArgsData != null && urlArgsData.length > 0) {
						url = String.format(url, urlArgsData);
					}

					// set query params
					if(queryStringData != null) {
						url = this.buildUrl(url, queryStringData);
					}
				}

				// set request url
				mRequest.mUrl = url;

				// set data mapper
				mRequest.mDataMapper = dataMapper;

				// set request headers
				if(userData != null && userData.containsKey("requestHeaders")) {
					mRequest.mHeaders = (Map<String,String>)userData.get("requestHeaders");
				}

				// set post body 
				if(postData != null && contentType != null) {
					if(contentType.equals(MSSDK.CONTENT_TYPE_APP_JSON)) {
						mRequest.mPostData = new StringEntity(buildJSONString(postData));
					} else if(contentType.equals(MSSDK.CONTENT_TYPE_APP_FORM_URLENCODED)) {
						mRequest.mPostData = new UrlEncodedFormEntity(buildNameValuePairs((Map<String,String>)postData),HTTP.UTF_8);
					} else {
						mRequest.mPostData = new ByteArrayEntity((byte[])postData);
					}
				}

				// set post body content type
				if(mRequest.mPostData != null) {
					mRequest.mPostData.setContentType(contentType);
				}

				return mRequest; 
			} catch (Exception e) {
				mRequest.doFailCallback(e);
			}
			return null;
		}

		private String buildUrl(String url, Map<String,String> queryStringData) throws UnsupportedEncodingException {
			String params = this.buildParams(queryStringData);
			if(params.length() > 0) {
				String seperator = (url.contains("?")) ? "&" : "?";
				return url + seperator + params;
			}
			return url;
		}

		private String buildParams(Map<String,String> queryStringData) throws UnsupportedEncodingException {         
			List<String> argList = new ArrayList<String>();         
			for (String key : queryStringData.keySet()) {
				String arg = key + "=" + URLEncoder.encode(queryStringData.get(key), "UTF-8");             
				argList.add(arg);         
			}
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < argList.size(); i++) {
				s.append(argList.get(i));             
				if (i != argList.size()-1) {                 
					s.append("&");             
				}         
			}
			return s.toString();  
		}  

		private String buildJSONString(Object postData) throws IOException, JSONException, ParseException {
			return MSJSONSerializer.getJSONString(postData); 
		}    

		private List<NameValuePair> buildNameValuePairs(Map<String,String> postData) {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			for(String key : postData.keySet()) {
				nameValuePairs.add(new BasicNameValuePair(key, postData.get(key)));     
			}
			return nameValuePairs;
		};

		private MSDataMapper getMSDataMapper(String type) {
			try {
				JSONObject dataWrappers = mSession.getDataMappers();
				if(dataWrappers.has(type)) {
					JSONObject dataMapper = dataWrappers.getJSONObject(type);
					return new MSDataMapper(dataMapper);
				} else {
					Log.e(TAG, String.format("Data Mapper Key '%s' does not exist", type));
				}
			} catch(Exception ex) {
				Log.e(TAG, ex.toString());
			}
			return null;
		}
	}
}