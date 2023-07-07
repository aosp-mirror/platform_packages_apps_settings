/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.language;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.profileselector.ProfileSelectDialog;
import com.android.settings.dashboard.profileselector.UserAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Controller of the On-device recognition preference. */
public class OnDeviceRecognitionPreferenceController extends BasePreferenceController {

    private static final String TAG = "OnDeviceRecognitionPreferenceController";

    private Optional<Intent> mIntent;

    private WeakReference<Dialog> mProfileSelectDialog = new WeakReference<>(null);

    public OnDeviceRecognitionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mIntent == null) {
            mIntent = Optional.ofNullable(onDeviceRecognitionIntent());
        }
        return mIntent.isPresent()
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mIntent != null && mIntent.isPresent()) {
            preference.setIntent(mIntent.get());
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        show(preference);
        return true;
    }

    private void show(Preference preference) {
        final List<UserHandle> userHandles = new ArrayList<>();
        for (UserInfo userInfo : UserManager.get(mContext).getUsers()) {
            userHandles.add(userInfo.getUserHandle());
        }

        // Only a single profile is installed. Proceed with its settings.
        if (userHandles.size() == 1) {
            mContext.startActivityAsUser(preference.getIntent(), userHandles.get(0));
            return;
        }

        // Multiple profiles are installed. Show a dialog to the user to pick one to proceed with.
        createAndShowProfileSelectDialog(preference.getIntent(), userHandles);
    }

    private UserAdapter.OnClickListener createProfileDialogClickCallback(
            Intent intent, List<UserHandle> userHandles) {
        return (int position) -> {
            mContext.startActivityAsUser(intent, userHandles.get(position));
            if (mProfileSelectDialog.get() != null) {
                mProfileSelectDialog.get().dismiss();
            }
        };
    }

    private void createAndShowProfileSelectDialog(Intent intent, List<UserHandle> userHandles) {
        Dialog profileSelectDialog = ProfileSelectDialog.createDialog(
                mContext, userHandles, createProfileDialogClickCallback(intent, userHandles));
        mProfileSelectDialog = new WeakReference<>(profileSelectDialog);
        profileSelectDialog.show();
    }

    /**
     * Create an {@link Intent} for the activity in the default on-device recognizer service if
     * there is a properly defined speech recognition xml meta-data for that service.
     *
     * @return {@link Intent} if the proper activity is fount, {@code null} otherwise.
     */
    @Nullable
    private Intent onDeviceRecognitionIntent() {
        final String resString = mContext.getString(
                R.string.config_defaultOnDeviceSpeechRecognitionService);

        if (resString == null) {
            Log.v(TAG, "No on-device recognizer, intent not created.");
            return null;
        }

        final ComponentName defaultOnDeviceRecognizerComponentName =
                ComponentName.unflattenFromString(resString);

        if (defaultOnDeviceRecognizerComponentName == null) {
            Log.v(TAG, "Invalid on-device recognizer string format, intent not created.");
            return null;
        }

        final ArrayList<VoiceInputHelper.RecognizerInfo> validRecognitionServices =
                VoiceInputHelper.validRecognitionServices(mContext);

        if (validRecognitionServices.isEmpty()) {
            Log.v(TAG, "No speech recognition services"
                    + "with proper `recognition-service` meta-data found.");
            return null;
        }

        // Filter the recognizer services which are in the same package as the default on-device
        // speech recognizer and have a settings activity defined in the meta-data.
        final ArrayList<VoiceInputHelper.RecognizerInfo> validOnDeviceRecognitionServices =
                new ArrayList<>();
        for (VoiceInputHelper.RecognizerInfo recognizerInfo: validRecognitionServices) {
            if (!defaultOnDeviceRecognizerComponentName.getPackageName().equals(
                    recognizerInfo.mService.packageName)) {
                Log.v(TAG, String.format("Recognition service not in the same package as the "
                        + "default on-device recognizer: %s.",
                        recognizerInfo.mComponentName.flattenToString()));
            } else if (recognizerInfo.mSettings == null) {
                Log.v(TAG, String.format("Recognition service with no settings activity: %s.",
                        recognizerInfo.mComponentName.flattenToString()));
            } else {
                validOnDeviceRecognitionServices.add(recognizerInfo);
                Log.v(TAG, String.format("Recognition service in the same package as the default "
                                + "on-device recognizer with settings activity: %s.",
                        recognizerInfo.mSettings.flattenToString()));
            }
        }

        if (validOnDeviceRecognitionServices.isEmpty()) {
            Log.v(TAG, "No speech recognition services with proper `recognition-service` "
                    + "meta-data found in the same package as the default on-device recognizer.");
            return null;
        }

        // Not more than one proper recognition services should be found in the same
        // package as the default on-device recognizer. If that happens,
        // the first one which passed the filter will be selected.
        if (validOnDeviceRecognitionServices.size() > 1) {
            Log.w(TAG, "More than one recognition services with proper `recognition-service` "
                    + "meta-data found in the same package as the default on-device recognizer.");
        }
        VoiceInputHelper.RecognizerInfo chosenRecognizer = validOnDeviceRecognitionServices.get(0);

        return new Intent(Intent.ACTION_MAIN).setComponent(chosenRecognizer.mSettings);
    }
}