package rs.raf.user_service.unit;

import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rs.raf.user_service.client.FeignClientInterceptor;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FeignClientInterceptorTest {

    private FeignClientInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new FeignClientInterceptor();
    }

    @Test
    void testApplyAddsAuthorizationHeader() {
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertTrue(template.headers().containsKey("Authorization"));
        assertTrue(template.headers().get("Authorization").iterator().next().startsWith("Bearer "));
    }
}

