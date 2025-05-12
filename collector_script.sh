sudo apt update
sudo apt install -y openjdk-17-jdk
javac MetricCollector.java
if [ $? -eq 0 ]; then
    echo "Running MetricCollector..."
    java MetricCollector
else
    echo "Compilation failed. Please check MetricCollector.java for errors."
fi