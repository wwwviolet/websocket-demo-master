package com.websocket.websocketdemo.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.websocket.websocketdemo.interceptor.UserInterceptor;
import com.websocket.websocketdemo.pojo.Message;
import com.websocket.websocketdemo.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
@ServerEndpoint(value = "/chat",configurator = GetHttpSessionConfigurator.class)
@Component//让springboot管理
//每一个客户端对应一个ChatEndPoint对象与之相对应
public class ChatEndPoint {

    //用线程安全的map来保存当前用户,对当前所有的客户端对象对应的ChatEndPoint进行管理
    private static Map<String,ChatEndPoint> onLineUsers = new ConcurrentHashMap<>();
    //声明一个session对象，通过该对象可以发送消息给指定用户，不能设置为静态，每个ChatEndPoint有一个session才能区分.(websocket的session)
    private Session session;
    //保存当前登录浏览器的用户,之前在HttpSession对象中存储了用户名
    private HttpSession httpSession;



    //建立连接时发送系统广播
    //连接建立时被调用
    @OnOpen
    public void onOpen(Session session, EndpointConfig config){
        //将局部的session对象赋值给成员session
        this.session = session;
        //获取HttpSession对象
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        //赋值给成员变量httpSession
        this.httpSession = httpSession;
        //从httpSession对象中获取用户名,在登录时进行存放
        String username = (String) httpSession.getAttribute("user");
        log.info("上线用户名称："+username);//在spring控制台中显示
        //加入到存放用户对象的map中
        onLineUsers.put(username,this);
        //将当前在线用户的用户名推送给所有的客户端
        //1.获取消息,转换为json格式
        String message = MessageUtils.getMessage(true,null,getNames());
        //2.调用方法进行系统消息的推送
        broadcastAllUsers(message);
    }

    //获取当前登录的所有用户
    private Set<String> getNames(){
        return onLineUsers.keySet();
    }


    //发送系统消息
    private void broadcastAllUsers(String message){
        try{
            //广播:通过服务器对所有客户端进行消息的转发,客户端接收后会调用onMessage
            Set<String> names = onLineUsers.keySet();
            for(String name : names){
                ChatEndPoint chatEndPoint = onLineUsers.get(name);
                chatEndPoint.session.getBasicRemote().sendText(message);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //用户之间的信息发送
    //接收到客户端发送的的数据时会自动被调用
    @OnMessage
    public void onMessage(String message,Session session){//message会接收ws传递过来的json格式转换的字符串
        try{
            ObjectMapper mapper = new ObjectMapper();
            //将message转换成Message对象
            Message mess = mapper.readValue(message,Message.class);
            //可以通过两个对象之间的关系单独保存一份聊天记录
            //获取接收数据的用户
            String toName = mess.getToName();
            //获取数据
            String data = mess.getMessage();
            //获取当前的客户端用户名
            String username = (String) httpSession.getAttribute("user");
            log.info(username + "向"+toName+"发送的消息：" + data);
            //将数据转换成json格式传递给接收数据的用户
            String resultMessage = MessageUtils.getMessage(false,username,data);
            //发送数据
            if(StringUtils.hasLength(toName)) {
                //从map里获取指定对象进行发送
                //在另外的客户端ws对象就会接收到该消息,不为系统消息
                onLineUsers.get(toName).session.getBasicRemote().sendText(resultMessage);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    //连接关闭时被调用
    //用户断开连接的断后操作
    @OnClose
    public void onClose(Session session){
        String username = (String) httpSession.getAttribute("user");
        log.info("离线用户："+ username);
        //从onLineUsers中删除指定用户
        if (username != null){
            onLineUsers.remove(username);
            UserInterceptor.onLineUsers.remove(username);
        }
        httpSession.removeAttribute("user");
        //获取推送的消息
        String message = MessageUtils.getMessage(true,null,getNames());
        //推送给所有客户端本客户端下线
        broadcastAllUsers(message);
    }
}
