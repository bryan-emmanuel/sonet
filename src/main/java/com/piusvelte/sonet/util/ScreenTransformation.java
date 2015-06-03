package com.piusvelte.sonet.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import com.squareup.picasso.Transformation;

/**
 * Created by bemmanuel on 5/27/15.
 */
public class ScreenTransformation implements Transformation {

    private Paint mTintPaint = new Paint();
    private String mKey;

    public ScreenTransformation(int colorRes) {
        mKey = ScreenTransformation.class.getSimpleName() + ":" + colorRes;
        ColorFilter tint = new PorterDuffColorFilter(colorRes, PorterDuff.Mode.SCREEN);
        mTintPaint.setColorFilter(tint);
    }

    @Override
    public Bitmap transform(Bitmap source) {
        if (source != null) {
            Bitmap out = source.copy(Bitmap.Config.ARGB_8888, true);

            if (out != source) {
                source.recycle();
            }

            if (out != null) {
                Canvas canvas = new Canvas(out);
                canvas.drawBitmap(out, 0, 0, mTintPaint);
            }

            return out;
        }

        return null;
    }

    @Override
    public String key() {
        return mKey;
    }
}
