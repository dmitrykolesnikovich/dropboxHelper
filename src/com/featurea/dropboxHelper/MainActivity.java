package com.featurea.dropboxHelper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

  public static Dropbox dropbox;
  private Button loginButton;
  private TextView accountTextView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    loginButton = (Button) findViewById(R.id.login);
    accountTextView = (TextView) findViewById(R.id.accout);
    loginButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dropbox.updateAccountStatus();
        updateUI();
      }
    });
    dropbox = new Dropbox(this);

    Intent serviceIntent = new Intent(this, UpdaterService.class);
    startService(serviceIntent);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dropbox.resume();
    updateUI();
  }

  public void installDropbox(View button) {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Dropbox.ID)));
  }

  private void updateUI() {
    boolean isInstalled = DropboxHelperApp.isInstalled();
    findViewById(R.id.install).setVisibility(isInstalled ? View.GONE : View.VISIBLE);
    findViewById(R.id.workspace).setVisibility(isInstalled ? View.VISIBLE : View.GONE);
    loginButton.setText(dropbox.isLogin() ? "Detach" : "Attach");
    accountTextView.setVisibility(dropbox.isLogin() ? View.VISIBLE : View.GONE);
    if (dropbox.isLogin()) {
      accountTextView.setText(dropbox.getAccount());
    }
  }

}