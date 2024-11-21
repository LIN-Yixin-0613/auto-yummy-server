package nus.autoyummy.kafka;

import nus.autoyummy.entity.DoneOrder;
import nus.autoyummy.entity.OrderAllocationEvent;
import nus.autoyummy.entity.Robot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class KafkaConsumerService {

    @Autowired
    private ApplicationEventPublisher publisher;

    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<DoneOrder>> doneOrders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Robot>> availableRobots = new ConcurrentHashMap<>();

    @KafkaListener(topics = "order_done", groupId = "order-group")
    public void listenOrderDone(DoneOrder doneOrder) {
//        System.out.println("new done order");
        System.out.println(doneOrder);
        //监听到有新的doneOrder进入队列了 先把doneOrder加进去doneOrders里面的餐厅对应的ConcurrentLinkedQueue
        //此时应该要查看"#{restaurantId}.robot.available"里面是否有robot
        //如果有的话 取出最早进入"#{restaurantId}.robot.available"队列的那一个robot
        //通过websocket将这个doneOrder的信息发给这个robot的终端 也就实现了分配了
        //分配的规则是一个DoneOrder分配给一个Robot
        //分配后这个DoneOrder和这个Robot就不会再存在于各自的队列中了
        //因为websocket控制器不在这里 所以应该是要发布事件 然后websocket控制器监听后再处理
        //如果"#{restaurantId}.robot.available"里面没有robot 那这个DoneOrder就在队列里面等待
        int restaurantId = doneOrder.getRestaurantId();

        doneOrders.computeIfAbsent(restaurantId, k -> new ConcurrentLinkedQueue<>()).offer(doneOrder);
        System.out.println(doneOrders);

        ConcurrentLinkedQueue<Robot> robots = availableRobots.get(restaurantId);
        System.out.println(availableRobots);

        if (robots != null && !robots.isEmpty()) {
            Robot robot = robots.poll(); // 取出最早进入队列的机器人

            //这里要用doneOrder和robot发布事件
            //已经简化成一个robot运送一个菜了
            publisher.publishEvent(new OrderAllocationEvent(this, robot.getRobotId(), doneOrder));

            //下面这两句是正确的吗？orders是指向哈希表里面的那个列表吗？是可以修改成功的吗？
            ConcurrentLinkedQueue<DoneOrder> q = doneOrders.get(restaurantId);
            q.remove(doneOrder);
        }
    }

    @KafkaListener(topics = "robot_available", groupId = "robot-group")
    public void listenRobotAvailable(Robot robot) {
        System.out.println(robot);
        //监听到该队列进来一个新的Robot 如果进来之前队列是空的 则有可能此时另一个队列有DoneOrder在等待
        //所以要判断一下另一个队列到底有没有DoneOrder在等待 有的话就进行分配 没有的话就让这个Robot等待
        int restaurantId = robot.getRestaurantId(); // 从topic中提取restaurant_id

        // 将Robot添加到对应的队列中
        availableRobots.computeIfAbsent(restaurantId, k -> new ConcurrentLinkedQueue<>()).offer(robot);
        System.out.println(availableRobots);

        // 检查是否有等待分配的DoneOrder，如果有，则进行分配
        ConcurrentLinkedQueue<DoneOrder> orders = doneOrders.get(restaurantId);
        System.out.println(orders);

        if (orders != null && !orders.isEmpty()) {
            DoneOrder doneOrder = orders.poll(); // 取出最早的DoneOrder

            //发布事件进行分配
            System.out.println("new robot");
            publisher.publishEvent(new OrderAllocationEvent(this, robot.getRobotId(), doneOrder));

            //下面这两句是正确的吗？orders是指向哈希表里面的那个列表吗？是可以修改成功的吗？
            ConcurrentLinkedQueue<Robot> robots = availableRobots.get(restaurantId);
            System.out.println(robots);
            robots.remove(robot);
            System.out.println(robots);
        }
    }
}