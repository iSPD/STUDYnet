package com.ispd.mommybook.ocr.recognition;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.ispd.mommybook.ocr.OCRManager;
import com.ispd.mommybook.ocr.OCRRecognitionMode;

import static com.ispd.mommybook.ocr.OCRManager.MSG_WORD_RECOGNITION_DONE;
import static com.ispd.mommybook.ocr.OCRManager.MULTI_MODEL_SCORING;
import static com.ispd.mommybook.ocr.OCRManager.SINGLE_MODEL_SCORING;
import static com.ispd.mommybook.ocr.OCRRecognitionMode.*;
import static com.ispd.mommybook.ocr.recognition.OCRRecognitionCharDB.CHAR_DICTIONARY_2450;
import static com.ispd.mommybook.ocr.recognition.OCRRecognitionCharDB.CHAR_DICTIONARY_2497;
import static com.ispd.mommybook.ocr.recognition.OCRRecognitionCharDB.CHAR_DICTIONARY_2445;

import com.ispd.mommybook.utils.UtilsLogger;

import java.util.ArrayList;


public class OCRRecognition {
    private static final UtilsLogger LOGGER = new UtilsLogger();
    public final static int RECOG_BOX_MAX = 3;
    public final static String TEXT_SEPARATOR = "//"; //집단채점시 각 인식결과 텍스트 사이에 들어가는 구분자

    private Context mContext;
    private Handler mOCRManagerHandler;
    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;
    private boolean mStopProcess = false;

    private ArrayList<OCRManager.OCRData> mOCRDataList;
    private int mRecogStartIndex = -1; //인식할 데이터 리스트 중 첫번째 데이터 인덱스
    private int mRecogEndIndex = -1; //인식할 데이터 리스트 중 마지막 데이터 인덱스
    private boolean mStartFlag = false;

    public OCRRecognition(Context context, Handler handler) {
        mContext = context;
        mOCRManagerHandler = handler;
        mHandlerThread = new HandlerThread("OCRRecognition");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg) {
                handleMessages(msg);
            }
        };

        runProcess();
    }

    public void SetRecogData(ArrayList<OCRManager.OCRData> list, int start, int end) {
        LOGGER.d("SallyRecog startIdx = " + start + " , endIdx = " + end);
        mRecogStartIndex = start;
        mRecogEndIndex = end;
        mOCRDataList = list;
    }

    public void StartRecognition() {
        mStartFlag = true;
    }

    /**
     * 각 인식 모드에 맞는 모델을 리턴한다.
     * @param recogMode
     * @return
     */
    private String[] getModelName(OCRRecognitionMode recogMode) {
        String[] modelName = {null, null, null};

        if(recogMode == TYPO_KOR ){
            modelName[0] = "typo0623retr-none-vgg-none-ctc-kor_eng_mix_math.pt"; //2445 chars
        }
        else if(recogMode == TYPO_ENG){
            //TODO : 좁게 쓰여진 영어 인쇄체의 경우 손글씨모델이 더 인식을 잘함. 더 테스트 해보긴 해야함.
//            modelName[0] = "typo0623retr-none-vgg-none-ctc-kor_eng_mix_math.pt"; //2445 chars //인쇄체 모델
            modelName[0] = "hw0409-none-vgg-bilstm-ctc-eng_hw_543ms.pt"; //2450 chars //손글씨 모델
        }
        else if(recogMode == TYPO_KOR_NUM || recogMode == TYPO_NUM_SIGN || recogMode == HW_NUM_SIGN){
            modelName[0] = "hw0723retr_none-vgg-bilstm-ctc-kor_hw_num.pt"; //2497 chars
        }
        else if(recogMode == HW_KOR_SINGLE ){
            modelName[0] = "hw0630_none-vgg-bilstm-ctc-kor_hw.pt"; //2497 chars
        }
        else if(recogMode == HW_KOR_MULTI ){ // TODO 세가지 모두 다른 모델로 바꾸기
            modelName[0] = "hw0708-none-vgg-none-ctc-kor-hw-199ms.pt"; //2497 chars
            modelName[1] = "hw0630_none-vgg-bilstm-ctc-kor_hw.pt"; //2497 chars
            modelName[2] = "hw0630_none-vgg-bilstm-ctc-kor_hw.pt"; //2497 chars
        }
        else if(recogMode == HW_ENG_MULTI ){
            modelName[0] = "hw0409-none-vgg-bilstm-ctc-eng_hw_543ms.pt"; //2450 chars
            modelName[1] = "hw0409-none-vgg-bilstm-ctc-eng_hw_543ms.pt"; //2450 chars
            modelName[2] = "hw0409-none-vgg-bilstm-ctc-eng_hw_543ms.pt"; //2450 chars
        }

        return modelName;
    }

    /**
     * 모델에 맞는 char dictionary index 을 리턴한다.
     */

    private int getDictionaryIndex(String modelName) {
        int dictIdx = -1;
        if(modelName == "typo0623retr-none-vgg-none-ctc-kor_eng_mix_math.pt") {
            dictIdx = CHAR_DICTIONARY_2445; //char_dict2445[]
        }
        else if(modelName == "hw0409-none-vgg-bilstm-ctc-eng_hw_543ms.pt") {
            dictIdx = CHAR_DICTIONARY_2450; //char_dict2450
        }
        else if( (modelName == "hw0630_none-vgg-bilstm-ctc-kor_hw.pt") ||
                (modelName == "hw0708-none-vgg-none-ctc-kor-hw-199ms.pt") ||
                (modelName == "hw0715-none-vgg-none-ctc-eng_hw_199ms.pt") ||
                (modelName == "hw0723retr_none-vgg-bilstm-ctc-kor_hw_num.pt") ){
            dictIdx = CHAR_DICTIONARY_2497; //char_dict2497
        }
        return dictIdx;
    }

    private String resultAll = "";
    private int modelIndex = 0;
    private void runProcess() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if( mStopProcess == true ) {
                    return;
                }
                if(mStartFlag == true && mOCRDataList != null) {

                    for(int i = mRecogStartIndex; i < (mRecogEndIndex + 1); i++) {
                        LOGGER.d("SallyRecog i ======> " + i + ", OCRDataList size : " + mOCRDataList.size());
                        OCRManager.OCRData data = mOCRDataList.get(i);

                        String[] modelName = getModelName(data.mRecogMode);

//                        String resultAll = "";
                        int scoringType = -1;
                        if(data.mRecogMode == HW_KOR_MULTI || data.mRecogMode == HW_ENG_MULTI) {
                            // 집단채점
//                            Thread[] thread = new Thread[3];
                            //for(int modelIndex = 0; modelIndex < 3; modelIndex++) {
                            resultAll = "";
                            for(modelIndex = 0; modelIndex < 3; modelIndex++) {
                                LOGGER.d("SallyRecog MULTI SCORING [" + modelIndex + "]~~~~~~~~~~~~~");
                                OCRRecognitionPredict predictClass =
                                        new OCRRecognitionPredict(mContext,
                                                                  data.mSerialNumber,
                                                                  modelName[modelIndex],
                                                                  getDictionaryIndex(modelName[modelIndex]));
                                String result = "";
                                if(modelIndex == 0) {
                                    result = predictClass.PredictWord(data.mMatWordImg);
                                }
                                else if(modelIndex == 1) {
                                    result = predictClass.PredictWord(data.mMatWordImgScnd);
                                }
                                else {
                                    result = predictClass.PredictWord(data.mMatWordImgThrd);
                                }
                                LOGGER.d("SallyRecog MULTI SCORING Predicted [" + modelIndex + "] :" + result);
                                LOGGER.d("SallyRecog               ModelName= " + modelName[modelIndex]);
                                resultAll = resultAll + result + TEXT_SEPARATOR;

                            }
                            LOGGER.d("SallyRecog MULTI SCORING Predicted : " + resultAll);
                            scoringType = MULTI_MODEL_SCORING;
                            // OCRManager 에 결과 스트링 전달.
                            Message retmsg = new Message();
                            retmsg.what = MSG_WORD_RECOGNITION_DONE;
                            retmsg.arg1 = data.mSerialNumber;
                            retmsg.arg2 = scoringType;  //멀티모델 인식인지의 여부
                            retmsg.obj = resultAll; // 인식 결과 스트링 (집단채점인 경우 세 모델의 데이터 모음)

                            mOCRManagerHandler.sendMessage(retmsg);
                        }
                        else {
                            // 단일채점

                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    LOGGER.d("SallyRecog Single SCORING !!!!!!!!!!!!!!!!!!");
                                    OCRRecognitionPredict predictClass =
                                            new OCRRecognitionPredict(mContext,
                                                                      data.mSerialNumber,
                                                                      modelName[0],
                                                                      getDictionaryIndex(modelName[0]));
                                    String result = predictClass.PredictWord(data.mMatWordImg);
                                    LOGGER.d("SallyRecog Single Scoring : Predicted= " + result + "(serial= " + data.mSerialNumber + ", refer= " + data.mAnswer + ")");
                                    LOGGER.d("SallyRecog                  ModelName= " + modelName[0]);
                                    resultAll = result;
                                }
                            });
                            thread.start();
                            try {
                                thread.join();
                            }catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                thread =  null;
                            }
                            scoringType = SINGLE_MODEL_SCORING;
                            // OCRManager 에 결과 스트링 전달.
                            Message retmsg = new Message();
                            retmsg.what = MSG_WORD_RECOGNITION_DONE;
                            retmsg.arg1 = data.mSerialNumber;
                            retmsg.arg2 = scoringType;  //멀티모델 인식인지의 여부
                            retmsg.obj = resultAll; // 인식 결과 스트링 (집단채점인 경우 세 모델의 데이터 모음)

                            mOCRManagerHandler.sendMessage(retmsg);
                        }


                    }

                    mStartFlag = false;
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
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
    }
    private void handleMessages(Message msg) {
        switch (msg.what) {
            default:
                break;
        }
    }
}
