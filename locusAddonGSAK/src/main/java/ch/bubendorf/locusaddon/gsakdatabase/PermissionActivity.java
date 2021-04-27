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
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import java.util.function.BiConsumer;

import ch.bubendorf.locusaddon.gsakdatabase.util.SimpleAlertDialog;

/**
 * Activity to ask the user for a permission.
 *
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class PermissionActivity extends ComponentActivity {

    private static final String TAG = "PermissionActivity";

    private final static String PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

    private static BiConsumer<Context, Object> biConsumer;
    private static Runnable runnable;
    private static Object data;

    public static <T> void checkPermission(final Context context, final BiConsumer<Context, T> callback, final T data, final boolean newTask) {
        Log.i(TAG, "checkPermission: BiConsumer, " + newTask);
        if (ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // Everything OK ==> Go On
            Log.i(TAG, "checkPermission: Everything OK ==> Go On");
            callback.accept(context, data);
        } else {
            //noinspection unchecked
            biConsumer = (BiConsumer<Context, Object>) callback;
            PermissionActivity.data = data;
            Intent intent = new Intent(context, PermissionActivity.class);
            if (newTask) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            Log.i(TAG, "checkPermission: Not granted ==> Start PermissionActivity");
            context.startActivity(intent);
        }
    }

    public static <T> void checkPermission(final Context context, final Runnable callback, final boolean newTask) {
        Log.i(TAG, "checkPermission: Runnable, " + newTask);
        if (ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // Everything OK ==> Go On
            Log.i(TAG, "checkPermission: Everything OK ==> Go On");
            callback.run();
        } else {
            //noinspection unchecked
            runnable = callback;
            Intent intent = new Intent(context, PermissionActivity.class);
            if (newTask) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            Log.i(TAG, "checkPermission: Not granted ==> Start PermissionActivity");
            context.startActivity(intent);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
    }

    private void checkPermission() {
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.
        Log.i(TAG, "checkPermission: Install Callback");
        final ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your app.
                        Log.i(TAG, "checkPermission: Everything OK ==> Go On");
                        goOn();
                    } else {
                        // Explain to the user that the feature is unavailable because the
                        // features requires a permission that the user has denied. At the
                        // same time, respect the user's decision. Don't link to system
                        // settings in an effort to convince the user to change their
                        // decision.
                        Log.i(TAG, "checkPermission: Show educational UI");
                        SimpleAlertDialog.show(this, R.string.permission_needed_title, R.string.permission_needed_text, true);
                        finish();
                        Log.i(TAG, "checkPermission: Go to app settings");
                        goToAppSettings(this);
                    }
                });

        if (ContextCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            Log.i(TAG, "checkPermission: Everything OK ==> Go On");
            goOn();
        } else if (shouldShowRequestPermissionRationale(PERMISSION)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            /*showAlert(this, "Erlaubnis", "Ohne Erlaubnis geht es nicht!");*/
            Log.i(TAG, "checkPermission: Show educational UI");
            SimpleAlertDialog.show(this, R.string.permission_needed_title, R.string.permission_needed_text, true);
            Log.i(TAG, "checkPermission: Ask for permission");
            requestPermissionLauncher.launch(PERMISSION);
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            Log.i(TAG, "checkPermission: Ask for permission");
            requestPermissionLauncher.launch(PERMISSION);
        }
    }

    /*private void showAlert(final Context context, final String title, final String text) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }*/

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
