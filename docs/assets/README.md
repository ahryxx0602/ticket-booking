# Assets & Diagram Source Files

## 1. Cơ Chế Hiển Thị Sơ Đồ (Docs-as-Code)
Tất cả các sơ đồ kỹ thuật quan trọng của hệ thống **Ticket Booking Platform** đã được tích hợp dưới dạng mã nguồn **Mermaid.js** ngay bên trong các tài liệu markdown (`01` đến `05`):
- Khi xem tài liệu trên **GitHub**, **GitLab**, **VS Code** (với Markdown Preview), **IntelliJ IDEA**, **Notion** hoặc **Obsidian**, các sơ đồ này được tự động vẽ thành đồ họa tương tác chất lượng cao mà **không cần tải các file ảnh tĩnh**.

---

## 2. Xuất Ra Ảnh Tĩnh (PNG / SVG) Phục Vụ Báo Cáo & In Ấn

Nếu bạn cần file ảnh `.png` hoặc `.svg` để chèn vào báo cáo Word hoặc Slide thuyết trình đồ án, bạn có thể sử dụng một trong các cách sau mà không cần vẽ lại từ đầu:

### Cách 1: Sử dụng Mermaid Live Editor (Nhanh nhất - Không cần cài đặt)
1. Mở bất kỳ file tài liệu `.md` trong thư mục `docs/`.
2. Copy khối code trong thẻ ````mermaid ... ````.
3. Truy cập [https://mermaid.live](https://mermaid.live) và dán code vào.
4. Nhấn nút **Download PNG** hoặc **Download SVG** ở góc dưới bên phải.

---

### Cách 2: Sử dụng Mermaid CLI (Xuất hàng loạt qua dòng lệnh)
```bash
# Cài đặt Mermaid CLI toàn cục (yêu cầu Node.js)
npm install -g @mermaid-js/mermaid-cli

# Xuất sơ đồ từ markdown sang file ảnh PNG
mmdc -i docs/05-architecture-fsm.md -o docs/assets/architecture-fsm.png
```

---

## 3. Danh Mục Sơ Đồ Đã Tạo Sẵn

| Tên sơ đồ | Loại sơ đồ | Vị trí trong tài liệu |
| :--- | :--- | :--- |
| **Bounded Contexts Map** | Flowchart Architecture | [01-business-requirements.md](../01-business-requirements.md) |
| **Domain Context Mapping** | Class / Package Diagram | [02-domain-model.md](../02-domain-model.md) |
| **Domain Lifecycle Overview** | Sequence Diagram | [02-domain-model.md](../02-domain-model.md) |
| **Database ERD Diagram** | Entity-Relationship (ERD) | [03-database-design.md](../03-database-design.md) |
| **Internal Domain Events** | Event Flowchart | [04-api-and-events.md](../04-api-and-events.md) |
| **Hexagonal Clean Architecture** | Layer Architecture Diagram | [05-architecture-fsm.md](../05-architecture-fsm.md) |
| **Booking State Machine (FSM)** | State Diagram (v2) | [05-architecture-fsm.md](../05-architecture-fsm.md) |
| **Hold Seat Redisson Lock Flow** | Concurrency Sequence Diagram | [05-architecture-fsm.md](../05-architecture-fsm.md) |
| **VNPay HMAC & Webhook Flow** | Payment Sequence Diagram | [05-architecture-fsm.md](../05-architecture-fsm.md) |
