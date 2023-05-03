package com.ispd.mommybook.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;

import com.ispd.mommybook.R;
import com.ispd.mommybook.aiscore.AIScoreMultiScoringInfo;
import com.ispd.mommybook.utils.UtilsPlaySound;

import org.opencv.android.Utils;
import org.opencv.core.Rect;

public class UIManager {
    public static final int GUIDE_SOUND_ON = 1000;
    public static final int GUIDE_SOUND_OFF = 2000;

    private UIPageScan mUIPageScan = null;
    private UICoverGuide mUICoverGuide = null;
    private UIInnerGuide mUIInnerGuide = null;
    private UIScore mUIScore = null;
    private UIView mUIView = null;
    private UIMultiModelScoring mUIMultiModelScoring = null;
    private UIToastView mUIToastView = null;

    private Context mContext = null;
    private Handler mMainHandler = null;

    public UIManager(View root, Handler handler) {
        mContext = root.getContext();
        mMainHandler = handler;

        mUIPageScan = new UIPageScan(root, UIPageScan.SCORING_WAIT_MODE);
        mUICoverGuide = new UICoverGuide(root);
        mUIInnerGuide = new UIInnerGuide(root);
        mUIScore = new UIScore(root);
        mUIMultiModelScoring = new UIMultiModelScoring(root, handler);
        mUIToastView = new UIToastView(root);

        mUIView = ((Activity)root.getContext()).findViewById(R.id.cv_uiview);
        mUIView.SetCallbackListener(mUIScore.GetTouchCallback());
    }

    public void DrawScoreMarkCircle(Rect rect) {
        mUIScore.markScore(rect);
    }

    public void DrawScoreMarkX(Rect rect) {
        mUIScore.markIncorrectScore(rect);
    }
	
    public void DrawScoredMarkTriangle(Rect rect) {
        mUIScore.markNotScored(rect);
    }

    public void SetScoreMarkMode(int mode) {
        mUIScore.SetScoreMarkMode(mode);
    }

    public void RemoveScoreMark() {
        mUIScore.RemoveScoreMark();
    }

    public void StartPageScanAnimation() {
        mUIPageScan.StartScanAnimation();
    }
    public void StopPageScanAnimation() {
        mUIPageScan.StopScanAnimation();
    }

    public void StartCoverGuideAnimation() {
        mUICoverGuide.StartCoverGuideAnimation(true);
    }

    public void StopCoverGuideAnimation() {
        mUICoverGuide.StartCoverGuideAnimation(false);
    }

    public boolean IsCoverGuideAnimationRunning() {
        return mUICoverGuide.IsCoverGuideRunning();
    }

    public void StartInnerGuideAnimation() {
        mUIInnerGuide.StartInnerGuideAnimation(true, GUIDE_SOUND_ON);
    }

    public void StopInnerGuideAnimation(int soundFlag) {
        mUIInnerGuide.StartInnerGuideAnimation(false, soundFlag);
    }

    public boolean IsInnerGuideAnimationRunning() {
        return mUIInnerGuide.IsInnerGuideRunning();
    }

    public void StartMultiModelScoringAnimation(AIScoreMultiScoringInfo scoreInfo) {
        mUIMultiModelScoring.StartScoreAnimation(scoreInfo);
    }
    public void StopMultiModelScoringAnimation() {
        mUIMultiModelScoring.StopScoreAnimation();
    }
    public void ShowScoringToast() {
        mUIToastView.ShowToast(mContext.getResources().getString(R.string.aiscore_scoring_guide));
    }
    public void HideScoringToast() {
        mUIToastView.HideToast();
    }
}
