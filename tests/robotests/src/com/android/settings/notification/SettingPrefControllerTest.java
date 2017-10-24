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

package com.android.settings.notification;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings.Global;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.android.settings.notification.SettingPref.TYPE_GLOBAL;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SettingPrefControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SoundSettings mSetting;
    @Mock
    private Activity mActivity;
    @Mock
    private ContentResolver mContentResolver;

    private Context mContext;
    private PreferenceControllerTestable mController;
    private SettingPref mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ShadowApplication.getInstance().getApplicationContext());
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        when(mSetting.getActivity()).thenReturn(mActivity);
        doReturn(mScreen).when(mSetting).getPreferenceScreen();
        mController = new PreferenceControllerTestable(mContext, mSetting, null);
        mPreference = mController.getPref();
    }

    @Test
    public void displayPreference_shouldInitPreference() {
        mController.displayPreference(mScreen);

        verify(mPreference).init(mSetting);
    }

    @Test
    public void isAvailable_shouldCallisApplicable() {
        mController.isAvailable();

        verify(mPreference).isApplicable(mContext);
    }

    @Test
    public void getPreferenceKey_shouldReturnPrefKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(mController.KEY_TEST);
    }

    @Test
    public void updateState_shouldUpdatePreference() {
        mController.updateState(null);

        verify(mPreference).update(mContext);
    }

    @Test
    public void onResume_shouldRegisterContentObserver() {
        mController.displayPreference(mScreen);
        mController.onResume();

        verify(mContentResolver).registerContentObserver(
            Global.getUriFor("Setting1"), false, mController.getObserver());
    }

    @Test
    public void onPause_shouldUnregisterContentObserver() {
        mController.displayPreference(mScreen);
        mController.onPause();

        verify(mContentResolver).unregisterContentObserver(mController.getObserver());
    }

    @Test
    public void onContentChange_shouldUpdatePreference() {
        mController.displayPreference(mScreen);
        mController.onResume();
        mController.getObserver().onChange(false, Global.getUriFor("Setting1"));

        verify(mPreference).update(mContext);
    }

    @Test
    public void updateNonIndexableKeys_applicable_shouldNotUpdate() {
        final List<String> keys = new ArrayList<>();

        mController.updateNonIndexableKeys(keys);

        assertThat(keys).isEmpty();
    }

    @Test
    public void updateNonIndexableKeys_notApplicable_shouldUpdate() {
        mController.setApplicable(false);
        final List<String> keys = new ArrayList<>();

        mController.updateNonIndexableKeys(keys);

        assertThat(keys).isNotEmpty();
    }

    private class PreferenceControllerTestable extends SettingPrefController {

        private static final String KEY_TEST = "key1";
        private boolean mApplicable = true;

        public PreferenceControllerTestable(Context context, SettingsPreferenceFragment parent,
            Lifecycle lifecycle) {
            super(context, parent, lifecycle);
            mPreference = spy(new SettingPref(
                TYPE_GLOBAL, KEY_TEST, "Setting1", 1) {
                @Override
                public boolean isApplicable(Context context) {
                    return mApplicable;
                }
            });
        }

        SettingPref getPref() {
            return mPreference;
        }

        PreferenceControllerTestable.SettingsObserver getObserver() {
            return mSettingsObserver;
        }

        void setApplicable(boolean applicable) {
            mApplicable = applicable;
        }

    }

}
