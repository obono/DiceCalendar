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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.provider.MediaStore;
import android.widget.Toast;

public class SettingActivity extends PreferenceActivity
        implements OnPreferenceClickListener, OnSharedPreferenceChangeListener {

    private static final int REQUEST_ID_GALLERY = 1;

    private boolean mStartingActivity;

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        setSummaries(getPreferenceScreen());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onUserLeaveHint(){
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
        ((MyApplication) getApplication()).getPrefsInSetting(prefs);
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
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        MainActivity.showVersion(this);
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        setSummary(key);
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
        if (MyApplication.PREF_KEY_ABOUT.equals(key)) {
            pref.setOnPreferenceClickListener(this);
        }
        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());
            if (DiceTexture.PREF_KEY_TEX.equals(key) &&
                    DiceTexture.PREF_VAL_TEX_CUSTOM.equals(listPref.getValue())) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                mStartingActivity = true;
                startActivityForResult(intent, REQUEST_ID_GALLERY);
            }
        }
        if (pref instanceof EditTextPreference) {
            pref.setSummary(((EditTextPreference) pref).getText());
        }
    }

}
