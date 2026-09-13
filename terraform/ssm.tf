resource "aws_ssm_parameter" "db_password" {
  name  = "/${local.name}/db/password"
  type  = "SecureString"
  value = local.db_password
}

resource "aws_ssm_parameter" "admin_password" {
  name  = "/${local.name}/app/admin-password"
  type  = "SecureString"
  value = var.admin_password
}

resource "aws_ssm_parameter" "api_url" {
  name  = "/${local.name}/app/api-url"
  type  = "String"
  value = local.enable_https ? "https://${aws_lb.app.dns_name}" : "http://${aws_lb.app.dns_name}"
}
