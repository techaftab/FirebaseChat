package alobha.chatapp.fragments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import alobha.chatapp.BuildConfig;
import alobha.chatapp.R;
import alobha.chatapp.activity.FullScreenImageActivity;
import alobha.chatapp.adapter.ChatRecyclerAdapter;
import alobha.chatapp.adapter.ClickListenerChatFirebase;
import alobha.chatapp.core.chat.ChatContract;
import alobha.chatapp.core.chat.ChatPresenter;
import alobha.chatapp.fcm.PushNotificationEvent;
import alobha.chatapp.model.Chat;
import alobha.chatapp.model.FileModel;
import alobha.chatapp.model.MapModel;
import alobha.chatapp.model.UserModel;
import alobha.chatapp.util.Constants;
import alobha.chatapp.util.Util;

import static android.app.Activity.RESULT_OK;

/**
 * Author: Manoj Singh Deopa
 */

public class ChatFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener, ChatContract.View, View.OnClickListener ,ClickListenerChatFirebase {

    static final String TAG = "ChatActivityFragment";
    static final String CHAT_REFERENCE = "chatmodel";
    FirebaseStorage storage = FirebaseStorage.getInstance();
    private DatabaseReference mFirebaseDatabaseReference;

    private static final int IMAGE_GALLERY_REQUEST = 1;
    private static final int IMAGE_CAMERA_REQUEST = 2;
    private static final int PLACE_PICKER_REQUEST = 3;

    //File
    private File filePathImageCamera;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Boolean isFabOpen = false;
    private FloatingActionButton fab,fab1,fab2,fab3;
    private Animation fab_open,fab_close,rotate_forward,rotate_backward;
    LinearLayout commentlayout;
    ImageView buttonMessage;

    private RecyclerView mRecyclerViewChat;
    private EditText mETxtMessage;

    private ProgressDialog mProgressDialog;
    ProgressBar progress;

    private ChatRecyclerAdapter mChatRecyclerAdapter;

    private ChatPresenter mChatPresenter;

    //CLass Model
    private UserModel userModel;
    private GoogleApiClient mGoogleApiClient;

    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));

    public static ChatFragment newInstance(String receiver,
                                           String receiverUid,
                                           String firebaseToken) {
        Bundle args = new Bundle();
        args.putString(Constants.ARG_RECEIVER, receiver);
        args.putString(Constants.ARG_RECEIVER_UID, receiverUid);
        args.putString(Constants.ARG_FIREBASE_TOKEN, firebaseToken);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_chat, container, false);
        bindViews(fragmentView);
        progress.setVisibility(View.GONE);
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
        return fragmentView;
    }

    private void bindViews(View view) {
        progress= view.findViewById(R.id.progress);
        mRecyclerViewChat = view.findViewById(R.id.recycler_view_chat);
        mETxtMessage = view.findViewById(R.id.edit_text_message);
        commentlayout= view.findViewById(R.id.commentlayout);
        buttonMessage= view.findViewById(R.id.buttonMessage);

        fab = view.findViewById(R.id.fab);
        fab1 = view.findViewById(R.id.fab1);
        fab2 = view.findViewById(R.id.fab2);
        fab3 = view.findViewById(R.id.fab3);
        fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getActivity(),R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getActivity(),R.anim.rotate_backward);
        buttonMessage.setOnClickListener(this);
        fab.setOnClickListener(this);
        fab1.setOnClickListener(this);
        fab2.setOnClickListener(this);
        fab3.setOnClickListener(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    private void init()
    {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle(getString(R.string.loading));
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setIndeterminate(true);
        mChatPresenter = new ChatPresenter(this);
        mChatPresenter.getMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(), getArguments().getString(Constants.ARG_RECEIVER_UID));
    }


    private void sendMessage() {
        progress.setVisibility(View.VISIBLE);
        String message = mETxtMessage.getText().toString();
        String receiver = getArguments().getString(Constants.ARG_RECEIVER);
        String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
        String sender = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String receiverFirebaseToken = getArguments().getString(Constants.ARG_FIREBASE_TOKEN);

        Chat chat = new Chat(sender,receiver,senderUid,receiverUid,message,System.currentTimeMillis());
        mChatPresenter.sendMessage(getActivity().getApplicationContext(),chat,receiverFirebaseToken);
    }

    @Override
    public void onSendMessageSuccess() {
        progress.setVisibility(View.GONE);
        mETxtMessage.setText("");
//        Toast.makeText(getActivity(), "Message Sent", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSendMessageFailure(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGetMessagesSuccess(Chat chat) {

        Log.e("Message--",chat.message);
        if (chat.getFile()!=null){
            Log.e("getUrl_file--",chat.getFile().getUrl_file());
        }





        if (mChatRecyclerAdapter == null) {
            mChatRecyclerAdapter = new ChatRecyclerAdapter(new ArrayList<Chat>());
            mRecyclerViewChat.setAdapter(mChatRecyclerAdapter);
        }
        mChatRecyclerAdapter.add(chat);
        mRecyclerViewChat.smoothScrollToPosition(mChatRecyclerAdapter.getItemCount() - 1);
    }

    @Override
    public void onGetMessagesFailure(String message) {
        try
        {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Subscribe
    public void onPushNotificationEvent(PushNotificationEvent pushNotificationEvent) {
        if (mChatRecyclerAdapter == null || mChatRecyclerAdapter.getItemCount() == 0) {
            mChatPresenter.getMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                    pushNotificationEvent.getUid());
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.buttonMessage:
                if(TextUtils.isEmpty(mETxtMessage.getText().toString()))
                {
                    Toast.makeText(getActivity(), "Please Enter your Message", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    sendMessage();
                }
                break;
            case R.id.fab:
                animateFAB();
                break;
            case R.id.fab1:
                verifyStoragePermissions();
                break;
            case R.id.fab2:
                photoGalleryIntent();
                break;
            case R.id.fab3:
                locationPlacesIntent();
                break;
        }

    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     */
    public void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    getActivity(),
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }else{
            // we already have permission, lets go ahead and call camera intent
            photoCameraIntent();
        }
    }

    private void photoCameraIntent(){
        String nomeFoto = DateFormat.format("yyyy-MM-dd_hhmmss", new Date()).toString();
        filePathImageCamera = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), nomeFoto+"camera.jpg");
        Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoURI = FileProvider.getUriForFile(getActivity(),BuildConfig.APPLICATION_ID + ".provider",filePathImageCamera);
        it.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
        startActivityForResult(it, IMAGE_CAMERA_REQUEST);
    }

    private void photoGalleryIntent(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture_title)), IMAGE_GALLERY_REQUEST);
    }


    private void locationPlacesIntent() {
        try
        {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(getActivity()), PLACE_PICKER_REQUEST);
        }
        catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e)
        {
            e.printStackTrace();
            System.out.print(e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode){
            case REQUEST_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    photoCameraIntent();
                }
                break;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        StorageReference storageRef = storage.getReferenceFromUrl(Util.URL_STORAGE_REFERENCE).child(Util.FOLDER_STORAGE_IMG);

        if (requestCode == IMAGE_GALLERY_REQUEST){
            if (resultCode == RESULT_OK){
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null){
                    sendFileFirebase(storageRef,selectedImageUri);
                } else {
                    //URI IS NULL
                }
            }
        }else if (requestCode == IMAGE_CAMERA_REQUEST){
            if (resultCode == RESULT_OK){
                if (filePathImageCamera != null && filePathImageCamera.exists()){
                    StorageReference imageCameraRef = storageRef.child(filePathImageCamera.getName()+"_camera");
                    sendFileFirebase(imageCameraRef,filePathImageCamera);
                }else{
                    //IS NULL
                }
            }
        }else if (requestCode == PLACE_PICKER_REQUEST){
            if (resultCode == RESULT_OK)
            {
                Place place = PlacePicker.getPlace(getActivity(), data);
                if (place!=null)
                {
                    progress.setVisibility(View.VISIBLE);
                    LatLng latLng = place.getLatLng();
                    final String name = String.valueOf(place.getName());
                    MapModel mapModel = new MapModel(latLng.latitude+"",latLng.longitude+"",name);

                    String message = "";
                    String receiver = getArguments().getString(Constants.ARG_RECEIVER);
                    String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
                    String sender = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    String receiverFirebaseToken = getArguments().getString(Constants.ARG_FIREBASE_TOKEN);

                    Chat chat = new Chat(sender,receiver,senderUid,receiverUid,message,System.currentTimeMillis(),mapModel);
                    mChatPresenter.sendMessage(getActivity().getApplicationContext(),chat,receiverFirebaseToken);


                }
                else
                {
                    Toast.makeText(getActivity(), "Place is Null", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    /**
     * Envia o arvquivo para o firebase
     */
    private void sendFileFirebase(final StorageReference storageReference, final Uri file){
        if (storageReference != null){
            progress.setVisibility(View.VISIBLE);
            final String name = DateFormat.format("yyyy-MM-dd_hhmmss", new Date()).toString();
            StorageReference imageGalleryRef = storageReference.child(name+"_gallery");
            UploadTask uploadTask = imageGalleryRef.putFile(file);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG,"onFailure sendFileFirebase "+e.getMessage());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.i(TAG,"onSuccess sendFileFirebase");
                    Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
                  //  String downloadUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl().toString();
                   // Log.e(TAG,"onSuccess sendFileFirebase--->"+downloadUrl);
                    task.addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            Log.v(TAG, "Media is uploaded");

                            String downloadURL = "https://" + task.getResult().getEncodedAuthority()
                                    + task.getResult().getEncodedPath()
                                    + "?alt=media&token="
                                    + task.getResult().getQueryParameters("token").get(0);

                            Log.e(TAG, "downloadURL: " + downloadURL);
                            FileModel fileModel = new FileModel("img", downloadURL,name,"");

                            String message = "";
                            String receiver = getArguments().getString(Constants.ARG_RECEIVER);
                            String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
                            String sender = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                            String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            String receiverFirebaseToken = getArguments().getString(Constants.ARG_FIREBASE_TOKEN);

                            Chat chat = new Chat(sender,receiver,senderUid,receiverUid,message,System.currentTimeMillis(),fileModel);
                            mChatPresenter.sendMessage(getActivity().getApplicationContext(),chat,receiverFirebaseToken);
                            //save your downloadURL
                        }
                    });

                    //save your downloadURL



                }
            });

        }else{
            //IS NULL
        }

    }

    private void sendFileFirebase(StorageReference storageReference, final File file){
        if (storageReference != null){
            progress.setVisibility(View.VISIBLE);
            Uri photoURI = FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider",file);
            UploadTask uploadTask = storageReference.putFile(photoURI);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG,"onFailure sendFileFirebase "+e.getMessage());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.i(TAG,"onSuccess sendFileFirebase");
                    Task<Uri> task = taskSnapshot.getStorage().getDownloadUrl();
                    //  String downloadUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl().toString();
                    // Log.e(TAG,"onSuccess sendFileFirebase--->"+downloadUrl);
                    task.addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            Log.v(TAG, "Media is uploaded");

                            String downloadURL = "https://" + task.getResult().getEncodedAuthority()
                                    + task.getResult().getEncodedPath()
                                    + "?alt=media&token="
                                    + task.getResult().getQueryParameters("token").get(0);

                            Log.e(TAG, "downloadURL: " + downloadURL);
                            FileModel fileModel = new FileModel("img", downloadURL,file.getName(),"");

                            String message = "";
                            String receiver = getArguments().getString(Constants.ARG_RECEIVER);
                            String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
                            String sender = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                            String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            String receiverFirebaseToken = getArguments().getString(Constants.ARG_FIREBASE_TOKEN);

                            Chat chat = new Chat(sender,receiver,senderUid,receiverUid,message,System.currentTimeMillis(),fileModel);
                            mChatPresenter.sendMessage(getActivity().getApplicationContext(),chat,receiverFirebaseToken);
                            //save your downloadURL
                        }
                    });

                }
            });
        }else{
            //IS NULL
        }

    }


    @SuppressLint("RestrictedApi")
    public void animateFAB(){

        if(isFabOpen){

            fab.startAnimation(rotate_backward);
            fab1.startAnimation(fab_close);
            fab2.startAnimation(fab_close);
            fab3.startAnimation(fab_close);
            fab1.setVisibility(View.GONE);
            fab2.setVisibility(View.GONE);
            fab3.setVisibility(View.GONE);
            commentlayout.setVisibility(View.VISIBLE);
            isFabOpen = false;
            Log.d("Actvity", "close");

        } else {
            fab.startAnimation(rotate_forward);
            fab1.startAnimation(fab_open);
            fab2.startAnimation(fab_open);
            fab3.startAnimation(fab_open);
            fab1.setVisibility(View.VISIBLE);
            fab2.setVisibility(View.VISIBLE);
            fab3.setVisibility(View.VISIBLE);
            commentlayout.setVisibility(View.GONE);
            isFabOpen = true;
            Log.d("Actvity","open");

        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Util.initToast(getActivity(),"Google Play Services error.");
    }

    @Override
    public void clickImageChat(View view, int position,String nameUser,String urlPhotoUser,String urlPhotoClick) {
        Intent intent = new Intent(getActivity(),FullScreenImageActivity.class);
        intent.putExtra("nameUser",nameUser);
        intent.putExtra("urlPhotoUser",urlPhotoUser);
        intent.putExtra("urlPhotoClick",urlPhotoClick);
        startActivity(intent);
    }

    @Override
    public void clickImageMapChat(View view, int position,String latitude,String longitude) {
        String uri = String.format("geo:%s,%s?z=17&q=%s,%s", latitude,longitude,latitude,longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }
}
