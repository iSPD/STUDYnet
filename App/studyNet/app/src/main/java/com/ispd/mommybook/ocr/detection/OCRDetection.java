package com.ispd.mommybook.ocr.detection;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;

import com.ispd.mommybook.utils.UtilsFile;
import com.ispd.mommybook.utils.UtilsLogger;
import com.ispd.mommybook.utils.UtilsMatrix;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Text Detection Main Class
 *
 * @author ispd_sally
 * @version 1.0
 */

public class OCRDetection {
    private static final UtilsLogger LOGGER = new UtilsLogger();

    private static final int TF_OD_API_INPUT_SIZE = 416;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "frozen_east_pvanet_sun_model_416.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;

    private Integer mSensorOrientation;

    private Classifier mDetector;

    private long mLastProcessingTimeMs;
    private Bitmap mBitmapRGBFrame = null;
    private Bitmap mBitmapResized = null;
    private Bitmap mBitmapCropCopy = null;

    private boolean mIsComputingDetection = false;

    private long mTimestamp = 0;

    private Matrix mMatrixFrameToCropTransform;
    private Matrix mMatrixCropToFrameTransform;

    private OCRDetectionBoxTracker mBoxTracker;

    private int mInputImgWidth;
    private int mInputImgHeight;

    public class DetectionType {
        public final static int SCORING_DETECTION = 1000;
        public final static int COVER_DETECTION = 1001;
    }

    public OCRDetection(Context in_context, AssetManager in_asset, int in_rotation) {
        mBoxTracker = new OCRDetectionBoxTracker(in_context);

        try {
            mDetector =
                    OCRDetectionInference.Create(
                            in_asset,
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            LOGGER.d("SallyDetect OCRDetectionInference.Create() SUCCESS !!");

        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "SallyDetect  ERROR !!! Exception initializing classifier!");
        }

        mSensorOrientation = in_rotation;
        LOGGER.i("Camera orientation relative to screen canvas: %d", mSensorOrientation);
    }

    private void readyForDetection(int in_width, int in_height) {
        mInputImgWidth = in_width;
        mInputImgHeight = in_height;

        int resizedSize = TF_OD_API_INPUT_SIZE;
        int baseLength;
        // 채점 영역을 가져다 붙일 정사각형 빈 bitmap을 생성. 정사각형인 이유는,
        // detection 입력이 정사각형이고 맞춰서 같은 비율로 resize하기 위함임.
        if (mInputImgWidth >= mInputImgHeight) {
            baseLength = mInputImgWidth;
            mBitmapRGBFrame = Bitmap.createBitmap(mInputImgWidth, mInputImgWidth, Bitmap.Config.ARGB_8888);
        } else {
            baseLength = mInputImgHeight;
            mBitmapRGBFrame = Bitmap.createBitmap(mInputImgHeight, mInputImgHeight, Bitmap.Config.ARGB_8888);
        }

        mBitmapResized = Bitmap.createBitmap(resizedSize, resizedSize, Bitmap.Config.ARGB_8888);

        mMatrixFrameToCropTransform =
                UtilsMatrix.GetTransformationMatrix(
//                        mInputImgWidth, mInputImgHeight,
                        baseLength, baseLength, //입력을 정사각형에 붙일 것이기 때문에 baseLength를 정해서 셋팅함.
                        resizedSize, resizedSize,
                        mSensorOrientation, MAINTAIN_ASPECT);

        mMatrixCropToFrameTransform = new Matrix();
        mMatrixFrameToCropTransform.invert(mMatrixCropToFrameTransform);

        //mBoxTracker.setFrameConfiguration(mInputImgWidth, mInputImgHeight, mSensorOrientation);
        mBoxTracker.setFrameConfiguration(baseLength, baseLength, mSensorOrientation);
    }

    //sally : 채점영역에서 mat를 넘겨주기 때문에 int array로 변환하는 함수 필요.
    static int[] matToIntArray(Mat mRgba) {
        MatOfInt rgb = new MatOfInt(CvType.CV_32S);
        mRgba.convertTo(rgb, CvType.CV_32S);
        int[] rgba = new int[(int) (rgb.total() * rgb.channels())];
//        rgb.get(0, 0, rgba);
        rgb.get(mRgba.height(), mRgba.width(), rgba);
        return rgba;
    }

    /**
     * width,height는 입력이미지의 크기임, 채점의 경우 채점할 영역임. 표지 인식의 경우 표지 전체.
     *
     * @param in_inputImg
     */
    //public void ProcessImage(int []rgbBytes) {
    public void StartDetection(Mat in_inputImg, int in_coverOrScoring) { //rename
        LOGGER.d("SallyDetect StartDetection()");

        int inputWidth = in_inputImg.width();
        int inputHeight = in_inputImg.height();

        LOGGER.d("SallyDetect StartDetection() input width : " + inputWidth + ", height : " + inputHeight);

        //입력크기가 가변적이어서 아래 함수는 초기화시에 호출하지 않고 detection 시작시 호출함.
        readyForDetection(inputWidth, inputHeight);

        ++mTimestamp;
        final long currTimestamp = mTimestamp;

        // No mutex needed as this method is not reentrant.
        if (mIsComputingDetection) {
            return;
        }

        mIsComputingDetection = true;

        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        //Start
        long startTimeForReference = SystemClock.uptimeMillis();

        // 정사각형 mBitmapRGBFrame 에 채점영역을 우상기준으로 갖다 붙임.
        Mat dst = new Mat(mBitmapRGBFrame.getWidth(), mBitmapRGBFrame.getHeight(),in_inputImg.type());
        dst.setTo(new Scalar(255,255,255)); //흰색으로 칠함.
        Mat dstroi = dst.submat(new Rect(0,0,inputWidth,inputHeight)); // Get the reference of the sub-image in the specified area of dst
        in_inputImg.copyTo(dstroi); //dst의 우상부분에 inputImg를 카피함.
        Imgcodecs.imwrite("/sdcard/studyNet/DEBUG/1_dstImg"+ Long.toString(startTimeForReference)+".jpg", dst);//for debug
//        Imgcodecs.imwrite("/sdcard/studyNet/DEBUG/0_inputImg"+ Long.toString(startTimeForReference)+".jpg", in_inputImg);//for debug

        //Bitmap으로 변환
//        mBitmapRGBFrame.setPixels(in_rgbBytes, 0, mInputImgWidth, 0, 0,
        Utils.matToBitmap(dst, mBitmapRGBFrame);
        UtilsFile.SaveBitmap(mBitmapRGBFrame, "mBitmapRGBFrame"+ Long.toString(startTimeForReference)+".jpg");  //for debug

        //TODO : crop을 하지 않고, 채점영역의 width로 정사각형 빈 이미지를 만들고 채점영역을 우상에 복사한 후,
        //       TF_OD_API_INPUT_SIZE 사이즈로 리사이즈하기.
//crop 해서 원본 사이즈로 resize 하는 부분은 패스
//        Bitmap resizedBmp = Bitmap.createBitmap(mBitmapRGBFrame
//                , mBitmapRGBFrame.getWidth() / 4 //X 시작위치 (원본의 4/1지점)
//                , mBitmapRGBFrame.getHeight() / 5 //Y 시작위치 (원본의 4/1지점)
//                , mBitmapRGBFrame.getWidth() / 4 * 2 // 넓이 (원본의 절반 크기)
//                , mBitmapRGBFrame.getHeight() / 5 * 3); // 높이 (원본의 절반 크기)

//        resizedBmp = Bitmap.createScaledBitmap(resizedBmp,
//                                               mBitmapRGBFrame.getWidth(),
//                                               mBitmapRGBFrame.getHeight(),
//                                          true);
//        LOGGER.i("Resized Bmp %d %d", resizedBmp.getWidth(), resizedBmp.getHeight());

//        TODO : 아래 소스중 resizeBmp를 mBitmapRGBFrame으로 치환하면 될듯.
        final Canvas canvas = new Canvas(mBitmapResized);
        //canvas.drawBitmap(resizedBmp, mMatrixFrameToCropTransform, null);
        canvas.drawBitmap(mBitmapRGBFrame, mMatrixFrameToCropTransform, null);

        Matrix sideInversion = new Matrix();
        //sideInversion.setScale(1, -1);  // 상하반전
        //sideInversion.setScale(-1, 1);  // 좌우반전
        mBitmapResized = Bitmap.createBitmap(mBitmapResized, 0, 0,
                mBitmapResized.getWidth(), mBitmapResized.getHeight(), sideInversion, false);

        long endTimeForReference = SystemClock.uptimeMillis();
        LOGGER.d("SallyDetect Elapsed Time(Crop Bitmap) : " + (endTimeForReference - startTimeForReference));

//        ImageUtils.saveBitmap(mCroppedBitmap, "mCroppedBitmap.png");
        UtilsFile.SaveBitmap(mBitmapResized, "mBitmapResized"+ Long.toString(startTimeForReference)+".jpg");

        // sally : 현재 VGA사이즈의 카메라 이미지(bmp)를 detector를 통해 TextBoxView 클래스에 전달한다.
        // TextBoxView 클래스에서는 detector의 recognizeImage() 의 setTextLocation()함수 호출 후
        // drawTextLines() 호출 및 decode() 루틴 적용 후 나온 결과(bbox들)를 인식루틴에 전달하기 위해
        // 이 rgbFrameBitmap을 이용한다.
        //mDetector.setRecogBmp(resizedBmp); //sally : OCRManager에서 할일임. 옮기기 TODO

        new Thread(() -> {
            //                LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
//            final List<Classifier.Recognition> results = mDetector.RecognizeImage(mBitmapResized);
            mDetector.RecognizeImage(mBitmapResized, in_coverOrScoring);
            mLastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
            LOGGER.i("SallyDetect lastProcessingTimeMs : " + mLastProcessingTimeMs);


            //LOGGER.d("CroppedBMP : " + croppedBitmap.getWidth() + ", " + croppedBitmap.getHeight()); //sally 320x320

//            //TODO : 아래 결과 그리는 부분 옮기든가 수정하든가 하기
//            mBitmapCropCopy = Bitmap.createBitmap(mBitmapResized);
//            final Canvas canvas2 = new Canvas(mBitmapCropCopy);
//            final Paint paint = new Paint();
//            paint.setColor(Color.RED);
//            paint.setStyle(Paint.Style.STROKE);
//            paint.setStrokeWidth(2.0f);
//
//            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
//            switch (MODE) {
//                case TF_OD_API:
//                    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
//                    break;
//            }
//
//            final List<Classifier.Recognition> mappedRecognitions =
//                    new LinkedList<Classifier.Recognition>();
//
//            for (final Classifier.Recognition result : results) {
//                final RectF location = result.getLocation();
//                if (location != null && result.getConfidence() >= minimumConfidence) {
//                    canvas2.drawRect(location, paint);
//
//                    mMatrixCropToFrameTransform.mapRect(location);
//
//                    result.setLocation(location);
//                    mappedRecognitions.add(result);
//                }
//            }
//            mBoxTracker.trackResults(mappedRecognitions, currTimestamp);
            mIsComputingDetection = false;
        }).start();
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    public void SetUseNNAPI(boolean in_isChecked) {
        mDetector.SetUseNNAPI(in_isChecked);
    }

    public void SetNumThreads(int in_numThreads) {
        mDetector.SetNumThreads(in_numThreads);
    }
}
