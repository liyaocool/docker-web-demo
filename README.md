# docker构建打包java项目

## 简介
本项目用于 研究和实践 docker的工作流部署发布
> [查看github源码](https://github.com/liyaocool/docker-web-demo) 

> 技术栈
- spring-web （RESTAPI 请求交互）
- redis （用于实验 多容器通信）
- maven

## 一. 本地启动(不属于本docker实验，需要自己本地有redis服务)

### 启动

```bash
java -jar xxx.jar
```

### 测试接口

1. localhost:8080
2. localhost:8080/hello 

## 二. docker部署

### 1. docker创建网络空间，以便多容器在同网络里可通信

```bash
docker network create web-demo-net
```

### 2. 获取redis镜像

```bash
docker pull redis:latest
```

- `:latest`拉取最新版镜像

### 3. redis镜像运行成容器

```bash
docker run -d -p 6379:6379 --name web-demo-redis --network web-demo-net --network-alias redis-net redis:latest
```

- `-d` 后台启动
- `-p 6379：6379` 容器的端口(:后面的6379) 映射 成 宿主机的端口(:前面的6379) 
- `--name` 给容器命名
- `--network` 指定使用的网络空间,同网络空间的容器才可通信
- `--network-alias` 指定本容器所使用网络地址的别名：`redis-net`， web项目配置文件中的redis连接host即为 `redis-net` ，（很重要！相当于分配独有有ip地址）
- `redis:latest` 指定生成容器所使用的镜像名和版本标签

### 4. docker打包web项目为镜像

#### `Dockfile`build文件编写
```Dockerfile
# 获取运行环境
FROM    openjdk:17
# 把本地文件目录 解析到 容器目录（容器若没有会自动创建文件夹）
ADD     ./target/web-demo.jar   /usr/local/workspace/web-demo/web-demo.jar
# 设置工作目录空间，方便后续操作指令
WORKDIR /usr/local/workspace/web-demo
# 执行指令
CMD     java -jar web-demo.jar
```

#### 打包命令
```bash
docker buildx b -t web-demo:v1 .
```
- `-t` 命名镜像名称， `:v1` 指定版本标签tag
- `.` 构建目录为当前目录

### 5. web-demo镜像运行成容器

```bash
docker run -d -p 8080:8080 --name web-demo --network web-demo-net web-demo:v1
```

### 6. 宿主机访问web应用测试
- localhost:8080
- localhost:8080/hello

# 其它docker命令 

> `docker --help` 可查看帮助指令 

## 查看 镜像列表

```bash
docker image ls
```

## 查看 容器列表

```bash
docker container ls
```

## 查看 日志

```bash
docker logs [容器containerId]
```

