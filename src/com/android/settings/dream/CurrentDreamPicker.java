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

package com.android.settings.dream;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.DreamInfo;
import com.android.settingslib.widget.CandidateInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CurrentDreamPicker extends RadioButtonPickerFragment {

    private DreamBackend mBackend;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mBackend = DreamBackend.getInstance(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.current_dream_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DREAM;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        Map<String, ComponentName> componentNameMap = getDreamComponentsMap();
        if (componentNameMap.get(key) != null) {
            mBackend.setActiveDream(componentNameMap.get(key));
            return true;
        }
        return false;
    }

    @Override
    protected String getDefaultKey() {
        return mBackend.getActiveDream().flattenToString();
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<DreamCandidateInfo> candidates;
        candidates = mBackend.getDreamInfos().stream()
                .map(DreamCandidateInfo::new)
                .collect(Collectors.toList());

        return candidates;
    }

    @Override
    protected void onSelectionPerformed(boolean success) {
        super.onSelectionPerformed(success);

        getActivity().finish();
    }

    private Map<String, ComponentName> getDreamComponentsMap() {
        Map<String, ComponentName> comps = new HashMap<>();
        mBackend.getDreamInfos()
                .forEach((info) ->
                        comps.put(info.componentName.flattenToString(), info.componentName));

        return comps;
    }

    private static final class DreamCandidateInfo extends CandidateInfo {
        private final CharSequence name;
        private final Drawable icon;
        private final String key;

        DreamCandidateInfo(DreamInfo info) {
            super(true);

            name = info.caption;
            icon = info.icon;
            key = info.componentName.flattenToString();
        }

        @Override
        public CharSequence loadLabel() {
            return name;
        }

        @Override
        public Drawable loadIcon() {
            return icon;
        }

        @Override
        public String getKey() {
            return key;
        }
    }
}
