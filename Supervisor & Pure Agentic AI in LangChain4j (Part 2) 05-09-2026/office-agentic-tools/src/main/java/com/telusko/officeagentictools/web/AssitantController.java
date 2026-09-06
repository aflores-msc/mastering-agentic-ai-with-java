package com.telusko.officeagentictools.web;

import com.telusko.officeagentictools.service.AssistantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/office")
@RestController
public class AssitantController
{
    private final AssistantService service;

    public AssitantController(AssistantService service) {
        this.service = service;
    }
    @GetMapping("/assist")
    public String assist(@RequestParam String request)
    {
        return service.assist(request);
    }
}
