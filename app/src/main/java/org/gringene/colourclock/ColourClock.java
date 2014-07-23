package org.gringene.colourclock;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ColourClock extends View {

    private static float OUTER_POS = 16;  // position of numbers in sixteenths of circle radius
    private static float INNER_POS = 11;
    private static float NUMBER_POS = (OUTER_POS + INNER_POS) / 2f;
    private static float SEC_POS = 10;
    private static float MIN_POS = 9;
    private static float HOUR_POS = 6.5f;
    private static float HOUR_WIDTH = 0.5f; // in sixteenths of circle radius
    private static float MIN_WIDTH = 0.25f;
    private static float SEC_WIDTH = 0.125f;
    private final Paint brushes = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Time mCalendar;
    private float mHours;
    private float mMinutes;
    private float mSeconds;

    private float centreX;
    private float centreY;
    private float bandWidth;

    private Bitmap backing;
    private Canvas painting;
    boolean ticking = false;
    boolean isDrawing = false;

    private ScheduledThreadPoolExecutor tickerTimer;
    ScheduledFuture clockTicker = null;


    private boolean started = false;

    public ColourClock(Context context) {
        super(context);
        init();
    }

    public ColourClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        mCalendar = new Time();
        tickerTimer = new ScheduledThreadPoolExecutor(1);
        startTick();
    }

    protected void updateTime(){
        mCalendar.setToNow();
        int hour = mCalendar.hour;
        int min = mCalendar.minute;
        long currentMillis = System.currentTimeMillis();
        int secInt = (int) ((currentMillis % 60000) / 1000);
        float mSecFrac = (currentMillis % 1000) / 1000f;
        mSecFrac = (float) (1 - Math.sin((0.5f - mSecFrac) * Math.PI))/2;
        mSeconds = secInt + mSecFrac;
        mMinutes = min + mSeconds/60;
        mHours = hour + mMinutes/60;
        mMinutes = (float) Math.floor(mMinutes); // now that the Hour is done, ensure minutes jump
        //Log.d("org.gringene.colourclock",String.format("Updating time, now %02d:%02d:%02.2f", hour, min, mSeconds));
        if(started) {
            drawClock(painting);
            this.postInvalidate();
        }
    }

    public int getAngleColour(double theta){
        // top: red, right: yellow, bottom: [dark] green, left: blue
        theta = theta % 360;
        double h,s,v;
        h=0; s = 1; v = 1;
        if        ((theta >=   0) && (theta <  90)) {
            h = (theta * (60f / 90f)); // red to yellow
        } else if ((theta >=  90) && (theta < 180)) {
            h = ((theta - 90) * (60f / 90f)) + 60; // yellow to dark green
            v = 1 - ((theta - 90) / 180f);
        } else if ((theta >= 180) && (theta < 270)) {
            h = ((theta - 180) * (120f / 90f)) + 120; // dark green to blue
            v = ((theta - 180) / 180f) + 0.5;
        } else if ((theta >= 270) && (theta < 360)) {
            h = ((theta - 270) * (120f / 90f)) + 240; // blue to red
        }
        float[] hsvVals = {(float) h, (float) s, (float) v};
        return Color.HSVToColor(hsvVals);
    }

    public void drawNumbers(Canvas tPainting){
        brushes.setStyle(Paint.Style.FILL);
        brushes.setColor(Color.BLACK);
        brushes.setTextSize(bandWidth * 3);
        Rect b = new Rect();
        for(int i = 1; i <= 12; i++){
            String is = Integer.toString(i);
            brushes.getTextBounds(is, 0, is.length(), b);
            double angle = (i * 30 - 90) * (Math.PI/180);
            double cx = centreX + Math.cos(angle) * bandWidth * ColourClock.NUMBER_POS;
            double cy = centreY + Math.sin(angle) * bandWidth * ColourClock.NUMBER_POS + b.height()/2f;
            tPainting.drawText(String.format("%d", i), (float) cx, (float) cy, brushes);
        }
        for(int i = 0; i < 60; i++){
            double angle = (i * 6 - 90) * (Math.PI/180);
            double tel = bandWidth / 3;
            double tsl = (i % 5 == 0) ? bandWidth : bandWidth / 2f;
            double tsx = centreX + Math.cos(angle) * (bandWidth * ColourClock.INNER_POS - tel - tsl);
            double tex = centreX + Math.cos(angle) * (bandWidth * ColourClock.INNER_POS - tel);
            double tsy = centreY + Math.sin(angle) * (bandWidth * ColourClock.INNER_POS - tel - tsl);
            double tey = centreY + Math.sin(angle) * (bandWidth * ColourClock.INNER_POS - tel);
            if(i % 5 == 0){
                brushes.setStrokeWidth(bandWidth * ColourClock.MIN_WIDTH * 0.75f);
            } else {
                brushes.setStrokeWidth(bandWidth * ColourClock.SEC_WIDTH * 0.75f);
            }
            tPainting.drawLine((float) tsx, (float) tsy, (float) tex, (float) tey, brushes);
        }
    }

    public void drawClock(Canvas tPainting){
        isDrawing = true;
        float hourAng = (mHours * 30); // 360/12
        float minAng = (mMinutes * 6); // 360/60
        float secAng = (mSeconds * 6); // 360/60
        //tPainting.drawColor(Color.WHITE); // fill in background
        brushes.setStyle(Paint.Style.STROKE);
        drawCircle(ColourClock.OUTER_POS, 0.125f, Color.WHITE, Color.BLACK, tPainting); // outer face
        drawCircle(ColourClock.INNER_POS, 0.125f, Color.WHITE, Color.BLACK, tPainting); // inner face
        drawNumbers(tPainting);
        brushes.setStyle(Paint.Style.STROKE);
        drawLine(hourAng, ColourClock.HOUR_POS, ColourClock.HOUR_WIDTH, tPainting); // hour hand
        drawCircle(hourAng, ColourClock.HOUR_POS - 2, 1.5f, ColourClock.HOUR_WIDTH, getAngleColour(hourAng), Color.BLACK, tPainting); // hour circle
        drawLine(minAng, ColourClock.MIN_POS, ColourClock.MIN_WIDTH, tPainting); // minute hand
        drawCircle(minAng, ColourClock.MIN_POS - 1.25f, 1f, ColourClock.MIN_WIDTH, getAngleColour(minAng), Color.BLACK, tPainting); // minute circle
        drawLine(secAng, ColourClock.SEC_POS, ColourClock.SEC_WIDTH, tPainting); // second hand
        drawCircle(secAng, ColourClock.SEC_POS - 0.75f, 0.5f, ColourClock.SEC_WIDTH, getAngleColour(secAng), Color.BLACK, tPainting); // second circle
        drawCircle(2, 0.5f, Color.WHITE, Color.BLACK, tPainting); // centre dot
        brushes.setStrokeWidth(1);
        brushes.setStyle(Paint.Style.FILL);
        isDrawing = false;
    }

    private void drawCircle(float radiusFactor, float strokeWFactor,
                            int fillCol, int strokeCol, Canvas tPainting){
        brushes.setStrokeWidth(strokeWFactor*bandWidth);
        brushes.setColor(fillCol);
        brushes.setStyle(Paint.Style.FILL);
        tPainting.drawCircle(centreX, centreY, radiusFactor * bandWidth, brushes);
        brushes.setColor(strokeCol);
        brushes.setStyle(Paint.Style.STROKE);
        tPainting.drawCircle(centreX, centreY, radiusFactor * bandWidth, brushes);
    }

    private void drawCircle(float angle, float lengthFactor, float radiusFactor, float strokeWFactor,
                            int fillCol, int strokeCol, Canvas tPainting){
        angle = (float) ((angle-90) * Math.PI/180);
        double cx = centreX + Math.cos(angle) * lengthFactor*bandWidth;
        double cy = centreY + Math.sin(angle) * lengthFactor*bandWidth;
        brushes.setStrokeWidth(bandWidth*strokeWFactor);
        brushes.setStrokeWidth(strokeWFactor*bandWidth);
        brushes.setColor(fillCol);
        brushes.setStyle(Paint.Style.FILL);
        tPainting.drawCircle((float)cx, (float)cy, radiusFactor*bandWidth, brushes);
        brushes.setColor(strokeCol);
        brushes.setStyle(Paint.Style.STROKE);
        tPainting.drawCircle((float)cx, (float)cy, radiusFactor*bandWidth, brushes);

    }

    private void drawLine(float angle, float lengthFactor, float widthFactor, Canvas tPainting) {
        angle = (float) ((angle-90) * Math.PI/180);
        brushes.setStrokeWidth(bandWidth * widthFactor);
        tPainting.drawLine(centreX, centreY,
                (float)(centreX + Math.cos(angle) * bandWidth * lengthFactor),
                (float)(centreY + Math.sin(angle) * bandWidth * lengthFactor), brushes);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!isDrawing) {
            canvas.drawBitmap(backing, 0, 0, null);
        } else {
            this.invalidate();
        }
    }

    public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        backing = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        painting = new Canvas(backing);
        centreX = width/2f;
        centreY = height/2f;
        float clockRadius = Math.min(width - 16, height - 16) / 2f;
        bandWidth = clockRadius / 16;
        started = true;
        brushes.setTextSize(bandWidth * 2);
        brushes.setColor(Color.BLACK);
        brushes.setTextAlign(Paint.Align.CENTER);
        brushes.setStrokeCap(Paint.Cap.ROUND);
        updateTime();
    }

    public void stopTick() {
        /* try to remove all traces of the update threads and stop them from running */
        ticking = false;
        clockTicker.cancel(true);
        for(Runnable t : tickerTimer.getQueue()){
            tickerTimer.remove(t);
        }
        clockTicker = null;
    }

    public void startTick() {
        if(!ticking) {
            ticking = true;
            int refreshRate = 50;
            clockTicker = tickerTimer.scheduleWithFixedDelay(new ClockTicker(this),
                    0, refreshRate, TimeUnit.MILLISECONDS);
        }
    }
}