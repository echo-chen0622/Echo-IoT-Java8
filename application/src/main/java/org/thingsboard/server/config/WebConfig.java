package org.thingsboard.server.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thingsboard.server.utils.MiscUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class WebConfig {

    @RequestMapping(value = {"/assets", "/assets/", "/{path:^(?!api$)(?!assets$)(?!static$)(?!webjars$)(?!swagger-ui$)[^\\.]*}/**"})
    public String redirect() {
        return "forward:/index.html";
    }

    @RequestMapping("/swagger-ui.html")
    public void redirectSwagger(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String baseUrl = MiscUtils.constructBaseUrl(request);
        response.sendRedirect(baseUrl + "/swagger-ui/");
    }

}
