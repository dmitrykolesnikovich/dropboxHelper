package com.featurea.dropboxHelper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class UpdaterService extends Service {

  private static int NOTIFICATION = 10002;
  private static UpdaterService instance;

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
    new Thread(new Runnable() {
      public void run() {
        while (true) {
          if (MainActivity.dropbox != null) {
            MainActivity.dropbox.update();
          }
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }

  public static void updating() {
    if (instance == null) {
      return;
    }
    NotificationManager notificationManager = (NotificationManager) instance.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    CharSequence text = "text";
    Notification notification = new Notification(R.drawable.status_icon_updating, text, System.currentTimeMillis());
    PendingIntent contentIntent = PendingIntent.getActivity(instance, 0, new Intent(instance, MainActivity.class), 0);
    notification.setLatestEventInfo(instance, "local_service_label", text, contentIntent);
    notificationManager.notify(NOTIFICATION, notification);
  }

  public static void updated() {
    if (instance == null) {
      return;
    }
    NotificationManager notificationManager = (NotificationManager) instance.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    CharSequence text = "text";
    Notification notification = new Notification(R.drawable.status_icon_updated, text, System.currentTimeMillis());
    PendingIntent contentIntent = PendingIntent.getActivity(instance, 0, new Intent(instance, MainActivity.class), 0);
    notification.setLatestEventInfo(instance, "local_service_label", text, contentIntent);
    notificationManager.notify(NOTIFICATION, notification);
  }

}
