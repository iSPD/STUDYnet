package com.ispd.mommybook.communicate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.ispd.mommybook.R;

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
public class CommunicateView extends AppCompatImageView {

    private static final int STATE_STILL=0;
    private static final int STATE_MOVING=1;
    private static int DEFAULT_COLOR;

    private int state=0;
    private ArrayList<Paint> paintPenList =new ArrayList<>();
    private Path latestPath;
    private Paint latestPaint;
    private ArrayList<Path> pathPenList =new ArrayList<>();
    private LocalCoordinateCallback mLocalCoordinateCallback;
    private int lineWidth =15;
    private int currentColor;

    /**
     * Init CommunicateView
     *
     */
    public CommunicateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Init CommunicateView
     *
     */
    private void init() {

        DEFAULT_COLOR= ContextCompat.getColor(getContext(), R.color.colorRed);
        currentColor=DEFAULT_COLOR;

        initPaintNPen(currentColor);

    }

    /**
     * Init CommunicateView
     *
     */
    private void initPaintNPen(int color){

        latestPaint=getNewPaintPen(color);
        latestPath=getNewPathPen();

        paintPenList.add(latestPaint);
        pathPenList.add(latestPath);

    }

    /**
     * Init CommunicateView
     *
     */
    private Path getNewPathPen() {
        Path path=new Path();
        return path;
    }

    /**
     * Init CommunicateView
     *
     * @param color
     */
    private Paint getNewPaintPen(int color){

        Paint mPaintPen =new Paint();

        mPaintPen.setStrokeWidth(lineWidth);
        mPaintPen.setAntiAlias(true);
        mPaintPen.setDither(true);
        mPaintPen.setStyle(Paint.Style.STROKE);
        mPaintPen.setStrokeJoin(Paint.Join.MITER);
        mPaintPen.setStrokeCap(Paint.Cap.ROUND);
        mPaintPen.setColor(color);

        return mPaintPen;

    }

    /**
     * Get Touch datas from remote teacher
     *
     * @param info touch info using string
     */
    public void setTouchInfo(String info)
    {
        Log.d("setTouchInfo", "setTouchInfo : "+info);

        String splitDatas[] = info.split(",");
        String getEvent = splitDatas[0];
        float eventX = Float.valueOf(splitDatas[1]);
        float eventY = Float.valueOf(splitDatas[2]);

        Log.d("setTouchInfo", "setTouchInfo : "+getEvent+", X : "+eventX+", Y : "+eventY);

        if( getEvent.equals("start") == true )
        {
            startPath(eventX, eventY);
        }
        else if( getEvent.equals("moving") == true )
        {
            updatePath(eventX, eventY);
        }
        else if( getEvent.equals("end") == true )
        {
            endPath(eventX, eventY);
        }
        invalidate();
    }

    /**
     * callback for
     * sending Touch datas to remote teacher
     *
     * @param  callback
     */
    public void setTouchCallback(LocalCoordinateCallback callback) {
        this.mLocalCoordinateCallback=callback;
    }

    /**
     * Touch callback
     * 1. draw touch in view
     * 2. send touch data to remote
     */
    @Override
    public boolean onTouchEvent( MotionEvent event) {
        float x=event.getX();
        float y=event.getY();
        Log.i("CO-ordinate",event.getX()+" : "+event.getY());

        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if( mLocalCoordinateCallback != null ) {
                mLocalCoordinateCallback.start(x, y);
            }

            startPath(x,y);

        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            if( mLocalCoordinateCallback != null ) {
                mLocalCoordinateCallback.moving(x, y);
            }

            updatePath(x,y);

        }else if(event.getAction()== MotionEvent.ACTION_UP){
            if( mLocalCoordinateCallback != null ) {
                mLocalCoordinateCallback.end(x, y);
            }

            endPath(x,y);

        }
        invalidate();
        return true;
    }

    /**
     * startPath
     *
     */
    private void startPath(float x, float y) {
        /*if(state==STATE_MOVING)
            mPath.lineTo(x,y);
        else
            mPath.moveTo(x,y);*/
        initPaintNPen(currentColor);
        latestPath.moveTo(x,y);
    }

    /**
     * updatePath
     *
     */
    private void updatePath(float x, float y) {
        state=STATE_MOVING;

        latestPath.lineTo(x,y);
    }

    /**
     * endPath
     *
     */
    private void endPath(float x, float y) {

    }

    /**
     * setDrawColor
     *
     */
    public void setDrawColor(int color) {

        currentColor=color;

    }

    /**
     * onDraw
     *
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for(int i=0;i<paintPenList.size();i++){
            canvas.drawPath(pathPenList.get(i),paintPenList.get(i));
        }
    }

    /**
     * increaseWidth
     *
     */
    public void increaseWidth(boolean decrease){

        if(decrease){
            if(lineWidth >5) {
                lineWidth = lineWidth - 10;
            }
        }else{
            if(lineWidth <50) {
                lineWidth = lineWidth + 10;
            }
        }

        invalidate();
    }

    /**
     * resetView
     *
     */
    public void resetView() {
        currentColor=DEFAULT_COLOR;
        state=STATE_STILL;

        latestPath.reset();
        latestPaint.reset();

        pathPenList.clear();
        paintPenList.clear();
        lineWidth = 20;

        initPaintNPen(currentColor);

        invalidate();
    }

    /**
     * undoPath
     *
     */
    public void UndoPath() {

        if(paintPenList.size()>1) {
            latestPaint = paintPenList.get(paintPenList.size() - 2);
            latestPath = pathPenList.get(pathPenList.size() - 2);

            paintPenList.remove(paintPenList.size() - 1);
            pathPenList.remove(pathPenList.size() - 1);

            currentColor=latestPaint.getColor();
            lineWidth= (int) latestPaint.getStrokeWidth();
        }else{
            resetView();
        }

        invalidate();
    }

    /**
     * LocalCoordinateCallback
     *
     */
    public interface LocalCoordinateCallback {
        void moving(float x, float y);
        void start(float x, float y);
        void end(float x, float y);
    }

    /**
     * RemoteCoordinateCallback
     *
     */
    public interface RemoteCoordinateCallback {
        void drawTouchInfo(String msg);
    }
}

