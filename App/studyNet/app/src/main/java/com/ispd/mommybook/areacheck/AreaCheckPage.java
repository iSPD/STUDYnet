package com.ispd.mommybook.areacheck;

import android.os.Handler;
import android.os.HandlerThread;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.utils.UtilsLogger;

import static com.ispd.mommybook.MainHandlerMessages.FINDING_PAGE_START;
import static com.ispd.mommybook.MainHandlerMessages.INNER_GUIDE_ON;

public class AreaCheckPage {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Handler mMainHandler = null;

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    private boolean mStopProcess = false;

    public AreaCheckPage(Handler handler) {
        mMainHandler = handler;

        mHandlerThread = new HandlerThread("AreaCheckPage");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        runProcess();
    }

    public void runProcess() {

        mHandler.post(new Runnable() {
            @Override
            public void run() {

                if( mStopProcess == true ) {
                    return;
                }

                boolean status = JniController.getCheckBookLocation();
                LOGGER.d("getCheckBookLocation : "+status);

                if(status == true) {
                    mMainHandler.sendEmptyMessage(FINDING_PAGE_START);
                }
                else {
                    mMainHandler.sendEmptyMessage(INNER_GUIDE_ON);
                }

                mHandler.postDelayed(this, 33);
            }
        });
    }

    public void PauseProcess() {
        mStopProcess = true;
    }

    public void ResumeProcess() {
        if( mStopProcess == true ) {
            mStopProcess = false;
            runProcess();
        }
    }

    public void Stop() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
    }
}
