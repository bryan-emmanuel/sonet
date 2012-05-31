package mobi.intuitit.android.widget;

import mobi.intuitit.android.content.LauncherIntent;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * 
 * @author Bo
 * 
 */
public abstract class WidgetCellLayout extends ViewGroup {

    public WidgetCellLayout(Context context) {
        super(context);
    }

    public WidgetCellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // do nothing here
    }

    /**
     * Called when this cell layout get into the viewport
     */
    public void onViewportIn() {
        View child;
        AppWidgetHostView widgetView;
        AppWidgetProviderInfo widgetInfo;
        Intent intent;
        for (int i = this.getChildCount() - 1; i >= 0; i--) {
            try {
                child = this.getChildAt(i);
                if (child instanceof AppWidgetHostView) {
                    widgetView = ((AppWidgetHostView) child);
                    widgetInfo = widgetView.getAppWidgetInfo();
                    int appWidgetId = widgetView.getAppWidgetId();
                    intent = new Intent(LauncherIntent.Notification.NOTIFICATION_IN_VIEWPORT)
                            .setComponent(widgetInfo.provider);
                    intent.putExtra(LauncherIntent.Extra.EXTRA_APPWIDGET_ID, appWidgetId);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    getContext().sendBroadcast(intent);
                }
            } catch (Exception e) {
                // LauncherApplication.reportExceptionStack(e);
            }
        }

    }

    /**
     * Called when this cell layout get into the viewport
     */
    public void onViewportOut() {
        View child;
        AppWidgetHostView widgetView;
        AppWidgetProviderInfo widgetInfo;
        Intent intent;

        for (int i = this.getChildCount() - 1; i >= 0; i--) {
            try {
                child = this.getChildAt(i);
                if (child instanceof AppWidgetHostView) {
                    widgetView = ((AppWidgetHostView) child);

                    // Stop all animations in the view
                    stopAllAnimationDrawables(widgetView);

                    // Notify the widget provider
                    widgetInfo = widgetView.getAppWidgetInfo();
                    int appWidgetId = widgetView.getAppWidgetId();
                    intent = new Intent(LauncherIntent.Notification.NOTIFICATION_OUT_VIEWPORT)
                            .setComponent(widgetInfo.provider);
                    intent.putExtra(LauncherIntent.Extra.EXTRA_APPWIDGET_ID, appWidgetId);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    getContext().sendBroadcast(intent);
                }
            } catch (Exception e) {
                // LauncherApplication.reportExceptionStack(e);
            }
        }
    }

    private void stopAllAnimationDrawables(ViewGroup vg) {
        View child;

        for (int i = vg.getChildCount() - 1; i >= 0; i--) {
            child = vg.getChildAt(i);
            if (child instanceof ImageView) {
                try {
                    AnimationDrawable ad = (AnimationDrawable) ((ImageView) child).getDrawable();
                    ad.stop();
                } catch (Exception e) {
                }
            } else if (child instanceof ViewGroup) {
                stopAllAnimationDrawables((ViewGroup) child);
            }
        }
    }
}
