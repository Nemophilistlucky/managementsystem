import {createRouter, createWebHistory} from "vue-router";
import Login from "@/views/Login.vue";
import Layout from "@/views/Layout.vue";
import UserInfo from "@/views/user/UserInfo.vue";
import UserList from "@/views/user/UserList.vue";
import RestPassword from "@/views/user/RestPassword.vue";



// 定义路由关系
const routes = [
  {path: '/login', component: Login},  // 一级路由
  {
    path: '/',
    redirect: '/login',  // 重定向
    component: Layout,
    // 子路由
    children: [
      {path: '/user/list', component: UserList},
      {path: '/user/info', component: UserInfo},
      {path: '/user/resetPassword', component: RestPassword},
    ]
  },
]

// 创建路由器
const router = createRouter({
  history: createWebHistory(), // 路由模式
  routes: routes
})

//导出暴露
export default router
