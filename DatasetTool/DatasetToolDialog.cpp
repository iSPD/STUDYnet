
#include "stdafx.h"
#include "DatasetToolDialog.h"
#include "afxdialogex.h"


IMPLEMENT_DYNAMIC(DatasetToolDialog, CDialogEx)

DatasetToolDialog::DatasetToolDialog(CWnd* pParent /*=nullptr*/)
	: CDialogEx(IDD_DIALOG1, pParent)
{
#ifndef _WIN32_WCE
	EnableActiveAccessibility();
#endif

}

DatasetToolDialog::~DatasetToolDialog()
{
}

void DatasetToolDialog::DoDataExchange(CDataExchange* pDX)
{
	CDialogEx::DoDataExchange(pDX);
}


BEGIN_MESSAGE_MAP(DatasetToolDialog, CDialogEx)
END_MESSAGE_MAP()

