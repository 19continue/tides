<template>
  <div class="app-container">

    <el-row class="sidebar">
      <el-col :span="24">
        <h5 class="mb-2">我的潮声</h5>
        <div class="menu_border">
          <el-menu
              :default-openeds="defaultOpened1"
              class="el-menu-vertical-demo"
              @open="handleOpen"
              @close="handleClose"

          >
            <el-sub-menu index="1" class="titleMenu">
              <template #title>
                <span>交易中心</span>
              </template>
              <el-menu-item index="5" :class="{ 'is-active': activeIndex === '5'}"><router-link to="/orderManagement/index">订单管理</router-link></el-menu-item>
            </el-sub-menu>

          </el-menu>
          <el-menu
              :default-openeds="defaultOpened2"
              class="el-menu-vertical-demo"
              @open="handleOpen"
              @close="handleClose"
          >
            <el-sub-menu index="1">
              <template #title>
                <span>账户中心</span>
              </template>
<!--              <el-menu-item index="1" :class="{ 'is-active': props.activeIndex === '1' }">收货地址</el-menu-item>-->
              <el-menu-item index="2" :class="{ 'is-active': props.activeIndex === '2' }"><router-link to="/accountSettings/index">账号设置</router-link></el-menu-item>
              <el-menu-item index="3" :class="{ 'is-active': props.activeIndex === '3' }"><router-link to="/personInfo/index">个人信息</router-link></el-menu-item>
              <el-menu-item index="4" :class="{ 'is-active': props.activeIndex === '4' }"><router-link to="/personInfo/ticketUser">常用购票人</router-link></el-menu-item>
            </el-sub-menu>

          </el-menu>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {Document, Menu as IconMenu, Location, Setting} from '@element-plus/icons-vue'
import {ref, defineProps} from 'vue'
// index: 需要打开的 sub-menu 的 index
const handleOpen = (key, keyPath) => {
  console.log(key, keyPath)
}
const handleClose = (key, keyPath) => {
}
//默认所有项都打开
const defaultOpened1 = ref(['1'])
const defaultOpened2 = ref(['1', '2', '3', '4'])
const props = defineProps({
  activeIndex: String
});


</script>

<style scoped lang="scss">
.app-container {
  .sidebar {
    width: 210px;
  }
}

.el-menu--vertical:not(.el-menu--collapse):not(.el-menu--popup-container) .el-menu-item,
.el-menu--vertical:not(.el-menu--collapse):not(.el-menu--popup-container) .el-menu-item-group__title,
.el-menu--vertical:not(.el-menu--collapse):not(.el-menu--popup-container) .el-sub-menu__title {
  padding-left: 20px;
}

:deep(.menu_border) {
  overflow: hidden;
  border: 0;
  border-radius: 10px;
  background: var(--brand-card);
  box-shadow: var(--brand-shadow-soft);
}

:deep(.el-menu) {
  border-right: none;
  background: transparent;
}

:deep(.el-sub-menu .el-menu-item) {
  height: 38px;
  line-height: 38px;
  color: var(--brand-muted);
  transition: background .16s ease, color .16s ease;
}

:deep(.el-sub-menu .el-menu-item:hover) {
  background: transparent;
  color: var(--brand-dark);
}

:deep(.el-sub-menu .el-menu-item.is-active) {
  border-left: 0;
  background: #fbf4df;
  height: 38px;
  margin-left: 0;
  position: relative;
  padding-left: 30px;
  text-decoration: none;
  color: var(--brand-dark);
}

:deep(.el-sub-menu .el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 14px;
  top: 12px;
  width: 4px;
  height: 14px;
  border-radius: 999px;
  background: var(--brand-primary-strong);
}

:deep(.el-menu .el-sub-menu .el-sub-menu__title) {
  background: transparent;
  border-bottom: 1px solid var(--brand-line);
  height: 42px;
  line-height: 42px;
  padding-left: 18px;
  color: var(--brand-dark);
  font-weight: 500;
}

/* 隐藏Element Plus侧边菜单展开/折叠箭头 */
:deep(.el-sub-menu .el-sub-menu__icon-arrow) {
  display: none;
}

:deep(.el-submenu__title .el-submenu__icon-arrow) {
  display: none;
}

.mb-2 {
  display: flex;
  align-items: center;
  width: 210px;
  height: 42px;
  padding-left: 16px;
  border-radius: 10px 10px 0 0;
  background: var(--brand-dark);
  font-size: 14px;
  color: var(--brand-primary);
  font-weight: 600;
  margin: 0;
}

:deep(.el-sub-menu .el-menu-item a){
  width: 100%;
  height: 100%;
  display: block;
}

</style>
