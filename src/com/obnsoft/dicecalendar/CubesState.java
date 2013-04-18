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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

public class CubesState {

    private static final String FNAME_STATE = "state.dat";
    private static final int DATA_VERSION = 1;
    private static final int FOCUS_NONE = -1;
    private static final int ROTATE_NONE = 0;
    private static final int ROTATE_X = 1;
    private static final int ROTATE_Y = 2;
    private static final int ROTATE_Z = 3;

    protected boolean mIsEach;
    protected int mFocusCube;

    protected float mBaseDegX;
    protected float mBaseDegY;
    protected float[] mCubePos = new float[4];
    protected float[] mCubeDegX = new float[4];
    protected float[] mCubeDegY = new float[4];
    protected float[] mCubeDegZ = new float[4];

    private Context mContext;
    private float mTouchX;
    private float mTouchY;
    private boolean mMoveMode;
    private int mRotateMode;
    private boolean mIsDirty;

    public CubesState(Context context) {
        mContext = context;
        loadState();

        mIsEach = false;
        mFocusCube = FOCUS_NONE;
        mMoveMode = false;
        mRotateMode = ROTATE_NONE;
    }

    public boolean onTouchEvent(int action, float x, float y) {
        Log.e("HOGE", "onTouchEvent" + action + " " + x + "," + y);
        return (mIsEach) ? rotateCube(action, x, y) : adjustCubes(action, x, y);
    }

    public boolean save() {
        if (mIsDirty) {
            mIsDirty = false;
            return saveState();
        } else {
            return true;
        }
    }

    /*-----------------------------------------------------------------------*/

    private boolean rotateCube(int action, float x, float y) {
        boolean ret = false;
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (Math.abs(x) < 0.25f) {
                mRotateMode = (Math.abs(y) < 0.25f) ? ROTATE_NONE : ROTATE_X;
            } else {
                mRotateMode = (Math.abs(y) < 0.25f) ? ROTATE_Y : ROTATE_Z;
            }
            mTouchX = x;
            mTouchY = y;
            break;
        case MotionEvent.ACTION_MOVE:
            float deg;
            switch (mRotateMode) {
            case ROTATE_X:
                mCubeDegX[mFocusCube] += (mTouchY - y) * 135f;
                mTouchY = y;
                ret = true;
                break;
            case ROTATE_Y:
                deg = (mTouchX - x) * 135f;
                switch (Math.round(mCubeDegX[mFocusCube] / 90f)) {
                case 0: mCubeDegY[mFocusCube] -= deg; break;
                case 1: mCubeDegZ[mFocusCube] += deg; break;
                case 2: mCubeDegY[mFocusCube] += deg; break;
                case 3: mCubeDegZ[mFocusCube] -= deg; break;
                }
                mTouchX = x;
                ret = true;
                break;
            case ROTATE_Z:
                deg = (mTouchX - x) * 135f;
                switch (Math.round(mCubeDegX[mFocusCube] / 90f)) {
                case 0: mCubeDegZ[mFocusCube] -= deg; break;
                case 1: mCubeDegY[mFocusCube] += deg; break;
                case 2: mCubeDegZ[mFocusCube] += deg; break;
                case 3: mCubeDegY[mFocusCube] -= deg; break;
                }
                mTouchX = x;
                ret = true;
                break;
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mRotateMode == ROTATE_NONE) {
                mIsEach = false;
            } else {
                mCubeDegX[mFocusCube] = Math.round(mCubeDegX[mFocusCube] / 90f) % 4 * 90f;
                mCubeDegY[mFocusCube] = Math.round(mCubeDegY[mFocusCube] / 90f) % 4 * 90f;
                mCubeDegZ[mFocusCube] = Math.round(mCubeDegZ[mFocusCube] / 90f) % 4 * 90f;
                mIsDirty = true;
            }
            ret = true;
            break;
        }
        return ret;
    }

    private boolean adjustCubes(int action, float x, float y) {
        boolean ret = false;
        float pos = x * 4.5f;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            for (int i = 0; i < 4; i++) {
                if (Math.abs(mCubePos[i] - pos) < 0.5f) {
                    mTouchX = x;
                    mFocusCube = i;
                    break;
                }
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (mFocusCube != FOCUS_NONE && Math.abs(mTouchX - x) > 0.0625f) {
                mMoveMode = true;
            }
            if (mMoveMode) {
                mCubePos[mFocusCube] = pos;
                for (int i = 0; i < 4; i++) {
                    float gap = mCubePos[i] - mCubePos[mFocusCube];
                    if (i != mFocusCube && Math.abs(gap) < 0.5f) {
                        mCubePos[i] += (gap > 0) ? -1f : 1f;
                    }
                }
                ret = true;
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mFocusCube != FOCUS_NONE) {
                if (mMoveMode) {
                    mCubePos[mFocusCube] = Math.round(mCubePos[mFocusCube] - 0.5f) + 0.5f;
                    mIsDirty = true;
                    mMoveMode = false;
                } else {
                    mIsEach = true;
                }
                ret = true;
            }
            break;
        }

        return ret;
    }

    private boolean loadState() {
        boolean ret = false;
        try {
            DataInputStream in =
                    new DataInputStream(mContext.openFileInput(FNAME_STATE));
            switch (in.read()) {
            case DATA_VERSION:
                mBaseDegX = in.readFloat();
                mBaseDegY = in.readFloat();
                for (int i = 0; i < 4; i++) {
                    mCubePos[i] = in.readFloat();
                    mCubeDegX[i] = in.readFloat();
                    mCubeDegY[i] = in.readFloat();
                    mCubeDegZ[i] = in.readFloat();
                }
            }
            in.close();
            ret = true;
        } catch (FileNotFoundException e) {
            mBaseDegX = mBaseDegY = 0f;
            for (int i = 0; i < 4; i++) {
                mCubePos[i] = i - 1.5f;
                mCubeDegX[i] = mCubeDegY[i] = mCubeDegZ[i] = 0f;
            }
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private boolean saveState() {
        boolean ret = false;
        try {
            DataOutputStream out =
                    new DataOutputStream(mContext.openFileOutput(FNAME_STATE, 0));
            out.write(DATA_VERSION);
            out.writeFloat(mBaseDegX);
            out.writeFloat(mBaseDegY);
            for (int i = 0; i < 4; i++) {
                out.writeFloat(mCubePos[i]);
                out.writeFloat(mCubeDegX[i]);
                out.writeFloat(mCubeDegY[i]);
                out.writeFloat(mCubeDegZ[i]);
            }
            out.close();
            ret = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

}
