package com.facechanger.faceswap.enhance.model;

/**
 * Holds data for the Edit Image flow, ready for future API calls.
 */
public class AppFaceEditImageData {

    private String originalImageUrl;
    private String selectedBackgroundUrl;
    private String enhancedImageUrl;

    public AppFaceEditImageData() {
    }

    public AppFaceEditImageData(String originalImageUrl) {
        this.originalImageUrl = originalImageUrl;
    }

    public String getOriginalImageUrl() {
        return originalImageUrl;
    }

    public void setOriginalImageUrl(String originalImageUrl) {
        this.originalImageUrl = originalImageUrl;
    }

    public String getSelectedBackgroundUrl() {
        return selectedBackgroundUrl;
    }

    public void setSelectedBackgroundUrl(String selectedBackgroundUrl) {
        this.selectedBackgroundUrl = selectedBackgroundUrl;
    }

    public String getEnhancedImageUrl() {
        return enhancedImageUrl;
    }

    public void setEnhancedImageUrl(String enhancedImageUrl) {
        this.enhancedImageUrl = enhancedImageUrl;
    }

    @Override
    public String toString() {
        return "EditImageData{" +
                "originalImageUrl='" + originalImageUrl + '\'' +
                ", selectedBackgroundUrl='" + selectedBackgroundUrl + '\'' +
                ", enhancedImageUrl='" + enhancedImageUrl + '\'' +
                '}';
    }
}
