package com.example.scanin.HomeModule;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scanin.DatabaseModule.Document;
import com.example.scanin.DatabaseModule.DocumentPreview;
import com.example.scanin.R;
import com.example.scanin.ScanActivity;
import com.example.scanin.StateMachineModule.MachineActions;
import com.example.scanin.StateMachineModule.MachineStates;
import com.example.scanin.Utils.FileUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, DocAdapterClickListener{

    private FloatingActionButton btnTakePicture;
    private ImageButton btnSavePicture;
    private ImageView capturePreview;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private HomeViewModel homeViewModel;

    public static final int CAMERA_ACTIVITY_REQUEST_CODE = 0;
    public static final int CAMERA_IMAGE_REQUEST_CODE = 1000;
    public static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 2000;

    private static String TAG="MainActivity";
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java4");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);

        RecyclerView recyclerView = findViewById(R.id.recyclerview_doc);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerViewDocAdapter mAdapter = new RecyclerViewDocAdapter(this);
        recyclerView.setAdapter(mAdapter);

        homeViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication()))
                            .get(HomeViewModel.class);
        homeViewModel.getmDocPreview().observe(this, new Observer<List<DocumentPreview>>() {
            @Override
            public void onChanged(List<DocumentPreview> documentPreviews) {
                mAdapter.setmDataset((ArrayList<DocumentPreview>) documentPreviews);
            }
        });

        btnTakePicture = (FloatingActionButton) findViewById(R.id.fab);
        btnTakePicture.setOnClickListener(MainActivity.this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab) {
            view.setClickable(false);
            startCameraActivity();
        }
    }

    public void startCameraActivity(){
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra("STATE", MachineStates.HOME);
        intent.putExtra("ACTION", MachineActions.HOME_ADD_SCAN);
        startActivity(intent);
    }

    @Override
    public void onClick(View view, DocumentPreview clickedItem) {
        findViewById(R.id.recyclerview_doc).setClickable(false);
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra("CURRENT_DOCUMENT_ID", clickedItem.getDocument().getDocumentId());
        intent.putExtra("STATE", MachineStates.HOME);
        intent.putExtra("ACTION", MachineActions.HOME_OPEN_DOC);
        startActivity(intent);
    }

    @Override
    public void openPdfView(View view, DocumentPreview clickedItem) {
        findViewById(R.id.recyclerview_doc).setClickable(false);
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra("CURRENT_DOCUMENT_ID", clickedItem.getDocument().getDocumentId());
        intent.putExtra("STATE", MachineStates.HOME);
        intent.putExtra("ACTION", MachineActions.HOME_OPEN_PDF);
        startActivity(intent);
    }

    @Override
    public void deleteDoc(View view, DocumentPreview documentPreview) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.AlertDialogCustom);

        TextView textView = new TextView(this);
        textView.setText("Are You Sure");
        textView.setPadding(20, 30, 20, 30);
        textView.setTextSize(20F);
        textView.setTextColor(Color.BLACK);
        alert.setCustomTitle(textView);

        alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                homeViewModel.deleteDoc(documentPreview);
                Toast.makeText(MainActivity.this, "Doc Deleted", Toast.LENGTH_SHORT).show();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // what ever you want to do with No option.
            }
        });

        alert.show();
    }

    @Override
    public void renameDoc(View view, DocumentPreview documentPreview) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.AlertDialogCustom);

        final EditText edittext = new EditText(this);
        edittext.setTextColor(getResources().getColor(R.color.black));
        edittext.setText(documentPreview.getDocument().getDocumentName());

        TextView textView = new TextView(this);
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
                    Document document = documentPreview.getDocument();
                    document.setDocumentName(new_name);
                    homeViewModel.updateDoc(document);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Allowed characters A-Z, a-z, 0-9, _, - ", Toast.LENGTH_LONG)
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

    @Override
    public void onLongClick(View view, int position) {
//        Document document = documentsAndFirstImages.get(position).getDocument();
        view.setBackgroundColor(Color.parseColor("#1C69E1"));
    }

    @Override
    protected void onDestroy() {
        Log.d("Main-Activity", "OnDestroyCalled");
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.d("Main-Activity", "OnPauseCalled");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("Main-Activity", "OnStopCalled");
        super.onStop();
    }

    @Override
    protected void onStart() {
        Log.d("Main-Activity", "OnStartCalled");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d("Main-Activity", "OnResumeCalled");
        findViewById(R.id.fab).setClickable(true);
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                return;
            }
        }
    }

    @Override
    protected void onRestart() {
        Log.d("Main-Activity", "OnRestartCalled");
        super.onRestart();
    }
}
