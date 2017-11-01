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

package com.android.settings.support;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.res.builder.RobolectricPackageManager;

import static com.android.settings.support.NewDeviceIntroSuggestionActivity
        .PERMANENT_DISMISS_THRESHOLD;
import static com.android.settings.support.NewDeviceIntroSuggestionActivity
        .PREF_KEY_SUGGGESTION_COMPLETE;
import static com.android.settings.support.NewDeviceIntroSuggestionActivity
        .PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME;
import static com.android.settings.support.NewDeviceIntroSuggestionActivity.isSuggestionComplete;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NewDeviceIntroSuggestionActivityTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mMockContext;

    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private RobolectricPackageManager mRobolectricPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest(mMockContext);
        mContext = RuntimeEnvironment.application;
        mRobolectricPackageManager = RuntimeEnvironment.getRobolectricPackageManager();

        when(mFeatureFactory.suggestionsFeatureProvider.getSharedPrefs(any(Context.class)))
                .thenReturn(getSharedPreferences());
    }

    @Test
    public void isSuggestionComplete_notSupported_shouldReturnTrue() {
        when(mMockContext.getResources()
                .getBoolean(R.bool.config_new_device_intro_suggestion_supported))
                .thenReturn(false);

        assertThat(isSuggestionComplete(mContext))
                .isTrue();
    }

    @Test
    public void isSuggestionComplete_suggestionExpired_shouldReturnTrue() {
        final long currentTime = System.currentTimeMillis();

        getSharedPreferences().edit().putLong(PREF_KEY_SUGGGESTION_FIRST_DISPLAY_TIME,
                currentTime - 2 * PERMANENT_DISMISS_THRESHOLD);
        assertThat(isSuggestionComplete(mContext))
                .isTrue();
    }

    @Test
    public void isSuggestionComplete_noUrl_shouldReturnTrue() {
        when(mFeatureFactory.supportFeatureProvider.getNewDeviceIntroUrl(any(Context.class)))
                .thenReturn(null);
        assertThat(isSuggestionComplete(mContext))
                .isTrue();
    }

    @Test
    public void isSuggestionComplete_alreadyLaunchedBefore_shouldReturnTrue() {
        when(mFeatureFactory.supportFeatureProvider.getNewDeviceIntroUrl(any(Context.class)))
                .thenReturn("https://com.android.settings");

        getSharedPreferences().edit().putBoolean(PREF_KEY_SUGGGESTION_COMPLETE, true).commit();

        assertThat(isSuggestionComplete(mContext))
                .isTrue();
    }

    @Test
    public void isSuggestionComplete_notExpiredAndCanOpenUrlInBrowser_shouldReturnFalse() {
        when(mFeatureFactory.supportFeatureProvider.getNewDeviceIntroUrl(any(Context.class)))
                .thenReturn("https://com.android.settings");

        final Intent intent = NewDeviceIntroSuggestionActivity.getLaunchIntent(mContext);
        mRobolectricPackageManager.addResolveInfoForIntent(intent, new ResolveInfo());
        assertThat(isSuggestionComplete(mContext)).isFalse();
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences("test_new_device_sugg", Context.MODE_PRIVATE);
    }

}
