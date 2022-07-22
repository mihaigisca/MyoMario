package com.local.miomario.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.local.miomario.R;
import com.local.miomario.common.BitmapMethods;

public class MyoMarioFlight {
    private final int xOffset = 32;
    private int x, y, width, height;
    private boolean isWingUp;
    private final Bitmap myoMarioFlight0, myoMarioFlight1;

    public MyoMarioFlight(int screenY, float screenRatioX, float screenRatioY, Context context) {
        this.x = (int)(xOffset * screenRatioX);
        this.y = (int)(0.85f * screenY);
        this.isWingUp = true;

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.flying_mario_0_cropped);
        this.width = drawable.getIntrinsicWidth();
        this.height = drawable.getIntrinsicHeight();

        this.width /= 3;
        this.height /= 3;

        this.width = (int)(this.width * screenRatioX);
        this.height = (int)(this.height * screenRatioY);

        this.myoMarioFlight0 = BitmapMethods.getBitmapFromDrawable(
                context,
                R.drawable.flying_mario_0_cropped,
                0,
                0,
                this.width,
                this.height
        );

        this.myoMarioFlight1 = BitmapMethods.getBitmapFromDrawable(
                context,
                R.drawable.flying_mario_1_cropped,
                0,
                0,
                this.width,
                this.height
        );
    }

    public Bitmap getMarioFlight() {
        Bitmap marioFlight;

        if (this.isWingUp) {
            this.isWingUp = false;
            marioFlight = this.myoMarioFlight0;
        } else {
            this.isWingUp = true;
            marioFlight = this.myoMarioFlight1;
        }

        return marioFlight;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Rect getCollisionShape() {
        return new Rect(this.x, this.y, this.x + this.width, this.y + this.height);
    }
}
