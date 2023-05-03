package com.ispd.mommybook.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.ispd.mommybook.utils.UtilsLogger;

import java.util.ArrayList;

public class PreviewBitmapStorage {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private ArrayList mBitmapList = null;

    public PreviewBitmapStorage(int index) {

        mBitmapList = new ArrayList();

        if( index == 0 ) {
            for (int i = 0; i < 7; i++) {
                addBitmap("/sdcard/studyNet/koreanAlpha/" + (i + 1) + ".png");
            }
        }
        else if( index == 1 ) {
            for (int i = 0; i < 7; i++) {
                addBitmap("/sdcard/studyNet/mathAlpha/" + (i + 1) + ".png");
            }
        }
        else if( index == 2 ) {
            for (int i = 0; i < 15; i++) {
                addBitmap("/sdcard/studyNet/englishAlpha/" + (i + 1) + ".png");
            }
        }
    }

    public ArrayList GetBitmapList() {
        if(mBitmapList != null) {
            return mBitmapList;
        }
        else {
            return null;
        }
    }

    public void Release() {
        for( int i = 0; i < mBitmapList.size(); i++ ) {
            ((Bitmap)mBitmapList.get(i)).recycle();
        }

        mBitmapList.clear();
        mBitmapList = null;
    }

    private void addBitmap(String path) {
        Bitmap image;

        image = BitmapFactory.decodeFile(path);

        LOGGER.d("addBitmap : "+image);
        if (image != null) {
            mBitmapList.add(image);
        }
    }
}
