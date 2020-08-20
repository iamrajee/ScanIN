package com.example.scanin.DatabaseModule;

import android.util.Log;

import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DocumentAndImageInfo {
    @Embedded public Document document;

    @Relation(
            parentColumn = "document_id",
            entityColumn = "img_document_id"
    )
    public List<ImageInfo> images;

    public DocumentAndImageInfo(){}
    @Ignore
    public DocumentAndImageInfo(Document document, List<ImageInfo> images){
        this.document = document;
        this.images = images;
    }

    @Ignore
    public DocumentAndImageInfo(Document document, ImageInfo images){
        this.document = document;
        this.images = new ArrayList<>(Collections.singletonList(images));
    }

    @Ignore
    boolean sorted = false;

    public Document getDocument() {
        return document;
    }

    public List<ImageInfo> getImages() {
        if (!sorted) {
            Collections.sort(images, (imageInfo, t1) -> (int) (imageInfo.getPosition() - t1.getPosition()));
            sorted = true;
        }
        return images;
    }
}
