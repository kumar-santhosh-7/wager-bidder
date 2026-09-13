variable "aws_region" {
  description = "AWS region. Mumbai = ap-south-1."
  type        = string
  default     = "ap-south-1"
}

variable "aws_profile" {
  description = "Named CLI profile for the dedicated wager-bidder AWS account. Leave empty to use the default credential chain."
  type        = string
  default     = "wager-bidder"
}

variable "allowed_account_ids" {
  description = "Optional lock so this stack cannot be applied to another account (for example vibuthar-academy 649058762344)."
  type        = list(string)
  default     = []
}

variable "project_name" {
  description = "Name prefix for ECR, ECS, ALB, RDS, and related resources."
  type        = string
  default     = "wager-bidder"
}

variable "environment" {
  description = "Environment tag. Use a dedicated AWS account per environment when possible."
  type        = string
  default     = "prod"
}

variable "vpc_cidr" {
  description = "VPC CIDR. 10.20.0.0/16 avoids collision with vibuthar-academy (10.0.0.0/16) if this is ever applied by mistake in the same account."
  type        = string
  default     = "10.20.0.0/16"
}

variable "ecs_cpu" {
  description = "Task CPU units for ECS on EC2. 512 leaves headroom on t3.micro (2 vCPU)."
  type        = string
  default     = "512"
}

variable "ecs_memory" {
  description = "Task memory (MiB). t3.micro has 1024 MiB total; 512 leaves room for the OS and ECS agent."
  type        = string
  default     = "512"
}

variable "ecs_instance_type" {
  description = "EC2 instance type for ECS container instances. t3.micro is Free Tier eligible."
  type        = string
  default     = "t3.micro"
}

variable "ecs_asg_min_size" {
  type    = number
  default = 1
}

variable "ecs_asg_max_size" {
  type    = number
  default = 1
}

variable "ecs_asg_desired_capacity" {
  type    = number
  default = 1
}

variable "desired_count" {
  description = "ECS service desired task count."
  type        = number
  default     = 1
}

variable "container_port" {
  description = "App port from application-prod.properties."
  type        = number
  default     = 8090
}

variable "create_rds" {
  description = "Create RDS MySQL 8. Set false to use an existing database."
  type        = bool
  default     = true
}

variable "db_name" {
  type    = string
  default = "ledger_bid"
}

variable "db_username" {
  description = "RDS master username. RDS MySQL does not allow 'root'."
  type        = string
  default     = "ledgerbid"
}

variable "db_instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "db_backup_retention_period" {
  description = "RDS automated backup retention in days. 0 disables automated backups (Free Tier friendly)."
  type        = number
  default     = 0
}

variable "existing_db_url" {
  type    = string
  default = ""
}

variable "existing_db_username" {
  type    = string
  default = ""
}

variable "existing_db_password" {
  type      = string
  default   = ""
  sensitive = true
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN in ap-south-1. Empty = HTTP only on port 80."
  type        = string
  default     = "arn:aws:acm:ap-south-1:701179922860:certificate/bfa8bee5-de80-48d8-8199-6f03a6d4210b"
}

variable "api_hostname" {
  description = "Public API hostname. HTTPS listener forwards this host to the target group; other hosts get 503."
  type        = string
  default     = "api.wager-game.online"
}

variable "alb_ssl_policy" {
  description = "ALB HTTPS SSL policy. Matches the listener created in the console."
  type        = string
  default     = "ELBSecurityPolicy-TLS13-1-2-Res-PQ-2025-09"
}

variable "admin_username" {
  description = "Initial admin username created on an empty database."
  type        = string
  default     = "admin"
}

variable "admin_password" {
  description = "Initial admin password. Stored in SSM and injected into ECS."
  type        = string
  sensitive   = true
}

variable "admin_name" {
  type    = string
  default = "House Admin"
}

# Unused. Kept so an existing terraform.tfvars still applies after OIDC was removed.
variable "github_repository" {
  type    = string
  default = "kumar-santhosh-7/wager-bidder"
}

variable "create_github_oidc_provider" {
  type    = bool
  default = true
}

variable "spring_jpa_hibernate_ddl_auto" {
  description = "Hibernate ddl-auto for the first deploy. update creates tables on a new RDS instance."
  type        = string
  default     = "update"
}

variable "log_retention_days" {
  type    = number
  default = 14
}
