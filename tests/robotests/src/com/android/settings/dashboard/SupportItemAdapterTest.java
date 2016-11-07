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

package com.android.settings.dashboard;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.overlay.SupportFeatureProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import com.android.settings.R;
import org.robolectric.shadows.ShadowActivity;

import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;
import static org.mockito.Mockito.when;
import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SupportItemAdapterTest {
    private static final String ACCOUNT_TYPE = "com.google";
    private final Account USER_1 = new Account("user1", ACCOUNT_TYPE);
    private final Account USER_2 = new Account("user2", ACCOUNT_TYPE);
    private final Account TWO_ACCOUNTS[] = {USER_1, USER_2};
    private final Account ONE_ACCOUNT[] = {USER_1};

    private ShadowActivity mShadowActivity;
    private Activity mActivity;
    private SupportItemAdapter mSupportItemAdapter;
    private SupportItemAdapter.ViewHolder mViewHolder;
    @Mock
    private SupportFeatureProvider mSupportFeatureProvider;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(Activity.class);
        mShadowActivity = shadowOf(mActivity);

        final View itemView = LayoutInflater.from(mActivity).inflate(
                R.layout.support_escalation_options, null);
        mViewHolder = new SupportItemAdapter.ViewHolder(itemView);

        // Mock this to prevent crash in testing
        when(mSupportFeatureProvider.getAccountLoginIntent()).thenReturn(
                new Intent(Settings.ACTION_ADD_ACCOUNT));
    }

    @Test
    public void testBindAccountPicker_TwoAccounts_ShouldHaveTwoAccounts() {
        testBindAccountPickerInner(mViewHolder, TWO_ACCOUNTS);
    }

    @Test
    public void testBindAccountPicker_OneAccount_ShouldHaveOneAccount() {
        testBindAccountPickerInner(mViewHolder, ONE_ACCOUNT);
    }

    @Test
    public void testOnSpinnerItemClick_AddAccountClicked_AccountLoginIntentInvoked() {
        bindAccountPickerInner(mViewHolder, TWO_ACCOUNTS);

        final Spinner spinner = (Spinner) mViewHolder.itemView.findViewById(R.id.account_spinner);
        spinner.setSelection(TWO_ACCOUNTS.length);

        Robolectric.flushForegroundThreadScheduler();

        verify(mSupportFeatureProvider).getAccountLoginIntent();
    }

    /**
     * Check after {@link SupportItemAdapter#bindAccountPicker(SupportItemAdapter.ViewHolder)} is
     * invoked, whether the spinner in {@paramref viewHolder} has all the data from {@paramref
     * accounts}
     *
     * @param viewHolder holds the view that contains the spinner to test
     * @param accounts holds the accounts info to be showed in spinner.
     */
    private void testBindAccountPickerInner(SupportItemAdapter.ViewHolder viewHolder,
            Account accounts[]) {
        bindAccountPickerInner(viewHolder, accounts);

        final Spinner spinner = (Spinner) viewHolder.itemView.findViewById(R.id.account_spinner);
        final SpinnerAdapter adapter = spinner.getAdapter();

        // Contains "Add account" option, so should be 'count+1'
        assertThat(adapter.getCount()).isEqualTo(accounts.length + 1);
        for (int i = 0; i < accounts.length; i++) {
            assertThat(adapter.getItem(i)).isEqualTo(accounts[i].name);
        }
    }

    /**
     * Create {@link SupportItemAdapter} and bind the account picker view into
     * {@paramref viewholder}
     *
     * @param viewHolder holds the view that contains the spinner to test
     * @param accounts holds the accounts info to be showed in spinner.
     */
    private void bindAccountPickerInner(SupportItemAdapter.ViewHolder viewHolder,
            Account accounts[]) {
        when(mSupportFeatureProvider.getSupportEligibleAccounts(mActivity)).thenReturn(accounts);
        mSupportItemAdapter = new SupportItemAdapter(mActivity, null, mSupportFeatureProvider,
                mMetricsFeatureProvider, null);

        mSupportItemAdapter.bindAccountPicker(viewHolder);
    }

}
