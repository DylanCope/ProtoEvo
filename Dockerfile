FROM nvidia/cuda:12.1.1-devel-ubuntu20.04
FROM gradle:8.1.1-jdk8
COPY --from=0 /usr/local/cuda /usr/local/cuda
ENV PATH="/usr/local/cuda/bin:$PATH"
RUN apt update; apt install build-essential -y
RUN apt-get -y install python3-pip
RUN pip3 install --upgrade pip
RUN pip3 install -r requirements.txt