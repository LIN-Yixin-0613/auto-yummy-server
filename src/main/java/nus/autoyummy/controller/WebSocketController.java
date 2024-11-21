package nus.autoyummy.controller;

import nus.autoyummy.entity.Order;
import nus.autoyummy.entity.OrderAllocationEvent;
import nus.autoyummy.entity.OrderDoneEvent;
import nus.autoyummy.entity.OrderEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.context.event.EventListener;

import java.util.List;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleOrderEvent(OrderEvent event) {

        List<Order> orders = event.getOrderList();
        System.out.println("listened");
        System.out.println(event.getRestaurantId());
        System.out.println(orders);
        messagingTemplate.convertAndSendToUser(String.valueOf(event.getRestaurantId()),"/restaurant", orders);
    }

    @EventListener
    public void handleOrderDoneEvent(OrderDoneEvent event) {
        String customerId = event.getCustomerId();
        Order order = event.getOrder();
        messagingTemplate.convertAndSendToUser(customerId,"/customer", order);
    }

    @EventListener
    public void handleOrderAllocation(OrderAllocationEvent event) {
        messagingTemplate.convertAndSendToUser(String.valueOf(event.getRobotId()), "/robot", event.getOrder());
    }
}