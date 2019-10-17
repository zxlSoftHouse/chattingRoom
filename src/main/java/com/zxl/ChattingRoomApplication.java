package com.zxl;

import com.zxl.common.netty.MyWebSocketChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"com.zxl.*"})
@MapperScan("com.zxl.cr.dao")
public class ChattingRoomApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChattingRoomApplication.class, args);
    }

    @Bean
    public void nettyConfig() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new MyWebSocketChannelHandler());
            System.out.print("服务开启等待客户端连接.....\n");
            Channel ch = b.bind(8888).sync().channel();
            ch.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 优雅退出程序
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
