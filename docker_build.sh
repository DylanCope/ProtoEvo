systemctl --user enable docker.service && systemctl --user start docker.service
docker build -t dylan/protoevo:latest .