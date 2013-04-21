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

public class CubesState {

    private static final String FNAME_STATE = "state.dat";
    private static final int DATA_VERSION = 1;

    private static final float MAX_BASE_DEGX = 20f;
    private static final float MAX_BASE_DEGY = 30f;

    protected boolean   isZooming;
    protected float     baseDegX;
    protected float     baseDegY;
    protected Cube[]    cubes = new Cube[4];
    protected Cube      focusCube;

    private Context mContext;

    /*-----------------------------------------------------------------------*/

    class Cube {

        protected int type;
        protected float pos;
        protected float degX;
        protected float degY;
        protected float degZ;

        public Cube(int type) {
            this.type = type;
        }

        protected void alignPositionDegrees() {
            pos = Math.round(pos - 0.5f) + 0.5f;
            if (pos < -1.5f) pos = -1.5f;
            if (pos > 1.5f)  pos = 1.5f;

            int angleX = Math.round(degX / 90f) & 3;
            int angleY = Math.round(degY / 90f) & 3;
            int angleZ = Math.round(degZ / 90f) & 3;
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
            degX = (angleX & 3) * 90f;
            degY = (angleY & 3) * 90f;
            degZ = (angleZ & 3) * 90f;
        }

        protected void setDegreesByFaceAngle(int face, int angle) {
            if (face == 0 || face == 2) {
                degX = 0f;
                degY = face * 90f;
                degZ = (angle * (1 - face) & 3) * 90f;
            } else {
                if (face == 1) {
                    face++;
                    angle--;
                }
                degX = 90f;
                degY = (angle & 3) * 90f;
                degZ = ((face - 1) & 3) * 90f;
            }
        }

    }

    /*-----------------------------------------------------------------------*/

    public CubesState(Context context) {
        mContext = context;
        for (int i = 0; i < cubes.length; i++) {
            cubes[i] = new Cube(i);
        }
        loadState();
        focusCube = null;
        isZooming = false;
    }

    public void addBaseDegree(float degX, float degY) {
        baseDegX += degX;
        baseDegY += degY;
        if (baseDegX < -MAX_BASE_DEGX) baseDegX = -MAX_BASE_DEGX;
        if (baseDegX > MAX_BASE_DEGX)  baseDegX = MAX_BASE_DEGX;
        if (baseDegY < -MAX_BASE_DEGY) baseDegY = -MAX_BASE_DEGY;
        if (baseDegY > MAX_BASE_DEGY)  baseDegY = MAX_BASE_DEGY;
    }

    public void resetBaseRotation() {
        baseDegX = 0f;
        baseDegY = 0f;
    }

    public void alignCubes() {
        for (Cube cube : cubes) {
            cube.alignPositionDegrees();
        }
    }

    public void arrangeToday() {
        arrangeByDate(new GregorianCalendar());
    }

    public boolean save() {
        return saveState();
    }

    /*-----------------------------------------------------------------------*/

    private void arrangeByDate(Calendar calendar) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int wday = calendar.get(Calendar.DAY_OF_WEEK);
        cubes[0].setDegreesByFaceAngle(month % 6, (month < 6) ? 0 : 2);
        cubes[3].setDegreesByFaceAngle((wday == Calendar.SUNDAY) ? 5 : wday - 2,
                (wday == Calendar.SUNDAY) ? 2 : 0);
        int cubeTen, cubeOne;
        if (day % 10 <= 5) {
            cubeTen = (day % 10 >= 3 || day >= 30 || cubes[1].pos > cubes[2].pos) ? 2 : 1;
            cubeOne = 3 - cubeTen;
            cubes[cubeTen].setDegreesByFaceAngle(day / 10, 0);
            cubes[cubeOne].setDegreesByFaceAngle(day % 10, 0);
        } else {
            cubeTen = 1;
            cubeOne = 2;
            cubes[cubeTen].setDegreesByFaceAngle(day / 10, 0);
            day %= 10;
            cubes[cubeOne].setDegreesByFaceAngle((day == 9) ? 3 : day - 3, (day == 9) ? 2 : 0);
        }
        if (cubes[cubeOne].pos < cubes[cubeTen].pos) {
            float tmp = cubes[cubeOne].pos;
            cubes[cubeOne].pos = cubes[cubeTen].pos;
            cubes[cubeTen].pos = tmp;
        }
    }

    private boolean loadState() {
        boolean ret = false;
        try {
            DataInputStream in =
                    new DataInputStream(mContext.openFileInput(FNAME_STATE));
            switch (in.read()) {
            case DATA_VERSION:
                baseDegX = in.readFloat();
                baseDegY = in.readFloat();
                for (Cube cube : cubes) {
                    cube.pos = in.readFloat();
                    cube.degX = in.readFloat();
                    cube.degY = in.readFloat();
                    cube.degZ = in.readFloat();
                }
            }
            in.close();
            ret = true;
        } catch (FileNotFoundException e) {
            baseDegX = baseDegY = 0f;
            for (Cube cube : cubes) {
                cube.pos = cube.type - 1.5f;
                cube.degX = cube.degY = cube.degZ = 0f;
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
            out.writeFloat(baseDegX);
            out.writeFloat(baseDegY);
            for (Cube cube : cubes) {
                out.writeFloat(cube.pos);
                out.writeFloat(cube.degX);
                out.writeFloat(cube.degY);
                out.writeFloat(cube.degZ);
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
