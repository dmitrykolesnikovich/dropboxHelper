package com.featurea.dropboxHelper;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class DropboxAnimation {

  private final Activity myActivity;
  RotateAnimation animRotate;
  private final ImageView mark;
  private boolean isUpdated;

  public DropboxAnimation(Activity myActivity) {
    this.myActivity = myActivity;
    this.mark = (ImageView) myActivity.findViewById(R.id.mark);
  }

  public void updated() {
    if (!isUpdated) {
      isUpdated = true;
      if (animRotate != null) {
        animRotate.cancel();
      }
      mark.setBackgroundDrawable(myActivity.getResources().getDrawable(R.drawable.updated));
    }
  }

  public void updating() {
    if (isUpdated) {
      isUpdated = false;
      Drawable updating = myActivity.getResources().getDrawable(R.drawable.updating);
      mark.setBackgroundDrawable(updating);
      rotate(mark);
    }
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


}
