package com.eskcti.algashop.billing.infrastructure;

import com.eskcti.algashop.billing.domain.model.creditcard.LimitedCreditCard;
import com.eskcti.algashop.billing.infrastructure.creditcard.fastpay.*;
import com.eskcti.algashop.billing.utils.MockJwtDecoderConfig;
import com.eskcti.algashop.billing.utils.TestcontainerPostgreSQLConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.TemplateEngine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Collections;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest
@ActiveProfiles("it")
@Import({ FastpayCreditCardTokenizationAPIClientConfig.class, TestcontainerPostgreSQLConfig.class,
        MockJwtDecoderConfig.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Sql(scripts = "classpath:sql/clean-database.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
@Sql(scripts = "classpath:sql/clean-database.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
public abstract class AbstractFastpayIT {

    private static final String WIREMOCK_FOLDER = "src/test/resources/wiremock/fastpay";

    @Autowired
    protected CreditCardProviderServiceFastpayImpl creditCardProvider;

    @Autowired
    protected FastpayCreditCardTokenizationAPIClient tokenizationAPIClient;

    protected static final UUID validCustomerId = UUID.randomUUID();
    protected static final String alwaysPaidCardNumber = "4622943127011022";

    protected static WireMockServer wiremockFastpay;

    protected static void startMock() {
        if (wiremockFastpay != null && wiremockFastpay.isRunning()) {
            return;
        }
        wiremockFastpay = new WireMockServer(options()
                .port(8788)
                .usingFilesUnderDirectory(WIREMOCK_FOLDER)
                .extensions(new ResponseTemplateTransformer(
                        TemplateEngine.defaultTemplateEngine(),
                        true,
                        new ClasspathFileSource(WIREMOCK_FOLDER),
                        Collections.emptyList())));
        wiremockFastpay.start();
    }

    protected static void stopMock() {
        if (wiremockFastpay != null && wiremockFastpay.isRunning()) {
            wiremockFastpay.stop();
        }
    }

    @BeforeEach
    public void startWireMock() {
        startMock();
    }

    @AfterEach
    public void stopWireMock() {
        stopMock();
    }

    protected LimitedCreditCard registerCard() {
        FastpayTokenizationInput input = FastpayTokenizationInput.builder()
                .number(alwaysPaidCardNumber)
                .cvv("222")
                .expMonth(1)
                .holderName("John Doe")
                .holderDocument("12345")
                .expYear(Year.now().getValue() + 5)
                .build();

        FastpayTokenizedCreditCardModel response = tokenizationAPIClient.tokenize(input);
        return creditCardProvider.register(validCustomerId, response.getTokenizedCard());
    }
}
