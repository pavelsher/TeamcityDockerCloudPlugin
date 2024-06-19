package run.var.teamcity.cloud.docker.agent;

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.util.EventDispatcher;
import org.junit.Test;
import run.var.teamcity.cloud.docker.agent.test.TestBuildAgentConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AgentController} test suite.
 */
public class AgentControllerTest {

    @Test
    public void unmanagedAgent() {
        TestBuildAgentConfiguration config = new TestBuildAgentConfiguration();

        AgentController ctrl = new AgentController(config, EventDispatcher.create(AgentLifeCycleListener.class));

        ctrl.init();

        assertThat(config.getConfigurationParameters()).isEmpty();
    }

    @Test
    public void agentWithUserData() {
        TestBuildAgentConfiguration config = new TestBuildAgentConfiguration();

        Map<String, String> customParams = new HashMap<>();
        customParams.put("KEY1", "VALUE1");
        customParams.put("KEY2", "VALUE2");

        CloudInstanceUserData userData = new CloudInstanceUserData("", "", "", null, "", "", customParams);

        config.getBuildParameters()
                .withEnv("TC_DK_CLD_CLIENT_UUID", UUID.randomUUID().toString())
                .withEnv("TC_DK_CLD_AGENT_PARAMS", userData.serialize());

        AgentController ctrl = new AgentController(config, EventDispatcher.create(AgentLifeCycleListener.class));

        ctrl.init();

        assertThat(config.getConfigurationParameters()).containsAllEntriesOf(customParams);
    }

    @Test
    public void agentWithUnparseableUserData() {
        TestBuildAgentConfiguration config = new TestBuildAgentConfiguration();

        config.getBuildParameters()
                .withEnv("TC_DK_CLD_CLIENT_UUID", UUID.randomUUID().toString())
                .withEnv("TC_DK_CLD_AGENT_PARAMS", "NOT A VALID CLOUD INSTANCE USER DATA");

        AgentController ctrl = new AgentController(config, EventDispatcher.create(AgentLifeCycleListener.class));

        ctrl.init();
    }
}
