package com.example.scanin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;

import com.example.scanin.DatabaseModule.AppDatabase;
import com.example.scanin.DatabaseModule.Document;
import com.example.scanin.DatabaseModule.DocumentAndImageInfo;
import com.example.scanin.DatabaseModule.ImageInfo;
import com.example.scanin.DatabaseModule.Repository;
import com.example.scanin.ImageDataModule.ImageData;
import com.example.scanin.ImageDataModule.ImageEditUtil;
import com.example.scanin.StateMachineModule.MachineActions;
import com.example.scanin.StateMachineModule.MachineStates;
import com.example.scanin.StateMachineModule.StateChangeHelper;
import com.example.scanin.StateMachineModule.StateMachine;
import com.google.common.util.concurrent.ListenableFuture;
import com.example.scanin.Utils.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.opencv.android.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.collections.ArraysKt;
import kotlin.jvm.internal.Intrinsics;

public class ScanActivity extends AppCompatActivity
        implements ImageGridFragment.ImageGridFragmentCallback, ImageEditFragment.ImageEditFragmentCallback,
        PdfFragment.PdfFragmentCallback {
    public ImageGridFragment imageGridFragment = null;
    public ImageEditFragment imageEditFragment = null;
    public PdfFragment pdfFragment = null;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Preview preview;
    ImageCapture imageCapture = null;
    private File outputDirectory;
    private Camera camera = null;
    public int CurrentMachineState = -1;
    PreviewView previewView;
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    FrameLayout fragment_cover;
    CameraSelector cameraSelector;
    String TAG = "Scan-Activity";

    private AppDatabase appDatabase;
    private String documentName = null;
    private long current_document_id = -1;
    private Repository repository;

    //Schedulers for Image and Database
    private Scheduler preview_executor = Schedulers.newThread();
    private final CompositeDisposable disposable = new CompositeDisposable();

    public DocumentAndImageInfo documentAndImageInfo;
    public int SELECT_PICTURES = 1234;

    public Repository getRepository () {
        return this.repository;
    }

    // It makes more sense to use a custom gallery, maybe next version.
    // https://github.com/mitchtabian/Android-Instagram-Clone/blob/master/app/src/main/java/tabian/com/instagramclone2/Share/PhotoFragment.java
    // https://stackoverflow.com/questions/38457497/instagram-like-scroll-in-custom-gallery
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_PICTURES) {
            if(resultCode == Activity.RESULT_OK) {
                if(data.getClipData() != null) {
                    int count = data.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                    List<Uri> uri_list = new LinkedList<>();

                    for(int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();

                        File photoFile = new File(
                                outputDirectory, new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + "_"
                                + String.valueOf(i) + ".jpg");
                        try {
                            photoFile.createNewFile();

                            disposable.add(Single.create(e -> {
                                FileUtils.copyFile(this, photoFile, imageUri);
                                e.onSuccess(true);
                            })
                                    .subscribeOn(Schedulers.single())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(s -> {

                                            },
                                            e -> {
                                                throw new Exception(e);
                                            }));
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Copy error: " + e.toString(), Toast.LENGTH_LONG).show();
                            Log.e ("ScanActivity", e.getMessage());
                            onBackPressed();
                        }

                        uri_list.add(Uri.fromFile(photoFile));
                    }
                    int position = 0;
                    if (documentAndImageInfo != null) {
                        position = documentAndImageInfo.getImages().size();
                    }
                    Toast.makeText(this, "Saving images. Please wait...", Toast.LENGTH_LONG).show();
                    saveImageInfo(uri_list, position);
                }
            } else if(data.getData() != null) {
                String imagePath = data.getData().getPath();
                //do something with the image (save it to some directory or whatever you need to do with it here)
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);
        ImageButton flash_button = findViewById(R.id.flash_button);

        int state = getIntent().getIntExtra("STATE", -1);
        int action = getIntent().getIntExtra("ACTION", -1);

        imageGridFragment = new ImageGridFragment();
        imageEditFragment = new ImageEditFragment();
        pdfFragment = new PdfFragment();

        cameraProviderFuture = ProcessCameraProvider.getInstance((Context)this);
        outputDirectory = getOutputDirectory();
        appDatabase = AppDatabase.getInstance(this);
        repository = new Repository(this.getApplication(), this);
        CurrentMachineState = MachineStates.HOME;

        if(action == MachineActions.HOME_ADD_SCAN){
            Log.d(TAG, "entered_add_scan");
            startCamera();
        }else if(action == MachineActions.HOME_OPEN_DOC){
            Log.d("Edit2_1", "open edit 2");
            current_document_id = getIntent().getLongExtra("CURRENT_DOCUMENT_ID", -1);
            Log.d("Edit2_1", String.valueOf(current_document_id));
            readDocumentImages(current_document_id);
        }else if(action == MachineActions.HOME_OPEN_PDF){
            Log.d("PDF2", "open pdf 2");
            current_document_id = getIntent().getLongExtra("CURRENT_DOCUMENT_ID", -1);
            openPdfView(current_document_id);
        }else{
            finish();
        }

        findViewById(R.id.grid_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setClickable(false);
                StateChangeHelper.CameraActionChange(MachineActions.CAMERA_EDIT_GRID, ScanActivity.this);
            }
        });

        findViewById(R.id.add_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURES);
            }
        });

        findViewById(R.id.camera_capture_button).setOnClickListener(view -> {
            view.setClickable(false);
            if(imageCapture == null){
                return;
            }
            takePhoto();
        });

        flash_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imageCapture == null){
                    return;
                }
                if(imageCapture.getFlashMode() == ImageCapture.FLASH_MODE_OFF) {
                    imageCapture.setFlashMode(ImageCapture.FLASH_MODE_ON);
                    flash_button.setImageResource(R.drawable.ic_flashon);
                }
                else{
                    imageCapture.setFlashMode(ImageCapture.FLASH_MODE_OFF);
                    flash_button.setImageResource(R.drawable.ic_flashoff);
                }
            }
        });
    }

    //start camera
    public void startCamera(){
        CurrentMachineState = MachineStates.CAMERA;
        cameraProviderFuture.addListener(() ->{
            try{
                Log.d("Camera-1", "bindPreview1");
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();
                bindPreview(cameraProvider);
            }catch (ExecutionException | InterruptedException e){

                Log.e("CameraFragment", "cameraProviderFuture.get() Failed", e);
            }
        }, ContextCompat.getMainExecutor((Context)this));
    }

    @SuppressLint("RestrictedApi")
    public void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
//        cameraProvider.unbindAll();
        Log.d("Camera-1", "bindPreview2");
        previewView = (PreviewView) findViewById(R.id.view_finder);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(displayMetrics);

        Log.d("Camera-1", "DisplayMetrics: heightPx " + displayMetrics.heightPixels +
                ", widthPx " + displayMetrics.widthPixels);

        preview = new Preview.Builder()
//                .setTargetResolution(new Size(768, 1024))
                .setTargetAspectRatioCustom(new Rational(3, 4))
                .build();

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//                .setTargetResolution(new Size(768, 1024))
                .setTargetAspectRatioCustom(new Rational(3, 4))
                .setTargetRotation(previewView.getDisplay().getRotation()).build();

        cameraSelector = new CameraSelector.Builder()
                .build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        this.camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);


//        CameraControl cameraControl = camera.getCameraControl();
//        final MeteringPointFactory factory = previewView.createMeteringPointFactory(cameraSelector);
//        final MeteringPoint point = factory.createPoint(event.getX(), event.getY());
//        FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
//                .addPoint(point, FocusMeteringAction.FLAG_AE) // could have many
//                // auto calling cancelFocusAndMetering in 5 seconds
//                .setAutoCancelDuration(5, TimeUnit.SECONDS)
//                .build();
//        cameraControl.startFocusAndMetering(action);

//        ListenableFuture future = cameraControl.startFocusAndMetering(action);
//        future.addListener( () -> {
//            try {
//                FocusMeteringResult result = (FocusMeteringResult) future.get();
//                // process the result
//            } catch (Exception e) {
//            }
//        } , executor);

        Log.d(TAG, "Camera bind done");
    }

    private void takePhoto(){
        if(imageCapture == null) return;
        File photoFile = new File(
                outputDirectory, new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(
                outputFileOptions, Executors.newSingleThreadExecutor(), new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
//                        imageEditFragment = new ImageEditFragment();
                        Uri savedUri = Uri.fromFile(photoFile);
                        Log.d("CameraSaved","Called");
                        long position = 0;
                        if(documentAndImageInfo != null) position = documentAndImageInfo.getImages().size();
                        saveImageInfo(savedUri, position, true);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exception);
                    }
                }
        );
    }

    @NotNull
    public File getOutputDirectory() {
        File[] mediaDir = getExternalMediaDirs();
        Intrinsics.checkExpressionValueIsNotNull(mediaDir, "externalMediaDirs");
        File fileDir = (File) ArraysKt.firstOrNull(mediaDir);
        if(fileDir != null){
            File tempDir =fileDir;
            File tempDir1 = (new File(tempDir, String.valueOf(R.string.app_name)));
            tempDir1.mkdirs();
            fileDir = tempDir1;
        }

        if(fileDir != null && fileDir.exists()){
            return fileDir;
        }
        else{
            File tempDir =this.getFilesDir();
            Intrinsics.checkExpressionValueIsNotNull(tempDir, "fileDir");
            return tempDir;
        }
    }

    @Override
    public void onBackPressed() {
        int nextState = StateMachine.getNextState(CurrentMachineState, MachineActions.BACK);
        if(nextState == MachineStates.ABORT){
            CurrentMachineState = -1;
            super.onBackPressed();
            overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
        }else{
            StateChangeHelper.AnonymousActionChange(this.CurrentMachineState, MachineActions.BACK, ScanActivity.this);
        }
    }

    public void pdfGoHome() {
        Log.d ("ScanActvity", "pdf go home");
        CurrentMachineState = -1;
        super.onBackPressed();
        overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
    }

    @Override
    public void onCreateGridCallback() {
        Log.d("onCreateGrid", "Called");
        imageGridFragment.setImagePathList(documentAndImageInfo);
    }

    @Override
    public void onCreatePdfCallback() {
        Log.d("onCreatePdf", "Called");
        pdfFragment.setImagePathList(documentAndImageInfo);
    }

    @Override
    public void onCreateEditCallback() {
        Log.d(TAG, "Pos1: " + String.valueOf(imageEditFragment.adapterPosition));
        imageEditFragment.setImagePathList(documentAndImageInfo);
//        if(imageEditFragment.adapterPosition != null && imageEditFragment.adapterPosition >=0){
//            Log.d(TAG, "Pos: " + String.valueOf(imageEditFragment.adapterPosition));
//            imageEditFragment.recyclerView.scrollToPosition(imageEditFragment.adapterPosition);
//        }
    }

    @Override
    public void onClickPdfCallback (int action) {
        StateChangeHelper.PdfActionChange(action, ScanActivity.this);
    }

    @Override
    public void onClickEditCallback(int action) {
        StateChangeHelper.EditActionChange(action, ScanActivity.this);
    }

    public void editDeleteImageCallback(int position){
        ImageInfo tempImageInfo = documentAndImageInfo.getImages().get(position);
        deleteImageFromFile(tempImageInfo.getUri());
        documentAndImageInfo.getImages().remove(position);
        imageEditFragment.setImagePathList(documentAndImageInfo);
        if(documentAndImageInfo.getImages().size() <= 1){
            repository.deleteDocument(documentAndImageInfo.getDocument());
        }
        else repository.deleteImage(tempImageInfo);
    }

    public void renameDoc (Document document, String new_name) {
        document.setDocumentName(new_name);
        repository.updateDocument(document, disposable);
    }

    @Override
    public void onClickGridCallback(int action, Integer position) {
        StateChangeHelper.GridActionChange(action, ScanActivity.this, position);
    }

    public void setCamera(int nextState){
        CurrentMachineState = nextState;
        findViewById(R.id.camera_capture_button).setClickable(true);
        findViewById(R.id.grid_button).setClickable(true);
    }

    public void readImageFromFile(Uri uri) {
        disposable.add(Single.create(e -> {
//            File file = new File(Objects.requireNonNull(uri.getPath()));
//            if (!file.exists()) e.onSuccess(null);
//            e.onSuccess(BitmapFactory.decodeFile(file.getAbsolutePath()));
            e.onSuccess(null);
        })
        .subscribeOn(preview_executor)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(s -> {
//            imageData.add(new ImageData((Bitmap) s));
            int nextState = StateMachine.getNextState(CurrentMachineState, MachineActions.CAMERA_CAPTURE_PHOTO);
            imageEditFragment.setCurrentMachineState(nextState);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_edit, imageEditFragment)
                    .commit();
        },
        e -> {
        }));
    }

    public void saveImageInfo (List <Uri> uri_list, long position) {
        if (current_document_id != -1) {
            int cnt = uri_list.size();
            for (int i = 0; i < cnt; i++) {
                saveImageInfo(uri_list.get(i), position + i, (boolean) (i == cnt - 1));
            }
        }
        else{
            disposable.add(Single.create(s->{
                Document document = createDocument();
                Log.d(TAG, "new_doc_saved");
                ImageInfo imageInfo = new ImageInfo(document.getDocumentId(), uri_list.get(0), position);
                saveImageInfoHelper(imageInfo);
                s.onSuccess(new DocumentAndImageInfo(document, imageInfo));
            })
                    .subscribeOn(Schedulers.single())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s->{
                        documentAndImageInfo = (DocumentAndImageInfo) s;
                        current_document_id = documentAndImageInfo.getDocument().getDocumentId();
                        int cnt = uri_list.size();
                        for (int i = 1; i < cnt; i++) {
                            saveImageInfo(uri_list.get(i), position + i, (boolean) (i == cnt - 1));
                        }
                    }, Throwable::printStackTrace));
        }
    }

    public void saveImageInfo(Uri uri, long position, boolean edit_mode) {
        if (current_document_id != -1) {
            disposable.add(Single.create(s -> {
                ImageInfo imageInfo = new ImageInfo(current_document_id, uri, position);
                saveImageInfoHelper(imageInfo);
                s.onSuccess(imageInfo);
            })
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(s->{
                documentAndImageInfo.images.add((ImageInfo) s);
                if (edit_mode)
                    StateChangeHelper.CameraActionChange(MachineActions.CAMERA_CAPTURE_PHOTO, ScanActivity.this);
            }, Throwable::printStackTrace));
        }
        else{
            disposable.add(Single.create(s->{
                Document document = createDocument();
                Log.d(TAG, "new_doc_saved");
                ImageInfo imageInfo = new ImageInfo(document.getDocumentId(), uri, position);
                saveImageInfoHelper(imageInfo);
                s.onSuccess(new DocumentAndImageInfo(document, imageInfo));
            })
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(s->{
                documentAndImageInfo = (DocumentAndImageInfo) s;
                current_document_id = documentAndImageInfo.getDocument().getDocumentId();
                if (edit_mode)
                    StateChangeHelper.CameraActionChange(MachineActions.CAMERA_CAPTURE_PHOTO, ScanActivity.this);
            }, Throwable::printStackTrace));
        }
    }

    public void saveImageInfoHelper(ImageInfo imageInfo){
        appDatabase.imageInfoDao().insertImageInfo(imageInfo);
    }

    public Document createDocument(){
        String document_name = "Unname101";
        long id = appDatabase.documentDao().insertDocument(new Document(document_name));
        documentName = "doc_" + id;
        Document document = new Document(id, documentName);
        renameDoc(document, documentName);
        return document;
    }

    public void openPdfView (long id) {
        disposable.add(Single.create(s->{
            DocumentAndImageInfo temp = appDatabase.documentAndImageDao().loadDocumentAllImageInfo(id);
            s.onSuccess(temp);
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s->{
                    documentAndImageInfo = (DocumentAndImageInfo) s;
                    Log.d("Pdf2, ", String.valueOf(documentAndImageInfo.getImages().size()
                            + String.valueOf(documentAndImageInfo.getDocument().getDocumentId())));
                    StateChangeHelper.HomeActionChange(MachineActions.HOME_OPEN_PDF, ScanActivity.this);
                }, Throwable::printStackTrace));
    }

    public void readDocumentImages(long id){
        disposable.add(Single.create(s->{
            DocumentAndImageInfo temp = appDatabase.documentAndImageDao().loadDocumentAllImageInfo(id);
            s.onSuccess(temp);
        }).subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(s->{
            documentAndImageInfo = (DocumentAndImageInfo) s;
            Log.d("Edit2, ", String.valueOf(documentAndImageInfo.getImages().size()
                    + String.valueOf(documentAndImageInfo.getDocument().getDocumentId())));
            StateChangeHelper.HomeActionChange(MachineActions.HOME_OPEN_DOC, ScanActivity.this);
        }, Throwable::printStackTrace));
    }

    public void updateImageInFile(Uri uri, Bitmap bitmap){
        /*
         bmp = ImageData.rotateBitmap(bmp, 270f);
            FileOutputStream outputStream = new FileOutputStream(newFile);
            // significant speed loss in PNG
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            bmp.recycle();

            ExifInterface newExif = new ExifInterface(newFile.getAbsolutePath());
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
            newExif.saveAttributes();
         */
        File file = new File(Objects.requireNonNull(uri.getPath()));
        if(!file.exists()) return;
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteImageFromFile(Uri uri){
        File file = new File(Objects.requireNonNull(uri.getPath()));
        Log.d ("ImageDelete", "delete " + uri.getPath());
        if (file.exists()) {
            Log.d("ImageDelete", "deleteHappened");
            return file.delete();
        }
        else return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause Called");
        if(CurrentMachineState == MachineStates.CAMERA) {
            findViewById(R.id.fragment_tools).setBackgroundColor(Color.parseColor("#000000"));
        }
    }
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.fragment_tools).setBackgroundColor(Color.parseColor("#00000000"));
            }
        }, 600);
        super.onResume();
    }
}