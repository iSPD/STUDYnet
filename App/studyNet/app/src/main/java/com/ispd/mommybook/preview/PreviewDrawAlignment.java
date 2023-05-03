package com.ispd.mommybook.preview;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.TuningManager;
import com.ispd.mommybook.motion.MotionHandTrackingDataManager;
import com.ispd.mommybook.motion.MotionHandTrackingImpl;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static org.opencv.core.CvType.CV_32F;

public class PreviewDrawAlignment {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;

    private int mGLProgram = -1;
    private FloatBuffer mGLVertex = null, mGLTexCoord = null;

    private float []mCropMatrix = new float[9];
    private float []mLeftMatrix = new float[9];
    private float []mRightMatrix = new float[9];
    private float []mAlignmentLeftMatrix = new float[9];
    private float []mAlignmentRightMatrix = new float[9];

    private AlignmentAndCurveValues mAlignmentAndCurveValues = null;
    private float mLeftCurvedValues[] = new float[4];
    private float mRightCurvedValues[] = new float[4];

    private static int mBookCoverIndex = 0;
    private static int mBookPageIndex = 3;

    /**
     * Create Shader for Preview
     *
     * @param width
     * @param height
     */
    public PreviewDrawAlignment(int width, int height) {

        mCameraPreviewWidth = width;
        mCameraPreviewHeight = height;

        if( mGLProgram == -1 ) {
            mGLProgram = PreviewRendererImpl.setProgram(SOURCE_DRAW_VS, SOURCE_DRAW_FS);

            float[] vtmp = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
            float[] ttmp = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};

            mGLVertex = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLVertex.put(vtmp);
            mGLVertex.position(0);

            mGLTexCoord = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mGLTexCoord.put(ttmp);
            mGLTexCoord.position(0);
        }
    }

    /**
     * Release Shader for Preview
     *
     */
    public void release() {
        if ( mGLProgram   > 0 ) GLES20.glDeleteProgram(mGLProgram);
        mGLProgram  = -1;

        mGLVertex = null;
        mGLTexCoord = null;

        LOGGER.d("Released Program & Vertex & TextureCoord");
    }

    public static void SetCurrentBookInfo(int bookCoverIndex, int bookPageIndex) {

        if (bookCoverIndex == -1 || bookPageIndex == -1) {
            return;
        }

        if (bookCoverIndex == 0) {
            if (bookPageIndex == 7) {
                return;
            }
        } else if (bookCoverIndex == 1) {
            if (bookPageIndex == 7) {
                return;
            }
        } else if (bookCoverIndex == 2) {
            if (bookPageIndex == 15) {
                return;
            }
        }

        mBookCoverIndex = bookCoverIndex;
        mBookPageIndex = bookPageIndex;
    }

    private void getAlignmentMatrix() {
        JniController.getCropMatrixValue(mCropMatrix, mCameraPreviewWidth, mCameraPreviewHeight);
        JniController.getAlignmentMatrixValue(mLeftMatrix, mRightMatrix, mCameraPreviewWidth, mCameraPreviewHeight);

        Mat spanMat = new Mat(3, 3, CV_32F);
        spanMat.put(0, 0, mCropMatrix);
        spanMat = spanMat.inv();

        Mat leftMat = new Mat(3, 3, CV_32F);
        leftMat.put(0, 0, mLeftMatrix);
        leftMat = leftMat.inv();

        Mat rightMat = new Mat(3, 3, CV_32F);
        rightMat.put(0, 0, mRightMatrix);
        rightMat = rightMat.inv();

        Mat lastLeftMat = new Mat(3, 3, CV_32F);
        Mat lastRightMat = new Mat(3, 3, CV_32F);

        Mat onesMat = new Mat(3, 3, CV_32F);
        float onesData[] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
        onesMat.put(0, 0, onesData);

        Core.gemm(leftMat, spanMat, 1, onesMat, 0, lastLeftMat, 0);
        Core.gemm(rightMat, spanMat, 1, onesMat, 0, lastRightMat, 0);

        lastLeftMat = lastLeftMat.inv();
        lastRightMat = lastRightMat.inv();

        lastLeftMat.get(0, 0, mAlignmentLeftMatrix);
        lastRightMat.get(0, 0, mAlignmentRightMatrix);

        {
            JniController.getAlignmentCurveValue(mLeftCurvedValues, mRightCurvedValues);

            float sumDatas[] = new float[64 + 2];
            System.arraycopy(mAlignmentLeftMatrix, 0, sumDatas, 0, mAlignmentLeftMatrix.length);
            System.arraycopy(mAlignmentRightMatrix, 0, sumDatas, 9, mAlignmentRightMatrix.length);//9

            System.arraycopy(mLeftCurvedValues, 0, sumDatas, 18, mLeftCurvedValues.length);//3
            System.arraycopy(mRightCurvedValues, 0, sumDatas, 22, mRightCurvedValues.length);//3

            System.arraycopy(TuningManager.mCurveValueTuneLeft, 0, sumDatas, 26, TuningManager.mCurveValueTuneLeft.length);//3
            System.arraycopy(TuningManager.mCurveVerticalTuneLeft, 0, sumDatas, 30, TuningManager.mCurveVerticalTuneLeft.length);//1
            System.arraycopy(TuningManager.mCurveVerticalStartLeft, 0, sumDatas, 32, TuningManager.mCurveVerticalStartLeft.length);//3
            System.arraycopy(TuningManager.mCurveVerticalCurveLeft, 0, sumDatas, 36, TuningManager.mCurveVerticalCurveLeft.length);//4
            System.arraycopy(TuningManager.mCurveStartByValueLeft, 0, sumDatas, 41, TuningManager.mCurveStartByValueLeft.length);//3

            System.arraycopy(TuningManager.mCurveValueTuneRight, 0, sumDatas, 45, TuningManager.mCurveValueTuneRight.length);//3
            System.arraycopy(TuningManager.mCurveVerticalTuneRight, 0, sumDatas, 49, TuningManager.mCurveVerticalTuneRight.length);//1
            System.arraycopy(TuningManager.mCurveVerticalStartRight, 0, sumDatas, 51, TuningManager.mCurveVerticalStartRight.length);//3
            System.arraycopy(TuningManager.mCurveVerticalCurveRight, 0, sumDatas, 55, TuningManager.mCurveVerticalCurveRight.length);//4
            System.arraycopy(TuningManager.mCurveStartByValueRight, 0, sumDatas, 60, TuningManager.mCurveStartByValueRight.length);//3
            //64

            float[] bookInfo = {(float) mBookCoverIndex, (float) mBookPageIndex};
            System.arraycopy(bookInfo, 0, sumDatas, 64, bookInfo.length);//3

            sendAlignmentAndCurveData(sumDatas);
        }
    }

    public void DrawPreview(int fbo, int exTextures, int textures, int front)
    {
        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        }

        GLES20.glUseProgram (mGLProgram);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, mCameraPreviewWidth, mCameraPreviewHeight);

        int ph = GLES20.glGetAttribLocation(mGLProgram, "vPosition");
        int tch = GLES20.glGetAttribLocation(mGLProgram, "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertex);
        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoord);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgram, "uMVPMatrix"), 1, false, PreviewRendererImpl.mMVPMatrixBuffer0);

        getAlignmentMatrix();
        GLES20.glUniform3f(GLES20.glGetUniformLocation(mGLProgram, "inverseHomographyLeft1"), mAlignmentRightMatrix[0], mAlignmentRightMatrix[1], mAlignmentRightMatrix[2]);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(mGLProgram, "inverseHomographyLeft2"), mAlignmentRightMatrix[3], mAlignmentRightMatrix[4], mAlignmentRightMatrix[5]);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(mGLProgram, "inverseHomographyLeft3"), mAlignmentRightMatrix[6], mAlignmentRightMatrix[7], mAlignmentRightMatrix[8]);

        GLES20.glUniform3f(GLES20.glGetUniformLocation(mGLProgram, "inverseHomographyRight1"), mAlignmentLeftMatrix[0], mAlignmentLeftMatrix[1], mAlignmentLeftMatrix[2]);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(mGLProgram, "inverseHomographyRight2"), mAlignmentLeftMatrix[3], mAlignmentLeftMatrix[4], mAlignmentLeftMatrix[5]);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(mGLProgram, "inverseHomographyRight3"), mAlignmentLeftMatrix[6], mAlignmentLeftMatrix[7], mAlignmentLeftMatrix[8]);

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, exTextures);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sCameraTexture"), 0);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "uFront"), front);

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        //Correct?
        //GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        GLES20.glUseProgram(0);

        if ( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }

    public static final String SOURCE_DRAW_VS = "" +
            "attribute vec2 vPosition;\n" +
            "attribute vec2 vTexCoord;\n" +
            "varying vec2 texCoord;\n" +

            "uniform mat4 uMVPMatrix;\n" +

            "void main() {\n" +
            "  texCoord = vTexCoord;\n" +
            "  gl_Position = uMVPMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
            "}";

    public static final String SOURCE_DRAW_FS = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            //"precision mediump float;\n" +
            "precision highp float;\n" +

            "uniform samplerExternalOES sCameraTexture;\n" +

            "uniform vec3 inverseHomographyLeft1;\n" +
            "uniform vec3 inverseHomographyLeft2;\n" +
            "uniform vec3 inverseHomographyLeft3;\n" +

            "uniform vec3 inverseHomographyRight1;\n" +
            "uniform vec3 inverseHomographyRight2;\n" +
            "uniform vec3 inverseHomographyRight3;\n" +

            "varying vec2 texCoord;\n" +
            "uniform int uFront;\n" +

            "void main() {\n" +
            "   vec2 newTexCoord = texCoord;\n" +
            "   if( uFront == 1 ) {\n" +
            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "   }\n" +

            "   float width = 1280.0; \n" +
            "   float height = 960.0; \n" +

            //Right
            "       if( newTexCoord.x < 0.5 ) {\n" +

            "           vec3 frameCoordinate = vec3(newTexCoord.x * width, newTexCoord.y * height, 1.0); \n" +

            "           float data1 = inverseHomographyLeft1.x * frameCoordinate.x + inverseHomographyLeft1.y * frameCoordinate.y + inverseHomographyLeft1.z * frameCoordinate.z; \n" +
            "           float data2 = inverseHomographyLeft2.x * frameCoordinate.x + inverseHomographyLeft2.y * frameCoordinate.y + inverseHomographyLeft2.z * frameCoordinate.z; \n" +
            "           float data3 = inverseHomographyLeft3.x * frameCoordinate.x + inverseHomographyLeft3.y * frameCoordinate.y + inverseHomographyLeft3.z * frameCoordinate.z; \n" +

            "           vec2 coords = vec2(data1 / width, data2 / height) / data3;  \n" +

            "           if (coords.x >= 0.0 && coords.x <= 1.0 && coords.y >= 0.0 && coords.y <= 1.0) { \n" +
            "               gl_FragColor = texture2D(sCameraTexture, coords); \n" +
            "           } else { \n" +
            "               gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0); \n" +
            "           } \n" +
            //Left
            "       } else {\n" +

            "           vec3 frameCoordinate = vec3(newTexCoord.x * width, newTexCoord.y * height, 1.0); \n" +

            "           float data1 = inverseHomographyRight1.x * frameCoordinate.x + inverseHomographyRight1.y * frameCoordinate.y + inverseHomographyRight1.z * frameCoordinate.z; \n" +
            "           float data2 = inverseHomographyRight2.x * frameCoordinate.x + inverseHomographyRight2.y * frameCoordinate.y + inverseHomographyRight2.z * frameCoordinate.z; \n" +
            "           float data3 = inverseHomographyRight3.x * frameCoordinate.x + inverseHomographyRight3.y * frameCoordinate.y + inverseHomographyRight3.z * frameCoordinate.z; \n" +

            "           vec2 coords = vec2(data1 / width, data2 / height) / data3;  \n" +

            "           if (coords.x >= 0.0 && coords.x <= 1.0 && coords.y >= 0.0 && coords.y <= 1.0) { \n" +
            "               gl_FragColor = texture2D(sCameraTexture, coords); \n" +
            "           } else { \n" +
            "               gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0); \n" +
            "           } \n" +
            "       }\n" +
            "}";

    //send to main...
    private void sendAlignmentAndCurveData(float datas[])
    {
        mAlignmentAndCurveValues.setAlignmentAndCurveValues(datas);
    }

    public void SetAlignmentAndCurveListener(AlignmentAndCurveValues listener)
    {
        mAlignmentAndCurveValues = listener;
    }

    public interface AlignmentAndCurveValues
    {
        void setAlignmentAndCurveValues(float datas[]);
    }
    //send to main...
}
