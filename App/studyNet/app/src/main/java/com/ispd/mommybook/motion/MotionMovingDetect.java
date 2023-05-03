package com.ispd.mommybook.motion;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.widget.ImageView;

import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.R;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import static com.ispd.mommybook.MainHandlerMessages.DEBUG_VIEW;
import static com.ispd.mommybook.MainHandlerMessages.FINDING_PAGE_INIT;
import static com.ispd.mommybook.MainHandlerMessages.FINDING_PAGE_START;
import static com.ispd.mommybook.MainHandlerMessages.MOTION_MOVING_DETECTED;
import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.*;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.*;

/**
 * MotionMovingDetect
 *
 * @author Daniel
 * @version 1.0
 */
public class MotionMovingDetect {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Handler mMainHandler = null;

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    private int mResizeRate = 2;

    Mat mCropInputMat;

    private Mat mPreMat = null;
    private Mat mDiffMat = null;

    private boolean mMoveRunning = false;
    private boolean mMoveCheckEnd = true;

    private float mThresholdValue = 2.5f;
    private int mThresholdSensValue = 15;

    private float mPercentNoZero = 0.0f;

    private movingAverage mMovingAverate;

    private Bitmap mRgbBitmap = null;

    /**
     * MotionMovingDetect constructure
     *
     */
    public MotionMovingDetect(Handler handler) {
        mHandlerThread = new HandlerThread("MotionMovingDetect");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mMainHandler = handler;
    }

    /**
     * MotionMovingDetect
     *
     * @param  inputMat Camera Preview Matrix(OpenCV)
     * @param cropDatas Crop datas(for warpPerspective)
     */
    private Mat preProcessInputMat(Mat inputMat, float []cropDatas)
    {
        Mat inputCloneMat = inputMat.clone();
        Mat resultMat = new Mat();

        cvtColor(inputCloneMat, inputCloneMat, COLOR_RGBA2GRAY);
        blur(inputCloneMat, inputCloneMat, new Size(5, 5));

        Mat resizeInputMat = new Mat();
        resize(inputCloneMat, resizeInputMat,
                new Size(inputCloneMat.cols()/mResizeRate,
                        inputCloneMat.rows()/mResizeRate));

        Mat src_mat=new Mat(4,1, CvType.CV_32FC2);
        Mat dst_mat=new Mat(4,1, CvType.CV_32FC2);

        float width = resizeInputMat.cols();
        float height = resizeInputMat.rows();

        src_mat.put(0, 0, cropDatas[0]/mResizeRate, cropDatas[1]/mResizeRate,
                                        cropDatas[2]/mResizeRate, cropDatas[3]/mResizeRate,
                                        cropDatas[4]/mResizeRate, cropDatas[5]/mResizeRate,
                                        cropDatas[6]/mResizeRate, cropDatas[7]/mResizeRate);
        dst_mat.put(0, 0, 0, 0, width, 0, 0, height, width, height);

        Mat perspectiveTransform= getPerspectiveTransform(src_mat, dst_mat);

        warpPerspective(resizeInputMat, resultMat, perspectiveTransform, resizeInputMat.size());

        resizeInputMat.release();
        inputCloneMat.release();
        return resultMat;
    }

    /**
     * Check moving for Camera Preview
     *
     * @param  inputMat Camera Preview Matrix(OpenCV)
     * @param cropDatas Crop datas(for warpPerspective)
     */
    public void CheckMoving(Mat inputMat, float []cropDatas)
    {
        if( mMovingAverate == null ) {
            mMovingAverate = new movingAverage();
        }

        if( cropDatas[0] == -1 ) {
            mThresholdValue = 300.0f;
            mThresholdSensValue = 10;
        }
        else {
            mThresholdValue = 4.0f;
            mThresholdSensValue = 30;
        }

        if( mMoveCheckEnd == false ) {
            return;
        }

        mMoveCheckEnd = false;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final long startTime = SystemClock.uptimeMillis();

                if( inputMat != null ) {

                    if( cropDatas[0] == -1 ) {
                        mCropInputMat = inputMat.clone();
                        cvtColor(mCropInputMat, mCropInputMat, COLOR_RGBA2GRAY);
                        blur(mCropInputMat, mCropInputMat, new Size(3, 3));
                    }
                    else {
                        mCropInputMat = preProcessInputMat(inputMat, cropDatas);
                    }

                    if( cropDatas[0] == -1 ) {
                        //imwrite("/sdcard/studyNet/DEBUG/moving/mCropInputMat-" + cropDatas[1] + ".jpg", mCropInputMat);
                    }

                    if( mPreMat == null ) {
                        if( cropDatas[0] == -1 ) {
                            mPreMat = mCropInputMat.clone();
                        }
                        else {
                            mPreMat = Mat.zeros(mCropInputMat.rows(), mCropInputMat.cols(), mCropInputMat.type());
                        }
                    }

                    if( mDiffMat == null ) {
                        mDiffMat = new Mat();
                    }

                    absdiff(mCropInputMat, mPreMat, mDiffMat);
                    threshold(mDiffMat, mDiffMat, mThresholdSensValue, 255, THRESH_BINARY);

                    int noZero = countNonZero(mDiffMat);
                    mPercentNoZero = ((float) noZero / (float) (mDiffMat.cols() * mDiffMat.rows())) * 100.0f;
                    LOGGER.d("mPercentNoZero1 : "+mPercentNoZero);

                    if( cropDatas[0] == -1 ) {
                        LOGGER.d(cropDatas[1]+"mPercentNoZero1 : "+mPercentNoZero);
                    }
                    else {
                        mPercentNoZero = mMovingAverate.getAverage(mPercentNoZero);
                        LOGGER.d("mPercentNoZero2 : " + mPercentNoZero);
                    }

                    mPreMat = mCropInputMat.clone();

                    if (mPercentNoZero > mThresholdValue) {
                        mMoveRunning = true;

                        if( cropDatas[0] != -1 ) {
                            mMainHandler.sendEmptyMessage(MOTION_MOVING_DETECTED);
                        }
                        else {
//                            if(mRgbBitmap == null) {
//                                mRgbBitmap = Bitmap.createBitmap(
//                                        mDiffMat.cols(), mDiffMat.rows(), Bitmap.Config.ARGB_8888);
//                            }
//                            Mat debugMat = new Mat();
//                            cvtColor(mDiffMat, debugMat, COLOR_GRAY2RGBA);
//
//                            Message msg = new Message();
//                            Utils.matToBitmap(debugMat, mRgbBitmap);
//                            msg.what = DEBUG_VIEW;
//                            msg.obj = (Bitmap)mRgbBitmap;
//                            msg.arg1 = 1;
//
//                            mMainHandler.sendMessage(msg);
                        }
                    } else {
                        mMoveRunning = false;

                        if(cropDatas[0] == -1) {
                            Message msg = new Message();
                            msg.what = DEBUG_VIEW;
                            msg.arg1 = 0;

                            mMainHandler.sendMessage(msg);
                        }
                    }
                }
                else
                {
                    mMoveRunning = true;
                    if( cropDatas[0] != -1 ) {
                        mMainHandler.sendEmptyMessage(MOTION_MOVING_DETECTED);
                    }
                }

                mMoveCheckEnd = true;

                if( cropDatas[0] == -1 ) {
                    LOGGER.d("moving time " + (SystemClock.uptimeMillis() - startTime));
                }
            }
        });
    }

    /**
     * get result for moving status
     *
     */
    public boolean GetMovingRunning()
    {
        LOGGER.d("mMoveRunning : "+mMoveRunning);
        return mMoveRunning;
    }

    public float GetMovingValue() {
        return mPercentNoZero;
    }

    public void stop() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
    }

    /**
     * Filter for moving(Inner Class)
     *
     */
    public class movingAverage {

        private final UtilsLogger LOGGER = new UtilsLogger();

        private int mSampleCount = 10;
        private float []mInputValues;
        private int mInCount = 0;

        /**
         * movingAverage constructure
         *
         */
        public movingAverage()
        {
            mInputValues = new float[mSampleCount];
            for(int i = 0; i < mSampleCount; i++)
            {
                mInputValues[i] = 0.f;
            }

            mInCount = 0;
        }

        /**
         * Get average for input value
         *
         * @param input
         */
        public float getAverage(float input)
        {
            mInputValues[mInCount] = input;

            mInCount++;
            if( mInCount >= mSampleCount )
            {
                mInCount = 0;
            }

            float sumInput = 0.f;
            LOGGER.d("input : "+input);
            for(int i = 0; i < mSampleCount; i++)
            {
                LOGGER.d("mInputValues"+"["+i+"] : "+mInputValues[i]);
                sumInput += mInputValues[i];
            }

            float average = sumInput / mSampleCount;
            LOGGER.d("average : "+average);

            return average;
        }
    }
}
