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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.preference.PreferenceManager;

public class DiceTexture {

    public static final String PREF_KEY_TEX = "texture";
    public static final String PREF_VAL_TEX_DEFAULT = "default";
    public static final String PREF_VAL_TEX_CUSTOM = "custom";

    private static final HashMap<String, ITextureDrawer> MAP = new HashMap<String, ITextureDrawer>();
    private static final ITextureDrawer DEFAULT_TEXTURE = new Default(0x333399FF);

    private static final String PREF_KEY_TEXPATH = "texture_path";
    private static final int PRESET_SIZE_TEX = 512;
    private static final int MAX_SIZE_TEX = 1024;

    static {
        MAP.put(PREF_VAL_TEX_DEFAULT, DEFAULT_TEXTURE);
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

    public static Bitmap getTextureBitmap(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String texId = prefs.getString(PREF_KEY_TEX, PREF_VAL_TEX_DEFAULT);
        Bitmap bitmap = null;
        if (PREF_VAL_TEX_CUSTOM.equals(texId)) {
            String path = prefs.getString(PREF_KEY_TEXPATH, null);
            if (path != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);
                int width = options.outWidth;
                if (isAvailableSize(width, options.outHeight)) {
                    if (width > MAX_SIZE_TEX) {
                        options.inSampleSize = width / MAX_SIZE_TEX;
                    }
                    options.inJustDecodeBounds = false;
                    bitmap = BitmapFactory.decodeFile(path, options);
                }
            }
        }
        if (bitmap == null) {
            ITextureDrawer texture = MAP.get(texId);
            if (texture == null) texture = DEFAULT_TEXTURE;
            bitmap = Bitmap.createBitmap(
                    PRESET_SIZE_TEX, PRESET_SIZE_TEX, Bitmap.Config.ARGB_8888);
            texture.draw(context, new Canvas(bitmap));
        }
        return bitmap;
    }

    public static boolean setTexturePath(Context context, String path) {
        boolean available = false;
        if (path != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            available = isAvailableSize(options.outWidth, options.outHeight);
        }
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(PREF_KEY_TEX, available ? PREF_VAL_TEX_CUSTOM : PREF_VAL_TEX_DEFAULT);
        editor.putString(PREF_KEY_TEXPATH, available ? path : null);
        editor.commit();
        return available;
    }

    private static boolean isAvailableSize(int width, int height) {
        return (width > 0 && width == height && (width & (width - 1)) == 0);
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

    private static Paint coloredPaint(int color) {
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        return paint;
    }

    /*-----------------------------------------------------------------------*/

    private interface ITextureDrawer {
        public void draw(Context context, Canvas canvas);
    }

    private static class Default implements ITextureDrawer {
        private int mColor;
        public Default(int color) {
            mColor = color;
        }
        @Override
        public void draw(Context context, Canvas canvas) {
            tileImage(context, R.drawable.tex0_font, canvas, coloredPaint(mColor));
        }
    }

    private static class Special implements ITextureDrawer {
        private int mBgId;
        private int mFontId;
        private boolean mWhite;
        public Special(int bgId, int fontId, boolean white) {
            mBgId = bgId;
            mFontId = fontId;
            mWhite = white;
        }
        @Override
        public void draw(Context context, Canvas canvas) {
            tileImage(context, mBgId, canvas, null);
            tileImage(context, mFontId, canvas, mWhite ? coloredPaint(Color.WHITE) : null);
        }
    }

}
