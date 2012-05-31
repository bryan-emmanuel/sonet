package mobi.intuitit.android.widget;

import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

/**
 * 
 * @author Koxx
 * 
 */
public class WidgetContentObserver extends ContentObserver {

	private static final String TAG = "WidgetContentObserver";

	private static final boolean LOGD = true;

	WidgetDataChangeListener widgetDataChangeListener;

	public WidgetContentObserver(Handler handler, WidgetDataChangeListener dataChangeListener_p) {
		super(handler);
		widgetDataChangeListener = dataChangeListener_p;
	}

	public void onChange(boolean selfChange) {

		if (widgetDataChangeListener != null) {
			if (LOGD)
				Log.d(TAG, "onChange");
			widgetDataChangeListener.onChange();
		} else {
			if (LOGD)
				Log.d(TAG, "onChange -> no listerner");
		}

	}

}
