package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.gathin.tvhome.CricketCatalog.CLIPS;
import static com.gathin.tvhome.CricketCatalog.MOODS;
import static com.gathin.tvhome.CricketCatalog.MOOD_COLORS;
import static com.gathin.tvhome.CricketCatalog.PICKS;

final class CricketShowcase {
    private static final int[] REEL_DELAYS = {58, 63, 70, 83, 100, 128, 167, 220, 295};
    private final Activity activity;
    private final FrameLayout root;
    private final CricketArt art;
    private final Consumer<CricketCatalog.Clip> watch;
    private final Predicate<CricketCatalog.Clip> installed;
    private final Runnable restoreFocus;
    private final CricketDeck deck = new CricketDeck(MOODS.length, new Random());
    private final ArrayList<CricketCatalog.Clip> options = new ArrayList<>();
    private FrameLayout panel, card, fan;
    private CricketLineup lineup;
    private CricketArt.Backdrop backdrop;
    private TextView headline, detail, cardTitle, cardDetail;
    private ProgressBar countdownBar;
    private int mood, reelStep, selected = -1;
    private long countdownEnd;
    private boolean spinning;
    private View origin;
    private final Runnable reelTick = this::rollFrame;
    private final Runnable countTick = this::count;
    private CricketFixtures.Match cachedMatch;
    private long fixturesChecked;
    private final CricketTitleStore watchLater;
    private final CricketTitleStore[] customPicks = new CricketTitleStore[MOODS.length];
    private CricketPickBrowser pickBrowser;

    CricketShowcase(Activity activity, FrameLayout root, CricketArt art,
                    Consumer<CricketCatalog.Clip> watch, Predicate<CricketCatalog.Clip> installed,
                    Runnable restoreFocus) {
        this.activity = activity;
        this.root = root;
        this.art = art;
        this.watch = watch;
        this.installed = installed;
        this.restoreFocus = restoreFocus;
        watchLater = new CricketTitleStore(activity, "cricket_watch_later");
        for (int i = 0; i < customPicks.length; i++) {
            customPicks[i] = new CricketTitleStore(activity, "cricket_custom_picks_" + CricketCatalog.CUSTOM_PICK_KEYS[i]);
        }
        android.content.SharedPreferences migrations = activity.getSharedPreferences("cricket_pick_migrations", Activity.MODE_PRIVATE);
        if (!migrations.getBoolean("cricket_combined_with_matches", false)) {
            List<CricketCatalog.Clip> previous = new ArrayList<>();
            for (int legacy : new int[]{0, 1, 4, 5}) {
                previous.addAll(new CricketTitleStore(activity, "cricket_custom_picks_" + legacy).items());
            }
            if (customPicks[0].importTitles(previous)) {
                migrations.edit().putBoolean("cricket_combined_with_matches", true).commit();
            }
        }
    }

    boolean isOpen() { return panel != null; }

    boolean close(boolean returnFocus) {
        if (panel == null) return false;
        if (pickBrowser != null) {
            if (returnFocus) {
                pickBrowser.finish();
                return true;
            }
            pickBrowser.dispose();
            root.removeView(pickBrowser);
            pickBrowser = null;
        }
        cancelRound();
        if (lineup != null) {
            lineup.close();
            lineup = null;
        }
        FrameLayout closing = panel;
        panel = null;
        if (card != null) card.animate().cancel();
        closing.setDescendantFocusability(FrameLayout.FOCUS_BLOCK_DESCENDANTS);
        closing.setFocusable(false);
        closing.clearFocus();
        if (returnFocus) closing.animate().alpha(0f).setDuration(185).withEndAction(() -> root.removeView(closing)).start();
        else root.removeView(closing);
        if (returnFocus && (origin == null || !origin.requestFocus())) restoreFocus.run();
        origin = null;
        return true;
    }

    boolean navigate(KeyEvent event) {
        if (panel == null || event.getAction() != KeyEvent.ACTION_DOWN) return false;
        int direction;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT: direction = View.FOCUS_LEFT; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: direction = View.FOCUS_RIGHT; break;
            case KeyEvent.KEYCODE_DPAD_UP: direction = View.FOCUS_UP; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: direction = View.FOCUS_DOWN; break;
            default: return false;
        }
        View scope = pickBrowser != null ? pickBrowser : panel;
        View focused = activity.getCurrentFocus();
        if (!inside(focused, scope)) {
            scope.requestFocus();
            return true;
        }
        if (scope.dispatchKeyEvent(event)) return true;
        View next = focused.focusSearch(direction);
        if (inside(next, scope)) next.requestFocus(direction);
        return true;
    }

    private boolean inside(View view, View scope) {
        while (view != null) {
            if (view == scope) return true;
            view = view.getParent() instanceof View ? (View) view.getParent() : null;
        }
        return false;
    }

    void favourites() {
        open("My Favourites", "↑ ↓  Change row     ← →  Browse     OK  Open     Hold OK  Save     Back  Home");
        lineup = new CricketLineup(activity, panel, art, backdrop, headline, detail, watch, this::saveTitle,
                root.getWidth(), root.getHeight());
    }

    private void saveTitle(CricketCatalog.Clip clip) {
        if (clip.url.isEmpty()) {
            Toast.makeText(activity, "Choose a movie, show or video to save", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean added = watchLater.save(clip);
        Toast.makeText(activity, added ? "Saved to Watch Later" : "Already in Watch Later", Toast.LENGTH_SHORT).show();
    }

    void watchLater() {
        open("Watch Later", "← →  Browse     OK  Open     Hold OK  Remove     Back  Home");
        List<CricketCatalog.Clip> saved = watchLater.items();
        if (saved.isEmpty()) {
            detail.setText("Hold OK on a title in Favourites or Pick something to save it here.");
            TextView empty = text(23, Gravity.CENTER);
            empty.setText("Your next watch starts here");
            panel.addView(empty, new FrameLayout.LayoutParams(-1, dp(100), Gravity.CENTER));
            panel.requestFocus();
            return;
        }
        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(48), dp(6), dp(48), dp(6));
        FrameLayout.LayoutParams shelfPosition = new FrameLayout.LayoutParams(-1, dp(225), Gravity.CENTER);
        shelfPosition.topMargin = dp(40);
        panel.addView(scroll, shelfPosition);
        LinearLayout shelf = new LinearLayout(activity);
        shelf.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(shelf);
        for (int i = 0; i < saved.size(); i++) {
            CricketCatalog.Clip clip = saved.get(i);
            LinearLayout tile = new LinearLayout(activity);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(8), dp(8), dp(8), dp(8));
            tile.setFocusable(true);
            tile.setBackgroundResource(R.drawable.cricket_app_focus);
            tile.setContentDescription(clip.title + ", " + provider(clip));
            LinearLayout.LayoutParams tilePosition = new LinearLayout.LayoutParams(dp(200), dp(205));
            tilePosition.setMargins(dp(5), 0, dp(5), 0);
            shelf.addView(tile, tilePosition);
            ImageView poster = new ImageView(activity);
            android.graphics.Bitmap cover = art.moviePoster(clip);
            if (cover == null && clip.title.equals("The Big Bang Theory")) cover = art.bigBangPoster();
            if (cover == null && clip.title.startsWith("Iron Man")) cover = art.layer("iron-man");
            if (cover == null && clip.title.startsWith("Spider-Man")) cover = art.layer("spider-man");
            if (cover == null && clip.title.equals("MasterChef Canada")) cover = art.scene("masterchef-canada");
            if (cover == null && clip.player.equals("AB de Villiers")) cover = art.photo(18);
            if (cover == null && clip.player.equals("Virat Kohli")) cover = art.photo(16);
            if (cover == null && clip.player.equals("MS Dhoni")) cover = art.layer("dhoni");
            if (cover == null && clip.photo != 0) cover = art.photo(clip.photo);
            poster.setImageBitmap(cover);
            poster.setScaleType(ImageView.ScaleType.FIT_CENTER);
            poster.setBackground(round(0xff15202c, dp(7)));
            tile.addView(poster, new LinearLayout.LayoutParams(-1, dp(120)));
            TextView title = text(16, Gravity.CENTER);
            title.setText(clip.title);
            tile.addView(title, new LinearLayout.LayoutParams(-1, 0, 1));
            TextView app = text(12, Gravity.CENTER);
            app.setText(provider(clip));
            app.setTextColor(0xffb8cbd7);
            tile.addView(app, new LinearLayout.LayoutParams(-1, dp(21)));
            tile.setOnFocusChangeListener((v, focused) -> {
                if (focused) {
                    headline.setText(clip.title);
                    detail.setText((saved.indexOf(clip) + 1) + " of " + saved.size() + " saved  ·  " + provider(clip)
                            + (installed.test(clip) ? "" : "  ·  App not installed"));
                }
            });
            tile.setOnClickListener(v -> watch.accept(clip));
            tile.setOnLongClickListener(v -> {
                int position = shelf.indexOfChild(tile);
                watchLater.remove(clip);
                saved.remove(clip);
                tile.setOnLongClickListener(null);
                shelf.removeView(tile);
                if (shelf.getChildCount() == 0) {
                    headline.setText("Watch Later");
                    detail.setText("All caught up. Hold OK on another title to save it.");
                    panel.requestFocus();
                } else {
                    shelf.getChildAt(Math.min(position, shelf.getChildCount() - 1)).requestFocus();
                    detail.setText(shelf.getChildCount() + " saved  ·  Hold OK to remove");
                }
                return true;
            });
        }
        shelf.getChildAt(0).requestFocus();
    }

    private String provider(CricketCatalog.Clip clip) {
        switch (clip.app) {
            case "com.netflix.ninja": return "Netflix";
            case "com.amazon.amazonvideo.livingroom": return "Prime Video";
            case "com.google.android.youtube.tv": return "YouTube";
            case "com.disney.disneyplus": return "Disney+";
            default: return clip.player;
        }
    }

    void matchDay(Runnable openWillow) {
        open("Match Day", "OK  Open Willow     Back  Home");
        FrameLayout shown = panel;
        detail.setText("Willow fixtures  ·  Eastern Time");

        LinearLayout scoreboard = new LinearLayout(activity);
        scoreboard.setOrientation(LinearLayout.VERTICAL);
        scoreboard.setGravity(Gravity.CENTER);
        scoreboard.setPadding(dp(22), dp(16), dp(22), dp(16));
        scoreboard.setBackground(round(0xe51b2937, dp(18)));
        panel.addView(scoreboard, new FrameLayout.LayoutParams(
                Math.min(dp(570), root.getWidth() * 4 / 5), dp(210), Gravity.CENTER));
        TextView label = text(14, Gravity.CENTER);
        label.setTextColor(0xffa9dbbf);
        label.setText("UPCOMING FIXTURE");
        scoreboard.addView(label, new LinearLayout.LayoutParams(-1, dp(26)));
        TextView teams = text(27, Gravity.CENTER);
        teams.setText("Checking Willow’s schedule…");
        scoreboard.addView(teams, new LinearLayout.LayoutParams(-1, 0, 1f));
        TextView start = text(17, Gravity.CENTER);
        start.setTextColor(0xffd4e4eb);
        scoreboard.addView(start, new LinearLayout.LayoutParams(-1, dp(38)));
        TextView willow = text(17, Gravity.CENTER);
        willow.setText("Open Willow");
        willow.setTextColor(0xff11221a);
        willow.setPadding(dp(18), 0, dp(18), 0);
        willow.setFocusable(true);
        willow.setOnClickListener(v -> openWillow.run());
        StateListDrawable button = new StateListDrawable();
        button.addState(new int[]{android.R.attr.state_focused}, round(0xffdcffeb, dp(10)));
        button.addState(new int[0], round(0xffa9dbbf, dp(10)));
        willow.setBackground(button);
        scoreboard.addView(willow, new LinearLayout.LayoutParams(-2, dp(38)));
        willow.requestFocus();

        Consumer<CricketFixtures.Match> display = match -> {
            if (match == null) {
                label.setText("UPCOMING FIXTURE");
                teams.setText("No upcoming fixtures listed");
                start.setText("");
                detail.setText("Willow fixtures  ·  Eastern Time");
                return;
            }
            label.setText(match.followed ? "NEXT MATCH FOR YOUR TEAMS" : "NEXT MATCH ON THE SCHEDULE");
            teams.setText(match.teams);
            start.setText(match.localStart());
            detail.setText(match.name + "  ·  Willow fixtures");
        };
        Instant now = Instant.now();
        boolean usableCache = cachedMatch != null && !cachedMatch.start.isBefore(now);
        if (usableCache) {
            display.accept(cachedMatch);
            if (SystemClock.elapsedRealtime() - fixturesChecked < 10 * 60_000L) return;
        }
        new Thread(() -> {
            try {
                CricketFixtures.Match match = CricketFixtures.fetch(Instant.now());
                activity.runOnUiThread(() -> {
                    if (panel != shown) return;
                    cachedMatch = match;
                    fixturesChecked = SystemClock.elapsedRealtime();
                    display.accept(match);
                });
            } catch (Exception unavailable) {
                activity.runOnUiThread(() -> {
                    if (panel != shown) return;
                    if (usableCache) label.setText("LAST KNOWN FIXTURE");
                    else teams.setText("Willow’s schedule is unavailable");
                });
            }
        }, "cricket-fixtures").start();
    }

    void moods() {
        open("Don’t have time to browse?", "← → Categories     OK Pick / Open     Hold OK Save     ↑ Add more     Back Home");
        TextView add = text(16, Gravity.CENTER);
        add.setText("+ Add more");
        add.setPadding(dp(16), 0, dp(16), 0);
        add.setFocusable(true);
        add.setBackgroundResource(R.drawable.cricket_top_focus);
        add.setTextColor(activity.getColorStateList(R.drawable.pill_text));
        FrameLayout.LayoutParams addPosition = new FrameLayout.LayoutParams(-2, dp(38), Gravity.TOP | Gravity.END);
        addPosition.topMargin = dp(28);
        addPosition.rightMargin = dp(40);
        panel.addView(add, addPosition);
        add.setOnClickListener(v -> browsePicks(add));
        add.setOnKeyListener((v, key, event) -> {
            if (key == KeyEvent.KEYCODE_DPAD_DOWN && event.getAction() == KeyEvent.ACTION_DOWN) {
                card.requestFocus();
                return true;
            }
            return false;
        });
        int w = Math.min(dp(375), root.getWidth() * 47 / 100);
        for (int layer = 2; layer > 0; layer--) {
            View backCard = new View(activity);
            backCard.setBackground(round(0xff202c38 + layer * 0x00020506, dp(23)));
            FrameLayout.LayoutParams layerPosition = new FrameLayout.LayoutParams(w, dp(245), Gravity.CENTER);
            layerPosition.leftMargin = dp(layer * 29);
            layerPosition.topMargin = dp(15 + layer * 18);
            panel.addView(backCard, layerPosition);
            backCard.setRotation(layer * 3f);
        }

        card = new FrameLayout(activity);
        FrameLayout.LayoutParams cardPosition = new FrameLayout.LayoutParams(w, dp(245), Gravity.CENTER);
        cardPosition.topMargin = dp(18);
        panel.addView(card, cardPosition);
        card.setFocusable(true);
        card.setOnClickListener(v -> { if (spinning) return; if (selected >= 0) openPick(); else spin(); });
        card.setOnLongClickListener(v -> {
            if (spinning || selected < 0) return true;
            CricketCatalog.Clip clip = options.get(selected);
            cancelRound();
            cardDetail.setText(clip.player + "  ·  OK to open");
            saveTitle(clip);
            return true;
        });
        card.setOnKeyListener((v, key, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0
                    && (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER)
                    && selected >= 0 && !spinning) {
                cancelRound();
                cardDetail.setText("OK to open  ·  Hold OK to save");
            }
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                skipMood(key == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                return true;
            }
            if (key == KeyEvent.KEYCODE_DPAD_UP) {
                cancelRound();
                showMood();
                add.requestFocus();
                return true;
            }
            return key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN;
        });

        fan = new FrameLayout(activity);
        FrameLayout.LayoutParams fanPosition = new FrameLayout.LayoutParams(-1, dp(139), Gravity.TOP);
        fanPosition.topMargin = dp(19);
        card.addView(fan, fanPosition);
        cardTitle = text(25, Gravity.CENTER);
        FrameLayout.LayoutParams titlePosition = new FrameLayout.LayoutParams(-1, dp(47), Gravity.BOTTOM);
        titlePosition.bottomMargin = dp(38);
        card.addView(cardTitle, titlePosition);
        cardDetail = text(13, Gravity.CENTER);
        FrameLayout.LayoutParams detailPosition = new FrameLayout.LayoutParams(-1, dp(28), Gravity.BOTTOM);
        detailPosition.bottomMargin = dp(16);
        card.addView(cardDetail, detailPosition);
        countdownBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        countdownBar.setMax(3000);
        FrameLayout.LayoutParams progress = new FrameLayout.LayoutParams(-1, dp(5), Gravity.BOTTOM);
        progress.leftMargin = dp(27);
        progress.rightMargin = dp(27);
        progress.bottomMargin = dp(13);
        card.addView(countdownBar, progress);
        showMood();
        card.requestFocus();
    }

    private void browsePicks(View returnTo) {
        cancelRound();
        showMood();
        pickBrowser = new CricketPickBrowser(activity, art, customPicks, mood, category -> {
            CricketPickBrowser closing = pickBrowser;
            pickBrowser = null;
            if (closing != null) {
                closing.dispose();
                root.removeView(closing);
            }
            mood = category;
            showMood();
            returnTo.requestFocus();
        });
        root.addView(pickBrowser, new FrameLayout.LayoutParams(-1, -1));
        pickBrowser.focusBrowser();
    }

    private void open(String title, String keys) {
        close(false);
        origin = activity.getCurrentFocus();
        panel = new FrameLayout(activity);
        panel.setBackgroundColor(0xff080c12);
        panel.setFocusableInTouchMode(true);
        panel.setDescendantFocusability(FrameLayout.FOCUS_AFTER_DESCENDANTS);
        root.addView(panel, new FrameLayout.LayoutParams(-1, -1));
        backdrop = new CricketArt.Backdrop(activity, true);
        backdrop.show(art.photo(1), false);
        panel.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));
        TextView kicker = text(14, Gravity.START);
        kicker.setText(title.toUpperCase(java.util.Locale.ROOT));
        kicker.setLetterSpacing(0.14f);
        kicker.setTextColor(0xff95b4c5);
        addLabel(kicker, Gravity.TOP, 29);
        headline = text(31, Gravity.START);
        headline.setText(title);
        addLabel(headline, Gravity.TOP, 53);
        detail = text(15, Gravity.START);
        detail.setTextColor(0xffc7d4dc);
        addLabel(detail, Gravity.TOP, 94);
        TextView hints = text(14, Gravity.CENTER);
        hints.setText(keys);
        hints.setTextColor(0xffc7d4dc);
        addLabel(hints, Gravity.BOTTOM, 28);
    }

    private void addLabel(TextView view, int gravity, int inset) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2, gravity);
        params.leftMargin = dp(52);
        params.rightMargin = dp(52);
        if (gravity == Gravity.TOP) params.topMargin = dp(inset);
        else params.bottomMargin = dp(inset);
        panel.addView(view, params);
    }

    private void showMood() {
        options.clear();
        for (int index : PICKS[mood]) {
            CricketCatalog.Clip clip = CLIPS[index];
            if (installed.test(clip)) options.add(clip);
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (CricketCatalog.Clip clip : options) seen.add(clip.app + "\n" + clip.url);
        for (int i = 0; i < customPicks.length; i++) {
            if (mood != MOODS.length - 1 && i != mood) continue;
            for (CricketCatalog.Clip clip : customPicks[i].items()) {
                if (seen.add(clip.app + "\n" + clip.url)) options.add(clip);
            }
        }
        selected = -1;
        cardTitle.setText(MOODS[mood]);
        cardDetail.setText(options.isEmpty() ? "No supported apps installed" : options.size() + " picks");
        card.setContentDescription(MOODS[mood] + ", " + cardDetail.getText());
        detail.setText((mood + 1) + " / " + MOODS.length + "  ·  Cricket & more");
        countdownBar.setVisibility(View.INVISIBLE);
        tintCard(MOOD_COLORS[mood]);
        fan.removeAllViews();
        for (int i = 0; i < Math.min(3, options.size()); i++) {
            CricketCatalog.Clip clip = options.get(i);
            addPoster(clip, (i - 1) * 48, (i - 1) * 12);
        }
        if (!options.isEmpty()) showArtwork(options.get(0), true);
    }

    private void skipMood(int direction) {
        cancelRound();
        card.animate().cancel();
        mood = direction < 0 ? deck.previous(mood) : deck.next(mood);
        showMood();
    }

    private void spin() {
        if (options.isEmpty()) return;
        cancelRound();
        selected = deck.pick(options.size(), selected);
        spinning = true;
        reelStep = 0;
        rollFrame();
    }

    private void rollFrame() {
        if (panel == null || !spinning) return;
        if (reelStep == REEL_DELAYS.length) {
            spinning = false;
            showPick(options.get(selected));
            countdownEnd = SystemClock.elapsedRealtime() + 3000L;
            countdownBar.setVisibility(View.VISIBLE);
            count();
            return;
        }
        showPick(options.get((selected + reelStep + 1) % options.size()));
        cardDetail.setText("Picking a title…");
        panel.postDelayed(reelTick, REEL_DELAYS[reelStep++]);
    }

    private void showPick(CricketCatalog.Clip clip) {
        fan.removeAllViews();
        addPoster(clip, 0, 0);
        cardTitle.setText(clip.title);
        tintCard(clip.color);
        showArtwork(clip, !spinning);
    }

    private void count() {
        if (panel == null || countdownEnd == 0) return;
        int remaining = (int) (countdownEnd - SystemClock.elapsedRealtime());
        if (remaining <= 0) {
            openPick();
            return;
        }
        countdownBar.setProgress(remaining);
        cardDetail.setText("Opening in " + ((remaining + 999) / 1000) + "  ·  ← Cancel  → Spin again");
        panel.postDelayed(countTick, 60);
    }

    private void openPick() {
        if (selected < 0 || selected >= options.size()) return;
        CricketCatalog.Clip clip = options.get(selected);
        cancelRound();
        cardDetail.setText(clip.player);
        watch.accept(clip);
    }

    private void cancelRound() {
        if (panel != null) {
            panel.removeCallbacks(reelTick);
            panel.removeCallbacks(countTick);
        }
        if (countdownBar != null) countdownBar.setVisibility(View.INVISIBLE);
        spinning = false;
        countdownEnd = 0;
    }

    private void showArtwork(CricketCatalog.Clip clip, boolean animate) {
        if (clip.photo == 0) backdrop.clear(clip.color);
        else backdrop.show(art.photo(clip.photo == 1 ? 11 : clip.photo), animate);
    }

    private void addPoster(CricketCatalog.Clip clip, int offset, int rotation) {
        View poster;
        android.graphics.Bitmap cover = art.moviePoster(clip);
        boolean landscapeCover = cover != null && cover.getWidth() > cover.getHeight();
        if (cover == null && clip.photo != 0) cover = art.photo(clip.photo);
        if (cover == null) {
            TextView film = text(12, Gravity.CENTER);
            film.setText(clip.title);
            film.setMaxLines(4);
            film.setPadding(dp(8), dp(8), dp(8), dp(8));
            GradientDrawable ink = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{clip.color & 0x00ffffff | 0x98000000, 0xff111420});
            ink.setCornerRadius(dp(8));
            ink.setStroke(dp(1), clip.color);
            film.setBackground(ink);
            poster = film;
        } else {
            ImageView image = new ImageView(activity);
            image.setImageBitmap(cover);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(round(0xff283442, dp(8)));
            image.setClipToOutline(true);
            poster = image;
        }
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                dp(landscapeCover ? 142 : 91), dp(landscapeCover ? 91 : 125), Gravity.CENTER);
        fan.addView(poster, p);
        poster.setTranslationX(dp(offset));
        poster.setRotation(rotation);
    }

    private void tintCard(int color) {
        GradientDrawable fill = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{color & 0x00ffffff | 0x95000000, 0xff0b121c});
        fill.setCornerRadius(dp(23));
        fill.setStroke(dp(1), color);
        card.setBackground(fill);
        countdownBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(color);
        result.setCornerRadius(radius);
        return result;
    }

    private TextView text(int size, int gravity) {
        TextView view = new TextView(activity);
        view.setTypeface(activity.getResources().getFont(R.font.outfit_medium));
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setGravity(gravity);
        view.setIncludeFontPadding(false);
        view.setMaxLines(2);
        return view;
    }

    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
}
