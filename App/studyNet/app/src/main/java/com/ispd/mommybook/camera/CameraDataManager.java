package com.ispd.mommybook.camera;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Trace;

import com.ispd.mommybook.motion.MotionHandTrackingDataManager;
import com.ispd.mommybook.motion.MotionMovingDetect;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * CameraDataManager
 *
 * @author Daniel
 * @version 1.0
 */
public class CameraDataManager
        implements ImageReader.OnImageAvailableListener,
        Camera.PreviewCallback {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    static final int kMaxChannelValue = 262143;

    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;

    private boolean mCameraPreviewBufferDone = false;

    //Camera preview rgb buffer
    private int[] mRgbBytes = null;

    //Camera2
    private byte[][] mYuvBytes = new byte[3][];
    private int mYRowStride;

    private Mat mYUVMat = null;
    private Mat mRGBAMat = null;
    private Bitmap mRgbBitmap = null;

    private CameraPreviewDataListener mCameraPreviewDataListener = null;

    /**
     * Init CameraDataManager
     *
     * @param width
     * @param height
     */
    public CameraDataManager(int width, int height) {

        mCameraPreviewWidth = width;
        mCameraPreviewHeight = height;

        if( mYUVMat == null ) {
            mYUVMat = new Mat(height + (height / 2), width, CV_8UC1);
        }

        if( mRGBAMat == null ) {
            mRGBAMat = new Mat(height, width, CV_8UC4);
        }

        if( mRgbBitmap == null ) {
            mRgbBitmap = Bitmap.createBitmap(
                    width, height, Bitmap.Config.ARGB_8888);
        }
    }

    /**
     * get camera1 callback buffer listener
     * @return Camera.PreviewCallback Listener for camera1
     */
    public Camera.PreviewCallback GetCamera1Listener() {
        return this;
    }

    /**
     * get camera2 callback buffer listener
     * @return ImageReader.OnImageAvailableListener Listener for camera2
     */
    public ImageReader.OnImageAvailableListener GetCamera2Listener() {
        return this;
    }

    /**
     * get preview buffer(Mat)
     * @return Mat Camera Priview Matrix(OpenCV)
     */
    public Mat GetCameraPreviewMat()
    {
        return mRGBAMat;
    }

    /**
     * get preview buffer(Bitmap)
     * @return Mat Camera Priview Bitmap(Android)
     */
    public Bitmap GetCameraPreviewBitmap()
    {
        return mRgbBitmap;
    }

    /**
     * Listener for callback buffer
     *
     * @param bytes camera preview buffer(yuv)
     * @param camera
     */
    /** Callback for android.hardware.Camera API */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {

        LOGGER.d("onPreviewFrame");

//        try {
//            // Initialize the storage bitmaps once when the resolution is known.
//            if (mRgbBytes == null) {
//                Camera.Size previewSize = camera.getParameters().getPreviewSize();
//                mCameraPreviewHeight = previewSize.height;
//                mCameraPreviewWidth = previewSize.width;
//                mRgbBytes = new int[mCameraPreviewWidth * mCameraPreviewHeight];
//            }
//        } catch (final Exception e) {
//            LOGGER.e(e, "Exception!");
//            return;
//        }

        double startTime, endTime;

//        startTime = System.currentTimeMillis();
//        convertYUV420SPToARGB8888(bytes, mCameraPreviewWidth, mCameraPreviewHeight, mRgbBytes);
//        endTime = System.currentTimeMillis();
//        LOGGER.d("convertTime : "+(endTime-startTime));

        startTime = System.currentTimeMillis();
        mYUVMat.put(0, 0, bytes);
        Imgproc.cvtColor(mYUVMat, mRGBAMat, Imgproc.COLOR_YUV2RGBA_NV21, 4);
        flip(mRGBAMat, mRGBAMat, 1);

        mCameraPreviewDataListener.setCameraPreviewData(mRGBAMat);
        endTime = System.currentTimeMillis();
        LOGGER.d("convertTime2 : "+(endTime-startTime));

        startTime = System.currentTimeMillis();
        Utils.matToBitmap(mRGBAMat, mRgbBitmap);

        mCameraPreviewDataListener.setCameraPreviewData(mRgbBitmap);
        //saveBitmap(mRgbBitmap, "mRgbBitmap.png");
        endTime = System.currentTimeMillis();
        LOGGER.d("convertTime3: "+(endTime-startTime));

        mCameraPreviewBufferDone = true;

        camera.addCallbackBuffer(bytes);
    }

    /**
     * Listener for callback buffer
     *
     * @param reader camera preview buffer(yuv)
     */
    /** Callback for Camera2 API */
    @Override
    public void onImageAvailable(final ImageReader reader) {

        LOGGER.d("onImageAvailable");

        if (mRgbBytes == null) {
            mRgbBytes = new int[mCameraPreviewWidth * mCameraPreviewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                LOGGER.d("image is null");

                return;
            }

            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, mYuvBytes);
            mYRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            convertYUV420ToARGB8888(
                    mYuvBytes[0],
                    mYuvBytes[1],
                    mYuvBytes[2],
                    mCameraPreviewWidth,
                    mCameraPreviewHeight,
                    mYRowStride,
                    uvRowStride,
                    uvPixelStride,
                    mRgbBytes);

//            mYUVMat.put(0, 0, mYuvBytes[0]);
//            Imgproc.cvtColor(mYUVMat, mRGBAMat, Imgproc.COLOR_YUV2RGBA_NV21, 4);

            mCameraPreviewBufferDone = true;

            image.close();

        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
    }

    /**
     * Set Camera Preveiw listener to MainActivity
     *
     * @param listener listener
     */
    public void SetCameraPreviewDataListener(CameraPreviewDataListener listener)
    {
        mCameraPreviewDataListener = listener;
    }

    public interface CameraPreviewDataListener
    {
        void setCameraPreviewData(Mat rgbMat);
        void setCameraPreviewData(Bitmap rgbBitmap);
    }

    /**
     * Yub to Rgb
     *
     * @param y
     * @param u
     * @param v
     */
    private int YUV2RGB(int y, int u, int v) {
        // Adjust and check YUV values
        y = (y - 16) < 0 ? 0 : (y - 16);
        u -= 128;
        v -= 128;

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        int y1192 = 1192 * y;
        int r = (y1192 + 1634 * v);
        int g = (y1192 - 833 * v - 400 * u);
        int b = (y1192 + 2066 * u);

        // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
        r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
        g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
        b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

        return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
    }

    //Camera1
    /**
     * Yuv420sp to argb8888
     *
     * @param input
     * @param width
     * @param height
     * @param output
     */
    private void convertYUV420SPToARGB8888(byte[] input, int width, int height, int[] output) {
        final int frameSize = width * height;
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width;
            int u = 0;
            int v = 0;

            for (int i = 0; i < width; i++, yp++) {
                int y = 0xff & input[yp];
                if ((i & 1) == 0) {
                    v = 0xff & input[uvp++];
                    u = 0xff & input[uvp++];
                }

                output[yp] = YUV2RGB(y, u, v);
            }
        }
    }

    //Camera2
    /**
     * Image format to byte
     *
     * @param planes
     * @param yuvBytes
     */
    private void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    /**
     * Yuv420 to argb8888
     *
     * @param yData
     * @param uData
     * @param vData
     * @param width
     * @param height
     * @param yRowStride
     * @param uvRowStride
     * @param uvPixelStride
     * @param out
     */
    private void convertYUV420ToARGB8888(
            byte[] yData,
            byte[] uData,
            byte[] vData,
            int width,
            int height,
            int yRowStride,
            int uvRowStride,
            int uvPixelStride,
            int[] out) {
        int yp = 0;
        for (int j = 0; j < height; j++) {
            int pY = yRowStride * j;
            int pUV = uvRowStride * (j >> 1);

            for (int i = 0; i < width; i++) {
                int uv_offset = pUV + (i >> 1) * uvPixelStride;

                out[yp++] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
            }
        }
    }

    /**
     * Saves a Bitmap object to disk for analysis.
     *
     * @param bitmap The bitmap to save.
     * @param filename The location to save the bitmap to.
     */
    public static void saveBitmap(final Bitmap bitmap, final String filename) {
        final String root =
                Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ispd";
        LOGGER.d("Saving %dx%d bitmap to %s.", bitmap.getWidth(), bitmap.getHeight(), root);
        final File myDir = new File(root);

        if (!myDir.mkdirs()) {
            LOGGER.d("Make dir failed");
        }

        final String fname = filename;
        final File file = new File(myDir, fname);
        if (file.exists()) {
            //file.delete();
        }
        LOGGER.d("file : "+file);
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
        }
    }
}
