package com.ispd.mommybook.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.ImageView;

import com.ispd.mommybook.R;
import com.ispd.mommybook.utils.UtilsLogger;
import com.ispd.mommybook.utils.UtilsPlaySound;

import static com.ispd.mommybook.ui.UIManager.GUIDE_SOUND_ON;

public class UIInnerGuide {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    //내지가이드뷰와 애니메이션
    private ImageView mImgViewInnerGuideT; //내지 가이드 T자 이미지
    private ImageView mImgViewInnerGuideHand; //내지 가이드 책속 이미지
    private ImageView mImgViewInnerGuideSmallT; //내지 가이드 작은 T자 이미지
    private ImageView mImgViewInnerGuideBalloon; //내지 가이드 작은 T자 이미지
    private Button mBtnInnerGuideAniStart; //내지 가이드 애니메이션 시작 버튼 (임시)
    private ObjectAnimator mInnerGuideAnim1; //내지 가이드 T자 애니메이션
    private ObjectAnimator mInnerGuideAnim2; //내지 가이드 책손 애니메이션
    private ObjectAnimator mInnerGuideAnim3; //내지 가이드 작은T자 애니메이션
    private ObjectAnimator mInnerGuideAnim4; //내지 가이드 작은T자 애니메이션2
    private ObjectAnimator mInnerGuideAnim5; //내지 가이드 말풍선2 애니메이션

    public UIInnerGuide(View in_root){
        setInnerGuideAnimation(in_root);
        mUtilsPlaySound1 = new UtilsPlaySound();
        mUtilsPlaySound2 = new UtilsPlaySound();
    }

    private boolean mIsInnerGuideAnimRunning = false;
    private UtilsPlaySound mUtilsPlaySound1 = null;
    private UtilsPlaySound mUtilsPlaySound2 = null;

    public void StartInnerGuideAnimation(boolean start, int soundFlag) {
        if(start == true) { //내지 가이드 애니메이션 시작하기
            if( mIsInnerGuideAnimRunning == false ) {
                LOGGER.d("StartInnerGuideAnimation On");

                mImgViewInnerGuideT.setVisibility(View.VISIBLE);
                mImgViewInnerGuideT.setAlpha(0.0f);

                mInnerGuideAnim5.start();
                mInnerGuideAnim1.start();
                mInnerGuideAnim2.start();
//            innerGuideAnimSet.start();

                mIsInnerGuideAnimRunning = true;

                boolean isRunning[] = {false};
                if(mUtilsPlaySound1.isSoundPlaying() == false) {
                    mUtilsPlaySound1.DoPlaySound("green", isRunning);
                }
            }
        }
        else { //내지 가이드 애니메이션 멈추기
            if( mIsInnerGuideAnimRunning == true ) {
                LOGGER.d("StartInnerGuideAnimation Off");

                mIsInnerGuideAnimRunning = false;
                if (mInnerGuideAnim1.isRunning()) mInnerGuideAnim1.cancel();
                if (mInnerGuideAnim2.isRunning()) mInnerGuideAnim2.cancel();
                if (mInnerGuideAnim3.isRunning()) mInnerGuideAnim3.cancel();
                if (mInnerGuideAnim4.isRunning()) mInnerGuideAnim4.cancel();
                if (mInnerGuideAnim5.isRunning()) mInnerGuideAnim5.cancel();

                mImgViewInnerGuideT.setVisibility(View.GONE);
                mImgViewInnerGuideHand.setVisibility(View.GONE);
                mImgViewInnerGuideSmallT.setVisibility(View.GONE);
                mImgViewInnerGuideBalloon.setVisibility(View.GONE);

                boolean isRunning[] = {false};
                mUtilsPlaySound1.stopSound();
                if(mUtilsPlaySound2.isSoundPlaying() == false) {
                    if(soundFlag == GUIDE_SOUND_ON) {
                    mUtilsPlaySound2.DoPlaySound("good", isRunning);
                    }
                }
            }
        }
    }

    private void setInnerGuideAnimation(View in_root) {

        //sally-v2 내지가이드 애니메이션 : 복합적인 애니메이션에는 PropertyValueHolders를 사용한다.---->
        mImgViewInnerGuideT = in_root.findViewById(R.id.imgview_innerguide_t);
        mImgViewInnerGuideHand = in_root.findViewById(R.id.imgview_innerguide_hand);
        mImgViewInnerGuideSmallT = in_root.findViewById(R.id.imgview_innerguide_small_t);
        mImgViewInnerGuideBalloon = in_root.findViewById(R.id.imgview_innerguide_balloon);

        //가이드 배경은 1 초동안 나타남
        mInnerGuideAnim1 =  ObjectAnimator.ofFloat(mImgViewInnerGuideT, "alpha", 1.0f);
        mInnerGuideAnim1.setDuration(1000);
//        mInnerGuideAnim1.setRepeatCount(1);

        //책손 이미지는 2초씩 반복해서 위로 올라옴
        PropertyValuesHolder alphaHand = PropertyValuesHolder.ofFloat("alpha", 0.2f, 0.8f);
        PropertyValuesHolder transHand = PropertyValuesHolder.ofFloat("translationY", 360.f, 30.f);

        mInnerGuideAnim2 = ObjectAnimator.ofPropertyValuesHolder(mImgViewInnerGuideHand, alphaHand, transHand);

        mInnerGuideAnim2.setDuration(2000); // duration 1 seconds
        mInnerGuideAnim2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mImgViewInnerGuideHand.setVisibility(View.VISIBLE);
                mImgViewInnerGuideBalloon.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(mIsInnerGuideAnimRunning == true) {
                    mInnerGuideAnim3.start();
                }
            }
        });

        //책손 이미지가 올라온 후 작은T자 이미지가 나타나는 애니메이션
        mInnerGuideAnim3 =  ObjectAnimator.ofFloat(mImgViewInnerGuideSmallT, "alpha", 0.0f, 0.6f);
        mInnerGuideAnim3.setDuration(1000);
        mInnerGuideAnim3.setInterpolator(new AccelerateInterpolator());
        mInnerGuideAnim3.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mImgViewInnerGuideSmallT.setVisibility(View.VISIBLE);

            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(mIsInnerGuideAnimRunning == true) {
                    mInnerGuideAnim4.start();
                }
            }
        });

        //작은T자 이미지가 깜빡거리는 애니메이션
        mInnerGuideAnim4 =  ObjectAnimator.ofFloat(mImgViewInnerGuideSmallT, "alpha", 0.0f, 0.6f);
        mInnerGuideAnim4.setDuration(300);
        mInnerGuideAnim4.setRepeatCount(3);
//        mInnerGuideAnim4.setStartDelay(2000);
        mInnerGuideAnim4.setRepeatMode(ValueAnimator.RESTART);
        mInnerGuideAnim4.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
//                mImgViewInnerGuideSmallT.setVisibility(View.VISIBLE);

            }
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(mIsInnerGuideAnimRunning == true) {
//                mImgViewInnerGuideT.setVisibility(View.GONE);
                    mImgViewInnerGuideHand.setVisibility(View.GONE);
                    mImgViewInnerGuideSmallT.setVisibility(View.GONE);
                    mInnerGuideAnim2.start();
                }
            }
        });

        //말풍선 나타나는 애니메이션
        PropertyValuesHolder scalex = PropertyValuesHolder.ofFloat("scaleX", 0.2f, 1.0f);
        PropertyValuesHolder scaley = PropertyValuesHolder.ofFloat("scaleY", 0.2f, 1.0f);
        mInnerGuideAnim5 = ObjectAnimator.ofPropertyValuesHolder(mImgViewInnerGuideBalloon, scalex, scaley);
        mInnerGuideAnim5.setDuration(1000);
        mInnerGuideAnim5.setInterpolator(new BounceInterpolator());


        //애니메이션 시작을 위한 버튼 정의
//        mBtnInnerGuideAniStart = findViewById(R.id.btn_innerguideani);
//        mBtnInnerGuideAniStart.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //innerGuideAnimation();
//                //내지가 잘 놓여졌는지 여부를 셋팅(임시코드)
//                mInnerAdjustDone ^= true;
//            }
//        });
    }

    public boolean IsInnerGuideRunning() {
        if(mInnerGuideAnim3.isRunning() == true) {
            return true;
        }
        else
            return false;
    }
}
