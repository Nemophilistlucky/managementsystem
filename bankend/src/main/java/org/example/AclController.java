package org.example;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin 
public class AclController {

    private static final Map<String, Map<String, String>> NETWORK_MAP = new HashMap<>();
    static {
        NETWORK_MAP.put("VLAN10", Map.of("name", "核心医疗区", "subnet", "192.168.10.0/24"));
        NETWORK_MAP.put("VLAN30", Map.of("name", "患者家属区", "subnet", "192.168.30.0/24"));
        NETWORK_MAP.put("VLAN40", Map.of("name", "公共区域", "subnet", "192.168.40.0/24"));
        NETWORK_MAP.put("VLAN50", Map.of("name", "服务器区", "subnet", "192.168.50.0/24"));
    }

    @PostMapping("/test_connect")
    public Map<String, Object> testConnect(@RequestBody Map<String, Object> requestParams) {
        String source = (String) requestParams.get("source");
        String target = (String) requestParams.get("target");
        String protocol = (String) requestParams.get("protocol");
        
        int port = 0;
        if (requestParams.get("port") != null && !requestParams.get("port").toString().isEmpty()) {
            port = Integer.parseInt(requestParams.get("port").toString());
        }

        boolean success = true;
        String message = "Permit: 默认全网OSPF路由畅通，核心交换机放行流量。";

        if ("VLAN30".equals(source) && "VLAN10".equals(target)) {
            success = false;
            message = "【ACL 101 拦截】拒绝访问！患者家属区严禁探测核心医疗区，数据包已被丢弃。";
        }
        else if ("VLAN40".equals(source) && "VLAN50".equals(target)) {
            if ("TCP".equals(protocol) && port == 80) {
                success = true;
                message = "【ACL 102 放行】匹配成功：允许公共区域通过 TCP 80 端口正常访问医院 Web 官网。";
            } else {
                success = false;
                message = "【ACL 102 拦截】拒绝访问！公共区域仅限访问Web服务器的80端口。当前非标端口被拦截。";
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        return response;
    }
}
