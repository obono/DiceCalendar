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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends Activity {

    private MyGLSurfaceView mGLView;
    private CubesState      mState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*  If the second (or more) widget is placed, exit immediately.  */
        Intent intent = getIntent();
        if (AppWidgetManager.ACTION_APPWIDGET_CONFIGURE.equals(intent.getAction())) {
            setResult(RESULT_OK, new Intent().putExtras(intent.getExtras()));
            AppWidgetManager awm = AppWidgetManager.getInstance(this);
            ComponentName cns = new ComponentName(this, MyWidgetProviderSmall.class);
            ComponentName cnl = new ComponentName(this, MyWidgetProviderLarge.class);
            if (awm.getAppWidgetIds(cns).length + awm.getAppWidgetIds(cnl).length >= 2) {
                finish();
                return;
            }
        }

        setContentView(R.layout.main);
        mGLView = (MyGLSurfaceView) findViewById(R.id.glview);
        mState = ((MyApplication) getApplication()).getCubesState();
        mGLView.setCubesState(mState);
        final ImageButton btnCompass = (ImageButton) findViewById(R.id.btn_compass);
        final ImageButton btnToday = (ImageButton) findViewById(R.id.btn_today);
        mGLView.setOnZoomListener(new MyGLSurfaceView.OnZoomListener() {
            @Override
            public void onZoomModeChanged(boolean isZooming) {
                int visibility = isZooming ? View.INVISIBLE : View.VISIBLE;
                btnCompass.setVisibility(visibility);
                btnToday.setVisibility(visibility);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        mGLView.regulate();
        mState.save();

        Intent intent = new Intent(this, MyService.class);
        intent.putExtra(MyService.EXTRA_REQUEST, MyService.REQUEST_REFRESH);
        startService(intent);
    }

    /*-----------------------------------------------------------------------*/

    public void onClickCompass(View v) {
        mState.resetBaseRotation();
        mGLView.requestRender();
    }

    public void onClickToday(View v) {
        mState.arrangeToday();
        mGLView.requestRender();
    }

    public void onClickPrefs(View v) {
        startActivity(new Intent(this, SettingActivity.class));
    }

    public void onClickAbout(View v) {
        showVersion(this);
    }

    public static void showVersion(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View aboutView = inflater.inflate(R.layout.about, null);
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            TextView textView = (TextView) aboutView.findViewById(R.id.text_about_version);
            textView.setText("Version " + packageInfo.versionName);

            StringBuilder buf = new StringBuilder();
            InputStream in = context.getResources().openRawResource(R.raw.license);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String str;
            while((str = reader.readLine()) != null) {
                buf.append(str).append('\n');
            }
            textView = (TextView) aboutView.findViewById(R.id.text_about_message);
            textView.setText(buf.toString());
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.prefs_about)
                .setView(aboutView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

}
