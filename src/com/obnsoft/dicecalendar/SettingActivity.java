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
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class SettingActivity extends PreferenceActivity
        implements OnPreferenceClickListener, OnSharedPreferenceChangeListener {

    private static final String PREF_KEY_AUTO = "auto";
    private static final String PREF_KEY_ABOUT = "about";
    private static final int REQUEST_ID_GALLERY = 1;
    private static final int DIALOG_ID_ABOUT = 1;

    private boolean mStartingActivity;
    private ImageView mTexPreview;
    private Bitmap mTexBitmap;

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        addPreferencesFromResource(R.xml.prefs);
        findPreference(PREF_KEY_ABOUT).setOnPreferenceClickListener(this);
        mTexPreview = (ImageView) findViewById(R.id.img_texpreview);
        setSummaries(getPreferenceScreen());
        setTexturePreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onUserLeaveHint() {
        if (mStartingActivity) {
            mStartingActivity = false;
        } else {
            MyApplication.refreshWidget(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearTexturePreview();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_ID_GALLERY) {
            String path = null;
            if (resultCode == RESULT_OK) {
                Cursor cur = getContentResolver().query(intent.getData(),
                        new String[] {MediaStore.Images.Media.DATA}, null, null, null);
                cur.moveToNext();
                path = cur.getString(0);
                cur.close();
            }
            if (!DiceTexture.setTexturePath(this, path)) {
                ListPreference listPref =
                        (ListPreference) findPreference(DiceTexture.PREF_KEY_TEX);
                listPref.setValue(DiceTexture.PREF_VAL_TEX_DEFAULT);
                listPref.setSummary(listPref.getEntry());
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, R.string.msg_invalid_texture, Toast.LENGTH_LONG).show();
                }
            }
            setTexturePreview();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_ID_ABOUT:
            dialog = MainActivity.createVersionDialog(this);
            break;
        }
        return dialog;
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        showDialog(DIALOG_ID_ABOUT);
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (DiceTexture.PREF_KEY_TEX.equals(key)) {
            setSummary(key);
            if (DiceTexture.PREF_VAL_TEX_CUSTOM.equals(prefs.getString(key, null))) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                mStartingActivity = true;
                startActivityForResult(intent, REQUEST_ID_GALLERY);
            } else {
                setTexturePreview();
            }
        } else if (PREF_KEY_AUTO.equals(key)) {
            setMidnightAlerm(this);
        }
    }

    public void onClickPreview(View v) {
        getListView().performItemClick(v, 0/* position of 'Texture' item*/, 0);
    }

    public static void setMidnightAlerm(Context context) {
        Intent intent = new Intent(context, MyService.class);
        intent.putExtra(MyService.EXTRA_REQUEST, MyService.REQUEST_ADJUST);
        PendingIntent pendingIntent = PendingIntent.getService(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(PREF_KEY_AUTO, true)) {
            Calendar calendar = new GregorianCalendar();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            am.setInexactRepeating(AlarmManager.RTC,
                    calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        } else {
            am.cancel(pendingIntent);
        }
    }

    /*-----------------------------------------------------------------------*/

    private void setSummaries(Preference pref) {
        if (pref instanceof PreferenceGroup) {
            PreferenceGroup pg = (PreferenceGroup) pref;
            for (int i = 0; i < pg.getPreferenceCount(); i++) {
                setSummaries(pg.getPreference(i));
            }
        } else {
            setSummary(pref.getKey());
        }
    }

    private void setSummary(String key) {
        Preference pref = findPreference(key);
        if (pref instanceof ListPreference) {
            pref.setSummary(((ListPreference) pref).getEntry());
        }
    }

    private void setTexturePreview() {
        if (mTexBitmap != null) {
            clearTexturePreview();
        }
        mTexBitmap = DiceTexture.getTextureBitmap(this);
        mTexPreview.setImageBitmap(mTexBitmap);
    }

    private void clearTexturePreview() {
        if (mTexBitmap != null) {
            mTexPreview.setImageBitmap(null);
            mTexBitmap.recycle();
            mTexBitmap = null;
        }
    }

}
