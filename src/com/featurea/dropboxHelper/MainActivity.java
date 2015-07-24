package com.featurea.dropboxHelper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

  private Button loginButton;
  private TextView accountTextView;
  private TextView directoryTextView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    loginButton = (Button) findViewById(R.id.login);
    accountTextView = (TextView) findViewById(R.id.account);
    directoryTextView = (TextView) findViewById(R.id.directory);
    loginButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (DropboxHelperApp.instance != null) {
          DropboxHelperApp.instance.dropbox.updateAccountStatus();
        }
        updateUI();
      }
    });

    Intent serviceIntent = new Intent(this, UpdaterService.class);
    startService(serviceIntent);
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

  private void updateUI() {
    boolean isInstalled = DropboxHelperApp.isInstalled();
    findViewById(R.id.install).setVisibility(isInstalled ? View.GONE : View.VISIBLE);
    findViewById(R.id.workspace).setVisibility(isInstalled ? View.VISIBLE : View.GONE);
    if (DropboxHelperApp.instance != null) {
      loginButton.setText(DropboxHelperApp.instance.dropbox.isLogin() ? "Detach Dropbox account" : "Attach Dropbox account");
      accountTextView.setVisibility(DropboxHelperApp.instance.dropbox.isLogin() ? View.VISIBLE : View.GONE);
      directoryTextView.setVisibility(DropboxHelperApp.instance.dropbox.isLogin() ? View.VISIBLE : View.GONE);
      if (DropboxHelperApp.instance.dropbox.isLogin()) {
        accountTextView.setText("Dropbox account: " + DropboxHelperApp.instance.dropbox.getAccount());
        directoryTextView.setText("Dropbox location: " + DropboxHelperApp.getRoot());
      }
      if (UpdaterService.instance != null) {
        if (DropboxHelperApp.instance.dropbox.isLogin()) {
          UpdaterService.instance.start();
        } else {
          UpdaterService.instance.stop();
        }
      }
    }
  }

}