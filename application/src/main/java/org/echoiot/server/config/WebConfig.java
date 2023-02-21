package org.echoiot.server.config;

import org.echoiot.server.utils.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class WebConfig {

    @NotNull
    @RequestMapping(value = {"/assets", "/assets/", "/{path:^(?!api$)(?!assets$)(?!static$)(?!webjars$)(?!swagger-ui$)[^\\.]*}/**"})
    public String redirect() {
        return "forward:/index.html";
    }

    @RequestMapping("/swagger-ui.html")
    public void redirectSwagger(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws IOException {
        String baseUrl = MiscUtils.constructBaseUrl(request);
        response.sendRedirect(baseUrl + "/swagger-ui/");
    }

}
