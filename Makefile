.PHONY: check-env jar docker-build docker-up docker-down docker-reset docker-restart docker-clean-images docker-logs docker-ps

check-env:
	@test -f .env || (echo ".env missing. Copy .env.example to .env and fill it in first."; exit 1)

jar:
	mvn clean package

docker-build: check-env jar
	docker build -t ourmusic-backend .
	docker compose build frontend

docker-up: check-env docker-build
	docker compose up -d

docker-down:
	docker compose down

docker-reset:
	docker compose down -v

docker-restart: check-env docker-build
	docker compose up -d --force-recreate backend frontend
	$(MAKE) docker-clean-images

docker-clean-images:
	docker image prune -f --filter "label=me.tejzs.project=ourmusic"

docker-logs: check-env
	docker compose logs -f backend

docker-ps: check-env
	docker compose ps
