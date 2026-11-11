# Reel Counter (Powered by Maaz Sharafat • PrimeSoft Solutions)

Ye Android app Facebook, TikTok, Instagram aur YouTube Shorts mein aap ki dekhi hui reels ginti hai.
Screen par chota sa bubble ("▶ 23") hota hai jo saath saath count dikhata hai, aur app ke andar
din wise / hafta wise / mahina wise statistics hain.

Koi internet, login ya permission nahi chahiye — sirf **Accessibility** ON karni hoti hai.
Ginti sirf aap ke phone mein save hoti hai, kahin upload nahi hoti.

---

## APK kaise banayen (do aasan raaste)

### Raasta 1: GitHub se (Android Studio ki zaroorat nahi) — sab se aasan
1. github.com par free account banayen, **New repository** banayen (naam: `ReelCounter`).
2. Is zip ko extract karein. Repository mein **uploading an existing file** se saari files/folders upload karein
   (`.github` folder samet — ye chhupa hua folder hai, usay bhi drag karein).
   Agar `.github` upload na ho: **Add file → Create new file**, naam likhein `.github/workflows/build.yml`
   aur is zip wali `build.yml` ka text paste kar dein.
3. Repository mein **Actions** tab kholein → **Build APK** → **Run workflow**. 3-5 minute intezar karein.
4. Poora hone par us run ko kholein, neeche **Artifacts** mein `ReelCounter-APK` download karein (zip ke andar `app-debug.apk`).

### Raasta 2: Android Studio se
1. Android Studio kholein → **Open** → is folder ko select karein, sync hone dein.
2. **Build → Build APK(s)**. APK `app/build/outputs/apk/debug/app-debug.apk` mein milti hai.

---

## Phone mein install aur chalana
1. `app-debug.apk` phone mein bhej dein (WhatsApp/USB/Drive) aur kholein. "Unknown source" ki ijazat den.
   Play Protect warning aaye to "Install anyway" karein (kyun ke ye Play Store ki app nahi hai).
2. **Reel Counter** kholein → **Accessibility kholein** → list mein *Reel Counter* → **ON**.
   - Android 13+ par agar ON nahi hota: **App info kholein** → upar ⋮ → **Restricted settings allow karein** → phir dobara ON karein.
3. Bas! Ab Instagram/TikTok/YouTube/Facebook mein reels dekhein, bubble mein count barhta rahega.

Dosron ko dene ke liye wahi `app-debug.apk` bhej dein.

## Zaroori batein
- **Battery**: Xiaomi/Oppo/Vivo/Samsung par Settings → Apps → Reel Counter → Battery → *No restrictions* (aur Autostart ON), warna phone service band kar sakta hai.
- **Detection andaza (heuristic) par hai**: har app ki Reels screen pehchan kar swipe gina jata hai. Ye apps update hoti rehti hain,
  is liye kabhi count ghat/barh sakta hai. TikTok sab se behtar chalta hai; Facebook sab se kam pakka hai.
  Agar kisi app mein ginti na ho to app ke andar **Loose mode** ON kar dein (phir us app ka har vertical scroll gina jata hai).
  Pehchan `Store.kt` mein `ReelApp` ki `hints` list se badli ja sakti hai.
- Ek swipe = ek reel. Reels tab kholne par pehli reel count nahi hoti, agli swipe se ginti shuru hoti hai.
- Aaj ki ginti raat 12 baje khud naye din mein chali jati hai.

## Files
- `app/src/main/java/com/primesoft/reelcounter/ReelService.kt` — ginti aur bubble
- `.../Store.kt` — data aur statistics
- `.../MainActivity.kt`, `BarChartView.kt` — app ki screen aur chart
- `.github/workflows/build.yml` — GitHub par automatic APK banane ka tareeqa
