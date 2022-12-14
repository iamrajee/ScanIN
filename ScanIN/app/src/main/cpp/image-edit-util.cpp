//
// Created by kaushal on 10/7/20.
//

#define DEBUG
#include <iostream>
#include <jni.h>
#include <string>
#include <vector>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <android/log.h>
#include "filters.hpp"
#include "corners.hpp"
using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_scanin_ImageDataModule_ImageEditUtil_cropImage(JNIEnv *env, jclass clazz,
                                                                jlong img_addr, jlong crop_img_addr,
                                                                jlong pts, jint interpolation) {
    Mat& src = *(Mat*) img_addr;
    Mat& dst = *(Mat*) crop_img_addr;
    Mat& points = *(Mat*) pts;
    std::vector <cv::Point> vec_pts (4);

    for (int i = 0; i < 4; i++) {
        vec_pts[i] = cv::Point(points.at<uint16_t> (i, 0), points.at<uint16_t> (i, 1));
    }

#ifdef DEBUG
    String currentLog = "";
    for (int i = 0; i < 4; i++) {
        currentLog += std::to_string(i) + ". " + std::to_string (vec_pts[i].x) +
                      " " + std::to_string (vec_pts[i].y) + "\n";
    }
    __android_log_print(ANDROID_LOG_DEBUG, "NativeCode", "%s", currentLog.c_str());
#endif

    // changing cv::INTER_NEAREST to cv::INTER_LANCZOS4 should improve results
    // but degrade performance.
    // This can raise errors due to order problem.
    int ret = four_point_transform(src, dst, vec_pts, interpolation);
    if (ret) {
        __android_log_print(ANDROID_LOG_ERROR, "NativeCode", "Order points function failed before warp");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_scanin_ImageDataModule_ImageEditUtil_getTestGray(JNIEnv *env, jclass clazz,
                                                                  jlong img_addr,
                                                                  jlong gray_img_addr) {
    Mat &src = *(Mat*) img_addr;
    Mat &dst = *(Mat*) gray_img_addr;

    dark_magic_filter(src, dst);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_scanin_ImageDataModule_ImageEditUtil_filterImage(JNIEnv *env, jclass clazz,
                                                                  jlong img_addr,
                                                                  jlong filter_img_addr,
                                                                  jint filter_id) {
    Mat &src = *(Mat*) img_addr;
    Mat &dst = *(Mat*) filter_img_addr;

#ifdef DEBUG
    __android_log_print(ANDROID_LOG_DEBUG, "NativeCode", "%s",
            ("Filter  rows=" + std::to_string(src.rows) + ", cols=" + std::to_string (src.cols)).c_str());
#endif

    Mat temp;
    cv::cvtColor(src, temp, cv::COLOR_RGBA2BGR);
    // filterList = {"magic_filter", "gray_filter", "dark_magic_filter", "sharpen_filter"};
    switch (filter_id) {
        case 0: magic_filter(temp, dst, 1.5, -20); break;
        case 1: sharpen_filter(temp, temp); gray_filter(temp, dst); break;
        case 2: dark_magic_filter(temp, dst); break;
        case 3: sharpen_filter(temp, dst); break;
        default: std::cerr << "We should never reach here." << std::endl;
    }
    temp.release();

    if (dst.type() == CV_8UC3) {
        cv::cvtColor (dst, dst, cv::COLOR_BGR2RGBA);
    } else if (dst.type() == CV_8UC1){
        cv::cvtColor (dst, dst, cv::COLOR_GRAY2RGBA);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "NativeCode", "%s", "incorrect image output type.");
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_scanin_ImageDataModule_ImageEditUtil_changeContrastAndBrightness(JNIEnv *env, jclass clazz,
                                                                                  jlong img_addr,
                                                                                  jlong output_img_addr,
                                                                                  jdouble alpha,  jint beta){
    Mat &src = *(Mat*) img_addr;
    Mat &dst = *(Mat*) output_img_addr;

    __android_log_print(ANDROID_LOG_DEBUG, "SrcChannel", "%s",
                        ("Filter  channel=" + std::to_string(src.channels()) + ", cols=" + std::to_string (src.cols)).c_str());

    Mat temp;
    cv::cvtColor(src, temp, cv::COLOR_RGBA2BGR);

    temp.convertTo(dst, -1, alpha, beta);
    cv::cvtColor (dst, dst, cv::COLOR_BGR2RGBA);
//    dst.convertTo (dst, CV_8UC3);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_scanin_ImageDataModule_ImageEditUtil_getBestPoints(JNIEnv *env, jclass clazz,
                                                                    jlong img_addr, jlong pts) {
    Mat &src = *(Mat*) img_addr;
    Mat &res = *(Mat*) pts;
    std::vector <Point> vec_pts;

#ifdef DEBUG
    std::string clog = "Filter  cols=" + std::to_string(src.cols) + ", rows=" + std::to_string(src.rows);
    __android_log_print(ANDROID_LOG_DEBUG, "NativeCode", "%s", clog.c_str());
#endif

    Mat temp;
    cv::cvtColor(src, temp, cv::COLOR_RGBA2BGR);

    find_best_corners(temp, vec_pts);
    temp.release();

#ifdef DEBUG
    String currentLog = "";
    for (int i = 0; i < 4; i++) {
        currentLog += std::to_string(i) + ". " + std::to_string (vec_pts[i].x) +
                " " + std::to_string (vec_pts[i].y) + "\n";
    }
    __android_log_print(ANDROID_LOG_DEBUG, "NativeCode", "%s", currentLog.c_str());
#endif

    for (int i = 0; i < 4; i++) {
        res.at<uint16_t> (i, 0) = vec_pts[i].x;
        res.at<uint16_t> (i, 1) = vec_pts[i].y;
    }

}