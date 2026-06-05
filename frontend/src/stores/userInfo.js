import {defineStore} from "pinia";
import {ref} from "vue";

export const useUserInfoStore = defineStore('userInfo',
    () => {
    // 定义变量
      const userInfo = ref({})

        // 设置值
      const setUserInfo = (newUserInfo) => {
        userInfo.value = newUserInfo
      }

      // 清理值
      const removeUserInfo = () => {
        userInfo.value = {}
      }

      return {
        userInfo, setUserInfo, removeUserInfo
      }
    },
    {
      persist: true
    });