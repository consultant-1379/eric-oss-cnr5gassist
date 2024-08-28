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
package com.ericsson.oss.apps.service.ncmp.handlers;

import com.ericsson.oss.apps.model.ncmp.AdditionalPLMNInfo;
import com.ericsson.oss.apps.model.ncmp.NcmpObject;
import com.ericsson.oss.apps.model.ncmp.NrCellCU;
import com.ericsson.oss.apps.model.ncmp.PlmnId;
import javassist.tools.rmi.ObjectNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.ericsson.oss.apps.util.TestDefaults.NR_CELL_111;
import static com.ericsson.oss.apps.util.TestDefaults.NR_CELL_EXT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class PlmnIdListpGroupingTest {

    @Mock
    private NrCellCUReader nrCellCUReader;
    @Mock
    private AdditionalPLMNInfoReader additionalPLMNInfoReader;
    @InjectMocks
    private PlmnIdListGrouping plmnIdListGrouping;

    private static final List<PlmnId> list = List.of(PlmnId.builder().mcc(33).mnc(444).build()
            , PlmnId.builder().mcc(77).mnc(888).build()
            ,PlmnId.builder().mcc(33).mnc(444).build()
            , PlmnId.builder().mcc(77).mnc(888).build());

    private static final Optional<NcmpObject<NrCellCU>> NR_CELL_CU_OBJECT = Optional.of(NcmpObject.<NrCellCU>builder()
            .id("111")
            .attributes(NrCellCU.builder()
                .pLMNIdList(null)
                .build())
            .build());

    private static final List<NcmpObject<AdditionalPLMNInfo>> ADDITIONAL_PLMN_ID_OBJECT_LIST = List.of(NcmpObject.<AdditionalPLMNInfo>builder()
            .id("1")
            .attributes(AdditionalPLMNInfo.builder()
                .pLMNIdList(null)
                .build())
            .build());

    @Test
    public void getUniquePLMNIdList() throws ObjectNotFoundException {
        NR_CELL_CU_OBJECT.get().getAttributes().setPLMNIdList(list.subList(0,1));
        Mockito.when(nrCellCUReader.read(eq(NR_CELL_EXT_ID.getRoot()), any()))
            .thenReturn(NR_CELL_CU_OBJECT);

        ADDITIONAL_PLMN_ID_OBJECT_LIST.get(0).getAttributes().setPLMNIdList(list);
        Mockito.when(additionalPLMNInfoReader.read(any()))
            .thenReturn(ADDITIONAL_PLMN_ID_OBJECT_LIST);

        Assertions.assertEquals(list.subList(0,2), plmnIdListGrouping.getPlmnIdList(NR_CELL_111));
    }

    @Test
    public void getUniquePLMNIdListWithNullPLMNIdListFromNrCellCU() throws ObjectNotFoundException {
        NR_CELL_CU_OBJECT.get().getAttributes().setPLMNIdList(null);
        Mockito.when(nrCellCUReader.read(eq(NR_CELL_EXT_ID.getRoot()), any()))
            .thenReturn(NR_CELL_CU_OBJECT);

        ADDITIONAL_PLMN_ID_OBJECT_LIST.get(0).getAttributes().setPLMNIdList(list);
        Mockito.when(additionalPLMNInfoReader.read(any()))
            .thenReturn(ADDITIONAL_PLMN_ID_OBJECT_LIST);

        Assertions.assertEquals(list.subList(0,2), plmnIdListGrouping.getPlmnIdList(NR_CELL_111));
    }

    @Test
    public void getUniquePLMNIdListWithNullPLMNIdListFromAdditionalPlmnId() throws ObjectNotFoundException {
        NR_CELL_CU_OBJECT.get().getAttributes().setPLMNIdList(list.subList(1,2));
        Mockito.when(nrCellCUReader.read(eq(NR_CELL_EXT_ID.getRoot()), any()))
            .thenReturn(NR_CELL_CU_OBJECT);

        ADDITIONAL_PLMN_ID_OBJECT_LIST.get(0).getAttributes().setPLMNIdList(null);
        Mockito.when(additionalPLMNInfoReader.read(any()))
            .thenReturn(ADDITIONAL_PLMN_ID_OBJECT_LIST);

        Assertions.assertEquals(list.subList(1,2)
            , plmnIdListGrouping.getPlmnIdList(NR_CELL_111));
    }

    @Test
    public void getUniquePLMNIdListWithNullAdditionalPlmnId() throws ObjectNotFoundException {
        NR_CELL_CU_OBJECT.get().getAttributes().setPLMNIdList(list.subList(1,2));
        Mockito.when(nrCellCUReader.read(eq(NR_CELL_EXT_ID.getRoot()), any()))
            .thenReturn(NR_CELL_CU_OBJECT);

        Mockito.when(additionalPLMNInfoReader.read(any()))
            .thenReturn(null);

        Assertions.assertEquals(list.subList(1,2)
            , plmnIdListGrouping.getPlmnIdList(NR_CELL_111));
    }
}
