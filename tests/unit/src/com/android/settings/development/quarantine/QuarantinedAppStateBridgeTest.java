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

package com.android.settings.development.quarantine;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class QuarantinedAppStateBridgeTest {
    private static final String TEST_PACKAGE = "com.example.test.pkg";
    private static final int TEST_APP_ID = 1234;
    private static final int TEST_USER_ID_1 = 0;
    private static final int TEST_USER_ID_2 = 10;

    @Mock
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void updateExtraInfo_packageQuarantined() throws Exception {
        setPackageQuarantined(TEST_PACKAGE, TEST_USER_ID_1, false);
        setPackageQuarantined(TEST_PACKAGE, TEST_USER_ID_2, true);

        final QuarantinedAppStateBridge bridge =
                new QuarantinedAppStateBridge(mContext, null, null);
        final AppEntry entry = mock(AppEntry.class);

        bridge.updateExtraInfo(entry, TEST_PACKAGE, UserHandle.getUid(TEST_USER_ID_2, TEST_APP_ID));
        assertThat(entry.extraInfo).isEqualTo(true);
    }

    @Test
    public void updateExtraInfo_packageNotQuarantined() throws Exception {
        setPackageQuarantined(TEST_PACKAGE, TEST_USER_ID_1, false);
        setPackageQuarantined(TEST_PACKAGE, TEST_USER_ID_2, false);

        final QuarantinedAppStateBridge bridge =
                new QuarantinedAppStateBridge(mContext, null, null);
        final AppEntry entry = mock(AppEntry.class);

        bridge.updateExtraInfo(entry, TEST_PACKAGE, UserHandle.getUid(TEST_USER_ID_2, TEST_APP_ID));
        assertThat(entry.extraInfo).isEqualTo(false);
    }

    private void setPackageQuarantined(String packageName, int userId, boolean quarantined)
            throws Exception {
        final Context userContext = mock(Context.class);
        when(mContext.createContextAsUser(eq(UserHandle.of(userId)), anyInt()))
                .thenReturn(userContext);
        final PackageManager packageManager = mock(PackageManager.class);
        when(userContext.getPackageManager()).thenReturn(packageManager);
        when(packageManager.isPackageQuarantined(packageName)).thenReturn(quarantined);
    }
}
