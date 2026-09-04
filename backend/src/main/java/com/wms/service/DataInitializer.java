package com.wms.service;

import com.wms.entity.*;
import com.wms.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class DataInitializer implements ApplicationRunner {
    private final ProductRepository productRepo;
    private final LocationRepository locationRepo;
    private final PutawayOrderRepository orderRepo;
    private final InventoryRepository inventoryRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ProductRepository productRepo,
                           LocationRepository locationRepo,
                           PutawayOrderRepository orderRepo,
                           InventoryRepository inventoryRepo,
                           UserRepository userRepo,
                           PasswordEncoder passwordEncoder) {
        this.productRepo = productRepo;
        this.locationRepo = locationRepo;
        this.orderRepo = orderRepo;
        this.inventoryRepo = inventoryRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepo.count() == 0) {
            User admin = new User("admin", passwordEncoder.encode("admin123"), "管理员", UserRole.ADMIN);
            admin.setEnabled(true);
            userRepo.save(admin);

            User operator = new User("operator", passwordEncoder.encode("operator123"), "作业员", UserRole.OPERATOR);
            operator.setEnabled(true);
            userRepo.save(operator);
        }

        if (productRepo.count() != 0) return;

        Product p1 = productRepo.save(new Product("SP-001", "深沟球轴承", "6205-ZZ", "套"));
        Product p2 = productRepo.save(new Product("SP-002", "O型密封圈", "Φ20", "个"));
        Product p3 = productRepo.save(new Product("SP-003", "LED指示灯", "24V", "个"));

        Location l1 = locationRepo.save(new Location("A-01-01", "A区", "常温", null));
        Location l2 = locationRepo.save(new Location("A-01-02", "A区", "常温", null));
        Location l3 = locationRepo.save(new Location("A-02-01", "A区", "大件", null));
        Location l4 = locationRepo.save(new Location("B-01-01", "B区", "常温", null));
        Location l5 = locationRepo.save(new Location("B-01-02", "B区", "冷藏", null));
        Location l6 = locationRepo.save(new Location("C-01-01", "C区", "危险品", null));

        PutawayOrder order = new PutawayOrder();
        order.setOrderNo("PASEED0001");
        order.setProduct(p1);
        order.setPlanQty(100);
        order.setPutQty(0);
        order.setStatus(PutawayStatus.PENDING);
        orderRepo.save(order);

        inventoryRepo.save(new Inventory(p2, l1, 50, Instant.now()));
        inventoryRepo.save(new Inventory(p3, l4, 30, Instant.now()));
    }
}
