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

import android.app.Application;
import android.content.Context;
import android.content.Intent;

public class MyApplication extends Application {

    private CubesState mState;

    @Override
    public void onCreate() {
        super.onCreate();
        mState = new CubesState(this);
        SettingActivity.setMidnightAlerm(this);
    }

    public CubesState getCubesState() {
        return mState;
    }

    public static void refreshWidget(Context context) {
        Intent intent = new Intent(context, MyService.class);
        intent.putExtra(MyService.EXTRA_REQUEST, MyService.REQUEST_REFRESH);
        context.startService(intent);
    }

}
