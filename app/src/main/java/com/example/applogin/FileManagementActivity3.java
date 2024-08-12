package com.example.applogin;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class FileManagementActivity3 extends AppCompatActivity implements ArchivoAdapter3.OnArchivoClickListener {

    private static final int REQUEST_CODE_PICK_FILE = 101;

    private Button btnSelectFile, btnUploadFile;
    private RecyclerView recyclerViewFiles;

    private Uri fileUri;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private DatabaseReference databaseRef;
    private ArchivoAdapter3 archivoAdapter;
    private List<Archivo> listaArchivos;

    String userId = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_management3);


        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference("usuarios").child(userId).child("archivos");

        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        recyclerViewFiles = findViewById(R.id.recyclerViewFiles);

        listaArchivos = new ArrayList<>();
        archivoAdapter = new ArchivoAdapter3(this, listaArchivos);
        archivoAdapter.setOnArchivoClickListener(this); // Configurar el listener en la actividad
        recyclerViewFiles.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFiles.setAdapter(archivoAdapter);

        btnSelectFile.setOnClickListener(v -> selectFile());
        btnUploadFile.setOnClickListener(v -> uploadFile());

        // Escuchar cambios en la base de datos de Firebase y actualizar la lista de archivos
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listaArchivos.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Archivo archivo = snapshot.getValue(Archivo.class);
                    archivo.setId(snapshot.getKey()); // Setear el ID del archivo desde la base de datos
                    listaArchivos.add(archivo);
                }
                archivoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FileManagementActivity3.this, "Error al leer archivos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            String fileName = getFileName(fileUri);
            TextView verseleccion = (TextView) findViewById(R.id.textViewSelectFile);
            verseleccion.setText("Seleciono: "+fileName);
        }
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private void uploadFile() {
        if (fileUri != null) {
            StorageReference storageRef = storage.getReference();
            String fileName = getFileName(fileUri);
            //String userId = mAuth.getCurrentUser().getUid();
            StorageReference fileRef = storageRef.child("archivos/" + userId + "/" + fileName);

            fileRef.putFile(fileUri).addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();

                    // Crear un objeto Archivo con nombre y URL
                    Archivo archivo = new Archivo(fileName, downloadUrl);

                    // Obtener un ID único para el archivo en la base de datos
                    String uploadId = databaseRef.push().getKey();

                    // Asignar el ID generado al objeto Archivo
                    archivo.setId(uploadId);

                    // Guardar el objeto Archivo en la base de datos
                    databaseRef.child(uploadId).setValue(archivo);

                    Toast.makeText(FileManagementActivity3.this, "Archivo subido correctamente", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(FileManagementActivity3.this, "Error al obtener URL de descarga: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(exception -> {
                Toast.makeText(FileManagementActivity3.this, "Error al subir el archivo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(FileManagementActivity3.this, "Selecciona un archivo primero", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onArchivoClick(Archivo archivo) {
        // Mostrar el ID del archivo en un Toast al hacer clic
        Toast.makeText(this, "ID del archivo: " + archivo.getId(), Toast.LENGTH_SHORT).show();
        //mostrarOpcionesArchivo(archivo);
    }

    @Override
    public void onArchivoLongClick(Archivo archivo) {
        // Implementación opcional para manejar el clic largo en un archivo
        //eliminarArchivoDeBaseDeDatos(archivo.getId());
        //archivoAdapter.descargar(archivo.getUrlDescarga());
        mostraropciones(archivo);
    }

    private void eliminarArchivoDeBaseDeDatos(String archivoId) {
        databaseRef.child(archivoId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(FileManagementActivity3.this, "Archivo eliminado de la base de datos", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(FileManagementActivity3.this, "Error al eliminar el archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void mostrarOpcionesArchivo(Archivo archivo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opciones de Archivo")
                .setMessage("Selecciona una acción para el archivo: " + archivo.getNombre())
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarArchivoDeBaseDeDatos(archivo.getId()))
                .setNeutralButton("Ver Archivo", (dialog, which) -> archivoAdapter.ver(archivo.getUrlDescarga()))
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void mostraropciones(Archivo archivo){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Elige una acción")
                .setItems(new CharSequence[]{"Eliminar", "Ver Archivo", "Descargar", "Cancelar"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // Acción para la opción 1
                                eliminarArchivoDeBaseDeDatos(archivo.getId());
                                break;
                            case 1:
                                // Acción para la opción 2
                                archivoAdapter.ver(archivo.getUrlDescarga());
                                break;
                            case 2:
                                // Acción para la opción 3
                                archivoAdapter.descargar(archivo.getUrlDescarga());
                                break;
                            case 3:
                                // Acción para la opción 4
                                break;
                        }
                    }
                });

        // Crear y mostrar el diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}