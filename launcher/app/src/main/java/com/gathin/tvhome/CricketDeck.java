package com.gathin.tvhome;

import java.util.Random;

final class CricketDeck {
    private final int size;
    private final Random random;

    CricketDeck(int size, Random random) {
        this.size = size;
        this.random = random;
    }

    int next(int position) {
        return (position + 1) % size;
    }

    int previous(int position) {
        return Math.floorMod(position - 1, size);
    }

    int pick(int count, int previous) {
        if (count < 2 || previous < 0 || previous >= count) return random.nextInt(count);
        int choice = random.nextInt(count - 1);
        return choice < previous ? choice : choice + 1;
    }
}
