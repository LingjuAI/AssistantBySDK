package com.lingju.assistant.entity;

public class Random {
    private static final java.util.Random RANDOM = new java.util.Random();

    public float getRandom(float lower, float upper) {
        return lower + RANDOM.nextFloat() * (upper - lower);
    }

    public float getRandom(float upper) {
        return RANDOM.nextFloat() * upper;
    }

    public int getRandom(int upper) {
        return RANDOM.nextInt(upper);
    }

}
