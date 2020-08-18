package com.example.scanin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.scanin.Utils.GenericFileProvider;
import com.github.barteksc.pdfviewer.PDFView;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scanin.DatabaseModule.Document;
import com.example.scanin.DatabaseModule.DocumentAndImageInfo;
import com.example.scanin.DatabaseModule.Repository;
import com.example.scanin.StateMachineModule.MachineActions;
import com.example.scanin.StateMachineModule.MachineStates;
import com.example.scanin.StateMachineModule.StateChangeHelper;
import com.example.scanin.Utils.FileUtils;
import androidx.appcompat.widget.Toolbar;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
//import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.shockwave.pdfium.PdfDocument;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageGridFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PdfFragment extends Fragment implements RecyclerViewGridAdapter.GridAdapterOnClickHandler {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String TAG = "GRID_FRAG";
    private DocumentAndImageInfo documentAndImageInfo;
    int CurrentMachineState = -1;
    private Repository mRepository;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private PDFView pdfView;
    private Integer pageNumber = 0;
    private String pdfFileName;
    private String fullName;
    private Uri pdfUri;
    private ProgressBar progressBar;
    private File fullFile;
    private float qualityFactor = 1.0f;

    public PdfFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ImageGridFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PdfFragment newInstance(String param1, String param2) {
        PdfFragment fragment = new PdfFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    PdfFragmentCallback pdfFragmentCallback;

    public interface PdfFragmentCallback{
        void onCreatePdfCallback();
        void onClickPdfCallback(int action);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        setHasOptionsMenu(true);
        Log.d(TAG, "onCreateCalled");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        setHasOptionsMenu(true);

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
             pdfFragmentCallback = (PdfFragmentCallback) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException(context.toString()
                    + "must implement imageGridFragmentCallback");
        }
    }

    public boolean onOptionsItemSelected(MenuItem item, Toolbar toolbar) {
        switch (item.getItemId()) {
            case R.id.nav_quality:
                Toast.makeText(getContext(), "Higher quality creates larger files", Toast.LENGTH_LONG).show();
                PopupMenu popup = new PopupMenu(Objects.requireNonNull(getContext()), toolbar, Gravity.RIGHT);
                // This activity implements OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.quality_very_low:
                                qualityFactor = 0.5f;
                                return true;
                            case R.id.quality_low:
                                qualityFactor = 0.75f;
                                return true;
                            case R.id.quality_normal:
                                qualityFactor = 1.0f;
                                return true;
                            case R.id.quality_high:
                                qualityFactor = 1.5f;
                                return true;
                            case R.id.quality_very_high:
                                qualityFactor = 2.0f;
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.inflate(R.menu.quality_menu);
                popup.show();
                return true;
            case R.id.nav_rename:
                rename_doc();
                toolbar.setTitle (documentAndImageInfo.getDocument().getDocumentName());
                return true;
            case R.id.nav_grid:
                Log.d ("FragmentPdf", "pdf reorder reached");
                pdfFragmentCallback.onClickPdfCallback(MachineActions.PDF_REORDER);
                return true;
//            case R.id.nav_help:
//                return true;
//            case R.id.nav_rateus:
//                return true;
//            case R.id.nav_settings:
//                return true;
//            case R.id.nav_whatsNew:
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private void rename_doc() {
        AlertDialog.Builder alert = new AlertDialog.Builder(Objects.requireNonNull(getContext()), R.style.AlertDialogCustom);

        final EditText edittext = new EditText(getContext());
        edittext.setTextColor(getResources().getColor(R.color.black));
        edittext.setText(documentAndImageInfo.getDocument().getDocumentName());

        TextView textView = new TextView(getContext());
        textView.setText("Enter Name");
        textView.setPadding(20, 30, 20, 30);
        textView.setTextSize(20F);
        textView.setTextColor(Color.BLACK);
        alert.setCustomTitle(textView);

        alert.setView(edittext);

        alert.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String new_name = edittext.getText().toString();
                if (FileUtils.validateFileName(new_name)) {
                    Document document = documentAndImageInfo.getDocument();
                    ((ScanActivity) Objects.requireNonNull(getActivity())).renameDoc(document, new_name);
                } else {
                    Toast.makeText(getContext(),
                            "Allowed characters A-Z, a-z, 0-9, _, -", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // what ever you want to do with No option.
            }
        });

        alert.show();
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private void displayFromUri(Uri uri) {
        pdfFileName = getFileName(uri);
        pdfView.fromUri(uri)
                .defaultPage(pageNumber)
                .onPageChange(new OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        pageNumber = page;
//                        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
                    }
                })
                .enableAnnotationRendering(true)
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        PdfDocument.Meta meta = pdfView.getDocumentMeta();
                        Log.e(TAG, "title = " + meta.getTitle());
                        Log.e(TAG, "author = " + meta.getAuthor());
                        Log.e(TAG, "subject = " + meta.getSubject());
                        Log.e(TAG, "keywords = " + meta.getKeywords());
                        Log.e(TAG, "creator = " + meta.getCreator());
                        Log.e(TAG, "producer = " + meta.getProducer());
                        Log.e(TAG, "creationDate = " + meta.getCreationDate());
                        Log.e(TAG, "modDate = " + meta.getModDate());

                        printBookmarksTree(pdfView.getTableOfContents(), "-");
                    }
                })
                .scrollHandle(new DefaultScrollHandle(getContext()))
                .spacing(10) // in dp
                .onPageError(new OnPageErrorListener() {
                    @Override
                    public void onPageError(int page, Throwable t) {
                        Log.e(TAG, "Cannot load page " + page);
                    }
                })
                .load();
    }

    protected void share_save_in_gallery () {
        showProgressBar();
        disposable.add(Single.create(s->{
            SaveFile.saveInGallery(getActivity(), documentAndImageInfo);
            s.onSuccess(true);
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s->{
                    Toast.makeText(getContext(), "Successfully saved all images in Pictures", Toast.LENGTH_LONG).show();
                    hideProgressBar();
                }, Throwable::printStackTrace));
    }

    private void create_pdf_helper() {
        showProgressBar();
        disposable.add(Single.create(s->{
            SaveFile.createPdfFromDocumentAndImageInfo(getActivity(), documentAndImageInfo, qualityFactor);
            s.onSuccess(true);
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s->{
                    Toast.makeText(getContext(), "Successfully saved pdf", Toast.LENGTH_LONG).show();
                    displayFromUri(pdfUri);
                    hideProgressBar();
                }, Throwable::printStackTrace));

//        try {
//            SaveFile.createPdfFromDocumentAndImageInfo(getActivity(), documentAndImageInfo);
//            Toast.makeText(getContext(), "Successfully saved pdf", Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
//            Toast.makeText(getContext(), "Failed in creating pdf", Toast.LENGTH_SHORT).show();
//        }
    }

    private void sendViaOtherApps() {
        try {
            Uri pdfURI = GenericFileProvider.getUriForFile(Objects.requireNonNull(getContext()),
                    getContext().getApplicationContext().getPackageName() + ".provider", fullFile);
            Log.d ("fragmentPdf", "pdfURI = " + pdfURI.toString());
            String subject = documentAndImageInfo.getDocument().getDocumentName();
            String message = documentAndImageInfo.getDocument().getDocumentName() + "\n" + "Please find attachment" +
                    "\n\nCreated with ScanIN";
            final Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("application/pdf");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT,subject);
            sendIntent.putExtra(Intent.EXTRA_STREAM, pdfURI);
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sendIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            sendIntent.putExtra(Intent.EXTRA_EMAIL, message);
            // Verify that the intent will resolve to an activity
            if (sendIntent.resolveActivity(
                    Objects.requireNonNull(getActivity()).getPackageManager()) != null) {
                this.startActivity(Intent.createChooser(sendIntent,"Sending ..."));
            } else {
                throw new Exception("Could not resolve activity.");
            }
        } catch (Throwable t) {
            Toast.makeText(getContext(),
                    "Request failed try again: " + t.toString(),
                    Toast.LENGTH_LONG).show();
        }

    }

    private void openViaOtherApps() {
        try {
            Uri pdfURI = GenericFileProvider.getUriForFile(Objects.requireNonNull(getContext()),
                    getContext().getApplicationContext().getPackageName() + ".provider", fullFile);
            Log.d ("fragmentPdf", "pdfURI = " + pdfURI.toString());
//            String subject = documentAndImageInfo.getDocument().getDocumentName();
//            String message = documentAndImageInfo.getDocument().getDocumentName() + "\n" + "Please find attachment" +
//                    "\n\nCreated with ScanIN";
            final Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.setDataAndType(pdfURI, "application/pdf");
//            sendIntent.putExtra(Intent.EXTRA_SUBJECT,subject);
            sendIntent.putExtra(Intent.EXTRA_STREAM, pdfURI);
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sendIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//            sendIntent.putExtra(Intent.EXTRA_EMAIL, message);
            // Verify that the intent will resolve to an activity
            if (sendIntent.resolveActivity(
                    Objects.requireNonNull(getActivity()).getPackageManager()) != null) {
                this.startActivity(Intent.createChooser(sendIntent,"Sending ..."));
            } else {
                throw new Exception("Could not resolve activity.");
            }
        } catch (Throwable t) {
            Toast.makeText(getContext(),
                    "Request failed try again: " + t.toString(),
                    Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_pdf, container, false);
        ((ScanActivity)getActivity()).CurrentMachineState = this.CurrentMachineState;

        pdfView = rootView.findViewById(R.id.pdfView);
        pdfView.isBestQuality();
        pdfFragmentCallback.onCreatePdfCallback();

        progressBar = rootView.findViewById(R.id.progressBarPdf);

        File savedPDFDirectory = getActivity().getExternalFilesDir("saved_pdf");
        fullName = savedPDFDirectory + "/" +
                documentAndImageInfo.getDocument().getDocumentName() + ".pdf";
        fullFile = new File(fullName);
        pdfUri = Uri.fromFile(fullFile);

        if (fullFile.exists()) {
            displayFromUri(pdfUri);
        } else {
            create_pdf_helper();
        }

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.my_awesome_toolbar);
        toolbar.inflateMenu(R.menu.pdf_menu);

        if (documentAndImageInfo != null)
            toolbar.setTitle (documentAndImageInfo.getDocument().getDocumentName());

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item, toolbar);
            }
        });

        rootView.findViewById(R.id.nav_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pdfFragmentCallback.onClickPdfCallback(MachineActions.PDF_GO_HOME);
            }
        });

        rootView.findViewById(R.id.nav_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pdfFragmentCallback.onClickPdfCallback(MachineActions.EDIT_PDF);
            }
        });

        rootView.findViewById(R.id.nav_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(Objects.requireNonNull(getContext()), view);

                // This activity implements OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.share_other_apps:
                                sendViaOtherApps();
                                return true;
                            case R.id.share_save_all:
                                share_save_in_gallery();
                                return true;
                            case R.id.share_open_apps:
                                openViaOtherApps();
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.inflate(R.menu.share_menu);
                popup.show();
            }
        });

        ImageButton nav_reload = rootView.findViewById(R.id.nav_reload);
        nav_reload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                create_pdf_helper();
            }
        });

        return rootView;
    }

    @Override
    public void onClick(int action) {
//        ScanActivity scanActivity = (ScanActivity)getActivity();
//        scanActivity.imageEditFragment.setImagePathList(documentAndImageInfo);
        pdfFragmentCallback.onClickPdfCallback(action);
    }

    @Override
    public void onDestroyView() {
        Log.d("Scan-Activity2", "imageFragmentDestroyed");

        super.onDestroyView();
    }

    public void setImagePathList(DocumentAndImageInfo documentAndImageInfo) {
        this.documentAndImageInfo = documentAndImageInfo;
    }

    public void setCurrentMachineState(int currentMachineState) {
        this.CurrentMachineState = currentMachineState;
    }
}