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

package com.android.settings.security;

import android.net.Uri;
import android.security.AppUriAuthenticationPolicy;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.List;

/**
 * Child adapter for the requesting credential management app. This adapter displays the list of
 * URIs for each app in the requesting app's authentication policy, when
 * {@link RequestManageCredentials} is started.
 *
 * @hide
 * @see CredentialManagementAppAdapter
 * @see RequestManageCredentials
 * @see AppUriAuthenticationPolicy
 */
public class UriAuthenticationPolicyAdapter extends
        RecyclerView.Adapter<UriAuthenticationPolicyAdapter.UriViewHolder> {

    private final List<Uri> mUris;

    /**
     * View holder for each URI which is part of the authentication policy in the
     * request manage credentials screen.
     */
    public class UriViewHolder extends RecyclerView.ViewHolder {
        TextView mUriNameView;

        public UriViewHolder(@NonNull View view) {
            super(view);
            mUriNameView = itemView.findViewById(R.id.uri_name);
        }
    }

    UriAuthenticationPolicyAdapter(List<Uri> uris) {
        this.mUris = uris;
    }

    @Override
    public UriAuthenticationPolicyAdapter.UriViewHolder onCreateViewHolder(ViewGroup parent,
            int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.app_authentication_uri_item, parent, false);
        return new UriViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UriAuthenticationPolicyAdapter.UriViewHolder holder,
            int position) {
        Uri uri = mUris.get(position);
        holder.mUriNameView.setText(Uri.decode(uri.toString()));
    }

    @Override
    public int getItemCount() {
        return mUris.size();
    }
}
