/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.testutils;

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Utility class for common methods used in the accessibility feature related tests
 */
public class AccessibilityTestUtils {

    public static void setSoftwareShortcutMode(
            Context context, boolean gestureNavEnabled, boolean floatingButtonEnabled) {
        int buttonMode = floatingButtonEnabled ? ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU : -1;
        int navMode = gestureNavEnabled ? NAV_BAR_MODE_GESTURAL : NAV_BAR_MODE_3BUTTON;

        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, buttonMode);
        SettingsShadowResources.overrideResource(
                com.android.internal.R.integer.config_navBarInteractionMode, navMode);
        assertThat(context.getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode)).isEqualTo(navMode);
    }

    /**
     * Returns a mock {@link AccessibilityManager}
     */
    public static AccessibilityManager setupMockAccessibilityManager(Context mockContext) {
        AccessibilityManager am = mock(AccessibilityManager.class);
        when(mockContext.getSystemService(AccessibilityManager.class)).thenReturn(am);
        return am;
    }

    public static AccessibilityServiceInfo createAccessibilityServiceInfo(
            Context context, ComponentName componentName, boolean isAlwaysOnService) {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = componentName.getPackageName();
        serviceInfo.packageName = componentName.getPackageName();
        serviceInfo.name = componentName.getClassName();
        serviceInfo.applicationInfo = applicationInfo;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;
        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    context);
            info.setComponentName(componentName);
            if (isAlwaysOnService) {
                info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
            }
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }
        return null;
    }
}
