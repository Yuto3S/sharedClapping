package com.google.location.nearby.apps.rockpaperscissors;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicBoolean;

class Clap {
    MediaPlayer mp;
    int duration;
    ProgressBar progressBar;

    Clap(AppCompatActivity activity, ProgressBar progressBar){
        mp = MediaPlayer.create(activity, R.raw.clapv2);
        duration = mp.getDuration();
        this.progressBar = progressBar;
    }

    void play(){
        progressBar.setVisibility(View.VISIBLE);
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                progressBar.setProgress(percent);
                Log.i("LOG", "percent: " + String.valueOf(percent));
            }
        });
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                progressBar.setSecondaryProgress(percent);
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.i("LOG", "complete");
                observer.stop();
                progressBar.setVisibility(View.INVISIBLE);
                progressBar.setProgress(0);
                mp.stop();
            }
        });
        observer = new MediaObserver();
        new Thread(observer).start();
        mp.start();
    }

    private MediaObserver observer = null;

    private class MediaObserver implements Runnable {
        private AtomicBoolean stop = new AtomicBoolean(false);

        public void stop() {
            stop.set(true);
        }

        @Override
        public void run() {
            while (!stop.get()) {
                progressBar.setProgress((int)((double)mp.getCurrentPosition() / (double)mp.getDuration()*100));
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                }

            }
        }
    }
}
