package com.srm.masterdata.application.service;

import java.util.List;
import java.util.Map;

public interface MasterDataBatchGateway {
    record ExportPage(List<String> headers, List<List<String>> rows, long total) {
        public ExportPage {
            headers = List.copyOf(headers);
            rows = rows.stream().map(List::copyOf).toList();
        }
    }

    List<String> headers(String objectType);
    void importRow(String objectType, Map<String, String> values);
    ExportPage exportPage(String objectType, int page, int pageSize);
}
