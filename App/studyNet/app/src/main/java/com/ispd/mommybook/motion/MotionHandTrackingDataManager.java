package com.ispd.mommybook.motion;

import android.graphics.Color;
import android.util.Log;

import com.google.mediapipe.formats.proto.DetectionProto;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.formats.proto.LocationDataProto;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketCallback;
import com.google.mediapipe.framework.PacketGetter;
import com.ispd.mommybook.JniController;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;

import java.util.Date;
import java.util.List;

import static org.opencv.core.CvType.*;

public class MotionHandTrackingDataManager {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;
    private int mLCDWidth;
    private int mLCDHeight;
    private int mAdjustWidth;
    private int mAdjustHeight;

    //finger
    private int mHandCount = 0;
    private float mUseHandCount = 0;

    private boolean mFingerDetected = false;
    private boolean mFingerDetected2 = false;

    private float mFingerX[] = new float[21];
    private float mFingerY[] = new float[21];
    private float mFingerZ[] = new float[21];

    private float mFingerX2[] = new float[21];
    private float mFingerY2[] = new float[21];
    private float mFingerZ2[] = new float[21];

    private float mUseFingerX[] = new float[21];
    private float mUseFingerY[] = new float[21];
    private float mUseFingerZ[] = new float[21];

    private float mUseFingerX2[] = new float[21];
    private float mUseFingerY2[] = new float[21];
    private float mUseFingerZ2[] = new float[21];

    private float mSendFingerX[] = new float[21];
    private float mSendFingerY[] = new float[21];
    private float mSendFingerZ[] = new float[21];

    private float mSendFingerX2[] = new float[21];
    private float mSendFingerY2[] = new float[21];
    private float mSendFingerZ2[] = new float[21];

    private int mCheckFingerCount = 5;//10

    private float mSaveFingerX[];
    private float mSaveFingerY[];
    private float mSaveFingerX2[];
    private float mSaveFingerY2[];

    private boolean mCheckPointFinger1 = false;
    private boolean mCheckPointFinger2 = false;

    private boolean mTouchAreaSelected = false;
    private boolean mTouchAreaSelected2 = false;

    private FingerInfoListener mFingerInfoListener = null;
    private float mSumFinger[] = new float[21*3+1+1];//fingerCheck, handCount
    private float mSumFinger2[] = new float[21*3+1+1];//fingerCheck, handCount

    private double mStartCheckTime = System.currentTimeMillis();
    private int mCheckCount = 0;
    private boolean mTouchPressed = false;

    private double mStartCheckTime2 = System.currentTimeMillis();
    private int mCheckCount2 = 0;
    private boolean mTouchPressed2 = false;

    private double mSetHandDetectTime = 0.0;

    private float mLandmarkPoint[][];
    private float mLandmarkPoint2[][];

    public MotionHandTrackingDataManager(int width, int height, int LCDWidth, int LCDHeight) {
        mCameraPreviewWidth = width;
        mCameraPreviewHeight = height;
        mLCDWidth = LCDWidth;//2000
        mLCDHeight = LCDHeight;//1200

        mAdjustWidth = mLCDHeight * mCameraPreviewWidth / mCameraPreviewHeight;
        mAdjustHeight = mLCDHeight;
    }

    //will reduce Process once...
    private void changeFingerLocation(float []pointsX, float []pointsY, int pointsSize) {
        double startTime = System.currentTimeMillis();
        float transValues[] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};

        JniController.getCropMatrixValue(transValues, mCameraPreviewWidth, mCameraPreviewHeight);

        double endTime = System.currentTimeMillis();
        LOGGER.d("time : "+(endTime-startTime));

        LOGGER.d("transValues : "+transValues[0]+", transValues : "+transValues[1]+", transValues : "+transValues[2]);

        if(transValues[0] == 1.0f && transValues[1] == 0.0f && transValues[2] == 0.0f)
        {
            LOGGER.d("return1");
            return;
        }

        Mat inpuMat = new Mat(3,3, CV_32F);
        inpuMat.put(0, 0, transValues);

        Mat invMat = inpuMat.inv();

        invMat.get(0, 0, transValues);

        float frameCoordinate[] = new float[3];
        for(int i = 0; i < pointsSize; i++)
        {
            frameCoordinate[0] = pointsX[i];
            frameCoordinate[1] = pointsY[i];
            frameCoordinate[2] = 1.0f;

            float transX = transValues[0] * frameCoordinate[0] + transValues[1] * frameCoordinate[1] + transValues[2] * frameCoordinate[2];
            float transY = transValues[3] * frameCoordinate[0] + transValues[4] * frameCoordinate[1] + transValues[5] * frameCoordinate[2];
            float transZ = transValues[6] * frameCoordinate[0] + transValues[7] * frameCoordinate[1] + transValues[8] * frameCoordinate[2];

            pointsX[i] = transX / transZ;
            pointsY[i] = transY / transZ;
        }
    }

    //will reduce Process once...
    private void changeFingerLocation2(float []pointsX, float []pointsY, int pointsSize) {

        float leftValues[] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
        float rightValues[] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
        float checkValues[] = new float[2];

        //MainActivity.getAlignmentValues3ForFinger(leftValues, rightValues, checkValues);
        JniController.getAlignmentMatrixValue(leftValues, rightValues, mCameraPreviewWidth, mCameraPreviewHeight);

        if(leftValues[0] == 1.0f && leftValues[1] == 0.0f && leftValues[2] == 0.0f)
        {
            LOGGER.d("return2");
            return;
        }

        Mat inpuMatLeft = new Mat(3, 3, CV_32F);
        inpuMatLeft.put(0, 0, leftValues);
        Mat invMatLeft = inpuMatLeft.inv();
        invMatLeft.get(0, 0, leftValues);

        Mat inpuMatRight = new Mat(3, 3, CV_32F);
        inpuMatRight.put(0, 0, rightValues);
        Mat invMatRight = inpuMatRight.inv();
        invMatRight.get(0, 0, rightValues);

        float frameCoordinate2[] = new float[3];
        for (int i = 0; i < pointsSize; i++) {

            float transX;
            float transY;
            float transZ;

            if( pointsX[i] < (float)mCameraPreviewWidth / 2.f ) {
                frameCoordinate2[0] = pointsX[i];
                frameCoordinate2[1] = pointsY[i];
                frameCoordinate2[2] = 1.0f;

                transX = leftValues[0] * frameCoordinate2[0] + leftValues[1] * frameCoordinate2[1] + leftValues[2] * frameCoordinate2[2];
                transY = leftValues[3] * frameCoordinate2[0] + leftValues[4] * frameCoordinate2[1] + leftValues[5] * frameCoordinate2[2];
                transZ = leftValues[6] * frameCoordinate2[0] + leftValues[7] * frameCoordinate2[1] + leftValues[8] * frameCoordinate2[2];

                pointsX[i] = transX / transZ;
                pointsY[i] = transY / transZ;
            }
            else
            {
                float minus = (float)mCameraPreviewWidth / 2.f;

                frameCoordinate2[0] = pointsX[i] - minus;
                frameCoordinate2[1] = pointsY[i];
                frameCoordinate2[2] = 1.0f;

                transX = rightValues[0] * frameCoordinate2[0] + rightValues[1] * frameCoordinate2[1] + rightValues[2] * frameCoordinate2[2];
                transY = rightValues[3] * frameCoordinate2[0] + rightValues[4] * frameCoordinate2[1] + rightValues[5] * frameCoordinate2[2];
                transZ = rightValues[6] * frameCoordinate2[0] + rightValues[7] * frameCoordinate2[1] + rightValues[8] * frameCoordinate2[2];

                pointsX[i] = transX / transZ;
                pointsY[i] = transY / transZ;

                pointsX[i] = pointsX[i] + minus;
            }
        }
    }

    private void setFingerInfo(float landmarkPoint[][]) {
        mFingerDetected = false;

        LOGGER.d("mFingerY : "+mFingerY[0]);

        for(int i = 0; i < 21; i++)
        {
            mFingerX[i] = (landmarkPoint[i][0]) * (float)mCameraPreviewWidth;
            mFingerY[i] = landmarkPoint[i][1] * (float)mCameraPreviewHeight;
            mFingerZ[i] = landmarkPoint[i][2];
        }

        LOGGER.d("mFingerY : "+mFingerY[0]);

        changeFingerLocation(mFingerX, mFingerY, 21);

        for(int i = 0; i < 21; i++)
        {
            mFingerX[i] = (float)mCameraPreviewWidth - mFingerX[i];
            mFingerY[i] = mFingerY[i];
            mFingerZ[i] = mFingerZ[i];
        }

        changeFingerLocation2(mFingerX, mFingerY, 21);

        LOGGER.d("mFingerY : "+mFingerY[0]);

        for(int i = 0; i < 21; i++)
        {
            mFingerX[i] = mFingerX[i] * (float)mAdjustWidth / (float)mCameraPreviewWidth;
            mFingerY[i] = mFingerY[i] * (float)mAdjustHeight / (float)mCameraPreviewHeight;
            mFingerZ[i] = mFingerZ[i];
        }

        LOGGER.d("mFingerY : "+mFingerY[0]);

        mFingerDetected = true;
    }

    private void setFingerInfo2(float landmarkPoint[][])
    {
        mFingerDetected2 = false;

        for(int i = 0; i < 21; i++)
        {
            mFingerX2[i] = (landmarkPoint[i][0]) * (float)mCameraPreviewWidth;
            mFingerY2[i] = landmarkPoint[i][1] * (float)mCameraPreviewHeight;
            mFingerZ2[i] = landmarkPoint[i][2];
        }

        changeFingerLocation(mFingerX2, mFingerY2, 21);

        for(int i = 0; i < 21; i++)
        {
            mFingerX2[i] = (float)mCameraPreviewWidth - mFingerX2[i];
            mFingerY2[i] = mFingerY2[i];
            mFingerZ2[i] = mFingerZ2[i];
        }

        changeFingerLocation2(mFingerX2, mFingerY2, 21);

        for(int i = 0; i < 21; i++)
        {
            mFingerX2[i] = mFingerX2[i] * (float)mAdjustWidth / (float)mCameraPreviewWidth;
            mFingerY2[i] = mFingerY2[i] * (float)mAdjustHeight / (float)mCameraPreviewHeight;
            mFingerZ2[i] = mFingerZ2[i];
        }

        mFingerDetected2 = true;
    }

    //for view
    public void GetFingerLocation(float inputX[], float inputY[], float inputZ[])
    {
        if( mFingerDetected == true ) {
            for (int i = 0; i < 21; i++) {
                inputX[i] = mFingerX[i];
                inputY[i] = mFingerY[i];
                inputZ[i] = mFingerZ[i];
            }
        }
    }

    //for view
    public void GetFingerLocation2(float inputX[], float inputY[], float inputZ[]) {
        if( mFingerDetected2 == true ) {
            for (int i = 0; i < 21; i++) {
                inputX[i] = mFingerX2[i];
                inputY[i] = mFingerY2[i];
                inputZ[i] = mFingerZ2[i];
            }
        }
    }

    private double mean(float[] array) {// 산술 평균 구하기
        double sum = 0.0;

        for (int i = 0; i < array.length; i++)
            sum += array[i];

        return sum / array.length;
    }


    private double standardDeviation(float[] array, int option) {
        if (array.length < 2) return Double.NaN;

        double sum = 0.0;
        double sd = 0.0;
        double diff;
        double meanValue = mean(array);

        for (int i = 0; i < array.length; i++) {
            diff = array[i] - meanValue;
            sum += diff * diff;
        }
        sd = Math.sqrt(sum / (array.length - option));

        return sd;
    }

    private boolean checkPointFinger(float []saveFingerX, float[]saveFingerY) {
        LOGGER.d("Compare1 : "+saveFingerY[8]+", "+saveFingerY[5]);
        LOGGER.d("Compare2 : "+saveFingerY[12]+", "+saveFingerY[11]);
        LOGGER.d("Compare3 : "+saveFingerY[16]+", "+saveFingerY[15]);
        LOGGER.d("Compare4 : "+saveFingerY[20]+", "+saveFingerY[19]);

        if(saveFingerY[8] < saveFingerY[7]
                && (saveFingerY[11] - saveFingerY[8]) > 20.f//detail up...
                //&& saveFingerY[12] >= saveFingerY[11]
                && saveFingerY[16] >= saveFingerY[15]
                && saveFingerY[20] >= saveFingerY[19])
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean CheckTouchPressed() {

        GetFingerLocation(mUseFingerX, mUseFingerY, mUseFingerZ);

        mCheckCount++;
        if( mCheckCount == mCheckFingerCount )
        {
            double time = System.currentTimeMillis();
            LOGGER.d("checkTouchPressedTime : " +(time - mStartCheckTime));

            mCheckCount = 0;
            mStartCheckTime = System.currentTimeMillis();
        }


        if(mSaveFingerX == null)
        {
            mSaveFingerX = new float[mCheckFingerCount];
            mSaveFingerY = new float[mCheckFingerCount];

            for(int i = 0; i < mCheckFingerCount; i++)
            {
                mSaveFingerX[i] = 0.0f;
                mSaveFingerY[i] = 0.0f;
            }
        }

        for(int i = 0; i < mCheckFingerCount-1; i++)
        {
            mSaveFingerX[i] = mSaveFingerX[i+1];
            mSaveFingerY[i] = mSaveFingerY[i+1];
        }
        mSaveFingerX[mCheckFingerCount-1] = mUseFingerX[8];
        mSaveFingerY[mCheckFingerCount-1] = mUseFingerY[8];

        //표준 편차 구하기...
        double stdX = standardDeviation(mSaveFingerX, 0);
        double stdY = standardDeviation(mSaveFingerY, 0);

        LOGGER.d("stdX : "+stdX+", stdY : "+stdY);

        boolean checkPoint = checkPointFinger(mUseFingerX, mUseFingerY);
        mCheckPointFinger1 = checkPoint;
        LOGGER.d("checkPoint : "+checkPoint);

        if(stdX < 10.0f && stdY < 10.0f)
        {
            if( checkPoint == true )
            {
                mTouchPressed = true;
            }
            else
            {
                mTouchPressed = false;
            }

            LOGGER.d("checkPoint : "+checkPoint+", mTouchPressed1 : "+mTouchPressed);
        }
        else if( stdX > 20.0f && stdY > 20.0f )
        {
            LOGGER.d("mTouchPressed2 : "+mTouchPressed);
            mTouchPressed = false;
        }
        else
        {
            LOGGER.d("mTouchPressed3 : "+mTouchPressed);
        }
        return mTouchPressed;
    }

    public boolean CheckTouchPressed2() {

        GetFingerLocation2(mUseFingerX2, mUseFingerY2, mUseFingerZ2);

        mCheckCount2++;
        if( mCheckCount2 == mCheckFingerCount )
        {
            double time = System.currentTimeMillis();
            LOGGER.d("checkTouchPressedTime : " +(time - mStartCheckTime2));

            mCheckCount2 = 0;
            mStartCheckTime2 = System.currentTimeMillis();
        }


        if(mSaveFingerX2 == null)
        {
            mSaveFingerX2 = new float[mCheckFingerCount];
            mSaveFingerY2 = new float[mCheckFingerCount];

            for(int i = 0; i < mCheckFingerCount; i++)
            {
                mSaveFingerX2[i] = 0.0f;
                mSaveFingerY2[i] = 0.0f;
            }
        }

        for(int i = 0; i < mCheckFingerCount-1; i++)
        {
            mSaveFingerX2[i] = mSaveFingerX2[i+1];
            mSaveFingerY2[i] = mSaveFingerY2[i+1];
        }
        mSaveFingerX2[mCheckFingerCount-1] = mUseFingerX2[8];
        mSaveFingerY2[mCheckFingerCount-1] = mUseFingerY2[8];

        //표준 편차 구하기...
        double stdX = standardDeviation(mSaveFingerX2, 0);
        double stdY = standardDeviation(mSaveFingerY2, 0);

        LOGGER.d("stdX2 : "+stdX+", stdY2 : "+stdY);

        boolean checkPoint = checkPointFinger(mUseFingerX2, mUseFingerY2);
        mCheckPointFinger2 = checkPoint;
        LOGGER.d("checkPoint : "+checkPoint);

        if(stdX < 10.0f && stdY < 10.0f)
        {
            if( checkPoint == true )
            {
                mTouchPressed2 = true;
            }
            else
            {
                mTouchPressed2 = false;
            }

            LOGGER.d("checkPoint : "+checkPoint+", mTouchPressed1 : "+mTouchPressed2);
        }
        else if( stdX > 20.0f && stdY > 20.0f )
        {
            LOGGER.d("mTouchPressed2 : "+mTouchPressed2);
            mTouchPressed2 = false;
        }
        else
        {
            LOGGER.d("mTouchPressed3 : "+mTouchPressed2);
        }
        return mTouchPressed2;
    }

    //mUseFingerX[8]
    public boolean CheckAreaTouched(float areaX1, float areaY1, float areaX2, float areaY2) {
        //draw touch area...

        boolean touchPressed = CheckTouchPressed();
        if(touchPressed == true)
        {
            int handCount = GetHandCount();

            if( handCount > 0 ) {
                LOGGER.d("touchPressed");

//                float adjustY2 = 10.f * 1060.f / 240.f;
//
//                float touchAreaX1 = 202.f / 1760.f * 0.9f;
//                float touchAreaX2 = 316.f / 1760.f * 1.1f;
//                float touchAreaY1 = (718.f-adjustY2) / 1060.f * 0.9f;
//                float touchAreaY2 = (790.f-adjustY2) / 1060.f * 1.1f;

                if( areaX1 < mUseFingerX[8] / (float)mAdjustWidth && mUseFingerX[8] / (float)mAdjustWidth < areaX2
                        && areaY1 < mUseFingerY[8] / (float)mAdjustHeight && mUseFingerY[8] / (float)mAdjustHeight < areaY2)
                {
                    mTouchAreaSelected = true;
                }
                else
                {
                    mTouchAreaSelected = false;
                }
            }
            else
            {
                mTouchAreaSelected = false;
            }
        }
        else
        {
            mTouchAreaSelected = false;
        }

        return mTouchAreaSelected;
    }

    public boolean CheckAreaTouched2(float areaX1, float areaY1, float areaX2, float areaY2) {
        //draw touch area...

        boolean touchPressed = CheckTouchPressed2();
        if(touchPressed == true)
        {
            int handCount = GetHandCount();

            if( handCount > 0 ) {
                LOGGER.d("touchPressed2");

//                float adjustY2 = 10.f * 1060.f / 240.f;
//
//                float touchAreaX1 = 202.f / 1760.f * 0.9f;
//                float touchAreaX2 = 316.f / 1760.f * 1.1f;
//                float touchAreaY1 = (718.f-adjustY2) / 1060.f * 0.9f;
//                float touchAreaY2 = (790.f-adjustY2) / 1060.f * 1.1f;

                if( areaX1 < mUseFingerX2[8] / (float)mAdjustWidth && mUseFingerX2[8] / (float)mAdjustWidth < areaX2
                        && areaY1 < mUseFingerY2[8] / (float)mAdjustHeight && mUseFingerY2[8] / (float)mAdjustHeight < areaY2)
                {
                    mTouchAreaSelected2 = true;
                }
                else
                {
                    mTouchAreaSelected2 = false;
                }
            }
            else
            {
                mTouchAreaSelected2 = false;
            }
        }
        else
        {
            mTouchAreaSelected2 = false;
        }

        return mTouchAreaSelected2;
    }

    public PacketCallback GetHandLandMarkCallback() {
        return mHandLandMarkPacketCallback;
    }

    public PacketCallback GetHandCallback() {
        return mHandPacketCallback;
    }

    private PacketCallback mHandLandMarkPacketCallback = new PacketCallback() {
        @Override
        public void process(Packet packet) {
            LOGGER.v("Received multi-hand landmarks packet.");
            List<LandmarkProto.NormalizedLandmarkList> multiHandLandmarks =
                    PacketGetter.getProtoVector(packet, LandmarkProto.NormalizedLandmarkList.parser());

            setHandCount(multiHandLandmarks.size());

            LOGGER.d("handDetection0 : "+multiHandLandmarks.size());

//            Log.v(
//                    TAG,
//                    "[TS:"
//                            + packet.getTimestamp()
//                            + "] "
//                            + handTracker.this.getMultiHandLandmarksDebugString(multiHandLandmarks));

            getMultiHandLandmarksDebugString(multiHandLandmarks);
        }
    };

    private PacketCallback mHandPacketCallback = new PacketCallback() {
        @Override
        public void process(Packet packet) {
            LOGGER.d("Received multi-hand landmarks packet2.");
            List<DetectionProto.Detection> handDetection =
                    PacketGetter.getProtoVector(packet, DetectionProto.Detection.parser());

//            LocationDataProto.LocationData lD = handDetection.get(0).getLocationData();
//            LocationDataProto.LocationData.BoundingBox bB = lD.getBoundingBox();

            LOGGER.d("handDetection1 : "+handDetection.get(0).getScoreCount());
            LOGGER.d("handDetection2 : "+handDetection.get(0).getScore(0));
        }
    };

    private String getMultiHandLandmarksDebugString(List<LandmarkProto.NormalizedLandmarkList> multiHandLandmarks) {

        if (multiHandLandmarks.isEmpty()) {
            return "No hand landmarks";
        }
        String multiHandLandmarksStr = "Number of hands detected: " + multiHandLandmarks.size() + "\n";
        int handIndex = 0;
        for (LandmarkProto.NormalizedLandmarkList landmarks : multiHandLandmarks) {
            multiHandLandmarksStr +=
                    "\t#Hand landmarks for hand[" + handIndex + "]: " + landmarks.getLandmarkCount() + "\n";
            int landmarkIndex = 0;

            if(mLandmarkPoint == null)
            {
                LOGGER.d("landmarks.getLandmarkList().size() : "+landmarks.getLandmarkList().size());
                mLandmarkPoint = new float[landmarks.getLandmarkList().size()][3];
                mLandmarkPoint2 = new float[landmarks.getLandmarkList().size()][3];
            }

            for (LandmarkProto.NormalizedLandmark landmark : landmarks.getLandmarkList()) {
                multiHandLandmarksStr +=
                        "\t\tLandmark ["
                                + landmarkIndex
                                + "]: ("
                                + landmark.getX()
                                + ", "
                                + landmark.getY()
                                + ", "
                                + landmark.getZ()
                                + ")\n";

                if( handIndex == 0 ) {
                    mLandmarkPoint[landmarkIndex][0] = landmark.getX();
                    mLandmarkPoint[landmarkIndex][1] = landmark.getY();
                    mLandmarkPoint[landmarkIndex][2] = landmark.getZ();

                    setFingerInfo(mLandmarkPoint);
                    sendHandData();
                }
                else
                {
                    mLandmarkPoint2[landmarkIndex][0] = landmark.getX();
                    mLandmarkPoint2[landmarkIndex][1] = landmark.getY();
                    mLandmarkPoint2[landmarkIndex][2] = landmark.getZ();

                    setFingerInfo2(mLandmarkPoint2);
                    sendHandData2();
                }

                setHandCount(multiHandLandmarks.size());

                ++landmarkIndex;
            }
            ++handIndex;
        }
        return multiHandLandmarksStr;
    }

    private void setHandCount(int count) {
        mSetHandDetectTime = System.currentTimeMillis();
        mHandCount = count;
    }

    public int GetHandCount() {
        if( mLandmarkPoint != null ) {

            double checkTime = System.currentTimeMillis() - mSetHandDetectTime;
            LOGGER.i("getHandCount(checkTime) : " + checkTime);
            if (checkTime > 1000.0)
            //if( checkTime > 300.0 )
            {
                LOGGER.i("getHandCount(checkTimeDone) : " + checkTime);
                mUseHandCount = 0;
                mFingerInfoListener.setHandCount(0);

                return 0;
            } else {
                float checkDistance = Math.abs(mLandmarkPoint[0][1] - mLandmarkPoint[9][1]);
                LOGGER.i("mLandmarkPoint[0][1] : " + mLandmarkPoint[0][1] + ", mLandmarkPoint[9][1] : " + mLandmarkPoint[9][1]);
                LOGGER.i("checkDistance : " + checkDistance);
                if (checkDistance < 0.04f) {
                    mUseHandCount = 0;
                    mFingerInfoListener.setHandCount(0);
                    return 0;
                }

                mUseHandCount = mHandCount;
                mFingerInfoListener.setHandCount(mHandCount);
                return mHandCount;
            }
        }
        return 0;
    }

    private Date lastTime = new Date();
    // lastTime은 기준 시간입니다.
    // 처음 생성당시의 시간을 기준으로 그 다음 1초가 지날때마다 갱신됩니다.
    private long frameCount = 0, nowFps = 0;
    // frameCount는 프레임마다 갱신되는 값입니다.
    // nowFps는 1초마다 갱신되는 값입니다.
    void count(){
        Date nowTime = new Date();
        long diffTime = nowTime.getTime() - lastTime.getTime();
        // 기준시간 으로부터 몇 초가 지났는지 계산합니다.

        if (diffTime >= 1000) {
            // 기준 시간으로 부터 1초가 지났다면
            nowFps = frameCount;
            LOGGER.i("[testKhkim] hand nowFps : "+nowFps);
            frameCount = 0;
            // nowFps를 갱신하고 카운팅을 0부터 다시합니다.
            lastTime = nowTime;
            // 1초가 지났으므로 기준 시간또한 갱신합니다.
        }

        frameCount++;
        // 기준 시간으로 부터 1초가 안지났다면 카운트만 1 올리고 넘깁니다.
    }

    int sendHandCount = 0;
    int sendHandCount2 = 0;

    //send to main...
    private void sendHandData()
    {
        sendHandCount++;
//        if( sendHandCount % 2 == 0 ) {
//            return;
//        }

        if(mFingerInfoListener != null && sendHandCount % 12 == 0)
        {
            count();

//            int totalCount = 0;
//            for(int i = 0; i < 21; i++) {
//                mSumFinger[totalCount] = mLandmarkPoint[i][0] * 1600.f;
//                totalCount++;
//            }
//            for(int i = 0; i < 21; i++) {
//                mSumFinger[totalCount] = mLandmarkPoint[i][1] * 1200.f;
//                totalCount++;
//            }
//            for(int i = 0; i < 21; i++) {
//                mSumFinger[totalCount] = mLandmarkPoint[i][2];
//                totalCount++;
//            }
//
//            mFingerInfoListener.setFingerLocation(mSumFinger);

            GetFingerLocation(mSendFingerX, mSendFingerY, mSendFingerZ);

            int totalCount = 0;
            for(int i = 0; i < 21; i++) {
                mSumFinger[totalCount] = mSendFingerX[i];
                totalCount++;
            }
            for(int i = 0; i < 21; i++) {
                mSumFinger[totalCount] = mSendFingerY[i];
                totalCount++;
            }
            for(int i = 0; i < 21; i++) {
                mSumFinger[totalCount] = mSendFingerZ[i];
                totalCount++;
            }

            if( CheckTouchPressed() == true ) {
                mSumFinger[totalCount] = 1.f;
            }
            else {
                mSumFinger[totalCount] = 0.f;
            }
            totalCount++;
            mSumFinger[totalCount] = mUseHandCount;

            mFingerInfoListener.setFingerLocation(mSumFinger);
        }
    }

    private void sendHandData2()
    {
        sendHandCount2++;

        if(mFingerInfoListener != null && sendHandCount2 % 12 == 0)
        {
//            int totalCount = 0;
//            for(int i = 0; i < 21; i++) {
//                mSumFinger2[totalCount] = mLandmarkPoint2[i][0] * 1600.f;
//                totalCount++;
//            }
//            for(int i = 0; i < 21; i++) {
//                mSumFinger2[totalCount] = mLandmarkPoint2[i][1] * 1200.f;
//                totalCount++;
//            }
//            for(int i = 0; i < 21; i++) {
//                mSumFinger2[totalCount] = mLandmarkPoint2[i][2];
//                totalCount++;
//            }
//
//            mFingerInfoListener.setFingerLocation2(mSumFinger2);

            GetFingerLocation2(mSendFingerX2, mSendFingerY2, mSendFingerZ2);

            int totalCount = 0;
            for(int i = 0; i < 21; i++) {
                mSumFinger2[totalCount] = mSendFingerX2[i];
                totalCount++;
            }
            for(int i = 0; i < 21; i++) {
                mSumFinger2[totalCount] = mSendFingerY2[i];
                totalCount++;
            }
            for(int i = 0; i < 21; i++) {
                mSumFinger2[totalCount] = mSendFingerZ2[i];
                totalCount++;
            }

            if( CheckTouchPressed() == true ) {
                mSumFinger2[totalCount] = 1.f;
            }
            else {
                mSumFinger2[totalCount] = 0.f;
            }
            totalCount++;
            mSumFinger2[totalCount] = mUseHandCount;

            mFingerInfoListener.setFingerLocation2(mSumFinger2);
        }
    }

    public void setFingerInfoListener(FingerInfoListener listener)
    {
        mFingerInfoListener = listener;
    }

    public interface FingerInfoListener
    {
        void setHandCount(int count);
        void setFingerLocation(float datas[]);
        void setFingerLocation2(float datas[]);

        //will add something...
        void setTouchedOn();
    }
    //send to main...

//    public static boolean getTouchArea()
//    {
//        return (mTouchAreaSelected || mTouchAreaSelected2);
//    }
//
//    public static boolean getPointFinger()
//    {
//        return (mCheckPointFinger1||mCheckPointFinger2);
//    }
}
