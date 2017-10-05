package com.android.settings.search;

import android.content.Context;

import com.android.settings.search.SearchResult;
import com.android.settings.search.SearchResultLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock loader to subvert the requirements of returning data while also driving the Loader
 * lifecycle.
 */
public class MockSearchResultLoader extends SearchResultLoader {

    public MockSearchResultLoader(Context context) {
        super(context, "test");
    }

    @Override
    public List<? extends SearchResult> loadInBackground() {
        return new ArrayList<>();
    }

    @Override
    protected void onDiscardResult(List<? extends SearchResult> result) {
    }
}
