

#include <iostream>
#include <cstdlib>  //abs(int)
#include <stdio.h>
#include <direct.h>
#include "TransformImage.h"
#include "TransformFlag.h"

using namespace std;
using namespace cv;

TransformImage::TransformImage()
{
	mSaveFolderName = "transformed/";
	mTransformedImgListsFileName = "listsnew.txt";
	mMaxWidth = 0;
	mMaxHeight = 0;
	_wsetlocale(LC_ALL, L"korean");

	mTotalCnt = 0;
}


TransformImage::~TransformImage()
{
	if (mOutFile.is_open())
		mOutFile.close();
}

void TransformImage::SetSaveFolder(string folder) {
	mSaveFolderName = folder;
	string annotationFilePath = folder + "gt.txt";  
	
	mOutFile.open(annotationFilePath, ios::app);
}

void TransformImage::PrintMax() {
	cout << "MaxWidth : " << mMaxWidth << " MaxHeight : " << mMaxHeight << endl;
}

void TransformImage::CopyOriginFile(string imgPath, string imgName, string imgText)
{
	string  imagepath = imgPath + imgName;
	ifstream inputfile(imagepath, ios::binary);
	if (inputfile.is_open()) {
		string originfilecpy = mSaveFolderName + imgName;

		ofstream outputfile(originfilecpy, ios::binary);
		if (outputfile.is_open()) {
			inputfile.seekg(0, ios::end);
			ifstream::pos_type size = inputfile.tellg();
			inputfile.seekg(0);
			char* buffer = new char[size];
			inputfile.read(buffer, size);
			outputfile.write(buffer, size);

			delete[] buffer;
			outputfile.close();
			inputfile.close();
		}
	}

	if (mOutFile.is_open()) {
		mOutFile << "./" << imgName << "" << imgText << endl;
	}
}

int TransformImage::Rotate(string imgPath, string imgName, string imgText)
{	
	string  imagepath = imgPath + imgName;
	double angle = 0.0;
	Mat inputImage = imread(imagepath, 1);
	int width = inputImage.cols;
	int height = inputImage.rows;
	
	int cx = width / 2;
	int cy = height / 2;

	int index = 0; 
	for (angle = -30.0, index = 0; angle <= 30; angle += 10.0, index++)
	{
		Point2d center = Point2d(cx, cy);

		Rect bounds = RotatedRect(center, inputImage.size(), angle).boundingRect();
		if (bounds.width < width) bounds.width = width; 
		if (bounds.height < height) bounds.height = height; 

		if (mMaxWidth < bounds.width) mMaxWidth = bounds.width;
		if (mMaxHeight < bounds.height) mMaxHeight = bounds.height;

		Mat resized = Mat::zeros(bounds.size(), inputImage.type());
		double offsetX = (bounds.width - width) / 2.0;
		double offsetY = (bounds.height - height) / 2.0;
		if (offsetX < 0) offsetX = 0;
		if (offsetY < 0) offsetY = 0;

		Rect roi = Rect(offsetX, offsetY, width, height);
		inputImage.copyTo(resized(roi));
		center += Point2d(offsetX, offsetY);

		Mat rotMat = getRotationMatrix2D(center, angle, 1.0);

		warpAffine(resized, resized, rotMat, resized.size(), INTER_LANCZOS4);

		stringstream ss;
		ss << index;
		string indexstr = ss.str(); 
		indexstr = "_r" + indexstr + ".jpg";
		int idx = imgName.find(".jpg"); 
		string imageNameNew = imgName;
		imageNameNew.replace(idx, 4, indexstr); 
		string outpath;
		outpath = mSaveFolderName + imageNameNew;

		imshow("out", resized); 
		imwrite(outpath, resized);
		
		if (mOutFile.is_open()) {
			mOutFile << "./" << imageNameNew << "" << imgText << endl;
		}

		resized.release();

	}
	inputImage.release();
	return 1;
}

Mat TransformImage::_brightnessAndContrast(Mat inputImage, double brightness, double contrast)
{
	Mat outImage;
	double alpha, beta;
	getAlphaBeta(brightness, contrast, &alpha, &beta);
	inputImage.convertTo(outImage, CV_8U, alpha, beta);

	return outImage;
}

static int cnt1 = 0;
static int cnt2 = 0;
Mat TransformImage::_makeEdgeShade(Mat inputImage, int mode, int iteration)
{
	Mat outImage;
	Mat src = inputImage.clone();
	Mat dst;
	char name[64] = { 0 };
	

	cv::Mat mask = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(3, 3), cv::Point(1, 1));
	if (mode == MODE_ERODE) {
		cv::erode(src, dst, mask, cv::Point(-1, -1), iteration);
		outImage = dst.clone();
		src.release();
		dst.release();
		cnt1++;
	}
	else if (mode == MODE_DILATE) {
		cv::dilate(src, dst, mask, cv::Point(-1, -1), iteration);

		outImage = dst.clone();
		src.release();
		dst.release();
		cnt2++;
	}
	return outImage;
}

int TransformImage::Brightness(string imgPath, string imgName, string imgText)
{
	string  imagepath = imgPath + imgName;
	Mat inputImage = imread(imagepath, 1);

	double brightness = 0.0;
	double contrast = 0.0;
	double alpha, beta;

	Mat outImage;

	int index = 0;
	
	for (brightness = /*-100.0*/-60.0, index = 0; brightness <= 100; brightness += 40.0, index++)
	{
		getAlphaBeta(brightness, contrast, &alpha, &beta);

		printf("[%d] brightness : %f \n", index, brightness);
		inputImage.convertTo(outImage, CV_8U, alpha, beta);

		stringstream ss;
		ss << index;
		string indexstr = ss.str(); 
		indexstr = "_b" + indexstr + ".jpg";
		int idx = imgName.find(".jpg"); 
		string imageNameNew = imgName;
		imageNameNew.replace(idx, 4, indexstr); 

		string outpath;
		outpath = mSaveFolderName + imageNameNew;

		cout << imageNameNew << endl;

		imshow("out", outImage); 
		imwrite(outpath, outImage);

		if (mOutFile.is_open()) {
			mOutFile << "./" << imageNameNew << "" << imgText << endl;
		}

	}

	outImage.release();
	inputImage.release();

	return 1;
}

int TransformImage::Contrast(string imgPath, string imgName, string imgText)
{
	string  imagepath = imgPath + imgName;
	Mat inputImage = imread(imagepath, 1);

	double brightness = 0.0;
	double contrast = 0.0;
	double alpha, beta;

	Mat outImage;

	int index = 0;

	for (contrast = -80.0, index = 0; contrast <= 40; contrast += 30.0, index++)
	{
		getAlphaBeta(brightness, contrast, &alpha, &beta);

		printf("[%d] contrast : %f \n", index, contrast);
		inputImage.convertTo(outImage, CV_8U, alpha, beta);

		stringstream ss;
		ss << index;
		string indexstr = ss.str(); 
		indexstr = "_c" + indexstr + ".jpg";
		int idx = imgName.find(".jpg"); 
		string imageNameNew = imgName;
		imageNameNew.replace(idx, 4, indexstr); 

		string outpath;
		outpath = mSaveFolderName + imageNameNew;

		cout << imageNameNew << endl;

		imshow("out", outImage); 
		imwrite(outpath, outImage);

		if (mOutFile.is_open()) {
			mOutFile << "./" << imageNameNew << "" << imgText << endl;
		}
	}

	outImage.release();
	inputImage.release();

	return 1;
}


int TransformImage::Perspective(string imgPath, string imgName, string imgText)
{
	string  imagepath = imgPath + imgName;
	Mat inputImage = imread(imagepath, 1);
	int w = inputImage.cols;
	int h = inputImage.rows;

	Mat outImage;

	int index = 0;

	float testBench[8][2] = { 
		{ 1, 40.0f }, { 2, 40.0f }, { 3, 40.0f }, { 4, 40.0f },
		{ 5, 30.0f }, { 6, 30.0f }, { 7, 30.0f }, { 8, 30.0f }
};
	

	//for (int direction = 1; direction <= 8; direction++) 
	{
		for(int i = 0; i < 8; i++) {
			printf("direction: %d , ratio : %f \n", testBench[i][0], testBench[i][1]);
			outImage = set3DRotate(inputImage, testBench[i][0], testBench[i][1], 0);

			stringstream ss;
			ss << index;
			string indexstr = ss.str(); 
			indexstr = "_p" + indexstr + ".jpg";
			int idx = imgName.find(".jpg"); 
			string imageNameNew = imgName;
			imageNameNew.replace(idx, 4, indexstr); 
			string outpath;
			outpath = mSaveFolderName + imageNameNew;

			cout << imageNameNew << endl;

			imshow("out", outImage); 
			imwrite(outpath, outImage);

			if (mOutFile.is_open()) {
				mOutFile << "./" << imageNameNew << "" << imgText << endl;
			}

			index++;
		}
	}

	inputImage.release();
	outImage.release();

	return 1;
}

void TransformImage::getAlphaBeta(double inBrightness, double inContrast, double *outAlpha, double *outBeta) {
	double alpha, beta;
	if (inContrast > 0) {
		double delta = 127.0 * inContrast / 100;
		alpha = 255.0 / (255.0 - delta * 2);
		beta = alpha * (inBrightness - delta);
	}
	else {
		double delta = -127.0 * inContrast / 100;
		alpha = (256.0 - delta * 2) / 255.0;
		beta = alpha * inBrightness + delta;
	}
	*outAlpha = alpha;
	*outBeta = beta;

}

Mat TransformImage::set3DRotate(Mat input, int option, int value, int backColor)
{
	Point2f inputQuad[4];
	Point2f outputQuad[4];

	// Lambda Matrix
	Mat lambda(2, 4, CV_32FC1);
	Mat output;

	// Set the lambda matrix the same type and size as input
	lambda = Mat::zeros(input.rows, input.cols, input.type());

	inputQuad[0] = Point2f(0, 0);
	inputQuad[1] = Point2f(input.cols, 0);
	inputQuad[2] = Point2f(0, input.rows);
	inputQuad[3] = Point2f(input.cols, input.rows);

	int reducePixelX1 = 0, reducePixelX2 = 0, reducePixelY1 = 0, reducePixelY2 = 0;
	if (option == 1) {
		reducePixelX1 = input.cols * value / 100;
	}
	else if (option == 2) {
		reducePixelX2 = input.cols * value / 100;
	}
	else if (option == 3) {
		reducePixelY1 = input.rows * value / 100;
	}
	else if (option == 4) {
		reducePixelY2 = input.rows * value / 100;
	}
	else if (option == 5) {
		reducePixelX2 = input.cols * value / 100;
		reducePixelY1 = input.rows * value / 100;
	}
	else if (option == 6) {
		reducePixelY2 = input.rows * value / 100;
		reducePixelX2 = input.cols * value / 100;
	}
	else if (option == 7) {
		reducePixelX1 = input.cols * value / 100;
		reducePixelY1 = input.rows * value / 100;
	}
	else if (option == 8) {
		reducePixelX1 = input.cols * value / 100;
		reducePixelY2 = input.rows * value / 100;
	}
	outputQuad[0] = Point2f(0 + reducePixelX1 / 2, 0 + reducePixelY1 / 2); 
	outputQuad[1] = Point2f(input.cols - reducePixelX1 / 2, reducePixelY2 / 2);  
	outputQuad[2] = Point2f(0 + reducePixelX2 / 2, input.rows - reducePixelY1 / 2);  
	outputQuad[3] = Point2f(input.cols - reducePixelX2 / 2, input.rows - reducePixelY2 / 2);  

	lambda = getPerspectiveTransform(inputQuad, outputQuad);
	cv::warpPerspective(input, output, lambda, output.size(), INTER_LINEAR, BORDER_CONSTANT, Scalar::all(backColor));

	return output;
}

int TransformImage::testcode()
{
	Mat frame;
	VideoCapture cap;
	cap.open(0);
	int deviceID = 0;             
	int apiID = cv::CAP_ANY;      
	cap.open(deviceID + apiID);
	if (!cap.isOpened()) {
		cerr << "ERROR! Unable to open camera\n";
		return -1;
	}

	cout << "Start grabbing" << endl
		<< "Press any key to terminate" << endl;
	for (;;)
	{
		cap.read(frame);
		if (frame.empty()) {
			cerr << "ERROR! blank frame grabbed\n";
			break;
		}
		imshow("Live", frame);
		if (waitKey(5) >= 0)
			break;
	}
	return 0;
}

Mat TransformImage::_rotate(Mat input, float angle, int backColor)
{
	Mat inputImage = input.clone();
	Mat output;

	int width = inputImage.cols;
	int height = inputImage.rows;

	int cx = width / 2;
	int cy = height / 2;

	Point2d center = Point2d(cx, cy);

	Rect bounds = RotatedRect(center, inputImage.size(), angle).boundingRect();
	if (bounds.width < width) bounds.width = width; 
	if (bounds.height < height) bounds.height = height; 

	Mat transformed = Mat::zeros(bounds.size(), inputImage.type());
	transformed.setTo(backColor);

	double offsetX = (bounds.width - width) / 2.0;
	double offsetY = (bounds.height - height) / 2.0;
	if (offsetX < 0) offsetX = 0;
	if (offsetY < 0) offsetY = 0;

	Rect roi = Rect(offsetX, offsetY, width, height);
	inputImage.copyTo(transformed(roi));
	center += Point2d(offsetX, offsetY);

	Mat rotMat = getRotationMatrix2D(center, angle, 1.0);

	warpAffine(transformed, transformed, rotMat, transformed.size(), INTER_LANCZOS4);

	imshow("out", transformed); 
		
	output = transformed.clone();
	
	inputImage.release();
	transformed.release();

	return output;
}

int TransformImage::Brightness(Mat input, Mat output, string imgPath, string imgName, string imgText)
{
	return 1;
}
int TransformImage::Contrast(Mat input, Mat output, string imgPath, string imgName, string imgText)
{
	return 1;
}
int TransformImage::Perspective(Mat input, Mat output, string imgPath, string imgName, string imgText)
{
	return 1;
}

void TransformImage::rotate(cv::Mat& src, double angle, int backColor, cv::Mat& dst)
{
	double angle2 = angle * 3.1416 / 180;
	int newHeight = tan(angle2)  * src.cols / 2;

	newHeight = abs(newHeight);

	dst = Mat::zeros(Size(src.cols, (src.rows / 2 + 1 + newHeight) * 2), src.type());
	dst.setTo(backColor);

	Mat roi = dst(Rect(0, (dst.rows - src.rows) / 2, src.cols, src.rows));
	src.copyTo(roi);

	cv::Point2f pt(dst.cols / 2, dst.rows / 2);
	cv::Mat r = cv::getRotationMatrix2D(pt, angle, 1.0);

	cv::warpAffine(dst, dst, r, dst.size(), 1, 0, Scalar::all(backColor));

}

void TransformImage::distortion(Mat &src, float value, float textHeight, int menu, int backColor, Mat &dst)
{
	dst = Mat::zeros(Size(src.cols, (int)((float)src.rows * value * 1.2f)), src.type()); 
	dst.setTo(backColor);
	Mat middleRoi = dst(Rect(0, (dst.rows - src.rows) / 2, src.cols, src.rows));
	src.copyTo(middleRoi);

	for (int i = 0; i < dst.cols; i++)
	{
		Mat roi = dst(Rect(i, 0, 1, dst.rows));
		float angle = (180.f / dst.cols) * (float)i;

		double angle2 = (double)angle * 3.1416 / 180;

		float targetY;
		if (angle <= 180)
		{
			targetY = sin(angle2) - 0.5f;
		}
		else
		{
			targetY = sin(angle2 - 3.1416) - 0.5f;
		}

		targetY = targetY * (dst.rows - (textHeight * 2.0f));

		if (menu == 1) targetY = -targetY;

		float transValue[6] = { 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, -targetY };
		Mat transMat(2, 3, CV_32F, transValue);
		warpAffine(roi, roi, transMat, Size(roi.cols, roi.rows), INTER_NEAREST, 1, Scalar::all(backColor));
	}

}

Mat TransformImage::set3DRotate2(Mat input, int option, int value, int backColor)
{
	Point2f inputQuad[4];
	Point2f outputQuad[4];

	Mat lambda(2, 4, CV_32FC1);
	Mat output;

	lambda = Mat::zeros(input.rows, input.cols, input.type());

	inputQuad[0] = Point2f(0, 0);
	inputQuad[1] = Point2f(input.cols, 0);
	inputQuad[2] = Point2f(0, input.rows);
	inputQuad[3] = Point2f(input.cols, input.rows);

	
	if (option == 1)
	{
		int reducePixel = input.cols * value / 100;

		outputQuad[0] = Point2f(0 + reducePixel / 2, 0);
		outputQuad[1] = Point2f(input.cols - reducePixel / 2, 0); 
		outputQuad[2] = Point2f(0, input.rows); 
		outputQuad[3] = Point2f(input.cols, input.rows); 
	}
	else if (option == 2)
	{
		int reducePixel = input.cols * value / 100;

		outputQuad[0] = Point2f(0, 0);
		outputQuad[1] = Point2f(input.cols, 0);
		outputQuad[2] = Point2f(0 + reducePixel / 2, input.rows);
		outputQuad[3] = Point2f(input.cols - reducePixel / 2, input.rows);
	}
	else if (option == 3)
	{
		int reducePixel = input.rows * value / 100;

		outputQuad[0] = Point2f(0, 0 + reducePixel / 2);
		outputQuad[1] = Point2f(input.cols, 0);
		outputQuad[2] = Point2f(0, input.rows - reducePixel / 2);
		outputQuad[3] = Point2f(input.cols, input.rows);
	}
	else if (option == 4)
	{
		int reducePixel = input.rows * value / 100;

		outputQuad[0] = Point2f(0, 0);
		outputQuad[1] = Point2f(input.cols, reducePixel / 2);
		outputQuad[2] = Point2f(0, input.rows);
		outputQuad[3] = Point2f(input.cols, input.rows - reducePixel / 2);
	}

	lambda = getPerspectiveTransform(inputQuad, outputQuad);
	warpPerspective(input, output, lambda, input.size(), INTER_LINEAR, BORDER_CONSTANT, Scalar::all(backColor));
	return output;
}

Mat TransformImage::transparentOverlay(Mat src, Mat overlay, int xPosition, int yPosition, double scale, float rate)
{
	int h = overlay.rows;
	int w = overlay.cols;
	int rows = src.rows;
	int cols = src.cols;
	int y = yPosition;
	int x = xPosition;

	Mat result = Mat(src.size(), src.type());

	for (int i = 0; i < h; i++)
	{
		for (int j = 0; j < w; j++)
		{
			if ((x + i) >= rows || (y + j) >= cols)
			{
				continue;
			}

			float alpha = (float)overlay.at<Vec4b>(i, j)[3] / 255.0;
			alpha = alpha * rate;

			result.at<Vec3b>(x + i, y + j)[0] = (uchar)(alpha * (float)overlay.at<Vec4b>(i, j)[0] + (1.0f - alpha) * (float)src.at<Vec3b>(x + i, y + j)[0]);
			result.at<Vec3b>(x + i, y + j)[1] = (uchar)(alpha * (float)overlay.at<Vec4b>(i, j)[1] + (1.0f - alpha) * (float)src.at<Vec3b>(x + i, y + j)[1]);
			result.at<Vec3b>(x + i, y + j)[2] = (uchar)(alpha * (float)overlay.at<Vec4b>(i, j)[2] + (1.0f - alpha) * (float)src.at<Vec3b>(x + i, y + j)[2]);
		}
	}

	return result;
}

Mat TransformImage::setBlur(Mat src, int value)
{
	Mat dst;
	blur(src, dst, Size(value, value));
	return dst;
}

#define TEST_ADVANCE
#define KOREAN_LETTER_LENGTH 2

int TransformImage::MakeTextImage(string fontPath, string myText, string myTextCv, int vocaIndex, int fontIndex)
{
	cv::Ptr<cv::freetype::FreeType2> ft2;
	ft2 = cv::freetype::createFreeType2();
	ft2->setSplitNumber(8);
	ft2->loadFontData(fontPath, 0);

	int textLength = myText.length();
	int thickness = -1;
	int fontHeight = 21;// 21;
	int linestyle = LINE_AA;
	int baseline = 0;
	int baseline2 = 0;
	int advance = 0;
#ifdef TEST_ADVANCE
	//Size rect = ft2->getTextSize(myText + "0",
	Size rect = ft2->getTextSize(myTextCv,
		fontHeight,
		thickness,
		&baseline,
		&advance
	);

#else
	Size rect = ft2->getTextSize(myText + "0",
		fontHeight,
		thickness,
		&baseline
	);

	Size rectZero = ft2->getTextSize("0",
		fontHeight,
		thickness,
		&baseline2);
#endif
	
	rect.height = fontHeight;

	if (rect.width < rect.height) 
	{ 
		rect.width = fontHeight * (textLength / 2);
	}

	
	float widthMargin = 1.1f;
	float heightMargin = 1.55f;

	int boxWidth = (int)((float)rect.width * widthMargin);
	int boxHeight = (int)((float)rect.height * heightMargin);

	cv::Point myPoint;
	myPoint.x = (boxWidth - rect.width) / 2;
	myPoint.y = 0 + boxHeight / 2 + rect.height / 2;

	int backColor[3] = { 255 / 2, 255 / 4, 255 }; 
	int textColor[3] = { 255 / 2, 255 / 4, 255 }; 
	int bfCount = 0;


	Mat colorMat[6];
	for (int i = 0; i < 3; i++)
	{
		Mat temp = Mat::zeros(Size(boxWidth, boxHeight), CV_8UC3);
		temp.setTo(backColor[i]);
		
		for (int j = 0; j < 3; j++)
		{
			if (i != j)
			{
				cv::Point textOrg((temp.cols - advance) / 2, 
					(temp.rows + rect.height) / 2);
				
				ft2->putText(temp, myTextCv, textOrg, fontHeight,
					cv::Scalar::all(textColor[j]), thickness, linestyle, true);

				colorMat[bfCount] = temp.clone();

				char name[64];
				sprintf_s(name, "%d_%d_ft%d_bgfg%d.jpg", mTotalCnt, vocaIndex, fontIndex, bfCount);
				saveFileNList(colorMat[bfCount], name, myTextCv, vocaIndex);
				bfCount++;
			}
		}
	} 

	char textName[256];
	strcpy_s(textName, myText.c_str());

	static int chooseMenu = -1;
	chooseMenu++;
	if (chooseMenu > 5)
	{
		chooseMenu = 0;
	}

	if (chooseMenu == 0) {
		Mat skewMat[6][2];
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 2; j++)
			{
				int rotValue = 5; //3
				if (j == 0) rotValue = -5; //-3

				rotate(colorMat[i], rotValue, backColor[i / 2], skewMat[i][j]);

				resize(skewMat[i][j], skewMat[i][j], Size(skewMat[i][j].cols, 32));

				char name[64];
				sprintf_s(name, "%d_%d_ft%d_bgfg%d_skw%d.jpg", mTotalCnt, vocaIndex, fontIndex, i, j);
				saveFileNList(skewMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if ((chooseMenu == 1) && (textLength > KOREAN_LETTER_LENGTH)) 
		Mat distortionMat[6][2];
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 2; j++)
			{
				distortion(colorMat[i], 1.0f, rect.height, j/*menu*/, backColor[i / 2], distortionMat[i][j]);
				resize(distortionMat[i][j], distortionMat[i][j], Size(distortionMat[i][j].cols, 32));

				char name[64];
				sprintf_s(name, "%d_%d_ft%d_bgfg%d_dst%d.jpg", mTotalCnt, vocaIndex, fontIndex, i, j);
				saveFileNList(distortionMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if (chooseMenu == 2) {
		Mat rot3DMat[6][4];
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 4; j++)
			{
				rot3DMat[i][j] = set3DRotate(colorMat[i], j + 1, 20, backColor[i / 2]);
				char name[64];
				sprintf_s(name, "%d_%d_ft%d_bgfg%d_prsp%d.jpg", mTotalCnt, vocaIndex, fontIndex, i, j);
				saveFileNList(rot3DMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if (chooseMenu == 3) {
		Mat backModiMat[6][4];
		for (int i = 0; i < 6; i++)
		{
			Mat rainSnow[4];
			rainSnow[0] = imread("bg1.png", IMREAD_UNCHANGED);
			rainSnow[1] = imread("bg2.png", IMREAD_UNCHANGED);

			rainSnow[2] = Mat::zeros(colorMat[0].size(), CV_8UC4);
			int maxDiv = 4;
			for (int aa = 1; aa < maxDiv; aa++)
			{
				line(rainSnow[2], Point(0, colorMat[0].rows * aa / maxDiv), Point(colorMat[0].cols, colorMat[0].rows * aa / maxDiv), Scalar::all(255 - backColor[i / 2]), 2);
			}

			rainSnow[3] = Mat::zeros(colorMat[0].size(), CV_8UC4);
			int maxDiv2 = 15;
			for (int bb = 1; bb < maxDiv2; bb++)
			{
				line(rainSnow[3], Point(colorMat[0].cols * bb / maxDiv2, 0), Point(colorMat[0].cols * bb / maxDiv2, colorMat[0].rows), Scalar::all(255 - backColor[i / 2]), 2);
			}

			resize(rainSnow[0], rainSnow[0], colorMat[0].size());
			resize(rainSnow[1], rainSnow[1], colorMat[0].size());

			for (int j = 0; j < 4; j++)
			{
				backModiMat[i][j] = transparentOverlay(colorMat[i], rainSnow[j], 0, 0, 1.0, 0.5f/*0.5f*/);

				char name[64];
				sprintf_s(name, "%d_%d_ft%d_bgfg%d_bg%d.jpg", mTotalCnt, vocaIndex, fontIndex, i, j);
				saveFileNList(backModiMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if (chooseMenu == 4) {
		Mat blurMat[6][2];
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 1; j++)
			{
				blurMat[i][j] = setBlur(colorMat[i], (j + 1) * 2 + 1);
				char name[64];
				sprintf_s(name, "%d_%d_ft%d_bgfg%d_blur%d.jpg", mTotalCnt, vocaIndex, fontIndex, i, j);
				saveFileNList(blurMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if (chooseMenu == 5) {
		Mat brightnessMat[6][2];
		Mat contrastMat[6][2];
		double brightnessFactors[2] = { -40.0, 50.0 };
		double contrastFactors[2] = { -40.0, 20.0 };
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 2; j++)
			{
				brightnessMat[i][j] = _brightnessAndContrast(colorMat[i], brightnessFactors[j], 0.0);
				contrastMat[i][j] = _brightnessAndContrast(colorMat[i], 0.0, contrastFactors[j]);
				char name[64];
				sprintf_s(name, "%d_%d_ft%d_bgfg%d_bright%d.jpg", mTotalCnt, vocaIndex, fontIndex, i, j);
				saveFileNList(brightnessMat[i][j], name, myTextCv, vocaIndex);
				sprintf_s(name, "%d_%d_ft%d_bgfg%d_cntrst%d.jpg", mTotalCnt, vocaIndex, fontIndex, i, j);
				saveFileNList(contrastMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	return 1;
}

void TransformImage::saveFileNList(Mat input, char *fileName, string word, int vocaIndex)
{
	string strFileName(fileName);
	string strOutPath = mSaveFolderName + strFileName;

	imwrite(strOutPath, input);
	
	if (mOutFile.is_open()) {
		mOutFile << strFileName << "\t" << word << endl; 
	}

	mTotalCnt++;
}

void TransformImage::InitFreeType(string fontPath, int fontIndex)
{
	mFreeType[fontIndex] = cv::freetype::createFreeType2();
	mFreeType[fontIndex]->setSplitNumber(8);
	mFreeType[fontIndex]->loadFontData(fontPath, 0);
}

int TransformImage::GetFontWidth(string fontPath, string myText, string myTextCv, int vocaIndex, int fontIndex)
{
	int textLength = myText.length();
	int thickness = -1;
	int fontHeight = 21;// 21;
	int linestyle = LINE_AA;
	int baseline = 0;
	int baseline2 = 0;
	int advance = 0;
	Size rect;
	if (mFreeType[fontIndex]) {
		rect = mFreeType[fontIndex]->getTextSize(myTextCv,
			fontHeight,
			thickness,
			&baseline,
			&advance
		);
	}
	
	return rect.width;

}

int TransformImage::GetRandomNumber(int min, int max) 
{
	static const double fraction = 1.0 / (RAND_MAX + 1.0); 
	return min + static_cast<int>((max - min + 1) * (std::rand() * fraction));
}

//#define USE_8STEP_COLOR
#define USE_16STEP_COLOR

int TransformImage::_getBGFGTextColors(int *bg, int *text, bool edgeMode)
{	
	//8ܰ  
	//index : 0   , 1    , 2    , 3     , 4      , 5      , 6      , 7
	//color : 0~31, 32~63, 64~95, 96~127, 128~159, 160~191, 192~223, 224~255

	//16ܰ  
	//index : 0,       1,       2,       3,       4,       5,       6,       7,
	//color : 0~15,    16~31,   32~47,   48~63,   64~79,   80~95,   96~111,  112~127, 
	//index : 8,       9,       10,      11,      12,      13,      14,      15,
	//        128~143, 144~159, 160~175, 176~191, 192~207, 208~223, 224~239, 240~255

#ifdef USE_16STEP_COLOR
	const int BG_IDX_START = 6;
	const int BG_IDX_END = 15;
	const int TXT_IDX_START = 0;
	const int TXT_IDX_END = 10;
	const int COLOR_UNIT = 16;    
	const int COLOR_ABS = 40; 
#else
	const int BG_IDX_START = 3;
	const int BG_IDX_END = 7;
	const int TXT_IDX_START = 0;
	const int TXT_IDX_END = 2;
	const int COLOR_UNIT = 32;
	const int COLOR_ABS = 15; 
#endif

	int bgColIndex = -1;
	int txtColIndex = -1;
	int fgColIndex = -1;
	int fgColor = 0;
	int bgColor, txtColor;
	while (true) {
		bgColIndex = GetRandomNumber(BG_IDX_START, BG_IDX_END); //BG
		txtColIndex = GetRandomNumber(TXT_IDX_START, TXT_IDX_END); //Text
		fgColIndex = GetRandomNumber(0, 7); 

		if (edgeMode == false) {  //no edge, no shadow color
			if (bgColIndex != txtColIndex) {
				
				bgColor = GetRandomNumber(COLOR_UNIT * bgColIndex, COLOR_UNIT * (bgColIndex + 1) - 1);
				txtColor = GetRandomNumber(COLOR_UNIT * txtColIndex, COLOR_UNIT * (txtColIndex + 1) - 1);
				int absvalue = abs(bgColor - txtColor);

				if (bgColor > txtColor && absvalue >= COLOR_ABS) {   
					*bg = bgColor;
					*text = txtColor;
					break;
				}
			}
		}
		else { 
			if ((bgColIndex != fgColIndex) && (fgColIndex != txtColIndex)) {
				bgColor = GetRandomNumber(32 * bgColIndex, 32 * (bgColIndex + 1) - 1);
				txtColor = GetRandomNumber(32 * txtColIndex, 32 * (txtColIndex + 1) - 1);
				fgColor = GetRandomNumber(32 * fgColIndex, 32 * (fgColIndex + 1) - 1);
				int absvalue1 = abs(bgColor - fgColor);
				int absvalue2 = abs(fgColor - txtColor);
				if ((absvalue1 > 60) && (absvalue2 > 60)) {  
					*bg = bgColor;
					*text = txtColor;
					break;
				}
			}
		}
	}
	return fgColor; 
}

int mBgMode[] = { BG_SOLID, BG_SOLID, BG_SOLID, BG_SOLID/*, BG_NOISE, BG_NOISE*/ };
int mFgMode[] = { FG_NONE, FG_NONE, FG_NONE, FG_NONE, FG_NONE, /*FG_NONE, FG_NONE, FG_NONE, FG_NONE, FG_NONE, 
				  FG_NONE, FG_NONE, FG_NONE, FG_NONE, FG_NONE, FG_NONE, FG_EDGE, FG_EDGE, FG_SHADOW */}; 
int mTfMode[] = { TRANSFORM_NONE, TRANSFORM_NONE, TRANSFORM_NONE, 
					TRANSFORM_NONE, TRANSFORM_NONE, TRANSFORM_NONE,
					TRANSFORM_NONE, TRANSFORM_NONE, TRANSFORM_NONE,
					/*TRANSFORM_DIST, TRANSFORM_PRSP, TRANSFORM_PRSP */};
int mShadowOffset[][2] = {/**/{-1, -1}, /**/{1, -1}, /**/{-2, -2}, /**/{2, -2} };
int TransformImage::MakeBGFGTransformTextImage(string fontPath, string myText, string myTextCv,
	int vocaIndex, int fontIndex, int in_fontWidth,
	Mat bgImg)
{
	bool bgSetFlag = false;
	int textLength = myText.length();
	int thickness = -1;
	int fontHeight = transformFlag[fontIndex][1];
	int linestyle = LINE_AA;
	int baseline = 0;
	int baseline2 = 0;
	int advance = 0;

	int numCharCnt = 0;    
	int puncCharCnt = 0; 
	for (int i = 0; i < textLength; i++) {
		if (48 <= myText[i] && myText[i] <= 57) {		
			numCharCnt++;
		}	
		else if (myText[i] == '~' || myText[i] == '&' || myText[i] == '?' || myText[i] == '-') {
			puncCharCnt++;
		}
		
	}
	numCharCnt = (numCharCnt + 1) / 2; 
	textLength = textLength + numCharCnt + puncCharCnt;

	if (transformFlag[fontIndex][0] == -1) { 
		return -1;
	}
	
	int realFontWidth = in_fontWidth;
	int realFontHeight = fontHeight;
	int yMargin = transformFlag[fontIndex][2]; 
	int xMargin = 0; 
	
	Size rect = mFreeType[fontIndex]->getTextSize(myTextCv,
		fontHeight,
		thickness,
		&baseline,
		&advance
	);

	rect.height = fontHeight;
	
	float widthMargin = 1.1f;
	float heightMargin = transformFlag[fontIndex][3] / 100.f;

	int boxWidth = advance;
	int boxHeight = (int)((float)rect.height * heightMargin);

	mBgModeCnt = 0;// ++mBgModeCnt % 6;// 5;
	mFgModeCnt = 0;// ++mFgModeCnt % 19;// 10;
	mTfModeCnt = 0;// ++mTfModeCnt % 12;

	Mat fontBackground;	
	if (((mTfMode[mTfModeCnt] == TRANSFORM_NONE) && (mBgMode[mBgModeCnt] == BG_NOISE)) && (transformFlag[fontIndex][0] != 0)) {
		//fontBackground
		if (bgImg.cols < boxWidth || bgImg.rows < boxHeight) {  bg ̹  ܾڽ  .
			fontBackground = bgImg.clone();
			cv::resize(fontBackground, fontBackground, Size(boxWidth, boxHeight));
		}
		else {
			fontBackground = Mat::zeros(Size(boxWidth, boxHeight), CV_8UC3); //rotate    
			int startXRandom = GetRandomNumber(0, bgImg.cols - boxWidth);
			int startYRandom = GetRandomNumber(0, bgImg.rows - boxHeight);
			bgImg(Rect(startXRandom, startYRandom, boxWidth, boxHeight)).copyTo(fontBackground);
		}
		bgSetFlag = true;
	}
	else {
		fontBackground = Mat::zeros(Size(boxWidth, boxHeight), CV_8UC3);
	}

	cv::Point textOrg((fontBackground.cols - advance) / 2 + xMargin,
		(fontBackground.rows + rect.height) / 2 + yMargin);   ̰ų ø  yMargin 


	int bgcolor, txtcolor, fgcolor;
	Mat erodeDilateMat;
	Mat step2ResultMat;
	char name[64];


	if ((mFgMode[mFgModeCnt] == FG_NONE) || (transformFlag[fontIndex][0] == 0)) {
		fgcolor = _getBGFGTextColors(&bgcolor, &txtcolor, false); 
	}
	else {
		fgcolor = _getBGFGTextColors(&bgcolor, &txtcolor, true);
	}

	if ( !((mTfMode[mTfModeCnt] == TRANSFORM_NONE) && (mBgMode[mBgModeCnt] == BG_NOISE))) {
		fontBackground.setTo(bgcolor);
	}
	else {
			if (bgSetFlag == true) {
				if (mFgMode[mFgModeCnt] == FG_NONE) {   
					txtcolor = GetRandomNumber(32 * 0, 32 * (0 + 1) - 1);
				}
				else {
					fontBackground.setTo(bgcolor);
				}
			}
			else {
				fontBackground.setTo(bgcolor);
			}
			
	}
		
	if ((mFgMode[mFgModeCnt] == FG_NONE) || (transformFlag[fontIndex][0] == 0)) {
		mFreeType[fontIndex]->putText(fontBackground, myTextCv, textOrg, realFontHeight/*fontHeight*/,
			cv::Scalar::all(txtcolor), thickness, linestyle, true);
		step2ResultMat = fontBackground.clone();
	}
	else { 
		mFreeType[fontIndex]->putText(fontBackground, myTextCv, textOrg, realFontHeight/*fontHeight*/,
			cv::Scalar::all(fgcolor), thickness, linestyle, true);

		int iterationNum = GetRandomNumber(1, 2);
		
		if (bgcolor < fgcolor) {
			erodeDilateMat = _makeEdgeShade(fontBackground, MODE_DILATE, iterationNum); 
		}
		else {
			erodeDilateMat = _makeEdgeShade(fontBackground, MODE_ERODE, iterationNum);
		}

		if ((mFgMode[mFgModeCnt] == FG_SHADOW) && (iterationNum == 1)) {
			
			int offsetIndex = 0; 

			cv::Point textOrgNew(textOrg.x + mShadowOffset[offsetIndex][0], textOrg.y + mShadowOffset[offsetIndex][1]); 

			mFreeType[fontIndex]->putText(erodeDilateMat, myTextCv, textOrgNew, realFontHeight/*fontHeight*/,
				cv::Scalar::all(txtcolor), thickness, linestyle, true); 
		}
		else {
			mFreeType[fontIndex]->putText(erodeDilateMat, myTextCv, textOrg, realFontHeight/*fontHeight*/,
				cv::Scalar::all(txtcolor), thickness, linestyle, true); 
		}

		step2ResultMat = erodeDilateMat.clone();
		erodeDilateMat.release(); 
	}

	Mat step3ResultMat;
	if ((mTfMode[mTfModeCnt] == TRANSFORM_NONE) || (transformFlag[fontIndex][0] == 0)) {
		step3ResultMat = step2ResultMat.clone();
	}
	else if ((mTfMode[mTfModeCnt] == TRANSFORM_DIST) &&
			 (textLength > (KOREAN_LETTER_LENGTH * 2))) { ں distortion
		distortion(step2ResultMat, 1.0f, rect.height, GetRandomNumber(0,1), bgcolor, step3ResultMat);
		cv::resize(step3ResultMat, step3ResultMat, Size(step3ResultMat.cols, 32));
	}
	else if (mTfMode[mTfModeCnt] == TRANSFORM_PRSP) {
		step3ResultMat = set3DRotate(step2ResultMat, GetRandomNumber(1,8), 20, bgcolor);
	}
	else {
		step3ResultMat = step2ResultMat.clone();
	}

	Mat step4ResultMat;
	if ((mTfMode[mTfModeCnt] == TRANSFORM_NONE) && (mBgMode[mBgModeCnt] == BG_NOISE)) {
		step4ResultMat = step3ResultMat.clone();
	}
	else {
		int rotValue = GetRandomNumber(-10, 10);
		rotate(step3ResultMat, rotValue, bgcolor, step4ResultMat);
	}

	int newCols = (int)((32 * step4ResultMat.cols) / (float)step4ResultMat.rows);
	cv::resize(step4ResultMat, step4ResultMat, Size(newCols, 32));

	Mat step5ResultMat;
	if ((mBgMode[mBgModeCnt] == BG_NOISE) && (transformFlag[fontIndex][0] == 1)&& (mTfMode[mTfModeCnt] != TRANSFORM_NONE)) {
		int newRows = 32;
		
		Mat noiseBGMat;
		if (bgImg.cols < newCols || bgImg.rows < newRows) { 
			noiseBGMat = bgImg.clone();
			cv::resize(noiseBGMat, noiseBGMat, Size(newCols, newRows));
		}
		else {
			noiseBGMat = Mat::zeros(Size(newCols, newRows), CV_8UC3);    
			int startXRandom = GetRandomNumber(0, bgImg.cols - newCols);
			int startYRandom = GetRandomNumber(0, bgImg.rows - newRows);
			bgImg(Rect(startXRandom, startYRandom, newCols, newRows)).copyTo(noiseBGMat);
		}

		addWeighted(noiseBGMat, 0.2, step4ResultMat, 0.8, 0, step5ResultMat);
		cvtColor(step5ResultMat, step5ResultMat, COLOR_BGR2GRAY);
		noiseBGMat.release();
	}
	else if ((mBgMode[mBgModeCnt] == BG_SOLID) || (transformFlag[fontIndex][0] == 0)) {
		cvtColor(step4ResultMat, step4ResultMat, COLOR_BGR2GRAY);
		step5ResultMat = step4ResultMat.clone();
	}
	else {
		cvtColor(step4ResultMat, step4ResultMat, COLOR_BGR2GRAY);
		step5ResultMat = step4ResultMat.clone();
	}

	
	sprintf_s(name, "ft%d/%d_%d_ft%d.jpg", fontIndex, mTotalCnt, vocaIndex, fontIndex);

	saveFileNList(step5ResultMat, name, myTextCv, vocaIndex);
	
	fontBackground.release();
	step2ResultMat.release();
	step3ResultMat.release();
	step4ResultMat.release();
	step5ResultMat.release();

	return 1;

}

int TransformImage::MakeBGTextImage(string fontPath, string myText, string myTextCv, 
									int vocaIndex, int fontIndex, int in_fontWidth, 
									Mat bgImg)
{
	int textLength = myText.length();
	int thickness = -1;
	int fontHeight = 21;// 21;
	int linestyle = LINE_AA;
	int baseline = 0;
	int baseline2 = 0;
	int advance = 0;

	Size rect = mFreeType[fontIndex]->getTextSize(myTextCv,
		fontHeight,
		thickness,
		&baseline,
		&advance
	);

	rect.height = fontHeight;
	rect.width = in_fontWidth * textLength / 2;

	float widthMargin = 1.1f;
	float heightMargin = 1.55f;

	int boxWidth = (int)((float)rect.width * widthMargin);
	int boxHeight = (int)((float)rect.height * heightMargin);

	cv::Point myPoint;
	myPoint.x = (boxWidth - rect.width) / 2;
	myPoint.y = 0 + boxHeight / 2 + rect.height / 2;

	int bfCount = 0;
	
	Mat colorMat[6];
	Mat dilateMat[6];
	Mat fontBackground = Mat::zeros(Size(boxWidth, boxHeight), CV_8UC3);
	cv::Point textOrg((fontBackground.cols - advance) / 2,
		(fontBackground.rows + rect.height) / 2);

	int bgcolor, txtcolor, fgcolor;

	char name[64];
	
	fgcolor = _getBGFGTextColors(&bgcolor, &txtcolor, false); 


	int rotValue = 0;
	Mat skewResultMat;
	rotValue = GetRandomNumber(-10, 10);

	fgcolor = _getBGFGTextColors(&bgcolor, &txtcolor, true);
	fontBackground.setTo(bgcolor);
	mFreeType[fontIndex]->putText(fontBackground, myTextCv, textOrg, fontHeight,
		cv::Scalar::all(fgcolor), thickness, linestyle, true);

	//dilate or erode
	if (bgcolor < fgcolor) {
		dilateMat[bfCount] = _makeEdgeShade(fontBackground, MODE_DILATE, 1);
	}
	else {
		dilateMat[bfCount] = _makeEdgeShade(fontBackground, MODE_ERODE, 1);
	}

	cv::Point textOrgNew(textOrg.x + 1, textOrg.y - 1);

	mFreeType[fontIndex]->putText(dilateMat[bfCount], myTextCv, textOrgNew, fontHeight,
		cv::Scalar::all(txtcolor), thickness, linestyle, true);

	rotate(dilateMat[bfCount], rotValue, bgcolor, skewResultMat);

	int newCols = (int)((32 * skewResultMat.cols) / (float)skewResultMat.rows);
	resize(skewResultMat, skewResultMat, Size(newCols, 32));

	int newRows = 32;
	Mat noiseBGMat = Mat::zeros(Size(newCols, newRows), CV_8UC3); 

	int startXRandom = GetRandomNumber(0, bgImg.cols - newCols);
	int startYRandom = GetRandomNumber(0, bgImg.rows - newRows);

	bgImg(Rect(startXRandom, startYRandom, newCols, newRows)).copyTo(noiseBGMat);

	Mat result;
	addWeighted(noiseBGMat, 0.3, skewResultMat, 0.7, 0, result);
	
	cvtColor(result, result, COLOR_BGR2GRAY);

	sprintf_s(name, "%d_%d_ft%d_skw%d_noise.jpg", mTotalCnt, vocaIndex, fontIndex, rotValue);
	saveFileNList(result, name, myTextCv, vocaIndex);

	fontBackground.release();

	return 1;

}

int TransformImage::MakeTextImagePre(string fontPath, string myText, string myTextCv, int vocaIndex, int fontIndex, int in_fontWidth)
{
	int textLength = myText.length();
	int thickness = -1;
	int fontHeight = 21;// 21;
	int linestyle = LINE_AA;
	int baseline = 0;
	int baseline2 = 0;
	int advance = 0;

	Size rect = mFreeType[fontIndex]->getTextSize(myTextCv,
		fontHeight,
		thickness,
		&baseline,
		&advance
	);
	
	rect.height = fontHeight;
	rect.width = in_fontWidth * textLength / 2;

	float widthMargin = 1.1f;
	float heightMargin = 1.55f;

	int boxWidth = (int)((float)rect.width * widthMargin);
	int boxHeight = (int)((float)rect.height * heightMargin);

	cv::Point myPoint;
	myPoint.x = (boxWidth - rect.width) / 2;
	myPoint.y = 0 + boxHeight / 2 + rect.height / 2;

	int backColor[3] = { 0,   255 / 2, 200}; 
	int textColor[3] = { 200, 255 / 2, 0 }; 
	int textColor2[3] = { 0,   255 / 2, 200 }; 

	int bfCount = 0;

	Mat colorMat[6];
	for (int i = 0; i < 3; i++)
	{
		Mat temp = Mat::zeros(Size(boxWidth, boxHeight), CV_8UC3);
		temp.setTo(backColor[i]);

		for (int j = 0; j < 3; j++)
		{
			if (i != j)
			{				
				cv::Point textOrg((temp.cols - advance) / 2, 
					(temp.rows + rect.height) / 2);

				mFreeType[fontIndex]->putText(temp, myTextCv, textOrg, fontHeight,
					cv::Scalar::all(textColor[j]), thickness, linestyle, true);

				colorMat[bfCount] = temp.clone();

				char name[64];
				sprintf_s(name, "ft%d/%d_%d_ft%d_bgfg%d.jpg", fontIndex, mTotalCnt, vocaIndex, fontIndex, bfCount);
				saveFileNList(colorMat[bfCount], name, myTextCv, vocaIndex);
				bfCount++;
			}
		}
	   
	char textName[256];
	strcpy_s(textName, myText.c_str());

	static int chooseMenu =  -1;

	if (chooseMenu == 0) {
		Mat skewMat[6][2];
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 2; j++)
			{
				int rotValue = 5; //3
				if (j == 0) rotValue = -5; //-3

				rotate(colorMat[i], rotValue, backColor[i / 2], skewMat[i][j]);

				resize(skewMat[i][j], skewMat[i][j], Size(skewMat[i][j].cols, 32));

				char name[64];
				sprintf_s(name, "ft%d/%d_%d_ft%d_bgfg%d_skw%d.jpg", fontIndex, mTotalCnt, vocaIndex, fontIndex, i, j);

				saveFileNList(skewMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if ((chooseMenu == 1) && (textLength > KOREAN_LETTER_LENGTH)) { 
		Mat distortionMat[6][2];
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 2; j++)
			{
				distortion(colorMat[i], 1.0f, rect.height, j/*menu*/, backColor[i / 2], distortionMat[i][j]);
				resize(distortionMat[i][j], distortionMat[i][j], Size(distortionMat[i][j].cols, 32));

				char name[64];
				sprintf_s(name, "ft%d/%d_%d_ft%d_bgfg%d_dst%d.jpg", fontIndex, mTotalCnt, vocaIndex, fontIndex, i, j);

				saveFileNList(distortionMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if (chooseMenu == 2) {
		//Option-4 * #4
		Mat rot3DMat[6][4];
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 4; j++)
			{
				rot3DMat[i][j] = set3DRotate(colorMat[i], j + 1, 20, backColor[i / 2]);
				char name[64];
				sprintf_s(name, "ft%d/%d_%d_ft%d_bgfg%d_prsp%d.jpg", fontIndex, mTotalCnt, vocaIndex, fontIndex, i, j);

				saveFileNList(rot3DMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if (chooseMenu == 3) {
		Mat backModiMat[6][4];
		for (int i = 0; i < 6; i++)
		{
			Mat rainSnow[4];
			rainSnow[0] = imread("bg1.png", IMREAD_UNCHANGED);
			rainSnow[1] = imread("bg2.png", IMREAD_UNCHANGED);

			rainSnow[2] = Mat::zeros(colorMat[0].size(), CV_8UC4);
			int maxDiv = 4;
			for (int aa = 1; aa < maxDiv; aa++)
			{
				line(rainSnow[2], Point(0, colorMat[0].rows * aa / maxDiv), Point(colorMat[0].cols, colorMat[0].rows * aa / maxDiv), Scalar::all(255 - backColor[i / 2]), 2);
			}

			rainSnow[3] = Mat::zeros(colorMat[0].size(), CV_8UC4);
			int maxDiv2 = 15;
			for (int bb = 1; bb < maxDiv2; bb++)
			{
				line(rainSnow[3], Point(colorMat[0].cols * bb / maxDiv2, 0), Point(colorMat[0].cols * bb / maxDiv2, colorMat[0].rows), Scalar::all(255 - backColor[i / 2]), 2);
			}

			resize(rainSnow[0], rainSnow[0], colorMat[0].size());
			resize(rainSnow[1], rainSnow[1], colorMat[0].size());

			for (int j = 0; j < 4; j++)
			{
				backModiMat[i][j] = transparentOverlay(colorMat[i], rainSnow[j], 0, 0, 1.0, 0.5f/*0.5f*/);

				char name[64];
				sprintf_s(name, "ft%d/%d_%d_ft%d_bgfg%d_bg%d.jpg", fontIndex, mTotalCnt, vocaIndex, fontIndex, i, j);

				saveFileNList(backModiMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if (chooseMenu == 4) {
		Mat blurMat[6][2];
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 1; j++)
			{
				blurMat[i][j] = setBlur(colorMat[i], (j + 1) * 2 + 1);
				char name[64];
				sprintf_s(name, "ft%d/%d_%d_ft%d_bgfg%d_blur%d.jpg", fontIndex, mTotalCnt, vocaIndex, fontIndex, i, j);

				saveFileNList(blurMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}

	else if (chooseMenu == 5) {
		Mat brightnessMat[6][2];
		Mat contrastMat[6][2];
		double brightnessFactors[2] = { -40.0, 50.0 };
		double contrastFactors[2] = { -40.0, 20.0 };
		for (int i = 0; i < 6; i++)
		{
			for (int j = 0; j < 2; j++)
			{
				brightnessMat[i][j] = _brightnessAndContrast(colorMat[i], brightnessFactors[j], 0.0);
				contrastMat[i][j] = _brightnessAndContrast(colorMat[i], 0.0, contrastFactors[j]);
				char name[64];
				sprintf_s(name, "ft%d/%d_%d_ft%d_bgfg%d_bright%d.jpg", fontIndex, mTotalCnt, vocaIndex, fontIndex, i, j);

				saveFileNList(brightnessMat[i][j], name, myTextCv, vocaIndex);
				sprintf_s(name, "ft%d/%d_%d_ft%d_bgfg%d_cntrst%d.jpg", fontIndex, mTotalCnt, vocaIndex, fontIndex, i, j);

				saveFileNList(contrastMat[i][j], name, myTextCv, vocaIndex);
			}
		}
	}
	else if (chooseMenu == 6) {  
		Mat tempMat[6];
		Mat tempMat2[6];
	
		Mat src1 = imread("data/dilate12_src.jpg");
		Mat src2 = imread("data/dilate14.jpg");
		Mat result;
		bitwise_or(src1, src2, result);
		imwrite("data/result2/result_or.jpg", result);
		bitwise_and(src1, src2, result);
		imwrite("data/result2/result_and.jpg", result);
		bitwise_xor(src1, src2, result);
		imwrite("data/result2/result_xor.jpg", result);
		bitwise_not(src1, src2, result);
		imwrite("data/result2/result_not.jpg", result);

	}
	return 1;
}

