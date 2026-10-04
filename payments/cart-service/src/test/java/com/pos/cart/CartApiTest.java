package com.pos.cart;

import com.pos.cart.client.CatalogClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartApiTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CatalogClient catalog;

    @Test
    void basketLifecycle() throws Exception {
        when(catalog.exists(anyString())).thenReturn(true);
        when(catalog.exists(eq("NOPE"))).thenReturn(false);

        String body = mvc.perform(post("/api/carts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"store-001\",\"terminalId\":\"t1\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(post("/api/carts/{id}/items", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"COF-001\",\"quantity\":1}")).andExpect(status().isOk());
        mvc.perform(post("/api/carts/{id}/items", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"COF-001\",\"quantity\":2}"))
                .andExpect(jsonPath("$.items[0].quantity").value(3));
        mvc.perform(post("/api/carts/{id}/items", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"NOPE\",\"quantity\":1}")).andExpect(status().isConflict());

        mvc.perform(post("/api/carts/{id}/lock", id)).andExpect(jsonPath("$.status").value("LOCKED"));
        // a locked cart cannot be edited or locked again (prevents double checkout)
        mvc.perform(put("/api/carts/{id}/items/COF-001", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}")).andExpect(status().isConflict());
        mvc.perform(post("/api/carts/{id}/lock", id)).andExpect(status().isConflict());

        mvc.perform(post("/api/carts/{id}/complete", id)).andExpect(jsonPath("$.status").value("CHECKED_OUT"));
        mvc.perform(post("/api/carts/{id}/complete", id)).andExpect(status().isOk());
    }

    @Test
    void emptyCartCannotBeLocked() throws Exception {
        String body = mvc.perform(post("/api/carts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"store-001\",\"terminalId\":\"t1\"}"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        mvc.perform(post("/api/carts/{id}/lock", id)).andExpect(status().isConflict());
    }
}
