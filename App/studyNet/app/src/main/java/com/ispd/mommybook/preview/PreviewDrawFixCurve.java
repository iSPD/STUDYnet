package com.ispd.mommybook.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.TuningManager;
import com.ispd.mommybook.utils.UtilsLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;

public class PreviewDrawFixCurve {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private Context mContext;

    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;

    private int mGLProgram = -1;
    private FloatBuffer mGLVertex = null, mGLTexCoord = null;

    private static int mBookCoverIndex = 0;
    private static int mBookPageIndex = 3;

    private static int mUseBitmapTexture[] = {-1};

    private PreviewBitmapStorage mBitmapStorage1 = null;
    private PreviewBitmapStorage mBitmapStorage2 = null;
    private PreviewBitmapStorage mBitmapStorage3 = null;

    private static int mBitmapTextureName1[] = {-1, -1, -1, -1, -1, -1, -1};
    private static int mBitmapTextureName2[] = {-1, -1, -1, -1, -1, -1, -1};
    private static int mBitmapTextureName3[] = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    private float mLeftCurvedValues[] = new float[4];
    private float mRightCurvedValues[] = new float[4];

    private int mCurveFixOn = 1;

    /**
     * Create Shader for Preview
     *
     * @param width
     * @param height
     */
    public PreviewDrawFixCurve(int width, int height) {

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

        createBitmapTexture();
    }

    /**
     * Release Shader for Preview
     *
     */
    public void release() {

        if (mBitmapTextureName1[0] > 0) {

            GLES20.glDeleteBuffers(7, mBitmapTextureName1, 0);
        }

        if (mBitmapTextureName2[0] > 0) {

            GLES20.glDeleteBuffers(7, mBitmapTextureName2, 0);
        }

        if (mBitmapTextureName3[0] > 0) {

            GLES20.glDeleteBuffers(16, mBitmapTextureName3, 0);
        }

        if ( mGLProgram   > 0 ) GLES20.glDeleteProgram(mGLProgram);
        mGLProgram  = -1;

        mGLVertex = null;
        mGLTexCoord = null;

        LOGGER.d("Released Program & Vertex & TextureCoord");
    }

    private void getAlignmentCurveValue() {
        JniController.getAlignmentCurveValue(mLeftCurvedValues, mRightCurvedValues);
    }

    private void createBitmapTexture() {

        LOGGER.d("createBitmapTexture start");

        Thread thread1 = new Thread(new Runnable() { //sally-v2
            @Override
            public void run() {
                mBitmapStorage1 = new PreviewBitmapStorage(0);
            }
        });
        thread1.start(); //sally-v2

        Thread thread2 = new Thread(new Runnable() { //sally-v2
            @Override
            public void run() {
                mBitmapStorage2 = new PreviewBitmapStorage( 1);
            }
        });
        thread2.start(); //sally-v2

        Thread thread3 = new Thread(new Runnable() { //sally-v2
            @Override
            public void run() {
                mBitmapStorage3 = new PreviewBitmapStorage(2);
            }
        });
        thread3.start(); //sally-v2
        //sally-v2
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        }
        catch(Exception e) {
            LOGGER.e("createBitmapTexture", "createBitmapTextrue ERROR", e);
        }

        //mBitmapStorage1 = new PreviewBitmapStorage(0);

        ArrayList bitmapList1 = mBitmapStorage1.GetBitmapList();
        for( int i = 0; i < 7; i++ ) {
            Bitmap bitmap = (Bitmap) bitmapList1.get(i);
            mBitmapTextureName1[i] = PreviewRendererImpl.createTextureBitmap(bitmap.getWidth(),  bitmap.getHeight(), bitmap);
        }

        //mBitmapStorage2 = new PreviewBitmapStorage(1);

        ArrayList bitmapList2 = mBitmapStorage2.GetBitmapList();
        for( int i = 0; i < 7; i++ ) {
            Bitmap bitmap = (Bitmap) bitmapList2.get(i);
            mBitmapTextureName2[i] = PreviewRendererImpl.createTextureBitmap(bitmap.getWidth(),  bitmap.getHeight(), bitmap);
        }

        //mBitmapStorage3 = new PreviewBitmapStorage(2);

        ArrayList bitmapList3 = mBitmapStorage3.GetBitmapList();
        for( int i = 0; i < 15; i++ ) {
            Bitmap bitmap = (Bitmap) bitmapList3.get(i);
            mBitmapTextureName3[i] = PreviewRendererImpl.createTextureBitmap(bitmap.getWidth(),  bitmap.getHeight(), bitmap);
        }

        LOGGER.d("createBitmapTexture end");
    }

    public static void SetCurrentBookInfo(int bookCoverIndex, int bookPageIndex) {

        mBookCoverIndex = bookCoverIndex;
        mBookPageIndex = bookPageIndex;

        if( mBookCoverIndex ==-1 || mBookPageIndex == -1 ) {
            return;
        }

        if( mBookCoverIndex == 0 ) {
            if(mBookPageIndex == 7) {
                return;
            }

            mUseBitmapTexture[0] = mBitmapTextureName1[mBookPageIndex];
        }
        else if( mBookCoverIndex == 1 ) {
            if(mBookPageIndex == 7) {
                return;
            }

            mUseBitmapTexture[0] = mBitmapTextureName2[mBookPageIndex];
        }
        else if( mBookCoverIndex == 2 ) {
            if(mBookPageIndex == 15) {
                return;
            }

            mUseBitmapTexture[0] = mBitmapTextureName3[mBookPageIndex];
        }
    }

    public void SetCurveFix(int onOff) {
        mCurveFixOn = onOff;
    }

    public void DrawPreview(int fbo, int cameraTexture, int alignedTexture, int front)
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

        getAlignmentCurveValue();

        //mLeftCurvedValues, mRightCurvedValues
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uLeftCurveValues"), mLeftCurvedValues[0], mLeftCurvedValues[1], mLeftCurvedValues[2], mLeftCurvedValues[3]);
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uRightCurveValues"), mRightCurvedValues[0], mRightCurvedValues[1], mRightCurvedValues[2], mRightCurvedValues[3]);

        //left
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneLeft1"), TuningManager.mCurveValueTuneLeft[0], TuningManager.mCurveValueTuneLeft[1], TuningManager.mCurveValueTuneLeft[2], TuningManager.mCurveValueTuneLeft[3]);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneLeft2"), TuningManager.mCurveVerticalTuneLeft[0], TuningManager.mCurveVerticalTuneLeft[1]);
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneLeft3"), TuningManager.mCurveVerticalStartLeft[0], TuningManager.mCurveVerticalStartLeft[1], TuningManager.mCurveVerticalStartLeft[2], TuningManager.mCurveVerticalStartLeft[3]);
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneLeft41"), TuningManager.mCurveVerticalCurveLeft[0], TuningManager.mCurveVerticalCurveLeft[1], TuningManager.mCurveVerticalCurveLeft[2], TuningManager.mCurveVerticalCurveLeft[3]);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneLeft42"), TuningManager.mCurveVerticalCurveLeft[4]);
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneLeft5"), TuningManager.mCurveStartByValueLeft[0], TuningManager.mCurveStartByValueLeft[1], TuningManager.mCurveStartByValueLeft[2], TuningManager.mCurveStartByValueLeft[3]);

        //right
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneRight1"), TuningManager.mCurveValueTuneRight[0], TuningManager.mCurveValueTuneRight[1], TuningManager.mCurveValueTuneRight[2], TuningManager.mCurveValueTuneRight[3]);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneRight2"), TuningManager.mCurveVerticalTuneRight[0], TuningManager.mCurveVerticalTuneRight[1]);
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneRight3"), TuningManager.mCurveVerticalStartRight[0], TuningManager.mCurveVerticalStartRight[1], TuningManager.mCurveVerticalStartRight[2], TuningManager.mCurveVerticalStartRight[3]);
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneRight41"), TuningManager.mCurveVerticalCurveRight[0], TuningManager.mCurveVerticalCurveRight[1], TuningManager.mCurveVerticalCurveRight[2], TuningManager.mCurveVerticalCurveRight[3]);
        GLES20.glUniform1f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneRight42"), TuningManager.mCurveVerticalCurveRight[4]);
        GLES20.glUniform4f(GLES20.glGetUniformLocation(mGLProgram, "uCurveTuneRight5"), TuningManager.mCurveStartByValueRight[0], TuningManager.mCurveStartByValueRight[1], TuningManager.mCurveStartByValueRight[2], TuningManager.mCurveStartByValueRight[3]);

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sCameraTexture"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_2D, alignedTexture);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sAlignedTexture"), 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
//        GLES20.glBindTexture(GL_TEXTURE_2D, mBitmapTextureName1[3]);
        GLES20.glBindTexture(GL_TEXTURE_2D, mUseBitmapTexture[0]);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sBitmapTexture"), 2);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "uFront"), front);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "uCurveFixOn"), mCurveFixOn);

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

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
            "precision mediump float;\n" +
            //"precision highp float;\n" +

            "uniform samplerExternalOES sCameraTexture;\n" +
            "uniform sampler2D sAlignedTexture;\n" +
            "uniform sampler2D sBitmapTexture;\n" +

            "varying vec2 texCoord;\n" +
            "uniform int uFront;\n" +

            "uniform vec4 uLeftCurveValues;\n" + //xyzw
            "uniform vec4 uRightCurveValues;\n" + //xyzw

            "uniform vec4 uCurveTuneLeft1;\n" + //xyzw
            "uniform vec2 uCurveTuneLeft2;\n" + //xy
            "uniform vec4 uCurveTuneLeft3;\n" + //xyzw
            "uniform vec4 uCurveTuneLeft41;\n" + //xyzw
            "uniform float uCurveTuneLeft42;\n" +
            "uniform vec4 uCurveTuneLeft5;\n" +

            "uniform vec4 uCurveTuneRight1;\n" + //xyzw
            "uniform vec2 uCurveTuneRight2;\n" + //xy
            "uniform vec4 uCurveTuneRight3;\n" + //xyzw
            "uniform vec4 uCurveTuneRight41;\n" + //xyzw
            "uniform float uCurveTuneRight42;\n" +
            "uniform vec4 uCurveTuneRight5;\n" +

            "uniform int uCurveFixOn;\n" +

            "void main() {\n" +
            "   vec2 newTexCoord = texCoord;\n" +

            "   if( uFront == 1 ) {\n" +
//            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "       newTexCoord.y = 1.0 - newTexCoord.y;\n" +
            "   }\n" +

            "   float resizeY = 960.0 / (960.0 - (10.0 * 4.0));\n" +
            "   resizeY = texCoord.y * resizeY;\n" +

            "   vec4 bitmapImage = vec4(0.0, 0.0, 0.0, 1.0); \n" +
            "   if( resizeY <= 1.0 ) {\n" +
            "       vec2 resizeTexCoord = vec2(texCoord.x, resizeY);\n" +
            "       bitmapImage = texture2D(sBitmapTexture, resizeTexCoord); \n" +
            "   }\n" +

            "   vec4 fixCurveImage = vec4(1.0, 1.0, 1.0, 1.0); \n" +

            //Left Values
            "   if( uCurveFixOn == 1 && newTexCoord.x < 0.5 ) {\n" +

            "       float useCurvedFactor = uLeftCurveValues.w;\n" +

            "       float curveX1 = uCurveTuneLeft1.x;\n" +
            "       float curveY1 = uCurveTuneLeft1.y;\n" +
            "       float curveX2 = uCurveTuneLeft1.z;\n" +
            "       float curveY2 = uCurveTuneLeft1.w;\n" +

            "       float curveA = (curveY2-curveY1) / (curveX2-curveX1);\n" +
            "       float curveB = curveY1 - (curveA * curveX1);\n" +

            "       float weightValue = (curveA * useCurvedFactor) + curveB;\n" +
            "       useCurvedFactor = useCurvedFactor * weightValue;\n" +

            //horizon fix...
            "		float somethingValue = useCurvedFactor;\n" +
            "		float horiX1 = uCurveTuneLeft5.x;\n" +
            "		float horiY1 = uCurveTuneLeft5.y;\n" +
            "		float horiX2 = uCurveTuneLeft5.z;\n" +
            "		float horiY2 = uCurveTuneLeft5.w;\n" +

            "		float horiA = (horiY2 - horiY1) / (horiX2 - horiX1);\n" +
            "		float horiB = horiY1 - (horiA * horiX1);\n" +

            "		float horiWeight = (horiA * somethingValue) + horiB;\n" +

            "		float useCurveVerticalStartLeft0 = uCurveTuneLeft3.x * horiWeight;\n" +
            "		float useCurveVerticalStartLeft2 = uCurveTuneLeft3.z * horiWeight;\n" +

            "		 if( newTexCoord.x > 0.5 - ( 0.5 * useCurveVerticalStartLeft2 / uCurveTuneLeft3.w) ) \n" +
            "		 { \n" +
            "		 	float diffRateY = uCurveTuneLeft2.x * (1.0 - newTexCoord.y) + uCurveTuneLeft2.y; \n" +
            "		 	if (diffRateY > 1.0) \n" +
            "		 	{ \n" +
            "		 		//diffRateY = 1.0; \n" +
            "		 	} \n" +
            "		    //diffRateY = 1.0; \n" +

            "		 	float x1 = 0.0; \n" +
            "		 	float y1 = 0.5 * useCurveVerticalStartLeft0 / uCurveTuneLeft3.y; \n" +
            "		 	float x2 = 1.0; \n" +
            "		 	float y2 = 0.5 * useCurveVerticalStartLeft2 / uCurveTuneLeft3.w; \n" +

            "		 	float graphA = (y2-y1) / (x2-x1); \n" +
            "		 	float graphB = y1 - graphA * x1; \n" +

            "		 	float calcWidth = graphA * (1.0 -newTexCoord.y) + graphB; \n" +

            "		 	float inputValue = ( newTexCoord.x - ( 0.5 - calcWidth ) ) / calcWidth; \n" +
            "		 	if (inputValue < 0.0) { \n" +
            "		 		inputValue = 0.0; \n" +
            "		 	} \n" +

            "		 	float xx1 = uCurveTuneLeft41.x; \n" +
            "		 	float yy1 = uCurveTuneLeft41.y; \n" +
            "		 	float xx2 = uCurveTuneLeft41.z; \n" +
            "		 	float yy2 = uCurveTuneLeft41.w; \n" +

            "		 	float graphAA = (yy2-yy1)/(xx2-xx1); \n" +
            "		 	float graphBB = yy1 - graphAA * xx1; \n" +

            //useValueY_3 - useValueY_1
            "		 	float graphFactor = graphAA * useCurvedFactor + graphBB; \n" +

            "		    if( graphFactor > uCurveTuneLeft41.y )\n" +
            "		    {\n" +
            "			    graphFactor = uCurveTuneLeft41.y;\n" +
            "		    }\n" +
            "		    else if( graphFactor < uCurveTuneLeft41.w )\n" +
            "		    {\n" +
            "			    graphFactor = uCurveTuneLeft41.w;\n" +
            "		    }\n" +

            //"		 	float calcY = pow(inputValue, uCurveTuneLeft42) / graphFactor; \n" +
            "		 	float calcY = sin(pow(1.8 * inputValue, 2.0)) / 2.4; \n" +
            //useValueY_3 - useValueY_1
            "		 	calcY = calcY * (useCurvedFactor * 2.0) * diffRateY; \n" +

            "           int tansY = int(calcY);\n" +
            "           int targetY = int(newTexCoord.y * 960.0) - tansY;\n" +
            "           float targetYF = float(float(targetY) / 960.0);\n" +

            "           fixCurveImage = texture2D(sAlignedTexture, vec2(newTexCoord.x, targetYF)); \n" +
            "		 } \n" +
            "        else {\n" +
            "           fixCurveImage = texture2D(sAlignedTexture, newTexCoord); \n" +
            "        }\n" +
            "   }\n" +
            //Right Values
            "   else if( uCurveFixOn == 1 && newTexCoord.x >= 0.5 ) {\n" +

            "       float useCurvedFactor = uRightCurveValues.x;\n" +

            "       float curveX1 = uCurveTuneRight1.x;\n" +
            "       float curveY1 = uCurveTuneRight1.y;\n" +
            "       float curveX2 = uCurveTuneRight1.z;\n" +
            "       float curveY2 = uCurveTuneRight1.w;\n" +

            "       float curveA = (curveY2-curveY1) / (curveX2-curveX1);\n" +
            "       float curveB = curveY1 - (curveA * curveX1);\n" +

            "       float weightValue = (curveA * useCurvedFactor) + curveB;\n" +
            "       useCurvedFactor = useCurvedFactor * weightValue;\n" +

            //horizon fix...
            "		float somethingValue = useCurvedFactor;\n" +
            "		float horiX1 = uCurveTuneRight5.x;\n" +
            "		float horiY1 = uCurveTuneRight5.y;\n" +
            "		float horiX2 = uCurveTuneRight5.z;\n" +
            "		float horiY2 = uCurveTuneRight5.w;\n" +

            "		float horiA = (horiY2 - horiY1) / (horiX2 - horiX1);\n" +
            "		float horiB = horiY1 - (horiA * horiX1);\n" +

            "		float horiWeight = (horiA * somethingValue) + horiB;\n" +

            "		float useCurveVerticalStartLeft0 = uCurveTuneRight3.x * horiWeight;\n" +
            "		float useCurveVerticalStartLeft2 = uCurveTuneRight3.z * horiWeight;\n" +

            "		 if( newTexCoord.x < 0.5 + (0.5 * useCurveVerticalStartLeft2 / uCurveTuneRight3.w) ) \n" +
            "		 { \n" +
            "		 	float diffRateY = uCurveTuneRight2.x * (1.0 - newTexCoord.y) + uCurveTuneRight2.y; \n" +
            "		 	if (diffRateY > 1.2) \n" +
            "		 	{ \n" +
            "		 		//diffRateY = 1.2; \n" +
            "		 	} \n" +
            "		    //diffRateY = 1.0; \n" +

            "		 	float x1 = 0.0; \n" +
            "		 	float y1 = 0.5 * useCurveVerticalStartLeft0 / uCurveTuneRight3.y; \n" +
            "		 	float x2 = 1.0; \n" +
            "		 	float y2 = 0.5 * useCurveVerticalStartLeft2 / uCurveTuneRight3.w; \n" +

            "		 	float graphA = (y2-y1) / (x2-x1); \n" +
            "		 	float graphB = y1 - graphA * x1; \n" +

            "		 	float calcWidth = graphA * (1.0 - newTexCoord.y) + graphB; \n" +

            "		 	float inputValue = (newTexCoord.x - 0.5) / calcWidth; \n" +
            "		 	if (inputValue > 1.0) { \n" +
            "		 		inputValue = 1.0; \n" +
            "		 	} \n" +

            "		 	float xx1 = uCurveTuneRight41.x; \n" +
            "		 	float yy1 = uCurveTuneRight41.y; \n" +
            "		 	float xx2 = uCurveTuneRight41.z; \n" +
            "		 	float yy2 = uCurveTuneRight41.w; \n" +

            "		 	float graphAA = (yy2-yy1)/(xx2-xx1); \n" +
            "		 	float graphBB = yy1 - graphAA * xx1; \n" +

            //useValueY_1 - useValueY_3
            "		 	float graphFactor = graphAA * useCurvedFactor + graphBB; \n" +

            "		    if( graphFactor > uCurveTuneRight41.y )\n" +
            "		    {\n" +
            "			    graphFactor = uCurveTuneRight41.y;\n" +
            "		    }\n" +
            "		    else if( graphFactor < uCurveTuneRight41.w )\n" +
            "		    {\n" +
            "			    graphFactor = uCurveTuneRight41.w;\n" +
            "		    }\n" +

            "		 	float calcY = (-pow(inputValue, uCurveTuneRight42) + 1.0) / graphFactor;//0.7, 0.4 : x \n" +

            //useValueY_1 - useValueY_3
            "		 	calcY = calcY * (useCurvedFactor * 2.0) * diffRateY; \n" +

            //input...
            "           int tansY = int(calcY);\n" +
            "           int targetY = int(newTexCoord.y * 960.0) - tansY;\n" +
            "           float targetYF = float(float(targetY) / 960.0);\n" +

            "           fixCurveImage = texture2D(sAlignedTexture, vec2(newTexCoord.x, targetYF)); \n" +
            "		 } \n" +
            "        else {\n" +
            "           fixCurveImage = texture2D(sAlignedTexture, newTexCoord); \n" +
            "        }\n" +
            "   }\n" +
            "   else {\n" +
            "       fixCurveImage = texture2D(sAlignedTexture, newTexCoord); \n" +
            "   }\n" +

//            "   fixCurveImage = texture2D(sAlignedTexture, newTexCoord); \n" +
//            "   gl_FragColor = fixCurveImage * 0.5 + bitmapImage * 0.5; \n" +

            "   if( bitmapImage.a == 0.0 ) { \n" +
            "       gl_FragColor = fixCurveImage; \n" +
            "   } \n" +
            "   else { \n" +
            "       gl_FragColor = bitmapImage; \n" +
            "   } \n" +
            "}";
}
