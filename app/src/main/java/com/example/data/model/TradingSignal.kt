package com.example.data.model

enum class SignalType {
    BUY,
    SELL,
    SHORT,
    COVER,
    HOLD
}

enum class SignalAction {
    BUY,
    SELL,
    SHORT,
    COVER,
    HOLD
}

enum class SignalStatus {
    GENERATED,
    RISK_REVIEW,
    APPROVED,
    REJECTED,
    ORDER_CREATED,
    ORDER_SUBMITTED,
    ORDER_FILLED,
    ORDER_CANCELLED,
    EXPIRED
}
