package com.perfect.faceeditor.facechanger.io.model;

import com.perfect.faceeditor.facechanger.io.model.api.SFS_RefacePosterItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A fast, memory-safe in-memory cache to temporarily hold massive lists of 
 * items being passed between Activities (such as from Home Screen to View All).
 * This exclusively avoids the TransactionTooLargeException Intent bundles cause.
 */
public class SFS_RefacePosterCache {
    
    private static List<SFS_RefacePosterItem> currentTemplates = new ArrayList<>();

    public static void setCurrentTemplates(List<SFS_RefacePosterItem> templates) {
        if (templates != null) {
            currentTemplates = new ArrayList<>(templates);
        } else {
            currentTemplates = new ArrayList<>();
        }
    }

    public static List<SFS_RefacePosterItem> getCurrentTemplates() {
        return currentTemplates;
    }
    
    public static void clear() {
        currentTemplates.clear();
    }
}
