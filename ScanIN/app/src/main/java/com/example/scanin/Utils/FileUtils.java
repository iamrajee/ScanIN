package com.example.scanin.Utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.scanin.ImageDataModule.ImageData;
import com.example.scanin.ImageDataModule.ImageEditUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class FileUtils {

    public static boolean isAlphabet (char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    public static boolean isNumber (char c) {
        return (c >= '0' && c <= '9');
    }

    public static boolean validateImageName (String imageFileName) {
        String[] fileSplit = imageFileName.split("\\.");
        if (fileSplit.length == 2) {
            return (fileSplit[1].equals("jpg")) && validateFileName(fileSplit[0]);
        }
        return false;
    }

    /*
     * Must contain only a-zA-Z0-9_-
     */
    public static boolean validateFileName (String fileName) {
        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);
            if (isAlphabet(c) || isNumber(c) || c == '_' || c == '-') {

            } else {
                return false;
            }
        }
        return true;
    }

    // saves in Android/com.example.Scanin/pictures
    public static void saveFile (File newFile, Bitmap bmp) throws IOException {
        try {
            bmp = ImageData.rotateBitmap(bmp, 270f);
            FileOutputStream outputStream = new FileOutputStream(newFile);
            // significant speed loss in PNG
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            ExifInterface newExif = new ExifInterface(newFile.getAbsolutePath());
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
            newExif.saveAttributes();
        } catch (Exception e) {
            Log.e("ScanActivity", e.getMessage());
            throw e;
        }
    }

    public static void copyFile (Context context, File newFile, Uri galleryUri) throws Exception{
        Bitmap bmp = ImageEditUtil.loadBitmap(galleryUri);
        //Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), galleryUri);
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
    }

    // saves in Pictures
    public static void saveImage(Context context, Bitmap bitmap, @NonNull String name) throws IOException {
        OutputStream fos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name + ".jpg");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            File image = new File(imagesDir, name + ".jpg");
            fos = new FileOutputStream(image);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        Objects.requireNonNull(fos).close();
    }
}
