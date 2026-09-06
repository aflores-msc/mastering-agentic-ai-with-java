package com.telusko.travelagenticsystem.controller;

import com.telusko.travelagenticsystem.service.ConciergeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/travel")
@RestController
public class ConciergeController
{
    private final ConciergeService service;
    public ConciergeController(ConciergeService service) {
        this.service = service;
    }

    @GetMapping("/help")
    public String concierge(@RequestParam String request,
                            @RequestParam(defaultValue = "SUMMARY") String strategy)
    {
        return service.concierge(request, strategy);
    }
    @GetMapping("/help-report")
    public String conciergeReport(@RequestParam String request)
    {
        return service.withReport(request);
    }

    @GetMapping("/help-manual")
    public String conciergeManual(@RequestParam String request)
    {
        return service.deterministic(request);
    }

}
