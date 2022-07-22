package com.local.miomario.fragment;

import android.content.Context;
import android.graphics.Bitmap;

import com.local.miomario.R;
import com.local.miomario.common.BitmapMethods;

public class MyoSkyBackground {
    private int xSky, ySky, screenX;
    private Bitmap backgroundSky;

    public MyoSkyBackground(int screenX, int screenY, Context context) {
        this.xSky = this.ySky = 0;
        this.screenX = screenX;
        this.backgroundSky = BitmapMethods.getBitmapFromDrawable(
                context,
                R.drawable.mario_continuous_sky,
                0,
                0,
                this.screenX,
                screenY);
    }

    public int getSkyX() {
        return this.xSky;
    }

    public int getSkyY() {
        return this.ySky;
    }

    public void setSkyX(int value) {
        this.xSky = value;
    }

    public void addSkyX(int value) {
        this.xSky += value;
    }

    public void checkSkyX() {
        if ((this.xSky + this.backgroundSky.getWidth()) < 0) {
            this.xSky = this.screenX;
        }
    }

    public Bitmap getSkyBitmap() {
        return this.backgroundSky;
    }
}
