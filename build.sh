#!/bin/bash
cd D:/JAVA/LTUDM/LTM
echo "=== Building project ==="
mvn clean compile -q
if [ $? -eq 0 ]; then
    echo "✓ Build successful!"
else
    echo "✗ Build failed!"
    exit 1
fi

