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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionServiceInfo;
import android.speech.RecognitionService;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.app.AssistUtils;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.applications.defaultapps.DefaultAppInfo;
import com.android.settings.applications.defaultapps.DefaultAppPickerFragment;

import java.util.ArrayList;
import java.util.List;

public class DefaultAssistPicker extends DefaultAppPickerFragment {

    private static final String TAG = "DefaultAssistPicker";
    private static final Intent ASSIST_SERVICE_PROBE =
            new Intent(VoiceInteractionService.SERVICE_INTERFACE);
    private static final Intent ASSIST_ACTIVITY_PROBE =
            new Intent(Intent.ACTION_ASSIST);
    private final List<Info> mAvailableAssistants = new ArrayList<>();

    private AssistUtils mAssistUtils;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEFAULT_ASSIST_PICKER;
    }

    @Override
    protected boolean shouldShowItemNone() {
        return true;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAssistUtils = new AssistUtils(context);
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        mAvailableAssistants.clear();
        addAssistServices();
        addAssistActivities();

        final List<String> packages = new ArrayList<>();
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        for (Info info : mAvailableAssistants) {
            final String packageName = info.component.getPackageName();
            if (packages.contains(packageName)) {
                // A service appears before an activity thus overrides it if from the same package.
                continue;
            }
            packages.add(packageName);
            candidates.add(new DefaultAppInfo(mPm, mUserId, info.component));
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final ComponentName cn = getCurrentAssist();
        if (cn != null) {
            return new DefaultAppInfo(mPm, mUserId, cn).getKey();
        }
        return null;
    }

    @Override
    protected String getConfirmationMessage(CandidateInfo appInfo) {
        if (appInfo == null) {
            return null;
        }
        return getContext().getString(R.string.assistant_security_warning, appInfo.loadLabel());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        if (TextUtils.isEmpty(key)) {
            setAssistNone();
            return true;
        }
        ComponentName cn = ComponentName.unflattenFromString(key);
        final Info info = findAssistantByPackageName(cn.getPackageName());
        if (info == null) {
            setAssistNone();
            return true;
        }

        if (info.isVoiceInteractionService()) {
            setAssistService(info);
        } else {
            setAssistActivity(info);
        }
        return true;
    }

    public ComponentName getCurrentAssist() {
        return mAssistUtils.getAssistComponentForUser(mUserId);
    }

    private void addAssistServices() {
        final PackageManager pm = mPm.getPackageManager();
        final List<ResolveInfo> services = pm.queryIntentServices(
                ASSIST_SERVICE_PROBE, PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo : services) {
            VoiceInteractionServiceInfo voiceInteractionServiceInfo =
                    new VoiceInteractionServiceInfo(pm, resolveInfo.serviceInfo);
            if (!voiceInteractionServiceInfo.getSupportsAssist()) {
                continue;
            }

            mAvailableAssistants.add(new Info(
                    new ComponentName(resolveInfo.serviceInfo.packageName,
                            resolveInfo.serviceInfo.name),
                    voiceInteractionServiceInfo));
        }
    }

    private void addAssistActivities() {
        final PackageManager pm = mPm.getPackageManager();
        final List<ResolveInfo> activities = pm.queryIntentActivities(
                ASSIST_ACTIVITY_PROBE, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : activities) {
            mAvailableAssistants.add(new Info(
                    new ComponentName(resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name)));
        }
    }

    private Info findAssistantByPackageName(String packageName) {
        for (Info info : mAvailableAssistants) {
            if (TextUtils.equals(info.component.getPackageName(), packageName)) {
                return info;
            }
        }
        return null;
    }

    private void setAssistNone() {
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ASSISTANT, "");
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE, "");
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE, getDefaultRecognizer());
    }

    private void setAssistService(Info serviceInfo) {
        final String serviceComponentName = serviceInfo.component.
                flattenToShortString();
        final String serviceRecognizerName = new ComponentName(
                serviceInfo.component.getPackageName(),
                serviceInfo.voiceInteractionServiceInfo.getRecognitionService())
                .flattenToShortString();

        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ASSISTANT, serviceComponentName);
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE, serviceComponentName);
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE, serviceRecognizerName);
    }

    private void setAssistActivity(Info activityInfo) {
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ASSISTANT, activityInfo.component.flattenToShortString());
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE, "");
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE, getDefaultRecognizer());
    }

    private String getDefaultRecognizer() {
        final ResolveInfo resolveInfo = mPm.getPackageManager().resolveService(
                new Intent(RecognitionService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Log.w(TAG, "Unable to resolve default voice recognition service.");
            return "";
        }

        return new ComponentName(resolveInfo.serviceInfo.packageName,
                resolveInfo.serviceInfo.name).flattenToShortString();
    }

    static class Info {
        public final ComponentName component;
        public final VoiceInteractionServiceInfo voiceInteractionServiceInfo;

        Info(ComponentName component) {
            this.component = component;
            this.voiceInteractionServiceInfo = null;
        }

        Info(ComponentName component, VoiceInteractionServiceInfo voiceInteractionServiceInfo) {
            this.component = component;
            this.voiceInteractionServiceInfo = voiceInteractionServiceInfo;
        }

        public boolean isVoiceInteractionService() {
            return voiceInteractionServiceInfo != null;
        }
    }
}
