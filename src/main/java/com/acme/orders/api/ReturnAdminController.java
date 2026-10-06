package com.acme.orders.api;

import com.acme.orders.api.ApiModels.ReturnDecision;
import com.acme.orders.api.ApiModels.ReturnView;
import com.acme.orders.returns.ReturnService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/returns")
@PreAuthorize("hasRole('ADMIN')")
public class ReturnAdminController {
    private final ReturnService returns;

    public ReturnAdminController(ReturnService returns) {
        this.returns = returns;
    }

    @PatchMapping("/{returnId}")
    public ReturnView decide(@PathVariable Long returnId, @Valid @RequestBody ReturnDecision decision) {
        return returns.decide(returnId, decision);
    }
}