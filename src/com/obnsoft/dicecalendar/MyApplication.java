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
import java.util.HashMap;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

public class MyApplication extends Application {

    public static final String PREF_KEY_TEX = "texture";
    public static final String PREF_KEY_ABOUT = "about";
    public static final String PREF_VAL_TEX_DEFAULT = "default";
    public static final String PREF_VAL_TEX_CUSTOM = "custom";

    private static final String PREF_KEY_AUTO = "auto";
    private static final String PREF_KEY_TEXPATH = "texture_path";
    private static final HashMap<String, Integer> MAP_TEXTURE_ID = new HashMap<String, Integer>();
    private static final int MAX_SIZE_TEX = 1024;

    static {
        MAP_TEXTURE_ID.put(PREF_VAL_TEX_DEFAULT, R.drawable.texture);
        MAP_TEXTURE_ID.put("negative", R.drawable.texture_negative);
    };

    private CubesState      mState;
    private PendingIntent   mAlarmIntent;

    /*-----------------------------------------------------------------------*/

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

    public static void refreshWidget(Context context) {
        Intent intent = new Intent(context, MyService.class);
        intent.putExtra(MyService.EXTRA_REQUEST, MyService.REQUEST_REFRESH);
        context.startService(intent);
    }

    public static Bitmap getTextureBitmap(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String texIdStr = prefs.getString(PREF_KEY_TEX, PREF_VAL_TEX_DEFAULT);
        Integer texId = MAP_TEXTURE_ID.get(texIdStr);
        int id = (texId != null) ? texId : R.drawable.texture;
        String path = prefs.getString(PREF_KEY_TEXPATH, null);

        Bitmap bitmap = null;
        if (PREF_VAL_TEX_CUSTOM.equals(texIdStr) && path != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            int width = options.outWidth;
            if (isAvailableSize(width, options.outHeight)) {
                if (width > MAX_SIZE_TEX) {
                    options.inSampleSize = width / MAX_SIZE_TEX;
                }
                options.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeFile(path, options);
            }
        }
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        }
        return bitmap;
    }

    public static boolean setTexturePath(Context context, String path) {
        boolean available = false;
        if (path != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            available = isAvailableSize(options.outWidth, options.outHeight);
        }
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(PREF_KEY_TEX, available ? PREF_VAL_TEX_CUSTOM : PREF_VAL_TEX_DEFAULT);
        editor.putString(PREF_KEY_TEXPATH, available ? path : null);
        editor.commit();
        return available;
    }

    private static boolean isAvailableSize(int width, int height) {
        return (width != 0 && width == height && (width & (width - 1)) == 0);
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
