package com.ispd.mommybook.imageclassifier;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import com.ispd.mommybook.camera.CameraDataManager;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.*;

public class ImageClassifierManager {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    private boolean mProcessDone = true;

    private ImageClassifier mImageClassifier = null;
    private int mSensorOrientation = 0;

    private List<ImageClassifier.Recognition> mResults = null;

    public ImageClassifierManager(Context context, int numThreads,
                                  String modelPath, String labelPath)
    {
        mHandlerThread = new HandlerThread("ImageClassifierManager");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        try {
            mImageClassifier = ImageClassifier.create((Activity)context,
                                                ImageClassifier.Model.FLOAT,
                                                ImageClassifier.Device.CPU,
                                                numThreads,
                                                modelPath,
                                                labelPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ImageClassifier.Recognition> RunInference(Bitmap rgbBitmap) {

        //CameraDataManager.saveBitmap(rgbBitmap, "RunInference.jpg");

        if (mImageClassifier != null) {
            long startTime = SystemClock.uptimeMillis();
            List<ImageClassifier.Recognition> results =
                    mImageClassifier.recognizeImage(rgbBitmap, mSensorOrientation);
            long endTime = SystemClock.uptimeMillis();
            LOGGER.d("InferenceTime : "+(endTime-startTime));

            return results;
        }
        return null;
    }

    public void RunInference(Mat rgbMat, float []cropDatas, boolean useCrop, boolean testOn) {

//        if( mProcessDone == false ) {
//            return;
//        }
//
//        mProcessDone = false;
//
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
                Bitmap rgbBitmap = cropImage(rgbMat, cropDatas, useCrop, testOn);
                //CameraDataManager.saveBitmap(rgbBitmap, "RunInference.jpg");

                if (mImageClassifier != null) {
                    long startTime = SystemClock.uptimeMillis();
                    mResults = mImageClassifier.recognizeImage(rgbBitmap, mSensorOrientation);
                    long endTime = SystemClock.uptimeMillis();
                    LOGGER.d("InferenceTime : "+(endTime-startTime));
                }

//                mProcessDone = true;
//            }
//        });
    }

    public List<ImageClassifier.Recognition> GetResult() {
        if( mResults != null ) {
            return mResults;
        }

        return null;
    }

    public String GetName() {
        String name = "";

        if( mResults != null ) {
            name  = mResults.get(0).getTitle();
        }

        return name;
    }

    public int GetIndex() {
        int index = -1;

        if( mResults != null ) {
            index  = Integer.parseInt(mResults.get(0).getId());
        }

        return index;
    }

    public float GetScore() {
        float score = 0.0f;

        if( mResults != null ) {
            score  = mResults.get(0).getConfidence();
        }

        return score;
    }

    public String GetName2() {
        String name = "";

        if( mResults != null ) {
            name  = mResults.get(1).getTitle();
        }

        return name;
    }

    public int GetIndex2() {
        int index = -1;

        if( mResults != null ) {
            index  = Integer.parseInt(mResults.get(1).getId());
        }

        return index;
    }

    public float GetScore2() {
        float score = 0.0f;

        if( mResults != null ) {
            score  = mResults.get(1).getConfidence();
        }

        return score;
    }

    public int GetLabelSize() {
        if( mImageClassifier != null ) {
            return mImageClassifier.GetLabelSize();
        }
        return 0;
    }

    public String GetLabelName(int index) {
        if( mImageClassifier != null ) {
            return mImageClassifier.GetLabelName(index);
        }

        return "";
    }

    private Bitmap cropImage(Bitmap inputBitmap)
    {
        Bitmap cropBitmap = null;

        return cropBitmap;
    }

    private Bitmap cropImage(Mat inputMat, float []cropDatas, boolean useCrop, boolean testOn)
    {
        Bitmap resultBitmap = Bitmap.createBitmap(
                inputMat.cols(), inputMat.rows(), Bitmap.Config.ARGB_8888);

        Mat dstMat = new Mat();

        if( useCrop == true ) {
            Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
            Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

            float width = inputMat.cols();
            float height = inputMat.rows();

            //lt, rt, lb, rb
            src_mat.put(0, 0, cropDatas[0], cropDatas[1], cropDatas[2], cropDatas[3], cropDatas[4], cropDatas[5], cropDatas[6], cropDatas[7]);
            dst_mat.put(0, 0, 0, 0, width, 0, 0, height, width, height);
            Mat perspectiveTransform = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

            Imgproc.warpPerspective(inputMat, dstMat, perspectiveTransform, inputMat.size());

            if( testOn == true ) {
                imwrite("/sdcard/tensorflow/dstMat.jpg", dstMat);
            }
        }
        else {
            dstMat = inputMat.clone();
            imwrite("/sdcard/tensorflow/dstMat.jpg", dstMat);
        }

        Utils.matToBitmap(dstMat, resultBitmap);

        return resultBitmap;
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

    //책 구분 루틴 넣기 다른데로 이동...
}
