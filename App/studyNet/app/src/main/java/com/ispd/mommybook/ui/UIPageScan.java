package com.ispd.mommybook.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.R;

public class UIPageScan {
    public static int PAGE_SCAN_MODE = 1000; //페이지 스캔 모드로 사용할 때
    public static int SCORING_WAIT_MODE = 2000; //채점용 기다림 표시 모드로 사용할 때

    //스캐너뷰와 애니메이션
    private ImageView mImgViewScanbar1; //스캔바 이미지1
    private ImageView mImgViewScanbar2; //스캔바 이미지2
    private LinearLayout mScanView;
    private ObjectAnimator mScanbarAnim1; //스캔바1 애니메이션
    private ObjectAnimator mScanbarAnim2; //스캔바2 애니메이션

    boolean mIsScanRunning = false;


    public UIPageScan(View in_root, int mode) {
        setScanAnimation(in_root, mode);

    }

    private void setScanAnimation(View in_root, int mode) {
        final float FACTOR = 200.f;
        mScanView = in_root.findViewById(R.id.scanview);
        mImgViewScanbar1 = in_root.findViewById(R.id.imgview_scanbar1);
        mImgViewScanbar2 = in_root.findViewById(R.id.imgview_scanbar2);
        mScanbarAnim1 = ObjectAnimator.ofFloat(mImgViewScanbar1, "translationY", FACTOR);
        mScanbarAnim1.setDuration(1000); // duration 1 seconds
//        mScanbarAnim1.setRepeatCount(ValueAnimator.INFINITE);
        if(mode == PAGE_SCAN_MODE) {
        mScanbarAnim1.setRepeatCount(3);
        }
        else { //SCORING_WAIT_MODE
            mScanbarAnim1.setRepeatCount(1);
        }
        mScanbarAnim1.setRepeatMode(ValueAnimator.REVERSE);
        mScanbarAnim1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mScanView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(mIsScanRunning == true) {
                    // 애니메이션 재시작하기
                    mScanbarAnim1.start();
                    mScanbarAnim2.start();
                }
                else {
                mScanView.setVisibility(View.INVISIBLE);
                }
            }
        });

        mScanbarAnim2 = ObjectAnimator.ofFloat(mImgViewScanbar2, "translationY", -FACTOR);
        mScanbarAnim2.setDuration(1000); // duration 1 seconds
        if(mode == PAGE_SCAN_MODE) {
            mScanbarAnim2.setRepeatCount(3);
        }
        else { //SCORING_WAIT_MODE
            mScanbarAnim2.setRepeatCount(1);
        }
        mScanbarAnim2.setRepeatMode(ValueAnimator.REVERSE);
    }

    public void StartScanAnimation() {
        mIsScanRunning = true;
        mScanView.setVisibility(View.VISIBLE);
        mScanbarAnim1.start();
        mScanbarAnim2.start();
    }
    public void StopScanAnimation() {
        mIsScanRunning = false;
    }
}
