package com.tides.mybatisplus;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.tides.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;
/**
 * @description: mybatisPlus更新填充
 * @author: 19continue
 **/
@Slf4j
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", DateUtils::now, Date.class);
        this.strictInsertFill(metaObject, "editTime", DateUtils::now, Date.class);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "editTime", DateUtils::now, Date.class);
    }
}