package com.eskcti.algashop.billing;

import com.eskcti.algashop.billing.infrastructure.security.BillingSecurityConfig;
import com.eskcti.algashop.billing.infrastructure.security.check.OAuth2SecurityCheckApplicationServiceImpl;
import com.eskcti.algashop.billing.infrastructure.token.JwtAuthenticationConverterConfig;
import com.eskcti.algashop.billing.infrastructure.token.JwtGrantedAuthoritiesDelegatingConverter;
import com.eskcti.algashop.billing.utils.MockJwtDecoderConfig;
import com.eskcti.algashop.billing.utils.MockJwtDecoderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@ActiveProfiles("it")
@Import({
        BillingSecurityConfig.class,
        MockJwtDecoderConfig.class,
        JwtAuthenticationConverterConfig.class,
        JwtGrantedAuthoritiesDelegatingConverter.class,
        OAuth2SecurityCheckApplicationServiceImpl.class
})
public abstract class AbstractControllerIT {

    @Autowired
    protected MockMvc mockMvc;

    protected ResultActions authenticated(MockHttpServletRequestBuilder request) throws Exception {
        return withToken(request, MockJwtDecoderFactory.DEFAULT_TOKEN_VALUE);
    }

    protected ResultActions authenticatedAsCustomer(MockHttpServletRequestBuilder request) throws Exception {
        return withToken(request, MockJwtDecoderFactory.CUSTOMER_TOKEN_VALUE);
    }

    protected ResultActions authenticatedAsMachine(MockHttpServletRequestBuilder request) throws Exception {
        return withToken(request, MockJwtDecoderFactory.MACHINE_TOKEN_VALUE);
    }

    private ResultActions withToken(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

}
