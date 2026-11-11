package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CricketFixtures {
    static final String SCHEDULE = "https://www.willow.tv/cricket-schedule/upcoming-series/all";
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final Pattern STRUCTURED_DATA = Pattern.compile(
            "<script\\b[^>]*type=\"application/ld\\+json\"[^>]*>(.*?)</script>", Pattern.DOTALL);
    private static final DateTimeFormatter EASTERN = DateTimeFormatter
            .ofPattern("EEE, MMM d  'at'  h:mm a z", Locale.US).withZone(ZoneId.of("America/New_York"));

    static final class Match {
        final String teams, name;
        final Instant start;
        final boolean followed;

        Match(String home, String away, String name, Instant start) {
            this.teams = home + " vs " + away;
            this.name = name;
            this.start = start;
            followed = follows(home) || follows(away);
        }

        String localStart() { return EASTERN.format(start); }
    }

    static Match fetch(Instant now) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(SCHEDULE).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try (InputStream input = connection.getInputStream();
             ByteArrayOutputStream body = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (body.size() + read > MAX_BYTES) throw new IOException("Willow schedule is too large");
                body.write(buffer, 0, read);
            }
            return next(new String(body.toByteArray(), StandardCharsets.UTF_8), now);
        } finally {
            connection.disconnect();
        }
    }

    static Match next(String html, Instant now) throws JSONException {
        Matcher scripts = STRUCTURED_DATA.matcher(html);
        Match next = null;
        boolean foundSchedule = false;
        while (scripts.find()) {
            String script = scripts.group(1).trim();
            if (!script.startsWith("{")) continue;
            JSONObject page = new JSONObject(script);
            if (!"WebPage".equals(page.optString("@type"))) continue;
            JSONObject list = page;
            // Willow wraps the fixtures in nested schema.org WebPage and ItemList objects.
            while (list != null) {
                JSONArray items = list.optJSONArray("itemListElement");
                if (items != null) {
                    foundSchedule = true;
                    for (int i = 0; i < items.length(); i++) {
                        Match match = read(items.optJSONObject(i));
                        if (match != null && !match.start.isBefore(now)
                                && (next == null || (match.followed && !next.followed)
                                || (match.followed == next.followed && match.start.isBefore(next.start)))) {
                            next = match;
                        }
                    }
                    break;
                }
                JSONObject entity = list.optJSONObject("mainEntity");
                list = entity != null ? entity : list.optJSONObject("itemListElement");
            }
        }
        if (!foundSchedule) throw new JSONException("Willow schedule data was missing");
        return next;
    }

    private static Match read(JSONObject event) {
        if (event == null || !"SportsEvent".equals(event.optString("@type"))
                || event.optString("eventStatus").endsWith("EventCancelled")
                || event.optString("eventStatus").endsWith("EventPostponed")) return null;
        try {
            JSONArray teams = event.getJSONArray("competitor");
            String home = teams.getJSONObject(0).getString("name").trim();
            String away = teams.getJSONObject(1).getString("name").trim();
            String name = event.getString("name").trim();
            if (home.isEmpty() || away.isEmpty() || name.isEmpty()) return null;
            return new Match(home, away, name, Instant.parse(event.getString("startDate")));
        } catch (JSONException | DateTimeParseException unavailable) {
            return null;
        }
    }

    private static boolean follows(String team) {
        return "India".equalsIgnoreCase(team) || "India Women".equalsIgnoreCase(team)
                || "Royal Challengers Bengaluru".equalsIgnoreCase(team);
    }
}
