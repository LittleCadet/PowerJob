package tech.powerjob.remote.akka;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import com.google.common.collect.Maps;
import tech.powerjob.common.PowerSerializable;
import tech.powerjob.common.RemoteConstant;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.remote.framework.base.RemotingException;
import tech.powerjob.remote.framework.base.ServerType;
import tech.powerjob.remote.framework.base.URL;
import tech.powerjob.remote.framework.transporter.Protocol;
import tech.powerjob.remote.framework.transporter.Transporter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

/**
 * AkkaTransporter
 *
 * @author tjq
 * @since 2022/12/31
 */
public class AkkaTransporter implements Transporter {

    private final ServerType serverType;
    private final ActorSystem actorSystem;

    private final String targetActorSystemName;

    /**
     * akka://<actor system>@<hostname>:<port>/<actor path>
     */
    private static final String AKKA_NODE_PATH = "akka://%s@%s/user/%s";

    private static final Map<String, String> SERVER_PATH_MAP = Maps.newHashMap();
    private static final Map<String, String> WORKER_PATH_MAP = Maps.newHashMap();

    /*
    Akka 使用 ActorName + 入参类型 寻址，因此只需要 rootPath
    HandlerLocation#rootPathName -> actorName
     */
    static {
        SERVER_PATH_MAP.put("benchmark", "benchmark");

        WORKER_PATH_MAP.put("benchmark", "benchmark");
    }

    public AkkaTransporter(ServerType serverType, ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        this.serverType = serverType;
        this.targetActorSystemName = AkkaConstant.fetchActorSystemName(serverType, false);
    }

    @Override
    public Protocol getProtocol() {
        return new AkkaProtocol();
    }

    @Override
    public void tell(URL url, PowerSerializable request) {
        ActorSelection actorSelection = fetchActorSelection(url);
        actorSelection.tell(request, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletionStage<T> ask(URL url, PowerSerializable request, Class<T> clz) throws RemotingException {
        ActorSelection actorSelection = fetchActorSelection(url);
        return (CompletionStage<T>) Patterns.ask(actorSelection, request, Duration.ofMillis(RemoteConstant.DEFAULT_TIMEOUT_MS));
    }

    private ActorSelection fetchActorSelection(URL url) {

        Map<String, String> rootPath2ActorNameMap = serverType == ServerType.SERVER ? SERVER_PATH_MAP : WORKER_PATH_MAP;
        final String actorName = rootPath2ActorNameMap.get(url.getLocation().getRootPath());
        CommonUtils.requireNonNull(actorName, "can't find actor by URL: " + url.getLocation());

        String address = url.getAddress().toFullAddress();

        return actorSystem.actorSelection(String.format(AKKA_NODE_PATH, targetActorSystemName, address, actorName));
    }
}
