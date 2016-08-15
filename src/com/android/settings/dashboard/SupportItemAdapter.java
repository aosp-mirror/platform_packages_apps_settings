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
import android.app.ActivityManager;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto;
import com.android.settings.R;
import com.android.settings.overlay.SupportFeatureProvider;
import com.android.settings.support.SupportDisclaimerDialogFragment;
import com.android.settings.support.SupportPhone;
import com.android.settings.support.SupportPhoneDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.android.settings.overlay.SupportFeatureProvider.SupportType.CHAT;
import static com.android.settings.overlay.SupportFeatureProvider.SupportType.PHONE;

/**
 * Item adapter for support tiles.
 */
public final class SupportItemAdapter extends RecyclerView.Adapter<SupportItemAdapter.ViewHolder> {

    private static final String STATE_SELECTED_COUNTRY = "STATE_SELECTED_COUNTRY";
    private static final int TYPE_ESCALATION_OPTIONS = R.layout.support_escalation_options;
    private static final int TYPE_ESCALATION_OPTIONS_OFFLINE =
            R.layout.support_offline_escalation_options;
    private static final int TYPE_SUPPORT_TILE = R.layout.support_tile;
    private static final int TYPE_SUPPORT_TILE_SPACER = R.layout.support_tile_spacer;
    private static final int TYPE_SIGN_IN_BUTTON = R.layout.support_sign_in_button;

    private final Activity mActivity;
    private final EscalationClickListener mEscalationClickListener;
    private final SpinnerItemSelectListener mSpinnerItemSelectListener;
    private final SupportFeatureProvider mSupportFeatureProvider;
    private final View.OnClickListener mItemClickListener;
    private final List<SupportData> mSupportData;

    private String mSelectedCountry;
    private boolean mHasInternet;
    private Account mAccount;

    public SupportItemAdapter(Activity activity, Bundle savedInstanceState,
            SupportFeatureProvider supportFeatureProvider, View.OnClickListener itemClickListener) {
        mActivity = activity;
        mSupportFeatureProvider = supportFeatureProvider;
        mItemClickListener = itemClickListener;
        mEscalationClickListener = new EscalationClickListener();
        mSpinnerItemSelectListener = new SpinnerItemSelectListener();
        mSupportData = new ArrayList<>();
        // Optimistically assume we have Internet access. It will be updated later to correct value.
        mHasInternet = true;
        if (savedInstanceState != null) {
            mSelectedCountry = savedInstanceState.getString(STATE_SELECTED_COUNTRY);
        } else {
            mSelectedCountry = mSupportFeatureProvider.getCurrentCountryCodeIfHasConfig(PHONE);
        }
        mAccount = mSupportFeatureProvider.getSupportEligibleAccount(mActivity);
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
                bindSignInPromoTile(holder, (EscalationData) data);
                break;
            case TYPE_ESCALATION_OPTIONS:
                bindEscalationOptions(holder, (EscalationData) data);
                break;
            case TYPE_ESCALATION_OPTIONS_OFFLINE:
                bindOfflineEscalationOptions(holder, (OfflineEscalationData) data);
                break;
            case TYPE_SUPPORT_TILE_SPACER:
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
            if (data.intent != null &&
                    mActivity.getPackageManager().resolveActivity(data.intent, 0) != null) {
                if (data.metricsEvent >= 0) {
                    MetricsLogger.action(mActivity, data.metricsEvent);
                }
                mActivity.startActivityForResult(data.intent, 0);
            }
        }
    }

    public void setHasInternet(boolean hasInternet) {
        if (mHasInternet != hasInternet) {
            mHasInternet = hasInternet;
            refreshEscalationCards();
        }
    }

    public void setAccount(Account account) {
        if (!Objects.equals(mAccount, account)) {
            mAccount = account;
            mSupportFeatureProvider.refreshOperationRules();
            refreshEscalationCards();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_SELECTED_COUNTRY, mSelectedCountry);
    }

    /**
     * Create data for the adapter. If there is already data in the adapter, they will be
     * destroyed and recreated.
     */
    private void refreshData() {
        mSupportData.clear();
        addEscalationCards();
        addMoreHelpItems();
        notifyDataSetChanged();
    }

    /**
     * Adds 1 escalation card. Based on current phone state, the escalation card can display
     * different content.
     */
    private void addEscalationCards() {
        if (mAccount == null) {
            addSignInPromo();
        } else if (mHasInternet) {
            addOnlineEscalationCards();
        } else {
            addOfflineEscalationCards();
        }
    }

    /**
     * Finds and refreshes escalation card data.
     */
    private void refreshEscalationCards() {
        if (getItemCount() > 0) {
            final int itemType = getItemViewType(0 /* position */);
            if (itemType == TYPE_SIGN_IN_BUTTON
                    || itemType == TYPE_ESCALATION_OPTIONS
                    || itemType == TYPE_ESCALATION_OPTIONS_OFFLINE) {
                mSupportData.remove(0 /* position */);
                addEscalationCards();
                notifyItemChanged(0 /* position */);
            }
        }
    }

    private void addOnlineEscalationCards() {
        final boolean hasPhoneOperation =
                mSupportFeatureProvider.isSupportTypeEnabled(mActivity, PHONE);
        final boolean hasChatOperation =
                mSupportFeatureProvider.isSupportTypeEnabled(mActivity, CHAT);
        final EscalationData.Builder builder = new EscalationData.Builder(mActivity);
        if (!hasPhoneOperation && !hasChatOperation) {
            // No support at all.
            builder.setTileTitle(R.string.support_escalation_title)
                    .setTileSummary(R.string.support_escalation_unavailable_summary);
        } else if (mSupportFeatureProvider.isAlwaysOperating(PHONE, null /* countryCode */)
                || mSupportFeatureProvider.isAlwaysOperating(CHAT, null /* countryCode */)) {
            // Support is available.
            builder.setTileTitle(R.string.support_escalation_24_7_title)
                    .setTileTitleDescription(R.string.support_escalation_24_7_content_description)
                    .setTileSummary(mActivity.getString(R.string.support_escalation_24_7_summary));
        } else if (mSupportFeatureProvider.isOperatingNow(PHONE)
                || mSupportFeatureProvider.isOperatingNow(CHAT)) {
            // Support is available now.
            builder.setTileTitle(R.string.support_escalation_title)
                    .setTileSummary(R.string.support_escalation_summary);
        } else {
            // Support is now temporarily unavailable.
            builder.setTileTitle(R.string.support_escalation_title)
                    .setTileSummary(
                            mSupportFeatureProvider.getOperationHours(mActivity, PHONE, null,
                                    true /* hasInternet */));
        }
        if (hasPhoneOperation) {
            builder.setText1(R.string.support_escalation_by_phone)
                    .setSummary1(mSupportFeatureProvider.getEstimatedWaitTime(mActivity, PHONE))
                    .setEnabled1(mSupportFeatureProvider.isOperatingNow(PHONE));
        }
        if (hasChatOperation) {
            builder.setText2(R.string.support_escalation_by_chat)
                    .setSummary2(mSupportFeatureProvider.getEstimatedWaitTime(mActivity, CHAT))
                    .setEnabled2(mSupportFeatureProvider.isOperatingNow(CHAT));
        }
        mSupportData.add(0 /* index */, builder.build());
    }

    private void addOfflineEscalationCards() {
        final CharSequence operatingHours;
        final boolean isPhoneSupportAlwaysOperating =
                mSupportFeatureProvider.isAlwaysOperating(PHONE, mSelectedCountry);
        if (isPhoneSupportAlwaysOperating) {
            operatingHours = mActivity.getString(R.string.support_escalation_24_7_summary);
        } else {
            operatingHours = mSupportFeatureProvider.getOperationHours(mActivity,
                    PHONE, mSelectedCountry, false /* hasInternet */);
        }
        mSupportData.add(0 /* index */, new OfflineEscalationData.Builder(mActivity)
                .setCountries(mSupportFeatureProvider.getPhoneSupportCountries())
                .setTollFreePhone(mSupportFeatureProvider.getSupportPhones(
                        mSelectedCountry, true /* isTollFree */))
                .setTolledPhone(mSupportFeatureProvider.getSupportPhones(
                        mSelectedCountry, false /* isTollFree */))
                .setTileTitle(isPhoneSupportAlwaysOperating
                        ? R.string.support_escalation_24_7_title
                        : R.string.support_escalation_title)
                .setTileTitleDescription(isPhoneSupportAlwaysOperating
                        ? R.string.support_escalation_24_7_content_description
                        : R.string.support_escalation_title)
                .setTileSummary(operatingHours)
                .build());
    }

    private void addSignInPromo() {
        mSupportData.add(0 /* index */, new EscalationData.Builder(mActivity, TYPE_SIGN_IN_BUTTON)
                .setText1(R.string.support_sign_in_button_text)
                .setText2(R.string.support_sign_in_required_help)
                .setTileTitle(R.string.support_sign_in_required_title)
                .setTileSummary(R.string.support_sign_in_required_summary)
                .build());
    }

    private void addMoreHelpItems() {
        mSupportData.add(new SupportData.Builder(mActivity, TYPE_SUPPORT_TILE_SPACER).build());
        PackageManager packageManager = mActivity.getPackageManager();
        Intent intent = mSupportFeatureProvider.getHelpIntent(mActivity);
        if (packageManager.resolveActivity(intent, 0) != null) {
            mSupportData.add(new SupportData.Builder(mActivity, TYPE_SUPPORT_TILE)
                    .setIcon(R.drawable.ic_help_24dp)
                    .setTileTitle(R.string.support_help_feedback_title)
                    .setIntent(intent)
                    .setMetricsEvent(MetricsProto.MetricsEvent.ACTION_SUPPORT_HELP_AND_FEEDBACK)
                    .build());
        }
        intent = mSupportFeatureProvider.getTipsAndTricksIntent(mActivity);
        if (packageManager.resolveActivity(intent, 0) != null) {
            mSupportData.add(new SupportData.Builder(mActivity, TYPE_SUPPORT_TILE)
                    .setIcon(R.drawable.ic_lightbulb_outline_24)
                    .setTileTitle(R.string.support_tips_and_tricks_title)
                    .setIntent(intent)
                    .setMetricsEvent(MetricsProto.MetricsEvent.ACTION_SUPPORT_TIPS_AND_TRICKS)
                    .build());
        }
    }

    private void bindEscalationOptions(ViewHolder holder, EscalationData data) {
        holder.tileTitleView.setText(data.tileTitle);
        holder.tileTitleView.setContentDescription(data.tileTitleDescription);
        holder.tileSummaryView.setText(data.tileSummary);
        if (data.text1 == 0) {
            holder.text1View.setVisibility(View.GONE);
        } else {
            holder.text1View.setText(data.text1);
            holder.text1View.setOnClickListener(mEscalationClickListener);
            holder.text1View.setEnabled(data.enabled1 && mHasInternet);
            holder.text1View.setVisibility(View.VISIBLE);
        }
        if (TextUtils.isEmpty(data.text2)) {
            holder.text2View.setVisibility(View.GONE);
        } else {
            holder.text2View.setText(data.text2);
            holder.text2View.setOnClickListener(mEscalationClickListener);
            holder.text2View.setEnabled(data.enabled2 && mHasInternet);
            holder.text2View.setVisibility(View.VISIBLE);
        }
        if (holder.summary1View != null) {
            holder.summary1View.setText(data.summary1);
            holder.summary1View.setVisibility(mHasInternet && !TextUtils.isEmpty(data.summary1)
                    ? View.VISIBLE : View.GONE);
        }
        if (holder.summary2View != null) {
            holder.summary2View.setText(data.summary2);
            holder.summary2View.setVisibility(mHasInternet && !TextUtils.isEmpty(data.summary2)
                    ? View.VISIBLE : View.GONE);
        }
    }

    private void bindOfflineEscalationOptions(ViewHolder holder, OfflineEscalationData data) {
        // Bind Title
        holder.tileTitleView.setText(data.tileTitle);
        holder.tileTitleView.setContentDescription(data.tileTitleDescription);
        holder.tileSummaryView.setText(data.tileSummary);
        // Bind spinner
        final Spinner spinner = (Spinner) holder.itemView.findViewById(R.id.spinner);
        final ArrayAdapter<String> adapter = new ArrayAdapter(
                mActivity, android.R.layout.simple_spinner_dropdown_item, data.countries);
        spinner.setAdapter(adapter);
        final List<String> countryCodes = mSupportFeatureProvider.getPhoneSupportCountryCodes();
        for (int i = 0; i < countryCodes.size(); i++) {
            if (TextUtils.equals(countryCodes.get(i), mSelectedCountry)) {
                spinner.setSelection(i);
                break;
            }
        }
        spinner.setOnItemSelectedListener(mSpinnerItemSelectListener);
        // Bind buttons
        if (data.tollFreePhone != null) {
            holder.text1View.setText(data.tollFreePhone.number);
            holder.text1View.setVisibility(View.VISIBLE);
            holder.text1View.setOnClickListener(mEscalationClickListener);
        } else {
            holder.text1View.setVisibility(View.GONE);
        }
        if (data.tolledPhone != null) {
            holder.text2View.setText(
                    mActivity.getString(R.string.support_international_phone_title));
            holder.text2View.setVisibility(View.VISIBLE);
            holder.text2View.setOnClickListener(mEscalationClickListener);
        } else {
            holder.text2View.setVisibility(View.GONE);
        }

        if (ActivityManager.isUserAMonkey()) {
            holder.text1View.setVisibility(View.GONE);
            holder.text2View.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
            holder.itemView.findViewById(R.id.support_text).setVisibility(View.GONE);
        }
    }

    private void bindSignInPromoTile(ViewHolder holder, EscalationData data) {
        holder.tileTitleView.setText(data.tileTitle);
        holder.tileTitleView.setContentDescription(data.tileTitleDescription);
        holder.tileSummaryView.setText(data.tileSummary);
        holder.text1View.setText(data.text1);
        holder.text2View.setText(data.text2);
        holder.text1View.setOnClickListener(mEscalationClickListener);
        holder.text2View.setOnClickListener(mEscalationClickListener);
    }

    private void bindSupportTile(ViewHolder holder, SupportData data) {
        if (holder.iconView != null) {
            holder.iconView.setImageResource(data.icon);
        }
        if (holder.tileTitleView != null) {
            holder.tileTitleView.setText(data.tileTitle);
            holder.tileTitleView.setContentDescription(data.tileTitleDescription);
        }
        if (holder.tileSummaryView != null) {
            holder.tileSummaryView.setText(data.tileSummary);
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
                        MetricsLogger.action(mActivity,
                                MetricsProto.MetricsEvent.ACTION_SUPPORT_SIGN_IN);
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
            } else if (mHasInternet) {
                switch (v.getId()) {
                    case android.R.id.text1:
                        MetricsLogger.action(mActivity,
                                MetricsProto.MetricsEvent.ACTION_SUPPORT_PHONE);
                        tryStartDisclaimerAndSupport(PHONE);
                        break;
                    case android.R.id.text2:
                        MetricsLogger.action(mActivity,
                                MetricsProto.MetricsEvent.ACTION_SUPPORT_CHAT);
                        tryStartDisclaimerAndSupport(CHAT);
                        break;
                }
            } else {
                switch (v.getId()) {
                    case android.R.id.text1: {
                        final SupportPhone phone = mSupportFeatureProvider
                                .getSupportPhones(mSelectedCountry, true /* isTollFree */);
                        if (phone != null) {
                            final Intent intent = phone.getDialIntent();
                            final boolean canDial = !mActivity.getPackageManager()
                                    .queryIntentActivities(intent, 0)
                                    .isEmpty();
                            if (canDial) {
                                MetricsLogger.action(mActivity,
                                        MetricsProto.MetricsEvent.ACTION_SUPPORT_DAIL_TOLLFREE);
                                mActivity.startActivity(intent);
                            }
                        }
                        break;
                    }
                    case android.R.id.text2: {
                        final SupportPhone phone = mSupportFeatureProvider
                                .getSupportPhones(mSelectedCountry, false /* isTollFree */);
                        final SupportPhoneDialogFragment fragment =
                                SupportPhoneDialogFragment.newInstance(phone);
                        MetricsLogger.action(mActivity,
                                MetricsProto.MetricsEvent.ACTION_SUPPORT_VIEW_TRAVEL_ABROAD_DIALOG);
                        fragment.show(mActivity.getFragmentManager(),
                                SupportPhoneDialogFragment.TAG);
                        break;
                    }
                }
            }
        }
    }

    private final class SpinnerItemSelectListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final List<String> countryCodes = mSupportFeatureProvider.getPhoneSupportCountryCodes();
            final String selectedCountry = countryCodes.get(position);
            if (!TextUtils.equals(selectedCountry, mSelectedCountry)) {
                mSelectedCountry = selectedCountry;
                refreshEscalationCards();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    /**
     * {@link RecyclerView.ViewHolder} for support items.
     */
    static final class ViewHolder extends RecyclerView.ViewHolder {

        final ImageView iconView;
        final TextView tileTitleView;
        final TextView tileSummaryView;
        final TextView text1View;
        final TextView text2View;
        final TextView summary1View;
        final TextView summary2View;

        ViewHolder(View itemView) {
            super(itemView);
            iconView = (ImageView) itemView.findViewById(android.R.id.icon);
            tileTitleView = (TextView) itemView.findViewById(R.id.tile_title);
            tileSummaryView = (TextView) itemView.findViewById(R.id.tile_summary);
            text1View = (TextView) itemView.findViewById(android.R.id.text1);
            text2View = (TextView) itemView.findViewById(android.R.id.text2);
            summary1View = (TextView) itemView.findViewById(R.id.summary1);
            summary2View = (TextView) itemView.findViewById(R.id.summary2);
        }
    }

    /**
     * Data for a single support item.
     */
    private static class SupportData {

        final Intent intent;
        final int metricsEvent;
        @LayoutRes
        final int type;
        @DrawableRes
        final int icon;
        @StringRes
        final int tileTitle;
        final CharSequence tileTitleDescription;
        final CharSequence tileSummary;


        private SupportData(Builder builder) {
            this.type = builder.mType;
            this.icon = builder.mIcon;
            this.tileTitle = builder.mTileTitle;
            this.tileTitleDescription = builder.mTileTitleDescription;
            this.tileSummary = builder.mTileSummary;
            this.intent = builder.mIntent;
            this.metricsEvent = builder.mMetricsEvent;
        }

        static class Builder {

            protected final Context mContext;
            @LayoutRes
            private final int mType;
            @DrawableRes
            private int mIcon;
            @StringRes
            private int mTileTitle;
            private CharSequence mTileTitleDescription;
            private CharSequence mTileSummary;
            private Intent mIntent;
            private int mMetricsEvent = -1;

            Builder(Context context, @LayoutRes int type) {
                mContext = context;
                mType = type;
            }

            Builder setIcon(@DrawableRes int icon) {
                mIcon = icon;
                return this;
            }

            Builder setTileTitle(@StringRes int title) {
                mTileTitle = title;
                return this;
            }

            Builder setTileTitleDescription(@StringRes int titleDescription) {
                mTileTitleDescription = mContext.getString(titleDescription);
                return this;
            }

            Builder setTileSummary(@StringRes int summary) {
                mTileSummary = mContext.getString(summary);
                return this;
            }

            Builder setTileSummary(CharSequence summary) {
                mTileSummary = summary;
                return this;
            }

            Builder setMetricsEvent(int metricsEvent) {
                mMetricsEvent = metricsEvent;
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

    /**
     * Data model for escalation cards.
     */
    private static class EscalationData extends SupportData {

        @StringRes
        final int text1;
        final CharSequence text2;
        final boolean enabled1;
        final boolean enabled2;
        final CharSequence summary1;
        final CharSequence summary2;

        private EscalationData(Builder builder) {
            super(builder);
            this.text1 = builder.mText1;
            this.text2 = builder.mText2;
            this.summary1 = builder.mSummary1;
            this.summary2 = builder.mSummary2;
            this.enabled1 = builder.mEnabled1;
            this.enabled2 = builder.mEnabled2;
        }

        static class Builder extends SupportData.Builder {

            @StringRes
            private int mText1;
            private CharSequence mText2;
            private CharSequence mSummary1;
            private CharSequence mSummary2;
            private boolean mEnabled1;
            private boolean mEnabled2;

            protected Builder(Context context, @LayoutRes int type) {
                super(context, type);
            }

            Builder(Context context) {
                this(context, TYPE_ESCALATION_OPTIONS);
            }

            Builder setEnabled1(boolean enabled) {
                mEnabled1 = enabled;
                return this;
            }

            Builder setText1(@StringRes int text1) {
                mText1 = text1;
                return this;
            }

            Builder setText2(@StringRes int text2) {
                mText2 = mContext.getString(text2);
                return this;
            }

            Builder setText2(CharSequence text2) {
                mText2 = text2;
                return this;
            }

            Builder setSummary1(String summary1) {
                mSummary1 = summary1;
                return this;
            }

            Builder setEnabled2(boolean enabled) {
                mEnabled2 = enabled;
                return this;
            }

            Builder setSummary2(String summary2) {
                mSummary2 = summary2;
                return this;
            }

            EscalationData build() {
                return new EscalationData(this);
            }
        }
    }

    /**
     * Support data for offline mode.
     */
    private static final class OfflineEscalationData extends EscalationData {

        final List<String> countries;
        final SupportPhone tollFreePhone;
        final SupportPhone tolledPhone;

        private OfflineEscalationData(Builder builder) {
            super(builder);
            countries = builder.mCountries;
            tollFreePhone = builder.mTollFreePhone;
            tolledPhone = builder.mTolledPhone;
        }

        static final class Builder extends EscalationData.Builder {

            private List<String> mCountries;
            private SupportPhone mTollFreePhone;
            private SupportPhone mTolledPhone;

            Builder(Context context) {
                super(context, TYPE_ESCALATION_OPTIONS_OFFLINE);
            }

            Builder setCountries(List<String> countries) {
                mCountries = countries;
                return this;
            }

            Builder setTollFreePhone(SupportPhone phone) {
                mTollFreePhone = phone;
                return this;
            }

            Builder setTolledPhone(SupportPhone phone) {
                mTolledPhone = phone;
                return this;
            }

            OfflineEscalationData build() {
                return new OfflineEscalationData(this);
            }
        }
    }
}
