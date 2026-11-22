import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ==============================================================================
// METRICS ĐO ĐẠC NÂNG CAO
// ==============================================================================
const successfulHoldCount = new Counter('successful_hold_seats');
const conflictCount = new Counter('conflict_400_seats');
const errorRate = new Rate('system_500_errors');
const holdDurationTrend = new Trend('hold_seat_response_time_ms');

// ==============================================================================
// CẤU HÌNH THỜI GIAN VÀ SỐ LƯỢNG NGƯỜI DÙNG ẢO (VUs)
// Mô phỏng 1000 người dùng ảo đồng thời lao vào tranh 2 chiếc ghế VIP (12A, 12B)
// ==============================================================================
export const options = {
    scenarios: {
        flash_sale_concurrency: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '3s', target: 200 },   // 3s đầu: vọt lên 200 users
                { duration: '5s', target: 1000 },  // 5s tiếp theo: đỉnh điểm 1000 users đồng thời
                { duration: '2s', target: 0 },     // 2s cuối: hạ tải
            ],
            gracefulRampDown: '0s',
        },
    },
    thresholds: {
        'system_500_errors': ['rate<0.01'],             // Tỷ lệ lỗi 500 phải < 1% (server không được sập)
        'successful_hold_seats': ['count<=1'],          // CHỈ TỐI ĐA 1 NGƯỜI GIỮ ĐƯỢC GHẾ (Zero Overbooking)
        'http_req_duration': ['p(95)<500'],             // 95% request phản hồi dưới 500ms
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Tài khoản khách hàng mẫu
const CUSTOMER_USER = {
    username: 'customer1',
    password: 'customer_dev_password',
};

// ==============================================================================
// SETUP: Đăng nhập lấy JWT Token trước khi bắt đầu bài test
// ==============================================================================
export function setup() {
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify(CUSTOMER_USER),
        { headers: { 'Content-Type': 'application/json' } }
    );

    check(loginRes, {
        'Login setup status is 200': (r) => r.status === 200,
    });

    const body = JSON.parse(loginRes.body);
    const token = body.data ? body.data.accessToken : '';

    return { token: token };
}

// ==============================================================================
// DEFAULT FUNCTION: Vòng lặp kịch bản gửi request đồng thời
// ==============================================================================
export default function (data) {
    const tripId = 1; // Chuyến xe số 1
    const seatNumbers = ['12A', '12B']; // 2 ghế VIP mục tiêu tranh giành

    const payload = JSON.stringify({
        tripId: tripId,
        seatNumbers: seatNumbers,
    });

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.token}`,
    };

    const startTime = new Date().getTime();
    const res = http.post(`${BASE_URL}/api/v1/bookings/hold`, payload, { headers: headers });
    const elapsed = new Date().getTime() - startTime;
    holdDurationTrend.add(elapsed);

    // Kiểm tra kết quả phản hồi
    if (res.status === 200) {
        successfulHoldCount.add(1);
        errorRate.add(0);
    } else if (res.status === 400) {
        // 400 Bad Request: Ghế đã bị giữ hoặc đang có người khác thao tác -> KẾT QUẢ ĐÚNG THEO BẤT BIẾN
        conflictCount.add(1);
        errorRate.add(0);
    } else {
        // Lỗi 500 hoặc lỗi mạng bất thường
        errorRate.add(1);
    }

    check(res, {
        'Response is either 200 OK or 400 Bad Request': (r) => r.status === 200 || r.status === 400,
    });

    sleep(0.05); // Ngắt nghỉ ngắn 50ms giữa các lần bắn
}

// ==============================================================================
// TEARDOWN: Tóm tắt kết quả
// ==============================================================================
export function teardown() {
    console.log('=== FLASH SALE CONCURRENCY TEST FINISHED ===');
}
