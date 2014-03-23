package org.sergez.splayer.activity;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import com.actionbarsherlock.internal.widget.IcsToast;

/**
 * @author Sergii Zhuk
 *         Date: 13.10.13
 *         Time: 17:11
 */
public class AnimationMakeGoneListener implements Animation.AnimationListener {
  View mViewToHide;

  Context mContext;

  public AnimationMakeGoneListener(View v, Context c){
    mViewToHide =v;
    mContext=c;
  }

  public void onAnimationEnd(Animation animation) {
    mViewToHide.setVisibility(View.GONE);
  }

  public void onAnimationRepeat(Animation animation) {}

  public void onAnimationStart(Animation animation) {}

}