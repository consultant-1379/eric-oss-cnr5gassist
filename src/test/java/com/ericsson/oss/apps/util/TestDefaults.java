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

package com.ericsson.oss.apps.util;

import com.ericsson.oss.apps.api.model.*;
import com.ericsson.oss.apps.client.cts.model.*;
import com.ericsson.oss.apps.model.*;
import com.ericsson.oss.apps.model.ncmp.*;
import lombok.experimental.UtilityClass;

import java.util.*;

import static com.ericsson.oss.apps.util.Constants.*;
import static org.springframework.data.jpa.domain.AbstractPersistable_.ID;

@UtilityClass
public class TestDefaults {
    public static final String LOGIN_ENDPOINT = "/auth/v1/login";
    public static final String NRC_CONTEXT_PATH = "/api/v1/nrc/";
    public static final String START_ENDPOINT = NRC_CONTEXT_PATH + "startNrc";
    public static final String MONITORING_ENDPOINT = NRC_CONTEXT_PATH + "monitoring";
    public static final String MONITORING_ENDPOINT_BY_ID = MONITORING_ENDPOINT + "/{id}";
    public static final String MOCK_UUID_VALUE = "123e4567-e89b-12d3-a456-556642440000";
    public static final String NCMP_OBJECT_ID_NR45 = "NR45gNodeBRadio00022";
    public static final UUID MOCK_UUID = UUID.fromString(MOCK_UUID_VALUE);
    public static final String MOCK_UUID_VALUE_2 = "123e4567-e89b-12d3-a456-556642440001";
    public static final UUID MOCK_UUID_2 = UUID.fromString(MOCK_UUID_VALUE_2);
    public static final String TEXT = "text";
    public static final long SEED = 7734;
    public static final long ENODEB_ID = 10001L;
    public static final long ENODEB_ID_3 = 10003L;
    public static final long ENODEB_ID_5 = 10005L;
    public static final long LTE_CELL_ID = 12L;
    public static final long GEOGRAPHIC_SITE_ID = 3L;
    public static final Map<String, Set<Integer>> FREQUENCY_PAIR_MAP = Collections.singletonMap("44", Set.of(1, 2));

    public static final String UNIQUE_APP_ID_VALUE = "app_name";
    public static final String INSTANCE_ID_VALUE = "instance_name";

    public static final NrcRequest NRC_REQUEST = NrcRequest.builder()
        .eNodeBIds(Collections.singleton(ENODEB_ID))
        .distance(42)
        .freqPairs(FREQUENCY_PAIR_MAP)
        .build();

    public static final NrcRequest NRC_REQUEST_NO_FREQ_PAIRS = NrcRequest.builder()
        .eNodeBIds(Collections.singleton(ENODEB_ID))
        .distance(42)
        .build();

    public static final NrcRequest NRC_REQUEST_MULTIPLE_ENODEBS = NrcRequest.builder()
        .eNodeBIds(new HashSet<>(Arrays.asList(ENODEB_ID_3, ENODEB_ID_5)))
        .distance(50)
        .build();

    public static final NrcRequest NRC_REQUEST_WILL_BE_REJECTED = NrcRequest.builder()
        .eNodeBIds(Collections.singleton(0L))
        .distance(42)
        .freqPairs(FREQUENCY_PAIR_MAP)
        .build();

    public static final NrcNeighbor NRC_NEIGHBOR = NrcNeighbor.builder()
        .eNodeBId(ENODEB_ID)
        .gNodeBDUs(Collections.singletonList(NrcGroupingGnbdu.builder()
            .gNodeBDUId(ENODEB_ID_3)
            .nrCellIds(Collections.singletonList(ENODEB_ID_5))
            .build()))
        .build();

    public static final NrcProcess NRC_PROCESS_SUCCESS = NrcProcess.builder()
        .minute(12)
        .hour(12)
        .id(TestDefaults.MOCK_UUID)
        .nrcStatus(NrcProcessStatus.SUCCEEDED)
        .build();


    public static final NrcProcess NRC_PROCESS_FAILED = NrcProcess.builder()
        .minute(12)
        .hour(12)
        .id(TestDefaults.MOCK_UUID)
        .nrcStatus(NrcProcessStatus.FAILED)
        .build();

    public static final NrcProcess NRC_PROCESS_PENDING = NrcProcess.builder()
        .minute(12)
        .hour(12)
        .id(TestDefaults.MOCK_UUID)
        .nrcStatus(NrcProcessStatus.PENDING)
        .build();

    public static final NrcProcess NRC_PROCESS_ONGOING = NrcProcess.builder()
        .minute(12)
        .hour(12)
        .id(TestDefaults.MOCK_UUID)
        .nrcStatus(NrcProcessStatus.ONGOING)
        .build();

    public static final NrcData NRC_DATA_SUCCESS = NrcData.builder()
        .nrcTask(NrcTask.builder()
            .request(NRC_REQUEST)
            .process(NRC_PROCESS_SUCCESS)
            .allNrcNeighbors(List.of(NRC_NEIGHBOR))
            .build())
        .isQueried(false)
        .build();

    public static final NrcTask NRC_TASK_SUCCESS = NrcTask.builder()
        .request(NRC_REQUEST)
        .process(NRC_PROCESS_SUCCESS)
        .allNrcNeighbors(List.of(NRC_NEIGHBOR))
        .build();

    public static final NrcTask NRC_TASK_FAILED = NrcTask.builder()
        .request(NRC_REQUEST)
        .process(NRC_PROCESS_FAILED)
        .build();

    public static final NrcTask NRC_TASK_ONGOING = NrcTask.builder()
        .request(NRC_REQUEST)
        .process(NRC_PROCESS_ONGOING)
        .build();

    public static final NrcTask NRC_TASK_ONGOING_2 = NrcTask.builder()
        .request(NRC_REQUEST_MULTIPLE_ENODEBS)
        .process(NRC_PROCESS_ONGOING)
        .build();

    public static final NrcTask NRC_TASK_PENDING = NrcTask.builder()
        .request(NRC_REQUEST)
        .process(NRC_PROCESS_PENDING)
        .build();

    public static final Map<String, String> GEO_DATA =
        new LinkedHashMap<>() {{
            put(GEO_QUERY_FILTER, "{\"center\":{\"type\":\"Point\",\"coordinates\":[34.3,-54.5]},\"distance\":200}");
            put(GEO_TYPE, "'GeospatialCoords'");
        }};

    public static final Map<String, String> GEO_DATA_OTHER =
        new LinkedHashMap<>() {{
            put(GEO_QUERY_FILTER, "{\"center\":{\"type\":\"Point\",\"coordinates\":[34.3,-54.5]},\"distance\":100}");
            put(GEO_TYPE, "'GeospatialCoords'");
        }};

    public static final Map<String, String> GEO_DATA_WITH_FREQ =
        new LinkedHashMap<>(GEO_DATA) {{
            put(GEO_FREQUENCY, "[1564I]");
        }};

    public static final GeoPoint GEO_POINT = GeoPoint.builder().longitude(34.3F).latitude(-54.5F).build();
    public static final GeoQueryObject GEO_QUERY_OBJECT = GeoQueryObject.builder().distance(200).geoPoint(GEO_POINT).build();
    public static final GeoQueryObject GEO_QUERY_OBJECT_OTHER = GeoQueryObject.builder().distance(100).geoPoint(GEO_POINT).build();

    //Regexs
    public static final String matcherRegex = "(downlinkEARFCN\\.in=\\[[0-9]+I(,[0-9]+I)*]&)*" +
        "geographicSite.locatedAt.geospatialData.geoDistanceWithin=" +
        "\\{\"center\":\\{\"type\":\"Point\",\"coordinates\":\\[-?[0-9.0-9]+,-?[0-9.0-9]+(,[0-9.0-9]+)?]},\"distance\":[0-9]+}&" +
        "geographicSite.locatedAt.type.eq='GeospatialCoords'";

    public static final String CM_HANDLE = "cmHandle";
    public static final String MANAGED_ELEMENT_RESOURCE_IDENTIFIER = "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022";
    public static final String SCTP_ENDPOINT_RESOURCE_PATH = "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmcomtop:Transport=1/erienmnrmrtnsctp:SctpEndpoint=NRAT";

    public static final String DN_PREFIX_STRING = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR45gNodeBRadio00022";
    public static final Fdn DN_PREFIX = Fdn.of(DN_PREFIX_STRING);
    public static final Fdn PARTIAL_IPV4_FDN = Fdn.of("ManagedElement=NR45gNodeBRadio00022,Transport=1,Router=VR_INNER,InterfaceIPv4=NRAT_CP,AddressIPv4=1");
    public static final Fdn IPV4_FDN = DN_PREFIX.addAll(PARTIAL_IPV4_FDN);
    public static final Fdn IPV6_FDN = Fdn.of(DN_PREFIX_STRING + ",ManagedElement=NR45gNodeBRadio00022,Transport=1,Router=VR_INNER,InterfaceIPv6=NRAT_CP,AddressIPv6=1");
    public static final Fdn LOCAL_SCTP_ENDPOINT_REF = Fdn.of(DN_PREFIX_STRING + ",ManagedElement=NR45gNodeBRadio00022,Transport=1,SctpEndpoint=NRAT");
    public static final Fdn LOCAL_SCTP_ENDPOINT_REF_1 = Fdn.of(DN_PREFIX_STRING + ",ManagedElement=NR45gNodeBRadio00022,Transport=1,SctpEndpoint=1");
    public static final Fdn LOCAL_SCTP_ENDPOINT_REF_2 = Fdn.of(DN_PREFIX_STRING + ",ManagedElement=NR45gNodeBRadio00022,Transport=2,SctpEndpoint=2");
    public static final Fdn ENDPOINT_RESOURCE_REF_1 = Fdn.of(DN_PREFIX_STRING + ",ManagedElement=NR45gNodeBRadio00022,GNBCUCPFunction=1,EndpointResource=PLMN1");
    public static final Fdn ENDPOINT_RESOURCE_REF_2 = Fdn.of(DN_PREFIX_STRING + ",ManagedElement=NR45gNodeBRadio00022,GNBCUCPFunction=1,EndpointResource=PLMN2");
    public static final Fdn G_UTRAN_SYNC_SIGNAL_FREQUENCY_REF = Fdn.of(DN_PREFIX_STRING + ",ManagedElement=NR45gNodeBRadio00022,ENodeBFunction=1,GUtraNetwork=1,GUtranSyncSignalFrequency=0");

    public static final ExternalId MANAGED_ELEMENT_EXTERNAL_ID = ExternalId.of(CM_HANDLE,
        MANAGED_ELEMENT_RESOURCE_IDENTIFIER);
    public static final ExternalId SCTP_ENDPOINT_EXTERNAL_ID = ExternalId.of(CM_HANDLE, SCTP_ENDPOINT_RESOURCE_PATH);
    public static final ExternalId GUTRA_NETWORK_EXTERNAL_ID = ExternalId.of(CM_HANDLE,
        "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmlrat:ENodeBFunction=1/erienmnrmlrat:GUtraNetwork=1");
    public static final ExternalId EXTERNAL_GNODEB_FUNCTION_EXTERNAL_ID = ExternalId.of(CM_HANDLE, "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmlrat:ENodeBFunction=1/erienmnrmlrat:GUtraNetwork=1/erienmnrmlrat:ExternalGNodeBFunction=NR45gNodeBRadio00022");
    public static final ExternalId TERM_POINT_EXTERNAL_ID = ExternalId.of(CM_HANDLE, "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmlrat:ENodeBFunction=1/erienmnrmlrat:GUtraNetwork=1/erienmnrmlrat:ExternalGNodeBFunction=NR45gNodeBRadio00022/erienmnrmlrat:TermPointToGNB=1");
    public static final ExternalId EXTERNAL_CELL_EXTERNAL_ID = ExternalId.of(CM_HANDLE, "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmlrat:ENodeBFunction=1/erienmnrmlrat:GUtraNetwork=1/erienmnrmlrat:ExternalGNodeBFunction=NR45gNodeBRadio00022/erienmnrmlrat:ExternalGUtranCell=111");
    public static final ExternalId ENODEB_EXTERNAL_ID = ExternalId.of(CM_HANDLE, "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmlrat:ENodeBFunction=1");
    public static final ExternalId GNODEB_MANAGED_ELEMENT_EXTERNAL_ID = ExternalId.of(CM_HANDLE, "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022");
    public static final ExternalId ENDPOINT_RESOURCE_EXTERNAL_ID = ExternalId.of(CM_HANDLE, "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmgnbcucp:GNBCUCPFunction=1/erienmnrmgnbcucp:EndpointResource=PLMN1");
    public static final String EXTERNAL_GNODEB_FUNCTION_ID = "NR45gNodeBRadio00022";
    public static final String TERM_POINT_TO_GNB_ID = "1";
    public static final String EXTERNAL_GUTRAN_CELL_ID = "111";
    public static final ExternalId IPV4_EXTERNAL_ID = ExternalId.of(CM_HANDLE,
        "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmcomtop:Transport=1/erienmnrmrtnl3router:Router" +
            "=VR_INNER/erienmnrmrtnl3interfaceipv4:InterfaceIPv4=NRAT_CP/erienmnrmrtnl3interfaceipv4:AddressIPv4=1");
    public static final ExternalId IPV6_EXTERNAL_ID = ExternalId.of(CM_HANDLE,
        "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmcomtop:Transport=1/erienmnrmrtnl3router:Router" +
            "=VR_INNER/erienmnrmrtnl3interfaceipv6:InterfaceIPv6=NRAT_CP/erienmnrmrtnl3interfaceipv6:AddressIPv6=1");
    public static final ExternalId NR_SECTOR_CARRIER_EXTERNAL_ID = ExternalId.of(CM_HANDLE,
        "/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/ericsson-enm-GNBDU:GNBDUFunction=1/ericsson-enm-GNBDU" +
            ":NRSectorCarrier=1");
    public static final ExternalId NR_CELL_EXT_ID = ExternalId.of("F369126BCA1921B4B12C5E211258F807/ericsson-enm-ComTop:ManagedElement=NR45gNodeBRadio00022/ericsson-enm-GNBDU:GNBDUFunction=1/ericsson-enm-GNBDU:NRCellDU=111");

    public static final ExternalId ADDITIONAL_PLMN_INFO = ExternalId.of("F369126BCA1921B4B12C5E211258F807/ericsson-enm-ComTop:ManagedElement=NR45gNodeBRadio00022/ericsson-enm-GNBDU:GNBDUFunction=1/ericsson-enm-GNBDU:NRCellDU=111/ericsson-enm-GNBDU:AdditionalPLMNInfo=1");

    public static final long GNODEB_ID = 4193960001L, LOCAL_CELL_ID_NCI = 25L;
    public static final int MCC = 216, MNC = 30, TRACKING_AREA_CODE = 10496, PHYSICAL_CELL_IDENTITY = 138;
    public static final ENodeB E_NODE_B = ENodeB.builder().id(ENODEB_ID)
        .externalId("cmHandle/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/erienmnrmlrat:ENodeBFunction=1")
        .build();
    public static final WirelessNetwork WIRELESS_NETWORK = WirelessNetwork.builder().mcc(MCC).mnc(MNC).build();
    public static final Gnbdu GNBDU = Gnbdu.builder().id(11L).gnbduId(GNODEB_ID)
        .externalId("cmHandle/erienmnrmcomtop:ManagedElement=NR45gNodeBRadio00022/gnbdufunction:GNBDUFunction=GNodeBId")
        .wirelessNetworks(List.of(Association.builder().mode(LOADED).value(WIRELESS_NETWORK).build())).build();
    public static final Gnbdu GNBDU_NO_WIRELESS_NETWORKS = Gnbdu.builder().id(11L).gnbduId(GNODEB_ID)
        .externalId(GNBDU.getExternalId()).build();

    public static final NrSectorCarrier NR_SECTOR_CARRIER_1 = NrSectorCarrier.builder()
                .id(1L)
                .externalId(NR_SECTOR_CARRIER_EXTERNAL_ID.toString())
                .arfcnDL(1)
                .build();
    public static final NrCell NR_CELL_111 = NrCell.builder()
        .id(111L)
        .localCellIdNci(LOCAL_CELL_ID_NCI)
        .externalId(NR_CELL_EXT_ID.toString())
        .nrSectorCarriers(List.of(Association.builder()
            .mode(LOADED)
            .value(NR_SECTOR_CARRIER_1)
            .build()))
        .trackingAreaCode(TRACKING_AREA_CODE).physicalCellIdentity(PHYSICAL_CELL_IDENTITY).build();

    public static final NrCell NR_CELL_222 = NrCell.builder().id(222L).build();
    public static final LteCell LTE_CELL = LteCell.builder().id(LTE_CELL_ID).build();
    public static final GeographicSite GEOGRAPHIC_SITE = GeographicSite.builder().id(GEOGRAPHIC_SITE_ID).build();

    public static final PlmnId PLMNID = PlmnId.builder().mcc(MCC).mnc(MNC).mncLength(2).build();
    public static final PlmnId PLMNID_1 = PlmnId.builder().mcc(240).mnc(80).mncLength(2).build();
    public static final PlmnId PLMNID_2 = PlmnId.builder().mcc(310).mnc(14).mncLength(2).build();

    public static final ExternalGNodeBFunction EXTERNAL_G_NODE_B_FUNCTION_ATTRIBUTES = ExternalGNodeBFunction.builder()
        .externalGNodeBFunctionId(EXTERNAL_GNODEB_FUNCTION_ID)
        .gNodeBPlmnId(PLMNID)
        .gNodeBId(GNODEB_ID)
        .gNodeBIdLength(32)
        .userLabel(NCMP_OBJECT_ID_NR45)
        .build();

    public static final NcmpObject<NrCellCU> NR_CELL_CU_PSCAPABLE_FALSE =
        NcmpObject.<NrCellCU>builder().id(ID).attributes(
            NrCellCU.builder().nRCellCUId(ID).pSCellCapable(false).build()).build();

    public static final IpAddress IPv4_ADDRESS = IpAddress.builder().address("10.55.168.22/29").build();
    public static final String STRIPPED_IPv4_ADDRESS = "10.55.168.22";

    public static final SctpEndpoint SCTP_ENDPOINT = SctpEndpoint.builder().localIpAddress(
        List.of(IPV4_FDN, IPV6_FDN)).build();
    public static final LocalSctpEndpoint LOCAL_SCTP_ENDPOINT_X2 = LocalSctpEndpoint.builder()
        .sctpEndpointRef(LOCAL_SCTP_ENDPOINT_REF)
        .interfaceUsed("X2").build();
    public static final LocalSctpEndpoint LOCAL_SCTP_ENDPOINT_1_X2 = LocalSctpEndpoint.builder()
        .sctpEndpointRef(LOCAL_SCTP_ENDPOINT_REF_1)
        .interfaceUsed("X2").build();
    public static final LocalSctpEndpoint LOCAL_SCTP_ENDPOINT_2_X2 = LocalSctpEndpoint.builder()
        .sctpEndpointRef(LOCAL_SCTP_ENDPOINT_REF_2)
        .interfaceUsed("X2").build();

    public static final ResourcePartitionMember RESOURCE_PARTITION_MEMBER_1 = ResourcePartitionMember.builder()
        .endpointResourceRef(ENDPOINT_RESOURCE_REF_1)
        .pLMNIdList(Collections.singletonList(PLMNID_1)).build();
    public static final ResourcePartitionMember RESOURCE_PARTITION_MEMBER_2 = ResourcePartitionMember.builder()
        .endpointResourceRef(ENDPOINT_RESOURCE_REF_2)
        .pLMNIdList(Collections.singletonList(PLMNID_2)).build();

    public static final ManagedElement MANAGED_ELEMENT = ManagedElement.builder().dnPrefix(DN_PREFIX).build();
    public static final GUtranSyncSignalFrequency G_UTRAN_SYNC_SIGNAL_FREQUENCY = GUtranSyncSignalFrequency.builder()
        .arfcn(1111).build();

    public static final GUtraNetwork G_UTRA_NETWORK = GUtraNetwork.builder().gUtraNetworkId("1").userLabel("1").build();

    public static final ExternalGUtranCell EXTERNAL_G_UTRAN_CELL = ExternalGUtranCell.builder()
        .externalGUtranCellId(EXTERNAL_GUTRAN_CELL_ID)
        .gUtranSyncSignalFrequencyRef(G_UTRAN_SYNC_SIGNAL_FREQUENCY_REF)
        .localCellId((int) LOCAL_CELL_ID_NCI)
        .physicalLayerCellIdGroup(46)
        .physicalLayerSubCellId(0)
        .plmnIdList(Collections.singletonList(PLMNID))
        .isRemoveAllowed(TRUE)
        .nRTAC(Integer.valueOf(TRACKING_AREA_CODE).toString())
        .build();

    public static final GnbduFunction GNBDU_FUNCTION = GnbduFunction.builder()
        .gNBId(4193960001L)
        .gNBIdLength(32).build();
    public static final GnbduFunction GNBDU_FUNCTION_PLMNID_1 = GnbduFunction.builder()
        .gNBId(4193960001L)
        .gNBIdLength(32)
        .dUpLMNId(PLMNID_1)
        .build();

    public static final TermPointToGNB TERM_POINT_TO_GNB = TermPointToGNB.builder().termPointToGNBId(TERM_POINT_TO_GNB_ID).ipAddress(STRIPPED_IPv4_ADDRESS).build();

    public static final EnmUpdateContext ENM_UPDATE_CONTEXT = new EnmUpdateContext(NRC_TASK_ONGOING, E_NODE_B, GNBDU);
    public static final EnmUpdateContext ENM_UPDATE_CONTEXT_2 = new EnmUpdateContext(NRC_TASK_ONGOING_2, E_NODE_B, GNBDU);

    public static <T extends NcmpAttribute> NcmpObject<T> toNcmpObject(T attribute) {
        return toNcmpObject("0", attribute);
    }

    public static <T extends NcmpAttribute> NcmpObject<T> toNcmpObject(String id, T attribute) {
        NcmpObject.NcmpObjectBuilder<T> builder = NcmpObject.builder();
        return builder.id(id).attributes(attribute).build();
    }
}
