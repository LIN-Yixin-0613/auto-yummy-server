package nus.autoyummy.entity;

import lombok.Data;

@Data
public class DoneOrder {

    private int orderId;
    private int tableId;
    private int restaurantId;

    public DoneOrder() {
        // 默认构造函数
    }

    public DoneOrder(int orderId, int tableId, int restaurantId) {
        this.orderId = orderId;
        this.tableId = tableId;
        this.restaurantId = restaurantId;
    }
}
