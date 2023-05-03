
#include "imageEdgeDetect.h"

#include <opencv2/opencv.hpp>
#include <opencv2/core/types.hpp>
#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/core/types_c.h>

#include "opencv2/highgui.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/features2d.hpp"
#include "opencv2/features2d.hpp"
#include "opencv2/core/ocl.hpp"

#include <thread>

#define useDEBUG 1

#include <android/log.h>

#define  LOG_TAG    "imageEdgeDetect"
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

#define PI 3.14159265

bool gEdgeDebugOn = true;

static int gPreviewWidth = 1280;
static int gPreviewHeight = 960;
int gResizeRate = 4;
float gAreaCheckValue = 20.f;

//Tuning Values
//int gCropStartLine = (int)((gPreviewHeight/gResizeRate) / 10.f * 0.5f);//12
//int gCropEndLine = (int)((gPreviewHeight/gResizeRate) / 10.f * 2.5f);//60
int gCropStartLine = (int)((gPreviewHeight/gResizeRate) / 10.f * 1.5f);//36
int gCropEndLine = (int)((gPreviewHeight/gResizeRate) / 10.f * 3.5f);//84

//tuning values
float gBookVerMin;
float gBookVerMax;

Vec4i gInitPoint = {-1, -1, -1, -1};

//related horizontal
//result horizontal line
Vec4i gHorizonUsePoint = gInitPoint;

//related horizontal
//result sidebar line
Vec4i gSideLeftUsePoint[3] = {gInitPoint, gInitPoint, gInitPoint};
int gLeftSideLineCount = -1;

Vec4i gSideRightUsePoint[3] = {gInitPoint, gInitPoint, gInitPoint};
int gRightSideLineCount = -1;

Vec4i gVerticalUsePoint = gInitPoint;

float gMeetLeftX, gMeetLeftY;
float gMeetRightX, gMeetRightY;
float gPredictLeftX, gPredictLeftY;
float gPredictRightX, gPredictRightY;

bool gLineDetectStarted = false;

bool gBookLocationCorrect = false;

//Function
bool getCheckBookLocation(float areaCheckValue);
void drawGuideLineForDebug(Mat &inputMat, int divideValue, int multipleValue);

Mat gDebugMat;

// 65 to 75 degree & 15pixel
//global value : gHorizonUsePoint
bool checkAngleAndDistance(int x1, int y1, int x2, int y2, bool isLeft)
{
    float angle = 0.f;

    int horiX1 = gHorizonUsePoint[0];
    int horiY1 = gHorizonUsePoint[1];
    int horiX2 = gHorizonUsePoint[2];
    int horiY2 = gHorizonUsePoint[3];

    float aValue0 = sqrt(pow(horiX2-horiX1, 2) + pow(horiY2-horiY1, 2));
    float bValue0 = sqrt(pow(horiX1-horiX1, 2) + pow(horiY1-horiY2, 2));

    if(aValue0 > 0.0f || aValue0 < 0.0f) {
        float targetValue0 = bValue0 / aValue0;
        float lineDegree0 = asin(targetValue0) * 180.0f / PI;

        float distanceA = sqrt(pow(x2 - x1, 2) + pow(y2 - y1, 2));
        float distanceB = sqrt(pow(x2 - x1, 2) + pow(y1 - y1, 2));

        LOGI("%d distanceA : %f", isLeft, distanceA);

        angle = acos((distanceB / distanceA)) * 180.0f / PI;

        if( isLeft == true )
        {
            if( horiY1 < horiY2 )
            {
                angle = angle - lineDegree0;
            }
            else
            {
                angle = angle + lineDegree0;
            }
        }
        else
        {
            if( horiY1 < horiY2 )
            {
                angle = angle + lineDegree0;
            }
            else
            {
                angle = angle - lineDegree0;
            }
        }

        if (65 < angle && angle < 75 && distanceA > 15) {
            return true;
        }
        else
        {
            return false;
        }
    }
    else
    {
        float distanceA = sqrt(pow(x2 - x1, 2) + pow(y2 - y1, 2));
        float distanceB = sqrt(pow(x2 - x1, 2) + pow(y1 - y1, 2));

        angle = acos((distanceB / distanceA)) * 180.0f / PI;

        if (65 < angle && angle < 75 && distanceA > 15) {
            return true;
        }
        else
        {
            return false;
        }
    }
}

void horizontalThread(Mat grayInputMat, int cropStartLine, int corpEndLine,
                        int cannyThreshold1, int cannyThreshold2,
                        int dilateKSize, int dilateIteration, int onOff) {

    //crop image
    Mat HRoiMat = grayInputMat(Rect(0, cropStartLine, grayInputMat.cols, corpEndLine - cropStartLine)).clone();

    blur(HRoiMat, HRoiMat, Size(5,1));

    cannyThreshold1 = 5000;//1000
    cannyThreshold2 = 2500;//500
//
//    dilateKSize = 3;
//    dilateIteration = 1;

    Sobel(HRoiMat, HRoiMat, -1, 0, 1);
    Canny(HRoiMat, HRoiMat, cannyThreshold1, cannyThreshold2, 5, true);

    Mat dilateSize = getStructuringElement(MORPH_CROSS, Size(1, dilateKSize), Point(-1, -1));
    dilate(HRoiMat, HRoiMat, dilateSize, Point(-1, -1), dilateIteration);

    if( gEdgeDebugOn == true ) {
        //draw DEBUG
        Mat debugCropMat = gDebugMat(
                Rect(0, cropStartLine, grayInputMat.cols, corpEndLine - cropStartLine));

        Mat grayDebugMat;
        cvtColor(HRoiMat, grayDebugMat, COLOR_GRAY2BGRA);
        //imwrite("/sdcard/test.jpg", grayDebugMat);
        grayDebugMat.copyTo(debugCropMat, grayDebugMat);
    }

    //잘 그려졌나 보는 용도
//    hLineMat2 = Mat::zeros(HRoiMat.size(), HRoiMat.type());
    Mat hLineMat = Mat::zeros(HRoiMat.size(), HRoiMat.type());

    vector<Vec4i> hLines;
    int hMinLength = hLineMat.cols / 10;//40; //min line length
    int hMaxLineGap = hLineMat.cols / 300;//30; //Until max value, pass as one line
    LOGD("[horizontalThread] hMinLength : %d, hMaxLineGap : %d", hMinLength, hMaxLineGap);

//    if( onOff == 0 ) {
//        hLineMat2 = HRoiMat.clone();
//    }

    gHorizonUsePoint = gInitPoint;

    HoughLinesP(HRoiMat, hLines, 0.5, CV_PI / 180, 50, hMinLength, hMaxLineGap);

    int hMaxLength = 0;
    int hMaxIndex = -1;

    //랜덤 색
//    RNG rng(12345);

    LOGD("[horizontalThread] hLinesSize : %d", hLines.size());
    for (size_t i = 0; i < hLines.size(); i++) {
        Vec4i l = hLines[i];

        if( gEdgeDebugOn == true ) {
            line(gDebugMat, Point(l[0], cropStartLine+l[1]), Point(l[2], cropStartLine+l[3]),
                 127, 1, CV_AA);
        }

        //가로 길이 제일 큰 거
        if (abs(l[2] - l[0]) > hMaxLength) {
            hMaxLength = abs(l[2] - l[0]);
            hMaxIndex = i;
        }
    }

    LOGI("[horizontalThread] hMaxIndex : %d hMaxLength : %d", hMaxIndex, hMaxLength);

    int maxHorizonX1, maxHorizonY1, maxHorizonX2, maxHorizonY2;

    if (hMaxIndex != -1) {
        Vec4i maxLine = hLines[hMaxIndex];

        //make graph of line
        maxHorizonX1 = maxLine[0];
        maxHorizonY1 = maxLine[1];
        maxHorizonX2 = maxLine[2];
        maxHorizonY2 = maxLine[3];

        float aGraph = (float)(maxHorizonY2-maxHorizonY1) / (float)(maxHorizonX2-maxHorizonX1);
        float bGraph = (float)maxHorizonY1 - (float)(aGraph * maxHorizonX1);

        int minXaxis = HRoiMat.cols * 2;
        int minYaxis = HRoiMat.rows * 2;
        int maxXaxis = 0;
        int maxYaxis = 0;

        for(int i = 0; i < hLines.size(); i++)
        {
            if(i != hMaxIndex)
            {
                Vec4i points = hLines[i];

                float sourceX1 = points[0];
                float sourceY1 = points[1];
                float sourceX2 = points[2];
                float sourceY2 = points[3];

                float targetY1 = aGraph * sourceX1 + bGraph;
                float targetY2 = aGraph * sourceX2 + bGraph;

                float tuningRate = 0.2f;

                //비슷한 위치의 Y 값이면,
                if( fabs(targetY1 - sourceY1) <= HRoiMat.rows * tuningRate &&
                    fabs(targetY2 - sourceY2) <= HRoiMat.rows * tuningRate)
                {
                    if( sourceX1 <= minXaxis )
                    {
                        minXaxis = sourceX1;
                        minYaxis = sourceY1;
                    }
                    if( sourceX2 <= minXaxis )
                    {
                        minXaxis = sourceX2;
                        minYaxis = sourceY2;
                    }

                    if( sourceX1 >= maxXaxis )
                    {
                        maxXaxis = sourceX1;
                        maxYaxis = sourceY1;
                    }
                    if( sourceX2 >= maxXaxis )
                    {
                        maxXaxis = sourceX2;
                        maxYaxis = sourceY2;
                    }
                }
            }
        }

        int useMinX, useMinY, useMaxX, useMaxY;

        if( maxHorizonX1 <= maxHorizonX2)
        {
            if( minXaxis <= maxHorizonX1 )
            {
                useMinX = minXaxis;
                useMinY = minYaxis;
            }
            else
            {
                useMinX = maxHorizonX1;
                useMinY = maxHorizonY1;
            }
        }
        else
        {
            if( minXaxis <= maxHorizonX2 )
            {
                useMinX = minXaxis;
                useMinY = minYaxis;
            }
            else
            {
                useMinX = maxHorizonX2;
                useMinY = maxHorizonY2;
            }
        }

        if( maxHorizonX1 >= maxHorizonX2)
        {
            if( maxXaxis >= maxHorizonX1 )
            {
                useMaxX = maxXaxis;
                useMaxY = maxYaxis;
            }
            else
            {
                useMaxX = maxHorizonX1;
                useMaxY = maxHorizonY1;
            }
        }
        else
        {
            if( maxXaxis >= maxHorizonX2 )
            {
                useMaxX = maxXaxis;
                useMaxY = maxYaxis;
            }
            else
            {
                useMaxX = maxHorizonX2;
                useMaxY = maxHorizonY2;
            }
        }

//        if( onOff == 1 || onOff == 2 ) {
//            line(hLineMat2, Point(useMinX, useMinY), Point(useMaxX, useMaxY),
//                 Scalar(127), 2, CV_AA);
//        }

// 밑에꺼로 합치시오
//        entireMinX1 = useMinX;
//        entireMinY1 = cropStartLine + useMinY;
//        entireMaxX2 = useMaxX;
//        entireMaxY2 = cropStartLine + useMaxY;

        gHorizonUsePoint[0] = useMinX;
        gHorizonUsePoint[1] = cropStartLine + useMinY;
        gHorizonUsePoint[2] = useMaxX;
        gHorizonUsePoint[3] = cropStartLine + useMaxY;
    }
}

//gVerStart, gVerEnd 책의 예측 세로 길이...
void sideBarLeftThread(Mat grayInputMat, int divide/*25*/, int multiple/*6*/,
                    int cannyThreshold1, int cannyThreshold2, int dilateKSize, int dilateIteration,
                    int cropStartLine, int cropEndLine, int onOff) {

    Mat SideRoiMat = grayInputMat(Rect(0, 0,
                        grayInputMat.cols/divide * multiple, grayInputMat.rows)).clone();
    blur(SideRoiMat, SideRoiMat, Size(1,5));

//    cannyThreshold1 = 1000;///2;
//    cannyThreshold2 = 500;///5;
//
//    dilateKSize = 3;
//    dilateIteration = 1;

    Canny(SideRoiMat, SideRoiMat, cannyThreshold1, cannyThreshold2, 5, true);

    Mat dilateSize = getStructuringElement(MORPH_CROSS, Size(dilateKSize, dilateKSize), Point(-1, -1));
    dilate(SideRoiMat, SideRoiMat, dilateSize, Point(-1, -1), dilateIteration);

    if( gEdgeDebugOn == true ) {
        //draw DEBUG
        Mat debugCropMat = gDebugMat(
                Rect(0, 0,
                     grayInputMat.cols/divide * multiple, grayInputMat.rows));

        Mat grayDebugMat;
        cvtColor(SideRoiMat, grayDebugMat, COLOR_GRAY2BGRA);
        grayDebugMat.copyTo(debugCropMat, grayDebugMat);
    }

    //up, below crop...(위 아래 책 아닌 공간 삭제...)
    Mat maskMat = Mat::zeros(grayInputMat.size(), grayInputMat.type());

    //Tuning values
    vector<cv::Point> point;
    point.push_back(Point(maskMat.cols / divide * multiple , maskMat.rows));  //point1
    point.push_back(Point(maskMat.cols / divide * 2, maskMat.rows));  //point2
    point.push_back(Point(-maskMat.cols / divide * 3, maskMat.rows / 5));  //point3
    point.push_back(Point(maskMat.cols / divide * ((multiple-2) - 3), maskMat.rows / 5));  //point4

    fillConvexPoly(maskMat,               //Image to be drawn on
                   point,                 //C-Style array of points
                   Scalar(255),  //Color , BGR form
                   CV_AA,             // connectedness, 4 or 8
                   0);            // Bits of radius to treat as fraction

    int horizonLeftY = gHorizonUsePoint[1];

    if( horizonLeftY != -1 ) {
        float firstX = cropStartLine;
        float firstY = gBookVerMin;//책 세로 길이(위 기준)
        float endX = cropEndLine;
        float endY = gBookVerMax;//책 세로 길이 (아래 기준)

        float calcA = (endY - firstY) / (endX - firstX);
        float calcB = firstY - (calcA * firstX);
        float targetDistance = calcA * (horizonLeftY) + calcB;

        Mat delMat1 = maskMat(Rect(0, 0, grayInputMat.cols/divide * multiple, horizonLeftY));
        delMat1.setTo(0);
        Mat delMat2 = maskMat(Rect(0, horizonLeftY+targetDistance, grayInputMat.cols/divide * multiple, grayInputMat.rows-(horizonLeftY+targetDistance)));
        delMat2.setTo(0);

        maskMat = maskMat(Rect(0, 0, grayInputMat.cols/divide * multiple, grayInputMat.rows)).clone();
    } else {
        maskMat = maskMat(Rect(0, 0, grayInputMat.cols/divide * multiple, grayInputMat.rows)).clone();
    }

    Mat reInputMat = Mat::zeros(SideRoiMat.size(), SideRoiMat.type());
    SideRoiMat.copyTo(reInputMat, maskMat);
    SideRoiMat = reInputMat.clone();

    //잘 그려졌나 보는 용도
//    sideLeftLineMat2 = Mat::zeros(SideRoiMat.size(), SideRoiMat.type());
    Mat sideLineMat = Mat::zeros(SideRoiMat.size(), SideRoiMat.type());

    vector<Vec4i> sideLines;
    int sideMinLength = sideLineMat.rows / 10;//30;
    int sideMaxLineGap = sideLineMat.rows / 300;//100;

//    if( onOff == 0 ) {
//        sideLeftLineMat2 = SideRoiMat.clone();
//    }

    gSideLeftUsePoint[0] = gInitPoint;
    gSideLeftUsePoint[1] = gInitPoint;
    gSideLeftUsePoint[2] = gInitPoint;

    HoughLinesP(SideRoiMat, sideLines, 1.0, CV_PI / 180, 50, sideMinLength, sideMaxLineGap);

    if( gEdgeDebugOn == true ) {
        RNG rng(12345);
        for (size_t i = 0; i < sideLines.size(); i++) {
            Vec4i l = sideLines[i];
            line(gDebugMat, Point(l[0], l[1]), Point(l[2], l[3]),
                 rng.uniform(0, 255), 1, CV_AA);
        }
    }

    //sort lines
    vector<Vec4i> sideSortedLines;
    if( sideLines.size() != 0) {
        sideSortedLines = sideLines;
        for (int i = 0; i < sideSortedLines.size() - 1; i++) {
            for (int j = i + 1; j < sideSortedLines.size(); j++) {
                Vec4i dataNow = sideSortedLines[i];
                int lengthNow = abs(dataNow[1] - dataNow[3]);

                Vec4i dataCompare = sideSortedLines[j];
                int lengthCompare = abs(dataCompare[1] - dataCompare[3]);

                if (lengthCompare > lengthNow) {
                    Vec4i temp = sideSortedLines[i];
                    sideSortedLines[i] = sideSortedLines[j];
                    sideSortedLines[j] = temp;
                }
            }
        }
    }

    LOGI("[sideBarLeftThread] sideSortedLines size : %d", sideSortedLines.size());

    //detected line count
    gLeftSideLineCount = 0;

    if(sideSortedLines.size() != 0) {
        int aliveCount = 0;
        float valueX0[sideSortedLines.size()];
        float valueX1[sideSortedLines.size()];
        int valueSaveIndex = 0;

        for (int ii = 0; ii < sideSortedLines.size(); ii++) {

            if (aliveCount == 3) {
                break;
            }

            int currentX1 = sideSortedLines[ii][0];
            int currentY1 = sideSortedLines[ii][1];
            int currentX2 = sideSortedLines[ii][2];
            int currentY2 = sideSortedLines[ii][3];

            float aGraph = (float) (currentY2 - currentY1) / (float) (currentX2 - currentX1);
            float bGraph = (float) currentY1 - (float) (aGraph * currentX1);

            //check original point...각도 1차 체크
            if( currentY1 > currentY2 )
            {
                if( currentX1 < currentX2 )
                {
                    continue;
                }
            }
            else
            {
                if( currentX2 < currentX1 )
                {
                    continue;
                }
            }

            //check original point...각도 2차 체크
            bool isAngleOkay = checkAngleAndDistance(currentX1, currentY1, currentX2, currentY2, true);
            if( isAngleOkay == false )
            {
                continue;
            }

            if( aliveCount == 0 )
            {
                valueX0[0] = (0.f - bGraph) / aGraph;
                valueX1[0] = (SideRoiMat.rows - bGraph) / aGraph;
                valueSaveIndex++;
            }
            else
            {
                float currentX0 = (0.f - bGraph) / aGraph;
                float currentX1 = (SideRoiMat.rows - bGraph) / aGraph;

                bool delOkay = false;

                for(int k = 0; k < valueSaveIndex; k++)
                {
                    if( abs(currentX0-valueX0[k]) < 10.f && abs(currentX1-valueX1[k]) < 10.f )
                    {
                        delOkay = true;
                    }
                }

                if( delOkay == true )
                {
                    continue;
                }

                valueSaveIndex++;
                valueX0[valueSaveIndex] = currentX0;
                valueX1[valueSaveIndex] = currentX1;
            }

            int minXaxis = SideRoiMat.cols * 2;
            int minYaxis = SideRoiMat.rows * 2;
            int maxXaxis = 0;
            int maxYaxis = 0;

            for (int i = 0; i < sideSortedLines.size(); i++) {
                Vec4i points = sideSortedLines[i];

                float sourceX1 = points[0];
                float sourceY1 = points[1];
                float sourceX2 = points[2];
                float sourceY2 = points[3];

                if (i != ii) {
                    float targetX = (sourceY1 - bGraph) / aGraph;
                    float targetX2 = (sourceY2 - bGraph) / aGraph;

                    float tuning = 0.05f;

                    //길이, 세로라인 체크
                    if (fabs(targetX - sourceX1) <= SideRoiMat.cols * tuning &&
                        fabs(targetX2 - sourceX2) <= SideRoiMat.cols * tuning &&
                        (fabs(sourceY2 - sourceY1) > SideRoiMat.rows / 10 * 1)) {

                        if (sourceY1 <= minYaxis) {
                            minXaxis = sourceX1;
                            minYaxis = sourceY1;
                        }
                        if (sourceY2 <= minYaxis) {
                            minXaxis = sourceX2;
                            minYaxis = sourceY2;
                        }

                        if (sourceY1 >= maxYaxis) {
                            maxXaxis = sourceX1;
                            maxYaxis = sourceY1;
                        }
                        if (sourceY2 >= maxYaxis) {
                            maxXaxis = sourceX2;
                            maxYaxis = sourceY2;
                        }
                    }
                }
            }

            int useMinX, useMinY, useMaxX, useMaxY;

            if (currentY1 <= currentY2) {
                if (minYaxis <= currentY1) {
                    useMinX = minXaxis;
                    useMinY = minYaxis;
                } else {
                    useMinX = currentX1;
                    useMinY = currentY1;
                }
            } else {
                if (minYaxis <= currentY2) {
                    useMinX = minXaxis;
                    useMinY = minYaxis;
                } else {
                    useMinX = currentX2;
                    useMinY = currentY2;
                }
            }

            if (currentY1 >= currentY2) {
                if (maxYaxis >= currentY1) {
                    useMaxX = maxXaxis;
                    useMaxY = maxYaxis;
                } else {
                    useMaxX = currentX1;
                    useMaxY = currentY1;
                }
            } else {
                if (maxYaxis >= currentY2) {
                    useMaxX = maxXaxis;
                    useMaxY = maxYaxis;
                } else {
                    useMaxX = currentX2;
                    useMaxY = currentY2;
                }
            }

            if( useMinX > useMaxX )
            {
                continue;
            }

            gSideLeftUsePoint[aliveCount][0] = useMinX;
            gSideLeftUsePoint[aliveCount][1] = useMinY;
            gSideLeftUsePoint[aliveCount][2] = useMaxX;
            gSideLeftUsePoint[aliveCount][3] = useMaxY;

            aliveCount++;
            gLeftSideLineCount = aliveCount;

//            if (onOff == 1 || onOff == 2) {
//                line(sideLeftLineMat2, Point(useMinX, useMinY), Point(useMaxX, useMaxY),
//                     rng.uniform(0, 255), 2, CV_AA);
//            }
        }//for (int ii = 0; ii < sideSortedLines.size(); ii++) {
    }
}

void sideBarRightThread(Mat grayInputMat, int divide/*25*/, int multiple/*6*/,
                    int cannyThreshold1, int cannyThreshold2, int dilateKSize, int dilateIteration,
                    int cropStartLine, int cropEndLine, int onOff) {

    Mat SideRoiMat = grayInputMat(Rect(grayInputMat.cols-grayInputMat.cols/divide * multiple, 0,
                 grayInputMat.cols/divide * multiple, grayInputMat.rows)).clone();
    blur(SideRoiMat, SideRoiMat, Size(1,5));

//    cannyThreshold1 = 500;
//    cannyThreshold2 = 100;
//    cannyThreshold1 = 1000;///2;
//    cannyThreshold2 = 500;///5;
//
//    dilateKSize = 3;
//    dilateIteration = 1;

    Canny(SideRoiMat, SideRoiMat, cannyThreshold1, cannyThreshold2, 5, true);

    Mat dilateSize = getStructuringElement(MORPH_CROSS, Size(dilateKSize, dilateKSize), Point(-1, -1));
    dilate(SideRoiMat, SideRoiMat, dilateSize, Point(-1, -1), dilateIteration);

    if( gEdgeDebugOn == true ) {
        //draw DEBUG
        Mat debugCropMat = gDebugMat(
                Rect(grayInputMat.cols-grayInputMat.cols/divide * multiple, 0,
                     grayInputMat.cols/divide * multiple, grayInputMat.rows));

        Mat grayDebugMat;
        cvtColor(SideRoiMat, grayDebugMat, COLOR_GRAY2BGRA);
        grayDebugMat.copyTo(debugCropMat, grayDebugMat);
    }

    //up, below crop...(위 아래 책 아닌 공간 삭제...) 원본 사진
    Mat maskMat = Mat::zeros(grayInputMat.size(), grayInputMat.type());

    //Tuning Values
    vector<cv::Point> point2;
    point2.push_back(Point(maskMat.cols - maskMat.cols / divide * 2, maskMat.rows));  //point1
    point2.push_back(Point(maskMat.cols - maskMat.cols / divide * multiple, maskMat.rows));  //point2
    point2.push_back(Point(maskMat.cols - maskMat.cols / divide * ((multiple-2) - 3), maskMat.rows / 5));  //point3
    point2.push_back(Point(maskMat.cols + maskMat.cols / divide * 3, maskMat.rows / 5));  //point4

    fillConvexPoly(maskMat,               //Image to be drawn on
                   point2,                 //C-Style array of points
                   Scalar(255),  //Color , BGR form
                   CV_AA,             // connectedness, 4 or 8
                   0);            // Bits of radius to treat as fraction

    int horizonRightY = gHorizonUsePoint[3];

    if( horizonRightY != -1 ) {
        float firstX = cropStartLine;
        float firstY = gBookVerMin;
        float endX = cropEndLine;
        float endY = gBookVerMax;

        float calcA = (endY - firstY) / (endX - firstX);
        float calcB = firstY - (calcA * firstX);
        float targetDistance = calcA * (horizonRightY) + calcB;

        Mat delMat1 = maskMat(Rect(grayInputMat.cols - grayInputMat.cols / divide * multiple, 0,
                              grayInputMat.cols/divide * multiple, horizonRightY));
        delMat1.setTo(0);
        Mat delMat2 = maskMat(Rect(grayInputMat.cols - grayInputMat.cols / divide * multiple, horizonRightY+targetDistance,
                              grayInputMat.cols/divide * multiple, grayInputMat.rows-(horizonRightY+targetDistance)));
        delMat2.setTo(0);

        maskMat = maskMat(Rect(grayInputMat.cols - grayInputMat.cols / divide * multiple, 0,
                               grayInputMat.cols / divide * multiple, grayInputMat.rows)).clone();
    }
    else
    {
        maskMat = maskMat(Rect(grayInputMat.cols - grayInputMat.cols / divide * multiple, 0,
                               grayInputMat.cols / divide * multiple, grayInputMat.rows)).clone();
    }

    Mat reInputMat = Mat::zeros(SideRoiMat.size(), SideRoiMat.type());
    SideRoiMat.copyTo(reInputMat, maskMat);
    SideRoiMat = reInputMat.clone();

//    sideRightLineMat2 = Mat::zeros(SideRoiMat.size(), SideRoiMat.type());
    Mat sideLineMat = Mat::zeros(SideRoiMat.size(), SideRoiMat.type());

    vector<Vec4i> sideLines;
    int sideMinLength = sideLineMat.rows / 10;//30;
    int sideMaxLineGap = sideLineMat.rows / 300;//100;

//    if( onOff == 0 ) {
//        sideRightLineMat2 = HRoiMat.clone();
//    }

    gSideRightUsePoint[0] = gInitPoint;
    gSideRightUsePoint[1] = gInitPoint;
    gSideRightUsePoint[2] = gInitPoint;

    HoughLinesP(SideRoiMat, sideLines, 1.0, CV_PI / 180, 50, sideMinLength, sideMaxLineGap);

    if( gEdgeDebugOn == true ) {
        RNG rng(12345);
        for (size_t i = 0; i < sideLines.size(); i++) {
            Vec4i l = sideLines[i];

//            line(gDebugMat,
//                 Point((grayInputMat.cols - grayInputMat.cols / divide * multiple) + l[0], l[1]),
//                 Point((grayInputMat.cols - grayInputMat.cols / divide * multiple) + l[2], l[3]),
//                 rng.uniform(0, 255), 1, CV_AA);
        }
    }

    vector<Vec4i> sideSortedLines;
    if( sideLines.size() != 0) {
        sideSortedLines = sideLines;

        for (int i = 0; i < sideSortedLines.size() - 1; i++) {
            for (int j = i + 1; j < sideSortedLines.size(); j++) {
                Vec4i dataNow = sideSortedLines[i];
                int lengthNow = abs(dataNow[1] - dataNow[3]);

                Vec4i dataCompare = sideSortedLines[j];
                int lengthCompare = abs(dataCompare[1] - dataCompare[3]);

                if (lengthCompare > lengthNow) {
                    Vec4i temp = sideSortedLines[i];
                    sideSortedLines[i] = sideSortedLines[j];
                    sideSortedLines[j] = temp;
                }
            }
        }
    }

    LOGI("[sideBarRightThread] sideSortedLines size : %d", sideSortedLines.size());

    //detected line count
    gRightSideLineCount = 0;

    if(sideSortedLines.size() != 0) {
        int aliveCount = 0;
        float valueX0[sideSortedLines.size()];
        float valueX1[sideSortedLines.size()];
        int valueSaveIndex = 0;

        for (int ii = 0; ii < sideSortedLines.size(); ii++) {

            if (aliveCount == 3) {
                break;
            }

            int currentX1 = sideSortedLines[ii][0];
            int currentY1 = sideSortedLines[ii][1];
            int currentX2 = sideSortedLines[ii][2];
            int currentY2 = sideSortedLines[ii][3];

            float aGraph = (float) (currentY2 - currentY1) / (float) (currentX2 - currentX1);
            float bGraph = (float) currentY1 - (float) (aGraph * currentX1);

            //check original point...각도 1차 체크
            if( currentY1 > currentY2 )
            {
                if( currentX1 > currentX2 )
                {
                    continue;
                }
            }
            else
            {
                if( currentX2 > currentX1 )
                {
                    continue;
                }
            }

            //check original point...각도 2차 체크
            bool isAngleOkay = checkAngleAndDistance(currentX1, currentY1, currentX2, currentY2, false);
            if( isAngleOkay == false )
            {
                continue;
            }

            if( aliveCount == 0 )
            {
                valueX0[0] = (0.f - bGraph) / aGraph;
                valueX1[0] = (SideRoiMat.rows - bGraph) / aGraph;
                valueSaveIndex++;
            }
            else
            {
                float currentX0 = (0.f - bGraph) / aGraph;
                float currentX1 = (SideRoiMat.rows - bGraph) / aGraph;

                bool delOkay = false;

                for(int k = 0; k < valueSaveIndex; k++)
                {
                    if( abs(currentX0-valueX0[k]) < 10.f && abs(currentX1-valueX1[k]) < 10.f )
                    {
                        delOkay = true;
                    }
                }

                if( delOkay == true )
                {
                    continue;
                }

                valueSaveIndex++;
                valueX0[valueSaveIndex] = currentX0;
                valueX1[valueSaveIndex] = currentX1;
            }

            int minXaxis = SideRoiMat.cols * 2;
            int minYaxis = SideRoiMat.rows * 2;
            int maxXaxis = 0;
            int maxYaxis = 0;

            for (int i = 0; i < sideSortedLines.size(); i++) {
                Vec4i points = sideSortedLines[i];

                float sourceX1 = points[0];
                float sourceY1 = points[1];
                float sourceX2 = points[2];
                float sourceY2 = points[3];

                if (i != ii) {
                    float targetX = (sourceY1 - bGraph) / aGraph;
                    float targetX2 = (sourceY2 - bGraph) / aGraph;

                    float tuning = 0.05f;

                    //길이, 세로라인 체크
                    if (fabs(targetX - sourceX1) <= SideRoiMat.cols * tuning &&
                        fabs(targetX2 - sourceX2) <= SideRoiMat.cols * tuning &&
                        (fabs(sourceY2 - sourceY1) > SideRoiMat.rows / 10 * 1)) {

                        if (sourceY1 <= minYaxis) {
                            minXaxis = sourceX1;
                            minYaxis = sourceY1;
                        }
                        if (sourceY2 <= minYaxis) {
                            minXaxis = sourceX2;
                            minYaxis = sourceY2;
                        }

                        if (sourceY1 >= maxYaxis) {
                            maxXaxis = sourceX1;
                            maxYaxis = sourceY1;
                        }
                        if (sourceY2 >= maxYaxis) {
                            maxXaxis = sourceX2;
                            maxYaxis = sourceY2;
                        }
                    }
                }
            }

            int useMinX, useMinY, useMaxX, useMaxY;

            if (currentY1 <= currentY2) {
                if (minYaxis <= currentY1) {
                    useMinX = minXaxis;
                    useMinY = minYaxis;
                } else {
                    useMinX = currentX1;
                    useMinY = currentY1;
                }
            } else {
                if (minYaxis <= currentY2) {
                    useMinX = minXaxis;
                    useMinY = minYaxis;
                } else {
                    useMinX = currentX2;
                    useMinY = currentY2;
                }
            }

            if (currentY1 >= currentY2) {
                if (maxYaxis >= currentY1) {
                    useMaxX = maxXaxis;
                    useMaxY = maxYaxis;
                } else {
                    useMaxX = currentX1;
                    useMaxY = currentY1;
                }
            } else {
                if (maxYaxis >= currentY2) {
                    useMaxX = maxXaxis;
                    useMaxY = maxYaxis;
                } else {
                    useMaxX = currentX2;
                    useMaxY = currentY2;
                }
            }

            if( useMinX < useMaxX )
            {
                continue;
            }

            gSideRightUsePoint[aliveCount][0] = (grayInputMat.cols-grayInputMat.cols/divide * multiple) + useMinX;
            gSideRightUsePoint[aliveCount][1] = useMinY;
            gSideRightUsePoint[aliveCount][2] = (grayInputMat.cols-grayInputMat.cols/divide * multiple) + useMaxX;
            gSideRightUsePoint[aliveCount][3] = useMaxY;

            aliveCount++;
            gRightSideLineCount = aliveCount;

//            if (onOff == 1 || onOff == 2) {
//                line(sideRightLineMat2, Point(useMinX, useMinY), Point(useMaxX, useMaxY),
//                     rng.uniform(0, 255), 2, CV_AA);
//            }
        }//for (int ii = 0; ii < sideSortedLines.size(); ii++) {
    }
}

void detectHorizonAndSideEdge(Mat grayInputMat, Mat hsvMat, float bookVericalMin, float bookVerticalMax, Mat &debugMat) {

    gDebugMat = debugMat.clone();

    //Debug 용도
    int onOff = -1;

    gBookVerMin = bookVericalMin;
    gBookVerMax = bookVerticalMax;

    int cannyThreshold1 = 1000;//10000;//1000
    int cannyThreshold2 = 500;//5000;//500

    int dilateKSize = 3;
    int dilateIteration = 1;

    thread horiThread(horizontalThread, hsvMat/*grayInputMat*/, gCropStartLine, gCropEndLine,
                cannyThreshold1, cannyThreshold2, dilateKSize, dilateIteration, onOff);

    int leftDivide = 25;
    int leftMultiple = 7;

    cannyThreshold1 = 1000;//1000
    cannyThreshold2 = 500;//500

    dilateKSize = 3;
    dilateIteration = 1;

    thread sideLeftThread(sideBarLeftThread, hsvMat/*grayInputMat*/, leftDivide, leftMultiple,
                        cannyThreshold1, cannyThreshold2, dilateKSize, dilateIteration,
                        gCropStartLine, gCropEndLine, onOff);

    cannyThreshold1 = 1000;
    cannyThreshold2 = 500;

    dilateKSize = 3;
    dilateIteration = 1;

    thread sideRightThread(sideBarRightThread, hsvMat/*grayInputMat*/, leftDivide, leftMultiple,
                        cannyThreshold1, cannyThreshold2, dilateKSize, dilateIteration,
                        gCropStartLine, gCropEndLine, onOff);

    horiThread.join();
    sideLeftThread.join();
    sideRightThread.join();

    debugMat = gDebugMat.clone();
}

Mat detectFourCorner(Mat grayInputMat, Mat inputDebugMat, float bookHoriLengthMin, float bookHoriLengthMax,
                        float bookVericalMin, float bookVerticalMax) {

    Mat debugMat = inputDebugMat.clone();

    if(gEdgeDebugOn == true) {
        int leftDivide = 25;
        int leftMultiple = 7;

        drawGuideLineForDebug(debugMat, leftDivide, leftMultiple);
    }

    //1. 위, 아래 기준 가로 길이를 위한 그래프
    float horiLineUp = (float)gCropStartLine;
    float horiMin = bookHoriLengthMin;
    float horiLineDown = (float)gCropEndLine;
    float horiMax = bookHoriLengthMax;

    float horiPredictGraphA = (horiMax - horiMin) / (horiLineDown - horiLineUp);
    float horiPredictGraphB = horiMin - (horiPredictGraphA * horiLineUp);

    float horiDetectedGraphA, horiDetectedGraphB,
            sideLeftGraphA, sideLeftGraphB, sideRightGraphA, sideRightGraphB,
            meetLeftX = -1.f, meetLeftY = -1.f, meetRightX = -1.f, meetRightY = -1.f;

    //2. 찾아진 가로선 그래프
    int horiDetectedX1 = gHorizonUsePoint[0];
    int horiDetectedY1 = gHorizonUsePoint[1];
    int horiDetectedX2 = gHorizonUsePoint[2];
    int horiDetectedY2 = gHorizonUsePoint[3];

    horiDetectedGraphA = (float)(horiDetectedY2 - horiDetectedY1) / (float)(horiDetectedX2 - horiDetectedX1);
    horiDetectedGraphB = (float)horiDetectedY1 - (horiDetectedGraphA * (float)horiDetectedX1);

    //3. 외곽 세로선 찾기
    float minLeftAxisX = 10000.f;
    float maxRightAxisX = -10000.f;
    int minLeftXIndex = -1;
    int maxRightXIndex = -1;

    LOGD("[detectFourCorner] gLeftSideLineCount : %d", gLeftSideLineCount);

    for(int i = 0; i < gLeftSideLineCount; i++) {
        int sideLeftX1 = gSideLeftUsePoint[i][0];
        int sideLeftY1 = gSideLeftUsePoint[i][1];
        int sideLeftX2 = gSideLeftUsePoint[i][2];
        int sideLeftY2 = gSideLeftUsePoint[i][3];

        float leftGraphA = (float) (sideLeftY2 - sideLeftY1) / (float) (sideLeftX2 - sideLeftX1);
        float leftGraphB = (float) sideLeftY1 - (leftGraphA * (float) sideLeftX1);

        float leftXAxis = (grayInputMat.rows / 2 - leftGraphB) / leftGraphA;

        if (leftXAxis <= minLeftAxisX) {
            minLeftAxisX = leftXAxis;
            minLeftXIndex = i;
        }

        float leftAxisXUp = (0.f - leftGraphB) / leftGraphA;
        float leftAxisXBelow = ((float) grayInputMat.rows - leftGraphB) / leftGraphA;

        if (gEdgeDebugOn == true) {
            //draw all side left line
            line(debugMat, Point(leftAxisXUp, 0), Point(leftAxisXBelow, grayInputMat.rows),
                 Scalar(0, 0, 255, 127), 1, CV_AA);
        }
    }

    LOGD("[detectFourCorner] gRightSideLineCount : %d", gRightSideLineCount);

    for(int i = 0; i < gRightSideLineCount; i++)
    {
        int sideRightX1 = gSideRightUsePoint[i][0];
        int sideRightY1 = gSideRightUsePoint[i][1];
        int sideRightX2 = gSideRightUsePoint[i][2];
        int sideRightY2 = gSideRightUsePoint[i][3];

        float graphRA = (float)(sideRightY2 - sideRightY1) / (float)(sideRightX2 - sideRightX1);
        float graphRB = (float) sideRightY1 - (graphRA * (float) sideRightX1);

        float rightXAxis = (grayInputMat.rows/2 - graphRB) / graphRA;

        if( rightXAxis >= maxRightAxisX )
        {
            maxRightAxisX = rightXAxis;
            maxRightXIndex = i;
        }

        float rightAxisXUp = (0.f - graphRB) / graphRA;
        float rightAxisXBelow = ((float)grayInputMat.rows - graphRB) / graphRA;

        if (gEdgeDebugOn == true) {
            //draw all side right line
            line(debugMat, Point(rightAxisXUp, 0), Point(rightAxisXBelow, grayInputMat.rows),
                 Scalar(0, 0, 255, 127), 1, CV_AA);
        }
    }

    int useLeftX1, useLeftY1, useLeftX2, useLeftY2;
    int useRightX1, useRightY1, useRightX2, useRightY2;

    if( minLeftXIndex != -1 ) {
        useLeftX1 = gSideLeftUsePoint[minLeftXIndex][0];
        useLeftY1 = gSideLeftUsePoint[minLeftXIndex][1];
        useLeftX2 = gSideLeftUsePoint[minLeftXIndex][2];
        useLeftY2 = gSideLeftUsePoint[minLeftXIndex][3];
    }

    if( maxRightXIndex != -1 ) {
        useRightX1 = gSideRightUsePoint[maxRightXIndex][0];
        useRightY1 = gSideRightUsePoint[maxRightXIndex][1];
        useRightX2 = gSideRightUsePoint[maxRightXIndex][2];
        useRightY2 = gSideRightUsePoint[maxRightXIndex][3];
    }
    //calc left, right last lines...


    //4. 가로와 세로를 이용하여 접점 찾기
    //calc meet lines start
    sideLeftGraphA = (float) (useLeftY2 - useLeftY1) / (float) (useLeftX2 - useLeftX1);
    sideLeftGraphB = (float) useLeftY1 - (sideLeftGraphA * (float) useLeftX1);

    sideRightGraphA = (float) (useRightY2 - useRightY1) / (float) (useRightX2 - useRightX1);
    sideRightGraphB = (float) useRightY1 - (sideRightGraphA * (float) useRightX1);

    meetLeftX = (sideLeftGraphB - horiDetectedGraphB) / (horiDetectedGraphA - sideLeftGraphA);
    meetLeftY = (horiDetectedGraphA * meetLeftX) + horiDetectedGraphB;

    meetRightX = (sideRightGraphB - horiDetectedGraphB) / (horiDetectedGraphA - sideRightGraphA);
    meetRightY = (horiDetectedGraphA * meetRightX) + horiDetectedGraphB;
    //calc meet lines end

    //5. 세로라인 각도와 길이 체크
    float aHoriLen = sqrt(pow(horiDetectedX2-horiDetectedX1, 2) + pow(horiDetectedY2-horiDetectedY1, 2));
    float bHoriLen = sqrt(pow(horiDetectedX1-horiDetectedX1, 2) + pow(horiDetectedY1-horiDetectedY2, 2));

    bool leftUse = false;
    bool rightUse = false;
    float leftAngle = 72.5f;
    float rightAngle = 72.5f;

    static float saveMeetLeftX = -1.f;
    static float saveMeetLeftY = -1.f;
    static float saveMeetRightX = gPreviewWidth/gResizeRate;
    static float saveMeetRightY = -1.f;

    if(aHoriLen > 0.0f) {
        float targetValue = bHoriLen / aHoriLen;
        float horiLineDegree = asin(targetValue) * 180.0f / PI;

        float distanceLA = sqrt(pow(useLeftX2 - useLeftX1, 2) +
                                pow(useLeftY2 - useLeftY1, 2));
        float distanceLB = sqrt(pow(useLeftX2 - useLeftX1, 2) +
                                pow(useLeftY1 - useLeftY1, 2));

        float distanceRA = sqrt(pow(useRightX2 -useRightX1, 2) +
                                pow(useRightY2 - useRightY1, 2));
        float distanceRB = sqrt(pow(useRightX2 - useRightX1, 2) +
                                pow(useRightY1 - useRightY1, 2));

        leftAngle = acos((distanceLB / distanceLA)) * 180.0f / PI;
        rightAngle = acos((distanceRB / distanceRA)) * 180.0f / PI;

        if( horiDetectedY1 < horiDetectedY2 )
        {
            leftAngle = leftAngle - horiLineDegree;
            rightAngle = rightAngle + horiLineDegree;
        }
        else
        {
            leftAngle = leftAngle + horiLineDegree;
            rightAngle = rightAngle - horiLineDegree;
        }

        LOGD("[detectFourCorner] minLeftXIndex : %d , leftAngle : %f, distanceLA : %f", minLeftXIndex, leftAngle, distanceLA);
        LOGD("[detectFourCorner] maxRightXIndex : %d , rightAngle : %f, distanceRA : %f", maxRightXIndex, rightAngle, distanceRA);

        if ( minLeftXIndex != -1 && (65 < leftAngle && leftAngle < 75 && distanceLA > 40/*5*/) ) {
            leftUse = true;
        }

        if ( maxRightXIndex != -1 && (65 < rightAngle && rightAngle < 75 && distanceRA > 40/*5*/) ) {
            rightUse = true;
        }

        if( leftUse == true && rightUse == true ) {
            static bool saveLeftUse;
            static bool saveRightUse;

            //가로 길이 예측
            float predictHoriInput = (meetLeftY + meetRightY) / 2.f;
            float predictHoriDistance = horiPredictGraphA * predictHoriInput + horiPredictGraphB;

            //가로 실제 길이
            float horizonLength = sqrt( pow((meetRightX-meetLeftX), 2) + pow((meetRightY-meetLeftY), 2) );

            //길이가 안 맞으면 한쪽 기준으로 선택
            if (abs(predictHoriDistance - horizonLength) <= 5)
            {
                saveLeftUse = leftUse;
                saveRightUse = rightUse;
            } else if (abs(predictHoriDistance - horizonLength) >= 10) {

                LOGD("[detectFourCorner] distanceLA : %f , distanceRA : %f", distanceLA, distanceRA);

                if (distanceLA > distanceRA) {
                    rightUse = false;
                } else {
                    leftUse = false;
                }

                saveLeftUse = leftUse;
                saveRightUse = rightUse;
            } else {
                leftUse = saveLeftUse;
                rightUse = saveRightUse;
            }
        }

        if (leftUse == false && rightUse == false) {
            LOGD("[detectFourCorner] saveMeetPoint %f %f %f %f", saveMeetLeftX, saveMeetLeftY, saveMeetRightX, saveMeetRightY);

            meetLeftX = saveMeetLeftX;
            meetLeftY = saveMeetLeftY;
            meetRightX = saveMeetRightX;
            meetRightY = saveMeetRightY;
        } else {
            LOGD("[detectFourCorner] rightUse : %d, leftUse : %d", rightUse, leftUse);

            if (rightUse == false) { //In case of right is no line
                float predictHoriInput = (horiDetectedY1 + horiDetectedY2) / 2;
                float predictHoriDistance = horiPredictGraphA * predictHoriInput + horiPredictGraphB;

                if (horiDetectedY2 < horiDetectedY1) {
                    meetRightX = meetLeftX + cos(horiLineDegree * (PI / 180.f)) * predictHoriDistance;
                    meetRightY = meetLeftY - sin(horiLineDegree * (PI / 180.f)) * predictHoriDistance;
                } else {
                    meetRightX = meetLeftX + cos(horiLineDegree * (PI / 180.f)) * predictHoriDistance;
                    meetRightY = meetLeftY + sin(horiLineDegree * (PI / 180.f)) * predictHoriDistance;
                }
            }

            if (leftUse == false) { //In case of left is no line
                float predictHoriInput = (horiDetectedY1 + horiDetectedY2) / 2;
                float predictHoriDistance = horiPredictGraphA * predictHoriInput + horiPredictGraphB;

                if (horiDetectedY2 < horiDetectedY1) {
                    meetLeftX = meetRightX - cos(horiLineDegree * (PI / 180.f)) * predictHoriDistance;
                    meetLeftY = meetRightY + sin(horiLineDegree * (PI / 180.f)) * predictHoriDistance;
                } else {
                    meetLeftX = meetRightX - cos(horiLineDegree * (PI / 180.f)) * predictHoriDistance;
                    meetLeftY = meetRightY - sin(horiLineDegree * (PI / 180.f)) * predictHoriDistance;
                }
            }
        }
    }
    else
    {
        LOGD("[detectFourCorner] No Line?");
    }

    saveMeetLeftX = meetLeftX;
    saveMeetLeftY = meetLeftY;
    saveMeetRightX = meetRightX;
    saveMeetRightY = meetRightY;
    LOGD("[detectFourCorner] meetPoints %f %f %f %f", meetLeftX, meetLeftY, meetRightX, meetRightY);

    if (gEdgeDebugOn == true) {
        //draw info...

        line(debugMat, Point(meetLeftX, meetLeftY),
             Point(meetRightX, meetRightY),//160~140
             Scalar(0, 255, 255, 127), 1, CV_AA);

        line(debugMat, Point(horiDetectedX1, horiDetectedY1),
             Point(horiDetectedX2, horiDetectedY2),//160~140
             Scalar(0, 255, 0, 127), 1, CV_AA);
        circle(debugMat, Point(meetLeftX, meetLeftY), 30, Scalar(255, 0, 255), 3);
        circle(debugMat, Point(meetRightX, meetRightY), 30, Scalar(255, 0, 255), 3);
    }

    //6. 세로라인 구하기
    float aHoriLength = sqrt(pow(meetRightX-meetLeftX, 2) + pow(meetRightY-meetLeftY, 2));
    float bHoriLength = sqrt(pow(meetLeftX-meetLeftX, 2) + pow(meetLeftY-meetRightY, 2));

    float targetLeftX = 0.f;
    float targetRightX = 0.f;

    if(aHoriLength > 0.0f) {
        float inputValue = bHoriLength / aHoriLength;
        float horizonDegree = asin(inputValue)  * 180.0f / PI;

        float wantedDegree = 75.0f;

        //draw left
        float calcDegreeLeft;
        if( meetRightY > meetLeftY) {
            calcDegreeLeft = 90.f - wantedDegree - horizonDegree;
        }
        else
        {
            calcDegreeLeft = 90.f - wantedDegree + horizonDegree;
        }

        float conectedLineLeft = grayInputMat.rows - meetLeftY;

        targetLeftX = tan(calcDegreeLeft * (PI / 180.f)) * conectedLineLeft;

        //draw right
        float calcDegreeRight;
        if( meetRightY > meetLeftY) {
            calcDegreeRight = 90.f - wantedDegree + horizonDegree;
        }
        else
        {
            calcDegreeRight = 90.f - wantedDegree - horizonDegree;
        }

        float conectedLineRight = grayInputMat.rows - meetRightY;

        targetRightX = tan(calcDegreeRight * (PI / 180.f)) * conectedLineRight;
    }

    //draw middle Line start...
    //7. 가운데 세로 선 그래프 구하기
    float middleGraphA = 0.f;
    if( horiDetectedGraphA != 0.0f )
    {
        middleGraphA = -1.f / horiDetectedGraphA;
    }

    float halfHoriLength = aHoriLength / 2;
    float inputValue = bHoriLength / aHoriLength;
    float horizonDegree = asin(inputValue)  * 180.0f / PI;

    LOGD("[detectFourCorner] halfHoriLength, inputValue, horizonDegree : %f %f %f", halfHoriLength, inputValue, horizonDegree);

    int middleInputX = cos(horizonDegree * (PI / 180.f)) * halfHoriLength;
    float middleGraphB = (horiDetectedGraphA - middleGraphA) * (meetLeftX + middleInputX) + horiDetectedGraphB;

    LOGD("[detectFourCorner] middleGraphA, middleGraphB : %f %f", middleGraphA, middleGraphB);

    float firstX = gCropStartLine;
    float firstY = bookVericalMin;
    float endX = gCropEndLine;
    float endY = bookVerticalMax;

    float verticalGraphA = (endY - firstY) / (endX - firstX);
    float verticalGraphB = firstY - ( verticalGraphA * firstX );

    float predictVerDistance = verticalGraphA * ((meetLeftY  + meetRightY) / 2) + verticalGraphB;

    LOGD("[detectFourCorner] predictVerDistance %f", predictVerDistance);

    float middleUpX = meetLeftX + middleInputX;
    float middleUpY = middleGraphA * middleUpX + middleGraphB;

    float verticalEndX = (grayInputMat.rows - middleGraphB) / middleGraphA;
    float verticalEndY = grayInputMat.rows;
    float verticalBelowX = meetLeftX + middleInputX;
    float verticalBelowY = grayInputMat.rows;

    float distanceA = sqrt( pow( verticalEndX - middleUpX, 2) + pow( verticalEndY - middleUpY, 2) );
    float distanceB = sqrt( pow( verticalBelowX - middleUpX, 2) + pow( verticalBelowY - middleUpY, 2) );
    float vericalDegree = acos(distanceB / distanceA)  * 180.0f / PI;

    float middleDownX, middleDownY;

    if( meetLeftY > meetRightY ) {
        middleDownX = middleUpX + sin(vericalDegree * (PI / 180.f)) * predictVerDistance;
        middleDownY = middleUpY + cos(vericalDegree * (PI / 180.f)) * predictVerDistance;
    }
    else if( meetLeftY < meetRightY ) {
        middleDownX = middleUpX - sin(vericalDegree * (PI / 180.f)) * predictVerDistance;
        middleDownY = middleUpY + cos(vericalDegree * (PI / 180.f)) * predictVerDistance;
    }
    else {
        middleDownX = middleUpX;
        middleDownY = middleUpY + predictVerDistance;
    }

    gVerticalUsePoint[0] = middleUpX;
    gVerticalUsePoint[1] = middleUpY;
    gVerticalUsePoint[2] = middleDownX;
    gVerticalUsePoint[3] = middleDownY;

    if (gEdgeDebugOn == true) {
        line(debugMat, Point(middleUpX, middleUpY),
             Point(middleDownX, middleDownY),//160~140
             Scalar(255, 0, 255, 127), 1, CV_AA);
    }
    //draw middle Line end...

    //가운데 세로 선 그래프 구하기

    //8. 좌우 아래서 포인트 예측
    //left
    float vericalPredictDistance = verticalGraphA * meetLeftY + verticalGraphB;//160~140
    float predictLeftY = meetLeftY + vericalPredictDistance;

    float predictLeftX = 0.f;
    if( leftUse == false )
    {
        predictLeftX = meetLeftX + targetLeftX;
    }
    else
    {
        predictLeftX = (predictLeftY - sideLeftGraphB) / sideLeftGraphA;
    }

    if (gEdgeDebugOn == true) {
        line(debugMat, Point(meetLeftX, meetLeftY),
             Point(predictLeftX, predictLeftY),
             Scalar(255, 0, 0, 127), 1, CV_AA);
    }

    //right
    float wantedRightDistance = verticalGraphA * meetRightY + verticalGraphB;;//160~140
    float predictRightY = meetRightY + wantedRightDistance;

    float predictRightX;
    if( rightUse == false )
    {
        predictRightX = meetRightX - targetRightX;
    } else
    {
        predictRightX = (predictRightY - sideRightGraphB) / sideRightGraphA;
    }

    if (gEdgeDebugOn == true) {
        line(debugMat, Point(meetRightX, meetRightY),
             Point(predictRightX, predictRightY),
             Scalar(255, 0, 0, 127), 1, CV_AA);
    }

    //save result
    if( getCheckBookLocation(20.f) == false )
    {
        gBookLocationCorrect = false;

        gMeetLeftX = 0.f;
        gMeetLeftY = 0.f;

        gMeetRightX = gPreviewWidth / gResizeRate;
        gMeetRightY = 0.f;

        gPredictLeftX = 0.f;
        gPredictLeftY = gPreviewHeight / gResizeRate;

        gPredictRightX = gPreviewWidth / gResizeRate;
        gPredictRightY = gPreviewHeight / gResizeRate;
    }
    else
    {
        gBookLocationCorrect = true;

        int filterCount = 10;

        static float queue1[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queue2[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queue3[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queue4[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queue5[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queue6[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queue7[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        static float queue8[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
                                ,0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};

        for(int i = 0; i < filterCount - 1; i++) {
            queue1[i] = queue1[i+1];
            queue2[i] = queue2[i+1];
            queue3[i] = queue3[i+1];
            queue4[i] = queue4[i+1];
            queue5[i] = queue5[i+1];
            queue6[i] = queue6[i+1];
            queue7[i] = queue7[i+1];
            queue8[i] = queue8[i+1];
        }

        queue1[filterCount-1] = meetLeftX;
        queue2[filterCount-1] = meetLeftY;
        queue3[filterCount-1] = meetRightX;
        queue4[filterCount-1] = meetRightY;
        queue5[filterCount-1] = predictLeftX;
        queue6[filterCount-1] = predictLeftY;
        queue7[filterCount-1] = predictRightX;
        queue8[filterCount-1] = predictRightY;

        float sum1 = 0.0f;
        float sum2 = 0.0f;
        float sum3 = 0.0f;
        float sum4 = 0.0f;
        float sum5 = 0.0f;
        float sum6 = 0.0f;
        float sum7 = 0.0f;
        float sum8 = 0.0f;

        for(int i = 0; i < filterCount; i++) {
            sum1 += queue1[i];
            sum2 += queue2[i];
            sum3 += queue3[i];
            sum4 += queue4[i];
            sum5 += queue5[i];
            sum6 += queue6[i];
            sum7 += queue7[i];
            sum8 += queue8[i];
        }

        LOGD("[predictLine] queue1[0] : %f", queue1[0]);

        if( queue1[0] > 0.0f ) {
            meetLeftX = sum1 / (float) filterCount;
            meetLeftY = sum2 / (float) filterCount;
            meetRightX = sum3 / (float) filterCount;
            meetRightY = sum4 / (float) filterCount;
            predictLeftX = sum5 / (float) filterCount;
            predictLeftY = sum6 / (float) filterCount;
            predictRightX = sum7 / (float) filterCount;
            predictRightY = sum8 / (float) filterCount;
        }

        gMeetLeftX = meetLeftX;
        gMeetLeftY = meetLeftY;

        gMeetRightX = meetRightX;
        gMeetRightY = meetRightY;

        gPredictLeftX = predictLeftX;
        gPredictLeftY = predictLeftY;

        gPredictRightX = predictRightX;
        gPredictRightY = predictRightY;
    }

    return debugMat;

    //last all lines are detected...여기서 모든 라인이 다 찾아짐...
}

void getCropFourPoint(float *points) {

    int width = gPreviewWidth;
    int height = gPreviewHeight;

    int leftX = (int) (gMeetLeftX * gResizeRate);
    int leftY = (int) (gMeetLeftY * gResizeRate);

    int rightX = (int) (gMeetRightX * gResizeRate);
    int rightY = (int) (gMeetRightY * gResizeRate);

    int belowLeftX = (int) (gPredictLeftX * gResizeRate);
    int belowLeftY = (int) (gPredictLeftY * gResizeRate);

    int belowRightX = (int) (gPredictRightX * gResizeRate);
    int belowRightY = (int) (gPredictRightY * gResizeRate);

    int tempX = width - leftX;
    leftX = width - rightX;
    rightX = tempX;

    tempX = width - belowLeftX;
    belowLeftX = width - belowRightX;
    belowRightX = tempX;

    int tempY = leftY;
    leftY = rightY;
    rightY = tempY;

    tempY = belowLeftY;
    belowLeftY = belowRightY;
    belowRightY = tempY;

    points[0] = leftX;
    points[1] = leftY;

    points[2] = rightX;
    points[3] = rightY;

    points[4] = belowLeftX;
    points[5] = belowLeftY;

    points[6] = belowRightX;
    points[7] = belowRightY;
}

void getCropVerticalPoint(float *points) {
    int width = gPreviewWidth;
    int height = gPreviewHeight;

    points[0] = gVerticalUsePoint[0] * gResizeRate;
    points[1] = gVerticalUsePoint[1] * gResizeRate;
    points[2] = gVerticalUsePoint[2] * gResizeRate;
    points[3] = gVerticalUsePoint[3] * gResizeRate;

    points[0] = width - points[0];
    points[2] = width - points[2];
}

void getCropMatrixValue(float *cropMatValue, float belowCropValue, int targetWidth, int targetHeight) {
    vector<Point2f> oriPoint(4);
    vector<Point2f> tarPoint(4);

    Mat transMat;

    int width = targetWidth;
    int height = targetHeight;

    LOGD("[warpLog] targetWidth : %d, targetHeight : %d", targetWidth, targetHeight);

    if( targetWidth == 1616 ) {
        LOGD("[warpLog] %f %f %f %f, %f %f %f %f",
             gMeetLeftX * gResizeRate,
             gMeetLeftY * gResizeRate,
             gMeetRightX * gResizeRate,
             gMeetRightY * gResizeRate,
             gPredictLeftX * gResizeRate,
             gPredictLeftY * gResizeRate,
             gPredictRightX * gResizeRate,
             gPredictRightY * gResizeRate);
    }

    int leftX = (int) (gMeetLeftX * gResizeRate * (float)targetWidth / (float)gPreviewWidth);
    int leftY = (int) (gMeetLeftY * gResizeRate * (float)targetHeight / (float)gPreviewHeight);

    int rightX = (int) (gMeetRightX * gResizeRate * (float)targetWidth / (float)gPreviewWidth);
    int rightY = (int) (gMeetRightY * gResizeRate * (float)targetHeight / (float)gPreviewHeight);

    int belowLeftX = (int) (gPredictLeftX * gResizeRate * (float)targetWidth / (float)gPreviewWidth);
    int belowLeftY = (int) (gPredictLeftY * gResizeRate * (float)targetHeight / (float)gPreviewHeight);

    int belowRightX = (int) (gPredictRightX * gResizeRate * (float)targetWidth / (float)gPreviewWidth);
    int belowRightY = (int) (gPredictRightY * gResizeRate * (float)targetHeight / (float)gPreviewHeight);

    if( targetWidth == 1616 ) {
        LOGD("[warpLog] post %d %d %d %d, %d %d %d %d",
             leftX,
             leftY,
             rightX,
             rightY,
             belowLeftX,
             belowLeftY,
             belowRightX,
             belowRightY);
    }

    int tempX = width - leftX;
    leftX = width - rightX;
    rightX = tempX;

    tempX = width - belowLeftX;
    belowLeftX = width - belowRightX;
    belowRightX = tempX;

    int tempY = leftY;
    leftY = rightY;
    rightY = tempY;

    tempY = belowLeftY;
    belowLeftY = belowRightY;
    belowRightY = tempY;

    oriPoint[0] = Point2f(leftX, leftY);//- (gUpCut * 4.f)
    oriPoint[1] = Point2f(rightX, rightY);//- (gUpCut * 4.f)
    oriPoint[2] = Point2f(belowLeftX, belowLeftY);
    oriPoint[3] = Point2f(belowRightX, belowRightY);

    if( targetWidth == 1616 ) {
        LOGD("[warpLog] height : %d", height - (int) (belowCropValue * gResizeRate));
    }

    int leftHeight = height - (belowCropValue * gResizeRate * (float)targetHeight / (float)gPreviewHeight);
    int rightHeight = height - (belowCropValue * gResizeRate * (float)targetHeight / (float)gPreviewHeight);

    if( targetWidth == 1616 ) {
        LOGD("[warpLog] leftHeight : %d", leftHeight);
    }

    tarPoint[0] = Point2f(0, 0);//0
    tarPoint[1] = Point2f(width, 0);//3
    tarPoint[2] = Point2f(0, leftHeight);//1
    tarPoint[3] = Point2f(width, rightHeight);//2

    transMat = getPerspectiveTransform(oriPoint, tarPoint);

    Mat transInv = transMat.inv();

    int countUp = 0;
    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
            float tempValue = transInv.at<double>(i, j);
            cropMatValue[countUp] = tempValue;
            countUp++;
        }
    }
}

bool getCheckBookLocation() {
    return gBookLocationCorrect;
}

bool getCheckBookLocation(float areaCheckValue)
{
    gAreaCheckValue = areaCheckValue;

    static bool ret = false;

    if( gLineDetectStarted == false ) {

        ret = true;

        if (gHorizonUsePoint[0] == -1) {
            LOGI("[getTStatus] Horizontal is not exist : %d", gHorizonUsePoint[0]);
            ret = false;
        } else {
            if (gSideLeftUsePoint[0][0] == -1 && gSideRightUsePoint[0][0] == -1) {
                LOGI("[getTStatus] Horizontal is exist. but each sides is not exist");
                ret = false;
            }
            LOGI("[getTStatus] Nothing");
        }

//        LOGI("[getTStatus] Horizontal is exist. and some side is exist......%d %d", gVerticalUsePoint[0], gVerticalUsePoint[2]);
//        if (gVerticalUsePoint[0] < (gPreviewWidth / gResizeRate) / 2.0f - gAreaCheckValue || gVerticalUsePoint[0] > (gPreviewWidth / gResizeRate) / 2.0f + gAreaCheckValue
//            || gVerticalUsePoint[2] < (gPreviewWidth / gResizeRate) / 2.0f - gAreaCheckValue || gVerticalUsePoint[2] > (gPreviewWidth / gResizeRate) / 2.0f + gAreaCheckValue) {
//            LOGI("[getTStatus] Horizontal is exist. and some side is exist......But Vertical is Incorrect");
//            ret = false;
//        }
    }
    //ping-pong check
    return ret;
}

Mat warpPerspectiveByDetectedFourCorner(Mat inputMat, int resizeFactor, int belowCropValue) {
    Mat resultMat;
    //resize(inputMat, resultMat, Size(inputMat.cols/gResizeRate, inputMat.rows/gResizeRate), 0, 0, INTER_AREA );
    resize(inputMat, resultMat, Size(inputMat.cols/2, inputMat.rows/2), 0, 0, INTER_AREA );
    //resultMat = inputMat.clone();

    vector<Point2f> oriPoint(4);
    vector<Point2f> tarPoint(4);

    Mat trans;

    int width = resultMat.cols;
    int height = resultMat.rows;

    float resize = 2.0f;
    //float resize = 1.0f;

    oriPoint[0] = Point2f(gMeetLeftX*resize, gMeetLeftY*resize);//-gUpCut
    oriPoint[1] = Point2f(gMeetRightX*resize, gMeetRightY*resize);//-gUpCut
    oriPoint[2] = Point2f(gPredictLeftX*resize, gPredictLeftY*resize);
    oriPoint[3] = Point2f(gPredictRightX*resize, gPredictRightY*resize);

    LOGD("belowCropValue : %d", belowCropValue);

    //need to modify here...
    int leftHeight = height - belowCropValue;
    int rightHeight = height - belowCropValue;

    tarPoint[0] = Point2f(0, 0);
    tarPoint[1] = Point2f(width, 0);
    tarPoint[2] = Point2f(0, leftHeight);
    tarPoint[3] = Point2f(width, rightHeight);

    trans = getPerspectiveTransform(oriPoint, tarPoint);
    warpPerspective(resultMat, resultMat, trans, resultMat.size());

    return resultMat;
}

void drawGuideLineForDebug(Mat &inputMat, int divideValue, int multipleValue) {
//Draw Infos
    //draw guide lines result start...
//    Mat resizeMat2;
//    resize(resizeMat, resizeMat2, Size(src.cols/lineResizeFactor, src.rows/lineResizeFactor), 0, 0, INTER_AREA );
//
//    //horizotal
//    Mat roiMatResult2 = resizeMat2(Rect(0, startLine, resizeMat2.cols, endLine - startLine));
//
//    Mat hLineMat2Convert;
//    cvtColor(hLineMat2, hLineMat2Convert, COLOR_GRAY2RGBA);
//    hLineMat2Convert.copyTo(roiMatResult2, hLineMat2Convert);

    //sideBar Left
//    int divideValue = 25;
//    int multipleValue = 6;

    Mat roiMatResult3 = inputMat(Rect(0, 0, inputMat.cols / divideValue * multipleValue, inputMat.rows));

//    Mat hLineMat3Convert;
//    cvtColor(sideLeftLineMat2, hLineMat3Convert, COLOR_GRAY2RGBA);
//    hLineMat3Convert.copyTo(roiMatResult3, hLineMat3Convert);

    //sideBar Right
    Mat roiMatResult4 = inputMat(Rect(inputMat.cols-inputMat.cols / divideValue * multipleValue, 0, inputMat.cols / divideValue * multipleValue, inputMat.rows));

//    Mat hLineMat4Convert;
//    cvtColor(sideRightLineMat2, hLineMat4Convert, COLOR_GRAY2RGBA);
//    hLineMat4Convert.copyTo(roiMatResult4, hLineMat4Convert);
    //draw guide lines result end...

    //draw guide lines start...

    //horizotal
    line(inputMat, Point(0, gCropStartLine), Point(inputMat.cols, gCropStartLine),
         Scalar(0, 255, 0, 127), 1, CV_AA);
    line(inputMat, Point(0, gCropEndLine), Point(inputMat.cols, gCropEndLine),
         Scalar(0, 255, 0, 127), 1, CV_AA);

    //대각선
    vector<cv::Point> point;
    point.push_back(Point(inputMat.cols / divideValue * multipleValue, inputMat.rows));  //point1
    point.push_back(Point(inputMat.cols / divideValue * 2, inputMat.rows));  //point2
    point.push_back(Point(-inputMat.cols / divideValue * 3, inputMat.rows / 5));  //point3
    point.push_back(Point(inputMat.cols / divideValue * ((multipleValue-2) - 3), inputMat.rows / 5));  //point4

    polylines(inputMat, point, true, Scalar(0, 255, 0, 127), 2);

    vector<cv::Point> point2;
    point2.push_back(Point(inputMat.cols - inputMat.cols / divideValue * 2, inputMat.rows));  //point1
    point2.push_back(Point(inputMat.cols - inputMat.cols / divideValue * multipleValue, inputMat.rows));  //point2
    point2.push_back(Point(inputMat.cols - inputMat.cols / divideValue * ((multipleValue-2) - 3), inputMat.rows / 5));  //point3
    point2.push_back(Point(inputMat.cols + inputMat.cols / divideValue * 3, inputMat.rows / 5));  //point4

    polylines(inputMat, point2, true, Scalar(0, 255, 0, 127), 2);
    //draw guide lines end...
}
