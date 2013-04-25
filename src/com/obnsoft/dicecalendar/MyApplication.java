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

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MyApplication extends Application {

    private static final String PREF_KEY_AUTO = "auto";

    private CubesState      mState;
    private PendingIntent   mAlarmIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        mState = new CubesState(this);
        Intent intent = new Intent(this, MyService.class);
        intent.putExtra(MyService.EXTRA_REQUEST, MyService.REQUEST_ADJUST);
        mAlarmIntent = PendingIntent.getService(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        getPrefsInSetting(prefs);
    }

    public void getPrefsInSetting(SharedPreferences prefs) {
        setMidnightAlerm(prefs.getBoolean(PREF_KEY_AUTO, true));
    }

    public CubesState getCubesState() {
        return mState;
    }

    /*-----------------------------------------------------------------------*/

    private void setMidnightAlerm(boolean enable) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (enable) {
            Calendar calendar = new GregorianCalendar();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            am.setInexactRepeating(AlarmManager.RTC,
                    calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, mAlarmIntent);
        } else {
            am.cancel(mAlarmIntent);
        }
    }

}
