package com.ispd.mommybook.utils;

import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

public class UtilsPlaySound {
    private static final UtilsLogger LOGGER = new UtilsLogger();

    MediaPlayer mediaPlayer;

    public UtilsPlaySound() {
    }
    public void DoPlaySound(String fileName, boolean[] inRunning) {
        //mediaPlayer = MediaPlayer.create(context, soundData);
        if(mIsSoundPlaying == false) {
            mIsSoundPlaying = true;
        mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.reset();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    mIsSoundPlaying = false;
                }
            });
        try {
            mediaPlayer.setDataSource("/sdcard/studyNet/sound/"+fileName+".mp3");
            mediaPlayer.prepare();

            inRunning[0] = true;
        } catch (IOException e) {
            LOGGER.e("Read Sound Error...");
            inRunning[0] = false;
            e.printStackTrace();
        }

        mediaPlayer.start();
    }
    }
    public synchronized void stopSound()
    {
        if( mediaPlayer != null ) {
            mIsSoundPlaying = false;
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private boolean mIsSoundPlaying = false; 

    public synchronized boolean isSourdPlaying()
    {
        if( mediaPlayer != null ) {
            LOGGER.d("isSourdPlaying : " + mediaPlayer.isPlaying());
            return mediaPlayer.isPlaying();
        }
        else
        {
            LOGGER.d("isSourdPlaying-mediaPlayer : "+mediaPlayer);
        }

        return false;
    }
    public synchronized boolean isSoundPlaying() {
        return mIsSoundPlaying; //added by sally
    }
}
