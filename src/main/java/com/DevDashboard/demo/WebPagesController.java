package com.DevDashboard.demo;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author ViseshiniReddy
 */
@Controller
public class WebPagesController {

    @RequestMapping(value = "/index")
    public String index() {
        return "redirect:https://github.****.com/login/oauth/authorize?scope=user:repo&client_id=********************";
    }

    @RequestMapping(value = "/organization")
    @Cacheable(value = "devListCache")
    public String organization() {
        return "organization";
        
    }

}
