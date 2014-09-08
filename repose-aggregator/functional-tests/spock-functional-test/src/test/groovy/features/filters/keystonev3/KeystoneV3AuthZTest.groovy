package features.filters.keystonev3

import framework.ReposeValveTest
import framework.mocks.MockKeystoneV3Service
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Ignore
import spock.lang.Unroll

/**
 * Created by jennyvo on 9/4/14.
 */
class KeystoneV3AuthZTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint
    //def static targetPort

    static MockKeystoneV3Service fakeKeystoneV3Service

    def setupSpec() {
        deproxy = new Deproxy()

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3", params)
        repose.configurationProvider.applyConfigs("features/filters/keystonev3/authz", params)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeKeystoneV3Service = new MockKeystoneV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeKeystoneV3Service.handler)
        //targetPort = properties.targetPort
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()

        if (repose)
            repose.stop()
    }

    @Unroll("service endpoint #endpointResponse status code #statusCode")
    def "When user is authorized should forward request to origin service"() {
        given:
        fakeKeystoneV3Service.with {
            endpointUrl = endpointResponse
        }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: "http://localhost:${properties.reposePort}/v3/${fakeKeystoneV3Service.client_token}/ss", method: 'GET', headers: ['X-Subject-Token': token])

        then: "User should receive a #statusCode response"
        mc.receivedResponse.code == statusCode
        mc.handlings.size() == handlings

        where:
        endpointResponse | statusCode | handlings | token
        "localhost"      | "200"      | 1         | "some-random-token"
        "myhost.com"     | "403"      | 0         | "some-other-token"
    }

    def "When user is not authorized should receive a 403 FORBIDDEN response"() {

        given: "IdentityService is configured with allowed endpoints that will differ from the user's requested endpoint"
        def token = UUID.randomUUID().toString()
        fakeKeystoneV3Service.with {
            client_token = token
            servicePort = 99999
        }

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: "http://localhost:${properties.reposePort}/v3/${token}/ss", method: 'GET', headers: ['X-Subject-Token': token])
        def foundLogs = reposeLogSearch.searchByString("User token: " + token +
                ": The user's service catalog does not contain an endpoint that matches the endpoint configured in keystone-v3.cfg.xml")

        then: "User should receive a 403 FORBIDDEN response"
        //foundLogs.size() == 1
        mc.receivedResponse.code == "403"
        mc.handlings.size() == 0
    }
}
