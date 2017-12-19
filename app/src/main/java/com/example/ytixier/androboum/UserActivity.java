package com.example.ytixier.androboum;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;

public class UserActivity extends AppCompatActivity {

//  on choisit une valeur arbitraire pour représenter la connexion
    private static final int RC_SIGN_IN = 123;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(myToolbar);
        E-mail email =
// on demande une instancedu mécanisme d'authentification
        FirebaseAuth auth = FirebaseAuth.getInstance();
// la méthode ci-dessous renvoi l'utilisateur connecté ou null si personne
        if (auth.getCurrentUser() != null) {
// déjà connecté
            Log.v("AndroBoum", "je suis déjà connecté sous l'email :" +auth.getCurrentUser().getEmail());
        }
        else {
// on lance l'activité qui gère l'écran de connexion en
//la paramétrant avec les providers googlet et facebook.
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(Arrays.asList(
                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                    new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build()))
                    .build(),123
            );
        }
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
                return;
            }
            else {
// echec de l'authentification
                if (response == null) {
// L'utilisateur a pressé "back", on revient à l'écran
//principalen fermant l'activité
                    Log.v("AndroBoum", "Back Button appuyé" );
                    finish();
                    return;
                }
// pas de réseau
                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Log.v("AndroBoum", "Erreur réseau" );
                    finish();
                    return;
                }
// une erreur quelconque
                if (response.getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Log.v("AndroBoum", "Erreur inconnue" );
                    finish();
                    return;
                }
            }
            Log.v("AndroBoum", "Réponse inconnue" );
        }
    }

    public boolean
    onCreateOptionsMenu(Menu menu) {
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

}
