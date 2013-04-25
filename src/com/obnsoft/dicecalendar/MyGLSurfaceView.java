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
import android.view.VelocityTracker;
import android.widget.Scroller;

public class MyGLSurfaceView extends GLSurfaceView {

    protected interface OnZoomListener {
        void onZoomModeChanged(boolean isZooming);
    }

    private static final int ROTATE_NONE = 0;
    private static final int ROTATE_X = 1;
    private static final int ROTATE_Y = 2;
    private static final int ROTATE_Z = 3;

    private static final float THETA_MOVE = 1f / 16f;
    private static final float THETA_AXIS = 0.2f;
    private static final float THETA_BASE = 0.2f;
    private static final float THETA_POS = 0.5f;
    private static final float THETA_VELDEG = 1f;

    private static final float COEFFICIENT_BASE_X = 180f;
    private static final float COEFFICIENT_BASE_Y = 90;
    private static final float COEFFICIENT_POS = 4.5f;
    private static final float COEFFICIENT_ROT = 135f;
    private static final int COEFFICIENT_VEROCITY = 2;
    private static final int COEFFICIENT_FLING = 4;
    private static final int COEFFICIENT_DUR_SCRL = 10;

    private static final int GRD_INTERPOL = 1024;
    private static final int DUR_INTERPOL = 500;

    private CubesState  mState;
    private MyRenderer  mRenderer;
    private Scroller    mScroller;
    private Scroller    mInterpolator;
    private VelocityTracker mTracker;
    private OnZoomListener  mListener;

    private boolean mZoomMode;
    private boolean mMoveMode;
    private float   mTouchX;
    private float   mTouchY;
    private int     mRotateMode;
    private int     mRotateDeg;

    /*-----------------------------------------------------------------------*/

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mZoomMode = false;
        mMoveMode = false;
        mRotateMode = ROTATE_NONE;
        mScroller = new Scroller(context);
        mInterpolator = new Scroller(context);
    }

    public void setCubesState(CubesState state) {
        mState = state;
        mRenderer = new MyRenderer(getContext(), mState, false);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setOnZoomListener(OnZoomListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float w = getWidth();
        float h = getHeight();
        float s = Math.min(w, h);
        float x = (event.getX() - w / 2) / s;
        float y = (h / 2 - event.getY()) / s;

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && mTracker == null) {
            mTracker = VelocityTracker.obtain();
        }
        if (mTracker != null) {
            mTracker.addMovement(event);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                mTracker.computeCurrentVelocity(100, s * COEFFICIENT_VEROCITY);
                x = mTouchX - mTracker.getXVelocity() / s;
                y = mTouchY + mTracker.getYVelocity() / s;
                mTracker.recycle();
                mTracker = null;
            }
        }
        boolean ret = mZoomMode ? handleRotation(action, x, y) : handlePosition(action, x, y);
        if (ret) {
            requestRender();
        }
        return true;
    }

    @Override
    public void computeScroll() {
        boolean toInvalidate = false;
        if (mScroller.computeScrollOffset()) {
            if (mRotateMode != ROTATE_NONE && mState.focusCube != null) {
                float deg = mScroller.getCurrX() - mRotateDeg;
                rotateCube(mState.focusCube, mRotateMode, deg);
                mRotateDeg = mScroller.getCurrX();
                if (mScroller.isFinished()) {
                    mState.focusCube.alignPositionDegrees();
                    mRotateMode = ROTATE_NONE;
                } else {
                    toInvalidate = true;
                }
            }
        }
        if (mInterpolator.computeScrollOffset()) {
            float val = mInterpolator.getCurrX() / (float) GRD_INTERPOL;
            mRenderer.setInterpolation(val);
            toInvalidate = !mInterpolator.isFinished();
            if (!mZoomMode && val == 0f) {
                mState.focusCube = null;
                toInvalidate = false;
            }
        }
        requestRender();
        if (toInvalidate) {
            postInvalidate();
        }
    }

    public void regulate() {
        mScroller.abortAnimation();
        mInterpolator.abortAnimation();
        setZoomMode(false);
        mMoveMode = false;
        mRotateMode = ROTATE_NONE;
        mState.alignCubes();
        mState.focusCube = null;
        mRenderer.setInterpolation(0f);
    }

    /*-----------------------------------------------------------------------*/

    private boolean handlePosition(int action, float x, float y) {
        boolean ret = false;
        float pos = x * COEFFICIENT_POS;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (!mInterpolator.isFinished()) {
                mInterpolator.abortAnimation();
                mState.focusCube = null;
                mRenderer.setInterpolation(0f);
            }
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
                ret = true;
            } else {
                if (!mMoveMode && Math.abs(mTouchX - x) < THETA_MOVE) {
                    break;
                }
                mMoveMode = true;
                if (mTouchX != x) {
                    float posL, posR, move;
                    if (mTouchX < x) {
                        posL = mTouchX * COEFFICIENT_POS;
                        posR = pos + THETA_POS;
                        move = -1f;
                    } else {
                        posL = pos - THETA_POS;
                        posR = mTouchX * COEFFICIENT_POS;
                        move = 1f;
                    }
                    for (CubesState.Cube cube : mState.cubes) {
                        if (cube != mState.focusCube && posL <= cube.pos && cube.pos <= posR) {
                            cube.pos += move;
                        }
                    }
                    mState.focusCube.pos = pos;
                    ret = true;
                }
            }
            mTouchX = x;
            mTouchY = y;
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mState.focusCube != null) {
                if (mMoveMode) {
                    mState.focusCube.alignPositionDegrees();
                    mState.focusCube = null;
                    mMoveMode = false;
                } else if (action == MotionEvent.ACTION_UP) {
                    setZoomMode(true);
                    mInterpolator.startScroll(0, 0, GRD_INTERPOL, 0, DUR_INTERPOL);
                    postInvalidate();
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
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
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
            float deg = calcDegreesToRotate(mRotateMode, mTouchX, mTouchY, x, y);
            rotateCube(mState.focusCube, mRotateMode, deg);
            mTouchX = x;
            mTouchY = y;
            ret = true;
            break;
        case MotionEvent.ACTION_UP:
            if (mRotateMode == ROTATE_NONE) {
                setZoomMode(false);
                mInterpolator.startScroll(GRD_INTERPOL, 0, -GRD_INTERPOL, 0, DUR_INTERPOL);
                postInvalidate();
                ret = true;
            } else {
                float velDeg = calcDegreesToRotate(mRotateMode, mTouchX, mTouchY, x, y);
                mRotateDeg = (int) rotateCube(mState.focusCube, mRotateMode, 0f);
                if (Math.abs(velDeg) > THETA_VELDEG) {
                    mScroller.fling(mRotateDeg, 0, (int) (-velDeg * COEFFICIENT_FLING), 0,
                            Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
                    mScroller.setFinalX(Math.round(mScroller.getFinalX() / 90f) * 90);
                } else {
                    int finalDeg = Math.round(mRotateDeg / 90f) * 90;
                    int diffDeg = finalDeg - mRotateDeg;
                    int duration = Math.abs(diffDeg) * COEFFICIENT_DUR_SCRL;
                    mScroller.startScroll(mRotateDeg, 0, diffDeg, 0, duration);
                }
                postInvalidate();
                ret = true;
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            if (mRotateMode != ROTATE_NONE) {
                mState.focusCube.alignPositionDegrees();
                ret = true;
            }
            break;
        }

        return ret;
    }

    private void setZoomMode(boolean isZooming) {
        mZoomMode = isZooming;
        if (mListener != null) {
            mListener.onZoomModeChanged(isZooming);
        }
    }

    private float calcDegreesToRotate(int mode, float x1, float y1, float x2, float y2) {
        switch (mode) {
        case ROTATE_X:
            return (y1 - y2) * COEFFICIENT_ROT;
        case ROTATE_Y:
            return (x1 - x2) * COEFFICIENT_ROT;
        case ROTATE_Z:
            float deg = (float) Math.toDegrees(Math.atan2(y1, x1) - Math.atan2(y2, x2));
            if (Math.abs(deg) > 180) deg -= Math.signum(deg) * 360;
            return deg;
        }
        return 0f;
    }

    private float rotateCube(CubesState.Cube cube, int mode, float deg) {
        int angleX = Math.round(cube.degX / 90f) & 3;
        int angleY = Math.round(cube.degY / 90f) & 3;
        //int angleZ = Math.round(cube.degZ / 90f) & 3;
        float workDeg = 0f;

        switch (mode) {
        case ROTATE_X:
            cube.degX += deg;
            workDeg = cube.degX;
            break;
        case ROTATE_Y:
            if (angleX == 0) {
                cube.degY -= deg;
                workDeg = -cube.degY;
            } else if ((angleY & 1) == 0) {
                float s = 1f - (angleY & 2);
                cube.degZ += deg * s;
                workDeg = cube.degZ * s;
            } else {
                cube.degX = 0;
                cube.degY -= deg;
                cube.degZ += 90f * (1 - (angleY & 2));
                workDeg = -cube.degY;
            }
            break;
        case ROTATE_Z:
            if (angleX == 0) {
                float s = (angleY == 0) ? -1f : 1f;
                cube.degZ += deg * s;
                workDeg = cube.degZ * s;
            } else {
                cube.degY -= deg;
                workDeg = -cube.degY;
            }
            break;
        }

        return workDeg;
    }

}
