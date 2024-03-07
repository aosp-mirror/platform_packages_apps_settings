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

package com.android.settings.print;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.print.PrintManager;
import android.print.PrinterDiscoverySession;
import android.print.PrinterDiscoverySession.OnPrintersChangeListener;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SettingsMainSwitchBar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment with print service settings.
 */
public class PrintServiceSettingsFragment extends SettingsPreferenceFragment
        implements OnCheckedChangeListener,
        LoaderManager.LoaderCallbacks<List<PrintServiceInfo>> {

    private static final String LOG_TAG = "PrintServiceSettings";

    private static final int LOADER_ID_PRINTERS_LOADER = 1;
    private static final int LOADER_ID_PRINT_SERVICE_LOADER = 2;

    private final AdapterDataObserver mDataObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            invalidateOptionsMenuIfNeeded();
            updateEmptyView();
        }

        private void invalidateOptionsMenuIfNeeded() {
            final int unfilteredItemCount = mPrintersAdapter.getUnfilteredCount();
            if ((mLastUnfilteredItemCount <= 0 && unfilteredItemCount > 0)
                    || mLastUnfilteredItemCount > 0 && unfilteredItemCount <= 0) {
                getActivity().invalidateOptionsMenu();
            }
            mLastUnfilteredItemCount = unfilteredItemCount;
        }
    };

    private SettingsMainSwitchBar mSwitchBar;

    private String mPreferenceKey;

    private Intent mSettingsIntent;

    private Intent mAddPrintersIntent;

    private ComponentName mComponentName;

    private PrintersAdapter mPrintersAdapter;

    private int mLastUnfilteredItemCount;

    private boolean mServiceEnabled;

    private SearchView mSearchView;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRINT_SERVICE_SETTINGS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        String title = getArguments().getString(PrintSettingsFragment.EXTRA_TITLE);
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        mServiceEnabled = getArguments().getBoolean(PrintSettingsFragment.EXTRA_CHECKED);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        initComponents();
        updateUiForArguments();
        updateEmptyView();
        updateUiForServiceState();
    }

    @Override
    public void onPause() {
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(null);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
        mPrintersAdapter.unregisterAdapterDataObserver(mDataObserver);
    }

    private void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ((PrintManager) getContext().getSystemService(Context.PRINT_SERVICE))
                .setPrintServiceEnabled(mComponentName, enabled);
    }

    private void updateEmptyView() {
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getEmptyView();
        if (!mSwitchBar.isChecked()) {
            if (emptyView != null) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                emptyView = getActivity().getLayoutInflater().inflate(
                        R.layout.empty_print_state, contentRoot, false);
                TextView textView = (TextView) emptyView.findViewById(R.id.message);
                textView.setText(R.string.print_service_disabled);
                contentRoot.addView(emptyView);
                setEmptyView(emptyView);
            }
        } else if (mPrintersAdapter.getUnfilteredCount() <= 0) {
            if (emptyView != null) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                emptyView = getActivity().getLayoutInflater().inflate(
                        R.layout.empty_printers_list_service_enabled, contentRoot, false);
                contentRoot.addView(emptyView);
                setEmptyView(emptyView);
            }
        } else if (mPrintersAdapter.getItemCount() <= 0) {
            if (emptyView != null) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                emptyView = getActivity().getLayoutInflater().inflate(
                        R.layout.empty_print_state, contentRoot, false);
                TextView textView = (TextView) emptyView.findViewById(R.id.message);
                textView.setText(R.string.print_no_printers_found);
                contentRoot.addView(emptyView);
                setEmptyView(emptyView);
            }
        } else if (mPrintersAdapter.getItemCount() > 0) {
            if (emptyView != null) {
                contentRoot.removeView(emptyView);
            }
        }
    }

    private void updateUiForServiceState() {
        if (mServiceEnabled) {
            mSwitchBar.setCheckedInternal(true);
            mPrintersAdapter.enable();
        } else {
            mSwitchBar.setCheckedInternal(false);
            mPrintersAdapter.disable();
        }
        getActivity().invalidateOptionsMenu();
    }

    private void initComponents() {
        mPrintersAdapter = new PrintersAdapter();
        mPrintersAdapter.registerAdapterDataObserver(mDataObserver);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.setTitle(
                getContext().getString(R.string.default_print_service_main_switch_title));
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();

        mSwitchBar.setOnBeforeCheckedChangeListener((checked) -> {
            onPreferenceToggled(mPreferenceKey, checked);
            return false;
        });

        getListView().setAdapter(mPrintersAdapter);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateEmptyView();
    }

    private void updateUiForArguments() {
        Bundle arguments = getArguments();

        // Component name.
        mComponentName = ComponentName.unflattenFromString(arguments
                .getString(PrintSettingsFragment.EXTRA_SERVICE_COMPONENT_NAME));

        // Key.
        mPreferenceKey = mComponentName.flattenToString();

        // Enabled.
        final boolean enabled = arguments.getBoolean(PrintSettingsFragment.EXTRA_CHECKED);
        mSwitchBar.setCheckedInternal(enabled);

        getLoaderManager().initLoader(LOADER_ID_PRINT_SERVICE_LOADER, null, this);
        setHasOptionsMenu(true);
    }

    @Override
    public Loader<List<PrintServiceInfo>> onCreateLoader(int id, Bundle args) {
        return new SettingsPrintServicesLoader(
                (PrintManager) getContext().getSystemService(Context.PRINT_SERVICE), getContext(),
                PrintManager.ALL_SERVICES);
    }

    @Override
    public void onLoadFinished(Loader<List<PrintServiceInfo>> loader,
            List<PrintServiceInfo> services) {
        PrintServiceInfo service = null;

        if (services != null) {
            final int numServices = services.size();
            for (int i = 0; i < numServices; i++) {
                if (services.get(i).getComponentName().equals(mComponentName)) {
                    service = services.get(i);
                    break;
                }
            }
        }

        if (service == null) {
            // The print service was uninstalled
            finishFragment();
        }

        mServiceEnabled = service.isEnabled();

        if (service.getSettingsActivityName() != null) {
            Intent settingsIntent = new Intent(Intent.ACTION_MAIN);

            settingsIntent.setComponent(
                    new ComponentName(service.getComponentName().getPackageName(),
                            service.getSettingsActivityName()));

            List<ResolveInfo> resolvedActivities = getPackageManager().queryIntentActivities(
                    settingsIntent, 0);
            if (!resolvedActivities.isEmpty()) {
                // The activity is a component name, therefore it is one or none.
                if (resolvedActivities.get(0).activityInfo.exported) {
                    mSettingsIntent = settingsIntent;
                }
            }
        } else {
            mSettingsIntent = null;
        }

        if (service.getAddPrintersActivityName() != null) {
            Intent addPrintersIntent = new Intent(Intent.ACTION_MAIN);

            addPrintersIntent.setComponent(
                    new ComponentName(service.getComponentName().getPackageName(),
                            service.getAddPrintersActivityName()));

            List<ResolveInfo> resolvedActivities = getPackageManager().queryIntentActivities(
                    addPrintersIntent, 0);
            if (!resolvedActivities.isEmpty()) {
                // The activity is a component name, therefore it is one or none.
                if (resolvedActivities.get(0).activityInfo.exported) {
                    mAddPrintersIntent = addPrintersIntent;
                }
            }
        } else {
            mAddPrintersIntent = null;
        }

        updateUiForServiceState();
    }

    @Override
    public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
        updateUiForServiceState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.print_service_settings, menu);

        MenuItem addPrinters = menu.findItem(R.id.print_menu_item_add_printer);
        if (mServiceEnabled && mAddPrintersIntent != null) {
            addPrinters.setIntent(mAddPrintersIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_add_printer);
        }

        MenuItem settings = menu.findItem(R.id.print_menu_item_settings);
        if (mServiceEnabled && mSettingsIntent != null) {
            settings.setIntent(mSettingsIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_settings);
        }

        MenuItem searchItem = menu.findItem(R.id.print_menu_item_search);
        if (mServiceEnabled && mPrintersAdapter.getUnfilteredCount() > 0) {
            mSearchView = (SearchView) searchItem.getActionView();
            mSearchView.setMaxWidth(Integer.MAX_VALUE);
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String searchString) {
                    mPrintersAdapter.getFilter().filter(searchString);
                    return true;
                }
            });
            mSearchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    if (AccessibilityManager.getInstance(getActivity()).isEnabled()) {
                        view.announceForAccessibility(getString(
                                R.string.print_search_box_shown_utterance));
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    Activity activity = getActivity();
                    if (activity != null && !activity.isFinishing()
                            && AccessibilityManager.getInstance(activity).isEnabled()) {
                        view.announceForAccessibility(getString(
                                R.string.print_search_box_hidden_utterance));
                    }
                }
            });
        } else {
            menu.removeItem(R.id.print_menu_item_search);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }


    private final class PrintersAdapter extends RecyclerView.Adapter<ViewHolder>
            implements LoaderManager.LoaderCallbacks<List<PrinterInfo>>, Filterable {

        private final Object mLock = new Object();

        private final List<PrinterInfo> mPrinters = new ArrayList<PrinterInfo>();

        private final List<PrinterInfo> mFilteredPrinters = new ArrayList<PrinterInfo>();

        private CharSequence mLastSearchString;

        public void enable() {
            getLoaderManager().initLoader(LOADER_ID_PRINTERS_LOADER, null, this);
        }

        public void disable() {
            getLoaderManager().destroyLoader(LOADER_ID_PRINTERS_LOADER);
            mPrinters.clear();
        }

        public int getUnfilteredCount() {
            return mPrinters.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    synchronized (mLock) {
                        if (TextUtils.isEmpty(constraint)) {
                            return null;
                        }
                        FilterResults results = new FilterResults();
                        List<PrinterInfo> filteredPrinters = new ArrayList<PrinterInfo>();
                        String constraintLowerCase = constraint.toString().toLowerCase();
                        final int printerCount = mPrinters.size();
                        for (int i = 0; i < printerCount; i++) {
                            PrinterInfo printer = mPrinters.get(i);
                            String name = printer.getName();
                            if (name != null && name.toLowerCase().contains(constraintLowerCase)) {
                                filteredPrinters.add(printer);
                            }
                        }
                        results.values = filteredPrinters;
                        results.count = filteredPrinters.size();
                        return results;
                    }
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    synchronized (mLock) {
                        mLastSearchString = constraint;
                        mFilteredPrinters.clear();
                        if (results == null) {
                            mFilteredPrinters.addAll(mPrinters);
                        } else {
                            List<PrinterInfo> printers = (List<PrinterInfo>) results.values;
                            mFilteredPrinters.addAll(printers);
                        }
                    }
                    notifyDataSetChanged();

                }
            };
        }

        @Override
        public int getItemCount() {
            synchronized (mLock) {
                return mFilteredPrinters.size();
            }
        }

        private Object getItem(int position) {
            synchronized (mLock) {
                return mFilteredPrinters.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Checks if a printer can be used for printing
         *
         * @param position The position of the printer in the list
         * @return true iff the printer can be used for printing.
         */
        public boolean isActionable(int position) {
            PrinterInfo printer = (PrinterInfo) getItem(position);
            return printer.getStatus() != PrinterInfo.STATUS_UNAVAILABLE;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.printer_dropdown_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.itemView.setEnabled(isActionable(position));

            final PrinterInfo printer = (PrinterInfo) getItem(position);
            CharSequence title = printer.getName();
            CharSequence subtitle = printer.getDescription();
            Drawable icon = printer.loadIcon(getActivity());

            TextView titleView = holder.itemView.findViewById(R.id.title);
            titleView.setText(title);

            TextView subtitleView = holder.itemView.findViewById(R.id.subtitle);
            if (!TextUtils.isEmpty(subtitle)) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(View.VISIBLE);
            } else {
                subtitleView.setText(null);
                subtitleView.setVisibility(View.GONE);
            }

            LinearLayout moreInfoView = holder.itemView.findViewById(R.id.more_info);
            if (printer.getInfoIntent() != null) {
                moreInfoView.setVisibility(View.VISIBLE);
                moreInfoView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Bundle options = ActivityOptions.makeBasic()
                                    .setPendingIntentBackgroundActivityStartMode(
                                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                                    .toBundle();
                            getActivity().startIntentSender(
                                    printer.getInfoIntent().getIntentSender(), null, 0, 0, 0,
                                    options);
                        } catch (SendIntentException e) {
                            Log.e(LOG_TAG, "Could not execute pending info intent: %s", e);
                        }
                    }
                });
            } else {
                moreInfoView.setVisibility(View.GONE);
            }

            ImageView iconView = holder.itemView.findViewById(R.id.icon);
            if (icon != null) {
                iconView.setVisibility(View.VISIBLE);
                if (!isActionable(position)) {
                    icon.mutate();

                    TypedValue value = new TypedValue();
                    getActivity().getTheme().resolveAttribute(android.R.attr.disabledAlpha, value,
                            true);
                    icon.setAlpha((int) (value.getFloat() * 255));
                }
                iconView.setImageDrawable(icon);
            } else {
                iconView.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                PrinterInfo pi = (PrinterInfo) getItem(position);

                if (pi.getInfoIntent() != null) {
                    try {
                        getActivity().startIntentSender(pi.getInfoIntent().getIntentSender(),
                                null, 0, 0, 0);
                    } catch (SendIntentException e) {
                        Log.e(LOG_TAG, "Could not execute info intent: %s", e);
                    }
                }
            });
        }

        @Override
        public Loader<List<PrinterInfo>> onCreateLoader(int id, Bundle args) {
            if (id == LOADER_ID_PRINTERS_LOADER) {
                return new PrintersLoader(getContext());
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<List<PrinterInfo>> loader,
                List<PrinterInfo> printers) {
            synchronized (mLock) {
                mPrinters.clear();
                final int printerCount = printers.size();
                for (int i = 0; i < printerCount; i++) {
                    PrinterInfo printer = printers.get(i);
                    if (printer.getId().getServiceName().equals(mComponentName)) {
                        mPrinters.add(printer);
                    }
                }
                mFilteredPrinters.clear();
                mFilteredPrinters.addAll(mPrinters);
                if (!TextUtils.isEmpty(mLastSearchString)) {
                    getFilter().filter(mLastSearchString);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<PrinterInfo>> loader) {
            synchronized (mLock) {
                mPrinters.clear();
                mFilteredPrinters.clear();
                mLastSearchString = null;
            }
            notifyDataSetChanged();
        }
    }

    private static class PrintersLoader extends Loader<List<PrinterInfo>> {

        private static final String LOG_TAG = "PrintersLoader";

        private static final boolean DEBUG = false;

        private final Map<PrinterId, PrinterInfo> mPrinters =
                new LinkedHashMap<PrinterId, PrinterInfo>();

        private PrinterDiscoverySession mDiscoverySession;

        public PrintersLoader(Context context) {
            super(context);
        }

        @Override
        public void deliverResult(List<PrinterInfo> printers) {
            if (isStarted()) {
                super.deliverResult(printers);
            }
        }

        @Override
        protected void onStartLoading() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onStartLoading()");
            }
            // The contract is that if we already have a valid,
            // result the we have to deliver it immediately.
            if (!mPrinters.isEmpty()) {
                deliverResult(new ArrayList<PrinterInfo>(mPrinters.values()));
            }
            // We want to start discovery at this point.
            onForceLoad();
        }

        @Override
        protected void onStopLoading() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onStopLoading()");
            }
            onCancelLoad();
        }

        @Override
        protected void onForceLoad() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onForceLoad()");
            }
            loadInternal();
        }

        @Override
        protected boolean onCancelLoad() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onCancelLoad()");
            }
            return cancelInternal();
        }

        @Override
        protected void onReset() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onReset()");
            }
            onStopLoading();
            mPrinters.clear();
            if (mDiscoverySession != null) {
                mDiscoverySession.destroy();
                mDiscoverySession = null;
            }
        }

        @Override
        protected void onAbandon() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onAbandon()");
            }
            onStopLoading();
        }

        private boolean cancelInternal() {
            if (mDiscoverySession != null
                    && mDiscoverySession.isPrinterDiscoveryStarted()) {
                mDiscoverySession.stopPrinterDiscovery();
                return true;
            }
            return false;
        }

        private void loadInternal() {
            if (mDiscoverySession == null) {
                PrintManager printManager = (PrintManager) getContext()
                        .getSystemService(Context.PRINT_SERVICE);
                mDiscoverySession = printManager.createPrinterDiscoverySession();
                mDiscoverySession.setOnPrintersChangeListener(new OnPrintersChangeListener() {
                    @Override
                    public void onPrintersChanged() {
                        deliverResult(new ArrayList<PrinterInfo>(
                                mDiscoverySession.getPrinters()));
                    }
                });
            }
            mDiscoverySession.startPrinterDiscovery(null);
        }
    }
}
