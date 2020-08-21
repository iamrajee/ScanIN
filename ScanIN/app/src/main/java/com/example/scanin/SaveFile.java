package com.example.scanin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import static java.lang.Math.min;
import static java.lang.Math.max;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.scanin.DatabaseModule.DocumentAndImageInfo;
import com.example.scanin.DatabaseModule.ImageInfo;
import com.example.scanin.HomeModule.MainActivity;
import com.example.scanin.ImageDataModule.ImageData;
import com.example.scanin.ImageDataModule.ImageEditUtil;
import com.example.scanin.Utils.FileUtils;
import com.squareup.picasso.Picasso;

import org.opencv.core.Point;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static com.example.scanin.ImageDataModule.ImageData.rotateBitmap;
import static com.example.scanin.ImageDataModule.ImageEditUtil.convertMap2ArrayList;
import static com.example.scanin.ImageDataModule.ImageEditUtil.getDefaultPoints;
import static com.example.scanin.ImageDataModule.ImageEditUtil.rotateCropPoints;
import static com.example.scanin.ImageDataModule.ImageEditUtil.scalePoints;

public class SaveFile {

    public static final int A4_width = 1050;
    public static final int A4_height = 1485;
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    public static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = MainActivity.WRITE_EXTERNAL_STORAGE_REQUEST_CODE;

    public static File saveImage(Activity myActivity, Bitmap bitmap) throws IOException {

        String externalStorageState = Environment.getExternalStorageState();
        File myFile = null;

        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {

            File savedImageDirectory = myActivity.getExternalFilesDir("ScanIN");

            Date currentDate = new Date();
            long elapsedTime = SystemClock.elapsedRealtime();
            String uniqueImageName = "/" + currentDate + "_" + elapsedTime + ".png";

            myFile = new File(savedImageDirectory + uniqueImageName);
            long freeSpace = savedImageDirectory.getFreeSpace();
            long requiredSpace = bitmap.getByteCount();

            if (requiredSpace * 1.8 < freeSpace) {
                // enough space to store img in external storage

                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(myFile);
                    boolean isImageSaveSuccessful = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);

                    if (isImageSaveSuccessful) {
                        return myFile;
                    } else {
                        throw new IOException("Something went wrong while saving the image.");
                    }

                } catch (Exception e) {
                    throw new IOException("Couldn't store the image.");
                }

            } else {
                throw new IOException("Not enough space to save image.");
            }
        } else {
            throw new IOException("This device doesn't have an external storage.");
        }
    }

    public static void savePDF(Activity myActivity, PdfDocument document) throws IOException {

        String externalStorageState = Environment.getExternalStorageState();
        FileOutputStream myFile = null;

        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {

            File savedPDFDirectory = myActivity.getExternalFilesDir("ScanIN");

            Date currentDate = new Date();
            long elapsedTime = SystemClock.elapsedRealtime();
            String uniquePDFName = "/ScanIN_" + currentDate + "_" + elapsedTime + ".pdf";

            myFile = new FileOutputStream(savedPDFDirectory + uniquePDFName);

            document.writeTo(myFile);

            Log.i("PDF", myFile.toString());

        } else {
            throw new IOException("This device doesn't have an storage.");
        }
    }

    public static List <File> prepareImagesForSharing (Activity myActivity,
            DocumentAndImageInfo documentAndImageInfo) throws IOException{
        File pictureDirectory = ((ScanActivity)myActivity).getOutputDirectory();
        List<File> resImageFileList = new LinkedList<>();
        for (ImageInfo img : documentAndImageInfo.getImages()) {
            Bitmap bmp = Picasso.get().load(img.getUri()).get();
            bmp = prepareBitmap(myActivity, bmp, img);
            String newName = new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis());
            File fullFile = new File(pictureDirectory + newName + ".jpg");
            FileUtils.saveFile(fullFile, bmp);
            resImageFileList.add (fullFile);
        }
        return resImageFileList;
    }

    public static void savePDF(Activity myActivity, PdfDocument document, String pdf_name) throws IOException {

        String externalStorageState = Environment.getExternalStorageState();
        FileOutputStream myFile = null;

        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {

            File savedPDFDirectory = myActivity.getExternalFilesDir("saved_pdf");

//            Date currentDate = new Date();
//            long elapsedTime = SystemClock.elapsedRealtime();
//            String uniquePDFName = "/ScanIN_" + currentDate + "_" + elapsedTime + ".pdf";

            String pdf_full_path = savedPDFDirectory + "/" + pdf_name + ".pdf";
            Log.d ("FragmentPdf", "PDF full path = " + pdf_full_path);
            myFile = new FileOutputStream(pdf_full_path);

            document.writeTo(myFile);

            Log.i("PDF", myFile.toString());

        } else {
            throw new IOException("This device doesn't have an storage.");
        }
    }

    public static Bitmap prepareBitmap (Context context, Bitmap bmp, ImageInfo imgInfo) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        bmp = rotateBitmap(bmp, imgInfo.getRotationConfig() * 90.0f);
        // comes indirectly from database.
        ArrayList <Point> points = ImageEditUtil.convertMap2ArrayList(imgInfo.getCropPositionMap());

        // if nothing in database
        if (points == null) {
            if (imgInfo.getRotationConfig() == 0 || imgInfo.getRotationConfig() == 2) {
                points = convertMap2ArrayList(getDefaultPoints(width, height));
            } else {
                points = convertMap2ArrayList(getDefaultPoints(height, width));
            }
            //currentImg.setCropPosition(points);
            // if database has cropPoints in orig config. Convert to the current rotation config.
        } else {
            double scale = ImageEditUtil.getScale(width, height);
            points = scalePoints(points, (float) scale);
            int rotValue = 0;
            int rotationConfig = imgInfo.getRotationConfig();
            while (rotValue != rotationConfig) {
                points = rotateCropPoints(points, width, height, rotValue);
                rotValue = (rotValue + 1) % 4;
            }
            //currentImg.setCropPosition(points);
        }

        bmp = ImageData.applyCropImage(bmp, points);
        bmp = ImageData.applyFilter(bmp, ImageEditUtil.getFilterName(imgInfo.getFilterId()));
        bmp = ImageData.changeContrastAndBrightness(context, bmp, (float) imgInfo.getAlpha(),
                (float) imgInfo.getBeta());
        return bmp;
    }

    public static void saveInGallery(Activity myActivity, DocumentAndImageInfo doc) throws IOException {
        List<ImageInfo> images = doc.getImages();

        for (ImageInfo img: images) {
            Bitmap bmp = Picasso.get().load(img.getUri()).get();
            bmp = prepareBitmap(myActivity, bmp, img);
            String new_name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis());
            FileUtils.saveImage(myActivity, bmp, new_name);
        }
    }

    public static void createPdfFromDocumentAndImageInfo(Activity myActivity, DocumentAndImageInfo doc, float quality)throws IOException {
        PdfDocument document = new PdfDocument();
        int pageNumber = 1;

        final int A4_width_final = (int) (A4_width * quality);
        final int A4_height_final = (int) (A4_height * quality);

        List<ImageInfo> images = doc.getImages();
        String pdf_name = doc.getDocument().getDocumentName();

        for (ImageInfo img: images) {

            // Create a page of the same size as the image

            // can be optimized to use the same bitmap memory.
//            Bitmap source = ImageEditUtil.loadBitmap(myActivity, img.getUri());
//            Matrix matrix = new Matrix();
//
//            Bitmap bmp = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
            Bitmap bmp = Picasso.get().load(img.getUri()).get();
            bmp = prepareBitmap (myActivity, bmp, img);

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(A4_width_final,
                    A4_height_final, pageNumber).create();

            Double scale = min((double) A4_width_final / bmp.getWidth(), (double) A4_height_final / bmp.getHeight());
            bmp = Bitmap.createScaledBitmap(bmp, (int) (bmp.getWidth() * scale), (int) (bmp.getHeight() * scale), false);

            PdfDocument.Page page = document.startPage(pageInfo);

            // Draw the bitmap onto the page
            Canvas canvas = page.getCanvas();
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            int left = (width - bmp.getWidth()) >> 1;
            int top = (height - bmp.getHeight()) >> 1;

            canvas.drawBitmap(bmp, left, top, null);
            document.finishPage(page);

            pageNumber += 1;

//            source.recycle();
//            bmp.recycle();
        }

        int permissionCheck = ContextCompat.checkSelfPermission(myActivity ,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {

            // write PDF file
            SaveFile.savePDF(myActivity, document, pdf_name);

//            Toast.makeText(myActivity,
//                    "The pdf is saved successfully to external storage.",
//                    Toast.LENGTH_LONG).show();

        } else {
            ActivityCompat.requestPermissions(myActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }
        document.close();
    }

    public static void createPdfFromListOfBitmap(Activity myActivity,
                                                 ArrayList<Bitmap> listBmp) throws IOException{

        PdfDocument document = new PdfDocument();
        int pageNumber = 1;
        for (Bitmap bmp : listBmp) {

            // Create a page of the same size as the image

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(A4_width,
                    A4_height, pageNumber).create();

            PdfDocument.Page page = document.startPage(pageInfo);


            // Draw the bitmap onto the page
            Canvas canvas = page.getCanvas();
            canvas.drawBitmap(bmp, 0f, 0f, null);
            document.finishPage(page);

            pageNumber += 1;
        }

        int permissionCheck = ContextCompat.checkSelfPermission(myActivity ,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {

                // write PDF file
                SaveFile.savePDF(myActivity, document);

                Toast.makeText(myActivity,
                        "The pdf is saved successfully to external storage.",
                        Toast.LENGTH_LONG).show();

        } else {
            ActivityCompat.requestPermissions(myActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }
        document.close();
    }

}
