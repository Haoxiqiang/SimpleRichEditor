package io.hxq.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 */
public class Fit169ImageView extends AppCompatImageView {


    private int width;
    private int height;

    private int intrinsicWidth;
    private int intrinsicHeight;


    public Fit169ImageView(Context context) {
        this(context, null);
    }

    public Fit169ImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Fit169ImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        height = (int) (width * 9.0f / 16.0f + 0.5f);

        setMeasuredDimension(width, height);

        scaleToFit(false);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != width) {
            height = (int) (width * 9.0f / 16.0f + 0.5f);
//            height = h;
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        free();
        if (drawable != null) {
            intrinsicWidth = drawable.getIntrinsicWidth();
            intrinsicHeight = drawable.getIntrinsicHeight();
            super.setImageDrawable(drawable);
            scaleToFit();
        } else {
            super.setImageDrawable(null);
        }
    }

    @Override
    public void setImageResource(int resourceId) {
        free();
        super.setImageResource(resourceId);

        Drawable d = this.getDrawable();

        if (d != null) {
            intrinsicWidth = d.getIntrinsicWidth();
            intrinsicHeight = d.getIntrinsicHeight();
            scaleToFit();
        }
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        free();
        if (bitmap != null) {
            intrinsicWidth = bitmap.getWidth();
            intrinsicHeight = bitmap.getHeight();
            super.setImageBitmap(bitmap);
            scaleToFit();
        } else {
            super.setImageBitmap(null);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        free();
    }

    private void free() {
        if (getDrawingCache() != null) {
            setImageBitmap(null);
            destroyDrawingCache();
        }

    }

    private void scaleToFit() {
        scaleToFit(true);
    }

    private void scaleToFit(boolean forceLayout) {

        float scale = height / (float) intrinsicHeight;
        float xTranslation = (width - (float) intrinsicWidth * scale) / 2.0f;
        float yTranslation = 0.0f;
        Matrix transformation = new Matrix();
        transformation.postTranslate(xTranslation, yTranslation);
        transformation.preScale(scale, scale);
        setImageMatrix(transformation);
        if (forceLayout) {
            requestLayout();
        }
        postInvalidate();

    }
}
