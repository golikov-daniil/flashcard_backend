// versions.tf
// ----------------------------------------------------------------------------
// Terraform and provider version constraints. These help ensure consistent
// behavior across machines and prevent accidental breaking changes when new
// major versions are released.
// ----------------------------------------------------------------------------
terraform {
  // Require Terraform CLI 1.3.0 or newer. Adjust as needed if you rely on
  // features from newer versions.
  required_version = ">= 1.3.0"
  required_providers {
    aws = {
      // Use the official AWS provider maintained by HashiCorp.
      source = "hashicorp/aws"
      // "~> 5.0" allows any 5.x release but prevents automatic jump to 6.0.
      // Pin or widen as your needs evolve.
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  // Configure the AWS provider to target the chosen region
  region = var.region
}
