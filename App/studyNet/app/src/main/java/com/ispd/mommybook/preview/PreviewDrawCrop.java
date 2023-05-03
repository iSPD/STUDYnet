package com.ispd.mommybook.preview;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.ispd.mommybook.JniController;
import com.ispd.mommybook.utils.UtilsLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;

public class PreviewDrawCrop {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;

    private int mGLProgram = -1;
    private FloatBuffer mGLVertex = null, mGLTexCoord = null;

    private float []mCropMatrix = new float[9];

    /**
     * Create Shader for Preview
     *
     * @param width
     * @param height
     */
    public PreviewDrawCrop(int width, int height) {

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

    private void getCropMatrix() {
        JniController.getCropMatrixValue(mCropMatrix, mCameraPreviewWidth, mCameraPreviewHeight);
    }

    public void DrawPreview(int fbo, int exTextures, int textures, int front)
    {
        //GLES20.glFinish();

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

        getCropMatrix();
        GLES20.glUniformMatrix3fv(GLES20.glGetUniformLocation(mGLProgram, "inverseHomographyMatrix"), 1, true, mCropMatrix, 0);

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, exTextures);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sTexture0"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL_TEXTURE_2D, textures);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "sTexture1"), 1);

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mGLProgram, "uFront"), front);

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(ph);
        GLES20.glDisableVertexAttribArray(tch);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
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
            //"precision mediump float;\n" +
            "precision highp float;\n" +

            "uniform samplerExternalOES sTexture0;\n" +
            "uniform sampler2D sTexture1;\n" +

            "varying vec2 texCoord;\n" +
            "uniform int uFront;\n" +
            "uniform mat3 inverseHomographyMatrix;\n" +

            "void main() {\n" +

            "   vec2 newTexCoord = texCoord;\n" +
            "   if( uFront == 1 ) {\n" +
            "       newTexCoord.x = 1.0 - newTexCoord.x;\n" +
//            "       newTexCoord.y = 1.0 - newTexCoord.y;\n" +
            "   }\n" +

            "   float width = 1280.0; \n" +
            "   float height = 960.0; \n" +

            "   vec3 frameCoordinate = vec3(newTexCoord.x * width, newTexCoord.y * height, 1.0); \n" +
            "   vec3 trans = inverseHomographyMatrix * frameCoordinate; \n" +
            "   vec2 coords = vec2(trans.x / width, trans.y / height) / trans.z;  \n" +

            "   if (coords.x >= 0.0 && coords.x <= 1.0 && coords.y >= 0.0 && coords.y <= 1.0) { \n" +
            "       gl_FragColor = texture2D(sTexture0, coords); \n" +
            "   } else { \n" +
            "       gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0); \n" +
            "   } \n" +
            "}";
}