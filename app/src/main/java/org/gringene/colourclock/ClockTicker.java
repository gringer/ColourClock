package org.gringene.colourclock;

import android.util.Log;

import java.util.TimerTask;

/**
 * Created by gringer on 17/07/14.
 */
public class ClockTicker implements Runnable {
    private ColourClock mClock;
    public ClockTicker(ColourClock tClock) {
        mClock = tClock;
    }

    public void run() {
        mClock.updateTime();
    }

    public void pause(){
    }

    public void resume(){
    }
}
