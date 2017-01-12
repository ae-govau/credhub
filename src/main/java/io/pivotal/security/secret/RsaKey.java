package io.pivotal.security.secret;

public class RsaKey implements Secret {
  private final String publicKey;
  private final String privateKey;

  public RsaKey(String publicKey, String privateKey) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public String getPrivateKey() {
    return privateKey;
  }
}
