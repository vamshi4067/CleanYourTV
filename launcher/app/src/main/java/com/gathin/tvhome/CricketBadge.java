package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;

final class CricketBadge extends View {
    enum Mark { NETFLIX, YOUTUBE, PRIME, WILLOW, DISNEY, APPLE, HTE, HULU, AHA }

    private final Mark mark;
    private final Drawable brand;
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();

    CricketBadge(Context context, Mark mark) {
        super(context);
        this.mark = mark;
        int id;
        switch (mark) {
            case NETFLIX: id = R.drawable.cricket_mark_netflix; break;
            case YOUTUBE: id = R.drawable.cricket_mark_youtube; break;
            case APPLE: id = R.drawable.cricket_mark_apple; break;
            default: id = 0; break;
        }
        brand = id == 0 ? null : context.getDrawable(id);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        int color;
        switch (mark) {
            case NETFLIX: color = 0xffe50914; break;
            case YOUTUBE: color = 0xffff0000; break;
            case PRIME: color = 0xff00a8e1; break;
            case WILLOW: color = 0xff00b594; break;
            case HULU: color = 0xff1ce783; break;
            case AHA: color = 0xffff762d; break;
            default: color = Color.WHITE; break;
        }
        ink.setColor(color);
        if (brand != null && mark != Mark.YOUTUBE) brand.setTint(color);
        ink.setStrokeCap(Paint.Cap.ROUND);
        ink.setStrokeJoin(Paint.Join.ROUND);
        ink.setTextAlign(Paint.Align.CENTER);
    }

    @Override protected void onDraw(Canvas canvas) {
        float scale = Math.min(getWidth(), getHeight()) / 48f;
        int save = canvas.save();
        canvas.translate((getWidth() - 48 * scale) / 2, (getHeight() - 48 * scale) / 2);
        canvas.scale(scale, scale);
        if (brand != null) {
            brand.setBounds(7, 7, 41, 41);
            brand.draw(canvas);
        } else if (mark == Mark.PRIME) {
            drawSmile(canvas);
        } else {
            String logo;
            switch (mark) {
                case WILLOW: logo = "willow"; break;
                case DISNEY: logo = "Disney+"; break;
                case HTE: logo = "HTE"; break;
                case HULU: logo = "hulu"; break;
                default: logo = "aha"; break;
            }
            ink.setStyle(Paint.Style.FILL);
            ink.setTypeface(Typeface.create(mark == Mark.DISNEY ? "sans-serif-condensed" : "sans-serif-black",
                    mark == Mark.DISNEY ? Typeface.BOLD_ITALIC : Typeface.BOLD));
            ink.setTextSize(20f);
            ink.setTextSize(Math.min(20f, 20f * 45f / ink.measureText(logo)));
            canvas.drawText(logo, 24f, 24f - (ink.ascent() + ink.descent()) / 2f, ink);
        }
        canvas.restoreToCount(save);
    }

    private void drawSmile(Canvas canvas) {
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeWidth(3f);
        shape.reset();
        shape.moveTo(5, 23);
        shape.cubicTo(14, 32, 30, 34, 42, 23);
        canvas.drawPath(shape, ink);
        ink.setStrokeWidth(2.2f);
        shape.reset();
        shape.moveTo(36, 21);
        shape.quadTo(42, 18, 45, 20);
        shape.quadTo(45, 24, 42, 28);
        canvas.drawPath(shape, ink);
    }
}
