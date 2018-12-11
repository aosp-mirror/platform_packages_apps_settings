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

package com.android.settings.wrapper;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.os.ServiceManager;

import java.util.ArrayList;
import java.util.List;

public class OverlayManagerWrapper {

    private final IOverlayManager mOverlayManager;

    public OverlayManagerWrapper(IOverlayManager overlayManager) {
        mOverlayManager = overlayManager;
    }

    public OverlayManagerWrapper() {
        this(IOverlayManager.Stub.asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE)));
    }

    public List<OverlayInfo> getOverlayInfosForTarget(String overlay, int userId) {
        if (mOverlayManager == null) {
            return new ArrayList<>();
        }
        try {
            List<android.content.om.OverlayInfo> infos
                    = mOverlayManager.getOverlayInfosForTarget(overlay, userId);
            ArrayList<OverlayInfo> result = new ArrayList<>(infos.size());
            for (int i = 0; i < infos.size(); i++) {
                result.add(new OverlayInfo(infos.get(i)));
            }
            return result;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setEnabled(String overlay, boolean enabled, int userId) {
        if (mOverlayManager == null) {
            return false;
        }
        try {
            return mOverlayManager.setEnabled(overlay, enabled, userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setEnabledExclusiveInCategory(String overlay, int userId) {
        if (mOverlayManager == null) {
            return false;
        }
        try {
            return mOverlayManager.setEnabledExclusiveInCategory(overlay, userId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class OverlayInfo {

        public static final String CATEGORY_THEME = android.content.om.OverlayInfo.CATEGORY_THEME;
        public final String packageName;
        public final String category;
        public final int priority;
        private final boolean mEnabled;

        public OverlayInfo(String packageName, String category, boolean enabled, int priority) {
            this.packageName = packageName;
            this.category = category;
            mEnabled = enabled;
            this.priority = priority;
        }

        public OverlayInfo(android.content.om.OverlayInfo info) {
            mEnabled = info.isEnabled();
            category = info.category;
            packageName = info.packageName;
            priority = info.priority;
        }

        public boolean isEnabled() {
            return mEnabled;
        }
    }
}
