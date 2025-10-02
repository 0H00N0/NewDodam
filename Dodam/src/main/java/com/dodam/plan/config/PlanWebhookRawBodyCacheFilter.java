// src/main/java/com/dodam/plan/config/WebhookRawBodyCacheFilter.java
package com.dodam.plan.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // ★ 최상위로 올려서 가장 먼저 래핑되게
public class PlanWebhookRawBodyCacheFilter implements Filter {
  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest http = (HttpServletRequest) req;
    String uri = http.getRequestURI();
    if (uri != null && (uri.equals("/webhooks/pg") || uri.startsWith("/webhooks/pg/"))) {
      ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(http);
      chain.doFilter(wrapper, res);
      return;
    }
    chain.doFilter(req, res);
  }
}
