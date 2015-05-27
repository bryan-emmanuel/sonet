package com.piusvelte.sonet.util;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.squareup.picasso.Transformation;

/**
 * Created by bemmanuel on 5/16/15.
 */
public class CircleTransformation implements Transformation {

    @Override
    public Bitmap transform(Bitmap source) {
        if (source != null) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new BitmapShader(source, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
            int radius = Math.min(source.getWidth(), source.getHeight()) / 2;

            Bitmap circle = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(circle);
            canvas.drawCircle(radius, radius, radius, paint);

            source.recycle();
            return circle;
        }

        return null;
    }

    @Override
    public String key() {
        return CircleTransformation.class.getSimpleName();
    }
}
