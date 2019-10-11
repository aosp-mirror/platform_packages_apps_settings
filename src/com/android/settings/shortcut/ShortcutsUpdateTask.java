/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.shortcut;

import static com.android.settings.shortcut.CreateShortcutPreferenceController.SHORTCUT_ID_PREFIX;
import static com.android.settings.shortcut.CreateShortcutPreferenceController.SHORTCUT_PROBE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

public class ShortcutsUpdateTask extends AsyncTask<Void, Void, Void> {

    private final Context mContext;

    public ShortcutsUpdateTask(Context context) {
        mContext = context;
    }

    @Override
    public Void doInBackground(Void... params) {
        ShortcutManager sm = mContext.getSystemService(ShortcutManager.class);
        PackageManager pm = mContext.getPackageManager();

        List<ShortcutInfo> updates = new ArrayList<>();
        for (ShortcutInfo info : sm.getPinnedShortcuts()) {
            if (!info.getId().startsWith(SHORTCUT_ID_PREFIX)) {
                continue;
            }
            ComponentName cn = ComponentName.unflattenFromString(
                    info.getId().substring(SHORTCUT_ID_PREFIX.length()));
            ResolveInfo ri = pm.resolveActivity(new Intent(SHORTCUT_PROBE).setComponent(cn), 0);
            if (ri == null) {
                continue;
            }
            updates.add(new ShortcutInfo.Builder(mContext, info.getId())
                    .setShortLabel(ri.loadLabel(pm)).build());
        }
        if (!updates.isEmpty()) {
            sm.updateShortcuts(updates);
        }
        return null;
    }
}
