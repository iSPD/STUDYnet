package com.ispd.mommybook.imageprocess;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.AKAZE;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;

/**
 * ImageProcessKeyPointMatch
 *
 * @author Daniel
 * @version 1.0
 */
public class ImageProcessKeyPointMatch {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private ORB orb = null;
    private AKAZE akaze = null;
    MatOfKeyPoint kpts1, kpts2;
    Mat desc1, desc2;

    private static Mat mInputMat = new Mat();
    private static Mat mTargetMat = new Mat();

    /**
     * ImageProcessKeyPointMatch
     *
     */
    public ImageProcessKeyPointMatch() {
        akaze = AKAZE.create();
        orb = ORB.create(1200);

        kpts1 = new MatOfKeyPoint();
        kpts2 = new MatOfKeyPoint();
        desc1 = new Mat();
        desc2 = new Mat();
    }

//    public void DoImageKeyPointMatch(Mat inputImageMat, Mat targetIamgeMat, int index) {
//        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
//        DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);;
//        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
//
//        //first image
//        Mat img1 = Highgui.imread("<image1 path>");
//        Mat descriptors1 = new Mat();
//        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
//        detector.detect(img1, keypoints1);
//        descriptor.compute(img1, keypoints1, descriptors1);
//
//        //second image
//        Mat img2 = Highgui.imread("<image2 path>");
//        Mat descriptors2 = new Mat();
//        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
//        detector.detect(img2, keypoints2);
//        descriptor.compute(img2, keypoints2, descriptors2);
//
//        //matcher should include 2 different image's descriptors
//        MatOfDMatch  matches = new MatOfDMatch();
//        matcher.match(descriptors1,descriptors2,matches);
//
//        //feature and connection colors
//        Scalar RED = new Scalar(255,0,0);
//        Scalar GREEN = new Scalar(0,255,0);
//
//        //output image
//        Mat outputImg = new Mat();
//        MatOfByte drawnMatches = new MatOfByte();
//
//        //this will draw all matches, works fine
//        Features2d.drawMatches(img1, keypoints1, img2, keypoints2, matches,
//                outputImg, GREEN, RED,  drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
//    }

    /**
     * DoImageKeyPointMatch
     *
     */
    //구현중
    public int DoImageKeyPointMatch(Mat inputImageMat, Mat targetImageMat, int index) {

        //imwrite("/sdcard/studyNet/DEBUG/inpuImageMat-"+index+".jpg", inputImageMat);
        //imwrite("/sdcard/studyNet/DEBUG/targetImageMat-"+index+".jpg", targetImageMat);

        mInputMat = inputImageMat.clone();
        mTargetMat = targetImageMat.clone();

        return JniController.findKeyPointMatching(mInputMat.getNativeObjAddr(), mTargetMat.getNativeObjAddr(), index);

//        try {
////            //AKAZE akaze = AKAZE.create();
////            ORB orb = ORB.create();
////
////            MatOfKeyPoint kpts1 = new MatOfKeyPoint(), kpts2 = new MatOfKeyPoint();
////            Mat desc1 = new Mat(), desc2 = new Mat();
//
//            orb.detectAndCompute(mInputMat, new Mat(), kpts1, desc1);
//            orb.detectAndCompute(mTargetMat, new Mat(), kpts2, desc2);
//
//            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
//            List<MatOfDMatch> knnMatches = new ArrayList<>();
////            matcher.knnMatch(desc1, desc2, knnMatches, 2);
//            matcher.knnMatch(desc1, desc2, knnMatches, 1);
//
//            float ratioThreshold = 0.8f; // Nearest neighbor matching ratio
//            List<KeyPoint> listOfMatched1 = new ArrayList<>();
//            List<KeyPoint> listOfMatched2 = new ArrayList<>();
//            List<KeyPoint> listOfKeypoints1 = kpts1.toList();
//            List<KeyPoint> listOfKeypoints2 = kpts2.toList();
//
//            LOGGER.d(index+"listOfKeypoints1 : " + listOfKeypoints1.size() + ", listOfKeypoints2 : " + listOfKeypoints2.size());
//            LOGGER.d("knnMatches : " + knnMatches.size());
//
//            for (int i = 0; i < knnMatches.size(); i++) {
//                DMatch[] matches = knnMatches.get(i).toArray();
////                float dist1 = matches[0].distance;
////                float dist2 = matches[1].distance;
////                if (dist1 < ratioThreshold * dist2) {
////                    listOfMatched1.add(listOfKeypoints1.get(matches[0].queryIdx));
////                    listOfMatched2.add(listOfKeypoints2.get(matches[0].trainIdx));
////                }
//
//                listOfMatched1.add(listOfKeypoints1.get(matches[0].queryIdx));
//                listOfMatched2.add(listOfKeypoints2.get(matches[0].trainIdx));
//            }
//
//            LOGGER.d(index+"listOfMatched1 : " + listOfMatched1.size() + ", listOfMatched2 : " + listOfMatched2.size());
//        }
//        catch(Exception e) {
//            LOGGER.d("Exception : "+e.getMessage());
//        }
    }

    public int FindImageKeyPoint(Mat inputImageMat, int index) {

        mInputMat = inputImageMat.clone();

        return JniController.findKeyPoint(mInputMat.getNativeObjAddr(), index);
    }
}
