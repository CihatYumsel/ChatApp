package com.example.chatapp.Fragment;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.chatapp.Model.Kullanici;
import com.example.chatapp.R;
import com.example.chatapp.View.GirisYapActivity;
import com.example.chatapp.View.MainActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
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
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfilFragment extends Fragment {
    private static final int IZIN_KODU = 0;
    private static final int IZIN_ALINDI_KODU = 1;

    private EditText editIsim, editEmail;
    private CircleImageView imgProfil;
    private ImageView imgYeniResim;
    private RelativeLayout relaCikis;
    private View v;

    private FirebaseFirestore mFirestore;
    private DocumentReference mRef;
    private FirebaseUser mUser;
    private Kullanici kullanici;

    private Intent galeriIntent;
    private Uri mUri;
    private Bitmap gelenResim;
    private ImageDecoder.Source imgSource;
    private ByteArrayOutputStream outputStream;
    private byte[] imgByte;
    private StorageReference mStorageRef, yeniRef, sRef;
    private String kayitYeri, indirmeLinki;
    private HashMap<String, Object> mData;

    private Query mQuery;

    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v =  inflater.inflate(R.layout.fragment_profil, container, false);

        editIsim = v.findViewById(R.id.profil_fragment_editIsim);
        editEmail = v.findViewById(R.id.profil_fragment_editEmail);
        imgProfil = v.findViewById(R.id.profil_fragment_imgUserProfil);
        imgYeniResim = v.findViewById(R.id.profil_fragment_imgYeniResim);
        relaCikis = v.findViewById(R.id.profil_fragment_relaCikis);

        mAuth = FirebaseAuth.getInstance();

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mFirestore = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        mRef = mFirestore.collection("Kullanıcılar").document(mUser.getUid());
        mRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Toast.makeText(v.getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null && value.exists()) {
                    kullanici = value.toObject(Kullanici.class);

                    if (kullanici != null) {
                        editIsim.setText(kullanici.getKullaniciIsim());
                        editEmail.setText(kullanici.getKullaniciEmail());

                        if (kullanici.getKullaniciProfil().equals("default")) {
                            imgProfil.setImageResource(R.mipmap.ic_launcher);
                        } else {
                            Picasso.get().load(kullanici.getKullaniciProfil()).resize(156, 156).into(imgProfil);
                        }
                    }
                }
            }
        });

        imgYeniResim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //PERMISSION.GRANTED vardı değiştirdim
                if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions((Activity) v.getContext(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, IZIN_KODU);
                } else {
                    galeriyeGit();
                }
            }
        });

        relaCikis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent cikisIntent = new Intent(getActivity(), GirisYapActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getActivity().finish();
                startActivity(cikisIntent);

                mFirestore.collection("Kullanıcılar").document(mUser.getUid()).update(
                        "kullaniciOnline", false
                );
            }
        });

        return v;
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == IZIN_ALINDI_KODU) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                mUri = data.getData();

                try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        imgSource = ImageDecoder.createSource(v.getContext().getContentResolver(), mUri);
                        gelenResim = ImageDecoder.decodeBitmap(imgSource);
                    } else {
                        gelenResim = MediaStore.Images.Media.getBitmap(v.getContext().getContentResolver(), mUri);
                    }

                    outputStream = new ByteArrayOutputStream();
                    gelenResim.compress(Bitmap.CompressFormat.PNG, 75, outputStream);
                    imgByte = outputStream.toByteArray();

                    kayitYeri = "Kullanıcılar/" + kullanici.getKullaniciEmail() + "/profil.png";
                    sRef = mStorageRef.child(kayitYeri);
                    sRef.putBytes(imgByte)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    yeniRef = FirebaseStorage.getInstance().getReference(kayitYeri);
                                    yeniRef.getDownloadUrl()
                                            .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    indirmeLinki = uri.toString();
                                                    mData = new HashMap<>();
                                                    mData.put("kullaniciProfil", indirmeLinki);

                                                    mFirestore.collection("Kullanıcılar").document(mUser.getUid())
                                                            .update(mData)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        iletisimIcinProfilGuncelle(indirmeLinki);
                                                                    } else {
                                                                        Toast.makeText(v.getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            });
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(v.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(v.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void iletisimIcinProfilGuncelle(final String link) {
        mQuery = mFirestore.collection("Kullanıcılar").document(mUser.getUid()).collection("Kanal");
        mQuery.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (queryDocumentSnapshots.getDocuments().size() > 0) {
                            for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                                mData = new HashMap<>();
                                mData.put("kullaniciId", link);

                                mFirestore.collection("Kullanıcılar").document(snapshot.getData().get("kullaniciId").toString())
                                        .collection("Kanal").document(mUser.getUid()).update(mData);
                            }

                            Toast.makeText(v.getContext(), "Profil Resminiz Başarıyla Güncellendi", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(v.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}