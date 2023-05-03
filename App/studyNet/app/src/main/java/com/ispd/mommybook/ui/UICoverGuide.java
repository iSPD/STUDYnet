package com.ispd.mommybook.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.ImageView;

import com.ispd.mommybook.R;
import com.ispd.mommybook.utils.UtilsLogger;
import com.ispd.mommybook.utils.UtilsPlaySound;

public class UICoverGuide {
    private static final UtilsLogger LOGGER = new UtilsLogger();

    //표지가이드뷰와 애니메이션
    private ImageView mImgViewCoverGuideHand; //표지 가이드 손 이미지
    private ImageView mImgViewCoverGuideBg; //표지 가이드 배경 이미지
    private ImageView mImgViewCoverGuideBalloon; //표지 가이드 배경 이미지
    private Button mBtnCoverGuideAniStart; //표지 가이드 애니메이션 시작 버튼 (임시)
    private ObjectAnimator mCoverGuideAnim1; //표지 가이드 배경 애니메이션
    private ObjectAnimator mCoverGuideAnim2; //표지 가이드 손 애니메이션
    private ObjectAnimator mCoverGuideAnim3; //표지 가이드 말풍선1 애니메이션

    public boolean mIsCoverGuideAnimRunning = false;
    private UtilsPlaySound mUtilsPlaySound1 = null;
    private UtilsPlaySound mUtilsPlaySound2 = null;

    public UICoverGuide(View in_root){
        setCoverGuideAnimation(in_root);
        mUtilsPlaySound1 = new UtilsPlaySound();
        mUtilsPlaySound2 = new UtilsPlaySound();
    }

    public void StartCoverGuideAnimation(boolean start) {
        if(start == true) { //표지 가이드 애니메이션 시작하기
            if( mIsCoverGuideAnimRunning == false ) {
                mImgViewCoverGuideBg.setVisibility(View.VISIBLE);
                mImgViewCoverGuideBg.setAlpha(0.0f);
                mImgViewCoverGuideBalloon.setVisibility(View.VISIBLE);

                mCoverGuideAnim3.start(); //말풍선
                mCoverGuideAnim1.start();
                mCoverGuideAnim2.start();

                mIsCoverGuideAnimRunning = true;

                boolean isRunning[] = {false};
                if(mUtilsPlaySound1.isSoundPlaying() == false) {
                    mUtilsPlaySound1.DoPlaySound("cover_guide", isRunning);
                }
            }
        }
        else { //표지 가이드 애니메이션 멈추기
            if( mIsCoverGuideAnimRunning == true ) {
                mIsCoverGuideAnimRunning = false;
                mImgViewCoverGuideBg.setVisibility(View.GONE);
                mImgViewCoverGuideHand.setVisibility(View.GONE);
                mImgViewCoverGuideBalloon.setVisibility(View.GONE);
                if (mCoverGuideAnim1.isRunning()) mCoverGuideAnim1.cancel();
                if (mCoverGuideAnim2.isRunning()) mCoverGuideAnim2.cancel();
                if (mCoverGuideAnim3.isRunning()) mCoverGuideAnim3.cancel();

                boolean isRunning[] = {false};
                mUtilsPlaySound1.stopSound();
                if(mUtilsPlaySound2.isSoundPlaying() == false) { 
                    mUtilsPlaySound2.DoPlaySound("good", isRunning);
                }
            }
        }
    }

    private void setCoverGuideAnimation(View in_root) {
        //sally-v2 표지가이드 애니메이션 : 복합적인 애니메이션에는 PropertyValueHolders를 사용한다.---->
        mImgViewCoverGuideBg = in_root.findViewById(R.id.imgview_cover_guide_bg);
        mImgViewCoverGuideHand = in_root.findViewById(R.id.imgview_coverguide);
        mImgViewCoverGuideBalloon = in_root.findViewById(R.id.imgview_coverguide_balloon);

        //가이드 배경은 1.5초동안 서서히 나타남
        mCoverGuideAnim1 = ObjectAnimator.ofFloat(mImgViewCoverGuideBg, "alpha", 1.0f);
        mCoverGuideAnim1.setDuration(1500);

        //책손 이미지는 2초씩 반복해서 위로 올라옴
        PropertyValuesHolder alphaHand = PropertyValuesHolder.ofFloat("alpha", 0.0f, 1.0f);
        PropertyValuesHolder transHand = PropertyValuesHolder.ofFloat("translationY", -300.f);

        mCoverGuideAnim2 = ObjectAnimator.ofPropertyValuesHolder(mImgViewCoverGuideHand, alphaHand, transHand);

        mCoverGuideAnim2.setDuration(2000); // duration 1 seconds
        mCoverGuideAnim2.setRepeatCount(ValueAnimator.INFINITE);
        mCoverGuideAnim2.setRepeatMode(ValueAnimator.RESTART);
        mCoverGuideAnim2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mImgViewCoverGuideHand.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mImgViewCoverGuideBg.setVisibility(View.GONE);
                mImgViewCoverGuideHand.setVisibility(View.GONE);
                mImgViewCoverGuideBalloon.setVisibility(View.GONE);
            }
        });

        //말풍선 나타나는 애니메이션
        PropertyValuesHolder scalex = PropertyValuesHolder.ofFloat("scaleX", 0.2f, 1.0f);
        PropertyValuesHolder scaley = PropertyValuesHolder.ofFloat("scaleY", 0.2f, 1.0f);
        mCoverGuideAnim3 = ObjectAnimator.ofPropertyValuesHolder(mImgViewCoverGuideBalloon, scalex, scaley);
        mCoverGuideAnim3.setDuration(1000);
//        mInnerGuideAnim5.setRepeatCount(3);
//        mInnerGuideAnim5.setStartDelay(2000);
//        mInnerGuideAnim5.setRepeatMode(ValueAnimator.RESTART);
//        mInnerGuideAnim5.setInterpolator(new DecelerateInterpolator());
        mCoverGuideAnim3.setInterpolator(new BounceInterpolator());

//        //애니메이션 시작을 위한 버튼 정의
//        mBtnCoverGuideAniStart = in_root.findViewById(R.id.btn_coverguideani);
//        mBtnCoverGuideAniStart.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //coverGuideAnimation();
//                //커버가 잘 놓여졌는지 여부를 셋팅(임시코드)
//                mCoverAdjustDone ^= true;
//
//            }
//        });
    }

    public boolean IsCoverGuideRunning() {
        if(mCoverGuideAnim3.isRunning() == true) {
            return true;
        }
        else
            return false;
    }
}
