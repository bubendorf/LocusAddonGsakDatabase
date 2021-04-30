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

package ch.bubendorf.locusaddon.gsakdatabase.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;

import ch.bubendorf.locusaddon.gsakdatabase.R;

/**
 * A simple Alert Dialog with a title, an icon , a text and one or two buttons.
 */
public class SimpleAlertDialog {
    public static void show(final Context context, final int titleId, final int textId, final Runnable okCallback, final Runnable cancelCallback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        final Resources resources = context.getResources();
        builder.setTitle(resources.getText(titleId));
        builder.setMessage(resources.getText(textId));
        builder.setIcon(R.mipmap.ic_launcher);

        if (okCallback != null) {
            builder.setPositiveButton(resources.getText(android.R.string.ok), (dialog, which) -> dialog.dismiss());
            builder.setOnDismissListener(dlg -> okCallback.run());
        }
        if (cancelCallback != null) {
            builder.setNegativeButton(resources.getText(android.R.string.cancel), (dialog, which) -> dialog.cancel());
            builder.setOnCancelListener(dlg -> cancelCallback.run());
        }
        builder.show();
    }
}
