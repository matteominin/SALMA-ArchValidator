package com.matteominin.pdf_extractor.service;

import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerificationReportService {
    public static String generateVerificationReport(Object verificationData) {
        ArrayList<?> verificationArray = (ArrayList<?>) verificationData;

        StringBuilder report = new StringBuilder();
        report.append("## Verification Report\n\n");

        for(int i=0; i<verificationArray.size(); i++) {
            Object item = verificationArray.get(i);

            appendField("## ", "req_id", item, report);
            appendField("**Description**: ", "description", item, report);
            appendField("\n**Status**: ", "verification_status", item, report);

            report.append("\n**Traceability Map**:\n\n");
            Object traceabilityMap = ((java.util.Map<?, ?>) item).get("traceability_map");
            if (traceabilityMap instanceof ArrayList) {
                ArrayList<?> traceabilityList = (ArrayList<?>) traceabilityMap;
                for (Object traceItem : traceabilityList) {
                    appendField("**Use Case ID:** ", "use_case_id", traceItem, report);
                    appendField("\n**Use Case Name:** ", "use_case_name", traceItem, report);

                    ArrayList<?> relatedComponents = (ArrayList<?>) ((java.util.Map<?, ?>) traceItem).get("related_components");
                    if (relatedComponents != null) {
                        report.append("\n**Related Components:**\n ");
                        for (Object component : relatedComponents) {
                            report.append(" - ").append(component.toString()).append("\n");
                        }
                    }
                }
            }

            appendField("\n**Reason:** ", "reason", item, report);
        }

        return report.toString();
    }

    private static void appendField(String reportField, String filed, Object item, StringBuilder report) {
        Object value = ((java.util.Map<?, ?>) item).get(filed);
        report.append(reportField).append(value != null ? value.toString() : "<unknown>").append("\n");
    }
}


