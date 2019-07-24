wget -O protoc-2.5.0 http://repo1.maven.org/maven2/com/google/protobuf/protoc/2.5.0/protoc-2.5.0-linux-x86_64.exe
wget -O protoc http://repo1.maven.org/maven2/com/google/protobuf/protoc/3.9.0/protoc-3.9.0-linux-x86_64.exe

chmod +x protoc
chmod +x protoc-2.5.0
sudo cp protoc-2.5.0 protoc /usr/local/bin/

