package com.ispd.mommybook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.opengl.EGLContext;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.ispd.mommybook.activities.ActivitiesManager;
import com.ispd.mommybook.aiscore.AIScoreManager;
import com.ispd.mommybook.aiscore.AIScoreMultiScoringInfo;
import com.ispd.mommybook.areacheck.AreaCheckCover;
import com.ispd.mommybook.areacheck.AreaCheckPage;
import com.ispd.mommybook.camera.Camera1Manager;
import com.ispd.mommybook.camera.Camera2Manager;
import com.ispd.mommybook.camera.CameraDataManager;
import com.ispd.mommybook.communicate.CommunicateManager;
import com.ispd.mommybook.finding.FindingCover;
import com.ispd.mommybook.finding.FindingPage;
import com.ispd.mommybook.imageclassifier.ImageClassifierManager;
import com.ispd.mommybook.imageprocess.ImageProcessPDAlignment;
import com.ispd.mommybook.motion.MotionHandTrackingDataManager;
import com.ispd.mommybook.motion.MotionHandTrackingManager;
import com.ispd.mommybook.motion.MotionMovingDetect;
import com.ispd.mommybook.preview.PreviewDrawAlignment;
import com.ispd.mommybook.preview.PreviewDrawFixCurve;
import com.ispd.mommybook.preview.PreviewRenderer;
import com.ispd.mommybook.ui.UIManager;
import com.ispd.mommybook.ui.UIPageScan;
import com.ispd.mommybook.ui.UIView;
import com.ispd.mommybook.utils.UtilsLogger;
import com.ispd.mommybook.MainHandlerMessages;
import com.ispd.mommybook.utils.UtilsPlaySound;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.ispd.mommybook.MainHandlerMessages.*;
import static com.ispd.mommybook.imageprocess.ImageProcessPDAlignment.*;
import static com.ispd.mommybook.preview.PreviewRenderer.*;
import static org.opencv.imgcodecs.Imgcodecs.*;

import static com.ispd.mommybook.ui.UIManager.GUIDE_SOUND_OFF;
import static com.ispd.mommybook.ui.UIManager.GUIDE_SOUND_ON;

/**
 * MainActivity
 *
 * @author Daniel
 * @version 1.0
 */

public class MainActivity extends AppCompatActivity {

    private static final UtilsLogger LOGGER = new UtilsLogger();
    private MainHandler mMainHandler = new MainHandler();

    private SurfaceView mCameraSurfaceView = null;
    private SurfaceHolder mCameraSurfaceHolder = null;

    private boolean mUseCamera2API;

    public static int CAMERA_INDEX = 1;

    public static int gCameraPreviewWidth = 1280;
    public static int gCameraPreviewHeight = 960;

    public static int gPreviewRenderWidth = 1600;
    public static int gPreviewRenderHeight = 1200;

    public static int gLCDWidth;
    public static int gLCDHeight;

    //UI Related
    private LayoutInflater mInflater = null;
    private ConstraintLayout mRootLayout = null;
    private FrameLayout mFrameLayoutForCommunication = null;

    private EditText mEditTextCommunicateNumber;
    private ImageButton mBtnCommunicateCall;
    private String mRoomID = "ispd0305";
    private TextView mTVCommunicationInfo;

    private Button mBtnCompareChange;
    private Button mBtnCurveFix;
    private int mCurveFixOn = 1;
	
    private boolean mLowDebugOn = false;
    private Button mBtnCompareLow;
    private Button mBtnCompare;

    private PreviewRenderer mPreviewRenderer = null;
    private EGLContext mEGLContext = null;

    private Camera1Manager mCamera1Manager = null;
    private Camera2Manager mCamera2Manager = null;
    private CameraDataManager mCameraDataManager = null;

    private MotionHandTrackingManager mMotionHandTrackingManager = null;
    private int mHandCount = 0;
    private float []mMotionHandTrackingDatas = new float[21*3+1+1];
    private float []mMotionHandTrackingDatas2 = new float[21*3+1+1];

    private CommunicateManager mCommunicateManager = null;

    //Datas
    private static Mat mCameraPreviewMat = null;
    private Bitmap mCameraPreviewBitmap = null;
    private Mat mPictureCaptureMat = null;

    private AreaCheckCover mAreaCheckCover = null;
    private FindingCover mFindingCover = null;
    private AreaCheckPage mAreaCheckPage = null;
    private FindingPage mFindingPage = null;

    //Check하자
    private AIScoreManager mAIScoreManager = null;
    private MotionMovingDetect mMotionMovingDetect = null;
    private ImageProcessPDAlignment mImageProcessPDAlignment = null;
    private UIManager mUIManager = null;

    private int mPreBookPage = -1;
    private int mAiScoreResultOn = 0;
    private int mAiScoreSaveFrame = 0;

    private ImageView mImageView1;

    /**
     * 집단채점시 3프레임을 얻기 위해 3회 카메라 촬영을 카운트하는 변수
     */
    private static int mTakePictureCnt = 0;
    /**
     * 현재 채점하려는 과목의 페이지에 집단채점할 문제가 들어있는지 체크하는 변수
     */
    private static boolean mHasMultiScoringPart = false;
    /**
     * 실제 채점을 수행하라는 플래그. 집단채점인 경우 3회 카메라 촬영이 끝나야 이 값이 true로 셋팅된다.
     */
    private static boolean mStartAIScoreFlag = false;

    private static int mCaptureCount[] = {0};

    /**
     * 현재 커버가이드 모드인지의 여부.
     */	
    private static boolean mIsCoverGuideMode = false;

    private Context mContext = null;
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    /**
     * Start
     * Check Camera, SDcard, Audio Permission
     * Set Size of Camera Surface View
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        //add layout
        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mRootLayout = findViewById(R.id.activity_main_view);
        //mInflater.inflate(R.layout.communicate_main, mRootLayout, true);

        mFrameLayoutForCommunication = findViewById(R.id.fl_communication);

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO}, 1);
        }

        int []lcdSize = getScreenSize();
        gLCDWidth = lcdSize[0];
        gLCDHeight = lcdSize[1];

        LOGGER.d("mLCDWidth : %d mLCDHeight : %d", gLCDWidth, gLCDHeight);

        mCameraSurfaceView = findViewById(R.id.camera_surface_view);
        mCameraSurfaceHolder = mCameraSurfaceView.getHolder();
        mCameraSurfaceHolder.setFixedSize(gCameraPreviewWidth, gCameraPreviewHeight);

        ViewGroup.LayoutParams cameraLayoutParameter = mCameraSurfaceView.getLayoutParams();
        cameraLayoutParameter.width = gLCDHeight * gCameraPreviewWidth / gCameraPreviewHeight;
        cameraLayoutParameter.height = gLCDHeight;
        mCameraSurfaceView.setLayoutParams(cameraLayoutParameter);

        mCameraSurfaceHolder.addCallback(mSurfaceListener);

        setButtonControl();

        mMotionMovingDetect = new MotionMovingDetect(mMainHandler);
        mAreaCheckCover = new AreaCheckCover(this, mMainHandler);
        mFindingCover = new FindingCover(this, mMainHandler);
        mAreaCheckPage = new AreaCheckPage(mMainHandler);
        mAreaCheckPage.PauseProcess();
        mFindingPage = new FindingPage(this, mMainHandler);

        mAIScoreManager = new AIScoreManager(this, mMainHandler);
        mImageProcessPDAlignment = new ImageProcessPDAlignment();
        mUIManager = new UIManager(mRootLayout, mMainHandler);

//        Thread mainThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while( true ) {
//                    mainController();
//
//                    try {
//                        Thread.sleep(33);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//        mainThread.start();

        mImageView1 = findViewById(R.id.imageDebugView1);
        mImageView1.setVisibility(View.GONE);
    }

    /**
     * Start Hand Tracking
     */
    private void startHandTracking()
    {
        if( mMotionHandTrackingManager == null ) {
            if( mPreviewRenderer != null ) {
            //use here...
            //if( true ) {

                mInflater.inflate(R.layout.motion_handtracking, mRootLayout, true);

                mMotionHandTrackingManager = new MotionHandTrackingManager(
                                    MainActivity.this,
                                            gCameraPreviewWidth,
                                            gCameraPreviewHeight,
                                            gLCDWidth, gLCDHeight,
                                            mPreviewRenderer.GetTextureID(),
                                            mPreviewRenderer.GetEGLContext());

//                mMotionHandTrackingManager = new MotionHandTrackingManager(
//                                    MainActivity.this,
//                                    gCameraPreviewWidth,
//                                    gCameraPreviewHeight,
//                                    gLCDWidth, gLCDHeight,
//                                    -1,
//                                    null);

                //put info to main
                mMotionHandTrackingManager.SetFingerInfoListener(mFingerInfoListener);
                //control int preview renderer
                mPreviewRenderer.SetPreviewTextureListener(mMotionHandTrackingManager.GetPreviewTextureListener());
                //mPreviewRenderer.SetPreviewTextureListener(mMotionHandTrackingManager.GetPreviewTextureListener());

                //임시로 전달하고 있습니다.
                mAIScoreManager.SetHandTrackingManager(mMotionHandTrackingManager);
            }
        }
    }

    /**
     * Init Buttons
     */
    private void setButtonControl()
    {
        mBtnCurveFix = findViewById(R.id.btn_curve_fix);
        mBtnCurveFix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurveFixOn = 1 - mCurveFixOn;
                mBtnCurveFix.setText("CurveFixOn-"+mCurveFixOn);
                mImageProcessPDAlignment.SetCurveFixOn(mCurveFixOn);
                mPreviewRenderer.SetCurveFix(mCurveFixOn);
            }
        });

        mBtnCompareChange = findViewById(R.id.btn_compare_change);
        mBtnCompareChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( mLowDebugOn == true ) {
                    mLowDebugOn = false;
                }
                else {
                    mLowDebugOn = true;
                }

                mPreviewRenderer.SetLowDebugMode(mLowDebugOn);
            }
        });

        mBtnCompareLow = findViewById(R.id.btn_compare_low);
        mBtnCompareLow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mode = mImageProcessPDAlignment.GetDebugMode() + 1;
                if(mode > 2) {
                    mode = 0;
                }

                mImageProcessPDAlignment.SetDebugMode(mode);
            }
        });

        mBtnCompare = findViewById(R.id.btn_compare);
        mBtnCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mode = mPreviewRenderer.GetDebugMode() + 1;
                if( mode > ALIGNMENT_MODE ) {
                    mode = BASIC_MODE;
                }
                mPreviewRenderer.SetDebugMode(mode);
            }
        });

        mBtnCurveFix.setVisibility(View.GONE);
        mBtnCompareChange.setVisibility(View.GONE);
        mBtnCompareLow.setVisibility(View.GONE);
        mBtnCompare.setVisibility(View.GONE);

        mEditTextCommunicateNumber = findViewById(R.id.et_communicate_number);
        mEditTextCommunicateNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if( editable.toString().equals("") == false )
                {
                    mRoomID = editable.toString();
                    Log.d("mRoomID", "mRoomID : "+mRoomID);
                }
            }
        });

        mBtnCommunicateCall = findViewById(R.id.btn_communicate_call);
        mBtnCommunicateCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                if( mBtnCommunicateCall.getText().toString().equals("CallImage") == true ) {
                if( mPreviewRenderer.GetCommunicationMode() == false ) {

                    mPreviewRenderer.SetCommunucationMode(true);
                    //add layout
                    //mInflater.inflate(R.layout.communicate_main, mRootLayout, true);
                    mInflater.inflate(R.layout.communicate_main, mFrameLayoutForCommunication, true);
                    mTVCommunicationInfo = findViewById(R.id.tv_communication_info);

//                    mBtnCommunicateCall.setText("StopImage");
                    mBtnCommunicateCall.setImageResource(R.drawable.btn_stop);

                    if (mCommunicateManager == null) {

                        if( mPreviewRenderer != null ) {
                            String roomID = mRoomID;//"ispd057581";

                            mTVCommunicationInfo.setText("Connected to\n"+mRoomID);

                            mCommunicateManager = new CommunicateManager(
                                            MainActivity.this,
                                                    mMainHandler,
                                                    mPreviewRenderer.GetEGLContext(),
                                                    mPreviewRenderer.GetTextureID(),
                                                    roomID);
//                            mCommunicateManager = new CommunicateManager(
//                                    MainActivity.this,
//                                    mMotionHandTrackingManager.GetEGLContext(),
//                                    mMotionHandTrackingManager.GetCameraTextureID(),
//                                    roomID);

                            mCommunicateManager.Start();

                            mPreviewRenderer.SetCommunucationListener(mCommunicateManager.GetPreviewTextureListener());
                        }
                    }
                }
                else
                {
                    mPreviewRenderer.SetCommunucationMode(false);

//                    mBtnCommunicateCall.setText("CallImage");
                    mBtnCommunicateCall.setImageResource(R.drawable.btn_call);

                    if (mCommunicateManager != null) {
                        mCommunicateManager.Stop();
                        mCommunicateManager = null;
                    }

                    FrameLayout communicateView = findViewById(R.id.communicate_main_view);
                    //mRootLayout.removeView(communicateView);
                    mFrameLayoutForCommunication.removeView(communicateView);

                    mTVCommunicationInfo.setText("");
                }
            }
        });
    }

    protected void saveState(){ // 데이터를 저장한다.
        SharedPreferences pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("text", mRoomID);

        editor.commit();

        LOGGER.d("[save-state] saveState : "+mRoomID);
    }
    protected void restoreState(){  // 데이터를 복구한다.
        SharedPreferences pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
        if((pref!=null) && (pref.contains("text"))){
            mRoomID = pref.getString("text", "");
            mEditTextCommunicateNumber.setText(mRoomID);

            LOGGER.d("[save-state] restoreState : "+mRoomID);
        }

    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent event){

        LOGGER.d("Long press KEYCODE : "+keycode);

        if(keycode == KeyEvent.KEYCODE_VOLUME_UP
                || keycode == KeyEvent.KEYCODE_VOLUME_DOWN)
        {
            if( mBtnCurveFix.getVisibility() == View.VISIBLE ) {
                mBtnCurveFix.setVisibility(View.GONE);
                mBtnCompareChange.setVisibility(View.GONE);
                mBtnCompareLow.setVisibility(View.GONE);
                mBtnCompare.setVisibility(View.GONE);
            }
            else {
                mBtnCurveFix.setVisibility(View.VISIBLE);
                mBtnCompareChange.setVisibility(View.VISIBLE);
                mBtnCompareLow.setVisibility(View.VISIBLE);
                mBtnCompare.setVisibility(View.VISIBLE);
            }

            return super.onKeyDown(keycode, event);
        }
        return super.onKeyDown(keycode, event);
    }

    /**
     * Get LCD Size
     * @return LCD Size
     */
    private int[] getScreenSize() {
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(point);
        int width = point.x;
        int height = point.y;
        return new int[]{width, height};
    }

    /**
     * Check whether use Android Camera2 API or not
     * @return If Camera2 is avaliable, return true
     */
    private boolean isCamera2Supported() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(String.valueOf(CAMERA_INDEX));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL == deviceLevel;
        }

        // deviceLevel is not LEGACY, can use numerical sort
        return CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL <= deviceLevel;
    }

    /**
     * Listener for Camera Surface View
     *
     * In surfaceChanged
     * 1. Init OpenGl for preview
     * 2. Open Camera1 or Camera2 API
     * 3. Set camera buffer listener
     *
     * In surfaceDestroyed
     * Deinit all
     */
    private SurfaceHolder.Callback mSurfaceListener = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            //startHandTracking();

            if( mPreviewRenderer == null )
            {
                mPreviewRenderer = new PreviewRenderer(
                                    getApplicationContext(),
                                    gCameraPreviewWidth,
                                    gCameraPreviewHeight,
                                    surfaceHolder.getSurface(),
                                    mEGLContext);

//                mPreviewRenderer = new PreviewRenderer(
//                        getApplicationContext(),
//                        gCameraPreviewWidth,
//                        gCameraPreviewHeight,
//                        surfaceHolder.getSurface(),
//                        mMotionHandTrackingManager.GetEGLContext());
//                mPreviewRenderer.SetCameraTextureID(mMotionHandTrackingManager.GetCameraTextureID());

                mPreviewRenderer.SetAlignmentAndCurveListener(mAlignmentAndCurveValues);
            }

            boolean useCamera2 = isCamera2Supported();
            LOGGER.d("useCamera2 : %s", useCamera2 ? "Yes" : "No");

            if( mCameraDataManager == null ) {
                mCameraDataManager = new CameraDataManager(
                                    gCameraPreviewWidth,
                                    gCameraPreviewHeight);
                mCameraDataManager.SetCameraPreviewDataListener(mCameraPreviewDataListener);
            }

            //if( useCamera2 == true ) {
            if( false ) {
                if( mCamera2Manager == null ) {
                    mCamera2Manager = new Camera2Manager(
                                    getApplicationContext(),
                                    gCameraPreviewWidth,
                                    gCameraPreviewHeight,
                                    mPreviewRenderer.GetSurfaceTexture(),
                                    mCameraDataManager.GetCamera2Listener(),
                                    CAMERA_INDEX);
                }
            }
            else {
                //use this...
                if( mCamera1Manager == null ) {
                    mCamera1Manager = new Camera1Manager(
                                    mMainHandler,
                                    gCameraPreviewWidth,
                                    gCameraPreviewHeight,
                                    mPreviewRenderer.GetSurfaceTexture(),
                                    mCameraDataManager.GetCamera1Listener(),
                                    CAMERA_INDEX);
                }

//                if( mCamera1Manager == null ) {
//                    mCamera1Manager = new Camera1Manager(
//                                    mMainHandler,
//                                    gCameraPreviewWidth,
//                                    gCameraPreviewHeight,
//                                    mMotionHandTrackingManager.GetSurfaceTexture(),
//                                    mCameraDataManager.GetCamera1Listener(),
//                                    CAMERA_INDEX);
//                }

            }

            //After Initing Preview Renderer, It's possible to start hand tracking.
            startHandTracking();
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            if( mCamera1Manager != null ) {
                mCamera1Manager.Release();
                mCamera1Manager = null;
            }

            if( mCamera2Manager != null ) {
                mCamera2Manager.Release();
                mCamera2Manager = null;
            }

            if( mPreviewRenderer != null ) {
                mPreviewRenderer.Release();
                mPreviewRenderer = null;
            }
        }
    };

    public static Mat GetPreviewData() {
        return mCameraPreviewMat;
    }

    private CameraDataManager.CameraPreviewDataListener mCameraPreviewDataListener = new CameraDataManager.CameraPreviewDataListener() {

        @Override
        public void setCameraPreviewData(Mat rgbMat) {
            mCameraPreviewMat = rgbMat.clone();

            if( mAreaCheckCover != null ) {
                mAreaCheckCover.SetPreviewData(mCameraPreviewMat);
            }

            //moving check
            float cropDatas[] = {gCameraPreviewWidth/4.f, gCameraPreviewHeight/5.f,
                    gCameraPreviewWidth/4.f * 3.f, gCameraPreviewHeight/5.f,
                    gCameraPreviewWidth/4.f, gCameraPreviewHeight/5.f * 4.f,
                    gCameraPreviewWidth/4.f * 3.f, gCameraPreviewHeight/5.f * 4.f};
            mMotionMovingDetect.CheckMoving(mCameraPreviewMat, cropDatas);

//            boolean movingOn = mMotionMovingDetect.GetMovingRunning();
//            LOGGER.d("movingOn : "+movingOn);

            //image alignment
            mImageProcessPDAlignment.ImageAlignment(mCameraPreviewMat);
        }

        @Override
        public void setCameraPreviewData(Bitmap rgbBitmap) {
            mCameraPreviewBitmap = rgbBitmap.copy(rgbBitmap.getConfig(), rgbBitmap.isMutable());
        }
    };

    private MotionHandTrackingDataManager.FingerInfoListener mFingerInfoListener = new MotionHandTrackingDataManager.FingerInfoListener() {

        @Override
        public void setHandCount(int count) {
            mHandCount = count;
        }

        @Override
        public void setFingerLocation(float[] datas) {
            System.arraycopy(datas, 0, mMotionHandTrackingDatas, 0, datas.length);

            if( mCommunicateManager != null ) {
                mCommunicateManager.sendFingerLocationData(mMotionHandTrackingDatas);
            }
        }

        @Override
        public void setFingerLocation2(float[] datas) {
            System.arraycopy(datas, 0, mMotionHandTrackingDatas2, 0, datas.length);

            if( mCommunicateManager != null ) {
                mCommunicateManager.sendFingerLocationData2(mMotionHandTrackingDatas2);
            }
        }

        @Override
        public void setTouchedOn() {

        }
    };

    private PreviewDrawAlignment.AlignmentAndCurveValues mAlignmentAndCurveValues = new PreviewDrawAlignment.AlignmentAndCurveValues() {

        @Override
        public void setAlignmentAndCurveValues(float[] datas) {
            if( mCommunicateManager != null ) {
                mCommunicateManager.sendImageExpandingData(datas);
            }
        }
    };

    private void mainController() {
        if( mCameraDataManager != null ) {
//            Bitmap previewBitmap = mCameraDataManager.GetCameraPreviewBitmap();
//            mImageClassifierManager.RunInference(previewBitmap);

//            Mat previewMat = mCameraDataManager.GetCameraPreviewMat();
//            float cropDatas[] = {gCameraPreviewWidth/4.f, gCameraPreviewHeight/5.f,
//                                gCameraPreviewWidth/4.f * 3.f, gCameraPreviewHeight/5.f,
//                                gCameraPreviewWidth/4.f, gCameraPreviewHeight/5.f * 4.f,
//                                gCameraPreviewWidth/4.f * 3.f, gCameraPreviewHeight/5.f * 4.f};
//            mImageClassifierManager.RunInference(previewMat, cropDatas);
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        restoreState();

//        mAreaCheckCover.PauseProcess();
//        mAreaCheckPage.ResumeProcess();
    }

    @Override
    public synchronized void onPause() {

        saveState();

        super.onPause();

//        mAreaCheckCover.PauseProcess();
//        mAreaCheckPage.PauseProcess();

        //destory hand, webrtc
        //mMotionHandTrackingManager.stopHandTracker();
        mMotionHandTrackingManager = null;

        mAIScoreManager.StopProcess();
    }

    long startTime;
    long stopTime;
    long mStartTimeForCoverGuide = 0;
    long sally_startT = 0;
    boolean mFlagForFindingCoverStart = true;
    boolean mFlagForFindingPageStart = false;
    boolean mFlagMovingDetectedSkip = false;
    /**
     * �� Ŭ�������� ���������� �ڵ鷯
     * ���� Ŭ���� : AreaCheckCover, AreaCheckPage, UICoverGuide, UIInnerGuide,
     *             FindingCover, FindingPage, ImageProcessPDAlignment, MotionMovingDetect,
     *
     */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what) {
                case COVER_GUIDE_ON:
                    if(mFlagForFindingCoverStart == false) {
						break;
					}

                    mAIScoreManager.ClearSoundIcon();
                    mIsCoverGuideMode = true;
                    
                    mAreaCheckPage.PauseProcess();
                    mUIManager.StopInnerGuideAnimation(GUIDE_SOUND_OFF);
                    mAreaCheckCover.ResumeProcess();
                    // UICoverGuide start
                    if( mPreviewRenderer != null ) {
                        mPreviewRenderer.SetDebugMode(BASIC_MODE);
                    }
                    if(mUIManager.IsCoverGuideAnimationRunning() == false) {
                        mUIManager.StartCoverGuideAnimation();
                        mFindingCover.ResetProcess();
                    }

                    break;
                case INNER_GUIDE_ON:
                    long endTime = System.currentTimeMillis();
                    if ((endTime - mStartTimeForCoverGuide) < 5000) {
                        mAIScoreManager.ClearSoundIcon();
                        mUIManager.StopMultiModelScoringAnimation();

                        mFlagMovingDetectedSkip = true;  //무빙 감지되어도 무반응하게 셋팅.

                        mAreaCheckCover.PauseProcess();
                        
                        // UIInnerGuide start
                        if(mUIManager.IsInnerGuideAnimationRunning() == false) {
                        mUIManager.StartInnerGuideAnimation();
                        mFindingPage.ResetProcess();
                        }

                        if( mPreviewRenderer != null ) {
                            mPreviewRenderer.SetDebugMode(BASIC_MODE);
                        }
                    } else { //3초 이내로 책을 펼치지 않으면 책표지가이드를 띄움.
                        mFlagForFindingCoverStart = true;
                        mMainHandler.sendEmptyMessage(COVER_GUIDE_ON);
                    }
                    break;
                case FINDING_COVER_START:
                    if(mFlagForFindingCoverStart == false) {//이값은 inner_guide_on 에서 설정함.
						break;
					}

                    // UICoverGuide stop
                    // FindingCover start'
                    if( mPreviewRenderer != null ) {
                        mPreviewRenderer.SetDebugMode(BASIC_MODE);
                    }

                    mUIManager.StopCoverGuideAnimation();
                    if( mCameraPreviewMat != null ) {
                        mFindingCover.RunProcess(mCameraPreviewMat);
                    }

                    break;
                case FINDING_COVER_DONE:

                    mFlagForFindingCoverStart = false;
                    mUIManager.StopCoverGuideAnimation();

                    // AreaCheckPage resume
                    // MainActivity �� ǥ������ �˷��ֱ�

                    mAreaCheckCover.PauseProcess();

                    //내지 Area Check...
                    int bookCover = mFindingCover.GetCoverIndex();
                    JniController.setCurrentBookInfo(bookCover, -1);
                    mImageProcessPDAlignment.SetAlignmentMode(EDGE_DETECT_MODE);

                    mIsCoverGuideMode = false; 
                    mAreaCheckPage.ResumeProcess();
                    break;
                case MOTION_MOVING_DETECTED:
                    mStartTimeForCoverGuide = System.currentTimeMillis();
                    if(mFlagMovingDetectedSkip == true) {
                        break;
                    }
                    LOGGER.d("SallyWork1 MOTION_MOVING_DETECTED");
                    if(mIsCoverGuideMode == true) { //표지인식 모드 중일때는 아래의 페이지 인식을 실행하지 않음.
                        break;
                    }
                case FINDING_PAGE_INIT:
                    if( mFindingPage != null ) {
                        mFindingPage.ResetProcess();
                    }

                    mAIScoreManager.ClearSticker();
                    mUIManager.RemoveScoreMark();
                    mUIManager.StopMultiModelScoringAnimation();

                    break;
                case FINDING_PAGE_START:

                    mUIManager.StopCoverGuideAnimation();

                    // UIInnerGuide stop
                    // FindingPage start
                    mUIManager.StopInnerGuideAnimation(GUIDE_SOUND_ON);

                    int bookCover2 = mFindingCover.GetCoverIndex();
                    if( bookCover2 > 2 ) {
                        mUIManager.StopInnerGuideAnimation(GUIDE_SOUND_ON);
                        mAreaCheckPage.PauseProcess();
                        //여기서 기다렸다가 진행
                        mMainHandler.sendEmptyMessage(COVER_GUIDE_ON);
                    }

                    if( mCameraPreviewMat != null ) {
                        mFindingPage.RunProcess(mCameraPreviewMat, bookCover2);
                    }

                    break;
                case FINDING_PAGE_DONE:
                    // MainActivity�� ���������� ������Ʈ
                    // PDAlignment �� ���������� ����

                    int bookCover3 = mFindingCover.GetCoverIndex();
                    int bookPage3 = mFindingPage.GetCoverIndex();
                    mAIScoreManager.SetCurrentCoverAndPage(bookCover3, bookPage3);

                    mFlagMovingDetectedSkip = false;

                    if(bookCover3 == 2 && bookPage3 == 15) {
                        return;
                    }

                    if( bookPage3 != mPreBookPage ) {
                        startAIScore();
                    }
                    mPreBookPage = bookPage3;

                    JniController.setCurrentBookInfo(bookCover3, bookPage3);
                    LOGGER.d("bookCover3 : "+bookCover3+", bookPage3 : "+bookPage3);
                    mImageProcessPDAlignment.SetAlignmentMode(ALIGNEMENT_MODE);

                    PreviewDrawAlignment.SetCurrentBookInfo(bookCover3, bookPage3);
                    PreviewDrawFixCurve.SetCurrentBookInfo(bookCover3, bookPage3);
                    if( mPreviewRenderer != null ) {
                        mPreviewRenderer.SetDebugMode(ALIGNMENT_MODE);
                    }

                    mAIScoreManager.RedrawSoundIcon();
//                    mAIScoreManager.DoProcess(mCameraPreviewBitmap,
//                                            mCameraPreviewMat,
//                                            bookCover3,
//                                            bookPage3);

                    break;
//                case MOTION_MOVING_DETECTED:
//                    // FindingPage start(���� finding page ���̶�� ���� ����)
//                    break;

                case REQUEST_PICTURE_CAPTURE:
                    {
						startTime = SystemClock.uptimeMillis();
                        mAiScoreResultOn = msg.arg1;
						mAiScoreSaveFrame = msg.arg2;

                        //현재 과목의 현재페이지에 집단채점할 문제가 포함되어 있는지 확인.
                        int bookCover4 = mFindingCover.GetCoverIndex();
                        int bookPage4 = mFindingPage.GetCoverIndex();
						// 채점 중이면 리턴	
                        if(mAIScoreManager.GetGradingStatus(bookCover4) == false) {
                            LOGGER.d("SallyScoring REQUEST_PICTURE_CAPTURE ---> Grading is running. Return");
                            break;
                        }

						//현재 과목의 현재페이지에 틀린문제,채점 안된 문제가 있는지 확인하기. 채점할게 있으면 촬영하기.
                        int numOfBeingGraded = mAIScoreManager.HasItemsToBeGraded(bookCover4, bookPage4);
                        if (numOfBeingGraded > 0) {
                            LOGGER.d("SallyScoring REQUEST_PICTURE_CAPTURE NumOfBeingGraded=" + numOfBeingGraded);
                            mAIScoreManager.SetGradingStatus(false); //채점 시작

                            //현재 과목의 현재페이지에 집단채점할 문제가 포함되어 있는지 확인.
                            mHasMultiScoringPart = mAIScoreManager.HasMultiScoringPart(bookCover4, bookPage4, mCaptureCount);
                            //IMAGE_MATCHING 방식의 채점인 경우 미리 사진을 찍어두는데, 이 경우엔 정식 채점시작이 아니므로 아래 루틴을 SKIP함.
                            if (mAiScoreSaveFrame != 1) {
                                //촬영 시점부터 채점 시작을 알림.
                                mAIScoreManager.SetDoProcessFlagOn(bookCover4);

                                //wait animation 시작, 채점버튼 disable -> 애니메이션 대신 toast를 띄움.
                                mMainHandler.sendEmptyMessage(AISCORING_GUIDE_START);

                                mAIScoreManager.SetEnableScoringButton(false);

                            }

                            //기존 채점 표시를 지움.
                            if( mCommunicateManager != null ) {
                                mCommunicateManager.UndoDrawing();
                            }
                            mUIManager.RemoveScoreMark();
                            mAIScoreManager.ClearSticker();
                            mUIManager.StopMultiModelScoringAnimation();

                            //채점용 촬영 시작
                        if( mCamera1Manager != null ) {
                            mCamera1Manager.TakePicture();
                        }

                        //sally for debug
                        sally_startT = System.currentTimeMillis();
                        LOGGER.d("SallyRecog-T Recognition Start Time = " +
                                sally_startT +
                                "msec");
                        }
                    }
                    break;

                case PICTURE_CAPTURED:

                    // 채점 표시 지움 : 버튼 클릭시에만 채점표시를 지우고 표시하기 위해, 아래의 채점시작시 지우는
                    //                루틴은 주석처리함.
                    //mMainHandler.sendEmptyMessage(RESET_DRAW_AISCORE);

                    mPictureCaptureMat = ((Mat)(msg.obj)).clone();
                    //imwrite("/sdcard/mPictureCaptureMat.jpg", mPictureCaptureMat);

                    stopTime = SystemClock.uptimeMillis();
                    LOGGER.d("CaptureTime : "+(stopTime-startTime));

                    int bookCover4 = mFindingCover.GetCoverIndex();
                    int bookPage4 = mFindingPage.GetCoverIndex();

                    mTakePictureCnt++;

                    if (mHasMultiScoringPart == true) {
                        if (mTakePictureCnt < mCaptureCount[0]) {
                            mAIScoreManager.SetCaptureMat(mPictureCaptureMat, bookCover4, bookPage4, mTakePictureCnt);
                            if (mCamera1Manager != null) {
                                mCamera1Manager.TakePicture();
                            }
                        }
                        else {
                            mStartAIScoreFlag = true;
                            mTakePictureCnt = 0;
                            mHasMultiScoringPart = false;
                        }
                    }
                    else {
                        mStartAIScoreFlag = true;
                        mTakePictureCnt = 0;
                    }

                    if(mStartAIScoreFlag == true) {

                        mStartAIScoreFlag = false;

	                    mAIScoreManager.DoAiScoreByCaptureMat(
	                            mPictureCaptureMat,
	                            bookCover4,
	                            bookPage4, mAiScoreResultOn, mAiScoreSaveFrame);
                    }

                    break;

                case DRAW_AISCORE:
                    Rect rect = (Rect)msg.obj;
                    if( msg.arg1 == 1) {
                        mUIManager.DrawScoreMarkCircle(rect);
                    }
                    else if( msg.arg1 == 0) { //틀림
                        mUIManager.DrawScoreMarkX(rect);
                    }
                    else if( msg.arg1 == 2 ){ //선생님 채점용
                        mUIManager.DrawScoredMarkTriangle(rect);
                    }

                    break;

                case RESET_DRAW_AISCORE:
                    mUIManager.RemoveScoreMark();
                    break;
				case DRAW_AISCORE_MULTI: //집단채점 결과
                    AIScoreMultiScoringInfo scoreInfo = (AIScoreMultiScoringInfo)msg.obj;
                    LOGGER.d("SallyRecog MainHandler : <<< DRAW_AISCORE_MULTI >>> ");
                    LOGGER.d("SallyRecog DRAW_AISCORE_MULTI x, y, w, h : " + scoreInfo.mX + ", "
                            + scoreInfo.mY + ", " + scoreInfo.mW + ", " + scoreInfo.mH);
                    if(scoreInfo.IsCorrect() == false) {// 틀렸을때만 보여줘야함.
                        mUIManager.StartMultiModelScoringAnimation(scoreInfo); //테스트 코드
                    }
 /*                   // score mark
                    Rect rectarea = new Rect(scoreInfo.mX, scoreInfo.mY, scoreInfo.mW, scoreInfo.mH);

                    if(scoreInfo.IsCorrect() == true)   mUIManager.DrawScoreMark(rectarea);
                    else                                mUIManager.DrawIncorrectScoreMark(rectarea);
*/
                    break;
                case REMOVE_AISCORE_MULTI: //집단채점 결과를 지움
                    mUIManager.StopMultiModelScoringAnimation();
                    break;
                case PLAY_SOUND_SCORING_MESSAGE:
//                    boolean isRunning[] = {false};
//                    mUtilsPlaySound = new UtilsPlaySound(getApplicationContext(), "write", isRunning);
                    break;

                case DEBUG_VIEW:
                    if( msg.arg1 == 0 ) {
                        //mImageView1.setVisibility(View.INVISIBLE);
                    }
                    else {
                        //mImageView1.setVisibility(View.VISIBLE);

                        Bitmap bitmap = (Bitmap)msg.obj;
                        mImageView1.setImageBitmap(bitmap);
                    }
                    break;
                case AISCORING_GUIDE_START:
                    {
					    mUIManager.ShowScoringToast();
                        mAIScoreManager.PlayGuideSound("now_scoring2");

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (!Thread.currentThread().isInterrupted()) {
                                    try {
                                        // 채점 중이면 리턴
                                        if (mAIScoreManager.GetGradingStatus(mFindingCover.GetCoverIndex()) == true) {
                                            LOGGER.d("SallyScoring AISCORING_GUIDE_START ---> Grading is Done. Anim Stop");
                                            long sally_endT = System.currentTimeMillis();
                                            long elapsedT = sally_endT - sally_startT;
                                            LOGGER.d("SallyRecog-T Scoring Elapsed Time = " +
                                                    (sally_endT - sally_startT) +
                                                    "msec");

                                            //채점이 너무 일찍 끝나는 경우 가이드 음성이 끝날때까지는 기다렸다가(약5초) 가이드를 끝냄
                                            if(elapsedT > 5000) {
                                                LOGGER.d("SallyScoring AISCORING_GUIDE_START End of buffer Time.....");
												String coverName = "";
	                                            int coverIndex = mFindingCover.GetCoverIndex();
	                                            if(coverIndex == 0) {
	                                                coverName = "korean";
	                                            }
	                                            else if(coverIndex == 1) {
	                                                coverName = "math";
	                                            }
	                                            else if(coverIndex == 2) {
	                                                coverName = "english";
	                                            }

	                                            if( mCommunicateManager != null ) {
	                                                String path = "/sdcard/studyNet/DB/" + coverName + "/result/result.txt";
	                                                String readData = ReadTextFile(path);
	                                                mCommunicateManager.sendAISCoreData(coverName + "\n" + readData);
	                                            }
                                            	break;
                                            }
                                            else {
                                                LOGGER.d("SallyScoring AISCORING_GUIDE_START now waiting.....");

                                            }
                                        }
                                        Thread.sleep(30); //채점 간격
                                    } catch (InterruptedException ex) {
                                        LOGGER.e("mainHandler interrupted exception : " + ex);
                                        Thread.currentThread().interrupt();
                                    }
                                }
                                //채점이 끝난 후 wait animation 정지
                                mMainHandler.sendEmptyMessage(AISCORING_GUIDE_STOP);

                            }
                        });
                        thread.start();

                    }
                    break;
                case AISCORING_GUIDE_STOP:
                    mAIScoreManager.SetEnableScoringButton(true);
                    mUIManager.HideScoringToast();
                    //mUIManager.StopPageScanAnimation();
                    break;
                case COMMUNICATION_STOP:
                    mPreviewRenderer.SetCommunucationMode(false);

                    mBtnCommunicateCall.setImageResource(R.drawable.btn_call);

                    if (mCommunicateManager != null) {
                        mCommunicateManager.ShowUI(false);
                        mCommunicateManager = null;
                    }

                    //FrameLayout communicateView = findViewById(R.id.communicate_main_view);
                    //mRootLayout.removeView(communicateView);
                    mFrameLayoutForCommunication.removeAllViews();

                    mTVCommunicationInfo.setText("");

                    break;
            }
        }
    }

    private void startAIScore() {
//        mUIManager.RemoveScoreMark();

        if( mCommunicateManager != null ) {
            mCommunicateManager.UndoDrawing();
        }

        int bookCover4 = mFindingCover.GetCoverIndex();
        int bookPage4 = mFindingPage.GetCoverIndex();

        mAIScoreManager.StartProcess(bookCover4, bookPage4);
    }

    //경로의 텍스트 파일읽기
    private String ReadTextFile(String path){
        StringBuffer strBuffer = new StringBuffer();
        try{
            InputStream is = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line="";
            while((line=reader.readLine())!=null){
                strBuffer.append(line+"\n");
            }

            reader.close();
            is.close();
        }catch (IOException e){
            e.printStackTrace();
            return "";
        }
        return strBuffer.toString();
    }
}