package nus.autoyummy.entity;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class OrderAllocationEvent extends ApplicationEvent {

    @Getter
    private final int robotId;
    private final DoneOrder doneOrder;

    public OrderAllocationEvent(Object source, int robotId, DoneOrder doneOrder) {
        super(source);
        this.robotId = robotId;
        this.doneOrder = doneOrder;
    }

    public DoneOrder getOrder() {
        return doneOrder;
    }

}
