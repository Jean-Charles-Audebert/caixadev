package dev.caixadev.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientInfoTest {

    @Test
    void ipPrefersCloudflareHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.7");

        assertThat(ClientInfo.ip(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void ipFallsBackToFirstForwardedAddressThenRemoteAddr() {
        MockHttpServletRequest forwarded = new MockHttpServletRequest();
        forwarded.addHeader("X-Forwarded-For", " 198.51.100.2 , 172.18.0.5");
        assertThat(ClientInfo.ip(forwarded)).isEqualTo("198.51.100.2");

        MockHttpServletRequest direct = new MockHttpServletRequest();
        direct.setRemoteAddr("192.0.2.9");
        assertThat(ClientInfo.ip(direct)).isEqualTo("192.0.2.9");
    }

    @Test
    void ipCannotInjectLogLines() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "1.2.3.4\nFAKE LOG LINE");

        assertThat(ClientInfo.ip(request)).doesNotContain("\n").startsWith("1.2.3.4_");
    }

    @Test
    void maskEmailKeepsFirstLetterAndDomain() {
        assertThat(ClientInfo.maskEmail("jean@example.com")).isEqualTo("j***@example.com");
        assertThat(ClientInfo.maskEmail("pas-un-email")).isEqualTo("***");
        assertThat(ClientInfo.maskEmail(" ")).isEqualTo("-");
        assertThat(ClientInfo.maskEmail(null)).isEqualTo("-");
    }
}
