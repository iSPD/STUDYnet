package com.ispd.mommybook.imageprocess;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.Core.*;
import static org.opencv.imgcodecs.Imgcodecs.*;
import static org.opencv.imgproc.Imgproc.*;

/**
 * ImageProcessSubtraction
 *
 * @author Daniel
 * @version 1.0
 */
public class ImageProcessSubtraction {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Mat mDebugMat1 = new Mat();
    private Mat mDebugMat2 = new Mat();
    private Mat mDebugMat3 = new Mat();

    /**
     * ImageProcessSubtraction
     *
     */
    public ImageProcessSubtraction() {

    }

    /**
     * DoImageSubtract
     * @param inputImageMat
     * @param targetImageMat
     * @param index
     *
     */

    //구현중
    public void DoImageSubtractCircle(Mat inputImageMat, Mat targetImageMat, int index) {
        Mat subtractMat = new Mat();

        //subtract(targetImageMat, inpuImageMat, subtractMat);

        Rect leftRect = new Rect(0, 0, inputImageMat.width()/2, inputImageMat.height());
        Mat leftMat = new Mat(inputImageMat, leftRect);

        //medianBlur(leftMat, leftMat, 3);
        blur(leftMat, leftMat, new Size(3, 3));
        //Canny(leftMat, leftMat, 0, 100, 3, true);

        //erode(leftMat, leftMat, new Mat(), new Point(-1, -1), 1);
        dilate(leftMat, leftMat, new Mat(), new Point(-1, -1), 1);

        Mat circles = new Mat();
        HoughCircles(leftMat, circles, HOUGH_GRADIENT, 1.0,
                (double)leftMat.rows()/16, // change this value to detect circles with different distances to each other
                100.0, 10.0, leftMat.rows()/2 * 2/3, leftMat.rows()/2 * 14/15); // change the last two parameters
        // (min_radius & max_radius) to detect larger circles

        LOGGER.d("circle size : "+circles.cols());
        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            Point center = new Point(Math.round(c[0]), Math.round(c[1]));
            // circle center
            circle(leftMat, center, 1, new Scalar(127), 3, 8, 0 );
            // circle outline
            int radius = (int) Math.round(c[2]);
            circle(leftMat, center, radius, new Scalar(127), 3, 8, 0 );
        }

////        blur(leftMat, leftMat, new Size(3,3));
//        Canny(leftMat, leftMat, 50, 150, 3, true);
////        //bilateralFilter(leftMat, leftMat, 7, 50, 50);
////        int leftNoZero = countNonZero(leftMat);
//
//        threshold(leftMat, leftMat, 200, 100, THRESH_BINARY);
//
//        List<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//
//        findContours(leftMat, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE);
//        LOGGER.d("[contour] contours : "+contours.size());
//        //leftMat.setTo(new Scalar(0));
////        for(int i = 0; i <  contours.size(); i++) {
////            MatOfPoint2f  NewMtx = new MatOfPoint2f( contours.get(i).toArray() );
////
////            LOGGER.d("NewMtx.size().width * NewMtx.size().height : "+(NewMtx.size().width * NewMtx.size().height));
////
////            if( NewMtx.size().width * NewMtx.size().height > 5 ) {
////                RotatedRect rect = fitEllipse(NewMtx);
////                ellipse(leftMat, rect, new Scalar(255));
////            }
////
////            //drawContours(leftMat, contours, i, new Scalar(255-i*20));
////        }
//
//        for(int i = 0; i <  contours.size(); i++) {
//            MatOfPoint2f  c2f = new MatOfPoint2f( contours.get(i).toArray() );
//            double peri = arcLength(c2f, true);
//            MatOfPoint2f approx = new MatOfPoint2f();
//
//            approxPolyDP(c2f, approx, 0.02 * peri, true);
//
//            Point[] points = approx.toArray();
//
//            MatOfPoint c2i = new MatOfPoint( approx.toArray() );
//            boolean ret = isContourConvex(c2i);
//
//            LOGGER.d("[contour] approx size: " + points.length+", ret : "+ret);
//
////            if( ret == true ) {
//            if( points.length > 10 ) {
//                drawContours(leftMat, contours, i, new Scalar(255));
//            }
//        }

        leftRect = new Rect(0, 0, targetImageMat.width()/2, targetImageMat.height());
        Mat leftTargetMat = new Mat(targetImageMat, leftRect);
        blur(leftTargetMat, leftTargetMat, new Size(3,3));
        Canny(leftTargetMat, leftTargetMat, 0, 150, 3, true);
        //bilateralFilter(leftTargetMat, leftTargetMat, 7, 50, 50);
        int leftTargetNoZero = countNonZero(leftTargetMat);

//        LOGGER.d("DoImageSubtract : "+index+", leftNoZero : "+leftNoZero+", leftTargetNoZero : "+leftTargetNoZero);

//        Rect rightRect = new Rect(inputImageMat.width()/2, 0, inputImageMat.width()/2, inputImageMat.height());
//        Mat rightMat = new Mat(inputImageMat, rightRect);
//        blur(rightMat, rightMat, new Size(3,3));
//        //  Canny(rightMat, rightMat, 0, 150, 3, true);
//        bilateralFilter(rightMat, rightMat, 7, 50, 50);
//        int rightNoZero = countNonZero(rightMat);

//        LOGGER.d("DoImageSubtract : "+index+", leftNoZero : "+leftNoZero+", rightNoZero : "+rightNoZero);

        imwrite("/sdcard/studyNet/DEBUG/leftMat-"+index+".jpg", leftMat);
        imwrite("/sdcard/studyNet/DEBUG/leftTargetMat-"+index+".jpg", leftTargetMat);
    }

    //구현중
    public void DoImageSubtract(Mat inputImageMat, Mat targetImageMat, int index) {

        Mat subtractMat = new Mat();

        Scalar meanValue1 = mean(inputImageMat);
        LOGGER.d("[processCount] mean value1 : %f", meanValue1.val[0]);

        Scalar meanValue2 = mean(targetImageMat);
        LOGGER.d("[processCount] mean value2 : %f", meanValue2.val[0]);

//        if( meanValue2.val[0] > meanValue1.val[0] ) {
//            subtract(targetImageMat, inputImageMat, subtractMat);
//        }
//        else {
//            subtract(inputImageMat, targetImageMat, subtractMat);
//        }

        blur(inputImageMat, inputImageMat, new Size(7,7));
        blur(targetImageMat, targetImageMat, new Size(7,7));

        absdiff(inputImageMat, targetImageMat, subtractMat);
        //subtract(inputImageMat, targetImageMat, subtractMat);

        Scalar meanValue = mean(subtractMat);
        //threshold(subtractMat, subtractMat, (int)(meanValue.val[0]) * 2, 255, THRESH_BINARY);
        threshold(subtractMat, subtractMat, 10, 255, THRESH_BINARY);

        if( index == 1 ) {
            mDebugMat1 = inputImageMat.clone();
            mDebugMat2 = targetImageMat.clone();
            mDebugMat3 = subtractMat.clone();
            mHandler.sendEmptyMessage(0);
        }

        Rect rect = new Rect(0, 0, subtractMat.cols()/2, subtractMat.rows());
        Mat roiMat = new Mat(subtractMat, rect);
        LOGGER.d("[processCount]"+(inputImageMat.cols()*inputImageMat.rows())+", nonZero : "+countNonZero(roiMat)+", sindexL : "+index);


        rect = new Rect(subtractMat.cols()/2, 0, subtractMat.cols()/2, subtractMat.rows());
        Mat roiMat2 = new Mat(subtractMat, rect);
        LOGGER.d("[processCount]"+(inputImageMat.cols()*inputImageMat.rows())+", nonZero2 : "+countNonZero(roiMat2)+", sindexR : "+index);

        imwrite("/sdcard/studyNet/DEBUG/inpuImageMat-"+index+".jpg", inputImageMat);
        imwrite("/sdcard/studyNet/DEBUG/targetImageMat-"+index+".jpg", targetImageMat);
        imwrite("/sdcard/studyNet/DEBUG/subtractMat-"+index+".jpg", subtractMat);
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {


            return false;
        }
    });
}
