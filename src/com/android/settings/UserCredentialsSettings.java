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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.KeyStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import java.util.EnumSet;
import java.util.SortedMap;
import java.util.TreeMap;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class UserCredentialsSettings extends OptionsMenuFragment implements OnItemClickListener {
    private static final String TAG = "UserCredentialsSettings";

    private View mRootView;
    private ListView mListView;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.USER_CREDENTIALS;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshItems();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.user_credentials, parent, false);

        // Set up an OnItemClickListener for the credential list.
        mListView = (ListView) mRootView.findViewById(R.id.credential_list);
        mListView.setOnItemClickListener(this);

        return mRootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Credential item = (Credential) parent.getItemAtPosition(position);
        CredentialDialogFragment.show(this, item);
    }

    protected void refreshItems() {
        if (isAdded()) {
            new AliasLoader().execute();
        }
    }

    public static class CredentialDialogFragment extends DialogFragment {
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
            View view = new CredentialAdapter(getActivity(), R.layout.user_credential,
                    new Credential[] {item}).getView(0, null, null);
            infoContainer.addView(view);

            UserManager userManager
                    = (UserManager) getContext().getSystemService(Context.USER_SERVICE);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setView(root)
                    .setTitle(R.string.user_credential_title)
                    .setPositiveButton(R.string.done, null);

            final String restriction = UserManager.DISALLOW_CONFIG_CREDENTIALS;
            final int myUserId = UserHandle.myUserId();
            if (!RestrictedLockUtils.hasBaseUserRestriction(getContext(), restriction, myUserId)) {
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int id) {
                        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                                getContext(), restriction, myUserId);
                        if (admin != null) {
                            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                                    admin);
                        } else {
                            new RemoveCredentialsTask(getContext(), getTargetFragment())
                                    .execute(item.alias);
                        }
                        dialog.dismiss();
                    }
                };
                builder.setNegativeButton(R.string.trusted_credentials_remove_label, listener);
            }
            return builder.create();
        }

        private class RemoveCredentialsTask extends AsyncTask<String, Void, Void> {
            private Context context;
            private Fragment targetFragment;

            public RemoveCredentialsTask(Context context, Fragment targetFragment) {
                this.context = context;
                this.targetFragment = targetFragment;
            }

            @Override
            protected Void doInBackground(String... aliases) {
                try {
                    final KeyChainConnection conn = KeyChain.bind(getContext());
                    try {
                        IKeyChainService keyChain = conn.getService();
                        for (String alias : aliases) {
                            keyChain.removeKeyPair(alias);
                        }
                    } catch (RemoteException e) {
                        Log.w(TAG, "Removing credentials", e);
                    } finally {
                        conn.close();
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, "Connecting to keychain", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (targetFragment instanceof UserCredentialsSettings) {
                    ((UserCredentialsSettings) targetFragment).refreshItems();
                }
            }
        }
    }

    /**
     * Opens a background connection to KeyStore to list user credentials.
     * The credentials are stored in a {@link CredentialAdapter} attached to the main
     * {@link ListView} in the fragment.
     */
    private class AliasLoader extends AsyncTask<Void, Void, SortedMap<String, Credential>> {
        @Override
        protected SortedMap<String, Credential> doInBackground(Void... params) {
            // Create a list of names for credential sets, ordered by name.
            SortedMap<String, Credential> credentials = new TreeMap<>();
            KeyStore keyStore = KeyStore.getInstance();
            for (final Credential.Type type : Credential.Type.values()) {
                for (final String alias : keyStore.list(type.prefix)) {
                    // Do not show work profile keys in user credentials
                    if (alias.startsWith(LockPatternUtils.PROFILE_KEY_NAME_ENCRYPT) ||
                            alias.startsWith(LockPatternUtils.PROFILE_KEY_NAME_DECRYPT)) {
                        continue;
                    }
                    Credential c = credentials.get(alias);
                    if (c == null) {
                        credentials.put(alias, (c = new Credential(alias)));
                    }
                    c.storedTypes.add(type);
                }
            }
            return credentials;
        }

        @Override
        protected void onPostExecute(SortedMap<String, Credential> credentials) {
            // Convert the results to an array and present them using an ArrayAdapter.
            mListView.setAdapter(new CredentialAdapter(getContext(), R.layout.user_credential,
                    credentials.values().toArray(new Credential[0])));
        }
    }

    /**
     * Helper class to display {@link Credential}s in a list.
     */
    private static class CredentialAdapter extends ArrayAdapter<Credential> {
        public CredentialAdapter(Context context, int resource,  Credential[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.user_credential, parent, false);
            }
            Credential item = getItem(position);
            ((TextView) view.findViewById(R.id.alias)).setText(item.alias);
            view.findViewById(R.id.contents_userkey).setVisibility(
                    item.storedTypes.contains(Credential.Type.USER_PRIVATE_KEY) ? VISIBLE : GONE);
            view.findViewById(R.id.contents_usercrt).setVisibility(
                    item.storedTypes.contains(Credential.Type.USER_CERTIFICATE) ? VISIBLE : GONE);
            view.findViewById(R.id.contents_cacrt).setVisibility(
                    item.storedTypes.contains(Credential.Type.CA_CERTIFICATE) ? VISIBLE : GONE);
            return view;
        }
    }

    static class Credential implements Parcelable {
        static enum Type {
            CA_CERTIFICATE (Credentials.CA_CERTIFICATE),
            USER_CERTIFICATE (Credentials.USER_CERTIFICATE),
            USER_PRIVATE_KEY (Credentials.USER_PRIVATE_KEY),
            USER_SECRET_KEY (Credentials.USER_SECRET_KEY);

            final String prefix;

            Type(String prefix) {
                this.prefix = prefix;
            }
        }

        /**
         * Main part of the credential's alias. To fetch an item from KeyStore, prepend one of the
         * prefixes from {@link CredentialItem.storedTypes}.
         */
        final String alias;

        /**
         * Should contain some non-empty subset of:
         * <ul>
         *   <li>{@link Credentials.CA_CERTIFICATE}</li>
         *   <li>{@link Credentials.USER_CERTIFICATE}</li>
         *   <li>{@link Credentials.USER_PRIVATE_KEY}</li>
         *   <li>{@link Credentials.USER_SECRET_KEY}</li>
         * </ul>
         */
        final EnumSet<Type> storedTypes = EnumSet.noneOf(Type.class);

        Credential(final String alias) {
            this.alias = alias;
        }

        Credential(Parcel in) {
            this(in.readString());

            long typeBits = in.readLong();
            for (Type i : Type.values()) {
                if ((typeBits & (1L << i.ordinal())) != 0L) {
                    storedTypes.add(i);
                }
            }
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(alias);

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
    }
}
