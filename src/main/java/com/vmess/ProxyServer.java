package com.vmess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ProxyServer {
    private static final String CONFIG_DIR = "/etc/proxy-server";
    private static final String XRAY_PATH = "/usr/local/bin/xray";
    private static final String HYSTERIA_PATH = "/usr/local/bin/hysteria";
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== VMess/Hysteria2 代理服务器 ===");
        System.out.println("初始化中...\n");
        
        Files.createDirectories(Paths.get(CONFIG_DIR));
        
        String uuid = UUID.randomUUID().toString();
        int vmessPort = 10086;
        int hy2Port = 36712;
        String serverIp = getServerIp();
        
        installDependencies();
        
        VmessConfig vmessConfig = generateVmessConfig(uuid, vmessPort);
        Hysteria2Config hy2Config = generateHysteria2Config(hy2Port);
        
        startServices(vmessConfig, hy2Config);
        
        System.out.println("\n╔═══════════════════════════════════════════════╗");
        System.out.println("║           代理链接 (保存使用)                 ║");
        System.out.println("╠═══════════════════════════════════════════════╣");
        System.out.println("║ VMess:                                        ║");
        System.out.println("║ " + generateVmessLink(uuid, serverIp, vmessPort));
        System.out.println("║                                               ║");
        System.out.println("║ Hysteria2:                                    ║");
        System.out.println("║ " + hy2Config.getLink(serverIp));
        System.out.println("╚═══════════════════════════════════════════════╝");
        System.out.println("\n服务运行中...");
        
        Thread.currentThread().join();
    }
    
    private static String getServerIp() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "curl -s ifconfig.me || curl -s ip.sb || echo 127.0.0.1"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String ip = reader.readLine();
            reader.close();
            p.waitFor();
            return ip != null && !ip.isEmpty() ? ip.trim() : "YOUR_SERVER_IP";
        } catch (Exception e) {
            return "YOUR_SERVER_IP";
        }
    }
    
    private static void installDependencies() throws Exception {
        System.out.println("检查并安装依赖...");
        
        if (!Files.exists(Paths.get(XRAY_PATH))) {
            System.out.println("安装 Xray-core...");
            exec("bash -c \"curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh | bash -s -- install\"");
        } else {
            System.out.println("✓ Xray 已安装");
        }
        
        if (!Files.exists(Paths.get(HYSTERIA_PATH))) {
            System.out.println("安装 Hysteria2...");
            exec("bash -c \"curl -fsSL https://get.hy2.sh/ | bash\"");
        } else {
            System.out.println("✓ Hysteria2 已安装");
        }
        
        System.out.println();
    }
    
    private static VmessConfig generateVmessConfig(String uuid, int port) throws Exception {
        VmessConfig config = new VmessConfig(uuid, port);
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(config.toJson());
        
        Files.writeString(Paths.get(CONFIG_DIR, "vmess.json"), json);
        System.out.println("✓ VMess 配置已生成");
        
        return config;
    }
    
    private static Hysteria2Config generateHysteria2Config(int port) throws Exception {
        exec("openssl req -x509 -nodes -newkey rsa:2048 -keyout " + CONFIG_DIR + "/key.pem -out " + CONFIG_DIR + "/cert.pem -days 3650 -subj '/CN=bing.com' 2>/dev/null");
        
        String password = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Hysteria2Config config = new Hysteria2Config(port, password);
        
        Files.writeString(Paths.get(CONFIG_DIR, "hysteria2.yaml"), config.toYaml());
        Files.writeString(Paths.get(CONFIG_DIR, "hy2_password.txt"), password);
        
        System.out.println("✓ Hysteria2 配置已生成");
        
        return config;
    }
    
    private static void startServices(VmessConfig vmess, Hysteria2Config hy2) throws Exception {
        System.out.println("\n启动服务...");
        
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("xray", "-c", CONFIG_DIR + "/vmess.json");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("started")) {
                        System.out.println("✓ Xray 服务启动成功 (端口 " + vmess.port + ")");
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("✗ Xray 启动失败: " + e.getMessage());
            }
        }).start();
        
        Thread.sleep(1000);
        
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("hysteria", "server", "-c", CONFIG_DIR + "/hysteria2.yaml");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("server up and running")) {
                        System.out.println("✓ Hysteria2 服务启动成功 (端口 " + hy2.port + ")");
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("✗ Hysteria2 启动失败: " + e.getMessage());
            }
        }).start();
        
        Thread.sleep(2000);
    }
    
    private static String generateVmessLink(String uuid, String ip, int port) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("v", "2");
        config.put("ps", "VMess-" + ip);
        config.put("add", ip);
        config.put("port", String.valueOf(port));
        config.put("id", uuid);
        config.put("aid", "0");
        config.put("net", "tcp");
        config.put("type", "none");
        config.put("host", "");
        config.put("path", "");
        config.put("tls", "");
        
        String json = new Gson().toJson(config);
        return "vmess://" + Base64.getEncoder().encodeToString(json.getBytes());
    }
    
    private static void exec(String cmd) throws Exception {
        Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
        p.waitFor(30, TimeUnit.SECONDS);
    }
    
    static class VmessConfig {
        String uuid;
        int port;
        
        VmessConfig(String uuid, int port) {
            this.uuid = uuid;
            this.port = port;
        }
        
        Map<String, Object> toJson() {
            Map<String, Object> config = new LinkedHashMap<>();
            
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("loglevel", "warning");
            config.put("log", log);
            
            Map<String, Object> inbound = new LinkedHashMap<>();
            inbound.put("port", port);
            inbound.put("protocol", "vmess");
            
            Map<String, Object> settings = new LinkedHashMap<>();
            Map<String, Object> client = new LinkedHashMap<>();
            client.put("id", uuid);
            client.put("alterId", 0);
            settings.put("clients", Arrays.asList(client));
            inbound.put("settings", settings);
            
            Map<String, Object> streamSettings = new LinkedHashMap<>();
            streamSettings.put("network", "tcp");
            inbound.put("streamSettings", streamSettings);
            
            config.put("inbounds", Arrays.asList(inbound));
            
            Map<String, Object> outbound = new LinkedHashMap<>();
            outbound.put("protocol", "freedom");
            config.put("outbounds", Arrays.asList(outbound));
            
            return config;
        }
    }
    
    static class Hysteria2Config {
        int port;
        String password;
        
        Hysteria2Config(int port, String password) {
            this.port = port;
            this.password = password;
        }
        
        String toYaml() {
            return String.format(
                "listen: :%d\n\n" +
                "tls:\n" +
                "  cert: %s/cert.pem\n" +
                "  key: %s/key.pem\n\n" +
                "auth:\n" +
                "  type: password\n" +
                "  password: %s\n\n" +
                "masquerade:\n" +
                "  type: proxy\n" +
                "  proxy:\n" +
                "    url: https://www.bing.com\n" +
                "    rewriteHost: true\n",
                port, CONFIG_DIR, CONFIG_DIR, password
            );
        }
        
        String getLink(String ip) {
            return String.format("hysteria2://%s@%s:%d?insecure=1#Hysteria2-%s", password, ip, port, ip);
        }
    }
}
