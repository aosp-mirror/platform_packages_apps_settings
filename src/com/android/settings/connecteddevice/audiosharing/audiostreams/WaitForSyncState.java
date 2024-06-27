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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsScanQrCodeController.REQUEST_SCAN_BT_BROADCAST_QR_CODE;

import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.utils.ThreadUtils;

class WaitForSyncState extends AudioStreamStateHandler {
    @VisibleForTesting
    static final int AUDIO_STREAM_WAIT_FOR_SYNC_STATE_SUMMARY =
            R.string.audio_streams_connecting_summary;

    @VisibleForTesting static final int WAIT_FOR_SYNC_TIMEOUT_MILLIS = 15000;

    @Nullable private static WaitForSyncState sInstance = null;

    @VisibleForTesting
    WaitForSyncState() {}

    static WaitForSyncState getInstance() {
        if (sInstance == null) {
            sInstance = new WaitForSyncState();
        }
        return sInstance;
    }

    @Override
    void performAction(
            AudioStreamPreference preference,
            AudioStreamsProgressCategoryController controller,
            AudioStreamsHelper helper) {
        var metadata = preference.getAudioStreamMetadata();
        if (metadata != null) {
            mHandler.postDelayed(
                    () -> {
                        if (preference.isShown()
                                && preference.getAudioStreamState() == getStateEnum()) {
                            controller.handleSourceLost(preference.getAudioStreamBroadcastId());
                            mMetricsFeatureProvider.action(
                                    preference.getContext(),
                                    SettingsEnums
                                            .ACTION_AUDIO_STREAM_JOIN_FAILED_WAIT_FOR_SYNC_TIMEOUT,
                                    preference.getSourceOriginForLogging().ordinal());
                            ThreadUtils.postOnMainThread(
                                    () -> {
                                        if (controller.getFragment() != null) {
                                            showBroadcastUnavailableDialog(
                                                    controller.getFragment(),
                                                    preference.getContext(),
                                                    AudioStreamsHelper.getBroadcastName(metadata));
                                        }
                                    });
                        }
                    },
                    WAIT_FOR_SYNC_TIMEOUT_MILLIS);
        }
    }

    @Override
    int getSummary() {
        return AUDIO_STREAM_WAIT_FOR_SYNC_STATE_SUMMARY;
    }

    @Override
    AudioStreamsProgressCategoryController.AudioStreamState getStateEnum() {
        return AudioStreamsProgressCategoryController.AudioStreamState.WAIT_FOR_SYNC;
    }

    private void showBroadcastUnavailableDialog(
            Fragment fragment, Context context, String broadcastName) {
        var broadcastUnavailableDialog =
                new AudioStreamsDialogFragment.DialogBuilder(context)
                        .setTitle(
                                context.getString(
                                        R.string.audio_streams_dialog_stream_is_not_available))
                        .setSubTitle1(broadcastName)
                        .setSubTitle2(context.getString(R.string.audio_streams_is_not_playing))
                        .setLeftButtonText(context.getString(R.string.audio_streams_dialog_close))
                        .setLeftButtonOnClickListener(AlertDialog::dismiss)
                        .setRightButtonText(context.getString(R.string.audio_streams_dialog_retry))
                        .setRightButtonOnClickListener(
                                dialog -> {
                                    launchQrCodeScanFragment(context, fragment);
                                    dialog.dismiss();
                                });

        AudioStreamsDialogFragment.show(
                fragment,
                broadcastUnavailableDialog,
                SettingsEnums.DIALOG_AUDIO_STREAM_MAIN_WAIT_FOR_SYNC_TIMEOUT);
    }

    @VisibleForTesting
    void launchQrCodeScanFragment(Context context, Fragment fragment) {
        new SubSettingLauncher(context)
                .setTitleRes(R.string.audio_streams_main_page_scan_qr_code_title)
                .setDestination(AudioStreamsQrCodeScanFragment.class.getName())
                .setResultListener(fragment, REQUEST_SCAN_BT_BROADCAST_QR_CODE)
                .setSourceMetricsCategory(
                        SettingsEnums.DIALOG_AUDIO_STREAM_MAIN_WAIT_FOR_SYNC_TIMEOUT)
                .launch();
    }
}
