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

package com.android.settings.testutils.shadow;

import static org.robolectric.RuntimeEnvironment.application;

import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import java.util.List;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadow.api.Shadow;

@Implements(ShortcutManager.class)
public class ShadowShortcutManager extends org.robolectric.shadows.ShadowShortcutManager {

    private List<ShortcutInfo> mPinnedShortcuts;
    private List<ShortcutInfo> mLastUpdatedShortcuts;

    @Resetter
    public void reset() {
        mPinnedShortcuts = null;
        mLastUpdatedShortcuts = null;
    }

    public static ShadowShortcutManager get() {
        return Shadow.extract(application.getSystemService(ShortcutManager.class));
    }

    @Implementation
    public boolean updateShortcuts(List<ShortcutInfo> shortcutInfoList) {
        mLastUpdatedShortcuts = shortcutInfoList;
        return true;
    }

    public List<ShortcutInfo> getLastUpdatedShortcuts() {
        return mLastUpdatedShortcuts;
    }

    @Implementation
    public List<ShortcutInfo> getPinnedShortcuts() {
        return mPinnedShortcuts;
    }

    public void setPinnedShortcuts(List<ShortcutInfo> shortcutInfos) {
        mPinnedShortcuts = shortcutInfos;
    }
}
