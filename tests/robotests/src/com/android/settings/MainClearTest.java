/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowUserManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class MainClearTest {

    private static final String TEST_ACCOUNT_TYPE = "android.test.account.type";
    private static final String TEST_CONFIRMATION_PACKAGE = "android.test.conf.pkg";
    private static final String TEST_CONFIRMATION_CLASS = "android.test.conf.pkg.ConfActivity";
    private static final String TEST_ACCOUNT_NAME = "test@example.com";

    @Mock
    private ScrollView mScrollView;
    @Mock
    private LinearLayout mLinearLayout;

    @Mock
    private PackageManager mPackageManager;

    @Mock
    private AccountManager mAccountManager;

    @Mock
    private FragmentActivity mMockActivity;

    @Mock
    private Intent mMockIntent;

    private MainClear mMainClear;
    private ShadowActivity mShadowActivity;
    private FragmentActivity mActivity;
    private ShadowUserManager mShadowUserManager;
    private View mContentView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mMainClear = spy(new MainClear() {
            @Override
            boolean showAnySubscriptionInfo(Context context) { return true; }
        });
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mShadowActivity = Shadows.shadowOf(mActivity);
        UserManager userManager = mActivity.getSystemService(UserManager.class);
        mShadowUserManager = Shadows.shadowOf(userManager);
        mShadowUserManager.setIsAdminUser(true);
        mContentView = LayoutInflater.from(mActivity).inflate(R.layout.main_clear, null);

        // Make scrollView only have one child
        when(mScrollView.getChildAt(0)).thenReturn(mLinearLayout);
        when(mScrollView.getChildCount()).thenReturn(1);
    }

    @After
    public void tearDown() {
        mShadowUserManager.setIsAdminUser(false);
    }

    @Test
    public void testShowFinalConfirmation_eraseEsimVisible_eraseEsimChecked() {
        final Context context = mock(Context.class);
        when(mMainClear.getContext()).thenReturn(context);

        mMainClear.mEsimStorage = mContentView.findViewById(R.id.erase_esim);
        mMainClear.mExternalStorage = mContentView.findViewById(R.id.erase_external);
        mMainClear.mEsimStorageContainer = mContentView.findViewById(R.id.erase_esim_container);
        mMainClear.mEsimStorageContainer.setVisibility(View.VISIBLE);
        mMainClear.mEsimStorage.setChecked(true);
        mMainClear.showFinalConfirmation();

        final ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);

        verify(context).startActivity(intent.capture());
        assertThat(intent.getValue().getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                .getBoolean(MainClear.ERASE_ESIMS_EXTRA, false))
                .isTrue();
    }

    @Test
    public void testShowFinalConfirmation_eraseEsimVisible_eraseEsimUnchecked() {
        final Context context = mock(Context.class);
        when(mMainClear.getContext()).thenReturn(context);

        mMainClear.mEsimStorage = mContentView.findViewById(R.id.erase_esim);
        mMainClear.mExternalStorage = mContentView.findViewById(R.id.erase_external);
        mMainClear.mEsimStorageContainer = mContentView.findViewById(R.id.erase_esim_container);
        mMainClear.mEsimStorageContainer.setVisibility(View.VISIBLE);
        mMainClear.mEsimStorage.setChecked(false);
        mMainClear.showFinalConfirmation();
        final ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);

        verify(context).startActivity(intent.capture());
        assertThat(intent.getValue().getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                .getBoolean(MainClear.ERASE_ESIMS_EXTRA, false))
                .isFalse();
    }

    @Test
    public void testShowFinalConfirmation_eraseEsimGone_eraseEsimChecked() {
        final Context context = mock(Context.class);
        when(mMainClear.getContext()).thenReturn(context);

        mMainClear.mEsimStorage = mContentView.findViewById(R.id.erase_esim);
        mMainClear.mExternalStorage = mContentView.findViewById(R.id.erase_external);
        mMainClear.mEsimStorageContainer = mContentView.findViewById(R.id.erase_esim_container);
        mMainClear.mEsimStorageContainer.setVisibility(View.GONE);
        mMainClear.mEsimStorage.setChecked(true);
        mMainClear.showFinalConfirmation();

        final ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);

        verify(context).startActivity(intent.capture());
        assertThat(intent.getValue().getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
            .getBoolean(MainClear.ERASE_ESIMS_EXTRA, false))
            .isTrue();
    }

    @Test
    public void testShowFinalConfirmation_eraseEsimGone_eraseEsimUnchecked() {
        final Context context = mock(Context.class);
        when(mMainClear.getContext()).thenReturn(context);

        mMainClear.mEsimStorage = mContentView.findViewById(R.id.erase_esim);
        mMainClear.mExternalStorage = mContentView.findViewById(R.id.erase_external);
        mMainClear.mEsimStorageContainer = mContentView.findViewById(R.id.erase_esim_container);
        mMainClear.mEsimStorageContainer.setVisibility(View.GONE);
        mMainClear.mEsimStorage.setChecked(false);
        mMainClear.showFinalConfirmation();
        final ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);

        verify(context).startActivity(intent.capture());
        assertThat(intent.getValue().getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
            .getBoolean(MainClear.ERASE_ESIMS_EXTRA, false))
            .isFalse();
    }

    @Test
    public void testShowWipeEuicc_euiccDisabled() {
        prepareEuiccState(
                false /* isEuiccEnabled */,
                true /* isEuiccProvisioned */,
                false /* isDeveloper */);
        assertThat(mMainClear.showWipeEuicc()).isFalse();
    }

    @Test
    public void testShowWipeEuicc_euiccEnabled_unprovisioned() {
        prepareEuiccState(
                true /* isEuiccEnabled */,
                false /* isEuiccProvisioned */,
                false /* isDeveloper */);
        assertThat(mMainClear.showWipeEuicc()).isFalse();
    }

    @Test
    public void testShowWipeEuicc_euiccEnabled_provisioned() {
        prepareEuiccState(
                true /* isEuiccEnabled */,
                true /* isEuiccProvisioned */,
                false /* isDeveloper */);
        assertThat(mMainClear.showWipeEuicc()).isTrue();
    }

    @Test
    public void testShowWipeEuicc_developerMode_unprovisioned() {
        prepareEuiccState(
                true /* isEuiccEnabled */,
                false /* isEuiccProvisioned */,
                true /* isDeveloper */);
        assertThat(mMainClear.showWipeEuicc()).isTrue();
    }

    @Test
    public void testEsimRecheckBoxDefaultChecked() {
        assertThat(((CheckBox) mContentView.findViewById(R.id.erase_esim)).isChecked()).isFalse();
    }

    @Test
    public void testHasReachedBottom_NotScrollDown_returnFalse() {
        initScrollView(100, 0, 200);

        assertThat(mMainClear.hasReachedBottom(mScrollView)).isFalse();
    }

    @Test
    public void testHasReachedBottom_CanNotScroll_returnTrue() {
        initScrollView(100, 0, 80);

        assertThat(mMainClear.hasReachedBottom(mScrollView)).isTrue();
    }

    @Test
    public void testHasReachedBottom_ScrollToBottom_returnTrue() {
        initScrollView(100, 100, 200);

        assertThat(mMainClear.hasReachedBottom(mScrollView)).isTrue();
    }

    @Test
    public void testInitiateMainClear_inDemoMode_sendsIntent() {
        ShadowUtils.setIsDemoUser(true);

        final ComponentName componentName =
                ComponentName.unflattenFromString("com.android.retaildemo/.DeviceAdminReceiver");
        ShadowUtils.setDeviceOwnerComponent(componentName);

        mMainClear.mInitiateListener.onClick(mContentView);
        final Intent intent = mShadowActivity.getNextStartedActivity();
        assertThat(Intent.ACTION_FACTORY_RESET).isEqualTo(intent.getAction());
        assertThat(componentName).isNotNull();
        assertThat(componentName.getPackageName()).isEqualTo(intent.getPackage());
    }

    @Test
    public void testOnActivityResultInternal_invalideRequest() {
        int invalidRequestCode = -1;
        doReturn(false).when(mMainClear).isValidRequestCode(eq(invalidRequestCode));

        mMainClear.onActivityResultInternal(invalidRequestCode, Activity.RESULT_OK, null);

        verify(mMainClear, times(1)).isValidRequestCode(eq(invalidRequestCode));
        verify(mMainClear, times(0)).establishInitialState();
        verify(mMainClear, times(0)).getAccountConfirmationIntent();
        verify(mMainClear, times(0)).showFinalConfirmation();
    }

    @Test
    public void testOnActivityResultInternal_resultCanceled() {
        doReturn(true).when(mMainClear).isValidRequestCode(eq(MainClear.KEYGUARD_REQUEST));
        doNothing().when(mMainClear).establishInitialState();

        mMainClear
                .onActivityResultInternal(MainClear.KEYGUARD_REQUEST, Activity.RESULT_CANCELED,
                        null);

        verify(mMainClear, times(1)).isValidRequestCode(eq(MainClear.KEYGUARD_REQUEST));
        verify(mMainClear, times(1)).establishInitialState();
        verify(mMainClear, times(0)).getAccountConfirmationIntent();
        verify(mMainClear, times(0)).showFinalConfirmation();
    }

    @Test
    public void testOnActivityResultInternal_keyguardRequestTriggeringConfirmAccount() {
        doReturn(true).when(mMainClear).isValidRequestCode(eq(MainClear.KEYGUARD_REQUEST));
        doReturn(mMockIntent).when(mMainClear).getAccountConfirmationIntent();
        doNothing().when(mMainClear).showAccountCredentialConfirmation(eq(mMockIntent));

        mMainClear
                .onActivityResultInternal(MainClear.KEYGUARD_REQUEST, Activity.RESULT_OK, null);

        verify(mMainClear, times(1)).isValidRequestCode(eq(MainClear.KEYGUARD_REQUEST));
        verify(mMainClear, times(0)).establishInitialState();
        verify(mMainClear, times(1)).getAccountConfirmationIntent();
        verify(mMainClear, times(1)).showAccountCredentialConfirmation(eq(mMockIntent));
    }

    @Test
    public void testOnActivityResultInternal_keyguardRequestTriggeringShowFinal() {
        doReturn(true).when(mMainClear).isValidRequestCode(eq(MainClear.KEYGUARD_REQUEST));
        doReturn(null).when(mMainClear).getAccountConfirmationIntent();
        doNothing().when(mMainClear).showFinalConfirmation();

        mMainClear
                .onActivityResultInternal(MainClear.KEYGUARD_REQUEST, Activity.RESULT_OK, null);

        verify(mMainClear, times(1)).isValidRequestCode(eq(MainClear.KEYGUARD_REQUEST));
        verify(mMainClear, times(0)).establishInitialState();
        verify(mMainClear, times(1)).getAccountConfirmationIntent();
        verify(mMainClear, times(1)).showFinalConfirmation();
    }

    @Test
    public void testOnActivityResultInternal_confirmRequestTriggeringShowFinal() {
        doReturn(true).when(mMainClear)
                .isValidRequestCode(eq(MainClear.CREDENTIAL_CONFIRM_REQUEST));
        doNothing().when(mMainClear).showFinalConfirmation();

        mMainClear.onActivityResultInternal(
                MainClear.CREDENTIAL_CONFIRM_REQUEST, Activity.RESULT_OK, null);

        verify(mMainClear, times(1))
                .isValidRequestCode(eq(MainClear.CREDENTIAL_CONFIRM_REQUEST));
        verify(mMainClear, times(0)).establishInitialState();
        verify(mMainClear, times(0)).getAccountConfirmationIntent();
        verify(mMainClear, times(1)).showFinalConfirmation();
    }

    @Test
    public void testGetAccountConfirmationIntent_unsupported() {
        when(mMainClear.getActivity()).thenReturn(mActivity);
        /* Using the default resources, account confirmation shouldn't trigger */
        assertThat(mMainClear.getAccountConfirmationIntent()).isNull();
    }

    @Test
    public void testGetAccountConfirmationIntent_no_relevant_accounts() {
        when(mMainClear.getActivity()).thenReturn(mMockActivity);
        when(mMockActivity.getString(R.string.account_type)).thenReturn(TEST_ACCOUNT_TYPE);
        when(mMockActivity.getString(R.string.account_confirmation_package))
                .thenReturn(TEST_CONFIRMATION_PACKAGE);
        when(mMockActivity.getString(R.string.account_confirmation_class))
                .thenReturn(TEST_CONFIRMATION_CLASS);

        Account[] accounts = new Account[0];
        when(mMockActivity.getSystemService(Context.ACCOUNT_SERVICE)).thenReturn(mAccountManager);
        when(mAccountManager.getAccountsByType(TEST_ACCOUNT_TYPE)).thenReturn(accounts);
        assertThat(mMainClear.getAccountConfirmationIntent()).isNull();
    }

    @Test
    public void testGetAccountConfirmationIntent_unresolved() {
        when(mMainClear.getActivity()).thenReturn(mMockActivity);
        when(mMockActivity.getString(R.string.account_type)).thenReturn(TEST_ACCOUNT_TYPE);
        when(mMockActivity.getString(R.string.account_confirmation_package))
                .thenReturn(TEST_CONFIRMATION_PACKAGE);
        when(mMockActivity.getString(R.string.account_confirmation_class))
                .thenReturn(TEST_CONFIRMATION_CLASS);
        Account[] accounts = new Account[]{new Account(TEST_ACCOUNT_NAME, TEST_ACCOUNT_TYPE)};
        when(mMockActivity.getSystemService(Context.ACCOUNT_SERVICE)).thenReturn(mAccountManager);
        when(mAccountManager.getAccountsByType(TEST_ACCOUNT_TYPE)).thenReturn(accounts);
        // The package manager should not resolve the confirmation intent targeting the non-existent
        // confirmation package.
        when(mMockActivity.getPackageManager()).thenReturn(mPackageManager);
        assertThat(mMainClear.getAccountConfirmationIntent()).isNull();
    }

    @Test
    public void testTryShowAccountConfirmation_ok() {
        when(mMainClear.getActivity()).thenReturn(mMockActivity);
        // Only try to show account confirmation if the appropriate resource overlays are available.
        when(mMockActivity.getString(R.string.account_type)).thenReturn(TEST_ACCOUNT_TYPE);
        when(mMockActivity.getString(R.string.account_confirmation_package))
                .thenReturn(TEST_CONFIRMATION_PACKAGE);
        when(mMockActivity.getString(R.string.account_confirmation_class))
                .thenReturn(TEST_CONFIRMATION_CLASS);
        // Add accounts to trigger the search for a resolving intent.
        Account[] accounts = new Account[]{new Account(TEST_ACCOUNT_NAME, TEST_ACCOUNT_TYPE)};
        when(mMockActivity.getSystemService(Context.ACCOUNT_SERVICE)).thenReturn(mAccountManager);
        when(mAccountManager.getAccountsByType(TEST_ACCOUNT_TYPE)).thenReturn(accounts);
        // The package manager should not resolve the confirmation intent targeting the non-existent
        // confirmation package.
        when(mMockActivity.getPackageManager()).thenReturn(mPackageManager);

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = TEST_CONFIRMATION_PACKAGE;
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        when(mPackageManager.resolveActivity(any(), eq(0))).thenReturn(resolveInfo);

        Intent actualIntent = mMainClear.getAccountConfirmationIntent();
        assertThat(TEST_CONFIRMATION_PACKAGE).isEqualTo(
                actualIntent.getComponent().getPackageName());
        assertThat(TEST_CONFIRMATION_CLASS).isEqualTo(actualIntent.getComponent().getClassName());
    }

    @Test
    public void testShowAccountCredentialConfirmation() {
        // Finally mock out the startActivityForResultCall
        doNothing().when(mMainClear)
                .startActivityForResult(eq(mMockIntent),
                        eq(MainClear.CREDENTIAL_CONFIRM_REQUEST));
        mMainClear.showAccountCredentialConfirmation(mMockIntent);
        verify(mMainClear, times(1))
                .startActivityForResult(eq(mMockIntent),
                        eq(MainClear.CREDENTIAL_CONFIRM_REQUEST));
    }

    @Test
    public void testIsValidRequestCode() {
        assertThat(mMainClear.isValidRequestCode(MainClear.KEYGUARD_REQUEST)).isTrue();
        assertThat(mMainClear.isValidRequestCode(MainClear.CREDENTIAL_CONFIRM_REQUEST))
                .isTrue();
        assertThat(mMainClear.isValidRequestCode(0)).isFalse();
    }

    @Test
    public void testOnGlobalLayout_shouldNotRemoveListener() {
        final ViewTreeObserver viewTreeObserver = mock(ViewTreeObserver.class);
        mMainClear.mScrollView = mScrollView;
        doNothing().when(mMainClear).onGlobalLayout();
        doReturn(true).when(mMainClear).hasReachedBottom(any());
        when(mScrollView.getViewTreeObserver()).thenReturn(viewTreeObserver);

        mMainClear.onGlobalLayout();

        verify(viewTreeObserver, never()).removeOnGlobalLayoutListener(mMainClear);
    }

    private void prepareEuiccState(
            boolean isEuiccEnabled, boolean isEuiccProvisioned, boolean isDeveloper) {
        doReturn(mActivity).when(mMainClear).getContext();
        doReturn(isEuiccEnabled).when(mMainClear).isEuiccEnabled(any());
        ContentResolver cr = mActivity.getContentResolver();
        Settings.Global.putInt(cr, Settings.Global.EUICC_PROVISIONED, isEuiccProvisioned ? 1 : 0);
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(mActivity, isDeveloper);
    }

    private void initScrollView(int height, int scrollY, int childBottom) {
        when(mScrollView.getHeight()).thenReturn(height);
        when(mScrollView.getScrollY()).thenReturn(scrollY);
        when(mLinearLayout.getBottom()).thenReturn(childBottom);
    }
}
