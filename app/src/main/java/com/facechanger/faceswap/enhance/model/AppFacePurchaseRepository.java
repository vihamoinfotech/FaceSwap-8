package com.facechanger.faceswap.enhance.model;

import android.content.Context;
import android.content.SharedPreferences;

public class AppFacePurchaseRepository {
    private static final String PREF_NAME = "coin_prefs";
    private static final String KEY_BALANCE = "coin_balance";

    public static void addCoins(Context context, String productId) {
        int amountToAdd = 0;
        switch (productId) {
            case "coins_100":
                amountToAdd = 100;
                break;
            case "coins_400":
                amountToAdd = 400;
                break;
            case "coins_1000":
                amountToAdd = 1000;
                break;
            case "coins_2500":
                amountToAdd = 2500;
                break;
            case "coins_5000":
                amountToAdd = 5000;
                break;
        }

        if (amountToAdd > 0) {
            int currentBalance = getCoins(context);
            saveBalance(context, currentBalance + amountToAdd);
        }
    }

    public static int getCoins(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_BALANCE, 0);
    }

    public static void deductCoins(Context context, int amount) {
        int currentBalance = getCoins(context);
        int newBalance = Math.max(0, currentBalance - amount);
        saveBalance(context, newBalance);
    }

    private static void saveBalance(Context context, int amount) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_BALANCE, amount).apply();
    }

    public static void clearCoins(Context context) {
        saveBalance(context, 0);
    }
}
