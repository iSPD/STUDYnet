package com.ispd.mommybook.imageprocess;

import android.os.Handler;
import android.os.HandlerThread;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.preview.PreviewRendererImpl;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;

public class ImageProcessPDAlignment {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    public static int EDGE_DETECT_MODE = 0;
    public static int ALIGNEMENT_MODE = 1;

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    private Mat mInputMat = null;
    private Mat mResultMat = null;

    private int mCompareIndex;

    private int mCanny1;
    private int mCanny2;

    private int mValue1;
    private int mValue2;
    private int mValue3;

    private float mMovingOnOff;

    private byte []mRGBAResultByte = null;

    private boolean mProcessDone = true;

    private int mAlignmentMode = EDGE_DETECT_MODE;
    private int mDebugOption = 2;//2;//0;
    private int mCurveFixOn = 1;

    public ImageProcessPDAlignment() {

        mHandlerThread = new HandlerThread("ImageProcessPDAlignment");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mCompareIndex = 4;
        mCanny1 = 10000;
        mCanny2 = 5000;

        mValue1 = 3;
        mValue2 = 1;
        mValue3 = 2;

        mMovingOnOff = 0.f;

        mRGBAResultByte = new byte[MainActivity.gCameraPreviewWidth * MainActivity.gCameraPreviewHeight * 4];
    }

    public void ImageAlignment(Mat inputMat) {

        if( mProcessDone == false ) {
            LOGGER.d("Processing");
            return;
        }

        mProcessDone = false;

        mHandler.post(new Runnable() {
            @Override
            public void run() {

                LOGGER.d("Start Process");

                mInputMat = inputMat.clone();

                if( mResultMat == null ) {
                    mResultMat = new Mat(mInputMat.rows(), mInputMat.cols(), mInputMat.type());
                }

                double startTime = System.currentTimeMillis();
                //JniController.imageAlignment(mInputMat.getNativeObjAddr(), mResultMat.getNativeObjAddr(), mCompareIndex, mCanny1, mCanny2, mValue1, mValue2, mValue3, mMovingOnOff);
                if( mAlignmentMode == EDGE_DETECT_MODE ) {
                    JniController.detectBookEdge(mInputMat.getNativeObjAddr(), mResultMat.getNativeObjAddr());
                }
                else if( mAlignmentMode == ALIGNEMENT_MODE ) {
                    JniController.detectBookEdgeAndPDAlignment(mInputMat.getNativeObjAddr(), mResultMat.getNativeObjAddr(), mDebugOption, mCurveFixOn);
                }

                double endTime = System.currentTimeMillis();
                LOGGER.d("AlignmentTime : "+(endTime-startTime));

                mResultMat.get(0, 0, mRGBAResultByte);
                PreviewRendererImpl.copyDebugData(mRGBAResultByte);

                mProcessDone = true;
                mInputMat.release();
                mResultMat.release();
            }
        });
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

    public void SetAlignmentMode(int mode) {
        mAlignmentMode = mode;
    }

    public int GetDebugMode() {
        return mDebugOption;
    }

    public void SetDebugMode(int mode) {
        mDebugOption = mode;
    }

    public void SetCurveFixOn(int onOff) {
        mCurveFixOn = onOff;
    }
}
