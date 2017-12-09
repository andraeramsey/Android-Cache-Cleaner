package it391.fileclean;

import android.graphics.drawable.Drawable;

/**
 * This class is used for storing app data used in lists. Modify this class to store additional
 * information or to list different objects.
 */

public class ListApps {
    //variable declarations
    private long myCacheSize;
    private String myPackageName;
    private String myApplicationName;
    private Drawable myIcon;
    //constructor
    public ListApps(String packageName, String applicationName, Drawable icon, long cacheSize) {
        myCacheSize = cacheSize;
        myPackageName = packageName;
        myApplicationName = applicationName;
        myIcon = icon;
    }
    //all getter methods are below
    public Drawable getApplicationIcon() {
        return myIcon;
    }

    public String getApplicationName() {
        return myApplicationName;
    }

    public long getCacheSize() {
        return myCacheSize;
    }

    public String getPackageName() {
        return myPackageName;
    }
}
