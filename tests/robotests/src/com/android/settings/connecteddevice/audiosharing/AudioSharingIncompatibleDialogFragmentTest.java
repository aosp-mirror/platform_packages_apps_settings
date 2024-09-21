/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class, ShadowBluetoothAdapter.class})
public class AudioSharingIncompatibleDialogFragmentTest {
    private static final String TEST_DEVICE_NAME = "test";
    private static final AudioSharingIncompatibleDialogFragment.DialogEventListener
            EMPTY_EVENT_LISTENER = () -> {};

    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Fragment mParent;
    private AudioSharingIncompatibleDialogFragment mFragment;

    @Before
    public void setUp() {
        ShadowAlertDialogCompat.reset();
        ShadowBluetoothAdapter shadowBluetoothAdapter = Shadow.extract(
                BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mFragment = new AudioSharingIncompatibleDialogFragment();
        mParent = new Fragment();
        FragmentController.setupFragment(mParent, FragmentActivity.class, /* containerViewId= */
                0, /* bundle= */ null);
    }

    @After
    public void tearDown() {
        ShadowAlertDialogCompat.reset();
    }

    @Test
    public void getMetricsCategory_correctValue() {
        // TODO: update to real metrics id
        assertThat(mFragment.getMetricsCategory()).isEqualTo(0);
    }

    @Test
    public void onCreateDialog_flagOff_dialogNotExist() {
        mSetFlagsRule.disableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingIncompatibleDialogFragment.show(mParent, TEST_DEVICE_NAME,
                EMPTY_EVENT_LISTENER);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_unattachedFragment_dialogNotExist() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingIncompatibleDialogFragment.show(new Fragment(), TEST_DEVICE_NAME,
                EMPTY_EVENT_LISTENER);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNull();
    }

    @Test
    public void onCreateDialog_flagOn_showDialog() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AudioSharingIncompatibleDialogFragment.show(mParent, TEST_DEVICE_NAME,
                EMPTY_EVENT_LISTENER);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView title = dialog.findViewById(R.id.title_text);
        assertThat(title).isNotNull();
        // TODO: use string res
        assertThat(title.getText().toString()).isEqualTo(
                "Can't share audio with " + TEST_DEVICE_NAME);
    }

    @Test
    public void onCreateDialog_clickBtn_callbackTriggered() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        AtomicBoolean isBtnClicked = new AtomicBoolean(false);
        AudioSharingIncompatibleDialogFragment.show(mParent, TEST_DEVICE_NAME,
                () -> isBtnClicked.set(true));
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        View btnView = dialog.findViewById(android.R.id.button1);
        assertThat(btnView).isNotNull();
        btnView.performClick();
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isFalse();
        assertThat(isBtnClicked.get()).isTrue();
    }
}


