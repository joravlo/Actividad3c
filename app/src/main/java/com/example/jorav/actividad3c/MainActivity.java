package com.example.jorav.actividad3c;

import android.content.Intent;
import android.content.IntentSender;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiActivity;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{
    private GoogleApiClient googleApiClient;
    protected static final int REQ_CREATE_FILE = 1;
    protected static final int REQ_OPEN_FILE = 2;

    Button button, button2;
    EditText editText;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.button2);
        button2 = (Button) findViewById(R.id.button4);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .build();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                crearFichero();

            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirFichero();
            }
        });
    }

    //Clase que obtendra el texto del EditText y lo escribira en el fichero
    private void ecribirFichero(DriveContents driveContents) {
        OutputStream outputStream = driveContents.getOutputStream();
        Writer writer = new OutputStreamWriter(outputStream);
        try {
            writer.write(editText.getText().toString());
            writer.close();
        } catch (IOException e) {
            e.getMessage();
        }
    }

    //Clase que creara el fichero que guardaremos en Drive
    private void crearFichero() {

        Drive.DriveApi.newDriveContents(googleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        MetadataChangeSet changeSet =
                                new MetadataChangeSet.Builder()
                                        .setMimeType("text/plain")
                                        .build();

                        //Escribimos el fichero
                        ecribirFichero(result.getDriveContents());

                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(changeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(googleApiClient);

                        try {
                            startIntentSenderForResult(
                                    intentSender, REQ_CREATE_FILE, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {

                        }
                    }
                });
    }

    //Clase que proporciona un DriveId segun el fichero
    private void abrirFichero() {
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[] { "text/plain" })
                .build(googleApiClient);

        try {
            startIntentSenderForResult(
                    intentSender, REQ_OPEN_FILE, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {

        }
    }

    //Clase que recupera el contenido de un fichero
    private void leerFichero(DriveId fileDriveId) {

        DriveFile file = fileDriveId.asDriveFile();

        file.open(googleApiClient, DriveFile.MODE_READ_ONLY, null)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e("Error","Error al abrir fichero (readFile)");
                            return;
                        }

                        DriveContents contents = result.getDriveContents();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(contents.getInputStream()));
                        StringBuilder builder = new StringBuilder();
                        try {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                builder.append(line);
                            }
                        } catch (IOException e) {
                            Log.e("Error","Error al leer fichero");
                        }
                        textView.setText(builder.toString());
                        contents.discard(googleApiClient);

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_CREATE_FILE:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                }
                break;
            case REQ_OPEN_FILE:
            if (resultCode == RESULT_OK) {
                //Recuperamos el driveId y se lo pasamos a leerFichero para poder leer el contenido
                DriveId driveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                leerFichero(driveId);
            }
            break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Error en la conexión", Toast.LENGTH_SHORT).show();
    }
}
