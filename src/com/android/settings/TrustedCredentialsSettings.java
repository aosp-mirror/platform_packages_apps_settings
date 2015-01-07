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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.content.res.TypedArray;
import android.net.http.SslCertificate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.util.SparseArray;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import com.android.internal.util.ParcelableString;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;

public class TrustedCredentialsSettings extends Fragment {

    private static final String TAG = "TrustedCredentialsSettings";

    private UserManager mUserManager;

    private static final String USER_ACTION = "com.android.settings.TRUSTED_CREDENTIALS_USER";

    private enum Tab {
        SYSTEM("system",
               R.string.trusted_credentials_system_tab,
               R.id.system_tab,
               R.id.system_progress,
               R.id.system_list,
               R.id.system_expandable_list,
               true),
        USER("user",
             R.string.trusted_credentials_user_tab,
             R.id.user_tab,
             R.id.user_progress,
             R.id.user_list,
             R.id.user_expandable_list,
             false);

        private final String mTag;
        private final int mLabel;
        private final int mView;
        private final int mProgress;
        private final int mList;
        private final int mExpandableList;
        private final boolean mSwitch;

        private Tab(String tag, int label, int view, int progress, int list, int expandableList,
                boolean withSwitch) {
            mTag = tag;
            mLabel = label;
            mView = view;
            mProgress = progress;
            mList = list;
            mExpandableList = expandableList;
            mSwitch = withSwitch;
        }

        private List<ParcelableString> getAliases(IKeyChainService service) throws RemoteException {
            switch (this) {
                case SYSTEM: {
                    return service.getSystemCaAliases().getList();
                }
                case USER:
                    return service.getUserCaAliases().getList();
            }
            throw new AssertionError();
        }
        private boolean deleted(IKeyChainService service, String alias) throws RemoteException {
            switch (this) {
                case SYSTEM:
                    return !service.containsCaAlias(alias);
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
                if (certHolder.mTab.mSwitch) {
                    certHolder.mDeleted = !certHolder.mDeleted;
                } else {
                    certHolder.mAdapter.remove(certHolder);
                }
                certHolder.mAdapter.notifyDataSetChanged();
            } else {
                // bail, reload to reset to known state
                certHolder.mAdapter.load();
            }
        }
    }

    private TabHost mTabHost;
    private AliasOperation mAliasOperation;
    private HashMap<Tab, AdapterData.AliasLoader>
            mAliasLoaders = new HashMap<Tab, AdapterData.AliasLoader>(2);
    private final SparseArray<KeyChainConnection>
            mKeyChainConnectionByProfileId = new SparseArray<KeyChainConnection>();

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
    @Override
    public void onDestroy() {
        for (AdapterData.AliasLoader aliasLoader : mAliasLoaders.values()) {
            aliasLoader.cancel(true);
        }
        if (mAliasOperation != null) {
            mAliasOperation.cancel(true);
            mAliasOperation = null;
        }
        closeKeyChainConnections();
        super.onDestroy();
    }

    private void closeKeyChainConnections() {
        final int n = mKeyChainConnectionByProfileId.size();
        for (int i = 0; i < n; ++i) {
            mKeyChainConnectionByProfileId.valueAt(i).close();
        }
        mKeyChainConnectionByProfileId.clear();
    }

    private void addTab(Tab tab) {
        TabHost.TabSpec systemSpec = mTabHost.newTabSpec(tab.mTag)
                .setIndicator(getActivity().getString(tab.mLabel))
                .setContent(tab.mView);
        mTabHost.addTab(systemSpec);

        if (mUserManager.getUserProfiles().size() > 1) {
            ExpandableListView lv = (ExpandableListView) mTabHost.findViewById(tab.mExpandableList);
            final TrustedCertificateExpandableAdapter adapter =
                    new TrustedCertificateExpandableAdapter(tab);
            lv.setAdapter(adapter);
            lv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                    @Override
                public boolean onChildClick(ExpandableListView parent, View v,
                        int groupPosition, int childPosition, long id) {
                    showCertDialog(adapter.getChild(groupPosition, childPosition));
                    return true;
                }
            });
        } else {
            ListView lv = (ListView) mTabHost.findViewById(tab.mList);
            final TrustedCertificateAdapter adapter = new TrustedCertificateAdapter(tab);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override public void onItemClick(AdapterView<?> parent, View view,
                        int pos, long id) {
                    showCertDialog(adapter.getItem(pos));
                }
            });
        }
    }

    /**
     * Common interface for adapters of both expandable and non-expandable certificate lists.
     */
    private interface TrustedCertificateAdapterCommons {
        /**
         * Remove a certificate from the list.
         * @param certHolder the certificate to be removed.
         */
        void remove(CertHolder certHolder);
        /**
         * Notify the adapter that the underlying data set has changed.
         */
        void notifyDataSetChanged();
        /**
         * Load the certificates.
         */
        void load();
        /**
         * Gets the identifier of the list view the adapter is connected to.
         * @param tab the tab on which the list view resides.
         * @return identifier of the list view.
         */
        int getListViewId(Tab tab);
    }

    /**
     * Adapter for expandable list view of certificates. Groups in the view correspond to profiles
     * whereas children correspond to certificates.
     */
    private class TrustedCertificateExpandableAdapter extends BaseExpandableListAdapter implements
            TrustedCertificateAdapterCommons {
        private AdapterData mData;

        private TrustedCertificateExpandableAdapter(Tab tab) {
            mData = new AdapterData(tab, this);
            load();
        }
        @Override
        public void remove(CertHolder certHolder) {
            mData.remove(certHolder);
        }
        @Override
        public int getGroupCount() {
            return mData.mCertHoldersByUserId.size();
        }
        @Override
        public int getChildrenCount(int groupPosition) {
            List<CertHolder> certHolders = mData.mCertHoldersByUserId.valueAt(groupPosition);
            if (certHolders != null) {
                return certHolders.size();
            }
            return 0;
        }
        @Override
        public UserHandle getGroup(int groupPosition) {
            return new UserHandle(mData.mCertHoldersByUserId.keyAt(groupPosition));
        }
        @Override
        public CertHolder getChild(int groupPosition, int childPosition) {
            return mData.mCertHoldersByUserId.valueAt(groupPosition).get(childPosition);
        }
        @Override
        public long getGroupId(int groupPosition) {
            return mData.mCertHoldersByUserId.keyAt(groupPosition);
        }
        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }
        @Override
        public boolean hasStableIds() {
            return false;
        }
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflateCategoryHeader(inflater, parent);
            }

            final TextView title = (TextView) convertView.findViewById(android.R.id.title);
            final UserHandle profile = getGroup(groupPosition);
            final UserInfo userInfo = mUserManager.getUserInfo(profile.getIdentifier());
            if (userInfo.isManagedProfile()) {
                title.setText(R.string.category_work);
            } else {
                title.setText(R.string.category_personal);
            }
            title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);

            return convertView;
        }
        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            return getViewForCertificate(getChild(groupPosition, childPosition), mData.mTab,
                    convertView, parent);
        }
        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
        @Override
        public void load() {
            mData.new AliasLoader().execute();
        }
        @Override
        public int getListViewId(Tab tab) {
            return tab.mExpandableList;
        }
        private View inflateCategoryHeader(LayoutInflater inflater, ViewGroup parent) {
            final TypedArray a = inflater.getContext().obtainStyledAttributes(null,
                    com.android.internal.R.styleable.Preference,
                    com.android.internal.R.attr.preferenceCategoryStyle, 0);
            final int resId = a.getResourceId(com.android.internal.R.styleable.Preference_layout,
                    0);
            return inflater.inflate(resId, parent, false);
        }

    }

    private class TrustedCertificateAdapter extends BaseAdapter implements
            TrustedCertificateAdapterCommons {
        private final AdapterData mData;
        private TrustedCertificateAdapter(Tab tab) {
            mData = new AdapterData(tab, this);
            load();
        }
        @Override
        public void remove(CertHolder certHolder) {
            mData.remove(certHolder);
        }
        @Override
        public int getListViewId(Tab tab) {
            return tab.mList;
        }
        @Override
        public void load() {
            mData.new AliasLoader().execute();
        }
        @Override public int getCount() {
            List<CertHolder> certHolders = mData.mCertHoldersByUserId.valueAt(0);
            if (certHolders != null) {
                return certHolders.size();
            }
            return 0;
        }
        @Override public CertHolder getItem(int position) {
            return mData.mCertHoldersByUserId.valueAt(0).get(position);
        }
        @Override public long getItemId(int position) {
            return position;
        }
        @Override public View getView(int position, View view, ViewGroup parent) {
            return getViewForCertificate(getItem(position), mData.mTab, view, parent);
        }
    }

    private class AdapterData {
        private final SparseArray<List<CertHolder>> mCertHoldersByUserId =
                new SparseArray<List<CertHolder>>();
        private final Tab mTab;
        private final TrustedCertificateAdapterCommons mAdapter;

        private AdapterData(Tab tab, TrustedCertificateAdapterCommons adapter) {
            mAdapter = adapter;
            mTab = tab;
        }

        private class AliasLoader extends AsyncTask<Void, Integer, SparseArray<List<CertHolder>>> {
            private ProgressBar mProgressBar;
            private View mList;
            private Context mContext;

            public AliasLoader() {
                mContext = getActivity();
                mAliasLoaders.put(mTab, this);
            }

            @Override protected void onPreExecute() {
                View content = mTabHost.getTabContentView();
                mProgressBar = (ProgressBar) content.findViewById(mTab.mProgress);
                mList = content.findViewById(mAdapter.getListViewId(mTab));
                mProgressBar.setVisibility(View.VISIBLE);
                mList.setVisibility(View.GONE);
            }
            @Override protected SparseArray<List<CertHolder>> doInBackground(Void... params) {
                SparseArray<List<CertHolder>> certHoldersByProfile =
                        new SparseArray<List<CertHolder>>();
                try {
                    List<UserHandle> profiles = mUserManager.getUserProfiles();
                    final int n = profiles.size();
                    // First we get all aliases for all profiles in order to show progress
                    // correctly. Otherwise this could all be in a single loop.
                    SparseArray<List<ParcelableString>> aliasesByProfileId = new SparseArray<
                            List<ParcelableString>>(n);
                    int max = 0;
                    int progress = 0;
                    for (int i = 0; i < n; ++i) {
                        UserHandle profile = profiles.get(i);
                        int profileId = profile.getIdentifier();
                        KeyChainConnection keyChainConnection = KeyChain.bindAsUser(mContext,
                                profile);
                        // Saving the connection for later use on the certificate dialog.
                        mKeyChainConnectionByProfileId.put(profileId, keyChainConnection);
                        IKeyChainService service = keyChainConnection.getService();
                        List<ParcelableString> aliases = mTab.getAliases(service);
                        if (isCancelled()) {
                            return new SparseArray<List<CertHolder>>();
                        }
                        max += aliases.size();
                        aliasesByProfileId.put(profileId, aliases);
                    }
                    for (int i = 0; i < n; ++i) {
                        UserHandle profile = profiles.get(i);
                        int profileId = profile.getIdentifier();
                        List<ParcelableString> aliases = aliasesByProfileId.get(profileId);
                        if (isCancelled()) {
                            return new SparseArray<List<CertHolder>>();
                        }
                        IKeyChainService service = mKeyChainConnectionByProfileId.get(profileId)
                                .getService();
                        List<CertHolder> certHolders = new ArrayList<CertHolder>(max);
                        final int aliasMax = aliases.size();
                        for (int j = 0; j < aliasMax; ++j) {
                            String alias = aliases.get(j).string;
                            byte[] encodedCertificate = service.getEncodedCaCertificate(alias,
                                    true);
                            X509Certificate cert = KeyChain.toCertificate(encodedCertificate);
                            certHolders.add(new CertHolder(service, mAdapter,
                                    mTab, alias, cert, profileId));
                            publishProgress(++progress, max);
                        }
                        Collections.sort(certHolders);
                        certHoldersByProfile.put(profileId, certHolders);
                    }
                    return certHoldersByProfile;
                } catch (RemoteException e) {
                    Log.e(TAG, "Remote exception while loading aliases.", e);
                    return new SparseArray<List<CertHolder>>();
                } catch (InterruptedException e) {
                    Log.e(TAG, "InterruptedException while loading aliases.", e);
                    return new SparseArray<List<CertHolder>>();
                }
            }
            @Override protected void onProgressUpdate(Integer... progressAndMax) {
                int progress = progressAndMax[0];
                int max = progressAndMax[1];
                if (max != mProgressBar.getMax()) {
                    mProgressBar.setMax(max);
                }
                mProgressBar.setProgress(progress);
            }
            @Override protected void onPostExecute(SparseArray<List<CertHolder>> certHolders) {
                mCertHoldersByUserId.clear();
                final int n = certHolders.size();
                for (int i = 0; i < n; ++i) {
                    mCertHoldersByUserId.put(certHolders.keyAt(i), certHolders.valueAt(i));
                }
                mAdapter.notifyDataSetChanged();
                mProgressBar.setVisibility(View.GONE);
                mList.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(0);
                mAliasLoaders.remove(mTab);
            }
        }

        public void remove(CertHolder certHolder) {
            if (mCertHoldersByUserId != null) {
                final List<CertHolder> certs = mCertHoldersByUserId.get(certHolder.mProfileId);
                if (certs != null) {
                    certs.remove(certHolder);
                }
            }
        }
    }

    private static class CertHolder implements Comparable<CertHolder> {
        public int mProfileId;
        private final IKeyChainService mService;
        private final TrustedCertificateAdapterCommons mAdapter;
        private final Tab mTab;
        private final String mAlias;
        private final X509Certificate mX509Cert;

        private final SslCertificate mSslCert;
        private final String mSubjectPrimary;
        private final String mSubjectSecondary;
        private boolean mDeleted;

        private CertHolder(IKeyChainService service,
                           TrustedCertificateAdapterCommons adapter,
                           Tab tab,
                           String alias,
                           X509Certificate x509Cert,
                           int profileId) {
            mProfileId = profileId;
            mService = service;
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
            try {
                mDeleted = mTab.deleted(mService, mAlias);
            } catch (RemoteException e) {
                Log.e(TAG, "Remote exception while checking if alias " + mAlias + " is deleted.",
                        e);
                mDeleted = false;
            }
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

    private View getViewForCertificate(CertHolder certHolder, Tab mTab, View convertView,
            ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            convertView = inflater.inflate(R.layout.trusted_credential, parent, false);
            holder = new ViewHolder();
            holder.mSubjectPrimaryView = (TextView)
                    convertView.findViewById(R.id.trusted_credential_subject_primary);
            holder.mSubjectSecondaryView = (TextView)
                    convertView.findViewById(R.id.trusted_credential_subject_secondary);
            holder.mSwitch = (Switch) convertView.findViewById(
                    R.id.trusted_credential_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.mSubjectPrimaryView.setText(certHolder.mSubjectPrimary);
        holder.mSubjectSecondaryView.setText(certHolder.mSubjectSecondary);
        if (mTab.mSwitch) {
            holder.mSwitch.setChecked(!certHolder.mDeleted);
            holder.mSwitch.setEnabled(!mUserManager.hasUserRestriction(
                    UserManager.DISALLOW_CONFIG_CREDENTIALS,
                    new UserHandle(certHolder.mProfileId)));
            holder.mSwitch.setVisibility(View.VISIBLE);
        }
        return convertView;
    }

    private static class ViewHolder {
        private TextView mSubjectPrimaryView;
        private TextView mSubjectSecondaryView;
        private Switch mSwitch;
    }

    private void showCertDialog(final CertHolder certHolder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(com.android.internal.R.string.ssl_certificate);

        final ArrayList<View> views =  new ArrayList<View>();
        final ArrayList<String> titles = new ArrayList<String>();
        addCertChain(certHolder, views, titles);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item,
                titles);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = new Spinner(getActivity());
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                        long id) {
                    for(int i = 0; i < views.size(); i++) {
                        views.get(i).setVisibility(i == position ? View.VISIBLE : View.GONE);
                    }
                }
               @Override
               public void onNothingSelected(AdapterView<?> parent) { }
            });

        LinearLayout container = new LinearLayout(getActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(spinner);
        for (int i = 0; i < views.size(); ++i) {
            View certificateView = views.get(i);
            if (i != 0) {
                certificateView.setVisibility(View.GONE);
            }
            container.addView(certificateView);
        }
        builder.setView(container);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        final Dialog certDialog = builder.create();

        ViewGroup body = (ViewGroup) container.findViewById(com.android.internal.R.id.body);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        Button removeButton = (Button) inflater.inflate(R.layout.trusted_credential_details,
                                                        body,
                                                        false);
        if (!mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_CREDENTIALS,
                new UserHandle(certHolder.mProfileId))) {
            body.addView(removeButton);
        }
        removeButton.setText(certHolder.mTab.getButtonLabel(certHolder));
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
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

    private void addCertChain(final CertHolder certHolder,
            final ArrayList<View> views, final ArrayList<String> titles) {

        List<X509Certificate> certificates = null;
        try {
            KeyChainConnection keyChainConnection = mKeyChainConnectionByProfileId.get(
                    certHolder.mProfileId);
            IKeyChainService service = keyChainConnection.getService();
            List<String> chain = service.getCaCertificateChainAliases(certHolder.mAlias, true);
            final int n = chain.size();
            certificates = new ArrayList<X509Certificate>(n);
            for (int i = 0; i < n; ++i) {
                byte[] encodedCertificate = service.getEncodedCaCertificate(chain.get(i), true);
                X509Certificate certificate = KeyChain.toCertificate(encodedCertificate);
                certificates.add(certificate);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "RemoteException while retrieving certificate chain for root "
                    + certHolder.mAlias, ex);
            return;
        }
        for (X509Certificate certificate : certificates) {
            addCertDetails(certificate, views, titles);
        }
    }

    private void addCertDetails(X509Certificate certificate, final ArrayList<View> views,
            final ArrayList<String> titles) {
        SslCertificate sslCert = new SslCertificate(certificate);
        views.add(sslCert.inflateCertificateView(getActivity()));
        titles.add(sslCert.getIssuedTo().getCName());
    }

    private class AliasOperation extends AsyncTask<Void, Void, Boolean> {
        private final CertHolder mCertHolder;

        private AliasOperation(CertHolder certHolder) {
            mCertHolder = certHolder;
            mAliasOperation = this;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                KeyChainConnection keyChainConnection = mKeyChainConnectionByProfileId.get(
                        mCertHolder.mProfileId);
                IKeyChainService service = keyChainConnection.getService();
                if (mCertHolder.mDeleted) {
                    byte[] bytes = mCertHolder.mX509Cert.getEncoded();
                    service.installCaCertificate(bytes);
                    return true;
                } else {
                    return service.deleteCaCertificate(mCertHolder.mAlias);
                }
            } catch (CertificateEncodingException | SecurityException | IllegalStateException
                    | RemoteException e) {
                Log.w(TAG, "Error while toggling alias " + mCertHolder.mAlias,
                        e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            mCertHolder.mTab.postOperationUpdate(ok, mCertHolder);
            mAliasOperation = null;
        }
    }
}
