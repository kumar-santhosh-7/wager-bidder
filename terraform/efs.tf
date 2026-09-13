resource "aws_efs_file_system" "uploads" {
  creation_token = "${local.name}-uploads"
  encrypted      = true

  throughput_mode = "bursting"

  lifecycle_policy {
    transition_to_ia = "AFTER_30_DAYS"
  }

  tags = {
    Name = "${local.name}-uploads"
  }
}

resource "aws_efs_mount_target" "uploads" {
  count = 2

  file_system_id  = aws_efs_file_system.uploads.id
  subnet_id       = aws_subnet.public[count.index].id
  security_groups = [aws_security_group.efs.id]
}
