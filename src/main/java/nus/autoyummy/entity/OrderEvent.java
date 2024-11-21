package nus.autoyummy.entity;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class OrderEvent extends ApplicationEvent {

    private final int restaurantId;
    private final List<Order> orderList;

    public OrderEvent(Object source, List<Order> orderList) {
        super(source);
        this.restaurantId = orderList.get(0).getRestaurantId();
        this.orderList = orderList;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public List<Order> getOrderList() {
        return orderList;
    }
}