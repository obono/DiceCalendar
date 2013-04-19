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
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.widget.RemoteViews;

public class MyService extends Service {

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.widget);
        PixelBuffer buffer = new PixelBuffer(512, 256);
        CubesState state = ((MyApplication) getApplication()).getCubesState();
        MyRenderer renderer = new MyRenderer(this, state, true);
        buffer.setRenderer(renderer);
        Bitmap bitmap = buffer.getBitmap();
        if (bitmap != null) {
            rv.setImageViewBitmap(R.id.widget_image, buffer.getBitmap());
        } else {
            rv.setImageViewResource(R.id.widget_image, R.drawable.icon);
        }
        rv.setOnClickPendingIntent(R.id.widget_image,
                PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));

        AppWidgetManager awm = AppWidgetManager.getInstance(this);
        ComponentName cns = new ComponentName(this, MyWidgetProviderSmall.class);
        ComponentName cnl = new ComponentName(this, MyWidgetProviderLarge.class);
        if (awm.getAppWidgetIds(cns).length > 0) {
            awm.updateAppWidget(cns, rv);
        }
        if (awm.getAppWidgetIds(cnl).length > 0) {
            awm.updateAppWidget(cnl, rv);
        }
        buffer.finish();
    }

}
