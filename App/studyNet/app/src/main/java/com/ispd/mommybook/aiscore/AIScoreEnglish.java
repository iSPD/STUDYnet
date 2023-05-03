package com.ispd.mommybook.aiscore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.imageclassifier.ImageClassifierManager;
import com.ispd.mommybook.imageprocess.ImageProcessKeyPointMatch;
import com.ispd.mommybook.motion.MotionHandTrackingManager;
import com.ispd.mommybook.ocr.OCRManager;
import com.ispd.mommybook.ocr.OCRRecognitionMode;
import com.ispd.mommybook.motion.MotionMovingDetect;
import com.ispd.mommybook.utils.UtilsLogger;
import com.ispd.mommybook.utils.UtilsPlaySound;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

import static com.ispd.mommybook.MainHandlerMessages.DRAW_AISCORE;
import static com.ispd.mommybook.MainHandlerMessages.REQUEST_PICTURE_CAPTURE;
import static com.ispd.mommybook.MainHandlerMessages.RESET_DRAW_AISCORE;
import static com.ispd.mommybook.aiscore.AIScoreFunctions.MSG_REQUEST_TEXT_SCORING;
import static com.ispd.mommybook.aiscore.AIScoreManager.SECOND_FRAME;
import static com.ispd.mommybook.aiscore.AIScoreManager.THIRD_FRAME;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.Method.*;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.Sticker.NONE;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.TextLanguage.ENGLISH;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.TextLanguage.KOREAN;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.TextLanguage.NUMBER;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.TextLanguage.SIGN;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.HW_ENG_MULTI;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.HW_KOR_MULTI;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.HW_KOR_SINGLE;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.HW_NUM_SIGN;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.TYPO_ENG;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.TYPO_KOR;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.TYPO_NUM_SIGN;

import static org.opencv.core.Core.flip;
import static org.opencv.core.CvType.CV_8U;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.COLOR_GRAY2RGB;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2GRAY;
import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.photo.Photo.detailEnhance;
import static org.opencv.photo.Photo.edgePreservingFilter;

public class AIScoreEnglish {

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

    private ImageProcessKeyPointMatch mImageProcessKeyPointMatch = null;
    private ImageClassifierManager mImageClassifierManager = null;
    private float mCropDatas[];

    private boolean mTouchPressed[] = new boolean[] {false, false, false, false, false, false, false, false, false, false};
    private UtilsPlaySound mUtilsPlaySound = null;

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
    private int mClassIndex[][] = new int[8][3];
    private float mClassScore[][] = new float[8][3];

    /**
     * AIScoreEnglish
     *
     * @param handler
     * @param scoreView
     *
     * 문자인식, 이미지 매칭, 이미지 비교, 음원 재생 등 선언
     */
    public AIScoreEnglish(Context context, Handler handler, AIScoreView scoreView) {
        mContext = context;
        mMainHandler = handler;
        mAIScoreView = scoreView;

        mImageProcessKeyPointMatch = new ImageProcessKeyPointMatch();

        //String modelPath = "frozen_mobilenet_english2.tflite";

        String modelPath = "frozen_mobilenet_english5m.tflite";//이게 가장 정확
//        String modelPath = "frozen_mobilenet_english5m33.tflite";
//        String modelPath = "frozen_mobilenet_english75.tflite";
//        String modelPath = "frozen_mobilenet_english83.tflite";

//        String modelPath = "frozen_inceptionv3_english.tflite";
//        String modelPath = "frozen_mobilenet214sm_english.tflite";
//        String modelPath = "frozen_mobilenet214big2_english.tflite";
        //String labelPath = "labels_english_sticker2.txt";

        String labelPath = "labels_english_sticker5.txt";//이게 가장 정확
//        String labelPath = "labels_english_sticker5m3.txt";

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
        mUtilsPlaySound = new UtilsPlaySound();
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
    public boolean HaveMultiScoringPart(int coverIndex, int pageIndex, int []captureCount) {
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
                captureCount[0] = 0;
                return true;
            }
            else if(mDataBase.get(i).mMethod == IMAGE_MATCHING) {
                captureCount[0] = 0;
                return true;
            }
            else if(mDataBase.get(i).mMethod == IMAGE_CLASSIFICATION) {
                captureCount[0] = 3;
                return true;
            }
        }
        return false;
    }

    // 카메라 캡처 시점부터 채점 시작을 셋팅하기 위한 함수
    public void SetDoProcessFlagOn() {
        mIsDoProcessDone = false;
    }

	
    public void DoImageProcess(Mat alignedMat, int coverIndex, int pageIndex, int aiScoreResultOn, int processCount) {
        //mIsDoProcessDone = false;  //SetDoProcessFlagOn() 함수에서 셋팅함.
        mPageIndex = pageIndex;

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
                                    addTextRecognitionData(alignedMat, cropRect, i, HW_KOR_MULTI);
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
            else if(mDataBase.get(i).mMethod == IMAGE_CLASSIFICATION) { //Image Classification
                int cropX = (int)(mDataBase.get(i).mX * (float)alignedMat.cols());
                int cropY = (int)(mDataBase.get(i).mY * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음
                int cropW = (int)(mDataBase.get(i).mW * (float)alignedMat.cols());
                int cropH = (int)(mDataBase.get(i).mH * (float)(alignedMat.rows()-10*4*alignedMat.rows()/mPreviewHeight));//책의 아래 40픽셀이 떠 있음

//                int moreGapX = (int)((float)cropW * 0.25f);
//                int moreGapY = (int)((float)cropH * 0.25f);
//
//                moreGapX = -moreGapX;
//                moreGapY = -moreGapY;
//
//                cropX = cropX - moreGapX;
//                cropY = cropY - moreGapY;
//                cropW = cropW + (moreGapX * 2);
//                cropH = cropH + (moreGapY * 2);

                Rect cropRect = new Rect(cropX, cropY, cropW, cropH);

                doImageClassification(alignedMat, cropRect, i, aiScoreResultOn, processCount);
                mClassCount++;
            }
            else if(mDataBase.get(i).mMethod == IMAGE_MATCHING) { //Image Classification
//                //do image matching
//                int x = (int)(mDataBase.get(i).mX * (float)mPreviewWidth);
//                int y = (int)(mDataBase.get(i).mY * (float)(mPreviewHeight-10*4));
//                int w = (int)(mDataBase.get(i).mW * (float)mPreviewWidth);
//                int h = (int)(mDataBase.get(i).mH * (float)(mPreviewHeight-10*4));
//                String targetPath = mDataBase.get(i).mImagePath;
            }
            else if(mDataBase.get(i).mMethod == IMAGE_SUBTRACT) { //Image Subtracting

            }
        } //end of for

        // 문자인식 채점 데이터가 있다면 문자인식 시작하라고 요청하기
        if( (mOCRManager.IsRecognitonDone() == true) &&
                (mOCRManager.GetOCRDataSize() > 0) &&
                (mRecognitionDoneCnt == 0) ) {
            mIsOCRDone = false;
            mOCRManager.StartRecognitonDetection();
//            mRecognitionDoneCnt = 0;
        }

        mAIScoreReferenceDB.WriteAIScoreResult("english");

        mIsDoProcessDone = true;
    }

    public void DoSoundProcess(int coverIndex, int pageIndex, MotionHandTrackingManager handManager) {
        //DB에서 할일 가져오기
        mDataBase = mAIScoreReferenceDB.GetAIScoreMethod(coverIndex, pageIndex);

        LOGGER.d("pageIndex : %d, mDataBase size : %d", pageIndex, mDataBase.size());

        if( mDataBase.size() == 0 ) {
            LOGGER.d("Nothing to do");
        }

        //할일 순서대로 진행
        //왼쪽 페이지부터 좌, 우, 아래 순서대로 진행
        for(int i = 0; i < mDataBase.size(); i++) {
            if (mDataBase.get(i).mMethod == IMAGE_SOUND) {

                float x = mDataBase.get(i).mX;
                float y = mDataBase.get(i).mY;
                float x2 = x + mDataBase.get(i).mW;
                float y2 = y + mDataBase.get(i).mH;

                if(handManager.GetTouchPressed(x, y, x2, y2) == true || handManager.GetTouchPressed2(x, y, x2, y2) == true) {

                    LOGGER.d("[DoSoundProcess] Do index-"+i);

                    if( mTouchPressed[i] == false ) {
                        mTouchPressed[i] = true;

                        //do sound activity
                        String fileName = "english/" + mDataBase.get(i).mPlayPath;
                        boolean isRunning[] = {false};
                        if(mUtilsPlaySound.isSoundPlaying() == false) { //added by sally 테스트 해야함.
                            mUtilsPlaySound.DoPlaySound(fileName, isRunning);
                        }
                    }
                }
                else {
                    LOGGER.d("[DoSoundProcess] Nothing");
                    mTouchPressed[i] = false;
                }
            }
        }
    }

    public void ResetSoundProcess() {
        for(int i = 0; i < 10; i++) {
            mTouchPressed[i] = false;
        }
    }

    /**
     * doImageKeyPointMatching
     *
     * @param alignedMat
     * @param rect
     * @param targetFilePath
     * @param index
     *
     * 이미지 키포인트 매칭
     */
    private void doImageKeyPointMatching(Mat alignedMat, Rect rect, String targetFilePath, int index) {
        LOGGER.d("doImageKeyPointMatching : "+targetFilePath);
        //원본 이미지 가져오기
        Mat targetMat = imread(targetFilePath);

        //입력 영상의 처리할 영상 Crop하여 원본이미지 사이지로 변경
        Mat cropInputMat = new Mat(alignedMat, rect).clone();
        resize(cropInputMat, cropInputMat, targetMat.size());

        resize(cropInputMat, cropInputMat, new Size(500, 500));
        resize(targetMat, targetMat, new Size(500, 500));

        //각각 Gray Scale로 변경
        cvtColor(cropInputMat, cropInputMat, COLOR_RGBA2GRAY);
        cvtColor(targetMat, targetMat, COLOR_RGB2GRAY);

        mImageProcessKeyPointMatch.DoImageKeyPointMatch(cropInputMat, targetMat, index);
    }

    private void doImageClassification(Mat alignedMat, Rect rect, int index, int aiScoreResultOn, int processCount) {

        //이미지 채점이 되어 있고 그 답이 맞는 답인 경우 리턴.
        if( mDataBase.get(index).mDoGrading == 1 && mDataBase.get(index).mIsCorrect == 1) {
            LOGGER.d("doImageClassification() - index : "+index+"---> Grading is already done.");
            // 이미 채점 된것은 화면에 표시만 하고 리턴 : 버튼 클릭시 화면 표시하도록 바꿨으므로 아래 코드 주석처리함.
            //DrawStickerAndGradingForText(index);
            return;
        }

        for(int count = 0; count < 3; count++) {

            LOGGER.d("doImageClassification rect : " + rect);

            int gapX = rect.width / 10;
            int gapY = rect.height / 10;

            Rect newRect = new Rect(rect.x + gapX, rect.y + gapY, rect.width - gapX * 2, rect.height - gapY * 2);

            Mat cropInputMat = null;
            if( count == 0 ) {
                cropInputMat = new Mat(alignedMat, newRect).clone();
            }
            else if( count == 1 ) {
                cropInputMat = new Mat(mAlignedMatScndFrame, newRect).clone();
            }
            else if( count == 2 ) {
                cropInputMat = new Mat(mAlignedMatThrdFrame, newRect).clone();
            }
            LOGGER.d("cropInputMat : %d %d", cropInputMat.cols(), cropInputMat.rows());

            resize(cropInputMat, cropInputMat, new Size(cropInputMat.cols() * 4, cropInputMat.rows() * 4), INTER_CUBIC);
            //resize(cropInputMat, cropInputMat, new Size(cropInputMat.cols()*4, cropInputMat.rows()*4), INTER_CUBIC);

            //imwrite("/sdcard/studyNet/DEBUG/cropInputMat-" + index + ".jpg", cropInputMat);

            mCropDatas = new float[]
                    {
                            cropInputMat.cols() / 4.f, cropInputMat.rows() / 4.f,
                            cropInputMat.cols() * 3.f / 4.f, cropInputMat.rows() / 4.f,
                            cropInputMat.cols() / 4.f, cropInputMat.rows() * 3.f / 4.f,
                            cropInputMat.cols() * 3.f / 4.f, cropInputMat.rows() * 3.f / 4.f
                    };

            mImageClassifierManager.RunInference(cropInputMat, mCropDatas, false, true);
            String stickerName = mImageClassifierManager.GetName();
            int stickerIndex = mImageClassifierManager.GetIndex();
            float stickerScore = mImageClassifierManager.GetScore();

            //drwaStickerAndGrading(index, stickerName, aiScoreResultOn);

//            int keypointCountGo = mImageProcessKeyPointMatch.FindImageKeyPoint(cropInputMat, index);
//            if (keypointCountGo < 20) {//Tuning Point
//                stickerName = "background";
//            }
//
            //LOGGER.d("[AIScoreManager] index : %d, stickerName1(%d) : %s, pageIndex : %d, pageScore : %f", index, keypointCountGo, stickerName, stickerIndex, stickerScore);
            LOGGER.d("[AIScoreManager] index : %d, stickerName1(%d) : %s, pageIndex : %d, pageScore : %f", index, 0, stickerName, stickerIndex, stickerScore);
//            drwaStickerAndGrading(index, stickerName, stickerScore, aiScoreResultOn);

            String stickerName2 = mImageClassifierManager.GetName2();
            int stickerIndex2 = mImageClassifierManager.GetIndex2();
            float stickerScore2 = mImageClassifierManager.GetScore2();

//            if (stickerName.equals("background") == true) {
//                LOGGER.d("Exception for background1 : " + stickerName);
//                LOGGER.d("Exception for background2 : " + stickerName2);
//
//                stickerName = stickerName2;
//                stickerIndex = stickerIndex2;
//                //stickerScore = stickerScore2;
//            }

//            mClassIndex[mClassCount][processCount] = stickerIndex;
//            mClassScore[mClassCount][processCount] = stickerScore;

            LOGGER.d("mClassCount : %d, count : %d", mClassCount, count);
            mClassIndex[mClassCount][count] = stickerIndex;
            mClassScore[mClassCount][count] = stickerScore;

            //LOGGER.d("[AIScoreManager] index : %d, stickerName2 : %s, pageIndex2 : %d, pageScore2 : %f", index, stickerName2, stickerIndex2, stickerScore2);

//            if (processCount == 2) {
            if( count == 2 ) {

                int labelSize = mImageClassifierManager.GetLabelSize();
                LOGGER.d("labelSize : " + labelSize);

                int resultIndex[] = new int[1];
                float resultScore[] = new float[1];
                calcAverageClass(labelSize, mClassIndex[mClassCount], mClassScore[mClassCount], resultIndex, resultScore);

                int newIndex = resultIndex[0];
                stickerName = mImageClassifierManager.GetLabelName(newIndex);
                LOGGER.d("getstickerName : " + stickerName);
                stickerScore = resultScore[0];

                int keypointCount = mImageProcessKeyPointMatch.FindImageKeyPoint(cropInputMat, index);
//                //if( processCount == 2 && keypointCount == 0 ) {
//                //if (processCount == 2 && keypointCount < 50) {
                if ( keypointCount < 30 ) {
                    stickerName = "background";
                }

                LOGGER.d("[AIScoreManager] index : %d, stickerName3 : %s(%d), pageIndex : %d, pageScore : %f", index, stickerName, keypointCount, stickerIndex, stickerScore);
//                LOGGER.d("[AIScoreManager] index : %d, stickerName3 : %s(%d), pageIndex : %d, pageScore : %f", index, stickerName, 0, stickerIndex, stickerScore);

                drawStickerAndGrading(index, stickerName, stickerScore, aiScoreResultOn);
            }
        }
    }

    private void calcAverageClass(int labelSize, int index[], float score[], int resultIndex[], float resultScore[]) {
        int indexCount[] = new int[labelSize];
        float ScoreSum[] = new float[labelSize];

        for(int i = 0; i < index.length; i++) {
            LOGGER.d("[calcAverageClass] index[%d] : %s", i, mImageClassifierManager.GetLabelName(index[i]));
            LOGGER.d("[calcAverageClass] score[%d] : %f", i, score[i]);
        }

        for(int i = 0; i < labelSize; i++) {
            indexCount[i] = 0;
            ScoreSum[i] = 0.f;
        }

        for(int i = 0; i < index.length; i++) {
            for(int j = 0; j < labelSize; j++) {
                if( index[i] == j ) {
                    indexCount[j]++;
                    ScoreSum[j] = ScoreSum[j] + score[i];
                }
            }
        }

        int maxCount = -1;
        int maxIndex = -1;
        for(int i = 0; i < labelSize; i++) {
            if( indexCount[i] > maxCount ) {
                maxCount = indexCount[i];
                maxIndex = i;
            }
        }

        resultIndex[0] = maxIndex;
        resultScore[0] = ScoreSum[maxIndex] / maxCount;

        LOGGER.d("[calcAverageClass] resultIndex : %s", mImageClassifierManager.GetLabelName(resultIndex[0]));
        LOGGER.d("[calcAverageClass] resultScore : %f", resultScore[0]);
    }

    private void drawStickerAndGrading(int index, String stickerName, float score, int aiScoreResultOn) {
        //mMainHandler.sendEmptyMessage(RESET_DRAW_AISCORE);
        //mAIScoreView.ResetBitmap();

        String correctName = mDataBase.get(index).mCorrectName;

        float x = mDataBase.get(index).mX;
        float y = mDataBase.get(index).mY;
        float w = mDataBase.get(index).mW;
        float h = mDataBase.get(index).mH;

        LOGGER.d("index "+index+"-stickerName : "+stickerName+", correctName : "+correctName+", aiScoreResultOn : "+aiScoreResultOn);

        if( stickerName.equals("background") == false && stickerName.contains(correctName) == true && score > 0.1f) {

            mDataBase.get(index).mDoGrading = 1;

            //if( aiScoreResultOn == 1 ) {
            if( true ) {
                mDataBase.get(index).mIsCorrect = 1;
//                doDrawGrading(x, y, w, h, 1);

//                if( mDataBase.get(index).mSticker == AIScoreReferenceDB.Sticker.STICKER) {
//                    int itemIndex = mDataBase.get(index).mStickerFileNumber;
//                    drawSticker(x, y, w, h, mPageIndex, itemIndex);
//                }
            }
        }
        else if( stickerName.equals("background") == false ) {

            if( index == 9 && score > 0.5f ) {
                if( mDataBase.get(index).mSticker == AIScoreReferenceDB.Sticker.STICKER) {

                    int itemIndex = 1;
                    if( stickerName.equals("cookie") == true ) {
                        itemIndex = 1;
                    }
                    else if( stickerName.equals("candy") == true ) {
                        itemIndex = 2;
                    }
                    else if( stickerName.equals("chocolate") == true ) {
                        itemIndex = 3;
                    }
                    else if( stickerName.equals("donut") == true ) {
                        itemIndex = 4;
                    }
                    else if( stickerName.equals("cake") == true ) {
                        itemIndex = 5;
                    }

                    drawSticker(x, y, w, h, mPageIndex, itemIndex);
                }
            }
            else if( score > 0.55f ) {
                //if( aiScoreResultOn == 1 ) {
                if( true ) {
                    mDataBase.get(index).mDoGrading = 1;
                    mDataBase.get(index).mIsCorrect = 0;
//                    doDrawGrading(x, y, w, h, 0);
                }
            }
        }
        else {
            mDataBase.get(index).mIsCorrect = -1;
        }
    }

    /**
     * drawSticker
     *
     * @param pageIndex
     * @param itemIndex
     *
     * 해당하는 위치에 스티커 그려주기
     */
    private void drawSticker(float x, float y, float w, float h, int pageIndex, int itemIndex) {

        //sally : 스티커번호가 9번인 경우가 있어서 아래 코드 주석처리함.
        //전체 사진 비교하는 곳으로 예외처리 하여야 합니다.
//        if( itemIndex == 9 ) {
//            itemIndex = 2;
//        }

        String fileName = "/sdcard/studyNet/DB/english/sticker/"+pageIndex+"-"+(itemIndex)+".png";
        LOGGER.d("EngfileName : "+fileName);

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

        // TODO detection 넣기.

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
        // 집단채점 결과를 그대로 적으면 공백때문에 다음에 앱을 열었을때 result.txt 파싱하다 죽으므로 공백 없이 답 저장.
        mAIScoreReferenceDB.WriteAIScoreResult("english");

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

        if(mDataBase.get(index).mMethod != IMAGE_SOUND &&
           mDataBase.get(index).mMethod != IMAGE_BUTTON) {
            doDrawGrading(x, y, w, h, mDataBase.get(index).mIsCorrect);
        }

        if( mDataBase.get(index).mSticker == AIScoreReferenceDB.Sticker.STICKER) {
            int itemIndex = mDataBase.get(index).mStickerFileNumber;
            if(mDataBase.get(index).mIsCorrect == 1) { //맞은 경우에만 스티커 그려줌
                drawSticker(x, y, w, h, pageIdx, itemIndex);
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
