package com.pos.assistant;

import com.pos.assistant.kb.KnowledgeBase;
import com.pos.assistant.tools.PosApiTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PosApiToolsTest {

    private MockRestServiceServer server;
    private PosApiTools tools;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gw");
        server = MockRestServiceServer.bindTo(builder).build();
        tools = new PosApiTools(builder.build(), mock(KnowledgeBase.class));
    }

    @Test
    void exposesReadOnlyToolSet() {
        assertThat(tools.definitions()).extracting(t -> t.name())
                .contains("get_transaction", "get_inventory", "get_sales_summary", "search_knowledge_base")
                .noneMatch(name -> name.contains("refund") || name.contains("adjust"));
    }

    @Test
    void callsGatewayAndReportsErrorsAsToolErrors() {
        server.expect(requestTo("http://gw/api/inventory/ELE-002"))
                .andRespond(withSuccess("{\"sku\":\"ELE-002\",\"onHand\":3}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://gw/api/transactions/TX-404"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"code\":\"TRANSACTION_NOT_FOUND\"}"));
        server.expect(requestTo("http://gw/api/recommendations?sku=COF-001&sku=BAK-002&limit=2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(tools.execute("get_inventory", Map.of("sku", "ELE-002")).content()).contains("\"onHand\":3");
        PosApiTools.Result missing = tools.execute("get_transaction", Map.of("transaction_id", "TX-404"));
        assertThat(missing.isError()).isTrue();
        assertThat(missing.content()).contains("404", "TRANSACTION_NOT_FOUND");
        assertThat(tools.execute("get_recommendations", Map.of("skus", List.of("COF-001", "BAK-002"), "limit", 2)).isError()).isFalse();
        assertThat(tools.execute("get_inventory", Map.of()).isError()).isTrue();
        assertThat(tools.execute("drop_tables", Map.of()).isError()).isTrue();
        server.verify();
    }
}
