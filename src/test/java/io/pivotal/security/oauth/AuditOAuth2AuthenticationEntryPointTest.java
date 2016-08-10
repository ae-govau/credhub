package io.pivotal.security.oauth;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.config.NoExpirationSymmetricKeySecurityConfiguration;
import io.pivotal.security.entity.AuthFailureAuditRecord;
import io.pivotal.security.repository.AuthFailureAuditRecordRepository;
import io.pivotal.security.service.AuditRecordParameters;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.util.Map;

import static com.greghaskins.spectrum.Spectrum.*;
import static io.pivotal.security.config.NoExpirationSymmetricKeySecurityConfiguration.EXPIRED_SYMMETRIC_KEY_JWT;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(Spectrum.class)
@SpringApplicationConfiguration(classes = CredentialManagerApp.class)
@WebAppConfiguration
public class AuditOAuth2AuthenticationEntryPointTest {

  @Autowired
  WebApplicationContext applicationContext;

  @Autowired
  Filter springSecurityFilterChain;

  @Autowired
  AuditOAuth2AuthenticationEntryPoint subject;

  @Autowired
  AuthFailureAuditRecordRepository auditRecordRepository;

  @Autowired
  ResourceServerTokenServices tokenServices;

  AuthenticationException authenticationException;

  AuditRecordParameters auditRecordParameters;

  MockHttpServletRequestBuilder get;

  private MockMvc mockMvc;

// three scenarios: token_expired, no_token, invalid_token
  // response can be null
  // need a mock request
  //Access Token Expired = token expired
  // token not valid
  // no token = Full authentication is required to access this resource
  {
    wireAndUnwire(this);

    beforeEach(() -> {
      mockMvc = MockMvcBuilders
          .webAppContextSetup(applicationContext)
          .addFilter(springSecurityFilterChain)
          .build();
    });

    afterEach(() -> {
      auditRecordRepository.deleteAll();
    });

    describe("when the token is invalid", () -> {
      beforeEach(() -> {
        get = get("/api/v1/data/test")
            .header("Authorization", "Bearer " + NoExpirationSymmetricKeySecurityConfiguration.INVALID_SYMMETRIC_KEY_JWT)
            .header("X-Forwarded-For", "1.1.1.1,2.2.2.2")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .with(request -> {
              request.setRemoteAddr("12346");
              return request;
            });
        mockMvc.perform(get);
      });

      it("logs the 'token_invalid' auth exception to the database", () -> {
        assertThat(auditRecordRepository.count(), equalTo(1L));
        AuthFailureAuditRecord auditRecord = auditRecordRepository.findAll().get(0);
        assertThat(auditRecord.getPath(), equalTo("/api/v1/data/test"));
        assertThat(auditRecord.getOperation(), equalTo("credential_access"));
        assertThat(auditRecord.getRequesterIp(), equalTo("12346"));
        assertThat(auditRecord.getXForwardedFor(), equalTo("1.1.1.1,2.2.2.2"));
        assertThat(auditRecord.getFailureReason(), equalTo(("invalid_token")));
        assertThat(auditRecord.getFailureDescription(), equalTo(("Cannot convert access token to JSON")));
        assertThat(auditRecord.getUserId(), equalTo(null));
        assertThat(auditRecord.getUserName(), equalTo(null));
        assertThat(auditRecord.getUaaUrl(), equalTo(null));
        assertThat(auditRecord.getTokenIssued(), equalTo(-1L));
        assertThat(auditRecord.getTokenExpires(), equalTo(-1L));
      });
    });

    describe("when the token is expired", () -> {
      beforeEach(() -> {
        get = get("/api/v1/data/foo")
            .header("Authorization", "Bearer " + EXPIRED_SYMMETRIC_KEY_JWT)
            .header("X-Forwarded-For", "1.1.1.1,2.2.2.2")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .with(request -> {
              request.setRemoteAddr("12346");
              return request;
            });
        mockMvc.perform(get);
      });

      it("logs the 'token_expired' auth exception to the database", () -> {
        OAuth2AccessToken accessToken = tokenServices.readAccessToken(EXPIRED_SYMMETRIC_KEY_JWT);
        Map<String, Object> additionalInformation = accessToken.getAdditionalInformation();

        assertThat(auditRecordRepository.count(), equalTo(1L));
        AuthFailureAuditRecord auditRecord = auditRecordRepository.findAll().get(0);

        assertThat(auditRecord.getPath(), equalTo("/api/v1/data/test"));
        assertThat(auditRecord.getOperation(), equalTo("credential_access"));
        assertThat(auditRecord.getRequesterIp(), equalTo("12346"));
        assertThat(auditRecord.getXForwardedFor(), equalTo("1.1.1.1,2.2.2.2"));
        assertThat(auditRecord.getFailureReason(), equalTo("invalid_token"));
        assertThat(auditRecord.getFailureDescription(), equalTo("Cannot convert access token to JSON"));
        assertThat(auditRecord.getUserId(), equalTo(additionalInformation.get("user_id")));
        assertThat(auditRecord.getUserName(), equalTo(additionalInformation.get("user_name")));
        assertThat(auditRecord.getUaaUrl(), equalTo(additionalInformation.get("iss")));
        assertThat(auditRecord.getTokenIssued(), equalTo(((Number) additionalInformation.get("iat")).longValue())); // 1469051704L
        assertThat(auditRecord.getTokenExpires(), equalTo(accessToken.getExpiration().getTime() / 1000)); // 1469051824L
      });
    });
  }
}