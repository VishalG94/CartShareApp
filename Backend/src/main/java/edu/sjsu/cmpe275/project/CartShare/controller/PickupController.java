package edu.sjsu.cmpe275.project.CartShare.controller;

import edu.sjsu.cmpe275.project.CartShare.model.Order;
import edu.sjsu.cmpe275.project.CartShare.model.Order_Items;
import edu.sjsu.cmpe275.project.CartShare.model.Pickup;
import edu.sjsu.cmpe275.project.CartShare.repository.OrderItemsRepository;
import edu.sjsu.cmpe275.project.CartShare.repository.OrderRepository;
import edu.sjsu.cmpe275.project.CartShare.repository.PickupRepository;
import edu.sjsu.cmpe275.project.CartShare.repository.UserRepository;
import edu.sjsu.cmpe275.project.CartShare.service.EmailService;
import edu.sjsu.cmpe275.project.CartShare.service.PickupService;
import edu.sjsu.cmpe275.project.CartShare.utils.EmailUtility;
import edu.sjsu.cmpe275.project.CartShare.utils.QRCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

@Transactional
@CrossOrigin(origins = "*", allowCredentials = "true")
@RestController
public class PickupController {
    @Autowired
    PickupService pickupService;
//    private Object QRCodeGenerator;
@Autowired
private OrderRepository orderRepository;

    @Autowired
    private PickupRepository pickupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderItemsRepository orderItemsRepository;

    @Autowired
    EmailService emailService;

    @GetMapping("/pickupmenu")
    public ResponseEntity<?> getPickup(@RequestParam Long id) {
        System.out.println("Inside pickupmenu: "+id);
        return pickupService.getPickup(id);
    }

    @GetMapping("/delivery")
    public ResponseEntity<?> getDeliver(@RequestParam Long id) {
        System.out.println("Inside Delivery Controller: "+id);
        return pickupService.getDelivery(id);
    }

    @GetMapping("qrcode/{id}")
    public void qrcode(@PathVariable("id") Long id, HttpServletResponse response ) throws Exception  {
        System.out.println("Inside pickupmenu: "+id);
        Optional<Pickup> pick = pickupRepository.findById(id);
        List<Order> orders = pick.get().getOrders();
        System.out.println("size:"+orders.size());

        for(Order o : orders) {
//            Optional<User> usr = userRepository.findById(o.getBuyerId());

            if (!o.getPickupOption().equals("self")) {
                System.out.println("Pickedup order for Nick Name: "+o.getBuyerId().getNickName());

                String message = EmailUtility.pickupnotification(o.getOrderid());
                emailService.sendEmail(o.getBuyerId().getEmail(), message, " Your Order has been Picked up");
            }else{
                String message = EmailUtility.pickupself(pick.get());
                emailService.sendEmail(o.getBuyerId().getEmail(), message, " Pickup Instructions for pickup id: "+pick.get().getId());
            }

            o.setStatus("PICKEDUP");
            List<Order_Items>  order_items = o.getOrder_items();
            for(Order_Items oi : order_items){
                oi.setStatus("PICKEDUP");
                orderItemsRepository.saveAndFlush(oi);
            }
            orderRepository.saveAndFlush(o);

        }
        pick.get().setStatus("PICKEDUP");
        pickupRepository.saveAndFlush(pick.get());
        response.setContentType("image/png");
        OutputStream outputStream =  response.getOutputStream();
        outputStream.write(QRCodeGenerator.getQRCodeImage(id.toString(),200,200));
        outputStream.flush();
        outputStream.close();
    }



}
