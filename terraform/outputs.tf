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

output "elastic_ip" {
  // The static Elastic IP address assigned to the instance
  description = "Elastic IP address (static, persists across restarts)"
  value       = aws_eip_association.this.public_ip
}

output "ssh_command" {
  // A convenience string you can copy/paste to connect. Replace the path with
  // your actual private key file that corresponds to var.key_name.
  // Username varies by base image:
  //  - Amazon Linux 2023: ec2-user
  //  - Ubuntu (22.04/24.04): ubuntu
  description = "Convenience SSH command (Ubuntu username is 'ubuntu')"
  value       = "ssh -i /path/to/your.pem ubuntu@${aws_eip_association.this.public_ip}"
}
