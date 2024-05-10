/**
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

package com.android.settings.regionalpreferences;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class RegionalFooterPreferenceControllerTest {

    private static String KEY_FOOTER_PREFERENCE = "regional_pref_footer";
    private Context mContext;
    private RegionalFooterPreferenceController mRegionalFooterPreferenceController;

    @Mock
    private FooterPreference mMockFooterPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = ApplicationProvider.getApplicationContext();
        mRegionalFooterPreferenceController = new RegionalFooterPreferenceController(mContext,
                KEY_FOOTER_PREFERENCE);
    }

    @Test
    public void setupFooterPreference_shouldSetAsTextInLearnMore() {
        mRegionalFooterPreferenceController.setupFooterPreference(mMockFooterPreference);
        verify(mMockFooterPreference).setLearnMoreText(anyString());
    }
}
