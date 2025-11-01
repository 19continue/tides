package com.tides.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tides.threadlocal.BaseParameterHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tides.constant.Constant.TRACE_ID;

/**
 * 后台操作人上下文，统一解析审计需要的 actor/source 信息。
 */
@Service
public class BackManageOperatorContextService {

    private static final String DEFAULT_OPERATOR_NAME = "平台运营";
    private static final String SOURCE_MANAGE_WEB = "MANAGE_WEB";
    private static final String SOURCE_API = "API";
    private static final String SOURCE_SYSTEM = "SYSTEM";
    private static final String USER_DETAIL = "userDetail";
    private static final Set<String> DEFAULT_ADMIN_ROLE_SET = Set.of("PLATFORM_SUPER_ADMIN", "PROJECT_APPROVER");

    public OperatorContext resolve(Long fallbackOperatorId, String fallbackOperatorName) {
        JSONObject userDetail = selectBackManageUserDetail();
        HttpServletRequest request = currentRequest();
        Long operatorId = firstNonNull(toLong(getString(userDetail, "userId")), fallbackOperatorId);
        String operatorName = firstNonBlank(getString(userDetail, "realName"), getString(userDetail, "username"),
                fallbackOperatorName, DEFAULT_OPERATOR_NAME);
        String operatorAccount = firstNonBlank(getString(userDetail, "username"), fallbackOperatorName);
        Set<String> roleCodeSet = resolveRoleCodeSet(userDetail, operatorId, operatorAccount);
        return new OperatorContext(operatorId, operatorName, operatorAccount, resolveOperationSource(request),
                resolveTraceId(request), resolveClientIp(request), resolveUserAgent(request),
                roleCodeSet, getString(userDetail, "departmentId"), getString(userDetail, "departmentName"),
                getString(userDetail, "dataScopeType"));
    }

    private JSONObject selectBackManageUserDetail() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            Object userDetail = StpUtil.getSession().get(USER_DETAIL);
            return Objects.isNull(userDetail) ? null : JSON.parseObject(String.valueOf(userDetail));
        } catch (Exception ignored) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String resolveOperationSource(HttpServletRequest request) {
        if (Objects.isNull(request)) {
            return SOURCE_SYSTEM;
        }
        String backManage = request.getHeader("back_manage");
        return "true".equals(backManage) ? SOURCE_MANAGE_WEB : SOURCE_API;
    }

    private String resolveTraceId(HttpServletRequest request) {
        return firstNonBlank(header(request, TRACE_ID), BaseParameterHolder.getParameter(TRACE_ID),
                header(request, "X-Request-Id"), header(request, "X-B3-TraceId"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = header(request, "X-Forwarded-For");
        if (isNotBlank(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return firstNonBlank(header(request, "X-Real-IP"), Objects.isNull(request) ? null : request.getRemoteAddr());
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return header(request, "User-Agent");
    }

    private String header(HttpServletRequest request, String name) {
        return Objects.isNull(request) ? null : request.getHeader(name);
    }

    private String getString(JSONObject object, String key) {
        return Objects.isNull(object) ? null : object.getString(key);
    }

    private Long toLong(String value) {
        if (!isNotBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (Objects.nonNull(value)) {
                return value;
            }
        }
        return null;
    }

    private Set<String> parseRoleCodeSet(JSONObject userDetail) {
        if (Objects.isNull(userDetail)) {
            return Collections.emptySet();
        }
        Object roleCodes = firstNonNull(userDetail.get("roleCodeList"), userDetail.get("roleCodes"),
                userDetail.get("roles"), userDetail.get("roleCode"));
        if (Objects.isNull(roleCodes)) {
            return Collections.emptySet();
        }
        if (roleCodes instanceof Iterable<?> iterable) {
            Set<String> result = new LinkedHashSet<>();
            for (Object item : iterable) {
                if (Objects.nonNull(item) && isNotBlank(String.valueOf(item))) {
                    result.add(String.valueOf(item).trim());
                }
            }
            return Collections.unmodifiableSet(result);
        }
        return Arrays.stream(String.valueOf(roleCodes).split(","))
                .map(String::trim)
                .filter(this::isNotBlank)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> resolveRoleCodeSet(JSONObject userDetail, Long operatorId, String operatorAccount) {
        Set<String> roleCodeSet = parseRoleCodeSet(userDetail);
        if (!roleCodeSet.isEmpty()) {
            return roleCodeSet;
        }
        if (Objects.equals(operatorId, 1L) || "admin".equalsIgnoreCase(operatorAccount)) {
            return DEFAULT_ADMIN_ROLE_SET;
        }
        return Collections.emptySet();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isNotBlank(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }

    public record OperatorContext(Long operatorId,
                                  String operatorName,
                                  String operatorAccount,
                                  String operationSource,
                                  String requestTraceId,
                                  String clientIp,
                                  String userAgent,
                                  Set<String> roleCodeSet,
                                  String departmentId,
                                  String departmentName,
                                  String dataScopeType) {
    }
}
