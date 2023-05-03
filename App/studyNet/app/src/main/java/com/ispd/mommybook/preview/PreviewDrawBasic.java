package com.ispd.mommybook.preview;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.utils.UtilsLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;

public class PreviewDrawBasic {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;

    private int mGLProgram = -1;
    private FloatBuffer mGLVertex = null, mGLTexCoord = null;

    private float mLeftTopX;
    private float mLeftTopY;

    private float mRightTopX;
    private float mRightTopY;

    private float mLeftBottomX;
    private float mLeftBottomY;

    private float mRightBottomX;
    private float mRightBottomY;

    private float mTopVerticalX;
    private float mTopVerticalY;

    private float mBottomVerticalX;
    private float mBottomVerticalY;

    private float mLineQueue[][] = new float[12][10];

    /**
     * Create Shader for Preview
     *
     * @param width
     * @param height
     */
    public PreviewDrawBasic(int width, int height) {

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

        for(int i = 0; i < 12; i++) {
            for(int j = 0; j < 10; j++) {
                mLineQueue[i][j] = 10.f;
            }
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

    private void doAnimationEffect() {
        //do animation
        int animationCount = 5;

        for(int i = 0; i < 12; i++) {
            for (int j = 0; j < animationCount; j++) {
                mLineQueue[i][j] = mLineQueue[i][j+1];
            }
        }

        mLineQueue[0][animationCount] = mLeftTopX;
        mLineQueue[1][animationCount] = mLeftTopY;
        mLineQueue[2][animationCount] = mRightTopX;
        mLineQueue[3][animationCount] = mRightTopY;
        mLineQueue[4][animationCount] = mLeftBottomX;
        mLineQueue[5][animationCount] = mLeftBottomY;
        mLineQueue[6][animationCount] = mRightBottomX;
        mLineQueue[7][animationCount] = mRightBottomY;
        mLineQueue[8][animationCount] = mTopVerticalX;
        mLineQueue[9][animationCount] = mTopVerticalY;
        mLineQueue[10][animationCount] = mBottomVerticalX;
        mLineQueue[11][animationCount] = mBottomVerticalY;

        float sumQueue[] = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
        for(int i = 0; i < 12; i++) {
            for(int j = 0; j < animationCount; j++ ) {
                sumQueue[i] += mLineQueue[i][j];
            }
        }

        mLeftTopX = sumQueue[0] / animationCount;
        mLeftTopY = sumQueue[1] / animationCount;
        mRightTopX = sumQueue[2] / animationCount;
        mRightTopY = sumQueue[3] / animationCount;
        mLeftBottomX = sumQueue[4] / animationCount;
        mLeftBottomY = sumQueue[5] / animationCount;
        mRightBottomX = sumQueue[6] / animationCount;
        mRightBottomY = sumQueue[7] / animationCount;
        mTopVerticalX = sumQueue[8] / animationCount;
        mTopVerticalY = sumQueue[9] / animationCount;
        mBottomVerticalX = sumQueue[10] / animationCount;
        mBottomVerticalY = sumQueue[11] / animationCount;
    }

    private void getCropFourPoints() {
        final float fourPoints[] = new float[8];
        final float verticalPoints[] = new float[4];

        JniController.getCropFourPoint(fourPoints, verticalPoints);

        mLeftTopX = fourPoints[0] / mCameraPreviewWidth;
        mLeftTopY = fourPoints[1] / mCameraPreviewHeight;

        mRightTopX = fourPoints[2] / mCameraPreviewWidth;
        mRightTopY = fourPoints[3] / mCameraPreviewHeight;

        mLeftBottomX = fourPoints[4] / mCameraPreviewWidth;
        mLeftBottomY = fourPoints[5] / mCameraPreviewHeight;

        mRightBottomX = fourPoints[6] / mCameraPreviewWidth;
        mRightBottomY = fourPoints[7] / mCameraPreviewHeight;

        mTopVerticalX = verticalPoints[0] / mCameraPreviewWidth;
        mTopVerticalY = verticalPoints[1] / mCameraPreviewHeight;

        mBottomVerticalX = verticalPoints[2] / mCameraPreviewWidth;
        mBottomVerticalY = verticalPoints[3] / mCameraPreviewHeight;

        doAnimationEffect();
    }

    /**
     * Draw Preview Frame
     *
     * @param fbo
     * @param exTextures
     * @param textures
     * @param front
     */
    public void DrawPreview(int fbo, int exTextures, int textures, int front, boolean lowDebugOn) {

        //GLES20.glFinish();

        if( fbo > 0 ) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        }

        GLES20.glUseProgram (mGLProgram);

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, MainActivity.gCameraPreviewWidth, MainActivity.gCameraPreviewHeight);

        int ph = GLES20.glGetAttribLocation(mGLProgram, "vPosition");
        int tch = GLES20.glGetAttribLocation(mGLProgram, "vTexCoord");

        GLES20.glVertexAttribPointer(ph, 2, GL_FLOAT, false, 4 * 2, mGLVertex);
        GLES20.glVertexAttribPointer(tch, 2, GL_FLOAT, false, 4 * 2, mGLTexCoord);
        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glEnableVertexAttribArray(tch);

        //rotation
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mGLProgram, "uMVPMatrix"), 1, false, PreviewRendererImpl.mMVPMatrixBuffer0);

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, exTextures);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sExTexture"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_2D, textures);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sTexture"), 1);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "uFront"), front);

        getCropFourPoints();
        GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgram, "uLeftTop"), mLeftTopX, mLeftTopY);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgram, "uRightTop"), mRightTopX, mRightTopY);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgram, "uLeftBottom"), mLeftBottomX, mLeftBottomY);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgram, "uRightBottom"), mRightBottomX, mRightBottomY);

        GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgram, "uTopVertical"), mTopVerticalX, mTopVerticalY);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(mGLProgram, "uBottomVertical"), mBottomVerticalX, mBottomVerticalY);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "uLowDebugOn"), lowDebugOn == true ? 1 : 0);

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        GLES20.glUseProgram(0);

        if( fbo > 0 ) {
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

            "uniform samplerExternalOES sExTexture;\n" +
            "uniform sampler2D sTexture;\n" +

            "varying vec2 texCoord;\n" +
            "uniform int uFront;\n" +

            "uniform vec2 uLeftTop;\n" +
            "uniform vec2 uRightTop;\n" +
            "uniform vec2 uLeftBottom;\n" +
            "uniform vec2 uRightBottom;\n" +

            "uniform vec2 uTopVertical;\n" +
            "uniform vec2 uBottomVertical;\n" +

            "uniform int uLowDebugOn;\n" +

            "float drawLine(vec2 p1, vec2 p2, vec2 uv, float a)\n" +
            "{\n" +
            "    float r = 0.0;\n" +
            "    float one_px = 1.0 / 1280.0; //not really one px\n" +

            // get dist between points
            "   float d = distance(p1, p2);\n" +

            // get dist between current pixel and p1
            "   float duv = distance(p1, uv);\n" +

            //if point is on line, according to dist, it should match current uv
            "   r = 1.0-floor(1.0-(a*one_px)+distance (mix(p1, p2, clamp(duv/d, 0.0, 1.0)), uv));\n" +

            "   return r;\n" +
            "}\n" +

            "void main() {\n" +
            "   vec2 newTexCoord = texCoord;\n" +
            "   if( uFront == 1 ) {\n" +
            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "   }\n" +

            "	if( uLowDebugOn == 0 ) {\n" +
            "	    gl_FragColor = texture2D(sExTexture, newTexCoord);\n" +

            "       float lineResult;\n" +
            "       lineResult = drawLine(uLeftTop, uRightTop, newTexCoord, 5.0);\n" +
            "       if( lineResult > 0.0 ) { \n" +
            "           gl_FragColor = vec4(lineResult, 0.0, 0.0, 0.3); \n" +
            "       } \n" +

            "       lineResult = drawLine(uLeftTop, uLeftBottom, newTexCoord, 5.0);\n" +
            "       if( lineResult > 0.0 ) { \n" +
            "           gl_FragColor = vec4(0.0, 0.0, lineResult, 0.3); \n" +
            "       } \n" +

            "       lineResult = drawLine(uRightTop, uRightBottom, newTexCoord, 5.0);\n" +
            "       if( lineResult > 0.0 ) { \n" +
            "           gl_FragColor = vec4(0.0, 0.0, lineResult, 0.3); \n" +
            "       } \n" +

            "       lineResult = drawLine(uLeftBottom, uRightBottom, newTexCoord, 5.0);\n" +
            "       if( lineResult > 0.0 ) { \n" +
            "           gl_FragColor = vec4(225.0/255.0, 241.0/255.0, 0.0/255.0, 1.0); \n" +
            "       } \n" +

            "       lineResult = drawLine(uTopVertical, uBottomVertical, newTexCoord, 5.0);\n" +
            "       if( lineResult > 0.0 ) { \n" +
            "           gl_FragColor = vec4(lineResult, 0.0, lineResult, 0.3); \n" +
            "       } \n" +

//            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
//            "	    gl_FragColor = texture2D(sTexture, newTexCoord);\n" +
            "	} \n" +
            "	else { \n" +
            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
            "	    gl_FragColor = texture2D(sTexture, newTexCoord);\n" +
            "	} \n" +
            "}";
}
