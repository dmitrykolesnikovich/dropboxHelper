package com.featurea.dropboxHelper;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class MainActivity extends Activity {

  private Dropbox dropbox;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    dropbox = new Dropbox(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dropbox.resume();
  }

  public void installDropbox(View button) {
    dropbox.install();
  }


  public void chooseDropboxFolder(View button) {
    dropbox.update();
  }

}