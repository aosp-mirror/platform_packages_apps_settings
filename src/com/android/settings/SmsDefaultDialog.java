/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.SmsApplication.SmsApplicationData;

import java.util.ArrayList;
import java.util.List;

public final class SmsDefaultDialog extends AlertActivity implements
        DialogInterface.OnClickListener {
    private SmsApplicationData mNewSmsApplicationData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String packageName = intent.getStringExtra(Intents.EXTRA_PACKAGE_NAME);

        setResult(RESULT_CANCELED);
        if (!buildDialog(packageName)) {
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                SmsApplication.setDefaultApplication(mNewSmsApplicationData.mPackageName, this);
                setResult(RESULT_OK);
                break;
            case BUTTON_NEGATIVE:
                break;
            default:
                if (which >= 0) {
                    AppListAdapter adapter = (AppListAdapter) mAlertParams.mAdapter;
                    if (!adapter.isSelected(which)) {
                        String packageName = adapter.getPackageName(which);
                        if (!TextUtils.isEmpty(packageName)) {
                            SmsApplication.setDefaultApplication(packageName, this);
                            setResult(RESULT_OK);
                        }
                    }
                }
                break;
        }
    }

    private boolean buildDialog(String packageName) {
        TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        if (!tm.isSmsCapable()) {
            // No phone, no SMS
            return false;
        }
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.sms_change_default_dialog_title);
        mNewSmsApplicationData = SmsApplication.getSmsApplicationData(packageName, this);
        if (mNewSmsApplicationData != null) {
            // New default SMS app specified, change to that directly after the confirmation
            // dialog.
            SmsApplicationData oldSmsApplicationData = null;
            ComponentName oldSmsComponent = SmsApplication.getDefaultSmsApplication(this, true);
            if (oldSmsComponent != null) {
                oldSmsApplicationData = SmsApplication.getSmsApplicationData(
                        oldSmsComponent.getPackageName(), this);
                if (oldSmsApplicationData.mPackageName.equals(
                        mNewSmsApplicationData.mPackageName)) {
                    return false;
                }
            }

            // Compose dialog; get
            if (oldSmsApplicationData != null) {
                p.mMessage = getString(R.string.sms_change_default_dialog_text,
                        mNewSmsApplicationData.getApplicationName(this),
                        oldSmsApplicationData.getApplicationName(this));
            } else {
                p.mMessage = getString(R.string.sms_change_default_no_previous_dialog_text,
                        mNewSmsApplicationData.getApplicationName(this));
            }
            p.mPositiveButtonText = getString(R.string.yes);
            p.mNegativeButtonText = getString(R.string.no);
            p.mPositiveButtonListener = this;
            p.mNegativeButtonListener = this;
        } else {
            // No new default SMS app specified, show a list of all SMS apps and let user to pick
            p.mAdapter = new AppListAdapter();
            p.mOnClickListener = this;
            p.mNegativeButtonText = getString(R.string.cancel);
            p.mNegativeButtonListener = this;
        }
        setupAlert();

        return true;
    }

    /**
     * The list of SMS apps with label, icon. Current default SMS app is marked as "default".
     */
    private class AppListAdapter extends BaseAdapter {
        /**
         * SMS app item in the list
         */
        private class Item {
            final String label;         // app label
            final Drawable icon;        // app icon
            final String packgeName;    // full app package name

            public Item(String label, Drawable icon, String packageName) {
                this.label = label;
                this.icon = icon;
                this.packgeName = packageName;
            }
        }

        // The list
        private final List<Item> mItems;
        // The index of selected
        private final int mSelectedIndex;

        public AppListAdapter() {
            mItems = getItems();
            int selected = getSelectedIndex();
            // Move selected up to the top so it is easy to find
            if (selected > 0) {
                Item item = mItems.remove(selected);
                mItems.add(0, item);
                selected = 0;
            }
            mSelectedIndex = selected;
        }

        @Override
        public int getCount() {
            return mItems != null ? mItems.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mItems != null && position < mItems.size() ? mItems.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Item item = ((Item) getItem(position));
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.app_preference_item, parent, false);
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            textView.setText(item.label);
            if (position == mSelectedIndex) {
                view.findViewById(R.id.default_label).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.default_label).setVisibility(View.GONE);
            }
            ImageView imageView = (ImageView)view.findViewById(android.R.id.icon);
            imageView.setImageDrawable(item.icon);
            return view;
        }

        /**
         * Get the selected package name by
         *
         * @param position the index of the item in the list
         * @return the package name of selected item
         */
        public String getPackageName(int position) {
            Item item = (Item) getItem(position);
            if (item != null) {
                return item.packgeName;
            }
            return null;
        }

        /**
         * Check if an item at a position is already selected
         *
         * @param position the index of the item in the list
         * @return true if the item at the position is already selected, false otherwise
         */
        public boolean isSelected(int position) {
            return position == mSelectedIndex;
        }

        // Get the list items by looking for SMS apps
        private List<Item> getItems() {
            PackageManager pm = getPackageManager();
            List<Item> items = new ArrayList<>();
            for (SmsApplication.SmsApplicationData app :
                    SmsApplication.getApplicationCollection(SmsDefaultDialog.this)) {
                try {
                    String packageName = app.mPackageName;
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0/*flags*/);
                    if (appInfo != null) {
                        items.add(new Item(
                                appInfo.loadLabel(pm).toString(),
                                appInfo.loadIcon(pm),
                                packageName));
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // Ignore package can't be found
                }
            }
            return items;
        }

        // Get the selected item index by looking for the current default SMS app
        private int getSelectedIndex() {
            ComponentName appName = SmsApplication.getDefaultSmsApplication(
                    SmsDefaultDialog.this, true);
            if (appName != null) {
                String defaultSmsAppPackageName = appName.getPackageName();
                if (!TextUtils.isEmpty(defaultSmsAppPackageName)) {
                    for (int i = 0; i < mItems.size(); i++) {
                        if (TextUtils.equals(mItems.get(i).packgeName, defaultSmsAppPackageName)) {
                            return i;
                        }
                    }
                }
            }
            return -1;
        }
    }
}
