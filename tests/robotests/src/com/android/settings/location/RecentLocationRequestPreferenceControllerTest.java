/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.location;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.location.RecentLocationApps;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class RecentLocationRequestPreferenceControllerTest {
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mCategory;
    private Context mContext;
    private RecentLocationRequestPreferenceController mController;
    private ShadowUserManager mUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = spy(
                new RecentLocationRequestPreferenceController(mContext, "key"));
        when(mCategory.getContext()).thenReturn(mContext);
        when(mScreen.findPreference("key")).thenReturn(mCategory);
        mUserManager = ShadowUserManager.getShadow();
        mController.mRecentLocationApps = spy(new RecentLocationApps(mContext));
    }

    @Test
    public void updateState_whenAppListMoreThanThree_shouldDisplayTopThreeApps() {
        final List<RecentLocationApps.Request> requests = createMockRequest(6);
        when(mController.mRecentLocationApps.getAppListSorted(false)).thenReturn(requests);

        mController.displayPreference(mScreen);

        verify(mCategory, times(3)).addPreference(any());
    }

    @Test
    public void updateState_workProfile_shouldShowOnlyWorkProfileApps() {
        final List<RecentLocationApps.Request> requests = createMockRequest(6);
        when(mController.mRecentLocationApps.getAppListSorted(false)).thenReturn(requests);
        mController.setProfileType(ProfileSelectFragment.ProfileType.WORK);
        final Set<Integer> profileIds = new HashSet<>();
        profileIds.add(4);
        profileIds.add(5);
        mUserManager.setManagedProfiles(profileIds);

        mController.displayPreference(mScreen);

        // contains userId 4 and userId 5
        verify(mCategory, times(2)).addPreference(any());
    }

    @Test
    public void updateState_Personal_shouldShowOnlyPersonalApps() {
        final List<RecentLocationApps.Request> requests = createMockRequest(6);
        when(mController.mRecentLocationApps.getAppListSorted(false)).thenReturn(requests);
        mController.setProfileType(ProfileSelectFragment.ProfileType.PERSONAL);
        final Set<Integer> profileIds = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            profileIds.add(i);
        }
        mUserManager.setManagedProfiles(profileIds);

        mController.displayPreference(mScreen);

        // contains userId 4 and userId 5
        verify(mCategory, times(2)).addPreference(any());
    }

    private List<RecentLocationApps.Request> createMockRequest(int count) {
        final List<RecentLocationApps.Request> requests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final Drawable icon = mock(Drawable.class);
            // Add mock accesses
            final RecentLocationApps.Request request = new RecentLocationApps.Request(
                    "packageName", UserHandle.of(i), icon,
                    "appTitle" + i, false, "appSummary" + i, 1000 - i);
            requests.add(request);
        }
        return requests;
    }
}
