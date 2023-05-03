package com.ispd.mommybook.ui;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.R;
import com.ispd.mommybook.aiscore.AIScoreMultiScoringInfo;
import com.ispd.mommybook.utils.UtilsLogger;

import static com.ispd.mommybook.MainHandlerMessages.PLAY_SOUND_SCORING_MESSAGE;

public class UIMultiModelScoring {
    private static final UtilsLogger LOGGER = new UtilsLogger();

    private LinearLayout mLlMultiRecogResult;
    private LinearLayout mLlMultiScoreResult;

    private TextView mTvModel1;
    private TextView mTvModel2;
    private TextView mTvModel3;
    private TextView mTvScore1;
    private TextView mTvScore2;
    private TextView mTvScore3;
    private TextView mTvScoringMessage;

    private ObjectAnimator mModelResultAnim1; //좌측 : 모델1 인식 결과를 나타내는 애니메이션
    private ObjectAnimator mModelResultAnim2; //좌측 : 모델2 인식 결과를 나타내는 애니메이션
    private ObjectAnimator mModelResultAnim3; //좌측 : 모델3 인식 결과를 나타내는 애니메이션
    private ObjectAnimator mScoreResultAnim1; //우측 : 채점1 결과를 나타내는 애니메이션
    private ObjectAnimator mScoreResultAnim2; //우측 : 채점2 결과를 나타내는 애니메이션
    private ObjectAnimator mScoreResultAnim3; //우측 : 채점3 결과를 나타내는 애니메이션
    private ObjectAnimator mScoringMessageAnim; // 글씨 보정 결과를 알려주는 애니메이션

    public boolean mIsScoreAnimRunning = false;

    private String mRecogResult[] = new String[3];

    private Handler mMainHandler;
    public UIMultiModelScoring(View in_root, Handler handler){
        mMainHandler = handler;
        setScoreAnimation(in_root);

    }

    // 각 언어 모델 3개에서 모델번호와 인식 결과를 셋팅하는 함수
    public void SetResultString(int in_modelNo, String result) {
        mRecogResult[in_modelNo] = result;
    }

    //TODO  : 이 함수 호출시 인식된단어와 점수를 받아 textview를 업데이트 한 후 애니메이션 시작하기.
    public void StartScoreAnimation(AIScoreMultiScoringInfo scoreInfo) {
        //애니메이션 시작하기
        if (mLlMultiScoreResult != null && mLlMultiRecogResult != null) {
            //글자수 만큼 뷰의 시작위치를 조정. (일단 글자수x30 으로 설정)
            final int recogViewWidth = scoreInfo.GetRecogResult(0).length() * 30;//200;
            int x = scoreInfo.mX;
            int y = scoreInfo.mY;
            int w = scoreInfo.mW;
            int h = scoreInfo.mH;

            LOGGER.d("SallyRecog StartScoreAnimation() x, y, w, h : " + x + ", " + y + ", " + w + ", " + h);
            LOGGER.d("SallyRecog StartScoreAnimation() Scores 1 : " + scoreInfo.GetScore(0) +
                    ", 2 : " + scoreInfo.GetScore(1) + ", 3 : " + scoreInfo.GetScore(2) );
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mLlMultiRecogResult.getLayoutParams();
//                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(300, 135);
            params.topMargin = y; //정답영역의 y값
            params.leftMargin = x - recogViewWidth;
            mLlMultiRecogResult.setLayoutParams(params);

            FrameLayout.LayoutParams params2 = (FrameLayout.LayoutParams) mLlMultiScoreResult.getLayoutParams();
//                FrameLayout.LayoutParams params2 = new FrameLayout.LayoutParams(300, 135);
            params2.topMargin = y; //정답영역의 y값
            params2.leftMargin = x + w + 20;
            mLlMultiScoreResult.setLayoutParams(params2);
        }

        //인식 결과를 텍스트뷰에 표시
        mTvModel1.setText(scoreInfo.GetRecogResult(0));
        mTvModel2.setText(scoreInfo.GetRecogResult(1));
        mTvModel3.setText(scoreInfo.GetRecogResult(2));

        //채점 결과를 텍스트뷰에 표시
        mTvScore1.setText(scoreInfo.GetScore(0));
        mTvScore2.setText(scoreInfo.GetScore(1));
        mTvScore3.setText(scoreInfo.GetScore(2));

        //모델별 결과 표시 애니메이션
        mTvModel1.setVisibility(View.VISIBLE);
        mTvModel1.setAlpha(0.0f);
        mTvModel2.setVisibility(View.VISIBLE);
        mTvModel2.setAlpha(0.0f);
        mTvModel3.setVisibility(View.VISIBLE);
        mTvModel3.setAlpha(0.0f);

        mModelResultAnim1.start();
        mModelResultAnim2.start();
        mModelResultAnim3.start();

        //점수 표시 애니메이션 시작
        mTvScore1.setVisibility(View.VISIBLE);
        mTvScore1.setAlpha(0.0f);
        mTvScore2.setVisibility(View.VISIBLE);
        mTvScore2.setAlpha(0.0f);
        mTvScore3.setVisibility(View.VISIBLE);
        mTvScore3.setAlpha(0.0f);

        mScoreResultAnim1.start();
        mScoreResultAnim2.start();
        mScoreResultAnim3.start();

        //글씨 보정 메세지 출력
        mTvScoringMessage.setVisibility((View.VISIBLE));
        mTvScoringMessage.setAlpha(0.0f);
        mScoringMessageAnim.start();

        mIsScoreAnimRunning = true;
    }

    public void StopScoreAnimation() {
        //애니메이션 멈추기
        mIsScoreAnimRunning = false;
        mTvModel1.setVisibility(View.GONE);
        mTvModel2.setVisibility(View.GONE);
        mTvModel3.setVisibility(View.GONE);
        mLlMultiRecogResult.setVisibility(View.GONE);
        if(mModelResultAnim1.isRunning()) mModelResultAnim1.cancel();
        if(mModelResultAnim2.isRunning()) mModelResultAnim2.cancel();
        if(mModelResultAnim3.isRunning()) mModelResultAnim3.cancel();

        mTvScore1.setVisibility(View.GONE);
        mTvScore2.setVisibility(View.GONE);
        mTvScore3.setVisibility(View.GONE);
        mLlMultiScoreResult.setVisibility(View.GONE);
        if(mScoreResultAnim1.isRunning()) mScoreResultAnim1.cancel();
        if(mScoreResultAnim2.isRunning()) mScoreResultAnim2.cancel();
        if(mScoreResultAnim3.isRunning()) mScoreResultAnim3.cancel();

        mTvScoringMessage.setVisibility(View.GONE);
        if(mScoringMessageAnim.isRunning()) mScoringMessageAnim.cancel();
    }
    private void setScoreAnimation(View in_root) {
        // 스코어 애니메이션 : 복합적인 애니메이션에는 PropertyValueHolders를 사용한다.
        mLlMultiRecogResult = in_root.findViewById(R.id.llview_recog_result);
        mLlMultiScoreResult = in_root.findViewById(R.id.llview_score_result);

        mTvModel1 = in_root.findViewById(R.id.txtview_model_1);
        mTvModel2 = in_root.findViewById(R.id.txtview_model_2);
        mTvModel3 = in_root.findViewById(R.id.txtview_model_3);

        mTvScore1 = in_root.findViewById(R.id.txtview_score_1);
        mTvScore2 = in_root.findViewById(R.id.txtview_score_2);
        mTvScore3 = in_root.findViewById(R.id.txtview_score_3);

        mTvScoringMessage = in_root.findViewById(R.id.tv_message);

        //좌측에서 차례로 등장
        PropertyValuesHolder alphaHand = PropertyValuesHolder.ofFloat("alpha", 0.0f, 1.0f);
        //PropertyValuesHolder transHand = PropertyValuesHolder.ofFloat("translationX", 120.f);
        PropertyValuesHolder transHand = PropertyValuesHolder.ofFloat("translationX", 0.f);
        //우측에서 차례로 등장
        PropertyValuesHolder alphaHand2 = PropertyValuesHolder.ofFloat("alpha", 0.0f, 1.0f);
//        PropertyValuesHolder transHand2 = PropertyValuesHolder.ofFloat("translationX", -90.f);
        PropertyValuesHolder transHand2 = PropertyValuesHolder.ofFloat("translationX", 0.f);

        mModelResultAnim1 = ObjectAnimator.ofPropertyValuesHolder(mTvModel1, alphaHand, transHand);
        mModelResultAnim2 = ObjectAnimator.ofPropertyValuesHolder(mTvModel2, alphaHand, transHand);
        mModelResultAnim3 = ObjectAnimator.ofPropertyValuesHolder(mTvModel3, alphaHand, transHand);

        mScoreResultAnim1 = ObjectAnimator.ofPropertyValuesHolder(mTvScore1, alphaHand2, transHand2);
        mScoreResultAnim2 = ObjectAnimator.ofPropertyValuesHolder(mTvScore2, alphaHand2, transHand2);
        mScoreResultAnim3 = ObjectAnimator.ofPropertyValuesHolder(mTvScore3, alphaHand2, transHand2);

        mModelResultAnim1.setStartDelay(10);
        mModelResultAnim2.setStartDelay(1000);
        mModelResultAnim3.setStartDelay(2000);

        mScoreResultAnim1.setStartDelay(500);
        mScoreResultAnim2.setStartDelay(1500);
        mScoreResultAnim3.setStartDelay(2500);

        mModelResultAnim1.setDuration(1000); // duration (milliseconds)
        mModelResultAnim2.setDuration(1000); // duration (milliseconds)
        mModelResultAnim3.setDuration(1000); // duration (milliseconds)

        mScoreResultAnim1.setDuration(1000); // duration (milliseconds)
        mScoreResultAnim2.setDuration(1000); // duration (milliseconds)
        mScoreResultAnim3.setDuration(1000); // duration (milliseconds)

//        mModelResultAnim.setRepeatCount(ValueAnimator.INFINITE);
        //mModelResultAnim.setRepeatMode(ValueAnimator.RESTART);
        mModelResultAnim1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mLlMultiRecogResult.setVisibility(View.VISIBLE);
                mTvModel1.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
        mModelResultAnim2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mTvModel2.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
        mModelResultAnim3.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mTvModel3.setVisibility(View.VISIBLE);
                mLlMultiScoreResult.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mMainHandler.sendEmptyMessage(PLAY_SOUND_SCORING_MESSAGE);
            }
        });

        //아래에서 위로 등장
        PropertyValuesHolder alphaHand3 = PropertyValuesHolder.ofFloat("alpha", 0.0f, 1.0f);
        PropertyValuesHolder transHand3 = PropertyValuesHolder.ofFloat("translationY", 20.f);
        mScoringMessageAnim = ObjectAnimator.ofPropertyValuesHolder(mTvScoringMessage, alphaHand3, transHand3);
        mScoringMessageAnim.setStartDelay(3500);
        mScoringMessageAnim.setDuration(1000); // duration (milliseconds)
        mScoringMessageAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mTvScoringMessage.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
    }
}
