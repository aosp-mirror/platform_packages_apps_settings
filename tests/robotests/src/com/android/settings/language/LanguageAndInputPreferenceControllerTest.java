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
 * limitations under the License
 */

package com.android.settings.language;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;

import com.android.settings.testutils.shadow.ShadowInputMethodManagerWithMethodList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowInputMethodManagerWithMethodList.class)
public class LanguageAndInputPreferenceControllerTest {
    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
    }

    @Test
    public void getSummary_shouldSetToCurrentImeName() {
        final ComponentName componentName = new ComponentName("name1", "cls");
        final ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putString(cr, Settings.Secure.DEFAULT_INPUT_METHOD,
                componentName.flattenToString());
        final List<InputMethodInfo> imis = new ArrayList<>();
        imis.add(mock(InputMethodInfo.class));
        when(imis.get(0).getPackageName()).thenReturn("name1");
        when(imis.get(0).loadLabel(any())).thenReturn("label");
        ShadowInputMethodManagerWithMethodList.getShadow().setInputMethodList(imis);

        final LanguageAndInputPreferenceController controller =
                new LanguageAndInputPreferenceController(mContext, "key");

        assertThat(controller.getSummary().toString()).contains("label");
    }
}
