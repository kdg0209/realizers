# Apache Flink Standalone Cluster Setup (Docker 기반)

#### 목표

- Standalone 모드의 Flink 클러스터를 멀티 노드 구조로 직접 구성
- Docker / Docker Compose 자동화 대신, 설치·설정 과정을 직접 수행하며 이해
- 총 3개의 Docker 컨테이너를 각각 하나의 인스턴스처럼 사용

#### 실습 환경

- OS: Ubuntu 22.04
- Java: OpenJDK 17
- Flink: 2.2.0
- Container 수: 3
- Network: Docker bridge (고정 IP 사용)

<br>

## 1. 인프라 구성

### 1-1. Docker Network 생성

```shell
> docker network create --driver bridge --subnet 172.18.0.0/16 flink-network
> docker network ls
NETWORK ID     NAME            DRIVER    SCOPE
9a41a868214b   flink-network   bridge    local
```

<br>

### 1-2. Ubuntu 22.04 컨테이너 3개 띄우기 (고정 IP + hostname)

| Node        | 역할                       | IP          |
| ----------- | ------------------------ | ----------- |
| flink-node1 | JobManager + TaskManager | 172.18.0.11 |
| flink-node2 | TaskManager              | 172.18.0.12 |
| flink-node3 | TaskManager              | 172.18.0.13 |


```shell
# node 1
> docker run -itd --name flink-node1 --hostname flink-node1 --network flink-network --ip 172.18.0.11 -p 8081:8081 ubuntu:22.04

# node 2
> docker run -itd --name flink-node2 --hostname flink-node2 --network flink-network --ip 172.18.0.12 ubuntu:22.04

# node 3
> docker run -itd --name flink-node3 --hostname flink-node3 --network flink-network --ip 172.18.0.13 ubuntu:22.04
```

<br>

### 1-3. 컨테이너 간 네트워크 통신 확인

- node1 컨테이너에 접속하여 node2, node3 ping 확인

```shell
> docker exec -it flink-node1 /bin/bash
> apt update && apt install -y iputils-ping
> ping -c 2 172.18.0.12
ping -c 2 172.18.0.12
PING 172.18.0.12 (172.18.0.12) 56(84) bytes of data.
64 bytes from 172.18.0.12: icmp_seq=1 ttl=64 time=0.433 ms
64 bytes from 172.18.0.12: icmp_seq=2 ttl=64 time=0.286 ms

--- 172.18.0.12 ping statistics ---
2 packets transmitted, 2 received, 0% packet loss, time 1067ms
rtt min/avg/max/mdev = 0.286/0.359/0.433/0.073 ms

> ping -c 2 172.18.0.13
PING 172.18.0.13 (172.18.0.13) 56(84) bytes of data.
64 bytes from 172.18.0.13: icmp_seq=1 ttl=64 time=0.230 ms
64 bytes from 172.18.0.13: icmp_seq=2 ttl=64 time=0.745 ms

--- 172.18.0.13 ping statistics ---
2 packets transmitted, 2 received, 0% packet loss, time 1037ms
rtt min/avg/max/mdev = 0.230/0.487/0.745/0.257 ms
```

<br>

## 2. Java & Flink 설치

- 각 컨테이너에 접속하여 동일한 방식으로 설치

### 2-1. Java 17 설치

```shell
> apt update
> apt install -y openjdk-17-jdk wget tar vim procps
> java -version
```

<br>

### 2-2. Flink 다운로드 및 설치

```shell
> cd opt
> wget https://dlcdn.apache.org/flink/flink-2.2.0/flink-2.2.0-bin-scala_2.12.tgz
> tar -xzf flink-2.2.0-bin-scala_2.12.tgz
```

<br>

### 2-3. 심볼릭 링크 생성

```shell
> ln -s /opt/flink-flink-2.2.0 /opt/flink
```

#### 💡 심볼릭 링크란

- 버전 디렉토리를 그대로 두고, 항상 같은 이름으로 Flink를 참조하기 위함
- 실제 디렉토리는 **/opt/flink-flink-2.2.0** 이지만 가짜 이름 **/opt/flink**을 할당하여 /opt/flink -> /opt/flink-flink-2.2.0를 가리키도록 설정
- 나중에 버전이 변경되더라도 심볼릭 링크만 변경하면 되기 때문에 관리 용이성 증가함

<br>

## 3. Flink Standalone 멀티 노드 설정

### 3-1. node1 (JobManager)의 flink-conf.yaml 수정

```shell
> cd /opt/flink-2.2.0/conf
> vi config.yaml

# JobManager RPC 주소
jobmanager.rpc.address: 172.18.0.11 # JobManager 노드 IP
jobmanager.bind-host: 0.0.0.0

# TaskManager 바인딩
taskmanager.bind-host: 0.0.0.0

# Web UI 외부 접근 허용
rest.address: 0.0.0.0

# 병렬도 / 슬롯 설정
taskmanager.numberOfTaskSlots: 2
parallelism.default: 2
```

<br>

### 3-2. node2 / node3 (TaskManager)의 flink-conf.yaml 수정

```shell
> cd /opt/flink-2.2.0/conf
> vi config.yaml

# 아래 정보로 수정
jobmanager.rpc.address: 172.18.0.11
jobmanager.bind-host: 0.0.0.0
taskmanager.bind-host: 0.0.0.0
rest.address: 0.0.0.0
```

<br>

## 4. Flink 클러스터 실행

### 4-1. node1에서 JobManager 시작

```shell
> cd /opt/flink-2.2.0/bin
> ./start-cluster.sh
```

<br>

### 4-2. node2 / node3에서 TaskManager 시작

```shell
> cd /opt/flink-2.2.0/bin
> ./taskmanager.sh start
```

<br>

### 4-3. 프로세스 확인

```shell
# node1
> jps
6881 StandaloneSessionClusterEntrypoint
7514 Jps
7436 TaskManagerRunner

# node2
> jps
6369 TaskManagerRunner
6431 Jps

# node 3
> jps
6418 Jps
6358 TaskManagerRunner
```

<br>

### 4-4. Web UI 확인

- 브라우저에서 접속(http://localhost:8081)

<img width="1032" height="471" alt="스크린샷 2026-01-25 오후 3 44 35" src="https://github.com/user-attachments/assets/d5c19a8c-b78f-4ba9-94bd-43a1dc2718ae" />








