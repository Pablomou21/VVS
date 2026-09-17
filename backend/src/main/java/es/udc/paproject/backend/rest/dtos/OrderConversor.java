package es.udc.paproject.backend.rest.dtos;

import es.udc.paproject.backend.model.entities.Order;

import java.util.List;

public class OrderConversor {

    private OrderConversor() {
    }

    public static OrderDto toOrderDto(Order order) {

        return new OrderDto(
                order.getId(),
                order.getDate().toString(),
                order.getSession().getMovie().getTitle(),
                order.getUnits(),
                order.getTotalPrice(),
                order.getSession().getDate().toString(),
                order.isDelivered());
    }

    public static List<OrderDto> toOrderDtos(List<Order> orders) {
        return orders.stream().map(OrderConversor::toOrderDto).toList();
    }
}