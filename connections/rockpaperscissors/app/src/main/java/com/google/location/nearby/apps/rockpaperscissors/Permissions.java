package com.google.location.nearby.apps.rockpaperscissors;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

class Permissions {

    /** Returns true if the app was granted all the permissions. Otherwise, returns false. */
    static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
