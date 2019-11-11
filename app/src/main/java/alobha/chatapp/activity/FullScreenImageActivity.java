package alobha.chatapp.activity;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;



import alobha.chatapp.R;
import alobha.chatapp.adapter.CircleTransform;

public class FullScreenImageActivity extends AppCompatActivity {

    private ImageView mImageView;
    private ImageView ivUser;
    private TextView tvUser;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);
        bindViews();
        setValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setValues();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        System.gc();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }


    private void bindViews() {
        progressDialog = new ProgressDialog(this);
        mImageView = (ImageView) findViewById(R.id.imageView);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ivUser = (ImageView) toolbar.findViewById(R.id.avatar);
        tvUser = (TextView) toolbar.findViewById(R.id.title);
    }

    private void setValues() {
        String nameUser, urlPhotoUser, urlPhotoClick;
        nameUser = getIntent().getStringExtra("nameUser");
        urlPhotoUser = getIntent().getStringExtra("urlPhotoUser");
        urlPhotoClick = getIntent().getStringExtra("urlPhotoClick");
        Log.i("TAG", "urlPhotoClick " + urlPhotoClick);
        tvUser.setText(nameUser); // Name
        Glide.with(this).load(urlPhotoUser).centerCrop().transform(new CircleTransform(this)).override(40, 40).into(ivUser);

        Glide.with(this).load(urlPhotoClick).centerCrop().transform(new CircleTransform(this)).override(40, 40).into(ivUser);


    }

}
