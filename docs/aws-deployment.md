# Deploy wager-bidder to its own AWS account

This stack is **not** shared with vibuthar-academy.

| | vibuthar-academy | wager-bidder |
|---|---|---|
| AWS account | `649058762344` | a **new** account |
| CLI profile | `default` | `wager-bidder` |
| VPC CIDR | `10.0.0.0/16` | `10.20.0.0/16` |
| Project prefix | `ias-academy` / `vibuthar-fe` | `wager-bidder` |
| Terraform state | local `terraform.tfstate` | local `terraform.tfstate` (gitignored) |

Terraform refuses to apply if the caller identity is `649058762344`.

## What gets created

```
Internet
   │
   ▼
ALB (HTTP 80 → HTTPS 443, host api.wager-game.online)
   │
   ▼
ECS on EC2 (t3.micro)  →  Spring Boot :8090
   ├── RDS MySQL 8 (ledger_bid, private subnet)
   └── EFS  (/application/uploads)
```

GitHub Actions authenticates with an IAM user access key (same pattern as vibuthar-academy).

## 1. New AWS account and CLI profile

1. Create a dedicated AWS account (Organizations member or standalone).
2. Create an IAM user or SSO role that can run Terraform.
3. Add a profile that is **not** the vibuthar default:

```ini
# ~/.aws/config
[profile wager-bidder]
region = ap-south-1

# ~/.aws/credentials
[wager-bidder]
aws_access_key_id = ...
aws_secret_access_key = ...
```

4. Confirm:

```bash
aws sts get-caller-identity --profile wager-bidder
```

The account ID must not be `649058762344`. Put that new ID in `allowed_account_ids` in `terraform.tfvars`.

State is stored locally in `terraform/terraform.tfstate` (gitignored). Do not commit it.

## 2. Application stack

```bash
cd terraform
copy terraform.tfvars.example terraform.tfvars
# set admin_password and allowed_account_ids
terraform init
terraform apply
```

Save this output for GitHub:

```bash
terraform output -raw github_actions_access_key_id
terraform output -raw github_actions_secret_access_key
terraform output -raw api_url
```

The first `apply` registers an ECS task that points at `:latest`. The service becomes healthy after the first image push.

## 3. GitHub Actions

Repo **Settings → Secrets and variables → Actions → Secrets**:

| Secret | Value |
|---|---|
| `AWS_ACCESS_KEY_ID` | `terraform output -raw github_actions_access_key_id` |
| `AWS_SECRET_ACCESS_KEY` | `terraform output -raw github_actions_secret_access_key` |

Deploys only when you run **Actions → Deploy to ECS → Run workflow** and enter the branch name. Pushes do not deploy.

## 4. Point the Expo app at the API

```
EXPO_PUBLIC_API_URL=https://api.wager-game.online/api
```

HTTP on the ALB redirects to HTTPS. The 443 listener forwards `api.wager-game.online` to ECS and returns 503 for other hosts.

Health check: `https://api.wager-game.online/api/health`

On an empty database, Terraform creates one admin from `admin_username` / `admin_password`. Demo users (`arun`, `meera`, …) are **not** created in prod.

## Local Docker

```bash
cd ledger-bid-api
docker compose up --build
```

API: http://localhost:8090/api/health
