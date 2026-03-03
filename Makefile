# sen-agent Makefile - 标准化交付

.PHONY: help build run clean docker-build docker-run docker-stop test

help:
	@echo "sen-agent Makefile"
	@echo ""
	@echo "Usage:"
	@echo "  make build        - Build the project"
	@echo "  make run          - Run the application"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make docker-build - Build Docker image"
	@echo "  make docker-run   - Run Docker container"
	@echo "  make docker-stop  - Stop Docker container"
	@echo "  make test        - Run tests"

build:
	@mvn clean package -DskipTests

run:
	@java -jar target/sen-agent-*.jar

clean:
	@mvn clean
	@rm -rf target/

docker-build:
	@docker build -t sen-agent:latest .

docker-run:
	@docker run -d \
		--name sen-agent \
		-p 8080:8080 \
		-e DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY} \
		-v /tmp/sen-agent-files:/tmp/sen-agent-files \
		sen-agent:latest

docker-stop:
	@docker stop sen-agent || true
	@docker rm sen-agent || true

test:
	@mvn test

# Docker Compose
docker-compose-up:
	@docker-compose up -d

docker-compose-down:
	@docker-compose down

# 部署到服务器
deploy:
	@echo "Deploying to server..."
	@scp target/sen-agent-*.jar root@124.223.177.67:/opt/sen-agent/
	@ssh root@124.223.177.67 "cd /opt/sen-agent && systemctl restart sen-agent"

# 查看日志
logs:
	@docker logs -f sen-agent

# 状态
status:
	@docker ps | grep sen-agent || echo "Container not running"
