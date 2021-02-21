package com.theost.wavenote.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

import java.util.ArrayList;
import java.util.Iterator;

public class LayeredImageView extends androidx.appcompat.widget.AppCompatImageView {

    private ArrayList<Layer> mLayers;
    private Matrix mDrawMatrix;

    private Resources mResources;

    public LayeredImageView(Context context) {
        super(context);
        init();
    }

    public LayeredImageView(Context context, AttributeSet set) {
        super(context, set);
        init();

        int[] attrs = {
                android.R.attr.src
        };
        TypedArray a = context.obtainStyledAttributes(set, attrs);
        TypedValue outValue = new TypedValue();
        if (a.getValue(0, outValue)) {
            setImageResource(outValue.resourceId);
        }
        a.recycle();
    }

    private void init() {
        mLayers = new ArrayList<>();
        mDrawMatrix = new Matrix();
        mResources = new LayeredImageViewResources();
    }

    @Override
    protected boolean verifyDrawable(Drawable dr) {
        for (int i = 0; i < mLayers.size(); i++) {
            Layer layer = mLayers.get(i);
            if (layer.drawable == dr) {
                return true;
            }
        }
        return super.verifyDrawable(dr);
    }

    @Override
    public void invalidateDrawable(Drawable dr) {
        if (verifyDrawable(dr)) {
            invalidate();
        } else {
            super.invalidateDrawable(dr);
        }
    }

    @Override
    public Resources getResources() {
        return mResources;
    }

    @Override
    public void setImageBitmap(Bitmap bm) throws RuntimeException {
        String detailMessage = "setImageBitmap not supported, use: setImageDrawable() " +
                "or setImageResource()";
        throw new RuntimeException(detailMessage);
    }

    @Override
    public void setImageURI(Uri uri) throws RuntimeException {
        String detailMessage = "setImageURI not supported, use: setImageDrawable() " +
                "or setImageResource()";
        throw new RuntimeException(detailMessage);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Matrix matrix = getImageMatrix();
        if (matrix != null) {
            int numLayers = mLayers.size();
            boolean pendingAnimations = false;
            for (int i = 0; i < numLayers; i++) {
                mDrawMatrix.set(matrix);
                Layer layer = mLayers.get(i);
                if (layer.matrix != null) {
                    mDrawMatrix.preConcat(layer.matrix);
                }
                if (layer.animation == null) {
                    draw(canvas, layer.drawable, mDrawMatrix, 255);
                } else {
                    Animation a = layer.animation;
                    if (!a.isInitialized()) {
                        Rect bounds = layer.drawable.getBounds();
                        Drawable parentDrawable = getDrawable();
                        if (parentDrawable != null) {
                            Rect parentBounds = parentDrawable.getBounds();
                            a.initialize(bounds.width(), bounds.height(), parentBounds.width(), parentBounds.height());
                        } else {
                            a.initialize(bounds.width(), bounds.height(), 0, 0);
                        }
                    }
                    long currentTime = AnimationUtils.currentAnimationTimeMillis();
                    boolean running = a.getTransformation(currentTime, layer.transformation);
                    if (running) {
                        // animation is running: draw animation frame
                        Matrix animationFrameMatrix = layer.transformation.getMatrix();
                        mDrawMatrix.preConcat(animationFrameMatrix);

                        int alpha = (int) (255 * layer.transformation.getAlpha());
                        draw(canvas, layer.drawable, mDrawMatrix, alpha);
                        pendingAnimations = true;
                    } else {
                        // animation ended: set it to null
                        layer.animation = null;
                        draw(canvas, layer.drawable, mDrawMatrix, 255);
                    }
                }
            }
            if (pendingAnimations) {
                // invalidate if any pending animations
                invalidate();
            }
        }
    }

    private void draw(Canvas canvas, Drawable drawable, Matrix matrix, int alpha) {
        canvas.save();
        canvas.concat(matrix);
        drawable.setAlpha(alpha);
        drawable.draw(canvas);
        canvas.restore();
    }

    public Layer addLayer(Drawable d, Matrix m) {
        Layer layer = new Layer(d, m);
        mLayers.add(layer);
        invalidate();
        return layer;
    }

    public Layer addLayer(Bitmap b, Matrix m) {
        Drawable d = new BitmapDrawable(getResources(), b);
        return addLayer(d, m);
    }

    public Layer addLayer(Drawable d) {
        return addLayer(d, null);
    }

    public Layer addLayer(int idx, Drawable d, Matrix m) {
        Layer layer = new Layer(d, m);
        mLayers.add(idx, layer);
        invalidate();
        return layer;
    }

    public Layer addLayer(int idx, Drawable d) {
        return addLayer(idx, d, null);
    }

    public void removeLayer(Layer layer) {
        layer.valid = false;
        mLayers.remove(layer);
    }

    public void removeAllLayers() {
        Iterator<Layer> iter = mLayers.iterator();
        while (iter.hasNext()) {
            LayeredImageView.Layer layer = iter.next();
            layer.valid = false;
            iter.remove();
        }
        invalidate();
    }

    public int getLayersSize() {
        return mLayers.size();
    }

    public class Layer {
        private final Drawable drawable;
        private Animation animation;
        private final Transformation transformation;
        private final Matrix matrix;
        private boolean valid;

        private Layer(Drawable d, Matrix m) {
            drawable = d;
            transformation = new Transformation();
            matrix = m;
            valid = true;
            Rect bounds = d.getBounds();
            if (bounds.isEmpty()) {
                if (d instanceof BitmapDrawable) {
                    int right = d.getIntrinsicWidth();
                    int bottom = d.getIntrinsicHeight();
                    d.setBounds(0, 0, right, bottom);
                } else {
                    String detailMessage = "drawable bounds are empty, use d.setBounds()";
                    throw new RuntimeException(detailMessage);
                }
            }
            d.setCallback(LayeredImageView.this);
        }

        public void startLayerAnimation(Animation a) throws RuntimeException {
            if (!valid) {
                String detailMessage = "this layer has already been removed";
                throw new RuntimeException(detailMessage);
            }
            transformation.clear();
            animation = a;
            if (a != null) {
                a.start();
            }
            invalidate();
        }

        public void stopLayerAnimation(int idx) throws RuntimeException {
            if (!valid) {
                String detailMessage = "this layer has already been removed";
                throw new RuntimeException(detailMessage);
            }
            if (animation != null) {
                animation = null;
                invalidate();
            }
        }
    }

    private class LayeredImageViewResources extends Resources {

        public LayeredImageViewResources() {
            super(getContext().getAssets(), new DisplayMetrics(), null);
        }

        @Override
        public Drawable getDrawable(int id) throws NotFoundException {
            @SuppressLint("UseCompatLoadingForDrawables") Drawable d = super.getDrawable(id);
            if (d instanceof BitmapDrawable) {
                BitmapDrawable bd = (BitmapDrawable) d;
                bd.getBitmap().setDensity(DisplayMetrics.DENSITY_DEFAULT);
                bd.setTargetDensity(DisplayMetrics.DENSITY_DEFAULT);
            }
            return d;
        }
    }
}