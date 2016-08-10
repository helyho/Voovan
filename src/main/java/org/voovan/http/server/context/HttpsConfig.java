package org.voovan.http.server.context;;

/**
 * Https配置类
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpsConfig {
    private String certificateFile;
    private String certificatePassword;
    private String keyPassword;

    public String getCertificateFile() {
        return certificateFile;
    }

    protected void setCertificateFile(String certificateFile) {
        this.certificateFile = certificateFile;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    protected void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    protected void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }
}
