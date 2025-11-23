variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}

variable "alb_name" {
  description = "Name of the shared ALB"
  type        = string
  default     = "shared-alb"
}

variable "api_target_group_name" {
  description = "Name for the target group used by the API (EC2 backend)"
  type        = string
  default     = "tg-api"
}

variable "web_target_group_name" {
  description = "Name for the target group used by the web app (Fargate backend)"
  type        = string
  default     = "tg-web"
}

variable "api_domain_name" {
  description = "Host name for API traffic"
  type        = string
  default     = "api.pocketlab.online"
}

variable "web_domain_name" {
  description = "Host name for web traffic"
  type        = string
  default     = "pocketlab.online"
}

variable "api_health_check_path" {
  description = "Health check path for API"
  type        = string
  default     = "/ping"
}

variable "web_health_check_path" {
  description = "Health check path for web app"
  type        = string
  default     = "/"
}
