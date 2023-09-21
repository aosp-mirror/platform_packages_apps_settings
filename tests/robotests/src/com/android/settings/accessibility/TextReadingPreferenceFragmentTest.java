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

import static com.android.settings.accessibility.FontWeightAdjustmentPreferenceController.BOLD_TEXT_ADJUSTMENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import com.android.settings.accessibility.TextReadingResetController.ResetStateListener;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Tests for {@link TextReadingPreferenceFragment}. */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class TextReadingPreferenceFragmentTest {

    @Rule
    public final MockitoRule mMockito = MockitoJUnit.rule();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    private Context mContext = ApplicationProvider.getApplicationContext();
    private TextReadingPreferenceFragment mFragment;

    @Before
    public void setUp() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);

        mFragment = spy(new TextReadingPreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(Robolectric.setupActivity(FragmentActivity.class));

        // Avoid a NPE is happened in ShadowWindowManagerGlobal
        doReturn(mock(DisplaySizeData.class)).when(mFragment).createDisplaySizeData(mContext);
        mFragment.createPreferenceControllers(mContext);
    }

    @Test
    public void onDialogPositiveButtonClicked_boldTextEnabled_needResetSettings() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, BOLD_TEXT_ADJUSTMENT);
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(
                DialogEnums.DIALOG_RESET_SETTINGS);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();

        assertThat(mFragment.mNeedResetSettings).isTrue();
    }

    @Test
    public void onDialogPositiveButtonClicked_boldTextDisabled_resetAllListeners() {
        final ResetStateListener listener1 = mock(ResetStateListener.class);
        final ResetStateListener listener2 = mock(ResetStateListener.class);
        mFragment.mResetStateListeners = new ArrayList<>(Arrays.asList(listener1, listener2));
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(
                DialogEnums.DIALOG_RESET_SETTINGS);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();

        verify(listener1).resetState();
        verify(listener2).resetState();
    }

    @Test
    public void onDialogPositiveButtonClicked_boldTextEnabled_showToast() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, BOLD_TEXT_ADJUSTMENT);
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(
                DialogEnums.DIALOG_RESET_SETTINGS);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();

        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(mContext.getString(R.string.accessibility_text_reading_reset_message));
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_text_reading_options);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("TextReadingPreferenceFragment");
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = TextReadingPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_text_reading_options);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }
}
