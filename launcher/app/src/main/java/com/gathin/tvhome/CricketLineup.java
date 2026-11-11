package com.gathin.tvhome;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.function.Consumer;

final class CricketLineup {
    private static final String NETFLIX = "com.netflix.ninja";
    private static final class Entry {
        final String layer, label, appName, extra;
        final CricketCatalog.Clip clip;
        final float x, y, width, height;

        Entry(String layer, String label, String appName, String extra, CricketCatalog.Clip clip,
              float x, float y, float width, float height) {
            this.layer = layer;
            this.label = label;
            this.appName = appName;
            this.extra = extra;
            this.clip = clip;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static final Entry[][] ROWS = {
        {
            new Entry("ab-de-villiers", "AB de Villiers", "YouTube", "youtube", CricketCatalog.CLIPS[3],
                    .025f, .24f, .18f, .52f),
            new Entry("iron-man", "Iron Man", "Disney+", null,
                    new CricketCatalog.Clip(0, 0xffc62c29, "Iron Man", "Iron Man",
                            "https://www.disneyplus.com/browse/entity-0c520152-b81f-4c20-9310-003debd1947e",
                            "com.disney.disneyplus"), .215f, .205f, .17f, .57f),
            new Entry("spider-man", "Spider-Man", "Prime Video", "prime-video",
                    new CricketCatalog.Clip(0, 0xffd14649, "Spider-Man", "Spider-Man: No Way Home",
                            "https://app.primevideo.com/detail?gti=amzn1.dv.gti.53065791-4335-46ce-bc1b-5c5ebbb21073",
                            "com.amazon.amazonvideo.livingroom"), .405f, .21f, .17f, .57f),
            new Entry("netflix", "Netflix", "Netflix", null,
                    new CricketCatalog.Clip(0, 0xffe50914, "Netflix", "Netflix", "", NETFLIX),
                    .605f, .22f, .15f, .46f),
            new Entry("dhoni", "MS Dhoni", "YouTube", null, CricketCatalog.CLIPS[2],
                    .79f, .21f, .18f, .56f)
        }, {
            new Entry("virat", "Virat Kohli", "YouTube", null, CricketCatalog.CLIPS[0],
                    .09f, .39f, .22f, .54f),
            new Entry("friends", "Friends", "YouTube", null, CricketCatalog.CLIPS[17],
                    .30f, .55f, .25f, .37f),
            new Entry("big-bang-theory", "The Big Bang Theory", "Netflix", null,
                    new CricketCatalog.Clip(0, 0xff8d99ec, "The Big Bang Theory", "The Big Bang Theory",
                            "https://www.netflix.com/title/70143830", NETFLIX),
                    .53f, .53f, .25f, .39f),
            new Entry("masterchef-canada", "MasterChef Canada", "Tubi", null, CricketCatalog.CLIPS[18],
                    .755f, .52f, .22f, .40f)
        }
    };

    private final Activity activity;
    private final FrameLayout panel;
    private final CricketArt art;
    private final CricketArt.Backdrop backdrop;
    private final Consumer<CricketCatalog.Clip> watch;
    private final Consumer<CricketCatalog.Clip> save;
    private final TextView headline, detail;
    private final CricketArt.Glow glow;
    private final ImageView appIcon;
    private final TextView appName;
    private final Bitmap[][] scenes = new Bitmap[2][5];
    private final java.util.Map<String, Drawable> serviceIcons = new java.util.HashMap<>();
    private final View[][] controls = new View[2][5];
    private final ImageView[][] extras = new ImageView[2][5];
    private final ColorMatrixColorFilter gray;
    private final int width, height;
    private MediaPlayer cuePlayer;
    private boolean sceneSelected;
    private Bitmap pendingScene;
    private int pendingAccent;
    private final Runnable showPendingScene = this::applyScene;

    CricketLineup(Activity activity, FrameLayout panel, CricketArt art, CricketArt.Backdrop backdrop,
                  TextView headline, TextView detail, Consumer<CricketCatalog.Clip> watch,
                  Consumer<CricketCatalog.Clip> save,
                  int width, int height) {
        this.activity = activity;
        this.panel = panel;
        this.art = art;
        this.backdrop = backdrop;
        this.headline = headline;
        this.detail = detail;
        this.watch = watch;
        this.save = save;
        this.width = width;
        this.height = height;
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        gray = new ColorMatrixColorFilter(matrix);
        backdrop.setFullScene();
        backdrop.setTransitionMillis(105);
        glow = new CricketArt.Glow(activity);
        panel.addView(glow, place(0f, .18f, 1f, .73f));

        LinearLayout service = new LinearLayout(activity);
        service.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        appIcon = new ImageView(activity);
        appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        service.addView(appIcon, new LinearLayout.LayoutParams(dp(42), dp(34)));
        appName = label(15);
        appName.setPadding(dp(8), 0, 0, 0);
        service.addView(appName);
        panel.addView(service, place(.69f, .06f, .25f, .08f));
        addRows();
    }

    private void addRows() {
        for (int row = 0; row < ROWS.length; row++) {
            FrameLayout group = new FrameLayout(activity);
            group.setClipChildren(false);
            panel.addView(group, new FrameLayout.LayoutParams(-1, -1));
            for (int column = 0; column < ROWS[row].length; column++) {
                Entry entry = ROWS[row][column];
                scenes[row][column] = art.scene(entry.layer);
                if (!serviceIcons.containsKey(entry.clip.app)) {
                    serviceIcons.put(entry.clip.app, loadServiceIcon(entry.clip.app));
                }
                addEntry(group, entry, row, column);
            }
        }
        controls[0][0].requestFocus();
    }

    private void addEntry(FrameLayout group, Entry entry, int row, int column) {
        if (entry.extra != null) {
            ImageView extra = new ImageView(activity);
            extra.setImageBitmap(art.layer(entry.extra));
            extra.setScaleType(ImageView.ScaleType.FIT_CENTER);
            extra.setAlpha(0f);
            extra.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            group.addView(extra, place(entry.x + entry.width * .72f, entry.y + .17f, .105f, .10f));
            extras[row][column] = extra;
        }

        FrameLayout tile = new FrameLayout(activity);
        tile.setFocusable(true);
        tile.setContentDescription(entry.label + ", " + entry.clip.title + ", " + entry.appName);
        tile.setClipChildren(false);
        group.addView(tile, place(entry.x, entry.y, entry.width, entry.height));
        controls[row][column] = tile;
        Cutout portrait = new Cutout(activity);
        if (entry.layer.equals("big-bang-theory")) {
            Bitmap poster = art.bigBangPoster();
            if (poster != null) portrait.setImageDrawable(new CastPoster(activity.getResources(), poster));
        } else {
            portrait.setImageBitmap(art.layer(entry.layer));
        }
        portrait.setScaleType(ImageView.ScaleType.FIT_CENTER);
        portrait.setColorFilter(gray);
        portrait.setAlpha(.74f);
        tile.addView(portrait, new FrameLayout.LayoutParams(-1, -1));

        TextView title = label(12);
        title.setText(entry.label);
        title.setTextColor(Color.WHITE);
        title.setShadowLayer(dp(4), 0, dp(1), Color.BLACK);
        FrameLayout.LayoutParams name = new FrameLayout.LayoutParams(-1, dp(22), Gravity.BOTTOM);
        if (row == 0) name.bottomMargin = dp(25);
        tile.addView(title, name);

        tile.setOnFocusChangeListener((view, focused) -> {
            if (focused) {
                select(entry, row, column);
                startCue(entry.layer);
            } else {
                stopCue();
            }
            if (focused) portrait.clearColorFilter();
            else portrait.setColorFilter(gray);
            portrait.setAlpha(focused ? 1f : .74f);
            view.animate().scaleX(focused ? 1.10f : 1f).scaleY(focused ? 1.10f : 1f)
                    .translationZ(focused ? dp(7) : 0f).setDuration(130).start();
            ImageView extra = extras[row][column];
            if (extra != null) {
                extra.animate().cancel();
                extra.setAlpha(focused ? 1f : 0f);
            }
        });
        tile.setOnClickListener(view -> watch.accept(entry.clip));
        tile.setOnLongClickListener(view -> {
            save.accept(entry.clip);
            return true;
        });
        tile.setOnKeyListener((view, key, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            switch (key) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (column > 0) controls[row][column - 1].requestFocus();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (column < ROWS[row].length - 1) controls[row][column + 1].requestFocus();
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (row == 1) controls[0][column].requestFocus();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (row == 0) controls[1][Math.min(column, ROWS[1].length - 1)].requestFocus();
                    return true;
                default: return false;
            }
        });
    }

    void close() {
        panel.removeCallbacks(showPendingScene);
        stopCue();
    }

    private void startCue(String layer) {
        stopCue();
        int resource;
        switch (layer) {
            case "friends": resource = R.raw.cricket_friends_cue; break;
            case "big-bang-theory": resource = R.raw.cricket_big_bang_cue; break;
            case "netflix": resource = R.raw.cricket_netflix_cue; break;
            case "virat": resource = R.raw.cricket_virat_cue; break;
            case "ab-de-villiers": resource = R.raw.cricket_abd_cue; break;
            case "dhoni": resource = R.raw.cricket_dhoni_cue; break;
            default: return;
        }
        MediaPlayer player = new MediaPlayer();
        cuePlayer = player;
        try (AssetFileDescriptor file = activity.getResources().openRawResourceFd(resource)) {
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            player.setVolume(.52f, .52f);
            player.setOnPreparedListener(ready -> {
                if (cuePlayer == ready) ready.start();
            });
            player.setOnCompletionListener(done -> {
                if (cuePlayer == done) stopCue();
            });
            player.setOnErrorListener((failed, what, extra) -> {
                if (cuePlayer == failed) stopCue();
                return true;
            });
            player.prepareAsync();
        } catch (IOException | RuntimeException unavailable) {
            stopCue();
        }
    }

    private void stopCue() {
        MediaPlayer player = cuePlayer;
        cuePlayer = null;
        if (player != null) player.release();
    }

    private void select(Entry entry, int row, int column) {
        headline.setText(entry.clip.title);
        detail.setText(entry.label.equals(entry.clip.title) ? "" : entry.label);
        appName.setText(entry.appName);
        appIcon.setImageDrawable(serviceIcons.get(entry.clip.app));
        glow.tint(entry.clip.color);
        pendingScene = scenes[row][column];
        pendingAccent = entry.clip.color;
        panel.removeCallbacks(showPendingScene);
        if (sceneSelected) panel.postDelayed(showPendingScene, 145);
        else applyScene();
    }

    private void applyScene() {
        if (pendingScene != null) backdrop.show(pendingScene, sceneSelected);
        else backdrop.clear(pendingAccent);
        sceneSelected = true;
    }

    private Drawable loadServiceIcon(String packageName) {
        try {
            return activity.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException unavailable) {
            return null;
        }
    }

    private FrameLayout.LayoutParams place(float x, float y, float w, float h) {
        FrameLayout.LayoutParams bounds = new FrameLayout.LayoutParams(Math.round(width * w), Math.round(height * h));
        bounds.leftMargin = Math.round(width * x);
        bounds.topMargin = Math.round(height * y);
        return bounds;
    }

    private TextView label(int size) {
        TextView result = new TextView(activity);
        result.setTypeface(activity.getResources().getFont(R.font.outfit_medium));
        result.setGravity(Gravity.CENTER);
        result.setTextColor(Color.WHITE);
        result.setTextSize(size);
        return result;
    }

    private int dp(int n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }

    private static final class CastPoster extends BitmapDrawable {
        private final Rect crop;
        private final RectF area = new RectF();
        private final Path rounded = new Path();

        CastPoster(Resources resources, Bitmap image) {
            super(resources, image);
            crop = new Rect(0, Math.round(image.getHeight() * .293f), image.getWidth(),
                    Math.round(image.getHeight() * .943f));
            setFilterBitmap(true);
        }

        @Override public int getIntrinsicWidth() { return crop.width(); }
        @Override public int getIntrinsicHeight() { return crop.height(); }

        @Override protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            area.set(bounds);
            rounded.reset();
            float radius = Math.min(area.width(), area.height()) * .045f;
            rounded.addRoundRect(area, radius, radius, Path.Direction.CW);
        }

        @Override public void draw(Canvas canvas) {
            int save = canvas.save();
            canvas.clipPath(rounded);
            canvas.drawBitmap(getBitmap(), crop, area, getPaint());
            canvas.restoreToCount(save);
        }
    }

    private static final class Cutout extends ImageView {
        private final Paint fade = new Paint();

        Cutout(Activity context) {
            super(context);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            // The portrait and its fade stay static while the tile's scale animates.
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        }

        @Override protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
            super.onSizeChanged(w, h, oldWidth, oldHeight);
            fade.setShader(new LinearGradient(0, h * .67f, 0, Math.max(1, h),
                    Color.WHITE, 0x00ffffff, Shader.TileMode.CLAMP));
        }

        @Override protected void onDraw(Canvas canvas) {
            int layer = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
            super.onDraw(canvas);
            canvas.drawRect(0, 0, getWidth(), getHeight(), fade);
            canvas.restoreToCount(layer);
        }
    }
}
