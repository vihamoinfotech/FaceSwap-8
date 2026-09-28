package com.perfect.faceeditor.facechanger.io.controller;

import com.revenuecat.purchases.Package;
import java.util.List;

public interface AppFacePurchaseListener {
    void onProductsLoaded(List<Package> packages);
    void onPurchaseSuccess(String productId);
    void onPurchaseError(String message);
    void onPurchaseCancelled();
    void onPurchaseVerified();
    void onPurchaseVerificationFailed(String error);
}
