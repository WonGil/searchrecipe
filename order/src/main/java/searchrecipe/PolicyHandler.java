package searchrecipe;

import searchrecipe.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyHandler{
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @Autowired
    OrderRepository orderRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverShipped_UpdateStatus(@Payload Shipped shipped){

        if(shipped.isMe()){
            Optional<Order> orderOptional = orderRepository.findById(shipped.getOrderId());
            Order order = orderOptional.get();
            order.setStatus(shipped.getStatus());

            orderRepository.save(order);
            System.out.println("##### listener UpdateStatus : " + shipped.toJson());
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverMaterialOrdered_Order(@Payload MaterialOrdered materialOrdered){

        if(materialOrdered.isMe()){
            System.out.println("##### listener  : " + materialOrdered.toJson());
            Order order = new Order();
            order.setMaterialNm(materialOrdered.getMaterialNm());
            order.setQty(materialOrdered.getQty());
            order.setStatus("Received Order");
            orderRepository.save(order);
        }
    }

}
