// variables.tf

variable "region" {
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
  description = "Existing EC2 key pair name for SSH"
  type        = string
  default = "ec2-demo"
}

variable "ingress_cidr_ssh" {
  description = "CIDR allowed to SSH"
  type        = string
  default     = "0.0.0.0/0"
}

variable "iam_role_name" {
  description = "Name of an existing IAM role to attach to the EC2 instance via an instance profile"
  type        = string
  default     = "ec2-java-backend-role"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.micro"
}
# ALB variables

variable "target_group_arn" {
  description = "ARN of an existing ALB target group for this instance"
  type        = string
}

variable "alb_security_group_id" {
  description = "Security group id of the ALB that is allowed to reach this instance on HTTP"
  type        = string
}

