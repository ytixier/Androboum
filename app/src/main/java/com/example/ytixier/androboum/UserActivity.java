package com.example.ytixier.androboum;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

public class UserActivity extends AppCompatActivity {
    private static final int SELECT_PICTURE = 124;
    //  on choisit une valeur arbitraire pour représenter la connexion
    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(myToolbar);

// on demande une instancedu mécanisme d'authentification
        FirebaseAuth auth = FirebaseAuth.getInstance();
// la méthode ci-dessous renvoi l'utilisateur connecté ou null si personne
        if (auth.getCurrentUser() != null) {
// déjà connecté
            Log.v("AndroBoum", "je suis déjà connecté sous l'email :" + auth.getCurrentUser().getEmail());
            TextView textView = (TextView) findViewById(R.id.email);
            textView.setText(auth.getCurrentUser().getEmail());
        } else {
// on lance l'activité qui gère l'écran de connexion en
//la paramétrant avec les providers googlet et facebook.
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(Arrays.asList(
                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                    new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build()))
                    .build(), 123
            );
        }
        ImageView imageView = (ImageView) findViewById(R.id.imageProfil);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Changer d'image
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                //Capture d'image
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.setAction(Intent.ACTION_PICK);
                Intent chooserIntent = Intent.createChooser(intent, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});
                startActivityForResult(chooserIntent, SELECT_PICTURE);
                uploadImage();

            }
        });

    }


    // cette méthode est appelée quand l'appel StartActivityForResult est terminé
    protected void onActivityResult(
            int requestCode,
            int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

// on vérifie que la réponse est bien liée au code de connexion choisi
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
// Authentification réussie
            if (resultCode == RESULT_OK) {
                Log.v("AndroBoum", "je me suis connecté et mon email est :" + response.getEmail());
                TextView textView = (TextView) findViewById(R.id.email);
                textView.setText(response.getEmail());
                return;

            } else {
// echec de l'authentification
                if (response == null) {
// L'utilisateur a pressé "back", on revient à l'écran
//principalen fermant l'activité
                    Log.v("AndroBoum", "Back Button appuyé");
                    finish();
                    return;
                }
// pas de réseau
                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Log.v("AndroBoum", "Erreur réseau");
                    finish();
                    return;
                }
// une erreur quelconque
                if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Log.v("AndroBoum", "Erreur inconnue");
                    finish();
                    return;
                }
            }
            Log.v("AndroBoum", "Réponse inconnue");
        }

        if (requestCode == SELECT_PICTURE) {
            if (resultCode == RESULT_OK) {
                try {
                    ImageView imageView = (ImageView) findViewById(R.id.imageProfil);
                    boolean isCamera = (data.getData() == null);
                    final Bitmap selectedImage;
                    if (!isCamera) {
                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        selectedImage = BitmapFactory.decodeStream(imageStream);
                    } else {
                        selectedImage = (Bitmap) data.getExtras().get("data");
                    }
// on redimensionne le bitmap pour ne pas qu'il soit trop grand
                    Bitmap finalbitmap = Bitmap.createScaledBitmap(selectedImage, 500, (selectedImage.getHeight() * 500) / selectedImage.getWidth(),
                            false);
                    imageView.setImageBitmap(finalbitmap);
                    downloadImage();
                } catch (Exception e) {
                    Log.v("AndroBoum", e.getMessage());
                }
                ;
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
// Inflate the menu; this adds items to the action bar if it is
        getMenuInflater().inflate(R.menu.actions, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
// choix de l'action "Paramètres", on ne fait rien
// pour l'instant
                return true;

            case R.id.action_logout:
// choix de l'action logout
// on déconnecte l'utilisateur
                //AndroboumApp.setIsConnected(false);
                AuthUI.getInstance().signOut(this);
// on termine l'activité
                finish();
                return true;

            default:
/// aucune action reconnue
                return super.onOptionsItemSelected(item);
        }
    }

    private StorageReference getCloudStorageReference() {
// on va chercher l'email de l'utilisateur connecté
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null)
            return null;
        String email = auth.getCurrentUser().getEmail();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
// on crée l'objet dans le sous-dossier de nom l 'email
        StorageReference photoRef = storageRef.child(email + "/photo.jpg");
        return photoRef;
    }

    private void downloadImage() {
        StorageReference photoRef = getCloudStorageReference();
        if (photoRef == null)
            return;
        ImageView imageView = (ImageView) findViewById(R.id.imageProfil);
// Load the image using Glide
        Glide.with(this/* context */).using(new FirebaseImageLoader())
                .load(photoRef)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.ic_person_black_24dp)
                .into(imageView);
    }

    private void uploadImage() {
        StorageReference photoRef = getCloudStorageReference();
        if (photoRef == null) return;
// on va chercher les données binaires de l'image de profil
        ImageView imageView = (ImageView) findViewById(R.id.imageProfil);
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache();
        Bitmap bitmap = imageView.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
// on lance l'upload
        UploadTask uploadTask = photoRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
// si on est là, échec de l'upload
            }
        }).addOnSuccessListener(
                new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
// ok, l'image est uploadée
// on fait pop un toast d'information
                        Toast toast = Toast.makeText(getApplicationContext(),
                                getString(R.string.imageUploaded), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
    }

}
