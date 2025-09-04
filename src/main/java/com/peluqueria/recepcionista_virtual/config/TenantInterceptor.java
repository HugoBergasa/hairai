package com.peluqueria.recepcionista_virtual.config;

import com.peluqueria.recepcionista_virtual.filter.TenantFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Autowired
    private TenantFilter tenantFilter;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String tenantId = (String) request.getAttribute("tenantId");
        if (tenantId != null) {
            tenantFilter.enableFilter(tenantId);
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {

        tenantFilter.disableFilter();
    }
}