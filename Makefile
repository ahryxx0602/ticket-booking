# ==============================================================================
# TICKET BOOKING SYSTEM - UNIFIED DEVELOPER MAKEFILE
# ==============================================================================
SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

DEV_COMPOSE  := docker compose -f docker-compose.dev.yml
INT_COMPOSE  := docker compose -f docker-compose.yml
PROD_COMPOSE := docker compose -f docker-compose.prod.yml

.PHONY: help setup doctor infra infra-stop infra-status dev backend frontend \
        stop logs test test-backend test-frontend build integration prod \
        smoke-test clean reset-db

help: ## Hiển thị danh sách commands hỗ trợ
	@echo "=============================================================================="
	@echo "  TICKET BOOKING SYSTEM - DEVELOPER COMMAND REFERENCE"
	@echo "=============================================================================="
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z0-9_-]+:.*?##/ { printf "  \033[1;36m%-16s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo "=============================================================================="

setup: ## Chuẩn bị local development environment (env, npm ci, mvn test-compile)
	@bash ./scripts/setup.sh --setup

doctor: ## Kiểm tra prerequisites (Java 21, Maven, Node, Docker, WSL2)
	@bash ./scripts/setup.sh --doctor

infra: ## Khởi động MySQL và Redis trong Docker (Hybrid Dev Infrastructure)
	@$(DEV_COMPOSE) up -d --remove-orphans
	@bash ./scripts/healthcheck.sh --wait-infra 60

infra-stop: ## Dừng MySQL và Redis containers (bảo toàn dữ liệu)
	@$(DEV_COMPOSE) stop

infra-status: ## Kiểm tra trạng thái infrastructure containers
	@$(DEV_COMPOSE) ps

dev: ## Khởi động toàn bộ Hybrid Development Environment (Infra + Backend + Vite HMR)
	@bash ./scripts/dev.sh all --foreground

backend: ## Khởi động Backend Spring Boot local (kèm Multi-module Auto-Compile + DevTools)
	@bash ./scripts/dev.sh backend --foreground

frontend: ## Khởi động Frontend Vite Dev Server local (HMR)
	@bash ./scripts/dev.sh frontend --foreground

stop: ## Dừng các tiến trình Development Environment an toàn (bảo toàn volumes)
	@bash ./scripts/dev-stop.sh

logs: ## Xem logs của MySQL và Redis containers
	@$(DEV_COMPOSE) logs -f

test: test-backend test-frontend ## Chạy toàn bộ automated tests (Backend + Frontend)

test-backend: ## Chạy Backend Maven unit & integration tests
	@cd backend && mvn test -B

test-frontend: ## Chạy Frontend Vitest unit tests
	@cd frontend && npm run test:unit

build: ## Build Production Docker images cho Backend và Frontend
	@$(INT_COMPOSE) build

integration: ## Khởi động Full Docker Integration Environment (4 containers)
	@bash ./scripts/dev-stop.sh
	@$(INT_COMPOSE) up --build -d

prod: ## Khởi động Production containers (yêu cầu biến môi trường prod hợp lệ)
	@$(PROD_COMPOSE) up --build -d

smoke-test: ## Kiểm tra nhanh sức khỏe hệ thống (Infra + Backend API + Frontend Proxy)
	@bash ./scripts/smoke-test.sh

clean: ## Dọn build artifacts an toàn (không xóa database volumes)
	@bash ./scripts/dev-stop.sh
	@cd backend && mvn clean -q
	@rm -rf frontend/dist .dev-logs/*.log .dev-logs/*.classpath .dev-logs/*.sha256

reset-db: ## Reset development database (Yêu cầu xác nhận [y/N])
	@read -r -p "CẢNH BÁO: Thao tác này sẽ xóa toàn bộ dữ liệu trong mysql_dev_data và nạp lại init.sql. Tiếp tục? [y/N] " ans; \
	if [[ "$$ans" =~ ^[Yy]$$ ]]; then \
		$(DEV_COMPOSE) down -v; \
		$(DEV_COMPOSE) up -d; \
		bash ./scripts/healthcheck.sh --wait-infra 60; \
		echo "[OK] Development database đã được khởi tạo lại từ environment/init.sql."; \
	else \
		echo "[ABORTED] Đã hủy thao tác reset-db. Dữ liệu được giữ nguyên."; \
	fi
