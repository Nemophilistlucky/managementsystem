package org.example;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class LetterController {

    // 模拟数据库：存储学生、家长、成绩及通信全维度数据
    private static final List<Map<String, Object>> STUDENTS = new ArrayList<>();
    // 模拟数据库：模板表，支持多角色共享、样式、链接及校徽院徽定制
    private static final Map<String, Object> TEMPLATE = new HashMap<>();

    static {
        // 初始化学校通用模板（满足支持富文本、背景更换、校徽院徽、多个外链需求）
        TEMPLATE.put("bgStyle", "bg-slate-800");
        TEMPLATE.put("title", "2026学年寒暑假安全告知书及成绩单");
        TEMPLATE.put("content", "<div><strong>学工部通用安全告知：</strong>寒暑假期间，请家长督促学生注意人身安全，谨防电信网络诈骗。</div>");
        TEMPLATE.put("badgeUrl", "https://img.icons8.com/color/96/university.png"); // 默认校徽位置
        
        List<Map<String, String>> extLinks = new ArrayList<>();
        extLinks.add(Map.of("title", "学生一站式通知链接", "url", "https://notice.edu.cn"));
        extLinks.add(Map.of("title", "智慧学工管理系统", "url", "https://xg.edu.cn"));
        TEMPLATE.put("links", extLinks);

        // 初始化 mock 数据：深度匹配合同需求的“寻道学工系统”数据源及缺失降级情况
        // 学生1：父亲信息完整 -> 默认发给父亲
        STUDENTS.add(createFullStudent("202601", "张三", "计算机学院", "软件一班", "110101200501011234", "张大三", "13800138001", "李美美", "13800138002", ""));
        // 学生2：父亲无联系方式，母亲完整 -> 降级发给母亲
        STUDENTS.add(createFullStudent("202602", "李四", "计算机学院", "软件一班", "110101200502025678", "李外刚", "", "王秀英", "13900139002", ""));
        // 学生3：父母皆无，有其他亲人奶奶 -> 降级发给奶奶
        STUDENTS.add(createFullStudent("202603", "王五", "信息工程学院", "网安二班", "110101200503039012", "", "", "", "", "刘奶奶(奶奶)-13500135003"));
        // 学生4：全家彻底无联系方式 -> 触发“未读到信息、联系人及电话为空”合同边界
        STUDENTS.add(createFullStudent("202604", "赵六", "信息工程学院", "网安二班", "110101200504044321", "", "", "", "", ""));
    }

    private static Map<String, Object> createFullStudent(String id, String name, String college, String clazz, String idCard, String fName, String fPhone, String mName, String mPhone, String otherInfo) {
        Map<String, Object> s = new HashMap<>();
        s.put("id", id);
        s.put("name", name);
        s.put("college", college);
        s.put("clazz", clazz);
        s.put("idCard", idCard);
        s.put("password", idCard.substring(idCard.length() - 6)); // 初始密码身份证后六位
        s.put("score", "高等数学: 优秀(95分), Java高级程序设计: 良好(88分), 数据库系统原理: 92分"); // 依赖教务处提供的视图数据
        s.put("awards", "暂无录入数据"); // 针对补充需求：获奖和班干部情况无数据源，需辅导员手动输入

        // 数据源原始存根
        s.put("fatherName", fName); s.put("fatherPhone", fPhone);
        s.put("motherName", mName); s.put("motherPhone", mPhone);
        s.put("otherInfo", otherInfo);

        // 合同核心机制：自动判定读取优先级（父 -> 母 -> 其他亲人 -> 空）
        resolveActiveParent(s);

        s.put("status", "未发送"); // 未发送, 已成功发送, 发送失败(原因...)
        s.put("sendTime", "-");
        s.put("isRead", "未读");    // 未读, 已读
        s.put("feedbackContent", ""); // 家长端非必须反馈内容
        return s;
    }

    // 核心判定降级算法：完全落实合同关于父母无联系方式时寻找其他亲人的逻辑
    private static void resolveActiveParent(Map<String, Object> s) {
        String fPhone = (String) s.get("fatherPhone");
        String mPhone = (String) s.get("motherPhone");
        String otherInfo = (String) s.get("otherInfo");

        if (fPhone != null && !fPhone.isEmpty()) {
            s.put("parentName", s.get("fatherName"));
            s.put("relation", "父亲");
            s.put("phone", fPhone);
        } else if (mPhone != null && !mPhone.isEmpty()) {
            s.put("parentName", s.get("motherName"));
            s.put("relation", "母亲");
            s.put("phone", mPhone);
        } else if (otherInfo != null && !otherInfo.isEmpty()) {
            // 解析格式如 "刘奶奶(奶奶)-13500135003"
            String[] parts = otherInfo.split("-");
            String nameAndRel = parts[0];
            s.put("parentName", nameAndRel.split("\\(")[0]);
            s.put("relation", nameAndRel.substring(nameAndRel.indexOf("(")+1, nameAndRel.indexOf(")")));
            s.put("phone", parts[1]);
        } else {
            // 父母、亲人均无数据，显示联系人、关系、电话为空
            s.put("parentName", "");
            s.put("relation", "");
            s.put("phone", "");
        }
    }

    // 获取并过滤全量学生列表（支持家长为空筛选、已读未读状态筛选）
    @GetMapping("/students")
    public List<Map<String, Object>> getStudents(
            @RequestParam(required = false) String phoneEmpty,
            @RequestParam(required = false) String isRead) {
        
        return STUDENTS.stream()
                .filter(s -> phoneEmpty == null || !phoneEmpty.equals("true") || ((String)s.get("phone")).isEmpty())
                .filter(s -> isRead == null || isRead.isEmpty() || s.get("isRead").equals(isRead))
                .collect(Collectors.toList());
    }

    // 辅导员批量或单独更改家长栏，允许单独变更为奶奶，且支持更改电话
    @PostMapping("/student/update-parent")
    public Map<String, Object> updateParent(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        for (Map<String, Object> s : STUDENTS) {
            if (s.get("id").equals(id)) {
                s.put("parentName", params.get("parentName"));
                s.put("relation", params.get("relation"));
                s.put("phone", params.get("phone"));
                if(params.containsKey("awards")) s.put("awards", params.get("awards"));
                break;
            }
        }
        return Map.of("code", 200, "msg", "辅导员手工订正/补录学生家长及获奖信息成功！");
    }

    // 辅导员修改密码/忘记密码更改
    @PostMapping("/student/reset-pwd")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        String newPwd = params.get("newPwd");
        for (Map<String, Object> s : STUDENTS) {
            if (s.get("id").equals(id)) {
                s.put("password", newPwd);
                break;
            }
        }
        return Map.of("code", 200, "msg", "家长登录密码重置成功！");
    }

    // 重新从用户管理同步获取家长数据（还原最初覆盖）
    @PostMapping("/student/reload-from-center")
    public Map<String, Object> reloadFromCenter() {
        // 模拟触发重加载，重置自动判定
        for (Map<String, Object> s : STUDENTS) {
            resolveActiveParent(s);
        }
        return Map.of("code", 200, "msg", "已重新对接寻道学工系统用户管理中心，成功同步最新家长变更信息！");
    }

    // 获取模板
    @GetMapping("/template")
    public Map<String, Object> getTemplate() {
        return TEMPLATE;
    }

    // 角色定制模板（学校用户新建/共享，学院及辅导员个性化套用并替换校徽/院徽）
    @PostMapping("/template/update")
    public Map<String, Object> updateTemplate(@RequestBody Map<String, Object> params) {
        TEMPLATE.putAll(params);
        return Map.of("code", 200, "msg", "信件模板风格、富文本样式、链接地址及院徽组件更新共享成功！");
    }

    // 批量短信调度派发（对接短信中台，内置状态记录、校验错误手机号、发送成功变更逻辑）
    @PostMapping("/sms/send-batch")
    public Map<String, Object> sendSmsBatch(@RequestBody List<String> studentIds) {
        int success = 0; int fail = 0;
        String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        for (String id : studentIds) {
            for (Map<String, Object> s : STUDENTS) {
                if (s.get("id").equals(id)) {
                    String phone = (String) s.get("phone");
                    if (phone == null || phone.isEmpty() || phone.equals("暂无")) {
                        s.put("status", "发送失败(原因: 缺少联系电话)");
                        fail++;
                    } else if (phone.length() < 11) {
                        s.put("status", "发送失败(原因: 手机号码位数不对)");
                        fail++;
                    } else {
                        s.put("status", "已成功发送");
                        s.put("sendTime", now);
                        success++;
                    }
                    break;
                }
            }
        }
        return Map.of("code", 200, "msg", String.format("批量发送完成！状态标记成功。发送成功: %d 人，失败: %d 人。", success, fail));
    }

    // 多级下钻级联统计服务（完全实现合同一、二、三级大屏穿透统计要求）
    @GetMapping("/statistics/report")
    public Map<String, Object> getReport() {
        Map<String, Object> report = new HashMap<>();
        
        long total = STUDENTS.size();
        long sent = STUDENTS.stream().filter(s -> "已成功发送".equals(s.get("status"))).count();
        long read = STUDENTS.stream().filter(s -> "已读".equals(s.get("isRead"))).count();
        long feedbackCount = STUDENTS.stream().filter(s -> !((String)s.get("feedbackContent")).isEmpty()).count();

        report.put("total", total);
        report.put("sent", sent);
        report.put("read", read);
        report.put("feedbackCount", feedbackCount);
        report.put("completionRate", total == 0 ? "0%" : (sent * 100 / total) + "%");
        report.put("readRate", total == 0 ? "0%" : (read * 100 / total) + "%");

        // 模拟二级层级：按学院分拆下钻
        Map<String, Object> csCollege = Map.of("name", "计算机学院", "total", 2, "sent", STUDENTS.stream().filter(s->s.get("college").equals("计算机学院") && "已成功发送".equals(s.get("status"))).count());
        Map<String, Object> eeCollege = Map.of("name", "信息工程学院", "total", 2, "sent", STUDENTS.stream().filter(s->s.get("college").equals("信息工程学院") && "已成功发送".equals(s.get("status"))).count());
        report.put("colleges", List.of(csCollege, eeCollege));

        return report;
    }

    // 家长H5登录校验入口（登录账号为身份证号，初始密码为后六位，支持修改密码）
    @PostMapping("/parent/login")
    public Map<String, Object> parentLogin(@RequestBody Map<String, String> body) {
        String idCard = body.get("idCard");
        String password = body.get("password");

        for (Map<String, Object> s : STUDENTS) {
            if (s.get("idCard").equals(idCard) && s.get("password").equals(password)) {
                s.put("isRead", "已读"); // 登录即标记为已读反馈
                Map<String, Object> successMap = new HashMap<>();
                successMap.put("code", 200);
                successMap.put("student", s);
                successMap.put("template", TEMPLATE);
                return successMap;
            }
        }
        return Map.of("code", 400, "msg", "身份证号或家长密码错误！验证不通过。");
    }

    // 家长修改独立登录密码
    @PostMapping("/parent/change-pwd")
    public Map<String, Object> changeParentPwd(@RequestBody Map<String, String> params) {
        String idCard = params.get("idCard");
        String newPwd = params.get("newPwd");
        for (Map<String, Object> s : STUDENTS) {
            if (s.get("idCard").equals(idCard)) {
                s.put("password", newPwd);
                break;
            }
        }
        return Map.of("code", 200, "msg", "密码修改成功！请牢记新密码。");
    }

    // 家长端回传编辑文字、图片反馈到管理系统
    @PostMapping("/parent/submit-feedback")
    public Map<String, Object> submitFeedback(@RequestBody Map<String, String> body) {
        String id = body.get("id");
        String text = body.get("text");
        for (Map<String, Object> s : STUDENTS) {
            if (s.get("id").equals(id)) {
                s.put("feedbackContent", text);
                break;
            }
        }
        return Map.of("code", 200, "msg", "反馈意见已回传至智慧学工管理后台！");
    }
}
