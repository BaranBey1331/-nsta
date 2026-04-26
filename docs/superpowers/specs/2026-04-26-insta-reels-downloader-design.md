# Tasarım Dokümanı: Instagram Reels Downloader

**Tarih:** 2026-04-26  
**Durum:** Taslak  
**Yazar:** Gemini CLI

## 1. Giriş
Bu proje, kullanıcıların Instagram Reels ve video postlarını URL yardımıyla Android cihazlarına indirmelerini sağlayan, yüksek performanslı ve hafif bir uygulamadır.

## 2. Teknik Gereksinimler
- **Minimum Android Sürümü:** Android 9 (API 28)
- **Hedef Android Sürümü:** Android 15 (API 35)
- **Desteklenen Mimariler:** Arm64-v8a, Armeabi-v7a
- **Bellek Hedefi:** < 256MB RAM (Uygulama aktifken)
- **Derleme Sistemi:** Gradle (Kotlin DSL)

## 3. Teknoloji Yığını
- **UI:** Jetpack Compose (Material 3)
- **Core Logic:** Rust (JNI üzerinden entegre)
  - `reqwest`: HTTP istekleri için.
  - `scraper` veya regex: HTML analizi ve video URL ayıklama için.
- **Veri Saklama:** Room (İndirme geçmişi için).
- **Ağ:** OkHttp (Büyük dosya indirmeleri ve ilerleme takibi için).

## 4. Kullanıcı Deneyimi (UI/UX)
`examplereelsdownloader.png` görseline sadık kalınarak:
- **Üst Kısım:** Ayarlar ikonu ve "Reels Downloader" başlığı.
- **Link Girişi:** "Paste reel link here" ipuçlu text alanı, "Paste" butonu ve temizleme ikonu.
- **Download Butonu:** Gradyan renkli (Mor -> Pembe) geniş buton.
- **Aktif İndirme Paneli:** 
  - Video önizleme görseli ve başlığı.
  - Dairesel ilerleme çubuğu (% gösterimli).
  - Dosya boyutu ve anlık indirme hızı.
- **Geçmiş (Recent Downloads):** Yatay kaydırılabilir kartlar.

## 5. Uygulama Akışı
1. Kullanıcı linki yapıştırır.
2. Rust tarafındaki JNI modülü çağrılır:
   - Instagram sayfası çekilir.
   - Meta tag'lerden veya JSON script'lerinden `og:video` veya benzeri kaynak URL bulunur.
3. Bulunan URL Kotlin tarafına iletilir.
4. Android `DownloadManager` veya özel bir `OkHttp` interceptor ile dosya `Movies` klasörüne indirilir.
5. İndirme tamamlanınca geçmişe eklenir.

## 6. Test Durumları
Aşağıdaki URL'ler ile doğrulama yapılacaktır:
- `https://www.instagram.com/reel/DXXlxgaCmqf/`
- `https://www.instagram.com/p/DXaarjVDWB1/`
- `https://www.instagram.com/reel/DXemXckiESX/`

## 7. Kısıtlamalar
- Giriş yapma (Login) gerektiren gizli hesaplar kapsam dışıdır.
- Sadece halka açık (public) içerikler desteklenir.
