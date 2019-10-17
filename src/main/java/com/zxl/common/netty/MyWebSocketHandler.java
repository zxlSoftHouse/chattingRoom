package com.zxl.common.netty;

import com.alibaba.fastjson.JSONObject;
import com.zxl.cr.service.CRService;
import com.zxl.cr.vo.CR;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 接收/处理/响应客户端websocket请求的核心业务处理类
 */
@Component
public class MyWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    Map<String, Channel> channelMap = new HashMap<String, Channel>();

    @Autowired
    private CRService crService;

    private static MyWebSocketHandler  myWebSocketHandler ;

    private WebSocketServerHandshaker handshaker;

    private static final String WEB_SOCKET_URL = "ws://192.168.20.98:8888/websocket";

    //通过@PostConstruct实现初始化bean之前进行的操作
    @PostConstruct
    public void init() {
        myWebSocketHandler = this;
        myWebSocketHandler.crService = this.crService;
    }

    /**
     * 客户端与服务端创建连接的时候调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.add(ctx.channel());
        channelMap.put(ctx.channel().id().toString(), ctx.channel());
        CR cr = new CR();
        cr.setChannelId(ctx.channel().id().asShortText());
        myWebSocketHandler.crService.insertChannel(cr);
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() +  " ====>>>您的channelId: " + ctx.channel().id() +  "\n");
        NettyConfig.group.find(ctx.channel().id()).writeAndFlush(tws);
        System.out.print("客户端与服务端连接开启。。。\n");
    }

    /**
     * 客户端与服务端断开连接的时候调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.group.remove(ctx.channel());
        channelMap.remove(ctx.channel().id());
        // CR cr = new CR();
        // cr.setChannelId(ctx.channel().id().asShortText());
        // myWebSocketHandler.crService.insertChannel(cr);
        System.out.print("客户端和服务端连接关闭。。。\n");
    }

    /**
     * 服务端接受客户端发送过来的数据结束之后调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 工程出现异常的时候调用
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 服务端处理客户端websocket请求的核心方法
     * @param channelHandlerContext
     * @param msg
     * @throws Exception
     */
    @Override
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        // 处理客户端向服务端发起的http握手请求的业务
        if (msg instanceof FullHttpRequest) {
            handHttpRequest(channelHandlerContext, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) { // 处理websocket的连接业务
            handWebSocketFrame(channelHandlerContext, (WebSocketFrame)msg);
        }
    }

    /**
     * 处理客户端和服务端之前的websocket业务
     * @param ctx
     * @param frame
     */
    private void handWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否关闭websocket的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), ((CloseWebSocketFrame) frame).retain());
        }
        // 判断是否是ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 判断是否是二进制消息，如果是二进制消息，抛出异常
        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.print("目前不支持二进制消息\n");
            throw new RuntimeException("[" + this.getClass().getName() + "]不支持消息\n");
        }
        // 返回应答信息
        String request = ((TextWebSocketFrame)frame).text();
        System.out.print("服务器收到客户端的消息》》》》》》》》》》》》" + request + "\n");
        JSONObject jsonObject = JSONObject.parseObject(request);
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() + ctx.channel().id() + " ====>>> " + jsonObject.get("message") + "\n");
        // 群发，服务端向每个连接上来的客户端群发消息
        // NettyConfig.group.writeAndFlush(tws);
        // 发给某个channel
        Channel sendChannel = channelMap.get(jsonObject.get("channelId").toString());
        NettyConfig.group.find(sendChannel.id()).writeAndFlush(tws);
    }

    /**
     * 处理客户端向服务端发起的http请求业务
     */
    private void handHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.getDecoderResult().isSuccess() || !("websocket").equals(req.headers().get("Upgrade"))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null,false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    /**
     * 服务端向客户端响应消息
     * @param ctx
     * @param req
     * @param res
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        // 服务端向客户端发送数据
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
