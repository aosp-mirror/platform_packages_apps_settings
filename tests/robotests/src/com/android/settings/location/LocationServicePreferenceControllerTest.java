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
package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class LocationServicePreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocationSettings mFragment;
    @Mock
    private PreferenceCategory mCategory;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SettingsInjector mSettingsInjector;

    private Context mContext;
    private LocationServicePreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(new LocationServicePreferenceController(
                mContext, mFragment, mLifecycle, mSettingsInjector));
        final String key = mController.getPreferenceKey();
        when(mScreen.findPreference(key)).thenReturn(mCategory);
        when(mCategory.getKey()).thenReturn(key);
    }

    @Test
    public void isAvailable_noInjectedSettings_shouldReturnFalse() {
        doReturn(false).when(mSettingsInjector).hasInjectedSettings(anyInt());

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_hasInjectedSettings_shouldReturnFalse() {
        doReturn(true).when(mSettingsInjector).hasInjectedSettings(anyInt());

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onResume_shouldRegisterListener() {
        mController.onResume();

        verify(mContext).registerReceiver(eq(mController.mInjectedSettingsReceiver),
                eq(mController.INTENT_FILTER_INJECTED_SETTING_CHANGED));
    }

    @Test
    public void onPause_shouldUnregisterListener() {
        mController.onResume();
        mController.onPause();

        verify(mContext).unregisterReceiver(mController.mInjectedSettingsReceiver);
    }

    @Test
    public void updateState_shouldRemoveAllAndAddInjectedSettings() {
        final List<Preference> preferences = new ArrayList<>();
        final Preference pref1 = new Preference(mContext);
        pref1.setTitle("Title1");
        final Preference pref2 = new Preference(mContext);
        pref2.setTitle("Title2");
        preferences.add(pref1);
        preferences.add(pref2);
        doReturn(preferences)
            .when(mSettingsInjector).getInjectedSettings(any(Context.class), anyInt());
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        mController.displayPreference(mScreen);

        mController.updateState(mCategory);

        verify(mCategory).removeAll();
        verify(mCategory).addPreference(pref1);
        verify(mCategory).addPreference(pref2);
    }

    @Test
    public void onLocationModeChanged_shouldRequestReloadInjectedSettigns() {
        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mSettingsInjector).reloadStatusMessages();
    }
}
