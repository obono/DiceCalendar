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
    private boolean mCameraMode;
    private int mRotateMode;
    private boolean mIsDirty;

    public CubesState(Context context) {
        mContext = context;
        loadState();

        mIsEach = false;
        mFocusCube = FOCUS_NONE;
        mMoveMode = false;
        mCameraMode = false;
        mRotateMode = ROTATE_NONE;
    }

    public boolean onTouchEvent(int action, float x, float y) {
        return (mIsEach) ? rotateCube(action, x, y) : adjustCubes(action, x, y);
    }

    public boolean placeAuto() {
        arrangeToday();
        return false;
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
        int degX = Math.round(mCubeDegX[mFocusCube] / 90f);
        int degY = Math.round(mCubeDegY[mFocusCube] / 90f);
        int degZ = Math.round(mCubeDegZ[mFocusCube] / 90f);
        boolean ret = false;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (Math.abs(x) < 0.20f) {
                mRotateMode = (Math.abs(y) < 0.20f) ? ROTATE_NONE : ROTATE_X;
            } else {
                mRotateMode = (Math.abs(y) < 0.20f) ? ROTATE_Y : ROTATE_Z;
            }
            mTouchX = x;
            mTouchY = y;
            break;
        case MotionEvent.ACTION_MOVE:
            float deg;
            switch (mRotateMode) {
            case ROTATE_X:
                deg = (mTouchY - y) * 135f;
                mCubeDegX[mFocusCube] += deg;
                mTouchY = y;
                ret = true;
                break;
            case ROTATE_Y:
                deg = (mTouchX - x) * 135f;
                if ((degX & 3) == 0) {
                    mCubeDegY[mFocusCube] -= deg;
                } else if ((degY & 1) == 0) {
                    mCubeDegZ[mFocusCube] += deg * (1 - (degY & 2));
                } else {
                    mCubeDegX[mFocusCube] = 0;
                    mCubeDegY[mFocusCube] -= deg;
                    mCubeDegZ[mFocusCube] += 90f * (1 - (degY & 2));
                }
                mTouchX = x;
                ret = true;
                break;
            case ROTATE_Z:
                deg = (float) Math.toDegrees(Math.atan2(mTouchY, mTouchX) - Math.atan2(y, x));
                if ((degX & 3) == 0) {
                    mCubeDegZ[mFocusCube] += (degY == 0) ? -deg : deg;
                } else {
                    mCubeDegY[mFocusCube] -= deg;
                }
                mTouchX = x;
                mTouchY = y;
                ret = true;
                break;
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mRotateMode == ROTATE_NONE) {
                if (Math.abs(x) < 0.25f && Math.abs(y) < 0.25f) {
                    mIsEach = false;
                    mFocusCube = FOCUS_NONE;
                    ret = true;
                }
            } else {
                if ((degX & 2) != 0) {
                    if ((degY & 1) == 0) {
                        degX += 2;
                        degY += 2;
                        degZ += 2;
                    } else {
                        degZ += (degX - 1) * (2 - (degY & 3));
                        degX = 1;
                    }
                }
                if ((degX & 3) == 0 && (degY & 1) != 0) {
                    degZ += (degY & 3) - 2;
                    degX = 1;
                }
                mCubeDegX[mFocusCube] = (degX & 3) * 90f;
                mCubeDegY[mFocusCube] = (degY & 3) * 90f;
                mCubeDegZ[mFocusCube] = (degZ & 3) * 90f;
                mIsDirty = true;
                ret = true;
            }
            break;
        }

        return ret;
    }

    private boolean adjustCubes(int action, float x, float y) {
        boolean ret = false;
        float pos = x * 4.5f;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (Math.abs(x) < 0.5f && Math.abs(y) < 0.125f) {
                for (int i = 0; i < 4; i++) {
                    if (Math.abs(mCubePos[i] - pos) < 0.5f) {
                        mTouchX = x;
                        mFocusCube = i;
                        ret = true;
                        break;
                    }
                }
            } else if (y < -0.125f) {
                mCameraMode = true;
                mTouchX = x;
                mTouchY = y;
            } else if (y > 0.125f) {
                arrangeToday();
                mIsDirty = true;
                ret = true;
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
            if (mCameraMode) {
                mBaseDegX += (mTouchY - y) * 180f;
                mBaseDegY += (x - mTouchX) * 90f;
                if (mBaseDegX < -20f) mBaseDegX = -20f;
                if (mBaseDegX > 20f)  mBaseDegX = 20f;
                if (mBaseDegY < -30f) mBaseDegY = -30f;
                if (mBaseDegY > 30f)  mBaseDegY = 30f;
                mTouchX = x;
                mTouchY = y;
                ret = true;
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mFocusCube != FOCUS_NONE) {
                if (mMoveMode) {
                    mCubePos[mFocusCube] = Math.round(mCubePos[mFocusCube] - 0.5f) + 0.5f;
                    if (mCubePos[mFocusCube] < -1.5f) mCubePos[mFocusCube] = -1.5f;
                    if (mCubePos[mFocusCube] > 1.5f)  mCubePos[mFocusCube] = 1.5f;
                    mIsDirty = true;
                    mMoveMode = false;
                    mFocusCube = FOCUS_NONE;
                } else {
                    mIsEach = true;
                }
                ret = true;
            }
            if (mCameraMode) {
                mIsDirty = true;
                mCameraMode = false;
            }
            break;
        }

        return ret;
    }

    private void arrangeToday() {
        Calendar calendar = new GregorianCalendar();
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
