package com.ispd.mommybook.finding;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.imageclassifier.ImageClassifierManager;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;

import static com.ispd.mommybook.MainHandlerMessages.COVER_GUIDE_ON;
import static com.ispd.mommybook.MainHandlerMessages.FINDING_COVER_DONE;
import static com.ispd.mommybook.MainHandlerMessages.FINDING_PAGE_DONE;
import static com.ispd.mommybook.MainHandlerMessages.INNER_GUIDE_ON;

public class FindingPage {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Context mContext = null;

    private Handler mMainHandler = null;

    private int gCameraPreviewWidth;
    private int gCameraPreviewHeight;

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    private ImageClassifierManager mImageClassifierManager = null;
    private ImageClassifierManager mImageClassifierManager1 = null;
    private ImageClassifierManager mImageClassifierManager2 = null;
    private ImageClassifierManager mImageClassifierManager3 = null;

    private Mat mCameraPreviewMat = null;
    private float mCropDatas[];
    private float mNoUseData[];

    private boolean mProcessRunning = false;
    private boolean mFindProcessDone = false;
    private int mFindCount = 0;

    public FindingPage(Context context, Handler handler) {
        mContext = context;

        mMainHandler = handler;

        gCameraPreviewWidth = MainActivity.gCameraPreviewWidth;
        gCameraPreviewHeight = MainActivity.gCameraPreviewHeight;

        mHandlerThread = new HandlerThread("FindingPage");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        String modelPath = "frozen_mobilenet_v2_korean.tflite";
        String labelPath = "labels_korean.txt";
        mImageClassifierManager1 = new ImageClassifierManager(mContext, 4,
                modelPath, labelPath);

        modelPath = "frozen_mobilenet_v2_math.tflite";
        labelPath = "labels_math.txt";
        mImageClassifierManager2 = new ImageClassifierManager(mContext, 4,
                modelPath, labelPath);

        modelPath = "frozen_mobilenet_v2_english.tflite";
        labelPath = "labels_english.txt";
        mImageClassifierManager3 = new ImageClassifierManager(mContext, 4,
                modelPath, labelPath);

        mCropDatas = new float[]
                {gCameraPreviewWidth/4.f, gCameraPreviewHeight/5.f,//lt
                        gCameraPreviewWidth/4.f * 3.f, gCameraPreviewHeight/5.f,//rt
                        gCameraPreviewWidth/4.f, gCameraPreviewHeight/5.f * 4.f,//lb
                        gCameraPreviewWidth/4.f * 3.f, gCameraPreviewHeight/5.f * 4.f};//rb
        mNoUseData = new float[4];
    }

    public void RunProcess(Mat previewMat, int coverPage) {

        if( mProcessRunning == true || mFindProcessDone == true ) {
            LOGGER.d("mProcessRunning");
            return;
        }

        mProcessRunning = true;

        //korean
        if( coverPage == 0 ) {
            mImageClassifierManager = mImageClassifierManager1;
        }//math
        else if( coverPage == 1 ) {
            mImageClassifierManager = mImageClassifierManager2;
        }//english
        else if( coverPage == 2 ) {
            mImageClassifierManager = mImageClassifierManager3;
        }
        else {
            mImageClassifierManager = mImageClassifierManager3;
        }

        //getCropData : lt, rt, lb, rb
        JniController.getCropFourPoint(mCropDatas, mNoUseData);
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

                    if(pageIndex != 0 && pageScore > 0.9f) {
                        mMainHandler.sendEmptyMessage(FINDING_PAGE_DONE);
                    }
                    else {
                        mMainHandler.sendEmptyMessage(INNER_GUIDE_ON);
                        //ResetProcess();
                    }
//                    JniController.setCurrentBookInfo(0, pageIndex);
//                    PreviewDrawFixCurve.SetCurrentBookInfo(0, pageIndex);
                }

                if( mFindProcessDone == false ) {
                    mFindCount++;
                }

                if ( mFindCount >= 3 ) {
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
