# 🧬 ASTPredict Android Application
### *AI-Powered On-Device Veterinary Bacterial Colony Detection & Pathogen Identification*

[![Platform](https://img.shields.io/badge/Platform-Android_API_26%2B-green.svg?style=flat-square&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-blue.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose_/_Material_3-8A2BE2.svg?style=flat-square&logo=jetpackcompose)](https://developer.android.com/compose)
[![ML Engine](https://img.shields.io/badge/ML_Engine-TensorFlow_Lite-orange.svg?style=flat-square&logo=tensorflow)](https://www.tensorflow.org/lite)
[![Database](https://img.shields.io/badge/Database-Room_SQL-blue.svg?style=flat-square&logo=sqlite)](https://developer.android.com/training/data-storage/room)

**ASTPredict** is a state-of-the-art on-device computer vision mobile application designed for veterinary microbiologists, researchers, and clinicians. By executing a high-performance **YOLOv8** deep learning model completely offline, ASTPredict detects and classifies **24 species** of veterinary-relevant bacterial pathogens from agar culture plates.

The application serves as a point-of-care digital tool to accelerate pathogen identification, supporting global workflows combating **Antimicrobial Resistance (AMR)** under the **One Health** framework.

---

## 🗺️ High-Level Technical Architecture

The application is built on a robust, offline-first MVVM (Model-View-ViewModel) pattern, adhering strictly to **Android Clean Architecture** guidelines.

```mermaid
graph TD
    subgraph UI Layer (Compose & Material 3)
        A[CameraScreen / Live Preview] -->|CameraX ImageAnalysis| B(Image Frame Analyzer)
        C[Gallery Import / Photo Selection] -->|URI Selection| D(Uri Processing)
        E[HomeScreen / Statistics / History]
    end

    subgraph Data & ML Layer (C++ & Kotlin Core)
        B -->|Bitmap Buffer| F[ColonyDetector - TFLite Engine]
        D -->|Decoded Bitmap| F
        F -->|Memory-Mapped Inference| G[Interpreter / GPU Delegate]
        G -->|Output Tensor [1x28x8400]| H[Output Parser & Letterbox Inversion]
        H -->|Class-Agnostic NMS| I[Analysis Result Object]
    end

    subgraph Persistence Layer (Offline Storage)
        I -->|Database Save| J[Room Database: AppDatabase]
        J -->|JSON Serializer| K[AnalysisEntity]
        K -->|DAO| L[(SQLite Local DB)]
    end

    L -->|Observe Live Data| E
```

### 1. Presentation Layer (Jetpack Compose & CameraX)
- **Fluid & Premium UI**: Styled using an Outfit/Inter-based premium dark-theme design system featuring dynamic micro-animations, glassmorphic card layouts, and intuitive overlays.
- **Adaptive CameraX Capture**: Implements real-time analysis overlays with an interactive petri dish alignment guide, optimizing capture angle, perspective, and lighting.
- **Coil Image Loading**: Memory-efficient asynchronous loading and caching for local media resources and captured plate images.

### 2. ML Inference Layer (TensorFlow Lite Core)
- **Memory-Mapped Loading**: Bypasses traditional file I/O overheads by reading the uncompressed `.tflite` model directly from APK assets via `MappedByteBuffer` using file descriptor offsets (`assets.openFd`).
- **GPU Delegate Acceleration**: Optional sub-millisecond execution using the mobile hardware's OpenCL/Vulkan GPU pipeline with automatic CPU fallback.
- **Pre-processing (Letterboxing)**: Automatically resizes raw inputs to a strict `640x640` square while maintaining aspect ratio, padding the remaining borders with gray (`rgb(114,114,114)`) to prevent object distortion.
- **Post-processing & NMS**: Custom implementation of class-agnostic **Non-Maximum Suppression (NMS)** using adjustable Intersection over Union (IoU) and confidence thresholds, resolving overlapping predictions in dense colony clusters.

### 3. Persistence Layer (Room ORM)
- **Local Relational SQLite DB**: Stores full plate diagnostics history including image URIs, species-level breakdowns, exact bounding-box coordinates, average confidence metrics, and processing times.
- **JSON Serialization**: Integrates Gson-backed type converters to securely serialize complex colony coordinates into database entities.

---

## 🧠 YOLOv8 TFLite Model Pipeline

```
  +--------------+     +-------------------+     +---------------------+
  | Input Image  | --> | Letterbox Resize  | --> | Normalization [0-1] |
  | (Any Aspect) |     |  (640x640 Padded) |     | (4x640x640x3 Float) |
  +--------------+     +-------------------+     +---------------------+
                                                            |
                                                            v
  +--------------+     +-------------------+     +---------------------+
  | Results &    | <-- | Class-Agnostic    | <-- | TFLite Interpreter  |
  | BBoxes (24c) |     | NMS Filtering     |     | (CPU / GPU Delegate) |
  +--------------+     +-------------------+     +---------------------+
```

### Model Specifications
- **Format**: TensorFlow Lite (FP32 precision)
- **Weight Size**: `12.3 MB` (YOLOv8 nano variant optimized for mobile edge deployment)
- **Input Tensor**: `[1, 640, 640, 3]` (Format: `NHWC`, Data Type: `Float32`)
- **Output Tensor**: `[1, 28, 8400]`
  - **4 Bounding Box Anchors**: `[x_center, y_center, width, height]` (normalized coordinates)
  - **24 Class Scores**: Pathogen probabilities mapped to specific classes

> [!NOTE]
> The letterbox algorithm scale factor ($S$) and padding ($P$) are calculated dynamically and mathematically inverted during post-processing to map bounding boxes precisely onto the original high-resolution camera photograph, ensuring clinical precision.

---

## 🧬 24 Detectable Veterinary Pathogens

ASTPredict is trained to identify the following pathologically and economically significant veterinary pathogens:

| Class ID | Bacterial Pathogen | Typical Colony Morphology | Primary Clinical / Veterinary Significance |
|:---:|:---|:---|:---|
| **0** | *Actinobacillus equuli* | Sticky, grayish, non-hemolytic colonies | Equine sleepiness, septicemia, and joint-ill in foals |
| **1** | *Actinobacillus pleuropneumoniae* | Small, shiny colonies, requires V-factor | Highly contagious porcine pleuropneumonia |
| **2** | *Aeromonas hydrophila* | Large, grey, smooth, beta-hemolytic | Hemorrhagic septicemia in fish, amphibians, and reptiles |
| **3** | *Bacillus cereus* | Large, feathery, frothy-grey, beta-hemolytic | Foodborne emetic toxin, bovine mastitis, food poisoning |
| **4** | *Bibersteinia trehalosi* | Smooth, grayish, beta-hemolytic, sweet odor | Ovine septicemia and severe shipping fever pneumonia in cattle |
| **5** | *Bordetella bronchiseptica* | Tiny, smooth, dew-drop-like colonies | Canine infectious tracheobronchitis (Kennel Cough), atrophic rhinitis in pigs |
| **6** | *Brucella ovis* | Small, circular, translucent, non-hemolytic | Ovine epididymitis, ram infertility, and abortion |
| **7** | *Clostridium perfringens* | Spreading, double-zone beta-hemolysis | Necrotizing enteritis, gas gangrene, and enterotoxemia |
| **8** | *Corynebacterium pseudotuberculosis* | Dry, opaque, cream-colored, beta-hemolytic | Caseous lymphadenitis ("cheesy gland") in sheep/goats |
| **9** | *Erysipelothrix rhusiopathiae* | Tiny, alpha-hemolytic, transparent ("dew-drop") | Swine erysipelas (diamond skin disease), zoonotic erysipeloid |
| **10** | *Escherichia coli* | Flat, grey, damp colonies; metallic sheen on EMB | Colibacillosis, calf scours, bovine mastitis, UTIs |
| **11** | *Glaesserella parasuis* | Minute, satellite-like growth near staph | Glässer's disease (fibrinous polyserositis) in weaner pigs |
| **12** | *Klebsiella pneumoniae* | Large, extremely mucoid, glistening colonies | Bovine mastitis, equine endometritis, canine UTIs |
| **13** | *Listeria monocytogenes* | Tiny, translucent, narrow zone of beta-hemolysis | Listeriosis in sheep/cattle (circling disease), septic abortion |
| **14** | *Paenibacillus larvae* | Flat, brownish, irregular edges | American foulbrood, a lethal bacterial disease of honeybee larvae |
| **15** | *Pasteurella multocida* | Small, grey-white, sweet/musty mustelid odor | Fowl cholera, swine atrophic rhinitis, bovine hemorrhagic septicemia |
| **16** | *Proteus mirabilis* | Circular, thin-film swarming waves across plate | Severe wound infections, canine otitis externa, cystitis |
| **17** | *Pseudomonas aeruginosa* | Large, metallic-green sheen, grape-like odor | Pyocyanin pigment, fleece rot, equine metritis, canine otitis |
| **18** | *Rhodococcus equi* | Glistening, salmon-pink mucoid colonies (late) | Severe pyogranulomatous bronchopneumonia in foals |
| **19** | *Salmonella enterica* | Translucent, smooth; black centers on XLD/HE | Salmonellosis, equine colic, swine paratyphoid, zoonotic food poison |
| **20** | *Staphylococcus aureus* | Golden-yellow, round, smooth, beta-hemolytic | Mastitis in dairy cattle, bumblefoot in poultry, abscesses, MRSA |
| **21** | *Staphylococcus hyicus* | White, non-hemolytic, smooth | Exudative epidermitis ("Greasy Pig Disease") in piglets |
| **22** | *Streptococcus agalactiae* | Small, grey, soft, narrow zone of beta-hemolysis | Contagious bovine mastitis, septicemia in warm-water fish |
| **23** | *Trueperella pyogenes* | Pinpoint, dry, white, sharp beta-hemolysis | Suppurative mastitis, summer mastitis, liver abscesses in ruminants |

---

## 🛠️ Build and Installation Instructions

### Prerequisites
* **Android Studio**: Iguana (2023.2.1) or newer
* **Java Development Kit (JDK)**: JDK 17
* **Android SDK**: API level 26 (Android 8.0 Oreo) up to API level 36 (Android 15+)
* **Build System**: Kotlin DSL Gradle Build System

---

### Step 1: Clone and Configure Environment
Verify or update the location of the Android SDK in the `local.properties` file located at the root of the project:
```properties
sdk.dir=/home/nikhil/Android/Sdk
```

---

### Step 2: Model Deployment to Assets
The TensorFlow Lite model must be located in the application's assets folder prior to compilation.

```bash
# Verify the destination assets folder exists
mkdir -p app/src/main/assets/

# Copy the TFLite model to the assets folder
cp best_float32.tflite app/src/main/assets/best_float32.tflite
```

> [!IMPORTANT]
> Do **NOT** rename `best_float32.tflite` to another name, as it is dynamically mapped by the memory asset reader. If you do use a different model name, you must update the constant `MODEL_FILE` inside [ColonyDetector.kt](file:///home/nikhil/Startup/maam/ASTPredict-app/app/src/main/java/com/astpredict/app/data/ml/ColonyDetector.kt).

---

### Step 3: Build the Application
Compile the debug application to generate the optimized APK containing the packed, uncompressed model.

```bash
# Clean and compile
./gradlew assembleDebug
```
The resulting APK is written to:
`app/build/outputs/apk/debug/app-debug.apk`

---

### Step 4: Installation onto Target Device
Make sure a physical Android device is connected via USB with **USB Debugging** enabled, or that an emulator is active.

```bash
# 1. Verify device is recognized
/home/nikhil/Android/Sdk/platform-tools/adb devices

# 2. Push and install the generated APK
/home/nikhil/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🩺 Troubleshooting

### 1. `DeviceException: No connected devices!`
* **Cause**: ADB daemon has lost connection to the physical Android device, or USB debugging is disabled.
* **Solution**:
  1. Unplug and replug the USB cable.
  2. Toggle **USB Debugging** OFF and ON under *Settings > Developer Options* on the target device.
  3. Reset the ADB server on your PC:
     ```bash
     /home/nikhil/Android/Sdk/platform-tools/adb kill-server
     /home/nikhil/Android/Sdk/platform-tools/adb start-server
     ```

### 2. `InstallException: EOF` during APK installation
* **Cause**: High-throughput data stream interrupted over low-quality or loose USB cables/ports while transferring the 56.8 MB package.
* **Solution**: 
  1. Bypass Gradle's internal transport by deploying using the direct adb command: `/home/nikhil/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`.
  2. Switch from a USB Hub/front-panel port to a high-speed motherboard-connected USB 3.0 port.

### 3. Out-Of-Memory (OOM) Errors on Ultra-High Resolution Images
* **Cause**: Decoding and manipulating massive raw multi-megapixel camera files inside the mobile runtime JVM.
* **Solution**: ASTPredict includes automatic safe downsampling. The `loadBitmapFromUri` routine dynamically assesses image dimensions and adjusts `inSampleSize` to restrict loaded dimensions to a maximum of 4096px before passing to the letterboxing stage.

---

## 📊 Veterinary & AMR Research Context

Pathogen identification is the critical first step in determining appropriate **Antimicrobial Susceptibility Testing (AST)** guidelines (such as **CLSI VET** standards). By providing instant, offline, on-device species identification directly from colony photographs, ASTPredict aids in:
- Speeding up diagnostics in remote field-work veterinary hospitals.
- Preventing the misuse of broad-spectrum antibiotics by steering early clinical insights.
- Enabling lower-cost surveillance of infectious animal diseases in bio-security operations.

---
*Developed for Veterinary Microbiology and One Health diagnostics — AI where it matters.*
