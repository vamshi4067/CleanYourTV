package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.IntConsumer;

final class CricketPickBrowser extends FrameLayout {
    private final Activity activity;
    private final CricketArt art;
    private final CricketTitleStore[] stores;
    private final IntConsumer done;
    private final TextView categoryButton, status;
    private final LinearLayout filters;
    private final RecyclerView shelf;
    private final Cards cards = new Cards();
    private final List<CricketCatalog.Clip> shown = new ArrayList<>();
    private final String[] filterNames = {"All", "Netflix", "Prime Video", "YouTube", "My additions"};
    private int category, filter;
    private AlertDialog categoryDialog;

    CricketPickBrowser(Activity activity, CricketArt art, CricketTitleStore[] stores,
                       int category, IntConsumer done) {
        super(activity);
        this.activity = activity;
        this.art = art;
        this.stores = stores;
        this.category = category;
        this.done = done;
        setBackgroundColor(0xff080c12);
        setFocusableInTouchMode(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        TextView title = label(29);
        title.setText("Add more picks");
        FrameLayout.LayoutParams heading = new LayoutParams(-2, dp(43), Gravity.TOP | Gravity.START);
        heading.leftMargin = dp(40); heading.topMargin = dp(20);
        addView(title, heading);

        LinearLayout controls = new LinearLayout(activity);
        LayoutParams controlsPosition = new LayoutParams(-2, dp(38), Gravity.TOP | Gravity.END);
        controlsPosition.rightMargin = dp(40); controlsPosition.topMargin = dp(26);
        addView(controls, controlsPosition);
        categoryButton = button("");
        controls.addView(categoryButton);
        categoryButton.setOnClickListener(v -> chooseCategory());
        TextView finish = button("Done");
        LinearLayout.LayoutParams finishPosition = new LinearLayout.LayoutParams(-2, -1);
        finishPosition.leftMargin = dp(12);
        controls.addView(finish, finishPosition);
        finish.setOnClickListener(v -> finish());

        status = label(15);
        status.setTextColor(0xffbed0df);
        LayoutParams statusPosition = new LayoutParams(-1, dp(28), Gravity.TOP);
        statusPosition.leftMargin = dp(40); statusPosition.topMargin = dp(65);
        addView(status, statusPosition);

        filters = new LinearLayout(activity);
        LayoutParams filterPosition = new LayoutParams(-1, dp(38), Gravity.TOP);
        filterPosition.leftMargin = dp(40); filterPosition.rightMargin = dp(40); filterPosition.topMargin = dp(105);
        addView(filters, filterPosition);
        for (int i = 0; i < filterNames.length; i++) {
            int choice = i;
            TextView item = button(filterNames[i]);
            LinearLayout.LayoutParams position = new LinearLayout.LayoutParams(-2, -1);
            position.rightMargin = dp(10);
            filters.addView(item, position);
            item.setOnClickListener(v -> { filter = choice; refresh(); });
            item.setOnKeyListener((v, code, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    focusFirstCard();
                    return true;
                }
                return false;
            });
        }

        shelf = new RecyclerView(activity);
        shelf.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));
        shelf.setClipToPadding(false);
        shelf.setPadding(dp(30), dp(5), dp(30), dp(5));
        shelf.setItemAnimator(null);
        shelf.setAdapter(cards);
        LayoutParams shelfPosition = new LayoutParams(-1, dp(258), Gravity.TOP);
        shelfPosition.topMargin = dp(158);
        addView(shelf, shelfPosition);

        TextView hints = label(14);
        hints.setGravity(Gravity.CENTER);
        hints.setText("← → Browse     OK Add / Remove     ↑ Filters     Back Done");
        hints.setTextColor(0xffbed0df);
        LayoutParams hintPosition = new LayoutParams(-1, dp(32), Gravity.BOTTOM);
        hintPosition.bottomMargin = dp(20);
        addView(hints, hintPosition);
        refresh();
        filters.getChildAt(0).requestFocus();
    }

    void finish() {
        if (categoryDialog != null) categoryDialog.dismiss();
        done.accept(category);
    }

    void focusBrowser() { filters.getChildAt(0).requestFocus(); }

    void dispose() {
        if (categoryDialog != null) categoryDialog.dismiss();
        shelf.setAdapter(null);
    }

    private void chooseCategory() {
        categoryDialog = new AlertDialog.Builder(activity).setTitle("Add picks to")
                .setSingleChoiceItems(CricketCatalog.MOODS, category, (dialog, choice) -> {
                    category = choice;
                    refresh();
                    dialog.dismiss();
                    categoryButton.requestFocus();
                })
                .setNegativeButton("Cancel", null).create();
        categoryDialog.show();
    }

    private void refresh() {
        categoryButton.setText("Category · " + CricketCatalog.MOODS[category]);
        LinkedHashMap<String, CricketCatalog.Clip> candidates = new LinkedHashMap<>();
        if (filter == 4) {
            for (CricketCatalog.Clip clip : stores[category].items()) candidates.put(key(clip), clip);
        } else {
            for (CricketCatalog.Clip clip : CricketDiscover.TITLES) candidates.put(key(clip), clip);
            for (CricketCatalog.Clip clip : CricketCatalog.CLIPS) candidates.putIfAbsent(key(clip), clip);
        }
        shown.clear();
        for (CricketCatalog.Clip clip : candidates.values()) {
            if (clip.app.equals("com.tubitv")) continue;
            boolean matches = filter == 0 || filter == 4
                    || filter == 1 && clip.app.equals("com.netflix.ninja")
                    || filter == 2 && clip.app.equals("com.amazon.amazonvideo.livingroom")
                    || filter == 3 && clip.app.equals("com.google.android.youtube.tv");
            if (matches) shown.add(clip);
        }
        for (int i = 0; i < filters.getChildCount(); i++) {
            ((TextView) filters.getChildAt(i)).setText((i == filter ? "✓ " : "") + filterNames[i]);
        }
        status.setText(shown.isEmpty() ? "No additions in this category yet. Choose All to find something."
                : shown.size() + " choices · Select a card to add it to " + CricketCatalog.MOODS[category]);
        cards.notifyDataSetChanged();
        shelf.scrollToPosition(0);
    }

    private void focusFirstCard() {
        if (shown.isEmpty()) return;
        focusCard(0);
    }

    private void focusCard(int position) {
        shelf.scrollToPosition(position);
        shelf.post(() -> {
            RecyclerView.ViewHolder holder = shelf.findViewHolderForAdapterPosition(position);
            if (holder != null) holder.itemView.requestFocus();
        });
    }

    private boolean included(CricketCatalog.Clip clip) {
        for (int index : CricketCatalog.PICKS[category]) {
            if (key(CricketCatalog.CLIPS[index]).equals(key(clip))) return true;
        }
        return false;
    }

    private boolean added(CricketCatalog.Clip clip) {
        for (CricketCatalog.Clip existing : stores[category].items()) {
            if (key(existing).equals(key(clip))) return true;
        }
        return false;
    }

    private void updateAction(Card holder, CricketCatalog.Clip clip) {
        holder.action.setText(included(clip) ? "✓ Already included" : added(clip) ? "✓ Added · Remove" : "+ Add");
        holder.itemView.setContentDescription(clip.title + ", " + clip.player + ", " + holder.action.getText());
    }

    private final class Cards extends RecyclerView.Adapter<Card> {
        @Override public Card onCreateViewHolder(ViewGroup parent, int type) { return new Card(); }
        @Override public int getItemCount() { return shown.size(); }
        @Override public void onBindViewHolder(Card holder, int position) {
            CricketCatalog.Clip clip = shown.get(position);
            android.graphics.Bitmap image = art.moviePoster(clip);
            if (image == null && clip.photo != 0) image = art.photo(clip.photo);
            holder.poster.setImageBitmap(image);
            holder.title.setText(clip.title);
            holder.provider.setText(clip.player);
            updateAction(holder, clip);
            holder.itemView.setOnClickListener(v -> {
                int index = holder.getBindingAdapterPosition();
                if (index == RecyclerView.NO_POSITION) return;
                if (included(clip)) {
                    Toast.makeText(activity, "Already in " + CricketCatalog.MOODS[category], Toast.LENGTH_SHORT).show();
                    return;
                }
                if (added(clip)) {
                    stores[category].remove(clip);
                    if (filter == 4) {
                        shown.remove(index);
                        cards.notifyItemRemoved(index);
                        status.setText(shown.size() + " additions in " + CricketCatalog.MOODS[category]);
                        if (shown.isEmpty()) filters.getChildAt(4).requestFocus();
                        else focusCard(Math.min(index, shown.size() - 1));
                    } else updateAction(holder, clip);
                    Toast.makeText(activity, "Removed from your picks", Toast.LENGTH_SHORT).show();
                } else {
                    stores[category].save(clip);
                    updateAction(holder, clip);
                    Toast.makeText(activity, "Added to " + CricketCatalog.MOODS[category], Toast.LENGTH_SHORT).show();
                }
            });
            holder.itemView.setOnKeyListener((v, code, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                if (code == KeyEvent.KEYCODE_DPAD_UP) { filters.getChildAt(filter).requestFocus(); return true; }
                int index = holder.getBindingAdapterPosition();
                return code == KeyEvent.KEYCODE_DPAD_DOWN
                        || code == KeyEvent.KEYCODE_DPAD_LEFT && index == 0
                        || code == KeyEvent.KEYCODE_DPAD_RIGHT && index == shown.size() - 1;
            });
        }
    }

    private final class Card extends RecyclerView.ViewHolder {
        final ImageView poster;
        final TextView title, provider, action;

        Card() {
            super(new LinearLayout(activity));
            LinearLayout card = (LinearLayout) itemView;
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setFocusable(true);
            card.setBackgroundResource(R.drawable.cricket_app_focus);
            card.setPadding(dp(9), dp(9), dp(9), dp(9));
            RecyclerView.LayoutParams position = new RecyclerView.LayoutParams(dp(194), dp(245));
            position.setMargins(dp(7), 0, dp(7), 0);
            card.setLayoutParams(position);
            poster = new ImageView(activity);
            poster.setScaleType(ImageView.ScaleType.FIT_CENTER);
            poster.setBackgroundColor(0xff162330);
            card.addView(poster, new LinearLayout.LayoutParams(-1, dp(124)));
            title = label(17);
            title.setGravity(Gravity.CENTER);
            title.setMaxLines(2);
            card.addView(title, new LinearLayout.LayoutParams(-1, 0, 1f));
            provider = label(12);
            provider.setTextColor(0xffb9ccda);
            provider.setGravity(Gravity.CENTER);
            card.addView(provider, new LinearLayout.LayoutParams(-1, dp(22)));
            action = label(15);
            action.setTextColor(0xfff1ce90);
            action.setGravity(Gravity.CENTER);
            card.addView(action, new LinearLayout.LayoutParams(-1, dp(28)));
        }
    }

    private TextView button(String text) {
        TextView button = label(15);
        button.setText(text);
        button.setPadding(dp(13), 0, dp(13), 0);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setMinHeight(dp(38));
        button.setTextColor(activity.getColorStateList(R.drawable.pill_text));
        button.setBackgroundResource(R.drawable.cricket_top_focus);
        return button;
    }

    private TextView label(int size) {
        TextView view = new TextView(activity);
        view.setTextSize(size);
        view.setTypeface(activity.getResources().getFont(R.font.outfit_medium));
        view.setTextColor(Color.WHITE);
        view.setIncludeFontPadding(false);
        return view;
    }

    private static String key(CricketCatalog.Clip clip) { return clip.app + "\n" + clip.url; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
