/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.provider.SettingsSlicesContract;
import android.util.Pair;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeInvalidSliderController;
import com.android.settings.testutils.FakeSliderController;
import com.android.settings.testutils.FakeToggleController;
import com.android.settings.testutils.FakeUnavailablePreferenceController;
import com.android.settings.testutils.SliceTester;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowRestrictedLockUtilsInternal.class)
public class SliceBuilderUtilsTest {

    private static final String KEY = "KEY";
    private static final String TITLE = "title";
    private static final String SUMMARY = "summary";
    private static final String SCREEN_TITLE = "screen title";
    private static final String KEYWORDS = "a, b, c";
    private static final String FRAGMENT_NAME = "fragment name";
    private static final String RESTRICTION = "no_brightness";
    private static final int ICON = R.drawable.ic_settings_accent;
    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private static final Class TOGGLE_CONTROLLER = FakeToggleController.class;
    private static final Class SLIDER_CONTROLLER = FakeSliderController.class;
    private static final Class INVALID_SLIDER_CONTROLLER = FakeInvalidSliderController.class;
    private static final Class CONTEXT_CONTROLLER = FakeContextOnlyPreferenceController.class;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);
    }

    @After
    public void tearDown() {
        ShadowRestrictedLockUtilsInternal.reset();
    }

    @Test
    public void buildIntentSlice_returnsMatchingSlice() {
        final SliceData sliceData = getMockData(CONTEXT_CONTROLLER, SliceData.SliceType.INTENT);
        final Slice slice = SliceBuilderUtils.buildSlice(mContext, sliceData);

        SliceTester.testSettingsIntentSlice(mContext, slice, sliceData);
    }

    @Test
    public void buildToggleSlice_returnsMatchingSlice() {
        final SliceData mockData = getMockData(TOGGLE_CONTROLLER, SliceData.SliceType.SWITCH);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, mockData);
        SliceTester.testSettingsToggleSlice(mContext, slice, mockData);
    }

    @Test
    public void buildSliderSlice_returnsMatchingSlice() {
        final SliceData data = getMockData(SLIDER_CONTROLLER, SliceData.SliceType.SLIDER);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);
        SliceTester.testSettingsSliderSlice(mContext, slice, data);
    }

    @Test
    public void buildToggleSlice_withUserRestriction_shouldReturnToggleSlice() {
        final SliceData mockData = getMockData(TOGGLE_CONTROLLER, SliceData.SliceType.SWITCH,
                RESTRICTION);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, mockData);

        SliceTester.testSettingsToggleSlice(mContext, slice, mockData);
    }

    @Test
    public void buildToggleSlice_withUserRestrictionAndRestricted_shouldReturnIntentSlice() {
        final SliceData mockData = getMockData(TOGGLE_CONTROLLER, SliceData.SliceType.SWITCH,
                RESTRICTION);
        ShadowRestrictedLockUtilsInternal.setRestricted(true);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, mockData);

        SliceTester.testSettingsIntentSlice(mContext, slice, mockData);
    }

    @Test
    public void testGetPreferenceController_buildsMatchingController() {
        final BasePreferenceController controller =
                SliceBuilderUtils.getPreferenceController(mContext, getMockData());

        assertThat(controller).isInstanceOf(FakeToggleController.class);
    }

    @Test
    public void testGetPreferenceController_contextOnly_buildsMatchingController() {
        final BasePreferenceController controller =
                SliceBuilderUtils.getPreferenceController(mContext,
                        getMockData(CONTEXT_CONTROLLER, 0));

        assertThat(controller).isInstanceOf(FakeContextOnlyPreferenceController.class);
    }

    @Test
    public void getDynamicSummary_allowDynamicSummary_returnsControllerSummary() {
        final SliceData data = getMockData();
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        final String controllerSummary = "new_Summary";
        doReturn(controllerSummary).when(controller).getSummary();

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(controllerSummary);
    }

    @Test
    public void getDynamicSummary_allowDynamicSummary_nullSummary_returnsNull() {
        final SliceData data = getMockData();
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));

        doReturn(null).when(controller).getSummary();

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isNull();
    }

    @Test
    public void getDynamicSummary_noScreenTitle_returnsPrefControllerSummary() {
        final SliceData data = getMockData("", "");
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        final String controllerSummary = "new_Summary";
        doReturn(controllerSummary).when(controller).getSummary();

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(controllerSummary);
    }

    @Test
    public void getDynamicSummary_screenTitleMatchesTitle_returnsPrefControllerSummary() {
        final SliceData data = getMockData("", TITLE);
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        final String controllerSummary = "new_Summary";
        doReturn(controllerSummary).when(controller).getSummary();

        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(controllerSummary);
    }

    @Test
    public void getDynamicSummary_emptyScreenTitle_emptyControllerSummary_returnsEmptyString() {
        final SliceData data = getMockData(null, null);
        final FakePreferenceController controller = new FakePreferenceController(mContext, KEY);
        final CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isNull();
    }

    @Test
    public void getDynamicSummary_screenTitleAndControllerPlaceholder_returnsSliceEmptyString() {
        final String summaryPlaceholder = mContext.getString(R.string.summary_placeholder);
        final SliceData data = getMockData(summaryPlaceholder, summaryPlaceholder);
        final FakePreferenceController controller = spy(
                new FakePreferenceController(mContext, KEY));
        doReturn(summaryPlaceholder).when(controller).getSummary();

        CharSequence summary = SliceBuilderUtils.getSubtitleText(mContext, controller, data);

        assertThat(summary).isEqualTo(summaryPlaceholder);
    }

    @Test
    public void getPathData_splitsIntentUri() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_INTENT)
                .appendPath(KEY)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair.first).isTrue();
        assertThat(pathPair.second).isEqualTo(KEY);
    }

    @Test
    public void getPathData_splitsActionUri() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(KEY)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair.first).isFalse();
        assertThat(pathPair.second).isEqualTo(KEY);
    }

    @Test
    public void getPathData_noKey_returnsNull() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair).isNull();
    }

    @Test
    public void getPathData_extraArg_returnsNull() {
        final Uri uri = new Uri.Builder()
                .authority(SettingsSliceProvider.SLICE_AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(KEY)
                .appendPath(KEY)
                .build();

        final Pair<Boolean, String> pathPair = SliceBuilderUtils.getPathData(uri);

        assertThat(pathPair.first).isFalse();
        assertThat(pathPair.second).isEqualTo(KEY + "/" + KEY);
    }

    @Test
    public void testUnsupportedSlice_validTitleSummary() {
        final SliceData data = getMockData(FakeUnavailablePreferenceController.class,
                SliceData.SliceType.SWITCH);
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        assertThat(slice).isNull();
    }

    @Test
    public void testDisabledForUserSlice_validTitleSummary() {
        final SliceData data = getMockData(FakeUnavailablePreferenceController.class,
                SliceData.SliceType.SWITCH);
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.DISABLED_FOR_USER);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        assertThat(slice).isNull();
    }

    @Test
    public void testDisabledDependentSettingSlice_validTitleSummary() {
        final SliceData data = getMockData(FakeUnavailablePreferenceController.class,
                SliceData.SliceType.INTENT);
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.DISABLED_DEPENDENT_SETTING);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        SliceTester.testSettingsUnavailableSlice(mContext, slice, data);
    }

    @Test
    public void testConditionallyUnavailableSlice_sliceShouldBeNull() {
        final SliceData data = getMockData(FakeUnavailablePreferenceController.class,
                SliceData.SliceType.SWITCH);
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        assertThat(slice).isNull();
    }

    @Test
    public void testContentIntent_includesUniqueData() {
        final SliceData sliceData = getMockData();
        final Uri expectedUri = new Uri.Builder().appendPath(sliceData.getKey()).build();

        final Intent intent = SliceBuilderUtils.getContentIntent(mContext, sliceData);
        final Uri intentData = intent.getData();

        assertThat(intentData).isEqualTo(expectedUri);
    }

    @Test
    public void buildIntentSlice_noIconPassed_returnsSliceWithIcon() {
        final int expectedIconResource = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_accent).toIcon().getResId();
        final SliceData sliceData = getMockData(CONTEXT_CONTROLLER, SliceData.SliceType.INTENT,
                0x0);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, sliceData);

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();
        final int actualIconResource = primaryAction.getIcon().toIcon().getResId();
        assertThat(actualIconResource).isEqualTo(expectedIconResource);
    }

    @Test
    public void buildDisabledDependentSlice_noIconPassed_returnsSliceWithIcon() {
        final int expectedIconResource = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_accent).toIcon().getResId();
        final SliceData data = getMockData(FakeUnavailablePreferenceController.class,
                SUMMARY, SliceData.SliceType.SWITCH, SCREEN_TITLE, 0 /* icon */,
                null /* unavailableSliceSubtitle */);
        Settings.Global.putInt(mContext.getContentResolver(),
                FakeUnavailablePreferenceController.AVAILABILITY_KEY,
                BasePreferenceController.DISABLED_DEPENDENT_SETTING);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();
        final int actualIconResource = primaryAction.getIcon().toIcon().getResId();
        assertThat(actualIconResource).isEqualTo(expectedIconResource);
    }

    @Test
    public void buildToggleSlice_noIconPassed_returnsSliceWithIcon() {
        final int expectedIconResource = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_accent).toIcon().getResId();
        final SliceData mockData = getMockData(TOGGLE_CONTROLLER, SliceData.SliceType.SWITCH,
                0x0);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, mockData);

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();
        final int actualIconResource = primaryAction.getIcon().toIcon().getResId();
        assertThat(actualIconResource).isEqualTo(expectedIconResource);
    }

    @Test
    public void buildSliderSlice_noIconPassed_returnsSliceWithIcon() {
        final int expectedIconResource = IconCompat.createWithResource(mContext,
                R.drawable.ic_settings_accent).toIcon().getResId();
        final SliceData data = getMockData(SLIDER_CONTROLLER, SliceData.SliceType.SLIDER, 0x0);

        final Slice slice = SliceBuilderUtils.buildSlice(mContext, data);

        final SliceMetadata metadata = SliceMetadata.from(mContext, slice);
        final SliceAction primaryAction = metadata.getPrimaryAction();
        final int actualIconResource = primaryAction.getIcon().toIcon().getResId();
        assertThat(actualIconResource).isEqualTo(expectedIconResource);
    }

    @Test
    public void buildSliderSlice_invalidSlider_returnNull() {
        final SliceData data = getMockData(INVALID_SLIDER_CONTROLLER, SliceData.SliceType.SLIDER,
                0x0 /* icon */);

        assertThat(SliceBuilderUtils.buildSlice(mContext, data)).isNull();
    }

    @Test
    public void getSafeIcon_replacesEmptyIconWithSettingsIcon() {
        final int settingsIcon = R.drawable.ic_settings_accent;
        final int zeroIcon = 0x0;
        final SliceData data = getMockData(TOGGLE_CONTROLLER, SliceData.SliceType.SWITCH,
                zeroIcon);

        final IconCompat actualIcon = SliceBuilderUtils.getSafeIcon(mContext, data);

        final int actualIconResource = actualIcon.toIcon().getResId();
        assertThat(actualIconResource).isNotEqualTo(zeroIcon);
        assertThat(actualIconResource).isEqualTo(settingsIcon);
    }

    @Test
    public void getSafeIcon_invalidResource_shouldFallbackToSettingsIcon() {
        final int settingsIcon = R.drawable.ic_settings_accent;
        final int badIcon = 0x12345678;
        final SliceData data = getMockData(TOGGLE_CONTROLLER, SliceData.SliceType.SWITCH,
                badIcon);

        final IconCompat actualIcon = SliceBuilderUtils.getSafeIcon(mContext, data);

        final int actualIconResource = actualIcon.toIcon().getResId();
        assertThat(actualIconResource).isEqualTo(settingsIcon);
    }

    private SliceData getMockData() {
        return getMockData(TOGGLE_CONTROLLER, SUMMARY, SliceData.SliceType.SWITCH, SCREEN_TITLE,
                ICON, null /* unavailableSliceSubtitle */);
    }

    private SliceData getMockData(Class prefController, int sliceType, int icon) {
        return getMockData(prefController, SUMMARY, sliceType, SCREEN_TITLE,
                icon, null /* unavailableSliceSubtitle */);
    }

    private SliceData getMockData(String summary, String screenTitle) {
        return getMockData(TOGGLE_CONTROLLER, summary, SliceData.SliceType.SWITCH, screenTitle,
                ICON, null /* unavailableSliceSubtitle */);
    }

    private SliceData getMockData(Class prefController, int sliceType) {
        return getMockData(prefController, SUMMARY, sliceType, SCREEN_TITLE, ICON,
                null /* unavailableSliceSubtitle */);
    }

    private SliceData getMockData(Class prefController, int sliceType, String userRestriction) {
        return getMockData(prefController, SUMMARY, sliceType, SCREEN_TITLE, ICON,
                null /* unavailableSliceSubtitle */, userRestriction);
    }

    private SliceData getMockData(Class prefController, String summary, int sliceType,
            String screenTitle, int icon, String unavailableSliceSubtitle) {
        return getMockData(prefController, summary, sliceType, screenTitle, icon,
                unavailableSliceSubtitle, null /* userRestriction */);
    }

    private SliceData getMockData(Class prefController, String summary, int sliceType,
            String screenTitle, int icon, String unavailableSliceSubtitle, String userRestriction) {
        return new SliceData.Builder()
                .setKey(KEY)
                .setTitle(TITLE)
                .setSummary(summary)
                .setScreenTitle(screenTitle)
                .setKeywords(KEYWORDS)
                .setIcon(icon)
                .setFragmentName(FRAGMENT_NAME)
                .setUri(URI)
                .setPreferenceControllerClassName(prefController.getName())
                .setSliceType(sliceType)
                .setUnavailableSliceSubtitle(unavailableSliceSubtitle)
                .setUserRestriction(userRestriction)
                .build();
    }
}
