
#include <opencv2/opencv.hpp>
#include <opencv2/core/types.hpp>
#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/core/types_c.h>

#include "opencv2/highgui.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/features2d.hpp"
#include "opencv2/features2d.hpp"
#include "opencv2/core/ocl.hpp"

using namespace cv;

void detectHorizonAndSideEdge(Mat grayInputMat, Mat hsvMat, float bookVericalMin, float bookVerticalMax, Mat &debuMat);

bool getCheckBookLocation();
void getCropFourPoint(float *points);
void getCropVerticalPoint(float *points);
void getCropMatrixValue(float *cropMatValueint, float belowCropValue, int targetWidth, int targetHeight);

Mat detectFourCorner(Mat grayInputMat, Mat inputDebugMat, float bookHoriLengthMin, float bookHoriLengthMax,
                      float bookVericalMin, float bookVerticalMax);

Mat warpPerspectiveByDetectedFourCorner(Mat inputMat, int resizeFactor, int gBelowCropValue);
