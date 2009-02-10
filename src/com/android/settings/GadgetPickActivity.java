/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.LauncherActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.gadget.GadgetInfo;
import android.gadget.GadgetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

public class GadgetPickActivity extends LauncherActivity
{
    private static final String TAG = "GadgetPickActivity";

    GadgetManager mGadgetManager;
    int mGadgetId;
    int mHostId;
    
    public GadgetPickActivity() {
        mGadgetManager = GadgetManager.getInstance(this);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Bundle extras = getIntent().getExtras();
        mHostId = extras.getInt(GadgetManager.EXTRA_HOST_ID);
        mGadgetId = extras.getInt(GadgetManager.EXTRA_GADGET_ID);

        setResultData(RESULT_CANCELED);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        Intent intent = intentForPosition(position);
        mGadgetManager.bindGadgetId(mGadgetId, intent.getComponent());
        setResultData(RESULT_OK);
        finish();
    }
    
    @Override
    public List<ListItem> makeListItems() {
        List<GadgetInfo> installed = mGadgetManager.getInstalledProviders();
        PackageManager pm = getPackageManager();

        Drawable defaultIcon = null;
        IconResizer resizer = new IconResizer();

        ArrayList<ListItem> result = new ArrayList();
        final int N = installed.size();
        for (int i=0; i<N; i++) {
            GadgetInfo info = installed.get(i);

            LauncherActivity.ListItem item = new LauncherActivity.ListItem();
            item.packageName = info.provider.getPackageName();
            item.className = info.provider.getClassName();
            
            item.label = info.label;
            if (info.icon != 0) {
                Drawable d = pm.getDrawable( item.packageName, info.icon, null);
                if (d != null) {
                    item.icon = resizer.createIconThumbnail(d);
                } else {
                    Log.w(TAG, "Can't load icon drawable 0x" + Integer.toHexString(info.icon)
                            + " for package: " + item.packageName);
                }
            }
            if (item.icon == null) {
                // (including error case above)
                if (defaultIcon == null) {
                    // TODO: Load standard icon.
                }
                item.icon = defaultIcon;
            }
            
            result.add(item);
        }
        return result;
    }
    
    void setResultData(int code) {
        Intent result = new Intent();
        result.putExtra(GadgetManager.EXTRA_GADGET_ID, mGadgetId);
        setResult(code, result);
    }
}

