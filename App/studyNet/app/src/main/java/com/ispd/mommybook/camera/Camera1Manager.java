package com.ispd.mommybook.camera;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Size;
import android.widget.Toast;

import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.ispd.mommybook.MainHandlerMessages.PICTURE_CAPTURED;
import static org.opencv.core.CvType.*;
import static org.opencv.imgcodecs.Imgcodecs.*;

/**
 * Camera1Manager
 *
 * @author Daniel
 * @version 1.0
 */
public class Camera1Manager {
    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Handler mMainHandler = null;

    private Camera mCamera = null;
    private int mCameraWidth = 0;
    private int mCameraHeight = 0;
    private int mCameraPictureWidth = 0;
    private int mCameraPictureHeight = 0;
    private SurfaceTexture mSurfaceTexture = null;
    private Camera.PreviewCallback mListener = null;

    private Mat mPictureMat = null;

    /**
     * Init camera1 manager
     */
    public Camera1Manager(
            Handler handler,
            int width, int height, SurfaceTexture surfaceTexture,
            Camera.PreviewCallback listener, int cameraIdx) {

        mMainHandler = handler;

        mCameraWidth = width;
        mCameraHeight = height;

        mSurfaceTexture = surfaceTexture;
        mListener = listener;

        init(cameraIdx);
    }

    /**
     * Open Camera1 API
     * Set preview callback buffer
     *
     * @param cameraIdx front or back camera, 0 : back 1 : front
     */
    private void init(int cameraIdx) {
        mCamera = Camera.open(cameraIdx);

        Camera.Parameters parameters = mCamera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        LOGGER.d("FOCUS_MODE : "+focusModes);

        if (focusModes != null
                && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            LOGGER.d("FOCUS_MODE_CONTINUOUS_PICTURE");
        }

        if (focusModes != null
                && focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            LOGGER.d("previewFocus", "FOCUS_MODE_AUTO");
        }

        //parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        for(int i = 0; i < previewSizes.size(); i++) {
            LOGGER.d("previewSizes : %d %d", previewSizes.get(i).width, +previewSizes.get(i).height);
        }

        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        for(int i = 0; i < pictureSizes.size(); i++) {
            LOGGER.d(i+"-PictureSize : %d %d", pictureSizes.get(i).width, +pictureSizes.get(i).height);
        }

        mCameraPictureWidth = pictureSizes.get(0).width;
        mCameraPictureHeight = pictureSizes.get(0).height;
        parameters.setPictureSize(mCameraPictureWidth, mCameraPictureHeight);

        parameters.setPreviewSize(mCameraWidth, mCameraHeight);

        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);

        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera.setPreviewCallbackWithBuffer(mListener);

        int ySize = mCameraWidth * mCameraHeight;
        int uvSize = ((mCameraWidth + 1) / 2) * ((mCameraHeight + 1) / 2) * 2;
        int yuvSize = ySize + uvSize;
        mCamera.addCallbackBuffer(new byte[yuvSize]);

        mCamera.startPreview();
    }

    /**
     * Deinit Camera1 API
     */
    public void Release() {
        if (mCamera != null) {

            mCamera.addCallbackBuffer(null);
            mCamera.setPreviewCallbackWithBuffer(null);

            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void TakePicture() {

        Camera.Parameters parameters = mCamera.getParameters();
        LOGGER.d("[ExposureCheck] getAutoExposureLock : "+parameters.getAutoExposureLock());
        LOGGER.d("[ExposureCheck] getExposureCompensation : "+parameters.getExposureCompensation());
        LOGGER.d("[ExposureCheck] getExposureCompensationStep : "+parameters.getExposureCompensationStep());
        LOGGER.d("[ExposureCheck] getMaxExposureCompensation : "+parameters.getMaxExposureCompensation());
        LOGGER.d("[ExposureCheck] getMinExposureCompensation : "+parameters.getMinExposureCompensation());

//        parameters.setAutoExposureLock(true);
//        parameters.setExposureCompensation(2);
        parameters.setExposureCompensation(-7);
        mCamera.setParameters(parameters);
        mCamera.enableShutterSound(false);

        mCamera.takePicture(null, null, mPicture);
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback(){

        @Override
        public void onPictureTaken( byte[] data, Camera camera){
//            String sd = Environment.getExternalStorageDirectory().getAbsolutePath();
//            String path = sd + "/testshot.jpg";
//
//            File file = new File(path);
//            try{
//                FileOutputStream fos = new FileOutputStream(file);
//                fos.write(data); //바이트배열의 데이터를 파일로 씀
//                fos.flush();
//                fos.close();
//            }
//            catch(Exception e){
//                //Toast. makeText(mContext , "파일 저장 중 에러 발생 : " + e.getMessage(), Toast.LENGTH_SHORT ).show();
//                return;
//            }
//
//            //새로 촬영된 이미지를 DB에 추가하기 위해서 SCAN_FILE방송을 보낸다.
//            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE );
//            Uri uri = Uri. parse( "file://"+path);
//            intent.setData(uri);
//            //sendBroadcast(intent);
//
//            LOGGER.d("onPictureTaken");
//
//            //Toast. makeText(mContext, "사진 저장 완료 " + path, Toast. LENGTH_SHORT).show();

            LOGGER.d("onPictureTaken");

            mPictureMat = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_UNCHANGED);
            //imwrite("/sdcard/test.jpg", mPictureMat);
            Message msg = new Message();
            msg.what = PICTURE_CAPTURED;
            msg.obj = mPictureMat;
            mMainHandler.sendMessage(msg);

            Camera.Parameters parameters = mCamera.getParameters();
//            parameters.setAutoExposureLock(false);
            parameters.setExposureCompensation(0);
            mCamera.setParameters(parameters);

            mCamera.startPreview();
        }
    };
}
