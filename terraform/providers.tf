provider "aws" {
  region              = var.aws_region
  profile             = var.aws_profile == "" ? null : var.aws_profile
  allowed_account_ids = length(var.allowed_account_ids) == 0 ? null : var.allowed_account_ids

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Owner       = "wager-bidder"
    }
  }
}
