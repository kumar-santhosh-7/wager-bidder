resource "terraform_data" "existing_db" {
  lifecycle {
    precondition {
      condition = var.create_rds || (
        var.existing_db_url != "" &&
        var.existing_db_username != "" &&
        var.existing_db_password != ""
      )
      error_message = "When create_rds is false, set existing_db_url, existing_db_username, and existing_db_password."
    }
  }
}

resource "terraform_data" "separate_account" {
  lifecycle {
    precondition {
      condition     = data.aws_caller_identity.current.account_id != "649058762344"
      error_message = "Refusing to apply wager-bidder into the vibuthar-academy AWS account (649058762344). Use a dedicated account and aws_profile = \"wager-bidder\"."
    }
  }
}
