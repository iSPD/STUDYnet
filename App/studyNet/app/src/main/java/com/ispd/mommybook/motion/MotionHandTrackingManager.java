package com.ispd.mommybook.motion;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;

import com.ispd.mommybook.R;

/**
 * MotionHandTrackingManager
 *
 * @author Daniel
 * @version 1.0
 */
public class MotionHandTrackingManager {

    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;

    private int mLCDWidth;
    private int mLCDHeight;

    private MotionHandTrackingImpl mMotionHandTrackingImpl = null;
    private MotionHandTrackingDataManager mMotionHandTrackingDataManager = null;
    private MotionHandTrackingView mMotionHandTrackingView = null;

    /**
     * Init MotionHandTrackingManager
     *
     * @param context
     * @param previewWidth
     * @param previewHeight
     * @param lcdWidth
     * @param lcdHeight
     * @param textureID camera preview texture id from preview renderer
     * @param eglContext shared egl context from preview renderer
     */
    public MotionHandTrackingManager(Context context,
                                     int previewWidth, int previewHeight,
                                     int lcdWidth, int lcdHeight,
                                     int textureID, EGLContext eglContext) {

        mCameraPreviewWidth = previewWidth;
        mCameraPreviewHeight = previewHeight;

        mLCDWidth = lcdWidth;
        mLCDHeight = lcdHeight;

        mMotionHandTrackingDataManager = new MotionHandTrackingDataManager(
                mCameraPreviewWidth,
                mCameraPreviewHeight,
                mLCDWidth,
                mLCDHeight);

        mMotionHandTrackingImpl = new MotionHandTrackingImpl(
                                    context,
                                    textureID,
                                    eglContext,
                                    mMotionHandTrackingDataManager.GetHandLandMarkCallback(),
                                    mMotionHandTrackingDataManager.GetHandCallback());

        mMotionHandTrackingView = ((Activity)context).findViewById(R.id.cv_motion_handtracking);
        mMotionHandTrackingView.SetDataManager(mMotionHandTrackingDataManager);
    }

    public EGLContext GetEGLContext()
    {
        return mMotionHandTrackingImpl.GetEGLContext();
    }

    //local
    public int GetCameraTextureID()
    {
        return mMotionHandTrackingImpl.GetCameraTextureID();
    }

    public SurfaceTexture GetSurfaceTexture()
    {
        return mMotionHandTrackingImpl.GetSurfaceTexture();
    }

    /**
     * Get Preview texture listener
     * In preview renderer, call this
     *
     */
    public MotionHandTrackingImpl.PreviewTextureListener GetPreviewTextureListener() {
        return mMotionHandTrackingImpl.GetPreviewTextureListener();
    }

    /**
     * Set Finger Point listener
     * In mainActivity, get this
     *
     */
    public void SetFingerInfoListener(MotionHandTrackingDataManager.FingerInfoListener mainListener) {
        mMotionHandTrackingDataManager.setFingerInfoListener(mainListener);
    }

    public boolean GetTouchPressed(float areaX, float areaY, float areaX2, float areaY2) {
        return mMotionHandTrackingDataManager.CheckAreaTouched(areaX, areaY, areaX2, areaY2);
    }

    public boolean GetTouchPressed2(float areaX, float areaY, float areaX2, float areaY2) {
        return mMotionHandTrackingDataManager.CheckAreaTouched2(areaX, areaY, areaX2, areaY2);
    }
}
