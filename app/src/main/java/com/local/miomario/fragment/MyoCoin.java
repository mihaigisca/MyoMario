package com.local.miomario.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.local.miomario.R;
import com.local.miomario.common.BitmapMethods;

public class MyoCoin {
    private final int baseSpeed = 24;
    public int Speed = 0;

    private enum CoinState {
        Coin0,
        Coin1,
        Coin2,
        Coin3,
    }
    private CoinState coinState;
    private int x, y, width, height;
    private final Bitmap myoCoin0, myoCoin1, myoCoin2, myoCoin3;

    public MyoCoin(int screenX, int screenY, float screenRatioX, float screenRatioY,
                   Context context) {
        this.Speed = (int)(this.baseSpeed * screenRatioX);
        this.x = screenX;
        this.y = screenY;
        this.coinState = CoinState.Coin0;

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.coin_0);
        this.width = drawable.getIntrinsicWidth();
        this.height = drawable.getIntrinsicHeight();

        this.width /= 3;
        this.height /= 3;

        this.width = (int)(this.width * screenRatioX);
        this.height = (int)(this.height * screenRatioY);

        this.myoCoin0 = BitmapMethods.getBitmapFromDrawable(
                context,
                R.drawable.coin_0,
                0,
                0,
                this.width,
                this.height
        );

        this.myoCoin1 = BitmapMethods.getBitmapFromDrawable(
                context,
                R.drawable.coin_1,
                0,
                0,
                this.width,
                this.height
        );

        this.myoCoin2 = BitmapMethods.getBitmapFromDrawable(
                context,
                R.drawable.coin_2,
                0,
                0,
                this.width,
                this.height
        );

        this.myoCoin3 = BitmapMethods.getBitmapFromDrawable(
                context,
                R.drawable.coin_3,
                0,
                0,
                this.width,
                this.height
        );
    }

    public Bitmap getCoin() {
        Bitmap coin;

        switch(this.coinState) {
            case Coin0:
                this.coinState = CoinState.Coin1;
                coin = this.myoCoin1;
                break;
            case Coin1:
                this.coinState = CoinState.Coin2;
                coin = this.myoCoin2;
                break;
            case Coin2:
                this.coinState = CoinState.Coin3;
                coin = this.myoCoin3;
                break;
            case Coin3:
            default:
                this.coinState = CoinState.Coin0;
                coin = this.myoCoin0;
                break;
        }

        return coin;
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

    public void setX(int x) {
        this.x = x;
    }

    public int getWidth() {
        return this.width;
    }

    public Rect getCollisionShape() {
        return new Rect(this.x, this.y, this.x + this.width, this.y + this.height);
    }
}
