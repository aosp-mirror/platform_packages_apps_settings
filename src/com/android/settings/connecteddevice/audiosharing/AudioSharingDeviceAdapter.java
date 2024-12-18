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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.List;

public class AudioSharingDeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "AudioSharingDeviceAdapter";

    private final Context mContext;
    private final List<AudioSharingDeviceItem> mDevices;
    private final OnClickListener mOnClickListener;
    private final ActionType mType;

    public AudioSharingDeviceAdapter(
            @NonNull Context context,
            @NonNull List<AudioSharingDeviceItem> devices,
            @NonNull OnClickListener listener,
            @NonNull ActionType type) {
        mContext = context;
        mDevices = devices;
        mOnClickListener = listener;
        mType = type;
    }

    /**
     * The action type when user click on the item.
     *
     * <p>We choose the item text based on this type.
     */
    public enum ActionType {
        // Click on the item will add the item to audio sharing
        SHARE,
        // Click on the item will remove the item from audio sharing
        REMOVE,
    }

    private class AudioSharingDeviceViewHolder extends RecyclerView.ViewHolder {
        private final Button mButtonView;

        AudioSharingDeviceViewHolder(View view) {
            super(view);
            mButtonView = view.findViewById(R.id.device_button);
        }

        public void bindView(int position) {
            if (mButtonView != null) {
                String btnText = switch (mType) {
                    case SHARE ->
                            mContext.getString(
                                    R.string.audio_sharing_share_with_button_label,
                                    mDevices.get(position).getName());
                    case REMOVE ->
                            mContext.getString(
                                    R.string.audio_sharing_disconnect_device_button_label,
                                    mDevices.get(position).getName());
                };
                mButtonView.setText(btnText);
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
