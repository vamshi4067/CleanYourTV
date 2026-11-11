package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class CricketHome {
    private static final long TURN = 15 * 60_000L;
    private static final int[] PHOTO_ORDER = {16, 17, 18};
    private static final String[] PHOTO_NAMES = {"Virat Kohli", "MS Dhoni & Virat Kohli", "AB de Villiers"};

    private static final class App {
        final CricketBadge.Mark mark;
        final String wordmark, tagline;
        final int color;
        final String[] packages;

        App(CricketBadge.Mark mark, String wordmark, String tagline, int color, String... packages) {
            this.mark = mark;
            this.wordmark = wordmark;
            this.tagline = tagline;
            this.color = color;
            this.packages = packages;
        }
    }

    private static final App[] APPS = {
            new App(CricketBadge.Mark.NETFLIX, "NETFLIX", "The next innings starts here.", 0xffe50914, "com.netflix.ninja"),
            new App(CricketBadge.Mark.YOUTUBE, "YouTube", "Every great shot deserves a replay.", 0xffff3434, "com.google.android.youtube.tv"),
            new App(CricketBadge.Mark.PRIME, "prime video", "Settle in for the long innings.", 0xff00a8e1, "com.amazon.amazonvideo.livingroom"),
            new App(CricketBadge.Mark.WILLOW, "WILLOW", "The match is on.", 0xff76c858, "tv.willow", "willow.android.tv"),
            new App(CricketBadge.Mark.DISNEY, "Disney+", "Stories worth staying up for.", 0xff526aff, "com.disney.disneyplus"),
            new App(CricketBadge.Mark.APPLE, "Apple TV", "Make it a movie night.", 0xff9ba6bc, "com.apple.atve.androidtv.appletv"),
            new App(CricketBadge.Mark.HTE, "HTE", "The best seat in the house.", 0xffdcb371),
            new App(CricketBadge.Mark.HULU, "hulu", "Something good is coming up.", 0xff1ce783, "com.hulu.livingroomplus"),
            new App(CricketBadge.Mark.AHA, "aha", "A little closer to home.", 0xffff762d, "ahaflix.tv")
    };

    static boolean available(Activity activity) {
        try (java.io.InputStream ignored = activity.getAssets().open("cricket/photo-1.jpg")) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private final Activity activity;
    private final FrameLayout root;
    private final FrameLayout dock;
    private final CricketArt.Glow glow;
    private final TextView caption, tagline;
    private final LinearLayout appRow;
    private final SharedPreferences prefs;
    private final CricketArt art;
    private final CricketArt.Backdrop wallpaper;
    private final CricketWeather weather;
    private final CricketShowcase showcase;
    private final List<View> tiles = new ArrayList<>();
    private final LinearLayout actions;
    private final View moreApps, shortcuts;
    private boolean focusedOnce;
    private int lastApp;
    private final Runnable rotation = this::rotate;

    CricketHome(Activity activity, FrameLayout root, FrameLayout gridFrame, View grid, Runnable openLibrary) {
        this.activity = activity;
        this.root = root;
        prefs = activity.getSharedPreferences("cricket_home", Activity.MODE_PRIVATE);
        art = new CricketArt(activity);
        wallpaper = new CricketArt.Backdrop(activity, false);
        wallpaper.setFullScene();
        root.addView(wallpaper, root.indexOfChild(activity.findViewById(R.id.sky)), full());

        TextView clock = activity.findViewById(R.id.clock);
        clock.setTypeface(activity.getResources().getFont(R.font.outfit_light));
        activity.findViewById(R.id.greeting).setVisibility(View.GONE);
        activity.findViewById(R.id.clockDivider).setVisibility(View.GONE);
        View date = activity.findViewById(R.id.date);
        ViewGroup dateColumn = (ViewGroup) date.getParent();
        dateColumn.setVisibility(View.GONE);
        TextView weatherLabel = text(16);
        weatherLabel.setTextColor(0xffd4e3e9);
        LinearLayout.LayoutParams weatherLayout = new LinearLayout.LayoutParams(-2, -2);
        weatherLayout.gravity = Gravity.BOTTOM;
        weatherLayout.setMargins(dp(18), 0, 0, dp(8));
        ((LinearLayout) clock.getParent()).addView(weatherLabel, weatherLayout);
        weather = new CricketWeather(weatherLabel, prefs);
        for (int id : new int[]{R.id.btnWifi, R.id.btnInputs, R.id.btnSettings}) {
            TextView button = activity.findViewById(id);
            button.setBackgroundResource(R.drawable.cricket_top_focus);
            button.setCompoundDrawableTintList(activity.getColorStateList(R.drawable.pill_text));
            button.setOnFocusChangeListener((view, focused) -> scale(view, focused ? 1.10f : 1f));
        }
        activity.findViewById(R.id.btnLibraryTop).setVisibility(View.GONE);

        grid.setVisibility(View.INVISIBLE);
        activity.findViewById(R.id.btnSettings).setOnLongClickListener(null);
        dock = new FrameLayout(activity);
        dock.setClipChildren(false);
        gridFrame.addView(dock, full());

        glow = new CricketArt.Glow(activity);
        FrameLayout.LayoutParams glowLayout = new FrameLayout.LayoutParams(-1, dp(168), Gravity.BOTTOM);
        glowLayout.bottomMargin = dp(45);
        dock.addView(glow, glowLayout);
        caption = text(26);
        caption.setLetterSpacing(0.08f);
        caption.setTextColor(0xfff3efe9);
        FrameLayout.LayoutParams captionLayout = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
        captionLayout.leftMargin = dp(10);
        captionLayout.bottomMargin = dp(160);
        dock.addView(caption, captionLayout);

        LinearLayout content = new LinearLayout(activity);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setOrientation(LinearLayout.VERTICAL);
        dock.addView(content, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        tagline = text(14);
        tagline.setTextColor(0xffdae1e6);
        tagline.setGravity(Gravity.CENTER);
        content.addView(tagline, new LinearLayout.LayoutParams(-1, dp(22)));
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setClipChildren(false);
        scroll.setFocusable(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        appRow = new LinearLayout(activity);
        appRow.setGravity(Gravity.CENTER);
        appRow.setClipChildren(false);
        scroll.addView(appRow);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, dp(68)));

        FrameLayout shortcutArea = new FrameLayout(activity);
        content.addView(shortcutArea, new LinearLayout.LayoutParams(-1, dp(51)));
        LinearLayout collapsed = new LinearLayout(activity);
        collapsed.setGravity(Gravity.CENTER);
        shortcutArea.addView(collapsed, full());
        TextView shortcutButton = action(collapsed, "Shortcuts", R.drawable.ic_apps, this::openShortcuts);
        shortcutButton.setCompoundDrawableTintList(shortcutButton.getTextColors());
        shortcuts = shortcutButton;
        shortcuts.setLayoutParams(new LinearLayout.LayoutParams(-2, dp(36)));
        shortcuts.setOnKeyListener((view, key, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (key == KeyEvent.KEYCODE_DPAD_UP) {
                focusDock();
                return true;
            }
            return key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT
                    || key == KeyEvent.KEYCODE_DPAD_DOWN;
        });

        actions = new LinearLayout(activity);
        actions.setGravity(Gravity.CENTER);
        actions.setVisibility(View.GONE);
        shortcutArea.addView(actions, full());
        moreApps = action(actions, "More apps", R.drawable.cricket_action_apps, openLibrary);
        showcase = new CricketShowcase(activity, root, art, this::watch, this::installed, this::focusDock);
        action(actions, "My Favourites", R.drawable.cricket_action_favourites, showcase::favourites);
        action(actions, "Pick something", R.drawable.cricket_action_pick, showcase::moods);
        action(actions, "Match Day", R.drawable.cricket_action_match, () -> showcase.matchDay(this::openWillow));
        action(actions, "Watch Later", R.drawable.cricket_watch_later, showcase::watchLater);
        for (int i = 0; i < actions.getChildCount(); i++) {
            final int index = i;
            actions.getChildAt(i).setOnKeyListener((view, key, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (key == KeyEvent.KEYCODE_DPAD_UP) {
                    closeShortcuts();
                    focusDock();
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    int next = key == KeyEvent.KEYCODE_DPAD_LEFT ? index - 1 : index + 1;
                    if (next >= 0 && next < actions.getChildCount()) actions.getChildAt(next).requestFocus();
                    return true;
                }
                return key == KeyEvent.KEYCODE_DPAD_DOWN;
            });
        }
        reloadApps();
        rotate();
    }

    void reloadApps() {
        boolean restoreFocus = appRow.hasFocus();
        String preferred = prefs.getString("last_app", "");
        appRow.removeAllViews();
        tiles.clear();
        PackageManager manager = activity.getPackageManager();
        lastApp = 0;
        for (App app : APPS) {
            Intent launch = null;
            for (String pkg : app.packages) {
                launch = launchIntent(pkg);
                if (launch != null) break;
            }
            if (app.packages.length == 0) {
                Intent leanback = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                for (ResolveInfo info : manager.queryIntentActivities(leanback, 0)) {
                    if (info.loadLabel(manager).toString().replaceAll("[^a-zA-Z]", "").equalsIgnoreCase("HTE")) {
                        launch = launchIntent(info.activityInfo.packageName);
                        break;
                    }
                }
                if (launch == null) launch = launchIntent("dev.couchtv.ontv");
            }
            if (launch == null || launch.getComponent() == null) continue;
            String pkg = launch.getComponent().getPackageName();
            final Intent target = launch;
            final int position = tiles.size();
            LinearLayout tile = new LinearLayout(activity);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setFocusable(true);
            tile.setBackgroundResource(R.drawable.cricket_app_focus);
            tile.setContentDescription(app.wordmark);
            CricketBadge logo = new CricketBadge(activity, app.mark);
            tile.addView(logo, new LinearLayout.LayoutParams(dp(43), dp(43)));
            TextView name = text(10);
            name.setText(app.wordmark);
            name.setAlpha(0f);
            name.setGravity(Gravity.CENTER);
            tile.addView(name, new LinearLayout.LayoutParams(-1, dp(16)));
            tile.setOnClickListener(v -> {
                prefs.edit().putString("last_app", pkg).apply();
                launch(target);
            });
            tile.setOnFocusChangeListener((v, focused) -> {
                scale(logo, focused ? 1.16f : 1f);
                name.animate().alpha(focused ? 1f : 0f).setDuration(145).start();
                if (focused) {
                    lastApp = position;
                    prefs.edit().putString("last_app", pkg).apply();
                    tagline.setText(app.tagline);
                    glow.tint(app.color);
                    glow.animate().alpha(1f).setDuration(160).start();
                }
            });
            tile.setOnKeyListener((v, key, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (key == KeyEvent.KEYCODE_DPAD_UP) {
                    activity.findViewById(R.id.btnSettings).requestFocus();
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusShortcuts();
                    return true;
                }
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    int next = key == KeyEvent.KEYCODE_DPAD_LEFT ? position - 1 : position + 1;
                    if (next >= 0 && next < tiles.size()) tiles.get(next).requestFocus();
                    return true;
                }
                return false;
            });
            if (pkg.equals(preferred)) {
                lastApp = position;
            }
            tiles.add(tile);
            appRow.addView(tile, new LinearLayout.LayoutParams(dp(90), dp(66)));
        }
        if (restoreFocus) focusDock();
    }

    void start() {
        weather.start();
        rotate();
        if (!focusedOnce || activity.getCurrentFocus() == null) dock.post(this::focusDock);
        focusedOnce = true;
    }

    void stop() {
        weather.stop();
        wallpaper.removeCallbacks(rotation);
        showcase.close(false);
        closeShortcuts();
    }

    void focusDock() {
        if (tiles.isEmpty()) shortcuts.requestFocus();
        else tiles.get(Math.min(lastApp, tiles.size() - 1)).requestFocus();
    }

    void returnHome() {
        showcase.close(false);
        closeShortcuts();
        focusDock();
    }

    boolean back() {
        if (showcase.close(true) || closeShortcuts()) return true;
        if (!appRow.hasFocus()) {
            focusDock();
            return true;
        }
        return false;
    }

    boolean navigate(KeyEvent event) { return showcase.navigate(event); }

    void focusShortcuts() {
        if (actions.getVisibility() == View.VISIBLE) moreApps.requestFocus();
        else shortcuts.requestFocus();
    }

    private void openShortcuts() {
        actions.setVisibility(View.VISIBLE);
        moreApps.requestFocus();
        shortcuts.setVisibility(View.GONE);
    }

    private boolean closeShortcuts() {
        if (actions.getVisibility() != View.VISIBLE) return false;
        shortcuts.setVisibility(View.VISIBLE);
        shortcuts.requestFocus();
        actions.setVisibility(View.GONE);
        return true;
    }

    private void rotate() {
        wallpaper.removeCallbacks(rotation);
        long now = SystemClock.elapsedRealtime();
        long origin = prefs.getLong("rotation_origin", now);
        if (origin > now || prefs.getInt("rotation_first", 0) != PHOTO_ORDER[0]) {
            origin = now;
            prefs.edit().putLong("rotation_origin", origin).putInt("rotation_first", PHOTO_ORDER[0]).apply();
        }
        long elapsed = now - origin;
        int index = (int) ((elapsed / TURN) % PHOTO_ORDER.length);
        int photo = PHOTO_ORDER[index];
        wallpaper.show(art.photo(photo), true);
        caption.setText(PHOTO_NAMES[index].toUpperCase(java.util.Locale.ROOT));
        wallpaper.postDelayed(rotation, TURN - elapsed % TURN);
    }

    private TextView action(LinearLayout row, String title, int iconResource, Runnable click) {
        TextView button = text(16);
        button.setText(title);
        button.setSingleLine(true);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(6), 0, dp(6), 0);
        Drawable icon = activity.getDrawable(iconResource);
        int width = icon.getIntrinsicWidth();
        int height = icon.getIntrinsicHeight();
        float iconScale = Math.min((float) dp(37) / width, (float) dp(29) / height);
        icon.setBounds(0, 0, Math.round(width * iconScale), Math.round(height * iconScale));
        button.setCompoundDrawablesRelative(icon, null, null, null);
        button.setCompoundDrawablePadding(dp(5));
        button.setFocusable(true);
        button.setOnClickListener(v -> click.run());

        GradientDrawable idle = new GradientDrawable();
        idle.setCornerRadius(dp(12));
        idle.setColor(0xe81c2532);
        idle.setStroke(dp(1), 0xff6c6358);
        GradientDrawable selected = new GradientDrawable();
        selected.setCornerRadius(dp(12));
        selected.setColor(0xfff6ecdb);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, selected);
        background.addState(new int[0], idle);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000), background, null));
        button.setTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, new int[0]},
                new int[]{0xff111923, Color.WHITE}));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(0, dp(42), 1f);
        layout.setMargins(dp(6), 0, dp(6), 0);
        row.addView(button, layout);
        return button;
    }

    private Intent launchIntent(String pkg) {
        PackageManager manager = activity.getPackageManager();
        Intent target = manager.getLeanbackLaunchIntentForPackage(pkg);
        return target == null ? manager.getLaunchIntentForPackage(pkg) : target;
    }

    private boolean installed(CricketCatalog.Clip clip) { return launchIntent(clip.app) != null; }

    private void openWillow() {
        for (String pkg : new String[]{"tv.willow", "willow.android.tv"}) {
            Intent intent = launchIntent(pkg);
            if (intent != null && launch(intent)) return;
        }
        if (launch(new Intent(Intent.ACTION_VIEW, Uri.parse(CricketFixtures.SCHEDULE)))) return;
        Toast.makeText(activity, "Willow isn’t available on this TV", Toast.LENGTH_SHORT).show();
    }

    private void watch(CricketCatalog.Clip clip) {
        if (clip.url.isEmpty()) {
            Intent app = launchIntent(clip.app);
            if (app != null && launch(app)) return;
            Toast.makeText(activity, "This app could not open on this TV", Toast.LENGTH_LONG).show();
            return;
        }
        Uri url = Uri.parse(clip.url);
        if ("com.netflix.ninja".equals(clip.app) && "www.netflix.com".equals(url.getHost())) {
            openNetflix(url);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, url);
        if (launch(new Intent(intent).setPackage(clip.app)) || launch(intent)) return;
        Toast.makeText(activity, "This link could not open on this TV", Toast.LENGTH_LONG).show();
    }

    private void openNetflix(Uri url) {
        Uri tvTitle = url.buildUpon().appendQueryParameter("source", "30").build();
        Intent title = new Intent(Intent.ACTION_VIEW, tvTitle).setPackage("com.netflix.ninja");
        if (launch(title)) return;
        Uri nativeTitle = tvTitle.buildUpon().scheme("nflx").build();
        if (launch(title.setData(nativeTitle))) return;
        Toast.makeText(activity, "This Netflix title could not open on this TV", Toast.LENGTH_LONG).show();
    }

    private boolean launch(Intent target) {
        try {
            activity.startActivity(target);
            return true;
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            return false;
        }
    }

    private TextView text(int size) {
        TextView label = new TextView(activity);
        label.setTypeface(activity.getResources().getFont(R.font.outfit_medium));
        label.setTextSize(size);
        label.setTextColor(Color.WHITE);
        label.setIncludeFontPadding(false);
        return label;
    }

    private int dp(int n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }
    private FrameLayout.LayoutParams full() { return new FrameLayout.LayoutParams(-1, -1); }
    private void scale(View view, float size) {
        view.animate().scaleX(size).scaleY(size).setDuration(150).start();
    }
}
