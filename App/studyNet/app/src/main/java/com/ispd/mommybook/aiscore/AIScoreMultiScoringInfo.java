package com.ispd.mommybook.aiscore;

public class AIScoreMultiScoringInfo {
    private String mResultString[] = {"", "", ""}; // 다중 모델 인식 결과
    private String mScore[] = {"", "", ""}; // 다중 모델 채점 결과
    public int mX, mY, mW, mH; // 채점 영역 (MainActivity.gPreviewRenderWidth, gPreviewRenderHeight기준)
    private boolean mIsCorrect; // 정답인지의 여부. 정답=true, 오답=false

    public AIScoreMultiScoringInfo(String[] result, int[] score, boolean isCorrect,
                                   int x, int y, int w, int h) {
        for (int i = 0; i < 3; i++) {
            mResultString[i] = result[i];
            mScore[i] = Integer.toString(score[i]);
        }
        mIsCorrect = isCorrect;
        mX = x;
        mY = y;
        mW = w;
        mH = h;
    }

    public String GetScore(int idx) {
        if(idx < 3) {
            return mScore[idx];
        }
        return "";
    }

    public String GetRecogResult(int idx) {
        if(idx < 3) {
            return mResultString[idx];
        }
        return "";
    }

    public boolean IsCorrect() {
        return mIsCorrect;
    }
}
