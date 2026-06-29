package com.sentinel.aiops.domain.enums;

/**
 * RBAC roles. Authority strings are prefixed with ROLE_ by Spring Security
 * via {@code hasRole(...)}.
 * <ul>
 *   <li>ADMIN — full control: manage users, services, delete incidents.</li>
 *   <li>RESPONDER — on-call engineer: create/transition incidents, comment, run AI.</li>
 *   <li>VIEWER — read-only stakeholder (e.g. support, management).</li>
 * </ul>
 */
public enum Role { ADMIN, RESPONDER, VIEWER }
