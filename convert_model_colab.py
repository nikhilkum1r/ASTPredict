# ASTPredict Google Colab Model Converter
# ==============================================================================
# This script is optimized to run inside a Google Colab notebook.
#
# How to use in Google Colab:
# 1. Open Google Colab (https://colab.research.google.com)
# 2. Create a new Python 3 notebook.
# 3. Copy this entire script, paste it into a code cell, and run it.
# 4. Upload your 'best.pt' PyTorch model when prompted, or upload it using 
#    the files sidebar in Colab.
# 5. Run the cell. The model will be converted and 'best_float32.tflite' 
#    will be automatically downloaded to your local computer!
# ==============================================================================

import os
import sys

# 1. Check if running in Google Colab
try:
    import google.colab
    IN_COLAB = True
except ImportError:
    IN_COLAB = False

if not IN_COLAB:
    print("WARNING: This script is optimized for Google Colab!")
    print("To run locally, please use 'convert_model.py' instead.")
    print("If you still want to run this in Colab, copy this file's content into a Colab cell.")

# 2. Install requirements in Google Colab
print("\n--- Step 1: Installing Required Packages (ultralytics, onnx, onnx2tf) ---")
import subprocess
try:
    import ultralytics
    import onnx
    import onnx2tf
    print("✓ Required packages are already installed.")
except ImportError:
    print("Installing requirements...")
    subprocess.run([sys.executable, "-m", "pip", "install", "ultralytics", "onnx", "onnx2tf"], check=True)
    import ultralytics
    import onnx
    import onnx2tf
    print("✓ All requirements installed successfully!")

# 3. Handle model file upload
print("\n--- Step 2: Uploading PyTorch model ('best.pt') ---")
from ultralytics import YOLO

model_filename = "best.pt"
colab_content_path = "/content/best.pt"

if IN_COLAB:
    if os.path.exists(colab_content_path):
        model_filename = colab_content_path
        print(f"✓ Found uploaded 'best.pt' at {model_filename}")
    elif not os.path.exists(model_filename):
        print("Please upload your 'best.pt' file using the file selector below:")
        from google.colab import files
        uploaded = files.upload()
        # Find the uploaded .pt file if it wasn't named best.pt
        for name in uploaded.keys():
            if name.endswith(".pt") and name != model_filename:
                os.rename(name, model_filename)
                print(f"Renamed {name} to {model_filename}")
                break
    else:
        print(f"✓ Found 'best.pt' in the current workspace.")
else:
    # If run locally, look for it in parent directory or prompt
    if not os.path.exists(model_filename):
        parent_pt = os.path.join("..", "ASTPredict", "best.pt")
        if os.path.exists(parent_pt):
            model_filename = parent_pt
            print(f"✓ Found model at: {model_filename}")
        else:
            print(f"ERROR: '{model_filename}' not found. Please place it in this directory.")
            sys.exit(1)

# 4. Convert YOLOv8 to TFLite (FP32)
print("\n--- Step 3: Converting PyTorch model to TFLite format ---")
try:
    print(f"Loading PyTorch model: {model_filename}...")
    model = YOLO(model_filename)
    
    print("Exporting model to TFLite (FP32, size=640x640)...")
    # half=False guarantees FP32 format matching ColonyDetector.kt requirements
    exported_path_str = model.export(format="tflite", imgsz=640, half=False)
    print(f"✓ Model successfully converted: {exported_path_str}")
    
    # YOLOv8 export usually creates a folder called 'best_saved_model'
    # containing 'best_float32.tflite' or 'best-fp16.tflite' depending on options.
    # Let's locate the .tflite file.
    tflite_file = None
    if os.path.exists("best_saved_model/best_float32.tflite"):
        tflite_file = "best_saved_model/best_float32.tflite"
    elif os.path.exists(exported_path_str):
        if exported_path_str.endswith(".tflite"):
            tflite_file = exported_path_str
        elif os.path.isdir(exported_path_str):
            for f in os.listdir(exported_path_str):
                if f.endswith(".tflite"):
                    tflite_file = os.path.join(exported_path_str, f)
                    break

    if tflite_file and os.path.exists(tflite_file):
        print(f"✓ Located TFLite model at: {tflite_file}")
        
        # 5. Trigger download in Google Colab
        if IN_COLAB:
            print("\n--- Step 4: Downloading converted TFLite model ---")
            print("Your browser should now prompt you to download 'best_float32.tflite'.")
            files.download(tflite_file)
        else:
            # If run locally, copy it directly to app assets
            assets_dir = "app/src/main/assets"
            if os.path.exists(assets_dir):
                import shutil
                dest = os.path.join(assets_dir, "best_float32.tflite")
                shutil.copy2(tflite_file, dest)
                print(f"✓ Copied converted model directly to app assets: {dest}")
            else:
                print(f"Conversion finished. You can find the model at: {tflite_file}")
    else:
        print("✗ Error: Could not locate the converted .tflite file in export output.")
        
except Exception as e:
    print(f"✗ Conversion failed: {str(e)}")
    raise e

print("\n==============================================================================")
print("NEXT STEPS:")
print("1. Take the downloaded 'best_float32.tflite' file.")
print("2. Put it in the Android project folder:")
print("   ASTPredict-app/app/src/main/assets/")
print("3. Build and run the Android App. The 'ColonyDetector not initialized' error will be resolved!")
print("==============================================================================")
