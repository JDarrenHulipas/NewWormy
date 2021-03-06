package com.stucom.flx.VCTR_DRRN.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.stucom.flx.VCTR_DRRN.MainActivity;
import com.stucom.flx.VCTR_DRRN.NewPlayActivity;
import com.stucom.flx.VCTR_DRRN.R;
import com.stucom.flx.VCTR_DRRN.model.MyVolley;
import com.stucom.flx.VCTR_DRRN.model.Prefs;

import java.util.HashMap;
import java.util.Map;

public class WormyView extends View implements SensorEventListener {

    public static final int SLOW_DOWN = 5;
    public static int TILE_SIZE = 48;
    private int nCols, nRows, top, left, bottom, right;
    private int wormX, wormY, score = 0, numCoins = 0, slowdown;
    private boolean playing = false;
    private char map[];
    private Paint paint;
    private MediaPlayer soundEffects;
    private Bitmap tiles, wormLeft, wormRight, worm;
    private float ax, ay;
    private Bitmap newGameImage;
    private Bitmap yes;
    private Bitmap no;
    private boolean gameLost = false;
    private float newGameLocationY;
    private float newGameLocationX;
    public WormyView(Context context) { this(context, null, 0); }
    public WormyView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public WormyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3.0f);
        tiles = BitmapFactory.decodeResource(getResources(), R.drawable.tiles);
        wormLeft = BitmapFactory.decodeResource(getResources(), R.drawable.worm_left);
        wormRight = BitmapFactory.decodeResource(getResources(), R.drawable.worm_right);
        newGameImage=BitmapFactory.decodeResource(getResources(), R.drawable.newgame);
        yes=BitmapFactory.decodeResource(getResources(), R.drawable.yes);
        no=BitmapFactory.decodeResource(getResources(), R.drawable.no);


        worm = wormLeft;
        gameLost=true;
    }



    @Override
    public void onMeasure(int specW, int specH) {
        int w = MeasureSpec.getSize(specW);
        int h = MeasureSpec.getSize(specH);
        if ((w != 0) && (h != 0)) {
            TILE_SIZE = 64;
            if ((w < 640) || (h < 640)) TILE_SIZE = 48;
            if ((w < 480) || (h < 480)) TILE_SIZE = 32;
            if ((w < 320) || (h < 320)) TILE_SIZE = 16;
            nCols = w / TILE_SIZE;
            nRows = h / TILE_SIZE;
            left = ( w - nCols * TILE_SIZE) / 2;
            top = (h - nRows * TILE_SIZE) / 2;
            right = left + nCols * TILE_SIZE;
            bottom = top + nRows * TILE_SIZE;
            if (!playing) resetMap(false);
        }
        setMeasuredDimension(w, h);
    }

    public void newGame() {
        slowdown = SLOW_DOWN;
        score = 0;
        playing = true;
        gameLost=false;
        resetMap(true);
    }

    public void resetMap(boolean full) {
        if ((nCols <= 0) || (nRows <= 0)) return;
        wormX = nCols / 2;
        wormY = nRows / 2;
        int size = nCols * nRows;
        map = new char[size];
        // Empty the board
        for (int i = 0; i < size; i++) map[i] = ' ';
        // Border around
        for (int i = 0; i < nCols; i++) {
            map[i] = 'X';
            map[(nRows - 1) * nCols + i] = 'X';
        }
        for (int i = 0; i < nRows; i++) {
            map[i * nCols] = 'X';
            map[(i + 1) * nCols - 1] = 'X';
        }
        if (full) {
            // Protect worm position
            map[wormY * nCols + wormX] = 'W';
            // Random plants
            int placed = 0;
            while (placed < size / 20) {
                int i = (int) (Math.random() * nCols);
                int j = (int) (Math.random() * nRows);
                int idx = j * nCols + i;
                if (map[idx] == ' ') {
                    map[idx] = (char) ('P' + (int) (Math.random() * 4));
                    placed++;
                }
            }
            // Random coins
            placed = 0;
            numCoins = size / 50;
            while (placed < numCoins) {
                int i = (int) (Math.random() * nCols);
                int j = (int) (Math.random() * nRows);
                int idx = j * nCols + i;
                if (map[idx] == ' ') {
                    map[idx] = (char) ('C' + (int) (Math.random() * 5));
                    placed++;
                }
            }
            // Remove worm lock
            map[wormY * nCols + wormX] = ' ';
        }
        this.invalidate();
    }

    private Rect src = new Rect(0, 0, 0, 0);
    private Rect dst = new Rect(0, 0, 0, 0);
    public void drawTile(Canvas canvas, int i, int x, int y) {
        int cols = tiles.getWidth() / 16;
        int row = i / cols;
        int col = i % cols;
        src.left = col * 16;
        src.top = row * 16;
        src.right = src.left + 16;
        src.bottom = src.top + 16;
        dst.left = x;
        dst.top = y;
        dst.right = dst.left + TILE_SIZE;
        dst.bottom = dst.top + TILE_SIZE;
        canvas.drawBitmap(tiles, src, dst, paint);
    }
    public void drawWorm(Canvas canvas, int  x, int y) {
        src.left = 0;
        src.top = 0;
        src.right = 32;
        src.bottom = 32;
        dst.left = x;
        dst.top = y;
        dst.right = dst.left + TILE_SIZE;
        dst.bottom = dst.top + TILE_SIZE;
        canvas.drawBitmap(worm, src, dst, paint);
    }

    @Override
    public void onDraw(Canvas canvas) {

        float width = getWidth();
        float height = getHeight();
       // tvScoreGame.setText(0);
        float scoreLocationY=height/8;
        float scoreLocationX= width/8;
        float newGameLocationY=height/2;
        float newGameLocationX= width/2;


        //float movementX = -ax * width / 25;
        //float movementY = ay * height / 25;

        canvas.drawColor(Color.WHITE);
        if (map == null) return;
        int idx = 0, x, y = top;
        for (int i = 0; i < nRows; i++) {
            x = left;
            for (int j = 0; j < nCols; j++) {
                int s = 3;
                char m = map[idx];
                switch (m) {
                    case 'X': s = 28; break;
                    case 'C': s = 16; map[idx] = 'D'; break;
                    case 'D': s = 17; map[idx] = 'E'; break;
                    case 'E': s = 18; map[idx] = 'F'; break;
                    case 'F': s = 19; map[idx] = 'G'; break;
                    case 'G': s = 23; map[idx] = 'C'; break;
                    case 'P': s = 2; break;
                    case 'Q': s = 21; break;
                    case 'R': s = 25; break;
                    case 'S': s = 29; break; }
                idx++;
                drawTile(canvas, s, x, y);
                x += TILE_SIZE;
            }
            y += TILE_SIZE;
        }
       //
        //drawWorm(canvas, left + movementX * TILE_SIZE, top + movementY * TILE_SIZE);

        drawWorm(canvas, left + wormX * TILE_SIZE, top + wormY * TILE_SIZE);
        canvas.drawRect(left, top, right, bottom, paint);
        paint.setTextSize(50f);
        canvas.drawText("SCORE: "+ String.valueOf(score),scoreLocationX,scoreLocationY,paint);


        if (gameLost) {
            canvas.drawBitmap(newGameImage, newGameLocationX - 500, newGameLocationY - 750, paint);
            //canvas.drawBitmap(yes ,newGameLocationX-500,newGameLocationY+100,paint);
            //canvas.drawBitmap(no,newGameLocationX,newGameLocationY+100,paint);
        }


        // canvas.drawBitmap(restartGame,scoreLocationX,scoreLocationY,paint);
        // gameLost=true;
    }

    private int counter = 0;
    public void update(float accelerationX, float accelerationY) {

        if (!playing) return;
        if (++counter == slowdown) {
            counter = 0;
            int newX = wormX, newY = wormY;
            if (accelerationX < -2) { newX--; worm = wormLeft; }
            if (accelerationX > +2) { newX++; worm = wormRight; }
            if (accelerationY < -2) newY--;
            if (accelerationY > +2) newY++;
            int idx = newY * nCols + newX;
            if (map[idx] != 'X') {
                wormX = newX;
                wormY = newY;
                if ((map[idx] >= 'C') && (map[idx] <= 'G')) {
                    // Coin collected


                    soundEffects=MediaPlayer.create(getContext(),R.raw.mariocoin);
                    soundEffects.start();

                    score += 10;
                    map[idx] = ' ';
                    numCoins--;
                    if (numCoins == 0) {
                        slowdown--;
                        if (slowdown < 1) slowdown = 1;
                        resetMap(true);
                    }
                    if (listener != null) listener.scoreUpdated(this, score);
                }
                else if ((map[idx] >= 'P') && (map[idx] <= 'S')) {
                    // Plant touched!

                    soundEffects=MediaPlayer.create(getContext(),R.raw.deathsound);
                    soundEffects.start();

                    playing = false;
                    if (listener != null) listener.gameLostWormy(this);
                    gameLost= true;
                    updateScore();
                }
            }
        }
        this.invalidate();
    }

    public boolean onTouchEvent(MotionEvent event){
        float x = event.getX(); // or getRawX();
        float y = event.getY();
        int action = event.getAction();
        switch(action){
            case MotionEvent.ACTION_DOWN:
                if (gameLost){
                if (x >= newGameLocationX-500 && x < (newGameLocationX + newGameImage.getWidth())
                        && y >= newGameLocationY-500 && y < (newGameLocationY + newGameImage.getHeight())) {
                    newGame();
                    return true;
                }}
                break;
        }return false;}

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        update(-ax,ay);
    }
    @Override public void onSensorChanged(SensorEvent sensorEvent) {
        // Read the sensor's information
        this.ax = sensorEvent.values[0];
        this.ay = sensorEvent.values[1];
        update(-ax*3,ay*3);

        // Redraw the view
        this.invalidate();
    }
    public interface WormyListener {
        void scoreUpdated(View view, int score);
        void gameLostWormy(View view);
    }

    private WormyListener listener;
    public void setWormyListener(WormyListener listener) {
        this.listener = listener;
    }
    public void updateScore() {
        String URL = "https://api.flx.cat/dam2game/user/score?token=" + Prefs.getInstance(getContext()).getToken();
        StringRequest request = new StringRequest
                (Request.Method.POST, URL,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {

                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String message = error.toString();
                        NetworkResponse response = error.networkResponse;
                        if (response != null) {
                            Context context = getContext();
                            CharSequence text = response.statusCode + " " + message;
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("level", String.valueOf(0));
                params.put("score", String.valueOf(score));
                return params;
            }
        };
        MyVolley.getInstance(getContext()).add(request);
    }


}
