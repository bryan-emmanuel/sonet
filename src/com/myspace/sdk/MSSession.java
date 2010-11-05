package com.myspace.sdk;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.myspace.datamapper.MSDataMapper;

/**
 * A MSSession represents user's context for MySpace application.
 * 
 * To create a new session, an user must use application key, application secret and application callback url.
 * Once successful login in performed, returned access token and token secret are saved into the current session. 
 * Access token and token secret are then cached into shared Preferences for future access. Everytime the user launches the application
 * saved token and token secret are used to make calls into MySpace SDK. This avoids asking the user to login on each instance.
 * Simply call resume method to retrieve already stored session.
 * 
 * @author Nilesh Rane
 */
public class MSSession {

	private static final String PREFS_NAME = "MySpacePrefs";
	private static final String PREF_TOKEN = "MSToken";
	private static final String PREF_TOKEN_SECRET = "MSTokenSecret";
	private static final String TAG = "MSSession";
	private static final String DATA_MAPPER_FILE_NAME = "MSDataMapper.txt";
	
	private static MSSession mSession;
	private Context mContext;
	private List<IMSSessionCallback> mCallbacks;
	private String mApiKey;
	private String mApiSecret;
	private String mApiCallBackUrl;
	private String mToken;
	private String mTokenSecret;
	private String mRequestedPermissions;
	
	private boolean mExternalDataMapper;
	private JSONObject mDataMappers;
	
	/**
	 * Private constructor.
	 * 
	 */
	private MSSession(String key, String secret, String callbackUrl) {
		mCallbacks = new ArrayList<IMSSessionCallback>();
		mApiKey = key;
		mApiSecret = secret;
		mApiCallBackUrl = callbackUrl;
	}

	/**
	 * Initializes a MSSession and stores it globally. This is Private method call.
	 * 
	 * @param key
	 *            myspace application key
	 * @param secret
	 *            myspace application secret
	 * @param callbackurl
	 *            myspace application external callback url     
	 *                  
	 * @return myspace shared session object           
	 */
	private static MSSession init(String key, String secret, String callbackUrl) {
		if (mSession == null) {
    		synchronized(MSSession.class) {
    			if (mSession == null) {
    				mSession = new MSSession(key, secret, callbackUrl);
    			}
    		}
    	}	
		return mSession;
	}

	/**
	 * Called by external myspace apps to generate or return myspace session object.
	 * 
	 * @param key
	 *            myspace application key
	 * @param secret
	 *            myspace application secret
	 * @param callbackurl
	 *            myspace application external callback url   
	 * @param callback
	 *            myspace session callback handler    
	 *                  
	 * @return myspace shared session object           
	 */
	public static MSSession getSession(String key, String secret, String callbackUrl, IMSSessionCallback callback) {
		MSSession session = init(key, secret, callbackUrl);
		session.mCallbacks.add(callback);
		return session;
	}

	public static MSSession getSession() {
		return mSession;
	}

	/* Basic Session Handling 
    -------------------------------------------------------------------------------------------------------------------- */

	/**
	 * Begins new session.
	 * 
	 * <p>
	 * New access token and token secret are saved into the session and Preferences.
	 * User is indicated by invoking Login session callback.
	 * </p>
	 * 
	 * @param context
	 *            current application context/activity
	 * @param token
	 *            access token
	 * @param tokenSecret
	 *            access token secret  
	 *                  
	 */
	public void begin(Context context, String token, String tokenSecret) {
		mToken = token;
		mTokenSecret = tokenSecret;
		saveTokenSecretInPrefs(context);
		mDataMappers = initDataMappers(context.getClass(), DATA_MAPPER_FILE_NAME);
		doLoginCallback();
	}

	/**
	 * Resumes current session with saved access token and secret.
	 * 
	 * <p>If access token is found in Preferences then the access token and secret are saved into 
	 * the session. The user is indicated by returning true.</p>
	 * <p>If access token is not found in Preferences then the user is indicated by returning false; 
	 * </p>
	 * 
	 * @param context
	 *            current application context/activity
	 *                  
	 */
	public boolean resume(Context context) {
		if(existsTokenSecretInPrefs(context)) {
			initTokenSecret(context);
			if(mDataMappers == null) {
				mDataMappers = initDataMappers(context.getClass(), DATA_MAPPER_FILE_NAME);
			}
			return true;
		}
		return false;
	}

	/**
	 * Deletes current session information.
	 * 
	 * <p>
	 * Access token and token secret are deleted from the session and shared Preferences.
	 * User is indicated by invoking Logout session callback.
	 * </p>
	 * 
	 * @param context
	 *            current application context/activity
	 *                  
	 */
	public void end(Context context) {
		mToken = null;
		mTokenSecret = null;
		deleteTokenSecretInPrefs(context);
		mDataMappers = null;
		doLogoutCallback();
	}

	/* Preferences
    -------------------------------------------------------------------------------------------------------------------- */

	private Boolean existsTokenSecretInPrefs(Context context) {
		SharedPreferences defaults = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		return defaults.contains(PREF_TOKEN);
	}

	private void initTokenSecret(Context context) {
		SharedPreferences defaults = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		mToken = defaults.getString(PREF_TOKEN, null);
		mTokenSecret = defaults.getString(PREF_TOKEN_SECRET, null);
	}

	/**
	 * Saves access token and token secret in shared Preferences
	 */
	private void saveTokenSecretInPrefs(Context context) {
		SharedPreferences defaults = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		Editor editor = defaults.edit();

		if (mToken != null) {
			editor.putString(PREF_TOKEN, mToken);
		} else {
			editor.remove(PREF_TOKEN);
		}

		if (mTokenSecret != null) {
			editor.putString(PREF_TOKEN_SECRET, mTokenSecret);
		} else {
			editor.remove(PREF_TOKEN_SECRET);
		}

		editor.commit();    	
	}

	/**
	 * Removes access token and token secret from shared Preferences
	 */
	private void deleteTokenSecretInPrefs(Context context) {
		Editor sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
		sharedPreferences.remove(PREF_TOKEN);
		sharedPreferences.remove(PREF_TOKEN_SECRET);
		sharedPreferences.commit();
	}

	/* Application Context
    -------------------------------------------------------------------------------------------------------------------- */
	
	public void setContext(Context context) {
		this.mContext = context;
	}

	public Context getContext() {
		return this.mContext;
	}

	/* Application Key Secret and Access Token Secrets.
    -------------------------------------------------------------------------------------------------------------------- */

	public String getApiKey() {
		return mApiKey;
	}

	public String getApiSecret() {
		return mApiSecret;
	}

	public String getApiCallBackUrl() {
		return mApiCallBackUrl;
	}

	public String getToken() {
		return mToken;
	}

	public String getTokenSecret() {
		return mTokenSecret;
	}
	
	public JSONObject getDataMappers() {
		return mDataMappers;
	}
	
	public void setToken(String token) {
		mToken = token;
	}

	public void setTokenSecret(String tokenSecret) {
		mTokenSecret = tokenSecret;
	}
	
	public boolean isLoggedIn() {
		return (mToken!= null && mTokenSecret != null);
	}
	
	/* Set Permissions
    -------------------------------------------------------------------------------------------------------------------- */
	public void requestPermissions(String permission) {
		this.mRequestedPermissions = permission;
	}
	
	public String getRequestedPermissions() {
		return this.mRequestedPermissions;
	}
	
	/* Session CallBacks
    -------------------------------------------------------------------------------------------------------------------- */

	private void doLoginCallback() {
		if(mCallbacks != null && mCallbacks.size() > 0) {
			for (IMSSessionCallback callback : mCallbacks) {
				callback.sessionDidLogin(this);
			}
		}
	}

	private void doLogoutCallback() {
		if(mCallbacks != null && mCallbacks.size() > 0) {
			for (IMSSessionCallback callback : mCallbacks) {
				callback.sessionDidLogout(this);
			}
		}
	}

	/* session login/logout contracts
    -------------------------------------------------------------------------------------------------------------------- */
	public static interface IMSSessionCallback {
		void sessionDidLogin(MSSession session);
		void sessionDidLogout(MSSession session);
	}
	
	
	/* datamappers
    -------------------------------------------------------------------------------------------------------------------- */
	public void setExternalDataMapper(boolean externalDataMapper) {
		mExternalDataMapper = externalDataMapper;
	}
	
	public JSONObject initDataMappers(Class<?> classLoader, String dataMapperFileName) {
		if(!mExternalDataMapper || classLoader == null) {
			classLoader = MSDataMapper.class; 
		}
		InputStream is = classLoader.getResourceAsStream(dataMapperFileName); 
		String jsonDataMapperString = MSRequest.getResponseFromStream(is);
		try {
			return new JSONObject(jsonDataMapperString);
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
		}
		return null;
	}
}