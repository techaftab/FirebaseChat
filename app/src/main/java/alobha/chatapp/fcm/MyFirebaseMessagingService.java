package alobha.chatapp.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.greenrobot.eventbus.EventBus;

import alobha.chatapp.FirebaseChatMainApp;
import alobha.chatapp.MainActivity;
import alobha.chatapp.R;
import alobha.chatapp.util.Constants;
import alobha.chatapp.util.SharedPrefUtil;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            String title = remoteMessage.getData().get("title");
            String message = remoteMessage.getData().get("text");
            String username = remoteMessage.getData().get("username");
            String uid = remoteMessage.getData().get("uid");
            String fcmToken = remoteMessage.getData().get("fcm_token");


            if (!FirebaseChatMainApp.isChatActivityOpen()) {
                sendNotification(title,
                        message,
                        username,
                        uid,
                        fcmToken);
            } else {
                EventBus.getDefault().post(new PushNotificationEvent(title,
                        message,
                        username,
                        uid,
                        fcmToken));
            }
        }
    }

    private void sendNotification(String title,
                                  String message,
                                  String receiver,
                                  String receiverUid,
                                  String firebaseToken) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Constants.ARG_RECEIVER, receiver);
        intent.putExtra(Constants.ARG_RECEIVER_UID, receiverUid);
        intent.putExtra(Constants.ARG_FIREBASE_TOKEN, firebaseToken);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        sendRegistrationToServer(s);
    }

    private void sendRegistrationToServer(final String token) {
        new SharedPrefUtil(getApplicationContext()).saveString(Constants.ARG_FIREBASE_TOKEN, token);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseDatabase.getInstance()
                    .getReference()
                    .child(Constants.ARG_USERS)
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child(Constants.ARG_FIREBASE_TOKEN)
                    .setValue(token);
        }
    }
}