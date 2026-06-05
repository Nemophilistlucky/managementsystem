import axios from "axios";
import {ElMessage} from "element-plus";
import router from "@/router";
//{baseURL: '/api'}
const instance = axios.create();


// axios 请求拦截
instance.interceptors.request.use(
    config => {
      return config
    },
    error => {
      return Promise.reject(error)
    }
)

// axios 相应拦截
instance.interceptors.response.use(
    result => {
      if (result.data.code === 0) {
        return result.data
      }
      ElMessage.error(result.data.message ? result.data.message : '服务异常')
      return Promise.reject(result.data)

    },
    error => {
      if (error.response.status === 70005) {
        ElMessage.error("请先登录")
        router.push('/login')
      } else {
        ElMessage.error('服务异常')
      }
      return Promise.reject(error)
    }
)

export default instance
