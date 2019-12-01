1 搭建ssm框架
1.1配置web工程配置文件web.xml
1.1配置spring4的配置文件applicationContext.xml
1.2配置数据源dbconfig.properties和mybatis配置文件mybatis-config.xml
1.3配置springmvc配置文件springmvc.xml
1.4引入json解析
1.5引入lombok
1.6引入log4j2（配置文件log4j2.xml）

2 整合redis(单机版redis应用)
【单机版，是指应用中只有一台Redis服务器，所有的操作都是在这台Redis服务器上面进行，
此时配置和使用Redis只需要一个host+port（就是Redis服务器的host和port）】
2.1安装redis
2.1.1启动命令
D:\developer\env\Redis-x64-3.2.100>redis-server.exe redis.windows.conf
2.1.2测试是否可以使用
D:\developer\env\Redis-x64-3.2.100>redis-cli.exe
127.0.0.1:6379> set mykey tudedong
OK
127.0.0.1:6379> get mykey
"tudedong"
2.2整合redis到ssm项目中
2.2.1引入jedis依赖(使用Jedis，不使用RedisTemplate)
2.2.2写redis工具类RedisUtil，初始化redis连接池和创建redis实例
2.2.3写redis配置类RedisConfig，将redis实例放入spring容器中供使用，注意使用注解@Configuration和@Bean

3 redis应用场景---缓存，例子中用的是对频繁访问的某个对象信息进行缓存
3.1 缓存雪崩
3.2 缓存击穿
3.3 缓存穿透
测试：
1.启动redis并连接redis，查看所有的key，并清空所有key
2.访问 http://localhost:8080/getTestTable/1

4 redis应用场景---分布式锁，例子中用的是秒杀场景
4.1所谓秒杀，从业务角度看，是短时间内多个用户“争抢”资源，这里的资源在大部分秒杀场景里是商品；
将业务抽象，技术角度看，秒杀就是多个线程对资源进行操作，所以实现秒杀，就必须控制线程对资源的争抢，
既要保证高效并发，也要保证操作的正确。
4.2考虑的问题
4.2.1怎么实现加锁？
“锁”其实是一个抽象的概念，将这个抽象概念变为具体的东西，就是一个存储在redis里的key-value对，
key是于商品ID相关的字符串来唯一标识，value其实并不重要，因为只要这个唯一的key-value存在，就表示这个商品已经上锁。
4.2.2如何释放锁？
既然key-value对存在就表示上锁，那么释放锁就自然是在redis里删除key-value对。
4.2.3阻塞还是非阻塞？
采用阻塞式的实现，若线程发现已经上锁，会在特定时间内轮询锁。
4.2.4如何处理异常情况？
比如一个线程把一个商品上了锁，但是由于各种原因，没有完成操作（在上面的业务场景里就是没有将库存-1写入数据库），自然没有释放锁，
这个情况就要加入了锁超时机制，利用redis的expire命令为key设置超时时长，过了超时时间redis就会将这个key自动删除，
即强制释放锁（可以认为超时释放锁是一个异步操作，由redis完成，应用程序只需要根据系统特点设置超时时间即可）。