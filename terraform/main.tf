// main.tf

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


data "aws_iam_role" "existing" {
  name = var.iam_role_name
}


resource "aws_iam_instance_profile" "this" {
  name = "${var.iam_role_name}-profile-${random_string.suffix.result}"
  role = data.aws_iam_role.existing.name
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
  name        = "java-backend-sg"
  description = "Allow SSH inbound and optional app port"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ingress_cidr_ssh]
  }

  # HTTP for Nginx
  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

// EC2 instance that runs the Java app
resource "aws_instance" "this" {
  ami                    = data.aws_ami.ubuntu_2404.id
  instance_type          = var.instance_type
  key_name               = var.key_name
  subnet_id              = data.aws_subnets.default.ids[0]

  // Attach the IAM instance profile created above so the EC2 instance assumes the role
  iam_instance_profile = aws_iam_instance_profile.this.name

  // Include the managed SG only
  vpc_security_group_ids = [aws_security_group.ssh.id]

  tags = {
    Name = var.instance_name
  }

  user_data = <<-EOF
#cloud-config
package_update: true
package_upgrade: true

# Install required packages
packages:
  - git
  - ec2-instance-connect
  - wget
  - curl
  - openjdk-21-jdk
  - nginx

# Create files on disk
write_files:
  - path: /etc/systemd/system/myapp.service
    permissions: '0644'
    owner: 'root:root'
    content: |
      [Unit]
      Description=My Java App
      After=network.target

      [Service]
      # Default Ubuntu user on this AMI
      User=ubuntu
      # Directory where the jar will live
      WorkingDirectory=/opt/myapp
      # Command to start your app
      ExecStart=/usr/bin/java -jar /opt/myapp/app.jar
      # Always restart on failure
      Restart=always
      RestartSec=5

      [Install]
      WantedBy=multi-user.target

  - path: /etc/nginx/sites-available/myapp.conf
    permissions: '0644'
    owner: 'root:root'
    content: |
      server {
        listen 80;
        server_name _;

        location / {
          proxy_pass http://127.0.0.1:8080;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
        }
      }

# Commands to run after packages are installed and files are written
runcmd:
  # Create app directory where GitHub Actions will copy the jar
  - mkdir -p /opt/myapp
  # Make sure the ubuntu user owns this directory
  - chown ubuntu:ubuntu /opt/myapp

  # Reload systemd so it picks up the new myapp.service file
  - systemctl daemon-reload

  # Enable the service so it starts automatically on boot
  - systemctl enable myapp.service

  # Try to start the service once at first boot
  # It will likely fail until the first deployment copies app.jar, which is fine
  - systemctl start myapp.service || true

  # Enable Nginx site and restart Nginx
  - ln -s /etc/nginx/sites-available/myapp.conf /etc/nginx/sites-enabled/myapp.conf
  - rm /etc/nginx/sites-enabled/default || true
  - nginx -t
  - systemctl enable nginx
  - systemctl restart nginx

  # Debug and sanity check files
  - echo "Hello from cloud-init" > /var/tmp/hello.txt
  - java -version | tee /var/tmp/java_version.txt
  EOF
}

// Associate an existing Elastic IP with this instance
resource "aws_eip_association" "this" {
  instance_id   = aws_instance.this.id
  allocation_id = var.eip_allocation_id
}
