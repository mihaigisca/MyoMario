package com.local.miomario.fragment;

import android.content.Context;
import android.graphics.Bitmap;

import com.local.miomario.R;
import com.local.miomario.common.BitmapMethods;

public class MyoGroundBackground {
    private int xGround, yGround, screenX;
    private Bitmap backgroundGround;

    public MyoGroundBackground(int screenX, int screenY, Context context) {
        this.xGround = this.yGround = 0;
        this.screenX = screenX;

        this.backgroundGround = BitmapMethods.getBitmapFromDrawable(
                context,
                R.drawable.mario_ground,
                0,
                (int)(screenY * 0.85),
                this.screenX,
                screenY);
    }

    public int getGroundX() {
        return this.xGround;
    }

    public int getGroundY() {
        return this.yGround;
    }

    public void setGroundX(int value) {
        this.xGround = value;
    }

    public void addGroundX(int value) {
        this.xGround += value;
    }

    public void checkGroundX() {
        if ((this.xGround + this.backgroundGround.getWidth()) <= 0) {
            this.xGround = this.screenX;
        }
    }

    public Bitmap getGroundBitmap() {
        return this.backgroundGround;
    }
}
