package nus.autoyummy.entity;

import org.springframework.context.ApplicationEvent;

public class OrderDoneEvent extends ApplicationEvent {

    private final int restaurantId;
    private final String customerId;
    private final Order order;

    public OrderDoneEvent(Object source, Order order) {
        super(source);
        this.restaurantId = order.getRestaurantId();
        this.customerId = order.getCustomerId();
        this.order = order;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Order getOrder() {
        return order;
    }
}
