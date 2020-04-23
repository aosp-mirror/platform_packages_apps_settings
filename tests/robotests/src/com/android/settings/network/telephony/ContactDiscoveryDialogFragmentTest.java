/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsRcsManager;
import android.telephony.ims.RcsUceAdapter;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialog.class)
public class ContactDiscoveryDialogFragmentTest {
    private static final int TEST_SUB_ID = 2;
    private static final String TEST_CARRIER = "TestMobile";

    @Mock private ImsManager mImsManager;
    @Mock private ImsRcsManager mImsRcsManager;
    @Mock private RcsUceAdapter mRcsUceAdapter;

    private ContactDiscoveryDialogFragment mDialogFragmentUT;
    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        mDialogFragmentUT = spy(ContactDiscoveryDialogFragment.newInstance(TEST_SUB_ID,
                TEST_CARRIER));
        doReturn(mImsManager).when(mDialogFragmentUT).getImsManager(any());
        doReturn(mImsRcsManager).when(mImsManager).getImsRcsManager(TEST_SUB_ID);
        doReturn(mRcsUceAdapter).when(mImsRcsManager).getUceAdapter();
    }

    @Test
    public void testCancelDoesNothing() throws Exception {
        final AlertDialog dialog = startDialog();
        assertThat(dialog).isNotNull();
        final Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(negativeButton).isNotNull();
        negativeButton.performClick();
        verify(mRcsUceAdapter, never()).setUceSettingEnabled(anyBoolean());
    }

    @Test
    public void testOkEnablesDiscovery() throws Exception {
        final AlertDialog dialog = startDialog();
        assertThat(dialog).isNotNull();
        final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        assertThat(positiveButton).isNotNull();
        positiveButton.performClick();
        verify(mRcsUceAdapter).setUceSettingEnabled(true /*isEnabled*/);
    }

    private AlertDialog startDialog() {
        mDialogFragmentUT.show(mActivity.getSupportFragmentManager(), null);
        return ShadowAlertDialog.getLatestAlertDialog();
    }
}
