package com.immersion.videoplayer.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.WindowManager;


/**
 * A wrapper class which can be used by any class in entire application to use
 * this application resources
 */
public class ResourceUtils {

    /**
     * Taking application context to get resources of the entire app
     */
    private static Resources getResources(Context context) {
        return context.getResources();
    }

    /**
     * Get the device screen height in pixels.
     *
     * @return screenHeight in PX.
     */
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        return displayMetrics.heightPixels;
    }

    /**
     * Get the device screen width in pixels.
     *
     * @return screenWidth in PX.
     */
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        return displayMetrics.widthPixels;
    }

    /**
     * Method to get string from app resource
     */
    public static String getString(Context context, int resId) {
        return getString(context, resId, null);
    }

    /**
     * Method to fetch string with replaceable content from app resources
     */
    public static String getString(Context context, int resId, String... args) {
        return getResources(context).getString(resId, args);
    }
}

