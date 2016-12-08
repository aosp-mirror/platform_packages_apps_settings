package com.android.internal.app;

/**
 * A placeholder class to prevent ClassNotFound exceptions caused by lack of visibility.
 */
public class LocalePickerWithRegion {

    public interface LocaleSelectedListener {
        void onLocaleSelected(LocaleStore.LocaleInfo locale);
    }
}
