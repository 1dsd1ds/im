package cn.edu.zjut.im.netty;

import io.netty.util.AttributeKey;

public class ChannelAttributes {
    public static final AttributeKey<Long> USER_ID = AttributeKey.valueOf("userId");
    public static final AttributeKey<String> USERNAME = AttributeKey.valueOf("username");
}
