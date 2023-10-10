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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowActivityManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class, ShadowActivityManager.class})
public class LocaleListEditorTest {

    private static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    private static final String TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT = "dialog_confirm_system_default";
    private static final String TAG_DIALOG_NOT_AVAILABLE = "dialog_not_available_locale";
    private static final int DIALOG_CONFIRM_SYSTEM_DEFAULT = 1;
    private static final int REQUEST_CONFIRM_SYSTEM_DEFAULT = 1;

    private LocaleListEditor mLocaleListEditor;

    private Context mContext;
    private FragmentActivity mActivity;
    private List mLocaleList;
    private Intent mIntent = new Intent();

    @Mock
    private LocaleDragAndDropAdapter mAdapter;
    @Mock
    private LocaleStore.LocaleInfo mLocaleInfo;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private View mView;
    @Mock
    private IActivityManager mActivityService;

    @Before
    public void setUp() throws Exception {
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
        ReflectionHelpers.setField(mLocaleListEditor, "mFragmentManager", mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
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

    @Test
    public void mayAppendUnicodeTags_appendUnicodeTags_success() {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.fromLocale(Locale.forLanguageTag("en-US"));

        LocaleStore.LocaleInfo result =
                LocaleListEditor.mayAppendUnicodeTags(localeInfo, "und-u-fw-wed-mu-celsius");

        assertThat(result.getLocale().getUnicodeLocaleType("fw")).isEqualTo("wed");
        assertThat(result.getLocale().getUnicodeLocaleType("mu")).isEqualTo("celsius");
    }

    @Test
    public void onActivityResult_ResultCodeIsOk_showNotAvailableDialog() {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_CONFIRM_SYSTEM_DEFAULT);
        mIntent.putExtras(bundle);
        setUpLocaleConditions();
        mLocaleListEditor.onActivityResult(REQUEST_CONFIRM_SYSTEM_DEFAULT, Activity.RESULT_OK,
                mIntent);

        verify(mFragmentTransaction).add(any(LocaleDialogFragment.class),
                eq(TAG_DIALOG_NOT_AVAILABLE));
    }

    @Test
    public void onActivityResult_ResultCodeIsCancel_notifyAdapterListChanged() {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_CONFIRM_SYSTEM_DEFAULT);
        mIntent.putExtras(bundle);
        setUpLocaleConditions();
        mLocaleListEditor.onActivityResult(REQUEST_CONFIRM_SYSTEM_DEFAULT, Activity.RESULT_CANCELED,
                mIntent);

        verify(mAdapter).notifyListChanged(mLocaleInfo);
    }

    @Test
    public void onTouch_dragDifferentLocaleToTop_showConfirmDialog() throws Exception {
        MotionEvent event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 0.0f, 0.0f, 0);
        setUpLocaleConditions();
        final Configuration config = new Configuration();
        config.setLocales((LocaleList.forLanguageTags("zh-TW,en-US")));
        when(mActivityService.getConfiguration()).thenReturn(config);
        when(mAdapter.getFeedItemList()).thenReturn(mLocaleList);
        mLocaleListEditor.onTouch(mView, event);

        verify(mFragmentTransaction).add(any(LocaleDialogFragment.class),
                eq(TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT));
    }

    @Test
    public void onTouch_dragSameLocaleToTop_updateAdapter() throws Exception {
        MotionEvent event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 0.0f, 0.0f, 0);
        setUpLocaleConditions();
        final Configuration config = new Configuration();
        config.setLocales((LocaleList.forLanguageTags("en-US,zh-TW")));
        when(mActivityService.getConfiguration()).thenReturn(config);
        when(mAdapter.getFeedItemList()).thenReturn(mLocaleList);
        mLocaleListEditor.onTouch(mView, event);

        verify(mAdapter).doTheUpdate();
    }

    private void setUpLocaleConditions() {
        ShadowActivityManager.setService(mActivityService);
        mLocaleList = new ArrayList<>();
        mLocaleList.add(mLocaleInfo);
        when(mLocaleInfo.getFullNameNative()).thenReturn("English");
        when(mLocaleInfo.getLocale()).thenReturn(LocaleList.forLanguageTags("en-US").get(0));
        when(mAdapter.getFeedItemList()).thenReturn(mLocaleList);
    }
}
