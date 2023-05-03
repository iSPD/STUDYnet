package com.ispd.mommybook.preview;

import android.graphics.Bitmap;
import android.opengl.GLES10;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.ispd.mommybook.MainActivity;
import com.ispd.mommybook.utils.UtilsLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static android.opengl.GLES20.*;

/**
 * PreviewRendererImpl
 *
 * @author Daniel
 * @version 1.0
 */
public class PreviewRendererImpl {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    public static FloatBuffer mMVPMatrixBuffer0;
    public static FloatBuffer mMVPMatrixBuffer90;

    private static ByteBuffer mPixelBufferForDebug;

    /**
     * Create rotation buffer for preview
     *
     */
    public static void setRotationBuffer() {
        float []mMVPMatrix0 = new float[16];

        Matrix.setIdentityM(mMVPMatrix0, 0);

        mMVPMatrixBuffer0 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBuffer0.put(mMVPMatrix0);
        mMVPMatrixBuffer0.position(0);

        float []mModelMatrix90 = new float[16];
        float []mRotationMatrix90 = new float[16];
        float []mMVPMatrix90 = new float[16];

        Matrix.setIdentityM(mModelMatrix90, 0);
        Matrix.setRotateM(mRotationMatrix90, 0, 90, 0, 0, -1.0f);
        Matrix.multiplyMM(mMVPMatrix90, 0, mModelMatrix90, 0, mRotationMatrix90, 0);

        mMVPMatrixBuffer90 = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mMVPMatrixBuffer90.put(mMVPMatrix90);
        mMVPMatrixBuffer90.position(0);
    }

    /**
     * Create shader program
     *
     * @param strVShader
     * @param strFShader
     */
    public static int setProgram(String strVShader, String strFShader) {
        int vshader = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER);
        GLES31.glShaderSource(vshader, strVShader);
        GLES31.glCompileShader(vshader);
        int[] compiled = new int[1];
        GLES31.glGetShaderiv(vshader, GLES31.GL_COMPILE_STATUS, compiled, 0);
        if( compiled[0] == 0 ) {
            LOGGER.e("Could not compile vshader");
            LOGGER.e("Could not compile vshader:" + GLES31.glGetShaderInfoLog(vshader));
            GLES31.glDeleteShader(vshader);
            vshader = 0;
            return -1;
        }

        int fshader = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER);
        GLES31.glShaderSource(fshader, strFShader);
        GLES31.glCompileShader(fshader);
        GLES31.glGetShaderiv(fshader, GLES31.GL_COMPILE_STATUS, compiled, 0);
        if( compiled[0] == 0 ) {
            LOGGER.e("Could not compile fshader");
            LOGGER.e("Could not compile fshader:" + GLES31.glGetShaderInfoLog(fshader));
            GLES31.glDeleteShader(fshader);
            fshader = 0;
            return -1;
        }

        int program = GLES31.glCreateProgram();
        GLES31.glAttachShader(program, vshader);
        GLES31.glAttachShader(program, fshader);
        GLES31.glLinkProgram(program);

        return program;
    }

    /**
     * Create external texture
     *
     */
    public static int createExternalTexture() {
        int texture_name[] = new int[1];

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glGenTextures(1, texture_name, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture_name[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_S, GLES10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_WRAP_T, GLES10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        LOGGER.d("createExternalTexture : "+texture_name[0]);

        return texture_name[0];
    }

    /**
     * Create texture
     *
     * @param width
     * @param height
     * @param depth
     */
    public static int createTexture(int width, int height, int depth) {
        int name[] = new int[1];
        GLES20.glGenTextures(1, name, 0);

        GLES20.glBindTexture(GL_TEXTURE_2D, name[0]);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        if ( depth == 32 )
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        else if ( depth == 24 )
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, null);
        else if ( depth == 8 )
        {
            GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, width, height, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, null);
        }

        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        return name[0];
    }

    /**
     * Create bitmap texture
     *
     * @param width
     * @param height
     * @param bitmap
     */
    public static int createTextureBitmap(int width, int height, Bitmap bitmap) {
        int name[] = new int[1];
        GLES20.glGenTextures(1, name, 0);

        GLES20.glBindTexture(GL_TEXTURE_2D, name[0]);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_NEAREST);
//        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_NEAREST);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

//        bitmap.recycle(); //sally commented
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        return name[0];
    }

    /**
     * Update bitmap texture
     *
     * @param textureID
     * @param bitmap
     */
    public static void updateTextureBitmap(int textureID, Bitmap bitmap) {
        GLES20.glBindTexture(GL_TEXTURE_2D, textureID);

        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

//        bitmap.recycle(); //sally commented
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Create luminance texture
     *
     * @param width
     * @param height
     */
    public static int createLuminanceTexture(int width, int height) {

        int textureid[] = new int[1];
        GLES20.glGenTextures(1, textureid, 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureid[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return textureid[0];
    }

    /**
     * Update luminance texture
     *
     * @param texture
     * @param width
     * @param height
     * @param buffer
     */
    public static void updateLuminanceTexture(int texture, int width, int height, IntBuffer buffer) {

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D,
                0,0, 0, width, height,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * Create framebuffer object
     *
     * @param texture
     */
    public static int createFBO(int texture) {
        int status = 0;
        int fbo[] = new int[1];
        GLES20.glGenFramebuffers(1, fbo, 0);

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, fbo[0]);
        GLES20.glFramebufferTexture2D(GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        // Check FBO status.
        status = GLES20.glCheckFramebufferStatus(GL_FRAMEBUFFER);

        if ( status != GL_FRAMEBUFFER_COMPLETE ) {
            LOGGER.d("fail to create FBO\n");
            return 0;
        }

        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        return fbo[0];
    }

    public static void copyDebugData(byte[] image)
    {
        int width = MainActivity.gCameraPreviewWidth;
        int height = MainActivity.gCameraPreviewHeight;

        if( mPixelBufferForDebug == null ) {
            byte[] initByte = new byte[width * height * 4];
            Arrays.fill(initByte, (byte) 0xff);

            mPixelBufferForDebug = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
            mPixelBufferForDebug.put(initByte, 0, width * height * 4);
            mPixelBufferForDebug.position(0);
        }

        if( mPixelBufferForDebug != null ) {
            mPixelBufferForDebug.put(image, 0, width * height * 4);
            mPixelBufferForDebug.position(0);
        }
    }

    public static void updateTextureForDebug(int []texID) {

        int width = MainActivity.gCameraPreviewWidth;
        int height = MainActivity.gCameraPreviewHeight;

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_2D, texID[0]);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, mPixelBufferForDebug);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
    }
}
