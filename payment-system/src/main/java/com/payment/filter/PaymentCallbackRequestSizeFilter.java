package com.payment.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class PaymentCallbackRequestSizeFilter extends OncePerRequestFilter {

    public static final int MAX_HTTP_BODY_BYTES = 131_072;
    private static final String CALLBACK_PATH_PREFIX = "/v1/open/payments/callbacks/";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return !path.startsWith(CALLBACK_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_HTTP_BODY_BYTES) {
            reject(response);
            return;
        }
        if (request.getContentLengthLong() >= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        LimitedRequest limitedRequest = new LimitedRequest(request);
        try {
            filterChain.doFilter(limitedRequest, response);
        } catch (IOException e) {
            if (containsPayloadLimit(e)) {
                reject(response);
                return;
            }
            throw e;
        } catch (ServletException e) {
            if (containsPayloadLimit(e)) {
                reject(response);
                return;
            }
            throw e;
        }
        if (limitedRequest.isLimitExceeded()) {
            reject(response);
        }
    }

    private boolean containsPayloadLimit(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof PayloadLimitExceededException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void reject(HttpServletResponse response) throws IOException {
        if (!response.isCommitted()) {
            response.reset();
            response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Payment callback payload too large");
        }
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private LimitedServletInputStream inputStream;

        private LimitedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (inputStream == null) {
                inputStream = new LimitedServletInputStream(super.getInputStream());
            }
            return inputStream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        private boolean isLimitExceeded() {
            return inputStream != null && inputStream.isLimitExceeded();
        }
    }

    private static final class LimitedServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private long bytesRead;
        private boolean limitExceeded;

        private LimitedServletInputStream(ServletInputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                addBytes(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = delegate.read(buffer, offset, length);
            if (count > 0) {
                addBytes(count);
            }
            return count;
        }

        private void addBytes(int count) throws PayloadLimitExceededException {
            bytesRead += count;
            if (bytesRead > MAX_HTTP_BODY_BYTES) {
                limitExceeded = true;
                throw new PayloadLimitExceededException();
            }
        }

        private boolean isLimitExceeded() {
            return limitExceeded;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }
    }

    private static final class PayloadLimitExceededException extends IOException {
    }
}
