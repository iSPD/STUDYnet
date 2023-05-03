package com.ispd.mommybook.aiscore;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.imageprocess.ImageProcessKeyPointMatch;
import com.ispd.mommybook.imageprocess.ImageProcessSubtraction;
import com.ispd.mommybook.motion.MotionHandTrackingManager;
import com.ispd.mommybook.ocr.OCRManager;
import com.ispd.mommybook.ocr.OCRRecognitionMode;
import com.ispd.mommybook.utils.UtilsLogger;
import com.ispd.mommybook.utils.UtilsPlaySound;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;

import java.util.List;

import static com.ispd.mommybook.MainHandlerMessages.DRAW_AISCORE;
import static com.ispd.mommybook.aiscore.AIScoreFunctions.MSG_REQUEST_TEXT_SCORING;
import static com.ispd.mommybook.aiscore.AIScoreManager.SECOND_FRAME;
import static com.ispd.mommybook.aiscore.AIScoreManager.THIRD_FRAME;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.Method.*;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.Sticker.NONE;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.TextLanguage.*;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.*;


import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.*;

/**
 * AIScoreKorean
 *
 * @author Daniel
 * @version 1.0
 */
public class AIScoreKorean {

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

    private boolean mTouchPressed = false;

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
    private boolean mIsDoProcessDone = true;
    private boolean mIsOCRDone = true;

    /**
     * AIScoreKorean
     *
     * @param handler
     * @param scoreView
     *
     * 문자인식, 이미지 매칭, 이미지 비교 등 선언
     */
    public AIScoreKorean(Context context, Handler handler, AIScoreView scoreView) {
        mContext = context;
        mMainHandler = handler;
        mAIScoreView = scoreView;

        mImageProcessSubtraction = new ImageProcessSubtraction();
        mImageProcessKeyPointMatch = new ImageProcessKeyPointMatch();
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
                captureCount[0] = 1;
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

        //할일 순서대로 진행
        //왼쪽 페이지부터 좌, 우, 아래 순서대로 진행
        for(int i = 0; i < mDataBase.size(); i++) {

            LOGGER.d("[processCount] mDataBase.get(i).mMethod : "+mDataBase.get(i).mMethod);

            if(mDataBase.get(i).mMethod == TEXT_RECOGNITION_WORD ||
               mDataBase.get(i).mMethod == TEXT_RECOGNITION_SENTENCE) {
                if(mOCRManager.IsRecognitonDone() == true && (mRecognitionDoneCnt == 0)) {
                    //*      //do ocr
                    int cropX = (int) (mDataBase.get(i).mX * (float) alignedMat.cols());
                    int cropY = (int) (mDataBase.get(i).mY * (float) (alignedMat.rows() - 10 * 4 * alignedMat.rows() / mPreviewHeight));//책의 아래 40픽셀이 떠 있음
                    int cropW = (int) (mDataBase.get(i).mW * (float) alignedMat.cols());
                    int cropH = (int) ((mDataBase.get(i).mH * (float) (alignedMat.rows() - 10 * 4 * alignedMat.rows() / mPreviewHeight)));//책의 아래 40픽셀이 떠 있음
                    Rect cropRect = new Rect(cropX, cropY, cropW, cropH);

                    if (mDataBase.get(i).mSticker == NONE) { //손글씨 모드임.
                        if (mDataBase.get(i).mTextLanguage == KOREAN) {
                            if (mDataBase.get(i).mMethod == TEXT_RECOGNITION_SENTENCE) {
                                doTextDetection(alignedMat, cropRect, i, HW_KOR_SINGLE);
                            } else {  //word recognition
                                if (mDataBase.get(i).mIndex == 4) { // TODO 임시코드, 국어 9쪽 띄어쓰기 문제인 경우 집단채점에서 제외. 추후 DB에 집단채점제외여부 를 추가하기.
                                    addTextRecognitionData(alignedMat, cropRect, i, HW_KOR_SINGLE);
                                } else {
                                    addTextRecognitionData(alignedMat, cropRect, i, HW_KOR_MULTI);
                                }
                            }
                        } else if (mDataBase.get(i).mTextLanguage == ENGLISH) {
                            addTextRecognitionData(alignedMat, cropRect, i, HW_ENG_MULTI);
                        } else if (mDataBase.get(i).mTextLanguage == NUMBER) {
                            addTextRecognitionData(alignedMat, cropRect, i, HW_NUM_SIGN);
                        } else if (mDataBase.get(i).mTextLanguage == SIGN) {
                            addTextRecognitionData(alignedMat, cropRect, i, HW_NUM_SIGN);
                        }
                    } else {//스티커모드
                        if (mDataBase.get(i).mTextLanguage == KOREAN) {
                            addTextRecognitionData(alignedMat, cropRect, i, TYPO_KOR);
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
                //do image matching
//                int x = (int)(mDataBase.get(i).mX * (float)mPreviewWidth);
//                int y = (int)(mDataBase.get(i).mY * (float)(mPreviewHeight-10*4));//책의 아래 40픽셀이 떠 있음
//                int w = (int)(mDataBase.get(i).mW * (float)mPreviewWidth);
//                int h = (int)(mDataBase.get(i).mH * (float)(mPreviewHeight-10*4));//책의 아래 40픽셀이 떠 있음
//                String targetPath = mDataBase.get(i).mImagePath;

                int x = (int)(mDataBase.get(i).mX * (float)alignedMat.cols());
                int y = (int)(mDataBase.get(i).mY * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음
                int w = (int)(mDataBase.get(i).mW * (float)alignedMat.cols());
                int h = (int)(mDataBase.get(i).mH * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음

                //doImageSubtract(alignedMat, new Rect(x, y, w, h), targetPath, i);
                if( pageIndex != 5 ) {
                    //doImageKeyPointMatching(alignedMat, new Rect(x, y, w, h), targetPath, i);
                    //문자 동그라미 페이지-수학의 시계 동그라미 알아내는 방식으로 시도-2
                    doImageSubtractWithFirstFrame(saveAlignedMat, alignedMat, new Rect(x, y, w, h), pageIndex, i, aiScoreResultOn, processCount);
                }
            }
            else if(mDataBase.get(i).mMethod == IMAGE_SUBTRACT) {
                //do image subtract
//                int x = (int)(mDataBase.get(i).mX * (float)mPreviewWidth);
//                int y = (int)(mDataBase.get(i).mY * (float)(mPreviewHeight-10*4));//책의 아래 40픽셀이 떠 있음
//                int w = (int)(mDataBase.get(i).mW * (float)mPreviewWidth);
//                int h = (int)(mDataBase.get(i).mH * (float)(mPreviewHeight-10*4));//책의 아래 40픽셀이 떠 있음

                int x = (int)(mDataBase.get(i).mX * (float)alignedMat.cols());
                int y = (int)(mDataBase.get(i).mY * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음
                int w = (int)(mDataBase.get(i).mW * (float)alignedMat.cols());
                int h = (int)(mDataBase.get(i).mH * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음

                String targetPath = mDataBase.get(i).mImagePath;

                //문자 동그라미 페이지-키포인트 매칭해서 빼기인데, 키포인트가 불안하면 안좋음. 시도-1
                doImageKeyPointMatchingNSubtract(alignedMat, new Rect(x, y, w, h), targetPath, i);
            }
            else if(mDataBase.get(i).mMethod == IMAGE_BUTTON) {
                //do button activity
            }
            else if(mDataBase.get(i).mMethod == TEXT_NARRITIVE) {
                //세모 띄우고 선생님 채점 표시 하기.
                int x = (int)(mDataBase.get(i).mX * (float)alignedMat.cols());
                int y = (int)(mDataBase.get(i).mY * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음
                int w = (int)(mDataBase.get(i).mW * (float)alignedMat.cols());
                int h = (int)(mDataBase.get(i).mH * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음

                askTeacherToGrade(alignedMat, new Rect(x, y, w, h), i);
            }
        }
        // 문자인식 채점 데이터가 있다면 문자인식 시작하라고 요청하기
        if( (mOCRManager.IsRecognitonDone() == true) &&
                 (mOCRManager.GetOCRDataSize() > 0) &&
                            (mRecognitionDoneCnt == 0) ) {
            mIsOCRDone = false;
            mOCRManager.StartRecognitonDetection();
        }
        mAIScoreReferenceDB.WriteAIScoreResult("korean");
        mIsDoProcessDone = true;
    }

    public boolean DoTouchButtonProcess(int coverIndex, int pageIndex, MotionHandTrackingManager handManager) {
        //DB에서 할일 가져오기
        mDataBase = mAIScoreReferenceDB.GetAIScoreMethod(coverIndex, pageIndex);

        LOGGER.d("pageIndex : %d, mDataBase size : %d", pageIndex, mDataBase.size());

        if( mDataBase.size() == 0 ) {
            LOGGER.d("Nothing to do");
        }

        //할일 순서대로 진행
        //왼쪽 페이지부터 좌, 우, 아래 순서대로 진행
        for(int i = 0; i < mDataBase.size(); i++) {
            if (mDataBase.get(i).mMethod == IMAGE_BUTTON) {

                float x = mDataBase.get(i).mX;
                float y = mDataBase.get(i).mY;
                float x2 = x + mDataBase.get(i).mW;
                float y2 = y + mDataBase.get(i).mH;

                if(handManager.GetTouchPressed(x, y, x2, y2) == true || handManager.GetTouchPressed2(x, y, x2, y2) == true) {

                    LOGGER.d("[DoTouchButtonProcess] Do index-"+i);

                    if( mTouchPressed == false ) {
                        mTouchPressed = true;
                    }
                }
                else {
                    LOGGER.d("[DoTouchButtonProcess] Nothing");
                    mTouchPressed = false;
                }
            }
        }

        return mTouchPressed;
    }

    /**
     * doImageSubtract
     *
     * @param alignedMat
     * @param rect
     * @param targetFilePath
     * @param index
     *
     * 이미지 차이 구하기
     */
    private void doImageSubtract(Mat alignedMat, Rect rect, String targetFilePath, int index) {
        LOGGER.d("doImageSubtract : "+targetFilePath);
        //원본 이미지 가져오기
        Mat targetMat = imread(targetFilePath);

        //입력 영상의 처리할 영상 Crop하여 원본이미지 사이지로 변경
        Mat cropInputMat = new Mat(alignedMat, rect).clone();
        resize(cropInputMat, cropInputMat, targetMat.size());

        //각각 Gray Scale로 변경
        cvtColor(cropInputMat, cropInputMat, COLOR_RGBA2GRAY);
        cvtColor(targetMat, targetMat, COLOR_RGB2GRAY);

        imwrite("/sdcard/studyNet/DEBUG/cropInputMat-"+index+".jpg", cropInputMat);

        mImageProcessSubtraction.DoImageSubtractCircle(cropInputMat, targetMat, index);
    }

    /**
     * doImageSubtractWithFirstFrame
     *
     * @param alignedMat
     * @param rect
     *
     */
    private void doImageSubtractWithFirstFrame(Mat saveAlignedMat, Mat alignedMat, Rect rect, int pageIndex, int itemIndex, int aiScoreResultOn, int processCount) {

        if( mDataBase.get(itemIndex).mDoGrading == 1 && mDataBase.get(itemIndex).mIsCorrect == 1) {
            LOGGER.d("SallyRecog index : "+itemIndex+"---> Grading is already done.");
            //채점 결과 그려주기
            //drawResult(itemIndex, true);
            return;
        }

        //입력 영상의 처리할 영상 Crop하여 원본이미지 사이지로 변경
        Mat cropInputMat = new Mat(alignedMat, rect).clone();
        Mat targetMatPre = saveAlignedMat.clone();
//        Mat targetMat = saveAlignedMat.clone();
        Mat targetMat = new Mat(targetMatPre, rect).clone();
        resize(targetMat, targetMat, cropInputMat.size());

        float changeRate[] = new float[3];

        for(int i = 0; i < 2; i++) {

            Mat cropInputMat2 = new Mat(cropInputMat, new Rect(0 + cropInputMat.cols() * i / 2, 0, cropInputMat.cols() / 2, cropInputMat.rows())).clone();
            Mat targetMat2 = new Mat(targetMat, new Rect(0 + targetMat.cols() * i / 2, 0, targetMat.cols() / 2, targetMat.rows())).clone();

            imwrite("/sdcard/studyNet/DEBUG/korean/"+itemIndex+"-cropInputMat2-"+i+".jpg", cropInputMat2);
            imwrite("/sdcard/studyNet/DEBUG/korean/"+itemIndex+"-targetMat2-"+i+".jpg", targetMat2);

            changeRate[i] = JniController.findCircle(cropInputMat2.getNativeObjAddr(), targetMat2.getNativeObjAddr(), i, itemIndex, 0);

            cropInputMat2.release();
            targetMat2.release();
        }

        LOGGER.d(itemIndex+"-changeRate %f %f", changeRate[0], changeRate[1]);

        //채점 결과 그려주기
        int detectedLocation = -1;
        if( changeRate[0] > 110.f ) {
            detectedLocation = 0;
        }
        else if( changeRate[1] > 110.f ) {
            detectedLocation = 1;
        }
        else {
            detectedLocation = -1;
        }

        int correctLocation = mDataBase.get(itemIndex).mImageSubtractLocation.value();

        LOGGER.d("correctLocation : "+correctLocation+", detectedLocation : "+detectedLocation);
        if( correctLocation == detectedLocation ) {
            //drawResult(itemIndex, true);
            mDataBase.get(itemIndex).mDoGrading = 1;
			mDataBase.get(itemIndex).mIsCorrect = 1;
        }
        else if( detectedLocation != -1 )
        {
            //drawResult(itemIndex, false);
            mDataBase.get(itemIndex).mDoGrading = 0;
			mDataBase.get(itemIndex).mIsCorrect = 0;
        }
        targetMat.release();
        targetMatPre.release();
    }

    /**
     * doImageKeyPointMatchingNSubtract
     *
     * @param alignedMat
     * @param rect
     * @param targetFilePath
     * @param index
     *
     * 이미지 키포인트 매칭
     */
    private void doImageKeyPointMatchingNSubtract(Mat alignedMat, Rect rect, String targetFilePath, int index) {
        LOGGER.d("doImageKeyPointMatching : "+targetFilePath);

        //이미지 채점이 되어 있고 그 답이 맞는 답인 경우 리턴.
        if( mDataBase.get(index).mDoGrading == 1 && mDataBase.get(index).mIsCorrect == 1) {
            LOGGER.d("SallyRecog index : "+index+"---> Grading is already done.");
            // 이미 채점 된것은 화면에 표시만 하고 리턴 : 버튼 클릭시 화면 표시하도록 바꿨으므로 아래 코드 주석처리함.
            //DrawStickerAndGradingForText(index);
            return;
        }

        //원본 이미지 가져오기
        Mat targetMat = new Mat();
        targetMat = imread(targetFilePath);

        //입력 영상의 처리할 영상 Crop하여 원본이미지 사이지로 변경
        Mat cropInputMat = new Mat();
        cropInputMat = new Mat(alignedMat, rect).clone();

        LOGGER.d("cropInputMat : "+cropInputMat.size()+", targetMat : "+targetMat.size());

        resize(cropInputMat, cropInputMat, new Size(cropInputMat.cols()*4, cropInputMat.rows()*4), INTER_CUBIC);
        resize(targetMat, targetMat, new Size(targetMat.cols()*4, targetMat.rows()*4), INTER_CUBIC);

        //각각 Gray Scale로 변경
        cvtColor(cropInputMat, cropInputMat, COLOR_RGB2GRAY);
        cvtColor(targetMat, targetMat, COLOR_RGB2GRAY);

        int correctLocation = mDataBase.get(index).mImageSubtractLocation.value();
        int detectedLocation = mImageProcessKeyPointMatch.DoImageKeyPointMatch(cropInputMat, targetMat, index);

        LOGGER.d("correctLocation : "+correctLocation+", detectedLocation : "+detectedLocation);
        if( correctLocation == detectedLocation ) {
//            drawResult(index, true);
            mDataBase.get(index).mDoGrading = 1;
            mDataBase.get(index).mIsCorrect = 1;

        }
        else if( detectedLocation != -1 )
        {
//            drawResult(index, false);
            mDataBase.get(index).mDoGrading = 1;
            mDataBase.get(index).mIsCorrect = 0;
        }
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
        String fileName = "/sdcard/studyNet/DB/korean/sticker/"+pageIndex+"-"+(itemIndex+1)+".png";

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

    private boolean checkStickerOn(Mat alignedMat, Rect rect, int pageIndex, int itemIndex) {

        //원본 이미지 Gray Scale로 가져오기
        String fileName = "/sdcard/studyNet/DB/korean/sticker/"+pageIndex+"-"+(itemIndex+1)+".png";
        LOGGER.d("[checkStickerOn] fileName : "+fileName);
        Mat targetMat = imread(fileName, IMREAD_GRAYSCALE);
        LOGGER.d("[checkStickerOn] targetMat : "+targetMat.size());

        //입력 영상의 처리할 영상 Crop하여 원본이미지 사이지로 변경
        Mat cropInputMat = new Mat(alignedMat, rect).clone();
        resize(cropInputMat, cropInputMat, targetMat.size());

        //Gray Scale로 변경
        cvtColor(cropInputMat, cropInputMat, COLOR_RGBA2GRAY);

        mImageProcessSubtraction.DoImageSubtract(cropInputMat, targetMat, itemIndex);

        return false;
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
    private void drawSticker(float x, float y, float w, float h, int pageIndex, int itemIndex) {

        //전체 사진 비교하는 곳으로 예외처리 하여야 합니다.
//        if( itemIndex == 9 ) {
//            itemIndex = 2;
//        }

        String fileName = "/sdcard/studyNet/DB/korean/sticker/"+pageIndex+"-"+(itemIndex)+".png";
        LOGGER.d("KorfileName : "+fileName);

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

    private void doDrawGrading(float xValue, float yValue, float width, float height, int isCorrect) {

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

        //mOCRManager2.DetectWordsInScoringArea(cropInputMat); //TODO : 좌표 결과를 받든가 하기.
        //TODO mOCRManager.DetectWordsInScoringArea(cropInputMat); //TODO : 좌표 결과를 받든가 하기.

        //Recognition : TODO - sally 여러개 단어를 인식하는 루틴으로 수정하기. detection이 쓰레드로 되어 있으므로 핸들러에게 연락이 오면 recognition을 시작해야 할듯함.
//        mOCRManager.RecognizeWord(cropInputMat, index, answer, recogMode);

        cropInputMat.release();
    }

    private void addTextRecognitionData(Mat alignedMat, Rect rect, int index, OCRRecognitionMode recogMode) {
        LOGGER.d("SallyRecog addTextRecognitionData()");
        //이미지 채점이 되어 있고 그 답이 맞는 답인 경우 화면에 채점 표시만 하고 리턴.
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

        // 화면에 채점 표시 : 버튼 클릭시 표시 하도록 바꿨기 때문에 아래 코드 주석처리함. 210915
        // DrawStickerAndGradingForText(in_serialNumber);

        // DoTextScoring() 에서 저장한 결과대로 파일에 기록한다.
        // DoProcess 루틴이 끝난 후일 수 있으므로 한 번 더 저장하는 것임.
        mAIScoreReferenceDB.WriteAIScoreResult("korean");
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
        doDrawGrading(x, y, w, h, mDataBase.get(index).mIsCorrect);

        if( mDataBase.get(index).mSticker == AIScoreReferenceDB.Sticker.STICKER) {
            int itemIndex = mDataBase.get(index).mStickerFileNumber;
            if(mDataBase.get(index).mIsCorrect == 1) { //맞은 경우에만 스티커 그려줌
                drawSticker(x, y, w, h, pageIdx, itemIndex);
            }
        }
    }

   /**
     * 서술형 문제의 경우 세모표기 후 선생님 채점 문구를 화면에 표시
     * @param alinedMat
     * @param rect
     */
    private void askTeacherToGrade(Mat alinedMat, Rect rect, int index) {
//        세모표시 후 선생님채점 표시
        mDataBase.get(index).mDoGrading = 1;
        mDataBase.get(index).mIsCorrect = 2;

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

    private void drawResult(int index, boolean correct) {
        //do text recognition
        //현재는 무조건 해당 위치에 채점하기
        float x2 = (mDataBase.get(index).mX + mDataBase.get(index).mW / 2.f) * MainActivity.gPreviewRenderWidth;
        float y2 = (mDataBase.get(index).mY + mDataBase.get(index).mH / 2.f) * MainActivity.gPreviewRenderHeight;

        Message msg = new Message();
        Rect rect = new Rect((int)x2, (int)y2, 300, 300);
        msg.what = DRAW_AISCORE;
        msg.obj = rect;
        msg.arg1 = correct == true ? 1 : 0;
        mMainHandler.sendMessage(msg);
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
