/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.annotation.LayoutRes;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.keystore.KeyProperties;
import android.security.keystore2.AndroidKeyStoreLoadStoreParameter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.wifi.helper.SavedWifiHelper;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.SecretKey;

public class UserCredentialsSettings extends SettingsPreferenceFragment
        implements View.OnClickListener {
    private static final String TAG = "UserCredentialsSettings";

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    @VisibleForTesting
    protected SavedWifiHelper mSavedWifiHelper;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USER_CREDENTIALS;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshItems();
    }

    @Override
    public void onClick(final View view) {
        final Credential item = (Credential) view.getTag();
        if (item == null) return;
        if (item.isInUse()) {
            item.setUsedByNames(mSavedWifiHelper.getCertificateNetworkNames(item.alias));
        }
        showCredentialDialogFragment(item);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.user_credentials);
        mSavedWifiHelper = SavedWifiHelper.getInstance(getContext(), getSettingsLifecycle());
    }

    @VisibleForTesting
    protected void showCredentialDialogFragment(Credential item) {
        CredentialDialogFragment.show(this, item);
    }

    protected void announceRemoval(String alias) {
        if (!isAdded()) {
            return;
        }
        getListView().announceForAccessibility(getString(R.string.user_credential_removed, alias));
    }

    protected void refreshItems() {
        if (isAdded()) {
            new AliasLoader().execute();
        }
    }

    /** The fragment to show the credential information. */
    public static class CredentialDialogFragment extends InstrumentedDialogFragment
            implements DialogInterface.OnShowListener {
        private static final String TAG = "CredentialDialogFragment";
        private static final String ARG_CREDENTIAL = "credential";

        public static void show(Fragment target, Credential item) {
            final Bundle args = new Bundle();
            args.putParcelable(ARG_CREDENTIAL, item);

            if (target.getFragmentManager().findFragmentByTag(TAG) == null) {
                final DialogFragment frag = new CredentialDialogFragment();
                frag.setTargetFragment(target, /* requestCode */ -1);
                frag.setArguments(args);
                frag.show(target.getFragmentManager(), TAG);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Credential item = (Credential) getArguments().getParcelable(ARG_CREDENTIAL);

            View root = getActivity().getLayoutInflater()
                    .inflate(R.layout.user_credential_dialog, null);
            ViewGroup infoContainer = (ViewGroup) root.findViewById(R.id.credential_container);
            View contentView = getCredentialView(item, R.layout.user_credential, null,
                    infoContainer, /* expanded */ true);
            infoContainer.addView(contentView);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setView(root)
                    .setTitle(R.string.user_credential_title)
                    .setPositiveButton(R.string.done, null);

            final String restriction = UserManager.DISALLOW_CONFIG_CREDENTIALS;
            final int myUserId = UserHandle.myUserId();
            if (!RestrictedLockUtilsInternal.hasBaseUserRestriction(getContext(), restriction,
                    myUserId)) {
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int id) {
                        final EnforcedAdmin admin = RestrictedLockUtilsInternal
                                .checkIfRestrictionEnforced(getContext(), restriction, myUserId);
                        if (admin != null) {
                            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                                    admin);
                        } else {
                            new RemoveCredentialsTask(getContext(), getTargetFragment())
                                    .execute(item);
                        }
                        dialog.dismiss();
                    }
                };
                builder.setNegativeButton(R.string.trusted_credentials_remove_label, listener);
            }
            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(this);
            return dialog;
        }

        /**
         * Override for the negative button enablement on demand.
         */
        @Override
        public void onShow(DialogInterface dialogInterface) {
            final Credential item = (Credential) getArguments().getParcelable(ARG_CREDENTIAL);
            if (item.isInUse()) {
                ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setEnabled(false);
            }
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_USER_CREDENTIAL;
        }

        /**
         * Deletes all certificates and keys under a given alias.
         *
         * If the {@link Credential} is for a system alias, all active grants to the alias will be
         * removed using {@link KeyChain}. If the {@link Credential} is for Wi-Fi alias, all
         * credentials and keys will be removed using {@link KeyStore}.
         */
        private class RemoveCredentialsTask extends AsyncTask<Credential, Void, Credential[]> {
            private Context context;
            private Fragment targetFragment;

            public RemoveCredentialsTask(Context context, Fragment targetFragment) {
                this.context = context;
                this.targetFragment = targetFragment;
            }

            @Override
            protected Credential[] doInBackground(Credential... credentials) {
                for (final Credential credential : credentials) {
                    if (credential.isSystem()) {
                        removeGrantsAndDelete(credential);
                    } else {
                        deleteWifiCredential(credential);
                    }
                }
                return credentials;
            }

            private void deleteWifiCredential(final Credential credential) {
                try {
                    final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
                    keyStore.load(
                            new AndroidKeyStoreLoadStoreParameter(
                                    KeyProperties.NAMESPACE_WIFI));
                    keyStore.deleteEntry(credential.getAlias());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to delete keys from keystore.");
                }
            }

            private void removeGrantsAndDelete(final Credential credential) {
                final KeyChainConnection conn;
                try {
                    conn = KeyChain.bind(getContext());
                } catch (InterruptedException e) {
                    Log.w(TAG, "Connecting to KeyChain", e);
                    return;
                }

                try {
                    IKeyChainService keyChain = conn.getService();
                    keyChain.removeKeyPair(credential.alias);
                } catch (RemoteException e) {
                    Log.w(TAG, "Removing credentials", e);
                } finally {
                    conn.close();
                }
            }

            @Override
            protected void onPostExecute(Credential... credentials) {
                if (targetFragment instanceof UserCredentialsSettings && targetFragment.isAdded()) {
                    final UserCredentialsSettings target = (UserCredentialsSettings) targetFragment;
                    for (final Credential credential : credentials) {
                        target.announceRemoval(credential.alias);
                    }
                    target.refreshItems();
                }
            }
        }
    }

    /**
     * Opens a background connection to KeyStore to list user credentials.
     * The credentials are stored in a {@link CredentialAdapter} attached to the main
     * {@link ListView} in the fragment.
     */
    private class AliasLoader extends AsyncTask<Void, Void, List<Credential>> {
        /**
         * @return a list of credentials ordered:
         * <ol>
         *   <li>first by purpose;</li>
         *   <li>then by alias.</li>
         * </ol>
         */
        @Override
        protected List<Credential> doInBackground(Void... params) {
            // Certificates can be installed into SYSTEM_UID or WIFI_UID through CertInstaller.
            final int myUserId = UserHandle.myUserId();
            final int systemUid = UserHandle.getUid(myUserId, Process.SYSTEM_UID);
            final int wifiUid = UserHandle.getUid(myUserId, Process.WIFI_UID);

            try {
                KeyStore processKeystore = KeyStore.getInstance(KEYSTORE_PROVIDER);
                processKeystore.load(null);
                KeyStore wifiKeystore = null;
                if (myUserId == 0) {
                    wifiKeystore = KeyStore.getInstance(KEYSTORE_PROVIDER);
                    wifiKeystore.load(new AndroidKeyStoreLoadStoreParameter(
                            KeyProperties.NAMESPACE_WIFI));
                }

                List<Credential> credentials = new ArrayList<>();
                credentials.addAll(getCredentialsForUid(processKeystore, systemUid).values());
                if (wifiKeystore != null) {
                    credentials.addAll(getCredentialsForUid(wifiKeystore, wifiUid).values());
                }
                return credentials;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load credentials from Keystore.", e);
            }
        }

        private SortedMap<String, Credential> getCredentialsForUid(KeyStore keyStore, int uid) {
            try {
                final SortedMap<String, Credential> aliasMap = new TreeMap<>();
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Credential c = new Credential(alias, uid);
                    if (!c.isSystem()) {
                        c.setInUse(mSavedWifiHelper.isCertificateInUse(alias));
                    }
                    Key key = null;
                    try {
                        key = keyStore.getKey(alias, null);
                    } catch (NoSuchAlgorithmException | UnrecoverableKeyException e) {
                        Log.e(TAG, "Error tying to retrieve key: " + alias, e);
                        continue;
                    }
                    if (key != null) {
                        // So we have a key
                        if (key instanceof SecretKey) {
                            // We don't display any symmetric key entries.
                            continue;
                        }
                        // At this point we have determined that we have an asymmetric key.
                        // so we have at least a USER_KEY and USER_CERTIFICATE.
                        c.storedTypes.add(Credential.Type.USER_KEY);

                        Certificate[] certs =  keyStore.getCertificateChain(alias);
                        if (certs != null) {
                            c.storedTypes.add(Credential.Type.USER_CERTIFICATE);
                            if (certs.length > 1) {
                                c.storedTypes.add(Credential.Type.CA_CERTIFICATE);
                            }
                        }
                    } else {
                        // So there is no key but we have an alias. This must mean that we have
                        // some certificate.
                        if (keyStore.isCertificateEntry(alias)) {
                            c.storedTypes.add(Credential.Type.CA_CERTIFICATE);
                        } else {
                            // This is a weired inconsistent case that should not exist.
                            // Pure trusted certificate entries should be stored in CA_CERTIFICATE,
                            // but if isCErtificateEntry returns null this means that only the
                            // USER_CERTIFICATE is populated which should never be the case without
                            // a private key. It can still be retrieved with
                            // keystore.getCertificate().
                            c.storedTypes.add(Credential.Type.USER_CERTIFICATE);
                        }
                    }
                    aliasMap.put(alias, c);
                }
                return aliasMap;
            } catch (KeyStoreException e) {
                throw new RuntimeException("Failed to load credential from Android Keystore.", e);
            }
        }

        @Override
        protected void onPostExecute(List<Credential> credentials) {
            if (!isAdded()) {
                return;
            }

            if (credentials == null || credentials.size() == 0) {
                // Create a "no credentials installed" message for the empty case.
                TextView emptyTextView = (TextView) getActivity().findViewById(android.R.id.empty);
                emptyTextView.setText(R.string.user_credential_none_installed);
                setEmptyView(emptyTextView);
            } else {
                setEmptyView(null);
            }

            getListView().setAdapter(
                    new CredentialAdapter(credentials, UserCredentialsSettings.this));
        }
    }

    /**
     * Helper class to display {@link Credential}s in a list.
     */
    private static class CredentialAdapter extends RecyclerView.Adapter<ViewHolder> {
        private static final int LAYOUT_RESOURCE = R.layout.user_credential_preference;

        private final List<Credential> mItems;
        private final View.OnClickListener mListener;

        public CredentialAdapter(List<Credential> items, @Nullable View.OnClickListener listener) {
            mItems = items;
            mListener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ViewHolder(inflater.inflate(LAYOUT_RESOURCE, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder h, int position) {
            getCredentialView(mItems.get(position), LAYOUT_RESOURCE, h.itemView, null, false);
            h.itemView.setTag(mItems.get(position));
            h.itemView.setOnClickListener(mListener);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View item) {
            super(item);
        }
    }

    /**
     * Mapping from View IDs in {@link R} to the types of credentials they describe.
     */
    private static final SparseArray<Credential.Type> credentialViewTypes = new SparseArray<>();
    static {
        credentialViewTypes.put(R.id.contents_userkey, Credential.Type.USER_KEY);
        credentialViewTypes.put(R.id.contents_usercrt, Credential.Type.USER_CERTIFICATE);
        credentialViewTypes.put(R.id.contents_cacrt, Credential.Type.CA_CERTIFICATE);
    }

    protected static View getCredentialView(Credential item, @LayoutRes int layoutResource,
            @Nullable View view, ViewGroup parent, boolean expanded) {
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(layoutResource, parent, false);
        }

        ((TextView) view.findViewById(R.id.alias)).setText(item.alias);
        updatePurposeView(view.findViewById(R.id.purpose), item);

        view.findViewById(R.id.contents).setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (expanded) {
            updateUsedByViews(view.findViewById(R.id.credential_being_used_by_title),
                    view.findViewById(R.id.credential_being_used_by_content), item);

            for (int i = 0; i < credentialViewTypes.size(); i++) {
                final View detail = view.findViewById(credentialViewTypes.keyAt(i));
                detail.setVisibility(item.storedTypes.contains(credentialViewTypes.valueAt(i))
                        ? View.VISIBLE : View.GONE);
            }
        }
        return view;
    }

    @VisibleForTesting
    protected static void updatePurposeView(TextView purpose, Credential item) {
        int subTextResId = R.string.credential_for_vpn_and_apps;
        if (!item.isSystem()) {
            subTextResId = (item.isInUse())
                    ? R.string.credential_for_wifi_in_use
                    : R.string.credential_for_wifi;
        }
        purpose.setText(subTextResId);
    }

    @VisibleForTesting
    protected static void updateUsedByViews(TextView title, TextView content, Credential item) {
        List<String> usedByNames = item.getUsedByNames();
        if (usedByNames.size() > 0) {
            title.setVisibility(View.VISIBLE);
            content.setText(String.join("\n", usedByNames));
            content.setVisibility(View.VISIBLE);
        } else {
            title.setVisibility(View.GONE);
            content.setVisibility(View.GONE);
        }
    }

    static class AliasEntry {
        public String alias;
        public int uid;
    }

    static class Credential implements Parcelable {
        static enum Type {
            CA_CERTIFICATE (Credentials.CA_CERTIFICATE),
            USER_CERTIFICATE (Credentials.USER_CERTIFICATE),
            USER_KEY(Credentials.USER_PRIVATE_KEY, Credentials.USER_SECRET_KEY);

            final String[] prefix;

            Type(String... prefix) {
                this.prefix = prefix;
            }
        }

        /**
         * Main part of the credential's alias. To fetch an item from KeyStore, prepend one of the
         * prefixes from {@link CredentialItem.storedTypes}.
         */
        final String alias;

        /**
         * UID under which this credential is stored. Typically {@link Process#SYSTEM_UID} but can
         * also be {@link Process#WIFI_UID} for credentials installed as wifi certificates.
         */
        final int uid;

        /**
         * Indicate whether or not this credential is in use.
         */
        boolean mIsInUse;

        /**
         * The list of networks which use this credential.
         */
        List<String> mUsedByNames = new ArrayList<>();

        /**
         * Should contain some non-empty subset of:
         * <ul>
         *   <li>{@link Credentials.CA_CERTIFICATE}</li>
         *   <li>{@link Credentials.USER_CERTIFICATE}</li>
         *   <li>{@link Credentials.USER_KEY}</li>
         * </ul>
         */
        final EnumSet<Type> storedTypes = EnumSet.noneOf(Type.class);

        Credential(final String alias, final int uid) {
            this.alias = alias;
            this.uid = uid;
        }

        Credential(Parcel in) {
            this(in.readString(), in.readInt());

            long typeBits = in.readLong();
            for (Type i : Type.values()) {
                if ((typeBits & (1L << i.ordinal())) != 0L) {
                    storedTypes.add(i);
                }
            }
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(alias);
            out.writeInt(uid);

            long typeBits = 0;
            for (Type i : storedTypes) {
                typeBits |= 1L << i.ordinal();
            }
            out.writeLong(typeBits);
        }

        public int describeContents() {
            return 0;
        }

        public static final Parcelable.Creator<Credential> CREATOR
                = new Parcelable.Creator<Credential>() {
            public Credential createFromParcel(Parcel in) {
                return new Credential(in);
            }

            public Credential[] newArray(int size) {
                return new Credential[size];
            }
        };

        public boolean isSystem() {
            return UserHandle.getAppId(uid) == Process.SYSTEM_UID;
        }

        public String getAlias() {
            return alias;
        }

        public EnumSet<Type> getStoredTypes() {
            return storedTypes;
        }

        public void setInUse(boolean inUse) {
            mIsInUse = inUse;
        }

        public boolean isInUse() {
            return mIsInUse;
        }

        public void setUsedByNames(List<String> names) {
            mUsedByNames = new ArrayList<>(names);
        }

        public List<String> getUsedByNames() {
            return new ArrayList<String>(mUsedByNames);
        }
    }
}
