package com.ispd.mommybook.areacheck;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.imageclassifier.ImageClassifierManager;
import com.ispd.mommybook.preview.PreviewDrawFixCurve;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;

import static com.ispd.mommybook.MainHandlerMessages.COVER_GUIDE_ON;
import static com.ispd.mommybook.MainHandlerMessages.FINDING_COVER_START;

/**
 * AreaCheckCover
 *
 * @author Daniel
 * @version 1.0
 */

//원래는 표지 Edge 인식을 해야 하나 표지 인식으로 대체함. 나중에 구현 필요
public class AreaCheckCover {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Context mContext = null;

    private Handler mMainHandler = null;

    private int gCameraPreviewWidth;
    private int gCameraPreviewHeight;

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    private ImageClassifierManager mImageClassifierManager = null;
    private boolean mCameraPreviewOn = false;
    private Mat mCameraPreviewMat = null;
    private float mCropDatas[];

    private boolean mStopProcess = false;

    public AreaCheckCover(Context context, Handler handler) {

        mContext = context;

        mMainHandler = handler;

        gCameraPreviewWidth = MainActivity.gCameraPreviewWidth;
        gCameraPreviewHeight = MainActivity.gCameraPreviewHeight;

        mHandlerThread = new HandlerThread("AreaCheckCover");
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

        runProcess();
    }

    private void runProcess() {

        mHandler.post(new Runnable() {
            @Override
            public void run() {

                LOGGER.d("runProcess : "+mCameraPreviewOn);

                if( mStopProcess == true ) {
                    return;
                }

                if( mCameraPreviewOn == true ) {
                    mCameraPreviewOn = false;

                    //image classification
                    mImageClassifierManager.RunInference(mCameraPreviewMat, mCropDatas, true, false);
                    int pageIndex = mImageClassifierManager.GetIndex();
                    float pageScore = mImageClassifierManager.GetScore();

                    if (pageIndex != -1) {
                        LOGGER.d("pageIndex : " + pageIndex+", pageScore : "+pageScore);

                        if(pageIndex != 3 && pageScore > 0.99f) {
                            mMainHandler.sendEmptyMessage(FINDING_COVER_START);
                        }
                        else {
                            mMainHandler.sendEmptyMessage(COVER_GUIDE_ON);
                        }
//                    JniController.setCurrentBookInfo(0, pageIndex);
//                    PreviewDrawFixCurve.SetCurrentBookInfo(0, pageIndex);
                    }

                    mHandler.postDelayed(this, 33);
                }
                else {
                    mHandler.postDelayed(this, 33);
                }
            }
        });
    }

    public void SetPreviewData(Mat previewMat) {
        mCameraPreviewMat = previewMat.clone();
        mCameraPreviewOn = true;
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
