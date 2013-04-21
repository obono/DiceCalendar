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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {

    private static final int ROTATE_NONE = 0;
    private static final int ROTATE_X = 1;
    private static final int ROTATE_Y = 2;
    private static final int ROTATE_Z = 3;

    private static final float THETA_MOVE = 1f / 16f;
    private static final float THETA_AXIS = 0.2f;
    private static final float THETA_BASE = 0.2f;
    private static final float THETA_POS = 0.5f;

    private static final float COEFFICIENT_BASE_X = 180f;
    private static final float COEFFICIENT_BASE_Y = 90;
    private static final float COEFFICIENT_POS = 4.5f;
    private static final float COEFFICIENT_ROT = 135f;

    private CubesState  mState;
    private MyRenderer  mRenderer;

    private float   mTouchX;
    private float   mTouchY;
    private boolean mMoveMode;
    private int     mRotateMode;

    /*-----------------------------------------------------------------------*/

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMoveMode = false;
        mRotateMode = ROTATE_NONE;
    }

    public void setCubesState(CubesState state) {
        mState = state;
        mRenderer = new MyRenderer(getContext(), mState, false);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float w = getWidth();
        float h = getHeight();
        float s = Math.min(w, h);
        float x = (event.getX() - w / 2) / s;
        float y = (h / 2 - event.getY()) / s;

        int action = event.getAction();
        boolean ret = (mState.isZooming) ?
                handleRotation(action, x, y) : handlePosition(action, x, y);
        if (ret) {
            requestRender();
        }
        return true;
    }

    /*-----------------------------------------------------------------------*/

    private boolean handlePosition(int action, float x, float y) {
        boolean ret = false;
        float pos = x * COEFFICIENT_POS;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (Math.abs(y) <= THETA_BASE) {
                for (CubesState.Cube cube : mState.cubes) {
                    if (Math.abs(cube.pos - pos) <= THETA_POS) {
                        mState.focusCube = cube;
                        mMoveMode = false;
                        ret = true;
                        break;
                    }
                }
            }
            mTouchX = x;
            mTouchY = y;
            break;
        case MotionEvent.ACTION_MOVE:
            if (mState.focusCube == null) {
                mState.addBaseDegree((mTouchY - y) * COEFFICIENT_BASE_X,
                        (x - mTouchX) * COEFFICIENT_BASE_Y);
                mTouchX = x;
                mTouchY = y;
                ret = true;
            } else {
                if (!mMoveMode && Math.abs(mTouchX - x) < THETA_MOVE) {
                    break;
                }
                mMoveMode = true;
                mState.focusCube.pos = pos;
                for (CubesState.Cube cube : mState.cubes) {
                    float gap = cube.pos - mState.focusCube.pos;
                    if (cube != mState.focusCube && Math.abs(gap) <= THETA_POS) {
                        float tmp = cube.pos;
                        cube.pos = mTouchX * COEFFICIENT_POS;
                        cube.alignPositionDegrees();
                        mTouchX = tmp / COEFFICIENT_POS;
                    }
                }
                ret = true;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mState.focusCube != null) {
                if (mMoveMode) {
                    mState.focusCube.alignPositionDegrees();
                    mState.focusCube = null;
                    mMoveMode = false;
                } else if (action == MotionEvent.ACTION_UP) {
                    mState.isZooming = true;
                } else {
                    mState.focusCube = null;
                }
                ret = true;
            }
            break;
        }

        return ret;
    }

    private boolean handleRotation(int action, float x, float y) {
        boolean ret = false;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mRotateMode = ROTATE_NONE;
            mTouchX = x;
            mTouchY = y;
            break;
        case MotionEvent.ACTION_MOVE:
            if (mRotateMode == ROTATE_NONE) {
                float diffX = Math.abs(mTouchX - x);
                float diffY = Math.abs(mTouchY - y);
                if (diffX < THETA_MOVE && diffY < THETA_MOVE) {
                    break;
                }
                boolean judge1 = (diffX < diffY);
                boolean judge2 = (Math.abs(y) < THETA_AXIS);
                if (Math.abs(x) < THETA_AXIS) {
                    mRotateMode = judge1 ? ROTATE_X : (judge2 ? ROTATE_Y : ROTATE_Z);
                } else {
                    mRotateMode = judge2 ? (judge1 ? ROTATE_Z : ROTATE_Y) : ROTATE_Z;
                }
            }
            float deg = 0f;
            switch (mRotateMode) {
            case ROTATE_X:
                deg = (mTouchY - y) * COEFFICIENT_ROT;
                break;
            case ROTATE_Y:
                deg = (mTouchX - x) * COEFFICIENT_ROT;
                break;
            case ROTATE_Z:
                deg = (float) Math.toDegrees(Math.atan2(mTouchY, mTouchX) - Math.atan2(y, x));
                break;
            }
            rotateCube(mState.focusCube, mRotateMode, deg);
            mTouchX = x;
            mTouchY = y;
            ret = true;
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mRotateMode == ROTATE_NONE) {
                if (action == MotionEvent.ACTION_UP) {
                    mState.isZooming = false;
                    mState.focusCube = null;
                    ret = true;
                }
            } else {
                mState.focusCube.alignPositionDegrees();
                ret = true;
            }
            break;
        }

        return ret;
    }

    private void rotateCube(CubesState.Cube cube, int mode, float deg) {
        int angleX = Math.round(cube.degX / 90f) & 3;
        int angleY = Math.round(cube.degY / 90f) & 3;
        //int angleZ = Math.round(cube.degZ / 90f) & 3;

        switch (mode) {
        case ROTATE_X:
            cube.degX += deg;
            break;
        case ROTATE_Y:
            if (angleX == 0) {
                cube.degY -= deg;
            } else if ((angleY & 1) == 0) {
                cube.degZ += deg * (1 - (angleY & 2));
            } else {
                cube.degX = 0;
                cube.degY -= deg;
                cube.degZ += 90f * (1 - (angleY & 2));
            }
            break;
        case ROTATE_Z:
            if (angleX == 0) {
                cube.degZ += (angleY == 0) ? -deg : deg;
            } else {
                cube.degY -= deg;
            }
            break;
        }
    }

}
