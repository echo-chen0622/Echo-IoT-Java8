package org.thingsboard.server.service.executors;

import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class SharedEventLoopGroupService {

    @Getter
    private EventLoopGroup sharedEventLoopGroup;

    @PostConstruct
    public void init() {
        this.sharedEventLoopGroup = new NioEventLoopGroup();
    }

    @PreDestroy
    public void destroy() {
        if (this.sharedEventLoopGroup != null) {
            this.sharedEventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

}
