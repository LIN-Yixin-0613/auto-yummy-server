package nus.autoyummy;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@EnableWebSocketMessageBroker
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 连接到websocket服务时的路由地址
        // websocket服务没有独立的端口 springboot使用8080端口
        // 这样设置以后 前端连接到websocket服务就要使用8080/ws这样的地址
        //设置了.withSockJS();之后postman无法连接 registry.addEndpoint("/ws")的话postman可以连接
        //加上.setAllowedOrigins("*")之后test.html和postman都可以连接
        registry.addEndpoint("/ws")
                // 这里是设置允许跨域请求的前端地址 比如一个前端运行在localhost:8000就要替换成这个
                // localhost:63342这个是test.html在浏览器打开之后的地址 因为这个应用没有前端 为了方便测试

                // 如果用户通过浏览器访问http://example.com:8000这个地址上的前端页面，
                // 并且这个页面尝试建立到ws://server:8080的WebSocket连接，
                // 那么在WebSocket握手请求中，浏览器会设置Origin头部为http://example.com:8000。
                // 这个头部告诉服务器，WebSocket连接请求是来自http://example.com:8000这个源的。
                .setAllowedOrigins("http://localhost:63342")
                .withSockJS();
    }
}

//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        // 简单消息转发的后端地址前缀 也就是客户端发客户端
//        // 如果前端想要使用简单消息转发的功能 设置的目的地要有/user作为前缀
//        // 例如 想要发送消息给另一个叫aaa的用户 就要设置目的地为/user/aaa
//        // 很显然 前端如果想接收消息 还要设置一下自己作为接收方的地址 也就是订阅地址 如果自己是bbb 设置/bbb即可
//        registry.enableSimpleBroker("/user");
//
//        // 这里是设置纯由服务器发消息给客户端 客户端订阅地址的前缀
//        registry.setApplicationDestinationPrefixes("/app");
//    }