package org.gringene.colourclock;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;

public class ClockActivity extends Activity {

    ColourClock mClock;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClock = new ColourClock(this);
        setContentView(mClock);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("org.gringene.colourclock", "Resuming...");
        mClock.startTick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("org.gringene.colourclock", "Pausing...");
        mClock.stopTick();
    }
}