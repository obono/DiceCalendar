/*
 * Copyright (C) 2013 OBN-soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.obnsoft.dicecalendar;

import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

public class PixelBuffer {

    private int     mWidth;
    private int     mHeight;
    private String  mThreadOwner;
    private Bitmap  mBitmap;

    private EGL10       mEGL; 
    private EGLDisplay  mEGLDisplay;
    private EGLConfig   mEGLConfig;
    private EGLContext  mEGLContext;
    private EGLSurface  mEGLSurface;
    private GL10        mGL;

    private GLSurfaceView.Renderer mRenderer;

    public PixelBuffer(int width, int height) {
        mWidth = width;
        mHeight = height;
        mThreadOwner = Thread.currentThread().getName();

        int[] version = new int[2];
        int[] attribList = {
            EGL10.EGL_WIDTH, mWidth,
            EGL10.EGL_HEIGHT, mHeight,
            EGL10.EGL_NONE
        };

        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (!mEGL.eglInitialize(mEGLDisplay, version)) {
            return;
        }
        mEGLConfig = chooseConfig(); // Choosing a config is a little more complicated
        if (mEGLConfig == null) {
            return;
        }
        mEGLContext = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig, EGL10.EGL_NO_CONTEXT, null);
        if (mEGLContext == EGL10.EGL_NO_CONTEXT) {
            return;
        }
        mEGLSurface = mEGL.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, attribList);
        if (mEGLSurface == EGL10.EGL_NO_SURFACE) {
            return;
        }
        if (!mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            return;
        }
        mGL = (GL10) mEGLContext.getGL();
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
    }

    public void setRenderer(GLSurfaceView.Renderer renderer) {
        if (mGL != null && checkThread()) {
            mRenderer = renderer;
            mRenderer.onSurfaceCreated(mGL, mEGLConfig);
            mRenderer.onSurfaceChanged(mGL, mWidth, mHeight);
        }
    }

    public Bitmap getBitmap() {
        if (mRenderer != null && checkThread()) {
            mRenderer.onDrawFrame(mGL);
            convertToBitmap(mGL, mBitmap);
        }
        return mBitmap;
    }

    public void finish() {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (mEGLSurface != null) {
            mEGL.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEGL.eglDestroySurface(mEGLDisplay, mEGLSurface);
            mEGLSurface = null;
        }
        if (mEGLContext != null) {
            mEGL.eglDestroyContext(mEGLDisplay, mEGLContext);
            mEGLContext = null;
            mGL = null;
        }
        if (mEGLDisplay != null) {
            mEGL.eglTerminate(mEGLDisplay);
            mEGLDisplay = null;
        }

    }

    /*-----------------------------------------------------------------------*/

    private boolean checkThread() {
        return Thread.currentThread().getName().equals(mThreadOwner);
    }

    private EGLConfig chooseConfig() {
        int[] attribList = {
            EGL10.EGL_DEPTH_SIZE,   24,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_RED_SIZE,     8,
            EGL10.EGL_GREEN_SIZE,   8,
            EGL10.EGL_BLUE_SIZE,    8,
            EGL10.EGL_ALPHA_SIZE,   8,
            EGL10.EGL_NONE
        };

        // Expand on this logic to be more selective in choosing a configuration
        int[] numConfig = new int[1];
        mEGL.eglChooseConfig(mEGLDisplay, attribList, null, 0, numConfig);
        int configSize = numConfig[0];
        if (configSize > 0) {
            EGLConfig[] configs = new EGLConfig[configSize];
            if (mEGL.eglChooseConfig(mEGLDisplay, attribList, configs, configSize, numConfig)) {
                return configs[0]; // Best match is probably the first configuration
            }
        }
        return null;
    }

    private void convertToBitmap(GL10 gl, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        /*  Capture  */
        IntBuffer buf = IntBuffer.allocate(mWidth * mHeight);
        gl.glReadPixels(0, 0, mWidth, mHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buf);

        /*  Convert upside down  */
        int[] swapArray = new int[mWidth];
        int[] array = buf.array();
        for (int i = 0; i < mHeight / 2; i++) {
            System.arraycopy(array, i * mWidth, swapArray, 0, mWidth);
            System.arraycopy(array, (mHeight - i - 1) * mWidth, array, i * mWidth, mWidth);
            System.arraycopy(swapArray, 0, array, (mHeight - i - 1) * mWidth, mWidth);
        }
        bitmap.copyPixelsFromBuffer(buf);
    }

}
