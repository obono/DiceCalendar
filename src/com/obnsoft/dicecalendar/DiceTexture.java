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

import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

public abstract class DiceTexture {

    private static final HashMap<String, DiceTexture> MAP = new HashMap<String, DiceTexture>();
    private static final DiceTexture DEFAULT_TEXTURE = new Default(0x333399FF);

    static {
        MAP.put(MyApplication.PREF_VAL_TEX_DEFAULT, DEFAULT_TEXTURE);
        MAP.put("default_pink",   new Default(0x33FF66CC));
        MAP.put("default_lime", new Default(0x3366FF00));
    }

    public static Bitmap getTextureBitmap(Context context, String val) {
        DiceTexture texture = MAP.get(val);
        if (texture == null) texture = DEFAULT_TEXTURE;
        return texture.create(context);
    }

    protected abstract Bitmap create(Context context);

    /*-----------------------------------------------------------------------*/

    static class Default extends DiceTexture {
        private int mColor;
        public Default(int color) {
            mColor = color;
        }
        @Override
        protected Bitmap create(Context context) {
            Bitmap workBmp = BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.tex0_default);
            Bitmap retBmp = workBmp.copy(Bitmap.Config.ARGB_8888, true);
            workBmp.recycle();
            Canvas canvas = new Canvas(retBmp);
            canvas.drawColor(mColor);
            return retBmp;
        }
    }

    /*-----------------------------------------------------------------------*/

}
