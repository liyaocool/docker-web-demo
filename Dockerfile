# 获取运行环境
FROM    openjdk:17
# 把本地文件目录 解析到 容器目录（容器若没有会自动创建文件夹）
ADD     ./target/web-demo.jar   /usr/local/workspace/web-demo/web-demo.jar
# 设置工作目录空间，方便后续操作指令
WORKDIR /usr/local/workspace/web-demo
# 执行指令
CMD     java -jar web-demo.jar