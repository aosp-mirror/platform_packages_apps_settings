/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class SatelliteSettingsSosFooterPreferenceControllerTest {
    private static final String KEY = "key_satellite_sos_footer";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private FooterPreference mFooterPreference;

    private Context mContext = null;
    private SatelliteSettingsSosFooterPreferenceController mController;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mFooterPreference.getKey()).thenReturn(KEY);
        mController = new SatelliteSettingsSosFooterPreferenceController(mContext, KEY);
    }

    @Test
    public void updateState_setLearnMoreAction_success() {

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setLearnMoreAction(any());
    }
}
