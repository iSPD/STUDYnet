package com.ispd.mommybook.ocr.recognition;

import android.graphics.Bitmap;

import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.INTER_AREA;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class OCRRecognitionReady {
    private static final UtilsLogger LOGGER = new UtilsLogger();
    private int mSerialNum = -1;

    
    public OCRRecognitionReady(int in_serialNum) {
        mSerialNum = in_serialNum;
    }
    /**
     *
     * @param in_imageMat : word image (that is not camera input)
     * @return Bitmap : resized word image
     */
    public Bitmap ReadyWordDataMatSingle(Mat in_imageMat) {
        LOGGER.d("SallyRecog readyWordDataMatSingle() ");
        long startTime = System.currentTimeMillis();

        Bitmap resizedBmp;
        // 인식정확도 향상을 위해, 단어이미지를 리사이즈함.
        resizedBmp = resizeForPredictionInput(in_imageMat);
        long endTime = System.currentTimeMillis();
        LOGGER.d("SallyRecog ReadyData Time : "+(endTime-startTime));
        return resizedBmp;
    }

    /**
     * 인식 정확도를 높히기 위해, 입력된 단어 이미지를 resize한다.
     * @param in_imgMat 전체이미지(카메라입력)에서 crop된 단어이미지
     * @return
     */
    private Bitmap resizeForPredictionInput(Mat in_imgMat) {
        LOGGER.d("SallyRecog OCRRecognitionWordDataReady Class : resizeForPredictionInput() ");
        Mat mat1 = in_imgMat.clone();

        // 리사이즈를 위해 사이즈 계산:
        //      width,height을 100,32로 고정시 두글자 한글 단어들이 거의 인식이 안됨.
        //      위아래로 눌린 형태일때 인식이 어려워짐.
        int resizeW = (32 * mat1.cols() / mat1.rows());
        int resizeH = 32;
        if (resizeW > 100) {
            resizeW = 100;
        }

        // 이미지를 축소하는 경우에는 INTER_AREA, 확대하는 경우에는 INTER_CUBIC+INTER_LINEAR를 사용하는
        // 것이 좋다
        Imgproc.resize(mat1, mat1, new Size(resizeW, resizeH), 0, 0, INTER_AREA);
        //cvtColor(mat1, mat1, Imgproc.COLOR_RGB2Luv);  //어떻게 변환하는가에 따라 인식 결과가 많이 달라짐.
        cvtColor(mat1, mat1, Imgproc.COLOR_RGBA2GRAY);  //어떻게 변환하는가에 따라 인식 결과가 많이 달라짐.
        imwrite("/sdcard/studyNet/DEBUG/preproc_a"+ mSerialNum +".jpg", mat1);
        // 기타 전처리 루틴 : 엣지 강화, 대비 조정 등
        //bilateral
        Mat dstmat = mat1.clone();
//        Mat dstmat = new Mat();
//        Imgproc.bilateralFilter(mat1, dstmat, -1, 10.0, 10.0);
//        imwrite("/sdcard/studyNet/DEBUG/preproc_b"+mThreadNum+".jpg", dstmat);

//        부등호 인식은 bilateral off, adaptive threshold 11,7 모드가 제일 상태 좋음. but, 인쇄체 숫자가 잘 인식 안됨. 잘리지 않도록 영역 조정해보기
        //adaptive threshold 적용. 연한 손글씨에는 효과적. 인쇄체는 결과가 들쑥날쑥 함. 왜???
//        Imgproc.adaptiveThreshold(dstmat, dstmat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 11, 7);
//        Imgproc.adaptiveThreshold(dstmat, dstmat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 21, 10); //이값은 부등호가 끊기는 문제있음.
//        Imgproc.adaptiveThreshold(mat1, dstmat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 11, 7);

        imwrite("/sdcard/studyNet/DEBUG/preproc_c"+ mSerialNum +".jpg", dstmat);

        Bitmap resizedBmp;
        if (resizeW < 100) {
            //리사이즈 후 width가 100보다 작은 경우 악당->악당당, 있든->있든든 으로 인식되는 현상 해결됨
            Mat mat2 = new Mat();

            Core.copyMakeBorder(dstmat, mat2, 0, 0, 0, 100 - resizeW,
                    Core.BORDER_REPLICATE);
            resizedBmp = Bitmap.createBitmap(mat2.width(), mat2.height(),
                    Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat2, resizedBmp);
            imwrite("/sdcard/studyNet/DEBUG/resizedmat1_"+ mSerialNum +".jpg", mat2);

            mat2.release();

        } else {
            resizedBmp = Bitmap.createBitmap(resizeW, resizeH, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(dstmat, resizedBmp);
            imwrite("/sdcard/studyNet/DEBUG/resizedmat2_"+ mSerialNum +".jpg", dstmat);

        }
        mat1.release();
        dstmat.release();

        return resizedBmp;
    }

}
