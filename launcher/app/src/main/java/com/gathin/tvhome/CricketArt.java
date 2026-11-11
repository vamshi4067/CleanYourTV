package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.LruCache;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

final class CricketArt {
    private final Context context;
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(12 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap image) { return image.getByteCount(); }
    };

    CricketArt(Context context) { this.context = context; }

    Bitmap photo(int number) { return load("photo-" + number + ".jpg"); }

    Bitmap layer(String name) { return load("lineup/" + name + ".png"); }

    Bitmap moviePoster(CricketCatalog.Clip clip) {
        android.net.Uri uri = android.net.Uri.parse(clip.url);
        String host = uri.getHost();
        if ("www.netflix.com".equals(host)) {
            String id = uri.getLastPathSegment();
            return id != null && id.matches("[0-9]+") ? load("posters/" + id + ".jpg") : null;
        }
        if ("app.primevideo.com".equals(host)) {
            String id = uri.getQueryParameter("gti");
            if (id == null) id = uri.getLastPathSegment();
            return id != null && (id.matches("amzn1\\.dv\\.gti\\.[a-f0-9-]+") || id.matches("[A-Z0-9]{26}"))
                    ? load("posters/" + id + ".jpg") : null;
        }
        if ("www.youtube.com".equals(host)) {
            String handle = uri.getLastPathSegment();
            if (handle != null && handle.startsWith("@")) {
                return load("posters/youtube-" + handle.substring(1).toLowerCase(java.util.Locale.ROOT)
                        .replaceAll("[^a-z0-9_-]", "") + ".jpg");
            }
        }
        return null;
    }

    Bitmap bigBangPoster() { return load("lineup/big-bang-theory-framed.jpg", 2); }

    Bitmap scene(String name) { return load("lineup/" + name + ".jpg"); }

    private Bitmap load(String name) { return load(name, 1); }

    private Bitmap load(String name, int sample) {
        Bitmap image = cache.get(name);
        if (image != null) return image;
        try (InputStream stream = context.getAssets().open("cricket/" + name)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            image = BitmapFactory.decodeStream(stream, null, options);
            if (image != null) cache.put(name, image);
            return image;
        } catch (IOException ignored) {
            return null;
        }
    }

    static final class Glow extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Glow(Context context) {
            super(context);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void tint(int color) {
            paint.setShader(new RadialGradient(0f, 0f, 1f,
                    new int[]{color & 0x00ffffff | 0x59000000, color & 0x00ffffff | 0x20000000, 0},
                    new float[]{0f, 0.54f, 1f}, Shader.TileMode.CLAMP));
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            if (paint.getShader() == null) return;
            canvas.save();
            canvas.translate(getWidth() / 2f, getHeight() * 0.58f);
            canvas.scale(getWidth() / 2f, getHeight() * 0.42f);
            canvas.drawCircle(0f, 0f, 1f, paint);
            canvas.restore();
        }
    }

    static final class Backdrop extends View {
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final RectF frame = new RectF();
        private final Paint feather = new Paint();
        private final PorterDuffXfermode keepInside = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
        private final ColorMatrixColorFilter monochrome;
        private final boolean dark;
        private Bitmap picture;
        private Bitmap previous;
        private boolean fullScene;
        private long transitionMillis = 320;
        private LinearGradient horizontalShade, verticalShade;
        private int backdropAccent = 0xff080c12;
        private ValueAnimator fade;
        private float fraction = 1f;

        Backdrop(Context context, boolean dark) {
            super(context);
            this.dark = dark;
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(dark ? 0f : 0.82f);
            monochrome = new ColorMatrixColorFilter(matrix);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void show(Bitmap next, boolean animate) {
            if (next == picture || next == null) return;
            if (fade != null) fade.cancel();
            previous = animate ? picture : null;
            picture = next;
            fraction = previous == null ? 1f : 0f;
            if (previous != null) {
                fade = ValueAnimator.ofFloat(0f, 1f);
                fade.setDuration(transitionMillis);
                fade.addUpdateListener(value -> {
                    fraction = (Float) value.getAnimatedValue();
                    if (fraction == 1f) previous = null;
                    invalidate();
                });
                fade.start();
            }
            invalidate();
        }

        void setFullScene() { fullScene = true; }

        void setTransitionMillis(long duration) { transitionMillis = duration; }

        @Override protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
            if (dark) {
                horizontalShade = new LinearGradient(0, 0, w, 0,
                        new int[]{0xff080c12, 0xd0080c12, 0x00080c12, 0x85080c12},
                        new float[]{0f, 0.34f, 0.69f, 1f}, Shader.TileMode.CLAMP);
            }
            verticalShade = new LinearGradient(0, 0, 0, h,
                    dark ? new int[]{0x77080c12, 0x00080c12, 0x00080c12, 0xff080c12}
                         : new int[]{0x90060a10, 0x16060a10, 0x33060a10, 0xf7060a10},
                    new float[]{0f, 0.24f, 0.60f, 1f}, Shader.TileMode.CLAMP);
        }

        void clear(int accent) {
            if (fade != null) fade.cancel();
            previous = null;
            picture = null;
            backdropAccent = accent;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            canvas.drawColor(0xff080c12);
            if (previous != null) drawPhoto(canvas, previous, 255, w, h);
            if (picture != null) drawPhoto(canvas, picture, Math.round(255 * fraction), w, h);
            else {
                ink.setColorFilter(null);
                ink.setAlpha(255);
                ink.setShader(new RadialGradient(w * 0.68f, h * 0.47f, Math.max(1f, w * 0.57f),
                        backdropAccent & 0x00ffffff | 0x8c000000, 0x00080c12, Shader.TileMode.CLAMP));
                canvas.drawRect(0, 0, w, h, ink);
            }
            ink.setColorFilter(null);
            ink.setAlpha(255);
            if (horizontalShade != null) {
                ink.setShader(horizontalShade);
                canvas.drawRect(0, 0, w, h, ink);
            }
            ink.setShader(verticalShade);
            canvas.drawRect(0, 0, w, h, ink);
            ink.setShader(null);
            if (dark) canvas.drawColor(0x62080c12);
        }

        private void drawPhoto(Canvas canvas, Bitmap image, int alpha, float w, float h) {
            float aspect = (float) image.getWidth() / image.getHeight();
            if (fullScene && aspect >= 1.35f) {
                float scale = Math.max(w / image.getWidth(), h / image.getHeight());
                float iw = image.getWidth() * scale, ih = image.getHeight() * scale;
                frame.set((w - iw) / 2, (h - ih) / 2, (w + iw) / 2, (h + ih) / 2);
                ink.setColorFilter(monochrome);
                ink.setAlpha(alpha);
                canvas.drawBitmap(image, null, frame, ink);
                return;
            }
            float width = h * aspect;
            float x = w * 0.72f - width / 2f;
            frame.set(x, 0f, x + width, h);
            ink.setColorFilter(monochrome);
            ink.setAlpha(alpha);
            int layer = canvas.saveLayer(x, 0, x + width, h, null);
            canvas.drawBitmap(image, null, frame, ink);
            feather.setShader(new LinearGradient(x, 0, x + width, 0,
                    new int[]{0x00ffffff, 0xffffffff, 0xffffffff, 0x00ffffff},
                    new float[]{0f, 0.13f, 0.87f, 1f}, Shader.TileMode.CLAMP));
            feather.setXfermode(keepInside);
            canvas.drawRect(0, 0, w, h, feather);
            canvas.restoreToCount(layer);
        }
    }
}
