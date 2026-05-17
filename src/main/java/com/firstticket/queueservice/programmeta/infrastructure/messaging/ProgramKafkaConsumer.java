package com.firstticket.queueservice.programmeta.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstticket.queueservice.programmeta.application.ProgramMetaService;
import com.firstticket.queueservice.programmeta.application.dto.CancelProgramCommand;
import com.firstticket.queueservice.programmeta.application.dto.CreateProgramMetaCommand;
import com.firstticket.queueservice.programmeta.application.dto.UpdateProgramTimeCommand;
import com.firstticket.queueservice.programmeta.infrastructure.messaging.payload.ProgramCancelledPayload;
import com.firstticket.queueservice.programmeta.infrastructure.messaging.payload.ProgramCreatedPayload;
import com.firstticket.queueservice.programmeta.infrastructure.messaging.payload.ProgramTimeUpdatedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * program 도메인 이벤트 Kafka Consumer.
 * Payload 역직렬화 + Command 변환 + Application Service 호출의 책임을 가진다.
 *
 * <p>처리 실패 시 ack 하지 않아 Kafka 가 재전송하도록 한다 (at-least-once).
 * 도메인 액션이 idempotent 하므로 중복 수신해도 안전하다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramKafkaConsumer {

    private final ProgramMetaService programMetaService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.program-created}")
    public void onProgramCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received program.created. key={}", record.key());
        try {
            ProgramCreatedPayload payload = objectMapper.readValue(
                record.value(), ProgramCreatedPayload.class);

            CreateProgramMetaCommand command = CreateProgramMetaCommand.of(
                payload.programId(), payload.openAt(), payload.closeAt(), payload.status());

            programMetaService.handleCreated(command);
            ack.acknowledge();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            // 메시지 자체가 잘못됨 → 건너뜀 (재시도해도 같은 결과)
            log.error("잘못된 메시지. 건너뜀. topic={}, key={}, value={}",
                record.topic(), record.key(), record.value(), e);
            ack.acknowledge();
        } catch (Exception e) {
            // 일시 장애 → 재전송 기다림
            log.error("program.created 처리 실패. record={}", record, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.program-time-updated}")
    public void onProgramTimeUpdated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received program.time.updated. key={}", record.key());
        try {
            ProgramTimeUpdatedPayload payload = objectMapper.readValue(
                record.value(), ProgramTimeUpdatedPayload.class);

            UpdateProgramTimeCommand command = UpdateProgramTimeCommand.of(
                payload.programId(), payload.openAt(), payload.closeAt());

            programMetaService.handleTimeUpdated(command);
            ack.acknowledge();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error("잘못된 메시지. 건너뜀. topic={}, key={}, value={}",
                record.topic(), record.key(), record.value(), e);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("program.time.updated 처리 실패. record={}", record, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.program-cancelled}")
    public void onProgramCancelled(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received program.cancelled. key={}", record.key());
        try {
            ProgramCancelledPayload payload = objectMapper.readValue(
                record.value(), ProgramCancelledPayload.class);

            CancelProgramCommand command = CancelProgramCommand.of(
                payload.programId(), payload.status());

            programMetaService.handleCancelled(command);
            ack.acknowledge();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error("잘못된 메시지. 건너뜀. topic={}, key={}, value={}",
                record.topic(), record.key(), record.value(), e);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("program.cancelled 처리 실패. record={}", record, e);
        }
    }
}
