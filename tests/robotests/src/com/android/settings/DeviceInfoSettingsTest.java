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

import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.settingslib.DeviceInfoUtils;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DeviceInfoSettingsTest extends AndroidTestCase {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;

    private FakeFeatureFactory mFeatureFactory;
    private DeviceInfoSettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        mSettings = spy(new DeviceInfoSettings());
        doReturn(mScreen).when(mSettings).getPreferenceScreen();
    }

    @Test
    public void testGetFormattedKernelVersion() throws Exception {
        if ("Unavailable".equals(DeviceInfoUtils.getFormattedKernelVersion())) {
            fail("formatKernelVersion can't cope with this device's /proc/version");
        }
    }

    @Test
    public void testFormatKernelVersion() throws Exception {
        assertEquals("Unavailable", DeviceInfoUtils.formatKernelVersion(""));
        assertEquals("2.6.38.8-gg784\n" +
                        "root@hpao4.eem.corp.google.com #2\n" +
                        "Fri Feb 24 03:31:23 PST 2012",
                DeviceInfoUtils.formatKernelVersion("Linux version 2.6.38.8-gg784 " +
                        "(root@hpao4.eem.corp.google.com) " +
                        "(gcc version 4.4.3 (Ubuntu 4.4.3-4ubuntu5) ) #2 SMP " +
                        "Fri Feb 24 03:31:23 PST 2012"));
        assertEquals("3.0.31-g6fb96c9\n" +
                        "android-build@vpbs1.mtv.corp.google.com #1\n" +
                        "Thu Jun 28 11:02:39 PDT 2012",
                DeviceInfoUtils.formatKernelVersion("Linux version 3.0.31-g6fb96c9 " +
                        "(android-build@vpbs1.mtv.corp.google.com) " +
                        "(gcc version 4.6.x-google 20120106 (prerelease) (GCC) ) #1 " +
                        "SMP PREEMPT Thu Jun 28 11:02:39 PDT 2012"));
        assertEquals("2.6.38.8-a-b-jellybean+\n" +
                        "x@y #1\n" +
                        "Tue Aug 28 22:10:46 CDT 2012",
                DeviceInfoUtils.formatKernelVersion("Linux version " +
                        "2.6.38.8-a-b-jellybean+ (x@y) " +
                        "(gcc version 4.4.3 (GCC) ) #1 PREEMPT Tue Aug 28 22:10:46 CDT 2012"));
        assertEquals("3.18.31-g3ce5faa-dirty\n" +
                        "x@y #5\n" +
                        "Fri Oct 28 14:38:13 PDT 2016",
                DeviceInfoUtils.formatKernelVersion("Linux version " +
                        "3.18.31-g3ce5faa-dirty (x@y) (Android clang " +
                        "version 3.8.275480 (based on LLVM 3.8.275480)) " +
                        "#5 SMP PREEMPT Fri Oct 28 14:38:13 PDT 2016"));
    }

    @Test
    public void testShowSystemUpdatesWhenIADisabled() throws Exception {
        when(mFeatureFactory.dashboardFeatureProvider.isEnabled()).thenReturn(true);
        mSettings.displaySystemUpdates(mContext);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void testHideSystemUpdatesWhenIAEnabled() throws Exception {
        when(mFeatureFactory.dashboardFeatureProvider.isEnabled()).thenReturn(false);
        mSettings.displaySystemUpdates(mContext);

        verify(mScreen, never()).removePreference(any(Preference.class));
    }
}
