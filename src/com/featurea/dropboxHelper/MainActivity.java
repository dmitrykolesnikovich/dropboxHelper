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
  private ImageView mark;
  RotateAnimation animRotate;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    mark = (ImageView) findViewById(R.id.mark);
    dropbox = new Dropbox(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dropbox.resume();
  }


  public void chooseDropboxFolder(View button) {
    if (mark.getBackground().getConstantState() == getResources().getDrawable(R.drawable.updated).getConstantState()) {
      updating();
    } else {
      updated();
    }
  }

  private void updated() {
    animRotate.cancel();
    mark.setBackgroundDrawable(getResources().getDrawable(R.drawable.updated));
  }

  private void updating() {
    Drawable updating = getResources().getDrawable(R.drawable.updating);
    mark.setBackgroundDrawable(updating);
    rotate(mark);
  }

  private void rotate(ImageView image) {
    AnimationSet animSet = new AnimationSet(true);
    animRotate = new RotateAnimation(0, 360,
        RotateAnimation.RELATIVE_TO_SELF, 0.5f,
        RotateAnimation.RELATIVE_TO_SELF, 0.5f);
    animRotate.setDuration(1000);
    animRotate.setFillAfter(true);
    animRotate.setRepeatCount(Animation.INFINITE);
    animSet.addAnimation(animRotate);
    image.startAnimation(animSet);
  }

  public void installDropbox(View button) {
    dropbox.install();
  }

}