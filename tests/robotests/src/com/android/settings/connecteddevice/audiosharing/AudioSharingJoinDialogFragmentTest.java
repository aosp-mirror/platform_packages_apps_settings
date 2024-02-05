/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAlertDialogCompat.class,
            ShadowBluetoothAdapter.class,
        })
public class AudioSharingJoinDialogFragmentTest {

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM1 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME1, /* groupId= */ 1, /* isActive= */ true);
    private static final AudioSharingDeviceItem TEST_DEVICE_ITEM2 =
            new AudioSharingDeviceItem(TEST_DEVICE_NAME2, /* groupId= */ 2, /* isActive= */ false);

    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    private Fragment mParent;
    private AudioSharingJoinDialogFragment mFragment;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;

    @Before
    public void setUp() {
        AlertDialog latestAlertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        if (latestAlertDialog != null) {
            latestAlertDialog.dismiss();
            ShadowAlertDialogCompat.reset();
        }
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        when(mCachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME1);
        when(mCachedDevice2.getName()).thenReturn(TEST_DEVICE_NAME2);
        mFragment = new AudioSharingJoinDialogFragment();
        mParent = new Fragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_flagOff_dialogNotExist() {
        mFragment.show(mParent, new ArrayList<>(), mCachedDevice2, () -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_flagOn_dialogShowTextForSingleDevice() {
        mFragment.show(mParent, new ArrayList<>(), mCachedDevice2, () -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getMessage().toString()).isEqualTo(TEST_DEVICE_NAME2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_flagOn_dialogShowTextForTwoDevice() {
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        mFragment.show(mParent, list, mCachedDevice2, () -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getMessage().toString())
                .isEqualTo(TEST_DEVICE_NAME1 + " and " + TEST_DEVICE_NAME2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_dialogIsShowing_updateDialog() {
        ArrayList<AudioSharingDeviceItem> list = new ArrayList<>();
        list.add(TEST_DEVICE_ITEM1);
        mFragment.show(mParent, list, mCachedDevice2, () -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();

        // Update the content
        ArrayList<AudioSharingDeviceItem> list2 = new ArrayList<>();
        list2.add(TEST_DEVICE_ITEM2);
        mFragment.show(mParent, list2, mCachedDevice1, () -> {});
        shadowMainLooper().idle();
        dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getMessage().toString())
                .isEqualTo(TEST_DEVICE_NAME2 + " and " + TEST_DEVICE_NAME1);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_clickCancel_dialogDismiss() {
        mFragment.show(mParent, new ArrayList<>(), mCachedDevice2, () -> {});
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        rootView.findViewById(R.id.cancel_btn).performClick();
        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreateDialog_clickShare_callbackTriggered() {
        AtomicBoolean isShareBtnClicked = new AtomicBoolean(false);
        mFragment.show(
                mParent, new ArrayList<>(), mCachedDevice2, () -> isShareBtnClicked.set(true));
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        View rootView = shadowDialog.getView();
        rootView.findViewById(R.id.share_btn).performClick();
        assertThat(dialog.isShowing()).isFalse();
        assertThat(isShareBtnClicked.get()).isTrue();
    }
}
