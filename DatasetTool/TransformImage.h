#pragma once

#include <iostream>
#include <fstream> 

#include <opencv2/opencv.hpp>
#include <opencv2/freetype.hpp>

using namespace cv;
using namespace std;

class TransformImage
{
public:
	TransformImage();
	~TransformImage();
		
	const int MODE_ERODE = 100;
	const int MODE_DILATE = 200;

	void CopyOriginFile(string imgPath, string imgName, string imgText);
	int Rotate(string imgPath, string imgName, string imgText);
	int Brightness(string imgPath, string imgName, string imgText);
	int Contrast(string imgPath, string imgName, string imgText);
	int Perspective(string imgPath, string imgName, string imgText);

	
	
	int Brightness(Mat input, Mat output, string imgPath, string imgName, string imgText);
	int Contrast(Mat input, Mat output, string imgPath, string imgName, string imgText);
	int Perspective(Mat input, Mat output, string imgPath, string imgName, string imgText);

	int MakeTextImage(string fontPath, string myText, string myTextCv, int vocaIndex, int fontIndex);
	int MakeBGFGTransformTextImage(string fontPath, string myText, string myTextCv, int vocaIndex, int fontIndex, int in_fontWidth, Mat bgImg);
	int MakeBGTextImage(string fontPath, string myText, string myTextCv, int vocaIndex, int fontIndex, int in_fontWidth, Mat bgImg);
	int MakeTextImagePre(string fontPath, string myText, string myTextCv, int vocaIndex, int fontIndex, int in_fontWidth);
	void InitFreeType(string fontPath, int fontIndex);
	int GetFontWidth(string fontPath, string myText, string myTextCv, int vocaIndex, int fontIndex);
		
	void SetSaveFolder(string folder);
	void PrintMax();
	int GetRandomNumber(int min, int max);
	

	int mTotalCnt;    
	
	class FontInfo {
		int fontWidth;
		int baseline;
		int advance;
	};

	vector<FontInfo> mFontInfo;
	cv::Ptr<cv::freetype::FreeType2> mFreeType[240];
	#define BG_SOLID		0x1000
	#define BG_NOISE		0x2000
	#define FG_NONE			0x3000
	#define FG_EDGE			0x4000
	#define FG_SHADOW		0x5000
	#define TRANSFORM_NONE	0x6000
	#define TRANSFORM_DIST	0x7000
	#define TRANSFORM_PRSP	0x8000
	#define TRANSFORM_BLUR	0x9000

	int mBgModeCnt = -1;
	int mFgModeCnt = -1;
	int mTfModeCnt = -1;

private:
	string mSaveFolderName;
	string mTransformedImgListsFileName;
	
	ofstream mOutFile;
	int mMaxWidth;
	int mMaxHeight;

	void getAlphaBeta(double inBrightness, double inContrast, double *outAlpha, double *outBeta);
	Mat set3DRotate(Mat input, int option, int value, int backColor);
	int testcode();

	void saveFileNList(Mat input, char *textName, string myTextCv, int vocaIndex);
	void rotate(Mat& src, double angle, int backColor, Mat& dst);
	void distortion(Mat &src, float value, float textHeight, int menu, int backColor, Mat &dst);
	Mat set3DRotate2(Mat input, int option, int value, int backColor);
	Mat transparentOverlay(Mat src, Mat overlay, int xPosition, int yPosition, double scale, float rate);
	Mat setBlur(Mat src, int value);
	
	int _getBGFGTextColors(int *bg, int *text, bool edgeMode);

	Mat _rotate(Mat input, float angle, int backColor);
	Mat _brightnessAndContrast(Mat inputImage, double brightness, double contrast);
	Mat _makeEdgeShade(Mat inputImage, int mode, int iteration);
	
};

