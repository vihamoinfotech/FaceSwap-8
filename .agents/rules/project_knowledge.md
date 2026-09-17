# FaceSwap Application — Complete Architecture Knowledge Base

> **Last Updated:** Session 1 — Deep Discovery Phase
> **Purpose:** Persistent memory file. Consult BEFORE every interaction.

---

## 1. PROJECT STRUCTURE

```
FaceSwap_2/app/src/main/java/com/facechanger/faceswap/enhance/
├── AppFaceSwap.java                    ← Application class (SDK init)
├── view/                               ← All Activities (Screens)
│   ├── AppFaceSplashActivity.java      ← Launch → version check → auth → routing
│   ├── AppFaceMainActivity.java        ← HOME SCREEN (templates + bottom nav)
│   ├── AppFaceSwapActivity.java        ← Core face-swap processing
│   ├── AppFaceLoadingActivity.java     ← Lottie loading + API dispatch
│   ├── AppFaceDownloadShareActivity.java ← Result display + download/share
│   ├── AppFaceSectionDetailActivity.java ← Template grid (2-col) per category
│   ├── AppFacePaywallActivity.java     ← Premium subscription paywall
│   ├── AppFaceStoreActivity.java       ← Coin purchase store
│   ├── AppFaceSettingsActivity.java    ← Settings (profile, language, links)
│   ├── AppFaceLanguageActivity.java    ← Language picker
│   ├── AppFaceOnboardingActivity.java  ← First-time onboarding
│   ├── BaseAppActivity.java            ← Root activity (locale, RTL, ad loading)
│   ├── LoginActivity.java             ← Firebase email auth (APP_EXP=1)
│   ├── SignupActivity.java            ← Firebase registration (APP_EXP=1)
│   ├── EditNameActivity.java          ← Profile name editing
│   ├── AppFaceVideoFaceSwapActivity.java ← Video face swap
│   ├── AppFaceCoupleSwapActivity.java    ← Couple face swap
│   ├── AppFaceMultiSwapActivity.java     ← Multi-face swap
│   ├── AppFaceTextToImageActivity.java   ← Text-to-image generation
│   ├── AppFaceAIImageGenerationActivity.java ← AI image generation
│   ├── AppFaceEditImageActivity.java     ← Image editing (remove bg, upscale, enhance)
│   ├── AppFaceBackgroundReplaceActivity.java ← Background replace
│   ├── AppFaceEnhanceFaceActivity.java   ← Face enhancement
│   ├── AppFaceMyWorkActivity.java        ← Gallery of saved results
│   └── adapter/
│       ├── AppFaceMainCategoryAdapter.java   ← Home screen RecyclerView (header + categories)
│       └── AppFaceStoreAdapter.java          ← Store coin packages
├── model/
│   ├── api/
│   │   ├── AppFaceTemplateCategory.java  ← Category with List<TemplateItem>
│   │   ├── AppFaceTemplateItem.java      ← Single template (id, name, prompt, categoryId, imageUrl)
│   │   ├── AppFaceFaceSwapResponse.java  ← Generic API response wrapper
│   │   ├── AppFaceVideoFaceSwapResponse.java ← Video response wrapper
│   │   └── AppFaceSplashDataResponse.java ← Splash session data
│   ├── AppFaceTemplateCache.java         ← In-memory template list for SectionDetailActivity
│   └── AppFaceSectionData.java           ← Serializable wrapper for category title + URLs
├── utils/
│   ├── AppFaceApiRepository.java         ← Central API service layer (all endpoints)
│   ├── AppFaceApiClient.java             ← HTTP client wrapper with auth header
│   ├── AppFaceSessionManager.java        ← In-memory session (token, credits, premium)
│   ├── AppFaceAppSystem.java             ← Feature flags + system config
│   ├── AppFaceCoinManager.java           ← Credit validation & gating dialogs
│   ├── AppFaceTools.java                 ← UI utilities (status bar, edge-to-edge)
│   ├── AppFaceGlideHelper.java           ← Authorized Glide URL factory
│   ├── AppFaceActivityNavHelper.java     ← Activity navigation with ad interstitials
│   ├── AppFaceNetworkUtils.java          ← Connectivity checks
│   ├── AppFaceLocaleHelper.java          ← Multi-language + RTL support
│   ├── AppFaceStaticValue.java           ← MMKV key constants
│   ├── AppFaceProductsID.java            ← RevenueCat product IDs
│   └── FirebaseAuthManager.java          ← Firebase email auth helper
└── controller/
    ├── AppFaceRevenueCatManager.java      ← RevenueCat IAP (subs + coins)
    ├── AppFaceAppDialogController.java    ← Dialog factory (retry, coins, server busy)
    ├── AppFacePurchaseListener.java       ← IAP callback interface
    ├── AppFaceFacebookEventsManager.java  ← Facebook Analytics events
    └── FirebaseManager.java              ← Firebase Analytics events
```

---

## 2. APP FLOW (Current)

```
App Launch
  → AppFaceSplashActivity
    → Version check (force update dialog if needed)
    → splash_data API → initializes SessionManager
    → Checks APP_EXP flag:
      ├── APP_EXP == 1: 
      │   ├── Not logged in → LoginActivity → SignupActivity
      │   └── Logged in → Paywall (if !premium) → AppFaceMainActivity
      └── APP_EXP != 1:
          ├── First launch → OnboardingActivity → Paywall → AppFaceMainActivity
          └── Not first → Paywall (if !premium) → AppFaceMainActivity

AppFaceMainActivity (HOME SCREEN)
  ├── Header: Logo + App Name | Coin Pill + Settings Button
  ├── Content: RecyclerView with AppFaceMainCategoryAdapter
  │   ├── Position 0: HEADER item (app_home_header_item.xml)
  │   │   └── "FACE SWAP" banner with "Try Now" button → opens AppFaceSwapActivity
  │   ├── Position 1..N: CATEGORY items (app_category_row_item.xml)
  │   │   ├── Section title + "View All" button
  │   │   └── 3 preview images loaded via authorized Glide URLs
  │   └── Each image click → AppFaceSwapActivity (with template data)
  │   └── "View All" → AppFaceSectionDetailActivity (2-col grid)
  ├── Shimmer loading state while templates fetch
  ├── Error/Retry state
  └── Bottom Navigation Bar:
      ├── AI Image → AppFaceAIImageGenerationActivity
      ├── Multi/Video → AppFaceMultiSwapActivity OR AppFaceVideoFaceSwapActivity
      ├── Face Swap → AppFaceSwapActivity
      └── My Work → AppFaceMyWorkActivity
```

---

## 3. KEY API ENDPOINTS (AppFaceApiRepository)

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/splash_data` | POST | Session init (token, credits, feature costs, config) |
| `/api/templates/list` | POST | Fetch template categories with items |
| `/api/templates/{id}/image` | GET | Template preview image (auth required) |
| `/api/faceswap/basic` | POST | Single face swap (source + target files) |
| `/api/faceswap/multi` | POST | Multi-face swap |
| `/api/faceswap/couple` | POST | Couple face swap |
| `/api/video/faceswap` | POST | Video face swap |
| `/api/edit_image` | POST | AI image edit (file + prompt) |
| `/api/background/remove` | POST | Remove background |
| `/api/background/replace` | POST | Replace background (file + prompt) |
| `/api/upscale/pro` | POST | Image upscale |
| `/api/enhance` | POST | Face enhance (GFPGAN) |
| `/api/enhance/face` | POST | Face restoration |
| `/api/image/generate` | POST | Text-to-image generation |
| `/api/verify_purchase` | POST | Coin pack purchase verification |
| `/api/verify_inapp_purchase` | POST | Subscription purchase verification |

---

## 4. IN-APP PURCHASES (RevenueCat)

### Architecture
- **RevenueCatManager** (singleton) → handles init, fetch offerings, purchase, restore
- **Entitlement ID:** `faceswap_subscription`
- **Offerings:**
  - `sub_offer` → Subscription plans (weekly/monthly/yearly with trial)
  - `video_offering` → Video-specific offerings
  - Default/Current → Coin packs
- **Product IDs:** Defined in `AppFaceProductsID.java` (e.g., `COINS_75`)

### Purchase Flow
1. User taps CTA → `purchasePackage()` → Google Play dialog
2. On success → `onPurchaseSuccess()` → server verification via API
3. On verified → `onPurchaseVerified()` → update premium/credits locally
4. On failure → retry/error dialogs

### Paywall Screen (AppFacePaywallActivity)
- Shows subscription with trial period parsing (ISO 8601)
- Dynamic pricing from RevenueCat phases
- APP_EXP variants: EXP=1 shows golden offer style, EXP≠1 shows standard
- After purchase → redirects to SplashActivity (full session refresh)
- Close/back → interstitial ad → either finish or go to MainActivity

### Store Screen (AppFaceStoreActivity)
- Shows coin packages from RevenueCat offerings
- Uses `AppFaceStoreAdapter` with selection and purchase flow
- Verification through `verifyPurchase` API (non-subscription)

---

## 5. COIN/CREDIT SYSTEM

### Architecture
- **SessionManager:** Holds `cachedCredits` (synced from splash_data API)
- **AppSystem:** Feature flags control per-feature coin requirements
- **CoinManager:** Centralized gating logic with 4-gate flow:
  1. Coin system disabled → bypass
  2. Feature doesn't require coins → bypass
  3. Balance ≥ cost + skip_check → auto-proceed
  4. Balance ≥ cost + !skip_check → confirmation dialog
  5. Balance < cost → insufficient coins dialog (Buy/Watch Ad)

### Feature Cost Resolution (SessionManager)
- Maps ACTION_* constants → feature cost keys (e.g., `face_swap`, `faceswap/basic`)
- 3-tier lookup: primary key → variant keys → fuzzy match
- Costs come from server via splash_data API `featureCosts` map

---

## 6. SESSION MANAGEMENT

### SessionManager (In-Memory Singleton)
- Initialized from `splash_data` API response in SplashActivity
- Holds: token, userId, deviceId, planType, isPremium, cachedCredits, featureCosts
- Premium override: `localPremiumOverride` (set after IAP, not persisted)
- Auth token auto-configured on ApiClient
- Cleared on logout/session expiry

### MMKV Persistence (GlobleMMKVManager)
- `APP_EXP` → experiment variant (1 = auth flow, else = non-auth)
- `IS_PRM_PRC_SHOW` → paywall price visibility flag
- `FIREBASE_DEVICE_ID`, `FIREBASE_USER_ID` → Firebase auth state
- `ONBOARDING_COMPLETE` → first-time flag

---

## 7. AD SYSTEM

### Architecture
- **AdManager** (singleton) → manages premium state, ad loading
- **BaseAppActivity.loadAds()** → called after first layout pass
- **SplashInterstitialAdManager** → splash/paywall interstitial ads
- **AppFaceActivityNavHelper.start()** → shows interstitial between activities
- **Banner ads:** `ad_view_container` and `second_ad_view_container` in layouts
- Premium users → all ads hidden

---

## 8. HOME SCREEN LAYOUT (app_face_activity_main_screen.xml)

```
RelativeLayout (root, dark background)
├── ImageView (ivBackground) — optional background
└── LinearLayout (mainContent, vertical)
    ├── Header Bar (56dp height)
    │   ├── Left: Logo icon + App name "FaceSwap"
    │   └── Right: Coin pill (balance) + Settings gear button
    ├── Divider
    ├── Ad banner (ad_view_container)
    └── Content Area (weight=1)
        ├── RecyclerView (rvMainCategories) — main scrollable content
        ├── ShimmerFrameLayout — loading skeleton
        ├── Error/Retry container
        ├── Bottom Nav Bar (elevated, 4 tabs)
        │   ├── AI Image
        │   ├── Multi/Video Swap
        │   ├── Face Swap
        │   └── My Work
        └── Bottom ad (second_ad_view_container)
```

---

## 9. TEMPLATE SYSTEM

### Data Flow
1. MainActivity.onCreate() → shows shimmer
2. `AppFaceApiRepository.getTemplates()` → fetches `/api/templates/list`
3. Response parsed → `AppFaceTemplateCategory.fromApiResponse(json)`
4. Each category → `{categoryId, categoryName, List<AppFaceTemplateItem>}`
5. Each item → `{id, templateName, prompt, categoryId, categoryName, noteMessage, isActive}`
6. Image URL → `baseUrl + "/api/templates/" + id + "/image"` (needs auth header)
7. `AppFaceMainCategoryAdapter.setCategories()` → renders sections

### Category Row Layout (app_category_row_item.xml)
- Section title + "View All" button
- 3 preview ImageViews (image1, image2, image3) loaded via Glide + auth
- Each image click → AppFaceSwapActivity with template data
- "View All" → stores templates in AppFaceTemplateCache → opens SectionDetailActivity

---

## 10. CURRENT HOME SCREEN PROBLEMS (User's Concern)
- Template sections take up ALL scrollable space
- Header banner + template categories = too much content
- Bottom nav bar provides the actual feature navigation
- User wants to MAXIMIZE home screen space for engagement
- User wants templates MOVED to a separate dedicated activity

---

## 11. DESIGN CONSTANTS
- **Font family:** `app_poppins_*` (regular, semibold, bold, extrabold)
- **Primary brand color:** `@color/app_primary_brand`
- **Dark text:** `#1A1A2E`
- **Background:** `@color/app_theme_dark_background`
- **Card background:** `@color/app_base_card_background`
- **Dimensions:** Uses `sdp` library for scalable dp units
- **Status bar:** Edge-to-edge with `AppFaceTools.setStatusBarBleed()`/`setEdgetoEdge()`
- **Button style:** Gradient backgrounds (`app_button_face_primary_bg`, `app_face_dialog_button_gradient_horizontal`)
- **Pill badges:** `app_pill_face_solid_gradient`

---

## 12. DECISIONS LOG
| # | Decision | Status |
|---|---|---|
| 1 | Remove template sections from home screen | CONFIRMED by user |
| 2 | Create new Template Gallery activity | CONFIRMED by user |
| 3 | Add attractive button on home screen for templates | CONFIRMED by user |
| 4 | No bugs, crashes, or broken functionality | MANDATORY constraint |
| 5 | Maintain UI consistency (fonts, colors, design) | MANDATORY constraint |
| 6 | User open to UI suggestions beyond consistency | CONFIRMED by user |
| 7 | Redesign hero banner (not keep as-is) | CONFIRMED by user |
| 8 | **DEPRECATED features:** Multi Swap, Couple Swap, Upscale | CONFIRMED by user |
| 9 | **ACTIVE features:** Photo Face Swap, AI Text-to-Image, Video Face Swap, Remove BG, Enhance Face | CONFIRMED by user |
| 10 | Surface Remove BG & Enhance on home screen (currently hidden) | CONFIRMED by user |
| 11 | ScrollView/Bottom Nav overlap is intentional (existing padding handles it) | CONFIRMED by user |
| 12 | Remove BG card → AppFaceRemoveBgActivity (Standalone Remove BG flow) | ✅ IMPLEMENTED |
| 13 | **HOME SCREEN REDESIGN COMPLETE** — Feature Hub with hero + templates card + 4 quick action cards | ✅ IMPLEMENTED & BUILD PASSING |
| 14 | Remove bottom navigation bar and adjust empty padding | CONFIRMED by user |
| 15 | Update global gradient color system from pink/purple to a premium teal/indigo palette | CONFIRMED by user |
| 16 | Completely remove "Change Background" (Background Replace) feature and API from codebase | CONFIRMED by user |
