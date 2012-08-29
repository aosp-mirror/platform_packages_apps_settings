/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static android.provider.Settings.Secure.SCREENSAVER_ENABLED;
import static android.provider.Settings.Secure.SCREENSAVER_ACTIVATE_ON_DOCK;
import static android.provider.Settings.Secure.SCREENSAVER_ACTIVATE_ON_SLEEP;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.dreams.Dream;
import android.service.dreams.IDreamManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DreamBackend {

    public static class DreamInfo {
        CharSequence caption;
        Drawable icon;
        boolean isActive;
        public ComponentName componentName;
        public ComponentName settingsComponentName;
    }

    private final Context mContext;
    private final IDreamManager mDreamManager;
    private final DreamInfoComparator mComparator;

    public DreamBackend(Context context) {
        mContext = context;
        mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));
        mComparator = new DreamInfoComparator(getDefaultDream());
    }

    public List<DreamInfo> getDreamInfos() {
        ComponentName activeDream = getActiveDream();
        PackageManager pm = mContext.getPackageManager();
        Intent dreamIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory("android.intent.category.DREAM");
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(dreamIntent,
                PackageManager.GET_META_DATA);
        List<DreamInfo> dreamInfos = new ArrayList<DreamInfo>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo == null)
                continue;
            DreamInfo dreamInfo = new DreamInfo();
            dreamInfo.caption = resolveInfo.loadLabel(pm);
            dreamInfo.icon = resolveInfo.loadIcon(pm);
            dreamInfo.componentName = getDreamComponentName(resolveInfo);
            dreamInfo.isActive = dreamInfo.componentName.equals(activeDream);
            dreamInfo.settingsComponentName = getSettingsComponentName(resolveInfo);
            dreamInfos.add(dreamInfo);
        }
        Collections.sort(dreamInfos, mComparator);
        return dreamInfos;
    }

    public ComponentName getDefaultDream() {
        if (mDreamManager == null)
            return null;
        try {
            return mDreamManager.getDefaultDreamComponent();
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean isEnabled() {
        return getBoolean(SCREENSAVER_ENABLED);
    }

    public void setEnabled(boolean value) {
        setBoolean(SCREENSAVER_ENABLED, value);
    }

    public boolean isActivatedOnDock() {
        return getBoolean(SCREENSAVER_ACTIVATE_ON_DOCK);
    }

    public void setActivatedOnDock(boolean value) {
        setBoolean(SCREENSAVER_ACTIVATE_ON_DOCK, value);
    }

    public boolean isActivatedOnSleep() {
        return getBoolean(SCREENSAVER_ACTIVATE_ON_SLEEP);
    }

    public void setActivatedOnSleep(boolean value) {
        setBoolean(SCREENSAVER_ACTIVATE_ON_SLEEP, value);
    }

    private boolean getBoolean(String key) {
        return Settings.Secure.getInt(mContext.getContentResolver(), key, 1) == 1;
    }

    private void setBoolean(String key, boolean value) {
        Settings.Secure.putInt(mContext.getContentResolver(), key, value ? 1 : 0);
    }

    public void startDreamingNow() {
        if (mDreamManager == null)
            return;
        try {
            mDreamManager.dream();
        } catch (RemoteException e) {
        }
    }

    public void setActiveDream(ComponentName dream) {
        if (mDreamManager == null)
            return;
        try {
            ComponentName[] dreams = { dream };
            mDreamManager.setDreamComponents(dream == null ? null : dreams);
        } catch (RemoteException e) {
            // noop
        }
    }

    public ComponentName getActiveDream() {
        if (mDreamManager == null)
            return null;
        try {
            ComponentName[] dreams = mDreamManager.getDreamComponents();
            return dreams != null && dreams.length > 0 ? dreams[0] : null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public void launchSettings(DreamInfo dreamInfo) {
        if (dreamInfo == null || dreamInfo.settingsComponentName == null)
            return;
        mContext.startActivity(new Intent().setComponent(dreamInfo.settingsComponentName));
    }

    public void preview(DreamInfo dreamInfo) {
        if (mDreamManager == null || dreamInfo == null || dreamInfo.componentName == null)
            return;
        try {
            mDreamManager.testDream(dreamInfo.componentName);
        } catch (RemoteException e) {
            // noop
        }
    }

    private static ComponentName getDreamComponentName(ResolveInfo ri) {
        if (ri == null || ri.serviceInfo == null)
            return null;
        return new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name);
    }

    private static ComponentName getSettingsComponentName(ResolveInfo ri) {
        if (ri == null || ri.serviceInfo == null || ri.serviceInfo.metaData == null)
            return null;
        String cn = ri.serviceInfo.metaData.getString(Dream.METADATA_NAME_CONFIG_ACTIVITY);
        return cn == null ? null : ComponentName.unflattenFromString(cn);
    }

    private static class DreamInfoComparator implements Comparator<DreamInfo> {
        private final ComponentName mDefaultDream;

        public DreamInfoComparator(ComponentName defaultDream) {
            mDefaultDream = defaultDream;
        }

        @Override
        public int compare(DreamInfo lhs, DreamInfo rhs) {
            return sortKey(lhs).compareTo(sortKey(rhs));
        }

        private String sortKey(DreamInfo di) {
            StringBuilder sb = new StringBuilder();
            sb.append(di.componentName.equals(mDefaultDream) ? '0' : '1');
            sb.append(di.caption);
            return sb.toString();
        }
    }
}
