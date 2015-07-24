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
        instance.builder.setContentText("Up to date").setSmallIcon(R.drawable.status_icon_updated);
        instance.makeNotification();
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
        final String contentText = "download \"" + file + "\"";
        instance.builder.setContentText("Syncing").setSmallIcon(R.drawable.status_icon_updating);
        instance.makeNotification();
        Log.d(DropboxHelperApp.TAG, "notificationManager.notify() updating");
        instance.isUpdated = false;
        MainActivity.instance.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            MainActivity.instance.statusTextView.setText("Dropbox syncing: " + contentText);
          }
        });
      }
    }
    MainActivity.instance.updateUI();
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  public static void paused() {
    if (DropboxHelperApp.instance.dropbox.isLogin()) {
      if (instance != null) {
        instance.builder.setContentText("Paused").setSmallIcon(R.drawable.status_icon_paused);
        instance.makeNotification();
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
    if (DropboxHelperApp.instance != null && !DropboxHelperApp.instance.dropbox.isLogin()) {
      NotificationManager notificationManager = (NotificationManager) instance.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
      notificationManager.cancel(NOTIFICATION_ID);
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

  public boolean isRunning() {
    if (updaterThread != null) {
      return updaterThread.isRunning;
    }
    return false;
  }

  /*private API*/

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private void makeNotification() {
    NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    Notification notification = builder.build();
    notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
    notificationManager.notify(NOTIFICATION_ID, notification);
  }

}
