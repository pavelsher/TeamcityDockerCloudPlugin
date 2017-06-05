package run.var.teamcity.cloud.docker.web;

import run.var.teamcity.cloud.docker.DockerClientAdapter;
import run.var.teamcity.cloud.docker.DockerClientAdapterFactory;
import run.var.teamcity.cloud.docker.DockerCloudClientConfig;
import run.var.teamcity.cloud.docker.client.DockerClientConfig;
import run.var.teamcity.cloud.docker.util.DockerCloudUtils;
import run.var.teamcity.cloud.docker.util.LockHandler;
import run.var.teamcity.cloud.docker.util.ScheduledFutureWithRunnable;
import run.var.teamcity.cloud.docker.web.TestContainerStatusMsg.Phase;
import run.var.teamcity.cloud.docker.web.TestContainerStatusMsg.Status;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Default {@link ContainerTestHandler} implementation.
 */
public class DefaultContainerTestHandler implements ContainerTestHandler {

    private final UUID uuid = UUID.randomUUID();
    private final LockHandler lock = LockHandler.newReentrantLock();
    private final DockerClientAdapter clientAdapter;
    private final DockerClientConfig clientConfig;
    private final ContainerTestListener statusListener;
    private final StreamingController streamingController;

    private long lastInteraction;
    private String containerId;
    private boolean buildAgentDetected = false;

    private ScheduledFutureWithRunnable<? extends ContainerTestTask> currentTaskFuture = null;

    private DefaultContainerTestHandler(DockerClientConfig clientConfig, DockerClientAdapter clientAdapter,
                                        ContainerTestListener statusListener, StreamingController streamingController) {
        assert clientAdapter != null && statusListener != null;

        this.clientConfig = clientConfig;
        this.clientAdapter = clientAdapter;
        this.statusListener = statusListener;
        this.streamingController = streamingController;

        notifyInteraction();
    }

    public static DefaultContainerTestHandler newTestInstance(@Nonnull DockerCloudClientConfig clientConfig,
                                                              @Nonnull DockerClientAdapterFactory clientAdapterFactory,
                                                              @Nonnull ContainerTestListener statusListener,
                                                              @Nullable StreamingController streamingController) {
        DockerCloudUtils.requireNonNull(clientConfig, "Client config cannot be null.");
        DockerCloudUtils.requireNonNull(clientAdapterFactory, "Docker client adapter factory cannot be null.");
        DockerCloudUtils.requireNonNull(statusListener, "Status listener cannot be null.");
        DockerClientAdapter clientAdapter = clientAdapterFactory.createAdapter(clientConfig.getDockerClientConfig()
                .connectionPoolSize(1));
        return new DefaultContainerTestHandler(clientConfig.getDockerClientConfig(), clientAdapter, statusListener,
                streamingController);
    }

    /**
     * Gets the test UUID.
     *
     * @return the test UUID
     */
    @Nonnull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the container ID associated with this test. May be {@code null} if the container creation did not succeed
     * yet.
     *
     * @return the container ID or {@code null}
     */
    @Nullable
    public String getContainerId() {
        return lock.call(() -> containerId);
    }

    /**
     * Gets the atmosphere resource associated with this test if any.
     *
     * @return the atmosphere resource or {@code null}
     */
    @Nullable
    public ContainerTestListener getStatusListener() {
        return lock.call(() -> statusListener);
    }

    /**
     * Gets the task currently associated with this test. May be {@code null} if no task was associated yet.
     *
     * @return the task
     */
    @Nullable
    public ScheduledFutureWithRunnable<? extends ContainerTestTask> getCurrentTaskFuture() {
        return lock.call(() -> currentTaskFuture);
    }

    /**
     * Gets the client adapter to run the test.
     *
     * @return the client adapter
     */
    @Nonnull
    @Override
    public DockerClientAdapter getDockerClientAdapter() {
        return clientAdapter;
    }

    /**
     * Notify a user interaction for this test.
     */
    public void notifyInteraction() {
        lastInteraction = System.nanoTime();
    }

    /**
     * Gets the last user interaction for this test as a nano timestamp.
     *
     * @return a nano timestamp
     */
    public long getLastInteraction() {
        return lastInteraction;
    }

    /**
     * Sets the current task associated with this test.
     *
     * @param currentTask the test task
     *
     * @throws NullPointerException if {@code currentTask} is {@code null}
     */
    public void setCurrentTask(@Nonnull ScheduledFutureWithRunnable<? extends ContainerTestTask>
                                       currentTask) {
        DockerCloudUtils.requireNonNull(currentTask, "Current task cannot be null.");
        lock.run(() -> {
            this.currentTaskFuture = currentTask;
            statusListener.notifyStatus(null);
            notifyInteraction();
        });
    }

    @Override
    public void notifyContainerId(@Nonnull String containerId) {
        DockerCloudUtils.requireNonNull(containerId, "Container ID cannot be null.");

        lock.run(() -> this.containerId = containerId);

        if (streamingController != null) {
            streamingController.registerContainer(uuid, new ContainerCoordinates(containerId, clientConfig));
        }
    }

    @Override
    public void notifyStatus(@Nonnull Phase phase, @Nonnull Status status, @Nullable String msg,
                             @Nullable Throwable failure, @Nonnull List<String> warnings) {
        DockerCloudUtils.requireNonNull(phase, "Test phase cannot be null.");
        DockerCloudUtils.requireNonNull(status, "Test status cannot be null.");
        DockerCloudUtils.requireNonNull(status, "Warnings list cannot be null.");

        statusListener.notifyStatus(new TestContainerStatusMsg(uuid, phase, status, msg, containerId, failure, warnings));
    }

    @Override
    public boolean isBuildAgentDetected() {
        return lock.call(() -> buildAgentDetected);
    }

    public void setBuildAgentDetected(boolean buildAgentDetected) {
        lock.run(() ->  this.buildAgentDetected = buildAgentDetected);
    }
}
