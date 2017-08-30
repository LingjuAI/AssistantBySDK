package com.lingju.assistant.view.wave;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;

import com.lingju.assistant.entity.Random;


public class PointFlake {
    private static final float ANGE_RANGE = 0.1f;
    private static final float HALF_ANGLE_RANGE = ANGE_RANGE / 2f;
    private static final float HALF_PI = (float) Math.PI / 2f;
    private static final float ANGLE_SEED = 25f;
    private static final float ANGLE_DIVISOR = 10f;
    private static final float INCREMENT_LOWER = 1f;
    private static final float INCREMENT_UPPER = 3f;
    private static final float FLAKE_SIZE_LOWER = 3f;
    private static final float FLAKE_SIZE_UPPER = 8f;

    private final Random random;
    private final Point position;
    private float angle;
    private final float increment;
    private final float flakeSize;
    private final Paint paint;
    private int boundaryLeft;
    private int boundaryTop;
    private int boundaryRight;
    private int boundaryBottom;

    public static PointFlake create(int left, int top, int right, int bottom, Paint paint) {
        Random random = new Random();
        int x = left;
        int y = (top+bottom)/2;
        Point position = new Point(x, y);
        float angle = random.getRandom(HALF_PI/5,HALF_PI/5*4);
        float increment = random.getRandom(INCREMENT_LOWER, INCREMENT_UPPER);
        float flakeSize = random.getRandom(FLAKE_SIZE_LOWER, FLAKE_SIZE_UPPER);
        PointFlake pointFlake = new PointFlake(random, position, angle, increment, flakeSize, paint);
        pointFlake.setBoundaries(left, top, right, bottom);
        return pointFlake;
    }

    private void setBoundaries(int left, int top, int right, int bottom) {
        boundaryLeft = left;
        boundaryTop = top;
        boundaryRight = right;
        boundaryBottom = bottom;
    }

    PointFlake(Random random, Point position, float angle, float increment, float flakeSize, Paint paint) {
        this.random = random;
        this.position = position;
        this.angle = angle;
        this.increment = increment;
        this.flakeSize = flakeSize;
        this.paint = paint;
    }

    private boolean xPositive = true;
    private boolean yPositive = true;

    private void move() {

        if(position.x >= boundaryRight ) {
            xPositive = false;
        }else if(position.x <= boundaryLeft) {
            xPositive = true;
        }

        if(position.y >= boundaryBottom) {
            yPositive = false;
        }else if(position.y <= boundaryTop) {
            yPositive = true;
        }
        double x;
        double y;
        if(xPositive){
            x = position.x + (increment * Math.sin(angle));
        }else {
            x = position.x - (increment * Math.sin(angle));
        }

        if(yPositive){
            y = position.y + (increment * Math.cos(angle));
        }else {
            y = position.y - (increment * Math.cos(angle));
        }

        position.set((int) x, (int) y);

    }

    public void draw(Canvas canvas) {
        move();
        canvas.drawCircle(position.x, position.y, flakeSize, paint);
    }
}
