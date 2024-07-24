/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.localepicker;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class LocaleHelperPreferenceControllerTest {
    private Context mContext;
    private LocaleHelperPreferenceController mLocaleHelperPreferenceController;
    private FakeFeatureFactory mFeatureFactory;

    @Mock
    private FooterPreference mMockFooterPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = ApplicationProvider.getApplicationContext();
        mLocaleHelperPreferenceController = new LocaleHelperPreferenceController(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void updateFooterPreference_setFooterPreference_hasClickAction() {
        mLocaleHelperPreferenceController.updateFooterPreference(mMockFooterPreference);
        verify(mMockFooterPreference).setLearnMoreText(anyString());
        mMockFooterPreference.setLearnMoreAction(v -> {
            verify(mFeatureFactory.metricsFeatureProvider).action(
                    mContext, SettingsEnums.ACTION_LANGUAGES_LEARN_MORE);
        });
    }
}
