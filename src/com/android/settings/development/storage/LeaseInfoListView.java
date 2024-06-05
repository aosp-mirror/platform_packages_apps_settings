/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development.storage;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.blob.BlobInfo;
import android.app.blob.BlobStoreManager;
import android.app.blob.LeaseInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;

import java.io.IOException;
import java.util.List;

public class LeaseInfoListView extends ListActivity {
    private static final String TAG = "LeaseInfoListView";

    private Context mContext;
    private BlobStoreManager mBlobStoreManager;
    private BlobInfo mBlobInfo;
    private LeaseListAdapter mAdapter;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mBlobStoreManager = (BlobStoreManager) getSystemService(BlobStoreManager.class);
        mInflater = (LayoutInflater) getSystemService(LayoutInflater.class);

        mBlobInfo = getIntent().getParcelableExtra(SharedDataUtils.BLOB_KEY);

        mAdapter = new LeaseListAdapter(this);
        if (mAdapter.isEmpty()) {
            // this should never happen since we're checking the size in BlobInfoListView
            Log.e(TAG, "Error fetching leases for shared data: " + mBlobInfo.toString());
            finish();
        }

        setListAdapter(mAdapter);
        getListView().addHeaderView(getHeaderView());
        getListView().addFooterView(getFooterView());
        getListView().setClickable(false);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    private LinearLayout getHeaderView() {
        final LinearLayout headerView = (LinearLayout) mInflater.inflate(
                R.layout.blob_list_item_view , null);
        headerView.setEnabled(false); // disable clicking
        final TextView blobLabel = headerView.findViewById(R.id.blob_label);
        final TextView blobId = headerView.findViewById(R.id.blob_id);
        final TextView blobExpiry = headerView.findViewById(R.id.blob_expiry);
        final TextView blobSize = headerView.findViewById(R.id.blob_size);

        blobLabel.setText(mBlobInfo.getLabel());
        blobLabel.setTypeface(Typeface.DEFAULT_BOLD);
        blobId.setText(getString(com.android.settingslib.R.string.blob_id_text, mBlobInfo.getId()));
        blobExpiry.setVisibility(View.GONE);
        blobSize.setText(SharedDataUtils.formatSize(mBlobInfo.getSizeBytes()));
        return headerView;
    }

    private Button getFooterView() {
        final Button deleteButton = new Button(this);
        deleteButton.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        deleteButton.setText(com.android.settingslib.R.string.delete_blob_text);
        deleteButton.setOnClickListener(getButtonOnClickListener());
        return deleteButton;
    }

    private View.OnClickListener getButtonOnClickListener() {
        return v -> {
            final AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setMessage(com.android.settingslib.R.string.delete_blob_confirmation_text)
                    .setPositiveButton(android.R.string.ok, getDialogOnClickListener())
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.show();
        };
    }

    private DialogInterface.OnClickListener getDialogOnClickListener() {
        return (dialog, which) -> {
            try {
                mBlobStoreManager.deleteBlob(mBlobInfo);
                setResult(SharedDataUtils.LEASE_VIEW_RESULT_CODE_SUCCESS);
            } catch (IOException e) {
                Log.e(TAG, "Unable to delete blob: " + e.getMessage());
                setResult(SharedDataUtils.LEASE_VIEW_RESULT_CODE_FAILURE);
            }
            finish();
        };
    }

    private class LeaseListAdapter extends ArrayAdapter<LeaseInfo> {
        private Context mContext;

        LeaseListAdapter(Context context) {
            super(context, 0);

            mContext = context;
            final List<LeaseInfo> leases = mBlobInfo.getLeases();
            if (CollectionUtils.isEmpty(leases)) {
                return;
            }
            addAll(leases);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final LeaseInfoViewHolder holder = LeaseInfoViewHolder.createOrRecycle(
                    mInflater, convertView);
            convertView = holder.rootView;
            convertView.setEnabled(false); // disable clicking

            final LeaseInfo lease = getItem(position);
            Drawable appIcon;
            try {
                appIcon = mContext.getPackageManager().getApplicationIcon(lease.getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
                // set to system default app icon
                appIcon = mContext.getDrawable(android.R.drawable.sym_def_app_icon);
            }
            holder.appIcon.setImageDrawable(appIcon);
            holder.leasePackageName.setText(lease.getPackageName());
            holder.leaseDescription.setText(getDescriptionString(lease));
            holder.leaseExpiry.setText(formatExpiryTime(lease.getExpiryTimeMillis()));
            return convertView;
        }

        private String getDescriptionString(LeaseInfo lease) {
            String description = null;
            try {
                description = getString(lease.getDescriptionResId());
            } catch (Resources.NotFoundException ignored) {
                if (lease.getDescription() != null) {
                    description = lease.getDescription().toString();
                }
            } finally {
                if (TextUtils.isEmpty(description)) {
                    description = getString(
                            com.android.settingslib.R.string.accessor_no_description_text);
                }
            }
            return description;
        }

        private String formatExpiryTime(long expiryTimeMillis) {
            if (expiryTimeMillis == 0) {
                return getString(R.string.accessor_never_expires_text);
            }
            return getString(com.android.settingslib.R.string.accessor_expires_text,
                    SharedDataUtils.formatTime(expiryTimeMillis));
        }
    }
}
