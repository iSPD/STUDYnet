/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.ispd.mommybook.motion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;

import java.util.LinkedList;
import java.util.List;

import static org.opencv.core.CvType.*;

/** A simple View providing a render callback to other classes. */
public class MotionHandTrackingView extends View {
  private static final UtilsLogger LOGGER = new UtilsLogger();

  private final List<DrawCallback> callbacks = new LinkedList<DrawCallback>();

  private Paint mFingerPaint;
  private Paint mFingerPaint2;
  private Paint mFingerTouchPaint;

  private float mUseFingerX[] = new float[21];
  private float mUseFingerY[] = new float[21];
  private float mUseFingerZ[] = new float[21];

  private float mUseFingerX2[] = new float[21];
  private float mUseFingerY2[] = new float[21];
  private float mUseFingerZ2[] = new float[21];

  private int mHandCount = 0;

  MotionHandTrackingDataManager mMotionHandTrackingDataManager = null;

  public MotionHandTrackingView(final Context context, final AttributeSet attrs) {
    super(context, attrs);

    LOGGER.d("MotionHandTrackingView");

    mFingerPaint = new Paint();
    mFingerPaint.setAlpha(50);
    mFingerPaint.setColor(Color.WHITE);
    mFingerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    //mFingerPaint.setStrokeWidth(15.f);

    mFingerPaint2 = new Paint();
    mFingerPaint2.setAlpha(50);
    mFingerPaint2.setColor(Color.WHITE);
    mFingerPaint2.setStyle(Paint.Style.FILL_AND_STROKE);
    mFingerPaint2.setStrokeWidth(15.f);

    mFingerTouchPaint = new Paint();
    mFingerTouchPaint.setAlpha(50);
    mFingerTouchPaint.setColor(Color.RED);
    mFingerTouchPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    //mFingerTouchPaint.setStrokeWidth(15.f);
  }

  public void addCallback(final DrawCallback callback) {
    callbacks.add(callback);
  }

  @Override
  public synchronized void draw(final Canvas canvas) {
    super.draw(canvas);  //added by sally

    for (final DrawCallback callback : callbacks) {
      callback.drawCallback(canvas);
    }

    LOGGER.d("MotionHandTrackingViewDraw");

    if( mMotionHandTrackingDataManager == null ) {
      invalidate();
      return;
    }

    mHandCount = mMotionHandTrackingDataManager.GetHandCount();

    //number1
    if( mHandCount > 0 ) {

      mMotionHandTrackingDataManager.GetFingerLocation(mUseFingerX, mUseFingerY, mUseFingerZ);

      int colorValue = 255;

      //colorValue = (int)(255.f * Math.abs(fingerZ[i]));
      colorValue = 255;
      mFingerPaint2.setColor(Color.argb(255, colorValue, colorValue, colorValue));

      canvas.drawLine(mUseFingerX[0], mUseFingerY[0], mUseFingerX[5], mUseFingerY[5], mFingerPaint2);
      canvas.drawLine(mUseFingerX[5], mUseFingerY[5], mUseFingerX[9], mUseFingerY[9], mFingerPaint2);
      canvas.drawLine(mUseFingerX[9], mUseFingerY[9], mUseFingerX[13], mUseFingerY[13], mFingerPaint2);
      canvas.drawLine(mUseFingerX[13], mUseFingerY[13], mUseFingerX[17], mUseFingerY[17], mFingerPaint2);
      canvas.drawLine(mUseFingerX[17], mUseFingerY[17], mUseFingerX[0], mUseFingerY[0], mFingerPaint2);

      for (int i = 0; i < 21; i++) {
        if (i != 4 && i != 8 && i != 12 && i != 16 && i != 20) {

          colorValue = (int) (255.f * Math.abs(mUseFingerZ[i]) * 5.0f);
          mFingerPaint2.setColor(Color.argb(255, colorValue, colorValue, colorValue));

          canvas.drawLine(mUseFingerX[i], mUseFingerY[i], mUseFingerX[i + 1], mUseFingerY[i + 1], mFingerPaint2);
        }

        colorValue = (int) (255.f * Math.abs(mUseFingerZ[i]) * 5.0f);
        mFingerPaint.setColor(Color.argb(255, colorValue, colorValue, colorValue));
        canvas.drawCircle(mUseFingerX[i], mUseFingerY[i], 20.f, mFingerPaint);
      }

      //draw click effect
//      float adjustY = 10.f * 1200.f / 240.f;//밑에 띄어주기
//
//      canvas.drawLine(202.f * 1600.f / 1760.f, 718.f * 1200.f / 1060.f - adjustY,
//              202.f * 1600.f / 1760.f, 790.f * 1200.f / 1060.f - adjustY, mFingerTouchPaint);
//      canvas.drawLine(202.f * 1600.f / 1760.f, 718.f * 1200.f / 1060.f - adjustY,
//              316.f * 1600.f / 1760.f, 718.f * 1200.f / 1060.f - adjustY, mFingerTouchPaint);
//      canvas.drawLine(202.f * 1600.f / 1760.f, 790.f * 1200.f / 1060.f - adjustY,
//              316.f * 1600.f / 1760.f, 790.f * 1200.f / 1060.f - adjustY, mFingerTouchPaint);
//      canvas.drawLine(316.f * 1600.f / 1760.f, 718.f * 1200.f / 1060.f - adjustY,
//              316.f * 1600.f / 1760.f, 790.f * 1200.f / 1060.f - adjustY, mFingerTouchPaint);

      if( mMotionHandTrackingDataManager.CheckTouchPressed() == true ) {
        mFingerTouchPaint.setColor(Color.BLUE);
      }
      else {
        mFingerTouchPaint.setColor(Color.RED);
      }

      canvas.drawCircle(mUseFingerX[8], mUseFingerY[8], 30.f, mFingerTouchPaint);
    }

    //number2
    if( mHandCount > 1 ) {

      mMotionHandTrackingDataManager.GetFingerLocation2(mUseFingerX2, mUseFingerY2, mUseFingerZ2);

      int colorValue = 255;

      //colorValue = (int)(255.f * Math.abs(fingerZ[i]));
      colorValue = 255;
      mFingerPaint2.setColor(Color.argb(255, colorValue, colorValue, colorValue));

      canvas.drawLine(mUseFingerX2[0], mUseFingerY2[0], mUseFingerX2[5], mUseFingerY2[5], mFingerPaint2);
      canvas.drawLine(mUseFingerX2[5], mUseFingerY2[5], mUseFingerX2[9], mUseFingerY2[9], mFingerPaint2);
      canvas.drawLine(mUseFingerX2[9], mUseFingerY2[9], mUseFingerX2[13], mUseFingerY2[13], mFingerPaint2);
      canvas.drawLine(mUseFingerX2[13], mUseFingerY2[13], mUseFingerX2[17], mUseFingerY2[17], mFingerPaint2);
      canvas.drawLine(mUseFingerX2[17], mUseFingerY2[17], mUseFingerX2[0], mUseFingerY2[0], mFingerPaint2);

      for (int i = 0; i < 21; i++) {
        if (i != 4 && i != 8 && i != 12 && i != 16 && i != 20) {

          colorValue = (int) (255.f * Math.abs(mUseFingerZ2[i]) * 5.0f);
          mFingerPaint2.setColor(Color.argb(255, colorValue, colorValue, colorValue));

          canvas.drawLine(mUseFingerX2[i], mUseFingerY2[i], mUseFingerX2[i + 1], mUseFingerY2[i + 1], mFingerPaint2);
        }

        colorValue = (int) (255.f * Math.abs(mUseFingerZ2[i]) * 5.0f);
        mFingerPaint.setColor(Color.argb(255, colorValue, colorValue, colorValue));
        canvas.drawCircle(mUseFingerX2[i], mUseFingerY2[i], 20.f, mFingerPaint);
      }

      if( mMotionHandTrackingDataManager.CheckTouchPressed2() == true ) {
        mFingerTouchPaint.setColor(Color.BLUE);
      }
      else {
        mFingerTouchPaint.setColor(Color.RED);
      }
      canvas.drawCircle(mUseFingerX2[8], mUseFingerY2[8], 30.f, mFingerTouchPaint);
    }

    invalidate();
  }

  public void SetDataManager(MotionHandTrackingDataManager dataManager) {
    mMotionHandTrackingDataManager = dataManager;
  }

  /** Interface defining the callback for client classes. */
  public interface DrawCallback {
    public void drawCallback(final Canvas canvas);
  }
}
