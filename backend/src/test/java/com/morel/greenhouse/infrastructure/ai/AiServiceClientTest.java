package com.morel.greenhouse.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morel.greenhouse.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiServiceClientTest {

    @Test
    void convertsRuntimeFailuresToBusinessExceptionForAllEndpoints() {
        AiServiceClient client = new AiServiceClient("bad url", new ObjectMapper());

        assertThatThrownBy(() -> client.chat(java.util.Map.of("q", "1")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> client.visionDiagnosis(java.util.Map.of("image", "x")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(client::rebuildIndex)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void readResponseBodyHandlesSuccessErrorAndNullStreams() throws Exception {
        AiServiceClient client = new AiServiceClient("http://localhost", new ObjectMapper());
        Method method = AiServiceClient.class.getDeclaredMethod("readResponseBody", HttpURLConnection.class, int.class);
        method.setAccessible(true);

        HttpURLConnection success = mock(HttpURLConnection.class);
        when(success.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8)));
        assertThat(method.invoke(client, success, 200)).isEqualTo("ok");

        HttpURLConnection error = mock(HttpURLConnection.class);
        when(error.getErrorStream()).thenReturn(new java.io.ByteArrayInputStream("bad".getBytes(StandardCharsets.UTF_8)));
        assertThat(method.invoke(client, error, 500)).isEqualTo("bad");

        HttpURLConnection noBody = mock(HttpURLConnection.class);
        when(noBody.getErrorStream()).thenReturn(null);
        assertThat(method.invoke(client, noBody, 500)).isEqualTo("");
    }
}
