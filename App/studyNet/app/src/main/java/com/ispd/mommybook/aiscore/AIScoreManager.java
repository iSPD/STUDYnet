package com.ispd.mommybook.aiscore;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.R;
import com.ispd.mommybook.activities.ActivitiesManager;
import com.ispd.mommybook.motion.MotionHandTrackingManager;
import com.ispd.mommybook.motion.MotionMovingDetect;
import com.ispd.mommybook.ocr.recognition.OCRRecognition;
import com.ispd.mommybook.utils.UtilsLogger;
import com.ispd.mommybook.utils.UtilsPlaySound;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.List;

import pl.droidsonroids.gif.GifImageView;
import static com.ispd.mommybook.MainActivity.gPreviewRenderHeight;
import static com.ispd.mommybook.MainActivity.gPreviewRenderWidth;
import static com.ispd.mommybook.MainHandlerMessages.DRAW_AISCORE_MULTI;
import static com.ispd.mommybook.MainHandlerMessages.REMOVE_AISCORE_MULTI;
import static com.ispd.mommybook.MainHandlerMessages.REQUEST_PICTURE_CAPTURE;
import static com.ispd.mommybook.MainHandlerMessages.RESET_DRAW_AISCORE;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.Method.*;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.Sticker.NONE;
import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.*;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.*;

/**
 * AIScoreManager
 *
 * @author Daniel
 * @version 1.0
 */
public class AIScoreManager {

    private static final UtilsLogger LOGGER = new UtilsLogger();
    public static final int SECOND_FRAME = 1; //FIXED
    public static final int THIRD_FRAME = 2; //FIXED

//    static {
//        System.loadLibrary("opencv_java4");
//        System.loadLibrary("native-lib");
//    }


    private Context mContext = null;
    private Handler mMainHandler = null;

    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    private int mPreviewWidth = MainActivity.gCameraPreviewWidth;
    private int mPreviewHeight = MainActivity.gCameraPreviewHeight;

    private int mCoverIndex = -1;
    private int mPageIndex = -1;

    private AIScoreKorean mAIScoreKorean = null;
    private AIScoreMath mAIScoreMath = null;
    private AIScoreEnglish mAIScoreEnglish = null;
    private AIScoreView mAIScoreView = null;
    private FrameLayout mAIScoreFrameLayout = null;

    private AIScoreUtils mAIScoreUtils1;
    private AIScoreUtils mAIScoreUtils2;
    private Mat mAlignedMat = new Mat();
    private Mat mSaveAlignedMat = new Mat();

    private AIScoreReferenceDB mAIScoreReferenceDB = new AIScoreReferenceDB();
    private List<AIScoreReferenceDB.DataBase> mDataBase;

    private Thread mAIScoreThread = null;
    private Thread mHandPointListenThread = null;

    private MotionMovingDetect mMotionMovingDetect = null;
    private boolean mAiScoreRunning = false;

    private MotionHandTrackingManager mMotionHandTrackingManager;

    private ImageButton mDPAIScoreBtn = null;
    private ImageButton mDoAIScoreBtn = null;
    private ImageView mSoundImageView = null;

    private int mProcessCount = 0;
    private boolean mProcessRunning = false;

    private boolean mImageMathchingOn = false;

    private boolean mSaveFirstFrameStarted = false;

    private ActivitiesManager mActivitiesManager = null;

	/**
	 * 채점 루틴이 모두 끝났는지의 여부.
	 */
    private boolean mIsGradingAllDone = true;
    private UtilsPlaySound mUtilsPlaySound = null;
	
    /**
     * AIScoreManager
     * @param context
     * @param handler
     *
     * 뷰 생성 => 스티커 화면에 표시 용도
     * 국어, 수학, 영어 생성 및 관리
     */
    public AIScoreManager(Context context, Handler handler) {

        mContext = context;
        mMainHandler = handler;

        mHandlerThread = new HandlerThread("AIScoreManager");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mAIScoreView = ((Activity)mContext).findViewById(R.id.cv_aiscore_view);
        mAIScoreView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();
                LOGGER.d("SallyRecog mAIScoreView.onTouch() %f %f", x, y);
                DisplayMultiScoringResult(mCoverIndex, mPageIndex, event.getX(), event.getY());
                return false;
            }
        });
        mAIScoreKorean = new AIScoreKorean(mContext, mMainHandler, mAIScoreView);
        mAIScoreMath = new AIScoreMath(mContext, mMainHandler, mAIScoreView);
        mAIScoreEnglish = new AIScoreEnglish(mContext, mMainHandler, mAIScoreView);

        mAIScoreUtils1 = new AIScoreUtils();
        mAIScoreUtils2 = new AIScoreUtils();

        mMotionMovingDetect = new MotionMovingDetect(mMainHandler);

        mAIScoreFrameLayout = ((Activity)mContext).findViewById(R.id.fl_aiscoreview);
        setButtonAIScore();
        mUtilsPlaySound = new UtilsPlaySound();
    }

    public void SetHandTrackingManager(MotionHandTrackingManager manager) {
        mMotionHandTrackingManager = manager;
    }
    public void SetEnableScoringButton(boolean flag){
        if(mDoAIScoreBtn!= null) {
            mDoAIScoreBtn.setEnabled(flag);
        }
    }
    private void setButtonAIScore() {
        mDPAIScoreBtn = ((Activity)mContext).findViewById(R.id.btn_display_aiscore);
        mDoAIScoreBtn = ((Activity)mContext).findViewById(R.id.btn_do_aiscore);
        //롱클릭시 채점표시 지움 
		mDPAIScoreBtn.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View v) {
                //기존 채점 표시 지움
                mMainHandler.sendEmptyMessage(RESET_DRAW_AISCORE);
                return true;

            }
        });
        mDPAIScoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //기존 채점 표시 지움
                mMainHandler.sendEmptyMessage(RESET_DRAW_AISCORE);

                //새로운 코드 : 버튼 클릭시 최신 채점 결과를 화면에 표시
                if (mCoverIndex == 0) {
                    List<AIScoreReferenceDB.DataBase> dataBase = mAIScoreKorean.GetDB(mCoverIndex, mPageIndex);
                    if(dataBase != null) {
                        for (int i = 0; i < dataBase.size(); i++) {
                            mAIScoreKorean.DrawStickerAndGradingForText(mPageIndex,i);
                        }
                    }
                    else { //for debug
                        LOGGER.e("SallyScoring onClick KoreanDB is NULL");
                    }
                } else if (mCoverIndex == 1) {
                    List<AIScoreReferenceDB.DataBase> dataBase = mAIScoreMath.GetDB(mCoverIndex, mPageIndex);
                    if(dataBase != null) {
                        for (int i = 0; i < dataBase.size(); i++) {
                            mAIScoreMath.DrawStickerAndGradingForText(mPageIndex,i);
                        }
                    }
                    else { //for debug
                        LOGGER.e("SallyScoring onClick MathDB is NULL");
                    }
                } else if (mCoverIndex == 2) {
                    List<AIScoreReferenceDB.DataBase> dataBase = mAIScoreEnglish.GetDB(mCoverIndex, mPageIndex);
                    if(dataBase != null) {
                        for (int i = 0; i < dataBase.size(); i++) {
                            mAIScoreEnglish.DrawStickerAndGradingForText(mPageIndex, i);
                        }
                    }
                }
            }
        });
        // 채점 시작 버튼
        mDoAIScoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Message msg = new Message();
                msg.what = REQUEST_PICTURE_CAPTURE;
                msg.arg1 = 0;
                mMainHandler.sendMessage(msg);
            }
        });
    }
    /**
     * DoProcess
     * @param coverIndex
     * @param pageIndex
     *
     * 이미지 얼라인 먼트 정보를 Jni에서 가져와서 펴줌
     * 책 커버 인덱스와 페이지 인덱스 정보 전달하여 프로세스 진행
     * 커버 인덴스 0 : 국어, 1 : 수학, 2 : 영어, 3 : 배경 (인덱스가 3이면 프로세스 진행하면 안됌)
     * 책 페이지 인덱스는 표지 0부터 시작함. 책의 뒷 표지도 인덱싱 됨. 배경 인덱스도 존재함.
     * 국어 : 0 ~ 6, 7은 배경
     * 수학 : 0 ~ 6, 7은 배경
     * 영어 : 0 ~ 14, 15는 배경
     */
    public void StartProcess(int coverIndex, int pageIndex) {

        mCoverIndex = coverIndex;
        mPageIndex = pageIndex;

        mAIScoreEnglish.ResetSoundProcess();

        if( mActivitiesManager == null ) {
            mActivitiesManager = new ActivitiesManager(mContext);
        }

        //Thread 들이 살아 있으면 모두 Kill
        if( mAIScoreThread != null ) {
            mAIScoreThread.interrupt();
            try {
                mAIScoreThread.join();
                LOGGER.d("mAIScoreThread is End");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if( mHandPointListenThread != null ) {
            mHandPointListenThread.interrupt();
            try {
                mHandPointListenThread.join();
                LOGGER.d("mHandPointListenThread is End");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mDataBase = mAIScoreReferenceDB.GetAIScoreMethod(coverIndex, pageIndex);
        for(int i = 0; i < mDataBase.size(); i++) {
            if(mDataBase.get(i).mMethod == TEXT_RECOGNITION_WORD ||
               mDataBase.get(i).mMethod == TEXT_RECOGNITION_SENTENCE ||
               mDataBase.get(i).mMethod == IMAGE_MATCHING ||
               mDataBase.get(i).mMethod == IMAGE_SUBTRACT ||
               mDataBase.get(i).mMethod == IMAGE_CLASSIFICATION ||
               mDataBase.get(i).mMethod == TEXT_NARRITIVE) {

                LOGGER.d("Start makeAIScoreThread");
                makeAIScoreThread();
                break;
            }
        }

        if( mActivitiesManager != null ) {
            mActivitiesManager.SetTouchViewVisible(false);
        }

        for(int i = 0; i < mDataBase.size(); i++) {
            if( mDataBase.get(i).mMethod == IMAGE_BUTTON ||
                    mDataBase.get(i).mMethod == IMAGE_SOUND) {

                LOGGER.d("Start makeHandPointListenThread");
                makeHandPointListenThread();

                if( mDataBase.get(i).mMethod == IMAGE_BUTTON )
                {
                    if( mActivitiesManager != null ) {
                        mActivitiesManager.SetTouchViewVisible(true);
                    }
                }

                break;
            }
            else { //집단채점한 아이템중 틀린 문제에 손가락을 대면 채점 내용을 보여줌.
                if (mDataBase.get(i).mMethod == TEXT_RECOGNITION_WORD ||
                        mDataBase.get(i).mMethod == TEXT_RECOGNITION_SENTENCE) {
                    if (mDataBase.get(i).mSticker == NONE && mDataBase.get(i).mDoGrading == 1 &&
                            mDataBase.get(i).mIsCorrect == 0) { //손글씨 모드
                        makeHandPointListenThread();
                    }
                }
            }
        }

        mAIScoreFrameLayout.removeAllViews();
        for(int i = 0; i < mDataBase.size(); i++) {
            if(mDataBase.get(i).mMethod == IMAGE_SOUND) {
                int x = (int)(mDataBase.get(i).mX * (float)MainActivity.gPreviewRenderWidth);
                int y = (int)(mDataBase.get(i).mY * (float)MainActivity.gPreviewRenderHeight);
                int w = 100;//mDataBase.get(i).mW;
                int h = 100;//mDataBase.get(i).mH;

                Rect rect = new Rect(x, y, w, h);

                drawSoundImage(rect);
            }
        }

        mImageMathchingOn = false;
        mDataBase = mAIScoreReferenceDB.GetAIScoreMethod(coverIndex, pageIndex);
        for(int i = 0; i < mDataBase.size(); i++) {
            if(mDataBase.get(i).mMethod == IMAGE_MATCHING) {
                if( mCoverIndex == 1 || (mCoverIndex == 0 && mPageIndex != 5 ) ) {
                    mImageMathchingOn = true;
                    break;
                }
            }
        }

        if( mImageMathchingOn == true && mSaveFirstFrameStarted == false ) {
            //얼라인먼트가 맞어지면 이거 시작해야함....

            mSaveFirstFrameStarted = true;

            Thread saveFirstFrameThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Message msg = new Message();
                    msg.what = REQUEST_PICTURE_CAPTURE;
                    msg.arg1 = 0;
                    msg.arg2 = 1;
                    mMainHandler.sendMessage(msg);
                }
            });
            saveFirstFrameThread.start();
        }
    }

    public void drawSoundImage(Rect rect) {
        //sally-v2 TODO : 나중엔 rect영역의 움직임(필기동작, 터치동작)이 끝난 직후 채점되도록 해야함.
        mSoundImageView = new ImageView(mContext);

        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(rect.width/* layout_width */, rect.height/* layout_height */);
        lparams.setMargins(rect.x - rect.width/2, rect.y - rect.height/2, 0, 0);
        mSoundImageView.setLayoutParams(lparams);
        mSoundImageView.setImageResource(R.drawable.soundicon);

        mAIScoreFrameLayout.addView(mSoundImageView);
    }

    private void makeAIScoreThread() {
        mAIScoreThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //페이지 넘긴 후 6초 후 while 루프 시작
                    Thread.sleep(6000);
                } catch (InterruptedException ex) {
                    LOGGER.e("mAIScoreThread interrupted exception error1 : " + ex);
                    Thread.currentThread().interrupt();
                }
//                while (!Thread.currentThread().isInterrupted()) {
//                    try {
//                        //기존소스
//                        //checkMovingStatuForAIScore();
//
//                        //새로운소스 : 일정 간격(3~5초)마다 채점(촬영부터) 요청함.
//                        Message msg = new Message();
//                        msg.what = REQUEST_PICTURE_CAPTURE;
//                        msg.arg1 = 0;
//                        mMainHandler.sendMessage(msg);
//                        Thread.sleep(4000); //채점 간격
//                    } catch (InterruptedException ex) {
//                        LOGGER.e("mAIScoreThread interrupted exception error2 : " + ex);
//                        Thread.currentThread().interrupt();
//                    }
//                }
            }
        });
        mAIScoreThread.start();
    }

    private void makeHandPointListenThread() {
        mHandPointListenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        //LOGGER.d("mHandPointListenThread is running");

                        if (mCoverIndex == 0) {
                            //mAIScoreKorean.DoProcess(mAlignedMat, mCoverIndex, mPageIndex);
                            boolean buttonTouched = mAIScoreKorean.DoTouchButtonProcess(mCoverIndex, mPageIndex, mMotionHandTrackingManager);
                            if (buttonTouched == true) {
                                if (mActivitiesManager != null) {
                                    mActivitiesManager.SetTouchButton();
                                }
                            }

                        } else if (mCoverIndex == 1) {
                            //mAIScoreMath.DoProcess(mSaveAlignedMat, mAlignedMat, mCoverIndex, mPageIndex, 0, 0/*mProcessCount*/);
                        } else if (mCoverIndex == 2) {
                            mAIScoreEnglish.DoSoundProcess(mCoverIndex, mPageIndex, mMotionHandTrackingManager);
                        }

                        DisplayMultiScoringResultByHand(mCoverIndex, mPageIndex, mMotionHandTrackingManager);

                        Thread.sleep(33);
                    } catch (InterruptedException ex) {
                        LOGGER.e("mHandPointListenThread interrupted exception error : " + ex);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        mHandPointListenThread.start();
    }

    //무빙을 바탕으로 채점 시작을 하는 함수. 주기적 채점으로 바뀌면서 현재 사용 안됨.
    private void checkMovingStatuForAIScore() {
        Mat previewMat = MainActivity.GetPreviewData().clone();
        flip(previewMat, previewMat, 1);//0: vertical, 1 : horizontal
        Mat alignedMat = mAIScoreUtils1.DoImageAlignment(previewMat);

        //여기서 좌우상하 좌표 체크해서 Crop하기
        //Mat cropInputMat = new Mat(alignedMat, cropRect).clone();
        Mat cropInputMat = alignedMat.clone();

        float[] indexData = {-1, 0};
        mMotionMovingDetect.CheckMoving(cropInputMat, indexData);

        LOGGER.d("mMotionMovingDetect : "+mMotionMovingDetect.GetMovingRunning()+", GetMovingValue : "+mMotionMovingDetect.GetMovingValue()+", mProcessRunning : "+mProcessRunning);

        if( mMotionMovingDetect.GetMovingRunning() == true ) {
            mAiScoreRunning = true;
        }
        else {
            if( mAiScoreRunning == true ) {
                mAiScoreRunning = false;

                if( mProcessRunning == false ) {
                    mProcessRunning = true;

                    mProcessCount = 0;
                    Message msg = new Message();
                    msg.what = REQUEST_PICTURE_CAPTURE;
                    msg.arg1 = 0;
                    msg.arg2 = 0;
                    mMainHandler.sendMessage(msg);
                }
            }
        }
    }

    public void DoAiScoreByCaptureMat(Mat capturedInputMat, int coverIndex, int pageIndex, int aiScoreResultOn, int aiScoreSaveFrame) {
        LOGGER.d("DoAiScoreByCaptureMat");

        mAlignedMat = mAIScoreUtils2.DoImageAlignment(capturedInputMat);
        imwrite("/sdcard/studyNet/DEBUG/mAlignedMat.jpg", mAlignedMat);

        if( aiScoreSaveFrame == 1 ) {
            mSaveAlignedMat = mAlignedMat.clone();
            imwrite("/sdcard/studyNet/DEBUG/math/mSaveAlignedMat.jpg", mSaveAlignedMat);
            mSaveFirstFrameStarted = false;
            return;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (coverIndex == 0) {
                    mAIScoreKorean.DoProcess(mSaveAlignedMat, mAlignedMat, coverIndex, pageIndex, aiScoreResultOn, mProcessCount);

//                    mProcessRunning = false;
                } else if (coverIndex == 1) {
                    mAIScoreMath.DoProcess(mSaveAlignedMat, mAlignedMat, coverIndex, pageIndex, aiScoreResultOn, mProcessCount);

//                    mProcessCount++;
//                    if( mProcessCount < 1/*2*/ ) {
//                        Message msg = new Message();
//                        msg.what = REQUEST_PICTURE_CAPTURE;
//                        msg.arg1 = 1;
//                        msg.arg2 = 0;
//                        mMainHandler.sendMessage(msg);
//                    }

//                    if( mProcessCount == 1/*2*/ ) {
//                        mProcessRunning = false;
//                    }
                } else if (coverIndex == 2) {
                    LOGGER.d("mProcessCount : "+mProcessCount);
                    mAIScoreEnglish.DoImageProcess(mAlignedMat, coverIndex, pageIndex, aiScoreResultOn, mProcessCount);

//                    mProcessCount++;
//                    if( mProcessCount < 1/*3*/ ) {
//                        Message msg = new Message();
//                        msg.what = REQUEST_PICTURE_CAPTURE;
//                        msg.arg1 = 1;
//                        msg.arg2 = 0;
//                        mMainHandler.sendMessage(msg);
//                    }

//                    if( mProcessCount == 1/*3*/ ) {
//                        mProcessRunning = false;
//                    }
                }
            }
        });
        thread.start();
    }
	
	/**
     * 카메라캡처 이미지를 alignment한 후 집단채점시 사용할 입력인 두번째 or 세번째 Mat를 각 과목에 전달한다.
     *
     * @param capturedInputMat
     * @param coverIndex
     * @param pageIndex
     * @param frameNo 1 or 2
     */
    public void SetCaptureMat(Mat capturedInputMat, int coverIndex, int pageIndex, int frameNo) {
        mAlignedMat = mAIScoreUtils2.DoImageAlignment(capturedInputMat);
        if (coverIndex == 0) {
            mAIScoreKorean.SetMutiScoringInputImgSub(mAlignedMat, frameNo);
        } else if (coverIndex == 1) {
            mAIScoreMath.SetMutiScoringInputImgSub(mAlignedMat, frameNo);
        } else if (coverIndex == 2) {
            mAIScoreEnglish.SetMutiScoringInputImgSub(mAlignedMat, frameNo);
        }
    }
    /**
     * 현재 과목의 해당 페이지에 집단 채점할 문제가 포함되어 있는지를 리턴함.
     * @param coverIndex
     * @param pageIndex
     * @return
     */
    public boolean HasMultiScoringPart(int coverIndex, int pageIndex, int []captureCount) {
        if (coverIndex == 0) {
            return mAIScoreKorean.HasMultiScoringPart(coverIndex, pageIndex, captureCount);
        } else if (coverIndex == 1) {
            return mAIScoreMath.HasMultiScoringPart(coverIndex, pageIndex, captureCount);
        } else if (coverIndex == 2) {
            return mAIScoreEnglish.HaveMultiScoringPart(coverIndex, pageIndex, captureCount);
        }
        return false;
    }

    /**
     * 해당 과목의 특정 페이지에 틀린문제, 채점이 안된 문제가 있는지의 여부를 리턴한다.
     * @param coverIndex
     * @param pageIndex
     * @return
     */
    public int HasItemsToBeGraded(int coverIndex, int pageIndex) {
        if (coverIndex == 0) {
            return mAIScoreKorean.HasItemsToBeGraded(coverIndex, pageIndex);
        } else if (coverIndex == 1) {
            return mAIScoreMath.HasItemsToBeGraded(coverIndex, pageIndex);
        } else if (coverIndex == 2) {
            return mAIScoreEnglish.HasItemsToBeGraded(coverIndex, pageIndex);
        }else {
            return 0;
        }
    }
	
   /**
     * 터치 입력시 집단채점 여부를 보고 틀린 경우 결과를 화면에 표시함.
     * @param coverIndex 현재 과목
     * @param pageIndex
     * @param touchX 화면터치시 x좌표
     * @param touchY 화면터치시 y좌표
     */
    public void DisplayMultiScoringResult(int coverIndex, int pageIndex, float touchX, float touchY){
        List<AIScoreReferenceDB.DataBase> db = null;
        if (coverIndex == 0) {
            db = mAIScoreKorean.GetDB(coverIndex, pageIndex);
        } else if (coverIndex == 1) {
            db = mAIScoreMath.GetDB(coverIndex, pageIndex);
        } else if (coverIndex == 2) {
            db = mAIScoreEnglish.GetDB(coverIndex, pageIndex);
        }

        if(db == null || db.size() == 0) {
            LOGGER.d("SallyRecog db.size() == 0");
            return;
        }
        for(int i = 0; i < db.size(); i++) {
            if(db.get(i).mMethod == TEXT_RECOGNITION_WORD ||
                    db.get(i).mMethod == TEXT_RECOGNITION_SENTENCE) {
                if (db.get(i).mSticker == NONE &&
                        db.get(i).mIsCorrect == 0) { //손글씨 모드
                    float x = (db.get(i).mX * (float) gPreviewRenderWidth);
                    float y = (db.get(i).mY * (float) gPreviewRenderHeight);
                    float w = (db.get(i).mW * (float) gPreviewRenderWidth);
                    float h = (db.get(i).mH * (float) gPreviewRenderHeight);

                    LOGGER.d("SallyRecog DisplayMultiScoringResult() (x, y, w, h) : " + x + ", "
                            + y + ", " + w + ", " + h);
                    if((touchX >= x) && (touchX < (x + w)) &&
                            (touchY >= y) && (touchY < (y + h))) {
                        //터치 입력이 채점 영역 내부이면 채점 결과 가져오기
                        //채점 결과를 AIScoreMultiScoringInfo 에 저장
                        String result[] = db.get(i).mSaveAnswer.split(OCRRecognition.TEXT_SEPARATOR);
                        for(int ii=0; ii < result.length; ii++) {
                            LOGGER.d("SallyRecog DisplayMultiScoringResult() ["+ii+"] = " + result[ii]);
                        }
                        if(result[0].contains("MULTI")) { //손글씨 모드이지만 집단채점을 안하는 경우도 있으므로 MULTI인지를 확인해야함.
                            int score[] = {0,0,0};
                            String answer[] = {"","",""};
                            boolean isCorrect = false;

                            answer[0] = result[1];
                            answer[1] = result[3];
                            answer[2] = result[5];
                            score[0] = Integer.parseInt(result[2]);
                            score[1] = Integer.parseInt(result[4]);
                            score[2] = Integer.parseInt(result[6]);

                            // 집단채점 중 100점인 결과는 실제 정답(대소문자 구분되고 띄어쓰기 있는 답)을 화면에 표시하도록 함.
                            for(int j = 0; j < 3; j++) {
                                if(score[j] == 100) {
                                    answer[j] = db.get(i).mAnswerText;
                                }
                                else{ //틀린 답의 경우엔 어떻게 하지..TODO

                                }
                            }

                            AIScoreMultiScoringInfo scoringData =
                                    new AIScoreMultiScoringInfo(answer, score, isCorrect, (int)x, (int)y, (int)w, (int)h);
                            LOGGER.d("SallyRecog DisplayMultiScoringResult() answer : " + answer[0] + " " + answer[1] + " " +answer[2]);
                            LOGGER.d("SallyRecog DisplayMultiScoringResult() score : " + score[0] + " " + score[1] + " " +score[2]);
                            LOGGER.d("SallyRecog DisplayMultiScoringResult() x, y, w, h : " + x + ", "
                                    + y + ", " + w + ", " + h);

                            // 메인 핸들러에게 채점 결과를 보내줘서 화면에 표시하도록 함.
                            // 전송해줄것들 : 집단채점여부, 답이 맞았는지 여부, 채점 결과 스트링("text1 text2 text3 100 66 55")
                            //             채점할 위치 영역.
                            Message retmsg = new Message();
                            retmsg.what = DRAW_AISCORE_MULTI;
                            retmsg.obj = scoringData;
                            mMainHandler.sendMessage(retmsg);
                            break;
                        }
                    }
                    else {
                        //다른 곳을 터치시 집단채점 결과 지우기
                        mMainHandler.sendEmptyMessage(REMOVE_AISCORE_MULTI);
                    }
                }
            }
        }
    }

    public void DisplayMultiScoringResultByHand(int coverIndex, int pageIndex, MotionHandTrackingManager handManager) {
        List<AIScoreReferenceDB.DataBase> db = null;
        if (coverIndex == 0) {
            db = mAIScoreKorean.GetDB(coverIndex, pageIndex);
        } else if (coverIndex == 1) {
            db = mAIScoreMath.GetDB(coverIndex, pageIndex);
        } else if (coverIndex == 2) {
            db = mAIScoreEnglish.GetDB(coverIndex, pageIndex);
        }

        if(db == null || db.size() == 0) {
            LOGGER.d("SallyRecog db.size() == 0");
            return;
        }
        for(int i = 0; i < db.size(); i++) {
            if(db.get(i).mMethod == TEXT_RECOGNITION_WORD ||
                    db.get(i).mMethod == TEXT_RECOGNITION_SENTENCE) {
                if (db.get(i).mSticker == NONE && db.get(i).mDoGrading == 1 &&
                        db.get(i).mIsCorrect == 0) { //손글씨 모드
                    float x = db.get(i).mX;
                    float y = db.get(i).mY;
                    float w = db.get(i).mW;
                    float h = db.get(i).mH;
                    float x2 = x + w;
                    float y2 = y + h;
                    if(handManager.GetTouchPressed(x, y, x2, y2) == true || handManager.GetTouchPressed2(x, y, x2, y2) == true) {


                        //채점 결과를 AIScoreMultiScoringInfo 에 저장
                        String result[] = db.get(i).mSaveAnswer.split(OCRRecognition.TEXT_SEPARATOR);
                        for(int ii=0; ii < result.length; ii++) {
                            LOGGER.d("SallyRecog DisplayMultiScoringResultByHand() ["+ii+"] = " + result[ii]);
                        }
                        if(result[0].contains("MULTI")) { //손글씨 모드이지만 집단채점을 안하는 경우도 있으므로 MULTI인지를 확인해야함.
                            int score[] = {0,0,0};
                            String answer[] = {"","",""};
                            boolean isCorrect = false;

                            answer[0] = result[1];
                            answer[1] = result[3];
                            answer[2] = result[5];
                            score[0] = Integer.parseInt(result[2]);
                            score[1] = Integer.parseInt(result[4]);
                            score[2] = Integer.parseInt(result[6]);

                            // 집단채점 중 100점인 결과는 실제 정답(대소문자 구분되고 띄어쓰기 있는 답)을 화면에 표시하도록 함.
                            for(int j = 0; j < 3; j++) {
                                if(score[j] == 100) {
                                    answer[j] = db.get(i).mAnswerText;
                                }
                                else{ //틀린 답의 경우엔 어떻게 하지..TODO

                                }
                            }

                            // 손 좌표를 그대로 이용하면 안되고 Preview Render 사이즈 기준으로 변경 후 그리는 루틴에 전달해야함.
                            float xx = (db.get(i).mX * (float) gPreviewRenderWidth);
                            float yy = (db.get(i).mY * (float) gPreviewRenderHeight);
                            float ww = (db.get(i).mW * (float) gPreviewRenderWidth);
                            float hh = (db.get(i).mH * (float) gPreviewRenderHeight);

                            AIScoreMultiScoringInfo scoringData =
                                    new AIScoreMultiScoringInfo(answer, score, isCorrect, (int)xx, (int)yy, (int)ww, (int)hh);
                            LOGGER.d("SallyRecog DisplayMultiScoringResultByHand() answer : " + answer[0] + " " + answer[1] + " " +answer[2]);
                            LOGGER.d("SallyRecog DisplayMultiScoringResultByHand() score : " + score[0] + " " + score[1] + " " +score[2]);
                            LOGGER.d("SallyRecog DisplayMultiScoringResultByHand() x, y, w, h : " + xx + ", "
                                    + yy + ", " + ww + ", " + hh);

                            // 메인 핸들러에게 채점 결과를 보내줘서 화면에 표시하도록 함.
                            // 전송해줄것들 : 집단채점여부, 답이 맞았는지 여부, 채점 결과 스트링("text1 text2 text3 100 66 55")
                            //             채점할 위치 영역.
                            Message retmsg = new Message();
                            retmsg.what = DRAW_AISCORE_MULTI;
                            retmsg.obj = scoringData;
                            mMainHandler.sendMessage(retmsg);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void ClearSticker() {
        mAIScoreView.ResetBitmap();

    }

    public void ClearSoundIcon(){
        if (mAIScoreFrameLayout != null) {
            mAIScoreFrameLayout.removeAllViews();
        }
    }
    public void RedrawSoundIcon() {
        if (mAIScoreFrameLayout != null) {
            mAIScoreFrameLayout.removeAllViews();
            if (mDataBase != null) {
                for (int i = 0; i < mDataBase.size(); i++) {
                    if (mDataBase.get(i).mMethod == IMAGE_SOUND) {
                        int x = (int) (mDataBase.get(i).mX * (float) MainActivity.gPreviewRenderWidth);
                        int y = (int) (mDataBase.get(i).mY * (float) MainActivity.gPreviewRenderHeight);
                        int w = 100;//mDataBase.get(i).mW;
                        int h = 100;//mDataBase.get(i).mH;

                        Rect rect = new Rect(x, y, w, h);

                        drawSoundImage(rect);
                    }
                }
            }
        }
    }

    public void StopProcess() {
        //Thread 들이 살아 있으면 모두 Kill
        if( mAIScoreThread != null ) {
            mDoAIScoreBtn.setEnabled(false);
            mAIScoreThread.interrupt();
            try {
                mAIScoreThread.join();
                LOGGER.d("mAIScoreThread is End");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if( mHandPointListenThread != null ) {
            mHandPointListenThread.interrupt();
            try {
                mHandPointListenThread.join();
                LOGGER.d("mHandPointListenThread is End");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 채점 시작 여부를 셋팅
     * @param flag
     */
    public void SetGradingStatus(boolean flag) {
        mIsGradingAllDone = flag;
    }

    /**
     * 채점 상태를 리턴
     * true = 채점 완료
     * false = 채점 미완료
     * @return
     */
    public boolean GetGradingStatus(int coverIndex) {
        if (coverIndex == 0) {
            mIsGradingAllDone = mAIScoreKorean.GetGradingStatus();
        } else if (coverIndex == 1) {
            mIsGradingAllDone = mAIScoreMath.GetGradingStatus();
        } else if (coverIndex == 2) {
            mIsGradingAllDone = mAIScoreEnglish.GetGradingStatus();
        }
        return mIsGradingAllDone;
    }

    public void SetCurrentCoverAndPage(int coverIdx, int pageIdx) {
        mCoverIndex = coverIdx;
        mPageIndex = pageIdx;
    }

    // 카메라 캡처 시점부터 채점 시작임을 알리기 위한 함수
    public void SetDoProcessFlagOn(int coverIndex) {
        if (coverIndex == 0) {
            mAIScoreKorean.SetDoProcessFlagOn();
        } else if (coverIndex == 1) {
            mAIScoreMath.SetDoProcessFlagOn();
        } else if (coverIndex == 2) {
            mAIScoreEnglish.SetDoProcessFlagOn();
        }
    }

    public void PlayGuideSound(String content) {
        boolean isRunning[] = {false};

        if(mUtilsPlaySound.isSoundPlaying() == false) {
            mUtilsPlaySound.DoPlaySound(content, isRunning);
        }
    }
}
