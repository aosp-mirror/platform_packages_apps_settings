/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.ArrayList;

public class AudioSharingDeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "AudioSharingDeviceAdapter";
    private final ArrayList<AudioSharingDeviceItem> mDevices;
    private final OnClickListener mOnClickListener;
    private final String mPrefix;

    public AudioSharingDeviceAdapter(
            ArrayList<AudioSharingDeviceItem> devices, OnClickListener listener, String prefix) {
        mDevices = devices;
        mOnClickListener = listener;
        mPrefix = prefix;
    }

    private class AudioSharingDeviceViewHolder extends RecyclerView.ViewHolder {
        private final Button mButtonView;

        AudioSharingDeviceViewHolder(View view) {
            super(view);
            mButtonView = view.findViewById(R.id.device_button);
        }

        public void bindView(int position) {
            if (mButtonView != null) {
                mButtonView.setText(mPrefix + mDevices.get(position).getName());
                mButtonView.setOnClickListener(
                        v -> mOnClickListener.onClick(mDevices.get(position)));
            } else {
                Log.w(TAG, "bind view skipped due to button view is null");
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.audio_sharing_device_item, parent, false);
        return new AudioSharingDeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((AudioSharingDeviceViewHolder) holder).bindView(position);
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public interface OnClickListener {
        /** Called when an item has been clicked. */
        void onClick(AudioSharingDeviceItem item);
    }
}
