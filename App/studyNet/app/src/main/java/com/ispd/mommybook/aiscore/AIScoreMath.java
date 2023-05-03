package com.ispd.mommybook.aiscore;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.imageclassifier.ImageClassifierManager;
import com.ispd.mommybook.imageprocess.ImageProcessKeyPointMatch;
import com.ispd.mommybook.imageprocess.ImageProcessSubtraction;
import com.ispd.mommybook.ocr.OCRManager;
import com.ispd.mommybook.ocr.OCRRecognitionMode;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.List;

import static com.ispd.mommybook.MainHandlerMessages.DRAW_AISCORE;
import static com.ispd.mommybook.aiscore.AIScoreFunctions.MSG_REQUEST_TEXT_SCORING;
import static com.ispd.mommybook.aiscore.AIScoreManager.SECOND_FRAME;
import static com.ispd.mommybook.aiscore.AIScoreManager.THIRD_FRAME;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.Method.*;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.Sticker.NONE;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.TextLanguage.*;

import static com.ispd.mommybook.ocr.OCRRecognitionMode.*;

import static java.lang.Math.abs;
import static org.opencv.core.Core.countNonZero;
import static org.opencv.core.Core.mean;
import static org.opencv.core.Core.multiply;
import static org.opencv.core.Core.subtract;
import static org.opencv.core.Core.sumElems;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.*;


/**
 * AIScoreMath
 *
 * @author Daniel
 * @version 1.0
 */
public class AIScoreMath {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Context mContext = null;
    private Handler mMainHandler = null;
    private AIScoreView mAIScoreView = null;

    private AIScoreReferenceDB mAIScoreReferenceDB = new AIScoreReferenceDB();
    private List<AIScoreReferenceDB.DataBase> mDataBase;

    private int mPreviewWidth = MainActivity.gCameraPreviewWidth;
    private int mPreviewHeight = MainActivity.gCameraPreviewHeight;

    private int mPreviewRenderWidth = MainActivity.gPreviewRenderWidth;
    private int mPreviewRenderHeight = MainActivity.gPreviewRenderHeight;

    private ImageProcessSubtraction mImageProcessSubtraction = null;
    private ImageProcessKeyPointMatch mImageProcessKeyPointMatch = null;
    private ImageClassifierManager mImageClassifierManager = null;
    private float mCropDatas[];

    private int mPageIndex = -1;
    private OCRManager mOCRManager;
    private AIScoreFunctions mAIScoreFunc;

    /**
     * 현재 시점에서 완료된 인식데이터 개수
     */
    private static int mRecognitionDoneCnt = 0;

    /**
     * 인식할 데이터의 수 (BBox 개수와 같음)
     */
    private static int mRecogDataLength = 0;

    private Mat mAlignedMatScndFrame = new Mat();
    private Mat mAlignedMatThrdFrame = new Mat();
	
    /**
     * 채점 루틴이 끝났는지의 여부, 이 값과 문자인식이 끝났는지의 여부를 체크해서 전체 채점이 끝났는지 확인할 수 있다.
     */
    private boolean mIsDoProcessDone = true;
    private boolean mIsOCRDone = true;

    private int mClassCount = 0;
    private double mFillRate[][] = new double[4][2];
    private double mFillArea[][] = new double[4][2];

    /**
     * AIScoreMath
     *
     * @param handler
     * @param scoreView
     *
     * 문자인식, 이미지 매칭, 이미지 비교 등 선언
     */
    public AIScoreMath(Context context, Handler handler, AIScoreView scoreView) {
        mContext = context;
        mMainHandler = handler;
        mAIScoreView = scoreView;

        mImageProcessSubtraction = new ImageProcessSubtraction();
        mImageProcessKeyPointMatch = new ImageProcessKeyPointMatch();

        String modelPath = "frozen_mobilenet_math83.tflite";
//        String modelPath = "frozen_inceptionv3_math.tflite";
        String labelPath = "labels_math_sticker.txt";
        mImageClassifierManager = new ImageClassifierManager(context, 4,
                modelPath, labelPath);

        mCropDatas = new float[]
                        {0, 0,
                        mPreviewWidth, 0,
                        0, mPreviewHeight,
                        mPreviewWidth, mPreviewHeight};
        mAIScoreFunc = new AIScoreFunctions(mMainHandler);
        mOCRManager = new OCRManager(mContext);
        mOCRManager.SetRecogResultListener(mRecogResultListener);
    }
    /**
     * 집단채점용 입력 이미지 셋팅
     * @param img
     * @param frameNo 프레임 넘버(1 or 2)
     */
    public void SetMutiScoringInputImgSub(Mat img, int frameNo) {
        if(frameNo == SECOND_FRAME) {
            //TODO null 이 아니면 release 하는 부분 넣기. 죽나 봐야함.
            mAlignedMatScndFrame = img.clone();
        }
        else if(frameNo == THIRD_FRAME) {
            //TODO null 이 아니면 release 하는 부분 넣기. 죽나 봐야함.
            mAlignedMatThrdFrame = img.clone();
        }
    }

    /**
     * 해당페이지의 미채점 또는 틀린문제, 세모표시 문제의 개수를 리턴한다.
     * @param coverIndex
     * @param pageIndex
     * @return 미채점,틀린문제,세모표시문제의 총 개수
     */
    public int HasItemsToBeGraded(int coverIndex, int pageIndex) {
        mPageIndex  = pageIndex;

        //DB에서 할일 가져오기
        mDataBase = mAIScoreReferenceDB.GetAIScoreMethod(coverIndex, pageIndex);

        LOGGER.d("pageIndex : %d, mDataBase size : %d", pageIndex, mDataBase.size());

        if( mDataBase.size() == 0 ) {
            LOGGER.d("Nothing to do");
        }

        int cnt = 0;

        for(int i = 0; i < mDataBase.size(); i++) {
            LOGGER.d("[processCount] mDataBase.get(i).mMethod : " + mDataBase.get(i).mMethod);
            if (mDataBase.get(i).mMethod != IMAGE_BUTTON && mDataBase.get(i).mMethod != IMAGE_SOUND)
            {
                if (mDataBase.get(i).mDoGrading == 0 || //채점 안된 문제
                        mDataBase.get(i).mIsCorrect == 0 /*|| //틀린 문제
                        mDataBase.get(i).mIsCorrect == 2*/)  //세모표시 문제(자유쓰기형)
                {
                    LOGGER.d("SallyScoring HasItemsToBeGraded() idx= " + i + ", mDoGrading = " + mDataBase.get(i).mDoGrading + ", mIsCorrect = " + mDataBase.get(i).mIsCorrect);
                    cnt++;
                }
            }
        }
        return cnt;
    }

    /**
     * 현재 과목의 해당 페이지에 집단 채점할 문제가 포함되어 있는지를 리턴함.
     * @param coverIndex
     * @param pageIndex
     * @return
     */
    public boolean HasMultiScoringPart(int coverIndex, int pageIndex, int []captureCount) {
        mPageIndex  = pageIndex;

        //DB에서 할일 가져오기
        mDataBase = mAIScoreReferenceDB.GetAIScoreMethod(coverIndex, pageIndex);

        LOGGER.d("pageIndex : %d, mDataBase size : %d", pageIndex, mDataBase.size());

        if( mDataBase.size() == 0 ) {
            LOGGER.d("Nothing to do");
        }

        for(int i = 0; i < mDataBase.size(); i++) {

            LOGGER.d("[processCount] mDataBase.get(i).mMethod : " + mDataBase.get(i).mMethod);

            if (mDataBase.get(i).mMethod == TEXT_RECOGNITION_WORD ||
                    mDataBase.get(i).mMethod == TEXT_RECOGNITION_SENTENCE) {
                if (mDataBase.get(i).mSticker == NONE) { //손글씨 모드임.
                    if (mDataBase.get(i).mTextLanguage == KOREAN ||
                            mDataBase.get(i).mTextLanguage == ENGLISH) {
                        captureCount[0] = 3;
                        return true;
                    }
                }
            }
            else if(mDataBase.get(i).mMethod == IMAGE_SUBTRACT) {
                captureCount[0] = 2;
                return true;
            }
            else if(mDataBase.get(i).mMethod == IMAGE_MATCHING) {
                captureCount[0] = 1;
                return true;
            }
            else if(mDataBase.get(i).mMethod == IMAGE_CLASSIFICATION) {
                captureCount[0] = 0;
                return true;
            }
        }
        return false;
    }

    // 카메라 캡처 시점부터 채점 시작을 셋팅하기 위한 함수
    public void SetDoProcessFlagOn() {
        mIsDoProcessDone = false;
    }

    /**
     * DoProcess
     *
     * @param alignedMat
     * @param coverIndex
     * @param pageIndex
     *
     * 현재 책과 페이지 정보로 DB에서 할일 가져와서 진행
     */
    public void DoProcess(Mat saveAlignedMat, Mat alignedMat, int coverIndex, int pageIndex, int aiScoreResultOn, int processCount) {
        //        mIsDoProcessDone = false;  //SetDoProcessFlagOn() 함수에서 셋팅함.
        mPageIndex  = pageIndex;

        //DB에서 할일 가져오기
        mDataBase = mAIScoreReferenceDB.GetAIScoreMethod(coverIndex, pageIndex);

        LOGGER.d("pageIndex : %d, mDataBase size : %d", pageIndex, mDataBase.size());

        if( mDataBase.size() == 0 ) {
            LOGGER.d("Nothing to do");
        }

        mClassCount = 0;

        //할일 순서대로 진행
        //왼쪽 페이지부터 좌, 우, 아래 순서대로 진행
        for(int i = 0; i < mDataBase.size(); i++) {
            if(mDataBase.get(i).mMethod == TEXT_RECOGNITION_WORD) {
                if (mOCRManager.IsRecognitonDone() == true && (mRecognitionDoneCnt == 0)) {
                //do ocr
                int cropX = (int) (mDataBase.get(i).mX * (float) alignedMat.cols());
                int cropY = (int) (mDataBase.get(i).mY * (float) (alignedMat.rows() - 10 * 4 * alignedMat.rows() / mPreviewHeight));//책의 아래 40픽셀이 떠 있음
                int cropW = (int) (mDataBase.get(i).mW * (float) alignedMat.cols());
                int cropH = (int) ((mDataBase.get(i).mH * (float) (alignedMat.rows() - 10 * 4 * alignedMat.rows() / mPreviewHeight)));//책의 아래 40픽셀이 떠 있음
                Rect cropRect = new Rect(cropX, cropY, cropW, cropH);
                if(mDataBase.get(i).mSticker == NONE) {
                    if(mDataBase.get(i).mTextLanguage == KOREAN) {
                        //수학에서는 집단채점을 사용하지 않음.
                        addTextRecognitionData(alignedMat, cropRect, i, HW_KOR_SINGLE);
                        } else if (mDataBase.get(i).mTextLanguage == ENGLISH) {
                            addTextRecognitionData(alignedMat, cropRect, i, HW_ENG_MULTI);
                        } else if (mDataBase.get(i).mTextLanguage == NUMBER) {
                            addTextRecognitionData(alignedMat, cropRect, i, HW_NUM_SIGN);
                        } else if (mDataBase.get(i).mTextLanguage == SIGN) {
                            addTextRecognitionData(alignedMat, cropRect, i, HW_NUM_SIGN);
                        }
                    } else {//스티커모드
                        if (mDataBase.get(i).mTextLanguage == KOREAN) {
                            //정답이 한글+숫자 라면 TYPO_KOR_NUM 으로 셋팅
                            if (mAIScoreFunc.HasNumbers(mDataBase.get(i).mAnswerText) == true) {
                                addTextRecognitionData(alignedMat, cropRect, i, TYPO_KOR_NUM);
                            } else { //정답이 한글 only 이면 TYPO_KOR로 셋팅
                                addTextRecognitionData(alignedMat, cropRect, i, TYPO_KOR);
                            }
                        } else if (mDataBase.get(i).mTextLanguage == ENGLISH) {
                            addTextRecognitionData(alignedMat, cropRect, i, TYPO_ENG);
                        } else if (mDataBase.get(i).mTextLanguage == NUMBER) {
                            addTextRecognitionData(alignedMat, cropRect, i, TYPO_NUM_SIGN);
                        } else if (mDataBase.get(i).mTextLanguage == SIGN) {
                            addTextRecognitionData(alignedMat, cropRect, i, TYPO_NUM_SIGN);
                        }
                    }
                } //end of if (mOCRManager.IsRecognitonDone() == true)
                else {
                    LOGGER.d("SallyRecog skip RecogData Add ++++++++++++++++++++++++++");
                }
            }
            else if(mDataBase.get(i).mMethod == IMAGE_MATCHING) {
                int x = (int)(mDataBase.get(i).mX * (float)alignedMat.cols());
                int y = (int)(mDataBase.get(i).mY * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음
                int w = (int)(mDataBase.get(i).mW * (float)alignedMat.cols());
                int h = (int)(mDataBase.get(i).mH * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음

                //시계 페이지
                doImageSubtractWithFirstFrame(saveAlignedMat, alignedMat, new Rect(x, y, w, h), pageIndex, i, aiScoreResultOn, processCount);
            }
            else if(mDataBase.get(i).mMethod == IMAGE_SUBTRACT) {

                int cropX = (int)(mDataBase.get(i).mX * (float)alignedMat.cols());
                int cropY = (int)(mDataBase.get(i).mY * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음
                int cropW = (int)(mDataBase.get(i).mW * (float)alignedMat.cols());
                int cropH = (int)(mDataBase.get(i).mH * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음
                Rect cropRect = new Rect(cropX, cropY, cropW, cropH);

                //나비 페이지
                doImageSubtract(alignedMat, cropRect, pageIndex, i, aiScoreResultOn, processCount);
                mClassCount++;
            }
            else if(mDataBase.get(i).mMethod == IMAGE_BUTTON) {
                //do button activity
            }
        }
        // 문자인식 채점 데이터가 있다면 문자인식 시작하라고 요청하기
        if( (mOCRManager.IsRecognitonDone() == true) &&
                (mOCRManager.GetOCRDataSize() > 0) &&
                (mRecognitionDoneCnt == 0) ) {
            mIsOCRDone = false;
            mOCRManager.StartRecognitonDetection();
        }

        mAIScoreReferenceDB.WriteAIScoreResult("math");
        mIsDoProcessDone = true;
    }

    /**
     * doImageSubtract
     *
     * @param alignedMat
     * @param rect
     * @param pageIndex
     * @param itemIndex
     *
     * 이미지 차이 구하기
     */
    private void doImageSubtract(Mat alignedMat, Rect rect, int pageIndex, int itemIndex, int aiScoreResultOn, int processCount) {
        if( mDataBase.get(itemIndex).mDoGrading == 1 && mDataBase.get(itemIndex).mIsCorrect == 1) {
            LOGGER.d("index : "+itemIndex+"---> Grading is already done.");
            return;
        }

        for(int count = 0; count < 2; count++) {

            int gapX = rect.width / 5;
            int gapY = rect.height / 5;

            Rect newRect = new Rect(rect.x + gapX, rect.y + gapY, rect.width - gapX * 2, rect.height - gapY * 2);

            String fileName = "/sdcard/studyNet/DB/math/compare/" + pageIndex + "-" + (itemIndex + 1) + ".png";
            Mat refMat = imread(fileName);
            imwrite("/sdcard/studyNet/DEBUG/math/refMat" + itemIndex + ".jpg", refMat);

            Mat cropInputMat = null;
            if( count == 0 ) {
                cropInputMat = new Mat(alignedMat, newRect).clone();
            }
            else if( count == 1 ) {
                cropInputMat = new Mat(mAlignedMatScndFrame, newRect).clone();
            }

            int gapX2 = refMat.cols() / 5;
            int gapY2 = refMat.rows() / 5;

            Mat refMat2 = new Mat(refMat, new Rect(0 + gapX2, 0 + gapY2, refMat.cols() - gapX2 * 2, refMat.rows() - gapY2 * 2)).clone();
            resize(refMat2, refMat2, cropInputMat.size());

            imwrite("/sdcard/studyNet/DEBUG/math/refMat2" + itemIndex + ".jpg", refMat2);
            imwrite("/sdcard/studyNet/DEBUG/math/cropInputMat" + itemIndex + ".jpg", cropInputMat);

            cvtColor(refMat2, refMat2, COLOR_BGR2GRAY);
            cvtColor(cropInputMat, cropInputMat, COLOR_BGR2GRAY);

            Scalar scalar1 = mean(refMat2);
            Scalar scalar2 = mean(cropInputMat);
            LOGGER.d("[doImageSubtract] pre scalar1 : " + scalar1.val[0] + ", scalar2 : " + scalar2.val[0]);

            double multiVal = (scalar1.val[0] / scalar2.val[0]);

            multiply(cropInputMat, new Scalar(multiVal), cropInputMat);
            //cropInputMat = cropInputMat * multiVal;
            //img_input2 = img_input1 * 1.5;

            scalar1 = mean(refMat2);
            scalar2 = mean(cropInputMat);

            LOGGER.d("[doImageSubtract] post scalar1 : " + scalar1.val[0] + ", scalar2 : " + scalar2.val[0]);

            Mat diffMatGo = new Mat();
            subtract(refMat2, cropInputMat, diffMatGo);
            diffMatGo.convertTo(diffMatGo, -1, 10.0, 0);  //increase brightness by 25 units

            Scalar scalar3 = mean(diffMatGo);

            Scalar sumScalar = sumElems(diffMatGo);
            int nonZeroCount = countNonZero(diffMatGo);
            LOGGER.d("sumScalar : " + sumScalar.val[0]);
            LOGGER.d("nonZeroCount : " + nonZeroCount);
            double meanValue3 = sumScalar.val[0] / nonZeroCount;
            LOGGER.d(itemIndex + "[doImageSubtract-sum] meanValue3 : " + meanValue3);

            LOGGER.d(itemIndex + "[doImageSubtract-sum] scalar3 : " + scalar3.val[0]);

            imwrite("/sdcard/studyNet/DEBUG/math/refMatA" + itemIndex + ".jpg", refMat2);
            imwrite("/sdcard/studyNet/DEBUG/math/cropInputMatA" + itemIndex + ".jpg", cropInputMat);

            imwrite("/sdcard/studyNet/DEBUG/math/subtractB" + itemIndex + ".jpg", diffMatGo);
            threshold(diffMatGo, diffMatGo, meanValue3, 255, THRESH_BINARY);
            //erode(diffMatGo, diffMatGo, new Mat(), new Point(-1, -1), 3);
            //dilate(diffMatGo, diffMatGo, new Mat(), new Point(-1, -1), 3);
            imwrite("/sdcard/studyNet/DEBUG/math/subtract" + itemIndex + ".jpg", diffMatGo);

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            //contour를 근사화한다.
            MatOfPoint2f approx = new MatOfPoint2f();

            /// Find contours
            findContours(diffMatGo, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE);

            Mat drawing = new Mat();
            cvtColor(diffMatGo, drawing, COLOR_GRAY2BGR);

            boolean isCorrect = false;

            double maxFillRate = -100.0;
            double maxFillArea = -100.0;

            for (int i = 0; i < contours.size(); i++) {
                Scalar color = new Scalar(0, 0, 255);
                drawContours(drawing, contours, i, color, 2, 8, hierarchy, 0, new Point());

                MatOfPoint2f c2f = new MatOfPoint2f(contours.get(i).toArray());
                double peri = arcLength(c2f, true);

                approxPolyDP(c2f, approx, peri * 0.02, true);

                Point[] points = approx.toArray();
                int size = points.length;

                if (contourArea(approx) > 1500/*50000*/) {

                    LOGGER.d(itemIndex + "-contourArea(approx)[" + i + "] : " + contourArea(approx));

                    //LOGGER.d(itemIndex+"-contourArea(approx)["+i+"] : "+contourArea(approx));

                    //cout << "fabs(contourArea(Mat(approx))) : " << fabs(contourArea(Mat(approx))) << endl;

                    Rect rectangle = boundingRect(new MatOfPoint(approx.toArray()));
                    LOGGER.d(itemIndex + ", " + i + ", rectangle : " + rectangle);

                    double rectangleArea = rectangle.width * rectangle.height;

                    double polygonArea = contourArea(c2f);
                    double polygonArea2 = contourArea(approx);
                    LOGGER.d("polygonArea : " + polygonArea + ", polygonArea2 : " + polygonArea2);

                    double fillRate = polygonArea * 100.0 / rectangleArea;

                    LOGGER.d("approx size : " + size);

                    //For Debug
                    Scalar debugScalar = new Scalar(0, 0, 255);
                    if (fillRate > 25.0 && polygonArea > 3500) {
                        debugScalar = new Scalar(255, 0, 0);
                    }

                    line(drawing, points[0], points[size - 1], debugScalar, 15);

                    for (int k = 0; k < size - 1; k++) {
                        line(drawing, points[k], points[k + 1], debugScalar, 15);
                    }


                    rectangle(drawing, new Point(rectangle.x, rectangle.y),
                            new Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height),
                            debugScalar, 5);
                    //For Debug

                    if (fillRate > maxFillRate) {
                        maxFillRate = fillRate;
                        maxFillArea = polygonArea;
                    }
                }
            }
//            imwrite("/sdcard/studyNet/DEBUG/math/drawing" + processCount + itemIndex + ".jpg", drawing);
//
//            mFillRate[mClassCount][processCount] = maxFillRate;
//            mFillArea[mClassCount][processCount] = maxFillArea;

            imwrite("/sdcard/studyNet/DEBUG/math/drawing" + count + itemIndex + ".jpg", drawing);

            mFillRate[mClassCount][count] = maxFillRate;
            mFillArea[mClassCount][count] = maxFillArea;

            //if (processCount == 1) {
            if( count == 1 ) {

                LOGGER.d("AverageSticker, " + mClassCount + "-mFillRate[mClassCount][0] : " + mFillRate[mClassCount][0] + ", mFillRate[mClassCount][1] : " + mFillRate[mClassCount][1]);
                LOGGER.d("AverageSticker, " + mClassCount + "-mFillArea[mClassCount][0] : " + mFillArea[mClassCount][0] + ", mFillArea[mClassCount][1] : " + mFillArea[mClassCount][1]);

                double averageFillRate = (mFillRate[mClassCount][0] + mFillRate[mClassCount][1]) / 2.0;
                double averatePolygonArea = (mFillArea[mClassCount][0] + mFillArea[mClassCount][1]) / 2.0;

                LOGGER.d("AverageSticker, " + mClassCount + "-averageFillRate : " + averageFillRate + ", averatePolygonArea : " + averatePolygonArea);

                if (averageFillRate > 25.0 && averatePolygonArea > 3500) {
                    doGradingForImageSubtract(pageIndex, itemIndex, true);
                } else {
                    doGradingForImageSubtract(pageIndex, itemIndex, false);
                }
            }
        }
    }

    Mat targetMat = new Mat();
    Mat cropInputMat = new Mat();
    Mat grayMat1 = new Mat();
    Mat grayMat2 = new Mat();

    /**
     * doImageSubtractWithFirstFrame
     *
     * @param alignedMat
     * @param rect
     *
     */
    private void doImageSubtractWithFirstFrame(Mat saveAlignedMat, Mat alignedMat, Rect rect, int pageIndex, int itemIndex, int aiScoreResultOn, int processCount) {
        //이미지 채점이 되어 있고 그 답이 맞는 답인 경우 리턴.
        if( mDataBase.get(itemIndex).mDoGrading == 1 && mDataBase.get(itemIndex).mIsCorrect == 1) {
            LOGGER.d("index : "+itemIndex+"---> Grading is already done.");
//            버튼 클릭시 화면 표시하도록 바꿨으므로 아래 코드 주석처리함.
//            float changeRateUse[] = {100.f, 100.f, 115.f};
//            doDrawGrading2(pageIndex, itemIndex, changeRateUse, aiScoreResultOn);
            return;
        }

        //입력 영상의 처리할 영상 Crop하여 원본이미지 사이지로 변경
        cropInputMat = new Mat(alignedMat, rect).clone();
        //targetMat = imread("/sdcard/studyNet/DB/math/compare/clock.jpg").clone();
        Mat targetMatPre = saveAlignedMat.clone();
        targetMat = new Mat(targetMatPre, rect).clone();
        resize(targetMat, targetMat, cropInputMat.size());

        float changeRate[] = new float[3];

        for(int i = 0; i < 3; i++) {
            //cropInputMat = new Mat(cropInputMat, new Rect(0, 0, cropInputMat.cols()/3, cropInputMat.rows()));
            Mat cropInputMat2 = new Mat(cropInputMat, new Rect(0 + cropInputMat.cols() * i / 3, 0, cropInputMat.cols() / 3, cropInputMat.rows())).clone();
            Mat targetMat2 = new Mat(targetMat, new Rect(0 + targetMat.cols() * i / 3, 0, targetMat.cols() / 3, targetMat.rows())).clone();
            //resize(cropInputMat, cropInputMat, new Size(cropInputMat.cols()*4, cropInputMat.rows()*4));

            imwrite("/sdcard/studyNet/DEBUG/math/cropInputMat2-"+i+".jpg", cropInputMat2);
            imwrite("/sdcard/studyNet/DEBUG/math/targetMat2-"+i+".jpg", targetMat2);

            //각각 Gray Scale로 변경
            //cvtColor(cropInputMat, grayMat1, COLOR_RGB2GRAY);
            changeRate[i] = JniController.findCircle(cropInputMat2.getNativeObjAddr(), targetMat2.getNativeObjAddr(), i, itemIndex, 1);
        }

        if( processCount == 0 ) {
            doDrawGrading2(pageIndex, itemIndex, changeRate, aiScoreResultOn);
        }
        targetMatPre.release();
    }

    private void doImageClassification(Mat alignedMat, Rect rect, int index) {

//        if( index == 0 ) {
//            Thread thread = new Thread(new Runnable() {
//                @Override
//                public void run() {
                    LOGGER.d("doImageClassification rect : "+rect);

                    Mat cropInputMat = new Mat(alignedMat, rect).clone();
                    LOGGER.d("cropInputMat : %d %d", cropInputMat.cols(), cropInputMat.rows());

                    //resize(cropInputMat, cropInputMat, new Size(cropInputMat.cols()*2, cropInputMat.rows()*2), INTER_CUBIC);
                    //cropInputMat = changeContrastNBrightness(cropInputMat, 1.5, 0);//1-3, 0-100

                    resize(cropInputMat, cropInputMat, new Size(cropInputMat.cols()*7, cropInputMat.rows()*7), INTER_CUBIC);
                    if(index >= 6) {
                        mCropDatas = new float[]
                        {
                            cropInputMat.cols()/3.f, cropInputMat.rows()/3.f,
                            cropInputMat.cols()*2.f/3.f, cropInputMat.rows()/3.f,
                            cropInputMat.cols()/3.f, cropInputMat.rows()*2.f/3.f,
                            cropInputMat.cols()*2.f/3.f, cropInputMat.rows()*2.f/3.f
                        };
                    }

                    imwrite("/sdcard/studyNet/DEBUG/cropInputMat-"+index+".jpg", cropInputMat);

                    mImageClassifierManager.RunInference(cropInputMat, mCropDatas, true, true);
                    String stickerName = mImageClassifierManager.GetName();
                    int stickerIndex = mImageClassifierManager.GetIndex();
                    float stickerScore = mImageClassifierManager.GetScore();

                    LOGGER.d("index : %d, stickerName : %s, pageIndex : %d, pageScore : %f", index, stickerName, stickerIndex, stickerScore);
//                }
//            });
//            thread.start();
//        }
    }

    private byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }

    public Mat changeContrastNBrightness(Mat inputMat, double alpha, int beta) {

        Mat newImageMat = Mat.zeros(inputMat.size(), inputMat.type());

        LOGGER.d(" Basic Linear Transforms ");
        LOGGER.d("-------------------------");
        LOGGER.d("* Enter the alpha value [1.0-3.0]: ");
        LOGGER.d("* Enter the beta value [0-100]: ");

        byte[] imageData = new byte[(int) (inputMat.total()*inputMat.channels())];
        inputMat.get(0, 0, imageData);

        byte[] newImageData = new byte[(int) (newImageMat.total()*newImageMat.channels())];

        for (int y = 0; y < inputMat.rows(); y++) {
            for (int x = 0; x < inputMat.cols(); x++) {
                for (int c = 0; c < inputMat.channels(); c++) {
                    double pixelValue = imageData[(y * inputMat.cols() + x) * inputMat.channels() + c];
                    pixelValue = pixelValue < 0 ? pixelValue + 256 : pixelValue;
                    newImageData[(y * inputMat.cols() + x) * inputMat.channels() + c]
                            = saturate(alpha * pixelValue + beta);
                }
            }
        }
        newImageMat.put(0, 0, newImageData);

        return newImageMat;
    }

    //image subtract
    private void doGradingForImageSubtract(int pageIndex, int index, boolean isCorrect) {

        boolean isGoodAnswer = false;
        String goodAnswer = mDataBase.get(index).mCorrectName;
        if( isCorrect == true ) {
            if(goodAnswer.equals("fly")) {
                isGoodAnswer = true;
            } else {
                isGoodAnswer = false;
            }
        } else {
            if(goodAnswer.equals("fly")) {
                isGoodAnswer = false;
            } else {
                isGoodAnswer = true;
            }
        }
        mDataBase.get(index).mDoGrading = 1;
        mDataBase.get(index).mIsCorrect = isGoodAnswer ? 1 : 0;
    }
    /*
        Message msg = new Message();
        Rect rect = new Rect((int)x2, (int)y2, 300, 300);
        msg.what = DRAW_AISCORE;
        msg.obj = rect;
        msg.arg1 = isGoodAnswer ? 1 : 0;
        mMainHandler.sendMessage(msg);

        //draw sticker
        //현재는 무조건 해당 위치에 스티커 그려주기
        if( mDataBase.get(index).mSticker == AIScoreReferenceDB.Sticker.STICKER && isCorrect == true ) {
            int x = (int) (mDataBase.get(index).mX *  MainActivity.gPreviewRenderWidth);
            int y = (int) (mDataBase.get(index).mY * MainActivity.gPreviewRenderHeight);
            int w = (int) (mDataBase.get(index).mW *  MainActivity.gPreviewRenderWidth);
            int h = (int) (mDataBase.get(index).mH * MainActivity.gPreviewRenderHeight);

            int gapX = w / 4;
            int gapY = h / 4;
            x = x + gapX;
            y = y + gapY;
            w = w - gapX * 2;
            h = h - gapY * 2;

            drawSticker(new Rect(x, y, w, h), pageIndex, index);
        }
    }
*/
    //image 양보고 체크
    private void doDrawGrading2(int pageIndex, int index, float []changeRate, int aiScoreResultOn) {
        float x2 = (mDataBase.get(index).mX + mDataBase.get(index).mW / 2.f) * MainActivity.gPreviewRenderWidth;
//        float y2 = (mDataBase.get(index).mY + mDataBase.get(index).mH / 2.f) * MainActivity.gPreviewRenderHeight;
//        float x2 = (mDataBase.get(index).mX + mDataBase.get(index).mW / 20.f) * MainActivity.gPreviewRenderWidth;
        float y2 = (mDataBase.get(index).mY + mDataBase.get(index).mH / 5.f) * MainActivity.gPreviewRenderHeight;
        float width2 = mDataBase.get(index).mW * MainActivity.gPreviewRenderWidth;
        float height2 = mDataBase.get(index).mH * MainActivity.gPreviewRenderHeight;

        LOGGER.d("doDrawGrading2 %f %f %f %f", x2, y2, width2, height2);

        boolean isGoodAnswer = false;
        if(changeRate[2] > 110.f && changeRate[0] < 105.f && changeRate[1] < 105.f) {
            isGoodAnswer = true;

            mDataBase.get(index).mDoGrading = 1;
            mDataBase.get(index).mIsCorrect = 1;
        }
        else if( changeRate[0] < 105.f && changeRate[1] < 105.f &&  changeRate[2] < 105.f ){
            mDataBase.get(index).mDoGrading = 1;
            mDataBase.get(index).mIsCorrect = 0;
            return;
        }

//        if( aiScoreResultOn == 1 ) {
//            Message msg = new Message();
//            //Rect rect = new Rect((int)x2, (int)y2, 300, 300);
//            Rect rect = new Rect((int) x2, (int) y2, (int) width2, (int) height2);
//            msg.what = DRAW_AISCORE;
//            msg.obj = rect;
//            msg.arg1 = isGoodAnswer ? 1 : 0;
//            mMainHandler.sendMessage(msg);
//        }
    }
	
	 /**
     * drawSticker
     *
     * @param rect
     * @param pageIndex
     * @param itemIndex
     *
     * 해당하는 위치에 스티커 그려주기
     */
    private void drawSticker(Rect rect, int pageIndex, int itemIndex) {
        String fileName = "/sdcard/studyNet/DB/math/sticker/"+pageIndex+"-"+(itemIndex+1)+".png";

        int leftTopX = rect.x;
        int leftTopY = rect.y;
        int rightBottomX = leftTopX + rect.width;
        int rightBottomY = leftTopY + rect.height;

        //Opencv용 Rect를 Android Rect로 변환
        //Opencv => x,y,w,h
        //Android => leftTop, rightBottom
        android.graphics.Rect useRect =
                new android.graphics.Rect(leftTopX, leftTopY, rightBottomX, rightBottomY);
        mAIScoreView.SetBitmap(fileName, useRect);
    }
	
    private void doTextDetection(Mat alignedMat, Rect rect, int index, OCRRecognitionMode recogMode) {
        LOGGER.d("SallyDetect doTextDetection()");
//        //이미지 채점이 되어 있고 그 답이 맞는 답인 경우 화면에 채점 표시만 하고 리턴.
        if( mDataBase.get(index).mDoGrading == 1 && mDataBase.get(index).mIsCorrect == 1) {
            LOGGER.d("SallyRecog index : "+index+"-Grading is already done.");
            // 이미 채점 된것은 화면에 표시만 하고 리턴 : 버튼 클릭시 화면 표시하도록 바꿨으므로 아래 코드 주석처리함.
            //DrawStickerAndGradingForText(index);
            return;
        }

        Mat cropInputMat = new Mat(alignedMat, rect).clone();

        String answer = mDataBase.get(index).mAnswerText;

        // TODO detection 넣기.

        cropInputMat.release();
    }



    private void addTextRecognitionData(Mat alignedMat, Rect rect, int index, OCRRecognitionMode recogMode) {
        LOGGER.d("SallyRecog addTextRecognitionData()");

//        //이미지 채점이 되어 있고 그 답이 맞는 답인 경우 화면에 채점 표시만 하고 리턴.
        if( mDataBase.get(index).mDoGrading == 1 && mDataBase.get(index).mIsCorrect == 1) {
            LOGGER.d("SallyRecog index : "+index+"-Grading is already done.");
            // 이미 채점 된것은 화면에 표시만 하고 리턴 : 버튼 클릭시 화면 표시하도록 바꿨으므로 아래 코드 주석처리함.
            //DrawStickerAndGradingForText(index);
            return;
        }

        Mat cropInputMat = new Mat(alignedMat, rect).clone();

        String answer = mDataBase.get(index).mAnswerText;

        if(recogMode == HW_KOR_MULTI || recogMode == HW_ENG_MULTI) {
            Mat cropInputMatScnd = new Mat(mAlignedMatScndFrame, rect).clone();
            Mat cropInputMatThrd = new Mat(mAlignedMatThrdFrame, rect).clone();
            mOCRManager.AddOCRData(index, answer, cropInputMat,  recogMode, cropInputMatScnd, cropInputMatThrd);
            cropInputMatScnd.release();
            cropInputMatThrd.release();
        }
        else {
            mOCRManager.AddOCRData(index, answer, cropInputMat,  recogMode, null, null);
        }
        mRecogDataLength++;
        cropInputMat.release();
    }

    private void doTextScoring(int in_serialNumber, int in_isMultiRecognition, String in_recognizedText) {
        AIScoreReferenceDB.DataBase db = mDataBase.get(in_serialNumber);
        mAIScoreFunc.DoTextScoring(db, in_isMultiRecognition, in_recognizedText);
        LOGGER.d("SallyRecog doTextScoring() savedAnswer = " + mDataBase.get(in_serialNumber).mSaveAnswer);
        //화면에 채점 표시
        //DrawStickerAndGradingForText(in_serialNumber);

        //DoTextScoring() 에서 저장한 결과대로 파일에 기록한다.
        mAIScoreReferenceDB.WriteAIScoreResult("math");
    }

    public List<AIScoreReferenceDB.DataBase> GetDB(int coverIndex, int pageIndex) {
        mDataBase = mAIScoreReferenceDB.GetAIScoreMethod(coverIndex, pageIndex);
        return mDataBase;
    }
    public void DrawStickerAndGradingForText(int pageIdx, int index) {
        float x = mDataBase.get(index).mX;
        float y = mDataBase.get(index).mY;
        float w = mDataBase.get(index).mW;
        float h = mDataBase.get(index).mH;
        //AIScoreFunctions.DoTextScoring() 에서 셋팅하므로 아래코드 주석처리. 여기서 셋팅하면 안됨.
        //mDataBase.get(index).mDoGrading = 1;

        doDrawGradingForText(x, y, w, h, mDataBase.get(index).mIsCorrect);
        if( mDataBase.get(index).mSticker == AIScoreReferenceDB.Sticker.STICKER) {
            int itemIndex = mDataBase.get(index).mStickerFileNumber;
            if(mDataBase.get(index).mIsCorrect == 1) { //맞은 경우에만 스티커 그려줌
                drawStickerForText(x, y, w, h, pageIdx, itemIndex);
            }
        }
    }

    private OCRManager.RecogResultListener mRecogResultListener = new OCRManager.RecogResultListener() {
        @Override
        public void onRecogResultListener(Message msg) {
            if(msg.what == MSG_REQUEST_TEXT_SCORING) {
                String resultString = msg.obj.toString();
                int serialNumber = msg.arg1 % 100;
                int isMultiRecognition = msg.arg2;

                LOGGER.d("SallyRecog serialNumber : " + serialNumber + ", ResultString : " + resultString);
                // 어떤 페이지의 어느 위치의 텍스트 인식인지는 serialNumber로 알 수 있음. db의 인덱스와 일치함.

                doTextScoring(serialNumber, isMultiRecognition, resultString);

                // 인식된 개수를 카운팅.
                mRecognitionDoneCnt++;
                LOGGER.d("SallyRecog mRecognitionDoneCnt= " + mRecognitionDoneCnt + ", mDataLength= " + mRecogDataLength);
                if(mRecognitionDoneCnt == mRecogDataLength) {
                    //인식 진행된 개수와 전체 데이터수가 같아지면, 새로운 인식 요청을 받을 준비를 함.
                    mOCRManager.ClearOCRData();
                    mRecognitionDoneCnt = 0;
                    mRecogDataLength = 0;
                    mIsOCRDone = true;
                }
            }
        }
    };

    private void doDrawGradingForText(float xValue, float yValue, float width, float height, int isCorrect) {

        float xvalue = xValue;
        float yvalue = yValue;
        float widthValue = width;
        float heightValue = height;

        float x = (xvalue + widthValue / 2.f) * (mPreviewRenderWidth);
        float y = (yvalue + heightValue / 2.f) * (mPreviewRenderHeight-(40*mPreviewRenderHeight/mPreviewHeight));

        LOGGER.d("doGrading %f %f", x, y);

        Message msg = new Message();
        Rect rect = new Rect((int)x, (int)y, 300, 300);
        msg.what = DRAW_AISCORE;
        msg.arg1 = isCorrect;
        msg.obj = rect;
        mMainHandler.sendMessage(msg);
    }

    /**
     * 해당하는 위치에 스티커를 그려줌.
     * @param x
     * @param y
     * @param w
     * @param h
     * @param pageIndex
     * @param itemIndex
     */
    private void drawStickerForText(float x, float y, float w, float h, int pageIndex, int itemIndex) {

        //전체 사진 비교하는 곳으로 예외처리 하여야 합니다.
//        if( itemIndex == 9 ) { //아이템 갯수가 9개를 넘어가는 경우 있음.
//            itemIndex = 2;
//        }

        String fileName = "/sdcard/studyNet/DB/math/sticker/"+pageIndex+"-"+(itemIndex)+".png";
        LOGGER.d("MathfileName : "+fileName);

        int leftTopX = (int)(x * (float)mPreviewRenderWidth);
        int leftTopY = (int)(y * (float)(mPreviewRenderHeight-(40*mPreviewRenderHeight/mPreviewHeight)));
        int rightBottomX = leftTopX + (int)(w * (float)mPreviewRenderWidth);
        int rightBottomY = leftTopY + (int)(h * (float)(mPreviewRenderHeight-(40*mPreviewRenderHeight/mPreviewHeight)));

        LOGGER.d("leftTopX %d %d %d %d - %d", leftTopX, leftTopY, rightBottomX, rightBottomY, itemIndex);

        //Opencv용 Rect를 Android Rect로 변환
        //Opencv => x,y,w,h
        //Android => leftTop, rightBottom
        android.graphics.Rect useRect =
                new android.graphics.Rect(leftTopX, leftTopY, rightBottomX, rightBottomY);
        mAIScoreView.SetBitmap(fileName, useRect);

    }
	
	/**
     * 현재 페이지의 채점이 모두 끝났는지의 여부를 리턴한다.
     * DoProcess 루틴이 끝났는지와 문자인식이 끝났는지를 체크해서 모두 끝났으면 true를 리턴한다.
     * @return
     */
    public boolean GetGradingStatus() {
        if(mIsDoProcessDone == true && mIsOCRDone == true) {
            return true;
        }
        else {
            return false;
        }
    }
}
