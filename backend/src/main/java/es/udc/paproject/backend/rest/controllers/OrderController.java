package es.udc.paproject.backend.rest.controllers;

import es.udc.paproject.backend.model.exceptions.AlreadyDeliveredException;
import es.udc.paproject.backend.model.exceptions.CreditCardException;
import es.udc.paproject.backend.model.entities.Order;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.model.exceptions.NotEnoughTicketsException;
import es.udc.paproject.backend.model.exceptions.SessionAlreadyStartedException;
import es.udc.paproject.backend.model.services.Block;
import es.udc.paproject.backend.model.services.OrderService;
import es.udc.paproject.backend.rest.common.ErrorsDto;
import es.udc.paproject.backend.rest.dtos.BlockDto;
import es.udc.paproject.backend.rest.dtos.BuyTicketsParamsDto;
import es.udc.paproject.backend.rest.dtos.OrderDto;
import es.udc.paproject.backend.rest.dtos.DeliverOrderParamsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

import static es.udc.paproject.backend.rest.dtos.OrderConversor.toOrderDtos;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final static String CREDIT_CARD_EXCEPTION_CODE = "project.exceptions.CreditCardException";
    private final static String ALREADY_DELIVERED_EXCEPTION_CODE = "project.exceptions.AlreadyDeliveredException";
    private final static String NOT_ENOUGH_TICKETS_EXCEPTION_CODE =  "project.exceptions.NotEnoughTicketsException";

    @Autowired
    private OrderService orderService;

    @Autowired
    private MessageSource messageSource;

    @ExceptionHandler(NotEnoughTicketsException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorsDto handleNotEnoughTicketsException(NotEnoughTicketsException exception, Locale locale) {
        String errorMessage = messageSource.getMessage(NOT_ENOUGH_TICKETS_EXCEPTION_CODE,
                new Object[] {exception.getNumberTicketsLeft()}, NOT_ENOUGH_TICKETS_EXCEPTION_CODE, locale);
        return new ErrorsDto(errorMessage);
    }

    @ExceptionHandler(CreditCardException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorsDto handleCreditCardException(CreditCardException exception, Locale locale) {
        String errorMessage = messageSource.getMessage(CREDIT_CARD_EXCEPTION_CODE,
                new Object[] {exception.getCreditCardNum(), exception.getOrderId()}, CREDIT_CARD_EXCEPTION_CODE, locale);
        return new ErrorsDto(errorMessage);
    }

    @ExceptionHandler(AlreadyDeliveredException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorsDto handleAlreadyDeliveredException(AlreadyDeliveredException exception, Locale locale) {
        String errorMessage = messageSource.getMessage(ALREADY_DELIVERED_EXCEPTION_CODE,
                null, ALREADY_DELIVERED_EXCEPTION_CODE, locale);
        return new ErrorsDto(errorMessage);
    }

    @PostMapping
    public Long buyTickets(@RequestAttribute Long userId,
        @Validated @RequestBody BuyTicketsParamsDto params)
        throws InstanceNotFoundException, SessionAlreadyStartedException, NotEnoughTicketsException {

        Order order = orderService.buyTickets(
                userId,
                params.getSessionId(),
                params.getNumTickets(),
                params.getCreditCardNum());

        return order.getId();
    }

    @GetMapping
    public BlockDto<OrderDto> findUserOrders(@RequestAttribute Long userId, @RequestParam(defaultValue = "0") int page)
        throws InstanceNotFoundException {

        Block<Order> orderBlock = orderService.findUserOrders(userId, page, 2);

        return new BlockDto<>(toOrderDtos(orderBlock.getItems()), orderBlock.getExistMoreItems());
    }

    @PostMapping("/{id}/deliver")
    public void deliverTickets(@PathVariable Long id, @Validated @RequestBody DeliverOrderParamsDto params)
            throws InstanceNotFoundException, CreditCardException, SessionAlreadyStartedException, AlreadyDeliveredException {
        orderService.deliverTickets(id, params.getCreditCardNum());
    }
}