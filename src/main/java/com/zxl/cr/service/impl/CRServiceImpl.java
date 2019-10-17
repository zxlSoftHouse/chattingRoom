package com.zxl.cr.service.impl;

import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.zxl.cr.dao.CRMapper;
import com.zxl.cr.service.CRService;
import com.zxl.cr.vo.CR;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CRServiceImpl extends ServiceImpl<CRMapper, CR> implements CRService {

    @Autowired
    private CRMapper crMapper;

    @Override
    public void insertChannel(CR cr) {
        crMapper.insert(cr);
    }
}
