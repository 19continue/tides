import { defineOverridesPreferences } from '@vben/preferences';

const CHAOSHENG_LOGO = '/logo.png?v=chaosheng';

/**
 * @description 项目配置文件
 * 只需要覆盖项目中的一部分配置，不需要的配置不用覆盖，会自动使用默认配置
 * !!! 更改配置后请清空缓存，否则可能不生效
 */
export const overridesPreferences = defineOverridesPreferences({
  // overrides
  app: {
    name: import.meta.env.VITE_APP_TITLE,
    defaultAvatar: CHAOSHENG_LOGO,
    enableCheckUpdates: false,
    layout: "header-sidebar-nav",
    authPageLayout: "panel-center",
    //动态标题
    dynamicTitle: true
  },
  shortcutKeys: {
    enable: false
  },
  copyright: {
    companyName: "潮声",
    companySiteLink: "",
    date: "2025",
    enable: true
  },
  theme: {
    mode: "light",
    //顶部不使用暗黑模式
    semiDarkHeader: false,
    //主题色
    builtinType: "default",
    //偏好色：使用 Vben 内置默认色
    colorPrimary: "hsl(212 100% 45%)",
    //圆度
    radius: "0.5"
  },
  logo: {
    fit: "contain",
    source: CHAOSHENG_LOGO
  },
  transition: {
    //滑动动画
    name: "fade-up"
  },
  widget: {
    languageToggle: false,
    notification: false
  }
});
