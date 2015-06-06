package com.featurea.dropboxHelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class MainActivity extends Activity {

  public static Dropbox dropbox;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    dropbox = new Dropbox(this);

    Intent serviceIntent = new Intent(this, UpdaterService.class);
    startService(serviceIntent);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dropbox.resume();
  }

  public void installDropbox(View button) {
    dropbox.install();
  }

}