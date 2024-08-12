package com.example.applogin;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ArchivoAdapter3 extends RecyclerView.Adapter<ArchivoAdapter3.ViewHolder> {

    private Context context;
    private List<Archivo> listaArchivos;
    private OnArchivoClickListener listener;

    public ArchivoAdapter3(Context context, List<Archivo> listaArchivos) {
        this.context = context;
        this.listaArchivos = listaArchivos;
    }

    public interface OnArchivoClickListener {
        void onArchivoClick(Archivo archivo);
        void onArchivoLongClick(Archivo archivo);
    }

    public void setOnArchivoClickListener(OnArchivoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archivo_3, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Archivo archivo = listaArchivos.get(position);
        holder.nombreArchivo.setText(archivo.getNombre());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onArchivoClick(archivo); // Pasar el archivo al hacer clic
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onArchivoLongClick(archivo);
            }
            return true; // Indica que el evento de clic largo está consumido
        });
    }

    @Override
    public int getItemCount() {
        return listaArchivos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nombreArchivo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombreArchivo = itemView.findViewById(R.id.nombreArchivo);
        }
    }


    public void ver(String urlDescarga){
        //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlDescarga));
        //context.startActivity(intent);

        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_view_file);
        dialog.setTitle("Vista Previa de Archivo");

        // Obtener el WebView del diálogo
        WebView webViewFile = dialog.findViewById(R.id.webViewFile);
        webViewFile.getSettings().setJavaScriptEnabled(true);
        WebSettings webSettings = webViewFile.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webViewFile.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        // Cargar el URL en el WebView
        webViewFile.loadUrl(urlDescarga);

        // Mostrar el diálogo
        dialog.show();

    }

    public void descargar(String urlDescarga) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlDescarga));
        request.setTitle("Descargando archivo");
        request.setDescription("Por favor, espere...");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(urlDescarga).getLastPathSegment());

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Toast.makeText(context, "Descarga iniciada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "No se pudo iniciar la descarga", Toast.LENGTH_SHORT).show();
        }
        //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlDescarga));
        //context.startActivity(intent);
    }



}
