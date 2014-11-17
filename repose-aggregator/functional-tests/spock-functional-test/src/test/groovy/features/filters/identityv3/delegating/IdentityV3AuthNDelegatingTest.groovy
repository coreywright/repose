package features.filters.identityv3.delegating

import framework.ReposeValveTest
import framework.mocks.MockIdentityV3Service
import org.joda.time.DateTime
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

/**
 * Created by jennyvo on 11/17/14.
 * Identity V3 with Delegating option
 */
class IdentityV3AuthNDelegatingTest extends ReposeValveTest{

    def static originEndpoint
    def static identityEndpoint

    def static MockIdentityV3Service fakeIdentityV3Service

    def setupSpec() {

        deproxy = new Deproxy()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityV3Service = new MockIdentityV3Service(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityV3Service.handler)

        def params = properties.defaultTemplateParams
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3", params)
        repose.configurationProvider.applyConfigs("features/filters/identityv3/delegating", params)
        repose.start()
        waitUntilReadyToServiceRequests('200')
    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def setup(){
        sleep(500)
        fakeIdentityV3Service.resetHandlers()
    }

    @Unroll ("When #method req without credential")
    def "when send req without credential with delegating option repose forward req and failure msg to origin service"() {
        given:
        def delegatingmsg = "status_code=401.components=openstack-identity-v3.message=Failure in Auth Filter.;q=0.7"
        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/123456/",
                method: method,
                headers: ['content-type': 'application/json'])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "200"
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-authorization")
        mc.handlings[0].request.headers.getFirstValue("X-Identity-Status") == "Indeterminate"
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingmsg

        where:
        method << ["GET","POST","PUT","PATCH","DELETE"]
    }

    @Unroll("#authResponseCode, #responseCode")
    def "when send req with unauthorized user with forward-unauthorized-request true"() {
        fakeIdentityV3Service.with {
            client_token = UUID.randomUUID()
            tokenExpiresAt = (new DateTime()).plusDays(1);
            client_projectid = reqProject
            service_admin_role = "not-admin"
        }

        if(authResponseCode != 200){
            fakeIdentityV3Service.validateTokenHandler = {
                tokenId, request ->
                    new Response(authResponseCode, null, null, responseBody)
            }
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/$reqProject/",
                method: 'GET',
                headers: ['content-type': 'application/json',
                          'X-Subject-Token': fakeIdentityV3Service.client_token])

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == responseCode
        mc.handlings.size() == 1
        mc.handlings[0].request.headers.getFirstValue("X-authorization") == "Proxy"
        mc.handlings[0].request.headers.getFirstValue("X-Identity-Status") == "Indeterminate"
        mc.handlings[0].request.headers.contains("X-Delegated")
        mc.handlings[0].request.headers.getFirstValue("X-Delegated") =~ delegatingMsg

        where:
        reqProject  | authResponseCode | responseCode   |responseBody                                           | delegatingMsg
        "p500"      | 401              | "200"          |"Unauthorized"                                         | "status_code=401.components=openstack-identity-v3.message=Failure in Auth Filter.;q=0.7"
        "p501"      | 403              | "200"          |"Unauthorized"                                         | "status_code=403.components=openstack-identity-v3.message=Failure in Auth Filter.;q=0.7"
        "p502"      | 404              | "200"          |fakeIdentityV3Service.identityFailureJsonRespTemplate  | "status_code=404.components=openstack-identity-v3.message=Failure in Auth Filter.;q=0.7"
    }

    def "when client failed to authenticate at the origin service, the WWW-Authenticate header should be expected" () {
        given:
        fakeIdentityV3Service.validateTokenHandler = {
            tokenId, request ->
                new Response(404, null, null, fakeIdentityV3Service.identityFailureJsonRespTemplate)
        }

        when: "User passes a request through repose"
        MessageChain mc = deproxy.makeRequest(
                url: "$reposeEndpoint/servers/11111/",
                method: 'GET',
                headers: [
                        'X-Subject-Token': fakeIdentityV3Service.client_token
                ],
                defaultHandler: {
                    new Response(401, "", ["www-authenticate":"delegated"], "")
                }
        )

        then: "Request body sent from repose to the origin service should contain"
        mc.receivedResponse.code == "401"
        mc.receivedResponse.headers.findAll("WWW-Authenticate").size() == 1
        mc.receivedResponse.headers.findAll("WWW-Authenticate").get(0).matches("Keystone uri=.*")
    }
}
