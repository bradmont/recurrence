package com.bleyl.recurrence.activities.shortcut;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.bleyl.recurrence.R;

public class ShortcutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            Intent launchIntent = new Intent(this, CreateEditShortcut.class);
            launchIntent.setAction(Intent.ACTION_VIEW);

            ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(this, "add_reminder")
                .setShortLabel(getString(R.string.add_reminder))
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(launchIntent)
                .build();

            ShortcutManagerCompat.requestPinShortcut(this, shortcutInfo, null);
        }

        finish();
    }
}
