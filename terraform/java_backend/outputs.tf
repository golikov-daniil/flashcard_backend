// outputs.tf
// ----------------------------------------------------------------------------
// Outputs make it easier to use values from this configuration after apply.
// You’ll see these printed at the end of `terraform apply` and you can also
// reference them from parent modules if you wrap this as a module.
// ----------------------------------------------------------------------------
output "instance_id" {
  // The unique ID of the created EC2 instance
  value = aws_instance.this.id
}