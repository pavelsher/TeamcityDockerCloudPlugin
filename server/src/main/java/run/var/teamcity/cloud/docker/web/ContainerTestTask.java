package run.var.teamcity.cloud.docker.web;

import com.intellij.openapi.diagnostic.Logger;
import run.var.teamcity.cloud.docker.util.DockerCloudUtils;
import run.var.teamcity.cloud.docker.web.TestContainerStatusMsg.Phase;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static run.var.teamcity.cloud.docker.web.TestContainerStatusMsg.Status;

/**
 * {@link Runnable} base class for container test tasks. This class is responsible for managing the test
 * {@link TestContainerStatusMsg.Status status} and provide helper methods to interact with the
 * {@link ContainerTestTaskHandler test task handler}.
 * <p>
 * A test task can covers multiple test {@link TestContainerStatusMsg.Phase phases}, and has one initial phase
 * which can be queried before the test has started running.
 * </p>
 * <p>
 * The {@link #run()} method of this task is never expected to throw an exception. Instead, it will manage its
 * status accordingly and notify the test handler.
 * </p>
 */
abstract class ContainerTestTask implements Runnable {

    private final static Logger LOG = DockerCloudUtils.getLogger(ContainerTestTask.class);

    final ReentrantLock lock = new ReentrantLock();

    private final List<String> warnings = new ArrayList<>();
    private Status status = Status.PENDING;
    private String msg = "";
    private Phase phase;
    ContainerTestTaskHandler testTaskHandler;

    /**
     * Creates a new task instance.
     *
     * @param testTaskHandler the test task handler
     * @param initialPhase    the initial phase of the test
     *
     * @throws NullPointerException if any argument is {@code null}
     */
    ContainerTestTask(@Nonnull ContainerTestTaskHandler testTaskHandler, @Nonnull Phase initialPhase) {
        DockerCloudUtils.requireNonNull(testTaskHandler, "Test task handler cannot be null.");
        DockerCloudUtils.requireNonNull(initialPhase, "Initial phase cannot be null.");
        this.testTaskHandler = testTaskHandler;
        this.phase = initialPhase;
    }

    /**
     * Notify a user message for the current phase.
     *
     * @param msg the message to be notified
     */
    void msg(@Nonnull String msg) {
        msg(msg, phase);
    }

    /**
     * Notify a user message and new phase.
     *
     * @param msg   the message to be notified
     * @param phase the new phase to be notified
     */
    void msg(@Nonnull String msg, @Nonnull Phase phase) {
        msg(msg, phase, status);
    }

    void fail(String msg) {
        throw new ContainerTestTaskException(msg);
    }

    void warning(@Nonnull String warning) {
        warnings.add(warning);
    }

    private void msg(String msg, Phase phase, Status status) {
        assert lock.isHeldByCurrentThread();
        assert phase != null;
        assert msg != null;

        this.phase = phase;
        this.status = status;
        this.msg = msg;

        testTaskHandler.notifyStatus(phase, status, msg, null, warnings);
    }

    /**
     * Gets the handler for this test task.
     *
     * @return the handler
     */
    @Nonnull
    public ContainerTestTaskHandler getTestTaskHandler() {
        return testTaskHandler;
    }

    /**
     * Gets this task status.
     *
     * @return the task status
     */
    @Nonnull
    public Status getStatus() {
        return status;
    }

    /**
     * Gets this task phase.
     *
     * @return the task phase
     */
    @Nonnull
    public Phase getPhase() {
        return phase;
    }

    /**
     * Internal method to perform the test logic.
     */
    abstract Status work();

    @Override
    public final void run() {
        lock.lock();
        try {
            Exception error = null;
            try {
                if (status != Status.PENDING) {
                    throw new IllegalStateException("Cannot run task in status " + status + ".");
                }
                status = work();
            } catch (Exception e) {
                status = Status.FAILURE;
                error = e;
                msg = e.getMessage();
                LOG.warn("Processing of task " + this + " failed.", e);
            }

            // IMPORTANT: status notification must occurs at least once per task cycle. The status messages that we are
            // sending also serves as heartbeats: they are used to detect idle or stalled tests, and will keep the
            // listener open when WebSockets are in use. The latter case is especially when behind a proxy such as
            // nginx, since it may allow only.
            testTaskHandler.notifyStatus(phase, status, msg, error, warnings);
        } finally {
            lock.unlock();
        }
    }


}
