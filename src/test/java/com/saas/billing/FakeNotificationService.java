package com.saas.billing;

import java.util.ArrayList;
import java.util.List;

/** Fake de {@link NotificationService}: só registra os avisos que seriam enviados. */
final class FakeNotificationService implements NotificationService {

    record Notification(String email, Money amount) {
    }

    private final List<Notification> sent = new ArrayList<>();

    @Override
    public void sendInvoiceProcessed(String email, Money amount) {
        sent.add(new Notification(email, amount));
    }

    List<Notification> sent() {
        return List.copyOf(sent);
    }
}
