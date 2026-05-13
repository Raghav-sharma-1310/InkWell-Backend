/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.repository;

import com.inkwell.auth.entity.PaymentOrder;
import com.inkwell.auth.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups payment order repository behavior so the module keeps a clear responsibility. */
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    Optional<PaymentOrder> findByPaymentOrderIdAndUserUserId(UUID paymentOrderId, UUID userId);

    List<PaymentOrder> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    List<PaymentOrder> findAllByUser(User user);
}
