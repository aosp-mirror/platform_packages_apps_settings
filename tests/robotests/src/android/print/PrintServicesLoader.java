package android.print;

import android.annotation.NonNull;
import android.content.Context;
import android.content.Loader;
import android.printservice.PrintServiceInfo;
import com.android.internal.util.Preconditions;

import java.util.List;

/**
 * A placeholder class to prevent ClassNotFound exceptions caused by lack of visibility.
 */
public class PrintServicesLoader extends Loader<List<PrintServiceInfo>> {
    public PrintServicesLoader(@NonNull PrintManager printManager, @NonNull Context context,
            int selectionFlags) {
        super(Preconditions.checkNotNull(context));
    }
}
