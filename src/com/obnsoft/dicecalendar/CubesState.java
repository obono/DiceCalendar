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
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.content.Context;
import android.view.MotionEvent;

public class CubesState {

    public static final int FOCUS_NONE = -1;

    private static final String FNAME_STATE = "state.dat";
    private static final int DATA_VERSION = 1;

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

    private static final float MAX_BASE_DEGX = 20f;
    private static final float MAX_BASE_DEGY = 30f;

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
        return (mIsEach) ? handleRotation(action, x, y) : handlePosition(action, x, y);
    }

    public boolean resetBaseRotation() {
        mBaseDegX = 0f;
        mBaseDegY = 0f;
        mIsDirty = true;
        return true;
    }

    public boolean arrangeToday() {
        arrangeByDate(new GregorianCalendar());
        mIsDirty = true;
        return true;
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

    private boolean handlePosition(int action, float x, float y) {
        boolean ret = false;
        float pos = x * COEFFICIENT_POS;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (Math.abs(y) <= THETA_BASE) {
                for (int i = 0; i < 4; i++) {
                    if (Math.abs(mCubePos[i] - pos) <= THETA_POS) {
                        mFocusCube = i;
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
            if (mFocusCube == FOCUS_NONE) {
                mBaseDegX += (mTouchY - y) * COEFFICIENT_BASE_X;
                mBaseDegY += (x - mTouchX) * COEFFICIENT_BASE_Y;
                if (mBaseDegX < -MAX_BASE_DEGX) mBaseDegX = -MAX_BASE_DEGX;
                if (mBaseDegX > MAX_BASE_DEGX)  mBaseDegX = MAX_BASE_DEGX;
                if (mBaseDegY < -MAX_BASE_DEGY) mBaseDegY = -MAX_BASE_DEGY;
                if (mBaseDegY > MAX_BASE_DEGY)  mBaseDegY = MAX_BASE_DEGY;
                mTouchX = x;
                mTouchY = y;
                ret = true;
            } else {
                if (!mMoveMode && Math.abs(mTouchX - x) < THETA_MOVE) {
                    break;
                }
                mMoveMode = true;
                mCubePos[mFocusCube] = pos;
                for (int i = 0; i < 4; i++) {
                    float gap = mCubePos[i] - mCubePos[mFocusCube];
                    if (i != mFocusCube && Math.abs(gap) <= THETA_POS) {
                        float tmp = mCubePos[i];
                        mCubePos[i] = mTouchX * COEFFICIENT_POS;
                        alignCube(i);
                        mTouchX = tmp / COEFFICIENT_POS;
                    }
                }
                ret = true;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mFocusCube == FOCUS_NONE) {
                mIsDirty = true;
            } else {
                if (mMoveMode) {
                    alignCube(mFocusCube);
                    mFocusCube = FOCUS_NONE;
                    mMoveMode = false;
                } else if (action == MotionEvent.ACTION_UP) {
                    mIsEach = true;
                } else {
                    mFocusCube = FOCUS_NONE;
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
            rotateCube(mFocusCube, mRotateMode, deg);
            mTouchX = x;
            mTouchY = y;
            ret = true;
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mRotateMode == ROTATE_NONE) {
                if (action == MotionEvent.ACTION_UP) {
                    mIsEach = false;
                    mFocusCube = FOCUS_NONE;
                    ret = true;
                }
            } else {
                alignCube(mFocusCube);
                mIsDirty = true;
                ret = true;
            }
            break;
        }

        return ret;
    }

    private void rotateCube(int type, int mode, float deg) {
        int angleX = Math.round(mCubeDegX[type] / 90f) & 3;
        int angleY = Math.round(mCubeDegY[type] / 90f) & 3;
        //int angleZ = Math.round(mCubeDegZ[type] / 90f) & 3;

        switch (mode) {
        case ROTATE_X:
            mCubeDegX[type] += deg;
            break;
        case ROTATE_Y:
            if (angleX == 0) {
                mCubeDegY[type] -= deg;
            } else if ((angleY & 1) == 0) {
                mCubeDegZ[type] += deg * (1 - (angleY & 2));
            } else {
                mCubeDegX[type] = 0;
                mCubeDegY[type] -= deg;
                mCubeDegZ[type] += 90f * (1 - (angleY & 2));
            }
            break;
        case ROTATE_Z:
            if (angleX == 0) {
                mCubeDegZ[type] += (angleY == 0) ? -deg : deg;
            } else {
                mCubeDegY[type] -= deg;
            }
            break;
        }
    }

    private void alignCube(int type) {
        mCubePos[type] = Math.round(mCubePos[type] - 0.5f) + 0.5f;
        if (mCubePos[type] < -1.5f) mCubePos[type] = -1.5f;
        if (mCubePos[type] > 1.5f)  mCubePos[type] = 1.5f;

        int angleX = Math.round(mCubeDegX[type] / 90f) & 3;
        int angleY = Math.round(mCubeDegY[type] / 90f) & 3;
        int angleZ = Math.round(mCubeDegZ[type] / 90f) & 3;
        if ((angleX & 2) != 0) {
            if ((angleY & 1) == 0) {
                angleX ^= 2;
                angleY ^= 2;
                angleZ ^= 2;
            } else {
                angleZ += (angleX - 1) * (2 - angleY);
                angleX = 1;
            }
        }
        if (angleX == 0 && (angleY & 1) != 0) {
            angleZ += angleY - 2;
            angleX = 1;
        }
        mCubeDegX[type] = (angleX & 3) * 90f;
        mCubeDegY[type] = (angleY & 3) * 90f;
        mCubeDegZ[type] = (angleZ & 3) * 90f;
    }

    private void arrangeByDate(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int wday = calendar.get(Calendar.DAY_OF_WEEK);
        calcDegreesByFaceAngle(0, month % 6, (month < 6) ? 0 : 2);
        calcDegreesByFaceAngle(3, (wday == Calendar.SUNDAY) ? 5 : wday - 2,
                (wday == Calendar.SUNDAY) ? 2 : 0);
        int ten, one;
        if (day % 10 <= 5) {
            ten = (day % 10 >= 3 || day >= 30 || mCubePos[1] > mCubePos[2]) ? 2 : 1;
            one = 3 - ten;
            calcDegreesByFaceAngle(ten, day / 10, 0);
            calcDegreesByFaceAngle(one, day % 10, 0);
        } else {
            ten = 1;
            one = 2;
            calcDegreesByFaceAngle(ten, day / 10, 0);
            day %= 10;
            calcDegreesByFaceAngle(one, (day == 9) ? 3 : day - 3, (day == 9) ? 2 : 0);
        }
        if (mCubePos[one] < mCubePos[ten]) {
            float tmp = mCubePos[one];
            mCubePos[one] = mCubePos[ten];
            mCubePos[ten] = tmp;
        }
    }

    private void calcDegreesByFaceAngle(int type, int face, int angle) {
        if (face == 0 || face == 2) {
            mCubeDegX[type] = 0f;
            mCubeDegY[type] = face * 90f;
            mCubeDegZ[type] = (angle * (1 - face) & 3) * 90f;
        } else {
            if (face == 1) {
                face++;
                angle--;
            }
            mCubeDegX[type] = 90f;
            mCubeDegY[type] = (angle & 3) * 90f;
            mCubeDegZ[type] = ((face - 1) & 3) * 90f;
        }
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
