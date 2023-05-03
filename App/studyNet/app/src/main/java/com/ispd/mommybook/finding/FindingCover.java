package com.ispd.mommybook.finding;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.imageclassifier.ImageClassifierManager;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;

import static com.ispd.mommybook.MainHandlerMessages.COVER_GUIDE_ON;
import static com.ispd.mommybook.MainHandlerMessages.FINDING_COVER_DONE;
import static com.ispd.mommybook.MainHandlerMessages.FINDING_COVER_START;

public class FindingCover {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Context mContext = null;

    private Handler mMainHandler = null;

    private int gCameraPreviewWidth;
    private int gCameraPreviewHeight;

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    private ImageClassifierManager mImageClassifierManager = null;
    private Mat mCameraPreviewMat = null;
    private float mCropDatas[];

    private boolean mProcessRunning = false;
    private boolean mFindProcessDone = false;
    private int mFindCount = 0;

    public FindingCover(Context context, Handler handler) {
        mContext = context;

        mMainHandler = handler;

        gCameraPreviewWidth = MainActivity.gCameraPreviewWidth;
        gCameraPreviewHeight = MainActivity.gCameraPreviewHeight;

        mHandlerThread = new HandlerThread("FindingCover");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        String modelPath = "frozen_mobilenet_v2_cover.tflite";
        String labelPath = "labels_cover.txt";
        mImageClassifierManager = new ImageClassifierManager(mContext, 4,
                modelPath, labelPath);

        mCropDatas = new float[]
                    {gCameraPreviewWidth/4.f, gCameraPreviewHeight/5.f,
                    gCameraPreviewWidth/4.f * 3.f, gCameraPreviewHeight/5.f,
                    gCameraPreviewWidth/4.f, gCameraPreviewHeight/5.f * 4.f,
                    gCameraPreviewWidth/4.f * 3.f, gCameraPreviewHeight/5.f * 4.f};
    }

    public void RunProcess(Mat previewMat) {

        if( mProcessRunning == true || mFindProcessDone == true ) {
            LOGGER.d("mProcessRunning");
            return;
        }

        mProcessRunning = true;

        mCameraPreviewMat = previewMat.clone();
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                //image classification
                mImageClassifierManager.RunInference(mCameraPreviewMat, mCropDatas, true, false);
                int pageIndex = mImageClassifierManager.GetIndex();
                float pageScore = mImageClassifierManager.GetScore();

                if (pageIndex != -1) {
                    LOGGER.d("pageIndex : " + pageIndex+", pageScore : "+pageScore);

                    if(pageIndex != 3 && pageScore > 0.99f) {
                        mMainHandler.sendEmptyMessage(FINDING_COVER_DONE);
                    }
                    else {
                        mMainHandler.sendEmptyMessage(COVER_GUIDE_ON);
                        //ResetProcess();
                    }
//                    JniController.setCurrentBookInfo(0, pageIndex);
//                    PreviewDrawFixCurve.SetCurrentBookInfo(0, pageIndex);
                }

                LOGGER.d("mFindProcessDone : " + mFindProcessDone+", mFindCount : "+mFindCount);

                if( mFindProcessDone == false ) {
                    mFindCount++;
                }

                if ( mFindCount >= 0 ) {
                    mFindProcessDone = true;
                }

                mProcessRunning = false;

//                mHandler.post(this);
            }
        });
    }

    public void ResetProcess() {
        mFindCount = 0;
        mFindProcessDone = false;
    }

    public int GetCoverIndex() {
        if( mImageClassifierManager != null ) {
            return mImageClassifierManager.GetIndex();
        }
        return -1;
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
