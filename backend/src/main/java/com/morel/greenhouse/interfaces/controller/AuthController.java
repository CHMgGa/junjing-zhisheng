package com.morel.greenhouse.interfaces.controller;

import com.morel.greenhouse.application.dto.LoginRequest;
import com.morel.greenhouse.application.dto.RegisterRequest;
import com.morel.greenhouse.application.dto.ResetPasswordRequest;
import com.morel.greenhouse.application.dto.VerificationCodeRequest;
import com.morel.greenhouse.application.service.AuthService;
import com.morel.greenhouse.shared.api.ApiResult;
import com.morel.greenhouse.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResult.ok(authService.login(request, clientIp(servletRequest)));
    }

    @PostMapping("/register")
    public ApiResult<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.ok(authService.register(request));
    }

    @PostMapping("/codes")
    public ApiResult<Map<String, String>> sendCode(@Valid @RequestBody VerificationCodeRequest request, HttpServletRequest servletRequest) {
        return ApiResult.ok(authService.sendCode(request, clientIp(servletRequest)));
    }

    @PostMapping("/password/reset")
    public ApiResult<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResult.ok();
    }

    @GetMapping("/policies/{type}")
    public ApiResult<Map<String, String>> policy(@PathVariable String type) {
        if (!"privacy".equals(type) && !"terms".equals(type)) {
            throw new BusinessException(404, "政策文档不存在");
        }
        String title = "privacy".equals(type) ? "隐私政策" : "服务条款";
        String content = "privacy".equals(type)
                ? """
                一、信息收集范围
                本系统仅围绕羊肚菌智慧生态调控业务收集必要信息，包括账号、邮箱、头像、性别、个人简介、登录 IP 方位、设备状态、环境传感数据、告警记录、操作日志和问题反馈。注册、登录、验证码、资料维护、告警处理、设备绑定等功能会产生对应业务记录。

                二、信息使用目的
                上述信息用于身份认证、权限控制、生产管理、设备联动、告警追踪、安全审计、故障排查和服务改进。管理员只能在业务管理和安全运维需要的范围内查看必要数据，普通农户只能访问与自身账号和授权大棚相关的信息。

                三、数据保护措施
                系统采用角色权限、密码加密、验证码校验、关键操作日志和接口异常记录等措施保护数据。后续接入真实硬件、网关或第三方云服务时，将优先采用最小必要原则，只传输完成业务所需的数据。

                四、用户权利
                用户可以在个人中心维护邮箱、头像、手机号、性别、简介等资料，也可以通过问题反馈功能提交数据更正、账号异常、隐私疑问或系统故障。涉及管理员账号、设备安全和生产记录的数据变更，需要经过管理员审核。

                五、第三方服务说明
                邮件验证码可能通过已配置的邮件推送服务发送；系统不会出售或出租用户个人信息。若未来接入短信、地图、物联网云平台或硬件厂商服务，将在配置和文档中明确用途、范围和安全要求。
                """
                : """
                一、账号规则
                用户应保证注册信息真实有效，妥善保管账号和密码。admin 是系统保留管理员用户名，普通用户不得注册、冒用或尝试修改为该用户名。发现账号异常、密码泄露或非本人操作时，应及时修改密码并联系管理员。

                二、使用规范
                用户不得提交虚假生产数据、恶意触发告警、绕过权限访问他人数据、攻击系统接口或干扰设备联动。管理员应按照实际业务需要维护大棚、设备、用户和告警，不得滥用权限查看或修改无关信息。

                三、硬件与生产安全
                当前系统预留了传感器、控制器、网关和外部硬件平台的扩展结构。接入真实设备后，所有远程控制、阈值调整和告警处理都应遵守现场安全规范，重要操作建议由具备权限的人员复核。

                四、日志与审计
                平台会记录登录、资料修改、验证码发送、设备绑定、告警处理、反馈提交等关键操作日志，用于安全审计、责任追踪、故障排查和系统优化。日志不会用于与本系统无关的商业用途。

                五、服务调整与免责声明
                本项目当前用于教学实训和原型验证。系统会持续完善功能、界面和硬件接入能力；在演示环境中产生的数据不应直接作为真实农业生产决策的唯一依据。正式部署前应完成安全测试、权限审核、数据备份和硬件联调。
                """;
        return ApiResult.ok(Map.of("title", title, "content", content));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
