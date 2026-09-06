package com.telusko.ragagentsapp.web;

import com.telusko.ragagentsapp.agent.SupportAgents;
import com.telusko.ragagentsapp.service.SupportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class SupportController
{
    private SupportService service;

    public SupportController(SupportService service)
    {
        this.service = service;
    }
    @GetMapping("/help")
    public String support(@RequestParam String message)
    {
        return service.handle(message);
    }
}
