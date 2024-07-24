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

import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_APP_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_ADD_SYSTEM_LOCALE;
import static com.android.settings.localepicker.LocaleListEditor.EXTRA_SYSTEM_LOCALE_DIALOG_TYPE;
import static com.android.settings.localepicker.LocaleListEditor.LOCALE_SUGGESTION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.IActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.LocaleList;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAlertDialogCompat.class,
        ShadowActivityManager.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
@LooperMode(LooperMode.Mode.LEGACY)
public class LocaleListEditorTest {

    private static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    private static final String TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT = "dialog_confirm_system_default";
    private static final String TAG_DIALOG_NOT_AVAILABLE = "dialog_not_available_locale";
    private static final String TAG_DIALOG_ADD_SYSTEM_LOCALE = "dialog_add_system_locale";
    private static final int DIALOG_CONFIRM_SYSTEM_DEFAULT = 1;
    private static final int REQUEST_CONFIRM_SYSTEM_DEFAULT = 1;

    private LocaleListEditor mLocaleListEditor;
    private Context mContext;
    private FragmentActivity mActivity;
    private List<LocaleStore.LocaleInfo> mLocaleList;
    private Intent mIntent = new Intent();
    private LocaleDragCell mLocaleDragCell;
    private LocaleDragAndDropAdapter.CustomViewHolder mCustomViewHolder;

    @Mock
    private LocaleDragAndDropAdapter mAdapter;
    @Mock
    private View mAddLanguage;
    @Mock
    private Resources mResources;
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
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock
    private TextView mLabel;
    @Mock
    private CheckBox mCheckbox;
    @Mock
    private TextView mMiniLabel;
    @Mock
    private TextView mLocalized;
    @Mock
    private TextView mCurrentDefault;
    @Mock
    private ImageView mDragHandle;
    @Mock
    private NotificationController mNotificationController;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule =
            DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLocaleListEditor = spy(new LocaleListEditor());
        when(mLocaleListEditor.getContext()).thenReturn(mContext);
        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class).get());
        when(mLocaleListEditor.getActivity()).thenReturn(mActivity);
        when(mLocaleListEditor.getNotificationController()).thenReturn(
                mNotificationController);
        ReflectionHelpers.setField(mLocaleListEditor, "mEmptyTextView",
                new TextView(RuntimeEnvironment.application));
        ReflectionHelpers.setField(mLocaleListEditor, "mRestrictionsManager",
                RuntimeEnvironment.application.getSystemService(Context.RESTRICTIONS_SERVICE));
        ReflectionHelpers.setField(mLocaleListEditor, "mUserManager",
                RuntimeEnvironment.application.getSystemService(Context.USER_SERVICE));
        ReflectionHelpers.setField(mLocaleListEditor, "mAdapter", mAdapter);
        ReflectionHelpers.setField(mLocaleListEditor, "mAddLanguage", mAddLanguage);
        ReflectionHelpers.setField(mLocaleListEditor, "mFragmentManager", mFragmentManager);
        ReflectionHelpers.setField(mLocaleListEditor, "mMetricsFeatureProvider",
                mMetricsFeatureProvider);
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
        FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() {
        ReflectionHelpers.setField(mLocaleListEditor, "mRemoveMode", false);
        ReflectionHelpers.setField(mLocaleListEditor, "mShowingRemoveDialog", false);
        ReflectionHelpers.setField(mLocaleListEditor, "mLocaleAdditionMode", false);
        ShadowAlertDialogCompat.reset();
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
    public void showConfirmDialog_systemLocaleSelected_shouldShowLocaleChangeDialog()
            throws Exception {
        //pre-condition
        setUpLocaleConditions();
        final Configuration config = new Configuration();
        config.setLocales((LocaleList.forLanguageTags("zh-TW,en-US")));
        when(mActivityService.getConfiguration()).thenReturn(config);
        when(mAdapter.getFeedItemList()).thenReturn(mLocaleList);
        when(mAdapter.getCheckedCount()).thenReturn(1);
        when(mAdapter.getItemCount()).thenReturn(2);
        when(mAdapter.isFirstLocaleChecked()).thenReturn(true);
        ReflectionHelpers.setField(mLocaleListEditor, "mRemoveMode", true);
        ReflectionHelpers.setField(mLocaleListEditor, "mShowingRemoveDialog", true);

        //launch the first dialog
        mLocaleListEditor.showRemoveLocaleWarningDialog();

        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        // click the remove button
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertThat(dialog.isShowing()).isFalse();

        // check the second dialog is showing
        verify(mFragmentTransaction).add(any(LocaleDialogFragment.class),
                eq(TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT));
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

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void showDiallogForAddedLocale_showConfirmDialog() {
        initIntentAndResourceForLocaleDialog();
        mLocaleListEditor.onViewStateRestored(null);

        verify(mFragmentTransaction).add(any(LocaleDialogFragment.class),
                eq(TAG_DIALOG_ADD_SYSTEM_LOCALE));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void showDiallogForAddedLocale_clickAdd() {
        initIntentAndResourceForLocaleDialog();
        mLocaleListEditor.onViewStateRestored(null);
        LocaleStore.LocaleInfo info = LocaleStore.fromLocale(Locale.forLanguageTag("en-US"));
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_ADD_SYSTEM_LOCALE);
        bundle.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, info);
        Intent intent = new Intent().putExtras(bundle);

        mLocaleListEditor.onActivityResult(DIALOG_ADD_SYSTEM_LOCALE, Activity.RESULT_OK, intent);

        verify(mAdapter).addLocale(any(LocaleStore.LocaleInfo.class));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void showDiallogForAddedLocale_clickCancel() {
        initIntentAndResourceForLocaleDialog();
        mLocaleListEditor.onViewStateRestored(null);
        LocaleStore.LocaleInfo info = LocaleStore.fromLocale(Locale.forLanguageTag("en-US"));
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_ADD_SYSTEM_LOCALE);
        bundle.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, info);
        Intent intent = new Intent().putExtras(bundle);

        mLocaleListEditor.onActivityResult(DIALOG_ADD_SYSTEM_LOCALE, Activity.RESULT_CANCELED,
                intent);

        verify(mAdapter, never()).addLocale(any(LocaleStore.LocaleInfo.class));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void showDiallogForAddedLocale_invalidLocale_noDialog() {
        Intent intent = new Intent("ACTION")
                .putExtra(EXTRA_APP_LOCALE, "ab-CD") // invalid locale
                .putExtra(EXTRA_SYSTEM_LOCALE_DIALOG_TYPE, LOCALE_SUGGESTION);
        mActivity.setIntent(intent);

        mLocaleListEditor.onViewStateRestored(null);

        verify(mFragmentTransaction, never()).add(any(LocaleDialogFragment.class),
                eq(TAG_DIALOG_ADD_SYSTEM_LOCALE));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void showDiallogForAddedLocale_noDialogType_noDialog() {
        Intent intent = new Intent("ACTION")
                .putExtra(EXTRA_APP_LOCALE, "ja-JP");
        // no EXTRA_SYSTEM_LOCALE_DIALOG_TYPE  in the extra
        mActivity.setIntent(intent);

        mLocaleListEditor.onViewStateRestored(null);

        verify(mFragmentTransaction, never()).add(any(LocaleDialogFragment.class),
                eq(TAG_DIALOG_ADD_SYSTEM_LOCALE));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_LOCALE_NOTIFICATION_ENABLED)
    public void showDiallogForAddedLocale_inSystemLocale_noDialog() {
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US,ar-AE-u-nu-arab"));
        Intent intent = new Intent("ACTION")
                .putExtra(EXTRA_APP_LOCALE, "ar-AE")
                .putExtra(EXTRA_SYSTEM_LOCALE_DIALOG_TYPE, LOCALE_SUGGESTION);
        mActivity.setIntent(intent);

        mLocaleListEditor.onViewStateRestored(null);

        verify(mFragmentTransaction, never()).add(any(LocaleDialogFragment.class),
                eq(TAG_DIALOG_ADD_SYSTEM_LOCALE));
    }

    private void initIntentAndResourceForLocaleDialog() {
        Intent intent = new Intent("ACTION")
                .putExtra(EXTRA_APP_LOCALE, "ja-JP")
                .putExtra(EXTRA_SYSTEM_LOCALE_DIALOG_TYPE, LOCALE_SUGGESTION);

        mActivity.setIntent(intent);
        String[] supportedLocales = new String[]{"en-US", "ja-JP"};
        View contentView = LayoutInflater.from(mActivity).inflate(R.layout.locale_dialog, null);
        doReturn(contentView).when(mLocaleListEditor).getLocaleDialogView();
        when(mLocaleListEditor.getSupportedLocales()).thenReturn(supportedLocales);
        when(mContext.getPackageName()).thenReturn("com.android.settings");
        when(mActivity.getCallingPackage()).thenReturn("com.android.settings");
    }

    @Test
    public void onBindViewHolder_shouldSetCheckedBoxText() {
        ReflectionHelpers.setField(mLocaleListEditor, "mRemoveMode", true);
        mLocaleList = new ArrayList<>();
        mLocaleList.add(mLocaleInfo);
        when(mLocaleInfo.getFullNameNative()).thenReturn("English");
        when(mLocaleInfo.getLocale()).thenReturn(LocaleList.forLanguageTags("en-US").get(0));

        mAdapter = spy(new LocaleDragAndDropAdapter(mLocaleListEditor, mLocaleList));
        ReflectionHelpers.setField(mAdapter, "mFeedItemList", mLocaleList);
        ReflectionHelpers.setField(mAdapter, "mCacheItemList", new ArrayList<>(mLocaleList));
        ReflectionHelpers.setField(mAdapter, "mContext", mContext);
        ViewGroup view = new FrameLayout(mContext);
        mCustomViewHolder = mAdapter.onCreateViewHolder(view, 0);
        mLocaleDragCell = new LocaleDragCell(mContext, null);
        ReflectionHelpers.setField(mCustomViewHolder, "mLocaleDragCell", mLocaleDragCell);
        ReflectionHelpers.setField(mLocaleDragCell, "mLabel", mLabel);
        ReflectionHelpers.setField(mLocaleDragCell, "mLocalized", mLocalized);
        ReflectionHelpers.setField(mLocaleDragCell, "mCurrentDefault", mCurrentDefault);
        ReflectionHelpers.setField(mLocaleDragCell, "mMiniLabel", mMiniLabel);
        ReflectionHelpers.setField(mLocaleDragCell, "mDragHandle", mDragHandle);
        ReflectionHelpers.setField(mLocaleDragCell, "mCheckbox", mCheckbox);

        mAdapter.onBindViewHolder(mCustomViewHolder, 0);

        verify(mAdapter).setCheckBoxDescription(any(LocaleDragCell.class), any(), anyBoolean());
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
