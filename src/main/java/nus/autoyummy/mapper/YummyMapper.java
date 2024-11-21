package nus.autoyummy.mapper;

// 这里的接口是在封装SQL语句
// 感觉其实可以直接在UserController里面写
// 这样的话可能就是将功能块分开吧 UserController就是接收http请求 设置路径等 这个接口是负责封装SQL语句

import nus.autoyummy.entity.Dish;
import nus.autoyummy.entity.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface YummyMapper {

    @Select("select * from menu where restaurant_id=#{restaurantId}")
    List<Dish> getDishes(int restaurantId);

    @Select("select * from orders " +
            "where restaurant_id=#{restaurantId} and customer_id=#{customerId}")
    List<Order> getOrders(int restaurantId, String customerId);

    @Insert({
            "<script>",
            "INSERT INTO orders (customer_id, product_id, amount, table_id, status, restaurant_id) VALUES " +
            "<foreach collection='orderList' item='order' separator=','>" +
            "(#{order.customerId}, #{order.productId}, #{order.amount}, #{order.tableId}, #{order.status}, #{order.restaurantId})" +
            "</foreach>" +
            "</script>"
    })
    @Options(useGeneratedKeys = true, keyProperty = "orderId", keyColumn = "order_id")
    void order(List<Order> orderList);

    //改order表的状态
    @Update("UPDATE orders " +
            "SET status = 'done' " +
            "WHERE order_id = #{orderId};")
    void orderDone(int orderId);

    @Update("<script>" +
            "   UPDATE orders " +
            "   SET status = 'paid' " +
            "   WHERE order_id IN " +
            "   <foreach item='order' collection='orders' separator=',' open='(' close=')'>" +
            "       #{order.orderId}" +
            "   </foreach>" +
            "</script>")
    void orderPaid(@Param("orders") List<Order> orders);

    // 是需要一个单独的restaurants表的 存储密码什么的 因为厨房端是需要登录的
    @Select("SELECT pwd FROM restaurants where restaurant_id=#{restaurantId}")
    String restaurantLogin(String restaurantId);

    @Select("select * from orders where restaurant_id=#{restaurantId} and status='paid'")
    List<Order> getRestaurantOrders(String restaurantId);
}