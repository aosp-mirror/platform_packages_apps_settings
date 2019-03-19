/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.panel;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.panel.PanelLoggingContract.PanelClosedKeys;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class PanelFragment extends Fragment {

    private static final String TAG = "PanelFragment";

    private TextView mTitleView;
    private Button mSeeMoreButton;
    private Button mDoneButton;
    private RecyclerView mPanelSlices;

    private PanelContent mPanel;
    private MetricsFeatureProvider mMetricsProvider;

    @VisibleForTesting
    PanelSlicesAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        final View view = inflater.inflate(R.layout.panel_layout, container, false);

        mPanelSlices = view.findViewById(R.id.panel_parent_layout);
        mSeeMoreButton = view.findViewById(R.id.see_more);
        mDoneButton = view.findViewById(R.id.done);
        mTitleView = view.findViewById(R.id.panel_title);

        final Bundle arguments = getArguments();
        final String panelType =
                arguments.getString(SettingsPanelActivity.KEY_PANEL_TYPE_ARGUMENT);
        final String callingPackageName =
                arguments.getString(SettingsPanelActivity.KEY_CALLING_PACKAGE_NAME);
        final String mediaPackageName =
                arguments.getString(SettingsPanelActivity.KEY_MEDIA_PACKAGE_NAME);

        // TODO (b/124399577) transform interface to take a context and bundle.
        mPanel = FeatureFactory.getFactory(activity)
                .getPanelFeatureProvider()
                .getPanel(activity, panelType, mediaPackageName);

        mMetricsProvider = FeatureFactory.getFactory(activity).getMetricsFeatureProvider();
        // Log panel opened.
        mMetricsProvider.action(
                0 /* attribution */,
                SettingsEnums.PAGE_VISIBLE /* opened panel - Action */,
                mPanel.getMetricsCategory(),
                callingPackageName,
                0 /* value */);

        mAdapter = new PanelSlicesAdapter(this, mPanel);

        mPanelSlices.setHasFixedSize(true);
        mPanelSlices.setLayoutManager(new LinearLayoutManager((activity)));
        mPanelSlices.setAdapter(mAdapter);

        mTitleView.setText(mPanel.getTitle());

        mSeeMoreButton.setOnClickListener(getSeeMoreListener());
        mDoneButton.setOnClickListener(getCloseListener());

        //If getSeeMoreIntent() is null, hide the mSeeMoreButton.
        if (mPanel.getSeeMoreIntent() == null) {
            mSeeMoreButton.setVisibility(View.GONE);
        }

        return view;
    }

    @VisibleForTesting
    View.OnClickListener getSeeMoreListener() {
        return (v) -> {
            mMetricsProvider.action(
                    0 /* attribution */,
                    SettingsEnums.PAGE_HIDE ,
                    mPanel.getMetricsCategory(),
                    PanelClosedKeys.KEY_SEE_MORE,
                    0 /* value */);
            final FragmentActivity activity = getActivity();
            activity.startActivityForResult(mPanel.getSeeMoreIntent(), 0);
            activity.finish();
        };
    }

    @VisibleForTesting
    View.OnClickListener getCloseListener() {
        return (v) -> {
            mMetricsProvider.action(
                    0 /* attribution */,
                    SettingsEnums.PAGE_HIDE,
                    mPanel.getMetricsCategory(),
                    PanelClosedKeys.KEY_DONE,
                    0 /* value */);
            getActivity().finish();
        };
    }
}
