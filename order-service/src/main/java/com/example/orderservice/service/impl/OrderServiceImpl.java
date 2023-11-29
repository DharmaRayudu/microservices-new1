package com.example.orderservice.service.impl;

import com.example.orderservice.dto.InventoryResponse;
import com.example.orderservice.dto.OrderLineItemsDto;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private OrderRepository orderRepository;
    private WebClient.Builder webClientBuilder;

    public OrderServiceImpl(OrderRepository orderRepository, WebClient.Builder webClient) {
        this.orderRepository = orderRepository;
        this.webClientBuilder = webClient;
    }

    @Override
    public String placeOrder(OrderRequest orderRequest) {

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItemsDto>  orderLineItemsDto = orderRequest.getOrderLineItems();
        order.setOrderLineItems(orderLineItemsDto.stream().map(items->mapToRequset(items)).collect(Collectors.toList()));

        List<String> skuCodes=  order.getOrderLineItems().stream()
                .map(OrderLineItems::getSkuCode).toList();


        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventories", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

            log.info("TEst ", inventoryResponses);

        boolean results=  Arrays.stream(inventoryResponses).anyMatch(InventoryResponse::isInStock);
        if(results){
            orderRepository.save(order);
            log.info("Data Saved placeOrder()");
            return "Order Placed";
        }else{
            throw  new IllegalArgumentException("Product is not in stock, please try after sometime.");
        }
    }

    @Override
    public List<OrderRequest> getAllOrders() {
       List<Order>  orderList =  orderRepository.findAll();

      return  orderList.stream().map(order -> mapToOrderRequest(order)).collect(Collectors.toList());
    }

    private OrderRequest mapToOrderRequest(Order order) {

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderNumber(order.getOrderNumber());
        List<OrderLineItems> orderLineItems =  order.getOrderLineItems();
        orderRequest.setOrderLineItems(orderLineItems.stream().map(orderLineItems1 -> mapToLineItemsDTO(orderLineItems1)).collect(Collectors.toList()));
        return orderRequest;

    }

    private OrderLineItemsDto mapToLineItemsDTO(OrderLineItems orderLineItems) {
        OrderLineItemsDto lineItemsDto = new OrderLineItemsDto();
        lineItemsDto.setSkuCode(orderLineItems.getSkuCode());
        lineItemsDto.setQuantity(orderLineItems.getQuantity());
        lineItemsDto.setPrice(orderLineItems.getPrice());
        return lineItemsDto;
    }

    private OrderLineItems mapToRequset(OrderLineItemsDto items) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setQuantity(items.getQuantity());
        orderLineItems.setPrice(items.getPrice());
        orderLineItems.setSkuCode(items.getSkuCode());
        return orderLineItems;

    }


}
