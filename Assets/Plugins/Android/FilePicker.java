package com.unity3d.player;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import com.unity3d.player.UnityPlayer;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.OutputStream;

public class FilePicker {
    private static final String TAG = "FilePicker";
    private static MediaRecorder mediaRecorder;
    private static String lastRecordedFilePath = "";
    private static HeadsetReceiver headsetReceiver;
    private static boolean isRecording = false;

    public static void pickVideo(String unityObjectName) {
        Activity currentActivity = UnityPlayer.currentActivity;
        if (currentActivity == null) {
            Log.e(TAG, "Current Activity is null!");
            return;
        }
        Intent intent = new Intent(currentActivity, AndroidFilePickerActivity.class);
        intent.putExtra("unityObjectName", unityObjectName);
        currentActivity.startActivity(intent);
    }

    public static boolean dosjadaGoruntuVarMi(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            Activity currentActivity = UnityPlayer.currentActivity;
            if (path.startsWith("content://") && currentActivity != null) {
                Uri uri = Uri.parse(path);
                retriever.setDataSource(currentActivity, uri);
            } else {
                retriever.setDataSource(path);
            }
            String hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
            retriever.release();
            return "yes".equals(hasVideo);
        } catch (Exception e) {
            Log.e(TAG, "MediaMetadataRetriever error: " + e.getMessage());
            try { retriever.release(); } catch (Exception ignored) {}
            return false;
        }
    }
	public static void startVoiceRecording() {
    if (isRecording) return;
    Activity currentActivity = UnityPlayer.currentActivity;
    if (currentActivity == null) return;

    try {
        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); } catch(Exception ignored){}
            mediaRecorder.release();
            mediaRecorder = null;
        }

        // AudioManager bloklari silindi (Kalite ve uyumluluk icin)

        if (headsetReceiver == null) {
            headsetReceiver = new HeadsetReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_HEADSET_PLUG);
            currentActivity.registerReceiver(headsetReceiver, filter);
        }

        File sampleDir = currentActivity.getCacheDir();
        File audioFile = File.createTempFile("studio_kayit_", ".AAC", sampleDir);
        lastRecordedFilePath = audioFile.getAbsolutePath();

        mediaRecorder = new MediaRecorder();
        
        // Ses kaynagý ve format optimize edildi
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setOutputFile(lastRecordedFilePath);
        
        mediaRecorder.prepare();
        mediaRecorder.start();
        isRecording = true;
        UnityPlayer.UnitySendMessage("PlayerYntScript", "OnVoiceRecordStatus", "KAYIT_BASLADI");
    } catch (Exception e) {
        Log.e(TAG, "Record start error: " + e.getMessage());
        UnityPlayer.UnitySendMessage("PlayerYntScript", "OnVoiceRecordStatus", "HATA_BASLATILAMADI");
    }
}
    
    public static void stopVoiceRecording() {
    if (!isRecording) return;
    try {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        isRecording = false;

        Activity currentActivity = UnityPlayer.currentActivity;
        if (currentActivity != null && lastRecordedFilePath != null && !lastRecordedFilePath.isEmpty()) {
            String finalPublicPath = saveToPublicDownloads(currentActivity, lastRecordedFilePath);
            if (!finalPublicPath.isEmpty()) {
                UnityPlayer.UnitySendMessage("PlayerYntScript", "OnVoiceRecordStatus", "KAYIT_BITTI:ok" + finalPublicPath);
            } else {
                UnityPlayer.UnitySendMessage("PlayerYntScript", "OnVoiceRecordStatus", "HATA_TELEFONA_KAYDEDILEMEDI");
            }
        }
    } catch (Exception e) {
        Log.e(TAG, "Record stop error: " + e.getMessage());
        UnityPlayer.UnitySendMessage("PlayerYntScript", "OnVoiceRecordStatus", "HATA_DURDURULAMADI");
    }
    // cleanAudioRouting(); burasý tamamen silindi
}

    private static void cleanAudioRouting() {
        Activity currentActivity = UnityPlayer.currentActivity;
        if (currentActivity != null) {
            android.media.AudioManager audioManager = (android.media.AudioManager) currentActivity.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                if (audioManager.isBluetoothScoOn()) {
                    audioManager.setBluetoothScoOn(false);
                    audioManager.stopBluetoothSco();
                }
                audioManager.setMode(android.media.AudioManager.MODE_NORMAL);
            }
            if (headsetReceiver != null) {
                try {
                    currentActivity.unregisterReceiver(headsetReceiver);
                } catch (Exception ignored) {}
                headsetReceiver = null;
            }
        }
    }

    private static String saveToPublicDownloads(Context context, String sourcePath) {
        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) return "";

            String publicFileName = "Studio_Kayit_" + System.currentTimeMillis() + ".AAC";
            Uri audioUri = null;

            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, publicFileName);
                values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/AAC");
                values.put("relative_path", Environment.DIRECTORY_DOWNLOADS + "/StudioKayitlari");

                Uri downloadsUri = Uri.parse("content://media/external/downloads");
                audioUri = context.getContentResolver().insert(downloadsUri, values);
            } else {
                File publicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "StudioKayitlari");
                if (!publicDir.exists()) publicDir.mkdirs();
                File destFile = new File(publicDir, publicFileName);
                audioUri = Uri.fromFile(destFile);
            }

            if (audioUri != null) {
                FileInputStream fis = new FileInputStream(sourceFile);
                OutputStream os = context.getContentResolver().openOutputStream(audioUri);
                
                byte[] buffer = new byte[3145728];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                
                os.flush();
                os.close();
                fis.close();

                try { sourceFile.delete(); } catch(Exception ignored){}

                return audioUri.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Save to downloads failed: " + e.getMessage());
        }
        return "";
    }

    private static class HeadsetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                int state = intent.getIntExtra("state", -1);
                if (state == 1) {
                    UnityPlayer.UnitySendMessage("PlayerYntScript", "OnKulaklikDurumu", "KULAKLIK_TAKILDI");
                } else if (state == 0) {
                    UnityPlayer.UnitySendMessage("PlayerYntScript", "OnKulaklikDurumu", "KULAKLIK_CIKARILDI");
                }
            } else if (android.media.AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)) {
                int state = intent.getIntExtra(android.media.AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                if (state == android.media.AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    UnityPlayer.UnitySendMessage("PlayerYntScript", "OnKulaklikDurumu", "BLUETOOTH_BAGLANDI");
                } else if (state == android.media.AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    UnityPlayer.UnitySendMessage("PlayerYntScript", "OnKulaklikDurumu", "BLUETOOTH_KOPTI");
                }
            }
        }
    }

    public static class MuzikServisi extends Service {
        private static final String CHANNEL_ID = "MuzikServisiKanal";
        private static final int NOTIFICATION_ID = 881;
        private static final String ACTION_STOP = "com.unity3d.player.ACTION_STOP";
        
        private static MediaPlayer mediaPlayer;
        private static float currentVolume = 1.0f;
        private boolean isReceiverRegistered = false;
        
        private String currentRunningPath = "";
        private final Handler restartHandler = new Handler(Looper.getMainLooper());
        private Runnable restartRunnable;

        private final BroadcastReceiver stopReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && ACTION_STOP.equals(intent.getAction())) {
                    try {
                        UnityPlayer.UnitySendMessage("PlayerYntScript", "StopMedia", "");
                    } catch (Exception e) {
                        Log.e("MuzikServisi", "UnitySendMessage failed: " + e.getMessage());
                    }
                    stopSelf();
                }
            }
        };

        @Override
        public void onCreate() {
            super.onCreate();
            createNotificationChannel();
            
            if (!isReceiverRegistered) {
                IntentFilter filter = new IntentFilter(ACTION_STOP);
                registerReceiver(stopReceiver, filter);
                isReceiverRegistered = true;
            }

            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent != null) {
                String urlPath = intent.getStringExtra("urlPath");
                if (urlPath != null && !urlPath.isEmpty()) {
                    if (restartRunnable != null) {
                        restartHandler.removeCallbacks(restartRunnable);
                    }
                    currentRunningPath = urlPath;
                    playAudio(urlPath);
                }
            }
            return START_NOT_STICKY;
        }

        private void playAudio(final String urlPath) {
            try {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                mediaPlayer = new MediaPlayer();
                
                if (urlPath.startsWith("content://")) {
                    Uri uri = Uri.parse(urlPath);
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                    if (pfd != null) {
                        FileDescriptor fd = pfd.getFileDescriptor();
                        mediaPlayer.setDataSource(fd);
                        pfd.close();
                    } else {
                        return;
                    }
                } else {
                    mediaPlayer.setDataSource(urlPath);
                }

                mediaPlayer.setVolume(currentVolume, currentVolume);
                mediaPlayer.setLooping(false);

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        restartRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (currentRunningPath != null && !currentRunningPath.isEmpty()) {
                                    playAudio(currentRunningPath);
                                }
                            }
                        };
                        restartHandler.postDelayed(restartRunnable, 60000);
                    }
                });

                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        return false;
                    }
                });

                mediaPlayer.prepareAsync();

            } catch (Exception e) {
                Log.e("MuzikServisi", "PlayAudio error: " + e.getMessage());
            }
        }

        public static void setJavaVolume(float volume) {
            currentVolume = volume;
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.setVolume(currentVolume, currentVolume);
                } catch (Exception e) {
                    Log.e("MuzikServisi", "SetVolume error: " + e.getMessage());
                }
            }
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Arkaplan Muzik Oynatici",
                        NotificationManager.IMPORTANCE_LOW
                );
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
        }

        private Notification createNotification() {
            Intent stopIntent = new Intent(ACTION_STOP);
            
            PendingIntent stopPendingIntent;
            if (Build.VERSION.SDK_INT >= 23) {
                stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | 0x04000000);
            } else {
                stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= 26) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }

            return builder.setContentTitle("Muzik Caliniyor")
                    .setContentText("Uygulama arkaplanda muzigi koruyor.")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .addAction(android.R.drawable.ic_media_pause, "Durdur", stopPendingIntent)
                    .setOngoing(true)
                    .build();
        }

        @Override
        public void onDestroy() {
            if (restartRunnable != null) {
                restartHandler.removeCallbacks(restartRunnable);
            }
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
            
            if (isReceiverRegistered) {
                try {
                    unregisterReceiver(stopReceiver);
                } catch (Exception ignored) {}
                isReceiverRegistered = false;
            }
            
            super.onDestroy();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}