package com.ticket.identity.domain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Danh mục chuẩn 7 Modules & 26 API Permissions của hệ thống Quản trị (/admin).
 * Thuần Domain (0% Spring/JPA).
 */
public final class AdminPermissionCatalog {

    private AdminPermissionCatalog() {
    }

    public record PermissionItem(
            String code,
            String name,
            String httpMethod,
            String apiPattern,
            String description,
            boolean viewPermission
    ) {}

    public record ModuleCatalog(
            String moduleCode,
            String moduleName,
            String routePath,
            String viewPermissionCode,
            List<PermissionItem> permissions
    ) {}

    public static final List<ModuleCatalog> MODULES = List.of(
            new ModuleCatalog(
                    "MOD_DASHBOARD",
                    "1. Tổng quan & Đối soát",
                    "/admin",
                    "PAYMENT_REPORT_VIEW",
                    List.of(
                            new PermissionItem("PAYMENT_REPORT_VIEW", "Xem báo cáo KPI doanh thu", "GET", "/api/v1/admin/payments/reports", "Xem 4 thẻ KPI GMV, hoa hồng, tổng đơn vé và nhà xe", true),
                            new PermissionItem("PAYMENT_LIST_VIEW", "Xem danh sách giao dịch & đối soát", "GET", "/api/v1/admin/payments", "Xem bảng lịch sử giao dịch và cảnh báo lệch số dư VNPay", true),
                            new PermissionItem("PAYMENT_RECONCILE_RESOLVE", "Xử lý cảnh báo đối soát", "POST", "/api/v1/admin/payments/*/reconcile", "Xác nhận đã xử lý cảnh báo lệch đối soát VNPay ↔ Escrow", false)
                    )
            ),
            new ModuleCatalog(
                    "MOD_OPERATORS",
                    "2. Nhà xe & Hoa hồng",
                    "/admin/operators",
                    "OPERATOR_VIEW",
                    List.of(
                            new PermissionItem("OPERATOR_VIEW", "Xem danh sách đối tác nhà xe", "GET", "/api/v1/admin/partners/operators*", "Hiển thị menu Nhà xe & xem danh sách hồ sơ đối tác vận tải", true),
                            new PermissionItem("OPERATOR_CREATE_UPDATE", "Thêm / Sửa / Xóa hồ sơ nhà xe", "POST|PUT|DELETE", "/api/v1/admin/partners/operators*", "Tạo mới, cập nhật thông tin hoặc xóa hồ sơ đối tác nhà xe", false),
                            new PermissionItem("OPERATOR_APPROVE_REJECT", "Phê duyệt / Từ chối nhà xe", "PUT", "/api/v1/admin/partners/operators/*/approve|reject", "Thẩm định duyệt hoặc từ chối hồ sơ đăng ký nhà xe mới", false),
                            new PermissionItem("OPERATOR_SUSPEND", "Đình chỉ / Mở khóa nhà xe", "PUT", "/api/v1/admin/partners/operators/*/suspend", "Tạm đình chỉ hoặc kích hoạt lại hoạt động của nhà xe", false),
                            new PermissionItem("OPERATOR_COMMISSION_MANAGE", "Cấu hình hoa hồng 2 cấp", "PUT", "/api/v1/admin/partners/operators/*/commission", "Thiết lập tỷ lệ chiết khấu hoa hồng riêng cho từng nhà xe", false)
                    )
            ),
            new ModuleCatalog(
                    "MOD_WITHDRAWALS",
                    "3. Giải ngân Ví Escrow",
                    "/admin/withdrawals",
                    "WITHDRAWAL_VIEW",
                    List.of(
                            new PermissionItem("WITHDRAWAL_VIEW", "Xem danh sách lệnh rút tiền", "GET", "/api/v1/admin/partners/withdrawals*", "Hiển thị menu Giải ngân & xem các yêu cầu rút tiền ví ký quỹ", true),
                            new PermissionItem("WITHDRAWAL_APPROVE", "Phê duyệt giải ngân", "PUT", "/api/v1/admin/partners/withdrawals/*/approve", "Duyệt chi chuyển khoản và khấu trừ số dư Locked Balance", false),
                            new PermissionItem("WITHDRAWAL_REJECT", "Từ chối lệnh rút tiền", "PUT", "/api/v1/admin/partners/withdrawals/*/reject", "Từ chối lệnh rút và hoàn trả số dư về ví khả dụng của nhà xe", false)
                    )
            ),
            new ModuleCatalog(
                    "MOD_BANNERS",
                    "4. Banner & Truyền thông",
                    "/admin/banners",
                    "BANNER_VIEW",
                    List.of(
                            new PermissionItem("BANNER_VIEW", "Xem danh sách banner toàn sàn", "GET", "/api/v1/admin/partners/banners*", "Hiển thị menu Banner & xem danh sách chiến dịch quảng cáo", true),
                            new PermissionItem("BANNER_CREATE_DELETE", "Tạo / Sửa / Xóa banner sàn", "POST|PUT|DELETE", "/api/v1/admin/partners/banners*", "Đăng mới, chỉnh sửa hoặc xóa banner truyền thông của sàn", false),
                            new PermissionItem("BANNER_APPROVE_REJECT", "Phê duyệt / Gỡ bỏ banner nhà xe", "PUT", "/api/v1/admin/partners/banners/*/approve|reject", "Duyệt hiển thị hoặc từ chối banner do nhà xe gửi lên", false)
                    )
            ),
            new ModuleCatalog(
                    "MOD_IAM",
                    "5. Người dùng & Phân quyền RBAC",
                    "/admin/users-bookings",
                    "USER_VIEW",
                    List.of(
                            new PermissionItem("USER_VIEW", "Xem 3 phân mục người dùng", "GET", "/api/v1/admin/users*", "Xem danh sách Khách hàng (Customer), Chủ xe (Operator) và Hệ thống (Admin)", true),
                            new PermissionItem("USER_CREATE_UPDATE_DELETE", "Tạo / Sửa / Xóa tài khoản", "POST|PUT|DELETE", "/api/v1/admin/users*", "Tạo mới, cập nhật hồ sơ hoặc xóa tài khoản người dùng", false),
                            new PermissionItem("USER_STATUS_LOCK", "Khóa / Mở khóa tài khoản", "PATCH", "/api/v1/admin/users/*/status", "Khóa hoặc kích hoạt lại trạng thái hoạt động của tài khoản", false),
                            new PermissionItem("USER_PASSWORD_RESET", "Đặt lại mật khẩu người dùng", "POST", "/api/v1/admin/users/*/reset-password", "Cấp lại mật khẩu mới cho tài khoản người dùng", false),
                            new PermissionItem("USER_ROLE_ASSIGN", "Gán / Thay đổi Role cho người dùng", "PATCH", "/api/v1/admin/users/*/role", "Gán hoặc chuyển đổi Role cho tài khoản người dùng", false),
                            new PermissionItem("ROLE_MANAGE_CRUD", "Quản lý Role & Phân quyền API", "GET|POST|PUT|DELETE", "/api/v1/admin/roles*", "Tạo mới, sửa ma trận 26 quyền API và xóa Custom Role hệ thống", true)
                    )
            ),
            new ModuleCatalog(
                    "MOD_BOOKINGS",
                    "6. Đơn vé & Hoàn tiền Admin",
                    "/admin/users-bookings",
                    "BOOKING_VIEW",
                    List.of(
                            new PermissionItem("BOOKING_VIEW", "Xem danh sách đơn vé toàn sàn", "GET", "/api/v1/admin/bookings*", "Tra cứu toàn bộ đơn đặt vé của hành khách trên hệ thống", true),
                            new PermissionItem("BOOKING_FORCE_REFUND", "Hoàn tiền cưỡng chế (Admin Refund)", "POST", "/api/v1/admin/bookings/*/refund", "Thực thi lệnh hoàn tiền 100% và thu hồi ví ký quỹ khi có khiếu nại", false)
                    )
            ),
            new ModuleCatalog(
                    "MOD_SETTINGS",
                    "7. Bến xe, Chuyến xe & Cấu hình",
                    "/admin/settings",
                    "STATION_VIEW",
                    List.of(
                            new PermissionItem("STATION_VIEW", "Xem danh mục bến xe liên tỉnh", "GET", "/api/v1/admin/stations*", "Hiển thị menu Cấu hình & xem danh sách nhà ga, bến xe", true),
                            new PermissionItem("STATION_CREATE_UPDATE_DELETE", "Thêm / Sửa / Xóa bến xe", "POST|PUT|DELETE", "/api/v1/admin/stations*", "Thêm mới, cập nhật hoặc xóa bến xe liên tỉnh", false),
                            new PermissionItem("TRIP_MANAGE_CRUD", "Quản lý chuyến xe (Tạo/Sửa/Hủy/Xóa)", "GET|POST|PUT|PATCH|DELETE", "/api/v1/admin/trips*", "Tạo chuyến xe, cập nhật giá/lịch trình, hủy hoặc xóa chuyến xe", true),
                            new PermissionItem("SYSTEM_SETTINGS_MANAGE", "Quản lý cấu hình tham số toàn sàn", "GET|PUT", "/api/v1/admin/settings & /commission/global", "Cập nhật hoa hồng mặc định, TTL khóa ghế Redis và tham số vận hành", true)
                    )
            )
    );

    public static final Set<String> ALL_PERMISSION_CODES;
    public static final Map<String, PermissionItem> PERMISSION_BY_CODE;

    static {
        Map<String, PermissionItem> map = new LinkedHashMap<>();
        for (ModuleCatalog module : MODULES) {
            for (PermissionItem item : module.permissions()) {
                map.put(item.code(), item);
            }
        }
        PERMISSION_BY_CODE = Collections.unmodifiableMap(map);
        ALL_PERMISSION_CODES = Collections.unmodifiableSet(map.keySet());
    }

    public static List<String> allPermissionsList() {
        return List.copyOf(ALL_PERMISSION_CODES);
    }

    public static boolean isValidPermission(String code) {
        return code != null && ALL_PERMISSION_CODES.contains(code.trim().toUpperCase());
    }

    public static List<String> sanitizePermissions(List<String> rawPermissions) {
        if (rawPermissions == null || rawPermissions.isEmpty()) {
            return Collections.emptyList();
        }
        return rawPermissions.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(p -> p.trim().toUpperCase())
                .filter(ALL_PERMISSION_CODES::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<String> parsePermissionsText(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        if ("*".equals(text.trim()) || "ALL".equalsIgnoreCase(text.trim())) {
            return allPermissionsList();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && ALL_PERMISSION_CODES.contains(s))
                .distinct()
                .collect(Collectors.toList());
    }

    public static String joinPermissionsText(List<String> permissions) {
        List<String> clean = sanitizePermissions(permissions);
        return String.join(",", clean);
    }
}
