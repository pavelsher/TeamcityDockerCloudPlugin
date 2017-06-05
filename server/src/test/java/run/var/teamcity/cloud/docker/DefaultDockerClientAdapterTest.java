package run.var.teamcity.cloud.docker;

import org.junit.Before;
import org.junit.Test;
import run.var.teamcity.cloud.docker.client.DockerAPIVersion;
import run.var.teamcity.cloud.docker.client.DockerClient;
import run.var.teamcity.cloud.docker.client.DockerClientConfig;
import run.var.teamcity.cloud.docker.client.DockerRegistryCredentials;
import run.var.teamcity.cloud.docker.client.StdioType;
import run.var.teamcity.cloud.docker.client.TestImage;
import run.var.teamcity.cloud.docker.test.Interceptor;
import run.var.teamcity.cloud.docker.test.TestDockerClient;
import run.var.teamcity.cloud.docker.test.TestDockerClient.Container;
import run.var.teamcity.cloud.docker.util.DockerCloudUtils;
import run.var.teamcity.cloud.docker.util.Node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static run.var.teamcity.cloud.docker.test.TestDockerClient.ContainerStatus.CREATED;
import static run.var.teamcity.cloud.docker.test.TestDockerClient.ContainerStatus.STARTED;
import static run.var.teamcity.cloud.docker.test.TestUtils.mapOf;
import static run.var.teamcity.cloud.docker.test.TestUtils.pair;

public class DefaultDockerClientAdapterTest {

    private TestDockerClient dockerClient;

    @Before
    public void init() {
        dockerClient = new TestDockerClient(new DockerClientConfig(TestDockerClient.TEST_CLIENT_URI, DockerAPIVersion
                .DEFAULT), DockerRegistryCredentials.ANONYMOUS);
    }

    @Test
    public void createImageConfigInvalidArguments() {
        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> adapter.createAgentContainer(null,
                "resolved-image:latest", emptyMap(), emptyMap()));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> adapter.createAgentContainer(
                Node.EMPTY_OBJECT,null, emptyMap(), emptyMap()));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> adapter.createAgentContainer(
                Node.EMPTY_OBJECT, "resolved-image:latest", null, emptyMap()));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> adapter.createAgentContainer(
                Node.EMPTY_OBJECT, "resolved-image:latest", emptyMap(), null));
    }

    @Test
    public void sourceImageIdIsSet() {
        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        TestImage img = dockerClient.newLocalImage("resolved-image", "latest");

        String containerId = adapter.createAgentContainer(Node.EMPTY_OBJECT, "resolved-image:latest", emptyMap(),
                emptyMap()).getId();

        List<Container> containers = dockerClient.getContainers();
        assertThat(containers).hasSize(1).first().matches(container -> container.getId().equals(containerId));

        Container container = containers.get(0);

        assertThat(container.getLabels()).isEqualTo(mapOf(pair(DockerCloudUtils.SOURCE_IMAGE_ID_LABEL, img.getId())));
    }

    @Test
    public void successfulPull() {
        TestImage img = dockerClient.newRegistryImage("resolved-image", "latest");

        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        adapter.pull("resolved-image:latest", DockerRegistryCredentials.ANONYMOUS);

        assertThat(dockerClient.getLocalImages()).hasSize(1).containsExactly(img);
    }


    @Test(expected = DockerClientAdapterException.class)
    public void failedPull() {

        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);
        // Pulling non existing image.
        adapter.pull("resolved-image:latest", DockerRegistryCredentials.ANONYMOUS);
    }

    @Test
    public void pullListener() {

        dockerClient.newRegistryImage("resolved-image", "latest")
                .pullProgress("layer1", "Pulling fs layer", null, null)
                .pullProgress("layer1", "Downloading", 0, 100)
                .pullProgress("layer2", "Pulling fs layer", null, null)
                .pullProgress("layer2", "Downloading", 0, new BigInteger("10000000000"))
                .pullProgress("layer1", "Downloading", 50, 100)
                .pullProgress("layer1", "Downloading", 100, 100)
                .pullProgress("layer1", "Pull complete", null, null)
                .pullProgress("layer2", "Downloading", new BigInteger("2500000000"), new BigInteger("10000000000"))
                .pullProgress("layer2", "Downloading", new BigInteger("10000000000"), new BigInteger("10000000000"))
                .pullProgress("layer2", "Pull complete", null, null);


        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        List<ListenerInvocation> invocations = new ArrayList<>();

        adapter.pull("resolved-image:latest", DockerRegistryCredentials.ANONYMOUS,
                (layer, status, percent) -> invocations.add(new ListenerInvocation(layer, status, percent)));

        assertThat(invocations).isEqualTo(Arrays.asList(
                new ListenerInvocation("layer1", "Pulling fs layer", PullStatusListener.NO_PROGRESS),
                new ListenerInvocation("layer1", "Downloading", 0),
                new ListenerInvocation("layer2", "Pulling fs layer", PullStatusListener.NO_PROGRESS),
                new ListenerInvocation("layer2", "Downloading", 0),
                new ListenerInvocation("layer1", "Downloading", 50),
                new ListenerInvocation("layer1", "Downloading", 100),
                new ListenerInvocation("layer1", "Pull complete", PullStatusListener.NO_PROGRESS),
                new ListenerInvocation("layer2", "Downloading", 25),
                new ListenerInvocation("layer2", "Downloading", 100),
                new ListenerInvocation("layer2", "Pull complete", PullStatusListener.NO_PROGRESS)
        ));
    }

    @Test
    public void pullListenerUnexpectedProgressReporting() {

        dockerClient.newRegistryImage("resolved-image", "latest")
                .pullProgress("layer", "Downloading", 100, 0) // Must avoid dividing by zero.
                .pullProgress("layer", "Downloading", 100, 50)  // Current > total
                .pullProgress("layer", "Downloading", -50, 100)
                .pullProgress("layer", "Downloading", -50, -100);


        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        List<ListenerInvocation> invocations = new ArrayList<>();

        adapter.pull("resolved-image:latest", DockerRegistryCredentials.ANONYMOUS,
                (layer, status, percent) -> invocations.add(new ListenerInvocation(layer, status, percent)));

        assertThat(invocations).isEqualTo(Arrays.asList(
                new ListenerInvocation("layer", "Downloading", PullStatusListener.NO_PROGRESS),
                new ListenerInvocation("layer", "Downloading", PullStatusListener.NO_PROGRESS),
                new ListenerInvocation("layer", "Downloading", PullStatusListener.NO_PROGRESS),
                new ListenerInvocation("layer", "Downloading", PullStatusListener.NO_PROGRESS)
        ));
    }

    private class ListenerInvocation {
        final String layer;
        final String status;
        final int percent;


        ListenerInvocation(String layer, String status, int percent) {
            this.layer = layer;
            this.status = status;
            this.percent = percent;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ListenerInvocation)) {
                return false;
            }
            ListenerInvocation invocation = (ListenerInvocation) obj;
            return Objects.equals(layer, invocation.layer) && Objects.equals(status, invocation.status) &&
                    Objects.equals(percent, invocation.percent);
        }

        @Override
        public String toString() {
            return "layer=" + layer + ", status=" + status + ", percent=" + percent;
        }
    }

    @Test
    public void containerLabels() {
        dockerClient.localImage("resolved-image", "latest");

        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        Map<String, String> labels = mapOf(pair("key1", "value1"), pair("key2", "value2"));
        adapter.createAgentContainer(Node.EMPTY_OBJECT,"resolved-image:latest", labels, emptyMap());
        List<Container> containers = dockerClient.getContainers();
        assertThat(containers).hasSize(1);
        assertThat(containers.get(0).getLabels()).containsEntry("key1", "value1").containsEntry("key2", "value2");
    }

    @Test
    public void containerEnv() {

        dockerClient.localImage("resolved-image", "latest");

        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        Map<String, String> env = mapOf(pair("key1", "value1"), pair("key2", "value2"));

        adapter.createAgentContainer(Node.EMPTY_OBJECT, "resolved-image:latest", emptyMap(), env);

        List<Container> containers = dockerClient.getContainers();
        assertThat(containers).hasSize(1);
        assertThat(containers.get(0).getEnv()).isEqualTo(env);
    }

    @Test
    public void inspectAgentContainer() {
        Interceptor<DockerClient> interceptor = Interceptor.wrap(dockerClient, DockerClient.class);

        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(interceptor.buildProxy());
        Container container = new Container().name("agent_name");
        dockerClient.container(container);

        ContainerInspection inspection = adapter.inspectAgentContainer(container.getId());

        assertThat(inspection.getName()).isEqualTo("agent_name");
    }

    @Test
    public void startAgentContainer() {

        Interceptor<DockerClient> interceptor = Interceptor.wrap(dockerClient, DockerClient.class);

        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(interceptor.buildProxy());

        Container container = new Container(CREATED);
        dockerClient.container(container);

        adapter.startAgentContainer(container.getId());

        assertThat(container.getStatus()).isEqualTo(STARTED);

        assertThat(interceptor.getInvocations()).hasSize(1).first().matches(invocation -> invocation.matches
                ("startContainer", container.getId()));
    }

    @Test
    public void restartAgentContainer() {

        Interceptor<DockerClient> interceptor = Interceptor.wrap(dockerClient, DockerClient.class);

        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(interceptor.buildProxy());

        Container container = new Container(STARTED);
        dockerClient.container(container);

        adapter.restartAgentContainer(container.getId());

        assertThat(container.getStatus()).isEqualTo(STARTED);

        assertThat(interceptor.getInvocations()).hasSize(1).first().matches(invocation -> invocation.matches
                ("restartContainer", container.getId()));
    }

    @Test
    public void listAgentContainers() {
        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        TestImage image1 = dockerClient.newLocalImage("image-1", "latest");
        TestImage image2 = dockerClient.newLocalImage("image-2", "latest");

        assertThat(adapter.listActiveAgentContainers("foo", "bar")).isEmpty();

        Container validContainer1 = new Container().
                label("foo", "bar").
                label(DockerCloudUtils.SOURCE_IMAGE_ID_LABEL, image1.getId()).
                image(image1);

        Container validContainer2 = new Container().
                label("foo", "bar").
                label(DockerCloudUtils.SOURCE_IMAGE_ID_LABEL, image2.getId()).
                image(image2);

        Container wrongLabelValue = new Container().
                label("foo", "baz").
                label(DockerCloudUtils.SOURCE_IMAGE_ID_LABEL, image1.getId()).
                image(image1);

        Container sourceImageIdNotMatching = new Container().
                label("foo", "bar").
                label(DockerCloudUtils.SOURCE_IMAGE_ID_LABEL, image1.getId()).
                image(image2);

        dockerClient.container(validContainer1).container(validContainer2).container(wrongLabelValue).container
                (sourceImageIdNotMatching);

        List<String> containerIds = adapter.listActiveAgentContainers("foo", "bar").stream().
                map(ContainerInfo::getId).
                collect(Collectors.toList());

        assertThat(containerIds).containsExactlyInAnyOrder(validContainer1.getId(), validContainer2.getId());

    }

    @Test
    public void getLogs() {
        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        Container container = new Container();

        container.getLogStreamHandler().
                fragment("txt on stdout, ", StdioType.STDOUT).
                fragment("txt on stdin, ", StdioType.STDIN).
                fragment("txt unknown std type", null);

        dockerClient.container(container);

        CharSequence logs = adapter.getLogs(container.getId());

        assertThat(logs.toString()).isEqualTo("txt on stdout, txt on stdin, txt unknown std type");
    }

    @Test
    public void close() {
        DefaultDockerClientAdapter adapter = new DefaultDockerClientAdapter(dockerClient);

        assertThat(dockerClient.isClosed()).isFalse();

        adapter.close();

        assertThat(dockerClient.isClosed()).isTrue();

        adapter.close();
    }

    private DockerImageConfig createImageConfig(boolean pullOnCreate) {
        return createImageConfig(Node.EMPTY_OBJECT, pullOnCreate);
    }

    private DockerImageConfig createImageConfig(Node containerSpec, boolean pullOnCreate) {
        return new DockerImageConfig("UnitTest", containerSpec, pullOnCreate, false, false,
                DockerRegistryCredentials.ANONYMOUS, 1, 111);


    }
}