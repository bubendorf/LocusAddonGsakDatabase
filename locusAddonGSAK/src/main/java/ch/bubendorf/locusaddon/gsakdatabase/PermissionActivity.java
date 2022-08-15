/*
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * Version 2, December 2004
 *
 * Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 * 0. You just DO WHAT THE FUCK YOU WANT TO.
 */

package ch.bubendorf.locusaddon.gsakdatabase;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.function.BiConsumer;

import ch.bubendorf.locusaddon.gsakdatabase.util.SimpleAlertDialog;

/**
 * Activity to ask the user for a permission.
 *
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public abstract class PermissionActivity extends ComponentActivity {

    //private static final String TAG = "PermissionActivity";



    private static BiConsumer<Context, Object> biConsumer;
    private static Runnable runnable;
    private static Object data;

    protected static <T> void checkPermission(final Context context,
                                           @NonNull final String permissionName,
                                          @NonNull final Class activityClass,
                                           @NonNull final BiConsumer<Context, T> successCallback,
                                           @Nullable final BiConsumer<Context, T> failureCallback,
                                           @Nullable final T data,
                                           final boolean ignoreOnNoPermission) {
        //Log.d(TAG, "checkPermission: BiConsumer, " + ignoreOnNoPermission);
        if (ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED) {
            // Everything OK ==> Go On
            //Log.d(TAG, "checkPermission: BiConsumer: Everything OK ==> Go On");
            successCallback.accept(context, data);
        } else if (!ignoreOnNoPermission) {
            //noinspection unchecked
            biConsumer = (BiConsumer<Context, Object>) successCallback;
            PermissionActivity.data = data;
            final Intent intent = new Intent(context, activityClass);
            //Log.d(TAG, "checkPermission: BiConsumer: Not granted ==> Start PermissionActivity");
            context.startActivity(intent);
        } else {
            //Log.d(TAG, "checkPermission: BiConsumer: Do nothing");
            if (failureCallback != null) {
                failureCallback.accept(context, data);
            }
        }
    }

    protected static void checkPermission(final Context context,
                                          @NonNull final String permissionName,
                                          @NonNull final Class activityClass,
                                          @NonNull final Runnable callback,
                                          final boolean ignoreOnNoPermission) {
        //Log.d(TAG, "checkPermission: Runnable, " + ignoreOnNoPermission);
        if (ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED) {
            // Everything OK ==> Go On
            //Log.d(TAG, "checkPermission: Runnable: Everything OK ==> Go On");
            callback.run();
        } else if (!ignoreOnNoPermission) {
            runnable = callback;
            final Intent intent = new Intent(context, activityClass);
            //Log.d(TAG, "checkPermission: Runnable: Not granted ==> Start PermissionActivity");
            context.startActivity(intent);
        }
    }

    protected abstract String getPermissionName();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        checkPermission();
    }

    private void checkPermission() {
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.
        //Log.d(TAG, "checkPermission: Install Callback");
        final ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your app.
                        //Log.d(TAG, "checkPermission: Everything OK ==> Go On");
                        goOn();
                    } else {
                        // Explain to the user that the feature is unavailable because the
                        // features requires a permission that the user has denied. At the
                        // same time, respect the user's decision. Don't link to system
                        // settings in an effort to convince the user to change their
                        // decision.
                        //Log.d(TAG, "checkPermission: Show educational UI");
                        SimpleAlertDialog.show(this, R.string.permission_needed_title,
                                R.string.permission_needed_text,
                                () ->{
                                    //Log.d(TAG, "checkPermission: Go to app settings");
                                    goToAppSettings(this);
                                    finish();
                                }, null);
                    }
                });

        if (ContextCompat.checkSelfPermission(this, getPermissionName()) == PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            //Log.d(TAG, "checkPermission: Everything OK ==> Go On");
            goOn();
        } else if (shouldShowRequestPermissionRationale(getPermissionName())) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            //Log.d(TAG, "checkPermission: Show educational UI");
            SimpleAlertDialog.show(this, R.string.permission_needed_title,
                    R.string.permission_needed_text, () -> {
                        //Log.d(TAG, "checkPermission: Ask for permission");
                        requestPermissionLauncher.launch(getPermissionName());
                    }, null);
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            //Log.d(TAG, "checkPermission: Ask for permission");
            requestPermissionLauncher.launch(getPermissionName());
        }
    }

    private void goToAppSettings(final Context context) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        final Uri uri = Uri.fromParts("package", GsakApplication.PACKAGE_NAME, null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    private void goOn() {
        finish();
        if (biConsumer != null) {
            biConsumer.accept(getParent(), data);
            biConsumer = null;
        }
        if (runnable != null) {
            runnable.run();
            runnable = null;
        }
    }

}
