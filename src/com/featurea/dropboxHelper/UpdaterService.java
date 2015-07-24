package com.featurea.dropboxHelper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class UpdaterService extends Service {

  private static int NOTIFICATION_ID = 10002;
  public static UpdaterService instance;
  private UpdaterThread updaterThread;

  public UpdaterService() {
    instance = this;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    start();
  }

  public static void updating(String message) {
    if (!DropboxHelperApp.instance.dropbox.isLogin()) {
      return;
    }
    if (instance != null) {
      NotificationManager notificationManager = (NotificationManager) instance.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
      Notification notification = new Notification(R.drawable.status_icon_updating, message, System.currentTimeMillis());
      PendingIntent contentIntent = PendingIntent.getActivity(instance, 0, new Intent(instance, MainActivity.class), 0);
      notification.setLatestEventInfo(instance, instance.getApplicationContext().getResources().getString(R.string.app_name) + " Synching...", message, contentIntent);
      notificationManager.notify(NOTIFICATION_ID, notification);
    }
  }

  public static void updated() {
    if (!DropboxHelperApp.instance.dropbox.isLogin()) {
      return;
    }
    if (instance != null) {
      NotificationManager notificationManager = (NotificationManager) instance.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
      CharSequence text = "All files are up to date";
      Notification notification = new Notification(R.drawable.status_icon_updated, text, System.currentTimeMillis());
      PendingIntent contentIntent = PendingIntent.getActivity(instance, 0, new Intent(instance, MainActivity.class), 0);
      notification.setLatestEventInfo(instance, instance.getApplicationContext().getResources().getString(R.string.app_name) + " Synched", text, contentIntent);
      notificationManager.notify(NOTIFICATION_ID, notification);
    }
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
    NotificationManager notificationManager = (NotificationManager) instance.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    notificationManager.cancel(NOTIFICATION_ID);
    if (updaterThread != null && updaterThread.isRunning) {
      updaterThread.isRunning = false;
    }
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
}
