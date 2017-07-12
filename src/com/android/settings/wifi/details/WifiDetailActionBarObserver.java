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
package com.android.settings.wifi.details;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;

/**
 * ActionBar lifecycle observer for {@link WifiNetworkDetailsFragment}.
 */
public class WifiDetailActionBarObserver implements LifecycleObserver, OnCreate {

    private final Fragment mFragment;
    private final Context mContext;

    public WifiDetailActionBarObserver(Context context, Fragment fragment) {
        mContext = context;
        mFragment = fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (mFragment.getActivity() != null) {
            mFragment.getActivity().getActionBar()
                    .setTitle(mContext.getString(R.string.wifi_details_title));
        }
    }
}
