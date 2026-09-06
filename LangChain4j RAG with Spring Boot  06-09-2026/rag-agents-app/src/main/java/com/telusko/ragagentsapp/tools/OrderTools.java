package com.telusko.ragagentsapp.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderTools
{
    private record Order(String status, LocalDate deliveredOn, boolean defective) {}
    private final Map<String, Order> orders = new LinkedHashMap<>();

    public OrderTools() {
        orders.put("ORD-1001", new Order("DELIVERED", LocalDate.now().minusDays(5), true));   // recent + defective
        orders.put("ORD-1002", new Order("DELIVERED", LocalDate.now().minusDays(40), false));  // old
        orders.put("ORD-1003", new Order("IN_TRANSIT", null, false));                          // not delivered yet
    }
    @Tool("Returns the status of an order, for example DELIVERED or IN_TRANSIT")
    public String orderStatus(@P("the order id, e.g. ORD-1001") String orderId) {
        Order o = orders.get(orderId);
        String result = (o == null) ? "NOT_FOUND" : o.status();
        System.out.println("[TOOL] orderStatus(" + orderId + ") -> " + result);
        return result;
    }

    @Tool("Returns how many days ago the order was delivered, or -1 if it is not delivered yet")
    public long daysSinceDelivery(@P("the order id") String orderId) {
        Order o = orders.get(orderId);
        long days = (o == null || o.deliveredOn() == null) ? -1
                : ChronoUnit.DAYS.between(o.deliveredOn(), LocalDate.now());
        System.out.println("[TOOL] daysSinceDelivery(" + orderId + ") -> " + days);
        return days;
    }

    @Tool("Returns true if the delivered item was reported defective")
    public boolean isDefective(@P("the order id") String orderId) {
        Order o = orders.get(orderId);
        boolean result = o != null && o.defective();
        System.out.println("[TOOL] isDefective(" + orderId + ") -> " + result);
        return result;
    }
}
