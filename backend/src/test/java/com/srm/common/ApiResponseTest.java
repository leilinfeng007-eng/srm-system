package com.srm.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.common.api.ApiResponse;
import com.srm.common.api.PageResult;
import com.srm.common.web.TraceContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ApiResponseTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void successResponseCarriesTraceIdAndTimestamp() {
        MDC.put(TraceContext.TRACE_ID, "trace-test-1234");

        ApiResponse<String> response = ApiResponse.success("ready");

        assertThat(response.code()).isEqualTo("0");
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ready");
        assertThat(response.traceId()).isEqualTo("trace-test-1234");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void pageResultCalculatesTotalPages() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 1, 2, 5);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.items()).containsExactly("a", "b");
    }
}

