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
package com.android.settings.accessibility;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.setupdesign.GlifPreferenceLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilitySetupWizardUtils} */
@RunWith(RobolectricTestRunner.class)
public class AccessibilitySetupWizardUtilsTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void setupGlifPreferenceLayout_assignValueToVariable() {
        final String title = "title";
        final String description = "description";
        final Drawable icon = mock(Drawable.class);
        GlifPreferenceLayout layout = mock(GlifPreferenceLayout.class);

        AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(mContext, layout, title,
                description, icon);

        verify(layout).setHeaderText(title);
        verify(layout).setDescriptionText(description);
        verify(layout).setIcon(icon);
        verify(layout).setHeaderText(title);
    }

    @Test
    public void setupGlifPreferenceLayout_descriptionIsNull_doesNotCallSetDescriptionText() {
        GlifPreferenceLayout layout = mock(GlifPreferenceLayout.class);

        AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(mContext, layout, "title",
                /* description= */ null, /* icon= */ null);

        verify(layout, times(0)).setDescriptionText(any());
    }
}
