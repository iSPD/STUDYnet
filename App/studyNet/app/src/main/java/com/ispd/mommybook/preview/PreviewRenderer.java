package com.ispd.mommybook.preview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.view.Surface;

import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.TuningManager;
import com.ispd.mommybook.communicate.CommunicateManager;
import com.ispd.mommybook.motion.MotionHandTrackingImpl;
import com.ispd.mommybook.utils.UtilsLogger;

import java.util.Date;

/**
 * PreviewRenderer
 *
 * @author Daniel
 * @version 1.0
 */
public class PreviewRenderer implements SurfaceTexture.OnFrameAvailableListener {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;

    private Context mContext;
    private Surface mSurface;
    private EGLContext mCurrentEGLConext;

    private static final int EGL_OPENGL_ES2_BIT = 4;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;

    // Camera Data
    private SurfaceTexture mCameraSurfaceTexture = null;
    private int mCameraTextureName[] = {-1};
    private int mCameraTextureDebug[] = {-1};

    private int mAlignedTexture[] = {-1};
    private int mAlignedFBO[] = {-1};

    private int mHandTexture[] = {-1};
    private int mHandFBO[] = {-1};

    private PreviewDrawBasic mPreviewDrawBasic = null;
    private PreviewDrawCrop mPreviewDrawCrop = null;
    private PreviewDrawAlignment mPreviewDrawAlignment = null;
    private PreviewDrawFixCurve mPreviewDrawFixCurve = null;
    private PreviewDrawForHand mPreviewDrawForHand = null;

    private MotionHandTrackingImpl.PreviewTextureListener mMotionHandTrackingListener = null;

    public static int BASIC_MODE = 0;
    public static int CROP_MODE = 1;
    public static int ALIGNMENT_MODE = 2;
    public static int ALIGNMENT_MODE2 = 3;

    private int mDebugMode = ALIGNMENT_MODE;

    private boolean mLowDebugOn = false;
    private int mCurveFixOn = 1;

    private boolean mCommunicationMode = false;
    private CommunicateManager.PreviewWebRTCListener mPreviewWebRTCListener = null;

    private Object camLock = new Object();

    /**
     * Init OpenGL for Camera Preview
     *
     * @param context
     * @param width
     * @param height
     * @param surface
     * @return eglcontext for sharing eglcontext
     */
    public PreviewRenderer(Context context, int width, int height, Surface surface, EGLContext eglcontext) {

        mContext = context;

        mCameraPreviewWidth = width;
        mCameraPreviewHeight = height;

        if ( surface == null ) {
            throw new NullPointerException();
        }
        mSurface = surface;

        eglSetup(eglcontext);

        makeCurrent(true);

        //create Texture & FBO
        PreviewRendererImpl.setRotationBuffer();
        mPreviewDrawBasic = new PreviewDrawBasic(width, height);
        mPreviewDrawCrop = new PreviewDrawCrop(width, height);
        mPreviewDrawAlignment = new PreviewDrawAlignment(width, height);
        mPreviewDrawFixCurve = new PreviewDrawFixCurve(width, height);
        mPreviewDrawForHand = new PreviewDrawForHand(width, height);
        //mPreviewDrawForHand = new PreviewDrawForHand(320, 240);

        createSurfaceTexture();
        createDebugTexture();
        createAlignedTexture();
        createHandTexture();

        //createBitmapTexture(context);

        mCurrentEGLConext = EGL14.eglGetCurrentContext();
        LOGGER.d("init");

        makeCurrent(false);

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(true) {
//                    //send preview texture to motion hand tracking
//                    if (mMotionHandTrackingListener != null && mCameraSurfaceTexture != null) {
//                        //LOGGER.d("mMotionHandTrackingListenerThread");
//                        synchronized(camLock) {
//                            mMotionHandTrackingListener.sendPreviewTexture(
//                                    mCameraTextureName[0],
//                                    mCameraSurfaceTexture.getTimestamp());
//                        }
//                    }
//                    try {
//                        Thread.sleep(0);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//        thread.start();

//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(true) {
//                    //send preview texture to motion hand tracking
//                    //use here...
//                    //if (mMotionHandTrackingListener != null && mCameraSurfaceTexture != null) {
//                    if( mCameraTextureName[0] != -1 ) {
//                        LOGGER.d("mCameraTextureName");
//                        drawPreview();
//                    }
//                    try {
//                        Thread.sleep(50);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//        thread.start();
    }

    /**
     * get EGLContext
     *
     * @return EGLContext
     */
    public EGLContext GetEGLContext()
    {
        return mCurrentEGLConext;
    }

    /**
     * Init egl
     *
     * @param eglcontext if null, create here
     */
    private void eglSetup(EGLContext eglcontext) {

        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }

        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                //EGL_RECORDABLE_ANDROID, 0,
                EGL14.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
        }

        LOGGER.d("Configs Num=" + numConfigs[0]);

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        if( eglcontext != null ) {
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], eglcontext, attrib_list, 0);
            LOGGER.d("eglCreateContext-1 : "+eglcontext);
            checkEglError("eglCreateContext-1 : "+eglcontext);
        }
        else
        {
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT, attrib_list, 0);
            LOGGER.d("eglCreateContext-2");
            checkEglError("eglCreateContext-2");
        }

        if (mEGLContext == null) {
            throw new RuntimeException("null context");
        }

        LOGGER.d("Context=" + mEGLContext);

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };

        mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface, surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
        if (mEGLSurface == null) {
            throw new RuntimeException("surface was null");
        }

        LOGGER.d("Surface=" + mEGLSurface);
    }

    /**
     * makeCurrent
     *
     * @param enable
     */
    private void makeCurrent(boolean enable) {

        if ( enable ) {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }
        else {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
                throw new RuntimeException("eglMakeCurrent failed (0)");
            }
        } // !enable
    }

    /**
     * swapBuffers
     *
     * @return eglSwapBuffers
     */
    private boolean swapBuffers() {
        return EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
    }

    /**
     * Print egl error
     *
     * @param msg
     */
    private void checkEglError(String msg) {
        boolean failed = false;
        int error;
        while ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            LOGGER.e(msg + ": EGL error: 0x" + Integer.toHexString(error));
            failed = true;
        }

        if (failed) {
            throw new RuntimeException("EGL error encountered (see log)");
        }
    }

    /**
     * Get Camera Preview Texture ID
     */
    public int GetTextureID()
    {
        //return mCameraTextureName[0];
        return mHandTexture[0];
        //return mAlignedTexture[0];
    }

    /**
     * Get Camera Preview Texture ID
     */
    public int GetTextureID2()
    {
        return mCameraTextureName[0];
        //return mHandTexture[0];
        //return mAlignedTexture[0];
    }


    /**
     * Create Camera Preview Texture
     */
    private void createSurfaceTexture() {
        if ( mCameraSurfaceTexture != null ) {
            mCameraSurfaceTexture.release();
            mCameraSurfaceTexture = null;
        }

        //use here...
        mCameraTextureName[0] = PreviewRendererImpl.createExternalTexture();

        mCameraSurfaceTexture = new SurfaceTexture(mCameraTextureName[0]);
        mCameraSurfaceTexture.setOnFrameAvailableListener(this);

//        mCameraTextureName[0] = PreviewRendererImpl.createExternalTexture();
//
//        mCameraSurfaceTexture = new SurfaceTexture(mCameraTextureName[0]);
//        mCameraSurfaceTexture.setOnFrameAvailableListener(this);
    }

    private void createDebugTexture() {
        mCameraTextureDebug[0] = PreviewRendererImpl.createTexture(mCameraPreviewWidth, mCameraPreviewHeight, 32);
    }

    private void createAlignedTexture() {
        mAlignedTexture[0] = PreviewRendererImpl.createTexture(mCameraPreviewWidth, mCameraPreviewHeight, 32);
        mAlignedFBO[0] = PreviewRendererImpl.createFBO(mAlignedTexture[0]);
    }

    private void createHandTexture() {
//        mHandTexture[0] = PreviewRendererImpl.createTexture(mCameraPreviewWidth, mCameraPreviewHeight, 32);
//        mHandFBO[0] = PreviewRendererImpl.createFBO(mHandTexture[0]);

        mHandTexture[0] = PreviewRendererImpl.createTexture(mCameraPreviewWidth, mCameraPreviewHeight, 32);
//        mHandTexture[0] = PreviewRendererImpl.createTexture(640, 480, 32);
        mHandFBO[0] = PreviewRendererImpl.createFBO(mHandTexture[0]);
    }

    /**
     * Get Camera Preview Texture
     *
     * @return Camera Preview SurfaceTexture
     */
    public SurfaceTexture GetSurfaceTexture()
    {
        return mCameraSurfaceTexture;
    }

    /**
     * Create Bitmap Texture
     *
     * @param context
     */
    private void createBitmapTexture(Context context) {

        LOGGER.d("createBitmapTexture start");
        LOGGER.d("createBitmapTexture end");
    }

    /**
     * Deinit OpenGL for Camera Preview
     *
     */
    public void Release() {

        synchronized (this) {

            makeCurrent(true);

            //use here...
            if (mCameraSurfaceTexture != null) {
                mCameraSurfaceTexture.setOnFrameAvailableListener(null);
                mCameraSurfaceTexture.release();
                mCameraSurfaceTexture = null;
            }

//            if (mCameraSurfaceTexture != null) {
//                mCameraSurfaceTexture.setOnFrameAvailableListener(null);
//                mCameraSurfaceTexture.release();
//                mCameraSurfaceTexture = null;
//            }

            if (mCameraTextureName[0] > 0) {

                GLES20.glDeleteBuffers(1, mCameraTextureName, 0);
            }

            if (mCameraTextureDebug[0] > 0) {

                GLES20.glDeleteBuffers(1, mCameraTextureDebug, 0);
            }

            if (mAlignedTexture[0] > 0) {

                GLES20.glDeleteBuffers(1, mAlignedTexture, 0);
            }

            if (mAlignedFBO[0] > 0) {

                GLES20.glDeleteBuffers(1, mAlignedFBO, 0);
            }

            if (mHandTexture[0] > 0) {

                GLES20.glDeleteBuffers(1, mHandTexture, 0);
            }

            if (mHandFBO[0] > 0) {

                GLES20.glDeleteBuffers(1, mHandFBO, 0);
            }

            mPreviewDrawBasic.release();
            mPreviewDrawCrop.release();
            mPreviewDrawAlignment.release();
            mPreviewDrawFixCurve.release();
            mPreviewDrawForHand.release();

            makeCurrent(false);

            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglTerminate(mEGLDisplay);

            // null everything out so future attempts to use this object will cause an NPE
            mEGLDisplay = null;
            mEGLContext = null;
            mEGLSurface = null;
            mSurface = null;
        }
    }

    private Date lastTime = new Date();
    // lastTime은 기준 시간입니다.
    // 처음 생성당시의 시간을 기준으로 그 다음 1초가 지날때마다 갱신됩니다.
    private long frameCount = 0, nowFps = 0;
    // frameCount는 프레임마다 갱신되는 값입니다.
    // nowFps는 1초마다 갱신되는 값입니다.
    void count(){
        Date nowTime = new Date();
        long diffTime = nowTime.getTime() - lastTime.getTime();
        // 기준시간 으로부터 몇 초가 지났는지 계산합니다.

        if (diffTime >= 1000) {
            // 기준 시간으로 부터 1초가 지났다면
            nowFps = frameCount;
            LOGGER.i("[testKhkim] gpu nowFps : "+nowFps);
            frameCount = 0;
            // nowFps를 갱신하고 카운팅을 0부터 다시합니다.
            lastTime = nowTime;
            // 1초가 지났으므로 기준 시간또한 갱신합니다.
        }

        frameCount++;
        // 기준 시간으로 부터 1초가 안지났다면 카운트만 1 올리고 넘깁니다.
    }

    private Date lastTime2 = new Date();
    // lastTime은 기준 시간입니다.
    // 처음 생성당시의 시간을 기준으로 그 다음 1초가 지날때마다 갱신됩니다.
    private long frameCount2 = 0, nowFps2 = 0;
    // frameCount는 프레임마다 갱신되는 값입니다.
    // nowFps는 1초마다 갱신되는 값입니다.
    void count2(){
        Date nowTime = new Date();
        long diffTime = nowTime.getTime() - lastTime2.getTime();
        // 기준시간 으로부터 몇 초가 지났는지 계산합니다.

        if (diffTime >= 1000) {
            // 기준 시간으로 부터 1초가 지났다면
            nowFps2 = frameCount2;
            LOGGER.i("[testKhkim] gpu nowFps2 : "+nowFps2);
            frameCount2 = 0;
            // nowFps를 갱신하고 카운팅을 0부터 다시합니다.
            lastTime2 = nowTime;
            // 1초가 지났으므로 기준 시간또한 갱신합니다.
        }

        frameCount2++;
        // 기준 시간으로 부터 1초가 안지났다면 카운트만 1 올리고 넘깁니다.
    }

    /**
     * Listener for Camera Preview
     *
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            drawPreview();
    }

    private long preTextureTimestamp = -1;
    private int textureCount = 0;
    private int mainTextureCount = 0;

    /**
     * Draw Camera Preview
     *
     */
    private void drawPreview() {
        //use here...
         if (mCameraSurfaceTexture != null && mCameraTextureName[0] > 0 ) {
        //if ( mCameraTextureName[0] > 0 ) {

//             count();

             makeCurrent(true);

//             mainTextureCount++;
//             if( mainTextureCount % 5 == 0 ) {
//                 LOGGER.d("[testKhkim] frame SkipDo");
//                 mCameraSurfaceTexture.updateTexImage();
//                 makeCurrent(false);
//
////                 if( mCommunicationMode == true ) {
////                     mPreviewWebRTCListener.DrawPreviewForWebRtc();
////                 }
//
//                 return;
//             }
//             else if( mainTextureCount % 2 == 0 ) {
//                 LOGGER.d("[testKhkim] frame SkipDo2");
//                 mCameraSurfaceTexture.updateTexImage();
//                 makeCurrent(false);
//
//                 //send preview texture to motion hand tracking
//                 if( mMotionHandTrackingListener != null ) {
//
//                     if( preTextureTimestamp != mCameraSurfaceTexture.getTimestamp() ) {
//
//                         textureCount++;
//
//                         int skipCount = 1;
//                         if( mCommunicationMode == true ) {
//                             skipCount = 1;
//                         }
//
//                         if( textureCount % skipCount == 0) {
//                             count2();
//
//                             mMotionHandTrackingListener.sendPreviewTexture(
//                                     mCameraTextureName[0],
//                                     //mHandTexture[0],
//                                     //mAlignedTexture[0],
//                                     mCameraSurfaceTexture.getTimestamp());
//                         }
//                     }
//
//                     preTextureTimestamp = mCameraSurfaceTexture.getTimestamp();
//                 }
//
//                 return;
//             }

             count();

            //use here...
             //synchronized(camLock) {
                 mCameraSurfaceTexture.updateTexImage();
             //}

             mainTextureCount++;

             if( mainTextureCount % 1 == 0) {
                 if (mLowDebugOn == true) {
                     TuningManager.LoadTuningFile();

                     PreviewRendererImpl.updateTextureForDebug(mCameraTextureDebug);
                     mPreviewDrawBasic.DrawPreview(-1,
                             mCameraTextureName[0], mCameraTextureDebug[0],
                             MainActivity.CAMERA_INDEX, mLowDebugOn);
                 } else {
                     if (mDebugMode == BASIC_MODE) {
                         //PreviewRendererImpl.updateTextureForDebug(mCameraTextureDebug);
                         mPreviewDrawBasic.DrawPreview(-1,
                                 mCameraTextureName[0], mCameraTextureDebug[0],
                                 MainActivity.CAMERA_INDEX, mLowDebugOn);
                     } else if (mDebugMode == CROP_MODE) {
                         mPreviewDrawCrop.DrawPreview(-1,
                                 mCameraTextureName[0], mCameraTextureDebug[0],
                                 MainActivity.CAMERA_INDEX);
                     } else if (mDebugMode == ALIGNMENT_MODE) {
                         mPreviewDrawAlignment.DrawPreview(mAlignedFBO[0],
                                 mCameraTextureName[0], mCameraTextureDebug[0],
                                 MainActivity.CAMERA_INDEX);

                         TuningManager.LoadTuningFile();

                         mPreviewDrawFixCurve.SetCurveFix(mCurveFixOn);
                         mPreviewDrawFixCurve.DrawPreview(-1,
                                 mCameraTextureName[0], mAlignedTexture[0],
                                 MainActivity.CAMERA_INDEX);
                     }
                 }

                 swapBuffers();
             }

             mPreviewDrawForHand.DrawPreview(mHandFBO[0], mCameraTextureName[0], mCameraPreviewWidth, mCameraPreviewHeight, MainActivity.CAMERA_INDEX);
//             mPreviewDrawForHand.DrawPreview(mHandFBO[0], mCameraTextureName[0], 640, 480, MainActivity.CAMERA_INDEX);
             GLES20.glFinish();

             makeCurrent(false);

             //send preview texture to motion hand tracking
             if( mMotionHandTrackingListener != null ) {

                 if( preTextureTimestamp != mCameraSurfaceTexture.getTimestamp() ) {

                     textureCount++;

                     int skipCount = 1;
                     if( mCommunicationMode == true ) {
                         skipCount = 1;
                     }

                     if( textureCount % skipCount == 0) {
                         count2();

                         if( mCommunicationMode == true ) {
                             mPreviewWebRTCListener.DrawPreviewForWebRtc();
                         }

//                         mMotionHandTrackingListener.sendPreviewTexture(
//                                 mCameraTextureName[0],
//                                 mCameraSurfaceTexture.getTimestamp());

                         mMotionHandTrackingListener.sendPreviewTexture(
                                 //mCameraTextureName[0],
                                 mHandTexture[0],
                                 //mAlignedTexture[0],
                                 mCameraSurfaceTexture.getTimestamp());
                     }
                 }

                 preTextureTimestamp = mCameraSurfaceTexture.getTimestamp();
             }
         }
    }

    public void SetCommunucationMode(boolean mode) {
        mCommunicationMode = mode;
    }

    public boolean GetCommunicationMode() {
        return mCommunicationMode;
    }

    public void SetCommunucationListener(CommunicateManager.PreviewWebRTCListener listener)
    {
        mPreviewWebRTCListener = listener;
    }

    public void SetCameraTextureID(int textureID) {
        mCameraTextureName[0] = textureID;
    }

    public int GetDebugMode() {
        return mDebugMode;
    }

    public void SetDebugMode(int mode) {
        mDebugMode = mode;
    }

    public void SetLowDebugMode(boolean onOff) {
        mLowDebugOn = onOff;
    }

    public void SetCurveFix(int onOff) {
        mCurveFixOn = onOff;
    }

    /**
     * set Preview Texture Listener for motion hand tracking
     *
     * @param callback send preview texture to motion handtraking
     */
    public void SetPreviewTextureListener(MotionHandTrackingImpl.PreviewTextureListener callback)
    {
        mMotionHandTrackingListener = callback;
    }

    public void SetAlignmentAndCurveListener(PreviewDrawAlignment.AlignmentAndCurveValues callback)
    {
        mPreviewDrawAlignment.SetAlignmentAndCurveListener(callback);
    }
}
