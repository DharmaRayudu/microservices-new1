package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.model.Order;

import java.util.List;

public interface OrderService {

    String placeOrder(OrderRequest orderRequest);

    List<OrderRequest> getAllOrders();
}
