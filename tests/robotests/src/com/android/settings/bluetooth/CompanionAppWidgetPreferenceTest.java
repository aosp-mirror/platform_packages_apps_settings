/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class})
public class CompanionAppWidgetPreferenceTest {
    private static final String TITLE_ONE = "Test Title 1";
    private static final String TITLE_TWO = "Test Title 1";
    private static final String KEY_ONE = "Test Key 1";
    private static final String KEY_TWO = "Test Key 1";

    private Context mContext;
    private Drawable mWidgetIconOne;
    private Drawable mWidgetIconTwo;
    private Drawable mAppIconOne;
    private Drawable mAppIconTwo;

    @Mock
    private View.OnClickListener mWidgetListenerOne;
    @Mock
    private View.OnClickListener mWidgetListenerTwo;

    private List<CompanionAppWidgetPreference> mPreferenceContainer;

    @Before
    public void setUp() {
        mPreferenceContainer = new ArrayList<>();
        Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        mContext = new ContextThemeWrapper(context, R.style.Theme_Settings);
        mWidgetIconOne = mock(Drawable.class);
        mAppIconOne = mock(Drawable.class);
        mWidgetListenerOne = mock(View.OnClickListener.class);
        mWidgetIconTwo = mock(Drawable.class);
        mAppIconTwo = mock(Drawable.class);
        mWidgetListenerTwo = mock(View.OnClickListener.class);
    }

    private void setUpPreferenceContainer(Drawable widgetIcon, Drawable appIcon,
            View.OnClickListener listener, String title, String key) {
        CompanionAppWidgetPreference preference = new CompanionAppWidgetPreference(
                widgetIcon, listener, mContext);
        preference.setIcon(appIcon);
        preference.setTitle(title);
        preference.setKey(key);
        mPreferenceContainer.add(preference);
    }

    @Test
    public void setUpPreferenceContainer_preferenceShouldBeAdded() {
        setUpPreferenceContainer(
                mWidgetIconOne, mAppIconOne, mWidgetListenerOne, TITLE_ONE, KEY_ONE);

        assertThat(mPreferenceContainer.get(0).getIcon()).isEqualTo(mAppIconOne);
        assertThat(mPreferenceContainer.get(0).getKey()).isEqualTo(KEY_ONE);
        assertThat(mPreferenceContainer.get(0).getTitle()).isEqualTo(TITLE_ONE);

        setUpPreferenceContainer(
                mWidgetIconTwo, mAppIconTwo, mWidgetListenerTwo, TITLE_TWO, KEY_TWO);

        assertThat(mPreferenceContainer.get(1).getIcon()).isEqualTo(mAppIconTwo);
        assertThat(mPreferenceContainer.get(1).getKey()).isEqualTo(KEY_TWO);
        assertThat(mPreferenceContainer.get(1).getTitle()).isEqualTo(TITLE_TWO);
    }
}
