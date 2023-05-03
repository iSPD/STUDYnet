package com.ispd.mommybook.activities;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.VideoView;

import com.ispd.mommybook.R;
import com.ispd.mommybook.utils.UtilsLogger;

import java.util.Set;

import static com.ispd.mommybook.MainHandlerMessages.COVER_GUIDE_ON;

public class ActivitiesTouch {

    private static final UtilsLogger LOGGER = new UtilsLogger();

    public static final int REMOVE_VIEW = 1000;

    Context mContext;
    ImageButton mTouchButton = null;
    private VideoView mVideoVIew;

    private boolean mTouchListenerStarted = false;

    public ActivitiesTouch(Context context) {
        mContext = context;

        mVideoVIew = ((Activity)mContext).findViewById(R.id.vv_playview);
        mVideoVIew.setVisibility(View.INVISIBLE);

        mTouchButton = ((Activity)mContext).findViewById(R.id.ibtn_touch);
        mTouchButton.setVisibility(View.VISIBLE);
        //mTouchButton.setAlpha(0.5f);

        mTouchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LOGGER.d("mTouchListenerStarted : "+mTouchListenerStarted);
                if( mTouchListenerStarted == true ) {
                    return;
                }

                mTouchListenerStarted = true;

                //mImageButton6.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake_anim));
                Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.scale);
                v.startAnimation(anim);

                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                        LOGGER.d("mVideoVIew isPlaying()-0 : "+mVideoVIew.isPlaying());
                        mVideoVIew.setVisibility(View.VISIBLE);

                        if( mVideoVIew.isPlaying() == false ) {

                            Uri videoUri = Uri.parse("android.resource://"+mContext.getPackageName() + "/" + R.raw.minsok);
                            mVideoVIew.setMediaController(new MediaController(mContext));
                            mVideoVIew.setVideoURI(videoUri);

                            mVideoVIew.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    LOGGER.d("mVideoVIew isPlaying()-1 : "+mVideoVIew.isPlaying());

                                    mTouchButton.setVisibility(View.GONE);
                                    //mVideoVIew.setVisibility(View.VISIBLE);
                                    mVideoVIew.start();
                                }
                            });
                        }
                        else
                        {
                            LOGGER.d("mVideoVIew isPlaying()-2 : "+mVideoVIew.isPlaying());

                            mVideoVIew.stopPlayback();
                            mVideoVIew.setVisibility(View.GONE);
                        }

                        LOGGER.d("mVideoVIew isPlaying()-3 : "+mVideoVIew.isPlaying());

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while( true )
                                {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }

                                    if( mVideoVIew.isPlaying() == false )
                                    {
                                        LOGGER.d("mVideoVIew isPlaying()-4 : "+mVideoVIew.isPlaying());
                                        //mUiHandler.sendEmptyMessage(10);
                                        mUiHandler.sendEmptyMessage(REMOVE_VIEW);

                                        mTouchListenerStarted = false;

                                        break;
                                    }
                                }
                            }
                        });
                        thread.start();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        });
    }

    public void SetTouchButton() {
        if( mTouchButton != null ) {
            mTouchButton.callOnClick();
        }
    }

    public void SetVisible(boolean visible) {
        if(visible == true) {
            mTouchButton.setVisibility(View.VISIBLE);
            //mVideoVIew.setVisibility(View.VISIBLE);
        }
        else {
            mTouchButton.setVisibility(View.GONE);
            mVideoVIew.setVisibility(View.GONE);
        }
    }

    protected Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message inputMessage) {
            switch (inputMessage.what) {
                case REMOVE_VIEW:
                    mVideoVIew.setVisibility(View.GONE);
                    //SetVisible(true);
                    break;
            }
        }
    };
}
