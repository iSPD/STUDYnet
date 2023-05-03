package com.ispd.mommybook.aiscore;

import com.ispd.mommybook.utils.UtilsLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * AIScoreReferenceDB
 *
 * @author Daniel
 * @version 1.0
 */
public class AIScoreReferenceDB {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    /**
     * 이미지 처리 방법
     */
    public enum Method {
        TEXT_RECOGNITION_WORD,
        TEXT_RECOGNITION_SENTENCE,
        TEXT_NARRITIVE, //서술형 문제, 채점은 세모로 표시해놓고 선생님에게 전송
        IMAGE_MATCHING,//파일 위치
        IMAGE_SUBTRACT,//좌우상하 위치
        IMAGE_CLASSIFICATION,
        IMAGE_BUTTON,
        IMAGE_SOUND,
    }

    /**
     * 언어 종류
     */
    public enum TextLanguage {
        KOREAN,
        ENGLISH,
        NUMBER,
        SIGN,
    }

    /**
     * 동그라미 정답의 위치
     */
    public enum ImageSubtractLocation {
        LEFT(0),
        RIGHT(1),
        UP(2),
        DOWN(3);

        ImageSubtractLocation(int value) { this.value = value; }
        private final int value;
        public int value() { return value;}
    }

    /**
     * 채점 여부
     */
    public enum Score {
        NONE,
        SCORE,
    }

    /**
     * 스티커 존재 여부
     */
    public enum Sticker {
        NONE,
        STICKER,
    }

    /**
     * DataBase
     */
    public static class DataBase {

        public int mIndex;

        public int mDoGrading;
        public String mSaveAnswer;
        public int mIsCorrect;

        public Method mMethod;

        //language
        public TextLanguage mTextLanguage;
        public String mAnswerText;

        //imageMatching
        public String mImagePath;

        //imageSubtract
        public ImageSubtractLocation mImageSubtractLocation;

        //imageClassification
        public String mCorrectName;

        //play movie or sound
        public String mPlayPath;

        public Sticker mSticker;
        public int mStickerFileNumber;
        public Score mScore;

        public float mX;
        public float mY;
        public float mW;
        public float mH;

        /**
         * textRecognition
         * 문자 인식용 DB 생성자
         */
        public DataBase(int index, Method method, TextLanguage language, String answer, Sticker sticker, int fileNumber,
                        Score score, float x, float y, float w, float h) {
            mIndex = index;

            mMethod = method;
            mTextLanguage = language;
            mAnswerText = answer;

            mSticker = sticker;
            mStickerFileNumber = fileNumber;
            mScore = score;

            mX = x;
            mY = y;
            mW = w;
            mH = h;
        }

        /**
         * imageMatching
         * 이미지 매칭용 DB 생성자
         */
        public DataBase(int index, Method method, String filePath, Sticker sticker, int fileNumber,
                        Score score, float x, float y, float w, float h) {
            mIndex = index;

            mMethod = method;
            mImagePath = filePath;

            mSticker = sticker;
            mStickerFileNumber = fileNumber;
            mScore = score;

            mX = x;
            mY = y;
            mW = w;
            mH = h;
        }

        /**
         * imageSubtract
         * 이미지 비교용 DB 생성자
         */
        public DataBase(int index, Method method, String filePath, ImageSubtractLocation compareLocation, Sticker sticker, int fileNumber,
                        Score score, float x, float y, float w, float h) {
            mIndex = index;

            mMethod = method;
            mImagePath = filePath;
            mImageSubtractLocation = compareLocation;

            mSticker = sticker;
            mStickerFileNumber = fileNumber;
            mScore = score;

            mX = x;
            mY = y;
            mW = w;
            mH = h;
        }

        /**
         * imageClassification
         * 이미지 매칭용 DB 생성자
         */
        public DataBase(int index, Method method, Sticker sticker, int fileNumber, String correctName,
                        Score score, float x, float y, float w, float h) {
            mIndex = index;

            mMethod = method;
            mCorrectName = correctName;

            mSticker = sticker;
            mStickerFileNumber = fileNumber;
            mScore = score;

            mX = x;
            mY = y;
            mW = w;
            mH = h;
        }

        /**
         * imageTouch
         * 터치버튼용 DB 생성자
         */
        public DataBase(int index, Method method, String playPath, Sticker sticker, int fileNumber,
                        float x, float y, float w, float h) {
            mIndex = index;

            mMethod = method;
            mPlayPath = playPath;

            mSticker = sticker;
            mStickerFileNumber = fileNumber;

            mX = x;
            mY = y;
            mW = w;
            mH = h;
        }
    }

    private List<DataBase> mKoreanDB= new ArrayList<>();
    private List<DataBase> mMathDB= new ArrayList<>();
    private List<DataBase> mEnglishDB= new ArrayList<>();

    public AIScoreReferenceDB() {
        //책의 인덱스 표지 : 0, 그다음 한바닥 페이지 : 1, 이미지처리방법(이미지 빼기), 이미지를 뺄 원본 사진 위치, 오른쪽에 동그라미가 있으면 정답, 채점안함, 위치(0~1)
        //스티커 존재 여부 추가함
//        mKoreanDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, "/sdcard/studyNet/DB/korean/compare/1-1.png", ImageSubtractLocation.RIGHT, Sticker.NONE, -1, Score.NONE, 0.254f, 0.351f, 0.104f, 0.061f));
//        mKoreanDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, "/sdcard/studyNet/DB/korean/compare/1-2.png", ImageSubtractLocation.LEFT, Sticker.NONE, -1, Score.NONE, 0.158f, 0.401f, 0.103f, 0.061f));
//        mKoreanDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, "/sdcard/studyNet/DB/korean/compare/1-3.png", ImageSubtractLocation.RIGHT, Sticker.NONE, -1, Score.NONE, 0.090f, 0.502f, 0.110f, 0.070f));
//        mKoreanDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, "/sdcard/studyNet/DB/korean/compare/1-4.png", ImageSubtractLocation.RIGHT, Sticker.NONE, -1, Score.NONE, 0.636f, 0.256f, 0.107f, 0.062f));
//        mKoreanDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, "/sdcard/studyNet/DB/korean/compare/1-5.png", ImageSubtractLocation.LEFT, Sticker.NONE, -1, Score.NONE, 0.561f, 0.308f, 0.105f, 0.063f));
//        mKoreanDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, "/sdcard/studyNet/DB/korean/compare/1-6.png", ImageSubtractLocation.LEFT, Sticker.NONE, -1, Score.NONE, 0.584f, 0.362f, 0.105f, 0.062f));

        mKoreanDB.add(new DataBase(1, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/1-1.png", ImageSubtractLocation.RIGHT, Sticker.NONE, -1, Score.NONE, 0.254f, 0.351f, 0.104f, 0.061f));
        mKoreanDB.add(new DataBase(1, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/1-2.png", ImageSubtractLocation.LEFT, Sticker.NONE, -1, Score.NONE, 0.158f, 0.401f, 0.103f, 0.061f));
        mKoreanDB.add(new DataBase(1, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/1-3.png", ImageSubtractLocation.RIGHT, Sticker.NONE, -1, Score.NONE, 0.090f, 0.502f, 0.110f, 0.070f));
        mKoreanDB.add(new DataBase(1, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/1-4.png", ImageSubtractLocation.RIGHT, Sticker.NONE, -1, Score.NONE, 0.636f, 0.256f, 0.107f, 0.062f));
        mKoreanDB.add(new DataBase(1, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/1-5.png", ImageSubtractLocation.LEFT, Sticker.NONE, -1, Score.NONE, 0.561f, 0.308f, 0.105f, 0.063f));
        mKoreanDB.add(new DataBase(1, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/1-6.png", ImageSubtractLocation.LEFT, Sticker.NONE, -1, Score.NONE, 0.584f, 0.362f, 0.105f, 0.062f));

        //책의 인덱스 표지 : 0, 그다음 한바닥 페이지 : 1, 이미지처리방법(버튼 띄우기), 동영상 위치(현재는 앱 raw폴더에 들어있음), 위치(0~1)
        //스티커 존재 여부 추가함
        mKoreanDB.add(new DataBase(2, Method.IMAGE_BUTTON, "/sdcard/minsok.mp4", Sticker.NONE, -1, 0.120f, 0.650f, 0.059f, 0.097f));

        //책의 인덱스 표지 : 0, 그다음 한바닥 페이지 : 1, 이미지처리방법(문자인식), 언어종류, 정답언어, 채점안함, 위치(0~1)
        //스티커 존재 여부 추가함
//        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "우리는", Sticker.STICKER, 2, Score.NONE,0.620f, 0.264f, 0.076f, 0.060f));
//        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "버스를", Sticker.STICKER, 3, Score.NONE,0.744f, 0.264f, 0.067f, 0.060f));
//        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "탔어요", Sticker.STICKER, 4, Score.NONE,0.811f, 0.264f, 0.061f, 0.060f));
//        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "부침개와", Sticker.STICKER, 5, Score.NONE,0.598f, 0.568f, 0.081f, 0.060f));
//        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "비빔밥을", Sticker.STICKER, 6, Score.NONE,0.680f, 0.568f, 0.080f, 0.060f));
//        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "먹었어요", Sticker.STICKER, 7, Score.NONE,0.811f, 0.568f, 0.084f, 0.060f));
//        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "볼거리가", Sticker.STICKER, 8, Score.NONE,0.652f, 0.874f, 0.095f, 0.060f));
//        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "많아서", Sticker.STICKER, 9, Score.NONE,0.747f, 0.874f, 0.092f, 0.060f));
//        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "재미있어요", Sticker.STICKER, 10, Score.NONE,0.838f, 0.874f, 0.093f, 0.060f));
        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "우리는", Sticker.STICKER, 2, Score.NONE,0.628f, 0.272f, 0.06f, 0.039f));
        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "버스를", Sticker.STICKER, 3, Score.NONE,0.75f, 0.271f, 0.061f, 0.041f));
        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "탔어요", Sticker.STICKER, 4, Score.NONE,0.81f, 0.271f, 0.060f, 0.042f));
        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "부침개와", Sticker.STICKER, 5, Score.NONE,0.60f, 0.575f, 0.075f, 0.048f));
        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "비빔밥을", Sticker.STICKER, 6, Score.NONE,0.680f, 0.576f, 0.072f, 0.042f));
        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "먹었어요", Sticker.STICKER, 7, Score.NONE,0.817f, 0.577f, 0.072f, 0.040f));
        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "볼거리가", Sticker.STICKER, 8, Score.NONE,0.656f, 0.883f, 0.09f, 0.040f));
        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "많아서", Sticker.STICKER, 9, Score.NONE,0.746f, 0.883f, 0.09f, 0.043f));
        mKoreanDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "재미있어요", Sticker.STICKER, 10, Score.NONE,0.839f, 0.882f, 0.089f, 0.043f));

//        mKoreanDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "무", Sticker.NONE, -1, Score.SCORE,0.716f, 0.202f, 0.101f, 0.101f));
//        mKoreanDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "게으름뱅이", Sticker.NONE, -1, Score.SCORE,0.716f, 0.311f, 0.219f, 0.101f));
//        mKoreanDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "며칠", Sticker.NONE, -1, Score.SCORE,0.716f, 0.421f, 0.101f, 0.101f));
//        mKoreanDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "다람쥐", Sticker.NONE, -1, Score.SCORE,0.716f, 0.533f, 0.136f, 0.101f));

        mKoreanDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "무", Sticker.NONE, -1, Score.SCORE,0.726f, 0.218f, 0.079f, 0.065f));
        mKoreanDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "게으름뱅이", Sticker.NONE, -1, Score.SCORE,0.725f, 0.329f, 0.198f, 0.066f));
        mKoreanDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "며칠", Sticker.NONE, -1, Score.SCORE,0.725f, 0.438f, 0.079f, 0.065f));
        mKoreanDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "다람쥐", Sticker.NONE, -1, Score.SCORE,0.725f, 0.55f, 0.119f, 0.065f));
		
        //책의 인덱스 표지 : 0, 그다음 한바닥 페이지 : 1, 이미지처리방법(버튼 띄우기), 동영상 위치(현재는 앱 raw폴더에 들어있음), 위치(0~1)
        //스티커 존재 여부 추가함
        //mKoreanDB.add(new DataBase(4, Method.IMAGE_BUTTON, "/sdcard/namul.mp4", Sticker.NONE, -1, 0.683f, 0.194f, 0.059f, 0.097f));

        //애매모호함 다시 체크 부탁 해요...
        //책의 인덱스 표지 : 0, 그다음 한바닥 페이지 : 1, 이미지처리방법(문자인식), 언어종류, 정답언어, 채점안함, 위치(0~1)
        //스티커 존재 여부 추가함
//        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "특히 나물을 참 맛있게 무치시지", Sticker.NONE, -1, Score.SCORE,0.566f, 0.307f, 0.368f, 0.154f));
//        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "귀여워서 그렇게 부르는 것이니까", Sticker.NONE, -1, Score.SCORE,0.566f, 0.790f, 0.368f, 0.154f));

//        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_SENTENCE, TextLanguage.KOREAN, "특히 나물을 참 맛있게 무치시지", Sticker.NONE, -1, Score.SCORE,0.566f, 0.307f, 0.368f, 0.154f));
//        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_SENTENCE, TextLanguage.KOREAN, "귀여워서 그렇게 부르는 것이니까", Sticker.NONE, -1, Score.SCORE,0.566f, 0.790f, 0.368f, 0.154f));

        //sentence를 word 단위로 쪼개어 따로 채점.
        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "특히", Sticker.NONE, -1, Score.SCORE,0.573f, 0.318f, 0.079f, 0.064f));
        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "나물을", Sticker.NONE, -1, Score.SCORE,0.69f, 0.315f, 0.119f, 0.068f));
        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "참", Sticker.NONE, -1, Score.SCORE,0.846f, 0.317f, 0.042f, 0.067f));
        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "맛있게", Sticker.NONE, -1, Score.SCORE,0.573f, 0.379f, 0.12f, 0.069f));
        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "무치시지", Sticker.NONE, -1, Score.SCORE,0.730f, 0.377f, 0.157f, 0.068f));
        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "귀여워서", Sticker.NONE, -1, Score.SCORE,0.575f, 0.798f, 0.158f, 0.071f));
        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "그렇게", Sticker.NONE, -1, Score.SCORE,0.769f, 0.796f, 0.119f, 0.071f));
        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "부르는", Sticker.NONE, -1, Score.SCORE,0.576f, 0.867f, 0.117f, 0.068f));
        mKoreanDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "것이니까", Sticker.NONE, -1, Score.SCORE,0.731f, 0.859f, 0.158f, 0.076f));

        //책의 인덱스 표지 : 0, 그다음 한바닥 페이지 : 1, 이미지처리방법(이미지 매칭), 이미지를 매칭할 원본 사진 위치, 채점안함, 위치(0~1)
        //스티커 존재 여부 추가함
//        mKoreanDB.add(new DataBase(5, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/5-1.png", Sticker.NONE, -1, Score.NONE,0.241f, 0.246f, 0.200f, 0.142f));
//        mKoreanDB.add(new DataBase(5, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/5-2.png", Sticker.NONE, -1, Score.NONE,0.241f, 0.622f, 0.200f, 0.142f));
//        mKoreanDB.add(new DataBase(5, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/5-3.png", Sticker.NONE, -1, Score.NONE,0.740f, 0.099f, 0.196f, 0.138f));
//        mKoreanDB.add(new DataBase(5, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/korean/compare/5-4.png", Sticker.NONE, -1, Score.NONE,0.748f, 0.478f, 0.196f, 0.138f));
       mKoreanDB.add(new DataBase(5, Method.TEXT_NARRITIVE, "/sdcard/studyNet/DB/korean/compare/5-1.png", Sticker.NONE, -1, Score.NONE,0.241f, 0.246f, 0.200f, 0.142f));
        mKoreanDB.add(new DataBase(5, Method.TEXT_NARRITIVE, "/sdcard/studyNet/DB/korean/compare/5-2.png", Sticker.NONE, -1, Score.NONE,0.241f, 0.622f, 0.200f, 0.142f));
        mKoreanDB.add(new DataBase(5, Method.TEXT_NARRITIVE, "/sdcard/studyNet/DB/korean/compare/5-3.png", Sticker.NONE, -1, Score.NONE,0.740f, 0.099f, 0.196f, 0.138f));
        mKoreanDB.add(new DataBase(5, Method.TEXT_NARRITIVE, "/sdcard/studyNet/DB/korean/compare/5-4.png", Sticker.NONE, -1, Score.NONE,0.748f, 0.478f, 0.196f, 0.138f));

        //math DB
//        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "72", Sticker.STICKER, 1, Score.SCORE,0.065f, 0.265f, 0.177f, 0.221f/2.0f));
//        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "80", Sticker.STICKER, 2, Score.SCORE,0.246f, 0.265f, 0.177f, 0.221f/2.0f));
//        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "75", Sticker.STICKER, 3, Score.SCORE,0.065f, 0.491f, 0.177f, 0.221f/2.0f));
//        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "78", Sticker.STICKER, 4, Score.SCORE,0.246f, 0.491f, 0.177f, 0.221f/2.0f));
        //솥뚜껑 숫자부분만 다시 지정함.
        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "72", Sticker.STICKER, 1, Score.SCORE,0.13f, 0.318f, 0.051f, 0.06f));
        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "80", Sticker.STICKER, 2, Score.SCORE,0.309f, 0.318f, 0.051f, 0.06f));
        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "75", Sticker.STICKER, 3, Score.SCORE,0.13f, 0.536f, 0.051f, 0.06f));
        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "78", Sticker.STICKER, 4, Score.SCORE,0.309f, 0.536f, 0.05f, 0.06f));

        //mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION, TextLanguage.SIGN, "<", Sticker.NONE, -1, Score.SCORE,0.694f, 0.188f, 0.073f, 0.116f));
        //mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION, TextLanguage.SIGN, ">", Sticker.NONE, -1, Score.SCORE,0.773f, 0.358f, 0.072f, 0.118f));
        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION_WORD, TextLanguage.SIGN, "<", Sticker.NONE, -1, Score.SCORE,0.709f, 0.209f, 0.043f, 0.071f));
        mMathDB.add(new DataBase(1, Method.TEXT_RECOGNITION_WORD, TextLanguage.SIGN, ">", Sticker.NONE, -1, Score.SCORE,0.786f, 0.381f, 0.043f, 0.072f));

        mMathDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, Sticker.STICKER, 7, "fly", Score.SCORE,0.561f, 0.592f, 0.123f, 0.154f));
        mMathDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, Sticker.STICKER, 8, "fly", Score.SCORE,0.756f, 0.592f, 0.123f, 0.154f));
        mMathDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, Sticker.STICKER, 9, "background", Score.SCORE,0.631f, 0.762f, 0.123f, 0.154f));
        mMathDB.add(new DataBase(1, Method.IMAGE_SUBTRACT, Sticker.STICKER, 10, "background", Score.SCORE,0.816f, 0.762f, 0.123f, 0.154f));

//        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "3시50분", Sticker.STICKER, 1, Score.SCORE,0.073f, 0.598f, 0.068f, 0.052f));
        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "3시50분", Sticker.STICKER, 1, Score.SCORE,0.078f, 0.604f, 0.059f, 0.038f));
//        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "7시5분", Sticker.STICKER, 2, Score.SCORE,0.215f, 0.598f, 0.068f, 0.052f));
        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "7시5분", Sticker.STICKER, 2, Score.SCORE,0.221f, 0.605f, 0.059f, 0.038f));
//        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.KOREAN, "2시35분", Sticker.STICKER, 3, Score.SCORE,0.353f, 0.598f, 0.068f, 0.052f));
        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.KOREAN, "2시35분", Sticker.STICKER, 3, Score.SCORE,0.357f, 0.606f, 0.059f, 0.038f));
		
//        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "8", Sticker.NONE, -1, Score.SCORE,0.566f, 0.443f, 0.045f, 0.075f));
		mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "8", Sticker.NONE, -1, Score.SCORE,0.569f, 0.451f, 0.039f, 0.061f));
//        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "25", Sticker.NONE, -1, Score.SCORE,0.629f, 0.443f, 0.045f, 0.075f));
        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "25", Sticker.NONE, -1, Score.SCORE,0.632f, 0.449f, 0.039f, 0.062f));
//        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "10", Sticker.NONE, -1, Score.SCORE,0.765f, 0.445f, 0.045f, 0.075f));
        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "10", Sticker.NONE, -1, Score.SCORE,0.768f, 0.445f, 0.038f, 0.064f));
//        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "40", Sticker.NONE, -1, Score.SCORE,0.828f, 0.445f, 0.045f, 0.075f));
        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "40", Sticker.NONE, -1, Score.SCORE,0.830f, 0.445f, 0.039f, 0.065f));
//        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "12", Sticker.NONE, -1, Score.SCORE,0.565f, 0.780f, 0.045f, 0.075f));
        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "12", Sticker.NONE, -1, Score.SCORE,0.568f, 0.780f, 0.038f, 0.064f));
//        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "10", Sticker.NONE, -1, Score.SCORE,0.627f, 0.780f, 0.045f, 0.075f));
        mMathDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "10", Sticker.NONE, -1, Score.SCORE,0.631f, 0.780f, 0.038f, 0.063f));
		
//        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "12", Sticker.NONE, -1, Score.SCORE,0.289f, 0.334f, 0.064f, 0.076f));
//        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "11", Sticker.NONE, -1, Score.SCORE,0.289f, 0.595f, 0.064f, 0.076f));
//        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "10", Sticker.NONE, -1, Score.SCORE,0.294f, 0.861f, 0.064f, 0.076f));
//        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "11", Sticker.NONE, -1, Score.SCORE,0.786f, 0.316f, 0.064f, 0.076f));
//        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "10", Sticker.NONE, -1, Score.SCORE,0.783f, 0.591f, 0.064f, 0.076f));
//        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION, TextLanguage.NUMBER, "13", Sticker.NONE, -1, Score.SCORE,0.786f, 0.858f, 0.064f, 0.076f));

        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "12", Sticker.NONE, -1, Score.SCORE,0.294f, 0.342f, 0.054f, 0.061f));//영역수정
        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "11", Sticker.NONE, -1, Score.SCORE,0.293f, 0.6f, 0.056f, 0.061f));//영역수정
        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "10", Sticker.NONE, -1, Score.SCORE,0.298f, 0.867f, 0.054f, 0.065f));//영역수정
        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "11", Sticker.NONE, -1, Score.SCORE,0.791f, 0.322f, 0.054f, 0.062f)); //영역수정
        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "10", Sticker.NONE, -1, Score.SCORE,0.787f, 0.598f, 0.054f, 0.063f));//영역수정
        mMathDB.add(new DataBase(3, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "13", Sticker.NONE, -1, Score.SCORE,0.792f, 0.866f, 0.054f, 0.063f));//영역수정

        mMathDB.add(new DataBase(4, Method.IMAGE_MATCHING, "/sdcard/studyNet/DB/math/compare/4-1.png", ImageSubtractLocation.RIGHT, Sticker.NONE, -1, Score.SCORE,0.052f, 0.775f, 0.398f, 0.158f));
        mMathDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "11", Sticker.NONE, -1, Score.SCORE,0.755f, 0.812f, 0.045f, 0.078f));
        mMathDB.add(new DataBase(4, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "50", Sticker.NONE, -1, Score.SCORE,0.817f, 0.812f, 0.045f, 0.078f));

        mMathDB.add(new DataBase(5, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "3", Sticker.NONE, -1, Score.SCORE,0.217f, 0.851f, 0.049f, 0.082f));
        mMathDB.add(new DataBase(5, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "12", Sticker.NONE, -1, Score.SCORE,0.286f, 0.851f, 0.066f, 0.082f));
        mMathDB.add(new DataBase(5, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "1", Sticker.NONE, -1, Score.SCORE,0.713f, 0.741f, 0.049f, 0.082f));
        mMathDB.add(new DataBase(5, Method.TEXT_RECOGNITION_WORD, TextLanguage.NUMBER, "11", Sticker.NONE, -1, Score.SCORE,0.782f, 0.741f, 0.066f, 0.082f));

        //english DB
        mEnglishDB.add(new DataBase(1, Method.IMAGE_SOUND, "welcome", Sticker.NONE, -1, 0.694f, 0.454f, 0.133f, 0.151f));

        mEnglishDB.add(new DataBase(2, Method.IMAGE_SOUND, "onno", Sticker.NONE, -1, 0.180f, 0.380f, 0.118f, 0.101f));
//        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "P", Sticker.STICKER, 2, Score.SCORE,0.414f, 0.564f, 0.072f, 0.117f));
//        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "C", Sticker.STICKER, 3, Score.SCORE,0.513f, 0.306f, 0.076f, 0.117f));
//        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "G", Sticker.STICKER, 4, Score.SCORE,0.801f, 0.306f, 0.067f, 0.111f));
//        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "K", Sticker.STICKER, 5, Score.SCORE,0.588f, 0.437f, 0.070f, 0.108f));
//        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "Z", Sticker.STICKER, 6, Score.SCORE,0.658f, 0.693f, 0.072f, 0.114f));
        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "p", Sticker.STICKER, 2, Score.SCORE,0.424f, 0.583f, 0.042f, 0.073f));
        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "c", Sticker.STICKER, 3, Score.SCORE,0.530f, 0.338f, 0.031f, 0.059f));
        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "g", Sticker.STICKER, 4, Score.SCORE,0.81f, 0.325f, 0.039f, 0.062f));
        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "k", Sticker.STICKER, 5, Score.SCORE,0.601f, 0.458f, 0.032f, 0.06f));
        mEnglishDB.add(new DataBase(2, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "z", Sticker.STICKER, 6, Score.SCORE,0.671f, 0.723f, 0.034f, 0.061f));

        mEnglishDB.add(new DataBase(3, Method.IMAGE_SOUND, "whatwouldyoulike", Sticker.NONE, -1, 0.283f, 0.512f, 0.202f, 0.145f));
        mEnglishDB.add(new DataBase(3, Method.IMAGE_SOUND, "iwantadonut", Sticker.NONE, -1, 0.616f, 0.518f, 0.255f, 0.187f));

        mEnglishDB.add(new DataBase(4, Method.IMAGE_SOUND, "donut", Sticker.NONE, -1, 0.389f, 0.412f, 0.074f, 0.134f));
        mEnglishDB.add(new DataBase(4, Method.IMAGE_SOUND, "socks", Sticker.NONE, -1, 0.307f, 0.513f, 0.075f, 0.154f));
        mEnglishDB.add(new DataBase(4, Method.IMAGE_SOUND, "hat", Sticker.NONE, -1, 0.186f, 0.729f, 0.095f, 0.152f));
        mEnglishDB.add(new DataBase(4, Method.IMAGE_SOUND, "hairbrush", Sticker.NONE, -1, 0.561f, 0.506f, 0.086f, 0.137f));
        mEnglishDB.add(new DataBase(4, Method.IMAGE_SOUND, "skirt", Sticker.NONE, -1, 0.668f, 0.726f, 0.092f, 0.154f));
        mEnglishDB.add(new DataBase(4, Method.IMAGE_SOUND, "handkerchief", Sticker.NONE, -1, 0.854f, 0.469f, 0.081f, 0.134f));

        mEnglishDB.add(new DataBase(5, Method.IMAGE_SOUND, "iwantjuice", Sticker.NONE, -1, 0.242f, 0.299f, 0.168f, 0.151f));
        mEnglishDB.add(new DataBase(5, Method.IMAGE_SOUND, "iwantafork", Sticker.NONE, -1, 0.789f, 0.174f, 0.157f, 0.142f));
        mEnglishDB.add(new DataBase(5, Method.IMAGE_SOUND, "iwantcrayons", Sticker.NONE, -1, 0.766f, 0.524f, 0.183f, 0.143f));

        mEnglishDB.add(new DataBase(6, Method.IMAGE_SOUND, "thankyou", Sticker.NONE, -1, 0.733f, 0.190f, 0.086f, 0.160f));
        mEnglishDB.add(new DataBase(6, Method.IMAGE_SOUND, "hereyouare", Sticker.NONE, -1, 0.834f, 0.183f, 0.104f, 0.221f));
        mEnglishDB.add(new DataBase(6, Method.IMAGE_SOUND, "hereyouare", Sticker.NONE, -1, 0.739f, 0.566f, 0.098f, 0.176f));
        mEnglishDB.add(new DataBase(6, Method.IMAGE_SOUND, "thankyou", Sticker.NONE, -1, 0.855f, 0.590f, 0.086f, 0.150f));

        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 1, "cookie", Score.SCORE,0.207f, 0.292f, 0.054f, 0.093f));
        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 2, "candy", Score.SCORE,0.090f, 0.417f, 0.047f, 0.062f));
        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 3, "chocolate", Score.SCORE,0.227f, 0.490f, 0.056f, 0.076f));
        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 4, "donut", Score.SCORE,0.126f, 0.679f, 0.048f, 0.086f));
        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 5, "cake", Score.SCORE,0.153f, 0.868f, 0.045f, 0.096f));
//        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 6, "cookie", Score.SCORE,0.356f, 0.390f, 0.098f, 0.104f));
//        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 7, "cake", Score.SCORE,0.335f, 0.621f, 0.098f, 0.115f));
        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 6, "cookie", Score.SCORE,0.356f, 0.390f+0.0104f, 0.098f, 0.104f-0.0104f));
        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 7, "cake", Score.SCORE,0.335f, 0.621f+0.0115f, 0.098f, 0.115f-0.0115f));

        mEnglishDB.add(new DataBase(7, Method.IMAGE_SOUND, "iwantacookie", Sticker.NONE, -1, 0.657f, 0.264f, 0.222f, 0.103f));
        mEnglishDB.add(new DataBase(7, Method.IMAGE_SOUND, "iwantacake", Sticker.NONE, -1, 0.652f, 0.459f, 0.189f, 0.099f));
//        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 1, "background", Score.SCORE,0.803f, 0.642f, 0.116f, 0.132f));
        mEnglishDB.add(new DataBase(7, Method.IMAGE_CLASSIFICATION, Sticker.STICKER, 1, "background", Score.SCORE,0.803f, 0.642f+0.0132f, 0.116f, 0.132f-0.0132f));

        mEnglishDB.add(new DataBase(9, Method.IMAGE_SOUND, "hereyouare", Sticker.NONE, -1, 0.185f, 0.309f, 0.172f, 0.143f));
        mEnglishDB.add(new DataBase(9, Method.IMAGE_SOUND, "thankyou", Sticker.NONE, -1, 0.787f, 0.147f, 0.175f, 0.124f));
        mEnglishDB.add(new DataBase(9, Method.IMAGE_SOUND, "youarewelcome", Sticker.NONE, -1, 0.521f, 0.459f, 0.220f, 0.123f));

        mEnglishDB.add(new DataBase(10, Method.IMAGE_SOUND, "hereyouare", Sticker.NONE, -1, 0.016f, 0.341f, 0.195f, 0.124f));

        //        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "Thank you.", Sticker.STICKER, 2, Score.SCORE,0.295f, 0.296f, 0.169f, 0.081f));
        //Thank you. 스티커모드
//        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "thankyou.", Sticker.STICKER, 2, Score.SCORE,0.333f, 0.298f, 0.128f, 0.073f));
        //Thank you. 집단채점 모드
        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "Thank you.", Sticker.NONE, -1, Score.SCORE,0.299f, 0.296f, 0.16f, 0.078f));

        mEnglishDB.add(new DataBase(10, Method.IMAGE_SOUND, "youarewelcome", Sticker.NONE, -1, 0.024f, 0.715f, 0.220f, 0.109f));
//        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "Here you are.", Sticker.STICKER, 4, Score.SCORE,0.531f, 0.155f, 0.168f, 0.083f));
        //Here you are. 스티커 모드
//        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "hereyouare.", Sticker.STICKER, 4, Score.SCORE,0.561f, 0.160f, 0.135f, 0.069f));
        //Here you are. 집단채점 모드
//        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "hereyouare.", Sticker.NONE, -1, Score.SCORE,0.561f, 0.160f, 0.135f, 0.069f));
        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "Here you are.", Sticker.NONE, -1, Score.SCORE,0.534f, 0.157f, 0.16f, 0.078f));

        mEnglishDB.add(new DataBase(10, Method.IMAGE_SOUND, "thankyou", Sticker.NONE, -1, 0.817f, 0.117f, 0.157f, 0.126f));
        mEnglishDB.add(new DataBase(10, Method.IMAGE_SOUND, "youarewelcome", Sticker.NONE, -1, 0.533f, 0.389f, 0.219f, 0.108f));
        mEnglishDB.add(new DataBase(10, Method.IMAGE_SOUND, "hereyouare", Sticker.NONE, -1, 0.517f, 0.684f, 0.184f, 0.119f));
        mEnglishDB.add(new DataBase(10, Method.IMAGE_SOUND, "thankyou", Sticker.NONE, -1, 0.808f, 0.526f, 0.168f, 0.124f));
//        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "You're welcome.", Sticker.STICKER, 9, Score.SCORE,0.531f, 0.838f, 0.204f, 0.077f));
//        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "yourewelcome.", Sticker.STICKER, 9, Score.SCORE,0.563f, 0.840f, 0.168f, 0.067f));
        mEnglishDB.add(new DataBase(10, Method.TEXT_RECOGNITION_WORD, TextLanguage.ENGLISH, "You're welcome.", Sticker.STICKER, 9, Score.SCORE,0.565f, 0.853f, 0.160f, 0.056f));

        mEnglishDB.add(new DataBase(11, Method.IMAGE_SOUND, "big", Sticker.NONE, -1, 0.574f, 0.186f, 0.175f, 0.179f));
        mEnglishDB.add(new DataBase(11, Method.IMAGE_SOUND, "little", Sticker.NONE, -1, 0.653f, 0.699f, 0.135f, 0.129f));

        mEnglishDB.add(new DataBase(13, Method.IMAGE_SOUND, "little", Sticker.NONE, -1, 0.155f, 0.228f, 0.123f, 0.082f));
        mEnglishDB.add(new DataBase(13, Method.IMAGE_SOUND, "big", Sticker.NONE, -1, 0.798f, 0.730f, 0.182f, 0.209f));

        for(int i = 0; i < mKoreanDB.size(); i++) {
            mKoreanDB.get(i).mDoGrading = 0;
            mKoreanDB.get(i).mSaveAnswer = "none";
            mKoreanDB.get(i).mIsCorrect = 0;
        }

        for(int i = 0; i < mMathDB.size(); i++) {
            mMathDB.get(i).mDoGrading = 0;
            mMathDB.get(i).mSaveAnswer = "none";
            mMathDB.get(i).mIsCorrect = 0;
        }

        for(int i = 0; i < mEnglishDB.size(); i++) {
            mEnglishDB.get(i).mDoGrading = 0;
            mEnglishDB.get(i).mSaveAnswer = "none";
            mEnglishDB.get(i).mIsCorrect = 0;
        }

        readAISCoreResult("korean");
        readAISCoreResult("math");
        readAISCoreResult("english");
    }

    /**
     * GetAIScoreMethod
     * @param coverIndex
     * @param pageIndex
     * @return List<DataBase>
     *
     * 책의 표지와 내지를 이용하여 DB 가져가는 함수
     */
    public List<DataBase> GetAIScoreMethod(int coverIndex, int pageIndex) {
        List<DataBase> resultList = new ArrayList<DataBase>();

        if(coverIndex == 0) {
            for(int i = 0; i < mKoreanDB.size(); i++) {
                if( mKoreanDB.get(i).mIndex == pageIndex ) {
                    resultList.add(mKoreanDB.get(i));
                }
            }
        }
        else if(coverIndex == 1) {
            for(int i = 0; i < mMathDB.size(); i++) {
                if( mMathDB.get(i).mIndex == pageIndex ) {
                    resultList.add(mMathDB.get(i));
                }
            }
        }
        else if(coverIndex == 2) {
            for(int i = 0; i < mEnglishDB.size(); i++) {
                if( mEnglishDB.get(i).mIndex == pageIndex ) {
                    resultList.add(mEnglishDB.get(i));
                }
            }
        }

        return resultList;
    }

    public void readAISCoreResult(String whatBook) {

        List<DataBase> dataBase = null;

        if(whatBook.equals("korean"))
        {
            dataBase = mKoreanDB;
        }
        else if(whatBook.equals("math"))
        {
            dataBase = mMathDB;
        }
        else if(whatBook.equals("english"))
        {
            dataBase = mEnglishDB;
        }

        try{
            String path = "/sdcard/studyNet/DB/"+whatBook+"/result/result.txt";
            InputStream is = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line="";

            for(int i = 0; i < dataBase.size(); i++) {

                line = reader.readLine();
                if(line == null) { // 비어있는 result.txt 을 읽어오는 경우 패스함.
                    LOGGER.d("SallyWork2 result file is empty!!");
                    break;
                }
                line = line.trim();

                int index = Integer.valueOf(line.split(" ")[0]);
                int grading = Integer.valueOf(line.split(" ")[1]);
                String answer = line.split(" ")[2];
                int isCorrect = Integer.valueOf(line.split(" ")[3]);

                dataBase.get(i).mDoGrading = grading;
                dataBase.get(i).mSaveAnswer = answer;
                dataBase.get(i).mIsCorrect = isCorrect;

                LOGGER.d("index : "+i+", grading : "+grading+", answer : "+answer+", isCorrect : "+isCorrect);
            }

            reader.close();
            is.close();
        }catch (IOException e){
            LOGGER.d("IOException : "+e);
            e.printStackTrace();
        }
    }

    public void WriteAIScoreResult(String whatBook) {

        List<DataBase> dataBase = null;

        if(whatBook.equals("korean"))
        {
            dataBase = mKoreanDB;
        }
        else if(whatBook.equals("math"))
        {
            dataBase = mMathDB;
        }
        else if(whatBook.equals("english"))
        {
            dataBase = mEnglishDB;
        }

        File file = new File("/sdcard/studyNet/DB/"+whatBook+"/result/result.txt") ;
        FileWriter fw = null ;
        BufferedWriter bufwr = null ;

        try {
            // open file.
            fw = new FileWriter(file) ;
            bufwr = new BufferedWriter(fw) ;

            for(int i = 0; i < dataBase.size(); i++) {
                String writeData = dataBase.get(i).mIndex+" "+dataBase.get(i).mDoGrading+" "+dataBase.get(i).mSaveAnswer+" "+dataBase.get(i).mIsCorrect;
                // write data to the file.
                bufwr.write(writeData);
                bufwr.newLine();
            }

        } catch (Exception e) {
            e.printStackTrace() ;
        }

        // close file.
        try {
            if (bufwr != null)
                bufwr.close() ;

            if (fw != null)
                fw.close() ;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
