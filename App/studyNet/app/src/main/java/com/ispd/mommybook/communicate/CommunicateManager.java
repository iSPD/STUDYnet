package com.ispd.mommybook.communicate;

import android.app.Activity;
import android.content.Context;
import android.opengl.EGLContext;
import android.os.Handler;

import com.ispd.mommybook.R;
import com.ispd.mommybook.communicate.apprtc.CallManager;
import com.ispd.mommybook.motion.MotionHandTrackingImpl;
import com.ispd.mommybook.utils.UtilsLogger;

/**
 * CommunicateManager
 *
 * @author Daniel
 * @version 1.0
 */
public class CommunicateManager {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private CallManager mCallManager = null;
    private CommunicateView mCommunicateView = null;

    /**
     * Init CommunicateManager
     *
     * @param context
     * @param egl_context Shared eglContext from Preview Renderer
     * @param textureID Camera Preview Texture ID
     * @param inputRoomID Room ID
     */
    public CommunicateManager(Context context, Handler handler, EGLContext egl_context, int textureID, String inputRoomID) {

        mCallManager = new CallManager(context, handler, egl_context, textureID, inputRoomID);
        mCallManager.setDrawTouchCallBack(mRemoteCoordinateCallback);

        mCommunicateView = ((Activity)context).findViewById(R.id.cv_communicate);
        mCommunicateView.setTouchCallback(mLocalCoordinateCallback);
    }

    public PreviewWebRTCListener GetPreviewTextureListener()
    {
        return mPreviewTextureListener;
    }

    public interface PreviewWebRTCListener
    {
        void DrawPreviewForWebRtc();
    };

    private PreviewWebRTCListener mPreviewTextureListener = new PreviewWebRTCListener()
    {
        @Override
        public void DrawPreviewForWebRtc() {
            if( mCallManager != null ) {
                LOGGER.d("DrawPreviewForWebRtc");
                mCallManager.DrawPreviewForWebRtc();
            }
        }
    };

    /**
     * Start Remote Call
     *
     */
    public void Start() {
        mCallManager.onStart();
        ShowUI(true);
    }

    /**
     * Stop Remote Call
     *
     */
    public void Stop() {
        ShowUI(false);
        mCallManager.onCallHangUp();
    }

    /**
     * set ui
     *
     * @param show When connected true, Not connected false
     */
    public void ShowUI(boolean show) {
        if( mCallManager != null ) {
            mCallManager.showUISetting(show);
        }
    }

    /**
     * Touch Draw Callback from view to call manager
     * for send touch data to teacher
     *
     */
    private CommunicateView.LocalCoordinateCallback mLocalCoordinateCallback
                                                = new CommunicateView.LocalCoordinateCallback() {
        @Override
        public void moving(float x, float y) {
            if (mCallManager != null) {
                mCallManager.onTouchDataSend("moving," + x + "," + y);
            }
        }

        @Override
        public void start(float x, float y) {
            if (mCallManager != null) {
                mCallManager.onTouchDataSend("start," + x + "," + y);
            }
        }

        @Override
        public void end(float x, float y) {
            if (mCallManager != null) {
                mCallManager.onTouchDataSend("end," + x + "," + y);
            }
        }
    };

    /**
     * send AIScore datas to remote teacher
     */
    public void sendAISCoreData(String datas) {
        if (mCallManager != null) {
            LOGGER.d("sendAISCoreData : %d", datas.length());
            mCallManager.onAIScoreDataSend(datas);
        }
    }

    /**
     * send finger datas to remote teacher For first hand
     *
     * @param datas 21 finger datas of x, y, z
     */
    public void sendFingerLocationData(float []datas) {
        if (mCallManager != null) {
            LOGGER.d("sendFingerLocationData : %d", datas.length);
            mCallManager.onFingerDataSend(datas);
        }
    }

    /**
     * send finger datas to remote teacher For second hand
     *
     * @param datas 21 finger datas of x, y, z
     */
    public void sendFingerLocationData2(float []datas) {
        if (mCallManager != null) {
            LOGGER.d("sendFingerLocationData2 : %d", datas.length);
            mCallManager.onFingerDataSend2(datas);
        }
    }

    /**
     * send image expanding datas to remote teacher
     *
     * @param datas matrix for warpPerspective at remote teacher
     */
    public void sendImageExpandingData(float []datas) {
        if (mCallManager != null) {
            mCallManager.onExpandingDataSend(datas);
        }
    }

    /**
     * Touch Draw Callback from call manager to view
     * for got touch data to student
     *
     */
    private CommunicateView.RemoteCoordinateCallback mRemoteCoordinateCallback
            = new CommunicateView.RemoteCoordinateCallback()
    {
        @Override
        public void drawTouchInfo(String msg) {
            mCommunicateView.setTouchInfo(msg);
        }
    };

    public void UndoDrawing() {
        mCommunicateView.resetView();
    }
}
