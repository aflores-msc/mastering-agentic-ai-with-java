package com.telusko.officeagentictools.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class OfficeTools
{
    private final Map<String, Double> orders = new LinkedHashMap<>();
    private String lastOrderId = "ORD-1007";
    public OfficeTools() {
        // Sample order data
        orders.put("ORD-1005", 1299.00);
        orders.put("ORD-1006", 899.00);
        orders.put("ORD-1007", 2499.00);
    }
    //tool 1 --> return the latest order id
    @Tool("Returns the id of the customer's most recent order")
    public String lastOrderId() {
        System.out.println("[TOOL] lastOrderId -> " + lastOrderId);
        return lastOrderId;
    }
    // tool 2 --> return amount for a given order
    @Tool("Returns the total amount in rupees for the given order id")
    public double orderTotal(
            @P("the order id, for example ORD-1007") String orderId) {
        double total = orders.getOrDefault(orderId, 0.0);
        System.out.println("[TOOL] orderTotal(" + orderId + ") -> " + total);
        return total;
    }
    // tool 3 --> Add tax or tip percentage
    @Tool("Adds the given percent to an amount and returns the new amount")
    public double addPercent(
            @P("the base amount") double amount,
            @P("the percent to add, for example 18") double percent) {
        double result = amount * (1 + percent / 100.0);
        System.out.println("[TOOL] addPercent(" + amount + ", " + percent + "%) -> " + result);
        return result;
    }

        //tool 4
        // create reminder
        @Tool("Sets a reminder for a task on a given day and returns a confirmation")
        public String setReminder(
                @P("what to be reminded about") String task,
                @P("when, for example tomorrow or 2026-09-01") String when) {
            System.out.println("[TOOL] setReminder -> '" + task + "' on " + when);
            return "Reminder set: " + task + " (" + when + ")";
        }
        //tool 5 --> return today's date
        @Tool("Returns today's date in YYYY-MM-DD form")
        public String today() {
            String today = LocalDate.now().toString();
            System.out.println("[TOOL] today -> " + today);
            return today;
        }
    }


