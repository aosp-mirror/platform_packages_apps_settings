/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.core;

import static com.android.internal.jank.InteractionJankMonitor.CUJ_SETTINGS_PAGE_SCROLL;
import static com.android.internal.jank.InteractionJankMonitor.Configuration;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.XmlRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.jank.InteractionJankMonitor;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.survey.SurveyMixin;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.instrumentation.SettingsJankMonitor;
import com.android.settingslib.core.instrumentation.VisibilityLoggerMixin;
import com.android.settingslib.core.lifecycle.ObservablePreferenceFragment;

/**
 * Instrumented fragment that logs visibility state.
 */
public abstract class InstrumentedPreferenceFragment extends ObservablePreferenceFragment
        implements Instrumentable {

    private static final String TAG = "InstrumentedPrefFrag";

    protected MetricsFeatureProvider mMetricsFeatureProvider;

    // metrics placeholder value. Only use this for development.
    protected final int PLACEHOLDER_METRIC = 10000;

    private VisibilityLoggerMixin mVisibilityLoggerMixin;
    private RecyclerView.OnScrollListener mOnScrollListener;

    @Override
    public void onAttach(Context context) {
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        // Mixin that logs visibility change for activity.
        mVisibilityLoggerMixin = new VisibilityLoggerMixin(getMetricsCategory(),
                mMetricsFeatureProvider);
        getSettingsLifecycle().addObserver(mVisibilityLoggerMixin);
        getSettingsLifecycle().addObserver(new SurveyMixin(this, getClass().getSimpleName()));
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Override the OnPreferenceTreeClickListener in super.onStart() to inject jank detection.
        getPreferenceManager().setOnPreferenceTreeClickListener((preference) -> {
            if (preference instanceof TwoStatePreference twoStatePreference) {
                SettingsJankMonitor.detectSwitchPreferenceClickJank(
                        getListView(), twoStatePreference);
            }
            return onPreferenceTreeClick(preference);
        });
    }

    @Override
    public void onResume() {
        mVisibilityLoggerMixin.setSourceMetricsCategory(getActivity());
        // Add scroll listener to trace interaction jank.
        final RecyclerView recyclerView = getListView();
        if (recyclerView != null) {
            mOnScrollListener = new OnScrollListener(getClass().getName());
            recyclerView.addOnScrollListener(mOnScrollListener);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        final RecyclerView recyclerView = getListView();
        if (mOnScrollListener != null) {
            recyclerView.removeOnScrollListener(mOnScrollListener);
            mOnScrollListener = null;
        }
        super.onPause();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final int resId = getPreferenceScreenResId();
        if (resId > 0) {
            addPreferencesFromResource(resId);
        }
    }

    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        updateActivityTitleWithScreenTitle(getPreferenceScreen());
    }

    @Override
    public <T extends Preference> T findPreference(CharSequence key) {
        if (key == null) {
            return null;
        }
        return super.findPreference(key);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        writePreferenceClickMetric(preference);
        return super.onPreferenceTreeClick(preference);
    }

    protected final Context getPrefContext() {
        return getPreferenceManager().getContext();
    }

    /**
     * Get the res id for static preference xml for this fragment.
     */
    protected int getPreferenceScreenResId() {
        return -1;
    }

    protected void writeElapsedTimeMetric(int action, String key) {
        mVisibilityLoggerMixin.writeElapsedTimeMetric(action, key);
    }

    protected void writePreferenceClickMetric(Preference preference) {
        mMetricsFeatureProvider.logClickedPreference(preference, getMetricsCategory());
    }

    private void updateActivityTitleWithScreenTitle(PreferenceScreen screen) {
        if (screen != null) {
            final CharSequence title = screen.getTitle();
            if (!TextUtils.isEmpty(title)) {
                getActivity().setTitle(title);
            } else {
                Log.w(TAG, "Screen title missing for fragment " + this.getClass().getName());
            }
        }
    }

    private static final class OnScrollListener extends RecyclerView.OnScrollListener {
        private final InteractionJankMonitor mMonitor = InteractionJankMonitor.getInstance();
        private final String mClassName;

        private OnScrollListener(String className) {
            mClassName = className;
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            switch (newState) {
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    final Configuration.Builder builder =
                            Configuration.Builder.withView(CUJ_SETTINGS_PAGE_SCROLL, recyclerView)
                                    .setTag(mClassName);
                    mMonitor.begin(builder);
                    break;
                case RecyclerView.SCROLL_STATE_IDLE:
                    mMonitor.end(CUJ_SETTINGS_PAGE_SCROLL);
                    break;
                default:
            }
        }
    }
}
