package com.ispd.mommybook.ocr;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.ispd.mommybook.ocr.recognition.OCRRecognition;

import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;

import java.util.ArrayList;

import static com.ispd.mommybook.aiscore.AIScoreFunctions.MSG_REQUEST_TEXT_SCORING;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.*;


public class OCRManager {
    private static final UtilsLogger LOGGER = new UtilsLogger();
    public static final int MSG_WORD_RECOGNITION_DONE = 1000;

    private Context mContext = null;
    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;

    public static final int THREAD_NUM = 1;
    private OCRRecognition[] mRecognizer;
    private boolean mStopProcess = false;

    /**
     * 인식할 데이터의 수 (BBox 개수와 같음)
     */
    private static int mDataLength = 0;

    /**
     * 현재 시점에서 완료된 인식데이터 개수
     */
    private static int mRecognitionDoneCnt = 0;

    /**
     * 현재 페이지의 모든 인식이 끝났는지의 여부.
     */
    private boolean mRecognitonDone = true;

    private String mResultString = "";

    /**
     * 인식할 데이터의 구조 : 시리얼넘버, 정답 텍스트, 입력이미지, 인식할 모드, 인식결과 텍스트
     *
     */
    public class OCRData {
        public int mSerialNumber = -1;
        public String mAnswer = "";
        public Mat mMatWordImg;
        public Mat mMatWordImgScnd; //집단채점 모드인 경우 두번째 프레임의 word이미지
        public Mat mMatWordImgThrd; //집단채점 모드인 경우 세번째 프레임의 word이미지
        public OCRRecognitionMode mRecogMode = NONE;
//        public String mPredicted = ""; // 집단채점인 경우엔 세 모델의 인식 결과 모음
        public OCRData(int serial, String answer, Mat img, OCRRecognitionMode mode) {
            mSerialNumber = serial;
            mAnswer = answer;
            mMatWordImg = img.clone();
            mRecogMode = mode;
        }
        public void SetScndImage(Mat img) {
            mMatWordImgScnd = img.clone();
        }
        public void SetThrdImage(Mat img) {
            mMatWordImgThrd = img.clone();
        }
        public void Destroy(){
            mMatWordImg.release();
            if(mMatWordImgScnd != null) {
                mMatWordImgScnd.release();
            }
            if(mMatWordImgThrd != null) {
                mMatWordImgThrd.release();
            }
        }
    }

    // 집단채점 or 단일채점 모드 정의
    public static final int SINGLE_MODEL_SCORING = 0;
    public static final int MULTI_MODEL_SCORING = 1;

    /**
     * 인식 또는 채점할 페이지 단위의 인식데이터(OCRData)를 가지고 있는 array.
     */
    private static ArrayList<OCRData> mOCRDataList;

    public OCRManager(Context context) {
        mContext = context;

        mOCRDataList = new ArrayList<OCRData>(4);

        mHandlerThread = new HandlerThread("OCRManager");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                handleMessages(msg);
            }
        };

        // 인식기 생성
        mRecognizer = new OCRRecognition[THREAD_NUM];
        for(int i = 0; i < THREAD_NUM; i++) {
            mRecognizer[i] = new OCRRecognition(mContext, mHandler);
        }
        runProcess();
    }

    public Handler GetHandler() {
        return mHandler;
    }

    /**
     * 현재 페이지의 인식이 모두 끝났는지의 여부를 리턴한다.
     * @return
     */
    public boolean IsRecognitonDone() {
        return mRecognitonDone;
    }

    /**
     * 인식할 데이터의 정보를 받아서 리스트에 저장.
     * @param serial  시리얼넘버
     * @param answer  정답 텍스트
     * @param img  입력이미지 Mat
     * @param mode 인식할 모드
     */
//    public void AddOCRData(int serial, String answer, Mat img, OCRRecognitionMode mode) {
    public void AddOCRData(int serial, String answer, Mat img, OCRRecognitionMode mode, Mat imgScnd, Mat imgThrd) {
        OCRData ocrData = new OCRData(serial, answer, img, mode);

        //집단채점 모드인 경우에 두번째, 세번째 프레임의 입력이미지를 OCRData에 셋팅해준다.
        if(imgScnd != null) {
            ocrData.SetScndImage(imgScnd);
        }
        if(imgThrd != null) {
            ocrData.SetThrdImage(imgThrd);
        }

        mOCRDataList.add(ocrData);
    }

    /**
     * 인식데이터의 개수를 리턴한다. 이 값이 1개 이상이면 인식 루틴 시작하게 됨.
     * @return
     */
    public int GetOCRDataSize() {
        return mOCRDataList.size();
    }

    /**
     * 채점할 페이지가 바뀌거나, 다시 채점하거나, 새로운 표지를 인식하게 될 경우 아래의 함수가 호출됨.
     * 인식진행 중 일때는 호출되면 안됨. TODO 처리루틴 넣기
     */
    public void ClearOCRData() {
        LOGGER.d("SallyRecog ClearOCRData() ");
        if (mOCRDataList.size() > 0) {
            for (OCRData data : mOCRDataList) {
                data.Destroy();
            }
            mOCRDataList.clear();
        }

    }

    public void StartRecognitonDetection() {
        if(mRecognitonDone == true && mOCRDataList.isEmpty() == false) {
            LOGGER.d("SallyRecog StartRecognitionDetection !!!! OCRData size = " + mOCRDataList.size());
            mRecognitonDone = false;
            ResumeProcess();
        }
    }

    private void runProcess() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if( mStopProcess == true ) {
                    LOGGER.d("SallyRecog Paused()~~");
                    return;
                }

                // 인식이 시작됨.
                if(mRecognitonDone == false ) {
                    //전체 쓰레드가 각각 인식할 데이터 개수. but 집단채점을 반영한 개수가 아님.
                    int dataLength = mOCRDataList.size();
                    mDataLength = dataLength;
                    mRecognitionDoneCnt = 0;
                    LOGGER.d("SallyRecog  RecogDataLength = " + mDataLength);
                    if(dataLength <= THREAD_NUM) { //인식할 데이터수 <= THREAD_NUM
                        for(int i = 0; i < dataLength; i++) {
                            //각 Thread 바로 실행
                            mRecognizer[i].SetRecogData(mOCRDataList, i, i);
                            mRecognizer[i].StartRecognition();
                        }
                    } else {
                        // 인식할 데이터수 > THREAD_NUM  일 때 각 쓰레드에 인식할 데이터 개수를 분배함.
                        int[] cnt = new int[THREAD_NUM];

                        int sum = 0;
                        for(int i = dataLength; i > 0; i -= THREAD_NUM) {
                            for(int j = 0; j < THREAD_NUM; j++) {
                                cnt[j] = cnt[j] + 1;
                                LOGGER.d("SallyRecog  cnt["+j+"] = " + cnt[j]);
                                sum += 1;
                                if (sum == dataLength) break;
                            }
                        }

                        int index = 0;
                        for(int i = 0; i < THREAD_NUM; i++) {
                            mRecognizer[i].SetRecogData(mOCRDataList, index, index + cnt[i] - 1);
                            // 갯수가 제대로 분배됐는지 확인용 코드
                            LOGGER.d("SallyRecog Recog Thread["+i+"] Start : " + index + " ~ " + (index + cnt[i] - 1));
                            index = index + cnt[i];
                            // 인식 시작
                            mRecognizer[i].StartRecognition();
                            try {
                                Thread.sleep(33);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    PauseProcess(); //인식쓰레드들이 모두 끝날때까지 pause loop
                }

                mHandler.postDelayed(this, 33);
            }
        });
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
        for(int i = 0; i < THREAD_NUM; i++) {
            mRecognizer[i].Stop();
        }

        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
    }

    //AIScore 클래스에 결과 전달을 위한 리스너 선언.
    public interface RecogResultListener {
        void onRecogResultListener(Message msg);
    }

    private OCRManager.RecogResultListener mRecogResultListener = null;

    public void SetRecogResultListener(OCRManager.RecogResultListener in_listener) {
        mRecogResultListener = in_listener;
    }

    private void handleMessages(Message msg) {
        switch(msg.what) {
            case MSG_WORD_RECOGNITION_DONE:
                int serialNumber = msg.arg1; //serialNo
                int isMultiRecognition = msg.arg2; //멀티 모델 인식인지 아닌지 여부
                mResultString = msg.obj.toString();
                LOGGER.d("SallyRecog OCRManager Handler : Predicted Result(" + serialNumber + ") String : " +
                        mResultString);

                // 정답을 요청한 각 과목 클래스에게 결과 스트링을 전달해서 채점하도록 함.
                Message retmsg = new Message();
                retmsg.what = MSG_REQUEST_TEXT_SCORING;
                retmsg.arg1 = serialNumber;
                retmsg.arg2 = isMultiRecognition;
                retmsg.obj = mResultString;

                mRecogResultListener.onRecogResultListener(retmsg);

                mRecognitionDoneCnt++;
                LOGGER.d("SallyRecog mRecognitionDoneCnt= " + mRecognitionDoneCnt + ", mDataLength= " + mDataLength);
                if(mRecognitionDoneCnt == mDataLength) {
                    //인식 진행된 개수와 전체 데이터수가 같아지면, 새로운 인식 요청을 받을 준비를 함.
                    mRecognitonDone = true;
                }
                break;
        }
    }
}
