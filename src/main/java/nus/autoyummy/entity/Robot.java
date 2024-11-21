package nus.autoyummy.entity;

import lombok.Data;

@Data
public class Robot {
    private int robotId;
    private int restaurantId;
    private boolean map;//有没有该餐厅的座位地图
}
