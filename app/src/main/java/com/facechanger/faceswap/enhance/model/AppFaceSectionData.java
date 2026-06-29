package com.facechanger.faceswap.enhance.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Data model representing a content section with a title and list of image URLs.
 * Used to display categorized image grids on the home screen.
 */
public class AppFaceSectionData implements Serializable {

    private final String title;
    private final ArrayList<String> imageUrls;

    public AppFaceSectionData(String title, ArrayList<String> imageUrls) {
        this.title = title;
        this.imageUrls = imageUrls;
    }

    public String getTitle() {
        return title;
    }

    public ArrayList<String> getImageUrls() {
        return imageUrls;
    }
}
