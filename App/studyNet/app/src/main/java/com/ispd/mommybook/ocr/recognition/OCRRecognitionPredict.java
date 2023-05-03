package com.ispd.mommybook.ocr.recognition;

import android.content.Context;
import android.graphics.Bitmap;

import com.ispd.mommybook.utils.UtilsFile;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static com.ispd.mommybook.ocr.recognition.OCRRecognitionCharDB.CHAR_DICTIONARY_2450;
import static com.ispd.mommybook.ocr.recognition.OCRRecognitionCharDB.CHAR_DICTIONARY_2497;
import static com.ispd.mommybook.ocr.recognition.OCRRecognitionCharDB.CHAR_DICTIONARY_2445;

public class OCRRecognitionPredict {
    private static final UtilsLogger LOGGER = new UtilsLogger();
    Module mModule = null;
    private int mSerialNum = -1;
    private OCRRecognitionReady mOCRRecognitionReady;
    private int mDictIndex = -1;

//    static {
//        System.loadLibrary("opencv_java4");
//    }
    // prediction 관련 파라미터 : in_context, in_threadIdx, in_model

    //dictIndex 0 : 2450 개 char, 1 : 2498 개 char
    public OCRRecognitionPredict(Context in_context, int in_serialNum, String in_model, int dictIndex) {
        mSerialNum = in_serialNum;
        mDictIndex = dictIndex;
        mOCRRecognitionReady = new OCRRecognitionReady(in_serialNum);
        loadModel(in_context, in_model);
    }

    //for Prediction
    private void loadModel(Context in_context, String in_model) {
        try {
            LOGGER.d("SallyRecog Try Model load. serialNum = " + mSerialNum );
            // loading serialized torchscript module from packaged into app android asset model.pt,
            long start = System.currentTimeMillis();
            mModule = Module.load(UtilsFile.GetAssetFilePath(in_context, in_model));
            long end = System.currentTimeMillis();
            LOGGER.d("SallyRecog Module.load End Time (SerialNo=" + mSerialNum + ") : " + (end - start) + "msec");

            LOGGER.d("SallyRecog Model load success. serialNum = " + mSerialNum);
        } catch (IOException e) {
            LOGGER.e("SallyRecog Model load fail. serialNum = " + mSerialNum, e);
        }
    }

    public String PredictWord(Mat in_imageMat) {
        Bitmap predictionInputBmp = mOCRRecognitionReady.ReadyWordDataMatSingle(in_imageMat);
        String predictionResult = predict(predictionInputBmp);
        return predictionResult;
    }

    //for Prediction
    private String predict(Bitmap in_wordBmp) {
        LOGGER.d("SallyRecog predictWord() in_wordBmp =" + in_wordBmp.getWidth() + ", " + in_wordBmp.getHeight());
        int wordBmpLength = in_wordBmp.getWidth() * in_wordBmp.getHeight();
        // float_buffer에 이미지 픽셀을 복사
        // bitmapToFloatBuffer()에서 정규화도 되는듯함.
        // 픽셀단위가 아닌 R,G,B 각각 덩어리로 저장됨.
        final FloatBuffer float_buffer = Tensor.allocateFloatBuffer(3 * wordBmpLength);
        TensorImageUtils.bitmapToFloatBuffer(in_wordBmp, 0, 0, in_wordBmp.getWidth(),
                in_wordBmp.getHeight(),
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                float_buffer, 0);

        //float_buffer의 세개 channel중 한 channel만 다시 float_buffer2에 복사함.
        final FloatBuffer float_buffer2 = Tensor.allocateFloatBuffer(wordBmpLength);

        final float[] dst = new float[float_buffer2.capacity()];
        float_buffer.get(dst, 0, wordBmpLength);
        float_buffer2.put(dst, 0, wordBmpLength);

        // channel 한 개 버퍼를 Tensor 형태로 변환함.
        final Tensor inputTensor = Tensor.fromBlob(
                float_buffer2,
                new long[]{1, 1,
                        in_wordBmp.getHeight(),
                        in_wordBmp.getWidth()
                });

        // make input tensor filled with 0
        int batch_size = 1;  //batch_size 는 1(입력이미지 개수)임. bitmap의 height가 아님.
        final long[] textForPred = new long[]{batch_size, 25 + 1};
        long[] longs = new long[batch_size * (25 + 1)];
        Arrays.fill(longs, (long) 0);
        // 첫번째 인자는 실제 넣을 값이고, 두번째 인자는 빈 버퍼임.
        final Tensor textForPredTensor = Tensor.fromBlob(longs, textForPred);

        //for debug
        long start = System.currentTimeMillis();
        // run the model
        final Tensor outputTensor = mModule.forward(IValue.from(inputTensor), IValue.from(textForPredTensor)).toTensor(); //속도
        // for debug : model run 끝나는 시점 계산
        long end = System.currentTimeMillis();
        LOGGER.d("SallyRecog Recognition Time (SerialNo=" + mSerialNum + ") : " + (end - start) + "msec");

        // getting tensor content as java array of floats
        final float[] scores = outputTensor.getDataAsFloatArray();
        final long shape[] = outputTensor.shape();

        // decode result
        final int rows = (int) shape[1];
        final int columns = (int) shape[2];
        float maxValue;
        float currValue;
        int maxIdx;
        int arrIndex[] = new int[rows];

        for (int row = 0; row < rows; row++) {
            maxValue = -Float.MAX_VALUE;
            maxIdx = -1;
            for (int col = 0; col < columns; col++) {
                currValue = scores[row * columns + col];
                if (currValue > maxValue) {
                    maxValue = currValue;
                    maxIdx = col;
                }
            }
            arrIndex[row] = maxIdx;
        }

        // CTC converter 의 내용을 자바로 구현함.
        String predict = "";
        for (int i = 0; i < arrIndex.length; i++) {
            if (arrIndex[i] != 0) { //결과값 중 0이거나 바로 이전 값과 같은 값은 제외
                if ((i == 0) || (i > 0 && !(arrIndex[i - 1] == arrIndex[i]))) {
                    // 아래 코드에서 ArrayIndexOutOfBoundsException 에러가 뜬다면
                    // char_dict의 정의가 제대로 되어 있는지 봐야함.
                    // 미리 정의한 char_dict에서 가져온 글자를 하나씩 붙여서 문자열을 완성한다.
                    if(mDictIndex == CHAR_DICTIONARY_2497) {
                        predict = predict + OCRRecognitionCharDB.char_dict2497[arrIndex[i] - 1];
                    }
                    else if(mDictIndex == CHAR_DICTIONARY_2445) {
                        predict = predict + OCRRecognitionCharDB.char_dict2445[arrIndex[i] - 1];
                    }
                    else if(mDictIndex == CHAR_DICTIONARY_2450) {
                        predict = predict + OCRRecognitionCharDB.char_dict2450[arrIndex[i] - 1];
                    }
                }
            }
        }
        LOGGER.d("Prediction Result = " + predict);

        mModule.destroy();
        mModule = null;

        return predict;
    }

}
