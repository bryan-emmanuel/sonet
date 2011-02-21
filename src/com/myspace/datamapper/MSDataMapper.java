package com.myspace.datamapper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class MSDataMapper {

	private static final String TAG = "MSDataMapper";
	
	private String mServiceURL;
	private String mObjectArrayKeyPath;
	private JSONObject mObjectAttributes;
	private JSONObject mObjectFormatters;
	
	public MSDataMapper(JSONObject objectMapper) throws JSONException {
		if(objectMapper.has("serviceURL")) {
			mServiceURL = objectMapper.getString("serviceURL");
		}
				
		if(objectMapper.has("objectArrayKeyPath")) {
			mObjectArrayKeyPath = objectMapper.getString("objectArrayKeyPath");
		}
		
		if(objectMapper.has("objectAttributes")) {
			mObjectAttributes = objectMapper.getJSONObject("objectAttributes");
		}
		
		if(objectMapper.has("objectFormatters")) {
			mObjectFormatters = objectMapper.getJSONObject("objectFormatters");
		}
	}
	
	public String getServiceURL() {
		return mServiceURL;
	}
	
	public String getObjectArrayKeyPath() {
		return mObjectArrayKeyPath;
	}
	
	public Object map(Object result) throws JSONException {
		if(mObjectAttributes == null) {
			return result;
		}
		
		Object object = null;
		
		if(result instanceof Map<?,?>) {
			object = map((Map<?,?>)result);
		}
		
		if(result instanceof List<?>) {
			object = map((List<?>)result);
		}
		
		return object;
	}
	
	/* OBJECT ATTRIBUTES MAPPER
    ------------------------------------------------------------------------------------------------------------------ */

	public Map<String,Object> mapObject(Object result) throws JSONException {
		Map<String,Object> map = new HashMap<String,Object>(); 
				
		JSONArray attributes = mObjectAttributes.names();
		for (int i = 0; i < attributes.length(); i++) {
			String key = (String) attributes.get(i);
			String keyPath = mObjectAttributes.getString(key);
			
			Object value = formatValue(valueForKeyPath(keyPath, result), withFormatter(key));
			
			if(value != null) {
				map.put(key, value);
			}
		}
		return map;
	}
	
	public Object map(Map<?,?> map) throws JSONException {
		if(mObjectArrayKeyPath == null) {
			return mapObject(map);
		} 
		
		if(map.containsKey(mObjectArrayKeyPath)) {
			List<?> entries = (List<?>)map.get(mObjectArrayKeyPath);
			return map(entries);
		} else {
			Log.e(TAG, "Result Map does not contain objectArrayKeyPath=" + mObjectArrayKeyPath);
		}
		return null;
	}
	
	public List<Object> map(List<?> array) throws JSONException {
		if(array != null && array.size() > 0) {
			List<Object> objects = new ArrayList<Object>(array.size());
			for (int i = 0; i < array.size(); i++) {
				objects.add(mapObject(array.get(i)));
			}
			return objects;
		}
		return null;
	}
	
	public static Object valueForKeyPath(String keyPath, Object object) {
		if(object instanceof Map<?,?>) {
			return valueForKeyPath(keyPath, (Map<?,?>)object);
		}

		if(object instanceof List<?>) {
			return valueForKeyPath(keyPath, (List<?>)object);				
		}
		
		return object;
	}
	
	private static Object valueForKeyPath(String keyPath, Map<?,?> map) {
		int endIndex = keyPath.indexOf('.');
		if(endIndex > -1) {
			String key = keyPath.substring(0, endIndex);
			if(map.containsKey(key)) {
				return valueForKeyPath(keyPath.substring(endIndex+1), map.get(key));
			}
		} else if(map.containsKey(keyPath)) {
			return map.get(keyPath);
		}
		return null;
	}
	
	/**
	 * Finds value for given key path which contains array
	 * 
	 * <p>
	 * Example - "commentUserNames": "recentComments.author.displayName" 
	 * </p>
	 * 
	 * <p>
	 * Output - commentUserNames = [DisplayName1, DisplayName2, ....]
	 * </p>
	 * 
	 * <p>
	 * recentComments = Array<br>
	 * author = Map<br>
	 * displayName = String
	 * </p>
	 * 
	 * @param keyPath
	 * 		  recentComments.author.displayname 
	 * @param array
	 * 		  recentComments
	 * @return
	 * 		  array of displayname
	 * 
	 */
	private static List<Object> valueForKeyPath(String keyPath, List<?> array) {
		List<Object> objects = new ArrayList<Object>();
		for (int i = 0; i < array.size(); i++) {
			Object value = valueForKeyPath(keyPath, array.get(i)); 
			if(value != null) {
				objects.add(value);
			}
		}
		return objects;
	}
	
	/* OBJECT FORMATTER
    ------------------------------------------------------------------------------------------------------------------ */
	
	private Object formatValue(Object value, IFormatter formatter) {
		if(formatter != null) {
			if(value instanceof List<?>) {
				List<Object> formattedValues = new ArrayList<Object>();
				List<?> items = (List<?>)value;
				for(int i=0; i < items.size(); i++) {
					formattedValues.add(formatValue(items.get(i), formatter));
				}
				return formattedValues;
			} else {
				return formatter.format(value);
			}
		}
		return value;
	}
	
	private IFormatter withFormatter(String key) throws JSONException {
		if(mObjectFormatters != null && mObjectFormatters.has(key)) {
			String formatterType = mObjectFormatters.getString(key);
			switch(Enum.valueOf(Formatter.class, formatterType.toUpperCase())) {
				case DATE : return new DateFormatter();
			}
		}
		return null;
	}
	
	private enum Formatter {
		DATE,
		HTML
	}
	
	private interface IFormatter {
		Object format(Object value);
	}
	
	private class DateFormatter implements IFormatter {
		public DateFormatter() {
		}

		@Override
		public Object format(Object value) {
			Date date = null;
			if(value != null) {
				try {
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					date = df.parse((String) value);
				} catch (ParseException e) {
					Log.w(TAG, e.toString());
				}
			}
			return date;
		}
	}
}