package com.peluqueria.recepcionista_virtual.filter;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Component
public class TenantFilter {

    @PersistenceContext
    private EntityManager entityManager;

    public void enableFilter(String tenantId) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter")
                .setParameter("tenantId", tenantId);
    }

    public void disableFilter() {
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter("tenantFilter");
    }
}