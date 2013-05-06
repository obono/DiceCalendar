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
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

public abstract class DiceTexture {

    private static final HashMap<String, DiceTexture> MAP = new HashMap<String, DiceTexture>();
    private static final DiceTexture DEFAULT_TEXTURE = new Default(0x333399FF);

    static {
        MAP.put(MyApplication.PREF_VAL_TEX_DEFAULT, DEFAULT_TEXTURE);
        MAP.put("default_pink", new Default(0x33FF66CC));
        MAP.put("default_lime", new Default(0x3366FF00));
        MAP.put("wood_pine",    new Special(R.drawable.tex1_bg1, R.drawable.tex1_font, false));
        MAP.put("wood_keyaki",  new Special(R.drawable.tex1_bg2, R.drawable.tex1_font, false));
        MAP.put("wood_ebony",   new Special(R.drawable.tex1_bg3, R.drawable.tex1_font, true));
        MAP.put("marble",       new Special(R.drawable.tex2_bg1, R.drawable.tex2_font, false));
        MAP.put("marble_green", new Special(R.drawable.tex2_bg2, R.drawable.tex2_font, false));
        MAP.put("kanji_light",  new Special(R.drawable.tex3_bg1, R.drawable.tex3_font, false));
        MAP.put("kanji_dark",   new Special(R.drawable.tex3_bg2, R.drawable.tex3_font, true));
    }

    public static Bitmap getTextureBitmap(Context context, String val) {
        DiceTexture texture = MAP.get(val);
        if (texture == null) texture = DEFAULT_TEXTURE;
        return texture.create(context);
    }

    private static Bitmap newBitmap() {
        return Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
    }

    private static void tileImage(Context context, int drawableId, Canvas canvas, Paint paint) {
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), drawableId);
        for (int y = 0, h = canvas.getHeight(); y < h; y += bmp.getWidth()) {
            for (int x = 0, w = canvas.getWidth(); x < w; x += bmp.getWidth()) {
                canvas.drawBitmap(bmp, x, y, paint);
            }
        }
        bmp.recycle();
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
            Bitmap bmp = newBitmap();
            Canvas canvas = new Canvas(bmp);
            tileImage(context, R.drawable.tex0_font, canvas, null);
            canvas.drawColor(mColor);
            return bmp;
        }
    }

    /*-----------------------------------------------------------------------*/

    static class Special extends DiceTexture {
        private int mBgDrawableId;
        private int mFontDrawableId;
        private boolean mIsNegative;
        public Special(int bgId, int fontId, boolean isNegative) {
            mBgDrawableId = bgId;
            mFontDrawableId = fontId;
            mIsNegative = isNegative;
        }
        @Override
        protected Bitmap create(Context context) {
            Bitmap bmp = newBitmap();
            Canvas canvas = new Canvas(bmp);
            Paint paint = null;
            if (mIsNegative) {
                paint = new Paint();
                paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[] {
                        0f, -0.5f, -0.5f, 0f, 255f,
                        -0.5f, 0f, -0.5f, 0f, 255f,
                        -0.5f, -0.5f, 0f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f,
                })));
            }
            tileImage(context, mBgDrawableId, canvas, null);
            tileImage(context, mFontDrawableId, canvas, paint);
            return bmp;
        }
    }

}
