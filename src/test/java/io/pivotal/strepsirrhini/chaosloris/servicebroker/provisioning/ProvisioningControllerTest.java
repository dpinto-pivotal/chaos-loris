/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal.strepsirrhini.chaosloris.servicebroker.provisioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.pivotal.strepsirrhini.chaosloris.model.Instance;
import io.pivotal.strepsirrhini.chaosloris.model.InstanceRepository;
import io.pivotal.strepsirrhini.chaosloris.servicebroker.AbstractControllerTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.pivotal.strepsirrhini.chaosloris.TestIds.ALTERNATE_ID;
import static io.pivotal.strepsirrhini.chaosloris.TestIds.INSTANCE_ID;
import static io.pivotal.strepsirrhini.chaosloris.TestIds.ORGANIZATION_ID;
import static io.pivotal.strepsirrhini.chaosloris.TestIds.PLAN_ID;
import static io.pivotal.strepsirrhini.chaosloris.TestIds.SERVICE_ID;
import static io.pivotal.strepsirrhini.chaosloris.TestIds.SPACE_ID;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ProvisioningControllerTest extends AbstractControllerTest {

    @Autowired
    private volatile InstanceRepository instanceRepository;

    @Test
    public void create() throws Exception {
        this.mockMvc.perform(
                put("/v2/service_instances/" + INSTANCE_ID)
                        .content(createPayload())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dashboard_url")
                        .value("https://10.128.10.4/dashboard/" + INSTANCE_ID));

        assertEquals(1, this.instanceRepository.count());
    }

    @Test
    public void createAlreadyExistsNoConflict() throws Exception {
        this.instanceRepository.saveAndFlush(new Instance(INSTANCE_ID, ORGANIZATION_ID, Collections.emptyMap(),
                SPACE_ID));

        this.mockMvc.perform(
                put("/v2/service_instances/" + INSTANCE_ID)
                        .content(createPayload())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboard_url")
                        .value("https://10.128.10.4/dashboard/" + INSTANCE_ID));

        assertEquals(1, this.instanceRepository.count());
    }

    @Test
    public void createAlreadyExistsConflict() throws Exception {
        this.instanceRepository.saveAndFlush(new Instance(INSTANCE_ID, ORGANIZATION_ID, Collections.emptyMap(),
                SPACE_ID));

        this.mockMvc.perform(
                put("/v2/service_instances/" + INSTANCE_ID)
                        .content(createAlternatePayload())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.*", hasSize(0)));
    }

    @Test
    public void testUpdate() throws Exception {
        this.mockMvc.perform(
                patch("/v2/service_instances/" + INSTANCE_ID)
                        .content(updatePayload())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(0)));
    }

    @Test
    public void testDelete() throws Exception {
        this.instanceRepository.saveAndFlush(new Instance(INSTANCE_ID, ORGANIZATION_ID, Collections.emptyMap(),
                SPACE_ID));

        this.mockMvc.perform(
                delete("/v2/service_instances/" + INSTANCE_ID)
                        .param("service_id", SERVICE_ID.toString())
                        .param("plan_id", PLAN_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(0)));

        assertEquals(0, this.instanceRepository.count());
    }

    @Test
    public void testDeleteDoesNotExist() throws Exception {
        this.mockMvc.perform(
                delete("/v2/service_instances/" + INSTANCE_ID)
                        .param("service_id", SERVICE_ID.toString())
                        .param("plan_id", PLAN_ID.toString()))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.*", hasSize(0)));
    }

    private String createAlternatePayload() throws JsonProcessingException {
        Map<String, Object> m = new HashMap<>();
        m.put("organization_guid", ORGANIZATION_ID);
        m.put("plan_id", PLAN_ID);
        m.put("service_id", SERVICE_ID);
        m.put("space_guid", ALTERNATE_ID);

        return this.objectMapper.writeValueAsString(m);
    }

    private String createPayload() throws JsonProcessingException {
        Map<String, Object> m = new HashMap<>();
        m.put("organization_guid", ORGANIZATION_ID);
        m.put("plan_id", PLAN_ID);
        m.put("service_id", SERVICE_ID);
        m.put("space_guid", SPACE_ID);

        return this.objectMapper.writeValueAsString(m);
    }

    private String updatePayload() throws JsonProcessingException {
        Map<String, ?> m = Collections.singletonMap("service_id", SERVICE_ID);

        return this.objectMapper.writeValueAsString(m);
    }

}