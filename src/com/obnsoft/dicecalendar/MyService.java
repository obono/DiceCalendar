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

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class MyService extends Service {

    public static final String ACTION_REFRESH = "com.obnsoft.dicecalendar.action.REFRESH";

    private static final String TAG = "DiceCalendar";

    private RemoteViews     mRemoteViews;
    private ComponentName   mComponent;

    @Override
    public IBinder onBind(Intent intent) {
        myLog("onBind " + intent.getAction());
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        String action = intent.getAction();
        myLog("onStart " + action);
        super.onStart(intent, startId);
        Context context = getBaseContext();
        initialize(context);
        if (ACTION_REFRESH.equals(action)) {
            refreshUI(context);
        }
    }

    @Override
    public void onDestroy() {
        myLog("onDestroy");
        super.onDestroy();
    }

    /*-----------------------------------------------------------------------*/

    private void initialize(Context context) {
        myLog("initialize");

        mRemoteViews = new RemoteViews(getPackageName(), R.layout.widget);
        mComponent = new ComponentName(context, MyWidgetProvider.class);
        Intent intent = new Intent(this, MainActivity.class);
        mRemoteViews.setOnClickPendingIntent(R.id.widget_image,
                PendingIntent.getActivity(context, 0, intent, 0));
        refreshUI(context);
    }

    private void refreshUI(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(mComponent, mRemoteViews);
    }

    private void myLog(String msg) {
        Log.d(TAG, msg);
    }

}
