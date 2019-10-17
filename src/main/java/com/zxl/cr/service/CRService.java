package com.zxl.cr.service;

import com.baomidou.mybatisplus.service.IService;
import com.zxl.cr.vo.CR;

public interface CRService extends IService<CR> {

    void insertChannel(CR cr);
}
