/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionServiceInfo;
import android.speech.RecognitionService;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.app.AssistUtils;
import com.android.settings.AppListPreferenceWithSettings;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class DefaultAssistPreference extends AppListPreferenceWithSettings {

    private static final String TAG = DefaultAssistPreference.class.getSimpleName();

    private final List<Info> mAvailableAssistants = new ArrayList<>();

    private final AssistUtils mAssistUtils;

    public DefaultAssistPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setShowItemNone(true);
        setDialogTitle(R.string.choose_assist_title);
        mAssistUtils = new AssistUtils(context);
    }

    @Override
    protected boolean persistString(String value) {
        final Info info = findAssistantByPackageName(value);
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

    private void setAssistNone() {
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ASSISTANT, ITEM_NONE_VALUE);
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE, "");
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE, getDefaultRecognizer());

        setSummary(getContext().getText(R.string.default_assist_none));
        setSettingsComponent(null);
    }

    private void setAssistService(Info serviceInfo) {
        final String serviceComponentName = serviceInfo.component.flattenToShortString();
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

        setSummary(getEntry());
        final String settingsActivity =
                serviceInfo.voiceInteractionServiceInfo.getSettingsActivity();
        setSettingsComponent(settingsActivity == null ?
                null :
                new ComponentName(serviceInfo.component.getPackageName(), settingsActivity));
    }

    private void setAssistActivity(Info activityInfo) {
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.ASSISTANT, activityInfo.component.flattenToShortString());
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_INTERACTION_SERVICE, "");
        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.VOICE_RECOGNITION_SERVICE, getDefaultRecognizer());

        setSummary(getEntry());
        setSettingsComponent(null);
    }

    private String getDefaultRecognizer() {
        ResolveInfo resolveInfo = getContext().getPackageManager().resolveService(
                new Intent(RecognitionService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Log.w(TAG, "Unable to resolve default voice recognition service.");
            return "";
        }

        return new ComponentName(resolveInfo.serviceInfo.packageName,
                resolveInfo.serviceInfo.name).flattenToShortString();
    }

    private Info findAssistantByPackageName(String packageName) {
        for (int i = 0; i < mAvailableAssistants.size(); ++i) {
            Info info = mAvailableAssistants.get(i);
            if (info.component.getPackageName().equals(packageName)) {
                return info;
            }
        }
        return null;
    }

    private void addAssistServices() {
        PackageManager pm = getContext().getPackageManager();

        List<ResolveInfo> services = pm.queryIntentServices(
                new Intent(VoiceInteractionService.SERVICE_INTERFACE),
                PackageManager.GET_META_DATA);
        for (int i = 0; i < services.size(); ++i) {
            ResolveInfo resolveInfo = services.get(i);
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
        PackageManager pm = getContext().getPackageManager();

        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(Intent.ACTION_ASSIST),
                PackageManager.MATCH_DEFAULT_ONLY);
        for (int i = 0; i < activities.size(); ++i) {
            ResolveInfo resolveInfo = activities.get(i);
            mAvailableAssistants.add(new Info(
                    new ComponentName(resolveInfo.activityInfo.packageName,
                                      resolveInfo.activityInfo.name)));
        }
    }

    public ComponentName getCurrentAssist() {
        return mAssistUtils.getAssistComponentForUser(UserHandle.myUserId());
    }

    public void refreshAssistApps() {
        mAvailableAssistants.clear();
        addAssistServices();
        addAssistActivities();

        List<String> packages = new ArrayList<>();
        for (int i = 0; i < mAvailableAssistants.size(); ++i) {
            String packageName = mAvailableAssistants.get(i).component.getPackageName();
            if (packages.contains(packageName)) {
                // A service appears before an activity thus overrides it if from the same package.
                continue;
            }
            packages.add(packageName);
        }

        ComponentName currentAssist = getCurrentAssist();
        setPackageNames(packages.toArray(new String[packages.size()]),
                currentAssist == null ? null : currentAssist.getPackageName());
    }

    private static class Info {
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
