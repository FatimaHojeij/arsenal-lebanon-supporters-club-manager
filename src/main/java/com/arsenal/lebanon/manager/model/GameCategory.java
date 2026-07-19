package com.arsenal.lebanon.manager.model;

public enum GameCategory {
    A(145),
    B(90),
    C(63),
    NA(0);

    private final int ticketPrice;

    GameCategory(int ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public int getTicketPrice() {
        return ticketPrice;
    }
}