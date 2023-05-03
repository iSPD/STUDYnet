package com.ispd.mommybook;

import org.opencv.core.Mat;

/**
 * Camera1Manager
 *
 * @author Daniel
 * @version 1.0
 */
public class JniController {

    public native static void detectBookEdge(long matAddrInput, long matAddrResult);
    public native static void detectBookEdgeAndPDAlignment(long matAddrInput, long matAddrResult, int debugOption, int curveFixOn);

    public native static boolean getCheckBookLocation();
    public native static void getCropFourPoint(float []fourPoints, float []verticalPoints);
    public native static boolean getCropMatrixValue(float []cropMatrix, int targetWidth, int targetHeight);
    public native static boolean getAlignmentMatrixValue(float []alignLeftMatrix, float []alignRightMatrix, int targetWidth, int targetHeight);
    public native static void getAlignmentCurveValue(float []leftCurveValues, float []rightCurveValues);
    public native static void setCurrentBookInfo(int bookCoverIndex, int bookPageIndex);

    public native static int findKeyPointMatching(long srcMat1, long srcMat2, int index);
    public native static int findKeyPoint(long srcMat1, int index);
    public native static float findCircle(long srcMat, long tarMat, int index, int itemIndex, int useContour);

    public native static void setTuneValues1ForCurve(float[] curveTuneLeft1, float[] curveTuneRight1,
                                                     float[] curveTuneLeft2, float[] curveTuneRight2, float[] curveTuneLeft3, float[] curveTuneRight3,
                                                     float[] curveTuneLeft4, float[] curveTuneRight4, float[] curveTuneLeft5, float[] curveTuneRight5);

    public native static void setTuneValues2ForCurve(float[] curveTuneLeft1, float[] curveTuneRight1,
                                                     float[] curveTuneLeft2, float[] curveTuneRight2);

//    public native static void imageAlignment(long matAddrInput, long matAddrResult, int onOff, int canny1, int canny2, int value1, int value2, int value3, float movingValue);
//
////    static {
////        System.loadLibrary("opencv_java4");
////        System.loadLibrary("native-lib");
////    }
//
//    //T자 그릴 때 필요함
//    public native static int getNeedFactors(int []vPoints, int []hPoints, int []leftPoints, int []rightPoints, int []middlePoints);
//    //T체크
//    public native static boolean getTStatus();
//
//    //영상 펴주기, 데모를 위해 필요함, 포인트 정보
//    public native static void getImageCropInfo(float []cropDatas);
//    //영상 펴주기, 데모를 위해 필요함, Matrix(WarpPerspectvie)
//    public native static void getTransValues(float []transValues);
//
//    //not use
//    public native static void getTransValuesFinger(float []transValues);
//
//    //alignment Matrix(WarpPerspective)
//    public native static void getAlignmentValues3(float []leftValues, float []rightValues, float []checkValues);
//    public native static void getAlignmentValues3ForFinger(float []leftValues, float []rightValues, float []checkValues);
//
//    //현재 페이지 알려주기
//    public native static void setCurrentPage(int whatBook, int whatPage);
//
//    //not use
//    public native static void setTReset(boolean onOff);
//
//    //debugging 용
//    public native static void getKeyPointCount(int []keyPoints);
//
//    //Tuning 정보들
//    public native static void setTuneValues(float split, float tune1, float tune2, float tune3, float tune4, float tune5, float tune6, float tune7, float tune8, float tune9, float tune10, float upCut, int belowCut);
//    public native static void setTuneValues2(float horiStart, float horiEnd, float verStart, float verEnd);
//    public native static void setTuneValues3ForCurve(float[] curveTuneLeft1, float[] curveTuneRight1,
//                                                     float[] curveTuneLeft2, float[] curveTuneRight2, float[] curveTuneLeft3, float[] curveTuneRight3,
//                                                     float[] curveTuneLeft4, float[] curveTuneRight4, float[] curveTuneLeft5, float[] curveTuneRight5);
//
//    public native static void setTuneValues4ForCurve(float[] curveTuneLeft1, float[] curveTuneRight1,
//                                                     float[] curveTuneLeft2, float[] curveTuneRight2);
//
//    public native static void setTuneValues5ForPDAlign(float[] curveTuneLeft1, float[] curveTuneRight1,
//                                                       float[] curveTuneLeft2, float[] curveTuneRight2,
//                                                       float[] curveTuneLeft3, float[] curveTuneRight3,
//                                                       float[] curveTuneLeft4, float[] curveTuneRight4);
//
//
//    //not use
//    public native static float getTuneInfo();
//
//    //커브보정 비교
//    public native static void setCompareOnOff(int onOff);
//
//    //alignment curve보정
//    public native static void getCurvedValue(float []leftCurveValues, float []rightCurveValues);
//
//    //alignment curve보정 정보(Overlay View)
//    public native static void getCurveTuneInfo(float []leftCurveValues, float []rightCurveValues);
}
