#include <jni.h>

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

#include  <vector>
#include  <iostream>
#include  <iomanip>

#include <time.h>
#include <omp.h>

#include <pthread.h>
#include <thread>

#include <string.h>
#include <stdlib.h>

#include "imageEdgeDetect.h"
#include "imagePDAlignment.h"

#define useDEBUG 1

#include <android/log.h>
#define  LOG_TAG    "NDK_TEST"
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

int gPreviewWidthInfo = 1280;
int gPreviewHeightInfo = 960;

int gCaptureTargetWidth = 2592;//1616;
int gCaptureTargetHeight = 1944;//1212;

bool gProcessDone = false;

int gResizeFactorForEdge = 4;
int gResizeFactorForPDAlignment = 2;

//TuneValues
int gBelowCropValue = 10;

//Page Info
int gBookCoverIndex = 0;
int gBookPageIndex = 3;

int gBookPageSaveIndex = -1;
static bool gBookPageChanged = false;

static float gPreviewCropMatrix[9] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
static float gPreviewLeftMatrix[9] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
static float gPreviewRightMatrix[9] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};

static float gCaptureCropMatrix[9] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
static float gCaptureLeftMatrix[9] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
static float gCaptureRightMatrix[9] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};

int gFrameSkipCount = 0;
int gCurveFixOn = 1;

Mat detectBookEdge(Mat inputMat, Size inputSize) {

    Mat resizeMat;

    Mat grayMat;
    Mat HSVMat;

    //1. 흑백 이미지 만들기
    resize(inputMat, resizeMat, Size(inputSize.width/gResizeFactorForEdge, inputSize.height/gResizeFactorForEdge), 0, 0, INTER_AREA );
    cvtColor(resizeMat, grayMat, COLOR_RGBA2GRAY);

    //2. HSV 이미지 만들기
    cvtColor(resizeMat, HSVMat, COLOR_RGBA2RGB);
    //imwrite("/sdcard/grayMat.jpg", HSVMat);

    cv::cvtColor(HSVMat, HSVMat, CV_RGB2HSV);
    //imwrite("/sdcard/hsvMat.jpg", HSVMat);

    std::vector<cv::Mat> channels;
    cv::split(HSVMat, channels);

    cv::Mat H = channels[0];
    cv::Mat S = channels[1];
    cv::Mat V = channels[2];

    HSVMat = V.clone();
    //imwrite("/sdcard/HSVMat.jpg", HSVMat);

//    Scalar avg = mean(grayMat) / 2.0;
//    grayMat = grayMat * 1.5 - avg[0];

//    grayMat = grayMat + 50.0;

    Mat inputDebugMat;
    resize(inputMat, inputDebugMat, Size(inputSize.width/gResizeFactorForEdge, inputSize.height/gResizeFactorForEdge), 0, 0, INTER_AREA );
    //cvtColor(grayMat, inputDebugMat, COLOR_GRAY2BGRA);

    //below tune values
    float bookVerticalMin, bookVerticalMax, bookHoriLengthMin, bookHoriLengthMax;

    if( gBookCoverIndex == 0 || gBookCoverIndex == 1 ) {
        bookVerticalMin = 145.f;//170.f;
        bookVerticalMax = 118.f;//143.f;
        bookHoriLengthMin = 350.f;//380.f;
        bookHoriLengthMax = 300.f;//360.f;

//        bookVerticalMin = 160.f;//170.f;
//        bookVerticalMax = 133.f;//143.f;
//        bookHoriLengthMin = 350.f;//380.f;
//        bookHoriLengthMax = 330.f;//360.f;
    }
    else if( gBookCoverIndex == 2 ) {
        bookVerticalMin = 138.f;//160.f;
        bookVerticalMax = 115.f;//137.f;
        bookHoriLengthMin = 343.f;//373.f;
        bookHoriLengthMax = 297.f;//337.f;
    }

    //detected horizontal & two side Lines
    detectHorizonAndSideEdge(grayMat, HSVMat, bookVerticalMin, bookVerticalMax, inputDebugMat);
    //By detected horizontal & two side Lines, Find Four Corner of Book
    Mat reusultDebugMat = detectFourCorner(grayMat, inputDebugMat, bookHoriLengthMin, bookHoriLengthMax, bookVerticalMin, bookVerticalMax);

    return reusultDebugMat;
}

Mat alignmentBook(Mat cropMat, Size inputSize) {
    Mat alignMat;
    resize(cropMat, alignMat, Size(inputSize.width/gResizeFactorForPDAlignment, inputSize.height/gResizeFactorForPDAlignment), 0, 0, INTER_CUBIC );
    //imwrite("/sdcard/cropMat.jpg", cropMat);

    //below tune values
    gBelowCropValue = 10;//10;
    setCurrentBookCoverAndPage(gBookCoverIndex, gBookPageIndex, gBelowCropValue, gResizeFactorForPDAlignment);//10, 1
    Mat resultMat = alignmentImage(alignMat, gCurveFixOn);

    return resultMat;
}

JNIEXPORT void JNICALL Java_com_ispd_mommybook_JniController_detectBookEdge(JNIEnv *, jclass clazz, jlong matAddrInput, jlong matAddrResult) {
    double duration;
    duration = static_cast<double>(cv::getTickCount());

    Mat &srcMat = *(Mat *) matAddrInput;
    Mat &matResult = *(Mat *) matAddrResult;

    Size inputSize = srcMat.size();

    //1. detect book edge
    Mat reusultDebugMat = detectBookEdge(srcMat, inputSize);

    resize(reusultDebugMat, reusultDebugMat, inputSize);
    matResult = reusultDebugMat;

    //Tune Factor
//    resize(reusultDebugMat, reusultDebugMat, inputSize);
//    Mat cropMat = warpPerspectiveByDetectedFourCorner(reusultDebugMat, gResizeFactorForEdge, gBelowCropValue);

//    Mat cropMat = warpPerspectiveByDetectedFourCorner(srcMat, gResizeFactorForEdge, gBelowCropValue);
//
//    resize(cropMat, cropMat, inputSize);
//    matResult = cropMat;

    LOGI("[TimeCheck] detectBookEdgeTime : %f ms", (static_cast<double>(cv::getTickCount())-duration) / cv::getTickFrequency() * 1000.f);
}

JNIEXPORT void JNICALL Java_com_ispd_mommybook_JniController_detectBookEdgeAndPDAlignment(JNIEnv *, jclass clazz, jlong matAddrInput, jlong matAddrResult, jint debugOption, jint curveFixOn) {

    try {
        LOGD("gBookCoverIndex : %d, gBookPageIndex : %d", gBookCoverIndex, gBookPageIndex);

        bool frameSkipOn = false;
        gFrameSkipCount++;
        if( gFrameSkipCount % 2 == 0 ) {
            frameSkipOn = true;
            if( gBookPageChanged == false ) {
                LOGD("gFrameSkipCount : %d, gBookPageChanged : %d", gFrameSkipCount, gBookPageChanged);
                return;
            }
        }

        gCurveFixOn = curveFixOn;

        if( gBookPageChanged == true ){
            gBookPageChanged = false;
        }

        gProcessDone = false;

        double duration;
        duration = static_cast<double>(cv::getTickCount());

        Mat &srcMat = *(Mat *) matAddrInput;
        Mat &matResult = *(Mat *) matAddrResult;

        Size inputSize = srcMat.size();

        //1. detect book edge
        Mat reusultDebugMat = detectBookEdge(srcMat, inputSize);

        if( debugOption == 0 ) {
            resize(reusultDebugMat, reusultDebugMat, inputSize);
            matResult = reusultDebugMat;

            return;
        }

        //Tune Factor
        gBelowCropValue = 20;//10;//10;
//    resize(reusultDebugMat, reusultDebugMat, inputSize);
//    Mat cropMat = warpPerspectiveByDetectedFourCorner(reusultDebugMat, gResizeFactorForEdge, gBelowCropValue);

        Mat cropMat = warpPerspectiveByDetectedFourCorner(srcMat, gResizeFactorForEdge,
                                                          gBelowCropValue);

        if( debugOption == 1 ) {
            resize(cropMat, cropMat, inputSize);
            matResult = cropMat;

            return;
        }

        LOGI("[TimeCheck] detectBookEdgeTime : %f ms",
             (static_cast<double>(cv::getTickCount()) - duration) / cv::getTickFrequency() *
             1000.f);

        duration = static_cast<double>(cv::getTickCount());

        //2. do book alignment
        Mat alignedMat = alignmentBook(cropMat, inputSize);

        //3. save crop & alignment info
        getCropMatrixValue(gPreviewCropMatrix, gBelowCropValue, gPreviewWidthInfo, gPreviewHeightInfo);
        getAlignmentMatrixValue(gPreviewWidthInfo, gPreviewHeightInfo, gResizeFactorForPDAlignment,
                                gPreviewLeftMatrix, gPreviewRightMatrix);

        getCropMatrixValue(gCaptureCropMatrix, gBelowCropValue, gCaptureTargetWidth, gCaptureTargetHeight);
        getAlignmentMatrixValue(gCaptureTargetWidth, gCaptureTargetHeight, gResizeFactorForPDAlignment,
                                gCaptureLeftMatrix, gCaptureRightMatrix);

        LOGI("[TimeCheck] alignmentBookTime : %f ms",
             (static_cast<double>(cv::getTickCount()) - duration) / cv::getTickFrequency() *
             1000.f);

        resize(alignedMat, alignedMat, inputSize);
        matResult = alignedMat;

        gProcessDone = true;
    }
    catch( Exception& e ) {
        const char* err_msg = e.what();
        LOGE("exception caught %s", err_msg);
    }
}

void getFloatArray(JNIEnv *env, jfloatArray arr, int dataCount, float *outData)
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

void setFloatArray(JNIEnv *env, jfloatArray outData, int dataCount, float *inputData)
{
    jfloat *floatArray = env->GetFloatArrayElements(outData, 0);
    if (floatArray == 0) {
        return;
    }
    jsize len = env->GetArrayLength(outData);

    for (int i = 0; i < dataCount; i++) {
        //LOGD("[tuning-test] float inputData[%d] : %f", i, inputData[i]);
        floatArray[i] = inputData[i];
    }

    //Step3. Release array resources
    env->ReleaseFloatArrayElements(outData, floatArray, 0);
}

void setIntArray(JNIEnv *env, jintArray outData, int dataCount, int *inputData)
{
    jint *intArray = env->GetIntArrayElements(outData, 0);
    if (intArray == 0) {
        return;
    }
    jsize len = env->GetArrayLength(outData);

    for (int i = 0; i < dataCount; i++) {
        //LOGD("[tuning-test] int inputData[%d] : %d", i, inputData[i]);
        intArray[i] = inputData[i];
    }

    //Step3. Release array resources
    env->ReleaseIntArrayElements(outData, intArray, 0);
}

//get
JNIEXPORT jboolean JNICALL Java_com_ispd_mommybook_JniController_getCheckBookLocation(JNIEnv *env, jclass clazz) {
    return getCheckBookLocation();
}

JNIEXPORT void JNICALL Java_com_ispd_mommybook_JniController_getCropFourPoint(JNIEnv *env, jclass clazz, jfloatArray outFourPoints, jfloatArray outVerPoints) {
    static float fourPoints[8] = {0.0f, 0.0f, (float)gPreviewWidthInfo, 0.0f,
                                  0.0f, (float)gPreviewHeightInfo, (float)gPreviewWidthInfo, (float)gPreviewHeightInfo};

    static float vericalPoints[4] = {0.0f, 0.0f, 0.0f, 0.0f};

    getCropFourPoint(fourPoints);
    getCropVerticalPoint(vericalPoints);

    setFloatArray(env, outFourPoints, 8, fourPoints);
    setFloatArray(env, outVerPoints, 4, vericalPoints);
}

JNIEXPORT jboolean JNICALL Java_com_ispd_mommybook_JniController_getCropMatrixValue(JNIEnv *env, jclass clazz, jfloatArray outCropMatValue,
                                                                                jint targetWidth, jint targetHeight) {
    if( targetWidth == gPreviewWidthInfo && targetHeight == gPreviewHeightInfo ) {
        setFloatArray(env, outCropMatValue, 9, gPreviewCropMatrix);
    }
    else {
        setFloatArray(env, outCropMatValue, 9, gCaptureCropMatrix);
    }

    return true;
}

JNIEXPORT jboolean JNICALL Java_com_ispd_mommybook_JniController_getAlignmentMatrixValue(JNIEnv *env, jclass clazz, jfloatArray outAlignLeftMat, jfloatArray outAlignRightMat,
                                                            jint targetWidth, jint targetHeight) {

    if( targetWidth == gPreviewWidthInfo && targetHeight == gPreviewHeightInfo ) {
        setFloatArray(env, outAlignLeftMat, 9, gPreviewLeftMatrix);
        setFloatArray(env, outAlignRightMat, 9, gPreviewRightMatrix);
    }
    else {
        setFloatArray(env, outAlignLeftMat, 9, gCaptureLeftMatrix);
        setFloatArray(env, outAlignRightMat, 9, gCaptureRightMatrix);
    }

    return true;
}

JNIEXPORT void JNICALL Java_com_ispd_mommybook_JniController_getAlignmentCurveValue(JNIEnv *env, jclass clazz, jfloatArray leftCurveValues, jfloatArray rightCurveValues) {
    static float inputLeftDatas[] = {-1.0f, -1.0f, -1.0f, -1.0f};
    static float inputRightDatas[] = {-1.0f, -1.0f, -1.0f, -1.0f};

    getAlignmentCurveValue(inputLeftDatas, inputRightDatas);

    setFloatArray(env, leftCurveValues, 4, inputLeftDatas);
    setFloatArray(env, rightCurveValues, 4, inputRightDatas);
}

//set
JNIEXPORT void JNICALL Java_com_ispd_mommybook_JniController_setCurrentBookInfo(JNIEnv *env, jclass clazz, jint bookCoverIndex, jint bookPageIndex) {
    if( bookCoverIndex == 0 ) {
        if( bookPageIndex ==  7 ) {
            return;
        }
    }
    else if( bookCoverIndex == 1 ) {
        if( bookPageIndex ==  7 ) {
            return;
        }
    }
    else if( bookCoverIndex == 2 ) {
        if( bookPageIndex ==  15 ) {
            return;
        }
    }

    if( gBookPageSaveIndex != bookPageIndex ) {
        gBookPageChanged = true;
    }
//    else {
//        gBookPageChanged = false;
//    }
    gBookPageSaveIndex = bookPageIndex;

    gBookCoverIndex = bookCoverIndex;
    gBookPageIndex = bookPageIndex;
}

//TEST

void setLabel(Mat& image, string str, vector<Point> contour)
{
    int fontface = FONT_HERSHEY_SIMPLEX;
    double scale = 0.5;
    int thickness = 1;
    int baseline = 0;

    Size text = getTextSize(str, fontface, scale, thickness, &baseline);
    Rect r = boundingRect(contour);

    Point pt(r.x + ((r.width - text.width) / 2), r.y + ((r.height + text.height) / 2));
    rectangle(image, pt + Point(0, baseline), pt + Point(text.width, -text.height), CV_RGB(200, 200, 200), FILLED);
    putText(image, str, pt, fontface, scale, CV_RGB(0, 0, 0), thickness, 8);
}



JNIEXPORT int JNICALL Java_com_ispd_mommybook_JniController_findKeyPointMatching(JNIEnv *, jclass clazz, jlong matAddrInput1, jlong matAddrInput2, int index) {

    LOGD("[findKeyPointMatching-jni-%d] findKeyPointMatching", index);

    Mat &srcMat = *(Mat *) matAddrInput1;
    Mat &targetMat = *(Mat *) matAddrInput2;

    std::vector<KeyPoint> keypoints1;
    Mat descriptors1;

    std::vector<KeyPoint> keypoints2;
    Mat descriptors2;

//  Ptr<AKAZE> akaze = AKAZE::create();
//  akaze->detectAndCompute(srcMat, Mat(), keypoints1, descriptors1);
    Ptr<ORB> orb = ORB::create();
    orb->detectAndCompute(srcMat, Mat(), keypoints1, descriptors1);
//    Ptr<SURF> surf1 = SURF::create(200.0/*400.0*/);
//    surf1->detectAndCompute(srcMat, Mat(), keypoints1, descriptors1);

//  Ptr<AKAZE> akaze2 = AKAZE::create();
//  akaze2->detectAndCompute(targetMat, Mat(), keypoints2, descriptors2);
    Ptr<ORB> orb2 = ORB::create();
    orb2->detectAndCompute(targetMat, Mat(), keypoints2, descriptors2);
//    Ptr<SURF> surf2 = SURF::create(400.0);
//    surf2->detectAndCompute(targetMat, Mat(), keypoints2, descriptors2);

    LOGD("Count KeyPoint : %d %d", keypoints1.size(), keypoints2.size());

    Mat draw1;
    drawKeypoints(srcMat, keypoints1, draw1);

    Mat draw2;
    drawKeypoints(targetMat, keypoints2, draw2);

    char name0[128];
    sprintf(name0, "/sdcard/studyNet/DEBUG/draw1-%d.jpg", index);
    imwrite(name0, draw1);

    sprintf(name0, "/sdcard/studyNet/DEBUG/draw2-%d.jpg", index);
    imwrite(name0, draw2);

    BFMatcher matcher(NORM_HAMMING, true);
//    BFMatcher matcher(NORM_L1, false);
    vector<vector<DMatch> > matches;
    matcher.knnMatch(descriptors1, descriptors2, matches, 1);

    LOGD("[Count KeyPoint] matches.size() : %d", matches.size());

    if (matches.size() == 0) {
        LOGE("Nothing Match Point");
        return -1;
    }

    std::vector<Point2f> points1, points2;

    std::vector<DMatch> newMatches;
    for (size_t i = 0; i < matches.size(); i++) {
        if (matches[i].size() > 0) {
            DMatch first = matches[i][0];
            newMatches.push_back(first);
        }
    }

    // Sort matches by score
    std::sort(newMatches.begin(), newMatches.end());

    // Remove not so good matches
    const int numGoodMatches = newMatches.size() * 0.95f;
    newMatches.erase(newMatches.begin() + numGoodMatches, newMatches.end());

    LOGD("[findKeyPointMatching-jni-%d] keypoints1 : %d keypoints2 : %d", index, keypoints1.size(), keypoints2.size());
    LOGD("[findKeyPointMatching-jni-%d] newMatches : %d", index, newMatches.size());

    Mat imMatches;
    drawMatches(srcMat, keypoints1, targetMat, keypoints2, newMatches, imMatches);

    char name[128];
    sprintf(name, "/sdcard/studyNet/DEBUG/drawMatches-%d.jpg", index);
    imwrite(name, imMatches);

    for (size_t i = 0; i < newMatches.size(); i++) {
        points1.push_back(keypoints1[newMatches[i].queryIdx].pt);
        points2.push_back(keypoints2[newMatches[i].trainIdx].pt);
    }

    // Find homography
    Mat h = findHomography(points1, points2, RANSAC);

    // Use homography to warp image
    Mat regMat;
    warpPerspective(srcMat, regMat, h, targetMat.size());

    sprintf(name, "/sdcard/studyNet/DEBUG/regOriMat-%d.jpg", index);
    imwrite(name, regMat);

    sprintf(name, "/sdcard/studyNet/DEBUG/regSumMat-%d.jpg", index);
    imwrite(name, regMat * 0.5 + targetMat * 0.5);

    //Scalar scalar(255);
    //scalar = countNonZero(diffMat);
    Scalar scalar = mean(regMat);
    LOGD("scalar : %f", scalar[0]);

    Scalar scalar2 = mean(targetMat);
    LOGD("scalar2 : %f", scalar2[0]);

    LOGD("regMat.cols/2 : %d", regMat.cols/2);
    int blockSize = (regMat.cols/2) % 2 == 0 ? (regMat.cols/2)+1 : (regMat.cols/2);
    LOGD("blockSize regMat.cols/2 : %d", blockSize);

    adaptiveThreshold(regMat, regMat,
                      255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV,
                      blockSize, 13);


    erode(targetMat, targetMat, Mat(), Point(-1, -1), 5);
    adaptiveThreshold(targetMat, targetMat,
                      255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV,
                      blockSize, 13);

    sprintf(name, "/sdcard/studyNet/DEBUG/regMat-%d.jpg", index);
    imwrite(name, regMat);
    sprintf(name, "/sdcard/studyNet/DEBUG/targetMat-%d.jpg", index);
    imwrite(name, targetMat);

    char nameT[128];
    char nameR[128];
    sprintf(nameT, "/sdcard/studyNet/DEBUG/edge1-%d.jpg", index);
    sprintf(nameR, "/sdcard/studyNet/DEBUG/edge2-%d.jpg", index);

//    Mat edgeMat1;
//    Canny(targetMat, edgeMat1, 500, 250, 5, true);
//    Mat zeroMat1 = Mat::zeros(targetMat.size(), targetMat.type());
//    zeroMat1.copyTo(targetMat, edgeMat1);
//    imwrite(nameT, targetMat);
//
//    Mat edgeMat;
//    Canny(regMat, edgeMat, 500, 250, 5, true);
//    Mat zeroMat = Mat::zeros(regMat.size(), regMat.type());
//    zeroMat.copyTo(regMat, edgeMat);
//    imwrite(nameR, regMat);

//    vector<vector<Point> > contours;
//    findContours(regMat, contours, RETR_CCOMP, CHAIN_APPROX_SIMPLE);
//    LOGD("%d-countours : %d", index, contours.size());
//
//    //contour를 근사화한다.
//    vector<Point2f> approx;
//    for (size_t i = 0; i < contours.size(); i++) {
//        //approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true) * 0.02, true);
//        drawContours(regMat, contours, i, Scalar(255, 255, 255), 5);
//    }
//    imwrite(nameR, regMat);

    //Mat diffMat =  (regMat * (scalar2[0] / scalar[0])) - targetMat;
    Mat diffMat = regMat - targetMat;

    //absdiff(targetMat, regMat, diffMat);

//    double min, max;
//    cv::minMaxLoc(diffMat, &min, &max);
//    LOGD("scalar3 : %f", max);

    Scalar scalar3 = mean(diffMat, diffMat);
    LOGD("scalar3 : %f", scalar3[0]);

    //threshold(diffMat, diffMat, (int)(scalar3[0] * 1.2f), 255, CV_THRESH_BINARY);

//    //contour를 찾는다.
//    vector<vector<Point> > contours;
//    findContours(diffMat, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);
//
//    //contour를 근사화한다.
//    vector<Point2f> approx;
//
//    for (size_t i = 0; i < contours.size(); i++)
//    {
//        //convexHull(Mat(contours[i]), contours[i], false);
//
//        approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true)*0.02, true);
//        drawContours(diffMat, contours, i, Scalar(255,255,255), 5);
//
//        if (fabs(contourArea(Mat(approx))) > 10 && fabs(contourArea(Mat(approx))) < 10000)  // <-----------------------   parameter 3
//        {
//
//            int size = approx.size();
//            const char *name[] = { "none", "none", "none", "triangle", "quadrangle", "pentagon", "hexagon", "heptagon", "octagon", "nonagon", "decagon", "circle" };
//
//            /*
//            switch (size) {
//
//            case 3: case 4: case 5:
//            case 6: case 10:
//            */
//
//            cout << "size : " << size << endl;
//            if (size > 10) {
//                size = 11;
//            }
//
//            //if (isContourConvex(Mat(approx))) { // convex 인지 검사
//            if( true ) {
//                //Contour를 근사화한 직선을 그린다.
//                if (size % 2 == 0) {
//                    line(diffMat, approx[0], approx[approx.size() - 1], Scalar(0, 255, 0), 3);
//
//                    for (int k = 0; k < size - 1; k++)
//                        line(diffMat, approx[k], approx[k + 1], Scalar(0, 255, 0), 3);
//
//                    for (int k = 0; k < size; k++)
//                        circle(diffMat, approx[k], 3, Scalar(0, 0, 255));
//                }
//                else {
//                    line(diffMat, approx[0], approx[approx.size() - 1], Scalar(0, 255, 0), 3);
//
//                    for (int k = 0; k < size - 1; k++)
//                        line(diffMat, approx[k], approx[k + 1], Scalar(0, 255, 0), 3);
//
//                    for (int k = 0; k < size; k++)
//                        circle(diffMat, approx[k], 3, Scalar(0, 0, 255));
//                }
//
//
//                //검출된 도형에 대한 라벨을 출력한다.
//                setLabel(diffMat, name[size], contours[i]);
//            }
//            /*
//                break;
//            deafult:
//                break;
//            }
//            */
//        }
//    }

    sprintf(name, "/sdcard/studyNet/DEBUG/PrediffMat-%d.jpg", index);
    imwrite(name, diffMat);

    threshold(diffMat, diffMat, 127, 255, CV_THRESH_BINARY);
    erode(diffMat, diffMat, Mat(), Point(-1, -1), 2);
    dilate(diffMat, diffMat, Mat(), Point(-1, -1), 4);

    Mat roiLeftMat = diffMat(Rect(0, 0, diffMat.cols/2, diffMat.rows));
    Mat roiRightMat = diffMat(Rect(diffMat.cols/2, 0, diffMat.cols/2, diffMat.rows));

    int leftCount = countNonZero(roiLeftMat);
    int rightCount = countNonZero(roiRightMat);

    int leftPercent = leftCount * 100 / (diffMat.cols/2 * diffMat.rows);
    int rightPercent = rightCount * 100 / (diffMat.cols/2 * diffMat.rows);

    LOGD("%dth LeftWowPercent : %d(%d)", index, leftPercent, leftCount);
    LOGD("%dth RightWowPercent : %d(%d)", index, rightPercent, rightCount);

    sprintf(name, "/sdcard/studyNet/DEBUG/diffMat-%d.jpg", index);
    imwrite(name, diffMat);

    if( leftPercent > 12 )
    {
        return 0;
    }
    else if( rightPercent > 12 )
    {
        return 1;
    }
    else
    {
        return -1;
    }
}

JNIEXPORT int JNICALL Java_com_ispd_mommybook_JniController_findKeyPoint(JNIEnv *, jclass clazz, jlong matAddrInput1, int index) {

    Mat &srcMat = *(Mat *) matAddrInput1;
    std::vector<KeyPoint> keypoints1;
    Mat descriptors1;

    //Ptr<AKAZE> akaze = AKAZE::create();

    //TemporaryBlock
    //Block here for test
    Ptr<SURF> surf = SURF::create(100.0);
    surf->detectAndCompute(srcMat, Mat(), keypoints1, descriptors1);

//    Ptr<ORB> orb = ORB::create();
//    orb->detectAndCompute(srcMat, Mat(), keypoints1, descriptors1);

    LOGD("keyPoint count-%d : %d", index, keypoints1.size());

    return keypoints1.size();
}

int doFindCircel(Mat input, int itemIndex, int index, int isTarget, int useContour)//0 : src, 1 : target
{
    Mat srcMat = input.clone();

    //1. bgr to HSV for using h
    cv::Mat hsv;
    cv::cvtColor(srcMat, hsv, CV_BGR2HSV);

    std::vector<cv::Mat> channels;
    cv::split(hsv, channels);

    cv::Mat H = channels[0];
    cv::Mat S = channels[1];
    cv::Mat V = channels[2];

    Mat vMat;
    vMat = V.clone();

    int pixelCount0 = countNonZero(vMat);
    LOGD("[findCircle-0] itemIndex : %d, index : %d, pixelCount0 : %d", itemIndex, index, pixelCount0);

    ///*
    int divideValue = 2;//7;
    int blockSize = (vMat.cols / divideValue) % 2 == 0 ? (vMat.cols / divideValue) + 1 : (vMat.cols / divideValue);
    adaptiveThreshold(vMat, vMat,
                      255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY_INV,
                      blockSize, 5);
    //*/
    //threshold(vMat, vMat, 0, 255, CV_THRESH_BINARY_INV + CV_THRESH_OTSU);

    char saveName0[256];
    //sprintf(saveName0, "/sdcard/studyNet/DEBUG/math/drawing0-%d-%d-%d.jpg", isTarget, index, itemIndex);
    sprintf(saveName0, "/sdcard/studyNet/DEBUG/korean/drawing(0)-%d-%d-%d.jpg", isTarget, index, itemIndex);
    imwrite(saveName0, vMat);

    Mat roiMat;
    if( useContour == 1 ) {
        vector<vector<Point> > contours;
        vector<Vec4i> hierarchy;
        //contour를 근사화한다.
        vector<Point2f> approx;

        /// Find contours
        findContours(vMat, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_NONE);

        RNG rng(12345);

        /// Draw contours
        cvtColor(vMat, vMat, CV_GRAY2RGB);
        Mat drawing = vMat.clone();

        double maxRect = -100.0;
        double minRect = 100000000.0;
        double maxArea = 0.0;
        double minArea = 0.0;

        Rect maxRectangle;

        for (int i = 0; i < contours.size(); i++) {
            Scalar color = Scalar(rng.uniform(0, 255), rng.uniform(0, 255), rng.uniform(0, 255));
            //drawContours(drawing, contours, i, color, 2, 8, hierarchy, 0, Point2i());

            //approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true)*0.02, true);

            //if (fabs(contourArea(Mat(approx))) > 100) {
            if (fabs(contourArea(Mat(contours[i]))) > 10000 / 10) {

                LOGD("fabs(contourArea(Mat(contours[i])) : %f",
                     fabs(contourArea(Mat(contours[i]))));

                Rect rect = boundingRect(contours[i]);

                LOGD("rectangle size : %d", rect.area());
                if (rect.area() < 8000) {
                    continue;
                }

                rectangle(drawing, rect, Scalar(255, 0, 255), 5);

                if (maxRect < rect.area()) {
                    maxRect = rect.area();
                    maxArea = fabs(contourArea(Mat(contours[i])));

                    maxRectangle = rect;
                }

                if (rect.area() < minRect) {
                    minRect = rect.area();
                    minArea = fabs(contourArea(Mat(contours[i])));
                }

                //int size = approx.size();
                //cout << "size[" << i << "] : " << size << endl;

                drawContours(drawing, contours, i, color, 3, 8, hierarchy, 0, Point2i());
            }
        }

        LOGD("[findCircle] --%d-- index : %d, itemIndex : %d, maxRect : %f, minRect : %f", isTarget,
             index, itemIndex, maxRect, minRect);
        LOGD("[findCircle] --%d-- index : %d, itemIndex : %d, maxArea : %f, minArea : %f", isTarget,
             index, itemIndex, maxArea, minArea);
        LOGD("[findCircle] --%d-- index : %d, itemIndex : %d, maxArea - minArea : %f", isTarget,
             index, itemIndex, maxArea - minArea);
        LOGD("[findCircle] --%d-- index : %d, itemIndex : %d, maxRect - minRect : %f", isTarget,
             index, itemIndex, maxRect - minRect);

        cvtColor(vMat, roiMat, CV_RGB2GRAY);
        roiMat = roiMat(maxRectangle).clone();

        char saveName[256];
        //sprintf(saveName, "/sdcard/studyNet/DEBUG/math/drawing1-%d-%d-%d.jpg", isTarget, index, itemIndex);
        sprintf(saveName, "/sdcard/studyNet/DEBUG/korean/drawing(1)-%d-%d-%d.jpg", isTarget, index, itemIndex);
        imwrite(saveName, drawing);

        //sprintf(saveName2, "/sdcard/studyNet/DEBUG/math/maxRectangle-%d-%d-%d.jpg", isTarget, index, itemIndex);
        sprintf(saveName, "/sdcard/studyNet/DEBUG/korean/maxRectangle-%d-%d-%d.jpg", isTarget, index, itemIndex);
        imwrite(saveName, roiMat);
    }
    else
    {
        roiMat = vMat.clone();
    }

    int pixelCount = countNonZero(roiMat);
    LOGD("[findCircle] --%d-- index : %d, itemIndex : %d, pixelCount1 : %d", isTarget, index, itemIndex, pixelCount);

    return pixelCount;
}

JNIEXPORT float JNICALL Java_com_ispd_mommybook_JniController_findCircle(JNIEnv *, jclass clazz, jlong matAddrInput, jlong matAddrInput2, int index, int itemIndex, int useContour) {

    Mat &srcMat = *(Mat *) matAddrInput;
    Mat &tarMat = *(Mat *) matAddrInput2;

    int pixelCount0 = doFindCircel(srcMat, itemIndex, index, 0, useContour);
    int pixelCount1 = doFindCircel(tarMat, itemIndex, index, 1, useContour);

    float changeRate = pixelCount0 * 100.f / pixelCount1;
    LOGD("changeRate : %f", changeRate);

    return changeRate;
}