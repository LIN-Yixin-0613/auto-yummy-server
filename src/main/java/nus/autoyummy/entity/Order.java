package nus.autoyummy.entity;

import lombok.Data;

@Data
public class Order {
    private int orderId;
    private String customerId;
    private int productId;
    private int amount;
    private int tableId;
    private String status;
    private int restaurantId;
}