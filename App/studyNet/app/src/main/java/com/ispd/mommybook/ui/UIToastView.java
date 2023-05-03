package com.ispd.mommybook.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ispd.mommybook.R;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Rect;

import pl.droidsonroids.gif.GifImageView;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class UIToastView {
    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Context mContext;

    private FrameLayout mUILayout = null;
    private LinearLayout mUIScoringToast = null;

    public UIToastView(View in_root) {
        mContext = in_root.getContext();
        mUILayout = ((Activity)mContext).findViewById(R.id.fl_uiview);
        mUIScoringToast = ((Activity)mContext).findViewById(R.id.ll_scoring_toast);
    }

    public void ShowToast(String message) {
        //LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        //ui_score_toast.xml 파일의 toast_design_root 속성을 로드
        //LinearLayout toastDesign = (LinearLayout)inflater.inflate(R.layout.ui_score_toast, ((Activity) mContext).findViewById(R.id.toast_design_root));
//        LinearLayout toastDesign = inflater.inflate(R.layout.ui_score_toast);

        TextView text = mUIScoringToast.findViewById(R.id.tv_scoring_toast);
//        text.setText(R.string.aiscore_scoring_guide);
        text.setText(message);

//        listview_list.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

//        LinearLayout.LayoutParams lparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        lparams.setMargins(400, 1000, 0, 0);
//
//        toastDesign.setLayoutParams(lparams);
//
//        mUILayout.addView(toastDesign);
//        mUIScoringToast.setEnabled(true);
        mUIScoringToast.setVisibility(VISIBLE);
    }
    public void HideToast() {
        mUIScoringToast.setVisibility(INVISIBLE);
    }
}
