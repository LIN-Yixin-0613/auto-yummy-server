package nus.autoyummy;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan //就会扫描所有带mapper标记的
//@EnableWebSocket //不确定这个注解是否真的需要
public class AutoyummyApplication {

    public static void main(String[] args) {
        // 这里其实已经会把controller文件夹里面的class运行起来了
        // 上面为啥一定还要来个MapperScan 就感觉很多余
        SpringApplication.run(AutoyummyApplication.class, args);
    }

}