//#include <opencv2/opencv.hpp>

#include <iostream>
#include <stdio.h>
#include <string>
#include <afx.h>
#include <direct.h>

#include "TransformImage.h"
#include "DatasetManager.h"
#include <opencv2/freetype.hpp>

using namespace std;
using namespace cv;

//font 40
//const string str_SAVE_FOLER_PATH = "data/transformed/";
//const string strFONT_WIDTH_FILE_PATH = "data/fontWidth.txt";
//const string strFONT_FILES_FOLDER = "data/fonts/";

//font 147
//const string str_SAVE_FOLER_PATH = "data/kor147/";
//const string strFONT_WIDTH_FILE_PATH = "data/fonts147/fontWidth.txt";
//const string strFONT_FILES_FOLDER = "data/fonts147/";

//MODIFY-POINT
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/kor217/"; 
//const string str_SAVE_FOLDER_PATH = "data/result20200107/";
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/dataset/kor159/"; 
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/dataset/kor159simple/"; //�ܾ�30���� ������ �����ͼ��� �����ϱ� ����.
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/dataset/kor165simple/"; //�ܾ�30���� ������ �����ͼ��� �����ϱ� ����. 6�� ��Ʈ �߰�
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/dataset/kor165/"; 
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/dataset/kor165_add/"; //�߰��ܾ�+�����ȣ+�������Դܾ�
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/dataset/kor165_add_noNum/"; //�߰��ܾ�+�����ȣ
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/dataset/eng_words_with_punc/"; //����ܾ�+�����ȣ
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/dataset/kor165_hw/";
const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/datasetToolText_Folder_byword_new_forHandWriting/datasetTool/data/hw_kor174fonts_dataset/";
//const string str_SAVE_FOLDER_PATH = "D:/DatasetTools/datasetToolText_Folder_byword_new_forHandWriting/datasetTool/data/";

//const string strFONT_WIDTH_FILE_PATH = "D:/DatasetTools/fonts217/fontWidth_217.txt";
//const string strFONT_WIDTH_FILE_PATH = "D:/DatasetTools/fonts159/fontWidth_159.txt";
//const string strFONT_WIDTH_FILE_PATH = "D:/DatasetTools/fonts159/fontWidth_159_new.txt";
const string strFONT_WIDTH_FILE_PATH = "D:/DatasetTools/fonts165_forstorybook/fontWidth_165_forstorybook.txt";

//const string strFONT_FILES_FOLDER = "D:/DatasetTools/fonts217/";
//const string strFONT_FILES_FOLDER = "D:/DatasetTools/fonts159/";
//const string strFONT_FILES_FOLDER = "D:/DatasetTools/fonts165_forstorybook/";
const string strFONT_FILES_FOLDER = "D:/DatasetTools/datasetToolText_Folder_byword_new_forHandWriting/datasetTool/fonts_hw_kor/";

//const string strBG_IMAGE_FOLDER = "D:/DatasetTools/bgimage/";
const string strBG_IMAGE_FOLDER = "D:/DatasetTools/bgimagestory/";

//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/hangeul_word_list6000.txt";
//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/hangeul_word_list50186.txt";
//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/hangeul_word_list_fortest.txt";
//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/hangeul_word_list50300.txt";
//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/hangeul_simple.txt";
//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/hangeul_word_add.txt";
//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/hangeul_word_add_punctuation_number.txt";
//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/hangeul_word_add_punctuation_noNumber.txt";
//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/eng_words_with_punctuation.txt";
//const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/hangeul_simple_child.txt";
const string str_HANGUEL_WORD_LIST_PATH = "D:/DatasetTools/wordlist/for_handwriting/hangeul_word_list50300_addword_punc_number.txt";

//#define TEST_ROUTINE //MODIFY-POINT
#ifdef TEST_ROUTINE
int g_testWordCount = 40;
//int g_testFontCount = 217;
int g_testFontCount = 159;
#endif

void createTransformedImages(TransformImage *transformImg, string imgPath, string imgName, string imgText);
char* __ANSIToUTF8(const char * pszCode)
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
int fontTestCode1(string fontPath) //font�� �ε��ؼ� ȭ�鿡 �׷���
{
	//String text = u8"Funny text inside the �ڽ�";  //�ѱ��� ����ϱ� ���� u8�� �ٿ��� ��.
	string str1 = "��";
	char wordBuf[255] = { 0, };
	strcpy(wordBuf, str1.c_str());
	//cout << wordBuf << endl;
	char *wordBufUTF8 = new char[255];
	wordBufUTF8 = __ANSIToUTF8(wordBuf);
	string text(wordBufUTF8);

	//string text = "��2";

	int fontHeight = 60;
	int thickness = -1;
	int linestyle = LINE_AA;// 8=LINE_8, LINE_AA=anti alias;
	Mat img(600, 800, CV_8UC3, Scalar::all(0));
	int baseline = 0;
	cv::Ptr<cv::freetype::FreeType2> ft2;

	ft2 = cv::freetype::createFreeType2();
	//ft2->loadFontData("./mplus-1p-regular.ttf", 0);
	//ft2->loadFontData("c:\\Windows\\Fonts\\HYWULM.ttf", 0);
	//ft2->loadFontData("c:\\Windows\\Fonts\\CURLZ___.ttf", 0);
	ft2->loadFontData(fontPath, 0);
	Size textSize = ft2->getTextSize(text,
		fontHeight,
		thickness,
		&baseline);
	if (thickness > 0) {
		baseline += thickness;
	}


	cout << "font name : " << fontPath << endl;
	cout << "text width : " << textSize.width << " baseline : " << baseline << endl;
	if (textSize.width < 0) {
		textSize.width = 60;
		cout << "text width fix : " << textSize.width << endl << endl;
	}
	textSize.height = 60;
	// center the text
	Point textOrg((img.cols - textSize.width) / 2,
		(img.rows + textSize.height) / 2);
	// draw the box
	rectangle(img, textOrg + Point(0, baseline),
		textOrg + Point(textSize.width, -textSize.height),
		Scalar(0, 255, 0), 1, 8);
	// ... and the baseline first
	line(img, textOrg + Point(0, thickness),
		textOrg + Point(textSize.width, thickness),
		Scalar(0, 0, 255), 1, 8);
	// then put the text itself
	ft2->putText(img, text, textOrg, fontHeight,
		Scalar::all(255), thickness, linestyle, true);

	imshow("out", img);
	for (;;)
	{
		if (waitKey(5) >= 0)
			break;
	}

	return 0;
}

int fontTestCode2() {
	Mat src = imread("sky.jpg", IMREAD_COLOR);

	if (src.empty()) {
		cerr << "Image load failed!" << endl;
		return -1;
	}

	// FreeType2 ��ü ����
	Ptr<cv::freetype::FreeType2> ft2 = cv::freetype::createFreeType2();
	Ptr<cv::freetype::FreeType2> ft2_1 = cv::freetype::createFreeType2();

	// ����ü �۲� �ҷ�����
	//ft2->loadFontData("c:\\Windows\\Fonts\\batang.ttc", 0);
	ft2->loadFontData("c:\\Windows\\Fonts\\HMFMOLD.ttf", 0);
	////cv::String filename = "HYBDAM.ttf";
	//ft2->loadFontData("HMKMMAG.ttf", 0);
	ft2_1->loadFontData("c:\\Windows\\Fonts\\HMKMMAG.ttf", 0);

	// ���ڿ� ���
	ft2->putText(src, u8"Hello?", Point(50, 50), 50, Scalar(255, 255, 255), -1, LINE_AA, false);
	ft2->putText(src, u8"�ȳ��ϼ���!", Point(50, 120), 50, Scalar(0, 255, 255), -1, LINE_AA, false);
	ft2_1->putText(src, u8"Hello?", Point(50, 180), 50, Scalar(255, 255, 255), -1, LINE_AA, false);
	ft2_1->putText(src, u8"�ȳ��ϼ���!", Point(50, 230), 50, Scalar(0, 255, 255), -1, LINE_AA, false);

	imshow("src", src);
	waitKey(0);
	return 0;
}

void fontTestCode3(string fontPath)
{
	//cv::String text = "\ufc45\ufb50\ufc44\ufb50\ufc43\ufb50\ufc42\ufb50\ufc41";
	//cv::String text = "\ufc44\ufb50\ufc43\ufb50\ufc42\ufb50\ufc41";

	string str1 = "��";
	char wordBuf[255] = { 0, };
	strcpy(wordBuf, str1.c_str());
	//cout << wordBuf << endl;
	char *wordBufUTF8 = new char[255];
	wordBufUTF8 = __ANSIToUTF8(wordBuf);
	string text(wordBufUTF8);

	int fontHeight = 60;
	int thickness = -1;
	int linestyle = LINE_AA;// 8=LINE_8, LINE_AA=anti alias;
	//Mat img(600, 800, CV_8UC3, Scalar::all(0));
	Mat img(100, 200, CV_8UC3, Scalar::all(0));
	int baseline = 0;
	int advance = 0;

	cv::Ptr<cv::freetype::FreeType2> ft2;
	ft2 = cv::freetype::createFreeType2();

	ft2->setSplitNumber(8);

	ft2->loadFontData(fontPath, 0);
	cv::Size textSize = ft2->getTextSize(text,
		//cv::Size textSize = ft2->getTextSize(str1+"0", //�̷��� �ϸ� ���� width�� �̻��ϰ� ����
		fontHeight,
		thickness,
		&baseline,
		&advance);
	if (thickness > 0) {
		baseline += thickness;
	}
	cout << "font name : " << fontPath << endl;
	cout << "text width : " << textSize.width << " baseline : " << baseline << endl;
	if (textSize.width < 0) {
		textSize.width = fontHeight;
		cout << "text width fix : " << textSize.width << endl << endl;
	}
	textSize.height = fontHeight;

	// show baseline and advance
	std::cout << "baseline " << baseline << ", advance "
		<< advance << ", Size " << textSize << std::endl;

	// center the text
	cv::Point textOrg((img.cols - advance) / 2, // ( img.cols - textSize.width ) / 2,
		(img.rows + textSize.height) / 2);

	// draw the box
	std::cout << "rectanle " << cv::Point(advance - textSize.width, baseline)
		<< ", " << cv::Point(advance, -textSize.height) << std::endl;
	cv::rectangle(img, textOrg + cv::Point(advance - textSize.width, baseline),
		textOrg + cv::Point(advance, -textSize.height),
		cv::Scalar(0, 255, 0), 1, 8);

	// ... and the vertical origin
	std::cout << "origin " << cv::Point(0, baseline)
		<< ", " << cv::Point(0, thickness) << std::endl;
	cv::line(img, textOrg + cv::Point(0, baseline),
		textOrg + cv::Point(0, -textSize.height),
		cv::Scalar(0, 0, 255), 1, 8);

	// ... and the baseline first
	std::cout << "baseline " << cv::Point(advance - textSize.width, thickness)
		<< ", " << cv::Point(advance, thickness) << std::endl;
	cv::line(img, textOrg + cv::Point(advance - textSize.width, thickness),
		textOrg + cv::Point(advance, thickness),
		cv::Scalar(0, 0, 255), 1, 8);

	// then put the text itself
	ft2->putText(img, text, textOrg, fontHeight,
		cv::Scalar::all(255), thickness, linestyle, true);

	cv::imshow("img_path", img);
	imwrite("data/img_path.jpg", img);

	cv::waitKey(0);
}
/* ImageTransformTest()
   DESC : ������ ������ lists.txt�� �о� �̹����� ���� �� trasformed������ �����ϰ�(��������) ���ο� �̹��� ����Ʈ�� listsnew.txt�� ����Ѵ�.
   �غ� :   �����̹��� �������丮 "testImages/"
			�����̹�������Ʈ �ؽ�Ʈ���ϸ�(path ����) "testImages/lists.txt"
			���������� ������ �������丮 "testImages/transformed/"
			���������͸���Ʈ �ؽ�Ʈ���ϸ� "testImages/transformed/listsnew.txt"
 */
int ImageTransformTest(void)
{
	TransformImage *transformImg = new TransformImage();
	DatasetManager *dsetMngr = new DatasetManager();
	transformImg->SetSaveFolder("testImages/transformed/"); //HARD-CODING

	int imageCount = dsetMngr->LoadImageList(_T("testImages/lists.txt")); //HARD-CODING
	if (imageCount > 0) {
		for (int i = 0; i < imageCount; i++) {
			//createTransformedImages(transformImg, "testimage.jpg");
			string imagePath = "testImages/"; //HARD-CODING
			string imageName;
			imageName = CT2CA(dsetMngr->mImageNameList.at(i));
			string imageText;
			imageText = CT2CA(dsetMngr->mImageTextList.at(i));
			createTransformedImages(transformImg, imagePath, imageName, imageText);
		}

		transformImg->PrintMax();

	}
	delete transformImg;
	delete dsetMngr;

	for (;;)
	{
		if (waitKey(5) >= 0)
			break;
	}
	return 0;
}

void createTransformedImages(TransformImage *transformImg, string imgPath, string imgName, string imgText) {
	//�Է�: image path
	//string  imagepath = "testimage.jpg";
	string  imagepath = imgPath;

	cout << "imagePath : " << imagepath << endl;

	transformImg->CopyOriginFile(imagepath, imgName, imgText);

	int result;
	result = transformImg->Rotate(imagepath, imgName, imgText);
	result = transformImg->Brightness(imagepath, imgName, imgText);
	result = transformImg->Contrast(imagepath, imgName, imgText);
	result = transformImg->Perspective(imagepath, imgName, imgText);


	//imshow("out", outImage);

}
/* ImageTransformTestForText()
   DESC : word list text ������ �ѱ� �ܾ �о �̹��� ���� �� �����ؼ� �����ϰ� listsnew.txt�� �̹������ text�� ����Ѵ�.
 */

void ImageTransformTestForText(TransformImage *transformImg, DatasetManager *dsetMngr, int fontCnt)
{
	//input 1. ���� ���� path
	//		2. word list file path & name
	//		3. output file path & name
		//TransformImage *transformImg = new TransformImage();
		//DatasetManager *dsetMngr = new DatasetManager();
	if (transformImg == NULL || dsetMngr == NULL) {
		cout << "ERROR : transformImg == NULL || dsetMngr == NULL" << endl;
		return;
	}

	cout << "Enter ImageTransformTestForText()" << endl;

	transformImg->SetSaveFolder(str_SAVE_FOLDER_PATH);//HARD-CODING

	//int wordCount = dsetMngr->LoadWordList("data/hangeul_word_list6000.txt"); //HARD-CODING
	int wordCount = dsetMngr->LoadWordList(str_HANGUEL_WORD_LIST_PATH); //HARD-CODING
	//int fontCount = dsetMngr->ReadFileList("data/fonts/", "*.ttf");
	int fontCount = fontCnt;
	int bgCount = dsetMngr->mBGImageList.size();

	cout << "word count : " << wordCount << endl;
	cout << "font count : " << fontCount << endl;
	int indexToggle = 0;

	if (wordCount > 0 && fontCount > 0) {
#ifndef TEST_ROUTINE
		for (int ftcnt = 0; ftcnt < fontCount; ftcnt++) {
			//for (int ftcnt = 2; ftcnt < 3; ftcnt++) {
#else
		for (int ftcnt = 0; ftcnt < g_testFontCount; ftcnt++) { //for test
#endif
			cout << "font[" << ftcnt << "]" << endl;
#ifndef TEST_ROUTINE
			indexToggle ^= 1;
		//for (int wdcnt = 0; wdcnt < wordCount; wdcnt++) {
			//for (int wdcnt = 0; wdcnt < 10; wdcnt++) {
			for (int wdcnt = indexToggle; wdcnt < wordCount; wdcnt+=2) {

#else
			for (int wdcnt = 0; wdcnt < g_testWordCount; wdcnt++) {  //for test
#endif
				string wordString;
				wordString = CT2CA(dsetMngr->mWordList.at(wdcnt));
				//wordString = (dsetMngr->mWordList.at(i));

				//string cs = dsetMngr->mWordList.at(i);
				//CT2CA pszConvertedAnsiString(cs);
				//string wordString(pszConvertedAnsiString);

				char wordBuf[255] = { 0, };
				strcpy(wordBuf, wordString.c_str());
				//cout << "[" << wdcnt << "] " << wordBuf << endl;
				char *wordBufUTF8 = new char[255];
				wordBufUTF8 = dsetMngr->ANSIToUTF8(wordBuf);
				string myText(wordBufUTF8);

				//			if (fontCount > 0) {
				//#ifndef TEST_ROUTINE
				//				for (int ftcnt = 0; ftcnt < fontCount; ftcnt++) {
				//				//for (int ftcnt = 2; ftcnt < 3; ftcnt++) {
				//#else
				//				for (int ftcnt = 0; ftcnt < g_testFontCount; ftcnt++) { //for test
				//#endif
				string fontPath;
				fontPath = CT2CA(dsetMngr->mFontPathList.at(ftcnt));

				//MakeTextImage() PARAM : font path, word string, word string(utf8), vaca index 
				//transformImg->MakeTextImage(fontPath, wordString, myText, wdcnt, ftcnt);

				int fontWidth = dsetMngr->mFontWidthList.at(ftcnt);
				//transformImg->MakeTextImagePre(fontPath, wordString, myText, wdcnt, ftcnt, fontWidth);

				int bgIndexRandom = transformImg->GetRandomNumber(0, bgCount - 1);//noise bg image���� ������ �̹����� index�� �����´�.
				//transformImg->MakeBGTextImage(fontPath, wordString, myText, wdcnt, ftcnt, fontWidth, 
				//							dsetMngr->mBGImageList.at(bgIndexRandom));
				transformImg->MakeBGFGTransformTextImage(fontPath, wordString, myText, wdcnt, ftcnt, fontWidth,
					dsetMngr->mBGImageList.at(bgIndexRandom));


				//fontTestCode1(fontPath); //for debug
			
		}

	}
}

cout << "Exit ImageTransformTestForText()" << endl;

}

int FontWidthTest(TransformImage *transformImg, DatasetManager *dsetMngr)
{
	//input 1. ���� ���� path
	//		2. word list file path & name
	//		3. output file path & name

	if (transformImg == NULL || dsetMngr == NULL) {
		cout << "ERROR : transformImg == NULL || dsetMngr == NULL" << endl;
		return 0;
	}
	cout << "Enter FontWidthTest()" << endl;

	//transformImg->SetSaveFolder("data/transformed/");//HARD-CODING

	//int wordCount = dsetMngr->LoadWordList("data/hangeul_word_list6000.txt"); //HARD-CODING
	int wordCount = dsetMngr->LoadWordList(str_HANGUEL_WORD_LIST_PATH); //HARD-CODING
	int fontCount = dsetMngr->ReadFileList(strFONT_FILES_FOLDER, "*.ttf");
	cout << "word count : " << wordCount << endl;
	cout << "font count : " << fontCount << endl;

	if (wordCount > 0 && fontCount > 0) {
		char wordBuf[255] = { 0, };
		char *wordBufUTF8 = new char[255];
		const int MAX_WORD_LENGTH = 8;

		int fontWidthAvg[42][MAX_WORD_LENGTH] = { 0, };

		//ofstream logfile;
		//logfile.open("fontTestResult.txt");
		ofstream outFontWidthFile;
		ifstream inFontWidthFile;
		int isfontWidthFileExist = 0;
		inFontWidthFile.open(strFONT_WIDTH_FILE_PATH);
		if (inFontWidthFile.is_open()) {
			isfontWidthFileExist = 1;
		}
		else {
			outFontWidthFile.open(strFONT_WIDTH_FILE_PATH);
		}

		if (isfontWidthFileExist == 1) {
			string linebuffer;
			while (inFontWidthFile.peek() != EOF) {
				getline(inFontWidthFile, linebuffer); //sally : �� ������ ����� �� ����.

				int font_width = stoi(linebuffer);
				dsetMngr->mFontWidthList.push_back(font_width);
			}
		}

		for (int ftcnt = 0; ftcnt < fontCount; ftcnt++) {
			//for (int ftcnt = 0; ftcnt < 3; ftcnt++) {

			string fontPath;
			fontPath = CT2CA(dsetMngr->mFontPathList.at(ftcnt));

			cout << "ft" << ftcnt << " : " << fontPath << endl;
			//logfile << "ft" << ftcnt << " : " << fontPath << endl;
			transformImg->InitFreeType(fontPath, ftcnt); //freetype �ʱ�ȭ

			if (isfontWidthFileExist == 0) {
				int cnt[16] = { 0, };
				int sum[16] = { 0, };
				int avg[16] = { 0, };
				int minusCnt[16] = { 0, };
				for (int wdcnt = 0; wdcnt < wordCount; wdcnt++) {
					//for (int wdcnt = 0; wdcnt < 20/*wordCount*/; wdcnt++) {					
					string wordString;
					wordString = CT2CA(dsetMngr->mWordList.at(wdcnt));
					strcpy(wordBuf, wordString.c_str());
					wordBufUTF8 = dsetMngr->ANSIToUTF8(wordBuf);
					string myText(wordBufUTF8);

					int fontWidth = transformImg->GetFontWidth(fontPath, wordString, myText, wdcnt, ftcnt);
					int textLength = wordString.length() / 2;

					if (textLength > MAX_WORD_LENGTH) {
						cout << "word length > 8  -> " << textLength << endl;
					}
					else {
						if (fontWidth > 0) {
							cnt[textLength]++;
							sum[textLength] += fontWidth;
						}
						else {
							//cnt[textLength]++;
							//sum[textLength] += 0;
							minusCnt[textLength]++;
						}
					}
					//cout << textLength << "  " << fontWidth << endl;
				}
				//int avg = 0;
				int lengthOneWidth = 0;  //�� ���ڴ� ��� width
				bool flagTooManyMinusValues = false;
				int avgSum = 0;
				int validCount = 0;
				for (int lcnt = 1; lcnt < 6/*MAX_WORD_LENGTH*/; lcnt++) {
					if (cnt[lcnt] > 0) {  //fontWidth > 0 �� �ܾ� ��
						fontWidthAvg[ftcnt][lcnt] = sum[lcnt] / cnt[lcnt];
						avg[lcnt] = (sum[lcnt] / cnt[lcnt]) / lcnt;  //��� fontWidth�� ���ڼ��� ������

						avgSum = avgSum + avg[lcnt];
						validCount++;
						//logfile << lcnt << " avg: " << avg[lcnt] << " cnt: " << cnt[lcnt] << " minusCnt: " << minusCnt[lcnt] << endl;
						cout << lcnt << " avg: " << avg[lcnt] << " cnt: " << cnt[lcnt] << " minusCnt: " << minusCnt[lcnt] << endl;
					}
					else { //fontWidth <= 0 �� �ܾ��
						avg[lcnt] = sum[lcnt];
						//logfile << lcnt << " avg: " << sum[lcnt] << " cnt: " << cnt[lcnt] << " minusCnt: " << minusCnt[lcnt] << endl;
						cout << lcnt << " avg: " << sum[lcnt] << " cnt: " << cnt[lcnt] << " minusCnt: " << minusCnt[lcnt] << endl;
					}

					if (minusCnt[lcnt] > 300) {
						flagTooManyMinusValues = true;
					}
				}

				lengthOneWidth = dsetMngr->FONT_HEIGHT;

				if (flagTooManyMinusValues == true) {  //fontWidth �� ���̳ʽ��� �ܾ���� 900���� �Ѿ�� �ش� ��Ʈ�� �� ���ڴ� width�� �׳� FONT_HEIGHT�� ������.
					lengthOneWidth = dsetMngr->FONT_HEIGHT;
				}
				else {
					//lengthOneWidth = avgSum / 5;
					lengthOneWidth = avgSum / validCount;
				}

				cout << "lengthOneWidth : " << lengthOneWidth << endl;
				dsetMngr->mFontWidthList.push_back(lengthOneWidth);  //font width�� ���� ����.

				outFontWidthFile << lengthOneWidth << endl;
			}
		}

		//logfile.close(); //for debug
		if (outFontWidthFile.is_open()) {
			outFontWidthFile.close();
		}
		if (inFontWidthFile.is_open()) {
			inFontWidthFile.close();
		}
		delete wordBufUTF8;
	}

	cout << "Exit FontWidthTest()" << endl;
	return fontCount;
}

int fileCopy(string src, string dst) {
	//clock_t start, end;
	//start = clock();

	//ifstream source("from.ogv", ios::binary);
	//ofstream dest("to.ogv", ios::binary);
	ifstream source(src, ios::binary);
	ofstream dest(dst, ios::binary);

	istreambuf_iterator<char> begin_source(source);
	istreambuf_iterator<char> end_source;
	ostreambuf_iterator<char> begin_dest(dest);
	copy(begin_source, end_source, begin_dest);

	source.close();
	dest.close();

	//end = clock();

	//cout << "CLOCKS_PER_SEC " << CLOCKS_PER_SEC << "\n";
	//cout << "CPU-TIME START " << start << "\n";
	//cout << "CPU-TIME END " << end << "\n";
	//cout << "CPU-TIME END - START " << end - start << "\n";
	//cout << "TIME(SEC) " << static_cast<double>(end - start) / CLOCKS_PER_SEC << "\n";

	return 0;
}

//void getFontWidth() {
//	TransformImage *transformImg = new TransformImage();
//	DatasetManager *dsetMngr = new DatasetManager();
//	vector<string> fontWidthList;
//	int fontCount = dsetMngr->ReadFileList("data/fonts/", "*.ttf");
//	
//
//	delete transformImg;
//	delete dsetMngr;
//}


int main(void) {
	/* test code : random number test
	int count = 0;
	int MAXNUM = 100;
	for (int i = 0; i < MAXNUM; i++) {
		int number = getRandomNumber(0, 7);
		int number2 = getRandomNumber(0, 7);
		cout << "random number  " << number << ", " << number2 ;

		if (number == number2) {
			cout << " ***";
			count++;
		}
		cout << endl;
	}

	cout << "same number : " << count << "/" << MAXNUM << endl;
	return 0;*/

	//*	
	TransformImage *transformImg = new TransformImage();
	DatasetManager *dsetMngr = new DatasetManager();

	int fontCount = FontWidthTest(transformImg, dsetMngr);
	int bgCount = dsetMngr->LoadBGImage(strBG_IMAGE_FOLDER, "*.jpg");

	cout << "font count : " << fontCount << " , bg count : " << bgCount << endl;

	if (fontCount > 0 && bgCount > 0) {
		ImageTransformTestForText(transformImg, dsetMngr, fontCount);
	}

	delete transformImg;
	delete dsetMngr;
	//*/
}


