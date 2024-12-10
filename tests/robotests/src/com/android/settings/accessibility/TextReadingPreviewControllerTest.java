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

package com.android.settings.accessibility;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.display.PreviewPagerAdapter;
import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowChoreographer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link TextReadingPreviewController}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowChoreographer.class, ShadowInteractionJankMonitor.class})
public class TextReadingPreviewControllerTest {
    private static final String PREVIEW_KEY = "preview";
    private static final String FONT_SIZE_KEY = "font_size";
    private static final String DISPLAY_SIZE_KEY = "display_size";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TextReadingPreviewController mPreviewController;
    private TextReadingPreviewPreference mPreviewPreference;
    private AccessibilitySeekBarPreference mFontSizePreference;
    private AccessibilitySeekBarPreference mDisplaySizePreference;

    @Mock
    private DisplaySizeData mDisplaySizeData;

    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final FontSizeData fontSizeData = new FontSizeData(mContext);
        final List<Integer> displayData = createFakeDisplayData();
        when(mDisplaySizeData.getValues()).thenReturn(displayData);
        mPreviewPreference = spy(new TextReadingPreviewPreference(mContext, /* attr= */ null));
        mPreviewController = new TextReadingPreviewController(mContext, PREVIEW_KEY, fontSizeData,
                mDisplaySizeData);
        mFontSizePreference = new AccessibilitySeekBarPreference(mContext, /* attr= */ null);
        mDisplaySizePreference = new AccessibilitySeekBarPreference(mContext, /* attr= */ null);
    }

    @Test
    public void initPreviewerAdapter_verifyAction() {
        when(mPreferenceScreen.findPreference(PREVIEW_KEY)).thenReturn(mPreviewPreference);
        when(mPreferenceScreen.findPreference(FONT_SIZE_KEY)).thenReturn(mFontSizePreference);
        when(mPreferenceScreen.findPreference(DISPLAY_SIZE_KEY)).thenReturn(mDisplaySizePreference);

        mPreviewController.displayPreference(mPreferenceScreen);

        verify(mPreviewPreference).setPreviewAdapter(any(PreviewPagerAdapter.class));
    }

    @Test(expected = NullPointerException.class)
    public void initPreviewerAdapterWithoutDisplaySizePreference_throwNPE() {
        when(mPreferenceScreen.findPreference(PREVIEW_KEY)).thenReturn(mPreviewPreference);
        when(mPreferenceScreen.findPreference(DISPLAY_SIZE_KEY)).thenReturn(mDisplaySizePreference);

        mPreviewController.displayPreference(mPreferenceScreen);

        verify(mPreviewPreference).setPreviewAdapter(any(PreviewPagerAdapter.class));
    }

    @Test(expected = NullPointerException.class)
    public void initPreviewerAdapterWithoutFontSizePreference_throwNPE() {
        when(mPreferenceScreen.findPreference(PREVIEW_KEY)).thenReturn(mPreviewPreference);
        when(mPreferenceScreen.findPreference(FONT_SIZE_KEY)).thenReturn(mFontSizePreference);

        mPreviewController.displayPreference(mPreferenceScreen);

        verify(mPreviewPreference).setPreviewAdapter(any(PreviewPagerAdapter.class));
    }

    private List<Integer> createFakeDisplayData() {
        final List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);

        return list;
    }
}
