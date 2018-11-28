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

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.LinearLayout;

import androidx.lifecycle.LiveData;
import androidx.slice.Slice;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;

import com.android.settings.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.overlay.FeatureFactory;

import java.util.ArrayList;
import java.util.List;

public class PanelFragment extends Fragment {

    private static final String TAG = "PanelFragment";

    private List<SliceView> mSliceViewList;
    private List<LiveData<Slice>> mSliceDataList;
    private LinearLayout mPanelLayout;

    public PanelFragment() {
        mSliceViewList = new ArrayList<>();
        mSliceDataList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        final View view = inflater.inflate(R.layout.panel_layout, container, false);

        mPanelLayout = view.findViewById(R.id.panel_parent_layout);
        final Bundle arguments = getArguments();

        final String panelType = arguments.getString(SettingsPanelActivity.KEY_PANEL_TYPE_ARGUMENT);

        final PanelContent panel = FeatureFactory.getFactory(activity)
                .getPanelFeatureProvider()
                .getPanel(activity, panelType);

        activity.setTitle(panel.getTitle());


        for (Uri uri : panel.getSlices()) {
            final SliceView sliceView = new SliceView(activity);
            mPanelLayout.addView(sliceView);
            final LiveData<Slice> liveData = SliceLiveData.fromUri(activity, uri);
            liveData.observe(this /* lifecycleOwner */, sliceView);

            mSliceDataList.add(liveData);
            mSliceViewList.add(sliceView);
        }

        return view;
    }
}
