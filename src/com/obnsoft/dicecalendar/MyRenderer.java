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

    private static final float VN = -0.5f;
    private static final float VP = 0.5f;
    private static final float[] VERTICES = {
        VN,VP,VP,   VP,VP,VP,   VN,VN,VP,   VP,VN,VP,   // front
        VP,VP,VP,   VP,VP,VN,   VP,VN,VP,   VP,VN,VN,   // right
        VP,VP,VN,   VN,VP,VN,   VP,VN,VN,   VN,VN,VN,   // back
        VP,VN,VN,   VN,VN,VN,   VP,VN,VP,   VN,VN,VP,   // bottom
        VN,VN,VN,   VN,VP,VN,   VN,VN,VP,   VN,VP,VP,   // left
        VN,VP,VN,   VP,VP,VN,   VN,VP,VP,   VP,VP,VP,   // top
    };
    private static final float[] NORMALS = {
        0f,0f,1f,   0f,0f,1f,   0f,0f,1f,   0f,0f,1f,   // front
        1f,0f,0f,   1f,0f,0f,   1f,0f,0f,   1f,0f,0f,   // right
        0f,0f,-1f,  0f,0f,-1f,  0f,0f,-1f,  0f,0f,-1f,  // back
        0f,-1f,0f,  0f,-1f,0f,  0f,-1f,0f,  0f,-1f,0f,  // bottom
        -1f,0f,0f,  -1f,0f,0f,  -1f,0f,0f,  -1f,0f,0f,  // left
        0f,1f,0f,   0f,1f,0f,   0f,1f,0f,   0f,1f,0f,   // top
    };
    private static final float T0 = 52f / 512f;
    private static final float T1 = 154f / 512f;
    private static final float T2 = 256f / 512f;
    private static final float T3 = 358f / 512f;
    private static final float T4 = 460f / 512f;
    private static final float TZ = 50f / 512f;
    private static final float[] TEXCOORDS = {
        T0-TZ, T4+TZ,   T0-TZ, T4-TZ,   T0+TZ, T4+TZ,   T0+TZ, T4-TZ, // cube0 Jan/Jul
        T0-TZ, T3+TZ,   T0-TZ, T3-TZ,   T0+TZ, T3+TZ,   T0+TZ, T3-TZ, // cube0 Feb/Aug
        T0-TZ, T2+TZ,   T0-TZ, T2-TZ,   T0+TZ, T2+TZ,   T0+TZ, T2-TZ, // cube0 Mar/Sep
        T1-TZ, T4+TZ,   T1-TZ, T4-TZ,   T1+TZ, T4+TZ,   T1+TZ, T4-TZ, // cube0 Apr/Oct
        T1-TZ, T3+TZ,   T1-TZ, T3-TZ,   T1+TZ, T3+TZ,   T1+TZ, T3-TZ, // cube0 May/Nov
        T1-TZ, T2+TZ,   T1-TZ, T2-TZ,   T1+TZ, T2+TZ,   T1+TZ, T2-TZ, // cube0 Jun/Dec
        T0-TZ, T0-TZ,   T0+TZ, T0-TZ,   T0-TZ, T0+TZ,   T0+TZ, T0+TZ, // cube1 0
        T1-TZ, T0-TZ,   T1+TZ, T0-TZ,   T1-TZ, T0+TZ,   T1+TZ, T0+TZ, // cube1 1
        T2-TZ, T0-TZ,   T2+TZ, T0-TZ,   T2-TZ, T0+TZ,   T2+TZ, T0+TZ, // cube1 2
        T0-TZ, T1-TZ,   T0+TZ, T1-TZ,   T0-TZ, T1+TZ,   T0+TZ, T1+TZ, // cube1 3
        T1-TZ, T1-TZ,   T1+TZ, T1-TZ,   T1-TZ, T1+TZ,   T1+TZ, T1+TZ, // cube1 4
        T2-TZ, T1-TZ,   T2+TZ, T1-TZ,   T2-TZ, T1+TZ,   T2+TZ, T1+TZ, // cube1 5
        T4+TZ, T4+TZ,   T4-TZ, T4+TZ,   T4+TZ, T4-TZ,   T4-TZ, T4-TZ, // cube2 0
        T3+TZ, T4+TZ,   T3-TZ, T4+TZ,   T3+TZ, T4-TZ,   T3-TZ, T4-TZ, // cube2 1
        T2+TZ, T4+TZ,   T2-TZ, T4+TZ,   T2+TZ, T4-TZ,   T2-TZ, T4-TZ, // cube2 2
        T4+TZ, T3+TZ,   T4-TZ, T3+TZ,   T4+TZ, T3-TZ,   T4-TZ, T3-TZ, // cube2 6
        T3+TZ, T3+TZ,   T3-TZ, T3+TZ,   T3+TZ, T3-TZ,   T3-TZ, T3-TZ, // cube2 7
        T2+TZ, T3+TZ,   T2-TZ, T3+TZ,   T2+TZ, T3-TZ,   T2-TZ, T3-TZ, // cube2 8
        T4+TZ, T0-TZ,   T4+TZ, T0+TZ,   T4-TZ, T0-TZ,   T4-TZ, T0+TZ, // cube3 Mon
        T4+TZ, T1-TZ,   T4+TZ, T1+TZ,   T4-TZ, T1-TZ,   T4-TZ, T1+TZ, // cube3 Tue
        T4+TZ, T2-TZ,   T4+TZ, T2+TZ,   T4-TZ, T2-TZ,   T4-TZ, T2+TZ, // cube3 Wed
        T3+TZ, T0-TZ,   T3+TZ, T0+TZ,   T3-TZ, T0-TZ,   T3-TZ, T0+TZ, // cube3 Thu
        T3+TZ, T1-TZ,   T3+TZ, T1+TZ,   T3-TZ, T1-TZ,   T3-TZ, T1+TZ, // cube3 Fri
        T3+TZ, T2-TZ,   T3+TZ, T2+TZ,   T3-TZ, T2-TZ,   T3-TZ, T2+TZ, // cube3 Sat/Sun
    };

    private final Context mContext;
    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mTexCoordBuffer;
    private final FloatBuffer mNormalBuffer;

    private float mDegX;
    private float mDegY;
    private float mDegZ;

    /*-----------------------------------------------------------------------*/

    public MyRenderer(Context context) {
        mContext = context;
        mVertexBuffer = getFloatBufferFromArray(VERTICES);
        mNormalBuffer = getFloatBufferFromArray(NORMALS);
        mTexCoordBuffer = getFloatBufferFromArray(TEXCOORDS);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glEnable(GL10.GL_TEXTURE_2D);
        int[] buffers = new int[1];
        gl.glGenTextures(1, buffers, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, buffers[0]);
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.texture);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        bitmap.recycle();

        gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_LIGHT0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 45f, (float) width / (float) height, 1f, 50f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
 
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glPushMatrix();
        gl.glTranslatef(0f, 0f, -3f);
        rotateXYZ(gl, mDegX, mDegY, mDegZ);
        for (int i = 0; i < 4; i++) {
            gl.glPushMatrix();
            gl.glTranslatef(i - 1.5f, 0f, 0f);
            rotateXYZ(gl, 0, 0, 0);
            drawCube(gl, i);
            gl.glPopMatrix();
        }
        gl.glPopMatrix();

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }

    /*-----------------------------------------------------------------------*/

    public void setRotation(float degX, float degY, float degZ) {
        mDegX = degX;
        mDegY = degY;
        mDegZ = degZ;
    }

    /*-----------------------------------------------------------------------*/

    private FloatBuffer getFloatBufferFromArray(float[] array) {
        FloatBuffer ret;
        ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
        bb.order(ByteOrder.nativeOrder());
        ret = bb.asFloatBuffer();
        ret.put(array);
        ret.position(0);
        return ret;
    }

    private void rotateXYZ(GL10 gl, float degX, float degY, float degZ) {
        gl.glRotatef(degX, 1, 0, 0);
        gl.glRotatef(degY, 0, 1, 0);
        gl.glRotatef(degZ, 0, 0, 1);
    }

    private void drawCube(GL10 gl, int type) {
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
        gl.glNormalPointer(GL10.GL_FLOAT, 0, mNormalBuffer);
        mTexCoordBuffer.position(type * 48);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexCoordBuffer);
        for (int i = 0; i < 6; i++) {
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, i * 4, 4);
        }
    }
}
