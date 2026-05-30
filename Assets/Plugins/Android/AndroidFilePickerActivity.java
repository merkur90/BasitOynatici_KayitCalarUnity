package com.unity3d.player;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.database.Cursor;
import android.provider.OpenableColumns;
import com.unity3d.player.UnityPlayer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AndroidFilePickerActivity extends Activity {
    private static final String TAG = "FilePickerActivity";
    private static final int REQUEST_PICK_FILE = 999;
    private String unityObjectName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getIntent() != null) {
            unityObjectName = getIntent().getStringExtra("unityObjectName");
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"audio/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        try {
            startActivityForResult(intent, REQUEST_PICK_FILE);
        } catch (Exception e) {
            Log.e(TAG, "Gallery open error: " + e.getMessage());
            sendToUnity("HATA: Galeri acilamadi!");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                final Uri fileUri = data.getData();
                
                sendToUnity("YUKLENIYOR: %0");

                try {
                    final String fileName = getFileName(fileUri);
                    String lowerName = fileName.toLowerCase();
                    
                    String mimeType = getContentResolver().getType(fileUri);
                    if (mimeType == null) mimeType = "";
                    mimeType = mimeType.toLowerCase();
                    
                    if (lowerName.endsWith(".wma") || mimeType.contains("wma") || mimeType.contains("x-ms-wma")) {
                        sendToUnity(".wma");
                        finish();
                        return;
                    }

                    boolean isAudio = lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a") || 
                                      lowerName.endsWith(".wav") || lowerName.endsWith(".ogg") ||
                                      mimeType.startsWith("audio/");

                    if (!isAudio) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    File cacheDir = getCacheDir();
                                    if (cacheDir != null) {
                                        File[] oldFiles = cacheDir.listFiles();
                                        if (oldFiles != null) {
                                            for (File oldFile : oldFiles) {
                                                if (oldFile.isFile()) oldFile.delete();
                                            }
                                        }

                                        File tempFile = new File(cacheDir, fileName);
                                        InputStream inputStream = getContentResolver().openInputStream(fileUri);
                                        
                                        long totalBytes = 0;
                                        Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
                                        if (cursor != null) {
                                            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                                            if (sizeIndex != -1 && cursor.moveToFirst()) {
                                                totalBytes = cursor.getLong(sizeIndex);
                                            }
                                            cursor.close();
                                        }

                                        FileOutputStream outputStream = new FileOutputStream(tempFile);
                                        byte[] buffer = new byte[3145728];
                                        int bytesRead;
                                        long bytesCopied = 0;
                                        int lastPercent = 0;

                                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                                            outputStream.write(buffer, 0, bytesRead);
                                            bytesCopied += bytesRead;

                                            if (totalBytes > 0) {
                                                int percent = (int) ((bytesCopied * 100) / totalBytes);
                                                if (percent > lastPercent) {
                                                    lastPercent = percent;
                                                    sendToUnity("YUKLENIYOR: %" + lastPercent);
                                                }
                                            }
                                        }
                                        
                                        outputStream.close();
                                        inputStream.close();

                                        sendToUnity(tempFile.getAbsolutePath());
                                    } else {
                                        sendToUnity("HATA: Onbellek klasoru bulunamadi!");
                                    }
                                } catch (Exception e) {
                                    sendToUnity("JAVA_HATASI: " + e.getMessage());
                                }
                            }
                        }).start();
                        
                        finish();
                        return;
                    } 
                    else {
                        final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            getContentResolver().takePersistableUriPermission(fileUri, takeFlags);
                        } catch (Exception e) {
                            Log.w(TAG, "Izin kilitleme hatasi: " + e.getMessage());
                        }
                        sendToUnity(fileUri.toString());
                    }
                    
                } catch (Exception e) {
                    sendToUnity("JAVA_HATASI: " + e.getMessage());
                }
            } else {
                sendToUnity("KULLANICI_IPTAL");
            }
            finish();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        if (result == null || result.isEmpty()) {
            result = "temp_media.mp4";
        }
        return result;
    }

    private void sendToUnity(String resultPath) {
        if (unityObjectName != null && !unityObjectName.isEmpty()) {
            UnityPlayer.UnitySendMessage(unityObjectName, "OnVideoPicked", resultPath);
        }
    }
}