/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.accounts.AuthenticatorHelper;
import com.android.settings.accounts.ManageAccountsSettings;
import com.android.settings.bluetooth.BluetoothEnabler;
import com.android.settings.wifi.WifiEnabler;

import java.util.List;

/**
 * A basic ArrayAdapter for dealing with the Headers
 */
public class HeaderAdapter extends ArrayAdapter<Header> {
    public static final int HEADER_TYPE_CATEGORY = 0;
    public static final int HEADER_TYPE_NORMAL = 1;
    public static final int HEADER_TYPE_SWITCH = 2;
    public static final int HEADER_TYPE_BUTTON = 3;

    private static final int HEADER_TYPE_COUNT = HEADER_TYPE_BUTTON + 1;

    private final WifiEnabler mWifiEnabler;
    private final BluetoothEnabler mBluetoothEnabler;
    private AuthenticatorHelper mAuthHelper;
    private DevicePolicyManager mDevicePolicyManager;

    private static class HeaderViewHolder {
        ImageView mIcon;
        TextView mTitle;
        TextView mSummary;
        Switch mSwitch;
        ImageButton mButton;
        View mDivider;
    }

    private LayoutInflater mInflater;

    public static int getHeaderType(Header header) {
        if (header.fragment == null && header.intent == null) {
            return HEADER_TYPE_CATEGORY;
        } else if (header.id == R.id.security_settings) {
            return HEADER_TYPE_BUTTON;
        } else {
            return HEADER_TYPE_NORMAL;
        }
    }

    @Override
    public int getItemViewType(int position) {
        Header header = getItem(position);
        return getHeaderType(header);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false; // because of categories
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != HEADER_TYPE_CATEGORY;
    }

    @Override
    public int getViewTypeCount() {
        return HEADER_TYPE_COUNT;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public HeaderAdapter(Context context, List<Header> objects,
                         AuthenticatorHelper authenticatorHelper, DevicePolicyManager dpm) {
        super(context, 0, objects);

        mAuthHelper = authenticatorHelper;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Temp Switches provided as placeholder until the adapter replaces these with actual
        // Switches inflated from their layouts. Must be done before adapter is set in super
        mWifiEnabler = new WifiEnabler(context, new Switch(context));
        mBluetoothEnabler = new BluetoothEnabler(context, new Switch(context));
        mDevicePolicyManager = dpm;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        Header header = getItem(position);
        int headerType = getHeaderType(header);
        View view = null;

        if (convertView == null) {
            holder = new HeaderViewHolder();
            switch (headerType) {
                case HEADER_TYPE_CATEGORY:
                    view = new TextView(getContext(), null,
                            android.R.attr.listSeparatorTextViewStyle);
                    holder.mTitle = (TextView) view;
                    break;

                case HEADER_TYPE_SWITCH:
                    view = mInflater.inflate(R.layout.preference_header_switch_item, parent,
                            false);
                    holder.mIcon = (ImageView) view.findViewById(R.id.icon);
                    holder.mTitle = (TextView)
                            view.findViewById(com.android.internal.R.id.title);
                    holder.mSummary = (TextView)
                            view.findViewById(com.android.internal.R.id.summary);
                    holder.mSwitch = (Switch) view.findViewById(R.id.switchWidget);
                    break;

                case HEADER_TYPE_BUTTON:
                    view = mInflater.inflate(R.layout.preference_header_button_item, parent,
                            false);
                    holder.mIcon = (ImageView) view.findViewById(R.id.icon);
                    holder.mTitle = (TextView)
                            view.findViewById(com.android.internal.R.id.title);
                    holder.mSummary = (TextView)
                            view.findViewById(com.android.internal.R.id.summary);
                    holder.mButton = (ImageButton) view.findViewById(R.id.buttonWidget);
                    holder.mDivider = view.findViewById(R.id.divider);
                    break;

                case HEADER_TYPE_NORMAL:
                    view = mInflater.inflate(
                            R.layout.preference_header_item, parent,
                            false);
                    holder.mIcon = (ImageView) view.findViewById(R.id.icon);
                    holder.mTitle = (TextView)
                            view.findViewById(com.android.internal.R.id.title);
                    holder.mSummary = (TextView)
                            view.findViewById(com.android.internal.R.id.summary);
                    break;
            }
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (HeaderViewHolder) view.getTag();
        }

        // All view fields must be updated every time, because the view may be recycled
        switch (headerType) {
            case HEADER_TYPE_CATEGORY:
                holder.mTitle.setText(header.getTitle(getContext().getResources()));
                break;

            case HEADER_TYPE_SWITCH:
                // Would need a different treatment if the main menu had more switches
                if (header.id == R.id.wifi_settings) {
                    mWifiEnabler.setSwitch(holder.mSwitch);
                } else {
                    mBluetoothEnabler.setSwitch(holder.mSwitch);
                }
                updateCommonHeaderView(header, holder);
                break;

            case HEADER_TYPE_BUTTON:
                if (header.id == R.id.security_settings) {
                    boolean hasCert = DevicePolicyManager.hasAnyCaCertsInstalled();
                    if (hasCert) {
                        holder.mButton.setVisibility(View.VISIBLE);
                        holder.mDivider.setVisibility(View.VISIBLE);
                        boolean isManaged = mDevicePolicyManager.getDeviceOwner() != null;
                        if (isManaged) {
                            holder.mButton.setImageResource(R.drawable.ic_settings_about);
                        } else {
                            holder.mButton.setImageResource(
                                    android.R.drawable.stat_notify_error);
                        }
                        holder.mButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(
                                        android.provider.Settings.ACTION_MONITORING_CERT_INFO);
                                getContext().startActivity(intent);
                            }
                        });
                    } else {
                        holder.mButton.setVisibility(View.GONE);
                        holder.mDivider.setVisibility(View.GONE);
                    }
                }
                updateCommonHeaderView(header, holder);
                break;

            case HEADER_TYPE_NORMAL:
                updateCommonHeaderView(header, holder);
                break;
        }

        return view;
    }

    private void updateCommonHeaderView(Header header, HeaderViewHolder holder) {
        if (header.extras != null
                && header.extras.containsKey(ManageAccountsSettings.KEY_ACCOUNT_TYPE)) {
            String accType = header.extras.getString(
                    ManageAccountsSettings.KEY_ACCOUNT_TYPE);
            Drawable icon = mAuthHelper.getDrawableForType(getContext(), accType);
            setHeaderIcon(holder, icon);
        } else {
            if (header.iconRes > 0) {
                holder.mIcon.setImageResource(header.iconRes);
            } else {
                holder.mIcon.setImageDrawable(null);
            }
        }
        if (holder.mIcon != null) {
            if (header.iconRes > 0) {
                holder.mIcon.setBackgroundResource(R.color.background_drawer_icon);
            } else {
                holder.mIcon.setBackground(null);
            }
        }
        holder.mTitle.setText(header.getTitle(getContext().getResources()));
        CharSequence summary = header.getSummary(getContext().getResources());
        if (!TextUtils.isEmpty(summary)) {
            holder.mSummary.setVisibility(View.VISIBLE);
            holder.mSummary.setText(summary);
        } else {
            holder.mSummary.setVisibility(View.GONE);
        }
    }

    private void setHeaderIcon(HeaderViewHolder holder, Drawable icon) {
        ViewGroup.LayoutParams lp = holder.mIcon.getLayoutParams();
        lp.width = getContext().getResources().getDimensionPixelSize(
                R.dimen.header_icon_width);
        lp.height = lp.width;
        holder.mIcon.setLayoutParams(lp);
        holder.mIcon.setImageDrawable(icon);
    }
}