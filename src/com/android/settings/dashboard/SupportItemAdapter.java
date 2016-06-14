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
import android.app.DialogFragment;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.overlay.SupportFeatureProvider;
import com.android.settings.support.SupportDisclaimerDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.android.settings.overlay.SupportFeatureProvider.SupportType.CHAT;
import static com.android.settings.overlay.SupportFeatureProvider.SupportType.PHONE;

/**
 * Item adapter for support tiles.
 */
public final class SupportItemAdapter extends RecyclerView.Adapter<SupportItemAdapter.ViewHolder> {

    private static final int TYPE_TITLE = R.layout.support_item_title;
    private static final int TYPE_ESCALATION_OPTIONS = R.layout.support_escalation_options;
    private static final int TYPE_SUPPORT_TILE = R.layout.support_tile;
    private static final int TYPE_SIGN_IN_BUTTON = R.layout.support_sign_in_button;

    private final Activity mActivity;
    private final EscalationClickListener mEscalationClickListener;
    private final SupportFeatureProvider mSupportFeatureProvider;
    private final View.OnClickListener mItemClickListener;
    private final List<SupportData> mSupportData;

    private boolean mHasInternet;
    private Account mAccount;

    public SupportItemAdapter(Activity activity, SupportFeatureProvider supportFeatureProvider,
            View.OnClickListener itemClickListener) {
        mActivity = activity;
        mSupportFeatureProvider = supportFeatureProvider;
        mItemClickListener = itemClickListener;
        mEscalationClickListener = new EscalationClickListener();
        mSupportData = new ArrayList<>();
        // Optimistically assume we have Internet access. It will be updated later to correct value.
        mHasInternet = true;
        setAccount(mSupportFeatureProvider.getSupportEligibleAccount(mActivity));
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
            case TYPE_ESCALATION_OPTIONS:
                bindEscalationOptions(holder, data);
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

    public void setAccount(Account account) {
        if (!Objects.equals(mAccount, account)) {
            mAccount = account;
            refreshData();
        }
    }

    /**
     * Create data for the adapter. If there is already data in the adapter, they will be
     * destroyed and recreated.
     */
    private void refreshData() {
        mSupportData.clear();
        if (mAccount == null) {
            addSignInPromo();
        } else {
            addEscalationCards();
        }
        addMoreHelpItems();
        notifyDataSetChanged();
    }

    private void addEscalationCards() {
        if (mHasInternet) {
            mSupportData.add(new SupportData.Builder(TYPE_TITLE)
                    .setText1(R.string.support_escalation_title)
                    .setText2(R.string.support_escalation_summary)
                    .build());
        } else {
            mSupportData.add(new SupportData.Builder(TYPE_TITLE)
                    .setText1(R.string.support_offline_title)
                    .setText2(R.string.support_offline_summary)
                    .build());
        }
        final SupportData.Builder builder = new SupportData.Builder(TYPE_ESCALATION_OPTIONS);
        if (mSupportFeatureProvider.isSupportTypeEnabled(mActivity, PHONE)) {
            builder.setText1(R.string.support_escalation_by_phone);
            builder.setSummary1(mSupportFeatureProvider.getEstimatedWaitTime(mActivity, PHONE));
        }
        if (mSupportFeatureProvider.isSupportTypeEnabled(mActivity, CHAT)) {
            builder.setText2(R.string.support_escalation_by_chat);
            builder.setSummary2(mSupportFeatureProvider.getEstimatedWaitTime(mActivity, CHAT));
        }
        mSupportData.add(builder.build());
    }

    private void addSignInPromo() {
        mSupportData.add(new SupportData.Builder(TYPE_TITLE)
                .setText1(R.string.support_sign_in_required_title)
                .setText2(R.string.support_sign_in_required_summary)
                .build());
        mSupportData.add(new SupportData.Builder(TYPE_SIGN_IN_BUTTON)
                .setText1(R.string.support_sign_in_button_text)
                .setText2(R.string.support_sign_in_required_help)
                .build());
    }

    private void addMoreHelpItems() {
        mSupportData.add(new SupportData.Builder(TYPE_SUPPORT_TILE)
                .setIcon(R.drawable.ic_forum_24dp)
                .setText1(R.string.support_forum_title)
                .setIntent(mSupportFeatureProvider.getForumIntent())
                .build());
        mSupportData.add(new SupportData.Builder(TYPE_SUPPORT_TILE)
                .setIcon(R.drawable.ic_lightbulb_outline_24)
                .setText1(R.string.support_tips_and_tricks_title)
                .setIntent(mSupportFeatureProvider.getTipsAndTricksIntent(mActivity))
                .build());
        mSupportData.add(new SupportData.Builder(TYPE_SUPPORT_TILE)
                .setIcon(R.drawable.ic_help_24dp)
                .setText1(R.string.help_feedback_label)
                .setIntent(mSupportFeatureProvider.getHelpIntent(mActivity))
                .build());
    }

    private void bindEscalationOptions(ViewHolder holder, SupportData data) {
        if (data.text1 == 0) {
            holder.text1View.setVisibility(View.GONE);
        } else {
            holder.text1View.setText(data.text1);
            holder.text1View.setOnClickListener(mEscalationClickListener);
            holder.text1View.setEnabled(mHasInternet);
            holder.text1View.setVisibility(View.VISIBLE);
        }
        if (data.text2 == 0) {
            holder.text2View.setVisibility(View.GONE);
        } else {
            holder.text2View.setText(data.text2);
            holder.text2View.setOnClickListener(mEscalationClickListener);
            holder.text2View.setEnabled(mHasInternet);
            holder.text2View.setVisibility(View.VISIBLE);
        }
        if (holder.summary1View != null) {
            holder.summary1View.setText(data.summary1);
            holder.summary1View.setVisibility(mHasInternet ? View.VISIBLE : View.GONE);
        }
        if (holder.summary2View != null) {
            holder.summary2View.setText(data.summary2);
            holder.summary2View.setVisibility(mHasInternet ? View.VISIBLE : View.GONE);
        }
    }

    private void bindSignInPromoTile(ViewHolder holder, SupportData data) {
        holder.text1View.setText(data.text1);
        holder.text2View.setText(data.text2);
        holder.text1View.setOnClickListener(mEscalationClickListener);
        holder.text2View.setOnClickListener(mEscalationClickListener);
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
     * Show a disclaimer dialog and start support action after disclaimer has been acknowledged.
     */
    private void tryStartDisclaimerAndSupport(final @SupportFeatureProvider.SupportType int type) {
        if (mSupportFeatureProvider.shouldShowDisclaimerDialog(mActivity)) {
            DialogFragment fragment = SupportDisclaimerDialogFragment.newInstance(mAccount, type);
            fragment.show(mActivity.getFragmentManager(), SupportDisclaimerDialogFragment.TAG);
            return;
        }
        mSupportFeatureProvider.startSupport(mActivity, mAccount, type);
    }

    /**
     * Click handler for starting escalation options.
     */
    private final class EscalationClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            if (mAccount == null) {
                switch (v.getId()) {
                    case android.R.id.text1:
                        mActivity.startActivityForResult(
                                mSupportFeatureProvider.getAccountLoginIntent(),
                                0 /* requestCode */);
                        break;
                    case android.R.id.text2:
                        mActivity.startActivityForResult(
                                mSupportFeatureProvider.getSignInHelpIntent(mActivity),
                                0 /* requestCode */);
                        break;
                }
            } else {
                switch (v.getId()) {
                    case android.R.id.text1:
                        tryStartDisclaimerAndSupport(PHONE);
                        break;
                    case android.R.id.text2:
                        tryStartDisclaimerAndSupport(CHAT);
                        break;
                }
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
        final TextView summary1View;
        final TextView summary2View;

        ViewHolder(View itemView) {
            super(itemView);
            iconView = (ImageView) itemView.findViewById(android.R.id.icon);
            text1View = (TextView) itemView.findViewById(android.R.id.text1);
            text2View = (TextView) itemView.findViewById(android.R.id.text2);
            summary1View = (TextView) itemView.findViewById(R.id.summary1);
            summary2View = (TextView) itemView.findViewById(R.id.summary2);
        }
    }

    /**
     * Data for a single support item.
     */
    private static final class SupportData {

        final Intent intent;
        @LayoutRes
        final int type;
        @DrawableRes
        final int icon;
        @StringRes
        final int text1;
        @StringRes
        final int text2;
        final String summary1;
        final String summary2;

        private SupportData(Builder builder) {
            this.type = builder.mType;
            this.icon = builder.mIcon;
            this.text1 = builder.mText1;
            this.text2 = builder.mText2;
            this.summary1 = builder.mSummary1;
            this.summary2 = builder.mSummary2;
            this.intent = builder.mIntent;
        }

        static final class Builder {
            @LayoutRes
            private final int mType;
            @DrawableRes
            private int mIcon;
            @StringRes
            private int mText1;
            @StringRes
            private int mText2;
            private String mSummary1;
            private String mSummary2;
            private Intent mIntent;

            Builder(@LayoutRes int type) {
                mType = type;
            }

            Builder setIcon(@DrawableRes int icon) {
                mIcon = icon;
                return this;
            }

            Builder setText1(@StringRes int text1) {
                mText1 = text1;
                return this;
            }

            Builder setSummary1(String summary1) {
                mSummary1 = summary1;
                return this;
            }

            Builder setText2(@StringRes int text2) {
                mText2 = text2;
                return this;
            }

            Builder setSummary2(String summary2) {
                mSummary2 = summary2;
                return this;
            }

            Builder setIntent(Intent intent) {
                mIntent = intent;
                return this;
            }

            SupportData build() {
                return new SupportData(this);
            }
        }
    }
}
