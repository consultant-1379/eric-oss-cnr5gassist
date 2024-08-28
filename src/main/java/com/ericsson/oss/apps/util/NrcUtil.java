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

import com.ericsson.oss.apps.api.model.NrcProcess;
import com.ericsson.oss.apps.api.model.NrcProcessStatus;
import com.ericsson.oss.apps.api.model.NrcRequest;
import com.ericsson.oss.apps.api.model.NrcTask;
import com.ericsson.oss.apps.model.ExternalId;
import lombok.experimental.UtilityClass;

import java.time.LocalTime;
import java.util.Collections;
import java.util.UUID;

import static com.ericsson.oss.apps.util.Constants.*;

@UtilityClass
public class NrcUtil {

    public static NrcTask generateNrcTask(NrcRequest nrcRequest) {
        LocalTime now = LocalTime.now();
        return NrcTask.builder()
            .request(nrcRequest)
            .process(NrcProcess.builder()
                .id(UUID.randomUUID())
                .nrcStatus(NrcProcessStatus.PENDING)
                .hour(now.getHour())
                .minute(now.getMinute())
                .build())
            .allNrcNeighbors(Collections.emptyList())
            .build();
    }

    public static String externalIdKey(ExternalId externalId) {
        return EXTERNAL_ID + EQUAL + SQUARE_BRACKET_OPEN + externalId.toString() + SQUARE_BRACKET_CLOSE;
    }
}
