/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.location;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.LifecycleOwner;
import android.content.Context;
import android.provider.Settings.Secure;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.widget.AppPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.location.RecentLocationApps;
import com.android.settingslib.location.RecentLocationApps.Request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;

/** Unit tests for {@link RecentLocationRequestSeeAllPreferenceController} */
@RunWith(SettingsRobolectricTestRunner.class)
public class RecentLocationRequestSeeAllPreferenceControllerTest {

    @Mock
    RecentLocationRequestSeeAllFragment mFragment;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mCategory;
    @Mock
    private RecentLocationApps mRecentLocationApps;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private RecentLocationRequestSeeAllPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(
                new RecentLocationRequestSeeAllPreferenceController(
                        mContext, mLifecycle, mFragment, mRecentLocationApps));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mCategory);
        final String key = mController.getPreferenceKey();
        when(mCategory.getKey()).thenReturn(key);
        when(mCategory.getContext()).thenReturn(mContext);
    }

    @Test
    public void onLocationModeChanged_locationOn_shouldEnablePreference() {
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Secure.LOCATION_MODE_HIGH_ACCURACY, false);

        verify(mCategory).setEnabled(true);
    }

    @Test
    public void onLocationModeChanged_locationOff_shouldDisablePreference() {
        mController.displayPreference(mScreen);

        mController.onLocationModeChanged(Secure.LOCATION_MODE_OFF, false);

        verify(mCategory).setEnabled(false);
    }

    @Test
    public void updateState_shouldRemoveAll() {
        doReturn(Collections.EMPTY_LIST).when(mRecentLocationApps).getAppListSorted();

        mController.displayPreference(mScreen);
        mController.updateState(mCategory);

        verify(mCategory).removeAll();
    }

    @Test
    public void updateState_hasRecentLocationRequest_shouldAddPreference() {
        Request request = mock(Request.class);
        AppPreference appPreference = mock(AppPreference.class);
        doReturn(appPreference)
                .when(mController).createAppPreference(any(Context.class), eq(request));
        when(mRecentLocationApps.getAppListSorted())
                .thenReturn(new ArrayList<>(Collections.singletonList(request)));

        mController.displayPreference(mScreen);
        mController.updateState(mCategory);

        verify(mCategory).removeAll();
        verify(mCategory).addPreference(appPreference);
    }
}
