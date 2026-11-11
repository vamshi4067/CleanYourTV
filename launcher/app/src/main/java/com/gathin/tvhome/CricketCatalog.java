package com.gathin.tvhome;

final class CricketCatalog {
    static final class Clip {
        final int photo, color;
        final String player, title, url, app;

        Clip(int photo, int color, String player, String title, String youtube) {
            this(photo, color, player, title, "https://www.youtube.com/watch?v=" + youtube, "com.google.android.youtube.tv");
        }

        Clip(int photo, int color, String player, String title, String url, String app) {
            this.photo = photo;
            this.color = color;
            this.player = player;
            this.title = title;
            this.url = url;
            this.app = app;
        }
    }

    private static final String YOUTUBE = "com.google.android.youtube.tv";
    private static final String NETFLIX = "com.netflix.ninja";
    private static final String PRIME = "com.amazon.amazonvideo.livingroom";

    static final Clip[] CLIPS = {
        new Clip(1, 0xfff78854, "Virat Kohli", "World number one", "lvtEVsiwbZw"),
        new Clip(4, 0xff83bcff, "Virat Kohli", "A Test hundred in Perth", "IEQixSBYTf0"),
        new Clip(8, 0xff77cfe1, "MS Dhoni", "The incredible MS Dhoni", "dIpaY_1aoUw"),
        new Clip(6, 0xffe4bf62, "AB de Villiers", "South Africa’s Mr 360", "5rKzWn_BizQ"),
        new Clip(9, 0xfff46460, "MS Dhoni & Virat Kohli", "Over to you, Virat", "iO5LjrQaN9s"),
        new Clip(5, 0xff76d8a9, "AB de Villiers", "South Africa’s run machine", "KDbd568iJZc"),
        new Clip(3, 0xffa7b9ff, "Match highlights", "Australia vs India · WTC Final", "gTo-lPOGPdg"),
        new Clip(2, 0xffebba82, "Match highlights", "Australia vs India · Perth", "pmxfvy3SImg"),
        new Clip(10, 0xffe3a471, "Film · Prime Video", "M.S. Dhoni: The Untold Story",
                "https://app.primevideo.com/detail?gti=amzn1.dv.gti.e28427d2-ce84-4356-ace8-855530420526", PRIME),
        new Clip(4, 0xffe9b761, "Film · Netflix", "83", "https://www.netflix.com/title/81144147", NETFLIX),
        new Clip(8, 0xffc391e6, "Film · Netflix", "Jersey", "https://www.netflix.com/title/81350340", NETFLIX),
        new Clip(1, 0xffe87e67, "Docuseries · Netflix", "The Greatest Rivalry: India vs Pakistan",
                "https://www.netflix.com/title/81500800", NETFLIX),
        new Clip(6, 0xff71b6fa, "Docuseries · Prime Video", "The Test",
                "https://app.primevideo.com/detail?gti=amzn1.dv.gti.bcd3587c-f6a0-4c9b-9000-ccf6b609ec45", PRIME),
        new Clip(9, 0xff80ceef, "YouTube channel", "ICC", "https://www.youtube.com/@ICC", YOUTUBE),
        new Clip(2, 0xffbadb7d, "YouTube channel", "cricket.com.au", "https://www.youtube.com/@cricketcomau", YOUTUBE),
        new Clip(7, 0xffed7777, "YouTube channel", "Royal Challengers Bengaluru",
                "https://www.youtube.com/@royalchallengersbengaluru", YOUTUBE),
        new Clip(12, 0xff78a8ff, "Official YouTube clips", "The Big Bang Theory", "U7yW6f7-DDk"),
        new Clip(13, 0xffd8a1ff, "Official YouTube clips", "Friends", "XvoU5QfU12g"),
        new Clip(0, 0xffe59464, "Free episodes · Tubi", "MasterChef Canada",
                "https://tubitv.com/series/300005713/masterchef-canada", "com.tubitv"),
        new Clip(0, 0xffeaa980, "Telugu · Netflix", "Hi Nanna",
                "https://www.netflix.com/title/81682028", NETFLIX),
        new Clip(0, 0xffe96f98, "Telugu · Netflix", "Tillu Square",
                "https://www.netflix.com/title/81671559", NETFLIX),
        new Clip(0, 0xffd2a271, "Telugu · Prime Video", "Sita Ramam",
                "https://app.primevideo.com/detail?gti=amzn1.dv.gti.0f68d95c-fc37-4a3a-b08a-f567c022a2d8", PRIME),
        new Clip(0, 0xffda805b, "Telugu · Prime Video", "Pushpa: The Rise",
                "https://app.primevideo.com/detail?gti=amzn1.dv.gti.4e7ad95a-3396-485c-9d74-11cf25b85f02", PRIME),
        new Clip(0, 0xff89c9a9, "Hindi · Netflix", "Laapataa Ladies",
                "https://www.netflix.com/title/81760391", NETFLIX),
        new Clip(0, 0xffe27d60, "Hindi · Netflix", "Jawan: Extended Cut",
                "https://www.netflix.com/title/81493411", NETFLIX),
        new Clip(0, 0xffd8a174, "Hindi · Prime Video", "Sardar Udham",
                "https://app.primevideo.com/detail?gti=amzn1.dv.gti.e3418f6a-f850-4b8c-b61f-035e9a4171b2", PRIME),
        new Clip(0, 0xffea8aa9, "Hindi · Prime Video", "Rocky Aur Rani Kii Prem Kahaani",
                "https://app.primevideo.com/detail?gti=amzn1.dv.gti.333f80ac-c7b8-4963-9b70-3d48c9031ec4", PRIME)
    };

    static final int[] FAVOURITES = {0, 3, 2, 16, 17, 18};
    static final String[] MOODS = {
        "Cricket", "Telugu movies", "Hindi movies", "YouTube channels", "Anything at all"
    };
    static final int[] MOOD_COLORS = {0xff83bcff, 0xfff49c6c, 0xffe990b4, 0xff76d8a9, 0xffbbabf6};
    // Keep existing category storage stable when the visible categories are consolidated.
    static final String[] CUSTOM_PICK_KEYS = {"cricket", "2", "3", "6", "7"};
    static final int[][] PICKS = {
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
        {19, 20, 21, 22}, {23, 24, 25, 26}, {13, 14, 15},
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 19, 20, 21, 22, 23, 24, 25, 26}
    };

    private CricketCatalog() { }
}
