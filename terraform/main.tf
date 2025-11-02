// main.tf
// ----------------------------------------------------------------------------
// Overview
// ----------------------------------------------------------------------------
// This configuration launches a single Amazon EC2 instance in your account's
// default VPC and subnets within the selected AWS region. It does the
// following:
//   1) Looks up the latest Ubuntu 24.04 LTS (x86_64) AMI published by Canonical
//      using safe filters (owner ID 099720109477 is the official Canonical ID).
//   2) Creates a minimal security group that allows inbound SSH (port 22)
//      from a configurable CIDR (by default, it's wide-open 0.0.0.0/0 — this
//      is convenient for demos but not recommended for production).
//   3) Launches a t2.micro instance (often Free Tier eligible) into the first
//      subnet of the default VPC, associates a public IP, and tags it.
//   4) Uses cloud-init user_data to install OpenJDK 21 (Java), git,
//      and writes a simple file so you can verify provisioning worked.
// ----------------------------------------------------------------------------

// ----------------------------------------------------------------------------
// Ubuntu LTS (Canonical owner 099720109477)
// ----------------------------------------------------------------------------
data "aws_ami" "ubuntu_2404" {
  most_recent = true
  owners      = ["099720109477"] // Canonical

  filter {
    name = "name"
    values = [
      "ubuntu/images/*ubuntu-noble-24.04-amd64-server-*",
    ]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
}

// Random suffix to avoid name conflicts when re-running
resource "random_string" "suffix" {
  length  = 5
  upper   = false
  numeric = true
  special = false
}

// Look up default VPC and subnets
data "aws_vpc" "default" { default = true }

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_security_group" "ssh" {
  // Minimal SG to allow SSH access to the instance + optional app port
  name        = "allow-ssh-java"
  description = "Allow SSH inbound and optional app port"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ingress_cidr_ssh]
  }

  // Optional app port 8080
  dynamic "ingress" {
    for_each = var.ingress_cidr_app == null ? [] : [var.ingress_cidr_app]
    content {
      description = "App 8080"
      from_port   = 8080
      to_port     = 8080
      protocol    = "tcp"
      cidr_blocks = [ingress.value]
    }
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "allow-ssh-java"
  }
}

resource "aws_instance" "this" {
  ami                         = data.aws_ami.ubuntu_2404.id
  instance_type               = "t3.micro"
  subnet_id                   = data.aws_subnets.default.ids[0]
  vpc_security_group_ids      = [aws_security_group.ssh.id]
  key_name                    = var.key_name
  associate_public_ip_address = true

  tags = {
    Name = var.instance_name
  }

  user_data = <<-EOF
  #cloud-config
  package_update: true
  package_upgrade: true
  packages:
    - git
    - ec2-instance-connect
    - wget
    - curl
    - openjdk-21-jdk
  runcmd:
    - echo "Hello from cloud-init" > /var/tmp/hello.txt
    - java -version | tee /var/tmp/java_version.txt
  EOF
}

// Elastic IP
resource "aws_eip" "this" {
  domain = "vpc"
  tags = { Name = "${var.instance_name}-eip" }
}

resource "aws_eip_association" "this" {
  instance_id   = aws_instance.this.id
  allocation_id = aws_eip.this.id
}
