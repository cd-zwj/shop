package com.payment.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentCallbackRequestSizeFilterTest {

    @Test
    void oversizedKnownLengthShouldBeRejectedBeforeFilterChain() throws Exception {
        PaymentCallbackRequestSizeFilter filter = new PaymentCallbackRequestSizeFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/open/payments/callbacks/ALIPAY_PAGE");
        request.setContent(new byte[PaymentCallbackRequestSizeFilter.MAX_HTTP_BODY_BYTES + 1]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void nonCallbackRequestShouldPassThrough() throws Exception {
        PaymentCallbackRequestSizeFilter filter = new PaymentCallbackRequestSizeFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/app/orders");
        request.setContent(new byte[PaymentCallbackRequestSizeFilter.MAX_HTTP_BODY_BYTES + 1]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void oversizedChunkedBodyShouldOverrideHandledReadErrorWith413() throws Exception {
        PaymentCallbackRequestSizeFilter filter = new PaymentCallbackRequestSizeFilter();
        byte[] body = new byte[PaymentCallbackRequestSizeFilter.MAX_HTTP_BODY_BYTES + 1];
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/v1/open/payments/callbacks/ALIPAY_PAGE") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(body);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (wrappedRequest, wrappedResponse) -> {
            try {
                ((ServletInputStream) wrappedRequest.getInputStream()).readAllBytes();
            } catch (java.io.IOException handledByMvc) {
                ((jakarta.servlet.http.HttpServletResponse) wrappedResponse).setStatus(500);
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
    }
}
