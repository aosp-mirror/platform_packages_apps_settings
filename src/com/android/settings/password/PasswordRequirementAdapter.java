/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.password;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.password.PasswordRequirementAdapter.PasswordRequirementViewHolder;

/**
 * Used in {@link ChooseLockPassword} to show password requirements.
 */
public class PasswordRequirementAdapter extends
        RecyclerView.Adapter<PasswordRequirementViewHolder> {

    private String[] mRequirements;
    private Context mContext;
    private boolean mIsTooShortError = true;

    public PasswordRequirementAdapter(Context context) {
        mContext = context;
        setHasStableIds(true);
    }

    @Override
    public PasswordRequirementViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.password_requirement_item, parent, false);
        return new PasswordRequirementViewHolder(v);
    }

    @Override
    public int getItemCount() {
        return  mRequirements.length;
    }

    public void setRequirements(String[] requirements, boolean isPasswordShort) {
        mRequirements = requirements;
        mIsTooShortError = isPasswordShort;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return mRequirements[position].hashCode();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull PasswordRequirementViewHolder holder) {
        holder.mDescriptionText.announceForAccessibility(holder.mDescriptionText.getText());
    }

    @Override
    public void onBindViewHolder(PasswordRequirementViewHolder holder, int position) {
        final int fontSize = mContext.getResources().getDimensionPixelSize(
                R.dimen.password_requirement_font_size);
        holder.mDescriptionText.setText(mRequirements[position]);
        if (mIsTooShortError) {
            holder.mDescriptionText.setTextAppearance(R.style.ScreenLockPasswordHintTextFontStyle);
        } else {
            holder.mDescriptionText.
                    setTextAppearance(R.style.ScreenLockPasswordHintTextFontStyleError);
        }
        holder.mDescriptionText.setTextSize(fontSize / mContext.getResources()
                .getDisplayMetrics().scaledDensity);
    }

    public static class PasswordRequirementViewHolder extends RecyclerView.ViewHolder {
        private TextView mDescriptionText;

        public PasswordRequirementViewHolder(View itemView) {
            super(itemView);
            mDescriptionText = (TextView) itemView;
        }
    }

}
