package com.ispd.mommybook.ocr;

public enum OCRRecognitionMode {
    // Typography text recognition mode
    TYPO_KOR,
    TYPO_ENG,
    TYPO_KOR_NUM,
    TYPO_ENG_NUM,
    TYPO_NUM_SIGN,
    TYPO_FORMULA,

    // Handwriting text recognition mode
    HW_KOR_SINGLE, //단일채점
    HW_KOR_MULTI,//집단채점
    HW_ENG_SINGLE, //단일채점
    HW_ENG_MULTI, //집단채점
    HW_KOR_NUM, 
    HW_ENG_NUM,
    HW_NUM_SIGN,
    HW_FORMULA,
    NONE
}
