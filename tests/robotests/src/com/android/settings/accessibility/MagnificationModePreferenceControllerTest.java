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
 * limitations under the License.
 */

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import static com.android.settings.accessibility.MagnificationModePreferenceController.MagnificationModeInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.utils.AnnotationSpan;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MagnificationModePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class MagnificationModePreferenceControllerTest {
    private static final String PREF_KEY = "screen_magnification_mode";
    private static final int MAGNIFICATION_MODE_DEFAULT = MagnificationMode.ALL;

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    private PreferenceScreen mScreen;
    private Context mContext;
    private MagnificationModePreferenceController mController;
    private Preference mModePreference;
    @Spy
    private TestDialogHelper mDialogHelper = new TestDialogHelper();

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mModePreference = new Preference(mContext);
        mModePreference.setKey(PREF_KEY);
        mScreen.addPreference(mModePreference);
        MagnificationCapabilities.setCapabilities(mContext, MAGNIFICATION_MODE_DEFAULT);
        mController = new MagnificationModePreferenceController(mContext, PREF_KEY);
        showPreferenceOnTheScreen(null);
    }

    @Test
    public void clickPreference_settingsModeIsDefault_checkedModeInDialogIsDefault() {
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);

        assertThat(getCheckedModeFromDialog()).isEqualTo(MAGNIFICATION_MODE_DEFAULT);

    }

    @Test
    public void choseModeIsDifferentFromInSettings_checkedModeInDialogIsExpected() {
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);

        performItemClickWith(MagnificationMode.WINDOW);

        assertThat(getCheckedModeFromDialog()).isEqualTo(MagnificationMode.WINDOW);
    }

    @Test
    public void dialogIsReCreated_settingsModeIsAllAndChoseWindowMode_checkedModeIsWindow() {
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);
        performItemClickWith(MagnificationMode.WINDOW);

        reshowPreferenceOnTheScreen();
        mDialogHelper.showDialog(MagnificationModePreferenceController.DIALOG_MAGNIFICATION_MODE);

        assertThat(getCheckedModeFromDialog()).isEqualTo(
                MagnificationMode.WINDOW);
    }

    @Test
    public void chooseFullscreenMode_tripleTapEnabled_notShowTripleTapWarningDialog() {
        enableTripleTap();
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);

        performItemClickWith(MagnificationMode.FULLSCREEN);
        mController.onMagnificationModeDialogPositiveButtonClicked(mDialogHelper.getDialog(),
                DialogInterface.BUTTON_POSITIVE);

        verify(mDialogHelper, never()).showDialog(
                MagnificationModePreferenceController.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING);
    }

    @Test
    public void chooseWindowMode_tripleTapEnabled_showTripleTapWarningDialog() {
        enableTripleTap();
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);

        performItemClickWith(MagnificationMode.WINDOW);
        mController.onMagnificationModeDialogPositiveButtonClicked(mDialogHelper.getDialog(),
                DialogInterface.BUTTON_POSITIVE);

        verify(mDialogHelper).showDialog(
                MagnificationModePreferenceController.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING);
    }

    @Test
    public void chooseAllMode_tripleTapEnabled_showTripleTapWarningDialog() {
        enableTripleTap();
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);

        performItemClickWith(MagnificationMode.ALL);
        mController.onMagnificationModeDialogPositiveButtonClicked(mDialogHelper.getDialog(),
                DialogInterface.BUTTON_POSITIVE);

        verify(mDialogHelper).showDialog(
                MagnificationModePreferenceController.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING);
    }

    @Test
    public void onTripleTapWarningDialogNegativeButtonClicked_showModeDialog() {
        mDialogHelper.showDialog(
                MagnificationModePreferenceController.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING);

        mController.onMagnificationTripleTapWarningDialogNegativeButtonClicked(
                mDialogHelper.getDialog(), DialogInterface.BUTTON_NEGATIVE);

        verify(mDialogHelper).showDialog(
                MagnificationModePreferenceController.DIALOG_MAGNIFICATION_MODE);
    }

    @Test
    public void onTripleTapWarningDialogPositiveButtonClicked_chooseAllMode_returnAllSummary() {
        enableTripleTap();
        mModePreference.getOnPreferenceClickListener().onPreferenceClick(mModePreference);
        performItemClickWith(MagnificationMode.ALL);
        mController.onMagnificationModeDialogPositiveButtonClicked(mDialogHelper.getDialog(),
                DialogInterface.BUTTON_POSITIVE);

        mController.onMagnificationTripleTapWarningDialogPositiveButtonClicked(
                mDialogHelper.getDialog(), DialogInterface.BUTTON_POSITIVE);

        final String allSummary = mContext.getString(
                R.string.accessibility_magnification_area_settings_all_summary);
        assertThat(TextUtils.equals(mController.getSummary(), allSummary)).isTrue();
    }

    @Test
    public void checkSpansInTripleTapWarningDialog_existAnnotationSpan() {
        mDialogHelper.showDialog(
                MagnificationModePreferenceController.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING);
        final View contentView = mDialogHelper.getDialog().findViewById(android.R.id.content);
        final TextView messageView = contentView.findViewById(R.id.message);
        final CharSequence textInTripleTapWarningDialog = messageView.getText();

        final AnnotationSpan[] annotationSpans =
                ((SpannableString) textInTripleTapWarningDialog).getSpans(/*queryStart= */ 0,
                        textInTripleTapWarningDialog.length(), AnnotationSpan.class);

        assertThat(annotationSpans[0]).isNotNull();
    }

    @Test
    public void getSummary_saveWindowScreen_shouldReturnWindowScreenSummary() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.WINDOW);

        final String windowSummary = mContext.getString(
                R.string.accessibility_magnification_area_settings_window_screen_summary);
        assertThat(TextUtils.equals(mController.getSummary(), windowSummary)).isTrue();
    }

    @Test
    public void getSummary_saveAll_shouldReturnAllSummary() {
        MagnificationCapabilities.setCapabilities(mContext, MagnificationMode.ALL);

        final String allSummary = mContext.getString(
                R.string.accessibility_magnification_area_settings_all_summary);
        assertThat(TextUtils.equals(mController.getSummary(), allSummary)).isTrue();
    }

    private int getCheckedModeFromDialog() {
        final ListView listView = mController.mMagnificationModesListView;
        assertThat(listView).isNotNull();

        final int checkedPosition = listView.getCheckedItemPosition();
        final MagnificationModeInfo modeInfo =
                (MagnificationModeInfo) listView.getAdapter().getItem(checkedPosition);
        return modeInfo.mMagnificationMode;
    }

    private void performItemClickWith(@MagnificationMode int mode) {
        final ListView listView = mController.mMagnificationModesListView;
        assertThat(listView).isNotNull();

        int modeIndex = AdapterView.NO_ID;
        // Index 0 is header.
        for (int i = 1; i < listView.getAdapter().getCount(); i++) {
            final MagnificationModeInfo modeInfo =
                    (MagnificationModeInfo) listView.getAdapter().getItem(i);
            if (modeInfo.mMagnificationMode == mode) {
                modeIndex = i;
                break;
            }
        }
        if (modeIndex == AdapterView.NO_ID) {
            throw new RuntimeException("The mode is not in the list.");
        }

        listView.performItemClick(listView.getChildAt(modeIndex), modeIndex, modeIndex);
    }

    private void enableTripleTap() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, ON);
    }

    private void reshowPreferenceOnTheScreen() {
        final Bundle bundle = new Bundle();
        mController.onSaveInstanceState(bundle);
        showPreferenceOnTheScreen(bundle);
    }

    private void showPreferenceOnTheScreen(Bundle savedInstanceState) {
        mController.setDialogHelper(mDialogHelper);
        mController.onCreate(savedInstanceState);
        mController.displayPreference(mScreen);
    }

    private static class TestDialogHelper implements DialogCreatable,
            MagnificationModePreferenceController.DialogHelper {
        private DialogCreatable mDialogDelegate;
        private Dialog mDialog;

        @Override
        public void showDialog(int dialogId) {
            mDialog = onCreateDialog(dialogId);
        }

        @Override
        public void setDialogDelegate(DialogCreatable delegate) {
            mDialogDelegate = delegate;
        }

        @Override
        public Dialog onCreateDialog(int dialogId) {
            return mDialogDelegate.onCreateDialog(dialogId);
        }

        @Override
        public int getDialogMetricsCategory(int dialogId) {
            return mDialogDelegate.getDialogMetricsCategory(dialogId);
        }

        public Dialog getDialog() {
            return mDialog;
        }
    }
}
