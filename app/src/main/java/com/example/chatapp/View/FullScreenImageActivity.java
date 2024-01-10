package com.example.chatapp.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class FullScreenImageActivity extends AppCompatActivity {
    private ImageView fullScreenImageView;
    private FirebaseUser mUser;
    private FirebaseFirestore mFirestore;
    private HashMap<String, Object> mData;

    public void init() {
        mFirestore = FirebaseFirestore.getInstance();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);
        init();

        fullScreenImageView = (ImageView) findViewById(R.id.fullScreenImageView);

        // Resmin URI'sini veya URL'sini al
        Intent intent = getIntent();
        String indirmeLinki = intent.getStringExtra("indirmeLinki");

        // Resmi tam ekran göster
        Picasso.get().load(indirmeLinki).fit().centerInside().into(fullScreenImageView);
    }

    private void kullaniciSetOnline(Boolean b) {
        mData = new HashMap<>();
        mData.put("kullaniciOnline", b);

        mFirestore.collection("Kullanıcılar").document(mUser.getUid())
                .update(mData);
    }

    @Override
    protected void onStart() {
        super.onStart();
        kullaniciSetOnline(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        kullaniciSetOnline(false);
    }
}
