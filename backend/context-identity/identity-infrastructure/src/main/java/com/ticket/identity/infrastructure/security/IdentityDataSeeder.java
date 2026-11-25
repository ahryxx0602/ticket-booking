package com.ticket.identity.infrastructure.security;

import com.ticket.identity.domain.model.AdminPermissionCatalog;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.infrastructure.persistence.entity.SystemRoleJpaEntity;
import com.ticket.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.ticket.identity.infrastructure.persistence.repository.SpringDataSystemRoleRepository;
import com.ticket.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class IdentityDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IdentityDataSeeder.class);

    private static final Set<String> FORBIDDEN_PROD_PASSWORDS = Set.of(
            "admin_dev_password",
            "customer_dev_password",
            "operator_dev_password",
            "password",
            "admin",
            "123456",
            "12345678",
            "123456789012"
    );

    private final SpringDataUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private SpringDataSystemRoleRepository systemRoleRepository;
    private Environment environment;

    @Value("${app.seed.demo-users-enabled:false}")
    private boolean demoUsersEnabled;

    @Value("${app.seed.admin-password:admin_dev_password}")
    private String adminPassword;

    @Value("${app.seed.customer-password:customer_dev_password}")
    private String customerPassword;

    @Value("${app.seed.operator-password:operator_dev_password}")
    private String operatorPassword;

    @Value("${app.bootstrap.admin.enabled:false}")
    private boolean bootstrapAdminEnabled;

    @Value("${app.bootstrap.admin.username:}")
    private String bootstrapAdminUsername;

    @Value("${app.bootstrap.admin.email:}")
    private String bootstrapAdminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String bootstrapAdminPassword;

    public IdentityDataSeeder(SpringDataUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired(required = false)
    public void setSystemRoleRepository(SpringDataSystemRoleRepository systemRoleRepository) {
        this.systemRoleRepository = systemRoleRepository;
    }

    @Autowired(required = false)
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setDemoUsersEnabled(boolean demoUsersEnabled) {
        this.demoUsersEnabled = demoUsersEnabled;
    }

    public void setBootstrapAdminConfig(boolean enabled, String username, String email, String password) {
        this.bootstrapAdminEnabled = enabled;
        this.bootstrapAdminUsername = username;
        this.bootstrapAdminEmail = email;
        this.bootstrapAdminPassword = password;
    }

    @Override
    public void run(String... args) {
        // 1. Luôn khởi tạo 5 System Roles chuẩn (danh mục hệ thống không chứa thông tin đăng nhập)
        seedDefaultRoles();

        boolean isProdProfile = environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"));

        // 2. BE-SEC-006: Tuyệt đối KHÔNG tạo tài khoản demo trên Production
        if (!isProdProfile && demoUsersEnabled) {
            seedAdminUser();
            seedCustomerUser();
            seedOperatorUser();
        } else if (isProdProfile && demoUsersEnabled) {
            log.warn("[IDENTITY SEEDER] Refusing to seed demo accounts because active profile is 'prod'");
        }

        // 3. BE-SEC-006: Opt-in Production Admin Bootstrap với kiểm tra mật khẩu mạnh bắt buộc
        if (bootstrapAdminEnabled) {
            bootstrapProductionAdmin();
        }
    }

    private void bootstrapProductionAdmin() {
        String cleanUsername = bootstrapAdminUsername != null ? bootstrapAdminUsername.trim() : "";
        String cleanEmail = bootstrapAdminEmail != null ? bootstrapAdminEmail.trim() : "";
        String rawPassword = bootstrapAdminPassword != null ? bootstrapAdminPassword : "";

        if (cleanUsername.isEmpty() || cleanEmail.isEmpty()) {
            throw new IllegalStateException("Production admin bootstrap requires non-empty username and email");
        }
        if (rawPassword.trim().length() < 12 || FORBIDDEN_PROD_PASSWORDS.contains(rawPassword.trim().toLowerCase())) {
            throw new IllegalStateException("Production admin bootstrap password must be at least 12 characters and must not use known default/demo passwords");
        }

        if (!userRepository.existsByUsername(cleanUsername)) {
            UserJpaEntity admin = new UserJpaEntity();
            admin.setUsername(cleanUsername);
            admin.setEmail(cleanEmail);
            admin.setPasswordHash(passwordEncoder.encode(rawPassword));
            admin.setFullName("Platform Super Administrator");
            admin.setRole("ROLE_ADMIN");
            admin.setCustomRoleCode("ROLE_ADMIN");
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
            log.info("[IDENTITY SEEDER] Bootstrapped opt-in production Super Admin account: {}", cleanUsername);
        }
    }

    private void seedDefaultRoles() {
        if (systemRoleRepository == null) {
            return;
        }
        createRoleIfAbsent(
                "ROLE_ADMIN",
                "Quản Trị Viên Toàn Sàn (Super Admin)",
                "Toàn quyền điều hành hệ thống, quản lý 7 Modules và 26 APIs",
                "SYSTEM",
                AdminPermissionCatalog.joinPermissionsText(AdminPermissionCatalog.allPermissionsList()),
                true
        );
        createRoleIfAbsent(
                "ROLE_OPERATOR",
                "Chủ Xe / Đối Tác Vận Tải",
                "Quản lý đội xe, tài xế, lập lịch chuyến xe và ví ký quỹ nhà xe",
                "OPERATOR",
                "",
                true
        );
        createRoleIfAbsent(
                "ROLE_CUSTOMER",
                "Khách Hàng Đặt Vé",
                "Tìm kiếm chuyến xe, chọn ghế, thanh toán và quản lý vé cá nhân",
                "CUSTOMER",
                "",
                true
        );
        createRoleIfAbsent(
                "ROLE_ACCOUNTANT",
                "Kế Toán & Đối Soát Tài Chính",
                "Chuyên trách báo cáo doanh thu, xử lý đối soát VNPay, giải ngân ví Escrow và hoa hồng nhà xe",
                "SYSTEM",
                String.join(",", List.of(
                        "PAYMENT_REPORT_VIEW", "PAYMENT_LIST_VIEW", "PAYMENT_RECONCILE_RESOLVE",
                        "WITHDRAWAL_VIEW", "WITHDRAWAL_APPROVE", "WITHDRAWAL_REJECT",
                        "OPERATOR_VIEW", "OPERATOR_COMMISSION_MANAGE",
                        "BOOKING_VIEW", "BOOKING_FORCE_REFUND"
                )),
                false
        );
        createRoleIfAbsent(
                "ROLE_CSKH",
                "CSKH & Điều Hành Vận Tải",
                "Hỗ trợ khách hàng, tra cứu/hoàn vé, duyệt nhà xe, kiểm duyệt banner và quản lý bến/chuyến xe",
                "SYSTEM",
                String.join(",", List.of(
                        "OPERATOR_VIEW", "OPERATOR_APPROVE_REJECT",
                        "BANNER_VIEW", "BANNER_CREATE_DELETE", "BANNER_APPROVE_REJECT",
                        "USER_VIEW", "USER_STATUS_LOCK", "USER_PASSWORD_RESET",
                        "BOOKING_VIEW", "BOOKING_FORCE_REFUND",
                        "STATION_VIEW", "STATION_CREATE_UPDATE_DELETE", "TRIP_MANAGE_CRUD"
                )),
                false
        );
    }

    private void createRoleIfAbsent(String code, String name, String description, String category, String permsText, boolean systemDefault) {
        if (!systemRoleRepository.existsByCode(code)) {
            SystemRoleJpaEntity role = new SystemRoleJpaEntity();
            role.setCode(code);
            role.setName(name);
            role.setDescription(description);
            role.setCategory(category);
            role.setPermissionsText(permsText);
            role.setSystemDefault(systemDefault);
            systemRoleRepository.save(role);
            log.info("[IDENTITY SEEDER] Seeded system role: {}", code);
        }
    }

    private void seedAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            UserJpaEntity admin = new UserJpaEntity();
            admin.setUsername("admin");
            admin.setEmail("admin@ticketbooking.com");
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setFullName("System Administrator");
            admin.setRole("ROLE_ADMIN");
            admin.setCustomRoleCode("ROLE_ADMIN");
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
            log.info("[IDENTITY SEEDER] Seeded default admin user");
        }
    }

    private void seedCustomerUser() {
        if (!userRepository.existsByUsername("customer1")) {
            UserJpaEntity customer = new UserJpaEntity();
            customer.setUsername("customer1");
            customer.setEmail("customer1@ticketbooking.com");
            customer.setPasswordHash(passwordEncoder.encode(customerPassword));
            customer.setFullName("Nguyen Van Customer");
            customer.setRole("ROLE_CUSTOMER");
            customer.setCustomRoleCode("ROLE_CUSTOMER");
            customer.setStatus(UserStatus.ACTIVE);
            userRepository.save(customer);
            log.info("[IDENTITY SEEDER] Seeded default customer user");
        }
    }

    private void seedOperatorUser() {
        if (!userRepository.existsByUsername("operator1")) {
            UserJpaEntity operator = new UserJpaEntity();
            operator.setUsername("operator1");
            operator.setEmail("operator1@ticketbooking.com");
            operator.setPasswordHash(passwordEncoder.encode(operatorPassword));
            operator.setFullName("Nguyen Van Operator");
            operator.setRole("ROLE_OPERATOR");
            operator.setCustomRoleCode("ROLE_OPERATOR");
            operator.setStatus(UserStatus.ACTIVE);
            userRepository.save(operator);
            log.info("[IDENTITY SEEDER] Seeded default operator user");
        }
    }
}
