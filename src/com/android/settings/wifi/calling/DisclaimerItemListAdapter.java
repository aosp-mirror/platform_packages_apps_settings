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

package com.android.settings.wifi.calling;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

import java.util.List;

/**
 * Adapter for disclaimer items list.
 */
public class DisclaimerItemListAdapter extends
        RecyclerView.Adapter<DisclaimerItemListAdapter.DisclaimerItemViewHolder> {

    private List<DisclaimerItem> mDisclaimerItemList;

    public DisclaimerItemListAdapter(List<DisclaimerItem> list) {
        mDisclaimerItemList = list;
    }

    @Override
    public DisclaimerItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.wfc_simple_disclaimer_item, null, false);
        return new DisclaimerItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DisclaimerItemViewHolder holder, int position) {
        holder.titleView.setText(mDisclaimerItemList.get(position).getTitleId());
        holder.descriptionView.setText(mDisclaimerItemList.get(position).getMessageId());
    }

    @Override
    public int getItemCount() {
        return mDisclaimerItemList.size();
    }

    public static class DisclaimerItemViewHolder extends RecyclerView.ViewHolder {
        @VisibleForTesting
        static final int ID_DISCLAIMER_ITEM_TITLE = R.id.disclaimer_title;
        @VisibleForTesting
        static final int ID_DISCLAIMER_ITEM_DESCRIPTION = R.id.disclaimer_desc;

        public final TextView titleView;
        public final TextView descriptionView;

        public DisclaimerItemViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(ID_DISCLAIMER_ITEM_TITLE);
            descriptionView = itemView.findViewById(ID_DISCLAIMER_ITEM_DESCRIPTION);
        }
    }
}
