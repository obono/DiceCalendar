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
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

public class MyWidgetProvider extends AppWidgetProvider {

    private static PixelBuffer  sPixelBuffer;
    private static MyRenderer   sRenderer;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        initalizeBitmap(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager awm, int[] awi) {
        super.onUpdate(context, awm, awi);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
        ComponentName cn = new ComponentName(context, MyWidgetProvider.class);
        rv.setImageViewBitmap(R.id.widget_image, drawBitmap(context));
        Intent intent = new Intent(context, MainActivity.class);
        rv.setOnClickPendingIntent(R.id.widget_image,
                PendingIntent.getActivity(context, 0, intent, 0));
        awm.updateAppWidget(cn, rv);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        finishBitmap();
    }

    /*-----------------------------------------------------------------------*/

    private void initalizeBitmap(Context context) {
        if (sPixelBuffer == null) {
            sPixelBuffer = new PixelBuffer(512, 256);
            sRenderer = new MyRenderer(context);
            sPixelBuffer.setRenderer(sRenderer);
        }
    }

    private Bitmap drawBitmap(Context context) {
        if (sPixelBuffer == null) {
            initalizeBitmap(context);
        }
        return sPixelBuffer.getBitmap();
    }

    private void finishBitmap() {
        if (sPixelBuffer != null) {
            sPixelBuffer.finish();
            sPixelBuffer = null;
            sRenderer = null;
        }
    }

}
