package com.zxl.cr.vo;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableName;

/**
 * 频道实体类
 */
@TableName("t_channel")
public class CR {

    @TableField("id")
    private Long id;

    @TableField("channel_id")
    private String channelId;

    @TableField("address_ip")
    private String addressIp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getAddressIp() {
        return addressIp;
    }

    public void setAddressIp(String addressIp) {
        this.addressIp = addressIp;
    }
}
