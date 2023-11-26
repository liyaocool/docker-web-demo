# docker构建打包java项目

## 简介
本项目用于 研究和实践 docker的工作流部署发布
> [查看github源码](https://github.com/liyaocool/docker-web-demo) 

> 技术栈
- spring-web （RESTAPI 请求交互）
- redis （用于实验 多容器通信）
- maven

## docker常用命令

> `docker --help` 可查看帮助指令

### 查看 镜像列表

```bash
docker image ls
```

### 查看 容器列表

```bash
docker container ls
```

### 查看 日志

```bash
docker logs [容器containerId]
```


## docker部署

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
docker run -d -p 6379:6379 \
--name web-demo-redis \
--network web-demo-net \
--network-alias redis-net \
-v redis-data:/data \
redis:latest
```

- `-d` 后台启动
- `-p 6379：6379` 容器的端口(:后面的6379) 映射 成 宿主机的端口(:前面的6379) 
- `--name` 给容器命名
- `--network` 指定使用的网络空间,同网络空间的容器才可通信
- `--network-alias` 指定本容器所使用网络地址的别名：`redis-net`， web项目配置文件中的redis连接host即为 `redis-net` ，（很重要！相当于分配独有有ip地址）
- `-v` 指定数据卷映射，其中`redis-data`是创建的数据卷名称，实际物理数据在宿主机，逻辑数据在容器中。其中`/data`为容器应用生成的数据路径
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

## docker备份容器数据

### 利用中转容器 目录挂载 获取 数据卷

```bash
docker run --rm \
 --volumes-from web-demo-redis \
 -v ~/backup:/backup \
  debian \
  tar cvf /backup/redis-data.tar /data
```

- `--rm` 作为临时容器启动，运行后即删除容器
- `--volumes-from` 数据卷挂载来源 `web-demo-redis`
- `-v` 数据卷映射目录，冒号：前的`~/backup`是宿主机的绝对路径，不再是上文中`redis-data`模式新建的数据卷名称，它用来真正接收压缩的备份文件。冒号:后的`/backup`是中转linux系统的目录，用来中转临时保存容器的压缩数据。
- `debian` 是指定的临时系统镜像名，可使用其他系统镜像，如`ubuntu`等。
- `tar` linux系统常用解压缩工具，这里是把 复制的数据卷目录`/data`压缩为 `/backup/redis-data.tar`

> 已经备份结束，可在宿主机的 `~/backup`目录查看到压缩的备份文件 `redis-data.tar`


## docker恢复容器数据

### 测试时，可`docker volume rm redis-data`删除数据卷，来模拟丢失数据

### 利用中转容器 目录挂载 获取 备份文件

```bash
docker run --rm \
 --volumes-from web-demo-redis \
  -v ~/backup:/backup \
  debian \
  bash -c "cd /data/ && tar xvf /backup/redis-data.tar --strip 1"
```

- `--strip 1`表示tar工具解压时去掉前面1层目录,这里去掉了tar压缩包里的/data/目录，根据压缩时确定层级路径是否需要此参数

> 备份已完成， dump.rdb 已经是旧数据文件


## docker compose 一件启动应用

> 上文手动启动redis容器，再启动web-demo容器，docker compose 可以 实现自动化一键启动所以服务容器

### 修改网络空间配置

> 上文中是手动配置web容器和redis容器的网络空间 web-demo-net, docker compose 无需配置，自动会在同个网络内，所以去掉web项目中的配置

修改 `application-prod.yml` 的 `host: redis-net` 为 `compose.yaml`里设置的服务名`redis`

```yaml
spring:
  data:
    redis:
#      host: redis-net  
      host: redis
      port: 6379
```

### 编写 compose.yml

```bash
services:

  web-demo:
    # 当前目录下的Dockerfile构建
    build: .
    ports:
      - '8080:8080'
    # 也给web项目一个数据卷
    volumes:
      - web-demo-data:/usr/local/workspace/web-demo
    environment:
      - TZ=Asia/Shanghai

  redis:
    image: 'redis:latest'
    volumes:
      - redis-data:/data
    environment:
      - TZ=Asia/Shanghai

volumes:
  web-demo-data:
  redis-data:
```

### 启动 compose

在 compose.yaml目录
```bash
docker compose up
```

