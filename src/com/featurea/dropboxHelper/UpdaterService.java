package com.featurea.dropboxHelper;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class UpdaterService extends Service {

  private static int NOTIFICATION_ID = 10002;
  public static UpdaterService instance;
  private UpdaterThread updaterThread;
  public boolean isUpdated = true;
  public Notification.Builder builder;
  private boolean isFirstTime = true;

  public UpdaterService() {
    instance = this;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  @Override
  public void onCreate() {
    super.onCreate();
    start();
    builder = new Notification.Builder(instance.getApplicationContext());
    PendingIntent contentIntent = PendingIntent.getActivity(instance, 0, new Intent(instance, MainActivity.class), 0);
    builder.setSmallIcon(R.drawable.status_icon_updating).
        setOnlyAlertOnce(true).setContentTitle("Dropbox").setContentIntent(contentIntent);
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  public static void updated() {
    if (DropboxHelperApp.instance.dropbox.isLogin()) {
      if (instance != null) {
        NotificationManager notificationManager = (NotificationManager) instance.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        instance.builder.setContentText(instance.getResources().getString(R.string.upToDate)).setSmallIcon(R.drawable.status_icon_updated);
        notificationManager.notify(NOTIFICATION_ID, instance.builder.build());
        Log.d(DropboxHelperApp.TAG, "notificationManager.notify() updated");
        instance.isUpdated = true;
      }
    }
    MainActivity.instance.updateUI();
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  public static void updating(String file) {
    if (DropboxHelperApp.instance.dropbox.isLogin()) {
      if (instance != null) {
        NotificationManager notificationManager = (NotificationManager) instance.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        String contentText = instance.getResources().getString(R.string.syncing) + " \"" + file + "\"";
        instance.builder.setContentText(contentText).setSmallIcon(R.drawable.status_icon_updating);
        notificationManager.notify(NOTIFICATION_ID, instance.builder.build());
        Log.d(DropboxHelperApp.TAG, "notificationManager.notify() updating");
        instance.isUpdated = false;
      }
    }
    MainActivity.instance.updateUI();
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  public static void paused() {
    if (DropboxHelperApp.instance.dropbox.isLogin()) {
      if (instance != null) {
        NotificationManager notificationManager = (NotificationManager) instance.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        instance.builder.setContentText(instance.getResources().getString(R.string.syncingPaused)).setSmallIcon(R.drawable.status_icon_paused);
        notificationManager.notify(NOTIFICATION_ID, instance.builder.build());
        Log.d(DropboxHelperApp.TAG, "notificationManager.notify() paused");
        instance.isUpdated = true;
      }
    }
    MainActivity.instance.updateUI();
  }


  public void start() {
    if (DropboxHelperApp.instance.dropbox.isLogin()) {
      if (updaterThread == null || !updaterThread.isRunning) {
        updaterThread = new UpdaterThread();
        updaterThread.start();
      }
    }
  }

  public void stop() {
    if (updaterThread != null && updaterThread.isRunning) {
      updaterThread.isRunning = false;
    }
    paused();
  }

  private class UpdaterThread extends Thread {

    private static final int TIME_OUT = 2000;
    public boolean isRunning = true;

    @Override
    public void run() {
      while (isRunning) {
        if (DropboxHelperApp.instance.dropbox != null) {
          DropboxHelperApp.instance.dropbox.update();
        }
        try {
          Thread.sleep(TIME_OUT);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

  }

  public boolean isRunning() {
    if (updaterThread != null) {
      return updaterThread.isRunning;
    }
    return false;
  }
}
