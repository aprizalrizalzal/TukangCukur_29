package com.bro.barbershop.ui.report.pdf;

import static com.bro.barbershop.ui.report.pdf.PdfFragment.EXTRA_PDF;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bro.barbershop.R;
import com.bro.barbershop.databinding.FragmentDetailPdfBinding;
import com.bro.barbershop.model.report.pdf.Pdf;
import com.bro.barbershop.utils.progressBar.CustomProgressDialog;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class DetailPdfFragment extends Fragment {
    private FragmentDetailPdfBinding binding;
    private View view;
    private CustomProgressDialog progressDialog;
    private Pdf extraPdf;
    private PDFView pdfView;
    public DetailPdfFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentDetailPdfBinding.inflate(getLayoutInflater(), container, false);
        view = binding.getRoot();

        progressDialog = new CustomProgressDialog(requireActivity());

        if (getArguments() != null) {
            extraPdf = getArguments().getParcelable(EXTRA_PDF);
        }

        if (extraPdf != null) {
            changeReportPdf(extraPdf);
        } else {
            Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
        }

        binding.fab.setOnClickListener(v -> changeDownloadReportPdf(extraPdf));

        pdfView = binding.pdfView;

        return  view;
    }
    private void changeReportPdf(Pdf extraPdf) {
        progressDialog.ShowProgressDialog();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            InputStream inputStream = null;
            try {
                URL url = new URL(extraPdf.getPdfUrl());
                HttpURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = new BufferedInputStream(urlConnection.getInputStream());
                }

            } catch (IOException e) {
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }

            InputStream finalInputStream = inputStream;
            handler.post(() -> pdfView.fromStream(finalInputStream).scrollHandle(new DefaultScrollHandle(requireContext())).onLoad(loadPages -> progressDialog.DismissProgressDialog()).onError(e -> {
                progressDialog.DismissProgressDialog();
                Toast.makeText(requireContext(), R.string.failed, Toast.LENGTH_SHORT).show();
            }).load());
        });

    }

    private void changeDownloadReportPdf(Pdf extraPdf) {
        DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(extraPdf.getPdfUrl());
        DownloadManager.Request request = new DownloadManager.Request(uri);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, extraPdf.getPdfId());

        downloadManager.enqueue(request);
    }
}