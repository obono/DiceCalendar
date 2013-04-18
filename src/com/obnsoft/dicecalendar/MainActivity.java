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
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class MainActivity extends Activity {

    private GLSurfaceView   mGLView;
    private CubesState      mState;
    private MyRenderer      mRenderer;

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
        mGLView = (GLSurfaceView) findViewById(R.id.glview);
        mState = new CubesState(this);
        mRenderer = new MyRenderer(this, mState, false);
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                float w = mGLView.getWidth();
                float h = mGLView.getHeight();
                float s = Math.min(w, h);
                float x = (e.getX() - w / 2) / s;
                float y = (h / 2 - e.getY()) / s;
                if (mState.onTouchEvent(e.getActionMasked(), x, y)) {
                    mGLView.requestRender();
                }
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mState.save();
    }

    public void onClickAbout(View v) {
        showVersion();
    }

    /*-----------------------------------------------------------------------*/

    private void showVersion() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View aboutView = inflater.inflate(R.layout.about, null);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            TextView textView = (TextView) aboutView.findViewById(R.id.text_about_version);
            textView.setText("Version " + packageInfo.versionName);

            StringBuilder buf = new StringBuilder();
            InputStream in = getResources().openRawResource(R.raw.license);
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
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.about)
                .setView(aboutView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

}
