package com.featurea.dropboxHelper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

  public static MainActivity instance;
  private Button loginButton;
  private Button pauseSyncingButton;
  private TextView accountTextView;
  private TextView directoryTextView;
  public TextView statusTextView;
  private TextView detailsTextView;

  public MainActivity() {
    instance = this;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    loginButton = (Button) findViewById(R.id.login);
    pauseSyncingButton = (Button) findViewById(R.id.pauseSyncing);
    accountTextView = (TextView) findViewById(R.id.account);
    directoryTextView = (TextView) findViewById(R.id.directory);
    statusTextView = (TextView) findViewById(R.id.status);
    detailsTextView = (TextView) findViewById(R.id.details);
    loginButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (DropboxHelperApp.instance != null) {
          DropboxHelperApp.instance.dropbox.updateAccountStatus();
        }
        updateUI();
      }
    });
    pauseSyncingButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (UpdaterService.instance.isRunning()) {
          UpdaterService.instance.stop();
        } else {
          UpdaterService.instance.start();
        }
        updateUI();
      }
    });

    Intent serviceIntent = new Intent(this, UpdaterService.class);
    startService(serviceIntent);

    updateUI();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (DropboxHelperApp.instance != null) {
      DropboxHelperApp.instance.dropbox.onResume();
    }
    updateUI();
  }

  public void installDropbox(View button) {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Dropbox.ID)));
  }

  public void updateUI() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        boolean isInstalled = DropboxHelperApp.isInstalled();
        findViewById(R.id.install).setVisibility(isInstalled ? View.GONE : View.VISIBLE);
        findViewById(R.id.workspace).setVisibility(isInstalled ? View.VISIBLE : View.GONE);
        if (DropboxHelperApp.instance != null) {
          loginButton.setText(DropboxHelperApp.instance.dropbox.isLogin() ? "Disconnect from Dropbox" : "Connect to Dropbox");
          if (!DropboxHelperApp.instance.dropbox.isLogin()) {
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText(R.string.attach);
          }
          accountTextView.setVisibility(DropboxHelperApp.instance.dropbox.isLogin() ? View.VISIBLE : View.GONE);
          directoryTextView.setVisibility(DropboxHelperApp.instance.dropbox.isLogin() ? View.VISIBLE : View.GONE);
          statusTextView.setVisibility(DropboxHelperApp.instance.dropbox.isLogin() ? View.VISIBLE : View.GONE);
          detailsTextView.setVisibility(DropboxHelperApp.instance.dropbox.isLogin() ? View.VISIBLE : View.GONE);
          if (DropboxHelperApp.instance.dropbox.isLogin()) {
            accountTextView.setText("Dropbox account: " + DropboxHelperApp.instance.dropbox.getAccount());
            directoryTextView.setText("Dropbox location: " + DropboxHelperApp.getRoot());
          }
          if (UpdaterService.instance != null) {
            if (UpdaterService.instance.isRunning()) {
              if (UpdaterService.instance.isUpdated) {
                String statusMessage = getResources().getString(R.string.syncingUpToDate);
                statusTextView.setText("Dropbox status: " + statusMessage);
              }
            } else {
              String statusMessage = getResources().getString(UpdaterService.instance.isUpdated ? R.string.syncingUpToDate : R.string.syncingPaused);
              statusTextView.setText("Dropbox syncing: " + statusMessage);
            }
            pauseSyncingButton.setVisibility(DropboxHelperApp.instance.dropbox.isLogin() ? View.VISIBLE : View.GONE);
            pauseSyncingButton.setText(UpdaterService.instance.isRunning() ? R.string.pauseSyncing : R.string.resumeSyncing);
          }
        }
      }
    });
  }

}
