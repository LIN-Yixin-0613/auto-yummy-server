package nus.autoyummy.controller;

import nus.autoyummy.entity.*;
import nus.autoyummy.kafka.KafkaProducerService;
import nus.autoyummy.mapper.YummyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    private YummyMapper yummyMapper;

    @Autowired
    private ApplicationEventPublisher publisher;

    //这里的api 是定义并运行函数的写法 有函数名
    //实际上上面已经命名了路径 这里后面接一个代码块就可以了 不需要再以函数的形式去定义
    //像JS的express框架 就不存在还需要再定义函数
    //根本原因还是因为java强制性面向对象 这个对象里面不能存在“代码块” 而必须是函数的形式
    //然后需要一些标记 来声明这不是普通的函数
    @GetMapping("/dishes")
    public List<Dish> getDishes(@RequestParam("restaurantId") int restaurantId) {

        //没有使用try-catch 如果中间任意一句出错了 就会中断 并且自动交给框架去向前端抛出一个异常
        //前端启动的时候就会初始化显示菜单和历史订单的空白列表了 所以失败的话前端就是维持空白呗 不会怎么样
        //try-catch的主要作用其实是针对性地抛出错误信息 如果针对某些步骤try-catch 可以根据错误信息知道是这个步骤出错了
        return yummyMapper.getDishes(restaurantId);
    }

    //前端那里还要有一个地方是接受websocket的orderList或者order来更新的
    @GetMapping("/orderList")
    // 前端必然是要收集到customerId才会访问后端这个接口
    // 如果在前端 用户不给customerid 就调用不了这个接口 前端展示订单的位置就会空
    public List<Order> getOrders(@RequestParam("restaurantId") int restaurantId,
                                 @RequestParam("customerId") String customerId) {
        return yummyMapper.getOrders(restaurantId, customerId);
    }

    // 如果是先付后吃 要付了才能调用这个接口 那传过来的order的状态就全都是paid
    // 如果先吃后付 就是点击“下单”就调用这个api 此时每个order的状态都是unpaid
    // 显然 无论是哪种模式 调用这个接口并写入数据库成功后 要发布事件让厨房知道
    @PostMapping("/order")
    //前端通过post请求传过来一个列表
    //前端在实现选择菜之前就要校验前端的状态里面有customer_id 如果没有的话就要提示登录
    //所以没有登录的话 前端连选择菜都实现不了 当然也就根本调用不了这个api
    //下面通过@RequestBody List<Order> orderList读取这个列表
    public void order(@RequestBody List<Order> orderList) {
//注意 前端传过来的json形式的列表里面的元素 是没有orderId这个特征的
        //但是没有关系 java这里会自动赋值为0 不会出错
        //然后到后面写进数据库的时候 这个为0的orderId不会被读取 而是会自增
        yummyMapper.order(orderList);
        //发布事件 令厨房做菜
        System.out.println(orderList);
        publisher.publishEvent(new OrderEvent(this, orderList));
        //如果这个函数运行成功的话 前端会知道成功了 这时候前端再调用一次上面的/orderList接口 就可以更新前端的菜列表
    }

    // 只用在先吃后付模式里面 因为先付后吃的话 用上面order接口就够了 传过来的时候就全都是已支付的
    @PatchMapping("/orderPaid")
    //前端需要将orderList放在请求体里面
    public void orderPaid(@RequestBody List<Order> orderList) {
        yummyMapper.orderPaid(orderList);
        // 这里将整个list里面所有order的状态都改为paid
        // 这里可以发布一个事件 令厨房知道用户已经付款了 也可以不发布 毕竟是先吃后付 不发布也不影响流程的闭环
    }

    @Autowired
    private KafkaProducerService kafkaProducerService;

    //这个是给厨房调用的接口 厨房是按照一个Order里面所有份都做完 才会点这个Order完成
    //把整个order传过来了 主要是为了下面要生成DoneOrder对象
    @PatchMapping("/orderDone")
    public void orderDone(@RequestBody Order order) {
        // 将一个order的状态改为done
        yummyMapper.orderDone(order.getOrderId());
        System.out.println(order);
        // 发布事件 websocket监听 改成done了 websocket给customer发消息
        // customer就会知道这道菜已经做好了
        // 根据下面的代码可以知道 用户会收到的一个order的最终状态就是done 然后安排送餐到收到餐 是没有信息的
        // 理论上done了之后就一定会安排送餐 所以也没有必要再有其他状态
        publisher.publishEvent(new OrderDoneEvent(this, order));

        // 然后将done的order 根据数量 生成DoneOrder 先推入某个kafka队列中
        for (int i = 0; i < order.getAmount(); i++) {
            DoneOrder doneOrder = new DoneOrder(order.getOrderId(), order.getTableId(), order.getRestaurantId());
            // 注意这里必须是每个餐厅都有一个.order.done队列
            kafkaProducerService.sendToKafka("order_done", doneOrder);
        }
    }

    // 机器人初次连接到系统 或者执行完一个送餐任务 机器人空闲了 于是将机器人信息发送到这个Kafka队列
    // 所以机器人就不在mysql数据库里了 就只是在kafka队列里面
    // 至于robot是如何收到要送的doneorder的信息的 就是上面那个接口 .order.done队列收到消息后
    // 由kafka consumer去进行分配 分配最终形式自然又是发布一个事件 让websocket监听到 再传给robot
    @PostMapping("/robot")
    public void getRobot(@RequestBody Robot robot) {
        // 这里本来会设定判断机器是否是含有该餐厅的地图 如果没有就要传给他 但是传图片比较复杂 先取消了
        // 这里设定的是机器人开机的时候输入属于哪家餐厅 所以robot对象可以读取到restaurant id
        kafkaProducerService.sendToKafka("robot_available", robot);
    }

    // restaurant登录验证 假设restaurant表有restaurant_id
    // 后端好像要给前端一个token什么的
    // 登录成功之后厨房端要设立一个websocket终端
    @PostMapping("/restaurant_login")
    public boolean restaurantLogin(@RequestBody Restaurant restaurant) {
        String pwd = yummyMapper.restaurantLogin(restaurant.getRestaurantId());
        //这里要给一个token给前端
        return pwd.equals(restaurant.getPwd());
    }
}