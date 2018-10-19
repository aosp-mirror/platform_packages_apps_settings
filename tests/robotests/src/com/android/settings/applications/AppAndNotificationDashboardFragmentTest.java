/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import com.android.settings.notification.EmergencyBroadcastPreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class AppAndNotificationDashboardFragmentTest {

    @Test
    @Config(shadows = {ShadowEmergencyBroadcastPreferenceController.class})
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = spy(RuntimeEnvironment.application);
        UserManager manager = mock(UserManager.class);
        when(manager.isAdminUser()).thenReturn(true);
        when(context.getSystemService(Context.USER_SERVICE)).thenReturn(manager);
        final List<String> niks = AppAndNotificationDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);
        AppAndNotificationDashboardFragment fragment = new AppAndNotificationDashboardFragment();
        final int xmlId = fragment.getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlId);

        assertThat(keys).containsAllIn(niks);
    }

    @Implements(EmergencyBroadcastPreferenceController.class)
    public static class ShadowEmergencyBroadcastPreferenceController {

        @Implementation
        public boolean isAvailable() {
            return true;
        }
    }
}
