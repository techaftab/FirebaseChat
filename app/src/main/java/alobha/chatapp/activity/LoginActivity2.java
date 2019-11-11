package alobha.chatapp.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import alobha.chatapp.R;
import alobha.chatapp.fragments.LoginFragment;


public class LoginActivity2 extends AppCompatActivity {
    private Toolbar mToolbar;

    public static void startIntent(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    public static void startIntent(Context context, int flags) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(flags);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);
        bindViews();
        init();
    }

    private void bindViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
    }

    private void init() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout_content_login, LoginFragment.newInstance(),LoginFragment.class.getSimpleName());
        fragmentTransaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
