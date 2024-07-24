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

package com.android.settings.remoteauth.enrolling

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R

class RemoteAuthEnrollEnrollingRecyclerViewAdapter :
    RecyclerView.Adapter<RemoteAuthEnrollEnrollingRecyclerViewAdapter.ViewHolder>() {
    var uiStates = listOf<DiscoveredAuthenticatorUiState>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.remote_auth_enrolling_authenticator_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(uiStates[position])
    }

    override fun getItemCount() = uiStates.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView: TextView = view.requireViewById(R.id.discovered_authenticator_name)
        private val selectButton: ImageView = view.requireViewById(R.id.authenticator_radio_button)
        private val checkedDrawable =
            view.context.getDrawable(R.drawable.ic_radio_button_checked_black_24dp)
        private val uncheckedDrawable =
            view.context.getDrawable(R.drawable.ic_radio_button_unchecked_black_24dp)

        fun bind(discoveredAuthenticatorUiState: DiscoveredAuthenticatorUiState) {
            titleTextView.text = discoveredAuthenticatorUiState.name
            selectButton.background = if (discoveredAuthenticatorUiState.isSelected) {
                checkedDrawable
            } else {
                uncheckedDrawable
            }
            selectButton.setOnClickListener { discoveredAuthenticatorUiState.onSelect() }
        }
    }
}
