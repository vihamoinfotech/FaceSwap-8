package com.facechanger.faceswap.enhance.model;

import com.facechanger.faceswap.enhance.model.api.TemplateItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A fast, memory-safe in-memory cache to temporarily hold massive lists of 
 * items being passed between Activities (such as from Home Screen to View All).
 * This exclusively avoids the TransactionTooLargeException Intent bundles cause.
 */
public class TemplateCache {
    
    private static List<TemplateItem> currentTemplates = new ArrayList<>();

    public static void setCurrentTemplates(List<TemplateItem> templates) {
        if (templates != null) {
            currentTemplates = new ArrayList<>(templates);
        } else {
            currentTemplates = new ArrayList<>();
        }
    }

    public static List<TemplateItem> getCurrentTemplates() {
        return currentTemplates;
    }
    
    public static void clear() {
        currentTemplates.clear();
    }
}
