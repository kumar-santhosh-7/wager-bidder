# wager-bidder

Spring Boot API for the Ledger Bid mobile app. Hosted in a **dedicated AWS account**, separate from vibuthar-academy (`649058762344`).

## Layout

| Path | Purpose |
|---|---|
| `ledger-bid-api/` | Java 17 / Spring Boot 3.4 API, Dockerfile, local Compose |
| `terraform/` | VPC, ALB, ECS on EC2, ECR, RDS MySQL, EFS uploads, GitHub OIDC (local state) |
| `.github/workflows/deploy-ecs.yml` | Build image, push ECR, deploy ECS |

## Local API

See [ledger-bid-api/README.md](ledger-bid-api/README.md) for XAMPP/MySQL.

```bash
cd ledger-bid-api
docker compose up --build
```

http://localhost:8090/api/health

## AWS

Follow [docs/aws-deployment.md](docs/aws-deployment.md). Use AWS CLI profile `wager-bidder`. Do not reuse vibuthar-academy credentials, Terraform state, or IAM users.
