import { acceptHMRUpdate, defineStore } from 'pinia';

interface BasicUserInfo {
  [key: string]: any;
  /**
   * 头像
   */
  avatar: string;
  /**
   * 用户昵称
   */
  realName: string;
  /**
   * 用户角色
   */
  roles?: string[];
  /**
   * 用户id
   */
  userId: string;
  /**
   * 用户名
   */
  username: string;
}

interface AccessState {
  /**
   * 用户信息
   */
  userInfo: BasicUserInfo | null;
  /**
   * 用户角色
   */
  userRoles: string[];
}

const ADMIN_DISPLAY_NAME = '19continue';
const ADMIN_DESCRIPTION = '潮声管理员';
const ADMIN_AVATAR = '/logo.png?v=chaosheng';

function normalizeUserInfo(userInfo: BasicUserInfo | null) {
  if (!userInfo) {
    return userInfo;
  }
  return {
    ...userInfo,
    avatar: ADMIN_AVATAR,
    desc: ADMIN_DESCRIPTION,
    realName: ADMIN_DISPLAY_NAME,
    username: ADMIN_DISPLAY_NAME,
  };
}

/**
 * @zh_CN 用户信息相关
 */
export const useUserStore = defineStore('core-user', {
  actions: {
    setUserInfo(userInfo: BasicUserInfo | null) {
      const normalizedUserInfo = normalizeUserInfo(userInfo);
      // 设置用户信息
      this.userInfo = normalizedUserInfo;
      // 设置角色信息
      const roles = normalizedUserInfo?.roles ?? [];
      this.setUserRoles(roles);
    },
    setUserRoles(roles: string[]) {
      this.userRoles = roles;
    },
  },
  state: (): AccessState => ({
    userInfo: null,
    userRoles: [],
  }),
});

// 解决热更新问题
const hot = import.meta.hot;
if (hot) {
  hot.accept(acceptHMRUpdate(useUserStore, hot));
}
