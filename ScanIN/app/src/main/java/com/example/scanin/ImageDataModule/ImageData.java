package com.example.scanin.ImageDataModule;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ColorSpace;
import android.graphics.Matrix;

import android.graphics.Paint;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;

import com.example.scanin.DatabaseModule.ImageInfo;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.io.IOException;
import java.util.ArrayList;

import static java.lang.Math.min;

public class ImageData {
    // original is what is loaded directly.
    // This can be a potential issue for memory errors if original is too big.
    private Bitmap originalBitmap;
    // current is after reszizing to MAX_SIZE maintaining aspect ratio.
    private Bitmap currentBitmap;
    private String filterName;
    private Uri fileName;
    private ArrayList <Point> cropPosition;
    private int THUMBNAIL_SIZE = 64;
    private final double EPS = 1e-10;
    private int rotationConfig = 0;

    // These values correspond to current Bitmap just after it was loaded and rotated in
    // in its correct configuration.
    private int loadWidth;
    private int loadHeight;
    public static int MAX_SIZE=1500;

    public ImageData(Uri uri) {
        this.originalBitmap = null;
        this.currentBitmap = null;
        this.filterName = null;
        this.fileName = uri;
        this.cropPosition = null;
        this.rotationConfig = 0;
    }

    public ImageData(ImageInfo imgInfo) {
        this.originalBitmap = null;
        this.currentBitmap = null;
        this.filterName = ImageEditUtil.getFilterName(imgInfo.getFilterId());
        this.fileName = imgInfo.getUri();
        this.cropPosition = ImageEditUtil.convertMap2ArrayList(imgInfo.getCropPositionMap());
        this.rotationConfig = imgInfo.getRotationConfig();
    }

    public Bitmap getOriginalBitmap() {
        return originalBitmap;
    }

    public Bitmap getCurrentBitmap() {
        return currentBitmap;
    }

    public Uri getFileName() {
        return fileName;
    }

    public String getFilterName() {
        return filterName;
    }

    public ArrayList <Point> getCropPosition() {
        return cropPosition;
    }

    public void setOriginalBitmap(Bitmap originalBitmap) {
        this.originalBitmap = originalBitmap;
        // Intentionally not updating loadWidth and loadHeight.
    }

    public void setCurrentBitmap (Bitmap currentBitmap) {
        this.currentBitmap = currentBitmap;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void setFileName(Uri fileName) {
        this.fileName = fileName;
    }

    public void setRotationConfig (int rotationConfig) {
        this.rotationConfig = rotationConfig;
    }

    public int getRotationConfig () {
        return rotationConfig;
    }

    public void setCropPosition(ArrayList <Point> cropPosition) {
        this.cropPosition = cropPosition;
    }

    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void setOriginalBitmap(Context context) throws IOException {
        try {
            this.originalBitmap =  MediaStore.Images.Media.getBitmap(context.getContentResolver() , fileName);

//            Bitmap temp = ImageEditUtil.loadBitmap(context, this.fileName);
//            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                ColorSpace cs1 = this.originalBitmap.getColorSpace();
//                int d1 = this.originalBitmap.getDensity();
//                Log.d ("imageData", cs1.toString() + " , density=" + d1 + ", byte=" +
//                        this.originalBitmap.getAllocationByteCount()
//                );
//                ColorSpace cs2 = temp.getColorSpace();
//                int d2 = temp.getDensity();
//                Log.d ("imageData", cs2.toString() + " , density=" + d2 + ", byte=" +
//                        temp.getAllocationByteCount());
//                temp.getConfig().toString();
//            }

            // As of now using the below commented code is giving me error related to bitmap2mat function.
            // OpenCV(4.2.0) Error: Assertion failed (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) in Java_org_opencv_android_Utils_nBitmapToMat2, file /build/master_pack-android/opencv/modules/java/generator/src/cpp/utils.cpp, line 38
            //2020-08-05 21:54:12.957 23053-23053/com.example.scanin E/org.opencv.android.Utils: nBitmapToMat caught cv::Exception: OpenCV(4.2.0) /build/master_pack-android/opencv/modules/java/generator/src/cpp/utils.cpp:38: error: (-215:Assertion failed) AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 in function 'Java_org_opencv_android_Utils_nBitmapToMat2'
            //2020-08-05 21:54:12.968 23053-23053/com.example.scanin A/libc: Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x18
            //this.originalBitmap = ImageEditUtil.loadBitmap(context, this.fileName);

            this.originalBitmap = ImageData.rotateBitmap(this.originalBitmap);
            this.currentBitmap = getResizedBitmap(this.originalBitmap, MAX_SIZE);
            this.loadHeight = this.currentBitmap.getHeight();
            this.loadWidth = this.currentBitmap.getWidth();
        } catch (Exception e){
            throw e;
        }
    }

    public static Bitmap rotateBitmap(Bitmap source) {
        float angle = 90.0f;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap rotateBitmap (Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public void rotateBitmap () {
        currentBitmap = rotateBitmap(currentBitmap);
        rotateCropPosition ();
        rotationConfig = (rotationConfig + 1) % 4;
    }

    public int getWidth() {
        return loadWidth;
    }

    public int getHeight() {
        return loadHeight;
    }

    public int getOriginalWidth () {
        return originalBitmap.getWidth();
    }

    public int getOriginalHeight() {
        return originalBitmap.getHeight();
    }

    public void rotateCropPosition () {
        if (cropPosition != null) {
            ArrayList <Point> res = new ArrayList<>();
            int width = currentBitmap.getWidth();
            int height = currentBitmap.getHeight();

            for (int i = 0; i < 4; i++) {
                int j = (i + 4) % 4;
                res.add (new Point (width - cropPosition.get(j).y, cropPosition.get(j).x));
            }
            cropPosition = res;
        }
    }

    public Bitmap getThumbnail() {
        Bitmap thumbImage = ThumbnailUtils.extractThumbnail(currentBitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        return thumbImage;
    }

    public double getScale (int cwidth, int cheight) {
        int width = currentBitmap.getWidth();
        int height = currentBitmap.getHeight();
        double fx = (double) cwidth / width;
        double fy = (double) cheight / height;
        double scale = min (fx, fy);
        return scale;
    }

    //Change Brightness and contrast
    public static Bitmap changeContrastAndBrightness(Context context, Bitmap bitmap, float alpha, float beta) {
        ContrastFilterTransformation1 t1 = new ContrastFilterTransformation1(context, alpha);
        BrightnessFilterTransformation1 t2 = new BrightnessFilterTransformation1(context, beta);
        bitmap = t1.transform(bitmap);
        bitmap = t2.transform(bitmap);
        return bitmap;
    }

    public static Bitmap applyCropImage (Bitmap bitmap, ArrayList<Point> cropPosition) {
        if (bitmap != null && cropPosition != null) {
            //Mat imgToProcess = new Mat();
            Mat imgToProcess = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC(4));

            Utils.bitmapToMat(bitmap, imgToProcess);
            Mat outMat = new Mat();
            Mat pts = new Mat(4, 2, CvType.CV_16U);
            for (int i = 0; i < 4; i++) {
                pts.put(i, 0, cropPosition.get(i).x);
                pts.put(i, 1, cropPosition.get(i).y);
            }
            ImageEditUtil.cropImage(imgToProcess.getNativeObjAddr(),
                    outMat.getNativeObjAddr(), pts.getNativeObjAddr());
            bitmap = Bitmap.createBitmap(outMat.cols(),
                                    outMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(outMat, bitmap);
        }
        return bitmap;
    }

    public static Bitmap applyFilter (Bitmap bitmap, String filterName) {
        if (bitmap != null && ImageEditUtil.isValidFilter(filterName)) {
            Mat imgToProcess = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC(4));
            Utils.bitmapToMat(bitmap, imgToProcess);
            Mat outMat = new Mat();
            int filter_id = ImageEditUtil.getFilterId (filterName);
            ImageEditUtil.filterImage(imgToProcess.getNativeObjAddr(), outMat.getNativeObjAddr(), filter_id);
            bitmap = Bitmap.createBitmap(outMat.cols(),
                    outMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(outMat, bitmap);
        }
        return bitmap;
    }

    public ArrayList <Point> getBestPoints () {
        ArrayList <Point> res = new ArrayList<Point>();
        if (currentBitmap != null) {
            Mat imgToProcess = new Mat(currentBitmap.getWidth(), currentBitmap.getHeight(), CvType.CV_8UC(4));
            Utils.bitmapToMat(currentBitmap, imgToProcess);
            Mat pts = new Mat(4, 2, CvType.CV_16U);
            pts.setTo(new Scalar(-1));
            ImageEditUtil.getBestPoints(imgToProcess.getNativeObjAddr(), pts.getNativeObjAddr());
            for (int i = 0; i < 4; ++i) {
                res.add(new Point (pts.get(i, 0)[0], pts.get(i, 1)[0]));
            }
        }
        return res;
    }

    public Bitmap getSmallCurrentImage(Context context) {
        return this.currentBitmap;
    }
}
