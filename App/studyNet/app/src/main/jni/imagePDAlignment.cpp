
#include "imagePDAlignment.h"
#include "imageMapperManager.h"

#include <opencv2/opencv.hpp>
#include <opencv2/core/types.hpp>
#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/core/types_c.h>

#include "opencv2/highgui.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/features2d.hpp"
//Block here for test
#include "opencv2/xfeatures2d.hpp"
#include "opencv2/core/ocl.hpp"

#include <thread>

#define useDEBUG 1

#include <android/log.h>

#define  LOG_TAG    "imagePDAlignment"
#define  LOGUNK(...)  __android_log_print(ANDROID_LOG_UNKNOWN,LOG_TAG,__VA_ARGS__)
#define  LOGDEF(...)  __android_log_print(ANDROID_LOG_DEFAULT,LOG_TAG,__VA_ARGS__)
#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
#if useDEBUG
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#else
#define  LOGD(...)
#endif
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGF(...)  __android_log_print(ANDROID_FATAL_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGS(...)  __android_log_print(ANDROID_SILENT_ERROR,LOG_TAG,__VA_ARGS__)

using namespace std;
using namespace cv;
//Block here for test
using namespace cv::xfeatures2d;

bool gAlignmentDebugOn = true;

static int gPreviewWidth = 1280;
static int gPreviewHeight = 960;

static int GOOD_MATCH_PERCENT = 1.0f;
int gOrbNumber = 1200;

int gUseCurveFix = 1;

//LT, LB, RT, RB 의 순서
int gKeyPointLeftCount[4] = {0, 0, 0, 0};
int gKeyPointRightCount[4] = {0, 0, 0, 0};

std::vector<Point2f> gKeyPointsInputLeft, gKeyPointsRefLeft;
std::vector<Point2f> gKeyPointsInputRight, gKeyPointsRefRight;

float gPDAlignLeftX[4] = {0.0f, 0.0f, 0.0f, 0.0f};
float gPDAlignLeftY[4] = {0.0f, 0.0f, 0.0f, 0.0f};
float gPDAlignRightX[4] = {0.0f, 0.0f, 0.0f, 0.0f};
float gPDAlignRightY[4] = {0.0f, 0.0f, 0.0f, 0.0f};

float gUseAverageGapYLeft[] = {0.f, 0.f, 0.f, 0.f};
float gUseAverageGapYRight[] = {0.f, 0.f, 0.f, 0.f};

//custom value
int gCurrentBookCover = -1;
int gCurrentBookPage = -1;
bool gBookCoverChanged = false;
bool gBookPageChanged = false;

static int gResizeFactorForPDAlignment = 2;
int gBookBelowCropValue = 10*gResizeFactorForPDAlignment;

//Tuning Values
//In case of Tune Value, Start with "m"

//2
float mCurveValueTuneLeft[] = {10.0f, 1.1f, 20.f, 1.05f};
float mCurveValueTuneRight[] = {5.0f, 1.3f, 20.f, 1.35f};

//3
float mCurveVerticalTuneLeft[] = {1.0f, 0.8f};
float mCurveVerticalTuneRight[] = {1.0f, 0.7f};

//4
float mCurveVerticalStartLeft[] = {1.0f, 11.0f, 2.f, 11.0f};
float mCurveVerticalStartRight[] = {1.0f, 11.0f, 2.f, 11.0f};

//5
float mCurveVerticalCurveLeft[] = {8.0f, 1.0f, 20.f, 1.0f, 1.3f};
float mCurveVerticalCurveRight[] = {8.0f, 1.2f, 20.f, 0.8f, 0.5f};

//6
float mWarpRightBottomLeft[] = {3.0f, 3.0f};
float mWarpLeftBottomRight[] = {1.0f, 3.0f};

//7
float mCurveStartByValueLeft[] = {0.0f, 1.0f, 15.f, 1.1f};
float mCurveStartByValueRight[] = {0.0f, 1.0f, 15.f, 1.1f};

Mat gResultLeftMat;
Mat gResultRightMat;

int openBookPageFile(Mat rgbInputMat, Mat &leftOutMat, Mat &rightOutMat, Rect leftRect, Rect rightRect);
bool checkTickValue(int inspectCount, float *queueValues, float currentValue);

void findKeyPointLeftImage(Mat compareMat, Mat referenceMat, bool referenceMatReset)
{
    Mat compareGrayMat;
    //1. 흑백 이미지 만들기
    cvtColor(compareMat, compareGrayMat, COLOR_BGRA2GRAY);

    //2. HSV 이미지 만들기
//    cvtColor(compareMat, compareGrayMat, COLOR_RGBA2RGB);
//
//    cv::cvtColor(compareGrayMat, compareGrayMat, CV_RGB2HSV);
//
//    std::vector<cv::Mat> channels;
//    cv::split(compareGrayMat, channels);
//
//    cv::Mat H = channels[0];
//    cv::Mat S = channels[1];
//    cv::Mat V = channels[2];
//
//    compareGrayMat = V.clone();

    // Variables to store keypoints and descriptors
    std::vector<KeyPoint> keypoints1;
    Mat descriptors1;

    static std::vector<KeyPoint> keypoints2;
    static Mat descriptors2;

//        Ptr<AKAZE> akaze = AKAZE::create();
//        akaze->detectAndCompute(compareGrayMat, Mat(), keypoints1, descriptors1);
//    Ptr<ORB> akaze = ORB::create(gOrbNumber);
//    akaze->detectAndCompute(compareGrayMat, Mat(), keypoints1, descriptors1);
//Block here for test
    Ptr<SURF> surf = SURF::create(100.0);
    surf->detectAndCompute(compareGrayMat, Mat(), keypoints1, descriptors1);
//    Ptr<SIFT> sift = SIFT::create(2000);
//    sift->detectAndCompute(compareGrayMat, Mat(), keypoints1, descriptors1);

    if (referenceMatReset == true) {

        Mat referenceGrayMat;
        //1. 흑백 이미지 만들기
        cvtColor(referenceMat, referenceGrayMat, COLOR_BGR2GRAY);

        //2. HSV 이미지 만들기
//        cv::cvtColor(referenceMat, referenceGrayMat, CV_BGR2HSV);
//
//        std::vector<cv::Mat> channels;
//        cv::split(referenceGrayMat, channels);
//
//        cv::Mat H = channels[0];
//        cv::Mat S = channels[1];
//        cv::Mat V = channels[2];
//
//        referenceGrayMat = V.clone();

//            Ptr<AKAZE> akaze2 = AKAZE::create();
//            akaze2->detectAndCompute(referenceGrayMat, Mat(), keypoints2, descriptors2);
//        Ptr<ORB> akaze2 = ORB::create(gOrbNumber);
//        akaze2->detectAndCompute(referenceGrayMat, Mat(), keypoints2, descriptors2);
        //Block here for test
        Ptr<SURF> surf2 = SURF::create(400.0);
        surf2->detectAndCompute(referenceGrayMat, Mat(), keypoints2, descriptors2);
//        Ptr<SIFT> sift2 = SIFT::create(2000);
//        sift2->detectAndCompute(referenceGrayMat, Mat(), keypoints2, descriptors2);
    }

    LOGD("[BFMatcher-left] keypoints1 : %d, keypoints2 : %d", keypoints1.size(), keypoints2.size());
    LOGD("[BFMatcher-left] descriptors1 : %d, descriptors2 : %d", descriptors1.cols, descriptors2.cols);

    //test
//    gKeyPointsInputLeft.clear();
//    gKeyPointsRefLeft.clear();
//
//    for(int i = 0; i < keypoints1.size(); i++) {
//        gKeyPointsInputLeft.push_back(keypoints1[i].pt);
//    }
//
//    for(int i = 0; i < keypoints1.size(); i++) {
//        gKeyPointsRefLeft.push_back(keypoints1[i].pt);
//    }
//
////    for(int i = 0; i < keypoints2.size(); i++) {
////        gKeyPointsRefLeft.push_back(keypoints2[i].pt);
////    }
//
//    return;
    //test

    if( descriptors2.cols == 0 ) {
        return;
    }

    //BFMatcher matcher(NORM_HAMMING, true);
    //vector<vector<DMatch> > matches;
    //matcher.knnMatch(descriptors1, descriptors2, matches, 1);

    //Block here for test
    BFMatcher matcher(NORM_L1, false);
//    BFMatcher matcher(NORM_L2, false);
//    vector<DMatch> matches;
    vector<vector<DMatch>> matches;
    matcher.knnMatch(descriptors1, descriptors2, matches, 1);

    if (matches.size() == 0) {
        LOGE("Nothing Match Point of Left");
        return;
    }

    std::vector<Point2f> points1, points2;
    gKeyPointLeftCount[0] = 0;
    gKeyPointLeftCount[1] = 0;
    gKeyPointLeftCount[2] = 0;
    gKeyPointLeftCount[3] = 0;

    LOGD("[Removed-KeyPoint-Left1] checkMatches.size() : %d", matches.size());

    std::vector<DMatch> newMatches;
    for (size_t i = 0; i < matches.size(); i++) {
        if (matches[i].size() > 0) {
            DMatch first = matches[i][0];
            newMatches.push_back(first);

//            DMatch second = matches[i][1];
//
//            if(first.distance < second.distance * 0.8f) {
//                newMatches.push_back(first);
//            }
        }
    }
//    for(int i = 0; i < matches.size(); i++) {
//        newMatches.push_back(matches[i]);
//    }

    LOGD("[Removed-KeyPoint-Left1] newMatches.size() : %d", newMatches.size());

    // Sort matches by score
    std::sort(newMatches.begin(), newMatches.end());

    // Remove not so good matches
    const int numGoodMatches = newMatches.size() * GOOD_MATCH_PERCENT;
    newMatches.erase(newMatches.begin() + numGoodMatches, newMatches.end());

    Mat imMatches;
    drawMatches(compareGrayMat, keypoints1, referenceMat, keypoints2, matches, imMatches);
    //imwrite("/sdcard/matches.jpg", imMatches);

    float maxLongDistance = -1.0f;

    float sumDistance1 = 0.0f;
    float sumDistance2 = 0.0f;
    float sumDistance3 = 0.0f;
    float sumDistance4 = 0.0f;

    int totalDistanceCount1 = 0;
    int totalDistanceCount2 = 0;
    int totalDistanceCount3 = 0;
    int totalDistanceCount4 = 0;

    int longDistanceCount = 0;

    //1차 검열
    for (size_t i = 0; i < newMatches.size(); i++) {
        Point inputPoint = keypoints1[newMatches[i].queryIdx].pt;
        Point originalPoint = keypoints2[newMatches[i].trainIdx].pt;

        float distanceKeyPoint = sqrt(pow(originalPoint.x - inputPoint.x, 2) +
                                  pow(originalPoint.y - inputPoint.y, 2));
        float distanceKeyPointX = inputPoint.x - originalPoint.x;
        float distanceKeyPointY = inputPoint.y - originalPoint.y;

        //Tuning Point
        //if ( fabs(distanceKeyPoint) <= 24.f * 1.0f * 1.f/*3.f*/ &&  fabs(distanceKeyPointX) < 32.f  * 1.f) {
        if ( fabs(distanceKeyPoint) <= 576.f / 20.0f && fabs(distanceKeyPointX) <= 320.f / 10.f) {
        //if ( fabs(distanceKeyPoint) <= 576.f / 10.0f && fabs(distanceKeyPointX) <= 320.f / 5.f) {
        //if( true ) {

//            totalDistanceCount++;
//            sumDistance = sumDistance + fabs(distanceKeyPoint);

            if (inputPoint.x < compareMat.cols / 2) {
                if (inputPoint.y < compareMat.rows / 2) {
                    totalDistanceCount1++;
                    sumDistance1 = sumDistance1 + fabs(distanceKeyPoint);
                } else {
                    totalDistanceCount2++;
                    sumDistance2 = sumDistance2 + fabs(distanceKeyPoint);
                }
            } else {
                if (inputPoint.y < compareMat.rows / 2) {
                    totalDistanceCount3++;
                    sumDistance3 = sumDistance3 + fabs(distanceKeyPoint);
                } else {
                    totalDistanceCount4++;
                    sumDistance4 = sumDistance4 + fabs(distanceKeyPoint);
                }
            }

            if(  fabs(distanceKeyPoint) > maxLongDistance )
            {
                maxLongDistance = fabs(distanceKeyPoint);
            }

            points1.push_back(keypoints1[newMatches[i].queryIdx].pt);
            points2.push_back(keypoints2[newMatches[i].trainIdx].pt);
        }
        else
        {
            LOGD("[%d]Long-Distance : %f", i, distanceKeyPoint);
            longDistanceCount++;
        }
    }
//    float averageDistance = sumDistance / (float)totalDistanceCount;

    float averageDistance1 = sumDistance1 / (float)totalDistanceCount1;
    float averageDistance2 = sumDistance2 / (float)totalDistanceCount2;
    float averageDistance3 = sumDistance3 / (float)totalDistanceCount3;
    float averageDistance4 = sumDistance4 / (float)totalDistanceCount4;

    LOGD("[Use-KeyPoint] MaxLong : %f, AverageDistance1 : %f, AverageDistance2 : %f, AverageDistance3 : %f, AverageDistance4 : %f",
            maxLongDistance, averageDistance1, averageDistance2, averageDistance3, averageDistance4);
    LOGD("[Removed-KeyPoint-Left2] points1.size() : %d, longDistanceCount : %d", points1.size(), longDistanceCount );

    //2차 검열
    std::vector<Point2f> new_points1, new_points2;
    for (size_t i = 0; i < points1.size(); i++) {

        int x1_input = (int)(points1[i].x);
        int y1_input = (int)(points1[i].y);

        int x1_original = (int)(points2[i].x);
        int y1_origional = (int)(points2[i].y);

        float distance = sqrt(pow(x1_original - x1_input, 2) +
                                  pow(y1_origional - y1_input, 2));

        //Tuning Point2
//        float margin = 2.0f;
////        if( distance > averageDistance * margin )
////        {
////            points1.erase(points1.begin()+i);
////            points2.erase(points2.begin()+i);
////            continue;
////        }
////
////        if (x1_input < compareMat.cols / 2) {
////            if (y1_input < compareMat.rows / 2) {
////                gKeyPointLeftCount[0]++;
////            } else {
////                gKeyPointLeftCount[1]++;
////            }
////        } else {
////            if (y1_input < compareMat.rows / 2) {
////                gKeyPointLeftCount[2]++;
////            } else {
////                gKeyPointLeftCount[3]++;
////            }
////        }

//        float margin = 5.0f;
//        if( distance < averageDistance * margin )
//        {
//            new_points1.push_back(points1[i]);
//            new_points2.push_back(points2[i]);
//
//            if (x1_input < compareMat.cols / 2) {
//                if (y1_input < compareMat.rows / 2) {
//                    gKeyPointLeftCount[0]++;
//                } else {
//                    gKeyPointLeftCount[1]++;
//                }
//            } else {
//                if (y1_input < compareMat.rows / 2) {
//                    gKeyPointLeftCount[2]++;
//                } else {
//                    gKeyPointLeftCount[3]++;
//                }
//            }
//        }

        float margin = 1.2f;

        if (x1_input < compareMat.cols / 2) {
            if (y1_input < compareMat.rows / 2) {
                if( distance < averageDistance1 * margin ) {
                    new_points1.push_back(points1[i]);
                    new_points2.push_back(points2[i]);

                    gKeyPointLeftCount[0]++;
                }
            } else {
                if( distance < averageDistance2 * margin ) {
                    new_points1.push_back(points1[i]);
                    new_points2.push_back(points2[i]);

                    gKeyPointLeftCount[1]++;
                }
            }
        } else {
            if (y1_input < compareMat.rows / 2) {
                if( distance < averageDistance3 * margin ) {
                    new_points1.push_back(points1[i]);
                    new_points2.push_back(points2[i]);

                    gKeyPointLeftCount[2]++;
                }
            } else {
                if( distance < averageDistance4 * margin ) {
                    new_points1.push_back(points1[i]);
                    new_points2.push_back(points2[i]);

                    gKeyPointLeftCount[3]++;
                }
            }
        }
    }

    LOGD("[Removed-KeyPoint-Left3] new_points1.size() : %d", new_points1.size());

    if (points1.size() == 0) {
        LOGE("Nothing Match Point2 of Left");
        return;
    }

    gKeyPointsInputLeft.clear();
    gKeyPointsRefLeft.clear();

//    gKeyPointsInputLeft.assign(points1.begin(), points1.end());
//    gKeyPointsRefLeft.assign(points2.begin(), points2.end());

    gKeyPointsInputLeft.assign(new_points1.begin(), new_points1.end());
    gKeyPointsRefLeft.assign(new_points2.begin(), new_points2.end());

    return;
}

void findKeyPointRightImage(Mat compareMat, Mat referenceMat, bool referenceMatReset)
{
    Mat compareGrayMat;
    //1. 흑백 이미지 만들기
    cvtColor(compareMat, compareGrayMat, COLOR_BGRA2GRAY);

    //2. HSV 이미지 만들기
//    cvtColor(compareMat, compareGrayMat, COLOR_RGBA2RGB);
//
//    cv::cvtColor(compareGrayMat, compareGrayMat, CV_RGB2HSV);
//
//    std::vector<cv::Mat> channels;
//    cv::split(compareGrayMat, channels);
//
//    cv::Mat H = channels[0];
//    cv::Mat S = channels[1];
//    cv::Mat V = channels[2];
//
//    compareGrayMat = V.clone();

    // Variables to store keypoints and descriptors
    std::vector<KeyPoint> keypoints1;
    Mat descriptors1;

    static std::vector<KeyPoint> keypoints2;
    static Mat descriptors2;

//        Ptr<AKAZE> akaze = AKAZE::create();
//        akaze->detectAndCompute(compareGrayMat, Mat(), keypoints1, descriptors1);
//    Ptr<ORB> akaze = ORB::create(gOrbNumber);
//    akaze->detectAndCompute(compareGrayMat, Mat(), keypoints1, descriptors1);
//Block here for test
    Ptr<SURF> surf = SURF::create(100.0);
    surf->detectAndCompute(compareGrayMat, Mat(), keypoints1, descriptors1);
//    Ptr<SIFT> sift = SIFT::create(2000);
//    sift->detectAndCompute(compareGrayMat, Mat(), keypoints1, descriptors1);

    if (referenceMatReset == true) {
        Mat referenceGrayMat;
        //1. 흑백 이미지 만들기
        cvtColor(referenceMat, referenceGrayMat, COLOR_BGR2GRAY);

        //2. HSV 이미지 만들기
//        cv::cvtColor(referenceMat, referenceGrayMat, CV_BGR2HSV);
//
//        std::vector<cv::Mat> channels;
//        cv::split(referenceGrayMat, channels);
//
//        cv::Mat H = channels[0];
//        cv::Mat S = channels[1];
//        cv::Mat V = channels[2];
//
//        referenceGrayMat = V.clone();

//            Ptr<AKAZE> akaze2 = AKAZE::create();
//            akaze2->detectAndCompute(referenceGrayMat, Mat(), keypoints2, descriptors2);
//        Ptr<ORB> akaze2 = ORB::create(gOrbNumber);
//        akaze2->detectAndCompute(referenceGrayMat, Mat(), keypoints2, descriptors2);
        //Block here for test
        Ptr<SURF> surf2 = SURF::create(400.0);
        surf2->detectAndCompute(referenceGrayMat, Mat(), keypoints2, descriptors2);
//        Ptr<SIFT> sift2 = SIFT::create(2000);
//        sift2->detectAndCompute(referenceGrayMat, Mat(), keypoints2, descriptors2);
    }

    LOGD("[BFMatcher-right] descriptors1 : %d, descriptors2 : %d", descriptors1.cols, descriptors2.cols);

    if( descriptors2.cols == 0 ) {
        return;
    }

//    BFMatcher matcher(NORM_HAMMING, true);
//Block here for test
    BFMatcher matcher(NORM_L1, false);
//    BFMatcher matcher(NORM_L2, false);
    vector<vector<DMatch>> matches;
    matcher.knnMatch(descriptors1, descriptors2, matches, 1);

    if (matches.size() == 0) {
        LOGE("Nothing Match Point of Right");
        return;
    }

    std::vector<Point2f> points1, points2;
    gKeyPointRightCount[0] = 0;
    gKeyPointRightCount[1] = 0;
    gKeyPointRightCount[2] = 0;
    gKeyPointRightCount[3] = 0;

    LOGD("[Removed-KeyPoint-Right1] checkMatches.size() : %d", matches.size());

    std::vector<DMatch> newMatches;

    for (size_t i = 0; i < matches.size(); i++) {
        if (matches[i].size() > 0) {
            DMatch first = matches[i][0];
            newMatches.push_back(first);
        }
    }

    LOGD("[Removed-KeyPoint-Right1] points1.size() : %d", newMatches.size());

    // Sort matches by score
    std::sort(newMatches.begin(), newMatches.end());

    // Remove not so good matches
    const int numGoodMatches = newMatches.size() * GOOD_MATCH_PERCENT;
    newMatches.erase(newMatches.begin() + numGoodMatches, newMatches.end());

    Mat imMatches;
    drawMatches(compareGrayMat, keypoints1, referenceMat, keypoints2, matches, imMatches);
    //imwrite("/sdcard/matches2.jpg", imMatches);

    float maxLongDistance = -1.0f;

    float sumDistance1 = 0.0f;
    float sumDistance2 = 0.0f;
    float sumDistance3 = 0.0f;
    float sumDistance4 = 0.0f;

    int totalDistanceCount1 = 0;
    int totalDistanceCount2 = 0;
    int totalDistanceCount3 = 0;
    int totalDistanceCount4 = 0;

    int longDistanceCount = 0;

    for (size_t i = 0; i < newMatches.size(); i++) {
        Point inputPoint = keypoints1[newMatches[i].queryIdx].pt;
        Point originalPoint = keypoints2[newMatches[i].trainIdx].pt;

        float distanceKeyPoint = sqrt(pow(originalPoint.x - inputPoint.x, 2) +
                          pow(originalPoint.y - inputPoint.y, 2));
        float distanceKeyPointX = inputPoint.x - originalPoint.x;
        float distanceKeyPointY = inputPoint.y - originalPoint.y;

        //Tuning Point
        //if ( fabs(distanceKeyPoint) <= 24.f * 1.f * 1.f/*3.f*/ &&  fabs(distanceKeyPointX) < 32.f  * 1.f) {
        if ( fabs(distanceKeyPoint) <= 576.f / 20.0f && fabs(distanceKeyPointX) <= 320.f / 10.f) {
        //if( true ) {

//            totalDistanceCount++;
//            sumDistance = sumDistance + fabs(distanceKeyPoint);

            if (inputPoint.x < compareMat.cols / 2) {
                if (inputPoint.y < compareMat.rows / 2) {
                    totalDistanceCount1++;
                    sumDistance1 = sumDistance1 + fabs(distanceKeyPoint);
                } else {
                    totalDistanceCount2++;
                    sumDistance2 = sumDistance2 + fabs(distanceKeyPoint);
                }
            } else {
                if (inputPoint.y < compareMat.rows / 2) {
                    totalDistanceCount3++;
                    sumDistance3 = sumDistance3 + fabs(distanceKeyPoint);
                } else {
                    totalDistanceCount4++;
                    sumDistance4 = sumDistance4 + fabs(distanceKeyPoint);
                }
            }

            if( fabs(distanceKeyPoint) > maxLongDistance )
            {
                maxLongDistance = fabs(distanceKeyPoint);
            }

            points1.push_back(keypoints1[newMatches[i].queryIdx].pt);
            points2.push_back(keypoints2[newMatches[i].trainIdx].pt);
        }
        else
        {
            LOGD("[%d]Long-Distance of Right : %f", i, distanceKeyPoint);
            longDistanceCount++;
        }
    }
    //float averageDistance = sumDistance / (float)totalDistanceCount;

    float averageDistance1 = sumDistance1 / (float)totalDistanceCount1;
    float averageDistance2 = sumDistance2 / (float)totalDistanceCount2;
    float averageDistance3 = sumDistance3 / (float)totalDistanceCount3;
    float averageDistance4 = sumDistance4 / (float)totalDistanceCount4;

    LOGD("[Use-KeyPoint-Right0] MaxLong : %f, AverageDistance1 : %f, AverageDistance2 : %f, AverageDistance3 : %f, AverageDistance4 : %f",
         maxLongDistance, averageDistance1, averageDistance2, averageDistance3, averageDistance4);

    LOGD("[Removed-KeyPoint-Right2] points1.size() : %d, longDistanceCount : %d", points1.size(), longDistanceCount );

    //2차 검열
    std::vector<Point2f> new_points1, new_points2;
    for (size_t i = 0; i < points1.size(); i++) {
        int x1_input = (int)(points1[i].x);
        int y1_input = (int)(points1[i].y);

        int x1_original = (int)(points2[i].x);
        int y1_origional = (int)(points2[i].y);

        float distance = sqrt(pow(x1_original - x1_input, 2) +
                                  pow(y1_origional - y1_input, 2));

        //LOGD("[Use-KeyPoint-Right0] distance : %f", distance);

        //Tuning Point2
//        //float margin = 2.0f;
//        float margin = 0.1f;
//        //LOGD("[averageDistanceTest] averageDistance : %f", averageDistance);
//        if( distance > averageDistance * margin )
//        {
//            eraseCount++;
//            points1.erase(points1.begin()+i);
//            points2.erase(points2.begin()+i);
//            continue;
//        }
//
//        if (x1_input < compareMat.cols / 2) {
//            if (y1_input < compareMat.rows / 2) {
//                gKeyPointRightCount[0]++;
//            } else {
//                gKeyPointRightCount[1]++;
//            }
//        } else {
//            if (y1_input < compareMat.rows / 2) {
//                gKeyPointRightCount[2]++;
//            } else {
//                gKeyPointRightCount[3]++;
//            }
//        }

//        float margin = 5.0f;
//        if( distance < averageDistance * margin )
//        {
//            eraseCount++;
//            new_points1.push_back(points1[i]);
//            new_points2.push_back(points2[i]);
//
//            if (x1_input < compareMat.cols / 2) {
//                if (y1_input < compareMat.rows / 2) {
//                    gKeyPointRightCount[0]++;
//                } else {
//                    gKeyPointRightCount[1]++;
//                }
//            } else {
//                if (y1_input < compareMat.rows / 2) {
//                    gKeyPointRightCount[2]++;
//                } else {
//                    gKeyPointRightCount[3]++;
//                }
//            }
//        }

        float margin = 1.2f;

        if (x1_input < compareMat.cols / 2) {
            if (y1_input < compareMat.rows / 2) {
                if( distance < averageDistance1 * margin ) {
                    new_points1.push_back(points1[i]);
                    new_points2.push_back(points2[i]);

                    gKeyPointRightCount[0]++;
                }
            } else {
                if( distance < averageDistance2 * margin ) {
                    new_points1.push_back(points1[i]);
                    new_points2.push_back(points2[i]);

                    gKeyPointRightCount[1]++;
                }
            }
        } else {
            if (y1_input < compareMat.rows / 2) {
                if( distance < averageDistance3 * margin ) {
                    new_points1.push_back(points1[i]);
                    new_points2.push_back(points2[i]);

                    gKeyPointRightCount[2]++;
                }
            } else {
                if( distance < averageDistance4 * margin ) {
                    new_points1.push_back(points1[i]);
                    new_points2.push_back(points2[i]);

                    gKeyPointRightCount[3]++;
                }
            }
        }
    }
    LOGD("[Removed-KeyPoint-Right3] new_points1.size() : %d", new_points1.size());

    if (points1.size() == 0) {
        LOGE("Nothing Match Point2 of Right");
        return;
    }

    gKeyPointsInputRight.clear();
    gKeyPointsRefRight.clear();

//    gKeyPointsInputRight.assign(points1.begin(), points1.end());
//    gKeyPointsRefRight.assign(points2.begin(), points2.end());

    gKeyPointsInputRight.assign(new_points1.begin(), new_points1.end());
    gKeyPointsRefRight.assign(new_points2.begin(), new_points2.end());

    return;
}

void getRegionKeyPointDistance(int alignWidth, int alignHeight,
                            float *averageGapLeftY, float *averageGapLeftX,
                            float *averageGapRightY, float *averageGapRightX,
                            int xIndex, int yIndex)
{
    int splitX = xIndex;
    int splitY = yIndex;

    //left
    int splitCountY[splitX*splitY];
    float splitGapY[splitX*splitY];

    int splitCountX[splitX*splitY];
    float splitGapX[splitX*splitY];

    //right
    int splitCountY2[splitX*splitY];
    float splitGapY2[splitX*splitY];

    int splitCountX2[splitX*splitY];
    float splitGapX2[splitX*splitY];

    for(int i = 0; i < splitX*splitY; i++)
    {
        splitCountY[i] = 0;
        splitGapY[i] = 0.f;
        averageGapLeftY[i] = 0.f;

        splitCountX[i] = 0;
        splitGapX[i] = 0.f;
        averageGapLeftX[i] = 0.f;

        splitCountY2[i] = 0;
        splitGapY2[i] = 0.f;
        averageGapRightY[i] = 0.f;

        splitCountX2[i] = 0;
        splitGapX2[i] = 0.f;
        averageGapRightX[i] = 0.f;
    }

    //left
    for (int i = 0; i < gKeyPointsInputLeft.size(); i++) {

        int indexCount = 0;

        for(int xAxis = 0; xAxis < splitX; xAxis++)
        {
            for(int yAxis = 0; yAxis < splitY; yAxis++)
            {
                if( gKeyPointsInputLeft[i].x >= alignWidth / splitX * xAxis  && gKeyPointsInputLeft[i].x < alignWidth / splitX * (xAxis+1) &&
                        gKeyPointsInputLeft[i].y  >= alignHeight / splitY * yAxis && gKeyPointsInputLeft[i].y  < alignHeight / splitY * (yAxis+1) )
                {
                    splitCountY[indexCount]++;
                    splitGapY[indexCount] += (gKeyPointsInputLeft[i].y - gKeyPointsRefLeft[i].y);

                    splitCountX[indexCount]++;
                    splitGapX[indexCount] += (gKeyPointsInputLeft[i].x - gKeyPointsRefLeft[i].x);
                }
                indexCount++;
            }
        }
    }

    for(int i = 0; i < splitX*splitY; i++)
    {
        if( splitCountY[i] != 0 ) {
            averageGapLeftY[i] = splitGapY[i] / (float) (splitCountY[i]);
        }
        else
        {
            averageGapLeftY[i] = 0;
        }

        if( splitCountX[i] != 0 ) {
            averageGapLeftX[i] = splitGapX[i] / (float) (splitCountX[i]);
        }
        else
        {
            averageGapLeftX[i] = 0;
        }
    }

    //right
    for (int i = 0; i < gKeyPointsInputRight.size(); i++) {

        int indexCount = 0;

        for(int xAxis = 0; xAxis < splitX; xAxis++)
        {
            for(int yAxis = 0; yAxis < splitY; yAxis++)
            {
                if( gKeyPointsInputRight[i].x >= alignWidth / splitX * xAxis  && gKeyPointsInputRight[i].x < alignWidth / splitX * (xAxis+1) &&
                        gKeyPointsInputRight[i].y  >= alignHeight / splitY * yAxis && gKeyPointsInputRight[i].y  < alignHeight / splitY * (yAxis+1) )
                {
                    splitCountY2[indexCount]++;
                    splitGapY2[indexCount] += (gKeyPointsInputRight[i].y - gKeyPointsRefRight[i].y);

                    splitCountX2[indexCount]++;
                    splitGapX2[indexCount] += (gKeyPointsInputRight[i].x - gKeyPointsRefRight[i].x);
                }
                indexCount++;
            }
        }
    }

    for(int i = 0; i < splitX*splitY; i++)
    {
        if( splitCountY2[i] != 0 ) {
            averageGapRightY[i] = splitGapY2[i] / (float) (splitCountY2[i]);
        }
        else
        {
            averageGapRightY[i] = 0;
        }

        if( splitCountX2[i] != 0 ) {
            averageGapRightX[i] = splitGapX2[i] / (float) (splitCountX2[i]);
        }
        else
        {
            averageGapRightX[i] = 0;
        }
    }
}

void getRegionKeyPointDistanceByTrans(int xIndex, int yIndex, Mat transMat,
                        vector<Point2f> refPoints, vector<Point2f> inputPoints,
                        vector<Point2f> &resultPoints, int width, int height,
                        float *averageGapY, float *averageGapX)
{
    double startTime = clock();

    Mat InvMat = transMat.clone();
    int transCount = 0;
    float transData[9];
    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
            float tempValue = InvMat.at<double>(i, j);//row, col
            transData[transCount] = tempValue;
            transCount++;
        }
    }

    float frameCoordinate[3];
    for(int i = 0; i < inputPoints.size(); i++)
    {
        frameCoordinate[0] = inputPoints[i].x;
        frameCoordinate[1] = inputPoints[i].y;
        frameCoordinate[2] = 1.0f;

        float transX = transData[0] * frameCoordinate[0] + transData[1] * frameCoordinate[1] + transData[2] * frameCoordinate[2];
        float transY = transData[3] * frameCoordinate[0] + transData[4] * frameCoordinate[1] + transData[5] * frameCoordinate[2];
        float transZ = transData[6] * frameCoordinate[0] + transData[7] * frameCoordinate[1] + transData[8] * frameCoordinate[2];

        resultPoints[i].x = transX / transZ;
        resultPoints[i].y = transY / transZ;
    }

    int splitX = xIndex;
    int splitY = yIndex;

    //left
    int splitCountY[splitX*splitY];
    float splitGapY[splitX*splitY];

    int splitCountX[splitX*splitY];
    float splitGapX[splitX*splitY];

    for(int i = 0; i < splitX*splitY; i++)
    {
        splitCountY[i] = 0;
        splitGapY[i] = 0.f;
        averageGapY[i] = 0.f;

        splitCountX[i] = 0;
        splitGapX[i] = 0.f;
        averageGapX[i] = 0.f;
    }

    //left
    for (int i = 0; i < resultPoints.size(); i++) {

        int indexCount = 0;

        for(int xAxis = 0; xAxis < splitX; xAxis++)
        {
            for(int yAxis = 0; yAxis < splitY; yAxis++)
            {
                if( resultPoints[i].x >= width / splitX * xAxis  && resultPoints[i].x < width / splitX * (xAxis+1) &&
                    resultPoints[i].y  >= height / splitY * yAxis && resultPoints[i].y  < height / splitY * (yAxis+1) )
                {
                    splitCountY[indexCount]++;
                    splitGapY[indexCount] += (refPoints[i].y - resultPoints[i].y);

                    splitCountX[indexCount]++;
                    splitGapX[indexCount] += (refPoints[i].x - resultPoints[i].x);
                }
                indexCount++;
            }
        }
    }

    for(int i = 0; i < splitX*splitY; i++)
    {
        if( splitCountY[i] != 0 ) {
            averageGapY[i] = splitGapY[i] / (float) (splitCountY[i]);
        }
        else
        {
            averageGapY[i] = 0;
        }

        if( splitCountX[i] != 0 ) {
            averageGapX[i] = splitGapX[i] / (float) (splitCountX[i]);
        }
        else
        {
            averageGapX[i] = 0;
        }
    }

    double endTime = clock();
    LOGD("getCurveFixValuesTime : %lf", (double) (endTime - startTime) / CLOCKS_PER_SEC);
}

Mat doFixCurvedLeftImage(Mat leftImage, float useValueY_1, float useValueY_2, float useValueY_3, float useValueX_1, float useValueX_2)
{
    Mat cloneLeftMat = leftImage.clone();

    LOGD("[doFixCurvedImage1] useValueY_1 : %f, useValueY_2 : %f, useValueY_3 : %f", useValueY_1, useValueY_2, useValueY_3);
    LOGD("[doFixCurvedImage1] useValueX_1 : %f, useValueX_2 : %f", useValueX_1, useValueX_2);

    Mat dstMat = Mat::ones(cloneLeftMat.size(), cloneLeftMat.type());
    dstMat.setTo(127);

    useValueY_3 = useValueY_3 * 1.0f;

    //float useCurvedFactor = useValueY_3 - useValueY_1;
    //float useCurvedFactor = -useValueY_3;
    float useCurvedFactor = useValueY_3;
    float curveX1 = mCurveValueTuneLeft[0];
    float curveY1 = mCurveValueTuneLeft[1];
    float curveX2 = mCurveValueTuneLeft[2];
    float curveY2 = mCurveValueTuneLeft[3];

    float curveA = (curveY2-curveY1) / (curveX2-curveX1);
    float curveB = curveY1 - (curveA * curveX1);

    float weightValue = (curveA * useCurvedFactor) + curveB;
    useCurvedFactor = useCurvedFactor * weightValue;

    LOGD("[doFixCurvedImage1] useCurvedFactor : %f", useCurvedFactor);

    //horizon fix...
    float somethingValue = useCurvedFactor;
    float horiX1 = mCurveStartByValueLeft[0];
    float horiY1 = mCurveStartByValueLeft[1];
    float horiX2 = mCurveStartByValueLeft[2];
    float horiY2 = mCurveStartByValueLeft[3];

    float horiA = (horiY2 - horiY1) / (horiX2 - horiX1);
    float horiB = horiY1 - (horiA * horiX1);

    float horiWeight = (horiA * somethingValue) + horiB;

    LOGD("[doFixCurvedImage1] horiWeight : %f", horiWeight);

    float useCurveVerticalStartLeft0 = mCurveVerticalStartLeft[0] * horiWeight;
    float useCurveVerticalStartLeft2 = mCurveVerticalStartLeft[2] * horiWeight;

    int lastTranY;

    for (int row = 0; row < cloneLeftMat.rows; row++)
    {
        for (int col = 0; col < cloneLeftMat.cols; col++)
        {
            int tansY = 0;
            int tranX = 0;

            if( col > cloneLeftMat.cols - (int)((float)cloneLeftMat.cols * useCurveVerticalStartLeft2 / mCurveVerticalStartLeft[3]) )
            {
                float diffRateY = mCurveVerticalTuneLeft[0] * ((float)row / cloneLeftMat.rows) + mCurveVerticalTuneLeft[1];
                if (diffRateY > 1.0f)
                {
//                    diffRateY = 1.0f;
                }
//                diffRateY = 1.0f;

                float x1 = 0.f;
                float y1 = cloneLeftMat.cols * useCurveVerticalStartLeft0 / mCurveVerticalStartLeft[1];
                float x2 = cloneLeftMat.rows;
                float y2 = cloneLeftMat.cols * useCurveVerticalStartLeft2 / mCurveVerticalStartLeft[3];

                float graphA = (y2-y1) / (x2-x1);
                float graphB = y1 - graphA * x1;

                float calcWidth = graphA * row + graphB;

                float inputValue = ( (float)col - ( (float)cloneLeftMat.cols - calcWidth ) ) / calcWidth;
                if (inputValue < 0.0f) {
                    inputValue = 0.0f;
                }

                float xx1 = mCurveVerticalCurveLeft[0];
                float yy1 = mCurveVerticalCurveLeft[1];
                float xx2 = mCurveVerticalCurveLeft[2];
                float yy2 = mCurveVerticalCurveLeft[3];

                float graphAA = (yy2-yy1)/(xx2-xx1);
                float graphBB = yy1 - graphAA * xx1;

                float graphFactor = graphAA * (useCurvedFactor) + graphBB;

                if( graphFactor > mCurveVerticalCurveLeft[1] )
                {
                    graphFactor = mCurveVerticalCurveLeft[1];
                }
                else if( graphFactor < mCurveVerticalCurveLeft[3] )
                {
                    graphFactor = mCurveVerticalCurveLeft[3];
                }

                float calcY = pow(inputValue, mCurveVerticalCurveLeft[4]) / graphFactor;//1.3f
                //float calcY = sin(pow(1.8f * inputValue, 2)) / 2.4f;
                calcY = calcY * (useCurvedFactor);

                calcY = calcY * diffRateY;
                //tansY = -calcY;
                tansY = calcY;

                lastTranY = tansY;
            }
            else
            {
                tansY = 0;
            }

            if (row + tansY >= 0 && row + tansY < cloneLeftMat.rows && col - tranX < cloneLeftMat.cols)
            {
                Vec4b src = cloneLeftMat.at<Vec4b>(row + tansY, col - tranX);
                dstMat.at<Vec4b>(row, col) = src;
            }
            else
            {
                Vec4b src = Vec4b(0, 0, 0, 255);
                dstMat.at<Vec4b>(row, col) = src;
            }
        }
    }

    LOGD("[doFixCurvedImage1] tansY : %f", lastTranY);

    return dstMat;
}

Mat doFixCurvedRightImage(Mat rightImage, float useValueY_1, float useValueY_2, float useValueY_3, float useValueX_1, float useValueX_2)
{
    Mat cloneRightMat = rightImage.clone();

    LOGD("[doFixCurvedImage2] useValueY_1 : %f, useValueY_2 : %f, useValueY_3 : %f", useValueY_1, useValueY_2, useValueY_3);
    LOGD("[doFixCurvedImage2] useValueX_1 : %f, useValueX_2 : %f", useValueX_1, useValueX_2);

    Mat dstMat = Mat::ones(cloneRightMat.size(), cloneRightMat.type());
    dstMat.setTo(127);

    useValueY_1 = useValueY_1 * 1.0f;

    //float useCurvedFactor = -useValueY_1;
    float useCurvedFactor = useValueY_1;

    float curveX1 = mCurveValueTuneRight[0];
    float curveY1 = mCurveValueTuneRight[1];
    float curveX2 = mCurveValueTuneRight[2];
    float curveY2 = mCurveValueTuneRight[3];

    float curveA = (curveY2-curveY1) / (curveX2-curveX1);
    float curveB = curveY1 - (curveA * curveX1);

    float weightValue = (curveA * useCurvedFactor) + curveB;
    useCurvedFactor = useCurvedFactor * weightValue;

    LOGD("[doFixCurvedImage2] useCurvedFactor : %f", useCurvedFactor);

    //horizon fix...
    float somethingValue = useCurvedFactor;
    float horiX1 = mCurveStartByValueRight[0];//Tune
    float horiY1 = mCurveStartByValueRight[1];
    float horiX2 = mCurveStartByValueRight[2];
    float horiY2 = mCurveStartByValueRight[3];

    float horiA = (horiY2 - horiY1) / (horiX2 - horiX1);
    float horiB = horiY1 - (horiA * horiX1);

    float horiWeight = (horiA * somethingValue) + horiB;

    float useCurveVerticalStartRight0 = mCurveVerticalStartRight[0] * horiWeight;
    float useCurveVerticalStartRight2 = mCurveVerticalStartRight[2] * horiWeight;

    for (int row = 0; row < cloneRightMat.rows; row++)
    {
        for (int col = 0; col < cloneRightMat.cols; col++)
        {
            int tansY = 0;
            int tranX = 0;

            if( col < (int)((float)cloneRightMat.cols * useCurveVerticalStartRight2 / mCurveVerticalStartRight[3]) )
            {
                float diffRateY = mCurveVerticalTuneRight[0] * ((float)row / cloneRightMat.rows) + mCurveVerticalTuneRight[1];
                if (diffRateY > 1.2f)
                {
//                    diffRateY = 1.2f;
                }
//                diffRateY = 1.0f;

                float x1 = 0.f;
                float y1 = cloneRightMat.cols * useCurveVerticalStartRight0 / mCurveVerticalStartRight[1];
                float x2 = cloneRightMat.rows;
                float y2 = cloneRightMat.cols * useCurveVerticalStartRight2 / mCurveVerticalStartRight[3];

                float graphA = (y2-y1) / (x2-x1);
                float graphB = y1 - graphA * x1;

                float calcWidth = graphA * row + graphB;

                float inputValue = (float) col / calcWidth;
                if (inputValue > 1.0f) {
                    inputValue = 1.0f;
                }

                float xx1 = mCurveVerticalCurveRight[0];
                float yy1 = mCurveVerticalCurveRight[1];
                float xx2 = mCurveVerticalCurveRight[2];
                float yy2 = mCurveVerticalCurveRight[3];

                float graphAA = (yy2-yy1)/(xx2-xx1);
                float graphBB = yy1 - graphAA * xx1;

                float graphFactor = graphAA * (useCurvedFactor) + graphBB;

                if( graphFactor > mCurveVerticalCurveRight[1] )
                {
                    graphFactor = mCurveVerticalCurveRight[1];
                }
                else if( graphFactor < mCurveVerticalCurveRight[3])
                {
                    graphFactor = mCurveVerticalCurveRight[3];
                }

                float calcY = (-pow(inputValue, mCurveVerticalCurveRight[4]) + 1.f) / graphFactor;//0.7f, 0.4f : x

                calcY = calcY * (useCurvedFactor);
                calcY = calcY * diffRateY;

                //tansY = -calcY;
                tansY = calcY;
            }
            else
            {
                tansY = 0;
            }

            if (row + tansY >= 0 && row + tansY < cloneRightMat.rows && col - tranX < cloneRightMat.cols)
            {
                Vec4b src = cloneRightMat.at<Vec4b>(row + tansY, col - tranX);
                dstMat.at<Vec4b>(row, col) = src;
            }
            else
            {
                Vec4b src = Vec4b(0, 0, 0, 255);
                dstMat.at<Vec4b>(row, col) = src;
            }
        }
    }
    return dstMat;
}

Mat doPDAlignment(Mat &leftMat, Mat &rightMat,
                    Mat originalLeftMat, Mat originalRightMat,
                    bool doAlign/*For Debug*/) {

    //For Debug
    Mat originalMat;

//    imwrite("/sdcard/originalMat.jpg", originalMat);
    //imwrite("/sdcard/leftMat.jpg", leftMat);
    //imwrite("/sdcard/rightMat.jpg", rightMat);

    if( gAlignmentDebugOn == true ) {
        originalMat = Mat::zeros(
                Size(leftMat.cols * 2, leftMat.rows),
                leftMat.type());

        Mat cvtLeftMat;
        cvtColor(originalLeftMat, cvtLeftMat, CV_BGR2BGRA);

        Mat cvtRightMat;
        cvtColor(originalRightMat, cvtRightMat, CV_BGR2BGRA);

        LOGD("rectError-1");
        Mat leftRoiMat = originalMat(Rect(0, 0, originalLeftMat.cols, originalLeftMat.rows));
        cvtLeftMat.copyTo(leftRoiMat);
        LOGD("rectError-2");
        Mat rightRoiMat = originalMat(
                Rect(originalLeftMat.cols, 0, originalLeftMat.cols, originalLeftMat.rows));
        cvtRightMat.copyTo(rightRoiMat);
    }

    int aligneWidth = leftMat.cols;
    int aligneHeight = rightMat.rows;

    //get 4 regeon keypoint information
    int xIndex = 2;
    int yIndex = 2;

    int splitX = xIndex;
    int splitY = yIndex;

    float averageGapLeftY[xIndex*yIndex];
    float averageGapLeftX[xIndex*yIndex];
    float averageGapRightY[xIndex*yIndex];
    float averageGapRightX[xIndex*yIndex];

    getRegionKeyPointDistance(aligneWidth, aligneHeight,
                          averageGapLeftY, averageGapLeftX,
                          averageGapRightY, averageGapRightX,
                          xIndex, yIndex);

    //Mat oriCopyMat = gOriImage.clone();

//I think just Information
//    leftCurveValue[0] = averageGapLeftY[0];
//    leftCurveValue[1] = averageGapLeftY[1];
//    leftCurveValue[2] = averageGapLeftY[2];
//    leftCurveValue[3] = averageGapLeftY[3];
//    leftCurveValue[4] = averageGapLeftX[0];
//    leftCurveValue[5] = averageGapLeftX[1];
//    leftCurveValue[6] = averageGapLeftX[2];
//    leftCurveValue[7] = averageGapLeftX[3];
//
//    rightCurveValue[0] = averageGapRightY[0];
//    rightCurveValue[1] = averageGapRightY[1];
//    rightCurveValue[2] = averageGapRightY[2];
//    rightCurveValue[3] = averageGapRightY[3];
//    rightCurveValue[4] = averageGapRightX[0];
//    rightCurveValue[5] = averageGapRightX[1];
//    rightCurveValue[6] = averageGapRightX[2];
//    rightCurveValue[7] = averageGapRightX[3];

    if( doAlign == true ) {

        float fraction = 4.0f;//2x2
        float multipleA = 1.f;
        float multipleB = 3.f;//fraction - 1

        int index1 = 0;
        int index2 = 2;
        int index3 = 1;
        int index4 = 3;

        //LEFT..................................
        vector<Point2f> oriPoint(4);
        vector<Point2f> tarPoint(4);

        Mat transMat;

        int width = aligneWidth;
        int height = aligneHeight;

        float leftTopX = (float) width / fraction * multipleA;
        float leftTopY = (float) height / fraction * multipleA;

        float rightTopX = (float) width / fraction * multipleB;
        float rightTopY = (float) height / fraction * multipleA;

        float leftBottomX = (float) width / fraction * multipleA;
        float leftBottomY = (float) height / fraction * multipleB;

        float rightBottomX = (float) width / fraction * multipleB;
        float rightBottomY = (float) height / fraction * multipleB;

        //Tune Value...
//        float rightBottomX = (float) width / fraction * mWarpRightBottomLeft[0];
//        float rightBottomY = (float) height / fraction * mWarpRightBottomLeft[1];

        oriPoint[0] = Point2f(leftTopX, leftTopY);//lt
        oriPoint[1] = Point2f(rightTopX, rightTopY);//rt
        oriPoint[2] = Point2f(leftBottomX, leftBottomY);//lb
        oriPoint[3] = Point2f(rightBottomX, rightBottomY);//rb

        //xAxis 미세조정
        float fixXA1 = averageGapLeftX[index1];
        float fixXB1 = averageGapLeftX[index2];
        float fixXC1 = averageGapLeftX[index3];
        float fixXD1 = averageGapLeftX[index4];

        //필요 없음???
        //KeyPoint 많은 쪽 값의 배수로 계산하기
//        if( mPDAlignmnetLeftKeypointCalc[0] > 0.0f ) {
//            if (gKeyPointLeftCount[0] > gKeyPointLeftCount[1]) {
//                fixXC1 = fixXA1 * mPDAlignmnetLeftKeypointCalc[0];
//            } else {
//                fixXA1 = fixXC1 * mPDAlignmnetLeftKeypointCalc[1];
//            }
//
//            if (gKeyPointLeftCount[2] > gKeyPointLeftCount[3]) {
//                fixXD1 = fixXB1 * mPDAlignmnetLeftKeypointCalc[0];
//            } else {
//                fixXB1 = fixXD1 * mPDAlignmnetLeftKeypointCalc[1];
//            }
//
//            //미세조정
//            fixXA1 *= mPDAlignmnetLeftDetailTune[0];
//            fixXB1 *= mPDAlignmnetLeftDetailTune[1];
//            fixXC1 *= mPDAlignmnetLeftDetailTune[2];//fixXA1 * 1.5f;//averageGapLeftX[index3];
//            fixXD1 *= mPDAlignmnetLeftDetailTune[3];//fixXB1 * 1.5f;//averageGapLeftX[index4];
//        }

        //yAxis
        float fixYA1 = averageGapLeftY[index1];
        float fixYB1 = averageGapLeftY[index2];
        float fixYC1 = averageGapLeftY[index3];
        float fixYD1 = averageGapLeftY[index4];

//        if( fixYA1 < mPDAlignmnetLeftException[0] || fixYA1 > mPDAlignmnetLeftException[1] || fixYC1 < mPDAlignmnetLeftException[2] || fixYC1 > mPDAlignmnetLeftException[3]
//            || fixYB1 < mPDAlignmnetLeftException[0] || fixYB1 > mPDAlignmnetLeftException[1] || fixYD1 < mPDAlignmnetLeftException[2] || fixYD1 > mPDAlignmnetLeftException[3])
//        {
//            fixXA1 = mPDAlignmnetLeftDefault[0];
//            fixXB1 = mPDAlignmnetLeftDefault[1];
//            fixXC1 = mPDAlignmnetLeftDefault[2];
//            fixXD1 = mPDAlignmnetLeftDefault[3];
//
//            fixYA1 = mPDAlignmnetLeftDefault[4];
//            fixYB1 = mPDAlignmnetLeftDefault[5];
//            fixYC1 = mPDAlignmnetLeftDefault[6];
//            fixYD1 = mPDAlignmnetLeftDefault[7];
//        }
//        else {
//            if( mPDAlignmnetLeftKeypointCalc[2] > 0.0f ) {
//                if (gKeyPointLeftCount[0] > gKeyPointLeftCount[1]) {
//                    fixYC1 = fixYA1 * mPDAlignmnetLeftKeypointCalc[2];
//                } else {
//                    fixYA1 = fixYC1 * mPDAlignmnetLeftKeypointCalc[3];
//                }
//
//                if (gKeyPointLeftCount[2] > gKeyPointLeftCount[3]) {
//                    fixYD1 = fixYB1 * mPDAlignmnetLeftKeypointCalc[2];
//                } else {
//                    fixYB1 = fixYD1 * mPDAlignmnetLeftKeypointCalc[3];
//                }
//
//                fixYA1 *= mPDAlignmnetLeftDetailTune[4];
//                fixYB1 *= mPDAlignmnetLeftDetailTune[5];
//                fixYC1 *= mPDAlignmnetLeftDetailTune[6];//fixA1 * 1.5f;//averageGapLeftY[index3];
//                fixYD1 *= mPDAlignmnetLeftDetailTune[7];//fixB1 * 1.5f;//averageGapLeftY[index4];
//            }
//        }

        LOGD("[countCheck] gKeyPointLeftCount : %d %d %d %d", gKeyPointLeftCount[0], gKeyPointLeftCount[1], gKeyPointLeftCount[2], gKeyPointLeftCount[3]);
        LOGD("[countCheck] gKeyPointLeftCountFixValueX : %f %f %f %f", fixXA1, fixXB1, fixXC1, fixXD1);
        LOGD("[countCheck] gKeyPointLeftCountFixValueY : %f %f %f %f", fixYA1, fixYB1, fixYC1, fixYD1);

        if( gCurrentBookCover == 1 && gCurrentBookPage == 4 ) {
            LOGD("[Exception-align] fixYC1 : %f, fixYD1 : %f", fixYC1, fixYD1);
            fixYC1 = fixYC1 * 1.5f;
        }

        gPDAlignLeftX[0] = fixXA1;
        gPDAlignLeftX[1] = fixXB1;
        gPDAlignLeftX[2] = fixXC1;
        gPDAlignLeftX[3] = fixXD1;

        gPDAlignLeftY[0] = fixYA1;
        gPDAlignLeftY[1] = fixYB1;
        gPDAlignLeftY[2] = fixYC1;
        gPDAlignLeftY[3] = fixYD1;

        tarPoint[0] = Point2f(leftTopX-fixXA1, leftTopY-fixYA1);
        tarPoint[1] = Point2f(rightTopX-fixXB1, rightTopY-fixYB1);
        tarPoint[2] = Point2f(leftBottomX-fixXC1, leftBottomY-fixYC1);
        tarPoint[3] = Point2f(rightBottomX-fixXD1, rightBottomY-fixYD1);

        transMat = getPerspectiveTransform(oriPoint, tarPoint);

        int xIndex2 = 3;
        int yIndex2 = 1;

        float averageGapLeftY2[xIndex2*yIndex2];
        float averageGapLeftX2[xIndex2*yIndex2];

        //transMat 이동해서 한번 더 평균값 만들어 내기...
        vector<Point2f> leftFixedPoints(gKeyPointsInputLeft.size());
        getRegionKeyPointDistanceByTrans(xIndex2, yIndex2, transMat, gKeyPointsRefLeft, gKeyPointsInputLeft,
                leftFixedPoints, aligneWidth, aligneHeight, averageGapLeftY2, averageGapLeftX2);

        gUseAverageGapYLeft[0] = averageGapLeftY2[0];
        gUseAverageGapYLeft[1] = averageGapLeftY2[1];
        gUseAverageGapYLeft[2] = averageGapLeftY2[2];
        gUseAverageGapYLeft[3] = averageGapLeftY[index4];//-averageGapLeftY2[2];//-(averageGapLeftY[index4] - averageGapLeftY[index3]);//averageGapLeftY[index4]

        LOGD("[l-value-check] averageGapLeftY2[2] : %f, averageGapLeftY2[1] : %f, averageGapLeftY2[0] : %f", averageGapLeftY2[2], averageGapLeftY2[1], averageGapLeftY2[0]);
        LOGD("[l-value-check] averageGapLeftY[index4] : %f, averageGapLeftY[index3] : %f", averageGapLeftY[index4], averageGapLeftY[index3]);

        if( gUseCurveFix == 1 )
        {
            LOGD("[gUseCurveFix] gUseAverageGapYLeft[2] %f", gUseAverageGapYLeft[2]);
            if( gUseAverageGapYLeft[2] < 1.0f ) {

                float weightA = 0.5f;
                float weightB = 1.5f;

                tarPoint[1].y = tarPoint[1].y + gUseAverageGapYLeft[2] * weightA;//Tuning Points
                tarPoint[3].y = tarPoint[3].y + gUseAverageGapYLeft[2] * weightB;
                transMat = getPerspectiveTransform(oriPoint, tarPoint);

                gPDAlignLeftY[1] = gPDAlignLeftY[1] - (gUseAverageGapYLeft[2] * weightA);//Tune Factor
                gPDAlignLeftY[3] = gPDAlignLeftY[3] - (gUseAverageGapYLeft[2] * weightB);
            }
        }

        if( gAlignmentDebugOn == true ) {
            warpPerspective(leftMat, leftMat, transMat, leftMat.size());
        }

        //RIGHT..................................
        vector<Point2f> oriPoint2(4);
        vector<Point2f> tarPoint2(4);

        Mat transMat2;

        int width2 = aligneWidth;
        int height2 = aligneHeight;

        float leftTopX2 = (float)width2 / fraction * multipleA;
        float leftTopY2 = (float)height2 / fraction * multipleA;

        float rightTopX2 = (float)width2 / fraction * multipleB;
        float rightTopY2 = (float)height2 / fraction * multipleA;

        float leftBottomX2 = (float)width2 / fraction * multipleA;
        float leftBottomY2 = (float)height2 / fraction * multipleB;

//        float leftBottomX2 = (float)width2 / fraction * mWarpLeftBottomRight[0];
//        float leftBottomY2 = (float)height2 / fraction * mWarpLeftBottomRight[1];

        float rightBottomX2 = (float)width2 / fraction * multipleB;
        float rightBottomY2 = (float)height2 / fraction * multipleB;

        oriPoint2[0] = Point2f(leftTopX2, leftTopY2);//lt
        oriPoint2[1] = Point2f(rightTopX2, rightTopY2);//rt
        oriPoint2[2] = Point2f(leftBottomX2, leftBottomY2);//lb
        oriPoint2[3] = Point2f(rightBottomX2, rightBottomY2);//rb

        //xAxis
        float fixXA2 = averageGapRightX[index1];// * mPDAlignmnetRightDetailTune[0];
        float fixXB2 = averageGapRightX[index2];// * mPDAlignmnetRightDetailTune[1];
        float fixXC2 = averageGapRightX[index3];// * mPDAlignmnetRightDetailTune[2];//fixXA1 * 1.5f;//averageGapRightX[index3];
        float fixXD2 = averageGapRightX[index4];// * mPDAlignmnetRightDetailTune[3];//fixXB1 * 1.5f;//averageGapRightX[index4];

        //필요 없음???
//        if( mPDAlignmnetRightKeypointCalc[0] > 0.0f ) {
//            if (gKeyPointRightCount[0] > gKeyPointRightCount[1]) {
//                fixXC2 = fixXA2 * mPDAlignmnetRightKeypointCalc[0];
//            } else {
//                fixXA2 = fixXC2 * mPDAlignmnetRightKeypointCalc[1];
//            }
//
//            if (gKeyPointRightCount[2] > gKeyPointRightCount[3]) {
//                fixXD2 = fixXB2 * mPDAlignmnetRightKeypointCalc[0];
//            } else {
//                fixXB2 = fixXD2 * mPDAlignmnetRightKeypointCalc[1];
//            }
//
//            fixXA2 *= mPDAlignmnetRightDetailTune[0];
//            fixXB2 *= mPDAlignmnetRightDetailTune[1];
//            fixXC2 *= mPDAlignmnetRightDetailTune[2];//fixXA1 * 1.5f;//averageGapRightX[index3];
//            fixXD2 *= mPDAlignmnetRightDetailTune[3];//fixXB1 * 1.5f;//averageGapRightX[index4];
//        }

        //yAxis
        float fixYA2 = averageGapRightY[index1];
        float fixYB2 = averageGapRightY[index2];
        float fixYC2 = averageGapRightY[index3];
        float fixYD2 = averageGapRightY[index4];

//        if( fixYB2 < mPDAlignmnetRightException[0] || fixYB2 > mPDAlignmnetRightException[1] || fixYD2< mPDAlignmnetRightException[2] || fixYD2 > mPDAlignmnetRightException[3]
//            || fixYA2 < mPDAlignmnetRightException[0] || fixYA2 > mPDAlignmnetRightException[1] || fixYC2< mPDAlignmnetRightException[2] || fixYC2 > mPDAlignmnetRightException[3])
//        {
//            fixXA2 = mPDAlignmnetRightDefault[0];
//            fixXB2 = mPDAlignmnetRightDefault[1];
//            fixXC2 = mPDAlignmnetRightDefault[2];
//            fixXD2 = mPDAlignmnetRightDefault[3];
//
//            fixYA2 = mPDAlignmnetRightDefault[4];
//            fixYB2 = mPDAlignmnetRightDefault[5];
//            fixYC2 = mPDAlignmnetRightDefault[6];
//            fixYD2 = mPDAlignmnetRightDefault[7];
//        }
//        else {
//            if( mPDAlignmnetRightKeypointCalc[2] > 0.0f ) {
//                if (gKeyPointRightLeftCount1 > gKeyPointRightLeftCount2) {
//                    fixYC2 = fixYA2 * mPDAlignmnetRightKeypointCalc[2];
//                } else {
//                    fixYA2 = fixYC2 * mPDAlignmnetRightKeypointCalc[3];
//                }
//
//                if (gKeyPointRightRightCount1 > gKeyPointRightRightCount2) {
//                    fixYD2 = fixYB2 * mPDAlignmnetRightKeypointCalc[2];
//                } else {
//                    fixYB2 = fixYD2 * mPDAlignmnetRightKeypointCalc[3];
//                }
//
//                fixYA2 *= mPDAlignmnetRightDetailTune[4];
//                fixYB2 *= mPDAlignmnetRightDetailTune[5];
//                fixYC2 *= mPDAlignmnetRightDetailTune[6];
//                fixYD2 *= mPDAlignmnetRightDetailTune[7];
//            }
//        }

        LOGD("[countCheck] gKeyPointRightCount : %d %d %d %d", gKeyPointRightCount[0], gKeyPointRightCount[1], gKeyPointRightCount[2], gKeyPointRightCount[3]);
        LOGD("[countCheck] gKeyPointRightCountFixValueX : %f %f %f %f", fixXA2, fixXB2, fixXC2, fixXD2);
        LOGD("[countCheck] gKeyPointRightCountFixValueY : %f %f %f %f", fixYA2, fixYB2, fixYC2, fixYD2);

        if( gKeyPointRightCount[3] < 4 ) {

            LOGD("[countCheck] gKeyPointRightCountFixValueON");
            fixXD2 = fixXB2;
            fixYD2 = fixYB2;
        }

        gPDAlignRightX[0] = fixXA2;
        gPDAlignRightX[1] = fixXB2;
        gPDAlignRightX[2] = fixXC2;
        gPDAlignRightX[3] = fixXD2;

        gPDAlignRightY[0] = fixYA2;
        gPDAlignRightY[1] = fixYB2;
        gPDAlignRightY[2] = fixYC2;
        gPDAlignRightY[3] = fixYD2;

        tarPoint2[0] = Point2f(leftTopX2-fixXA2, leftTopY2-fixYA2);
        tarPoint2[1] = Point2f(rightTopX2-fixXB2, rightTopY2-fixYB2);
        tarPoint2[2] = Point2f(leftBottomX2-fixXC2, leftBottomY2-fixYC2);
        tarPoint2[3] = Point2f(rightBottomX2-fixXD2, rightBottomY2-fixYD2);

        transMat2 = getPerspectiveTransform(oriPoint2, tarPoint2);

        float averageGapRightY2[xIndex2*yIndex2];
        float averageGapRightX2[xIndex2*yIndex2];

        vector<Point2f> rightFixedPoints(gKeyPointsInputRight.size());
        getRegionKeyPointDistanceByTrans(xIndex2, yIndex2, transMat2, gKeyPointsRefRight, gKeyPointsInputRight,
                rightFixedPoints, aligneWidth, aligneHeight, averageGapRightY2, averageGapRightX2);

        gUseAverageGapYRight[0] = averageGapRightY[index3];//-averageGapRightY2[0];//-(averageGapRightY[index3] - averageGapRightY[index4]);
        gUseAverageGapYRight[1] = averageGapRightY2[0];
        gUseAverageGapYRight[2] = averageGapRightY2[1];
        gUseAverageGapYRight[3] = averageGapRightY2[2];

        LOGD("[r-value-check] averageGapRightY2[0] : %f, averageGapRightY2[1] : %f, averageGapRightY2[2] : %f", averageGapRightY2[0], averageGapRightY2[1], averageGapRightY2[2]);
        LOGD("[r-value-check] averageGapRightY[index3] : %f, averageGapRightY[index4] : %f", averageGapRightY[index3], averageGapRightY[index4]);

        if( gUseCurveFix == 1 )
        {
            LOGD("[gUseCurveFix] gUseAverageGapYRight[1] %f", gUseAverageGapYRight[1]);
            if( gUseAverageGapYRight[1] < 1.f ) {

                float weightA = 0.5f;
                float weightB = 1.5f;

                tarPoint2[0].y = tarPoint2[0].y + gUseAverageGapYRight[1] * weightA;//Tuning Points
                tarPoint2[2].y = tarPoint2[2].y + gUseAverageGapYRight[1] * weightB;
                transMat2 = getPerspectiveTransform(oriPoint2, tarPoint2);

                gPDAlignRightY[0] = gPDAlignRightY[0] - (gUseAverageGapYRight[1] * weightA);//Tune Factor
                gPDAlignRightY[2] = gPDAlignRightY[2] - (gUseAverageGapYRight[1] * weightB);
            }
        }

        if( gAlignmentDebugOn == true ) {
            warpPerspective(rightMat, rightMat, transMat2, rightMat.size());
        }

        if( gAlignmentDebugOn == true ) {
            Mat cloneLeftMat = doFixCurvedLeftImage(leftMat, gUseAverageGapYLeft[0],
                                                    gUseAverageGapYLeft[1],
                                                    gUseAverageGapYLeft[3], averageGapLeftX2[0],
                                                    averageGapLeftX2[2]);

            Mat cloneRightMat = doFixCurvedRightImage(rightMat, gUseAverageGapYRight[0],
                                                      gUseAverageGapYRight[1],
                                                      gUseAverageGapYRight[3], averageGapRightX2[0],
                                                      averageGapRightX2[2]);

            if (gUseCurveFix == 1) {
                leftMat = cloneLeftMat.clone();
                rightMat = cloneRightMat.clone();
            }
        }

        if( gAlignmentDebugOn == true ) {
            line(leftMat, Point(leftMat.cols / 2, 0), Point(leftMat.cols / 2, leftMat.rows),
                 Scalar(255), 2);
            line(leftMat, Point(0, leftMat.rows / 2), Point(leftMat.cols, leftMat.rows / 2),
                 Scalar(255), 2);

            line(rightMat, Point(rightMat.cols / 2, 0), Point(rightMat.cols / 2, rightMat.rows),
                 Scalar(255), 2);
            line(rightMat, Point(0, rightMat.rows / 2), Point(rightMat.cols, rightMat.rows / 2),
                 Scalar(255), 2);

            for (int i = 0; i < gKeyPointsInputLeft.size(); i++) {
                circle(leftMat, leftFixedPoints[i], 2, Scalar(255, 0, 255, 255), 2);
                circle(originalMat, gKeyPointsRefLeft[i], 4, Scalar(0, 0, 255, 255), 4);

                line(originalMat, leftFixedPoints[i], gKeyPointsRefLeft[i], Scalar(255), 3);
            }

            for (int i = 0; i < gKeyPointsInputRight.size(); i++) {
                circle(rightMat, rightFixedPoints[i], 2, Scalar(255, 0, 255, 255), 2);
                circle(originalMat, Point(gKeyPointsRefRight[i].x + originalMat.cols / 2,
                                          gKeyPointsRefRight[i].y), 4, Scalar(0, 0, 255, 255), 4);

                line(originalMat,
                     Point(rightFixedPoints[i].x + originalMat.cols / 2, rightFixedPoints[i].y),
                     Point(gKeyPointsRefRight[i].x + originalMat.cols / 2, gKeyPointsRefRight[i].y),
                     Scalar(255), 3);
            }
        }
    }
    else // 얼라인먼트 전
    {
        if( gAlignmentDebugOn == true ) {
            //4분할
            if (gUseCurveFix == 0) {
                line(leftMat, Point(leftMat.cols / 2, 0), Point(leftMat.cols / 2, leftMat.rows),
                     Scalar(255), 2);
                line(leftMat, Point(0, leftMat.rows / 2), Point(leftMat.cols, leftMat.rows / 2),
                     Scalar(255), 2);

                line(rightMat, Point(rightMat.cols / 2, 0), Point(rightMat.cols / 2, rightMat.rows),
                     Scalar(255), 2);
                line(rightMat, Point(0, rightMat.rows / 2), Point(rightMat.cols, rightMat.rows / 2),
                     Scalar(255), 2);
            }

            for (int i = 0; i < gKeyPointsInputLeft.size(); i++) {
                circle(leftMat, Point(gKeyPointsInputLeft[i].x, gKeyPointsInputLeft[i].y), 2,
                       Scalar(255, 0, 0), 2);
                circle(originalMat, gKeyPointsRefLeft[i], 4, Scalar(255, 0, 0, 255), 2);

                line(originalMat, Point(gKeyPointsInputLeft[i].x, gKeyPointsInputLeft[i].y),
                     gKeyPointsRefLeft[i], Scalar(255), 3);
            }

            for (int i = 0; i < gKeyPointsInputRight.size(); i++) {
                circle(rightMat, Point(gKeyPointsInputRight[i].x, gKeyPointsInputRight[i].y), 2,
                       Scalar(255, 0, 0), 2);
                circle(originalMat, Point(gKeyPointsRefRight[i].x + originalMat.cols / 2,
                                          gKeyPointsRefRight[i].y), 4, Scalar(255, 0, 0, 255), 2);

                line(originalMat, Point(gKeyPointsInputRight[i].x + originalMat.cols / 2,
                                        gKeyPointsInputRight[i].y),
                     Point(gKeyPointsRefRight[i].x + originalMat.cols / 2, gKeyPointsRefRight[i].y),
                     Scalar(255), 3);
            }
        }
    }

    if( gAlignmentDebugOn == true ) {
        //4분할의 가운데 점에서 거리 재기
        int indexCount = 0;
        for (int xAxis = 0; xAxis < splitX; xAxis++) {
            for (int yAxis = 0; yAxis < splitY; yAxis++) {
                int xAxis1 =
                        ((leftMat.cols / splitX * xAxis) + (leftMat.cols / splitX * (xAxis + 1))) /
                        2;
                int yAxis1 =
                        ((leftMat.rows / splitY * yAxis) + (leftMat.rows / splitY * (yAxis + 1))) /
                        2;

                int xAxis2 =
                        ((leftMat.cols / splitX * xAxis) + (leftMat.cols / splitX * (xAxis + 1))) /
                        2 + averageGapLeftX[indexCount];
                int yAxis2 = yAxis1 + averageGapLeftY[indexCount];

                Scalar color = Scalar(0, 255, 255);
                if (abs(yAxis2 - yAxis1) <= 2 && abs(xAxis2 - xAxis1) <= 2) {
                    color = Scalar(0, 0, 255);
                }

                line(leftMat, Point(xAxis1, yAxis1), Point(xAxis2, yAxis2), color, 10);

                indexCount++;
            }
        }

        int indexCount2 = 0;
        for (int xAxis = 0; xAxis < splitX; xAxis++) {
            for (int yAxis = 0; yAxis < splitY; yAxis++) {
                int xAxis1 = ((rightMat.cols / splitX * xAxis) +
                              (rightMat.cols / splitX * (xAxis + 1))) / 2;
                int yAxis1 = ((rightMat.rows / splitY * yAxis) +
                              (rightMat.rows / splitY * (yAxis + 1))) / 2;

                int xAxis2 = xAxis1 + averageGapRightX[indexCount2];
                int yAxis2 = yAxis1 + averageGapRightY[indexCount2];

                Scalar color = Scalar(0, 255, 255);
                if (abs(yAxis2 - yAxis1) <= 2 && abs(xAxis2 - xAxis1) <= 2) {
                    color = Scalar(0, 0, 255);
                }

                line(rightMat, Point(xAxis1, yAxis1), Point(xAxis2, yAxis2), color, 10);

                indexCount2++;
            }
        }


        Mat resultInputMat = Mat::ones(Size(leftMat.cols * 2, leftMat.rows), leftMat.type());
        LOGD("rectError-3");
        Mat roiLeftMat = resultInputMat(Rect(0, 0, leftMat.cols, leftMat.rows));
        leftMat.copyTo(roiLeftMat);
        LOGD("rectError-4");
        Mat roiRightMat = resultInputMat(Rect(leftMat.cols, 0, leftMat.cols, leftMat.rows));
        rightMat.copyTo(roiRightMat);

        Mat resultMat;
        resultMat = originalMat * 0.5f + resultInputMat * 0.5f;

        return resultMat;
    }
    else {
        static Mat resultMat = Mat(Size(leftMat.cols * 2, leftMat.rows), leftMat.type());
        resultMat.setTo(255);

        return resultMat;
    }
}

Mat alignmentImage(Mat inputMat, int curveFixOn) {

    Mat leftBookFileMat, rightBookFileMat;
    Mat leftInputMat, rightInputMat;

    Size targetSize = inputMat.size();

    LOGD("rectError-5 : %d %d", targetSize.width, targetSize.height);
    Rect cropLeftRect = Rect(0, 0, targetSize.width / 2, targetSize.height);
    LOGD("rectError-6");
    Rect cropRightRect = Rect(targetSize.width / 2, 0, targetSize.width / 2, targetSize.height);

    //get split book Mat from files
    int ret = openBookPageFile(inputMat, leftBookFileMat, rightBookFileMat, cropLeftRect, cropRightRect);

    //get split book Mat from input
    leftInputMat = inputMat(cropLeftRect).clone();
    rightInputMat = inputMat(cropRightRect).clone();

    LOGD("leftBookFileMat : %d %d", leftBookFileMat.cols, leftBookFileMat.rows);
    LOGD("leftInputMat : %d %d", leftInputMat.cols, leftInputMat.rows);

    if( leftBookFileMat.cols == 0 || rightBookFileMat.cols == 0 ) {
        return inputMat;
    }

#ifdef OPENCV_ENABLE_NONFREE
    LOGD("OPENCV_ENABLE_NONFREE");
#endif

    thread leftThread(findKeyPointLeftImage, leftInputMat, leftBookFileMat, ret == 1 ? true : false);
    thread rightThread(findKeyPointRightImage, rightInputMat, rightBookFileMat, ret == 1 ? true : false);

    leftThread.join();
    rightThread.join();

    gUseCurveFix = curveFixOn;

    Mat resultMat = doPDAlignment(leftInputMat, rightInputMat, leftBookFileMat, rightBookFileMat, true/*For Debug*/);
    return resultMat;
}

void getAlignmentMatrixValue(int targetWidth, int targetHeight, int resizeRate,
                            float *alignLeftMat, float *alignRightMat) {

    float fractionWidth = 8.0f;
    float fractionHeight = 4.0f;

    float multipleA = 1.f;
    float multipleB = 3.f;

    int index1 = 0;
    int index2 = 2;
    int index3 = 1;
    int index4 = 3;

    //Left
    vector<Point2f> oriPoint(4);
    vector<Point2f> tarPoint(4);

    Mat transMat;

    int width = targetWidth;
    int height = targetHeight;

    float leftTopX = (float) width / fractionWidth * 5.0f;
    float leftTopY = (float) height / fractionHeight * multipleA;

    float rightTopX = (float) width / fractionWidth * 7.0f;
    float rightTopY = (float) height / fractionHeight * multipleA;

    float leftBottomX = (float) width / fractionWidth * 5.0f;
    float leftBottomY = (float) height / fractionHeight * multipleB;

//    float rightBottomX = (float) width / fractionWidth * mWarpRightBottomLeft[0];
//    float rightBottomY = (float) height / fractionHeight * mWarpRightBottomLeft[1];

    float rightBottomX = (float) width / fractionWidth * 7.0f;
    float rightBottomY = (float) height / fractionHeight * multipleB;

    oriPoint[0] = Point2f(leftTopX, leftTopY);//lt
    oriPoint[1] = Point2f(rightTopX, rightTopY);//rt
    oriPoint[2] = Point2f(leftBottomX, leftBottomY);//lb
    oriPoint[3] = Point2f(rightBottomX, rightBottomY);//rb

    if( targetWidth == 1616 ) {
        LOGD("[warpLog] pre-leftX1 %f %f %f %f %f %f %f %f",
             gPDAlignLeftX[1] * (float) resizeRate,
             gPDAlignLeftX[0] * (float) resizeRate,
             gPDAlignLeftX[3] * (float) resizeRate,
             gPDAlignLeftX[2] * (float) resizeRate,
             gPDAlignLeftY[1] * (float) resizeRate,
             gPDAlignLeftY[0] * (float) resizeRate,
             gPDAlignLeftY[3] * (float) resizeRate,
             gPDAlignLeftY[2] * (float) resizeRate);
    }

    float leftX1 = gPDAlignLeftX[1]*(float)resizeRate*(float)targetWidth/(float)gPreviewWidth;
    float leftX2 = gPDAlignLeftX[0]*(float)resizeRate*(float)targetWidth/(float)gPreviewWidth;
    float leftX3 = gPDAlignLeftX[3]*(float)resizeRate*(float)targetWidth/(float)gPreviewWidth;
    float leftX4 = gPDAlignLeftX[2]*(float)resizeRate*(float)targetWidth/(float)gPreviewWidth;

    leftX1 = -leftX1;
    leftX2 = -leftX2;
    leftX3 = -leftX3;
    leftX4 = -leftX4;

    float leftY1 = gPDAlignLeftY[1]*(float)resizeRate*(float)targetHeight/(float)gPreviewHeight;
    float leftY2 = gPDAlignLeftY[0]*(float)resizeRate*(float)targetHeight/(float)gPreviewHeight;
    float leftY3 = gPDAlignLeftY[3]*(float)resizeRate*(float)targetHeight/(float)gPreviewHeight;
    float leftY4 = gPDAlignLeftY[2]*(float)resizeRate*(float)targetHeight/(float)gPreviewHeight;

    if( targetWidth == 2592 ) {
        LOGD("[warpLog] post-leftX1 %f %f %f %f %f %f %f %f",
             leftX1,
             leftX2,
             leftX3,
             leftX4,
             leftY1,
             leftY2,
             leftY3,
             leftY4);
    }

    if( false && targetWidth == 1280 ) {
        static int inspectCount = 10;
        static float queueLeftX1[10] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queueLeftX2[10] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queueLeftX3[10] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queueLeftX4[10] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queueLeftY1[10] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queueLeftY2[10] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queueLeftY3[10] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queueLeftY4[10] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};

        for (int i = 0; i < inspectCount - 1; i++) {
            queueLeftX1[i] = queueLeftX1[i + 1];
            queueLeftX2[i] = queueLeftX2[i + 1];
            queueLeftX3[i] = queueLeftX3[i + 1];
            queueLeftX4[i] = queueLeftX4[i + 1];
            queueLeftY1[i] = queueLeftY1[i + 1];
            queueLeftY2[i] = queueLeftY2[i + 1];
            queueLeftY3[i] = queueLeftY3[i + 1];
            queueLeftY4[i] = queueLeftY4[i + 1];
        }
        queueLeftX1[inspectCount - 1] = leftX1;
        queueLeftX2[inspectCount - 1] = leftX2;
        queueLeftX3[inspectCount - 1] = leftX3;
        queueLeftX4[inspectCount - 1] = leftX4;
        queueLeftY1[inspectCount - 1] = leftY1;
        queueLeftY2[inspectCount - 1] = leftY2;
        queueLeftY3[inspectCount - 1] = leftY3;
        queueLeftY4[inspectCount - 1] = leftY4;

        float sum1 = 0.0f;
        float sum2 = 0.0f;
        float sum3 = 0.0f;
        float sum4 = 0.0f;
        float sum5 = 0.0f;
        float sum6 = 0.0f;
        float sum7 = 0.0f;
        float sum8 = 0.0f;

        for(int i = 0; i < inspectCount; i++) {
            sum1 += queueLeftX1[i];
            sum2 += queueLeftX2[i];
            sum3 += queueLeftX3[i];
            sum4 += queueLeftX4[i];
            sum5 += queueLeftY1[i];
            sum6 += queueLeftY2[i];
            sum7 += queueLeftY3[i];
            sum8 += queueLeftY4[i];
        }

        leftX1 = sum1 / (float)inspectCount;
        leftX2 = sum2 / (float)inspectCount;
        leftX3 = sum3 / (float)inspectCount;
        leftX4 = sum4 / (float)inspectCount;
        leftY1 = sum5 / (float)inspectCount;
        leftY2 = sum6 / (float)inspectCount;
        leftY3 = sum7 / (float)inspectCount;
        leftY4 = sum8 / (float)inspectCount;

//        bool ret1 = checkTickValue(inspectCount, queueLeftX1, leftX1);
//        bool ret2 = checkTickValue(inspectCount, queueLeftX2, leftX2);
//        bool ret3 = checkTickValue(inspectCount, queueLeftX3, leftX3);
//        bool ret4 = checkTickValue(inspectCount, queueLeftX4, leftX4);
//        bool ret5 = checkTickValue(inspectCount, queueLeftY1, leftY1);
//        bool ret6 = checkTickValue(inspectCount, queueLeftY2, leftY2);
//        bool ret7 = checkTickValue(inspectCount, queueLeftY3, leftY3);
//        bool ret8 = checkTickValue(inspectCount, queueLeftY4, leftY4);
//
//        if( ret1 == false || ret2 == false || ret3 == false || ret4 == false || ret5 == false || ret6 == false || ret7 == false || ret8 == false )
//        {
//            LOGD("pdAlignment is not good");
//            queueLeftX1[inspectCount - 1] = queueLeftX1[inspectCount - 2];
//            queueLeftX2[inspectCount - 1] = queueLeftX2[inspectCount - 2];;
//            queueLeftX3[inspectCount - 1] = queueLeftX3[inspectCount - 2];;
//            queueLeftX4[inspectCount - 1] = queueLeftX4[inspectCount - 2];;
//            queueLeftY1[inspectCount - 1] = queueLeftY1[inspectCount - 2];;
//            queueLeftY2[inspectCount - 1] = queueLeftY2[inspectCount - 2];;
//            queueLeftY3[inspectCount - 1] = queueLeftY3[inspectCount - 2];;
//            queueLeftY4[inspectCount - 1] = queueLeftY4[inspectCount - 2];;
//
//            return;
//        }
    }

    tarPoint[0] = Point2f(leftTopX-leftX1, leftTopY-leftY1);
    tarPoint[1] = Point2f(rightTopX-leftX2, rightTopY-leftY2);
    tarPoint[2] = Point2f(leftBottomX-leftX3, leftBottomY-leftY3);//Tuning??? 1.6f
    tarPoint[3] = Point2f(rightBottomX-leftX4, rightBottomY-leftY4);

    transMat = getPerspectiveTransform(oriPoint, tarPoint);

    //Right
    vector<Point2f> oriPoint2(4);
    vector<Point2f> tarPoint2(4);

    Mat transMat2;

    int width2 = targetWidth;
    int height2 = targetHeight;

    multipleA = 1.0f;
    multipleB = 3.0f;

    float leftTopX2 = (float)width2 / fractionWidth * multipleA;
    float leftTopY2 = (float)height2 / fractionHeight * multipleA;

    float rightTopX2 = (float)width2 / fractionWidth * multipleB;
    float rightTopY2 = (float)height2 / fractionHeight * multipleA;

    float leftBottomX2 = (float)width2 / fractionWidth * mWarpLeftBottomRight[0];
    float leftBottomY2 = (float)height2 / fractionHeight * mWarpLeftBottomRight[1];

    float rightBottomX2 = (float)width2 / fractionWidth * multipleB;
    float rightBottomY2 = (float)height2 / fractionHeight * multipleB;

    oriPoint2[0] = Point2f(leftTopX2, leftTopY2);//lt
    oriPoint2[1] = Point2f(rightTopX2, rightTopY2);//rt
    oriPoint2[2] = Point2f(leftBottomX2, leftBottomY2);//lb
    oriPoint2[3] = Point2f(rightBottomX2, rightBottomY2);//rb

    leftX1 = gPDAlignRightX[1]*(float)resizeRate*(float)targetWidth/(float)gPreviewWidth;
    leftX2 = gPDAlignRightX[0]*(float)resizeRate*(float)targetWidth/(float)gPreviewWidth;
    leftX3 = gPDAlignRightX[3]*(float)resizeRate*(float)targetWidth/(float)gPreviewWidth;
    leftX4 = gPDAlignRightX[2]*(float)resizeRate*(float)targetWidth/(float)gPreviewWidth;

    leftX1 = -leftX1;
    leftX2 = -leftX2;
    leftX3 = -leftX3;
    leftX4 = -leftX4;

    leftY1 = gPDAlignRightY[1]*(float)resizeRate*(float)targetHeight/(float)gPreviewHeight;
    leftY2 = gPDAlignRightY[0]*(float)resizeRate*(float)targetHeight/(float)gPreviewHeight;
    leftY3 = gPDAlignRightY[3]*(float)resizeRate*(float)targetHeight/(float)gPreviewHeight;
    leftY4 = gPDAlignRightY[2]*(float)resizeRate*(float)targetHeight/(float)gPreviewHeight;

    tarPoint2[0] = Point2f(leftTopX2-leftX1, leftTopY2-leftY1);
    tarPoint2[1] = Point2f(rightTopX2-leftX2, rightTopY2-leftY2);
    tarPoint2[2] = Point2f(leftBottomX2-leftX3, leftBottomY2-leftY3);
    tarPoint2[3] = Point2f(rightBottomX2-leftX4, rightBottomY2-leftY4);

    transMat2 = getPerspectiveTransform(oriPoint2, tarPoint2);

    int countUp = 0;

    if (transMat.cols != 0) {
        Mat InvMat = transMat.inv();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                float tempValue = InvMat.at<double>(i, j);
                alignLeftMat[countUp] = tempValue;
                countUp++;
            }
        }
    }

    int countUp2 = 0;
    if (transMat2.cols != 0) {
        Mat InvMat = transMat2.inv();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                float tempValue = InvMat.at<double>(i, j);
                alignRightMat[countUp2] = tempValue;
                countUp2++;
            }
        }
    }
}

void getAlignmentCurveValue(float *leftCurveValue, float *rightCurveValue) {
    for(int i = 0; i < 4; i++ ) {
        leftCurveValue[i] = gUseAverageGapYLeft[i];
        rightCurveValue[i] = gUseAverageGapYRight[i];
    }
}

//-1 : Error, 0 : book is not changed, 1 : book is changed
int openBookPageFile(Mat rgbInputMat, Mat &leftOutMat, Mat &rightOutMat, Rect leftRect, Rect rightRect) {

    //open file of jpg is 3 channels
    if( gResultLeftMat.cols == 0 ) {
        gResultLeftMat = Mat::zeros(Size(rgbInputMat.cols/2, rgbInputMat.rows), CV_8UC3);
        gResultRightMat = Mat::zeros(Size(rgbInputMat.cols/2, rgbInputMat.rows), CV_8UC3);
    }
//    static Mat resultLeftMat = Mat::zeros(Size(rgbInputMat.cols/2, rgbInputMat.rows), CV_8UC3);
//    static Mat resultRightMat = Mat::zeros(Size(rgbInputMat.cols/2, rgbInputMat.rows), CV_8UC3);

    if( gBookCoverChanged == true || gBookPageChanged == true ) {

        //Book Index Error Check
        gBookCoverChanged = false;
        gBookPageChanged = false;

        if (gCurrentBookCover == -1 ||
            gCurrentBookPage == -1 || gCurrentBookCover == 3 ) {

            LOGD("[whyError] Cover or Page is not correct1");
            leftOutMat = NULL;
            rightOutMat = NULL;
            return -1;
        }

        if ((gCurrentBookCover == 0 && gCurrentBookPage == 7) ||
            (gCurrentBookCover == 1 && gCurrentBookPage == 7) ||
            (gCurrentBookCover == 2 && gCurrentBookPage == 15)) {

            LOGD("[whyError] Cover or Page is not correct2");
            leftOutMat = NULL;
            rightOutMat = NULL;
            return -1;
        }
        //Book Index Error Check

        //Book Open
        //string bookFilename("/sdcard/studyNet/");
        char bookFilename[256];
        if (gCurrentBookCover == 0) {
            //bookFilename = bookFilename + "korean/" + to_string(gCurrentBookPage + 1) + ".jpg";
            sprintf(bookFilename, "/sdcard/studyNet/korean/%d.jpg", gCurrentBookPage+1);
        } else if (gCurrentBookCover == 1) {
            //bookFilename = bookFilename + "math/" + to_string(gCurrentBookPage + 1) + ".jpg";
            sprintf(bookFilename, "/sdcard/studyNet/math/%d.jpg", gCurrentBookPage+1);
        } else if (gCurrentBookCover == 2) {
            //bookFilename = bookFilename + "english/" + to_string(gCurrentBookPage + 1) + ".jpg";
            sprintf(bookFilename, "/sdcard/studyNet/english/%d.jpg", gCurrentBookPage+1);
        }

        Size targetSize = rgbInputMat.size();

        LOGD("[whyError] read bookMat");

        Mat bookMat = imread(bookFilename);

        LOGD("[whyError] bookMatSize : %d %d", bookMat.cols, bookMat.rows);
        LOGD("[whyError2] targetSize : %d %d", targetSize.width, targetSize.height);
        LOGD("[whyError2] targetSize2 : %d %d", gBookBelowCropValue, gResizeFactorForPDAlignment);
        LOGD("[whyError2] targetSize3 : %d", (gBookBelowCropValue * (4 / gResizeFactorForPDAlignment)));

        //gBookBelowCropValue 신경쓸것
        Mat resizeBookMat;
        resize(bookMat, resizeBookMat,
                Size(targetSize.width,targetSize.height - (gBookBelowCropValue * (4 / gResizeFactorForPDAlignment) )));

        LOGD("resizeBookMat %d %d", resizeBookMat.cols, resizeBookMat.rows);

        Mat cropBookMat = Mat::zeros(targetSize.height, targetSize.width, bookMat.type());

        LOGD("[whyError] rectError-7");
        Mat targetRoiMat = cropBookMat(Rect(0, 0,
                cropBookMat.cols, cropBookMat.rows - (gBookBelowCropValue * (4 / gResizeFactorForPDAlignment) )));

        LOGD("[whyError] rectError-8");
        resizeBookMat.copyTo(targetRoiMat);

        LOGD("[whyError] rectError-9");
        gResultLeftMat = cropBookMat(leftRect).clone();
        gResultRightMat = cropBookMat(rightRect).clone();

        LOGD("[whyError] rectError-9.5 %d %d %d %d", gResultLeftMat.cols, gResultLeftMat.rows, gResultRightMat.cols, gResultRightMat.rows);

        LOGD("[whyError] rectError-10");
        leftOutMat = gResultLeftMat.clone();
        rightOutMat = gResultRightMat.clone();
        LOGD("[whyError] rectError-11");

        //imwrite("/sdcard/leftOutMat.jpg", leftOutMat);
        //imwrite("/sdcard/rightOutMat.jpg", rightOutMat);

        return 1;
    }

    LOGD("[whyError] rectError-12 %d %d %d %d", gResultLeftMat.cols, gResultLeftMat.rows, gResultRightMat.cols, gResultRightMat.rows);
    LOGD("[whyError] rectError-12.5 %d %d %d %d", leftOutMat.cols, leftOutMat.rows, rightOutMat.cols, rightOutMat.rows);

    leftOutMat = gResultLeftMat.clone();
    rightOutMat = gResultRightMat.clone();
    LOGD("[whyError] rectError-13");

    return 0;
}

void setCurrentBookCoverAndPage(int coverIndex, int pageIndex, int belowCropValue, int resizeFactor) {

    if( gCurrentBookCover != coverIndex ) {
        gCurrentBookCover = coverIndex;

        gBookCoverChanged = true;
    }

    if( gCurrentBookPage != pageIndex ) {
        gCurrentBookPage = pageIndex;

        gBookPageChanged = true;
    }

    gBookBelowCropValue = belowCropValue;//10
    gResizeFactorForPDAlignment = resizeFactor;//1
}

bool checkTickValue(int inspectCount, float *queueValues, float currentValue)
{
    float queueSort[inspectCount];
    for(int i = 0; i < inspectCount; i++)
    {
        queueSort[i] = queueValues[i];
    }

    for (int i = 0; i < inspectCount - 1; i++) {
        for (int j = i + 1; j < inspectCount; j++) {
            if (queueSort[i] > queueSort[j]) {
                float temp = queueSort[i];
                queueSort[i] = queueSort[j];
                queueSort[j] = temp;
            }
        }
    }

    float firstQuartile = (inspectCount + 1) / 4.f;
    float thirdQuartile = firstQuartile * 3.f;

    LOGD("[NormalizeAlignment] firstQuartile : %f thirdQuartile : %f", firstQuartile, thirdQuartile);

    float firstValue = (queueSort[(int) (floor(firstQuartile-1))] +
                        queueSort[(int) (round(firstQuartile-1))]) / 2.0f;
    float thirdValue = (queueSort[(int) (floor(thirdQuartile-1))] +
                        queueSort[(int) (round(thirdQuartile-1))]) / 2.0f;

    LOGD("[NormalizeAlignment] firstValue : %f thirdValue : %f", firstValue, thirdValue);

    float quartileRange = thirdValue - firstValue;
    float minNormalRange = firstValue - 1.5f * quartileRange;
    float maxNormalRange = thirdValue + 1.5f * quartileRange;

    for (int i = 0; i < inspectCount; i++) {
        LOGD("[NormalizeAlignment] queueSort[%d] :  %f", i, queueSort[i]);
    }

    LOGD("[NormalizeAlignment] %f %f", minNormalRange, maxNormalRange);
    if (currentValue < minNormalRange || maxNormalRange < currentValue) {
        LOGD("[NormalizeAlignment] %f is drop", currentValue);
        return false;
    }
    return true;
}

static void getFloatArray(JNIEnv *env, jfloatArray arr, int dataCount, float *outData)
{
    //Step1. Parameter로 받은 java array를 C언어의 array 형태로 변환. 길이 정보도 받아냄
    jfloat *floatArray = env->GetFloatArrayElements(arr, NULL);

    if (floatArray == NULL) {
        return;
    }
    jsize len = env->GetArrayLength(arr);

    //Step2. 배열값에 대한 연산 수행
    for (int i = 0; i < len; i++){
        outData[i] = floatArray[i];
    }

    //Step3. Release array resources
    env->ReleaseFloatArrayElements(arr, floatArray, 0);
}

JNIEXPORT void JNICALL Java_com_ispd_mommybook_JniController_setTuneValues1ForCurve(JNIEnv *env, jobject,
        jfloatArray curveTuneLeft1, jfloatArray curveTuneRight1,
        jfloatArray curveTuneLeft2, jfloatArray curveTuneRight2,
        jfloatArray curveTuneLeft3, jfloatArray curveTuneRight3,
        jfloatArray curveTuneLeft4, jfloatArray curveTuneRight4,
        jfloatArray curveTuneLeft5, jfloatArray curveTuneRight5)
{
//    getFloatArray(env, curveTuneLeft1, 4, mCurveUpMoreLeft);
//    getFloatArray(env, curveTuneRight1, 4, mCurveUpMoreRight);
    getFloatArray(env, curveTuneLeft2, 4, mCurveValueTuneLeft);
    getFloatArray(env, curveTuneRight2, 4, mCurveValueTuneRight);
    getFloatArray(env, curveTuneLeft3, 2, mCurveVerticalTuneLeft);
    getFloatArray(env, curveTuneRight3, 2, mCurveVerticalTuneRight);
    getFloatArray(env, curveTuneLeft4, 4, mCurveVerticalStartLeft);
    getFloatArray(env, curveTuneRight4, 4, mCurveVerticalStartRight);
    getFloatArray(env, curveTuneLeft5, 5, mCurveVerticalCurveLeft);
    getFloatArray(env, curveTuneRight5, 5, mCurveVerticalCurveRight);

    for(int i = 0; i < 5; i++)
    {
        LOGD("[setTuneValues1ForCurve-L] mCurveValueTuneLeft[%d] : %f", i, mCurveValueTuneLeft[i]);
        LOGD("[setTuneValues1ForCurve-R] mCurveValueTuneRight[%d] : %f", i, mCurveValueTuneRight[i]);
    }

    for(int i = 0; i < 5; i++)
    {
        LOGD("mCurveVerticalCurveLeft[%d] : %f", i, mCurveVerticalCurveLeft[i]);
        LOGD("mCurveVerticalCurveRight[%d] : %f", i, mCurveVerticalCurveRight[i]);
    }
}

JNIEXPORT void JNICALL Java_com_ispd_mommybook_JniController_setTuneValues2ForCurve(JNIEnv *env, jobject,
        jfloatArray curveTuneLeft1, jfloatArray curveTuneRight1,
        jfloatArray curveTuneLeft2, jfloatArray curveTuneRight2)
{
    getFloatArray(env, curveTuneLeft1, 2, mWarpRightBottomLeft);
    getFloatArray(env, curveTuneRight1, 2, mWarpLeftBottomRight);
    getFloatArray(env, curveTuneLeft2, 4, mCurveStartByValueLeft);
    getFloatArray(env, curveTuneRight2, 4, mCurveStartByValueRight);

    for(int i = 0; i < 4; i++)
    {
        LOGD("mCurveStartByValueLeft[%d] : %f", i, mCurveStartByValueLeft[i]);
        LOGD("mCurveStartByValueRight[%d] : %f", i, mCurveStartByValueRight[i]);
    }
}