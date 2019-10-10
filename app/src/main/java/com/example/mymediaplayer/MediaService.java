package com.example.mymediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MediaService extends Service implements MediaPlayerCallback  {

    final String TAG = MediaService.class.getSimpleName();
    private MediaPlayer mMediaPlayer = null;
    private boolean isReady;
    public final static String ACTION_CREATE = "com.example.mysound.mediaservice.create";
    public final static String ACTION_DESTROY = "com.example.mysound.mediaservice.destroy";
    public final static int PLAY = 0;
    public final static int STOP = 1;

    public MediaService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_CREATE:
                    if (mMediaPlayer == null) {
                        init();
                    }
                    break;
                case ACTION_DESTROY:
                    if (!mMediaPlayer.isPlaying()) {
                        stopSelf();
                    }
                    break;
                default:
                    break;
            }
        }
        Log.d(TAG, "onStartCommand: ");
        return flags;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: ");
        return mMessenger.getBinder();
    }

    @Override
    public void onPlay() {
        if (!isReady) {
            mMediaPlayer.prepareAsync();
        } else {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                showNotif();
            } else {
                mMediaPlayer.start();
            }
        }
    }

    @Override
    public void onStop() {
        if (mMediaPlayer.isPlaying() || isReady) {
            mMediaPlayer.stop();
            isReady = false;
            showNotif();
        }
    }

    private void init() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.guitar_background);
        try {
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                isReady = true;
                mMediaPlayer.start();
                showNotif();
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }

    private final Messenger mMessenger = new Messenger(new IncomingHandler(this));

    static class IncomingHandler extends Handler {

        private WeakReference<MediaPlayerCallback> mediaPlayerCallbackWeakReference;

        IncomingHandler(MediaPlayerCallback playerCallback) {
            this.mediaPlayerCallbackWeakReference = new WeakReference<>(playerCallback);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PLAY:
                    mediaPlayerCallbackWeakReference.get().onPlay();
                    break;
                case STOP:
                    mediaPlayerCallbackWeakReference.get().onStop();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    void showNotif() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        String CHANNEL_DEFAULT_IMPORTANCE = "Channel_Test";
        int ONGOING_NOTIFICATION_ID = 1;
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                .setContentTitle("TES1")
                .setContentText("TES2")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setTicker("TES3")
                .build();
        createChannel(CHANNEL_DEFAULT_IMPORTANCE);
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    void createChannel(String CHANNEL_ID) {
        NotificationManager mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Battery", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);
        }
    }
    void stopNotif() {
        stopForeground(false);
    }
}
