package com.ispd.mommybook.ocr.detection;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Trace;

import com.ispd.mommybook.utils.UtilsFile;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static org.opencv.core.CvType.CV_32FC1;

/**
 * Text Detection Inference
 *
 * @author ispd_sally
 * @version 1.0
 */

public class OCRDetectionInference implements Classifier {
    private static final UtilsLogger LOGGER = new UtilsLogger();

    // Only return this many results.
    private static final int NUM_DETECTIONS = 10;
    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    // Number of threads in the java app
    private static final int NUM_THREADS = 4;

    private boolean mIsModelQuantized;
    // Config values.
    private int mInputSize;
    // Pre-allocated buffers.
    private Vector<String> mLabels = new Vector<String>();
    private int[] mInputImgIntData;
    // mOutputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] mOutputLocations;
    // mOutputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] mOutputClasses;
    // mOutputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] mOutputScores;
    // mNumDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] mNumDetections;

    private float[][][][] outputImage;

    private float[][][][] outputScore;
    private float[][][][] outputGeometry;
    private float mScoreFloat[];
    private float mGeoFloat[];
    private Mat mScoreMat;
    private Mat mGeomertyMat;

    private ByteBuffer mInputImgByteData;

    private Interpreter mTfLite;

    private float []mFloatOutput;
    private Mat mInputMat;
    private Mat mEightMat;

    private OCRDetectionInference() {}

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager in_assets, String in_modelFileName)
            throws IOException {
        AssetFileDescriptor fileDescriptor = in_assets.openFd(in_modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param in_assetManager The asset manager to be used to load assets.
     * @param in_modelFileName The filepath of the model GraphDef protocol buffer.
     * @param in_labelFileName The filepath of label file for classes.
     * @param in_inputSize The size of image input
     * @param in_isQuantized Boolean representing model is quantized or not
     */
    public static Classifier Create(
            final AssetManager in_assetManager,
            final String in_modelFileName,
            final String in_labelFileName,
            final int in_inputSize,
            final boolean in_isQuantized)
            throws IOException {

        final OCRDetectionInference d = new OCRDetectionInference();

        String actualFilename = in_labelFileName.split("file:///android_asset/")[1];
        InputStream labelsInput = in_assetManager.open(actualFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            LOGGER.w(line);
            d.mLabels.add(line);
        }
        br.close();

        d.mInputSize = in_inputSize;

        Interpreter.Options tfliteOptions = new Interpreter.Options();
        GpuDelegate gpuDelegate = new GpuDelegate();
        tfliteOptions.addDelegate(gpuDelegate);

        try {
            d.mTfLite = new Interpreter(loadModelFile(in_assetManager, in_modelFileName)
                    /*, tfliteOptions*/); //마지막 파라미터는 GPU 사용시 넣기.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.mIsModelQuantized = in_isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (in_isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.mInputImgByteData = ByteBuffer.allocateDirect(1 * d.mInputSize * d.mInputSize * 3 *
                                                        numBytesPerChannel);
        d.mInputImgByteData.order(ByteOrder.nativeOrder());
        d.mInputImgIntData = new int[d.mInputSize * d.mInputSize];

        LOGGER.i("NUM_THREADS : "+NUM_THREADS);
        d.mTfLite.setNumThreads(NUM_THREADS);
        d.mOutputLocations = new float[1][NUM_DETECTIONS][4];
        d.mOutputClasses = new float[1][NUM_DETECTIONS];
        d.mOutputScores = new float[1][NUM_DETECTIONS];
        d.mNumDetections = new float[1];
        return d;
    }

//    @Override
//    public List<Classifier.Recognition> RecognizeImage(final Bitmap in_bitmap) {
    public void RecognizeImage(final Bitmap in_bitmap, int in_coverOrScoring) {

        if( mFloatOutput == null )
        {
            mFloatOutput = new float[mInputSize * mInputSize];
            mInputMat = new Mat(mInputSize, mInputSize, CvType.CV_32FC1);
            mEightMat = new Mat();
        }
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        in_bitmap.getPixels(mInputImgIntData, 0, in_bitmap.getWidth(), 0, 0,
                            in_bitmap.getWidth(), in_bitmap.getHeight());
        UtilsFile.SaveBitmap(in_bitmap, "mBitmapRecognizeInput.png"); //for debug

        mInputImgByteData.rewind();

        for (int i = 0; i < mInputSize; ++i) {
            for (int j = 0; j < mInputSize; ++j) {
                int pixelValue = mInputImgIntData[i * mInputSize + j];
                if (mIsModelQuantized) {
                    // Quantized model
                    mInputImgByteData.put((byte) ((pixelValue >> 16) & 0xFF));
                    mInputImgByteData.put((byte) ((pixelValue >> 8) & 0xFF));
                    mInputImgByteData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    mInputImgByteData.putFloat(((pixelValue >> 16) & 0xFF));
                    mInputImgByteData.putFloat(((pixelValue >> 8) & 0xFF));
                    mInputImgByteData.putFloat((pixelValue & 0xFF));
                }
            }
        }
        Trace.endSection(); // preprocessBitmap

        int outSize = mInputSize / 4;

        outputScore = new float[1][outSize][outSize][1];
        outputGeometry = new float[1][outSize][outSize][5];

        if( mScoreFloat == null ) {

            mScoreFloat = new float[outSize * outSize];
            mGeoFloat = new float[outSize * outSize * 5];

            mScoreMat = new Mat(outSize, outSize, CV_32FC1);
            mGeomertyMat = new Mat(outSize * 5, outSize, CV_32FC1);
        }

        Object[] inputArray = {mInputImgByteData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputScore);
        outputMap.put(1, outputGeometry);

        // Run the inference call.
        double startTextTime = SystemClock.uptimeMillis();
        mTfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        LOGGER.d("SallyDetect DetectBoxTime : "+(SystemClock.uptimeMillis()-startTextTime));

        int indexCount = 0;
        for(int i = 0; i < outSize; i++)
        {
            for(int j = 0; j < outSize; j++)
            {
                mScoreFloat[indexCount] = outputScore[0][i][j][0];
                indexCount++;
            }
        }

        indexCount = 0;
        for( int k = 0; k < 5; k++ ) {
            for (int i = 0; i < outSize; i++) {
                for (int j = 0; j < outSize; j++) {
                    mGeoFloat[indexCount] = outputGeometry[0][i][j][k];
                    indexCount++;
                }
            }
        }

        mScoreMat.put(0, 0, mScoreFloat);
        mGeomertyMat.put(0, 0, mGeoFloat);

        double startTime = SystemClock.uptimeMillis();

        //TODO : decode 코드 넣기.
        //TextBoxView.setTextLocation(mScoreMat, mGeomertyMat, outSize); TODO 지우기.
        Point[][] result = OCRDetectionDecodeResult.decodeDetectionResult(mScoreMat, mGeomertyMat, outSize, in_coverOrScoring);

        if(result != null) {
            LOGGER.d("SallyDetect decodeDetectionResult() SUCCESS !!");
        }else {
            LOGGER.d("SallyDetect decodeDetectionResult() FAIL !!!!");
        }

        //LOGGER.d("SallyDetect decodeDetectionResult() N of boxes = " + result.length);
    }

    @Override
    public void EnableStatLogging(final boolean in_logStats) {}

    @Override
    public String GetStatString() {
        return "";
    }

    @Override
    public void Close() {LOGGER.d("close!!!");}

    public void SetNumThreads(int in_numThreads) {
        if (mTfLite != null) mTfLite.setNumThreads(in_numThreads);
    }

    @Override
    public void SetUseNNAPI(boolean in_isChecked) {
        if (mTfLite != null) mTfLite.setUseNNAPI(in_isChecked);
    }
}
