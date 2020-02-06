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

package com.android.settings.notification.zen;

import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS;
import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES;

import android.app.Activity;
import android.app.Application;
import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class ZenModePeopleSettings extends ZenModeSettingsBase implements Indexable {

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final Activity activity = getActivity();
        final Application app;
        if (activity != null) {
            app = activity.getApplication();
        } else {
            app = null;
        }
        return buildPreferenceControllers(
                context, getSettingsLifecycle(), app, this, getFragmentManager());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, Application app, Fragment host, FragmentManager fragmentManager) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new ZenModePriorityMessagesPreferenceController(context, lifecycle));
        controllers.add(new ZenModeStarredContactsPreferenceController(context, lifecycle,
                PRIORITY_CATEGORY_MESSAGES, "zen_mode_starred_contacts_messages"));
        controllers.add(new ZenModePriorityCallsPreferenceController(context, lifecycle));
        controllers.add(new ZenModeStarredContactsPreferenceController(context, lifecycle,
                PRIORITY_CATEGORY_CALLS, "zen_mode_starred_contacts_callers"));
        controllers.add(new ZenModeRepeatCallersPreferenceController(context, lifecycle,
                context.getResources().getInteger(com.android.internal.R.integer
                        .config_zen_repeat_callers_threshold)));
        controllers.add(new ZenModePriorityConversationsPreferenceController(context, lifecycle));
        controllers.add(new ZenModeSettingsFooterPreferenceController(context, lifecycle,
                fragmentManager));
        return controllers;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_people_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DND_PEOPLE;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.zen_mode_people_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null, null, null, null);
                }
            };
}
