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

package com.android.settings.localepicker;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class LocaleListEditorTest {

    private LocaleListEditor mLocaleListEditor;

    private Context mContext;
    private FragmentActivity mActivity;

    @Mock
    private LocaleDragAndDropAdapter mAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLocaleListEditor = spy(new LocaleListEditor());
        when(mLocaleListEditor.getContext()).thenReturn(mContext);
        mActivity = Robolectric.buildActivity(FragmentActivity.class).get();
        when(mLocaleListEditor.getActivity()).thenReturn(mActivity);
        ReflectionHelpers.setField(mLocaleListEditor, "mEmptyTextView",
                new TextView(RuntimeEnvironment.application));
        ReflectionHelpers.setField(mLocaleListEditor, "mRestrictionsManager",
                RuntimeEnvironment.application.getSystemService(Context.RESTRICTIONS_SERVICE));
        ReflectionHelpers.setField(mLocaleListEditor, "mUserManager",
                RuntimeEnvironment.application.getSystemService(Context.USER_SERVICE));
        ReflectionHelpers.setField(mLocaleListEditor, "mAdapter", mAdapter);
        FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setField(mLocaleListEditor, "mRemoveMode", false);
        ReflectionHelpers.setField(mLocaleListEditor, "mShowingRemoveDialog", false);
    }

    @Test
    public void testDisallowConfigLocale_unrestrict() {
        ReflectionHelpers.setField(mLocaleListEditor, "mIsUiRestricted", true);
        mLocaleListEditor.onAttach(mContext);
        mLocaleListEditor.onResume();
        assertThat(mLocaleListEditor.getEmptyTextView().getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testDisallowConfigLocale_restrict() {
        ReflectionHelpers.setField(mLocaleListEditor, "mIsUiRestricted", false);
        mLocaleListEditor.onAttach(mContext);
        mLocaleListEditor.onResume();
        assertThat(mLocaleListEditor.getEmptyTextView().getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void showRemoveLocaleWarningDialog_allLocaleSelected_shouldShowErrorDialog() {
        //pre-condition
        when(mAdapter.getCheckedCount()).thenReturn(1);
        when(mAdapter.getItemCount()).thenReturn(1);
        when(mAdapter.isFirstLocaleChecked()).thenReturn(true);
        ReflectionHelpers.setField(mLocaleListEditor, "mRemoveMode", true);
        ReflectionHelpers.setField(mLocaleListEditor, "mShowingRemoveDialog", true);

        //launch dialog
        mLocaleListEditor.showRemoveLocaleWarningDialog();

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        final ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);

        assertThat(shadowDialog.getTitle()).isEqualTo(
                mContext.getString(R.string.dlg_remove_locales_error_title));
    }

    @Test
    public void showRemoveLocaleWarningDialog_mainLocaleSelected_shouldShowLocaleChangeDialog() {
        //pre-condition
        when(mAdapter.getCheckedCount()).thenReturn(1);
        when(mAdapter.getItemCount()).thenReturn(2);
        when(mAdapter.isFirstLocaleChecked()).thenReturn(true);
        ReflectionHelpers.setField(mLocaleListEditor, "mRemoveMode", true);
        ReflectionHelpers.setField(mLocaleListEditor, "mShowingRemoveDialog", true);

        //launch dialog
        mLocaleListEditor.showRemoveLocaleWarningDialog();

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        final ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.dlg_remove_locales_message));
    }

    @Test
    public void showRemoveLocaleWarningDialog_mainLocaleNotSelected_shouldShowConfirmDialog() {
        //pre-condition
        when(mAdapter.getCheckedCount()).thenReturn(1);
        when(mAdapter.getItemCount()).thenReturn(2);
        when(mAdapter.isFirstLocaleChecked()).thenReturn(false);
        ReflectionHelpers.setField(mLocaleListEditor, "mRemoveMode", true);
        ReflectionHelpers.setField(mLocaleListEditor, "mShowingRemoveDialog", true);

        //launch dialog
        mLocaleListEditor.showRemoveLocaleWarningDialog();

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        final ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isNull();
    }
}
