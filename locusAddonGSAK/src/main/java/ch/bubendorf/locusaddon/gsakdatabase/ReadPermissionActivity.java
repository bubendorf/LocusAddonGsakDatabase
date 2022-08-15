package ch.bubendorf.locusaddon.gsakdatabase;

import android.Manifest;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.BiConsumer;

public class ReadPermissionActivity extends PermissionActivity {

    private final static String PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

    public static <T> void checkPermission(final Context context,
                                           @NonNull final BiConsumer<Context, T> successCallback,
                                           @Nullable final BiConsumer<Context, T> failureCallback,
                                           @Nullable final T data,
                                           final boolean ignoreOnNoPermission) {
        checkPermission(context, PERMISSION, ReadPermissionActivity.class, successCallback, failureCallback, data, ignoreOnNoPermission);
    }

    public static void checkPermission(final Context context, final Runnable callback, final boolean ignoreOnNoPermission) {
        checkPermission(context, PERMISSION, ReadPermissionActivity.class, callback, ignoreOnNoPermission);
    }

        @Override
    protected String getPermissionName() {
        return PERMISSION;
    }
}
