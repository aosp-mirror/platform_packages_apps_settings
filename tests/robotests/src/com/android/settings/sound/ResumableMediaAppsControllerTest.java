/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.sound;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.ResolveInfoBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ResumableMediaAppsControllerTest {

    private static final String KEY = "media_controls_resumable_apps";

    private static final String FAKE_APP = "com.test.fakeapp1";

    private Context mContext;
    private int mOriginalQs;
    private int mOriginalResume;
    private String mOriginalBlocked;
    private ContentResolver mContentResolver;
    private ResumableMediaAppsController mController;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceGroup mPreferenceGroup;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        mOriginalQs = Settings.Global.getInt(mContentResolver,
                Settings.Global.SHOW_MEDIA_ON_QUICK_SETTINGS, 1);
        mOriginalResume = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.MEDIA_CONTROLS_RESUME, 1);
        mOriginalBlocked = Settings.Secure.getString(mContentResolver,
                Settings.Secure.MEDIA_CONTROLS_RESUME_BLOCKED);

        // Start all tests with feature enabled, nothing blocked
        Settings.Global.putInt(mContentResolver, Settings.Global.SHOW_MEDIA_ON_QUICK_SETTINGS, 1);
        Settings.Secure.putInt(mContentResolver, Settings.Secure.MEDIA_CONTROLS_RESUME, 1);
        Settings.Secure.putString(mContentResolver, Settings.Secure.MEDIA_CONTROLS_RESUME_BLOCKED,
                mOriginalBlocked);

        mPreferenceScreen = mock(PreferenceScreen.class);
        mPreferenceGroup = mock(PreferenceGroup.class);
        mPackageManager = mock(PackageManager.class);

        List<ResolveInfo> fakeInfo = new ArrayList<>();
        fakeInfo.add(createResolveInfo(FAKE_APP));
        when(mPackageManager.queryIntentServices(any(), anyInt())).thenReturn(fakeInfo);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPreferenceScreen.findPreference(KEY)).thenReturn(mPreferenceGroup);

        mController = new ResumableMediaAppsController(mContext, KEY);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContentResolver, Settings.Global.SHOW_MEDIA_ON_QUICK_SETTINGS,
                mOriginalQs);
        Settings.Secure.putInt(mContentResolver, Settings.Secure.MEDIA_CONTROLS_RESUME,
                mOriginalResume);
        Settings.Secure.putString(mContentResolver, Settings.Secure.MEDIA_CONTROLS_RESUME_BLOCKED,
                mOriginalBlocked);
    }

    @Test
    public void getAvailability_hasEligibleApps_isAvailable() {
        // The package manager already has an eligible app from setUp()
        assertEquals(AVAILABLE, mController.getAvailabilityStatus());
    }

    @Test
    public void getAvailability_noEligibleApps_isConditionallyUnavailable() {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        List<ResolveInfo> fakeInfo = new ArrayList<>();
        when(packageManager.queryIntentServices(any(), anyInt())).thenReturn(fakeInfo);
        when(context.getPackageManager()).thenReturn(packageManager);
        ResumableMediaAppsController controller = new ResumableMediaAppsController(context, KEY);

        assertEquals(CONDITIONALLY_UNAVAILABLE, controller.getAvailabilityStatus());
    }

    @Test
    public void displayPreference_addsApps() {
        mController.displayPreference(mPreferenceScreen);
        verify(mPreferenceGroup, times(1)).addPreference(any());
    }

    @Test
    public void unblockedApp_isChecked() {
        ArgumentCaptor<ResumableMediaAppsController.MediaSwitchPreference> argument =
                ArgumentCaptor.forClass(ResumableMediaAppsController.MediaSwitchPreference.class);
        mController.displayPreference(mPreferenceScreen);
        verify(mPreferenceGroup).addPreference(argument.capture());
        assertTrue(argument.getValue().isChecked());
    }

    @Test
    public void blockedApp_isNotChecked() {
        Settings.Secure.putString(mContentResolver, Settings.Secure.MEDIA_CONTROLS_RESUME_BLOCKED,
                FAKE_APP);

        ArgumentCaptor<ResumableMediaAppsController.MediaSwitchPreference> argument =
                ArgumentCaptor.forClass(ResumableMediaAppsController.MediaSwitchPreference.class);
        mController.displayPreference(mPreferenceScreen);
        verify(mPreferenceGroup).addPreference(argument.capture());

        assertFalse(argument.getValue().isChecked());
    }

    private ResolveInfo createResolveInfo(String name) {
        ResolveInfoBuilder builder = new ResolveInfoBuilder(name);
        builder.setActivity(name, name);
        return builder.build();
    }
}
