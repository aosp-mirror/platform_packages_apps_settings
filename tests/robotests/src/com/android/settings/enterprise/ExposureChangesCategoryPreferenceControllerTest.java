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

package com.android.settings.enterprise;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.DynamicAvailabilityPreferenceController;
import com.android.settings.core.PreferenceAvailabilityObserver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ExposureChangesCategoryPreferenceController}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class ExposureChangesCategoryPreferenceControllerTest {

    private static final String KEY_1 = "key_1";
    private static final String KEY_2 = "key_2";
    private static final String KEY_EXPOSURE_CHANGES_CATEGORY = "exposure_changes_category";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private List<DynamicAvailabilityPreferenceController> mControllers;
    private ExposureChangesCategoryPreferenceController mController;
    @Mock private PreferenceAvailabilityObserver mObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mControllers = Arrays.asList(mock(DynamicAvailabilityPreferenceController.class),
                mock(DynamicAvailabilityPreferenceController.class));
        mController = new ExposureChangesCategoryPreferenceController(mContext,
                null /* lifecycle */, mControllers, true /* controllingUi */);
        mController.setAvailabilityObserver(mObserver);
    }

    @Test
    public void testInitialization() {
        verify(mControllers.get(0)).setAvailabilityObserver(mController);
        verify(mControllers.get(1)).setAvailabilityObserver(mController);
    }

    @Test
    public void testGetAvailabilityObserver() {
        assertThat(mController.getAvailabilityObserver()).isEqualTo(mObserver);
    }

    @Test
    public void testOnPreferenceAvailabilityUpdated() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(true);

        mController.updateState(preference);
        assertThat(preference.isVisible()).isFalse();

        mController.onPreferenceAvailabilityUpdated(KEY_1, true);
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_EXPOSURE_CHANGES_CATEGORY, true);
        assertThat(preference.isVisible()).isTrue();
        reset(mObserver);

        mController.onPreferenceAvailabilityUpdated(KEY_2, true);
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_EXPOSURE_CHANGES_CATEGORY, true);
        assertThat(preference.isVisible()).isTrue();
        reset(mObserver);

        mController.onPreferenceAvailabilityUpdated(KEY_1, false);
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_EXPOSURE_CHANGES_CATEGORY, true);
        assertThat(preference.isVisible()).isTrue();
        reset(mObserver);

        mController.onPreferenceAvailabilityUpdated(KEY_2, false);
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_EXPOSURE_CHANGES_CATEGORY, false);
        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(false);

        mController.onPreferenceAvailabilityUpdated(KEY_1, true);
        mController.updateState(preference);
        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    public void testIsAvailableForUi() {
        assertThat(mController.isAvailable()).isTrue();
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(
                eq(KEY_EXPOSURE_CHANGES_CATEGORY), anyBoolean());

        mController.onPreferenceAvailabilityUpdated(KEY_1, true);
        reset(mObserver);
        assertThat(mController.isAvailable()).isTrue();
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(
                eq(KEY_EXPOSURE_CHANGES_CATEGORY), anyBoolean());

        mController.onPreferenceAvailabilityUpdated(KEY_1, false);
        reset(mObserver);
        assertThat(mController.isAvailable()).isTrue();
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(
                eq(KEY_EXPOSURE_CHANGES_CATEGORY), anyBoolean());
    }

    @Test
    public void testIsAvailableForSearch() {
        final ExposureChangesCategoryPreferenceController controller
                = new ExposureChangesCategoryPreferenceController(mContext, null /* lifecycle */,
                        mControllers, false /* controllingUi */);
        controller.setAvailabilityObserver(mObserver);
        verify(mControllers.get(0)).setAvailabilityObserver(controller);
        verify(mControllers.get(1)).setAvailabilityObserver(controller);

        assertThat(controller.isAvailable()).isFalse();
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_EXPOSURE_CHANGES_CATEGORY, false);
        reset(mObserver);

        controller.onPreferenceAvailabilityUpdated(KEY_1, true);
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(
                eq(KEY_EXPOSURE_CHANGES_CATEGORY), anyBoolean());
        assertThat(controller.isAvailable()).isTrue();
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_EXPOSURE_CHANGES_CATEGORY, true);
        reset(mObserver);

        controller.onPreferenceAvailabilityUpdated(KEY_2, true);
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(
                eq(KEY_EXPOSURE_CHANGES_CATEGORY), anyBoolean());
        assertThat(controller.isAvailable()).isTrue();
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_EXPOSURE_CHANGES_CATEGORY, true);
        reset(mObserver);

        controller.onPreferenceAvailabilityUpdated(KEY_1, false);
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(
                eq(KEY_EXPOSURE_CHANGES_CATEGORY), anyBoolean());
        assertThat(controller.isAvailable()).isTrue();
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_EXPOSURE_CHANGES_CATEGORY, true);
        reset(mObserver);

        controller.onPreferenceAvailabilityUpdated(KEY_2, false);
        verify(mObserver, never()).onPreferenceAvailabilityUpdated(
                eq(KEY_EXPOSURE_CHANGES_CATEGORY), anyBoolean());
        assertThat(controller.isAvailable()).isFalse();
        verify(mObserver).onPreferenceAvailabilityUpdated(KEY_EXPOSURE_CHANGES_CATEGORY, false);
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(KEY_EXPOSURE_CHANGES_CATEGORY);
    }
}
