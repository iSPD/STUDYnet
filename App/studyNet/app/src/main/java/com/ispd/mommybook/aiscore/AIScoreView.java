package com.ispd.mommybook.aiscore;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.ispd.mommybook.R;
import com.ispd.mommybook.ui.UIScore;
import com.ispd.mommybook.utils.UtilsLogger;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * CommunicateView
 *
 * @author Daniel
 * @version 1.0
 */
public class AIScoreView extends AppCompatImageView {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Paint mPaint = null;
    private Bitmap mBitmap = null;
    List<Bitmap> mBitmapArray = new ArrayList<>();
    List<Rect> mRectArray = new ArrayList<>();

    public AIScoreView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

        LOGGER.d("SallyRecog onTouchEvent %f %f", x, y);

        invalidate();

        //return true;
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        LOGGER.d("onDraw : "+mBitmapArray.size());

        for(int i = 0; i < mBitmapArray.size(); i++) {
            Bitmap bitmap = mBitmapArray.get(i);

            Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            Rect dstRect = mRectArray.get(i);

            canvas.drawBitmap(bitmap, srcRect, dstRect, mPaint);
        }

        invalidate();
    }

    public void SetBitmap(String fileName, Rect rect) {
        mBitmap = BitmapFactory.decodeFile(fileName);
        mBitmapArray.add(mBitmap);
        mRectArray.add(rect);
    }

    public void ResetBitmap() {
        mBitmapArray.clear();
        mRectArray.clear();
    }
}

