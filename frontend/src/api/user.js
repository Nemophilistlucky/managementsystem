import request from "@/utils/request";

export const userInfoService = () => request.get('/api/auth/userInfo')
