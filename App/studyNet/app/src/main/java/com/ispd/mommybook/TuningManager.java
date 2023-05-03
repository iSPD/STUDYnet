package com.ispd.mommybook;

import com.ispd.mommybook.utils.UtilsLogger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TuningManager {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    public static float mMovingValueForImage;
    public static float mMovingSensForImage;
    public static float mMovingValueForCrop;
    public static float mMovingSensForCrop;

    public static float mHoriStartValue;
    public static float mHoriEndValue;
    public static float mVerStartValue;
    public static float mVerEndValue;

    public static float mTuneSplit = 5.0f;
    public static float []mTuneValues = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    public static float mUpCut = 0.95f;
    public static int mBelowCut = 50;

    //new start2...
    public static float []mPDAlignmnetLeftException = {-30.f, 30.f, -30.f, 30.f};
    public static float []mPDAlignmnetRightException = {-30.f, 30.f, -30.f, 30.f};

    public static float []mPDAlignmnetLeftKeypointCalc = {1.1f, 0.9f, 1.1f, 0.9f};//x, y
    public static float []mPDAlignmnetRightKeypointCalc = {1.1f, 0.9f, 1.1f, 0.9f};//x, y

    public static float []mPDAlignmnetLeftDefault = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};//x, y
    public static float []mPDAlignmnetRightDefault = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};//x, y

    public static float []mPDAlignmnetLeftDetailTune = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};//x, y
    public static float []mPDAlignmnetRightDetailTune = {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};//x, y

    //new start...
    public static float []mCurveUpMoreLeft = {0.0f, 1.1f, 20.f, 1.45f};
    public static float []mCurveUpMoreRight = {0.0f, 1.4f, 20.f, 1.55f};

    public static float []mCurveValueTuneLeft = {10.0f, 1.0f, 20.f, 1.2f};
    public static float []mCurveValueTuneRight = {10.0f, 1.0f, 20.f, 1.2f};

    public static float []mCurveVerticalTuneLeft = {1.0f, 0.4f};
    public static float []mCurveVerticalTuneRight = {1.0f, 0.4f};

    public static float []mCurveVerticalStartLeft = {1.0f, 11.0f, 2.f, 11.0f};
    public static float []mCurveVerticalStartRight = {1.0f, 11.0f, 2.f, 11.0f};

    public static float []mCurveVerticalCurveLeft = {8.0f, 1.2f, 20.f, 1.0f, 1.3f};
    public static float []mCurveVerticalCurveRight = {8.0f, 1.2f, 20.f, 0.8f, 0.5f};

    public static float []mWarpRightBottomLeft = {3.0f, 3.0f};
    public static float []mWarpLeftBottomRight = {1.0f, 3.0f};

    public static float []mCurveStartByValueLeft = {0.0f, 1.0f, 20.f, 1.5f};
    public static float []mCurveStartByValueRight = {0.0f, 1.0f, 20.f, 1.5f};

    public static float mClassConfidenceThreshold = 0.8f;

    public static void LoadTuningFile()
    {
        StringBuffer strBuffer = new StringBuffer();
        try{
            String path = "/sdcard/studyNet/Tuning/studyTuneNew2.txt";
            InputStream is = new FileInputStream(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line="";

            //Moving
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning1 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning2 : "+line);

            mMovingValueForImage = Float.valueOf(line.split("\\s{1,}")[1]);
            LOGGER.d("mMovingValueForImage : "+mMovingValueForImage);
            mMovingSensForImage = Float.valueOf(line.split("\\s{1,}")[2]);
            LOGGER.d("mMovingSensForImage : "+mMovingSensForImage);

            line = reader.readLine();
            LOGGER.d("lineTuning3 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning4 : "+line);

            mMovingValueForCrop = Float.valueOf(line.split("\\s{1,}")[1]);
            LOGGER.d("mMovingValueForCrop : "+mMovingValueForCrop);
            mMovingSensForCrop = Float.valueOf(line.split("\\s{1,}")[2]);
            LOGGER.d("mMovingSensForCrop : "+mMovingSensForCrop);

            //세로 펴주기 시작
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning5 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning6 : "+line);
            for(int i = 1; i < 12; i++) {
                if( i == 1 )
                {
                    mTuneSplit = Float.valueOf(line.split("\\s{1,}")[i]);
                }
                else {
                    mTuneValues[i - 2] = Float.valueOf(line.split("\\s{1,}")[i]);
                    LOGGER.d((i) + "-mTuneValues : " + mTuneValues[i-2]);
                }
            }

            mUpCut = Float.valueOf(line.split("\\s{1,}")[12]);
            mBelowCut = Integer.valueOf(line.split("\\s{1,}")[13]);

            LOGGER.d("mUpCut : " + mUpCut);
            LOGGER.d("mBelowCut : " + mBelowCut);

            //가로길이
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning7 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning8 : "+line);

            mHoriStartValue = Float.valueOf(line.split("\\s{1,}")[1]);
            LOGGER.d("mHoriStartValue : "+mHoriStartValue);
            mHoriEndValue = Float.valueOf(line.split("\\s{1,}")[2]);
            LOGGER.d("mHoriEndValue : "+mHoriEndValue);

            //세로길이
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning9 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning10 : "+line);

            mVerStartValue = Float.valueOf(line.split("\\s{1,}")[1]);
            LOGGER.d("mVerStartValue : "+mVerStartValue);
            mVerEndValue = Float.valueOf(line.split("\\s{1,}")[2]);
            LOGGER.d("mVerEndValue : "+mVerEndValue);

            //New Start2...
            //A. pd-alignment exception
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuningA1 : "+line);
            line = line.trim();
            LOGGER.d("lineTuningA2 : "+line);

            mPDAlignmnetLeftException[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mPDAlignmnetLeftException[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mPDAlignmnetLeftException[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mPDAlignmnetLeftException[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            for(int i = 0; i < 8; i++) {
                mPDAlignmnetLeftDefault[i] = Float.valueOf(line.split("\\s{1,}")[5+i]);
            }

            line = reader.readLine();
            LOGGER.d("lineTuningA3 : "+line);
            line = line.trim();
            LOGGER.d("lineTuningA4 : "+line);

            mPDAlignmnetRightException[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mPDAlignmnetRightException[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mPDAlignmnetRightException[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mPDAlignmnetRightException[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            for(int i = 0; i < 8; i++) {
                mPDAlignmnetRightDefault[i] = Float.valueOf(line.split("\\s{1,}")[5+i]);
            }

            for(int i = 0; i < 4; i++)
            {
                LOGGER.d("mPDAlignmnetLeftException["+i+"] : "+mPDAlignmnetLeftException[i]);
                LOGGER.d("mPDAlignmnetRightException["+i+"] : "+mPDAlignmnetRightException[i]);
            }

            for(int i = 0; i < 8; i++)
            {
                LOGGER.d("mPDAlignmnetLeftDefault["+i+"] : "+mPDAlignmnetLeftDefault[i]);
                LOGGER.d("mPDAlignmnetRightDefault["+i+"] : "+mPDAlignmnetRightDefault[i]);
            }

            //B. pd-alignment keypoint Tune
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuningB1 : "+line);
            line = line.trim();
            LOGGER.d("lineTuningB2 : "+line);

            mPDAlignmnetLeftKeypointCalc[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mPDAlignmnetLeftKeypointCalc[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mPDAlignmnetLeftKeypointCalc[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mPDAlignmnetLeftKeypointCalc[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            line = reader.readLine();
            LOGGER.d("lineTuningB3 : "+line);
            line = line.trim();
            LOGGER.d("lineTuningB4 : "+line);

            mPDAlignmnetRightKeypointCalc[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mPDAlignmnetRightKeypointCalc[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mPDAlignmnetRightKeypointCalc[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mPDAlignmnetRightKeypointCalc[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            for(int i = 0; i < 4; i++)
            {
                LOGGER.d("mPDAlignmnetLeftKeypointCalc["+i+"] : "+mPDAlignmnetLeftKeypointCalc[i]);
                LOGGER.d("mPDAlignmnetRightKeypointCalc["+i+"] : "+mPDAlignmnetRightKeypointCalc[i]);
            }

            //C. pd-alignment detail Tune
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuningC1 : "+line);
            line = line.trim();
            LOGGER.d("lineTuningC2 : "+line);

            mPDAlignmnetLeftDetailTune[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mPDAlignmnetLeftDetailTune[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mPDAlignmnetLeftDetailTune[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mPDAlignmnetLeftDetailTune[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            mPDAlignmnetLeftDetailTune[4] = Float.valueOf(line.split("\\s{1,}")[5]);
            mPDAlignmnetLeftDetailTune[5] = Float.valueOf(line.split("\\s{1,}")[6]);
            mPDAlignmnetLeftDetailTune[6] = Float.valueOf(line.split("\\s{1,}")[7]);
            mPDAlignmnetLeftDetailTune[7] = Float.valueOf(line.split("\\s{1,}")[8]);

            line = reader.readLine();
            LOGGER.d("lineTuningC3 : "+line);
            line = line.trim();
            LOGGER.d("lineTuningC4 : "+line);

            mPDAlignmnetRightDetailTune[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mPDAlignmnetRightDetailTune[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mPDAlignmnetRightDetailTune[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mPDAlignmnetRightDetailTune[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            mPDAlignmnetRightDetailTune[4] = Float.valueOf(line.split("\\s{1,}")[5]);
            mPDAlignmnetRightDetailTune[5] = Float.valueOf(line.split("\\s{1,}")[6]);
            mPDAlignmnetRightDetailTune[6] = Float.valueOf(line.split("\\s{1,}")[7]);
            mPDAlignmnetRightDetailTune[7] = Float.valueOf(line.split("\\s{1,}")[8]);

            for(int i = 0; i < 8; i++)
            {
                LOGGER.d("mPDAlignmnetLeftDetailTune["+i+"] : "+mPDAlignmnetLeftDetailTune[i]);
                LOGGER.d("mPDAlignmnetRightDetailTune["+i+"] : "+mPDAlignmnetRightDetailTune[i]);
            }

            //New Start...
            //1. Curve
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning11 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning12 : "+line);

            mCurveUpMoreLeft[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveUpMoreLeft[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveUpMoreLeft[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveUpMoreLeft[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            line = reader.readLine();
            LOGGER.d("lineTuning13 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning14 : "+line);

            mCurveUpMoreRight[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveUpMoreRight[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveUpMoreRight[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveUpMoreRight[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            for(int i = 0; i < 4; i++)
            {
                LOGGER.d("mCurveUpMoreLeft["+i+"] : "+mCurveUpMoreLeft[i]);
                LOGGER.d("mCurveUpMoreRight["+i+"] : "+mCurveUpMoreRight[i]);
            }

            //2. Curve
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning15 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning16 : "+line);

            mCurveValueTuneLeft[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveValueTuneLeft[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveValueTuneLeft[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveValueTuneLeft[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            line = reader.readLine();
            LOGGER.d("lineTuning17 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning18 : "+line);

            mCurveValueTuneRight[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveValueTuneRight[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveValueTuneRight[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveValueTuneRight[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            for(int i = 0; i < 4; i++)
            {
                LOGGER.d("mCurveValueTuneLeft["+i+"] : "+mCurveValueTuneLeft[i]);
                LOGGER.d("mCurveValueTuneRight["+i+"] : "+mCurveValueTuneRight[i]);
            }

            //3. Curve
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning19 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning20 : "+line);

            mCurveVerticalTuneLeft[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveVerticalTuneLeft[1] = Float.valueOf(line.split("\\s{1,}")[2]);

            line = reader.readLine();
            LOGGER.d("lineTuning21 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning22 : "+line);

            mCurveVerticalTuneRight[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveVerticalTuneRight[1] = Float.valueOf(line.split("\\s{1,}")[2]);

            for(int i = 0; i < 2; i++)
            {
                LOGGER.d("mCurveVerticalTuneLeft["+i+"] : "+mCurveVerticalTuneLeft[i]);
                LOGGER.d("mCurveVerticalTuneRight["+i+"] : "+mCurveVerticalTuneRight[i]);
            }

            //4. Curve
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning23 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning24 : "+line);

            mCurveVerticalStartLeft[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveVerticalStartLeft[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveVerticalStartLeft[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveVerticalStartLeft[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            line = reader.readLine();
            LOGGER.d("lineTuning25 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning26 : "+line);

            mCurveVerticalStartRight[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveVerticalStartRight[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveVerticalStartRight[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveVerticalStartRight[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            for(int i = 0; i < 4; i++)
            {
                LOGGER.d("mCurveVerticalStartLeft["+i+"] : "+mCurveVerticalStartLeft[i]);
                LOGGER.d("mCurveVerticalStartRight["+i+"] : "+mCurveVerticalStartRight[i]);
            }

            //5. Curve
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning27 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning28 : "+line);

            mCurveVerticalCurveLeft[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveVerticalCurveLeft[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveVerticalCurveLeft[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveVerticalCurveLeft[3] = Float.valueOf(line.split("\\s{1,}")[4]);
            mCurveVerticalCurveLeft[4] = Float.valueOf(line.split("\\s{1,}")[5]);

            line = reader.readLine();
            LOGGER.d("lineTuning29 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning30 : "+line);

            mCurveVerticalCurveRight[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveVerticalCurveRight[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveVerticalCurveRight[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveVerticalCurveRight[3] = Float.valueOf(line.split("\\s{1,}")[4]);
            mCurveVerticalCurveRight[4] = Float.valueOf(line.split("\\s{1,}")[5]);

            for(int i = 0; i < 5; i++)
            {
                LOGGER.d("mCurveVerticalCurveLeft["+i+"] : "+mCurveVerticalCurveLeft[i]);
                LOGGER.d("mCurveVerticalCurveRight["+i+"] : "+mCurveVerticalCurveRight[i]);
            }

            //6. Curve
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning31 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning32 : "+line);

            mWarpRightBottomLeft[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mWarpRightBottomLeft[1] = Float.valueOf(line.split("\\s{1,}")[2]);

            line = reader.readLine();
            LOGGER.d("lineTuning33 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning34 : "+line);

            mWarpLeftBottomRight[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mWarpLeftBottomRight[1] = Float.valueOf(line.split("\\s{1,}")[2]);

            for(int i = 0; i < 2; i++)
            {
                LOGGER.d("mWarpRightBottomLeft["+i+"] : "+mWarpRightBottomLeft[i]);
                LOGGER.d("mWarpLeftBottomRight["+i+"] : "+mWarpLeftBottomRight[i]);
            }

            //7. Curve
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning35 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning36 : "+line);

            mCurveStartByValueLeft[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveStartByValueLeft[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveStartByValueLeft[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveStartByValueLeft[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            line = reader.readLine();
            LOGGER.d("lineTuning37 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning38 : "+line);

            mCurveStartByValueRight[0] = Float.valueOf(line.split("\\s{1,}")[1]);
            mCurveStartByValueRight[1] = Float.valueOf(line.split("\\s{1,}")[2]);
            mCurveStartByValueRight[2] = Float.valueOf(line.split("\\s{1,}")[3]);
            mCurveStartByValueRight[3] = Float.valueOf(line.split("\\s{1,}")[4]);

            for(int i = 0; i < 4; i++)
            {
                LOGGER.d("mCurveStartByValueLeft["+i+"] : "+mCurveStartByValueLeft[i]);
                LOGGER.d("mCurveStartByValueRight["+i+"] : "+mCurveStartByValueRight[i]);
            }

            //NA. ConfidenceThreshold
            reader.readLine();
            reader.readLine();

            line = reader.readLine();
            LOGGER.d("lineTuning39 : "+line);
            line = line.trim();
            LOGGER.d("lineTuning40 : "+line);

            mClassConfidenceThreshold = Float.valueOf(line.split("\\s{1,}")[1]);
            LOGGER.d("mClassConfidenceThreshold : "+mClassConfidenceThreshold);

            reader.close();
            is.close();
        }catch (IOException e){
            LOGGER.d("what the1");
            e.printStackTrace();
        }

//        LOGGER.d("what the2");
//        MainActivity.setTuneValues(mTuneSplit, mTuneValues[0], mTuneValues[1], mTuneValues[2], mTuneValues[3], mTuneValues[4], mTuneValues[5], mTuneValues[6], mTuneValues[7], mTuneValues[8], mTuneValues[9], mUpCut, mBelowCut);
//        MainActivity.setTuneValues2(mHoriStartValue, mHoriEndValue, mVerStartValue, mVerEndValue);

        JniController.setTuneValues1ForCurve(mCurveUpMoreLeft, mCurveUpMoreRight, mCurveValueTuneLeft, mCurveValueTuneRight,
                mCurveVerticalTuneLeft, mCurveVerticalTuneRight, mCurveVerticalStartLeft, mCurveVerticalStartRight, mCurveVerticalCurveLeft, mCurveVerticalCurveRight);
        JniController.setTuneValues2ForCurve(mWarpRightBottomLeft, mWarpLeftBottomRight, mCurveStartByValueLeft, mCurveStartByValueRight);
//
//        MainActivity.setTuneValues5ForPDAlign(mPDAlignmnetLeftException, mPDAlignmnetRightException, mPDAlignmnetLeftKeypointCalc, mPDAlignmnetRightKeypointCalc, mPDAlignmnetLeftDetailTune, mPDAlignmnetRightDetailTune,
//                                              mPDAlignmnetLeftDefault, mPDAlignmnetRightDefault);
    }
}
