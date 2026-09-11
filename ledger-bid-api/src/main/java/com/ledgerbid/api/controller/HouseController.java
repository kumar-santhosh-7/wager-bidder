package com.ledgerbid.api.controller;

import com.ledgerbid.api.config.WebConfig;
import com.ledgerbid.api.dto.CreateRoundRequest;
import com.ledgerbid.api.dto.EndsAtRequest;
import com.ledgerbid.api.dto.PhotosRequest;
import com.ledgerbid.api.dto.PlaceBidRequest;
import com.ledgerbid.api.dto.PlaceBidResponse;
import com.ledgerbid.api.dto.SettleRequest;
import com.ledgerbid.api.service.HouseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HouseController {
    private final HouseService house;

    public HouseController(HouseService house) {
        this.house = house;
    }

    @PostMapping("/rounds")
    public Map<String, Boolean> createRound(HttpServletRequest request, @Valid @RequestBody CreateRoundRequest req) {
        WebConfig.admin(request);
        house.createRound(req);
        return Map.of("ok", true);
    }

    @PostMapping("/rounds/{id}/ends-at")
    public Map<String, Boolean> setEndsAt(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody EndsAtRequest req) {
        WebConfig.admin(request);
        house.setEndsAt(id, req.endsAt());
        return Map.of("ok", true);
    }

    @PostMapping("/rounds/{id}/photos")
    public Map<String, Boolean> setPhotos(HttpServletRequest request, @PathVariable String id, @RequestBody PhotosRequest req) {
        WebConfig.admin(request);
        house.setPhotos(id, req.photoA(), req.photoB());
        return Map.of("ok", true);
    }

    @PostMapping("/rounds/{id}/settle")
    public Map<String, Boolean> settle(HttpServletRequest request, @PathVariable String id, @Valid @RequestBody SettleRequest req) {
        WebConfig.admin(request);
        house.settle(id, req.winner());
        return Map.of("ok", true);
    }

    @PostMapping("/bids")
    public PlaceBidResponse placeBid(HttpServletRequest request, @Valid @RequestBody PlaceBidRequest req) {
        return house.placeBid(WebConfig.actor(request), req);
    }

    @PostMapping("/bids/{id}/approve")
    public Map<String, Boolean> approve(HttpServletRequest request, @PathVariable String id) {
        WebConfig.admin(request);
        house.approveBid(id);
        return Map.of("ok", true);
    }

    @PostMapping("/bids/{id}/reject")
    public Map<String, Boolean> reject(HttpServletRequest request, @PathVariable String id) {
        WebConfig.admin(request);
        house.rejectBid(id);
        return Map.of("ok", true);
    }

    @PostMapping("/bids/{id}/withdraw")
    public Map<String, Boolean> withdraw(HttpServletRequest request, @PathVariable String id) {
        house.withdrawBid(WebConfig.actor(request), id);
        return Map.of("ok", true);
    }
}
