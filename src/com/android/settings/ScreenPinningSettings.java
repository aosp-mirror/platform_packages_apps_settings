/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.settings;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;

/**
 * Screen pinning settings.
 */
public class ScreenPinningSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, Indexable {

    private SwitchBar mSwitchBar;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();
        mSwitchBar.setChecked(isLockToAppEnabled());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_pinning_instructions, null);
    }

    private boolean isLockToAppEnabled() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.LOCK_TO_APP_ENABLED)
                != 0;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    private void setLockToAppEnabled(boolean isEnabled) {
        Settings.System.putInt(getContentResolver(), Settings.System.LOCK_TO_APP_ENABLED,
                isEnabled ? 1 : 0);
    }

    /**
     * Listens to the state change of the lock-to-app master switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setLockToAppEnabled(isChecked);
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.screen_pinning_title);
                data.screenTitle = res.getString(R.string.screen_pinning_title);
                result.add(data);

                // Screen pinning description.
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.screen_pinning_description);
                data.screenTitle = res.getString(R.string.screen_pinning_title);
                result.add(data);

                return result;
            }
        };
}
