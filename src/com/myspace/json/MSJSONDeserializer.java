package com.myspace.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MSJSONDeserializer {

	public static Object getJSONObject(String jsonString) throws JSONException {
		Object object = null;
		JSONType jsonType = getJSONType(jsonString);
		switch(jsonType) {
		case OBJECT : 
			object = new JSONObject(jsonString); 
			break;
		case ARRAY :  
			object = new JSONArray(jsonString); 
			break;
		default: 
			object = jsonString; 
			break;
		}
		return deserialize(object);
	}

	private static Object deserialize(Object object) throws JSONException {
		if (object instanceof JSONObject) {
			return deserialize((JSONObject) object);
		}
		else if (object instanceof JSONArray) {
			return deserialize((JSONArray) object);
		}
		return object;
	}

	private static Map<String,Object> deserialize(JSONObject jsonObject) throws JSONException {
		Map<String,Object> map = new HashMap<String,Object>();
		JSONArray jsonArray = jsonObject.names();
		for (int i = 0; i < jsonArray.length(); i++) {
			String key = (String) jsonArray.get(i);
			Object value = deserialize(jsonObject.get(key));
			map.put(key, value);
		}
		return map;
	}

	private static List<Object> deserialize(JSONArray jsonArray) throws JSONException {
		List<Object> list = new ArrayList<Object>();
		for (int i = 0; i < jsonArray.length(); i++) {
			Object value = deserialize(jsonArray.get(i));
			list.add(value);
		}
		return list;
	}

	private static JSONType getJSONType(String string) {
		if (string.startsWith("[")) {
			return JSONType.ARRAY;
		}
		if (string.startsWith("\"")) {
			return JSONType.STRING;
		}
		if (string.startsWith("{")) {
			return JSONType.OBJECT;
		} else {
			return JSONType.UNKNOWN;
		}
	}

	private enum JSONType {
		ARRAY,
		OBJECT,
		STRING,
		UNKNOWN
	}
}
