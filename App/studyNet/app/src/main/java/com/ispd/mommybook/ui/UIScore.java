package com.ispd.mommybook.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ispd.mommybook.R;
import com.ispd.mommybook.communicate.CommunicateView;
import com.ispd.mommybook.utils.UtilsLogger;
import com.ispd.mommybook.utils.UtilsPlaySound;

import org.opencv.core.Rect;

import pl.droidsonroids.gif.GifImageView;

public class UIScore {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Context mContext;

    private FrameLayout mUILayout = null;
    private GifImageView mScoregif = null;

    private int mScoreMarkMode = 0;
    private UtilsPlaySound mUtilsPlaySound = null;

    public UIScore(View in_root) {

        mContext = in_root.getContext();

        mUILayout = ((Activity)mContext).findViewById(R.id.fl_uiview);
        mScoregif = new GifImageView(mContext);
    }

    /**
     * 지정하는 위치에 O 채점 표시를 그림
     * @param rect
     */
    public void markScore(Rect rect) {
        //sally-v2 TODO : 나중엔 rect영역의 움직임(필기동작, 터치동작)이 끝난 직후 채점되도록 해야함.
        mScoregif = new GifImageView(mContext);

        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(rect.width/* layout_width */, rect.height/* layout_height */);
        lparams.setMargins(rect.x - rect.width/2, rect.y - rect.height/2, 0, 0);
        mScoregif.setLayoutParams(lparams);
        mScoregif.setImageResource(R.drawable.score_mark);
        mScoregif.setScaleType(ImageView.ScaleType.FIT_XY);

        mUILayout.addView(mScoregif);
//        mFLScoreView.addView(scoregif);

//        boolean isRunning[] = {false};
//        mUtilsPlaySound = new UtilsPlaySound("correct", isRunning);
    }

    /**
     * 지정하는 위치에 X 자 채점 표시를 그림
     * @param rect
     */
    public void markIncorrectScore(Rect rect) {
        //sally-v2 TODO : 나중엔 rect영역의 움직임(필기동작, 터치동작)이 끝난 직후 채점되도록 해야함.
        mScoregif = new GifImageView(mContext);

        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(rect.width/2/* layout_width */, rect.height/2/* layout_height */);
        lparams.setMargins(rect.x - rect.width/4, rect.y - rect.height/4 , 0, 0);
        mScoregif.setLayoutParams(lparams);
        mScoregif.setImageResource(R.drawable.score_mark_x);
 //       mScoregif.setScaleType(ImageView.ScaleType.FIT_XY);

        mUILayout.addView(mScoregif);
//        mFLScoreView.addView(scoregif);

//        boolean isRunning[] = {false};
//        mUtilsPlaySound = new UtilsPlaySound("incorrect", isRunning);
    }

    /**
     * 지정하는 위치에 세모 표시를 그림
     * @param rect
     */
    public void markNotScored(Rect rect) {
        mScoregif = new GifImageView(mContext);

        //"세모+선생님채점" 일때 아래 코드 사용
        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(rect.width/* layout_width */, rect.height/* layout_height */);
        lparams.setMargins(rect.x - rect.width/2, rect.y - rect.height/2, 0, 0);

        //세모만 그릴 때 아래 코드 사용
//        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(rect.width / 2/* layout_width */, rect.height / 2/* layout_height */);
//        lparams.setMargins(rect.x - rect.width / 4, rect.y - rect.height / 4, 0, 0);

        mScoregif.setLayoutParams(lparams);

//        mScoregif.setImageResource(R.drawable.score_mark_triangle); //세모
        mScoregif.setImageResource(R.drawable.teacher_grading); //"세모+선생님채점"

        mUILayout.addView(mScoregif);
    }

    /**
     * 채점 표시를 모두 지움
     */
    public void RemoveScoreMark() {
        mUILayout.removeAllViews();
//        mFLScoreView.removeAllViews();
    }

    public void SetScoreMarkMode(int mode) {
        mScoreMarkMode = mode;
    }

    public touchEventCallback GetTouchCallback()
    {
        return mtouchEventCallback;
    }

    private touchEventCallback mtouchEventCallback
            = new touchEventCallback()
    {
        @Override
        public void onTouch(float x, float y) {
            // sally :  이 기능 사용 안함.
//            Rect rect = new Rect((int)x, (int)y, 300, 300);
//            if( mScoreMarkMode == 0 ) {
//                markScore(rect);
//            }
//            else {
//                markIncorrectScore(rect);
//            }
        }

        @Override
        public void removeAll() {
            RemoveScoreMark();
        }
    };

    public interface touchEventCallback {
        void onTouch(float x, float y);
        void removeAll();
    }
}
