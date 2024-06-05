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

package com.android.settings.remoteauth.settings


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R

class RemoteAuthSettingsRecyclerViewAdapter() :
    RecyclerView.Adapter<RemoteAuthSettingsRecyclerViewAdapter.ViewHolder>() {
    var uiStates = listOf<RemoteAuthAuthenticatorItemUiState>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.remote_auth_settings_authenticator_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(uiStates[position])
    }

    override fun getItemCount() = uiStates.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.requireViewById(R.id.authenticator_name_text)
        private val unregisterButton: ImageView = view.requireViewById(R.id.remove_icon)

        fun bind(authenticatorUiState: RemoteAuthAuthenticatorItemUiState) {
            titleTextView.text = authenticatorUiState.name
            unregisterButton.setOnClickListener { authenticatorUiState.unregister() }
        }
    }
}
