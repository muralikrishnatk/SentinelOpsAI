package com.sentinel.aiops.domain.enums;

/**
 * Multi-burn-rate alert tiers (Google SRE Workbook). FAST burns the budget
 * quickly (page now); SLOW is a sustained drain (page); TICKET is low-urgency.
 */
public enum BurnTier { FAST, SLOW, TICKET, NONE }
