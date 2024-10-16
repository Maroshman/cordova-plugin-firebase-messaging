package by.chemerisuk.cordova.firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rocketconsulting.rocketmobile.R;

import static android.content.ContentResolver.SCHEME_ANDROID_RESOURCE;
import static androidx.core.content.ContextCompat.getSystemService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class FirebaseMessagingPluginService extends FirebaseMessagingService {
    private static final String TAG = "FCMPluginService";

    public static final String ACTION_FCM_MESSAGE = "by.chemerisuk.cordova.firebase.ACTION_FCM_MESSAGE";
    public static final String EXTRA_FCM_MESSAGE = "by.chemerisuk.cordova.firebase.EXTRA_FCM_MESSAGE";
    public static final String ACTION_FCM_TOKEN = "by.chemerisuk.cordova.firebase.ACTION_FCM_TOKEN";
    public static final String EXTRA_FCM_TOKEN = "by.chemerisuk.cordova.firebase.EXTRA_FCM_TOKEN";
    public final static String NOTIFICATION_ICON_KEY = "com.google.firebase.messaging.default_notification_icon";
    public final static String NOTIFICATION_COLOR_KEY = "com.google.firebase.messaging.default_notification_color";
    public final static String NOTIFICATION_CHANNEL_KEY = "com.google.firebase.messaging.default_notification_channel_id";

    private LocalBroadcastManager broadcastManager;
    private NotificationManager notificationManager;
    private int defaultNotificationIcon;
    private int defaultNotificationColor;
    private String defaultNotificationChannel;

    // private File copySoundFileToInternalStorage(Context context, String fileName) {
    //     File externalFile = new File(Environment.getExternalStorageDirectory(), "Music/Rocket/" + fileName);
    //     File internalFile = new File(context.getFilesDir(), fileName);

    //     try (FileInputStream fis = new FileInputStream(externalFile);
    //          FileOutputStream fos = new FileOutputStream(internalFile)) {
    //         byte[] buffer = new byte[1024];
    //         int length;
    //         while ((length = fis.read(buffer)) > 0) {
    //             fos.write(buffer, 0, length);
    //         }
    //         return internalFile;
    //     } catch (IOException e) {
    //         Log.e(TAG, "Error copying sound file to internal storage", e);
    //         return null;
    //     }
    // }

    @Override
    public void onCreate() {
        broadcastManager = LocalBroadcastManager.getInstance(this);
        notificationManager = ContextCompat.getSystemService(this, NotificationManager.class);
        
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
            defaultNotificationIcon = ai.metaData.getInt(NOTIFICATION_ICON_KEY, ai.icon);
            defaultNotificationChannel = ai.metaData.getString(NOTIFICATION_CHANNEL_KEY, "default");
            defaultNotificationColor = ContextCompat.getColor(this, ai.metaData.getInt(NOTIFICATION_COLOR_KEY));
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Failed to load meta-data", e);
        } catch(Resources.NotFoundException e) {
            Log.d(TAG, "Failed to load notification color", e);
        }
        // On Android O or greater we need to create a new notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel defaultChannel = notificationManager.getNotificationChannel(defaultNotificationChannel);
            if (defaultChannel == null) {
                notificationManager.createNotificationChannel(
                        new NotificationChannel(defaultNotificationChannel, "Default", NotificationManager.IMPORTANCE_HIGH));
            }
        }
    }

    // private void registerNotificationChannels(final int count) {
    //     Context context = FirebaseMessagingPlugin.getContext();
    //     // Retry 5 times to get permission to read external storage, don't try to create channel without permissions
    //     if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
    //         createNotificationChannel("1", "Notification", "notification.mp3");
    //         createNotificationChannel("4", "Urgent Notification", "urgent_notification.mp3");
    //     } else if (count < 5) {
    //         Log.e(TAG, "Permission denied to read external storage custom sounds");
    //         // Create a Handler instance
    //         Handler handler = new Handler();
    //         // Use postDelayed to execute a Runnable after a delay
    //         handler.postDelayed(new Runnable() {
    //             @Override
    //             public void run() {
    //                 // Code to be executed after the delay
    //                 registerNotificationChannels(count + 1);
    //             }
    //         }, 30000); // Delay in milliseconds
    //     }
    // }

    // private void createNotificationChannel(String channelId, String channelName, String soundFileName) {
    //     NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
    //     if (channel != null) {
    //         Log.e(TAG, "Deleting existing channel " + channelId);
    //         notificationManager.deleteNotificationChannel(channelId);
    //     }
    //     channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
    //     // Construct the file path
    //     String filePath = "/storage/emulated/0/Music/Rocket/" + soundFileName;
    //     // Create a File object
    //     File soundFile = new File(filePath);
    //     // File file = new File(Environment.getExternalStorageDirectory(), "Music/Rocket/urgent_notification.mp3");
    //     Context context = FirebaseMessagingPlugin.getContext();

    //     // Check if the file exists
    //     if (soundFile.exists()) {
    //         File internalSoundFile = copySoundFileToInternalStorage(context, soundFileName);
    //         Uri soundUri = null;
    //         if (internalSoundFile != null) {
    //             soundUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", internalSoundFile);
    //             Log.e(TAG, soundUri.toString());
    //             AudioAttributes audioAttributes = new AudioAttributes.Builder()
    //                     .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    //                     .setUsage(AudioAttributes.USAGE_NOTIFICATION)
    //                     .build();
    //             channel.setSound(soundUri, audioAttributes);
    //         } else {
    //             Log.e(TAG, "Failed to copy sound file to internal storage");
    //             return;
    //         }

    //         // Parse the URI
    //         Log.e(TAG, "Found custom sound " + soundFileName + " URI: " + soundUri.toString());

    //         // Playing the sound for testing
    //         // MediaPlayer mediaPlayer = new MediaPlayer();
    //         // try {
    //         //     mediaPlayer.setDataSource(soundFile.getAbsolutePath());
    //         //     mediaPlayer.prepare();
    //         //     mediaPlayer.start();
    //         // } catch (Exception e) {
    //         //     Log.e(TAG, "Error playing sound " + soundFileName);
    //         // }

    //         channel.setSound(soundUri, new AudioAttributes.Builder()
    //                 .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    //                 .setUsage(AudioAttributes.USAGE_NOTIFICATION)
    //                 .build());
    //     } else {
    //         // Handle the case where the file does not exist
    //         Log.e(TAG, "Not found custom sound " + soundFileName);
    //     }
    //     notificationManager.createNotificationChannel(channel);
    // }

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseMessagingPlugin.sendToken(token);

        Intent intent = new Intent(ACTION_FCM_TOKEN);
        intent.putExtra(EXTRA_FCM_TOKEN, token);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        FirebaseMessagingPlugin.sendNotification(remoteMessage);

        Intent intent = new Intent(ACTION_FCM_MESSAGE);
        intent.putExtra(EXTRA_FCM_MESSAGE, remoteMessage);
        broadcastManager.sendBroadcast(intent);

        if (FirebaseMessagingPlugin.isForceShow()) {
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification != null) {
                showAlert(notification);
            }
        }
    }

    private void showAlert(RemoteMessage.Notification notification) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getNotificationChannel(notification))
                .setSound(getNotificationSound(notification.getSound()))
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getBody())
                .setGroup(notification.getTag())
                .setSmallIcon(defaultNotificationIcon)
                // .setColor(defaultNotificationColor)
                // must set priority to make sure forceShow works properly
                .setPriority(1);

        notificationManager.notify(0, builder.build());
        // dismiss notification to hide icon from status bar automatically
        new Handler(getMainLooper()).postDelayed(() -> {
            notificationManager.cancel(0);
        }, 3000);
    }

    private String getNotificationChannel(RemoteMessage.Notification notification) {
        String channel = notification.getChannelId();
        if (channel == null) {
            return defaultNotificationChannel;
        } else {
            return channel;
        }
    }

    private Uri getNotificationSound(String soundName) {
        if (soundName == null || soundName.isEmpty()) {
            return null;
        } else if (soundName.equals("default")) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            return Uri.parse(SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/raw/" + soundName);
        }
    }
}
