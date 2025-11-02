// variables.tf
// ----------------------------------------------------------------------------
// Input variables for this configuration. These let you customize behavior
// without editing the resources directly. Each variable has a description and
// type; some include sensible defaults for quick demos.
// ----------------------------------------------------------------------------
variable "region" {
  // Which AWS region to operate in. Must match a valid region code like
  // "us-west-2", "us-east-1", etc. This is consumed by the AWS provider in
  // versions.tf.
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}

variable "instance_name" {
  // Value used for the EC2 Name tag so it's easy to spot in the console.
  description = "Name tag for the instance"
  type        = string
  default     = "ec2-java"
}

variable "key_name" {
  // Name of an EXISTING EC2 key pair in the selected region. Terraform will
  // attach this key pair to the instance so you can SSH. Create one first in
  // the AWS console or CLI if you don't already have it.
  // Example: "my-macbook-key"
  description = "Existing EC2 key pair name for SSH"
  type        = string
  # validation {
  #   // Prevent common mistake of providing the local private key filename.
  #   // The value here must be the EC2 key pair NAME, not a path ending in .pem.
  #   condition     = !can(regex("\\.pem$", var.key_name))
  #   error_message = "Provide the EC2 key pair NAME (e.g., 'my-key'), not the .pem filename."
  # }
  default = "ec2-demo"
}

variable "ingress_cidr_ssh" {
  // CIDR block permitted to access SSH (TCP/22). For demos, 0.0.0.0/0 is
  // convenient but unsafe. In real use, restrict to your IP, e.g.
  //   203.0.113.42/32
  // You can determine your IP by visiting https://checkip.amazonaws.com
  description = "CIDR allowed to SSH"
  type        = string
  default     = "0.0.0.0/0"
}

variable "ingress_cidr_app" {
  // Optional: allow inbound App traffic (TCP/8080) from this CIDR. Leave null to keep closed.
  description = "Optional CIDR allowed to access app port 8080 (e.g., your_ip/32). Leave null to keep closed."
  type        = string
  default     = null
}