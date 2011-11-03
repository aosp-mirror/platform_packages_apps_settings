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

import static android.provider.Settings.Secure.DREAM_COMPONENT;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DreamTesterPreference extends Preference {
    private static final String TAG = "DreamTesterPreference";
    
    private final PackageManager pm;
    private final ContentResolver resolver;

    public DreamTesterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        pm = getContext().getPackageManager();
        resolver = getContext().getContentResolver();
    }

    @Override
    protected void onClick() {
        String component = Settings.Secure.getString(resolver, DREAM_COMPONENT);
        if (component != null) {
            ComponentName cn = ComponentName.unflattenFromString(component);
            Intent intent = new Intent(Intent.ACTION_MAIN)
                .setComponent(cn)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    )
                .putExtra("android.dreams.TEST", true);
            getContext().startActivity(intent);
        }
    }
}
