package com.android.settings.mahdi.superuser;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.settings.Utils;
import com.koushikdutta.superuser.LogNativeFragment;
import com.koushikdutta.superuser.PolicyFragmentInternal;
import com.koushikdutta.superuser.SettingsNativeFragment;

public class PolicyNativeFragment extends com.koushikdutta.superuser.PolicyNativeFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Utils.forcePrepareCustomPreferencesList(container, view, getInternal().getListView(), false);
        return view;
    }

    @Override
    public PolicyFragmentInternal createFragmentInterface() {
        return new FragmentInternal(this) {
            @Override
            protected LogNativeFragment createLogNativeFragment() {
                return new LogNativeFragment() {
                    @Override
                    public View onCreateView(LayoutInflater inflater,
                            ViewGroup container, Bundle savedInstanceState) {
                        View view = super.onCreateView(inflater, container, savedInstanceState);
                        adjustListPadding(getInternal().getListView());
                        return view;
                    }
                };
            }

            @Override
            protected SettingsNativeFragment createSettingsNativeFragment() {
                return new SettingsNativeFragment() {
                    @Override
                    public View onCreateView(LayoutInflater inflater,
                            ViewGroup container, Bundle savedInstanceState) {
                        View view = super.onCreateView(inflater, container, savedInstanceState);
                        adjustListPadding(getInternal().getListView());
                        return view;
                    }
                };
            };
        };
    }

    private static void adjustListPadding(ListView list) {
        final Resources res = list.getResources();
        final int paddingSide = res.getDimensionPixelSize(
                com.android.internal.R.dimen.preference_fragment_padding_side);
        final int paddingBottom = res.getDimensionPixelSize(
                com.android.internal.R.dimen.preference_fragment_padding_bottom);

        list.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        list.setPadding(paddingSide, 0, paddingSide, paddingBottom);
    }
}
