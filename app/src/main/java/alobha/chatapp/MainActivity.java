package alobha.chatapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import alobha.chatapp.activity.FullScreenImageActivity;
import alobha.chatapp.activity.LoginActivity;
import alobha.chatapp.activity.UserListingActivity;
import alobha.chatapp.adapter.ChatFirebaseAdapter;
import alobha.chatapp.adapter.ClickListenerChatFirebase;
import alobha.chatapp.model.Chat;
import alobha.chatapp.model.ChatModel;
import alobha.chatapp.model.FileModel;
import alobha.chatapp.model.UserModel;
import alobha.chatapp.util.Constants;
import alobha.chatapp.util.Util;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, ClickListenerChatFirebase {

    private static final int IMAGE_GALLERY_REQUEST = 1;
    private static final int IMAGE_CAMERA_REQUEST = 2;
    private static final int PLACE_PICKER_REQUEST = 3;

    static final String TAG = MainActivity.class.getSimpleName();
    static final String CHAT_REFERENCE = "chatmodel";

    //Firebase and GoogleApiClient
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mFirebaseDatabaseReference;
    FirebaseStorage storage = FirebaseStorage.getInstance();

    //CLass Model
    private UserModel userModel;

    //Views UI
    private RecyclerView rvListMessage;
    private LinearLayoutManager mLinearLayoutManager;
    private ImageView btSendMessage;
    private EditText edMessage;
    private View contentRoot;
//    private EmojIconActions emojIcon;

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
    RelativeLayout commentlayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Util.verificaConexao(this)){
            Util.initToast(this,"Please Check Your Internet Connection !");
            finish();
        }else{
            bindViews();
            checkforLogin();
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API)
                    .build();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        StorageReference storageRef = storage.getReferenceFromUrl(Util.URL_STORAGE_REFERENCE).child(Util.FOLDER_STORAGE_IMG);

        if (requestCode == IMAGE_GALLERY_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    sendFileFirebase(storageRef, selectedImageUri);
                } else {
                    //URI IS NULL
                }
            }
        } else if (requestCode == IMAGE_CAMERA_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (filePathImageCamera != null && filePathImageCamera.exists()) {
                    StorageReference imageCameraRef = storageRef.child(filePathImageCamera.getName() + "_camera");
                    sendFileFirebase(imageCameraRef, filePathImageCamera);
                } else {
                    //IS NULL
                }
            }
        } else if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                if (place != null) {
//                    LatLng latLng = place.getLatLng();
//                    MapModel mapModel = new MapModel(latLng.latitude+"",latLng.longitude+"");
//                    ChatModel chatModel = new ChatModel(userModel,Calendar.getInstance().getTime().getTime()+"",mapModel);
//                    mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(chatModel);
                } else {
                    //PLACE IS NULL
                }
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.sendPhoto:
                verifyStoragePermissions();
//                photoCameraIntent();
                break;
            case R.id.sendPhotoGallery:
                photoGalleryIntent();
                break;
            case R.id.sendLocation:
                locationPlacesIntent();
                break;
            case R.id.sign_out:
                signOut();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Util.initToast(this,"Google Play Services error.");
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.buttonMessage:
                sendMessageFirebase();
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

    @Override
    public void clickImageChat(View view, int position,String nameUser,String urlPhotoUser,String urlPhotoClick) {
        Intent intent = new Intent(this,FullScreenImageActivity.class);
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


    /**
     * Envia o arvquivo para o firebase
     */
    private void sendFileFirebase(StorageReference storageReference, final Uri file){
        if (storageReference != null){
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

                                //FileModel fileModel = new FileModel("img", downloadUrl,name,"");
                                ChatModel chatModel = new ChatModel(userModel,"",Calendar.getInstance().getTime().getTime()+"",fileModel);
                                mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(chatModel);
                                //save your downloadURL
                            }
                        });

                    }
                });
        }else{
            //IS NULL
        }

    }

    /**
     * Envia o arvquivo para o firebase
     */
    private void sendFileFirebase(StorageReference storageReference, final File file){
        if (storageReference != null){
            Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider",file);
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

                  /*  String downloadUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl().toString();
                    FileModel fileModel = new FileModel("img", downloadUrl,file.getName(),file.length()+"");
                    ChatModel chatModel = new ChatModel(userModel,"",Calendar.getInstance().getTime().getTime()+"",fileModel);
                    mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(chatModel);*/
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
                            FileModel fileModel = new FileModel("img", downloadURL,file.getName(),file.length()+"");

                            //FileModel fileModel = new FileModel("img", downloadUrl,name,"");
                            ChatModel chatModel = new ChatModel(userModel,"",Calendar.getInstance().getTime().getTime()+"",fileModel);
                            mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(chatModel);
                            //save your downloadURL
                        }
                    });
                }
            });
        }else{
            //IS NULL
        }

    }

    /**
     * Obter local do usuario
     */
    private void locationPlacesIntent(){
        try {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * Enviar foto tirada pela camera
     */
    private void photoCameraIntent(){
        String nomeFoto = DateFormat.format("yyyy-MM-dd_hhmmss", new Date()).toString();
        filePathImageCamera = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), nomeFoto+"camera.jpg");
        Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                BuildConfig.APPLICATION_ID + ".provider",
                filePathImageCamera);
        it.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
        startActivityForResult(it, IMAGE_CAMERA_REQUEST);
    }

    /**
     * Enviar foto pela galeria
     */
    private void photoGalleryIntent(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture_title)), IMAGE_GALLERY_REQUEST);
    }

    /**
     * Enviar msg de texto simples para chat
     */
    private void sendMessageFirebase(){
        ChatModel model = new ChatModel(userModel,edMessage.getText().toString(), Calendar.getInstance().getTime().getTime()+"",null);
        mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(model);
        edMessage.setText(null);
    }

    /**
     * Ler collections chatmodel Firebase
     */
    private void lerMessagensFirebase(){
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        final ChatFirebaseAdapter firebaseAdapter = new ChatFirebaseAdapter(mFirebaseDatabaseReference.child(CHAT_REFERENCE),userModel.getName(),this);
        firebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = firebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    rvListMessage.scrollToPosition(positionStart);
                }
            }
        });
        rvListMessage.setLayoutManager(mLinearLayoutManager);
        rvListMessage.setAdapter(firebaseAdapter);
    }

    /**
     * Verificar se usuario está logado
     */
    private void checkforLogin(){
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null){
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }else
        {
            startActivity(new Intent(this, UserListingActivity.class));
            finish();
//            userModel = new UserModel(mFirebaseUser.getDisplayName(), mFirebaseUser.getPhotoUrl().toString(), mFirebaseUser.getUid());
//            lerMessagensFirebase();
        }
    }

    /**
     * Vincular views com Java API
     */
    private void bindViews(){
        contentRoot = findViewById(R.id.contentRoot);
        edMessage = findViewById(R.id.editTextMessage);
        btSendMessage = findViewById(R.id.buttonMessage);
        commentlayout= findViewById(R.id.commentlayout);
        btSendMessage.setOnClickListener(this);
        fab = findViewById(R.id.fab);
        fab1 = findViewById(R.id.fab1);
        fab2 = findViewById(R.id.fab2);
        fab3 = findViewById(R.id.fab3);
        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate_backward);
        fab.setOnClickListener(this);
        fab1.setOnClickListener(this);
        fab2.setOnClickListener(this);
        fab3.setOnClickListener(this);

//        emojIcon = new EmojIconActions(this,contentRoot,edMessage,btEmoji);
//        emojIcon.ShowEmojIcon();
        rvListMessage = findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
    }

    /**
     * Sign Out no login
     */
    private void signOut()
    {
        mFirebaseAuth.signOut();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     */
    public void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }else{
            // we already have permission, lets go ahead and call camera intent
            photoCameraIntent();
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

}
