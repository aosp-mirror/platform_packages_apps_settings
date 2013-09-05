/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.android.org.conscrypt.TrustedCertificateStore;

public class TrustedCredentialsSettings extends Fragment {

    private static final String TAG = "TrustedCredentialsSettings";

    private UserManager mUserManager;

    private static final String USER_ACTION = "com.android.settings.TRUSTED_CREDENTIALS_USER";

    private static final int REQUEST_PIN_CHALLENGE = 12309;
    // If the restriction PIN is entered correctly.
    private boolean mChallengeSucceeded;
    private boolean mChallengeRequested;


    private enum Tab {
        SYSTEM("system",
               R.string.trusted_credentials_system_tab,
               R.id.system_tab,
               R.id.system_progress,
               R.id.system_list,
               true),
        USER("user",
             R.string.trusted_credentials_user_tab,
             R.id.user_tab,
             R.id.user_progress,
             R.id.user_list,
             false);

        private final String mTag;
        private final int mLabel;
        private final int mView;
        private final int mProgress;
        private final int mList;
        private final boolean mCheckbox;
        private Tab(String tag, int label, int view, int progress, int list, boolean checkbox) {
            mTag = tag;
            mLabel = label;
            mView = view;
            mProgress = progress;
            mList = list;
            mCheckbox = checkbox;
        }
        private Set<String> getAliases(TrustedCertificateStore store) {
            switch (this) {
                case SYSTEM:
                    return store.allSystemAliases();
                case USER:
                    return store.userAliases();
            }
            throw new AssertionError();
        }
        private boolean deleted(TrustedCertificateStore store, String alias) {
            switch (this) {
                case SYSTEM:
                    return !store.containsAlias(alias);
                case USER:
                    return false;
            }
            throw new AssertionError();
        }
        private int getButtonLabel(CertHolder certHolder) {
            switch (this) {
                case SYSTEM:
                    if (certHolder.mDeleted) {
                        return R.string.trusted_credentials_enable_label;
                    }
                    return R.string.trusted_credentials_disable_label;
                case USER:
                    return R.string.trusted_credentials_remove_label;
            }
            throw new AssertionError();
        }
        private int getButtonConfirmation(CertHolder certHolder) {
            switch (this) {
                case SYSTEM:
                    if (certHolder.mDeleted) {
                        return R.string.trusted_credentials_enable_confirmation;
                    }
                    return R.string.trusted_credentials_disable_confirmation;
                case USER:
                    return R.string.trusted_credentials_remove_confirmation;
            }
            throw new AssertionError();
        }
        private void postOperationUpdate(boolean ok, CertHolder certHolder) {
            if (ok) {
                if (certHolder.mTab.mCheckbox) {
                    certHolder.mDeleted = !certHolder.mDeleted;
                } else {
                    certHolder.mAdapter.mCertHolders.remove(certHolder);
                }
                certHolder.mAdapter.notifyDataSetChanged();
            } else {
                // bail, reload to reset to known state
                certHolder.mAdapter.load();
            }
        }
    }

    // be careful not to use this on the UI thread since it is does file operations
    private final TrustedCertificateStore mStore = new TrustedCertificateStore();

    private TabHost mTabHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
    }


    @Override public View onCreateView(
            LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mTabHost = (TabHost) inflater.inflate(R.layout.trusted_credentials, parent, false);
        mTabHost.setup();
        addTab(Tab.SYSTEM);
        // TODO add Install button on Tab.USER to go to CertInstaller like KeyChainActivity
        addTab(Tab.USER);
        if (getActivity().getIntent() != null &&
                USER_ACTION.equals(getActivity().getIntent().getAction())) {
            mTabHost.setCurrentTabByTag(Tab.USER.mTag);
        }
        return mTabHost;
    }

    private void addTab(Tab tab) {
        TabHost.TabSpec systemSpec = mTabHost.newTabSpec(tab.mTag)
                .setIndicator(getActivity().getString(tab.mLabel))
                .setContent(tab.mView);
        mTabHost.addTab(systemSpec);

        ListView lv = (ListView) mTabHost.findViewById(tab.mList);
        final TrustedCertificateAdapter adapter = new TrustedCertificateAdapter(tab);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                showCertDialog(adapter.getItem(pos));
            }
        });
    }

    private class TrustedCertificateAdapter extends BaseAdapter {
        private final List<CertHolder> mCertHolders = new ArrayList<CertHolder>();
        private final Tab mTab;
        private TrustedCertificateAdapter(Tab tab) {
            mTab = tab;
            load();
        }
        private void load() {
            new AliasLoader().execute();
        }
        @Override public int getCount() {
            return mCertHolders.size();
        }
        @Override public CertHolder getItem(int position) {
            return mCertHolders.get(position);
        }
        @Override public long getItemId(int position) {
            return position;
        }
        @Override public View getView(int position, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                view = inflater.inflate(R.layout.trusted_credential, parent, false);
                holder = new ViewHolder();
                holder.mSubjectPrimaryView = (TextView)
                        view.findViewById(R.id.trusted_credential_subject_primary);
                holder.mSubjectSecondaryView = (TextView)
                        view.findViewById(R.id.trusted_credential_subject_secondary);
                holder.mCheckBox = (CheckBox) view.findViewById(R.id.trusted_credential_status);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            CertHolder certHolder = mCertHolders.get(position);
            holder.mSubjectPrimaryView.setText(certHolder.mSubjectPrimary);
            holder.mSubjectSecondaryView.setText(certHolder.mSubjectSecondary);
            if (mTab.mCheckbox) {
                holder.mCheckBox.setChecked(!certHolder.mDeleted);
                holder.mCheckBox.setVisibility(View.VISIBLE);
            }
            return view;
        };

        private class AliasLoader extends AsyncTask<Void, Integer, List<CertHolder>> {
            ProgressBar mProgressBar;
            View mList;
            @Override protected void onPreExecute() {
                View content = mTabHost.getTabContentView();
                mProgressBar = (ProgressBar) content.findViewById(mTab.mProgress);
                mList = content.findViewById(mTab.mList);
                mProgressBar.setVisibility(View.VISIBLE);
                mList.setVisibility(View.GONE);
            }
            @Override protected List<CertHolder> doInBackground(Void... params) {
                Set<String> aliases = mTab.getAliases(mStore);
                int max = aliases.size();
                int progress = 0;
                List<CertHolder> certHolders = new ArrayList<CertHolder>(max);
                for (String alias : aliases) {
                    X509Certificate cert = (X509Certificate) mStore.getCertificate(alias, true);
                    certHolders.add(new CertHolder(mStore,
                                                   TrustedCertificateAdapter.this,
                                                   mTab,
                                                   alias,
                                                   cert));
                    publishProgress(++progress, max);
                }
                Collections.sort(certHolders);
                return certHolders;
            }
            @Override protected void onProgressUpdate(Integer... progressAndMax) {
                int progress = progressAndMax[0];
                int max = progressAndMax[1];
                if (max != mProgressBar.getMax()) {
                    mProgressBar.setMax(max);
                }
                mProgressBar.setProgress(progress);
            }
            @Override protected void onPostExecute(List<CertHolder> certHolders) {
                mCertHolders.clear();
                mCertHolders.addAll(certHolders);
                notifyDataSetChanged();
                View content = mTabHost.getTabContentView();
                mProgressBar.setVisibility(View.GONE);
                mList.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(0);
            }
        }
    }

    private static class CertHolder implements Comparable<CertHolder> {
        private final TrustedCertificateStore mStore;
        private final TrustedCertificateAdapter mAdapter;
        private final Tab mTab;
        private final String mAlias;
        private final X509Certificate mX509Cert;

        private final SslCertificate mSslCert;
        private final String mSubjectPrimary;
        private final String mSubjectSecondary;
        private boolean mDeleted;

        private CertHolder(TrustedCertificateStore store,
                           TrustedCertificateAdapter adapter,
                           Tab tab,
                           String alias,
                           X509Certificate x509Cert) {
            mStore = store;
            mAdapter = adapter;
            mTab = tab;
            mAlias = alias;
            mX509Cert = x509Cert;

            mSslCert = new SslCertificate(x509Cert);

            String cn = mSslCert.getIssuedTo().getCName();
            String o = mSslCert.getIssuedTo().getOName();
            String ou = mSslCert.getIssuedTo().getUName();
            // if we have a O, use O as primary subject, secondary prefer CN over OU
            // if we don't have an O, use CN as primary, empty secondary
            // if we don't have O or CN, use DName as primary, empty secondary
            if (!o.isEmpty()) {
                if (!cn.isEmpty()) {
                    mSubjectPrimary = o;
                    mSubjectSecondary = cn;
                } else {
                    mSubjectPrimary = o;
                    mSubjectSecondary = ou;
                }
            } else {
                if (!cn.isEmpty()) {
                    mSubjectPrimary = cn;
                    mSubjectSecondary = "";
                } else {
                    mSubjectPrimary = mSslCert.getIssuedTo().getDName();
                    mSubjectSecondary = "";
                }
            }
            mDeleted = mTab.deleted(mStore, mAlias);
        }
        @Override public int compareTo(CertHolder o) {
            int primary = this.mSubjectPrimary.compareToIgnoreCase(o.mSubjectPrimary);
            if (primary != 0) {
                return primary;
            }
            return this.mSubjectSecondary.compareToIgnoreCase(o.mSubjectSecondary);
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof CertHolder)) {
                return false;
            }
            CertHolder other = (CertHolder) o;
            return mAlias.equals(other.mAlias);
        }
        @Override public int hashCode() {
            return mAlias.hashCode();
        }
    }

    private static class ViewHolder {
        private TextView mSubjectPrimaryView;
        private TextView mSubjectSecondaryView;
        private CheckBox mCheckBox;
    }

    private void showCertDialog(final CertHolder certHolder) {
        View view = certHolder.mSslCert.inflateCertificateView(getActivity());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(com.android.internal.R.string.ssl_certificate);
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        final Dialog certDialog = builder.create();

        ViewGroup body = (ViewGroup) view.findViewById(com.android.internal.R.id.body);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        Button removeButton = (Button) inflater.inflate(R.layout.trusted_credential_details,
                                                        body,
                                                        false);
        body.addView(removeButton);
        removeButton.setText(certHolder.mTab.getButtonLabel(certHolder));
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (mUserManager.hasRestrictionsChallenge() && !mChallengeSucceeded) {
                    ensurePin();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(certHolder.mTab.getButtonConfirmation(certHolder));
                builder.setPositiveButton(
                        android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int id) {
                        new AliasOperation(certHolder).execute();
                        dialog.dismiss();
                        certDialog.dismiss();
                    }
                });
                builder.setNegativeButton(
                        android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        certDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PIN_CHALLENGE) {
            mChallengeRequested = false;
            if (resultCode == Activity.RESULT_OK) {
                mChallengeSucceeded = true;
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void ensurePin() {
        if (!mChallengeSucceeded) {
            final UserManager um = UserManager.get(getActivity());
            if (!mChallengeRequested) {
                if (um.hasRestrictionsChallenge()) {
                    Intent requestPin =
                            new Intent(Intent.ACTION_RESTRICTIONS_CHALLENGE);
                    startActivityForResult(requestPin, REQUEST_PIN_CHALLENGE);
                    mChallengeRequested = true;
                }
            }
        }
        mChallengeSucceeded = false;
    }


    private class AliasOperation extends AsyncTask<Void, Void, Boolean> {
        private final CertHolder mCertHolder;
        private AliasOperation(CertHolder certHolder) {
            mCertHolder = certHolder;
        }
        @Override protected Boolean doInBackground(Void... params) {
            try {
                KeyChainConnection keyChainConnection = KeyChain.bind(getActivity());
                IKeyChainService service = keyChainConnection.getService();
                try {
                    if (mCertHolder.mDeleted) {
                        byte[] bytes = mCertHolder.mX509Cert.getEncoded();
                        service.installCaCertificate(bytes);
                        return true;
                    } else {
                        return service.deleteCaCertificate(mCertHolder.mAlias);
                    }
                } finally {
                    keyChainConnection.close();
                }
            } catch (CertificateEncodingException e) {
                return false;
            } catch (IllegalStateException e) {
                // used by installCaCertificate to report errors
                return false;
            } catch (RemoteException e) {
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        @Override protected void onPostExecute(Boolean ok) {
            mCertHolder.mTab.postOperationUpdate(ok, mCertHolder);
        }
    }
}
