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
package com.android.settings.dashboard;

import android.accounts.Account;
import android.annotation.DrawableRes;
import android.annotation.LayoutRes;
import android.annotation.StringRes;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.overlay.SupportFeatureProvider;

import java.util.ArrayList;
import java.util.List;

import static com.android.settings.overlay.SupportFeatureProvider.SupportType.CHAT;
import static com.android.settings.overlay.SupportFeatureProvider.SupportType.EMAIL;
import static com.android.settings.overlay.SupportFeatureProvider.SupportType.PHONE;

/**
 * Item adapter for support tiles.
 */
public final class SupportItemAdapter extends RecyclerView.Adapter<SupportItemAdapter.ViewHolder> {

    private static final int TYPE_TITLE = R.layout.support_item_title;
    private static final int TYPE_SUBTITLE = R.layout.support_item_subtitle;
    private static final int TYPE_ESCALATION_CARD = R.layout.support_escalation_card;
    private static final int TYPE_SUPPORT_TILE = R.layout.support_tile;
    private static final int TYPE_SIGN_IN_BUTTON = R.layout.support_sign_in_button;

    private final Activity mActivity;
    private final SignInPromoClickListener mSignInPromoClickListener;
    private final SupportFeatureProvider mSupportFeatureProvider;
    private final View.OnClickListener mItemClickListener;
    private final List<SupportData> mSupportData;

    private boolean mHasInternet;

    public SupportItemAdapter(Activity activity, SupportFeatureProvider supportFeatureProvider,
            View.OnClickListener itemClickListener) {
        mActivity = activity;
        mSupportFeatureProvider = supportFeatureProvider;
        mItemClickListener = itemClickListener;
        mSignInPromoClickListener = new SignInPromoClickListener();
        mSupportData = new ArrayList<>();
        // Optimistically assume we have Internet access. It will be updated later to correct value.
        mHasInternet = true;
        refreshData();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final SupportData data = mSupportData.get(position);
        switch (holder.getItemViewType()) {
            case TYPE_SIGN_IN_BUTTON:
                bindSignInPromoTile(holder, data);
                break;
            default:
                bindSupportTile(holder, data);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mSupportData.get(position).type;
    }

    @Override
    public int getItemCount() {
        return mSupportData.size();
    }

    /**
     * Called when a support item is clicked.
     */
    public void onItemClicked(int position) {
        if (position >= 0 && position < mSupportData.size()) {
            final SupportData data = mSupportData.get(position);
            if (data.intent != null) {
                mActivity.startActivityForResult(data.intent, 0);
            }
        }
    }

    public void setHasInternet(boolean hasInternet) {
        if (mHasInternet != hasInternet) {
            mHasInternet = hasInternet;
            refreshData();
        }
    }

    /**
     * Create data for the adapter. If there is already data in the adapter, they will be
     * destroyed and recreated.
     */
    public void refreshData() {
        mSupportData.clear();
        final Account[] accounts = mSupportFeatureProvider.getSupportEligibleAccounts(mActivity);
        if (accounts.length == 0) {
            addSignInPromo();
        } else {
            addEscalationCards(accounts[0]);
        }
        addMoreHelpItems();
        notifyDataSetChanged();
    }

    private void addEscalationCards(Account account) {
        if (mHasInternet) {
            mSupportData.add(new SupportData(TYPE_TITLE, 0 /* icon */,
                    R.string.support_escalation_title, R.string.support_escalation_summary,
                    null /* intent */));
        } else {
            mSupportData.add(new SupportData(TYPE_TITLE, 0 /* icon */,
                    R.string.support_offline_title, R.string.support_offline_summary,
                    null /* intent */));
        }
        if (mSupportFeatureProvider.isSupportTypeEnabled(mActivity, PHONE)) {
            mSupportData.add(new SupportData(TYPE_ESCALATION_CARD, R.drawable.ic_call_24dp,
                    R.string.support_escalation_by_phone, 0 /* summary */,
                    mSupportFeatureProvider.getSupportIntent(mActivity, account, PHONE)));
        }
        if (mSupportFeatureProvider.isSupportTypeEnabled(mActivity, EMAIL)) {
            mSupportData.add(new SupportData(TYPE_ESCALATION_CARD, R.drawable.ic_mail_24dp,
                    R.string.support_escalation_by_email, 0 /* summary */,
                    mSupportFeatureProvider.getSupportIntent(mActivity, account, EMAIL)));
        }
        if (mSupportFeatureProvider.isSupportTypeEnabled(mActivity, CHAT)) {
            mSupportData.add(new SupportData(TYPE_ESCALATION_CARD, R.drawable.ic_chat_24dp,
                    R.string.support_escalation_by_chat, 0 /* summary */,
                    mSupportFeatureProvider.getSupportIntent(mActivity, account, CHAT)));
        }
    }

    private void addSignInPromo() {
        mSupportData.add(new SupportData(TYPE_TITLE, 0 /* icon */,
                R.string.support_sign_in_required_title, R.string.support_sign_in_required_summary,
                null /* intent */));
        mSupportData.add(new SupportData(TYPE_SIGN_IN_BUTTON, 0 /* icon */,
                R.string.support_sign_in_button_text, R.string.support_sign_in_required_help,
                null /* intent */));

    }

    private void addMoreHelpItems() {
        mSupportData.add(new SupportData(TYPE_SUBTITLE, 0 /* icon */,
                R.string.support_more_help_title, 0 /* summary */, null /* intent */));
        mSupportData.add(new SupportData(TYPE_SUPPORT_TILE, R.drawable.ic_forum_24dp,
                R.string.support_forum_title, 0 /* summary */,
                mSupportFeatureProvider.getForumIntent()));
        mSupportData.add(new SupportData(TYPE_SUPPORT_TILE, R.drawable.ic_help_24dp,
                R.string.support_articles_title, 0 /* summary */, null /*intent */));
        mSupportData.add(new SupportData(TYPE_SUPPORT_TILE, R.drawable.ic_feedback_24dp,
                R.string.support_feedback_title, 0 /* summary */, null /*intent */));
    }

    private void bindSignInPromoTile(ViewHolder holder, SupportData data) {
        holder.text1View.setText(data.text1);
        holder.text2View.setText(data.text2);
        holder.text1View.setOnClickListener(mSignInPromoClickListener);
        holder.text2View.setOnClickListener(mSignInPromoClickListener);
    }

    private void bindSupportTile(ViewHolder holder, SupportData data) {
        if (holder.iconView != null) {
            holder.iconView.setImageResource(data.icon);
        }
        if (holder.text1View != null) {
            holder.text1View.setText(data.text1);
        }
        if (holder.text2View != null) {
            holder.text2View.setText(data.text2);
        }
        holder.itemView.setOnClickListener(mItemClickListener);
    }

    /**
     * Click handler for sign-in promo.
     */
    private final class SignInPromoClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case android.R.id.text1:
                    mActivity.startActivityForResult(
                            mSupportFeatureProvider.getAccountLoginIntent(), 0 /* requestCode */);
                    break;
                case android.R.id.text2:
                    mActivity.startActivityForResult(
                            mSupportFeatureProvider.getSignInHelpIntent(mActivity),
                            0 /* requestCode */);
                    break;
            }
        }
    }

    /**
     * {@link RecyclerView.ViewHolder} for support items.
     */
    static final class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView iconView;
        final TextView text1View;
        final TextView text2View;

        ViewHolder(View itemView) {
            super(itemView);
            iconView = (ImageView) itemView.findViewById(android.R.id.icon);
            text1View = (TextView) itemView.findViewById(android.R.id.text1);
            text2View = (TextView) itemView.findViewById(android.R.id.text2);
        }
    }

    /**
     * Data for a single support item.
     */
    private static final class SupportData {

        final Intent intent;
        @LayoutRes final int type;
        @DrawableRes final int icon;
        @StringRes final int text1;
        @StringRes final int text2;

        SupportData(@LayoutRes int type, @DrawableRes int icon, @StringRes int text1,
                @StringRes int text2, Intent intent) {
            this.type = type;
            this.icon = icon;
            this.text1 = text1;
            this.text2 = text2;
            this.intent = intent;
        }
    }
}
