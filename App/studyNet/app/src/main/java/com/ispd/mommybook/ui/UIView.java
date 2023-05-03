package com.ispd.mommybook.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ispd.mommybook.R;
import com.ispd.mommybook.utils.UtilsLogger;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

/**
 * CommunicateView
 *
 * @author Daniel
 * @version 1.0
 */
public class UIView extends AppCompatImageView implements View.OnLongClickListener {

    private static final UtilsLogger LOGGER = new UtilsLogger();
    private UIScore.touchEventCallback mtouchEventCallback;

    private Bitmap mBitmap_1, mBitmap_2, mBitmap_3, mBitmap_4;

    public UIView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        Resources r = context.getResources();
        mBitmap_1 = BitmapFactory.decodeResource(r, R.drawable.math72);
        mBitmap_2 = BitmapFactory.decodeResource(r, R.drawable.math80);
        mBitmap_3 = BitmapFactory.decodeResource(r, R.drawable.math78);
        mBitmap_4 = BitmapFactory.decodeResource(r, R.drawable.math75);
    }

    /**
     * Touch callback
     * 1. draw touch in view
     * 2. send touch data to remote
     */
    @Override
    public boolean onTouchEvent( MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        LOGGER.d("onTouchEvent %f %f", x, y);

        if(event.getAction() == MotionEvent.ACTION_DOWN){
            LOGGER.d("onTouchEvent(ACTION_DOWN) %f %f", x, y);
            mtouchEventCallback.onTouch(x, y);
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            LOGGER.d("onTouchEvent(ACTION_MOVE) %f %f", x, y);
        }else if(event.getAction()== MotionEvent.ACTION_UP){

        }
        invalidate();
        //return true;
        return false;
    }

    @Override
    public boolean onLongClick(View v) {

        LOGGER.d("onLongClick");

        mtouchEventCallback.removeAll();
        return true;
    }

    private static boolean gDraw_1 = false, gDraw_2 = false, gDraw_3 = false, gDraw_4 = false;
    public static void DrawCover(boolean mode1, boolean mode2, boolean mode3, boolean mode4) {
        gDraw_1 = mode1;
        gDraw_2 = mode2;
        gDraw_3 = mode3;
        gDraw_4 = mode4;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float fixY = 5.f;

        if( gDraw_1 == true ) {
            Rect dst1 = new Rect((int) (124.f * 1600.f / 1280.f), (int) ((283.f - fixY) * 1200.f / 960.f), (int) (281.f * 1600.f / 1280.f), (int) ((367.f - fixY) * 1200.f / 960.f));
            canvas.drawBitmap(mBitmap_1, null, dst1, null);
        }

        if( gDraw_2 == true ) {
            Rect dst2 = new Rect((int) (358.f * 1600.f / 1280.f), (int) ((293.f - fixY) * 1200.f / 960.f), (int) (501.f * 1600.f / 1280.f), (int) ((366.f - fixY) * 1200.f / 960.f));
            canvas.drawBitmap(mBitmap_2, null, dst2, null);
        }

        float fixY2 = 10.f;

        if( gDraw_3 == true ) {
            Rect dst3 = new Rect((int) (130.f * 1600.f / 1280.f), (int) ((497.f - fixY2) * 1200.f / 960.f), (int) (281.f * 1600.f / 1280.f), (int) ((577.f - fixY2) * 1200.f / 960.f));
            canvas.drawBitmap(mBitmap_3, null, dst3, null);
        }

        if( gDraw_4 == true ) {
            Rect dst4 = new Rect((int) (359.f * 1600.f / 1280.f), (int) ((491.f - fixY2) * 1200.f / 960.f), (int) (503.f * 1600.f / 1280.f), (int) ((576.f - fixY2) * 1200.f / 960.f));
            canvas.drawBitmap(mBitmap_4, null, dst4, null);
        }

        invalidate();
    }

    public void SetCallbackListener(UIScore.touchEventCallback callback) {
        mtouchEventCallback = callback;
    }
}

