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

package com.android.settings.applications.assist;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.internal.app.AssistUtils;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;
import com.android.settingslib.applications.DefaultAppInfo;

import java.util.ArrayList;
import java.util.List;

public class DefaultVoiceInputPicker extends DefaultAppPickerFragment {

    private VoiceInputHelper mHelper;
    private AssistUtils mAssistUtils;
    private String mAssistRestrict;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEFAULT_VOICE_INPUT_PICKER;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAssistUtils = new AssistUtils(context);
        mHelper = new VoiceInputHelper(context);
        mHelper.buildUi();
        final ComponentName assist = getCurrentAssist();
        if (isCurrentAssistVoiceService(assist, getCurrentService(mHelper))) {
            mAssistRestrict = assist.flattenToShortString();
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.default_voice_settings;
    }

    @Override
    protected List<VoiceInputDefaultAppInfo> getCandidates() {
        final List<VoiceInputDefaultAppInfo> candidates = new ArrayList<>();
        final Context context = getContext();
        boolean hasEnabled = true;
        for (VoiceInputHelper.InteractionInfo info : mHelper.mAvailableInteractionInfos) {
            final boolean enabled = TextUtils.equals(info.key, mAssistRestrict);
            hasEnabled |= enabled;
            candidates.add(new VoiceInputDefaultAppInfo(context, mPm, mUserId, info, enabled));
        }

        final boolean assistIsService = !hasEnabled;
        for (VoiceInputHelper.RecognizerInfo info : mHelper.mAvailableRecognizerInfos) {
            final boolean enabled = !assistIsService;
            candidates.add(new VoiceInputDefaultAppInfo(context, mPm, mUserId, info, enabled));
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final ComponentName currentService = getCurrentService(mHelper);
        if (currentService == null) {
            return null;
        }
        return currentService.flattenToShortString();
    }

    @Override
    protected boolean setDefaultKey(String value) {
        for (VoiceInputHelper.InteractionInfo info : mHelper.mAvailableInteractionInfos) {
            if (TextUtils.equals(value, info.key)) {
                Settings.Secure.putString(getContext().getContentResolver(),
                        Settings.Secure.VOICE_INTERACTION_SERVICE, value);
                Settings.Secure.putString(getContext().getContentResolver(),
                        Settings.Secure.VOICE_RECOGNITION_SERVICE,
                        new ComponentName(info.service.packageName,
                                info.serviceInfo.getRecognitionService())
                                .flattenToShortString());
                return true;
            }
        }

        for (VoiceInputHelper.RecognizerInfo info : mHelper.mAvailableRecognizerInfos) {
            if (TextUtils.equals(value, info.key)) {
                Settings.Secure.putString(getContext().getContentResolver(),
                        Settings.Secure.VOICE_INTERACTION_SERVICE, "");
                Settings.Secure.putString(getContext().getContentResolver(),
                        Settings.Secure.VOICE_RECOGNITION_SERVICE, value);
                return true;
            }
        }
        return true;
    }

    public static ComponentName getCurrentService(VoiceInputHelper helper) {
        if (helper.mCurrentVoiceInteraction != null) {
            return helper.mCurrentVoiceInteraction;
        } else if (helper.mCurrentRecognizer != null) {
            return helper.mCurrentRecognizer;
        } else {
            return null;
        }
    }

    private ComponentName getCurrentAssist() {
        return mAssistUtils.getAssistComponentForUser(mUserId);
    }

    public static boolean isCurrentAssistVoiceService(ComponentName currentAssist,
            ComponentName currentVoiceService) {
        return currentAssist == null && currentVoiceService == null ||
                currentAssist != null && currentAssist.equals(currentVoiceService);
    }

    public static class VoiceInputDefaultAppInfo extends DefaultAppInfo {

        public VoiceInputHelper.BaseInfo mInfo;

        public VoiceInputDefaultAppInfo(Context context, PackageManager pm, int userId,
                VoiceInputHelper.BaseInfo info, boolean enabled) {
            super(context, pm, userId, info.componentName, null /* summary */, enabled);
            mInfo = info;
        }

        @Override
        public String getKey() {
            return mInfo.key;
        }

        @Override
        public CharSequence loadLabel() {
            if (mInfo instanceof VoiceInputHelper.InteractionInfo) {
                return mInfo.appLabel;
            } else {
                return mInfo.label;
            }
        }

        public Intent getSettingIntent() {
            if (mInfo.settings == null) {
                return null;
            }
            return new Intent(Intent.ACTION_MAIN).setComponent(mInfo.settings);
        }
    }
}
