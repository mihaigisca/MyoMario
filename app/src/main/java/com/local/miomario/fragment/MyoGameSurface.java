package com.local.miomario.fragment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceView;

import java.util.Random;

public class MyoGameSurface extends SurfaceView implements Runnable {
    private final int SkyStep = -24;
    private final int GroundStep = -16;
    private final int FramesPerSecond = 30;
    private final int SleepPeriod = 1000 / this.FramesPerSecond;
    private final int MaxCoinsPerScreen = 10;
    private Thread thread;
    private boolean isPlaying;
    private Paint paint;
    private MyoSkyBackground myoSkyBg1, myoSkyBg2;
    private MyoGroundBackground myoGroundBg1, myoGroundBg2;
    private MyoMarioFlight myoMarioFlight;
    private MyoCoin[] coins;
    private int screenX, screenY;
    private float screenRatioX, screenRatioY;
    private Random random;

    public MyoGameSurface(Context context, int screenX, int screenY) {
        super(context);
        this.screenX = screenX;
        this.screenY = screenY;
        this.screenRatioX = 1920f / this.screenX;
        this.screenRatioY = 1080f / this.screenY;
        this.myoSkyBg1 = new MyoSkyBackground(this.screenX, this.screenY, context);
        this.myoGroundBg1 = new MyoGroundBackground(this.screenX, this.screenY, context);
        this.myoSkyBg2 = new MyoSkyBackground(this.screenX, this.screenY, context);
        this.myoGroundBg2 = new MyoGroundBackground(this.screenX, this.screenY, context);
        this.myoSkyBg2.setSkyX(this.screenX);
        this.myoGroundBg2.setGroundX(this.screenX);
        this.myoMarioFlight = new MyoMarioFlight(this.screenY, this.screenRatioX,
                this.screenRatioY, context);
        this.paint = new Paint();
        this.coins = new MyoCoin[this.MaxCoinsPerScreen];
        this.random = new Random();

        for (byte coinIndex = 0; coinIndex < this.MaxCoinsPerScreen; coinIndex++) {
            int y = (this.random.nextInt(51) + 25) * this.screenY / 100;
            MyoCoin coin = new MyoCoin(this.screenX, y, this.screenRatioX, this.screenRatioY,
                    context);
            this.coins[coinIndex] = coin;
        }
    }

    public MyoGameSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void run() {
        while (this.isPlaying) {
            this.update();
            this.draw();
            this.sleep();
        }
    }

    public void update() {
        this.myoSkyBg1.addSkyX((int)(this.SkyStep * this.screenRatioX));
        this.myoGroundBg1.addGroundX((int)(this.GroundStep * this.screenRatioX));
        this.myoSkyBg1.checkSkyX();
        this.myoGroundBg1.checkGroundX();

        this.myoSkyBg2.addSkyX((int)(this.SkyStep * this.screenRatioX));
        this.myoGroundBg2.addGroundX((int)(this.GroundStep * this.screenRatioX));
        this.myoSkyBg2.checkSkyX();
        this.myoGroundBg2.checkGroundX();

        for (MyoCoin coin : this.coins) {
            coin.setX(coin.getX() - coin.Speed);

            if (coin.getX() + coin.getWidth() < 0) {
                coin.setX(this.screenX);
                coin.setY((this.random.nextInt(51) + 25) * this.screenY / 100);
            }

            if (Rect.intersects(coin.getCollisionShape(), this.myoMarioFlight.getCollisionShape())) {
                coin.setX(this.screenX);
                coin.setY((this.random.nextInt(51) + 25) * this.screenY / 100);
            }
        }
    }

    public void draw() {
        if(getHolder().getSurface().isValid()) {
            Canvas canvas = getHolder().lockCanvas();

            canvas.drawBitmap(this.myoSkyBg1.getSkyBitmap(), this.myoSkyBg1.getSkyX(),
                    this.myoSkyBg1.getSkyY(), this.paint);
            canvas.drawBitmap(this.myoSkyBg2.getSkyBitmap(), this.myoSkyBg2.getSkyX(),
                    this.myoSkyBg2.getSkyY(), this.paint);

            canvas.drawBitmap(this.myoGroundBg1.getGroundBitmap(), this.myoGroundBg1.getGroundX(),
                    this.myoGroundBg1.getGroundY(), this.paint);
            canvas.drawBitmap(this.myoGroundBg2.getGroundBitmap(), this.myoGroundBg2.getGroundX(),
                    this.myoGroundBg2.getGroundY(), this.paint);

            canvas.drawBitmap(this.myoMarioFlight.getMarioFlight(), this.myoMarioFlight.getX(),
                    this.myoMarioFlight.getY(), this.paint);

            for (MyoCoin coin : this.coins) {
                canvas.drawBitmap(coin.getCoin(), coin.getX(), coin.getY(), this.paint);
            }

            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    public void sleep() {
        try {
            Thread.sleep(this.SleepPeriod);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        this.isPlaying = true;
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void pause() {
        try {
            this.isPlaying = false;
            this.thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void updateMarioY(int y) {
        this.myoMarioFlight.setY(y);
    }
}
