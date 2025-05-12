sudo apt update
sudo apt install -y openjdk-17-jdk
javac MetricExporter.java
if [ $? -eq 0 ]; then
    echo "Running MetricExporter..."
    java MetricExporter
else
    echo "Compilation failed. Please check MetricExporter.java for errors."
fi