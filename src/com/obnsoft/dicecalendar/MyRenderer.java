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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.opengl.GLUtils;

public class MyRenderer implements Renderer {

    private Context mContext;
    private MyCube mCube;
    private float mDegX;
    private float mDegY;
    private float mDegZ;

    public MyRenderer(Context context) {
        mContext = context;
        mCube = new MyCube();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_LIGHT0);
        gl.glEnable(GL10.GL_TEXTURE_2D);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 45f, (float) width / (float) height, 1f, 50f);

        int[] buffers = new int[1];
        gl.glGenTextures(1, buffers, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, buffers[0]);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.texture);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
        bitmap.recycle();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -3f);
        gl.glRotatef(mDegX, 1, 0, 0);
        gl.glRotatef(mDegY, 0, 1, 0);
        gl.glRotatef(mDegZ, 0, 0, 1);
        mCube.draw(gl);
    }

    /*-----------------------------------------------------------------------*/

    public void setRotation(float degX, float degY, float degZ) {
        mDegX = degX;
        mDegY = degY;
        mDegZ = degZ;
    }

    /*-----------------------------------------------------------------------*/

    class MyCube {

        private static final float T0 = 52f / 512f;
        private static final float T1 = 154f / 512f;
        private static final float T2 = 256f / 512f;
        private static final float T3 = 358f / 512f;
        private static final float T4 = 460f / 512f;
        private static final float TZ = 50f / 512f;

        private final FloatBuffer mVertexBuffer;
        private final FloatBuffer mTextureBuffer;

        public MyCube() {
            float vertices[] = {
                // front
                -0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                // back
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                -0.5f, 0.5f, -0.5f,
                0.5f, 0.5f, -0.5f,
                // left
                -0.5f, -0.5f, 0.5f,
                -0.5f, -0.5f, -0.5f,
                -0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f,
                // right
                0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, -0.5f,
                // top
                -0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f,
                0.5f, 0.5f, -0.5f,
                // bottom
                -0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f
            };
            ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
            vbb.order(ByteOrder.nativeOrder());
            mVertexBuffer = vbb.asFloatBuffer();
            mVertexBuffer.put(vertices);
            mVertexBuffer.position(0);

            float[] uv = {
                    T0-TZ, T0-TZ,   T0-TZ, T0+TZ,   T0+TZ, T0-TZ,   T0+TZ, T0+TZ,
                    T1-TZ, T0-TZ,   T1-TZ, T0+TZ,   T1+TZ, T0-TZ,   T1+TZ, T0+TZ,
                    T2-TZ, T0-TZ,   T2-TZ, T0+TZ,   T2+TZ, T0-TZ,   T2+TZ, T0+TZ,
                    T0-TZ, T1-TZ,   T0-TZ, T1+TZ,   T0+TZ, T1-TZ,   T0+TZ, T1+TZ,
                    T1-TZ, T1-TZ,   T1-TZ, T1+TZ,   T1+TZ, T1-TZ,   T1+TZ, T1+TZ,
                    T2-TZ, T1-TZ,   T2-TZ, T1+TZ,   T2+TZ, T1-TZ,   T2+TZ, T1+TZ,
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(uv.length * 4);
            bb.order(ByteOrder.nativeOrder());
            mTextureBuffer = bb.asFloatBuffer();
            mTextureBuffer.put(uv);
            mTextureBuffer.position(0);
        }

        public void draw(GL10 gl){
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);
            // Front
            gl.glNormal3f(0, 0, 1.0f);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
            // Back
            gl.glNormal3f(0, 0, -1.0f);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 4, 4);
            // Left
            gl.glNormal3f(-1.0f, 0, 0);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 8, 4);
            // Right
            gl.glNormal3f(1.0f, 0, 0);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 12, 4);
            // Top
            gl.glNormal3f(0, 1.0f, 0);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 16, 4);
            // Right
            gl.glNormal3f(0, -1.0f, 0);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 20, 4);
        }
    }

}
