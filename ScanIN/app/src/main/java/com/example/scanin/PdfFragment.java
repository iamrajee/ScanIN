package com.example.scanin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

import java.io.File;
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
    TextView textView, textView2, textView3;
    private final CompositeDisposable disposable = new CompositeDisposable();

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

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_quality:
                return true;
            case R.id.nav_rename:
                rename_doc();
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

    private void create_pdf_helper() {
        disposable.add(Single.create(s->{
            SaveFile.createPdfFromDocumentAndImageInfo(getActivity(), documentAndImageInfo);
            s.onSuccess(true);
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s->{

                    Toast.makeText(getContext(), "Successfully saved pdf", Toast.LENGTH_LONG).show();

//                    File savedPDFDirectory = Objects.requireNonNull(getActivity()).getExternalFilesDir("saved_pdf");
//                    String pdf_full_path = savedPDFDirectory + "/" + documentAndImageInfo.getDocument().getDocumentName() + ".pdf";
//                    File fullPathFile = new File (pdf_full_path);
//
//                    Intent intent = new Intent(getContext(), PdfViewerActivity.class);
//                    intent.putExtra(PdfViewerActivity.FILE_URL, fullPathFile.toURI());
//                    intent.putExtra(PdfViewerActivity.FILE_TITLE, documentAndImageInfo.getDocument().getDocumentName());
//                    intent.putExtra(PdfViewerActivity.FILE_DIRECTORY, "*");
//                    intent.putExtra(PdfViewerActivity.ENABLE_FILE_DOWNLOAD, false);
//                    intent.putExtra(PdfViewerActivity.IS_GOOGLE_ENGINE, false);
//
//                    Log.d ("fragmentPDF", "start pdf activity");
//                    startActivity(intent);


                }, Throwable::printStackTrace));

//        try {
//            SaveFile.createPdfFromDocumentAndImageInfo(getActivity(), documentAndImageInfo);
//            Toast.makeText(getContext(), "Successfully saved pdf", Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
//            Toast.makeText(getContext(), "Failed in creating pdf", Toast.LENGTH_SHORT).show();
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_pdf, container, false);
        ((ScanActivity)getActivity()).CurrentMachineState = this.CurrentMachineState;

        pdfFragmentCallback.onCreatePdfCallback();
        //TextView fileName = rootView.findViewById(R.id.file_name_edit);

        create_pdf_helper();

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.my_awesome_toolbar);
        toolbar.inflateMenu(R.menu.pdf_menu);

        if (documentAndImageInfo != null)
            toolbar.setTitle (documentAndImageInfo.getDocument().getDocumentName());

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
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