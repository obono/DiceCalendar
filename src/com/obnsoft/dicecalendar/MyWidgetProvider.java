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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.widget.RemoteViews;

public class MyWidgetProvider extends AppWidgetProvider {

    private static Bitmap sBitmap;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        initalizeBitmap();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager awm, int[] awi) {
        super.onUpdate(context, awm, awi);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
        ComponentName cn = new ComponentName(context, MyWidgetProvider.class);
        drawBitmap();
        rv.setImageViewBitmap(R.id.widget_image, sBitmap);
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

    private void initalizeBitmap() {
        if (sBitmap == null) {
            sBitmap = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888);
        }
    }

    private void drawBitmap() {
        if (sBitmap == null) {
            initalizeBitmap();
        }
        Canvas canvas = new Canvas(sBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.GRAY);
        paint.setTextSize(64);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawText(String.valueOf(System.currentTimeMillis()), 0, 128, paint);
    }

    private void finishBitmap() {
        if (sBitmap != null) {
            sBitmap.recycle();
            sBitmap = null;
        }
    }

}
