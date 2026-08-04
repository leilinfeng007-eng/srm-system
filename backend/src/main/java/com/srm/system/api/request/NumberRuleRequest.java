package com.srm.system.api.request;

public record NumberRuleRequest(String ruleCode, String ruleName, String objectType,
                                String prefix, String dateFormat, Integer serialLength,
                                String resetCycle, Boolean organizationDimension) {}
