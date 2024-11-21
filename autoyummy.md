创建mysql容器 要对root的密码进行设置 否则容器不能正常运行

```shell
docker run -e MYSQL_ROOT_PASSWORD=andrew -p 3306:3306 --name container-mysql -d mysql
```

在IDEA的service功能进入容器 是自动以容器的root用户进入的

下面是用mysql的root账号进入mysql之后一通操作

```shell
mysql -uroot -p # 输入密码 进入mysql
```

```mysql
CREATE USER 'ayuser'@'%' IDENTIFIED BY 'aypass';
CREATE DATABASE autoyummy;
GRANT ALL PRIVILEGES ON autoyummy.* TO 'ayuser'@'%';
FLUSH PRIVILEGES;
```

创建表

```mysql
USE autoyummy;

CREATE TABLE menu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    type VARCHAR(100) NOT NULL,
    restaurant_id INT NOT NULL
);

CREATE TABLE restaurant (
    restaurant_id INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_name VARCHAR(255) NOT NULL,
    pwd VARCHAR(255) NOT NULL,
    map_address VARCHAR(255) NOT NULL
);

# orders表应该还需要一列payment_id来存储付款成功之后支付系统给的id
# 目前不清楚支付系统如何 只用status记录状态
CREATE TABLE orders (
	order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    product_id INT NOT NULL,
    amount INT NOT NULL,
    table_id INT NOT NULL,
    status VARCHAR(255) NOT NULL,
    restaurant_id INT NOT NULL
);
```

查看数据库/表

```mysql
DESCRIBE menu;
```

给menu表和orders表都各插入一条记录 用于测试UserController.java



创建kafka服务：

- 编辑一个docker compose文件，存放在server目录下
- PC的cmd终端运行命令：docker-compose up -d
- 在进入kafka的容器后（可以从IDEA进入）创建主题的命令：kafka-topics.sh --create --topic <topic_name> --bootstrap-server localhost:9092 --partitions <num_partitions> --replication-factor <num_replicas>



想法

- 是否陷入在某种karma：做某工作的时候，对深层次的内容没有兴趣，且对别的工作抱有幻想，并自学别的工作需要的技能 → 换到了别的工作，又重复上述karma，弊端是始终无法专注某个工作（领域），所有知识和技能都不深入，但优点则是获得了多领域的技能，有复合背景优势
  - 从开发回到生信，是一种进步，是对两项工作进行多方面的对比后做出的决定，而不再只是跟风就选择。
  - 不过误判了生物科技的发展速度和前景，受限于基础研究，发展停滞；且未了解投资界对高通量测序行业的投资状况，大概在18~19已经停滞，而互联网是22年才真正停滞。所以总体上，互联网和生物科技都是处于平稳期，在行业中短期前景上没有显著差异。所以只是在这一方面对生物科技有错误的乐观判断，但其他方面的判断肯定都是正确的（复合背景优势、加班情况等），且结论不变，就国内及新加坡而言，生信岗更适合我



备用代码

```
@Scheduled(fixedDelay = 15000)
    public void assignOrdersToRobots() {
        // Fetch orders that are ready for delivery but not yet assigned to a robot
        // 查出status为cooked且robot_id为空的order
        List<Order> unassignedOrders = yummyMapper.getUnassignedOrders();

        List<Order> batch = new ArrayList<>();
        for (Order order : unassignedOrders) {
            batch.add(order);
            if (batch.size() == 4) {
                assignBatchToRobot(batch);
                batch.clear();
            }
        }

        // Assign remaining orders if any (less than 4)
        //妙啊这个函数 如果前面的batch长度超过4的话 就batch.clear()了就不会运行下面的东西
        //如果batch长度不到4的话 不会batch.clear() 所以会运行下面这个把不到4的dish都打包送出去
        if (!batch.isEmpty()) {
            assignBatchToRobot(batch);
        }
    }

    private void assignBatchToRobot(List<Order> batch) {
        // Find an available robot
        String availableRobotId = yummyMapper.getAvailableRobot();
        if (availableRobotId != null) {
            // Assign orders to the robot
            for (Order order : batch) {
                order.setRobotId(availableRobotId);
                yummyMapper.updateOrder(order);
            }
        }
    }
    
//api:给robot的 用来给robot监听有没有分配给自己的菜 感觉是要websocket的 robot端要给一个robot_id过来

    //robot在接到被分配的order上面的菜后 会修改order的状态和robot自身的状态
    //可以suppose机器人可以自动通过CV等手段检测是不是菜已经给全了
    //robot送完菜之后 改robot的状态
```

```
在将做好的菜分配给机器人这件事情上 一个机器人最多装5道菜 希望实现每10秒钟分配一次 在这10秒间隔内 不论产生多少道菜 即使是只有1道 也会被分配送餐 因为要保证出餐的速度 不会一直等到有5道菜做好才送餐 那么 有两种解决方案：1. 每10秒钟轮询一次数据库特定表 查出做好的菜进行分配 2. 一旦该表中一道菜显示做好 就从数据库发送消息到后端给一个kafka队列 这个队列会每10秒钟分配一次菜给机器人

在您描述的场景中，两种解决方案都有其优势和局限性。以下是对两种方案的分析和建议：

方案1：定时轮询数据库
优势:

实现相对简单，无需引入额外的消息中间件。
直接由后端控制轮询频率和任务分配逻辑。
局限性:

定时轮询可能不够实时，如果在两次轮询之间有菜品完成，可能会有轻微的延迟。
高频率轮询可能会对数据库造成不必要的压力，尤其是当订单量不大时。
方案2：使用Kafka队列
优势:

Kafka提供了高吞吐量的消息处理能力，可以实时响应数据库中的变更。
通过Kafka的发布/订阅模型，可以实现更灵活的消息消费策略，例如每10秒处理一次消息。
可以更好地扩展系统，适应高并发情况。
局限性:

需要引入和维护Kafka集群，增加了系统复杂性。
需要开发额外的逻辑来处理Kafka消息队列的分配。
综合建议
考虑到您的需求是每10秒分配一次菜品，即使只有一道菜也要及时分配，以下是一个改进的解决方案：

使用数据库触发器和消息队列：

在数据库中设置触发器，当菜品状态更新为“完成”时，触发器自动将菜品信息写入Kafka队列。
这种方式可以确保任何状态变更都能实时地反映到消息队列中。
定时任务处理Kafka队列：

在后端设置定时任务（例如使用Quartz Scheduler或Spring的@Scheduled），每10秒检查一次Kafka队列。
定时任务从Kafka队列中读取消息，并根据当前累积的菜品数量分配给机器人。
优化数据库查询：

确保数据库查询是优化的，避免全表扫描，使用索引加快查询速度。
考虑并发和事务：

确保在处理菜品分配时考虑到并发问题，避免多个任务同时分配同一道菜。
使用数据库事务保证数据的一致性。
异常处理：

设计异常处理逻辑，如果菜品分配失败，需要有重试机制或将菜品重新放回待分配队列。
监控和日志：

对系统的关键部分进行监控和记录日志，以便跟踪问题和优化性能。
测试：

在部署前进行充分的测试，包括压力测试和异常测试，确保系统在高负载下仍能稳定运行。
通过结合数据库触发器、消息队列和定时任务，您可以创建一个既实时又高效的菜品分配系统。这种方法既可以减少数据库的压力，又能确保菜品分配的及时性和准确性。
```

````
事件驱动架构是一种设计模式，它允许服务或组件在事件发生时响应。对于您的点餐和送餐系统，以下是实现事件驱动的具体解决方案：

### 1. 定义事件
首先，定义系统中的事件类型，例如：
- `OrderCompleted`：表示订单完成的事件。

### 2. 事件生产者
在您的系统中，事件生产者通常是订单管理系统或厨房显示系统，它们在订单状态更新时触发事件。

#### MySQL触发器示例
```sql
DELIMITER $$

CREATE TRIGGER after_order_completed
AFTER UPDATE ON orders
FOR EACH ROW
BEGIN
  IF OLD.status <> 'Completed' AND NEW.status = 'Completed' THEN
    -- 这里调用一个存储过程或者直接执行系统命令
    CALL send_order_completed_event(NEW.order_id);
  END IF;
END$$

DELIMITER ;
```
这个触发器会在订单状态更新为“Completed”时触发。

### 3. 事件消费者
事件消费者是负责处理事件的组件，例如分配订单给机器人的逻辑。

### 4. 消息队列或事件总线
使用消息队列（如RabbitMQ、Kafka）或事件总线来传递事件。

#### 集成消息队列
- 在触发器的存储过程中，调用消息队列的API，将事件信息发送到队列。

#### 示例伪代码
```python
def send_order_completed_event(order_id):
    # 连接到消息队列
    producer = KafkaProducer(bootstrap_servers='localhost:9092')
    # 构造事件消息
    event_message = {
        'event_type': 'OrderCompleted',
        'order_id': order_id,
        'timestamp': current_timestamp()
    }
    # 发送事件到队列
    producer.send('order-events', value=json.dumps(event_message).encode('utf-8'))
    producer.flush()
```

### 5. 事件处理服务
创建一个服务来订阅消息队列中的事件，并作出响应。

#### 示例伪代码
```python
def process_order_completed_event(event):
    order_id = event['order_id']
    # 查询数据库获取订单详情
    order_details = query_order_details(order_id)
    # 分配给空闲的机器人
    allocate_order_to_robot(order_details)

def query_order_details(order_id):
    # 数据库查询逻辑
    pass

def allocate_order_to_robot(order_details):
    # 机器人分配逻辑
    pass

# 连接到Kafka并订阅事件
consumer = KafkaConsumer('order-events',
                          bootstrap_servers='localhost:9092',
                          value_deserializer=lambda m: json.loads(m.decode('utf-8')))
for message in consumer:
    event = message.value
    if event['event_type'] == 'OrderCompleted':
        process_order_completed_event(event)
```

### 6. 确保可靠性
- 使用消息确认机制确保消息被正确处理。
- 实现重试逻辑和死信队列处理机制。

### 7. 安全性和权限
- 确保事件传输过程中的数据安全。
- 控制对事件队列的访问权限。

### 8. 监控和日志
- 对事件处理流程进行监控和记录日志，以便问题排查和性能优化。

通过上述步骤，您可以构建一个事件驱动的系统，该系统能够实时响应订单完成事件，并及时分配给送餐机器人，从而提高整体的效率和响应速度。

````

