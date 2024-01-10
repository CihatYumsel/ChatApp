package com.example.chatapp.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.Adapter.ChatAdapter;
import com.example.chatapp.Model.Chat;
import com.example.chatapp.Model.Kullanici;
import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity implements ChatAdapter.OnResimClickListener{
    private static final int IZIN_KODU = 0;
    private static final int IZIN_ALINDI_KODU = 1;

    private ProgressDialog mProgress;
    private Intent galeriIntent;
    private Uri imgUri;
    private String kayitYeri, indirmeLinki;
    private Bitmap imgBitmap;
    private ImageDecoder.Source imgSource;
    private ByteArrayOutputStream outputStream;
    private byte[] imgByte;

    private StorageReference mStorageRef, yeniRef, sRef;
    private FirebaseUser mUser;
    private HashMap<String, Object> mData;

    private RecyclerView mRecyclerView;
    private EditText editMesaj;
    private String txtMesaj, docId;
    private CircleImageView hedefProfil;
    private TextView hedefIsim;
    private Intent gelenIntent;
    private String hedefId, kanalId, hedefProfilUrl;
    private DocumentReference hedefRef;
    private Kullanici hedefKullanici;
    private FirebaseFirestore mFirestore;

    private Query chatQuery;
    private ArrayList<Chat> mChatList;
    private Chat mChat;
    private ChatAdapter chatAdapter;

    private void init() {
        mRecyclerView = (RecyclerView) findViewById(R.id.chat_activity_recyclerView);
        editMesaj = (EditText) findViewById(R.id.chat_activity_editMesaj);
        hedefProfil = (CircleImageView) findViewById(R.id.chat_activity_imgHedefProfil);
        hedefIsim = (TextView) findViewById(R.id.chat_activity_txtHedefIsim);

        mProgress = new ProgressDialog(ChatActivity.this);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirestore = FirebaseFirestore.getInstance();
        gelenIntent = getIntent();
        hedefId = gelenIntent.getStringExtra("hedefId");
        kanalId = gelenIntent.getStringExtra("kanalId");
        hedefProfilUrl = gelenIntent.getStringExtra("hedefProfil");

        mChatList = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        init();

//        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
//                super.onScrollStateChanged(recyclerView, newState);
//
//                if (!recyclerView.canScrollVertically(1)) {
//                    // Sayfayı yenile
//                    recreate();
//                }
//            }
//        });


        hedefRef = mFirestore.collection("Kullanıcılar").document(hedefId);
        hedefRef.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null && value.exists()) {
                    hedefKullanici = value.toObject(Kullanici.class);

                    if (hedefKullanici != null) {
                        hedefIsim.setText(hedefKullanici.getKullaniciIsim());

                        if (hedefKullanici.getKullaniciProfil().equals("default")) {
                            hedefProfil.setImageResource(R.mipmap.ic_launcher);
                        } else {
                            Picasso.get().load(hedefKullanici.getKullaniciProfil()).resize(76, 76).into(hedefProfil);
                        }
                    }
                }
            }
        });


        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));

        chatQuery = mFirestore.collection("Mesaj Kanalları").document(kanalId).collection("Mesajlar")
                .orderBy("mesajTarihi", Query.Direction.DESCENDING);
        chatQuery.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null) {
                    mChatList.clear();

                    for (DocumentSnapshot snapshot : value.getDocuments()) {
                        mChat = snapshot.toObject(Chat.class);

                        if (mChat != null) {
                            mChatList.add(mChat);
                        }
                    }

                    chatAdapter = new ChatAdapter(mChatList, ChatActivity.this, mUser.getUid(), hedefProfilUrl);
                    chatAdapter.setOnResimClickListener(ChatActivity.this);
                    mRecyclerView.setAdapter(chatAdapter);
                }
            }
        });
    }

    public void btnMesajGonder(View v) {
        txtMesaj = editMesaj.getText().toString();

        if (!TextUtils.isEmpty(txtMesaj)) {
            //Mesaj Gönder
            mesajGonder(txtMesaj, "text");
        } else {
            Toast.makeText(ChatActivity.this, "Mesaj Göndermek İçin Bir Şeyler Yazın", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnChatKapat(View v) {
        finish();
    }

    public void btnResimGonder(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, IZIN_KODU);
        } else {
            galeriyeGit();
        }
    }

//    public void btnResimAc(View v) {
//        Intent intent = new Intent(ChatActivity.this, FullScreenImageActivity.class);
//        intent.putExtra("indirmeLinki", indirmeLinki); // Resmin URI'si veya URL'si
//        startActivity(intent);
//    }

    @Override
    public void onResimClick(int position) {
        Chat chat = mChatList.get(position);
        String indirmeLinki = chat.getMesajIcerigi();
        Intent intent = new Intent(ChatActivity.this, FullScreenImageActivity.class);
        intent.putExtra("indirmeLinki", indirmeLinki); // Resmin URI'si veya URL'si
        startActivity(intent);
    }

    private void galeriyeGit() {
        galeriIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galeriIntent, IZIN_ALINDI_KODU);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == IZIN_KODU) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                galeriyeGit();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == IZIN_ALINDI_KODU) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                imgUri = data.getData();

                try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        imgSource = ImageDecoder.createSource(this.getContentResolver(), imgUri);
                        imgBitmap = ImageDecoder.decodeBitmap(imgSource);
                    } else {
                        imgBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
                    }

                    outputStream = new ByteArrayOutputStream();
                    imgBitmap.compress(Bitmap.CompressFormat.PNG, 75, outputStream);
                    imgByte = outputStream.toByteArray();

                    kayitYeri = "Chat Resimler/" + kanalId + "/" + mUser.getEmail() + "/" + System.currentTimeMillis() + ".png";
                    sRef = mStorageRef.child(kayitYeri);
                    sRef.putBytes(imgByte)
                            .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    mProgress.setTitle("Resim Gönderiliyor...");
                                    mProgress.show();

                                    yeniRef = FirebaseStorage.getInstance().getReference(kayitYeri);
                                    yeniRef.getDownloadUrl()
                                            .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    indirmeLinki = uri.toString();

                                                    //Mesaj Gönder
                                                    mesajGonder(indirmeLinki, "resim");

                                                    chatQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                        @Override
                                                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                                            if (error != null) {
                                                                Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                                                                return;
                                                            }

                                                            if (value != null) {
                                                                mChatList.clear();

                                                                for (DocumentSnapshot snapshot : value.getDocuments()) {
                                                                    mChat = snapshot.toObject(Chat.class);

                                                                    if (mChat != null) {
                                                                        mChatList.add(mChat);
                                                                    }
                                                                }

                                                                chatAdapter = new ChatAdapter(mChatList, ChatActivity.this, mUser.getUid(), hedefProfilUrl);
                                                                mRecyclerView.setAdapter(chatAdapter);
                                                            }
                                                        }
                                                    });

                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    e.printStackTrace();
                                                    progressAyari();
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    e.printStackTrace();
                                }
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }



    private void mesajGonder(String txtMesaj, String mesajTipi) {
        docId = UUID.randomUUID().toString();

        mData = new HashMap<>();

        mData.put("mesajIcerigi", txtMesaj);
        mData.put("gonderen", mUser.getUid());
        mData.put("alici", hedefId);
        mData.put("mesajTipi", mesajTipi);
        mData.put("mesajTarihi", FieldValue.serverTimestamp());
        mData.put("docId", docId);

        mFirestore.collection("Mesaj Kanalları").document(kanalId).collection("Mesajlar").document(docId)
                .set(mData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            editMesaj.setText("");
                            progressAyari();
                        } else {
                            Toast.makeText(ChatActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void progressAyari() {
        if (mProgress.isShowing()) {
            mProgress.dismiss();
        }
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