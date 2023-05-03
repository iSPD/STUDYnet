

#include <iostream>
#include <fstream> 
#include <string>
#include <stdio.h>
#include <vector>
#include <afx.h>
#include <locale.h>  

#include <WTypes.h >
#include <oleauto.h>

#include "DatasetManager.h"

DatasetManager::DatasetManager()
{
	mSavePath = _T("./");
	mImagePath = _T("./");
}


DatasetManager::~DatasetManager()
{
	
}

int DatasetManager::LoadImageList(string path)
{
	int lineCount = 0;
	
	ifstream annotationFile(path);
	string linebuffer;
	if (annotationFile.is_open()) {
		while (annotationFile.peek() != EOF) {
			getline(annotationFile, linebuffer); 
			char *name;
			name = UTF8ToANSI(linebuffer.c_str());
			cout << lineCount << " " << name << endl;
			CString lineStr = (LPCSTR)(LPSTR)name; //char* to CString
			int findIdx = lineStr.Find(_T(" "));
			if (findIdx != -1) {
				CString imgName = lineStr.Left(findIdx);
				mImageNameList.push_back(imgName);
				CString imgText = lineStr.Mid(findIdx);
				mImageTextList.push_back(imgText);
			}
			lineCount++;
		}

		annotationFile.close();
	}

	return(int) mImageNameList.size();
}

int DatasetManager::LoadWordList(string path)
{
	int lineCount = 0;

	ifstream wordListFile(path);
	string linebuffer;
	if (wordListFile.is_open()) {
		while (wordListFile.peek() != EOF) {
			getline(wordListFile, linebuffer); 
			char *wordstring;
			 wordstring = UTF8ToANSI(linebuffer.c_str());
			CString lineStr = (LPCSTR)(LPSTR)wordstring; 
			mWordList.push_back(lineStr);

			lineCount++;
		}

		wordListFile.close();
	}
	return lineCount;
}

char* DatasetManager::ANSIToUTF8(const char * pszCode)
{
	int		nLength, nLength2;
	BSTR	bstrCode;
	char*	pszUTFCode = NULL;

	nLength = MultiByteToWideChar(CP_ACP, 0, pszCode, lstrlen(pszCode), NULL, NULL);
	bstrCode = SysAllocStringLen(NULL, nLength);
	MultiByteToWideChar(CP_ACP, 0, pszCode, lstrlen(pszCode), bstrCode, nLength);

	nLength2 = WideCharToMultiByte(CP_UTF8, 0, bstrCode, -1, pszUTFCode, 0, NULL, NULL);
	pszUTFCode = (char*)malloc(nLength2 + 1);
	WideCharToMultiByte(CP_UTF8, 0, bstrCode, -1, pszUTFCode, nLength2, NULL, NULL);

	return pszUTFCode;
}

char* DatasetManager::UTF8ToANSI(const char *pszCode)
{
	BSTR    bstrWide;
	char*   pszAnsi;
	int     nLength;
	nLength = MultiByteToWideChar(CP_UTF8, 0, pszCode, lstrlen(pszCode) + 1, NULL, NULL);
	bstrWide = SysAllocStringLen(NULL, nLength);
	MultiByteToWideChar(CP_UTF8, 0, pszCode, lstrlen(pszCode) + 1, bstrWide, nLength);
	nLength = WideCharToMultiByte(CP_ACP, 0, bstrWide, -1, NULL, 0, NULL, NULL);
	pszAnsi = new char[nLength];
	WideCharToMultiByte(CP_ACP, 0, bstrWide, -1, pszAnsi, nLength, NULL, NULL);
	SysFreeString(bstrWide);
	return pszAnsi;
}

int DatasetManager::ReadFileList(string fontPath, string format) 
{
	CString ext = format.c_str();
	CString sctrFontPath = fontPath.c_str();
	CString tpath = sctrFontPath + ext;

	CFileFind finder;
	BOOL bWorking = finder.FindFile(tpath); 

	CString fileName;
	CString DirName;
		
	ofstream fontFile; 
	fontFile.open("fontList.txt");  

	int fontCnt = 0;

	while (bWorking)
	{
		bWorking = finder.FindNextFile();
		if (finder.IsArchived())
		{
			CString _fileName = finder.GetFileName();

			if (_fileName == _T(".") ||
				_fileName == _T("..") ||
				_fileName == _T("Thumbs.db")) continue;

			fileName = finder.GetFileName();// GetFileTitle();
			mFontPathList.push_back(sctrFontPath+fileName);

			fontFile << "ft" << fontCnt << " " << sctrFontPath + fileName << endl; //for debug
			
			fontCnt++;
		}		
	}

	fontFile.close(); //for debug
	return mFontPathList.size();
}

int DatasetManager::GetImageNameFromText(CString path)
{
	int lineCount = 0;

	ifstream annotationFile(path);
	string linebuffer;
	if (annotationFile.is_open()) {
		while (annotationFile.peek() != EOF) {
			getline(annotationFile, linebuffer); 
			char *name;
			name = UTF8ToANSI(linebuffer.c_str());
			CString lineStr = (LPCSTR)(LPSTR)name; //char* to CString
			int findIdx = lineStr.Find(_T(" "));
			if (findIdx != -1) {
				CString imgName = lineStr.Left(findIdx);
				mImageNameList.push_back(imgName);
				CString imgText = lineStr.Mid(findIdx);
				mImageTextList.push_back(imgText);

			}
			lineCount++;
		}

		annotationFile.close();
	}
	
	return(int)mImageNameList.size();
}

int DatasetManager::LoadBGImage(string imageFolderPath, string format)
{
	CString ext = format.c_str();
	CString sctrImgPath = imageFolderPath.c_str();
	CString tpath = sctrImgPath + ext;

	CFileFind finder;
	BOOL bWorking = finder.FindFile(tpath); //

	CString fileName;
	CString DirName;

	string imageFullPathcv;

	int imgCnt = 0;

	while (bWorking)
	{
		bWorking = finder.FindNextFile();
		if (finder.IsArchived())
		{
			CString _fileName = finder.GetFileName();

			if (_fileName == _T(".") ||
				_fileName == _T("..") ||
				_fileName == _T("Thumbs.db")) continue;

			fileName = finder.GetFileName();

			imageFullPathcv = CT2CA(sctrImgPath + fileName);
			Mat bgimg = imread(imageFullPathcv, IMREAD_UNCHANGED);

			mBGImageList.push_back(bgimg);

			imgCnt++;
		}
	}

	return mBGImageList.size();
}
