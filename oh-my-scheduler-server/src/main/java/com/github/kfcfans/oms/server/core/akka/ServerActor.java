package com.github.kfcfans.oms.server.core.akka;

import akka.actor.AbstractActor;
import com.github.kfcfans.common.request.WorkerHeartbeat;
import com.github.kfcfans.common.response.AskResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理 Worker 请求
 *
 * @author tjq
 * @since 2020/3/30
 */
@Slf4j
public class ServerActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WorkerHeartbeat.class, this::onReceiveWorkerHeartbeat)
                .match(Ping.class, this::onReceivePing)
                .matchAny(obj -> log.warn("[ServerActor] receive unknown request: {}.", obj))
                .build();
    }

    /**
     * 处理存活检测的请求
     * @param ping 存活检测请求
     */
    private void onReceivePing(Ping ping) {
        AskResponse askResponse = new AskResponse();
        askResponse.setSuccess(true);
        askResponse.setExtra(System.currentTimeMillis() - ping.getCurrentTime());
        getSender().tell(askResponse, getSelf());
    }

    private void onReceiveWorkerHeartbeat(WorkerHeartbeat heartbeat) {
    }
}