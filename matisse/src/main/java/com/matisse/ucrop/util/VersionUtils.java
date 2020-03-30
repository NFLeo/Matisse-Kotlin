package com.matisse.ucrop.util;

import android.os.Build;

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 9/8/16.
 */
public class VersionUtils {

    private VersionUtils() {

    }

    public static boolean isAndroidQ() {
        return Build.VERSION.SDK_INT >= 29;
    }

}
