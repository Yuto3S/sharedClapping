package com.google.location.nearby.apps.rockpaperscissors;

import android.media.MediaPlayer;

import androidx.appcompat.app.AppCompatActivity;

class Clap {
    MediaPlayer mp;

    Clap(AppCompatActivity activity){
        mp = MediaPlayer.create(activity, R.raw.clap);
    }
}
