package com.ispd.mommybook.aiscore;

import android.os.Handler;
import android.os.Message;

import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.ocr.recognition.OCRRecognition;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Rect;

import static com.ispd.mommybook.MainHandlerMessages.DRAW_AISCORE;
import static com.ispd.mommybook.MainHandlerMessages.DRAW_AISCORE_MULTI;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.TextLanguage.ENGLISH;
import static com.ispd.mommybook.aiscore.AIScoreReferenceDB.TextLanguage.KOREAN;

public class AIScoreFunctions {
    private static final UtilsLogger LOGGER = new UtilsLogger();
    public static final int MSG_REQUEST_TEXT_SCORING = 1000;
    private Handler mMainHandler;

    public AIScoreFunctions(Handler in_handler) {
        mMainHandler = in_handler;
    }

    // 인식된 문자열과 정답 문자열을 비교해서 점수를 계산.
    private int calcTextScore(String reference, String answer) {

        // 글자 단위로 1:1 비교를 위해 한글자씩 잘라서 배열에 저장함.
        String[] arrayAnswer;
        arrayAnswer = answer.split("");
        String[] arrayRefer;
        arrayRefer = reference.split("");
        int answerLen = arrayAnswer.length - 1; //array의 맨 처음 요소는 항상 공백이어서 1을 빼야함.
        int referLen = arrayRefer.length - 1; //array의 맨 처음 요소는 항상 공백이어서 1을 빼야함.

        int correctSpellingsCnt = referLen;
        int loopCnt = 0;
        if(referLen >= answerLen) {
            loopCnt = answerLen;
        }
        else {
            loopCnt = referLen;
        }

        //한글자씩 비교하면서 틀린 글자만큼 개수를 세어 점수 계산함.
        //array의 맨 처음 요소가 항상 공백이어서 인덱스는 1부터 적용.
        for(int i = 1; i <= loopCnt; i++) {
            LOGGER.d("SallyRecog-A[" + i + "]=" + arrayAnswer[i]);

            if(!(arrayRefer[i].equals(arrayAnswer[i]))){
                correctSpellingsCnt--;
            }
        }
        // 정답을 덜 쓴 경우엔 덜 쓴 만큼 점수를 깎음.
        if (referLen > answerLen) {
            correctSpellingsCnt -= (referLen - answerLen);
        }

        // 점수 계산
        int score = (int)((float)correctSpellingsCnt / referLen * 100.f);
        LOGGER.d("SallyRecog-A calcTextScore : refer = " +reference + ", answer = " + answer + ", score = " + score);
        return score;
    }

    public void DoTextScoring(AIScoreReferenceDB.DataBase in_dataBase, /*int in_serialNumber,*/
                               int in_isMultiRecognition, String in_recognizedText) {
        LOGGER.d("SallyRecog doTextScoring()");
        //int dbIdx = in_serialNumber;
        String realAnswerKey = in_dataBase.mAnswerText; //실제 정답
        String convertedAnswerKey = realAnswerKey; //대문자->소문자, 띄어쓰기 없는 형태의 변환된 정답
        if(in_dataBase.mTextLanguage == ENGLISH) {
            // 영어인 경우 정답과 인식결과 비교를 위해 정답문자열을 대문자->소문자, 띄어쓰기 없는 형태로 변환한다.
            convertedAnswerKey = realAnswerKey.toLowerCase();
            convertedAnswerKey = convertedAnswerKey.replaceAll(" ", "");
            convertedAnswerKey = convertedAnswerKey.replaceAll("'", "");
            LOGGER.d("SallyRecog doTextScoring() answer : " + realAnswerKey + " -> " + convertedAnswerKey);
        }
        else if(in_dataBase.mTextLanguage == KOREAN) {
            if(in_dataBase.mSticker == AIScoreReferenceDB.Sticker.STICKER) {
                // typo 한글 인식의 경우, 한영 통합 모델이어서 인식 결과에 알파벳,숫자가 섞여 나오는 경우가 있어서 제거하는 루틴을 넣음.
                // 알파벳 제거 루틴 : 예) i우리는 -> 우리는, 1볼거리가 -> 볼거리가, 재미있어s ->재미있어

                // 정답이 숫자+한글이면 인식결과에서 알파벳,숫자를 제거하지 않고,
                //    정답이 한글 only인 경우에만 인식결과에서 알파벳,숫자를 제거하기.
                boolean hasNumbers = HasNumbers(in_recognizedText);
                if (hasNumbers == false) {
                    for (int i = 0; i < in_recognizedText.length(); i++) {
                        if ((48 <= in_recognizedText.charAt(i) && in_recognizedText.charAt(i) <= 57) || //숫자
                                (65 <= in_recognizedText.charAt(i) && in_recognizedText.charAt(i) <= 90) || //알파벳 대문자
                                (97 <= in_recognizedText.charAt(i) && in_recognizedText.charAt(i) <= 122)) { //알파벳 소문자
                            String compared = String.valueOf(in_recognizedText.charAt(i));
                            LOGGER.d("SallyRecog doTextScoring()-2 " + in_recognizedText + " -> ");
                            in_recognizedText = in_recognizedText.replaceAll(compared, "");
                            LOGGER.d("SallyRecog ----------------" + in_recognizedText);
                        }
                    }
                }
//                //공백만 제거
//                LOGGER.d("SallyRecog doTextScoring()-2 " + in_recognizedText + " -> ");
//                in_recognizedText = in_recognizedText.replaceAll(" ", "");
//                LOGGER.d("SallyRecog ----------------" + in_recognizedText);
            }
            else if(in_dataBase.mSticker == AIScoreReferenceDB.Sticker.NONE) {
                // 손글씨 한글 인식의 경우, 인식 결과에 숫자,문장부호가 섞여 나오는 경우가 있어서 제거하는 루틴을 넣음.
                // 추후 문장부호가 필요한 경우엔 별도 처리 루틴 넣기.
                // 숫자,부호 제거 루틴 : 예) "우리는 -> 우리는, 1귀여워서 -> 귀여워서
                for (int i = 0; i < in_recognizedText.length(); i++) {
                    if ((33 <= in_recognizedText.charAt(i) && in_recognizedText.charAt(i) <= 46) ||
                       (48 <= in_recognizedText.charAt(i) && in_recognizedText.charAt(i) <= 64) /*||
                            (97 <= in_recognizedText.charAt(i) && in_recognizedText.charAt(i) <= 122)*/) {
                        String compared = String.valueOf(in_recognizedText.charAt(i));
                        LOGGER.d("SallyRecog doTextScoring()-3 " + in_recognizedText + " -> ");
                        in_recognizedText = in_recognizedText.replaceAll(compared, "");
                        LOGGER.d("SallyRecog ----------------" + in_recognizedText);
                    }
                }
            }
        }
        //TODO: 한글인식인 경우 띄어쓰기만 없애고 비교. 일단 현재는 단어만 인식하는 구조이므로 추후 구현

        int x = (int) (in_dataBase.mX *  MainActivity.gPreviewRenderWidth);
        int y = (int) (in_dataBase.mY * (MainActivity.gPreviewRenderHeight - 10*4));
        int w = (int) (in_dataBase.mW *  MainActivity.gPreviewRenderWidth);
        int h = (int) (in_dataBase.mH * (MainActivity.gPreviewRenderHeight - 10*4));

        if(in_isMultiRecognition == 1) { //집단 채점인 경우
            // 저장하는 형태 : "MULTI//결과1//점수1//결과2//점수2//결과3//점수3//평균점수"
            String result[] = in_recognizedText.split(OCRRecognition.TEXT_SEPARATOR);
            int score[] = {0,0,0};
            int scoreSum = 0;
            int scoreAvg = 0;
            String saveAnswer = "MULTI";
            LOGGER.d("SallyRecog DoTextScoring() Multi Result Length= " + result.length);
            if(result.length == 3) {
            for(int i = 0; i < 3; i++) {
                score[i] = calcTextScore(convertedAnswerKey, result[i]);
                scoreSum += score[i];
                saveAnswer = saveAnswer + "//" + result[i] + "//" + Integer.toString(score[i]);
            }
                scoreAvg = scoreSum / 3;
            saveAnswer = saveAnswer + "//" + Integer.toString(scoreAvg);
            }
            else { //인식결과가 제대로 나오지 않는 경우임. 아예 인식이 안되거나 숫자,기호로만 인식된 경우임.
                scoreAvg = 0;
                saveAnswer = saveAnswer+"//NAN//0//NAN//0//NAN//0//0";
            }

            boolean isCorrect = false;
            if(scoreAvg == 100) { //3개의 모델 채점 결과 평균이 100이 나와야 정답으로 함.
                isCorrect = true;
            }

            // 파일에 채점 결과를 기록하기 위해 db에 미리 저장함.
            in_dataBase.mSaveAnswer = saveAnswer;//in_recognizedText;
            in_dataBase.mDoGrading = 1;
            in_dataBase.mIsCorrect = isCorrect ? 1 : 0;

            LOGGER.d("SallyRecog doTextScoring() mSaveAnswer = " + in_dataBase.mSaveAnswer);
            //채점 결과를 AIScoreMultiScoringInfo 에 저장
//            AIScoreMultiScoringInfo scoringData =
//                    new AIScoreMultiScoringInfo(result, score, isCorrect, x, y, w, h);
//            LOGGER.d("SallyRecog doTextScoring() x, y, w, h : " + x + ", "
//                    + y + ", " + w + ", " + h);

            // 메인 핸들러에게 채점 결과를 보내줘서 화면에 표시하도록 함.
            // 전송해줄것들 : 집단채점여부, 답이 맞았는지 여부, 채점 결과 스트링("text1 text2 text3 100 66 55")
            //             채점할 위치 영역.
//            Message retmsg = new Message();
//            retmsg.what = DRAW_AISCORE_MULTI;
//            retmsg.arg1 = in_isMultiRecognition;
//            retmsg.obj = scoringData;
//            mMainHandler.sendMessage(retmsg);
        }
        else { // 단일채점인 경우
            // 저장하는 형태 : "SINGLE//결과스트링"
            int score = calcTextScore(convertedAnswerKey, in_recognizedText);

            int isCorrect = 0;
            if(score == 100) {
                isCorrect = 1;
            }else {
                //스티커모드인 경우 80%정도만 인식돼도 맞다고 판단함.
                if(in_dataBase.mSticker == AIScoreReferenceDB.Sticker.STICKER) {
                    if(score >= 75) {
                        isCorrect = 1;
                    }
                }
            }
            String saveAnswer = "SINGLE//" + in_recognizedText;

            // 파일에 채점 결과를 기록하기 위해 db에 미리 저장함.
            in_dataBase.mSaveAnswer = saveAnswer;//in_recognizedText;
            in_dataBase.mDoGrading = 1;
            in_dataBase.mIsCorrect = isCorrect;
            LOGGER.d("SallyRecog doTextScoring() mSaveAnswer = " + in_dataBase.mSaveAnswer);
//            Rect rectArea = new Rect(x, y, w, h);
//
//            Message retmsg = new Message();
//            retmsg.what = DRAW_AISCORE;
//            retmsg.arg1 = isCorrect; // 정답인지 오답인지 여부, 1=정답, 0=오답
//            retmsg.obj = rectArea;
//            mMainHandler.sendMessage(retmsg);
        }
    }
    // 입력 스트링에 숫자가 포함되어 있는지 여부를 리턴함.
    public boolean HasNumbers(String in_string) {
        for (int i = 0; i < in_string.length(); i++) {
            if ((33 <= in_string.charAt(i) && in_string.charAt(i) <= 64) ){
                return true;
            }
        }
        return false;
    }

}
