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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;

import java.io.IOException;
import java.util.List;

public class BlobInfoListView extends ListActivity {
    private static final String TAG = "BlobInfoListView";

    private Context mContext;
    private BlobStoreManager mBlobStoreManager;
    private BlobListAdapter mAdapter;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        mBlobStoreManager = (BlobStoreManager) getSystemService(BlobStoreManager.class);
        mInflater = (LayoutInflater) getSystemService(LayoutInflater.class);

        mAdapter = new BlobListAdapter(this);
        setListAdapter(mAdapter);

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

    @Override
    protected void onResume() {
        super.onResume();
        queryBlobsAndUpdateList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SharedDataUtils.LEASE_VIEW_REQUEST_CODE
                && resultCode == SharedDataUtils.LEASE_VIEW_RESULT_CODE_FAILURE) {
            Toast.makeText(this, R.string.shared_data_delete_failure_text, Toast.LENGTH_LONG)
                    .show();
        }
        // do nothing on LEASE_VIEW_RESULT_CODE_SUCCESS since data is updated in onResume()
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BlobInfo blob = mAdapter.getItem(position);
        if (CollectionUtils.isEmpty(blob.getLeases())) {
            showDeleteBlobDialog(blob);
        } else {
            final Intent intent = new Intent(this, LeaseInfoListView.class);
            intent.putExtra(SharedDataUtils.BLOB_KEY, blob);
            startActivityForResult(intent, SharedDataUtils.LEASE_VIEW_REQUEST_CODE);
        }
    }

    private void showDeleteBlobDialog(BlobInfo blob) {
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setMessage(R.string.shared_data_no_accessors_dialog_text)
                .setPositiveButton(android.R.string.ok, getDialogOnClickListener(blob))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    private DialogInterface.OnClickListener getDialogOnClickListener(BlobInfo blob) {
        return (dialog, which) -> {
            try {
                mBlobStoreManager.deleteBlob(blob);
            } catch (IOException e) {
                Log.e(TAG, "Unable to delete blob: " + e.getMessage());
                Toast.makeText(this, R.string.shared_data_delete_failure_text, Toast.LENGTH_LONG)
                        .show();
            }
            queryBlobsAndUpdateList();
        };
    }

    private void queryBlobsAndUpdateList() {
        try {
            mAdapter.updateList(mBlobStoreManager.queryBlobsForUser(UserHandle.CURRENT));
        } catch (IOException e) {
            Log.e(TAG, "Unable to fetch blobs for current user: " + e.getMessage());
            Toast.makeText(this, R.string.shared_data_query_failure_text, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private class BlobListAdapter extends ArrayAdapter<BlobInfo> {
        BlobListAdapter(Context context) {
            super(context, 0);
        }

        void updateList(List<BlobInfo> blobs) {
            clear();
            if (blobs.isEmpty()) {
                finish();
            } else {
                addAll(blobs);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final BlobInfoViewHolder holder = BlobInfoViewHolder.createOrRecycle(
                    mInflater, convertView);
            convertView = holder.rootView;

            final BlobInfo blob = getItem(position);
            holder.blobLabel.setText(blob.getLabel());
            holder.blobId.setText(getString(R.string.blob_id_text, blob.getId()));
            holder.blobExpiry.setText(getString(R.string.blob_expires_text,
                    SharedDataUtils.formatTime(blob.getExpiryTimeMs())));
            holder.blobSize.setText(SharedDataUtils.formatSize(blob.getSizeBytes()));
            return convertView;
        }
    }
}
