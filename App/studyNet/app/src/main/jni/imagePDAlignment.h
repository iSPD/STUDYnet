
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

//Mat alignmentImage(Mat inputMat);
Mat alignmentImage(Mat inputMat, int curveFixOn);

void setCurrentBookCoverAndPage(int coverIndex, int pageIndex, int belowCropValue, int resizeFactor);

void getAlignmentMatrixValue(int targetWidth, int targetHeight, int resizeRate,
                             float *alignLeftMat, float *alignRightMat);

void getAlignmentCurveValue(float *leftCurveValue, float *rightCurveValue);