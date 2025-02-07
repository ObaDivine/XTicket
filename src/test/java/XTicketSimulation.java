
import io.gatling.javaapi.core.CoreDsl;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import io.gatling.javaapi.core.OpenInjectionStep.RampRate.RampRateOpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpDsl;
import static io.gatling.javaapi.http.HttpDsl.http;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import static io.gatling.javaapi.http.HttpDsl.status;
import java.util.HashMap;

/**
 *
 * @author bokon
 */
public class XTicketSimulation extends Simulation {

    public XTicketSimulation() {
        this.setUp(scenarioBuilder().injectOpen(injection()).protocols(setupProtocol()))
                .assertions(global().responseTime().max().lte(10000), global().successfulRequests().percent().gt(90d));
    }

    private static ScenarioBuilder scenarioBuilder() {
        return CoreDsl.scenario("Load POST Test")
                .feed(feedData())
                .exec(http("request_hello")
                        .get("/")
                        .check(status().is(200)));
    }

    private static Iterator<Map<String, Object>> feedData() {
        return Stream.generate(() -> {
            Map<String, Object> objectMap = new HashMap<>();
            objectMap.put("email", "bokon@ngxgroup.com");
            objectMap.put("password", "Green@25$");
            return objectMap;
        }).iterator();
    }

    private static HttpProtocolBuilder setupProtocol() {
        return HttpDsl.http.baseUrl("http://localhost:8080")
                .acceptHeader("application/json")
                .maxConnectionsPerHost(10)
                .userAgentHeader("XTicket Performance Test");
    }

    private RampRateOpenInjectionStep injection() {
        int totalUsers = 100;
        double userRampUpPerInterval = 10;
        double rampUpIntervalInSeconds = 30;
        int rampUptimeSeconds = 300;
        int duration = 300;
        return rampUsersPerSec(userRampUpPerInterval / (rampUpIntervalInSeconds)).to(totalUsers)
                .during(Duration.ofSeconds(rampUptimeSeconds + duration));
    }
}
