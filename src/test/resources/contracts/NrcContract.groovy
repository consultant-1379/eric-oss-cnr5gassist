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

package contracts

import org.springframework.cloud.contract.spec.Contract

[
    Contract.make {
        name("startnrc 200 ok")
        request {
            method POST()
            urlPath("/api/v1/nrc/startNrc")
            headers {
                contentType(applicationJson())
            }
            body([
                "eNodeBIds": [10001],
                "distance" : 42,
                "freqPairs": ["44": [1, 2]]
            ])
        }
        response {
            status OK()
            headers {
                contentType(applicationJson())
            }
            body("\"123e4567-e89b-12d3-a456-556642440000\"")
        }
    },

    Contract.make {
        name("startnrc 208 already reported")
        request {
            method POST()
            urlPath("/api/v1/nrc/startNrc")
            headers {
                contentType(applicationJson())
            }
            body([
                "eNodeBIds": [10003, 10005],
                "distance" : 50
            ])
        }
        response {
            status ALREADY_REPORTED()
            headers {
                contentType(applicationJson())
            }
            body("\"123e4567-e89b-12d3-a456-556642440001\"")
        }
    },

    Contract.make {
        name("startnrc 400 bad json body")
        request {
            method POST()
            urlPath("/api/v1/nrc/startNrc")
            headers {
                contentType(applicationJson())
            }
            body([
                "eNodeBIds": null,
                "distance" : 42,
                "freqPairs": ["44": [1, 2]]
            ])
        }
        response {
            status BAD_REQUEST()
        }
    },

    Contract.make {
        name("startnrc 503 overloaded")
        request {
            method POST()
            urlPath("/api/v1/nrc/startNrc")
            headers {
                contentType(applicationJson())
            }
            body([
                "eNodeBIds": [0],
                "distance" : 42,
                "freqPairs": ["44": [1, 2]]
            ])
        }
        response {
            status SERVICE_UNAVAILABLE()
            headers {
                contentType(applicationJson())
            }
            body([[ message : 'TASK QUEUE IS FULL']])
        }
    },

    Contract.make {
        name("monitoring 200 ok")
        request {
            method GET()
            urlPath("/api/v1/nrc/monitoring")
        }
        response {
            status OK()
            headers {
                contentType(applicationJson())
            }
            body([[
                      "id"    : "123e4567-e89b-12d3-a456-556642440000",
                      "nrcStatus": "Succeeded",
                      "hour"  : 12,
                      "minute": 12
                  ]])
        }
    },

    Contract.make {
        name("monitoring by id 200 ok")
        request {
            method GET()
            urlPath("/api/v1/nrc/monitoring/123e4567-e89b-12d3-a456-556642440000")
        }
        response {
            status OK()
            headers {
                contentType(applicationJson())
            }
            body([
                "request" : [
                    "eNodeBIds": [10001],
                    "distance" : 42,
                    "freqPairs": ["44": [1, 2]]
                ],
                "process" : [
                    "id"    : "123e4567-e89b-12d3-a456-556642440000",
                    "nrcStatus": "Succeeded",
                    "hour"  : 12,
                    "minute": 12
                ],
                "allNrcNeighbors" : [
                    [
                        "eNodeBId" : 10001,
                        "gNodeBDUs": [
                            [
                                "gNodeBDUId": 10003,
                                "nrCellIds" : [10005]
                            ]
                        ]
                    ]
                ]
            ])
        }
    },

    Contract.make {
        name("monitoring 400 malformed id")
        request {
            method GET()
            urlPath("/api/v1/nrc/monitoring/123e4567-e89b-12d3-a456-55WRONG!!!!40000")
        }
        response {
            status BAD_REQUEST()
        }
    },

    Contract.make {
        name("monitoring 404 not found")
        request {
            method GET()
            urlPath("/api/v1/nrc/monitoring/123e4567-e89b-12d3-a456-556642440001")
        }
        response {
            status NOT_FOUND()
        }
    }
]