#!/usr/bin/env python3
"""
ASTPredict Model Conversion Script
Converts YOLOv8 PyTorch model (best.pt) to TensorFlow Lite format for Android deployment.

Usage:
    python convert_model.py [--input best.pt] [--output app/src/main/assets/]

Requirements:
    pip install ultralytics tensorflow
"""

import os
import sys
import argparse
from pathlib import Path

def convert_to_tflite(model_path: str, output_dir: str, quantize: bool = False):
    """Convert YOLOv8 .pt model to TFLite format."""
    try:
        from ultralytics import YOLO
    except ImportError:
        print("ERROR: ultralytics not installed. Run: pip install ultralytics")
        sys.exit(1)

    print("=" * 60)
    print("ASTPredict Model Conversion")
    print("=" * 60)

    # Check model exists
    if not os.path.exists(model_path):
        print(f"✗ Model not found: {model_path}")
        sys.exit(1)

    print(f"\nSource model: {model_path}")
    print(f"Output dir:   {output_dir}")

    # Load model
    print("\n1. Loading YOLOv8 model...")
    model = YOLO(model_path)

    # Get model info
    print(f"   Classes: {len(model.names)} ({', '.join(list(model.names.values())[:5])}...)")
    print(f"   Model size: {os.path.getsize(model_path) / (1024*1024):.1f} MB")

    # Create output directory
    os.makedirs(output_dir, exist_ok=True)

    # Step 1: Export to TFLite (FP32)
    print("\n2. Exporting to TFLite (FP32)...")
    exported = model.export(
        format="tflite",
        imgsz=640,
        half=False
    )
    print(f"   Exported: {exported}")

    # Find the exported file
    exported_path = Path(exported)
    if exported_path.exists():
        output_file = os.path.join(output_dir, "best_float32.tflite")

        # Copy to assets
        import shutil
        shutil.copy2(str(exported_path), output_file)

        output_size = os.path.getsize(output_file) / (1024 * 1024)
        print(f"   Saved to: {output_file}")
        print(f"   Size: {output_size:.1f} MB")

    # Step 2: Optional INT8 quantization
    if quantize:
        print("\n3. Exporting INT8 quantized model...")
        try:
            exported_int8 = model.export(
                format="tflite",
                imgsz=640,
                int8=True,
                data="coco128.yaml"  # Calibration dataset
            )
            if Path(exported_int8).exists():
                output_int8 = os.path.join(output_dir, "best_int8.tflite")
                import shutil
                shutil.copy2(str(exported_int8), output_int8)
                int8_size = os.path.getsize(output_int8) / (1024 * 1024)
                print(f"   Saved to: {output_int8}")
                print(f"   Size: {int8_size:.1f} MB")
        except Exception as e:
            print(f"   ⚠ INT8 quantization failed: {e}")
            print("   Continuing with FP32 model only.")

    print("\n" + "=" * 60)
    print("✓ Conversion Complete!")
    print("=" * 60)
    print(f"\nNext steps:")
    print(f"  1. The TFLite model has been placed in: {output_dir}")
    print(f"  2. Open the Android project in Android Studio")
    print(f"  3. Build and run on your device")
    print(f"\nNote: The model file name in ColonyDetector.kt is 'best_float32.tflite'")
    print(f"If you renamed the file, update MODEL_FILE in ColonyDetector.kt")


def main():
    parser = argparse.ArgumentParser(description="Convert YOLOv8 model to TFLite for Android")
    parser.add_argument("-i", "--input", default="../best.pt",
                        help="Path to the YOLOv8 .pt model (default: ../best.pt)")
    parser.add_argument("-o", "--output", default="app/src/main/assets/",
                        help="Output directory for TFLite model (default: app/src/main/assets/)")
    parser.add_argument("--quantize", action="store_true",
                        help="Also produce INT8 quantized model")
    args = parser.parse_args()

    convert_to_tflite(args.input, args.output, args.quantize)


if __name__ == "__main__":
    main()
