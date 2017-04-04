package io.pivotal.security.auth;

import static io.pivotal.security.auth.UserContext.AUTH_METHOD_MUTUAL_TLS;
import static io.pivotal.security.auth.UserContext.AUTH_METHOD_UAA;
import static io.pivotal.security.util.AuthConstants.INVALID_SCOPE_KEY_JWT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.util.DatabaseProfileResolver;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


//not using Spectrum runner, since Spring Context was not coming up correctly with it :(
@RunWith(SpringRunner.class)
@ActiveProfiles(value = {"unit-test"}, resolver = DatabaseProfileResolver.class)
@SpringBootTest(classes = CredentialManagerApp.class)
public class UserContextTest {

  @Mock
  ResourceServerTokenServices tokenServicesMock;

  @Autowired
  ResourceServerTokenServices realTokenServices;

  @Test
  public void fromAuthenication_readsFromOAuthDetails() throws Exception {
    OAuth2Authentication oauth2Authentication = setupOAuthMock("TEST_GRANT_TYPE");
    UserContext context = UserContext
        .fromAuthentication(oauth2Authentication, null, tokenServicesMock);

    assertThat(context.getUserId(), equalTo("TEST_USER_ID"));
    assertThat(context.getUserName(), equalTo("TEST_USER_NAME"));
    assertThat(context.getIssuer(), equalTo("TEST_UAA_URL"));
    assertThat(context.getScope(), equalTo("scope1,scope2"));
    assertThat(context.getGrantType(), equalTo("TEST_GRANT_TYPE"));
    assertThat(context.getValidFrom(), equalTo(1413495264L));
    assertThat(context.getValidUntil(), equalTo(1413538464L));
    assertThat(context.getAuthMethod(), equalTo(AUTH_METHOD_UAA));
  }


  @Test
  public void fromAuthentication_handlesAccessDeniedToken() throws Exception {

    OAuth2Authentication oauth2Authentication = setupOAuthMock("TEST_GRANT_TYPE");
    UserContext context = UserContext
        .fromAuthentication(oauth2Authentication, INVALID_SCOPE_KEY_JWT, realTokenServices);

    assertThat(context.getUserName(), equalTo("credhub_cli"));
    assertThat(context.getIssuer(), containsString("/oauth/token"));
    assertThat(context.getScope(), equalTo("credhub.bad_scope"));
    assertThat(context.getAuthMethod(), equalTo(AUTH_METHOD_UAA));
  }


  @Test
  public void fromAuthentication_handlesMtlsAuth() throws Exception {

    PreAuthenticatedAuthenticationToken mtlsAuth = setupMtlsMock();
    UserContext context = UserContext.fromAuthentication(mtlsAuth, null, null);

    assertThat(context.getUserName(), equalTo(null));
    assertThat(context.getUserId(), equalTo(null));
    assertThat(context.getIssuer(), equalTo(null));
    assertThat(context.getScope(), equalTo(null));
    assertThat(context.getValidFrom(), equalTo(1413495264L));
    assertThat(context.getValidUntil(), equalTo(1413538464L));
    assertThat(context.getClientId(), equalTo("CN=test_cn,OU=app:e054393e-c9c3-478b-9047-e6d05c307bf2"));
    assertThat(context.getAuthMethod(), equalTo(AUTH_METHOD_MUTUAL_TLS));
  }

  @Test
  public void getAclUser_fromOAuthPasswordGrant_returnsTheUserGuid() throws Exception {
    OAuth2Authentication oauth2Authentication = setupOAuthMock("password");
    UserContext context = UserContext.fromAuthentication(oauth2Authentication,
        null,
        tokenServicesMock);

    assertThat(context.getAclUser(),
        equalTo("uaa-user:TEST_USER_ID"));
  }

  @Test
  public void getAclUser_fromOAuthClientGrant_returnsTheClientId() throws Exception {
    OAuth2Authentication oauth2Authentication = setupOAuthMock("client_credentials");
    UserContext context = UserContext.fromAuthentication(oauth2Authentication,
        null,
        tokenServicesMock);

    assertThat(context.getAclUser(),
        equalTo("uaa-client:TEST_CLIENT_ID"));
  }

  @Test
  public void getAclUser_fromMtlsCertificate_returnsAppGuid() throws Exception {
    final PreAuthenticatedAuthenticationToken authenticationToken = setupMtlsMock();
    UserContext context = UserContext.fromAuthentication(authenticationToken,
        null,
        tokenServicesMock);

    assertThat(context.getAclUser(),
        equalTo("mtls:app:e054393e-c9c3-478b-9047-e6d05c307bf2"));
  }


  private OAuth2Authentication setupOAuthMock(String grantType) {
    OAuth2Authentication authentication = mock(OAuth2Authentication.class);
    OAuth2Request oauth2Request = spy(new OAuth2Request(
        null,
        "TEST_CLIENT_ID",
        null,
        false,
        null,
        null,
        null,
        null,
        null));
    OAuth2AccessToken token = mock(OAuth2AccessToken.class);
    OAuth2AuthenticationDetails authDetails = mock(OAuth2AuthenticationDetails.class);

    Map<String, Object> additionalInformation = new HashMap<>();
    additionalInformation.put("user_id", "TEST_USER_ID");
    additionalInformation.put("user_name", "TEST_USER_NAME");
    additionalInformation.put("iss", "TEST_UAA_URL");
    additionalInformation.put("iat", 1413495264);

    Set<String> scopes = new HashSet<>();
    scopes.add("scope1");
    scopes.add("scope2");

    when(oauth2Request.getGrantType()).thenReturn(grantType);
    when(authentication.getDetails()).thenReturn(authDetails);
    when(authDetails.getTokenValue()).thenReturn("tokenValue");

    when(authentication.getOAuth2Request()).thenReturn(oauth2Request);
    when(token.getAdditionalInformation()).thenReturn(additionalInformation);
    when(token.getExpiration()).thenReturn(Date.from(Instant.ofEpochSecond(1413538464)));
    when(token.getScope()).thenReturn(scopes);

    when(tokenServicesMock.readAccessToken("tokenValue")).thenReturn(token);

    return authentication;
  }

  private PreAuthenticatedAuthenticationToken setupMtlsMock() {
    X509Certificate certificate = mock(X509Certificate.class);
    Principal principal = mock(Principal.class);
    PreAuthenticatedAuthenticationToken token = mock(PreAuthenticatedAuthenticationToken.class);

    when(certificate.getSubjectDN()).thenReturn(principal);
    when(principal.getName()).thenReturn("CN=test_cn,OU=app:e054393e-c9c3-478b-9047-e6d05c307bf2");

    when(certificate.getNotAfter()).thenReturn(Date.from(Instant.ofEpochSecond(1413538464L)));
    when(certificate.getNotBefore()).thenReturn(Date.from(Instant.ofEpochSecond(1413495264L)));
    when(token.getCredentials()).thenReturn(certificate);

    return token;
  }
}
