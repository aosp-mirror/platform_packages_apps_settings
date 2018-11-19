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

package com.android.settings.wifi.dpp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;

import com.android.settings.core.InstrumentedFragment;
import com.android.settings.R;

public abstract class WifiDppQrCodeBaseFragment extends InstrumentedFragment {
    private TextView mTitle;
    private TextView mDescription;
    private SurfaceView mPreviewView;
    private TextView mErrorMessage; //optional, view used to surface connectivity errors to the user
    private Button mButtonLeft;
    private Button mButtonRight;

    abstract protected int getLayout();

    @Override
    public int getMetricsCategory() {
        //TODO:Should we use a new metrics category for Wi-Fi DPP?
        return MetricsProto.MetricsEvent.WIFI_NETWORK_DETAILS;
    }

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mTitle = view.findViewById(R.id.title);
        mDescription = view.findViewById(R.id.description);
        mPreviewView = view.findViewById(R.id.preview_view);
        mErrorMessage = view.findViewById(R.id.error_message);
        mButtonLeft = view.findViewById(R.id.button_left);
        mButtonRight = view.findViewById(R.id.button_right);
    }

    protected void setTitle(String title) {
        mTitle.setText(title);
    }

    protected void setDescription(String description) {
        mDescription.setText(description);
    }

    protected void setErrorMessage(String errorMessage) {
        if (mErrorMessage != null) {
            mErrorMessage.setText(errorMessage);
        }
    }

    protected void setLeftButtonText(String text) {
        mButtonLeft.setText(text);
    }

    protected void setRightButtonText(String text) {
        mButtonRight.setText(text);
    }

    protected void hideLeftButton() {
        mButtonLeft.setVisibility(View.INVISIBLE);
    }

    protected void hideRightButton() {
        mButtonRight.setVisibility(View.INVISIBLE);
    }

    protected void setLeftButtonOnClickListener(View.OnClickListener listener) {
        mButtonLeft.setOnClickListener(listener);
    }

    protected void setRightButtonOnClickListener(View.OnClickListener listener) {
        mButtonRight.setOnClickListener(listener);
    }
}
