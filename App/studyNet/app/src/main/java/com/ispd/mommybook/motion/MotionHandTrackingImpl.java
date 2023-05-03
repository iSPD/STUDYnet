package com.ispd.mommybook.motion;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGLContext;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.mediapipe.formats.proto.DetectionProto;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.framework.PacketCallback;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.glutil.EglManager;
import com.ispd.mommybook.preview.PreviewRendererImpl;
import com.ispd.mommybook.utils.UtilsLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MotionHandTrackingImpl {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private static final String TAG = "handTracker";
    private static final String BINARY_GRAPH_NAME = "hand_tracking_mobile_gpu.binarypb";
    private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
    private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "hand_landmarks";
    private static final String OUTPUT_DETECTIONS_STREAM_NAME = "palm_detections";
    private static final String INPUT_NUM_HANDS_SIDE_PACKET_NAME = "num_hands";
    private static final int NUM_HANDS = 2;//2;//2;
    //private static final CameraHelper.CameraFacing CAMERA_FACING = CameraHelper.CameraFacing.FRONT;
    // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
// processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
// This is needed because OpenGL represents images assuming the image origin is at the bottom-left
// corner, whereas MediaPipe in general assumes the image origin is at top-left.
    private static final boolean FLIP_FRAMES_VERTICALLY = false;//true;

    static {
        // Load all native libraries needed by the app.
        //System.loadLibrary("opencv_java3");
        System.loadLibrary("mediapipe_jni");
    }

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private SurfaceTexture previewFrameTexture;
    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private SurfaceView previewDisplayView;
    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
// frames onto a {@link Surface}.
    private FrameProcessor processor;
    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
// consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private ExternalTextureConverter converter;
    // ApplicationInfo for retrieving metadata defined in the manifest.
    private ApplicationInfo applicationInfo;
    // Handles camera access via the {@link CameraX} Jetpack support library.
    //private CameraXPreviewHelper cameraHelper;
    Context mContext;

    private int mNewSurfaceID = -1;

    public MotionHandTrackingImpl(Context context, int surfaceID, EGLContext eglcontext,
                                  PacketCallback handLandMarkPacketCallback,
                                  PacketCallback handPacketCallback)
    {
        mContext = context;
        previewDisplayView = new SurfaceView(mContext);
        //setupPreviewDisplayView();

        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        AndroidAssetUtil.initializeNativeAssetManager(mContext);
        eglManager = new EglManager(eglcontext);

        processor =
                new FrameProcessor(
                        mContext,
                        eglManager.getNativeContext(),
                        BINARY_GRAPH_NAME,
                        INPUT_VIDEO_STREAM_NAME,
                        OUTPUT_VIDEO_STREAM_NAME);
        processor
                .getVideoSurfaceOutput()
                .setFlipY(FLIP_FRAMES_VERTICALLY);

        processor.getVideoSurfaceOutput().setSurface(null);

        //PermissionHelper.checkAndRequestCameraPermissions(mContext);
        AndroidPacketCreator packetCreator = processor.getPacketCreator();
        Map<String, Packet> inputSidePackets = new HashMap<>();
        inputSidePackets.put(INPUT_NUM_HANDS_SIDE_PACKET_NAME, packetCreator.createInt32(NUM_HANDS));
        processor.setInputSidePackets(inputSidePackets);

        processor.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME,
                handLandMarkPacketCallback);

        processor.addPacketCallback(
                OUTPUT_DETECTIONS_STREAM_NAME,
                handPacketCallback);

        //if you want use local surfaceTexture
        if(false) {
            converter =
                    new ExternalTextureConverter(
                            /*(javax.microedition.khronos.egl.EGLContext)eglcontext*/eglManager.getContext(), 2/*2*/, 0);
            converter.setFlipY(FLIP_FRAMES_VERTICALLY);
            converter.setRotation(Surface.ROTATION_90);
            converter.setConsumer(processor);

            Size viewSize = new Size(1200, 1716);//computeViewSize(width, height);
            Size displaySize = new Size(160, 120);
            boolean isCameraRotated = false;//cameraHelper.isCameraRotated();

            //uncomment below...if want to use here block...
            mNewSurfaceID = PreviewRendererImpl.createExternalTexture();
            previewFrameTexture = new SurfaceTexture(mNewSurfaceID);

            LOGGER.d("setSurfaceTextureAndAttachToGLContext-1 : "+mNewSurfaceID);
            //converter.setSurfaceTextureAndAttachToGLContext(
            converter.setSurfaceTexture(
                    previewFrameTexture,
                    isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                    isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());

            converter.controlSpeed(0);
        }
        else { //if you want use external surfaceTexture
            converter =
                    new ExternalTextureConverter(
                            /*(javax.microedition.khronos.egl.EGLContext)eglcontext*/eglManager.getContext(), 2/*4*//*8*//*2*/, 1, 0);
//            /*(javax.microedition.khronos.egl.EGLContext)eglcontext*/eglManager.getContext(), 2/*4*//*8*//*2*/, 1, 1);
            converter.setFlipY(FLIP_FRAMES_VERTICALLY);
            converter.setConsumer(processor);

            Size viewSize = new Size(1200, 1716);//computeViewSize(width, height);
            //Size displaySize = new Size(1280, 720);
            Size displaySize = new Size(640, 480);

            boolean isCameraRotated = false;//cameraHelper.isCameraRotated();

            converter.setTextureID(surfaceID,
                    isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                    isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
            LOGGER.d("setSurfaceTextureAndAttachToGLContext-2");
        }
    }

    public EGLContext GetEGLContext()
    {
        LOGGER.d("getEGLContext : "+eglManager.getEgl14Context());
        return eglManager.getEgl14Context();
    }

    //local
    public int GetCameraTextureID()
    {
        return mNewSurfaceID;
    }

    public SurfaceTexture GetSurfaceTexture()
    {
        return previewFrameTexture;
    }

    //looper
    public PreviewTextureListener GetPreviewTextureListener()
    {
        return mPreviewTextureListener;
    }

    public interface PreviewTextureListener
    {
        void sendPreviewTexture(int surfaceID, long timeStamp);
    };

    private PreviewTextureListener mPreviewTextureListener = new PreviewTextureListener()
    {
        @Override
        public void sendPreviewTexture(int surfaceID, long timeStamp) {
            LOGGER.d("sendPreviewTexture %d %d", surfaceID, timeStamp);

            converter.doDrawFrame(surfaceID, timeStamp, 0);
//            converter.doDrawFrame(surfaceID, timeStamp, 1);
            converter.setTimestampOffsetNanos(999999999);
        }
    };

    public void PauseHandTracker(boolean stop)
    {
        converter.controlDrawing(stop);
    }
}
