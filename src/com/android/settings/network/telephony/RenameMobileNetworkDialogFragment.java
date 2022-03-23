/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.DeviceInfoUtils;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

/**
 * A dialog allowing the display name of a mobile network subscription to be changed
 */
public class RenameMobileNetworkDialogFragment extends InstrumentedDialogFragment {

    public static final String TAG = "RenameMobileNetwork";

    private static final String KEY_SUBSCRIPTION_ID = "subscription_id";

    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private int mSubId;
    private EditText mNameView;
    private Spinner mColorSpinner;
    private Color[] mColors;
    private Map<Integer, Integer> mLightDarkMap;

    public static RenameMobileNetworkDialogFragment newInstance(int subscriptionId) {
        final Bundle args = new Bundle(1);
        args.putInt(KEY_SUBSCRIPTION_ID, subscriptionId);
        final RenameMobileNetworkDialogFragment fragment = new RenameMobileNetworkDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @VisibleForTesting
    protected TelephonyManager getTelephonyManager(Context context) {
        return context.getSystemService(TelephonyManager.class);
    }

    @VisibleForTesting
    protected SubscriptionManager getSubscriptionManager(Context context) {
        return context.getSystemService(SubscriptionManager.class);
    }

    @VisibleForTesting
    protected EditText getNameView() {
        return mNameView;
    }

    @VisibleForTesting
    protected Spinner getColorSpinnerView() {
        return mColorSpinner;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mTelephonyManager = getTelephonyManager(context);
        mSubscriptionManager = getSubscriptionManager(context);
        mSubId = getArguments().getInt(KEY_SUBSCRIPTION_ID);
        Resources res = context.getResources();
        mLightDarkMap = ImmutableMap.<Integer, Integer>builder()
                .put(res.getInteger(R.color.SIM_color_cyan),
                        res.getInteger(R.color.SIM_dark_mode_color_cyan))
                .put(res.getInteger(R.color.SIM_color_blue800),
                        res.getInteger(R.color.SIM_dark_mode_color_blue))
                .put(res.getInteger(R.color.SIM_color_green800),
                        res.getInteger(R.color.SIM_dark_mode_color_green))
                .put(res.getInteger(R.color.SIM_color_purple800),
                        res.getInteger(R.color.SIM_dark_mode_color_purple))
                .put(res.getInteger(R.color.SIM_color_pink800),
                        res.getInteger(R.color.SIM_dark_mode_color_pink))
                .put(res.getInteger(R.color.SIM_color_orange),
                        res.getInteger(R.color.SIM_dark_mode_color_orange))
                .build();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mColors = getColors();

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater layoutInflater = builder.getContext().getSystemService(
                LayoutInflater.class);
        final View view = layoutInflater.inflate(R.layout.dialog_mobile_network_rename, null);
        populateView(view);
        builder.setTitle(R.string.mobile_network_sim_name)
                .setView(view)
                .setPositiveButton(R.string.mobile_network_sim_name_rename, (dialog, which) -> {
                    mSubscriptionManager.setDisplayName(mNameView.getText().toString(), mSubId,
                            SubscriptionManager.NAME_SOURCE_USER_INPUT);
                    final Color color = (mColorSpinner == null) ? mColors[0]
                            : mColors[mColorSpinner.getSelectedItemPosition()];
                    mSubscriptionManager.setIconTint(color.getColor(), mSubId);
                })
                .setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    @VisibleForTesting
    protected void populateView(View view) {
        mNameView = view.findViewById(R.id.name_edittext);
        SubscriptionInfo info = null;
        final List<SubscriptionInfo> infoList = mSubscriptionManager
                .getAvailableSubscriptionInfoList();
        if (infoList != null) {
            for (SubscriptionInfo subInfo : infoList) {
                if (subInfo.getSubscriptionId() == mSubId) {
                    info = subInfo;
                    break;
                }
            }
        }
        if (info == null) {
            Log.w(TAG, "got null SubscriptionInfo for mSubId:" + mSubId);
            return;
        }
        final CharSequence displayName = SubscriptionUtil.getUniqueSubscriptionDisplayName(
                info, getContext());
        mNameView.setText(displayName);
        if (!TextUtils.isEmpty(displayName)) {
            mNameView.setSelection(displayName.length());
        }

        mColorSpinner = view.findViewById(R.id.color_spinner);
        final ColorAdapter adapter = new ColorAdapter(getContext(),
                R.layout.dialog_mobile_network_color_picker_item, mColors);
        mColorSpinner.setAdapter(adapter);
        mColorSpinner.setSelection(getSimColorIndex(info.getIconTint()));

        final TextView operatorName = view.findViewById(R.id.operator_name_value);
        mTelephonyManager = mTelephonyManager.createForSubscriptionId(mSubId);
        operatorName.setText(info.getCarrierName());

        final TextView phoneTitle = view.findViewById(R.id.number_label);
        phoneTitle.setVisibility(info.isOpportunistic() ? View.GONE : View.VISIBLE);

        final TextView phoneNumber = view.findViewById(R.id.number_value);
        final String pn = DeviceInfoUtils.getBidiFormattedPhoneNumber(getContext(), info);
        if (!TextUtils.isEmpty(pn)) {
            phoneNumber.setText(pn);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.MOBILE_NETWORK_RENAME_DIALOG;
    }

    private class ColorAdapter extends ArrayAdapter<Color> {

        private Context mContext;
        private int mItemResId;

        public ColorAdapter(Context context, int resource, Color[] colors) {
            super(context, resource, colors);
            mContext = context;
            mItemResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(mItemResId, null);
            }
            boolean isDarkMode = false;
            if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES) {
                isDarkMode = true;
            }
            ((ImageView) convertView.findViewById(R.id.color_icon))
                    .setImageDrawable(getItem(position).getDrawable(isDarkMode));
            ((TextView) convertView.findViewById(R.id.color_label))
                    .setText(getItem(position).getLabel());

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }

    private Color[] getColors() {
        final Resources res = getContext().getResources();
        final int[] colorInts = res.getIntArray(R.array.sim_color_light);
        final String[] colorStrings = res.getStringArray(R.array.color_picker);
        final int iconSize = res.getDimensionPixelSize(R.dimen.color_swatch_size);
        final int strokeWidth = res.getDimensionPixelSize(R.dimen.color_swatch_stroke_width);
        final Color[] colors = new Color[colorInts.length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = new Color(colorStrings[i], colorInts[i], iconSize, strokeWidth);
        }
        return colors;
    }

    private class Color {

        private String mLabel;
        private int mColor;
        private ShapeDrawable mDrawable;

        private Color(String label, int color, int iconSize, int strokeWidth) {
            mLabel = label;
            mColor = color;
            mDrawable = new ShapeDrawable(new OvalShape());
            mDrawable.setIntrinsicHeight(iconSize);
            mDrawable.setIntrinsicWidth(iconSize);
            mDrawable.getPaint().setStrokeWidth(strokeWidth);
            mDrawable.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            mDrawable.getPaint().setColor(color);
        }

        private String getLabel() {
            return mLabel;
        }

        private int getColor() {
            return mColor;
        }

        private ShapeDrawable getDrawable(boolean isDarkMode) {
            if (isDarkMode) {
                mDrawable.getPaint().setColor(getDarkColor(mColor));
            }
            return mDrawable;
        }
    }

    private int getDarkColor(int lightColor) {
        return mLightDarkMap.getOrDefault(lightColor, lightColor);
    }

    /*
    * Get the color index from previous color that defined in Android OS
    * (frameworks/base/core/res/res/values/arrays.xml). If can't find the color, continue to look
    * for it in the new color plattee. If not, give it the first index.
    */

    private int getSimColorIndex(int color) {
        int index = -1;
        final int[] previousSimColorInts =
                getContext().getResources().getIntArray(com.android.internal.R.array.sim_colors);
        for (int i = 0; i < previousSimColorInts.length; i++) {
            if (previousSimColorInts[i] == color) {
                index = i;
            }
        }

        if (index == -1) {
            for (int i = 0; i < mColors.length; i++) {
                if (mColors[i].getColor() == color) {
                    index = i;
                }
            }
        }

        return index == -1 ? 0 : index;
    }
}
