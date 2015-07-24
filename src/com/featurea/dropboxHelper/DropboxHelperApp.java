package com.featurea.dropboxHelper;

import android.app.Application;
import android.content.pm.PackageManager;

import java.io.File;

public class DropboxHelperApp extends Application {

  public static DropboxHelperApp instance;
  public Dropbox dropbox;

  public DropboxHelperApp() {
    instance = this;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    dropbox = new Dropbox();
  }

  public static boolean isInstalled() {
    try {
      instance.getPackageManager().getPackageInfo(Dropbox.ID, PackageManager.GET_ACTIVITIES);
      return true;
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
  }

  public static String getRoot() {
    String packageName = instance.getPackageName();
    File dir;
    if (isExternalStorageAvailable()) {
      dir = android.os.Environment.getExternalStorageDirectory();
    } else {
      dir = new File("/data/data/" + packageName);
    }
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return dir.getAbsolutePath() + "/Dropbox/";
  }

  private static boolean isExternalStorageAvailable() {
    boolean mExternalStorageAvailable;
    boolean mExternalStorageWriteable;
    String state = android.os.Environment.getExternalStorageState();
    if (android.os.Environment.MEDIA_MOUNTED.equals(state)) {
      // We can read and write the media
      mExternalStorageAvailable = mExternalStorageWriteable = true;
    } else if (android.os.Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
      // We can only read the media
      mExternalStorageAvailable = true;
      mExternalStorageWriteable = false;
    } else {
      // Something else is wrong. It may be one of many other states,
      // but all we need to know is we can neither read nor write
      mExternalStorageAvailable = mExternalStorageWriteable = false;
    }
    return mExternalStorageAvailable && mExternalStorageWriteable;
  }
}
