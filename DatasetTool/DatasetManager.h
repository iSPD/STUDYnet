#pragma once

#undef _WINDOWS_
#include <afx.h>
#include <iostream>
#include <vector>
#include <string>

#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;
class DatasetManager
{
public:
	DatasetManager();
	~DatasetManager();

	vector<CString> mImageNameList;
	vector<CString> mImageTextList;
	vector<CString> mWordList;
	vector<CString> mFontPathList;
	vector<int> mFontWidthList;
	vector<Mat> mBGImageList;

	const int FONT_HEIGHT = 21;

	int LoadImageList(string path);
	int LoadWordList(string path);
	int ReadFileList(string fontPath, string format);
	int GetImageNameFromText(CString path);
	void CopyImgFile(string srcPath, string dstPath);
	
	char* UTF8ToANSI(const char *pszCode);
	char* ANSIToUTF8(const char * pszCode);

	int LoadBGImage(string path, string format);

private:

	CString mSavePath;
	CString mImagePath;
	
	
};

