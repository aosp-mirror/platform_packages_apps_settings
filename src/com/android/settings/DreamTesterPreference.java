/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.service.dreams.IDreamManager;
import android.util.AttributeSet;
import android.util.Log;

public class DreamTesterPreference extends Preference {
    private static final String TAG = "DreamTesterPreference";

    public DreamTesterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onClick() {
        IDreamManager dm = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));
        try {
            ComponentName[] dreams = dm.getDreamComponents();
            if (dreams == null || dreams.length == 0)
                return;
            ComponentName cn = dreams[0];
            Log.v(TAG, "DreamComponent cn=" + cn);
            dm.testDream(cn);
        } catch (RemoteException ex) {
            Log.w(TAG, "error testing dream", ex);
            // too bad, so sad, oh mom, oh dad
        }
    }

}
