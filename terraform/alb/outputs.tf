output "alb_arn" {
  description = "ARN of the shared ALB"
  value       = aws_lb.this.arn
}

output "alb_dns_name" {
  description = "DNS name of the shared ALB"
  value       = aws_lb.this.dns_name
}

output "alb_security_group_id" {
  description = "Security group id of the shared ALB"
  value       = aws_security_group.alb.id
}

output "api_target_group_arn" {
  description = "Target group ARN for API (EC2 backend)"
  value       = aws_lb_target_group.api.arn
}

output "web_target_group_arn" {
  description = "Target group ARN for web (Fargate backend)"
  value       = aws_lb_target_group.web.arn
}
