package org.example;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*") // 允许跨域
public class LetterController {

    // 1. 模拟数据库：存储学生与家长联动数据
    private static final List<Map<String, Object>> STUDENTS = new ArrayList<>();
    // 2. 模拟数据库：存储信件模板数据
    private static final Map<String, Object> TEMPLATE = new HashMap<>();

    static {
        // 初始化信件通用模板
        TEMPLATE.put("bgStyle", "bg-slate-800");
        TEMPLATE.put("title", "2026学年寒暑假安全告知书及成绩单");
        TEMPLATE.put("content", "亲爱的家长：寒暑假将至，请共同做好学生人身安全、防诈骗教育...");
        TEMPLATE.put("logo", "校徽/院徽展示区");
        TEMPLATE.put("links", List.of(Map.of("title", "学校官网", "url", "https://www.university.edu.cn")));

        // 初始化 mock 学生及家长基础数据
        STUDENTS.add(createStudent("202601", "张三", "计算机学院", "软件一班", "110101200501011234", "张大三(父亲)", "13800138001"));
        STUDENTS.add(createStudent("202602", "李四", "计算机学院", "软件一班", "110101200502025678", "李小四(母亲)", "13900139002"));
        STUDENTS.add(createStudent("202603", "王五", "商学院", "金融一班", "110101200503039012", "", "")); // 缺失家长，测试异常情况
    }

    private static Map<String, Object> createStudent(String id, String name, String college, String clazz, String idCard, String parent, String phone) {
        Map<String, Object> s = new HashMap<>();
        s.put("id", id);
        s.put("name", name);
        s.put("college", college);
        s.put("clazz", clazz);
        s.put("idCard", idCard);
        s.put("parentName", parent.isEmpty() ? "未填写" : parent.split("\\(")[0]);
        s.put("relation", parent.isEmpty() ? "未填写" : parent.substring(parent.indexOf("(")+1, parent.indexOf(")")));
        s.put("phone", phone.isEmpty() ? "暂无" : phone);
        s.put("score", "高等数学: 95, Java程序设计: 88, 计算机网络: 91");
        s.put("awards", "未输入");
        s.put("status", "未发送"); // 未发送, 已发送
        s.put("isRead", "未读");    // 未读, 已读
        s.put("feedback", "无反馈");
        return s;
    }

    // ================== 【PC端接口区域】 ==================

    // 获取当前所有学生及家长列表（支持辅导员变更、添加、导出）
    @GetMapping("/students")
    public List<Map<String, Object>> getStudents() {
        return STUDENTS;
    }

    // 辅导员单独变更或补录家长联系方式
    @PostMapping("/student/update-parent")
    public Map<String, Object> updateParent(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        for (Map<String, Object> s : STUDENTS) {
            if (s.get("id").equals(id)) {
                s.put("parentName", params.get("parentName"));
                s.put("relation", params.get("relation"));
                s.put("phone", params.get("phone"));
                s.put("awards", params.get("awards")); // 辅导员手动输入获奖/班干部情况
                break;
            }
        }
        return Map.of("code", 200, "msg", "家长及获奖扩展数据补录成功！");
    }

    // 获取并个性化信件富文本模板
    @GetMapping("/template")
    public Map<String, Object> getTemplate() {
        return TEMPLATE;
    }

    // 学校/学院用户变更背景色样式及外部扩展链接
    @PostMapping("/template/update")
    public Map<String, Object> updateTemplate(@RequestBody Map<String, Object> params) {
        TEMPLATE.putAll(params);
        return Map.of("code", 200, "msg", "信件样式与公共扩展链接共享发布成功！");
    }

    // 批量发送通知短信（核心接口：内置降级读取逻辑与对接模拟）
    @PostMapping("/sms/send-batch")
    public Map<String, Object> sendSmsBatch(@RequestBody List<String> studentIds) {
        int successCount = 0;
        int failCount = 0;
        String timeLog = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        for (String id : studentIds) {
            for (Map<String, Object> s : STUDENTS) {
                if (s.get("id").equals(id)) {
                    String phone = (String) s.get("phone");
                    if ("暂无".equals(phone) || phone.isEmpty()) {
                        failCount++;
                        s.put("status", "发送失败(原因: 父母及关联亲人联系方式均为空)");
                    } else {
                        successCount++;
                        s.put("status", "已成功发送");
                        s.put("sendTime", timeLog);
                    }
                    break;
                }
            }
        }
        return Map.of("code", 200, "msg", String.format("短信平台推送完毕。成功: %d 条, 失败: %d 条。", successCount, failCount));
    }

    // 数据结果多级下钻统计（一级：学院；二级：辅导员；三级：已读未读率）
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalCount = STUDENTS.size();
        long sentCount = STUDENTS.stream().filter(s -> "已成功发送".equals(s.get("status"))).count();
        long readCount = STUDENTS.stream().filter(s -> "已读".equals(s.get("isRead"))).count();

        stats.put("total", totalCount);
        stats.put("sent", sentCount);
        stats.put("read", readCount);
        stats.put("completionRate", totalCount == 0 ? "0%" : (sentCount * 100 / totalCount) + "%");
        stats.put("readRate", totalCount == 0 ? "0%" : (readCount * 100 / totalCount) + "%");
        return stats;
    }

    // ================== 【H5 移动端家长入口区域】 ==================

    // 家长通过身份证与密码登录并回显已读状态
    @PostMapping("/parent/login")
    public Map<String, Object> parentLogin(@RequestBody Map<String, String> loginForm) {
        String idCard = loginForm.get("idCard");
        String password = loginForm.get("password");

        for (Map<String, Object> s : STUDENTS) {
            String targetIdCard = (String) s.get("idCard");
            String defaultPwd = targetIdCard.substring(targetIdCard.length() - 6);
            
            if (targetIdCard.equals(idCard) && defaultPwd.equals(password)) {
                s.put("isRead", "已读"); // 触发家长已读状态变更为“已读”
                Map<String, Object> res = new HashMap<>();
                res.put("code", 200);
                res.put("studentInfo", s);
                res.put("letterTemplate", TEMPLATE);
                return res;
            }
        }
        return Map.of("code", 400, "msg", "身份证号或初始密码错误，请联系辅导员重置密码。");
    }

    // 家长编辑文字和图片反馈提交
    @PostMapping("/parent/feedback")
    public Map<String, Object> submitFeedback(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        String content = params.get("content");
        for (Map<String, Object> s : STUDENTS) {
            if (s.get("id").equals(id)) {
                s.put("feedback", content);
                break;
            }
        }
        return Map.of("code", 200, "msg", "反馈内容已安全送达管理后台。");
    }
}
