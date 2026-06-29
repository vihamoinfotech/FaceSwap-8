package com.facechanger.faceswap.enhance.controller;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.models.StoreTransaction;

import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;

import java.util.Collections;

public class AppFaceRevenueCatManager {
    private static final String TAG = "RevenueCatManager";
    private static AppFaceRevenueCatManager instance;
    public String appUserID;

    private AppFaceRevenueCatManager() {
    }

    public static synchronized AppFaceRevenueCatManager getInstance() {
        if (instance == null) {
            instance = new AppFaceRevenueCatManager();
        }
        return instance;
    }

    public void init(Application application) {
        String apiKey = "goog_TccIqHsCGYuxYaKfBVMdTjJoMLw";

        Purchases.setDebugLogsEnabled(true);

        PurchasesConfiguration configuration = new PurchasesConfiguration.Builder(application, apiKey)
                .build();

        Purchases.configure(configuration);
        appUserID = configuration.getAppUserID();
        Log.d(TAG, "RevenueCat initialized");
    }

    public void fetchOfferings(AppFacePurchaseListener listener) {
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                Log.d(TAG, "Offerings received from RevenueCat: " + offerings.getAll().keySet());
                // Try current offering first, then 'default'
                com.revenuecat.purchases.Offering offering = offerings.getCurrent();
                if (offering == null || offering.getAvailablePackages().isEmpty()) {
                    offering = offerings.get("default");
                }

                if (AppFaceAppSystem.isDebugMode()) {
                    for (com.revenuecat.purchases.Package pkg : offering.getAvailablePackages()) {
                        Log.d("RC", pkg.getProduct().getId());
                    }
                }

                if (offering != null && !offering.getAvailablePackages().isEmpty()) {
                    Log.d(TAG, "Offerings loaded: " + offering.getAvailablePackages().size() + " packages");
                    listener.onProductsLoaded(offering.getAvailablePackages());
                } else {
                    Log.e(TAG, "No packages found in current or 'default' offering");
                    listener.onProductsLoaded(Collections.emptyList());
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                String underlyingError = error.getUnderlyingErrorMessage();
                Log.e(TAG, "Error fetching offerings: [" + error.getCode() + "] " + error.getMessage());
                if (underlyingError != null) {
                    Log.e(TAG, "Underlying error: " + underlyingError);
                }
                listener.onPurchaseError(error.getMessage());
            }
        });
    }

    public void purchasePackage(Activity activity, Boolean isFromPaywall, Package packageToPurchase, AppFacePurchaseListener listener) {
        Purchases.getSharedInstance().purchasePackage(activity, packageToPurchase, new PurchaseCallback() {
            @Override
            public void onCompleted(@NonNull StoreTransaction storeTransaction,
                                    @NonNull com.revenuecat.purchases.CustomerInfo customerInfo) {

                String productId = storeTransaction.getProductIds().get(0);
                String transactionId = storeTransaction.getPurchaseToken();
                String orderId = storeTransaction.getOrderId();

                // Extract the raw JSON receipt from Google Play instead of the Kotlin object
                // string
                String receiptData = "";
                try {
                    if (storeTransaction.getOriginalJson() != null) {
                        receiptData = storeTransaction.getOriginalJson().toString();
                    } else {
                        receiptData = storeTransaction.toString();
                    }
                } catch (Exception e) {
                    receiptData = storeTransaction.toString();
                }

                Log.d(TAG, "Purchase successful - productId: " + productId);
                Log.d(TAG, "Purchase successful - transactionId: " + transactionId);
                Log.d(TAG, "Purchase successful - orderId: " + orderId);
                Log.d(TAG, "Purchase successful - receiptData: " + receiptData);

                // Notify UI of success immediately
                listener.onPurchaseSuccess(productId);

                if (isFromPaywall) {
                    // Server-side verification
                    AppFaceApiRepository.verifyInAppPurchase(productId, receiptData, transactionId, orderId,
                            new AppFaceApiRepository.VerificationCallback() {
                                @Override
                                public void onSuccess() {
                                    listener.onPurchaseVerified();
                                }

                                @Override
                                public void onError(String error) {
                                    listener.onPurchaseVerificationFailed(error);
                                }
                            });
                } else {
                    // Server-side verification
                    AppFaceApiRepository.verifyPurchase(productId, receiptData, transactionId, orderId,
                            new AppFaceApiRepository.VerificationCallback() {
                                @Override
                                public void onSuccess() {
                                    listener.onPurchaseVerified();
                                }

                                @Override
                                public void onError(String error) {
                                    listener.onPurchaseVerificationFailed(error);
                                }
                            });
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                if (userCancelled) {
                    Log.d(TAG, "User cancelled purchase");
                    listener.onPurchaseCancelled();
                } else if (error.getCode() == com.revenuecat.purchases.PurchasesErrorCode.ProductAlreadyPurchasedError) {
                    Log.d(TAG, "Product already purchased. Checking customer info for entitlement.");
                    Purchases.getSharedInstance().getCustomerInfo(new ReceiveCustomerInfoCallback() {
                        @Override
                        public void onReceived(@NonNull com.revenuecat.purchases.CustomerInfo customerInfo) {
                            com.revenuecat.purchases.EntitlementInfo entitlement = customerInfo.getEntitlements().get("faceswap_subscription");
                            if (entitlement != null && entitlement.isActive()) {
                                Log.d(TAG, "Active premium entitlement found. Granting premium access.");
                                listener.onPurchaseVerified();
                            } else {
                                Log.w(TAG, "No active faceswap_subscription entitlement found. Resolving purchase error.");
                                listener.onPurchaseError(error.getMessage());
                            }
                        }

                        @Override
                        public void onError(@NonNull PurchasesError e) {
                            listener.onPurchaseError(error.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "Purchase error: " + error.getMessage());
                    listener.onPurchaseError(error.getMessage());
                }
            }
        });
    }

    public void restorePurchases(Activity activity, AppFacePurchaseListener listener) {
        Purchases.getSharedInstance().restorePurchases(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull com.revenuecat.purchases.CustomerInfo customerInfo) {
                // If there are entitlements or active purchases, we could verify them here.
                // For now, simply notify success to the UI.
                Log.d(TAG, "Restore successful");
                listener.onPurchaseVerified();
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.e(TAG, "Restore error: " + error.getMessage());
                listener.onPurchaseError(error.getMessage());
            }
        });
    }

    public interface PremiumStatusCallback {
        void onResult(boolean isPremium);
    }

    /**
     * Checks directly with RevenueCat if the "faceswap_subscription" entitlement is currently active.
     */
    public void checkPremiumStatus(PremiumStatusCallback callback) {
        Purchases.getSharedInstance().getCustomerInfo(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull com.revenuecat.purchases.CustomerInfo customerInfo) {
                com.revenuecat.purchases.EntitlementInfo entitlement = customerInfo.getEntitlements().get("faceswap_subscription");
                boolean isActive = entitlement != null && entitlement.isActive();
                callback.onResult(isActive);
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                Log.e(TAG, "Error checking premium status via RevenueCat: " + error.getMessage());
                callback.onResult(false);
            }
        });
    }
}
