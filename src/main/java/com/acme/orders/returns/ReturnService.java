package com.acme.orders.returns;

import com.acme.orders.api.ApiModels.ReturnDecision;
import com.acme.orders.api.ApiModels.ReturnRequestBody;
import com.acme.orders.api.ApiModels.ReturnView;
import com.acme.orders.common.BusinessRuleException;
import com.acme.orders.common.ResourceNotFoundException;
import com.acme.orders.orders.OrderService;
import com.acme.orders.orders.OrderStatus;
import com.acme.orders.orders.PurchaseOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ReturnService {
    private final ReturnRequestRepository returns;
    private final OrderService orders;

    public ReturnService(ReturnRequestRepository returns, OrderService orders) {
        this.returns = returns;
        this.orders = orders;
    }

    public ReturnView request(String username, Long orderId, ReturnRequestBody request) {
        PurchaseOrder order = orders.lockOrder(orderId);
        if (!order.getCustomerUsername().equals(username)) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Only delivered orders can be returned");
        }
        if (returns.findByOrderId(orderId).isPresent()) {
            throw new BusinessRuleException("A return has already been requested for this order");
        }
        ReturnRequest entity = new ReturnRequest();
        entity.setOrder(order);
        entity.setReason(request.reason().trim());
        entity.setStatus(ReturnStatus.REQUESTED);
        entity.setCreatedAt(Instant.now());
        return view(returns.save(entity));
    }

    public ReturnView decide(Long returnId, ReturnDecision decision) {
        ReturnRequest request = returns.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found: " + returnId));
        if (request.getStatus() != ReturnStatus.REQUESTED) {
            throw new BusinessRuleException("Return request has already been decided");
        }
        if (decision.status() == ReturnStatus.REQUESTED) {
            throw new BusinessRuleException("Decision must be APPROVED or DECLINED");
        }
        request.setStatus(decision.status());
        if (decision.status() == ReturnStatus.APPROVED) {
            PurchaseOrder order = orders.lockOrder(request.getOrder().getId());
            orders.finishReturn(order);
        }
        return view(request);
    }

    private ReturnView view(ReturnRequest request) {
        return new ReturnView(request.getId(), request.getOrder().getId(), request.getReason(), request.getStatus(), request.getCreatedAt());
    }
}