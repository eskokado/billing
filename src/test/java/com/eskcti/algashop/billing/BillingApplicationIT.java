package com.eskcti.algashop.billing;

import com.eskcti.algashop.billing.utils.MockJwtDecoderConfig;
import com.eskcti.algashop.billing.utils.TestcontainerPostgreSQLConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("it")
@Import({ TestcontainerPostgreSQLConfig.class, MockJwtDecoderConfig.class })
class BillingApplicationIT {

	@Test
	void contextLoads() {
	}

}
