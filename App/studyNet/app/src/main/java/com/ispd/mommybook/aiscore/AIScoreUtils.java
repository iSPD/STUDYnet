package com.ispd.mommybook.aiscore;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import static org.opencv.core.Core.flip;
import static org.opencv.core.CvType.CV_32F;
import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;
import static org.opencv.imgproc.Imgproc.warpPerspective;

public class AIScoreUtils {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private float []mCropMatrix = new float[9];
    private float []mLeftMatrix = new float[9];
    private float []mRightMatrix = new float[9];
    private Mat mAlignmentLeftMatrix = new Mat();
    private Mat mAlignmentRightMatrix = new Mat();

    public AIScoreUtils() {

    }

    /**
     * AIScoreManager
     * @param inputImageMat
     */
    public Mat DoImageAlignment(Mat inputImageMat) {
        //얼라인먼트 Matrix가 좌우 반전 돼있음. 이유는 Shader에서 Input이 반전 돼 있기 때문에 그렇게 만듬.
        //Mat inputFlipMat = new Mat();
        //flip(inputImageMat, inputFlipMat, 1);//0: vertical, 1 : horizontal

        //이미지 캡처의 경우 이미 좌우 반전 되어 있음
        Mat inputFlipMat = inputImageMat.clone();

        int captureWidth = inputImageMat.cols();
        int captureHeight = inputImageMat.rows();

        LOGGER.d("captureWidth : %d, captureHeight : %d", captureWidth, captureHeight);

        //얼라인먼트 Matrix 가져오기
        getAlignmentMatrix(captureWidth, captureHeight);
        Mat warpInputMatLeft = new Mat();
        Mat warpInputMatRight = new Mat();

        //좌, 우 각각 펴주기. 좌우가 반전 되서 Matrix입력도 반대임.
//        warpPerspective(inputFlipMat, warpInputMatLeft, mAlignmentRightMatrix, inputFlipMat.size(), INTER_LINEAR);
//        warpPerspective(inputFlipMat, warpInputMatRight, mAlignmentLeftMatrix, inputFlipMat.size(), INTER_LINEAR);
        warpPerspective(inputFlipMat, warpInputMatLeft, mAlignmentRightMatrix, inputFlipMat.size(), INTER_CUBIC);
        warpPerspective(inputFlipMat, warpInputMatRight, mAlignmentLeftMatrix, inputFlipMat.size(), INTER_CUBIC);

        //하나로 합치기
        Rect rect = new Rect(captureWidth/2, 0, captureWidth/2, captureHeight);
        Mat rightRoiTargetMat = new Mat(warpInputMatLeft, rect);
        Mat rightRoiSourceMat = new Mat(warpInputMatRight, rect);

        rightRoiSourceMat.copyTo(rightRoiTargetMat);

        //다시 원래 대로 좌우 반전 시켜주기.
        flip(warpInputMatLeft, warpInputMatLeft, 1);
        return warpInputMatLeft;
    }

    /**
     * getAlignmentMatrix
     * 1. 책의 Edge를 따서 1차적으로 화면 전체로 펴줌. 이때 1280x960 기준으로 책 아래 40픽셀 여유를 줌.
     * 2. 전체 화면이 된 책을 Alignment 하는 좌,우 각각의 Matrix
     * 3. 1번 Matrix와 2번 Matrix를 곱하여 한번에 WarpPerspective 할수 있음
     *
     */
    private void getAlignmentMatrix(int captureWidth, int captureHeight) {
        //Crop Matrix 가져오기. 책의 Edge를 따서 화면 전체로 펴주는 용도
        boolean ret = JniController.getCropMatrixValue(mCropMatrix, captureWidth, captureHeight);
        //Crop된 이미지를 Alignment 하는 Matrix. 좌우 각각 존재
        boolean ret2 = JniController.getAlignmentMatrixValue(mLeftMatrix, mRightMatrix, captureWidth, captureHeight);
        LOGGER.d("getCropMatrixValue : "+ret+", getAlignmentMatrixValue : "+ret2);

        Mat spanMat = new Mat(3, 3, CV_32F);
        spanMat.put(0, 0, mCropMatrix);
        spanMat = spanMat.inv();

        Mat leftMat = new Mat(3, 3, CV_32F);
        leftMat.put(0, 0, mLeftMatrix);
        leftMat = leftMat.inv();

        Mat rightMat = new Mat(3, 3, CV_32F);
        rightMat.put(0, 0, mRightMatrix);
        rightMat = rightMat.inv();

        Mat lastLeftMat = new Mat(3, 3, CV_32F);
        Mat lastRightMat = new Mat(3, 3, CV_32F);

        Mat onesMat = new Mat(3, 3, CV_32F);
        float onesData[] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
        onesMat.put(0, 0, onesData);

        //전체화면용 Matrix와 Alignment를 곱하여 하나로 만들어줌. Matrix 순서 중요함.
        Core.gemm(leftMat, spanMat, 1, onesMat, 0, lastLeftMat, 0);
        Core.gemm(rightMat, spanMat, 1, onesMat, 0, lastRightMat, 0);

//        mAlignmentLeftMatrix = lastLeftMat.inv();
//        mAlignmentRightMatrix = lastRightMat.inv();

        mAlignmentLeftMatrix = lastLeftMat.clone();
        mAlignmentRightMatrix = lastRightMat.clone();
    }

//    /**
//     * AIScoreManager
//     * @param inputImageMat
//     */
//    private Mat doImageAlignment(Mat inputImageMat) {
//        //얼라인먼트 Matrix가 좌우 반전 돼있음. 이유는 Shader에서 Input이 반전 돼 있기 때문에 그렇게 만듬.
//        Mat inputFlipMat = new Mat();
//        flip(inputImageMat, inputFlipMat, 1);//0: vertical, 1 : horizontal
//
//        //얼라인먼트 Matrix 가져오기
//        getAlignmentMatrix();
//        Mat warpInputMatLeft = new Mat();
//        Mat warpInputMatRight = new Mat();
//
//        //좌, 우 각각 펴주기. 좌우가 반전 되서 Matrix입력도 반대임.
//        warpPerspective(inputFlipMat, warpInputMatLeft, mAlignmentRightMatrix, inputFlipMat.size());
//        warpPerspective(inputFlipMat, warpInputMatRight, mAlignmentLeftMatrix, inputFlipMat.size());
//
//        //하나로 합치기
//        Rect rect = new Rect(mPreviewWidth/2, 0, mPreviewWidth/2, mPreviewHeight);
//        Mat rightRoiTargetMat = new Mat(warpInputMatLeft, rect);
//        Mat rightRoiSourceMat = new Mat(warpInputMatRight, rect);
//
//        rightRoiSourceMat.copyTo(rightRoiTargetMat);
//
//        //다시 원래 대로 좌우 반전 시켜주기.
//        flip(warpInputMatLeft, warpInputMatLeft, 1);
//        return warpInputMatLeft;
//    }
//
//    /**
//     * getAlignmentMatrix
//     * 1. 책의 Edge를 따서 1차적으로 화면 전체로 펴줌. 이때 1280x960 기준으로 책 아래 40픽셀 여유를 줌.
//     * 2. 전체 화면이 된 책을 Alignment 하는 좌,우 각각의 Matrix
//     * 3. 1번 Matrix와 2번 Matrix를 곱하여 한번에 WarpPerspective 할수 있음
//     *
//     */
//    private void getAlignmentMatrix() {
//        //Crop Matrix 가져오기. 책의 Edge를 따서 화면 전체로 펴주는 용도
//        JniController.getCropMatrixValue(mCropMatrix);
//        //Crop된 이미지를 Alignment 하는 Matrix. 좌우 각각 존재
//        JniController.getAlignmentMatrixValue(mLeftMatrix, mRightMatrix);
//
//        Mat spanMat = new Mat(3, 3, CV_32F);
//        spanMat.put(0, 0, mCropMatrix);
//        spanMat = spanMat.inv();
//
//        Mat leftMat = new Mat(3, 3, CV_32F);
//        leftMat.put(0, 0, mLeftMatrix);
//        leftMat = leftMat.inv();
//
//        Mat rightMat = new Mat(3, 3, CV_32F);
//        rightMat.put(0, 0, mRightMatrix);
//        rightMat = rightMat.inv();
//
//        Mat lastLeftMat = new Mat(3, 3, CV_32F);
//        Mat lastRightMat = new Mat(3, 3, CV_32F);
//
//        Mat onesMat = new Mat(3, 3, CV_32F);
//        float onesData[] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
//        onesMat.put(0, 0, onesData);
//
//        //전체화면용 Matrix와 Alignment를 곱하여 하나로 만들어줌. Matrix 순서 중요함.
//        Core.gemm(leftMat, spanMat, 1, onesMat, 0, lastLeftMat, 0);
//        Core.gemm(rightMat, spanMat, 1, onesMat, 0, lastRightMat, 0);
//
////        mAlignmentLeftMatrix = lastLeftMat.inv();
////        mAlignmentRightMatrix = lastRightMat.inv();
//
//        mAlignmentLeftMatrix = lastLeftMat.clone();
//        mAlignmentRightMatrix = lastRightMat.clone();
//    }

    private byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }

    public Mat ChangeContrastNBrightness(Mat inputMat, double alpha, int beta) {

        Mat newImageMat = Mat.zeros(inputMat.size(), inputMat.type());

        LOGGER.d(" Basic Linear Transforms ");
        LOGGER.d("-------------------------");
        LOGGER.d("* Enter the alpha value [1.0-3.0]: ");
        LOGGER.d("* Enter the beta value [0-100]: ");

        byte[] imageData = new byte[(int) (inputMat.total()*inputMat.channels())];
        inputMat.get(0, 0, imageData);

        byte[] newImageData = new byte[(int) (newImageMat.total()*newImageMat.channels())];

        for (int y = 0; y < inputMat.rows(); y++) {
            for (int x = 0; x < inputMat.cols(); x++) {
                for (int c = 0; c < inputMat.channels(); c++) {
                    double pixelValue = imageData[(y * inputMat.cols() + x) * inputMat.channels() + c];
                    pixelValue = pixelValue < 0 ? pixelValue + 256 : pixelValue;
                    newImageData[(y * inputMat.cols() + x) * inputMat.channels() + c]
                            = saturate(alpha * pixelValue + beta);
                }
            }
        }
        newImageMat.put(0, 0, newImageData);

        return newImageMat;
    }
}
