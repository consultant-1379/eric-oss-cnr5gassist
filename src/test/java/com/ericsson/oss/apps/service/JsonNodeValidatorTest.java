/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.service;

import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.NrCellCU;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest()
public class JsonNodeValidatorTest {

    @Autowired
    private JsonHelper jsonHelper;
    @Autowired
    private JsonNodeValidator jsonNodeValidator;

    private List<NcmpObject<NrCellCU>> createNcmpObjectList(String pLMNIdList) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("nRCellCUId", "1");
        attributes.put("cellLocalId", 1L);
        attributes.put("pSCellCapable", true);
        attributes.put("pLMNIdList", pLMNIdList);

        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("id", "1");
        objectMap.put("attributes", attributes);

        JsonNode jsonNode = jsonHelper.convertValue(Collections.singletonList(objectMap), JsonNode.class);

        TypeFactory typeFactory = jsonHelper.getTypeFactory();
        JavaType javaType = typeFactory.constructParametricType(NcmpObject.class, NrCellCU.class);
        CollectionType collectionType = typeFactory.constructCollectionType(List.class, javaType);

        JsonNode validatedJsonNode = jsonNodeValidator.validate(jsonNode, javaType);
        return jsonHelper.convertValue(validatedJsonNode, collectionType);
    }

    @Test
    public void nullPlmnIdListTest() {
        List<NcmpObject<NrCellCU>> ncmpObjectList = createNcmpObjectList("null");
        Assertions.assertNull(ncmpObjectList.get(0).getAttributes().getPLMNIdList());
    }

    @Test
    public void wrongPlmnIdListTest() {
        List<NcmpObject<NrCellCU>> ncmpObjectList = createNcmpObjectList("[{mcc=128, mncb=49 mccc=68]");
        Assertions.assertNull(ncmpObjectList.get(0).getAttributes().getPLMNIdList());
    }

    @Test
    public void emptyPlmnIdListTest() {
        List<NcmpObject<NrCellCU>> ncmpObjectList = createNcmpObjectList("");
        Assertions.assertNull(ncmpObjectList.get(0).getAttributes().getPLMNIdList());
    }

    @Test
    public void stringPlmnIdListTest() {
        List<NcmpObject<NrCellCU>> ncmpObjectList = createNcmpObjectList(
            "[{mcc=128, mnc=49}, {mcc=129, mnc=50}, {mcc=130, mnc=51}, {mcc=131, mnc=52}]");
        Assertions.assertEquals(4, ncmpObjectList.get(0).getAttributes().getPLMNIdList().size());
    }

    @Test
    public void wrongJsonString() throws JsonProcessingException {
        Assertions.assertThrows(JsonProcessingException.class, () -> jsonHelper.readValue("[{mcc=128, mncb=49 mccc=68]", JsonNode.class));
    }
}
