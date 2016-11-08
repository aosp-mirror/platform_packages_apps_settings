/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Top level fragment for gesture settings.
 * This will create individual switch preference for each gesture and handle updates when each
 * preference is updated
 */
public class GestureSettings extends DashboardFragment {

    private static final String TAG = "GestureSettings";
    private List<GesturePreference> mPreferences;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.gesture_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final AmbientDisplayConfiguration ambientConfig = new AmbientDisplayConfiguration(context);
        final List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new SwipeToNotificationPreferenceController(context));
        controllers.add(new DoubleTapPowerPreferenceController(context));
        controllers.add(new DoubleTwistPreferenceController(context));
        controllers.add(new PickupGesturePreferenceController(
                context, ambientConfig, UserHandle.myUserId()));
        controllers.add(new DoubleTapScreenPreferenceController(
                context, ambientConfig, UserHandle.myUserId()));
        return controllers;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        mPreferences = new ArrayList();
        addPreferenceToTrackingList(SwipeToNotificationPreferenceController.class);
        addPreferenceToTrackingList(DoubleTapScreenPreferenceController.class);
        addPreferenceToTrackingList(DoubleTwistPreferenceController.class);
        addPreferenceToTrackingList(PickupGesturePreferenceController.class);
        addPreferenceToTrackingList(DoubleTapPowerPreferenceController.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        RecyclerView listview = getListView();
        listview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    for (GesturePreference pref : mPreferences) {
                        pref.setScrolling(true);
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    for (GesturePreference pref : mPreferences) {
                        pref.setScrolling(false);
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        for (GesturePreference preference : mPreferences) {
            preference.onViewVisible();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        for (GesturePreference preference : mPreferences) {
            preference.onViewInvisible();
        }
    }

    @Override
    protected String getCategoryKey() {
        return null;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_gestures;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SETTINGS_GESTURES;
    }

    private <T extends PreferenceController> void addPreferenceToTrackingList(Class<T> clazz) {
        final PreferenceController controller = getPreferenceController(clazz);
        final Preference preference = findPreference(controller.getPreferenceKey());
        if (preference != null && preference instanceof GesturePreference) {
            mPreferences.add((GesturePreference) preference);
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.gesture_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    AmbientDisplayConfiguration ambientConfig
                            = new AmbientDisplayConfiguration(context);
                    new DoubleTapPowerPreferenceController(context)
                            .updateNonIndexableKeys(result);
                    new PickupGesturePreferenceController(
                            context, ambientConfig, UserHandle.myUserId())
                            .updateNonIndexableKeys(result);
                    new DoubleTapScreenPreferenceController(
                            context, ambientConfig, UserHandle.myUserId())
                            .updateNonIndexableKeys(result);
                    new SwipeToNotificationPreferenceController(context)
                            .updateNonIndexableKeys(result);
                    new DoubleTwistPreferenceController(context)
                            .updateNonIndexableKeys(result);
                    return result;
                }
            };
}
