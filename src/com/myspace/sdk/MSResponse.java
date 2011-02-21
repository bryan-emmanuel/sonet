package com.myspace.sdk;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;

import com.myspace.datamapper.MSDataMapper;
import com.myspace.json.MSJSONDeserializer;

public class MSResponse {
	
	private Boolean mSuccess;
	private Object mResult;
	private Throwable mError;
	
	public MSResponse() {
	}
	
	public MSResponse(Boolean success, Object result, Throwable error) {
		mSuccess = success;
		mResult = result;
		mError = error;
	}
	
	public void setSuccess(Boolean success) {
		mSuccess = success;
	}
	
	public void setResult(Object result) {
		mResult = result;
	}
	
	public void setError(Throwable error) {
		mError = error;
	}
	
	public Boolean isSuccess() {
		return mSuccess;
	}
	
	public Object getResult() {
		return mResult;
	}
	
	public Throwable getError() {
		return mError;
	}
	
	public static Object parse(String jsonString) throws JSONException {
		return MSJSONDeserializer.getJSONObject(jsonString);
	}
	
	@SuppressWarnings("unchecked")
	public static Object parse(String jsonString, MSDataMapper dataMapper) throws JSONException {
		if(jsonString != null && jsonString.length() > 0) {
			Object object = parse(jsonString);
			Map<String, Object> data = new HashMap<String, Object>(); 

			if(dataMapper != null) {
				// map data
				data.put("data", dataMapper.map(object));

				// map other data
				String objectKeyPath = dataMapper.getObjectArrayKeyPath();
				if(objectKeyPath != null) {
					Map<String, Object> otherData = (Map<String, Object>)object;
					otherData.remove(objectKeyPath);
					data.put("otherData", otherData);
				}
			} else {
				data.put("data", object);
			}
			return data;
		}
		return null;
	}
	
	public static MSRequestException getError(String response) throws JSONException {
		Object result = parse(response);
		if (result != null && result instanceof Map<?,?>) {
			Map<?,?> resultMap = (Map<?,?>)result;
			if (resultMap.containsKey("statusCode")) {
				Object statusCodeObject = resultMap.get("statusCode");
				int statusCode = Integer.parseInt(statusCodeObject.toString());
				if(statusCode != 200 && statusCode != 201) {
					String statusCodeDescription = (String)resultMap.get("statusDescription");
					return new MSRequestException(statusCode, statusCodeDescription);
				}
			}
		}
		return null;
	}
}


