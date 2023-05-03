//#include <windows.h>

#include <afx.h>
#include <iostream>
#include <vector>
#include <string>

#include "DatasetManager.h"

using namespace std;

int _tmain___(int argc, TCHAR *argv[])
{
	DatasetManager *imgMngr = new DatasetManager();
	int imageCount = imgMngr->LoadImageList(_T(" "));
	
	return TRUE;
}


int testCode() {

	CString tpath = _T("D:/DatasetTools/datasetTool/datasetTool/*.*");

	CFileFind finder;
	BOOL bWorking = finder.FindFile(tpath); //

	CString fileName;
	CString DirName;

	vector<CString> jpgFileList;
	string strFileName;

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

			strFileName = CT2CA(fileName);
			jpgFileList.push_back(fileName);

			cout << "filename : " << strFileName << endl;

		}
	}

	return TRUE;
}


