package com.ordertracker.service;









import com.ordertracker.entity.OrderEntity;
import com.ordertracker.entity.WebhookLogEntity;
import com.ordertracker.enums.OrderStatus;
import com.ordertracker.repository.OrderRepository;

import com.ordertracker.repository.WebHookLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebHookService {

    private static final Logger logger =
            LoggerFactory.getLogger(WebHookService.class);
    private final OrderRepository orderRepository;
    private final WebHookLogRepository webHookLogRepository;
    private final EmailService emailService;

    public void handlePaymentWebhook(
            Long orderId,
            OrderStatus status, String payload) {

        logger.info("ORDER ID FROM PARAM = {}", orderId);

        logger.info("Payment webhook received. OrderId={}, Status={}",
                orderId, status);

        WebhookLogEntity log = new WebhookLogEntity();

        log.setEventType("PAYMENT");
        log.setOrderId(orderId);
        log.setPayload(payload);
        log.setReceivedAt(LocalDateTime.now());


        try {

            OrderEntity order = orderRepository.findById(orderId)
                    .orElseThrow(() ->
                            new RuntimeException("Order not found"));


            order.setStatus(status);
            orderRepository.save(order);

            logger.info("Order {} updated to {}", orderId, status);
            emailService.sendOrderStatusEmail(
                    order.getUser().getEmail(),
                    status.name()
            );

            log.setStatus("SUCCESS");
            log.setResponseMessage("Payment processed");

        } catch (Exception e) {

            log.setStatus("FAILED");
            log.setResponseMessage(e.getMessage());

            log.setRetryCount(0);

            logger.error("Payment webhook failed for order {}", orderId, e);
            log.setStatus("FAILED");
            log.setResponseMessage(e.getMessage());

            e.printStackTrace();
        }

        webHookLogRepository.save(log);
    }



    public void handleShipmentWebhook(
            Long orderId,
            OrderStatus status, String payload) {

        logger.info("Shipment webhook received. OrderId={}, Status={}",
                orderId, status);


        WebhookLogEntity webhookLogEntity =
                new WebhookLogEntity();

        webhookLogEntity.setEventType("SHIPMENT");
        webhookLogEntity.setOrderId(orderId);

        webhookLogEntity.setPayload(payload);

        webhookLogEntity.setReceivedAt(LocalDateTime.now());

        try {

            OrderEntity order =
                    orderRepository.findById(orderId)
                            .orElseThrow(() ->
                                    new RuntimeException("Order not found"));

           
            order.setStatus(status);

            orderRepository.save(order);
            logger.info("Shipment updated. OrderId={}, Status={}",
                    orderId, status);
            emailService.sendOrderStatusEmail(
                    order.getUser().getEmail(),
                    status.name()
            );

            webhookLogEntity.setStatus("SUCCESS");

            webhookLogEntity.setResponseMessage(
                    "Shipment processed successfully"
            );

        } catch (Exception e) {
            webhookLogEntity.setStatus("FAILED");
            webhookLogEntity.setResponseMessage(e.getMessage());

            webhookLogEntity.setRetryCount(0);

            logger.error("Shipment webhook failed for order {}", orderId, e);

            webhookLogEntity.setStatus("FAILED");

            webhookLogEntity.setResponseMessage(
                    e.getMessage()
            );

            e.printStackTrace();
        }

        webHookLogRepository.save(webhookLogEntity);
    }


    public List<WebhookLogEntity> getLogs(String status, String date) {


        LocalDateTime start = null;
        LocalDateTime end = null;

        if (date != null) {
            LocalDate localDate = LocalDate.parse(date);

            start = localDate.atStartOfDay();
            end = localDate.atTime(23, 59, 59);
        }
        if (status != null && date != null) {
            return webHookLogRepository.findByStatusIgnoreCaseAndReceivedAtBetween(status, start, end);
        }

        if (status != null) {
            return webHookLogRepository.findByStatusIgnoreCase(status);
        }

        if (date != null) {
            return webHookLogRepository.findByReceivedAtBetween(start, end);
        }

        return webHookLogRepository.findAll();


    }


    public Page<WebhookLogEntity> getLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return webHookLogRepository.findAll(pageable);
    }




@Scheduled(fixedDelay = 60000)
public void retryFailedWebhooks() {

    List<WebhookLogEntity> failedLogs =
            webHookLogRepository.findByStatus("FAILED");

    for (WebhookLogEntity log : failedLogs) {

        int retry = Optional.ofNullable(log.getRetryCount()).orElse(0);


      
        if (retry >= 3) {
            log.setStatus("FAILED_FINAL");
            webHookLogRepository.save(log);
            continue; 
        }

        try {
            log.setStatus("RETRYING");

           
            OrderEntity order = orderRepository.findById(log.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

           
            log.setStatus("SUCCESS");
            log.setResponseMessage("Retried successfully");

        } catch (Exception e) {

            log.setStatus("FAILED");
            log.setResponseMessage(e.getMessage());
        }

        log.setRetryCount(retry + 1);

        webHookLogRepository.save(log);

  public void handlePaymentWebhook(Long orderId, OrderStatus status) {

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);

        orderRepository.save(order);

    }
}


        public Map<String, Object> getDashboardData() {

            long totalWebhooks = webHookLogRepository.count();
            long success = webHookLogRepository.countByStatus("SUCCESS");
            long failed = webHookLogRepository.countByStatus("FAILED");

            long totalOrders = orderRepository.count();

            Map<String, Object> data = new HashMap<>();

            data.put("totalOrders", totalOrders);
            data.put("totalWebhooks", totalWebhooks);
            data.put("successWebhooks", success);
            data.put("failedWebhooks", failed);

            return data;
        }
    }





































































