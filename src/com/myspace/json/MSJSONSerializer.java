package com.myspace.json;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MSJSONSerializer {

	public static String getJSONString(Object object) throws JSONException {
		JSONObject jsonObject = (JSONObject) serialize(object);
		return jsonObject.toString();
	}

	private static Object serialize(Object object) throws JSONException {
		if (object instanceof Map<?,?>) {
			return serialize((Map<?,?>) object);
		}
		else if (object instanceof List<?>) {
			return serialize((List<?>) object);
		}
		return object;
	}

	private static JSONObject serialize(Map<?,?> map) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		for (Object keyObject : map.keySet())  {
			String key = keyObject.toString();
			Object value = serialize(map.get(key));
			jsonObject.put(key, value);
		}
		return jsonObject;
	}

	private static JSONArray serialize(List<?> list) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (int i = 0; i < list.size(); i++) {
			Object value = serialize(list.get(i));
			jsonArray.put(i, value);
		}
		return jsonArray;
	}
}
