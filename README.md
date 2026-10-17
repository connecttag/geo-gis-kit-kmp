# ConnectTag Geo-GIS Kit (KMP) 🗺️

[![JitPack](https://jitpack.io/v/connecttag/geo-gis-kit-kmp.svg)](https://jitpack.io/#connecttag/geo-gis-kit-kmp)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20JVM%20%7C%20iOS-green.svg)](https://kotlinlang.org/docs/multiplatform.html)

**ConnectTag Geo-GIS Kit** هي حزمة برمجية متكاملة وخفيفة الوزن لنظم المعلومات الجغرافية (GIS) والعمليات المكانية والجيوديسية، مبنية بنظام **Kotlin Multiplatform (KMP)** لتعمل بسلاسة على **Android** و **Desktop JVM** و **iOS**.

---

## 🚀 المميزات الرئيسية (Key Features)

- 📐 **العمليات الرياضية والجيوديسية (Geodesic Math):**
  - حساب المسافات بدقة عبر صيغتي Haversine و Vincenty.
  - حساب زوايا السير والاتجاه الأولي (Azimuth / Bearing).
  - حساب نقطة الوصول بناءً على الإحداثيات والمسافة وزاوية السير (`destinationPoint`).
  - حساب المساحة الجيوديسية للمضلعات الجغرافية (Shoelace Geodesic Polygon Area).
  - خوارزمية فحص وقوع النقطة داخل مضلع جغرافي (Ray-Casting Point-in-Polygon).
  - تحويل وتوليد الجيوهاش (Geohash Encoding).
  - إسقاط بلاطات الويب ميركاتور (Mercator Tiles X/Y/Zoom).

- 🧩 **تجميع الأهداف والنقاط الجغرافية (Spatial Clustering):**
  - خوارزمية تجميع جغرافي نقية وخفيفة (Pure Kotlin Spatial Clusterer) لتكتيل المعالم على الخريطة التفاعلية.

- 📄 **دعم صيغ وتبادل بيانات الـ GIS (Formats Engine):**
  - **GeoJSON:** قراءة وتدفق كتابة مواصفات RFC 7946 (FeatureCollection, Features, Geometries) بأمان وحماية ضد استهلاك الذاكرة.
  - **KML / KMZ:** قراءة وتوليد ملفات KML للنقاط والخطوط والمضلعات مع الألوان والأنماط (Styles, LineStyle, PolyStyle).
  - **GPX 1.1:** قراءة وتوليد ملفات GPS Exchange Format للمسارات (Tracks)، مقاطع المسار (Segments)، والعلامات الجغرافية (Waypoints) مع الارتفاع والسرعة والتوقيت.
  - **WKT (Well-Known Text):** تحويل المعالم إلى نصوص معيارية (POINT, LINESTRING, POLYGON).

---

## 📦 التثبيت (Installation)

أضف مستودع JitPack في ملف `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

ثم أضف الاعتمادية في ملف `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.connecttag:geo-gis-kit-kmp:1.0.0")
}
```

---

## 💻 أمثلة الاستخدام (Usage Examples)

### 1. العمليات الجيوديسية وحساب المسافات
```kotlin
import org.connecttag.lib.geo.GeoCoordinate
import org.connecttag.lib.geo.GeoMathUtils

val sanaa = GeoCoordinate(15.3694, 44.1910)
val aden = GeoCoordinate(12.7855, 45.0187)

// حساب المسافة بالمتر
val distanceMeters = GeoMathUtils.haversineDistanceMeters(sanaa, aden)

// حساب زاوية السير (Bearing)
val bearing = GeoMathUtils.calculateBearing(sanaa.latitude, sanaa.longitude, aden.latitude, aden.longitude)

// فحص وجود نقطة داخل منطقة/مضلع
val isInside = GeoMathUtils.isPointInPolygon(targetPoint, polygonBoundary)
```

### 2. قراءة وكتابة GeoJSON
```kotlin
import org.connecttag.lib.gis.GeoJson

val geoJsonText = """{"type":"FeatureCollection","features":[...]}"""
val features = GeoJson.read(geoJsonText)

// كتابة GeoJSON
val outputString = buildString {
    GeoJson.write(features.asSequence()) { append(it) }
}
```

### 3. قراءة وتوليد ملفات GPX
```kotlin
import org.connecttag.lib.gis.GpxParser
import org.connecttag.lib.gis.GpxWriter

// تحليل ملف GPX
val gpxDoc = GpxParser.parse(gpxXmlContent)
println("Loaded ${gpxDoc.tracks.size} tracks and ${gpxDoc.waypoints.size} waypoints")

// كتابة مستند GPX
val xml = GpxWriter.write(gpxDoc)
```

---

## 🛡️ الترخيص (License)
مرخص تحت رخصة [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt).
