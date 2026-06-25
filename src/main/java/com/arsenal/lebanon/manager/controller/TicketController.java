package com.arsenal.lebanon.manager.controller;

import com.arsenal.lebanon.manager.service.AllocationService;
import com.arsenal.lebanon.manager.service.MembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private AllocationService allocationService;

    @GetMapping("/allocate/{gameId}")
    public String runAllocationProcess(@PathVariable Long gameId) {
        try {
            System.out.println("🚀 Starting full administrative match cycle...");

            // 1. First run: Clean up and lapse expired memberships automatically
            membershipService.checkAndLapseExpiredMemberships();

            // 2. Second run: Process the priority algorithm scores and assign tickets
            allocationService.allocateTicketsForGame(gameId);

            return "❤️ Success! Membership cleanup and ticket allocation completed for Game ID: " + gameId;

        } catch (Exception e) {
            return "❌ Error executing allocation: " + e.getMessage();
        }
    }
}